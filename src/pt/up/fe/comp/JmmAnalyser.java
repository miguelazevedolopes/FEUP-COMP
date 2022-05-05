package pt.up.fe.comp;
import java.util.Collections;
import java.util.List;

import pt.up.fe.comp.analysers.SemanticAnalyser;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.symbolTable.SymbolTableBuilder;
import pt.up.fe.comp.symbolTable.SymbolTableVisitor;
public class JmmAnalyser implements JmmAnalysis {
    
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        SymbolTableVisitor symbolTableVisitor = new SymbolTableVisitor(); 
        symbolTableVisitor.visit(parserResult.getRootNode());
        SymbolTableBuilder symbolTable = symbolTableVisitor.getSymbolTable();
        List<Report> reports=symbolTableVisitor.getReports();
        SemanticAnalyser semanticAnalyser= new SemanticAnalyser(symbolTable);
        semanticAnalyser.visit(parserResult.getRootNode());
        reports.addAll(semanticAnalyser.getReports());
        return new JmmSemanticsResult(parserResult, symbolTable,reports);
    }
}