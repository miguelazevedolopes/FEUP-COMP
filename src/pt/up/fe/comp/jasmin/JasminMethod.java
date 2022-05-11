package pt.up.fe.comp.jasmin;

import java.util.HashMap;
import java.util.Map;
import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.exceptions.NotImplementedException;


public class JasminMethod {
    private final Method method;
    private final StringBuilder jasminCode;
    private final ClassUnit ollir;
    private final String superName;
    private final String className;
    private final Map<String, Descriptor> localVars;
    private int n_locals = 0;
    private int stackMax;
    private int currStack;


    public JasminMethod(Method method, String className, String superName, ClassUnit ollir) {
        this.method = method;
        this.jasminCode = new StringBuilder();
        this.superName = superName;
        this.className = className;
        this.localVars = new HashMap<>();
        this.n_locals = 0;
        this.stackMax = 0;
        this.currStack = 0;
        this.ollir = ollir;

        
        addLocalVariable("this", VarScope.FIELD, new Type(ElementType.CLASS));
    }

    public String getSuperName() {
        return superName;
    }

    public String getClassName() {
        return className;
    }

    /**
     * Increment stack and update stackMax in case it's value is exceeded
     */
    public void incrementStack(){
        currStack++;
        if(currStack > stackMax) stackMax = currStack;
    }

    public void decrementStack(){
        currStack--;
    }

    public Method getMethod() {
        return method;
    }

    /**
     * Returns a local variable using key
     * @param dest used to get key
     * @param type 
     * @return
     */

    public Descriptor getLocalVariable(Element dest, VarScope type) {
        String key = ((Operand) dest).getName();
        if (localVars.get(key) == null) {
            addLocalVariable(key, type, dest.getType());
        }
        return localVars.get(key);
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

    
    public String getCode(){

        generateDeclaration();

        jasminCode.append(JasminUtils.getJasminType(method.getReturnType().getTypeOfElement(), className));
        
        jasminCode.append("\n\t.limit stack 99\n");
        jasminCode.append("\t.limit locals 99\n");

        // Get code for each instruction in method
        for(var inst: method.getInstructions()){
           jasminCode.append(new JasminInstruction(inst, this).getCode());
        }
        
        jasminCode.append("\n.end method");

        return jasminCode.toString();
    }
    
}
