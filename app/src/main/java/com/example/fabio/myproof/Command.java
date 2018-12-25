package com.example.fabio.myproof;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static android.text.TextUtils.join;
import static com.example.fabio.myproof.MainActivity.store;

/**
 * Created by fabio on 13/03/2017.
 */

public class Command implements Serializable {
    public String name;
    String description;
    public String[] type;
    String latex;
    Bracket[] brackets;
    Token[] definition;
    private String[] source;

    Command() { //Construct a blank command
        name = "blank";
        description = "";
        type = new String[]{"Premise"};
        latex = "";
        brackets = new Bracket[0];
        definition = new Token[0];
    }
    Command(String constant) {
        definition = new Token[0];
        setConstant(constant);
    }
    Command(String commandName,Steps commandDefinition) {
        name = commandName;
        setDefinition(commandDefinition);
    }
    Command(String commandName,List<String> fileSource) {
        name = commandName;
        while (fileSource.size()<4)
            fileSource.add("");
        description = fileSource.get(0);
        type = fileSource.get(1).split("->");
        latex = fileSource.get(2);
        setBrackets(fileSource.get(3));
        source = new String[fileSource.size()-4];
        for (int i=0;i<source.length;i++)
            source[i] = fileSource.get(i+4);
    }

    void loadDefinition() {
        definition = new Token[source.length];
        for (int i=0;i<definition.length;i++)
            definition[i] = new Token(source[i]);
        source = null;
    }
    public void set(List<String> fileSource) {
        description = fileSource.get(0).replace("  "," blank ");
        type = fileSource.get(1).split("->");
        latex = fileSource.get(2);
        setBrackets(fileSource.get(3));
        definition = new Token[fileSource.size()-4];
        for (int i=0;i<definition.length;i++) {
            definition[i] = new Token(fileSource.get(i + 4));
        }
    }
    public void setCommand(Command command) {
        name = command.name;
        description = command.description;
        type = command.type;
        latex = command.latex;
        brackets = command.brackets;
        definition = command.definition;
    }
    void setConstant(String s) {
        name = s; //name = s.replace("\\","");
        description = "";
        if(s.startsWith("#")) {
            type = new String[]{"Token","Token"};
            latex = "\\sharp "+s.substring(1)+"(#1)";
        } else if (s.startsWith("§")) {
            type = new String[]{"Token"};
            latex = s.replace("§","");
        } else {
            type = new String[]{"Variable"};
            latex = s;
        }
        setBrackets();
        definition = new Token[0];
    }
    void setDefinition(Steps steps) {
        definition = new Token[steps.size()];
        ArrayList<String> typeList = new ArrayList<>();
        ArrayList<String> argsList = new ArrayList<>();
        ArrayList<String> seqsList = new ArrayList<>();
        String output, code;
        for (int i=0;i<steps.size();i++) {
            definition[i] = steps.get(i);
            if (steps.get(i).isGeneric()) {
                output = steps.reduced.get(i).get(0).output();
                code = steps.reduced.get(i).getLaTeXCode();
                typeList.add(output);
                argsList.add(code+":#"+typeList.size());
                if (output.equals("Sequent"))
                    seqsList.add(code);
            }
        }
        Token conclusion = steps.getLastReducedStep();
        String args = "\\(\\begin{array}{l}"
                + join("\\\\ ",argsList)
                + "\\end{array}\\)";
        if (typeList.isEmpty()) latex = conclusion.getLaTeXCode();
        else latex = "$$\\frac{"
                    + join("\\quad ",seqsList)
                    + "}{"
                    + conclusion.getLaTeXCode()
                    + "}\\text{("
                    + name.replace("_"," ")
                    + ")}$$"
                    + args;
        typeList.add(conclusion.get(0).output());
        type = typeList.toArray(new String[typeList.size()]);
        setBrackets();
        if (argsList.isEmpty()) description = conclusion.toString();
    }

    private void setBrackets() {setBrackets(arity());}
    private void setBrackets(int n) {
        if (brackets==null||brackets.length<n)
            brackets = new Bracket[n];
        else return;
        for (int i=0;i<n;i++)
            brackets[i] = new Bracket();
    }
    void setBrackets(String source) {
        setBrackets();
        String[] array = source.split(",");
        int n = array.length;
        if (brackets.length<n) n = brackets.length;
        for (int i=0;i<n;i++)
            brackets[i].set(array[i]);
    }
    String getBrackets() {
        String list[] = new String[brackets.length];
        for (int i=0;i<brackets.length;i++)
            list[i] = brackets[i].toString();
        return join(",",list);
    }

