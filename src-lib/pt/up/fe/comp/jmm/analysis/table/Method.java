package pt.up.fe.comp.jmm.analysis.table;

import java.util.List;

public class Method {
    private String methodSignature;
    private List<Symbol> parameters;
    private List<Symbol> localVariables;
    private Type type;

    public Method(String methodSignature, List<Symbol> parameters, List<Symbol> localVariables, Type type) {
        this.methodSignature = methodSignature;
        this.parameters = parameters;
        this.localVariables = localVariables;
        this.type = type;
    }
    public String getMethodSignature() {
        return methodSignature;
    }
    public Type getType() {
        return type;
    }
    public void setType(Type type) {
        this.type = type;
    }
    public List<Symbol> getLocalVariables() {
        return localVariables;
    }
    public void setLocalVariables(List<Symbol> localVariables) {
        this.localVariables = localVariables;
    }
    public List<Symbol> getParameters() {
        return parameters;
    }
    public void setParameters(List<Symbol> parameters) {
        this.parameters = parameters;
    }
    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }
}
