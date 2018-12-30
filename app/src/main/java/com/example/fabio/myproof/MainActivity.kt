package com.example.fabio.myproof

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast

import com.facebook.stetho.Stetho

import java.io.IOException
import java.util.Collections

import io.github.kexanie.library.MathView

import com.example.fabio.myproof.R.layout.file
import com.example.fabio.myproof.Timing.Companion.duration
import com.example.fabio.myproof.Timing.Companion.time

class MainActivity : AppCompatActivity() {

    private var layout: LinearLayout? = null
    private var scrollView: ScrollView? = null
    private var scrollListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var inputText: AutoCompleteTextView? = null
    private var adapter: ArrayAdapter<String>? = null
    private var steps: Steps? = null
    private var sharedTemp: SharedPreferences? = null
    companion object {
        lateinit var store: Store
        lateinit var clipboard: Token
    }
    private var confirmDialog: AlertDialog.Builder? = null
    private val inputName: String
        get() {
            var name = inputText!!.text.toString().replace(" ", "_")
            name = name.replace("_+".toRegex(), "_")
            if (name.endsWith("_"))
                name = name.substring(0, name.length - 1)
            if (name.startsWith("_"))
                name = name.substring(1, name.length)
            return name
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layout = findViewById(R.id.formulas) as LinearLayout

        sharedTemp = getSharedPreferences("temp", Context.MODE_PRIVATE)
        store = Store()
        showMessage(store.path.toString())
        if (!store.load()) showMessage("Warning: loading commands error.")
        //store.loadAll();
        Stetho.initializeWithDefaults(this)

        setScrollView()
        setInputText()
        setConfirmDialog()

        time()
        loadSteps()
        time()
        showSteps()
        enableAutoScroll()
        showMessage(duration.toString())
    }

    protected fun onBackupClick(v: View) {
        val name = inputName
        when (v.tag.toString()) {
            "backup" -> if (store.backup())
                showMessage("Backup executed.")
            else
                showMessage("Backup error.")
            "restore" -> if (store.restore()) {
                showMessage("Restore executed.")
                setAdapter()
            } else
                showMessage("Restore error.")
            "import" -> if (name.isEmpty())
                showMessage("Insert pack name.")
            else if (store.importPack(name)) {
                showMessage("Pack $name imported.")
                setAdapter()
            } else
                showMessage("Importing error.")
            "save" -> if (name.isEmpty())
                showMessage("Insert pack name.")
            else if (store.savePack(name)) {
                showMessage("Pack $name saved.")
                setAdapter()
            } else
                showMessage("Saving error.")
            "new" -> if (store.newPack()) {
                showMessage("New pack.")
                setAdapter()
            } else
                showMessage("No new pack.")
        }
    }

    fun test(v: View) {
        steps!!.reduceAll()
        showSteps()
    }

    public override fun onResume() {
        super.onResume()
        steps!!.seek()
        showSteps()
        hideSoftKeyboard()
    }

    public override fun onDestroy() {
        super.onDestroy()
        //store.saveAll();  With this commands, sometimes the file commands is partially written only and this generates exceptions!
        saveSteps()
    }

