package pt.up.fe.comp;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Method;

import pt.up.fe.comp.jmm.analysis.table.SymbolTableInterface;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class SymbolTable implements SymbolTableInterface{
    private final List<String> imports = new ArrayList<>();
    private String className = null;
    private String superExtends = null;
    private final List<Symbol> fields = new ArrayList<>();
    private final List<Method> methods = new ArrayList<>();

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superExtends;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    @Override
    public List<String> getMethods() {
        return null;
    }

    @Override
    public Type getReturnType(String methodSignature) {
        for (Method method : methods) {
            if(method.getMethodSignature()==methodSignature)
                return method.getType();
        }
        return null;
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        for (Method method : methods) {
            if(method.getMethodSignature()==methodSignature)
                return method.getParameters();
        }
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        for (Method method : methods) {
            if(method.getMethodSignature()==methodSignature)
                return method.getLocalVariables();
        }
        return null;
    }
    
}
