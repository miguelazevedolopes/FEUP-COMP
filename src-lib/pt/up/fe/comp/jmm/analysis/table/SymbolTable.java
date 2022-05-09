package pt.up.fe.comp.jmm.analysis.table;

import java.util.List;
import java.util.stream.Collectors;

public interface SymbolTable {

    /**
     * @return a list of fully qualified names of imports
     */
    List<String> getImports();

    /**
     * @return the name of the main class
     */
    String getClassName();

    /**
     * 
     * @return the name that the classes extends, or null if the class does not extend another class
     */
    String getSuper();

    /**
     * 
     * @return a list of Symbols that represent the fields of the class
     */
    List<Symbol> getFields();

    /**
     * 
     * @return a list with the methods signatures of the class
     */
    List<String> getMethods();

    /**
     * 
     * @return the return type of the given method
     */
    Type getReturnType(String methodSignature);

    /**
     * 
     * @param methodSignature
     * @return a list of parameters of the given method
     */
    List<Symbol> getParameters(String methodSignature);

    /**
     * 
     * @param methodSignature
     * @return a list of local variables declared in the given method
     */
    List<Symbol> getLocalVariables(String methodSignature);

    Symbol getLocalVariable(String methodSignature,String variable);

    Symbol getField(String methodSignature,String fieldName);

    Symbol getParam(String methodSignature,String param);

    List<Method> getMethodList();
    /**
     * 
     * @return a String with information about the contents of the SymbolTable
     */
    default String print() {
        var builder = new StringBuilder();

        builder.append("Class: " + getClassName() + "\n");
        var superClass = getSuper() != null ? getSuper() : "java.lang.Object";
        builder.append("Super: " + superClass + "\n");
        builder.append("\nImports:");
        var imports = getImports();

        if (imports.isEmpty()) {
            builder.append(" <no imports>\n");
        } else {
            builder.append("\n");
            imports.forEach(fullImport -> builder.append(" - " + fullImport + "\n"));
        }

        var fields = getFields();
        builder.append("\nFields:");
        if (fields.isEmpty()) {
            builder.append(" <no fields>\n");
        } else {
            builder.append("\n");
            fields.forEach(field -> builder.append(" - " + field.print() + "\n"));
        }

        var methods = getMethods();
        builder.append("\nMethods: " + methods.size() + "\n");

        for (var method : methods) {
            builder.append(" - signature: ").append(method);
            builder.append("; returnType: ").append(getReturnType(method));

            // var returnType = getReturnType(method);
            var params = getParameters(method);
            // builder.append(" - " + returnType.print() + " " + method + "(");
            var paramsString = params.stream().map(param -> param != null ? param.print() : "<null param>")
                    .collect(Collectors.joining(", "));
            // builder.append(paramsString + ")\n");
            
            var localVariables =getLocalVariables(method);
            builder.append("; params: ").append(paramsString);
            var localVariablesString = localVariables.stream().map(localVar -> localVar != null ? localVar.print() : "<null var>")
                    .collect(Collectors.joining(", "));
            builder.append("; local vars: ").append(localVariablesString);
            builder.append("\n");
        }

        return builder.toString();
    }

}
