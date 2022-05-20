package pt.up.fe.comp.jasmin.Methods;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.specs.comp.ollir.*;

import pt.up.fe.comp.jasmin.JasminUtils;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.exceptions.NotImplementedException;


public class JasminInstruction {
    private final Instruction instruction;
    private final StringBuilder jasminCode;
    private final JasminMethod method;
    private final List<Report> reports;

    public JasminInstruction(Instruction instruction, JasminMethod method) {
        this.instruction = instruction;
        this.method = method;
        this.jasminCode = new StringBuilder();
        this.reports = new ArrayList<>();
    }

    private void addCode(String code){
        jasminCode.append(code);
    }

    public JasminMethod getMethod(){
        return method;
    }

    public List<Report> getReports() {
        return reports;
    }

    public String getCode(){

        switch (instruction.getInstType()) {
            case PUTFIELD:
                generateCode((PutFieldInstruction) instruction);
                break;
            case BRANCH:
                    break;
            case GOTO:
                generateCode((GotoInstruction) instruction);
                break;
            case CALL:
                generateCode((CallInstruction)instruction, false);
                break;
            case RETURN:
                generateCode((ReturnInstruction)instruction);
                break;
            case ASSIGN:
                generateCode((AssignInstruction) instruction);
                break;

            default:
                throw new NotImplementedException("Intruction Type not implemented: " + instruction.getInstType().toString());
        }



        return jasminCode.toString();

    }


    //------ASSING STARTS----------------------

    private void generateCode(AssignInstruction instruction) {
        Instruction rhs = instruction.getRhs();
        switch (rhs.getInstType()) {
            case BINARYOPER:
                generateAssignBinaryOper(instruction);
                break;
            case GETFIELD:
                generateAssignGetfield(instruction);
                break;
            case CALL:
                generateAssignCall(instruction);
                break;
            case NOPER:
                generateAssignNOper(instruction);
                break;

            case UNARYOPER:
                generateAssignNot(instruction);
                break;
            default:
                throw new NotImplementedException("Rhs Type: " + rhs.getInstType().toString());
        }
    }

    private void generateAssignNOper(AssignInstruction instruction) {
        Instruction rhs = instruction.getRhs();

        Element singleOperand = ((SingleOpInstruction) rhs).getSingleOperand();
        Element dest = instruction.getDest();
        Descriptor localVariable = method.getLocalVariableByKey(dest, null);

        if (dest.getType().getTypeOfElement() == ElementType.INT32 && !dest.isLiteral()) {
            if (localVariable.getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                addCode("\n\t\ta" + JasminUtils.getLoadSize(method, dest, null));
                Element indexElem = ((ArrayOperand) dest).getIndexOperands().get(0);
                addCode("\n\t\ti" + JasminUtils.getLoadSize(method, indexElem, null));
                constOrLoad(singleOperand, null);
                addCode("\n\t\tiastore\n");
                method.decrementStack();
                return;
            }
        }
        constOrLoad(singleOperand, null);
        decideType(instruction.getDest());
        addCode(JasminUtils.getStoreSize(method, dest, VarScope.LOCAL) + "\n");
    }

    private void generateAssignNot(AssignInstruction instruction) {
        Instruction rhs = instruction.getRhs();

        Element singleOperand = ((UnaryOpInstruction) rhs).getOperand();
        Element dest = instruction.getDest();

        constOrLoad(singleOperand, null);
        addCode("\n\t\ticonst_1\n\t\tixor");
        decideType(instruction.getDest());
        addCode(JasminUtils.getStoreSize(method, dest, VarScope.LOCAL) + "\n");
    }

