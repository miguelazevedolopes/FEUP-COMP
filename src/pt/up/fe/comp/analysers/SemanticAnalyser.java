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

    public Boolean visitMethod(JmmNode methodRoot,Boolean dummy){
        for (JmmNode child : methodRoot.getChildren()) {
            String methodName=methodRoot.get("name");

            // Checks if return type matches the method's declared return type
            if(child.getKind().equals("Return")){
                System.out.println("Methodname: "+ methodName);
                switch(child.getJmmChild(0).getKind()){
                    case "IntegerLiteral":
                        if(!symbolTable.getReturnType(methodName).getName().equals("TypeInt"))
                            reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1, "Return value doesn't match the declared method type. Expected "+symbolTable.getReturnType(methodName).getName()+" but got TypeInt"));
                        break;
                    case "Boolean":
                        if(!symbolTable.getReturnType(methodName).getName().equals("TypeBoolean"))
                            reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1, "Return value doesn't match the declared method type. Expected "+symbolTable.getReturnType(methodName).getName()+" but got TypeBoolean"));
                        break;
                    case "Id":
                        Symbol returnSymbol=symbolTable.getLocalVariable(methodName, child.getJmmChild(0).get("name"));
                        if(returnSymbol==null){
                            returnSymbol=symbolTable.getField(methodName, child.getJmmChild(0).get("name"));
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

    public List<Report> getReports(){
        return reports;
    };
}
