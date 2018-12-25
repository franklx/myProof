package com.example.fabio.myproof;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.facebook.stetho.Stetho;

import java.util.Collections;

import io.github.kexanie.library.MathView;

import static com.example.fabio.myproof.R.layout.file;
import static com.example.fabio.myproof.Timing.duration;
import static com.example.fabio.myproof.Timing.time;

public class MainActivity extends AppCompatActivity {

    private LinearLayout layout;
    private ScrollView scrollView;
    private ViewTreeObserver.OnGlobalLayoutListener scrollListener;
    private AutoCompleteTextView inputText;
    private ArrayAdapter<String> adapter;
    private Steps steps;
    private SharedPreferences sharedTemp;
    static Store store;
    static Token clipboard;
    private AlertDialog.Builder confirmDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layout = (LinearLayout) findViewById(R.id.formulas);

        sharedTemp = getSharedPreferences("temp",Context.MODE_PRIVATE);
        store = new Store();
        if (!store.load()) showMessage("Warning: loading commands error.");
        //store.loadAll();
        Stetho.initializeWithDefaults(this);

        setScrollView();
        setInputText();
        setConfirmDialog();

        time();loadSteps();time();
        showSteps();
        enableAutoScroll();
        showMessage(String.valueOf(duration));
    }

    protected void onBackupClick(View v) {
        String name = getInputName();
        switch (v.getTag().toString()) {
            case "backup":
                if (store.backup())
                    showMessage("Backup executed.");
                else showMessage("Backup error.");
                break;
            case "restore":
                if (store.restore()) {
                    showMessage("Restore executed.");
                    setAdapter();
                }
                else showMessage("Restore error.");
                break;
            case "import":
                if (name.isEmpty())
                    showMessage("Insert pack name.");
                else if (store.importPack(name)) {
                        showMessage("Pack "+name+" imported.");
                        setAdapter();
                    }
                    else showMessage("Importing error.");
                break;
            case "save":
                    if (name.isEmpty())
                        showMessage("Insert pack name.");
                    else if (store.savePack(name)) {
                        showMessage("Pack "+name+" saved.");
                        setAdapter();
                    }
                    else showMessage("Saving error.");
                break;
            case "new":
                if (store.newPack()) {
                    showMessage("New pack.");
                    setAdapter();
                }
                else showMessage("No new pack.");
                break;
        }
    }

    public void test(View v) {
        steps.reduceAll();
        showSteps();
    }

    public void onResume(){
        super.onResume();
        steps.seek();
        showSteps();
        hideSoftKeyboard();
    }
    public void onDestroy() {
        super.onDestroy();
        //store.saveAll();  With this commands, sometimes the file commands is partially written only and this generates exceptions!
        saveSteps();
    }

    private void showMessage(String message) {
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();
    }

    public void onDisplayClick(View v) {
        clipboard = steps.activeStep();
        Intent intent = new Intent(this,DisplayRuleActivity.class);
        startActivity(intent);
    }

    private void setConfirmDialog() {
        DialogInterface.OnClickListener dialogSaveClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        String name = getInputName();
                        if (store.set(name,steps)) {
                            store.saveAll();
                            showMessage("Command " + name + " saved.");
                        }
                        else showMessage("Command "+name+" not saved.");
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };
        confirmDialog = new AlertDialog.Builder(this);
        confirmDialog.setPositiveButton("Yes", dialogSaveClickListener);
        confirmDialog.setNegativeButton("No", dialogSaveClickListener);
    }
    private void setInputText() {
        if (inputText==null) {
            inputText = (AutoCompleteTextView) findViewById(R.id.input_text);
            View.OnFocusChangeListener actvClicked = new View.OnFocusChangeListener() {
                public void onFocusChange(View v, boolean hasFocus) {
                    if(hasFocus){
                        setActiveTab("file");
                    } else {
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            };
            inputText.setOnFocusChangeListener(actvClicked);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        for (String item:store.keySet())
            adapter.add(item.replace("_"," "));
        inputText.setAdapter(adapter);
    }
    private void setAdapter() {
        if (adapter.getCount()>0)
            adapter.clear();
        Collections.sort(store.names);
        for (String item:store.names)
            adapter.add(item.replace("_"," "));
        adapter.notifyDataSetChanged();
    }
    private void setAdapter(String name) {
        if (adapter.getPosition(name)<0) {
            adapter.add(name.replace("_", " "));
            adapter.notifyDataSetChanged();
        }
    }
    private void setScrollView() {
        scrollView = (ScrollView) findViewById(R.id.scroll_view);
        scrollListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                scrollView.post(new Runnable(){public void run(){scrollToActiveStep();}});
            }
        };
    }
    private void hideSoftKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    private String getInputName() {
        String name = inputText.getText().toString().replace(" ","_");
        name=name.replaceAll("_+","_");
        if (name.endsWith("_"))
            name = name.substring(0,name.length()-1);
        if (name.startsWith("_"))
            name = name.substring(1,name.length());
        return name;
    }

    public void clearInputTextClick(View v) {inputText.setText("");}
    public void onClearClick(View v) {
        inputText.clearFocus();
        saveSteps();
        layout.removeAllViews();
        steps = new Steps();
        addMathView();
        showStep(0);
    }
    public void onSaveClick(View v) {
        inputText.clearFocus();
        if (onEditSource()) {
            saveCommandSource(v);
            return;
        }
        String name = getInputName();
        saveSteps();
        if (name.isEmpty()) {
            store.set("temp", steps);
            showMessage("Commands saved.");
        } else if (store.containsKey(name)) {
            confirmDialog.setMessage("Overwrite command "+name+"?");
            confirmDialog.show();
        } else if (!name.matches("\\w+")) {
            showMessage("Invalid command name.");
            return;
        } else if (store.add(new Command(name,steps))) {
            adapter.add(name.replace("_", " "));
            adapter.notifyDataSetChanged();
            showMessage("Command " + name + " saved.");
        } else showMessage("Command " + name + " not saved.");
        store.saveAll();
        store.update();
    }
    public void onOpenClick(View v) {
        inputText.clearFocus();
        saveSteps();
        String name = getInputName();
        if (name.isEmpty()) name = "temp";
        Command command = store.get(name);
        time();steps = new Steps(command.definition);time();
        //layout.removeAllViews();
        showSteps();
        enableAutoScroll();
        showMessage(String.valueOf(duration));
    }
    public void onApplyClick(View v) {
        inputText.clearFocus();
        String name = getInputName();
        if (name.isEmpty()) name = "temp";
        time();steps.activeStep().app(name);time();
        if (steps.seek()==0) {
            steps.reduceFrom(steps.active);
            showStepsFrom(steps.active);
        } else {
            steps.reduceActiveStep();
            showActiveStep();
        }
        enableAutoScroll();
        inputText.setText("");
        showMessage(String.valueOf(duration));
    }
    public void onUpdateClick(View v) {
        inputText.clearFocus();
        store.update();
        showMessage("Commands restored.");
        steps.reduceAll();
        showSteps();
    }
    public void onRenameClick(View v) {
        inputText.clearFocus();
        String name = getInputName();
        if (!name.matches("\\w+")) {
            showMessage("Invalid command name.");
            return;
        }
        if (steps.size()>1) return;
        Command command = steps.activeStep().get(0);
        //store.renameSource(command.name,name);
        adapter.remove(command.name.replace("_"," "));
        setAdapter(name);
        showMessage("Command "+command.name+" renamed.");
        command.rename(name);
        //store.saveAll();
    }
    public void onCheckClick(View v) {
        inputText.clearFocus();
        saveSteps();
        Token check = steps.get(steps.active);
        inputText.setText(check.get(0).name.replace("_"," "));
        Token[] arg = new Token[check.get(0).arity()];
        for (int n=0;n<arg.length;n++)
            arg[n] = check.leaf(0,n).reducedCopy(steps.reduced);
        steps = new Steps(check.get(0).definition,arg);
        layout.removeAllViews();
        showSteps();
        enableAutoScroll();
    }
    public void onUndoClick(View v) {
        loadSteps();
        layout.removeAllViews();
        showSteps();
        enableAutoScroll();
    }
    public void onStepSourceClick(View v) {
        if (steps.onSelect()) return;
        inputText.clearFocus();
        int i = steps.active;
        String source;
        EditText editText;
        View view = layout.getChildAt(i);
        if (view instanceof MathView) {
            editText = inflateNewEditText();
            source = steps.activeStep().toString();
            editText.setText(i+":"+source);
            layout.removeViewAt(i);
            layout.addView(editText, i);
        } else if (view instanceof EditText) {
            editText = (EditText) view;
            source = editText.getText().toString();
            source = source.substring(source.indexOf(":")+1);
            steps.get(i).put(0,source);
            steps.reduceFrom(i);
            layout.removeViewAt(i);
            layout.addView(inflateNewMathView(),i);
            editText.clearFocus();
            if (steps.seek()==0) {
                steps.reduceFrom();
                showStepsFrom(steps.active);
            } else
                showActiveStep();
        }
    }
    public void onCommandSourceClick(View v) {
        if (steps.onSelect()) return;
        inputText.clearFocus();
        String name = getInputName();
        if (!name.matches("\\w+")) {
            showMessage("Invalid command name.");
            return;
        }
        if (steps.isBlank()) {
            layout.removeViewAt(0);
            Command command = store.get(name);
            EditText etDescription = inflateNewEditText();
            etDescription.setText(command.description);
            layout.addView(etDescription);
            EditText etType = inflateNewEditText();
            etType.setText(command.getType());
            layout.addView(etType);
            EditText etLaTeX = inflateNewEditText();
            etLaTeX.setText(command.latex);
            layout.addView(etLaTeX);
            EditText etBrackets = inflateNewEditText();
            etBrackets.setText(command.getBrackets());
            layout.addView(etBrackets);
        }
    }
    private boolean onEditSource() {
        if (!steps.isBlank()) return false;
        if (layout.getChildCount()!=4) return false;
        for (int i=0;i<4;i++)
            if (!(layout.getChildAt(i) instanceof EditText))
                return false;
        return true;
    }
    private void saveCommandSource(View v) {
        String name = getInputName();
        if (!name.matches("\\w+")) {
            showMessage("Invalid command name.");
            return;
        }
        Command command = store.get(name);
        command.description = ((EditText)layout.getChildAt(0)).getText().toString();
        command.type = ((EditText)layout.getChildAt(1)).getText().toString().replace(" ","").split("->");
        command.latex = ((EditText)layout.getChildAt(2)).getText().toString();
        command.setBrackets(((EditText)layout.getChildAt(3)).getText().toString());
        if (store.add(command)) {
            adapter.add(command.name.replace("_"," "));
            adapter.notifyDataSetChanged();
        } //else store.save(command);
        store.saveAll();
        showMessage("Command source saved.");
        onClearClick(v);
    }

    private void saveSteps() {
        SharedPreferences.Editor editor = sharedTemp.edit();
        editor.putString("steps",steps.toString());
        editor.commit();
    }
    private void loadSteps () {
        String source = sharedTemp.getString("steps","");
        steps = new Steps(source);
    }

    protected void onTabClick(View v) {
        inputText.clearFocus();
        String name = v.getTag().toString();
        setActiveTab(name);
    }
    private void setActiveTab(String name) {
        LinearLayout frameHold = (LinearLayout) findViewById(R.id.frame_hold);
        View tab;
        int inflation;
        switch (name) {
            case "file":
                tab = findViewById(R.id.tab_file);
                inflation = file;
                break;
            case "latin":
                tab = findViewById(R.id.tab_latin);
                inflation = R.layout.latin;
                break;
            case "upper_latin":
                tab = findViewById(R.id.tab_upper_latin);
                inflation = R.layout.upper_latin;
                break;
            case "greek":
                tab = findViewById(R.id.tab_greek);
                inflation = R.layout.greek;
                break;
            case "upper_greek":
                tab = findViewById(R.id.tab_upper_greek);
                inflation = R.layout.upper_greek;
                break;
            case "logic":
                tab = findViewById(R.id.tab_logic);
                inflation = R.layout.logic;
                break;
            case "sets":
                tab = findViewById(R.id.tab_sets);
                inflation = R.layout.sets;
                break;
            case "functions":
                tab = findViewById(R.id.tab_functions);
                inflation = R.layout.functions;
                break;
            case "pack":
                tab = findViewById(R.id.tab_pack);
                inflation = R.layout.pack;
                break;
            default:
                tab = null;
                inflation = 0;
                return;
        }
        if (frameHold.getChildCount()==4)
            frameHold.removeViewAt(3);
        if (tab != null)
            frameHold.addView(tab, 3);
        else {
            View tabInflated = getLayoutInflater().inflate(inflation,frameHold,false);
            frameHold.addView(tabInflated,3);
        }
    }

    void showStep(int i) {
        if (i<0||i>=steps.size()) return;
        //if (!steps.reduced.get(i).edited) return;
        if (layout.getChildAt(i) instanceof EditText) {
            EditText v = (EditText) layout.getChildAt(i);
            v.setText(steps.get(i).toString());
        } else if (layout.getChildAt(i) instanceof MathView) {
            MathView v = (MathView) layout.getChildAt(i);
            String code = steps.getLaTeXCodeStep(i);
            if (!code.contains("$$"))
                code = "\\(" + code + "\\)";
            v.setText(code);
        }
    }
    void showActiveStep() {
        showStep(steps.active);
        //enableAutoScroll();
    }
    void showStepsFrom(int n) {
        for (int i=n;i<steps.size();i++) {
            if (layout.getChildAt(i)==null)
                addMathView();
            showStep(i);
        }
        if (steps.size()<layout.getChildCount())
            layout.removeViews(steps.size(),layout.getChildCount()-steps.size());
    }
    void showSteps() {showStepsFrom(0);}
    private void scrollToActiveStep() {
        int st = scrollView.getScrollY();
        int sb = st + scrollView.getBottom() - scrollView.getTop();
        int t = layout.getChildAt(steps.active).getTop();
        int b = layout.getChildAt(steps.active).getBottom();
        if (st > t)
            scrollView.scrollTo(0, t);
        else if (sb < b)
            scrollView.scrollTo(0, b - sb + st);
    }
    private void enableAutoScroll() {
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(scrollListener);
    }
    private void disableAutoScroll() {
        scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(scrollListener);
    }

    protected void onCommandClick(View v) {
        String name = v.getTag().toString();
        steps.activeStep().app(name);
        if (steps.seek()==0) {
            steps.reduceFrom(steps.active);
            showStepsFrom(steps.active);
        } else
            showActiveStep();
    }
    protected void onVariableClick(View v) {
        String name = v.getTag().toString();
        steps.activeStep().put(name);
        if (steps.seek()==0) {
            steps.reduceFrom(steps.active);
            showStepsFrom(steps.active);
        } else
            showActiveStep();
    }
    protected void onAcClick(View v) {
        offSelect();
        steps.activeStep().put(0,"blank");
        steps.seek();
        showActiveStep();
    }
    protected void onDelClick(View v) {
        offSelect();
        steps.activeStep().put("blank");
        steps.seek();
        showStep(steps.active);
    }
    private void offSelect() {
        if (steps.onSelect()) { // Exit from selection mode.
            steps.offSelect();
            showStep(steps.last);
        }
    }
    void onStepTouch(View v) {
        int i = layout.indexOfChild(v);
        if (i>=steps.active) {   // Activate a step below the active one.
            offSelect();
            steps.setActiveStep(i);
            showStep(steps.last);
            showStep(steps.active);
        } else if (i==steps.select) {   // Exit from selection mode.
            offSelect();
            steps.seek();
            showStep(steps.active);
            scrollToActiveStep();
        } else if (steps.onSelect()) {  // Change selected step during selection mode.
            steps.setSelectStep(i);
            showStep(steps.last); // Shows the last selected step.
            showStep(steps.select);     // Shows the selected step.
            showStep(steps.active);     // Shows the active step.
        } else if (steps.activeStep().root().isBlank()) {  // Enter in selection mode.
            steps.setSelectStep(i);
            showStep(steps.select);
            showStep(steps.active);
        } else {    // Activate a step above the active one.
            steps.setActiveStep(i);
            showStep(steps.last);
            showStep(steps.active);
        }
    }

    protected void onShiftLeafClick(View v) {
        int t = Integer.parseInt(v.getTag().toString());
        if (steps.onSelect()) {
            steps.shiftSelectLeaf(t);
            showStep(steps.select);
            showStep(steps.active);
        } else {
            steps.shiftActiveLeaf(t);
            showStep(steps.active);
        }
    }
    protected void onShiftTokenClick(View v) {
        int t = Integer.parseInt(v.getTag().toString());
        if (steps.onSelect()) {
            steps.shiftSelectToken(t);
            showStep(steps.select);
            showStep(steps.active);
        } else {
            steps.shiftActiveToken(t);
            showStep(steps.active);
        }
    }
    protected void onShiftStepClick(View v) {
        int t = Integer.parseInt(v.getTag().toString());
        if (steps.onSelect()) {
            steps.shiftSelectStep(t);
            showStep(steps.last);
            showStep(steps.select);
            showStep(steps.active);
        } else {
            steps.shiftActiveStep(t);
            showStep(steps.last);
            showStep(steps.active);
            scrollToActiveStep();
        }
    }

    protected void onExeClick(View v) {
        offSelect();
        steps.seek();
        if (steps.activeStep().isBlank())
            removeStep();
        else addStep();
    }
    private void addStep() {
        steps.shiftReference(1);
        addMathView(steps.active+1);
        steps.add(steps.active+1,new Token());
        steps.shiftActiveStep(1);
        showStep(steps.last);
        showStep(steps.active);
        enableAutoScroll();
    }
    private void removeStep() {
        if (steps.active==0) {// Never remove the first line.
            steps.activeStep().put(0,"blank");
            steps.seek();
            showActiveStep();
            return;
        }
        steps.shiftReference(-1);
        steps.shiftActiveStep(-1);
        steps.remove(steps.active + 1);
        layout.removeViewAt(steps.active);
        showStep(steps.active);
    }

    public MathView activeMathView() {return (MathView) layout.getChildAt(steps.active);}
    public MathView inflateNewMathView() {
        View.OnTouchListener myListener = new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent event) {
                float maxX = layout.getWidth();
                float abscissa = event.getX() + view.getX();
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        disableAutoScroll();
                        break;
                    case MotionEvent.ACTION_UP:
                        if (abscissa<maxX/2)
                            onStepTouch(view);
                        else
                            splitSequent(view);
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                }
                return true;
            }
        };

        View v = getLayoutInflater().inflate(R.layout.math_view_sample,layout,false);
        v.setOnTouchListener(myListener);
        return (MathView) v;
    }
    public EditText inflateNewEditText() {
        View v = getLayoutInflater().inflate(R.layout.edit_text_sample,layout,false);
        return (EditText) v;
    }
    public void addMathView(int i) {
        layout.addView(inflateNewMathView(),i);
    }
    public void addMathView() {
        layout.addView(inflateNewMathView());
    }
    public void splitSequent(View v) {
        int i = layout.indexOfChild(v);
        steps.reduced.get(i).toggleSplitStyle();
        showStep(i);
    }

}
