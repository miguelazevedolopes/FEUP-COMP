package pt.up.fe.comp.symbolTable;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Method;

import pt.up.fe.comp.jmm.analysis.table.SymbolTableInterface;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class SymbolTable implements SymbolTableInterface{
    private List<String> imports = new ArrayList<>();
    private String className = null;
    private String superExtends = null;
    private List<Symbol> fields = new ArrayList<>();
    private List<Method> methods = new ArrayList<>();

    public SymbolTable() {
        this.imports = new ArrayList<>();
        this.className = null;
        this.superExtends = null;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String getSuper() {
        return superExtends;
    }

    public void setSuper(String superExtends) {
        this.superExtends = superExtends;
    }


    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    @Override
    public List<String> getMethods() {
        List<String> methodsStrings = new ArrayList<String>();
        for (Method method : this.methods){
            methodsStrings.add(method.getType().getName());
        }
        return methodsStrings;
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

    public void addImport(String importName){
        this.imports.add(importName);
    }

    public void addMethod(Method method) {
        this.methods.add(method);
    }
    
}
