package com.example.fabio.myproof;

import java.util.ArrayList;
import java.util.Arrays;

import static android.text.TextUtils.join;

/**
 * Created by fabio on 09/04/2017.
 */

class Steps extends ArrayList<Token> {
    ArrayList<Token> reduced;
    int active;
    int select;
    int last;

    Steps() {
        reduced = new ArrayList<Token>();
        add(new Token());
        active = 0;
        select = -1;
        last = 0;
    }
    Steps(String source) {
        // Provide an input structure on (a shallow copy, i.e. with same members of) steps.
        reduced = new ArrayList<>();
        if (source.isEmpty()) add(new Token());
        for (String line:source.split("\\n"))
            add(new Token(line));
        active = size()-1;
        select = -1;
        last = 0;
    }
    Steps(Token[] definition) {
        reduced = new ArrayList<>();
        if (definition.length==0) add(new Token());
        else for (Token item:definition)
            add(item.copy());
        active = size()-1;
        select = -1;
        last = 0;
    }
    Steps(Token[] definition,Token[] arg) {
        // Apply this definition to the list of (reduced) tokens arg.
        reduced = new ArrayList<>();
        int n = 0;
        if (definition.length==0) add(new Token());
        else for (Token step:definition)
            if (step.isGeneric()) {
                if (!step.reducedCopy(reduced).fits(arg[n]))
                    add(arg[n],new Token("error"));
                else add(arg[n],arg[n]);
                n++;
            } else add(step.copy());
        active = size()-1;
        select = -1;
        last = 0;
    }

    public boolean add(Token step) {
        step.index=0;
        super.add(step);
        reduced.add(step.reducedCopy(reduced));
        return getLastReducedStep().isComplete(); //reduced.get(size()-1).get(0).name.equals("error");
    }
    public void add(Token step,Token reducedStep) {
        step.index=0;
        reducedStep.index=0;
        super.add(step);
        reduced.add(reducedStep);
    }
    public void add(int i,Token step) {
        step.index=0;
        super.add(i,step);
        reduced.add(i,step.reducedCopy(reduced));
    }
    public Token remove(int i) {
        reduced.remove(i);
        return super.remove(i);
    }
    public void clear() {
        super.clear();
        reduced.clear();
    }

    public void set(String source) {
        clear();
        reduced.clear();
        if (source.isEmpty()) return;
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(source.split("\\n")));
        for (String line:list)
            add(new Token(line));
    }
    public Token set(int i,Token step) {
        Token output = super.set(i,step);
        reduced.set(i,step.reducedCopy(reduced));
        return output;
    }

    private int seek(int i) {
        Token a = get(i);
        Token b = get(i).copy();
        reduced.set(i,b);
        a.index = 0;
        b.index = 0;
        for (int j=b.size()-1;j>=0;j--) {
            if (b.index==0)
                for (int n=0;n<b.arity(j);n++)
                    if (!b.get(j,n).check(b.get(j).type[n])) {
                        a.index = a.arg(j,n);
                        b.index = b.arg(j,n);
                        break;
                    }
            if (b.get(j).isReducible())
                if (b.index<=j || b.next(j)<=b.index)
                    b.put(j,b.get(j).applyTo(b.getLeaf(j),reduced));
        }
        return a.index;
    }
    int seek() {return seek(active);}

    private void reduce(int i) {reduced.set(i,get(i).reducedCopy(reduced));}
    private void reduce() {reduce(active);}
    void reduceFrom(int i) {
        for (int j=i;j<size();j++)
            reduce(j);
    }
    void reduceFrom() {reduceFrom(active);}
    void reduceAll() {reduceFrom(0);}

    Token activeStep() {return get(active);}
    public Command activeToken(int i) {return get(active).get(i);}
    private Token selectStep() {return reduced.get(select);}
    private Command selectToken(int i) {return reduced.get(select).get(i);}
    void reduceActiveStep() {reduce(active);}

    public void set(Command command) {activeStep().put(command);}

    private int modulo(int i) {
        int mod;
        if (select <0) mod = size();
        else mod = active; // During selection mode only steps above the active one are navigable.
        int output = i;
        while (output<0)
            output += mod;
        output %= mod;
        return output;
    }

    void setActiveStep(int i) {
        activeStep().index=0;
        reduceActiveStep();
        last = active;
        active = modulo(i);
        seek();
    }
    void shiftActiveStep(int t) {setActiveStep(active+t);}
    void shiftActiveToken(int t) {
        activeStep().shift(t);
        reduceActiveStep();
    }
    void shiftActiveLeaf(int t) {
        activeStep().shiftLeaf(t);
        reduceActiveStep();
    }

    // Selection mode methods.
    boolean onSelect() {return select >=0;}
    void setSelectStep(int i) {
        if (select>=0) selectStep().index=0;
        last = select;
        select = modulo(i); // select is computed modulo active step index.
        selectStep().index=0;
        activeStep().put(new Command("§" + select));
        //activeStep().root().setOutput(selectToken(0).output());
        reduceActiveStep();
    }
    void shiftSelectStep(int t) {setSelectStep(select+t);}
    void shiftSelectToken(int t) {
        selectStep().shift(t);
        activeStep().put(new Command("§" + select));
        //activeStep().root().setOutput(selectToken(0).output());
        selectStep().putReference(activeStep());
        reduceActiveStep();
    }
    void shiftSelectLeaf(int t) {
        selectStep().shiftLeaf(t);
        activeStep().put(new Command("§" + select));
        selectStep().putReference(activeStep());
        reduceActiveStep();    }
    void offSelect() {
        last = select;
        select = -1;
    }
    void shiftReference(int t) {
        for (int i=active+1;i<size();i++)
            get(i).shiftReference(active,t);    // Shift references greater than lastActive by t.
    }

    String getLaTeXCodeStep(int i) {
        if (onSelect()) return reduced.get(i).getLaTeXCode(i==select);
        else return reduced.get(i).getLaTeXCode(i==active);
    }

    public String toString() {
        ArrayList<String> list = new ArrayList<>();
        for (Token step:this)
            list.add(step.toString());
        return join("\n",list);
    }

    boolean isBlank() {return size()==1 && get(0).isBlank();}
    Token getLastReducedStep() {
        int n = reduced.size();
        if (n<1) return new Token();
        else return reduced.get(n-1);
    }
}