    private void generateAssignBinaryOper(AssignInstruction instruction) {
        Instruction rhs = instruction.getRhs();

        OperationType operation = ((BinaryOpInstruction) rhs).getOperation().getOpType();
        Element leftElement = ((BinaryOpInstruction) rhs).getLeftOperand();
        Element rightElement = ((BinaryOpInstruction) rhs).getRightOperand();

        if (iincInstruction(leftElement, rightElement, instruction.getDest(), operation)) return;

        Element element = instruction.getDest();
        if (element.getType().getTypeOfElement() == ElementType.INT32 && !element.isLiteral()) {
            if (method.getLocalVariableByKey(element, null).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                addCode("\n\t\ta" + JasminUtils.getLoadSize(method, element, null));
                Element indexElem = ((ArrayOperand) element).getIndexOperands().get(0);
                addCode("\n\t\ti" + JasminUtils.getLoadSize(method, indexElem, null));

                constOrLoad(leftElement, VarScope.LOCAL);
                constOrLoad(rightElement, VarScope.LOCAL);

                method.decrementStack();
                method.decrementStack();
                decideType(leftElement);
                if (operation.toString().equals("ANDB"))
                    addCode("and");
                else
                    addCode(operation.toString().toLowerCase(Locale.ROOT));
                method.incrementStack();

                addCode("\n\t\tiastore\n");
                method.decrementStack();
                method.decrementStack();
                method.decrementStack();
                return;
            }
        }
        constOrLoad(leftElement, VarScope.LOCAL);

        constOrLoad(rightElement, VarScope.LOCAL);

        method.decrementStack();
        method.decrementStack();
        if (operation.toString().equals("LTH")) getLessThanOperation(instruction);
        else {
            decideType(leftElement);
            if (operation.toString().equals("ANDB"))
                addCode("and");
            else
                addCode(operation.toString().toLowerCase(Locale.ROOT));
            method.incrementStack();
            storeOrIastore(element);
        }
    }

    private void getLessThanOperation(AssignInstruction instruction) {
        addCode("\n\n\t\tif_icmpge ElseLTH" + method.getNBranches() + JasminUtils.getConstSize(method, "1"));
        storeOrIastore(instruction.getDest());
        addCode("\n\t\tgoto AfterLTH" + method.getNBranches());

        addCode("\n\n\tElseLTH" + method.getNBranches() + ":" + JasminUtils.getConstSize(method, "0"));
        storeOrIastore(instruction.getDest());
        addCode("\n\n\tAfterLTH" + method.getNBranches() + ":");
        method.incrementBranches();
    }

    private void generateAssignGetfield(AssignInstruction instruction) {
        Instruction rhs = instruction.getRhs();
        generateCode((GetFieldInstruction) rhs);
        storeOrIastore(instruction.getDest());
    }

    private void generateAssignCall(AssignInstruction instruction) {
        Instruction rhs = instruction.getRhs();
        Element element = instruction.getDest();
        if (element.getType().getTypeOfElement() == ElementType.INT32 && !element.isLiteral()) {
            if (method.getLocalVariableByKey(element, null).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                addCode("\n\t\ta" + JasminUtils.getLoadSize(method, element, null));
                Element indexElem = ((ArrayOperand) element).getIndexOperands().get(0);
                addCode("\n\t\ti" + JasminUtils.getLoadSize(method, indexElem, null));

                generateAssignCallAuxiliar((CallInstruction) rhs);

                addCode("\n\t\tiastore\n");
                method.decrementStack();
                method.decrementStack();
                method.decrementStack();
                return;
            }
        }
        generateAssignCallAuxiliar((CallInstruction) rhs);
        storeOrIastore(instruction.getDest());
    }

    public void generateAssignCallAuxiliar(CallInstruction rhs) {
        generateCode(rhs, true);
        Element firstArg = rhs.getFirstArg();
        Operand opFirstArg = (Operand) firstArg;
        if (firstArg.getType().getTypeOfElement() == ElementType.OBJECTREF &&
                opFirstArg.getName().equals(method.getClassName())) {
            addCode("\n\t\tinvokespecial " + method.getClassName() + ".<init>()V");
        }
    }

    //---------ASSIGN ENDS

    //---------CALL STARTS
        
    private void generateCode(CallInstruction instruction, boolean assign) {
        if (method.getMethod().isConstructMethod()) {
            addCode("\n\taload_0\n\tinvokespecial ");
            if (method.getSuperName() == null)
                addCode("java/lang/Object");
            else
                addCode(method.getSuperName());
            addCode(".<init>()V\n\treturn");
        } else {
            Element firstArg = instruction.getFirstArg();
            Operand opFirstArg = (Operand) firstArg;

            if (firstArg.getType().getTypeOfElement() == ElementType.OBJECTREF) {
                generateCallObjectRef(instruction);
                if (!JasminUtils.getJasminType(instruction.getReturnType().getTypeOfElement(), method.getClassName()).equals("V") && !assign)
                    generatePop();
            } else if (opFirstArg.getType().getTypeOfElement() == ElementType.ARRAYREF) {

                if (instruction.getInvocationType() == CallType.NEW) generateNewArray(instruction);

                else if (instruction.getInvocationType() == CallType.arraylength) {
                    decideType(firstArg);
                    addCode(JasminUtils.getLoadSize(method, firstArg, null) + "\n\t\tarraylength");
                }
                if (!assign)
                    generatePop();
            } else if (opFirstArg.getType().getTypeOfElement() == ElementType.CLASS) {
                generateStaticMethod(instruction);
            } else if (opFirstArg.getName().equals("this")) {
                generateClassMethod(instruction);
                if (!assign)
                    generatePop();
            }
        }
    }

