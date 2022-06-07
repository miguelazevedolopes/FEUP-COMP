package pt.up.fe.comp.jasmin;

import java.util.ArrayList;
import java.util.List;
import org.specs.comp.ollir.ClassUnit;

import pt.up.fe.comp.jasmin.Methods.JasminMethod;
import pt.up.fe.comp.jmm.report.Report;

public class OllirToJasmin{

    private final ClassUnit ollir;
    private List<Report> reports;

    public OllirToJasmin(ClassUnit ollir){
        this.ollir = ollir;
        this.reports = new ArrayList<>();
    }

    public List<Report> getReports(){
        return reports;
    }


    public String getCode(){

        StringBuilder jasminCode = new StringBuilder();

        jasminCode.append(".class public ").append(ollir.getClassName());

        jasminCode.append(getSuper());


        //Fields
        for (var field : ollir.getFields()) {
            jasminCode.append("\n.field ");
            if (field.isFinalField())
                jasminCode.append("final ");
            jasminCode.append("'").append(field.getFieldName()).append("' ");
            jasminCode.append(JasminUtils.getJasminType(field.getFieldType().getTypeOfElement(), ollir.getClassName()));

        }

        //Methods 
        for (var method : ollir.getMethods()) {
            JasminMethod jasminMethod = new JasminMethod(method, ollir.getClassName(), ollir.getSuperClass());
            jasminCode.append(jasminMethod.generateJasminCode());
            reports.addAll(jasminMethod.getReports());
        }

        return jasminCode.toString();
    }


    private String getSuper() {
        StringBuilder jasminCode = new StringBuilder();
        jasminCode.append("\n.super ");

        if (ollir.getSuperClass() != null)
            jasminCode.append(ollir.getSuperClass());
        else
            jasminCode.append("java/lang/Object");
        jasminCode.append("\n");
        return jasminCode.toString();
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