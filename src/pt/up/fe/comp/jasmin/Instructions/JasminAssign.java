package pt.up.fe.comp.jasmin.Instructions;

import org.specs.comp.ollir.AssignInstruction;
import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.Instruction;

import pt.up.fe.comp.jasmin.JasminMethod;

public class JasminAssign {
    private final JasminMethod method;
    private final AssignInstruction assignInstruction;
    public JasminAssign(AssignInstruction assignInstruction, JasminMethod method){
        this.assignInstruction = assignInstruction;
        this.method = method;
    }

    private void generateAssign(AssignInstruction instruction) {
        Instruction rhs = instruction.getRhs();
        switch (rhs.getInstType()) {
            case NOPER:
                generateAssignNOper(instruction);
                break;
            case BINARYOPER:
                generateAssignBinaryOper(instruction);
                break;
            case GETFIELD:
                generateAssignGetfield(instruction);
                break;
            case CALL:
                generateAssignCall(instruction);
                break;
            case UNARYOPER:
                generateAssignNot(instruction);
                break;
            default:
                break;
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
                method.decN_stack();
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

                method.decN_stack();
                method.decN_stack();
                decideType(leftElement);
                if (operation.toString().equals("ANDB"))
                    addCode("and");
                else
                    addCode(operation.toString().toLowerCase(Locale.ROOT));
                method.incN_stack();

                addCode("\n\t\tiastore\n");
                method.decN_stack();
                method.decN_stack();
                method.decN_stack();
                return;
            }
        }
        constOrLoad(leftElement, VarScope.LOCAL);

        constOrLoad(rightElement, VarScope.LOCAL);

        method.decN_stack();
        method.decN_stack();
        if (operation.toString().equals("LTH")) getLessThanOperation(instruction);
        else {
            decideType(leftElement);
            if (operation.toString().equals("ANDB"))
                addCode("and");
            else
                addCode(operation.toString().toLowerCase(Locale.ROOT));
            method.incN_stack();
            storeOrIastore(element);
        }
    }

    private void getLessThanOperation(AssignInstruction instruction) {
        addCode("\n\n\t\tif_icmpge ElseLTH" + method.getN_branches() + JasminUtils.getConstSize(method, "1"));
        storeOrIastore(instruction.getDest());
        addCode("\n\t\tgoto AfterLTH" + method.getN_branches());

        addCode("\n\n\tElseLTH" + method.getN_branches() + ":" + JasminUtils.getConstSize(method, "0"));
        storeOrIastore(instruction.getDest());
        addCode("\n\n\tAfterLTH" + method.getN_branches() + ":");
        method.incN_branches();
    }

    private void generateAssignGetfield(AssignInstruction instruction) {
        Instruction rhs = instruction.getRhs();
        generateGetField((GetFieldInstruction) rhs);
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
                method.decN_stack();
                method.decN_stack();
                method.decN_stack();
                return;
            }
        }
        generateAssignCallAuxiliar((CallInstruction) rhs);
        storeOrIastore(instruction.getDest());
    }

    public void generateAssignCallAuxiliar(CallInstruction rhs) {
        generateCall(rhs, true);
        Element firstArg = rhs.getFirstArg();
        Operand opFirstArg = (Operand) firstArg;
        if (firstArg.getType().getTypeOfElement() == ElementType.OBJECTREF &&
                opFirstArg.getName().equals(method.getClassName())) {
            addCode("\n\t\tinvokespecial " + method.getClassName() + ".<init>()V");
        }
    }
    
}
