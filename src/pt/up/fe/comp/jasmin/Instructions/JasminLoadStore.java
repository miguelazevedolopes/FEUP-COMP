package pt.up.fe.comp.jasmin.Instructions;

import java.util.Map;
import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.ElementType;
import org.specs.comp.ollir.LiteralElement;
import org.specs.comp.ollir.Operand;

import pt.up.fe.comp.jasmin.JasminMethod;
import pt.up.fe.comp.jasmin.JasminUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public class JasminLoadStore{
    private final Element element;
    private final Map<String, Descriptor> table;
    private JasminInstruction jasminInstruction;

    
    public JasminLoadStore(Element element, Map<String,Descriptor> table, JasminInstruction jasminInstruction) {
        this.element = element;
        this.table = table;
        this.jasminInstruction = jasminInstruction;
    }

    public  String getStoreCode(String rhs) {
        ElementType elementType = element.getType().getTypeOfElement();

       if (elementType == ElementType.INT32 || elementType == ElementType.STRING || elementType == ElementType.BOOLEAN) {
            var operand = (Operand) element;

            ElementType typeVar = table.get(operand.getName()).getVarType().getTypeOfElement();


            if (typeVar == ElementType.ARRAYREF)
                return getStoreArrayAccess(rhs);
            else {
                int register = JasminUtils.getVirtualReg(element, table);
                return rhs + istore(register);
            }
        } else if (elementType == ElementType.OBJECTREF || elementType == ElementType.THIS || elementType == ElementType.ARRAYREF) {
            int register = JasminUtils.getVirtualReg(element, table);
            return rhs + astore(register);
        }
        return
        
        
        
        element.toString();
    }


    public String getLoadCode(){
        ElementType elementType = element.getType().getTypeOfElement();

        //  iconst_
        if (element.isLiteral()) {
            var litElem = (LiteralElement) element;
            return iconst(litElem.getLiteral());
        }
        //  iload
        else if (elementType == ElementType.INT32 || elementType == ElementType.STRING || elementType == ElementType.BOOLEAN) {
            
            ElementType typeVar = table.get(((Operand) element).getName()).getVarType().getTypeOfElement();
            // Array accesses are treated as integers. Thus, this verification is necessary.
            if (typeVar == ElementType.ARRAYREF)
                throw new NotImplementedException("[LoadStore, Load]ARRAYREF");
                //return getLoadArrayAccess(element, table);
            else {
                int register = JasminUtils.getVirtualReg(element, table);
                return iload(register);
            }
        }
        //aload
        else if (elementType == ElementType.OBJECTREF || elementType == ElementType.THIS || elementType == ElementType.ARRAYREF) {
            int register = JasminUtils.getVirtualReg(element, table);
            return aload(register);
        }
        return "";
    }

    private String aload(int register) {
        jasminInstruction.getMethod().updateMaxStack(0,1);
        if (register > 3 || register < 0)
            return "aload " + register + "\n";
        return "aload_"+ register + "\n";

    }

    public  String iconst(String number){

        jasminInstruction.getMethod().updateMaxStack(0,1);
        int constant = Integer.parseInt(number);
        if (constant == -1)
            return "iconst_m1\n";
        if (constant <= 5 && constant >= -1)
            return "iconst_" + number + "\n";
        if (constant <= 127 && constant >= -128)
            return "bipush " + number + "\n";
        if (constant >= -32768 && constant <= 32767)
            return "sipush " + number + "\n";
        return "ldc " + number + "\n";
    }

    public  String iload(int reg){
        jasminInstruction.getMethod().updateMaxStack(0,1);
        if (reg > 3 || reg < 0)
            return "iload " + reg + "\n";
        return "iload_" + reg + "\n";
    }

    public  String istore(int reg){
        jasminInstruction.getMethod().updateMaxStack(1,1);
        if (reg > 3 || reg < 0)
            return "istore " + reg + "\n";
        return "istore_" + reg + "\n";
    }

    public  String astore(int reg){
        jasminInstruction.getMethod().updateMaxStack(2,1);
        if (reg > 3 || reg < 0)
            return "astore " + reg + "\n";
        return "astore_" + reg + "\n";
    }


    private String getStoreArrayAccess(String rhs) {
        return null;
    }
}