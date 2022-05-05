package pt.up.fe.comp.ollir;

import java.util.Collections;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class JmmOptmizer implements JmmOptimization{
    public OllirResult toOllir(JmmSemanticsResult semanticsResult){
        var ollirGenerator = new OllirGenerator(SemanticsResult.getSymbolTable());
        ollirGenerator.visit(SemanticsResult.getRootNode());
        var ollirCode = ollirGenerator.getCode();

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }
}