    private void generatePop() {
        addCode("\n\t\tpop");
        method.decrementStack();
    }

    private void generateCallObjectRef(CallInstruction instruction) {
        Element firstArg = instruction.getFirstArg();
        Operand opFirstArg = (Operand) firstArg;

        if (opFirstArg.getName().equals(method.getClassName())) {
            addCode("\n\t\tnew " + method.getClassName() + "\n\t\tdup");
            method.incrementStack();
        } else {
            if (instruction.getNumOperands() > 1) {
                Element secondArg = instruction.getSecondArg();
                if (secondArg.isLiteral())
                    if (((LiteralElement) secondArg).getLiteral().replace("\"", "").equals("<init>")) {
                        method.incrementStack();
                        return;
                    }
            }

            loadOrAload(opFirstArg, null);
            virtualParameters(instruction);
            method.decrementStack();
        }
    }

    private void generateNewArray(CallInstruction instruction) {
        Element element = instruction.getListOfOperands().get(0);
        if (element.isLiteral())
            addCode(JasminUtils.getConstSize(method, ((LiteralElement) element).getLiteral()));
        else {
            loadOrAload(element, null);
        }
        addCode("\n\t\tnewarray int");
    }

    private void generateStaticMethod(CallInstruction instruction) {
        Element firstArg = instruction.getFirstArg();
        Operand opFirstArg = (Operand) firstArg;
        for (Element parameter : instruction.getListOfOperands()) {
            if (!parameter.isLiteral())
                loadOrAload(parameter, VarScope.LOCAL);
            else
                addCode(JasminUtils.getConstSize(method, ((LiteralElement) parameter).getLiteral()));
        }
        addCode("\n\t\tinvokestatic " + opFirstArg.getName());
        invokeParameters(instruction);
        for (Element ignored : instruction.getListOfOperands()) {
            method.decrementStack();
        }
    }

    private void generateClassMethod(CallInstruction instruction) {
        addCode("\n\t\taload_0");
        virtualParameters(instruction);
    }

    private void virtualParameters(CallInstruction instruction) {
        for (Element parameter : instruction.getListOfOperands()) {
            if (!parameter.isLiteral())
                loadOrAload(parameter, VarScope.LOCAL);
            else
                addCode(JasminUtils.getConstSize(method, ((LiteralElement) parameter).getLiteral()));
        }
        addCode("\n\t\tinvokevirtual " + method.getClassName());
        invokeParameters(instruction);
        for (Element ignored : instruction.getListOfOperands()) {
            method.decrementStack();
        }
    }
    //---------CALL ENDS


    //--------RETURN STARTS
    private void generateCode(ReturnInstruction instruction) {
        Element e1 = instruction.getOperand();
        if (e1 != null) {
            if (!e1.isLiteral()) {
                boolean type = decideType(instruction.getOperand());
                addCode(JasminUtils.getLoadSize(method, e1, null) + "\n\t\t");
                if (!type) decideType(instruction.getOperand());
                addCode("return");
            } else {
                String literal = ((LiteralElement) e1).getLiteral();
                addCode(JasminUtils.getConstSize(method, literal) + "\n\t\tireturn");
            }
            method.decrementStack();
        } else {
            addCode("\n\t\treturn");
        }
    }

    //-------PUTFIELD STARTS
    private void generateCode(PutFieldInstruction instruction) {

        Element e1 = instruction.getFirstOperand();
        Element e2 = instruction.getSecondOperand();
        Element e3 = instruction.getThirdOperand();
        Operand o1 = (Operand) e1;
        Operand o2 = (Operand) e2;

        String name = o1.getName();
        loadOrAload(e1, VarScope.FIELD);
        constOrLoad(e3, null);

        if (name.equals("this")) name = method.getClassName();

        method.decrementStack();
        method.decrementStack();
        addCode("\n\t\tputfield " + name + "/" + o2.getName() + " " + JasminUtils.getJasminType(e2.getType().getTypeOfElement(), method.getClassName()) + "\n");

    }

