package pt.up.fe.comp.jasmin;

import java.util.Collections;

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
        }catch(OllirErrorException e){
            return new JasminResult(ollir.getClassName(), null,
            Collections.singletonList(Report.newError(Stage.GENERATION, -1, -1, "Exception during Jasmin generation", e)));
        }
        
        
        String jasminCode = new OllirToJasmin(ollir).getCode();
        return new JasminResult(ollirResult, jasminCode, Collections.emptyList());
    }
    
    
}
