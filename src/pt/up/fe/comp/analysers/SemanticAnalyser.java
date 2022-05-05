package pt.up.fe.comp.analysers;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.jmm.analysis.table.Method;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.ast.JmmNode;



public class SemanticAnalyser extends PreorderJmmVisitor<Boolean, Boolean>{
    private List<Report> reports;
    private SymbolTable symbolTable;

    public SemanticAnalyser(SymbolTable symbolTable) {
        this.reports=new ArrayList<>();
        this.symbolTable=symbolTable;
        addVisit("ClassDeclaration", this::visitClass);
    }

    public Boolean visitMethod(JmmNode methodRoot,Boolean dummy){
        for (JmmNode child : methodRoot.getChildren()) {
            switch(child.getKind()){
                case "Return":
                    switch(child.getJmmChild(0).getKind()){
                        case "IntegerLiteral":
                            
                            break;
                    }
                break;
            }
        }
        return true;
    }

    public Boolean visitClass(JmmNode classRoot,Boolean dummy){
        for (JmmNode child : classRoot.getChildren()) {
            if (child.getKind().equals("MainMethod") || child.getKind().equals("NormalMethod")) {
                visitMethod(child, dummy);
            }
        }
        return true;
    }

    List<Report> getReports(){
        return reports;
    };
}
