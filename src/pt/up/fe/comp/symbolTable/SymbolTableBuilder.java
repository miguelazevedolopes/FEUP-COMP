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
            if(method.getMethodSignature().equals(methodSignature))
                return method.getType();
        }
        return null;
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        for (Method method : methods) {
            if(method.getMethodSignature().equals(methodSignature))
                return method.getParameters();
        }
        return new ArrayList<Symbol>();
    }

    @Override
    public Symbol getParam(String methodSignature, String param) {
        for (Method method : methods) {
            if(method.getMethodSignature().equals(methodSignature))
                for (Symbol symbol : method.getParameters()) {
                    if(symbol.getName().equals(param))
                        return symbol;
                }
        }
        return null;
    }
    
    @Override
    public int getParamPos(String methodSignature, String param) {
        for (Method method : methods) {
            if(method.getMethodSignature().equals(methodSignature))
                for (int i = 0; i < method.getParameters().size(); i++) {
                    Symbol symbol = method.getParameters().get(i);
                    if(symbol.getName().equals(param))
                        return i+1;
                }
        }
        return 0;
    }



    @Override
    public Symbol getParameter(String methodSignature, String varName){
        for (Method method : methods) {
            if(method.getMethodSignature().equals(methodSignature))
                for (Symbol param : method.getParameters()) {
                    if(param.getName().equals(varName)){
                        return param;
                    }
                }
        }
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        for (Method method : methods) {
            if(method.getMethodSignature().equals(methodSignature))
                return method.getLocalVariables();
        }
        return new ArrayList<Symbol>();
    }

    @Override
    public Symbol getLocalVariable(String methodSignature,String variable) {
        for (Method method : methods) {
            if(method.getMethodSignature().equals(methodSignature)){
                for (Symbol symbol : method.getLocalVariables()) {
                    if(symbol.getName().equals(variable))
                        return symbol;
                }
            }
        }
        return null;
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

    @Override
    public Symbol getField(String methodSignature, String fieldName) {
        for (Symbol field : fields) {
            if(field.getName().equals(fieldName)){
                return field;
            }
        }
        return null;
    }

    @Override
    public Boolean methodExists(String methodSignature) {
        for (Method method : methods) {
            if(method.getMethodSignature().equals(methodSignature))
                return true;

        }
        return false;
    }

    @Override
    public Boolean isField(String field){
        for(Symbol f : fields){
            if(f.getName().equals(field)){
                return true;
            }
        }
        return false;
    }


    @Override
    public String getVariableType(String methodSignature, String name) {
        Symbol s = getLocalVariable(methodSignature, name);
        if(s == null){
            s = getParam(methodSignature, name);
        }
        if(s==null){
            s=getField(methodSignature, name);
        }
        if(s==null){
            return "";
        }
        return s.getType().getName();
    }

    @Override
    public boolean isArray(String methodSignature, String name) {
        Symbol s = getLocalVariable(methodSignature, name);
        if(s == null){
            s = getParam(methodSignature, name);
        }
        if(s==null){
            s = getField(methodSignature, name);
        }
        if(s==null){
            return false;
        }
        return s.getType().isArray();
    }
    
}
