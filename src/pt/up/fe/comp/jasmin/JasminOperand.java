package pt.up.fe.comp.jasmin;

import java.util.Map;

import org.specs.comp.ollir.*;

import pt.up.fe.comp.jasmin.Instructions.JasminBoolCond;
import pt.up.fe.comp.jasmin.Instructions.JasminInstruction;
import pt.up.fe.comp.jasmin.Instructions.JasminLoadStore;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public class JasminOperand{
    

    Element lhs;
    Map<String, Descriptor> table;
    StringBuilder code;
    JasminInstruction instruction;

    public JasminOperand(JasminInstruction instruction, Map<String, Descriptor> table, Element lhs) {
        this.table = table;
        this.lhs = lhs;
        this.instruction = instruction;
        this.code = new StringBuilder();
    }

    public String getOperand(Instruction inst) {

        switch (inst.getInstType()) {
            case BINARYOPER:
                addBinaryOper( (BinaryOpInstruction)inst);
                break;
            case NOPER:
                addNoOper((SingleOpInstruction) inst);
                break;
            case GETFIELD:
                addGetField((GetFieldInstruction) inst);
                break;
            case CALL:
                addCall((CallInstruction) inst);
                break;
            default:
                throw new NotImplementedException("Operand not implemented: " + inst.getInstType());
        }

        return super.toString();
    }

    public void addBinaryOper(BinaryOpInstruction inst) {
        Element leftElem = inst.getLeftOperand();
        Element rightElem = inst.getRightOperand();
        // a = b[i];

        String leftInstruction = new JasminLoadStore(leftElem, table, instruction).getLoadCode( );
        String rightInstruction = new JasminLoadStore(rightElem, table, instruction).getLoadCode( );
        OperationType opType = inst.getOperation().getOpType();

        if (!isBooleanOp(opType)) {
            code.append(leftInstruction);
            code.append(rightInstruction);

            code.append(JasminUtils.getOp(opType, instruction));
        }else
            code.append(new JasminBoolCond(instruction, leftInstruction, rightInstruction, null, opType).getJasminInst());
    }


    public static boolean isBooleanOp(OperationType opType){
        return opType == OperationType.ANDB || opType == OperationType.NOTB || opType == OperationType.LTH ;
    }

    public void addNoOper(SingleOpInstruction inst) {
        Element element = inst.getSingleOperand();
        code.append(new JasminLoadStore(element, table,instruction).getLoadCode());
    }

    public void addCall(CallInstruction callInstruction){
        code.append(instruction.getCode(callInstruction, true));
    }

    public void addGetField(GetFieldInstruction inst){
        code.append(instruction.getCode(inst));
    }
}
