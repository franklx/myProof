package com.example.fabio.myproof;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Arrays;

import io.github.kexanie.library.MathView;

import static com.example.fabio.myproof.MainActivity.clipboard;
import static com.example.fabio.myproof.MainActivity.store;

/**
 * Created by fabio on 06/04/2017.
 */

public class DisplayRuleActivity  extends AppCompatActivity {

    private ViewGroup layout;
    private EditText searchText;
    private ArrayList<Command> displayedCommand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_rule);

        layout = (ViewGroup) findViewById(R.id.store_linear_layout);
        searchText = (EditText) findViewById(R.id.search_text);
        displayedCommand = new ArrayList<Command>();

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

    public void onSearchClick(View v) {
        searchText.clearFocus();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        ArrayList<String> list = new ArrayList<String>();
        ArrayList<String> required = new ArrayList<String>();
        required.addAll(Arrays.asList(searchText.getText().toString().split(" ")));
        displayedCommand.clear();
        for (String item:store.names) {
            list.clear();
            list.addAll(Arrays.asList(item.split("_")));
            if (list.containsAll(required))
                displayedCommand.add(store.get(item));
        }
        showList();
    }

    public void showList() {

        View.OnTouchListener myListener = new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent event) {
                float maxX = layout.getWidth();
                float abscissa = event.getX() + view.getX();
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        //view.setBackgroundColor(0xFF00FF00);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (abscissa < maxX/2)
                            onCommandTouch(view);
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

        layout.removeAllViews();
        String latexCode;
        for (Command item:displayedCommand) {
            MathView myMathView = (MathView) getLayoutInflater().inflate(R.layout.math_view_sample,layout,false);
            layout.addView(myMathView);
            myMathView.requestDisallowInterceptTouchEvent(true);
            myMathView.setOnTouchListener(myListener);
            latexCode = item.latex.replaceAll("#\\d+","");
            if (!latexCode.contains("$$"))
                latexCode = "\\(" + latexCode + "\\)";
            myMathView.setText(latexCode);
        }
    }

    public void onCommandTouch(View v) {
        int i = layout.indexOfChild(v);
        clipboard.app(displayedCommand.get(i));
        finish(); //DisplayRuleActivity.this.finish();
    }

}
