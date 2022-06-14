package pt.up.fe.comp.jasmin.Methods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.up.fe.comp.jasmin.JasminUtils;
import pt.up.fe.comp.jmm.report.Report;
import org.specs.comp.ollir.*;

import pt.up.fe.specs.util.exceptions.NotImplementedException;

import static java.lang.Math.max;


public class JasminMethod {
    private final Method method;
    private final StringBuilder jasminCode;
    private final String superName;
    private final String className;
    private final Map<String, Descriptor> localVars;
    private int n_locals = 0;
    private int stackMax;
    private int currStack;
    private List<Report> reports;
    private int nNranches;
    public int labelAux;


    public JasminMethod(Method method, String className, String superName) {
        this.method = method;
        this.jasminCode = new StringBuilder();
        this.superName = superName;
        this.className = className;
        this.localVars = new HashMap<>();
        this.n_locals = 0;
        this.stackMax = 0;
        this.currStack = 0;
        this.reports = new ArrayList<>();
        this.nNranches = 0;
        this.labelAux = 0;
        
        addLocalVariable("this", VarScope.FIELD, new Type(ElementType.CLASS));
    }

    public List<Report> getReports(){
        return this.reports;
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
        stackMax = max(stackMax, currStack);
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
        boolean isConstructMethod = method.isConstructMethod();
        jasminCode.append("\n\n.method ");
        jasminCode.append(getAccessModifiers(method.getMethodAccessModifier(), isConstructMethod));
        if (isConstructMethod)
            jasminCode.append(" <init>");
        else {
            if (method.isStaticMethod()) jasminCode.append(" static");
            if (method.isFinalMethod()) jasminCode.append(" final");

            jasminCode.append(" ");
            jasminCode.append(method.getMethodName());
        }
        jasminCode.append("(");

        jasminCode.append(JasminUtils.getParametersFromMethod(this));

        jasminCode.append(")");

    }



    public String generateJasminCode() {

        generateDeclaration();

        jasminCode.append(JasminUtils.getJasminType(method.getReturnType().getTypeOfElement(), className));

        StringBuilder code = new StringBuilder();
        String currentlabel = "";
        for (var inst : method.getInstructions()) {
            if (!method.getLabels(inst).isEmpty())
                if (!currentlabel.equals(method.getLabels(inst).get(0))) {
                    currentlabel = method.getLabels(inst).get(0);
                    for (String label : method.getLabels(inst)) {
                        code.append("\n\t").append(label).append(":");
                    }
                }
            JasminInstruction jasminInstruction = new JasminInstruction(inst, this);
            code.append(jasminInstruction.getCode());
            this.reports.addAll(jasminInstruction.getReports());
        }
        if (!this.method.isConstructMethod()) {
            this.jasminCode.append("\n\t\t.limit locals ").append(n_locals);
            this.jasminCode.append("\n\t\t.limit stack ").append(max(stackMax,5)).append("\n");
        }
        this.jasminCode.append(code);
        jasminCode.append("\n.end method");
        return jasminCode.toString();
    }

    
    
}
