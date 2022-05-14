package pt.up.fe.comp.jasmin.Instructions;

import org.specs.comp.ollir.OperationType;

import pt.up.fe.comp.jasmin.JasminMethod;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

/**
 * Translates a boolean expression.
 */
public class JasminBoolCond {

    private final String leftInst;
    private final String rightInst;
    private final String label;
    private final OperationType opType;
    private final JasminInstruction jasminInstruction; 

    public JasminBoolCond(JasminInstruction jasminInstruction, String leftInst, String rightInst, String label, OperationType opType) {
        this.jasminInstruction = jasminInstruction;
        this.leftInst = leftInst;
        this.rightInst = rightInst;
        this.label = label;
        this.opType = opType;
    }


    public String getJasminInst() {
        StringBuilder code = new StringBuilder();

        switch(opType){
            case LTH:
                code.append(getLTHCode());
                break;

            // case ANDB: //TODO andb
            //     break;

            // case NOTB: //TODO notb
            //     break;

            case GTE:
                code.append(getGTECode());
                break;
            default:
                throw new NotImplementedException("[JasminBoolCond] OperationType " + opType.toString());
        }

        // if (opType == OperationType.LTH) code.append(lthInst(leftInst, rightInst, label));
        // else if (opType == OperationType.ANDB) code.append(andInst(leftInst, rightInst, label));
        // else if (opType == OperationType.NOTB) code.append(notInst(rightInst, label));
        // else if (opType == OperationType.GTE) code.append(geInst(leftInst, rightInst, label));

        return code.toString();
    }


    private String getLTHCode(){
        StringBuilder code = new StringBuilder();

        // Append right and left element
        code.append(leftInst);
        code.append(rightInst);

        code.append("if_icmplt ").append(label).append("\n");

        return code.toString();
    }

    private String getGTECode(){
        StringBuilder code = new StringBuilder();

        // Append right and left element
        code.append(leftInst);
        code.append(rightInst);

        code.append("if_icmpge ").append(label).append("\n");

        return code.toString();
    }




}
