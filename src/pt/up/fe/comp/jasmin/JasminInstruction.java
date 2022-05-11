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

    }

    private void generateInstruction(CallInstruction inst, boolean assign) {


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
