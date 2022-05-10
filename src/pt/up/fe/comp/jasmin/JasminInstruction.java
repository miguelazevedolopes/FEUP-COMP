package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.AssignInstruction;
import org.specs.comp.ollir.CallInstruction;
import org.specs.comp.ollir.CallType;
import org.specs.comp.ollir.CondBranchInstruction;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.ElementType;
import org.specs.comp.ollir.GotoInstruction;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.PutFieldInstruction;
import org.specs.comp.ollir.ReturnInstruction;
import org.specs.comp.ollir.VarScope;


public class JasminInstruction {
    private final Instruction instruction;
    private final StringBuilder jasminCode;
    private final JasminMethod method;

    public JasminInstruction(Instruction instruction, JasminMethod method) {
        this.instruction = instruction;
        this.method = method;
        this.jasminCode = new StringBuilder();
    }

    public String getCode(){

        switch (instruction.getInstType()) {
            case ASSIGN:
                generateInstruction((AssignInstruction) instruction);
                break;
            case CALL:
                generateInstruction((CallInstruction) instruction, false);
                break;
            case RETURN:
                generateInstruction((ReturnInstruction) instruction);
                break;
            case PUTFIELD:
                generateInstruction((PutFieldInstruction) instruction);
                break;
            case BRANCH:
                generateInstruction((CondBranchInstruction) instruction);
                break;
            case GOTO:
                generateInstruction((GotoInstruction) instruction);
                break;

            default:
                break;
        }

        return jasminCode.toString();

    }

    private void generateInstruction(AssignInstruction inst) {
    }

    private void generateInstruction(GotoInstruction inst) {
    }

    private void generateInstruction(CondBranchInstruction inst) {
    }

    private void generateInstruction(PutFieldInstruction inst) {
    }

    private void generateInstruction(ReturnInstruction inst) {
        Element e1 = inst.getOperand();
        if (e1 != null) {
            if (!e1.isLiteral()) {
                boolean type = decideType(inst.getOperand());
                jasminCode.append(JasminUtils.getLoadSize(method, e1, null) + "\n\t\t");
                if (!type) decideType(inst.getOperand());
                jasminCode.append("return");
            } else {
                String literal = ((LiteralElement) e1).getLiteral();
                jasminCode.append(JasminUtils.getConstSize(method, literal) + "\n\t\tireturn");
            }
            method.decrementStack();
        } else {
            jasminCode.append("\n\t\treturn");
        }

    }

    private void generateInstruction(CallInstruction inst, boolean assign) {
        if (method.getMethod().isConstructMethod()) {
            jasminCode.append("\n\taload_0\n\tinvokespecial ");
            if (method.getSuperName() == null)
                jasminCode.append("java/lang/Object");
            else
                jasminCode.append(method.getSuperName());
            jasminCode.append(".<init>()V\n\treturn");
        } else {
            Element firstArg = inst.getFirstArg();
            Operand opFirstArg = (Operand) firstArg;

            if (firstArg.getType().getTypeOfElement() == ElementType.OBJECTREF) {
                generateCallObjectRef(inst);
                if (!JasminUtils.getReturnFromMethod(method, inst.getReturnType()).equals("V") && !assign)
                    popStack();
            } else if (opFirstArg.getType().getTypeOfElement() == ElementType.ARRAYREF) {

                if (inst.getInvocationType() == CallType.NEW) generateNewArray(inst);

                else if (inst.getInvocationType() == CallType.arraylength) {
                    decideType(firstArg);
                    jasminCode.append(getLoadSize(firstArg, null) + "\n\t\tarraylength");
                }
                if (!assign)
                    popStack();
            } else if (opFirstArg.getType().getTypeOfElement() == ElementType.CLASS) {
                generateStaticMethod(inst);
            } else if (opFirstArg.getName().equals("this")) {
                generateClassMethod(inst);
                if (!assign){
                    popStack();

                }
            }
        }



    }

    private void generateNewArray(CallInstruction inst) {
        Element element = inst.getListOfOperands().get(0);
        if (element.isLiteral())
            jasminCode.append(JasminUtils.getConstSize(method, ((LiteralElement) element).getLiteral()));
        else {
            loadOrAload(element, null);
        }
        jasminCode.append("\n\t\tnewarray int");
        //Increase for newarray, decrease for length of the array
    }

    private void loadOrAload(Element element, VarScope varScope) {
        boolean type = decideType(element);
        if (!type)
            jasminCode.append(getLoadSize(method, element, varScope));
        else {
            method.decrementStack();
            method.decrementStack();
            jasminCode.append("\n\t\tiaload");
            method.incrementStack();
        }
    }

    private boolean decideType(Element element) {
        switch (element.getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                if (!element.isLiteral())
                    if (method.getLocalVariable(element, null).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                        jasminCode.append("\n\t\ta" + getLoadSize(element, null));
                        Element indexElem = ((ArrayOperand) element).getIndexOperands().get(0);
                        jasminCode.append("\n\t\ti" + getLoadSize(indexElem, null));
                        return true;
                    }
                jasminCode.append("\n\t\ti");
                break;
            case ARRAYREF:
            case THIS:
            case OBJECTREF:
                jasminCode.append("\n\t\ta");
                break;
            default:
                jasminCode.append("\n\t\t");
                break;
        }
        return false;
    }

    private void generateCallObjectRef(CallInstruction inst) {
        Element firstArg = inst.getFirstArg();
        Operand opFirstArg = (Operand) firstArg;

        if (opFirstArg.getName().equals(method.getClass())) {
            jasminCode.append("\n\t\tnew " + method.getClass() + "\n\t\tdup");
            method.incrementStack();
        } else {
            if (inst.getNumOperands() > 1) {
                Element secondArg = inst.getSecondArg();
                if (secondArg.isLiteral())
                    if (((LiteralElement) secondArg).getLiteral().replace("\"", "").equals("<init>")) {
                        method.incrementStack();
                        return;
                    }
            }

            loadOrAload(opFirstArg, null);
            virtualParameters(inst);
            method.decrementStack();();
        }
    }



    public String getLoadSize(Element element, VarScope varScope) {
        String aux;
        int num = method.getLocalVariable(element, varScope).getVirtualReg();
        if (num >= 0 && num <= 3) aux = "load_";
        else aux = "load ";
        method.incrementStack();
        return aux + num;
    }





    private void popStack(){
        jasminCode.append("\n\t\tpop");
        method.decrementStack();
    }

    
}