    //-----------GETFIELD STARTS
    private void generateCode(GetFieldInstruction instruction) {
        String firstName = "";
        Element e1 = instruction.getFirstOperand();
        if (!e1.isLiteral()) {
            firstName = ((Operand) e1).getName();
            addCode("\n\t\ta" + JasminUtils.getLoadSize(method, e1, VarScope.FIELD));
        }

        if (firstName.equals("this")) firstName = method.getClassName();
        addCode("\n\t\tgetfield " + firstName + "/");
        e1 = instruction.getSecondOperand();

        if (!e1.isLiteral()) {
            Operand o1 = (Operand) e1;
            addCode(o1.getName() + " " + JasminUtils.getJasminType(o1.getType().getTypeOfElement(), method.getClassName()));
        }

    }

    private void generateCode(GotoInstruction instruction) {
        addCode("\n\t\tgoto " + instruction.getLabel() + "\n");
    }

        

    // -------------- Auxiliary Functions --------------


    

    private boolean decideType(Element element) {
        switch (element.getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                if (!element.isLiteral())
                    if (method.getLocalVariableByKey(element, null).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                        addCode("\n\t\ta" + JasminUtils.getLoadSize(method, element, null));
                        Element indexElem = ((ArrayOperand) element).getIndexOperands().get(0);
                        addCode("\n\t\ti" + JasminUtils.getLoadSize(method, indexElem, null));
                        return true;
                    }
                addCode("\n\t\ti");
                break;
            case ARRAYREF:
            case THIS:
            case OBJECTREF:
                addCode("\n\t\ta");
                break;
            default:
                addCode("\n\t\t");
                break;
        }
        return false;
    }

    private void constOrLoad(Element element, VarScope varScope) {
        if (element.isLiteral()) {
            String value = ((LiteralElement) element).getLiteral();
            addCode(JasminUtils.getConstSize(method, value));
        } else loadOrAload(element, varScope);
    }

    private void storeOrIastore(Element element) {
        boolean type = decideType(element);
        if (!type)
            addCode(JasminUtils.getStoreSize(method, element, VarScope.LOCAL) + "\n");
        else {
            addCode("\n\t\tiastore\n");
            method.decrementStack();
            method.decrementStack();
            method.decrementStack();
        }
    }

    private void loadOrAload(Element element, VarScope varScope) {
        boolean type = decideType(element);
        if (!type)
            addCode(JasminUtils.getLoadSize(method, element, varScope));
        else {
            method.decrementStack();
            method.decrementStack();
            addCode("\n\t\tiaload");
            method.incrementStack();
        }
    }

    private void invokeParameters(CallInstruction instruction) {
        if (instruction.getNumOperands() > 1) {
            Element secondArg = instruction.getSecondArg();
            if (secondArg.isLiteral()) {
                addCode("." + ((LiteralElement) secondArg).getLiteral().replace("\"", "") + "(");
                for (Element parameter : instruction.getListOfOperands()) {
                    addCode(JasminUtils.getJasminType(parameter.getType().getTypeOfElement(), method.getClassName()));
                }
                String ret = JasminUtils.getJasminType(instruction.getReturnType().getTypeOfElement(), method.getClassName());
                addCode(")" + ret);
                if (!ret.equals("V"))
                    method.incrementStack();
            }
        }
    }

    private boolean sameOperand(Element first, Element second) {
        if (first.isLiteral() || second.isLiteral())
            return false;
        return (((Operand) first).getName().equals(((Operand) second).getName()));
    }

    private boolean iincInstruction(Element leftElement, Element rightElement, Element dest, OperationType operation) {
        String literal;
        if ((operation.toString().equals("ADD") || operation.toString().equals("SUB"))) {
            if (sameOperand(dest, leftElement) && rightElement.isLiteral())
                literal = ((LiteralElement) rightElement).getLiteral();
            else if (sameOperand(dest, rightElement) && leftElement.isLiteral())
                literal = ((LiteralElement) leftElement).getLiteral();
            else return false;
            Descriptor var = method.getLocalVariableByKey(dest, null);
            if (var.getVarType().getTypeOfElement() != ElementType.ARRAYREF) {
                addCode("\n\t\tiinc " + var.getVirtualReg());
                if ((operation.toString().equals("ADD")))
                    addCode(" " + literal);
                else
                    addCode(" -" + literal);
                return true;
            }
        }
        return false;
    }
    
}
