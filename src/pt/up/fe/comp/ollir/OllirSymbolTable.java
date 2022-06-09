package pt.up.fe.comp.ollir;

import java.util.ArrayList;

public class OllirSymbolTable {
    public ArrayList<Integer> table;

    public OllirSymbolTable() {
        table = new ArrayList<>();
    }

    public String newTemp(){
        Integer n = 0;
        while(table.contains(n))
            n++;
        table.add(n);
        return "t".concat(n.toString(n));
    }

}
