package pt.up.fe.comp.jmm.analysis.table;

import java.util.List;

public class Method {
    private String methodSignature=null;
    private List<Symbol> parameters=null;
    private List<Symbol> localVariables=null;
    private Type type;

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
