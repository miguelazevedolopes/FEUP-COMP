package pt.up.fe.comp.jasmin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pt.up.fe.comp.jmm.report.Report;
import org.specs.comp.ollir.*;

import pt.up.fe.comp.jasmin.Instructions.JasminInstruction;
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
    private List<Report> reports;
    private int nNranches;


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
        this.reports = new ArrayList<>();
        this.nNranches = 0;
        
        addLocalVariable("this", VarScope.FIELD, new Type(ElementType.CLASS));
    }

    public String getSuperName() {
        return superName;
    }

    public String getClassName() {
        return className;
    }

    public int getNBranches() {
        return nNranches;
    }

    public void incrementBranches(){
        this.nNranches++;
    }

    /**
     * Increment stack and update stackMax in case it's value is exceeded
     */
    public void updateMaxStack(int popSize, int pushSize) {
        currStack -= popSize;
        currStack += pushSize;
        stackMax = Math.max(stackMax, currStack);
    }


    public void decrementStack(){
        currStack--;
    }

    public void incrementStack() {
        currStack++;
        if (currStack > stackMax)
            stackMax = currStack;
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

    public Descriptor getLocalVariableByKey(Element dest, VarScope type) {
        String key = ((Operand) dest).getName();
        if (localVars.get(key) == null) {
            addLocalVariable(key, type, dest.getType());
        }
        return localVars.get(key);
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
        //var varTable = method.getVarTable();

        generateDeclaration();

        jasminCode.append(JasminUtils.getJasminType(method.getReturnType().getTypeOfElement(), className));
        jasminCode.append("\n\t.limit stack 99\n");
        jasminCode.append("\t.limit locals 99\n");//TODO 

        // Get code for each instruction in method
        StringBuilder code = new StringBuilder();
        for(var inst: method.getInstructions()){
            String currentlabel = "";
            if (!method.getLabels(inst).isEmpty())
                if (!currentlabel.equals(method.getLabels(inst).get(0))) {
                    currentlabel = method.getLabels(inst).get(0);
                    for (String label : method.getLabels(inst)) {
                        code.append("\n\t").append(label).append(":");
                    }
                }   
           
            JasminInstruction jasminInstruction = new JasminInstruction(inst, this);
            jasminCode.append(jasminInstruction.getCode());
            this.reports.addAll(jasminInstruction.getReports());
        }
        

        if (!this.method.isConstructMethod()) {
            this.jasminCode.append("\n\t\t.limit locals ").append(n_locals);
            this.jasminCode.append("\n\t\t.limit stack ").append(stackMax).append("\n");
        }
        this.jasminCode.append(code);
        jasminCode.append("\n.end method");


        return jasminCode.toString();
    }
    
}
