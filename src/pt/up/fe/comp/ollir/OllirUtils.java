package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

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
            case "IntegerLiteral": return "i32";
            case "TypeString": return "String";
            case "TypeBoolean": return "bool";
            case "void": return "V";
            case "TypeIntArray": return "array.i32";
            case "ANDD": return "&&";
            case "SUM": return "+";
            case "SUB": return "-";
            case "LESSTHAN": return "<";
            case "MUL": return "*";
            case "DIV": return "/";
            default: return jmmType;
        }
    }




}
