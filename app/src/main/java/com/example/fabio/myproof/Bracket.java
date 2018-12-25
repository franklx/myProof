package com.example.fabio.myproof;

import java.util.ArrayList;
import java.util.Arrays;

import static android.text.TextUtils.join;

/**
 * Created by fabio on 29/03/2017.
 */

class Bracket extends ArrayList<String> {

    Bracket() {}
    public void set(String source) {
        clear();
        if (source.isEmpty()) return;
        addAll(Arrays.asList(source.split(" ")));
    }
    public String toString() {return join(" ",this);}
    public boolean check(Command command) {
        boolean output = false;
        for (String item:this) {
            switch (item.replace("-","")) {
                case "Term":
                    item += "Variable";
                    break;
                case "Premise":
                    item += "Statement";
                    break;
                case "Token":
                    item += "VariableTermStatementPremiseSequent";
                    break;
            }
            if (item.startsWith("-")) {
                output &= !item.replace("-","").contains(command.output());
                output &= !item.replace("-","").equals(command.name);
            } else {
                output |= item.contains(command.output());
                output |= item.equals(command.name);
            }
        }
        return output;
    }
}
