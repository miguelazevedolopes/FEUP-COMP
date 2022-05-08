package pt.up.fe.comp.analysers;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.jmm.analysis.table.Method;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp.jmm.ast.JmmNode;



public class SemanticAnalyser extends PreorderJmmVisitor<Boolean, Boolean>{
    private List<Report> reports;
    private SymbolTable symbolTable;

    public SemanticAnalyser(SymbolTable symbolTable) {
        this.reports=new ArrayList<>();
        this.symbolTable=symbolTable;
        addVisit("ClassDeclaration", this::visitClass);
    }

    public Boolean checkReturnType(JmmNode node, String methodName){
        switch(node.getJmmChild(0).getKind()){
            case "IntegerLiteral":
                if(!symbolTable.getReturnType(methodName).getName().equals("TypeInt"))
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1, "Return value doesn't match the declared method type. Expected "+symbolTable.getReturnType(methodName).getName()+" but got TypeInt"));
                break;
            case "Boolean":
                if(!symbolTable.getReturnType(methodName).getName().equals("TypeBoolean"))
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1, "Return value doesn't match the declared method type. Expected "+symbolTable.getReturnType(methodName).getName()+" but got TypeBoolean"));
                break;
            case "Id":
                Symbol returnSymbol=symbolTable.getLocalVariable(methodName, node.getJmmChild(0).get("name"));
                if(returnSymbol==null){
                    returnSymbol=symbolTable.getField(methodName, node.getJmmChild(0).get("name"));
                }
                if(returnSymbol==null){
                    returnSymbol=symbolTable.getParameter(methodName, node.getJmmChild(0).get("name"));
                }
                if(!symbolTable.getReturnType(methodName).getName().equals(returnSymbol.getType().getName())){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1, "Return value doesn't match the declared method type. Expected "+symbolTable.getReturnType(methodName).getName()+" but got "+returnSymbol.getType().getName()));
                }
                break;
            case "Negation":
                if(!symbolTable.getReturnType(methodName).getName().equals("TypeBoolean")){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1, "Return value doesn't match the declared method type. Expected "+symbolTable.getReturnType(methodName).getName()+" but got TypeBoolean"));
                }
                break;
            case "ANDD":
                if(!symbolTable.getReturnType(methodName).getName().equals("TypeBoolean")){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1, "Return value doesn't match the declared method type. Expected "+symbolTable.getReturnType(methodName).getName()+" but got TypeBoolean"));
                }
                break;
            case "SUM":
                if(!symbolTable.getReturnType(methodName).getName().equals("TypeInt")){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1, "Return value doesn't match the declared method type. Expected "+symbolTable.getReturnType(methodName).getName()+" but got TypeInt"));
                }
                break;
            default:
                break;
        }
        return true;
    }

    public Boolean checkDeclaredVars(JmmNode node, String methodName){
        return true;
    }

    public Boolean visitScope(JmmNode node, String methodName){
        for (JmmNode child : node.getChildren()) {
            switch(child.getKind()){
                case "WhileStatement":
                    visitScope(node.getJmmChild(0), methodName);
                    break;
                case "IfStatement":
                    visitScope(node.getJmmChild(0), methodName);
                    break;    
                case "Return":
                    checkReturnType(node, methodName);
                    break;
                case "Equality":
                    checkDeclaredVars(node,methodName);
                    break;
                default:
                    break;
            }
        }
        return true;
    }

    public Boolean visitMethod(JmmNode methodRoot,Boolean dummy){
        JmmNode methodBodyNode=null;
        String methodName=methodRoot.get("name");
        for (JmmNode child : methodRoot.getChildren()) {
            if(child.getKind().equals("MethodBody")){
                methodBodyNode=child;
            }
        }
        visitScope(methodBodyNode, methodName);
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

    public List<Report> getReports(){
        return reports;
    };
}