    private fun showMessage(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    fun onDisplayClick(v: View) {
        clipboard = steps!!.activeStep()
        val intent = Intent(this, DisplayRuleActivity::class.java)
        startActivity(intent)
    }

    private fun setConfirmDialog() {
        val dialogSaveClickListener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val name = inputName
                    if (store.set(name, steps!!)) {
                        store.saveAll()
                        showMessage("Command $name saved.")
                    } else
                        showMessage("Command $name not saved.")
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                }
            }
        }
        confirmDialog = AlertDialog.Builder(this)
        confirmDialog!!.setPositiveButton("Yes", dialogSaveClickListener)
        confirmDialog!!.setNegativeButton("No", dialogSaveClickListener)
    }

    private fun setInputText() {
        if (inputText == null) {
            inputText = findViewById(R.id.input_text) as AutoCompleteTextView
            val actvClicked = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    setActiveTab("file")
                } else {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
            inputText!!.onFocusChangeListener = actvClicked
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        for (item in store.keys)
            adapter!!.add(item.replace("_", " "))
        inputText!!.setAdapter<ArrayAdapter<String>>(adapter)
    }

    private fun setAdapter() {
        if (adapter!!.count > 0)
            adapter!!.clear()
        Collections.sort(store.names)
        for (item in store.names)
            adapter!!.add(item.replace("_", " "))
        adapter!!.notifyDataSetChanged()
    }

    private fun setAdapter(name: String) {
        if (adapter!!.getPosition(name) < 0) {
            adapter!!.add(name.replace("_", " "))
            adapter!!.notifyDataSetChanged()
        }
    }

    private fun setScrollView() {
        scrollView = findViewById(R.id.scroll_view) as ScrollView
        scrollListener = ViewTreeObserver.OnGlobalLayoutListener { scrollView!!.post { scrollToActiveStep() } }
    }

    private fun hideSoftKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun clearInputTextClick(v: View) {
        inputText!!.setText("")
    }

    fun onClearClick(v: View) {
        inputText!!.clearFocus()
        saveSteps()
        layout!!.removeAllViews()
        steps = Steps()
        addMathView()
        showStep(0)
    }

    fun onSaveClick(v: View) {
        inputText!!.clearFocus()
        if (onEditSource()) {
            saveCommandSource(v)
            return
        }
        val name = inputName
        saveSteps()
        if (name.isEmpty()) {
            store.set("temp", steps!!)
            showMessage("Commands saved.")
        } else if (store.names.contains(name)) {
            confirmDialog!!.setMessage("Overwrite command $name?")
            confirmDialog!!.show()
        } else if (store.add(Command(name, steps!!))) {
            adapter!!.add(name.replace("_", " "))
            adapter!!.notifyDataSetChanged()
            showMessage("Command $name saved.")
        } else
            showMessage("Command $name not saved.")
        store.saveAll()
        store.update()
    }

    fun onOpenClick(v: View) {
        inputText!!.clearFocus()
        saveSteps()
        var name = inputName
        if (name.isEmpty()) name = "temp"
        val command = store.get(name)
        time()
        steps = Steps(command.definition!!)
        time()
        //layout.removeAllViews();
        showSteps()
        enableAutoScroll()
        showMessage(duration.toString())
    }

    fun onApplyClick(v: View) {
        inputText!!.clearFocus()
        var name = inputName
        if (name.isEmpty()) name = "temp"
        time()
        steps!!.activeStep().app(name)
        time()
        if (steps!!.seek() == 0) {
            steps!!.reduceFrom(steps!!.active)
            showStepsFrom(steps!!.active)
        } else {
            steps!!.reduceActiveStep()
            showActiveStep()
        }
        enableAutoScroll()
        inputText!!.setText("")
        showMessage(duration.toString())
    }

    fun onUpdateClick(v: View) {
        inputText!!.clearFocus()
        val name = inputName
        if (!name.isEmpty()) {
            store.update(name)
            showMessage("Command $name updated.")
        } else {
            store.update()
            showMessage("Commands restored.")
        }
        steps!!.reduceAll()
        showSteps()
    }

    fun onRenameClick(v: View) {
        inputText!!.clearFocus()
        val name = inputName
        if (name.isEmpty()) return
        if (steps!!.size > 1) return
        val command = steps!!.activeStep()[0]
        //store.renameSource(command.name,name);
        adapter!!.remove(command.name.replace("_", " "))
        setAdapter(name)
        showMessage("Command " + command.name + " renamed.")
        command.rename(name)
        store.saveAll()
        //store.save(command);
    }

    fun onCheckClick(v: View) {
        inputText!!.clearFocus()
        saveSteps()
        val check = steps!![steps!!.active]
        inputText!!.setText(check[0].name.replace("_", " "))
        val arg = Array(check[0].arity()) {check.leaf(0, it).reducedCopy(steps!!.reduced)}
        steps = Steps(check[0].definition!!, arg)
        layout!!.removeAllViews()
        showSteps()
        enableAutoScroll()
    }

    fun onUndoClick(v: View) {
        loadSteps()
        layout!!.removeAllViews()
        showSteps()
        enableAutoScroll()
    }

    fun onStepSourceClick(v: View) {
        if (steps!!.onSelect()) return
        inputText!!.clearFocus()
        val i = steps!!.active
        var source: String
        val editText: EditText
        val view = layout!!.getChildAt(i)
        if (view is MathView) {
            editText = inflateNewEditText()
            source = steps!!.activeStep().toString()
            editText.setText(i.toString() + ":" + source)
            layout!!.removeViewAt(i)
            layout!!.addView(editText, i)
        } else if (view is EditText) {
            editText = view
            source = editText.text.toString()
            source = source.substring(source.indexOf(":") + 1)
            steps!![i].put(0, source)
            steps!!.reduceFrom(i)
            layout!!.removeViewAt(i)
            layout!!.addView(inflateNewMathView(), i)
            editText.clearFocus()
            if (steps!!.seek() == 0) {
                steps!!.reduceFrom()
                showStepsFrom(steps!!.active)
            } else
                showActiveStep()
        }
    }

    fun onCommandSourceClick(v: View) {
        if (steps!!.onSelect()) return
        inputText!!.clearFocus()
        val name = inputName
        if (!name.isEmpty() && steps!!.isBlank) {
            layout!!.removeViewAt(0)
            val command = store.get(name)
            val etDescription = inflateNewEditText()
            etDescription.setText(command.description)
            layout!!.addView(etDescription)
            val etType = inflateNewEditText()
            etType.setText(command.getType())
            layout!!.addView(etType)
            val etLaTeX = inflateNewEditText()
            etLaTeX.setText(command.latex)
            layout!!.addView(etLaTeX)
            val etBrackets = inflateNewEditText()
            etBrackets.setText(command.getBrackets())
            layout!!.addView(etBrackets)
        }
    }

    private fun onEditSource(): Boolean {
        if (!steps!!.isBlank) return false
        if (layout!!.childCount != 4) return false
        for (i in 0..3)
            if (layout!!.getChildAt(i) !is EditText)
                return false
        return true
    }

    private fun saveCommandSource(v: View) {
        val name = inputName
        if (name.isEmpty()) return
        val command = store.get(name)
        command.description = (layout!!.getChildAt(0) as EditText).text.toString()
        command.type = (layout!!.getChildAt(1) as EditText).text.toString().replace(" ", "").split("->".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        command.latex = (layout!!.getChildAt(2) as EditText).text.toString()
        command.setBrackets((layout!!.getChildAt(3) as EditText).text.toString())
        if (store.add(command)) {
            adapter!!.add(command.name.replace("_", " "))
            adapter!!.notifyDataSetChanged()
        } //else store.save(command);
        store.saveAll()
        showMessage("Command source saved.")
        onClearClick(v)
    }

    private fun saveSteps() {
        val editor = sharedTemp!!.edit()
        editor.putString("steps", steps!!.toString())
        editor.commit()
    }

    private fun loadSteps() {
        val source = sharedTemp!!.getString("steps", "")
        steps = Steps(source!!)
    }

    protected fun onTabClick(v: View) {
        inputText!!.clearFocus()
        val name = v.tag.toString()
        setActiveTab(name)
    }

    private fun setActiveTab(name: String) {
        val frameHold = findViewById(R.id.frame_hold) as LinearLayout
        val tab: View?
        val inflation: Int
        when (name) {
            "file" -> {
                tab = findViewById(R.id.tab_file)
                inflation = file
            }
            "latin" -> {
                tab = findViewById(R.id.tab_latin)
                inflation = R.layout.latin
            }
            "upper_latin" -> {
                tab = findViewById(R.id.tab_upper_latin)
                inflation = R.layout.upper_latin
            }
            "greek" -> {
                tab = findViewById(R.id.tab_greek)
                inflation = R.layout.greek
            }
            "upper_greek" -> {
                tab = findViewById(R.id.tab_upper_greek)
                inflation = R.layout.upper_greek
            }
            "logic" -> {
                tab = findViewById(R.id.tab_logic)
                inflation = R.layout.logic
            }
            "sets" -> {
                tab = findViewById(R.id.tab_sets)
                inflation = R.layout.sets
            }
            "functions" -> {
                tab = findViewById(R.id.tab_functions)
                inflation = R.layout.functions
            }
            "pack" -> {
                tab = findViewById(R.id.tab_pack)
                inflation = R.layout.pack
            }
            else -> {
                tab = null
                inflation = 0
                return
            }
        }
        if (frameHold.childCount == 4)
            frameHold.removeViewAt(3)
        if (tab != null)
            frameHold.addView(tab, 3)
        else {
            val tabInflated = layoutInflater.inflate(inflation, frameHold, false)
            frameHold.addView(tabInflated, 3)
        }
    }

    internal fun showStep(i: Int) {
        if (i < 0 || i >= steps!!.size) return
        //if (!steps.reduced.get(i).edited) return;
        if (layout!!.getChildAt(i) is EditText) {
            val v = layout!!.getChildAt(i) as EditText
            v.setText(steps!![i].toString())
        } else if (layout!!.getChildAt(i) is MathView) {
            val v = layout!!.getChildAt(i) as MathView
            var code = steps!!.getLaTeXCodeStep(i)
            if (!code.contains("$$"))
                code = "\\($code\\)"
            v.text = code
        }
    }

    internal fun showActiveStep() {
        showStep(steps!!.active)
        //enableAutoScroll();
    }

    internal fun showStepsFrom(n: Int) {
        for (i in n until steps!!.size) {
            if (layout!!.getChildAt(i) == null)
                addMathView()
            showStep(i)
        }
        if (steps!!.size < layout!!.childCount)
            layout!!.removeViews(steps!!.size, layout!!.childCount - steps!!.size)
    }

    internal fun showSteps() {
        showStepsFrom(0)
    }

    private fun scrollToActiveStep() {
        val st = scrollView!!.scrollY
        val sb = st + scrollView!!.bottom - scrollView!!.top
        val t = layout!!.getChildAt(steps!!.active).top
        val b = layout!!.getChildAt(steps!!.active).bottom
        if (st > t)
            scrollView!!.scrollTo(0, t)
        else if (sb < b)
            scrollView!!.scrollTo(0, b - sb + st)
    }

    private fun enableAutoScroll() {
        scrollView!!.viewTreeObserver.addOnGlobalLayoutListener(scrollListener)
    }

    private fun disableAutoScroll() {
        scrollView!!.viewTreeObserver.removeOnGlobalLayoutListener(scrollListener)
    }

    protected fun onCommandClick(v: View) {
        val name = v.tag.toString()
        steps!!.activeStep().app(name)
        if (steps!!.seek() == 0) {
            steps!!.reduceFrom(steps!!.active)
            showStepsFrom(steps!!.active)
        } else
            showActiveStep()
    }

    protected fun onVariableClick(v: View) {
        val name = v.tag.toString()
        steps!!.activeStep().put(name)
        if (steps!!.seek() == 0) {
            steps!!.reduceFrom(steps!!.active)
            showStepsFrom(steps!!.active)
        } else
            showActiveStep()
    }

    protected fun onAcClick(v: View) {
        offSelect()
        steps!!.activeStep().put(0, "blank")
        steps!!.seek()
        showActiveStep()
    }

    protected fun onDelClick(v: View) {
        offSelect()
        steps!!.activeStep().put("blank")
        steps!!.seek()
        showStep(steps!!.active)
    }

    private fun offSelect() {
        if (steps!!.onSelect()) { // Exit from selection mode.
            steps!!.offSelect()
            showStep(steps!!.last)
        }
    }

    internal fun onStepTouch(v: View) {
        val i = layout!!.indexOfChild(v)
        if (i >= steps!!.active) {   // Activate a step below the active one.
            offSelect()
            steps!!.setActiveStep(i)
            showStep(steps!!.last)
            showStep(steps!!.active)
        } else if (i == steps!!.select) {   // Exit from selection mode.
            offSelect()
            steps!!.seek()
            showStep(steps!!.active)
            scrollToActiveStep()
        } else if (steps!!.onSelect()) {  // Change selected step during selection mode.
            steps!!.setSelectStep(i)
            showStep(steps!!.last) // Shows the last selected step.
            showStep(steps!!.select)     // Shows the selected step.
            showStep(steps!!.active)     // Shows the active step.
        } else if (steps!!.activeStep().root().isBlank) {  // Enter in selection mode.
            steps!!.setSelectStep(i)
            showStep(steps!!.select)
            showStep(steps!!.active)
        } else {    // Activate a step above the active one.
            steps!!.setActiveStep(i)
            showStep(steps!!.last)
            showStep(steps!!.active)
        }
    }

    protected fun onShiftLeafClick(v: View) {
        val t = Integer.parseInt(v.tag.toString())
        if (steps!!.onSelect()) {
            steps!!.shiftSelectLeaf(t)
            showStep(steps!!.select)
            showStep(steps!!.active)
        } else {
            steps!!.shiftActiveLeaf(t)
            showStep(steps!!.active)
        }
    }

    protected fun onShiftTokenClick(v: View) {
        val t = Integer.parseInt(v.tag.toString())
        if (steps!!.onSelect()) {
            steps!!.shiftSelectToken(t)
            showStep(steps!!.select)
            showStep(steps!!.active)
        } else {
            steps!!.shiftActiveToken(t)
            showStep(steps!!.active)
        }
    }

    protected fun onShiftStepClick(v: View) {
        val t = Integer.parseInt(v.tag.toString())
        if (steps!!.onSelect()) {
            steps!!.shiftSelectStep(t)
            showStep(steps!!.last)
            showStep(steps!!.select)
            showStep(steps!!.active)
        } else {
            steps!!.shiftActiveStep(t)
            showStep(steps!!.last)
            showStep(steps!!.active)
            scrollToActiveStep()
        }
    }

    protected fun onExeClick(v: View) {
        offSelect()
        steps!!.seek()
        if (steps!!.activeStep().isBlank)
            removeStep()
        else
            addStep()
    }

    private fun addStep() {
        steps!!.shiftReference(1)
        addMathView(steps!!.active + 1)
        steps!!.add(steps!!.active + 1, Token())
        steps!!.shiftActiveStep(1)
        showStep(steps!!.last)
        showStep(steps!!.active)
        enableAutoScroll()
    }

    private fun removeStep() {
        if (steps!!.active == 0) {// Never remove the first line.
            steps!!.activeStep().put(0, "blank")
            steps!!.seek()
            showActiveStep()
            return
        }
        steps!!.shiftReference(-1)
        steps!!.shiftActiveStep(-1)
        steps!!.removeAt(steps!!.active + 1)
        layout!!.removeViewAt(steps!!.active)
        showStep(steps!!.active)
    }

    fun activeMathView(): MathView {
        return layout!!.getChildAt(steps!!.active) as MathView
    }

    fun inflateNewMathView(): MathView {
        val myListener = View.OnTouchListener { view, event ->
            val maxX = layout!!.width.toFloat()
            val abscissa = event.x + view.x
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> disableAutoScroll()
                MotionEvent.ACTION_UP -> if (abscissa < maxX / 2)
                    onStepTouch(view)
                else
                    splitSequent(view)
                MotionEvent.ACTION_POINTER_DOWN -> {
                }
                MotionEvent.ACTION_POINTER_UP -> {
                }
                MotionEvent.ACTION_MOVE -> {
                }
            }
            true
        }

        val v = layoutInflater.inflate(R.layout.math_view_sample, layout, false)
        v.setOnTouchListener(myListener)
        return v as MathView
    }

    fun inflateNewEditText(): EditText {
        val v = layoutInflater.inflate(R.layout.edit_text_sample, layout, false)
        return v as EditText
    }

    fun addMathView(i: Int) {
        layout!!.addView(inflateNewMathView(), i)
    }

    fun addMathView() {
        layout!!.addView(inflateNewMathView())
    }

    fun splitSequent(v: View) {
        val i = layout!!.indexOfChild(v)
        steps!!.reduced[i].toggleSplitStyle()
        showStep(i)
    }

}
