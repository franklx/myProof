package com.example.fabio.myproof;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static com.example.fabio.myproof.MainActivity.store;

/**
 * Created by fabio on 13/03/2017.
 */

public class Token extends ArrayList<Command> {
    public int index;

    public Token() {
        add(store.BLANK);
        index = 0;
    }
    public Token(List<Command> list) {
        if (list.isEmpty()) add(store.BLANK);
        else addAll(list);
        index = 0;
        resize();
    }
    public Token(Command command, Token... arg) {
        add(command);
        for (Token item:arg) addAll(item);
        index = 0;
        resize();
    }
    public Token(String commandName, Token... arg) {this(store.get(commandName),arg);}
    public Token(String source) {
        if (source.isEmpty()) super.add(store.BLANK);
        else for (String item:source.split(" ")) {
            super.add(store.get(item));
        }
        index = 0;
        resize();
    }
    public Token(HashSet<Token> list) {
        // Make list into a premise.
        this();
        for (Token item:list) {
            if (item.isBlank());
            else if (isBlank()) put(item);
            else app("comma", item);
        }
    }

    private void resize() {
        int n = next(0);
        if (n<size()) removeRange(n,size());
        else while (n>size()) super.add(store.BLANK);
    }
    int arity(int i) {
        try{return get(i).arity();}
        catch (Exception e) {return 0;}
    }
    int next(int i) {
        int output = i+1;
        int steps = 1;
        while (steps>0) {
            int a = arity(i);
            output += a;
            steps += a-1;
            i++;
        }
        return output;
    }
    private Token subToken(int i) {return new Token(subList(i,next(i)));}
    public Command root() {
        return get(index);
    }
    Token[] getLeaf(int i) {
        Token[] output = new Token[arity(i++)];
        for (int n=0;n<output.length;n++)
            output[n] = new Token(subList(i,i=next(i)));
        return output;
    }
    Command get(int i,int n) {return get(arg(i,n));}
    int arg(int i,int n) {
        i++;
        while (n-->0) i = next(i);
        return i;
    }
    Token leaf(int i,int n) {return subToken(arg(i,n));}

    public void removeRange(int i,int j) {
        super.removeRange(i,j);
        if (index<i);
        else if(index<j)
            index = -1;
        else index = index-j+i;
    }
    public void add(int i,Command command) {
        super.add(i,command);
        if (index==-1) index = i;
        else if (index>=i) index++;
    }
    public boolean addAll(int i,Collection<? extends Command> token) {
        if (index==-1) index = i;
        else if (index>=i) index+=token.size();
        return super.addAll(i,token);
    }

    private Token cut(int i) {
        int j = next(i);
        Token output = new Token(subList(i,j));
        removeRange(i,j);
        return output;
    }

    void put(int i,Token token) {
        removeRange(i,next(i));
        addAll(i,token);
    }
    private void put(Token token) {put(index,token);}
    void put(String source) {put(new Token(source));}
    void put(int i,String source) {put(i,new Token(source));}

    private void put(int i,Command command) {
        removeRange(i,next(i));
        add(i++,command);
        for (int n=0;n<command.arity();n++)
            add(i,store.BLANK);
    }
    void put(Command command) {put(index,command);}

    public void app(int i,Command command,Token... arg) {
        Token arg0 = cut(i);
        add(i++,command);
        for (int n=0;n<command.arity();n++)
            if (n==0) {
                addAll(i,arg0);
                i += arg0.size();
            } else {
                addAll(i,arg[n-1]);
                i += arg[n-1].size();
            }
    }
    public void app(String name,Token... arg) {app(index,store.get(name),arg);}

    public void app(int i,Command command) {
        Token arg0 = cut(i);
        add(i++,command);
        for (int n=0;n<command.arity();n++)
            if (n==0) {
                addAll(i,arg0);
                i += arg0.size();
            } else add(i,store.BLANK);
    }
    public void app(int i,String name) {app(i,store.get(name));}
    public void app(Command command) {app(index,command);}
    public void app(String name) {app(index,store.get(name));}

    public Token copy() {
        Token output = (Token) clone();
        output.index = index;
        return output;
    }

