package pt.up.fe.comp.jasmin;
import javax.management.RuntimeErrorException;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Method;

import pt.up.fe.specs.util.SpecsIo;

public class OllirToJasmin{

    private final ClassUnit ollir;
    protected StringBuilder jasminCode;

    public OllirToJasmin(ClassUnit ollir){
        this.ollir = ollir;
    }



    public String getCode(){

        var code = new StringBuilder();
        code.append(".class public ").append(ollir.getClassName()).append("\n"); //Add class name
    
        var superClassName = ollir.getSuperClass();
        code.append(".super ").append(superClassName).append("\n"); //Add super class

        /*Generate code for method
        Always the same template*/
        code.append(SpecsIo.getResource("src/pt/up/fe/comp/jasmin/jasminConstructor.template").replace("${SUPER_NAME}",superClassName)).append('\n');


        //TODO: Generate fields



        //TODO: Generate methods
        
        for(var method: ollir.getMethods()){
            //System.out.println("METHOD " + method.getMethodName());
            JasminMethod jasminMethod = new JasminMethod(method, ollir.getClassName());
            jasminMethod.generateCode();


        }


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