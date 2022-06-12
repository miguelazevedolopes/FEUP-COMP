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
            ollir.buildCFGs(); 
            ollir.checkMethodLabels(); 
            ollir.buildVarTables();

            OllirToJasmin ollirToJasmin = new OllirToJasmin(ollir);
            String jasminCode = ollirToJasmin.getCode();
            System.out.println("\n------------------- JASMIN CODE GENERATE -------------------");
            System.out.println(jasminCode);
            System.out.println("---------------------------------------------------");

            return new JasminResult(ollirResult, jasminCode, ollirToJasmin.getReports());
        }catch(OllirErrorException e){
            return new JasminResult(ollir.getClassName(), null,
            Collections.singletonList(Report.newError(Stage.GENERATION, -1, -1, "Exception during Jasmin generation", e)));
        }
        
    }
        
}
