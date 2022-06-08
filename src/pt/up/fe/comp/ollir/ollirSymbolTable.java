package pt.up.fe.comp.ollir;

import java.util.HashMap;

public class ollirSymbolTable {
    public HashMap<String, String> table = new HashMap<>();

    public ollirSymbolTable() {
    }

    public void put(String var, String temp){
        table.put(var,temp);
    }

    public String get(String var){
        return table.get(var);
    }

    public String newTemp(){
        Integer n = 0;
        while(table.containsValue("t".concat(n.toString())))
            n++;
        return "t".concat(n.toString(n));
    }

}
