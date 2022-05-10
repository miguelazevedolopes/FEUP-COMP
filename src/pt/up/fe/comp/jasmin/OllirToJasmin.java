package pt.up.fe.comp.jasmin;
import java.util.ArrayList;

import javax.management.RuntimeErrorException;
import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Method;

import pt.up.fe.specs.util.SpecsIo;

public class OllirToJasmin{

    private final ClassUnit ollir;
    protected StringBuilder jasminCode;

    public OllirToJasmin(ClassUnit ollir){
        jasminCode = new StringBuilder();
        this.ollir = ollir;
    }



    public String getCode(){

        jasminCode.append(".class public ").append(ollir.getClassName()).append("\n"); //Add class name
        
        
        String superClassName =  getFullyClassifiedName(ollir.getSuperClass());

        jasminCode.append(".super ").append(superClassName).append("\n\n"); //Add super class


        /*Generate code for method
        Always the same template*/
        String template = getTemplate();
        jasminCode.append(template.replace("${SUPER_NAME}",superClassName)).append('\n');

        //TODO: Generate fields



        //TODO: Generate the rest of the methods
        ArrayList<Method> methods = ollir.getMethods();
        for(var method: methods.subList(1, methods.size())){
            JasminMethod jasminMethod = new JasminMethod(method, ollir.getClassName(), ollir.getSuperClass(), ollir);           
            jasminCode.append(jasminMethod.getCode());
        }


        return jasminCode.toString();
    }

    private String getTemplate() {
        StringBuilder template = new StringBuilder();
        template.append(".method public <init>()V\n").append("\taload_0\n");
        template.append("\tinvokenonvirtual ${SUPER_NAME}<init>()V\n").append("\treturn\n").append(".end method");
        return template.toString();
    }



    public String getFullyClassifiedName(String className){
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
        return "java/lang/Object";

    }
}