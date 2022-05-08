package pt.up.fe.comp.jasmin;
import javax.management.RuntimeErrorException;

import org.specs.comp.ollir.ClassUnit;

public class OllirToJasmin{

    private final ClassUnit ollir;
    protected StringBuilder jasminCode;

    public OllirToJasmin(ClassUnit ollir){
        this.ollir = ollir;
    }



    public String getCode(){

        var code = new StringBuilder();

        code.append(".class public ").append(ollir.getClassName()).append("\n"); //Add class name
        code.append(".super "); //Add super class
        code.append(ollir.getSuperClass()).append("\n");

        return code.toString();
    }

    public String getSuperClass(String className){
        for (var importString : ollir.getImports()){
            var imports = importString.split("\\."); //get all imports

            String lastImport;
            if(imports.length == 0){
                lastImport = importString;
            }else{
                lastImport = imports[imports.length -1];
            }
            

            if(lastImport.equals(className)){
                return importString.replace("." , "/"); 
            }


        }
        throw new RuntimeErrorException(null, "Couldn't find import for the class " + className);

    }
}