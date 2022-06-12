package pt.up.fe.comp.ollir;

import java.util.ArrayList;
import java.util.Stack;

public class OllirSymbolTable {
    public ArrayList<Integer> table;
    public Stack<Integer> ifStack;
    public Stack<Integer> elseStack;

    public Stack<Integer> whileStack;

    public OllirSymbolTable() {
        table = new ArrayList<>();
        ifStack = new Stack<>();
        elseStack = new Stack<>();
        whileStack = new Stack<>();
    }

    public String newTemp(){
        Integer n = 0;
        while(table.contains(n))
            n++;
        table.add(n);
        return "t".concat(n.toString(n));
    }

    public String newElse(){
        Integer n = 0;
        while(elseStack.contains(n))
            n++;
        elseStack.add(n);
        return "else".concat(n.toString(n));
    }

    public String newEndIf(){
        Integer n = 0;
        while(ifStack.contains(n))
            n++;
        ifStack.add(n);
        return "endif".concat(n.toString(n));
    }

    public Integer newWhile(){
        Integer n = 0;
        while(whileStack.contains(n))
            n++;
        whileStack.add(n);
        return n;
    }


}
