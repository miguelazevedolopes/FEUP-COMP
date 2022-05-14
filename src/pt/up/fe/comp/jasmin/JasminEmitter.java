package pt.up.fe.comp.jasmin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.OllirErrorException;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;


public class JasminEmitter implements JasminBackend{

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollir = ollirResult.getOllirClass();

        try{
            ollir.buildCFGs(); // build the CFG of each method
            ollir.checkMethodLabels(); // check the use of labels in the OLLIR loaded
            ollir.outputCFGs(); // output to .dot files the CFGs, one per method
            ollir.buildVarTables(); // build the table of variables for each method

            ollir.show(); // print to console main information about the input OLLIR

            // Convert the OLLIR to a String containing the equivalent Jasmin code
            StringBuilder jasminCode = new StringBuilder();
            // More reports from this stage
            List<Report> reports = new ArrayList<>();

            jasminCode.append(".class public ").append(ollir.getClassName());

            jasminCode.append("\n.super ");

            if (ollir.getSuperClass() != null)
                jasminCode.append(ollir.getSuperClass());
            else
                jasminCode.append("java/lang/Object");
            jasminCode.append("\n");

            for (var field : ollir.getFields()) {
                jasminCode.append("\n.field ");
                if (field.isFinalField())
                    jasminCode.append("final ");
                jasminCode.append("'").append(field.getFieldName()).append("' ");
                switch (field.getFieldType().toString()) {
                    case "INT32":
                        jasminCode.append("I");
                        break;
                    case "BOOLEAN":
                        jasminCode.append("Z");
                        break;
                    case "ARRAYREF":
                        jasminCode.append("[I");
                        break;
                    case "OBJECTREF":
                        jasminCode.append(ollir.getClassName());
                    default:
                        break;
                }
            }

            for (var method : ollir.getMethods()) {
                JasminMethod jasminMethod = new JasminMethod(method, ollir.getClassName(), ollir.getSuperClass());
                jasminCode.append(jasminMethod.generateJasminCode());
                reports.addAll(jasminMethod.getReports());
            }

            System.out.println("\n------------------- JASMIN CODE GENERATE -------------------");
            System.out.println(jasminCode);
            System.out.println("---------------------------------------------------");

            return new JasminResult(ollirResult, jasminCode.toString(), reports);
        }catch(OllirErrorException e){
            return new JasminResult(ollir.getClassName(), null,
            Collections.singletonList(Report.newError(Stage.GENERATION, -1, -1, "Exception during Jasmin generation", e)));
        }
        
    }
    
    
}
