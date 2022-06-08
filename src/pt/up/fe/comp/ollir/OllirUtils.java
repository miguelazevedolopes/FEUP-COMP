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
            case "int":
            case "TypeInt":
            case "IntegerLiteral": return "i32";
            case "TypeString": return "String";
            case "boolean":
            case "TypeBoolean": return "bool";
            case "void": return "V";
            case "TypeIntArray": return "array.i32";
            case "ANDD": return "bool";
            case "SUM":
            case "SUB":
            case "LESSTHAN":
            case "MUL":
            case "DIV": return "i32";
            default: return jmmType;
        }
    }


    public static String getCode(String op) {
        StringBuilder code = new StringBuilder();
        switch (op){
            case "SUM" -> code.append("+.i32");
            case "SUB" -> code.append("-.i32");
            case "MUL" -> code.append("*.i32");
            case "DIV" -> code.append("/.i32");
            case "LESSTHAN" -> code.append("<.i32");
            case "ANDD" -> code.append("&&.bool");
        }
        return code.toString();
    }
}
