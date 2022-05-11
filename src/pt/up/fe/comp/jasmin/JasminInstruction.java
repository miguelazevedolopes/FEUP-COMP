package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.*;

import pt.up.fe.specs.util.exceptions.NotImplementedException;


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
                                      


    public String getLoadSize(Element element, VarScope varScope) {
        String aux;
        int num = method.getLocalVariable(element, varScope).getVirtualReg();
        if (num >= 0 && num <= 3) aux = "load_";
        else aux = "load ";
        method.incrementStack();
        return aux + num;
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
