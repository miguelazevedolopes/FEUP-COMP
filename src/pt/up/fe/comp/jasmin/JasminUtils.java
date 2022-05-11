package pt.up.fe.comp.jasmin;
import org.specs.comp.ollir.*;

public class JasminUtils {

    /**
     * TODO: check prof video for the arrayref, might not be '[I'
     * @param method
     */
    public static String getParametersFromMethod(JasminMethod method) {
        StringBuilder res = new StringBuilder();
        if (method.getMethod().getMethodName().equals("main")) {
            res = new StringBuilder("[Ljava/lang/String;");
            method.addLocalVariable("args", VarScope.PARAMETER, new Type(ElementType.ARRAYREF));
        } else {
            for (Element param : method.getMethod().getParams()) {
                if (param.isLiteral()) res = new StringBuilder("L");
                switch (param.getType().getTypeOfElement()) {
                    case INT32:
                        res.append("I");
                        break;
                    case BOOLEAN:
                        res.append("Z");
                        break;
                    case ARRAYREF:
                        res.append("[I");
                        break;
                    case OBJECTREF:
                        res.append("OBJECTREF");
                        break;
                    case STRING:
                        res.append("java/lang/String");
                        break;
                    default:
                        break;
                }
                method.addLocalVariable(((Operand) param).getName(), VarScope.PARAMETER, param.getType());
            }
        }
        return res.toString();
    }

   

    
    public static String getJasminType(ElementType type, String className) {
        String res = "";
        //switch (method.getReturnType().getTypeOfElement()) {
        switch (type) {
            case INT32:
                res = "I";
                break;
            case BOOLEAN:
                res = "Z";
                break;
            case ARRAYREF:
                res = "[I";
                break;
            case OBJECTREF:
                res = className;
                break;
            case VOID:
                res = "V";
                break;
            default:
                break;
        }
        return res;
    }


}
