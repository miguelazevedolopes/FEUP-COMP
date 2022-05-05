package pt.up.fe.comp.symbolTable;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Method;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class SymbolTableBuilder implements SymbolTable{
    private List<String> imports;
    private String className;
    private String superExtends;
    private List<Symbol> fields;
    private List<Method> methods;

    public SymbolTableBuilder() {
        this.imports = new ArrayList<>();
        this.className = new String();
        this.superExtends = new String();
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
    }

    public boolean hasMethod(String methodName){
        for (Method method : methods) {
            if(method.getMethodSignature().equals(methodName))
                return true;
        }
        return false;
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
            methodsStrings.add(method.getMethodSignature());
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
        return new ArrayList<Symbol>();
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        for (Method method : methods) {
            if(method.getMethodSignature()==methodSignature)
                return method.getLocalVariables();
        }
        return new ArrayList<Symbol>();
    }
    
    @Override
    public List<Method> getMethodList(){
        return methods;
    }

    public void addImport(String importName){
        this.imports.add(importName);
    }

    public void addMethod(Method method) {
        this.methods.add(method);
    }

    public void setFields(List<Symbol> fields2) {
        this.fields = fields2;
    }

    public void setExtends(String string) {
        this.superExtends = string;
    }
    
}