    void setOutput(String output) {type[arity()] = output;}
    String getType() {
        return join("->",type);
    }
    int arity() {
        return type.length-1;
    }
    String output() {
        return type[arity()];
    }

    boolean check(String required) {
        // Check if this command has the required output.
        //if (output().equals("Token")) return true;  // Non-reduced token.
        switch (required) {
            case "Term":
                required += "Variable";
                break;
            case "Premise":
                required += "Statement";
                break;
            case "Token":
                return true;
        }
        return required.contains(output());
    }
    boolean equals(Command command) {return name.equals(command.name);}
    boolean isBlank() {return name.equals("blank");}
    boolean isError() {return name.equals("error");}
    boolean isGeneric() {
        switch (name) {
            case "display_premise":case "premise":
            case "display_statement":case "statement":
            case "display_sentence":case "sentence":
            case "display_term":case "term":
            case "display_constant":case "constant":
            case "display_variable":case "variable":
                return true;
        }
        return false;
    }
    boolean isReducible() {return definition.length>0 || arity()>0 || name.startsWith("§");}

    private Token applyDefinitionTo(Token[] arg) {
        // Apply these steps to the list of (reduced) tokens arg.
        ArrayList<Token> reduced = new ArrayList<>();
        if (arg.length==0 && !description.isEmpty()) return new Token(description);
        int n = 0;
        for (Token step:definition)
            if (step.isGeneric()) {
                if (!step.reducedCopy(reduced).fits(arg[n]))
                    return new Token("error");
                else reduced.add(arg[n++]);
            } else {
                reduced.add(step.reducedCopy(reduced));
                if (reduced.get(reduced.size() - 1).isError())
                    return new Token("error");
            }

        if (arg.length==0) {
            description = reduced.get(reduced.size()-1).toString();
            store.save(this);
        }
        return reduced.get(reduced.size()-1);
    }
    Token applyTo(Token[] arg, List<Token> reducedSteps) {
        // Apply this command to the (reduced) token in arg.
        // Explicits references over reducedSteps.
        // Check argument type.
        for (int n=0;n<arg.length;n++)
            if (!arg[n].root().check(type[n]))
                return new Token("error");
        try {
            // root has definition.
            if (definition.length>0)
                return applyDefinitionTo(arg);
            // root is a reference.
            if (name.startsWith("§")) {
                int i = Integer.parseInt(name.substring(1));
                Token temp = reducedSteps.get(i).copy();
                temp.mergeStyle();
                return temp;
            }
            // root is an argument.
            if (name.startsWith("#")) {
                int n = Integer.parseInt(name.substring(1));
                return arg[0].leaf(0,n);
            }
            //
            if (name.startsWith("display_"))
                return new Token(name.replace("display_",""),arg);
            // root is a primitive command.
            switch (name) {
                case "comma":   // Merge two premises.
                    HashSet<Token> premise = arg[0].toHashSet();
                    premise.addAll(arg[1].toHashSet());
                    return new Token(premise);
                case "drop":   // Drop the statement from the premise.
                    premise = arg[0].toHashSet();
                    premise.remove(arg[1]);
                    return new Token(premise);
                case "new":
                    Token temp = arg[0].copy();
                    while (arg[1].hasFreeOccurrenceOf(0,temp)) //arg[1].toTokenList().contains(temp)
                        temp.app("next");
                    return temp;
                case "free":
                    if (!arg[0].hasFreeOccurrenceOf(0,arg[1]))   // Reduces if arg[0] doesn't contains free occurrence of arg[1].
                        return arg[0];
                    else break;
                case "substitute":
                    return arg[0].substitution(arg[1],arg[2]);
            }
        } catch (Exception e) {return new Token("error");}
        return new Token(this,arg);
    }

    public int hashCode() {return toString().hashCode();}
    public String toString() {return name.replace(" ","_");}
    public String getSource() {
        ArrayList<String> temp = new ArrayList<>();
        temp.add(description);
        temp.add(getType());
        temp.add(latex);
        temp.add(getBrackets());
        for (Token step:definition)
            temp.add(step.toString());
        return join("\n",temp);
    }
    public String getIntegerSource() {
        ArrayList<String> temp = new ArrayList<>();
        temp.add("@"+store.names.indexOf(name)+":"+name);
        temp.add(description);
        temp.add(getType());
        temp.add(latex);
        temp.add(getBrackets());
        for (Token step:definition)
            temp.add(step.toIntegerString());
        return join("\n",temp);
    }
    void rename(String newName) {
        latex = latex.replace(name.replace("_"," "),newName.replace("_"," "));
        name = newName;
    }
}