    private void putReference(int i,Token active) {
        int j = 0;
        if (i<0||i>=size()) active.put(store.BLANK);
        while (j<size())
            if (j++==i) return;
            else {
                int k=0, temp;
                while ((temp=next(j))<=i) {j = temp; k++;}
                active.app(new Command("#"+k));
                active.root().setOutput(get(j).output());   // deprecated
            }

    }
    void putReference(Token active) {
        int i = 0;
        if (index>=size()) active.put(store.BLANK);
        while (i<size())
            if (i++==index) return;
            else {
                int k=0, temp;
                while ((temp=next(i))<=index) {i = temp; k++;}
                active.app(new Command("#"+k));
                active.root().setOutput(get(i).output());   // deprecated
            }

    }
    void shiftReference(int i,int t) {
        // Shift references greater than i by t.
        for (Command item:this)
            if (item.name.startsWith("§")) {
                int j = Integer.parseInt(item.name.substring(1));
                if (t<0 && j==i) item.setCommand(store.BLANK);
                if (j>i) item.setConstant("§" + (j+t));
            }
    }

    boolean isComplete() {return true;}
    boolean isGeneric() {
        // Check if this token contains a generic command.
        for (Command item:this)
            if (item.isGeneric())
                return true;
        return false;
    }
    private boolean compare(int i,int j) {
        // Compare the subtoken at i with that at j.
        int u=next(i);
        if (j+u-i!=next(j)) return false;
        for (int k=i;k<u;k++)
            if (!get(k).equals(get(j+k-i)))
                return false;
        return true;
    }
    private Token getBounderReference(int i) {
        // Return the reference of the command which bounds the token at i
        Token temp = new Token();
        int j = i;
        while (j-->0)
            for (int n=0;n<arity(j);n++)
                if (get(j).type[n].equals("Variable"))
                   if (i<next(j) && compare(i,arg(j,n))) {
                       putReference(arg(j,n),temp);
                       return temp;
                   }
        return temp;
    }
    private void reduce(List<Token> reducedSteps) {
        for (int i=size()-1;i>=0;i--)
            if (get(i).isReducible())
                if (index<=i || next(i)<=index)
                    put(i,get(i).applyTo(getLeaf(i),reducedSteps));
    }
    Token reducedCopy(List<Token> reducedSteps) {
        Token output = copy();
        output.reduce(reducedSteps);
        return output;
    }

    boolean oldfits(Token t) {
        // Check if token t fits into this token.
        int i=0,j=0;
        while (i<size())
            if (get(i).isGeneric())
                if (t.get(j).check(get(i).output())) {
                    i = next(i);
                    j = t.next(j);
                } else return false;
            else if (t.get(j).equals(get(i))) {
                i++;
                j++;
                } else return false;
        return true;
    }
    boolean fits(Token t) {
        // Check if token t fits into this token.
        int i=0,j=0;
        Token temp;
        while (i<size())
            if (get(i).isGeneric()) // The subtoken at i is generic
                if (t.get(j).check(get(i).output())) {
                    i = next(i);
                    j = t.next(j);
                } else return false;
            else if (!(temp=getBounderReference(i)).isBlank())  // The subtoken at i is bounded
                if (temp.equals(t.getBounderReference(j))) {
                    i = next(i);
                    j = t.next(j);
                } else return false;
            else if (t.get(j).equals(get(i))) {
                i++;
                j++;
            } else return false;
        return true;
    }
    private List<Token> getBoundedVariables(int i) {
        // Return the list of variables bounded by the command located at position i.
        ArrayList<Token> output = new ArrayList<>();
        for (int n=0;n<arity(i);n++)
            if (get(i).type[n].equals("Variable"))
                output.add(leaf(i,n));
        return output;
    }
    private boolean bounds(int i,Token variable) {
        // Check if the command at i bounds the given variable.
        for (int n=0;n<arity(i);n++)
            if (get(i).type[n].equals("Variable"))
                if (contains(arg(i,n),variable))
                    return true;
        return false;
    }
    boolean hasFreeOccurrenceOf(int i,Token variable) {
        int j=i,u=next(i);
        while (j<u) {
            if (bounds(j,variable))
                j = next(j);
            else if (contains(j,variable))
                return true;
            else j++;
        }
        return false;
    }
    private void generalSubstitution(int i,Token variable,Token term) {
        int j=i;
        while (j<next(i))
            if (contains(j,variable)) {
                put(j,term);
                j+=term.size();
            } else if (!hasFreeOccurrenceOf(j,variable))
                j=next(j);
            else {
                List<Token> bounded = getBoundedVariables(j);
                for (Token item:bounded) {
                    Token temp=item.copy();
                    while (term.hasFreeOccurrenceOf(0,temp)
                            || hasFreeOccurrenceOf(j,temp)
                            || (!temp.equals(item) && bounded.contains(temp)))
                        temp.app(0, "next");
                    if (!temp.equals(item))
                        for (int n=0;n<arity(j);n++)
                            generalSubstitution(arg(j,n),item,temp);
                }
                j++;
            }
    }
    Token substitution(Token variable,Token term) {
        Token output = copy();
        output.generalSubstitution(0,variable,term);
        return output;
    }

