package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.ArrayType;
import org.specs.comp.ollir.ClassType;
import org.specs.comp.ollir.ElementType;
import org.specs.comp.ollir.Type;

public class JasminType {
    public static String getJasminType(Type type, String className) {
        switch (type.getTypeOfElement()) {
            case ARRAYREF:
                return getJasminTypeArray((ArrayType) type);
            case THIS:
                return className;
            case OBJECTREF:
                return ((ClassType)type).getName();
            default:
                return getJasminTypeVar(type, type.getTypeOfElement());
        }


    }

    public static String getJasminTypeArray(ArrayType type) {
        StringBuilder translation = new StringBuilder();

        translation.append("[".repeat(Math.max(0, type.getNumDimensions())));

        translation.append(getJasminTypeVar(type, type.getTypeOfElements()));
        return translation.toString();

    }

    public static String getJasminTypeVar(Type type, ElementType elementType) {
        switch (elementType) {
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case STRING:
                return "Ljava/lang/String;";
            case CLASS:
                return ((ClassType) type).getName();
            case VOID:
                return "V";

        }
        return "";
    }
}
