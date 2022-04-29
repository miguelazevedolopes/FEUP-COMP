package pt.up.fe.comp;
import java.util.Collections;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.symbolTable.SymbolTable;
import pt.up.fe.comp.symbolTable.SymbolTableVisitor;
public class JmmAnalyser implements JmmAnalysis {
    
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        SymbolTableVisitor symbolTableVisitor = new SymbolTableVisitor(); 
        symbolTableVisitor.visit(parserResult.getRootNode());
        SymbolTable symbolTable = symbolTableVisitor.getSymbolTable();
        System.out.println(symbolTable.print());
        return new JmmSemanticsResult(parserResult, symbolTable,Collections.emptyList());
    }
}