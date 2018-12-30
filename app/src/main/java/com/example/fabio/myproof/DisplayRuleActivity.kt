package com.example.fabio.myproof

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

import java.util.ArrayList
import java.util.Arrays

import io.github.kexanie.library.MathView

import com.example.fabio.myproof.MainActivity.Companion.clipboard
import com.example.fabio.myproof.MainActivity.Companion.store

/**
 * Created by fabio on 06/04/2017.
 */

class DisplayRuleActivity : AppCompatActivity() {

    private var layout: ViewGroup? = null
    private var searchText: EditText? = null
    private var displayedCommand: ArrayList<Command>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_rule)

        layout = findViewById(R.id.store_linear_layout) as ViewGroup
        searchText = findViewById(R.id.search_text) as EditText
        displayedCommand = ArrayList()

        /*searchText.addTextChangedListener(new TextWatcher() {
            ArrayList<String> list = new ArrayList<String>();
            public void afterTextChanged(Editable s) {
                displayedCommand.clear();
                for (Command item:store) {
                    list.clear();
                    list.addAll(Arrays.asList(item.name.split(" ")));
                    if (list.contains(searchText.getText().toString()))
                        displayedCommand.add(item);
                }
                showList();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });*/

    }

    fun onSearchClick(v: View) {
        searchText!!.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
        val list = ArrayList<String>()
        val required = ArrayList<String>()
        required.addAll(Arrays.asList(*searchText!!.text.toString().split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()))
        displayedCommand!!.clear()
        for (item in store.keys) {
            list.clear()
            list.addAll(Arrays.asList(*item.split("_".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()))
            if (list.containsAll(required))
                displayedCommand!!.add(store.get(item))
        }
        showList()
    }

    fun showList() {

        val myListener = View.OnTouchListener { view, event ->
            val maxX = layout!!.width.toFloat()
            val abscissa = event.x + view.x
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                }
                MotionEvent.ACTION_UP -> if (abscissa < maxX / 2)
                    onCommandTouch(view)
                MotionEvent.ACTION_POINTER_DOWN -> {
                }
                MotionEvent.ACTION_POINTER_UP -> {
                }
                MotionEvent.ACTION_MOVE -> {
                }
            }//view.setBackgroundColor(0xFF00FF00);
            true
        }

        layout!!.removeAllViews()
        var latexCode: String
        for (item in displayedCommand!!) {
            val myMathView = layoutInflater.inflate(R.layout.math_view_sample, layout, false) as MathView
            layout!!.addView(myMathView)
            myMathView.requestDisallowInterceptTouchEvent(true)
            myMathView.setOnTouchListener(myListener)
            latexCode = item.latex.replace("#\\d+".toRegex(), "")
            if (!latexCode.contains("$$"))
                latexCode = "\\($latexCode\\)"
            myMathView.text = latexCode
        }
    }

    fun onCommandTouch(v: View) {
        val i = layout!!.indexOfChild(v)
        clipboard.app(displayedCommand!![i])
        finish() //DisplayRuleActivity.this.finish();
    }

}
