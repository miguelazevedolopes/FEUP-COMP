package pt.up.fe.comp.ollir;

import java.util.Collections;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

public class JmmOptimizer implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        var OllirGenerator = new OllirGenerator(semanticsResult.getSymbolTable());
        OllirGenerator.visit(semanticsResult.getRootNode());
        var ollirCode = OllirGenerator.getCode();

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    
    
}
