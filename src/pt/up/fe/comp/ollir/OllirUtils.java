package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class OllirUtils {
    
    public static String getCode(Symbol symbol){
        return symbol.getName() + "." + getCode(symbol.getType());
    }

    public static String getCode(Type type) {
        StringBuilder code = new StringBuilder();

        if(type.isArray()) { 
            code.append("array.");
        }

        code.append(getOllirType(type.getName()));

        return code.toString();
    }

    public static String getOllirType(String jmmType){

        switch(jmmType){
            case "TypeInt": return "i32";
            case "TypeString": return "String";
            case "void": return "V";
            default: return jmmType;
        }
    }


}
