package pt.up.fe.comp.jasmin;
import java.util.ArrayList;
import java.util.List;

import javax.management.RuntimeErrorException;
import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.ElementType;
import org.specs.comp.ollir.Field;
import org.specs.comp.ollir.Method;

import freemarker.core.builtins.sourceBI;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsIo;

public class OllirToJasmin{

    private final ClassUnit ollir;
    protected StringBuilder jasminCode;
    private List<Report> reports;

    public OllirToJasmin(ClassUnit ollir){
        
        jasminCode = new StringBuilder();
        this.ollir = ollir;
        this.reports = new ArrayList<>();
    }



    public String getCode(){

        jasminCode.append(".class public ").append(ollir.getClassName()).append("\n"); //Add class name
        
        
        String superClassName =  (ollir.getSuperClass());

        jasminCode.append(".super ");
        if (superClassName!= null)
            jasminCode.append(superClassName);
        else jasminCode.append("java/lang/Object");

        /*Generate code for method
        Always the same template*/
        String template = getTemplate(superClassName);

        jasminCode.append(template).append('\n');

            

        //TODO: Generate fields

        for (var field : ollir.getFields()) {
            jasminCode.append(getFields(field));
        }

        //TODO: Generate the rest of the methods
        ArrayList<Method> methods = ollir.getMethods();
        for(var method: methods.subList(1, methods.size())){
            JasminMethod jasminMethod = new JasminMethod(method, ollir.getClassName(), ollir.getSuperClass());           
            jasminCode.append(jasminMethod.generateJasminCode());
        }

        return jasminCode.toString();
    }

    private String getFields(Field field){
        StringBuilder code = new StringBuilder();
        code.append("\n.field ");
            if (field.isFinalField())
                code.append("final ");
            code.append("'").append(field.getFieldName()).append("' ");
            code.append(JasminUtils.getJasminType(field.getFieldType().getTypeOfElement(), ollir.getClassName()));
        return code.toString();
    }

    private String getTemplate(String superClassName) {
        StringBuilder template = new StringBuilder();
        template.append(".method public <init>()V\n").append("\taload_0\n");
        template.append("\tinvokenonvirtual " + superClassName+ "/<init>()V\n").append("\treturn\n").append(".end method");
        return template.toString();
    }



    // public String getFullyClassifiedName(String className){
    //     for (var importString : ollir.getImports()){
    //         var imports = importString.split("\\."); //get all imports

    //         String lastImport;
    //         if(imports.length == 0){
    //             lastImport = importString;
    //         }else{
    //             lastImport = imports[imports.length -1];
    //         }
            

    //         if(lastImport.equals(className)){
    //             return importString.replace("." , "/"); 
    //         }


    //     }
    //     return "java/lang/Object";

    // }
}