    private boolean contains(int i,Token t) {
        for (int j=0;j<t.size();j++)
            if (i+j>=size()) return false;
            else if (!get(i+j).equals(t.get(j))) return false;
        return true;
    }

    private int modulo(int i,int x) {
        if (i<0||i>=size()) return 0;
        int m = next(i)-i;
        while (x<i) x+=m;
        return i+(x-i)%m;
    }
    private int getParent(int i) {
        int j=i;
        while (j-->0)
            if (i<next(j))
                break;
        return j;
    }
    public void shift(int t) {index = modulo(0,index+t);}
    void shiftLeaf(int t) {
        int i = getParent(index);
        int m = arity(i);
        if (m==0) return;
        while (t<0)
            t+=m;
        t%=m;
        while (t-->0) {
            index = modulo(i,next(index));
            if (index==i)
                index++;
        }
        //index = modulo(index);
    }

    public boolean isBlank() {return get(0).isBlank();}
    public boolean isError() {
        for (Command item:this)
            if (item.name.equals("error"))
                return true;
        return false;
    }
    HashSet<Token> toHashSet() {
        HashSet<Token> output = new HashSet<>();
        Token temp = copy();
        Command comma = store.get("comma");
        while (temp.contains(comma)) temp.remove(comma);
        while (!temp.isEmpty())
            output.add(temp.cut(0));
        return output;
    }
    private ArrayList<String> toStringList() {
        ArrayList<String> output = new ArrayList<>();
        for (Command item:this)
            output.add(item.toString());
        return output;
    }
    private ArrayList<String> toIntegerStringList() {
        ArrayList<String> output = new ArrayList<>();
        for (Command item:this) {
            int i=store.names.indexOf(item.name);
            if (i<0)
                output.add(item.name);
            else
                output.add(Integer.toString(i));
        }
        return output;
    }

    public int hashCode() {
        return toString().hashCode();
    }
    public boolean equals(Object o) {return hashCode()==o.hashCode();}
    public String toString() {return TextUtils.join(" ",toStringList());}
    public String toIntegerString() {return TextUtils.join(" ",toIntegerStringList());}

    String getLaTeXCode(boolean active) {
        String[] temp = new String[size()];
        for (int i=size()-1;i>=0;i--) {
            temp[i]=get(i).latex;
            int k = i+1;
            for (int j=0;j<arity(i);j++) {
                if (get(i).brackets[j].check(get(k)))
                    temp[k]="\\left("+temp[k]+"\\right)";
                temp[i]=temp[i].replace("#"+(j+1),temp[k]);
                k = next(k);
            }
            if (active && i==index)
                if (get(i).name.equals("split")) {
                    String[] list = temp[i].split("\\\\\\\\");
                    for (int u=0;u<list.length;u++)
                        list[u] = "\\color{Red}{"+list[u]+"}";
                    temp[i] = TextUtils.join("\\\\",list)+"|";
                } else if (!temp[i].contains("$$"))
                    temp[i]="\\color{Red}{"+temp[i]+"}|";
        }
        return temp[0];
    }
    String getLaTeXCode() {return getLaTeXCode(false);}
    void toggleSplitStyle() {
        for (int i=0;i<size();i++)
            switch (get(i).name) {
                case "sequent":
                    set(i,store.get("therefore"));
                    break;
                case "therefore":
                    set(i,store.get("sequent"));
                    break;
                case "comma":
                    set(i,store.get("split"));
                    break;
                case "split":
                    set(i,store.get("comma"));
                    break;
            }
    }
    void mergeStyle() {
        for (int i=0;i<size();i++)
            switch (get(i).name) {
                case "therefore":
                    set(i,store.get("sequent"));
                    break;
                case "split":
                    set(i,store.get("comma"));
                    break;
            }
    }
}