package pt.up.fe.comp.jasmin.Instructions;

import java.util.Map;

import org.specs.comp.ollir.*;

import freemarker.core.builtins.sourceBI;
import pt.up.fe.comp.jasmin.JasminMethod;
import pt.up.fe.comp.jasmin.JasminUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;


public class JasminInstruction {
    private final Instruction instruction;
    private final StringBuilder jasminCode;
    private final JasminMethod method;
    private final Map<String, Descriptor> varTable;

    public JasminInstruction(Instruction instruction, JasminMethod method, Map<String, Descriptor> varTable) {
        this.instruction = instruction;
        this.method = method;
        this.jasminCode = new StringBuilder();
        this.varTable = varTable;
    }

    public JasminMethod getMethod(){
        return method;
    }

    public String getCode(){

        StringBuilder code = new StringBuilder();
        switch (instruction.getInstType()) {
            case CALL:
                code.append(getCode((CallInstruction) instruction));
                break;
            case RETURN:
                code.append(getCode((ReturnInstruction) instruction));
                break;
            // case ASSIGN:
            //     code.append(getCode((CallInstruction) instruction, false));
            //     break;
            // case PUTFIELD:
            //     getCode((PutFieldInstruction) instruction);
            //     break;
            case BRANCH:
                 getCode((CondBranchInstruction) instruction);
                 break;
            // case GOTO:
            //     getCode((GotoInstruction) instruction);
            //     break;

            default:
                throw new NotImplementedException("Intruction Type not implemented: " + instruction.getInstType().toString());
        }

        return code.toString();

    }

    // private void generateInstruction(AssignInstruction inst) {
    // }

    // private void generateInstruction(GotoInstruction inst) {
    // }

    // private void generateInstruction(CondBranchInstruction inst) {
    // }

    // private void generateInstruction(PutFieldInstruction inst) {
    // }

        //TODO Incomplete
    private String getCode(ReturnInstruction instruction){
        var code = new StringBuilder();

        Element op = instruction.getOperand();
        

        code.append("\n\treturn");

        return code.toString();

    }


    private String getCode(CondBranchInstruction instruction){
        var code = new StringBuilder();
        Element leftElement = instruction.getOperands().get(0);
        Element rightElement = instruction.getOperands().get(0);

        String leftInst = new JasminLoadStore(leftElement, varTable).getLoadCode(this);
        String rightInst = new JasminLoadStore(rightElement, varTable).getLoadCode(this);

        System.out.println("leftInst");

        //get condition type
        var  cond = instruction.getCondition();
        OperationType condOperation =  ((OpInstruction) cond).getOperation().getOpType();

        String label = instruction.getLabel();

        code.append(new JasminBoolCond(this, leftInst, rightInst, label, condOperation).getJasminInst());

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

        code.append(methodClass).append("/"); //TODO fully classified name
        code.append(((LiteralElement) secondArg).getLiteral().replace("\"", ""));

        code.append("(");
        
        //Operands
        for(var operand: instruction.getListOfOperands()){
            getArgumentsCode(operand);
        }
        code.append(")");

        code.append(JasminUtils.getJasminType(instruction.getReturnType().getTypeOfElement(), method.getClassName()));
        return code.toString();


    }
                                    


    //TODO operands
    private void getArgumentsCode(Element operand) {
        throw new NotImplementedException(operand.toString());
    }


    private void popStack(){
        jasminCode.append("\n\t\tpop");
        method.decrementStack();
    }

    
}
