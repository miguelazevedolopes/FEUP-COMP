package pt.up.fe.comp.jasmin;

import java.util.HashMap;
import java.util.Map;

import com.javacc.parser.tree.Literal;

import org.specs.comp.ollir.*;

import freemarker.core.builtins.sourceBI;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
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

    public String getJasminType(ElementType type) {
        String res = "";
        //switch (method.getReturnType().getTypeOfElement()) {
        switch (type) {
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
        return res;
    }

    public String getCode(){

        generateDeclaration();

        jasminCode.append(getJasminType(method.getReturnType().getTypeOfElement()));
        
        jasminCode.append("\n\t.limit stack 99\n");
        jasminCode.append("\t.limit locals 99\n");

        for(var inst: method.getInstructions()){
           jasminCode.append(getCode(inst));
        }
        
        jasminCode.append("\n.end method");

        return jasminCode.toString();
    }

    private String getCode(Instruction instruction) {

        StringBuilder code = new StringBuilder();
        switch (instruction.getInstType()) {
            case CALL:
                code.append(getCode((CallInstruction) instruction));
                break;
            case RETURN:
                code.append(getCode((ReturnInstruction) instruction));
                break;
            // case ASSIGN:
            //     getCode((CallInstruction) instruction, false);
            //     break;
            // case PUTFIELD:
            //     getCode((PutFieldInstruction) instruction);
            //     break;
            // case BRANCH:
            //     getCode((CondBranchInstruction) instruction);
            //     break;
            // case GOTO:
            //     getCode((GotoInstruction) instruction);
            //     break;

            default:
                throw new NotImplementedException("Intruction Type not implemented: " + instruction.getInstType().toString());
        }

        return code.toString();
    } 


    //TODO Incomplete
    private String getCode(ReturnInstruction instruction){
        var code = new StringBuilder();

        Element op = instruction.getOperand();
        

        code.append("\n\treturn");

        return code.toString();

    }

    private String getCode(CallInstruction instruction){

        var code = new StringBuilder();
        /*TODO
        invokevirtual,
        invokeinterface,
        invokespecial,
        invokestatic,
        NEW,
        arraylength,
        ldc
         */
        switch(instruction.getInvocationType()){
            case invokestatic:
                code.append(getInvokeSataticCode(instruction));
                break;
            default:
                throw new NotImplementedException(instruction.getInvocationType());
        }


        return code.toString();

    }

    private String getInvokeSataticCode(CallInstruction instruction) {
        StringBuilder code = new StringBuilder();

        code.append("\tinvokestatic ");

        var methodClass = ((Operand)instruction.getFirstArg()).getName();
        Element secondArg = instruction.getSecondArg();
        instruction.getSecondArg().show();
        code.append(methodClass).append("/"); //TODO fully classified name
        code.append(((LiteralElement) secondArg).getLiteral().replace("\"", ""));

        code.append("(");
        
        //Operands
        for(var operand: instruction.getListOfOperands()){
            getArgumentsCode(operand);
        }
        code.append(")");

        code.append(getJasminType(instruction.getReturnType().getTypeOfElement()));
        return code.toString();


    }

    //TODO operands
    private void getArgumentsCode(Element operand) {
        throw new NotImplementedException(operand.toString());
    }
    
}
