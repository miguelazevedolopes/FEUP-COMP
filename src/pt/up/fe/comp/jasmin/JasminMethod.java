package pt.up.fe.comp.jasmin;

import java.util.HashMap;
import java.util.Map;

import org.specs.comp.ollir.*;


public class JasminMethod {
    private final Method method;
    private final StringBuilder jasminCode;
    private final String className;
    private final Map<String, Descriptor> localVars;
    private int n_locals = 0;

    public JasminMethod(Method method, String className) {
        this.method = method;
        this.jasminCode = new StringBuilder();

        this.className = className;
        this.localVars = new HashMap<>();
        
        addLocalVariable("this", VarScope.FIELD, new Type(ElementType.CLASS));
    }

    public Method getMethod() {
        return method;
    }

    /*
    * Save method local variables
    */
    public void addLocalVariable(String variable, VarScope type, Type t) {
        if (!localVars.containsKey(variable)) {
            localVars.put(variable, new Descriptor(type, n_locals, t));
            n_locals++;
        }
    }

    private String getAccessModifiers(AccessModifiers accessModifier, boolean isConstructMethod){
        switch (accessModifier) {
            case PUBLIC:
                return  "public";
            case PRIVATE:
                return "private";
            case DEFAULT:
                return isConstructMethod ? "public" : "private";
            case PROTECTED:
                return "protected";
            default:
                return "";
        }
    }

    private void generateDeclaration(){
        jasminCode.append("\n\n.method ");

        String accessModifiers = getAccessModifiers(method.getMethodAccessModifier(), method.isConstructMethod());
        jasminCode.append(accessModifiers);

        if (method.isConstructMethod())
            jasminCode.append(" <init>");
        else {
            if (method.isStaticMethod()) jasminCode.append(" static");
            if (method.isFinalMethod()) jasminCode.append(" final");

            jasminCode.append(" ").append(method.getMethodName());
        }
        jasminCode.append("(").append(JasminUtils.getParametersFromMethod(this)).append(")");

    }

    public void generateReturnType() {
        String res = "";
        switch (method.getReturnType().getTypeOfElement()) {
            case INT32:
                res = "I";
                break;
            case BOOLEAN:
                res = "Z";
                break;
            case ARRAYREF:
                res = "[I";
                break;
            case OBJECTREF:
                res = this.className;
                break;
            case VOID:
                res = "V";
                break;
            default:
                break;
        }
        jasminCode.append(res);
    }

    public void generateCode(){

        generateDeclaration();
        generateReturnType();
        
        //TODO: Next are instructions




        jasminCode.append("\n.end method");
    } 

    
}
