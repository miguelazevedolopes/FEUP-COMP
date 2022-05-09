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

    private Symbol getDeclaredSymbol(String name, String methodName){
        Symbol returnSymbol=null;

        returnSymbol=symbolTable.getLocalVariable(methodName, name);

        if(returnSymbol==null){
            returnSymbol=symbolTable.getField(methodName, name);
        }
        if(returnSymbol==null){
            returnSymbol=symbolTable.getParameter(methodName, name);
        }
        return returnSymbol;
    }

    private void checkReturnType(JmmNode node, String methodName){
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
                Symbol returnSymbol=getDeclaredSymbol(node.getJmmChild(0).get("name"), methodName);
                if(returnSymbol==null) break;
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

    private void visitScope(JmmNode node, String methodName){
        for (JmmNode child : node.getChildren()) {
            switch(child.getKind()){
                case "WhileStatement":
                    visitScope(child, methodName);
                    break;
                case "IfStatement":
                    visitScope(child, methodName);
                    break;    
                case "Return":
                    visitScope(child, methodName);
                    checkReturnType(child, methodName);
                    break;
                case "Id":
                    Symbol s=getDeclaredSymbol(child.get("name"), methodName);
                    if(s==null){
                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"The variable with name '"+child.get("name")+"'' is used without being declared"));
                    }
                    break;
                case "Equality":
                    checkValidEquality(child, methodName);
                    visitScope(child, methodName);
                    break;
                case "SUM":
                    visitScope(child, methodName);
                    String firstOperandType = resolveType(child.getJmmChild(0),methodName);
                    String secondOperandType = resolveType(child.getJmmChild(1),methodName);
                    if(!firstOperandType.equals(secondOperandType) || !firstOperandType.equals("TypeInt")){
                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"Invalid operation: "+child.getJmmChild(0).getKind()+" + "+child.getJmmChild(1).getKind()));
                    }
                    break;
                case "MUL":
                    visitScope(child, methodName);
                    firstOperandType = resolveType(child.getJmmChild(0),methodName);
                    secondOperandType = resolveType(child.getJmmChild(1),methodName);
                    if(!firstOperandType.equals(secondOperandType) || !(firstOperandType.equals("TypeInt") || firstOperandType.equals("TypeString") )){
                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"Invalid operation: "+child.getJmmChild(0).getKind()+" + "+child.getJmmChild(1).getKind()));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private String resolveType(JmmNode child, String methodName) {
        switch(child.getKind()){
            case "Boolean":
                return "TypeBoolean";
            case "String":
                return "TypeString";
            case "IntegerLiteral":
                return "TypeInt";
            case "Id":
                Symbol s=getDeclaredSymbol(child.get("name"), methodName);
                if(s==null){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"The variable with name '"+child.get("name")+"'' is used without being declared"));
                    return "Undefined";
                }
                else if (s.getType().isArray()){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"Variable '"+s.getName()+"': "+"Array isn't being accessed properly"));
                    return "Undefined";
                }
                else{
                    return s.getType().getName();
                }
            // case "AccessToArray":
            //     s=getDeclaredSymbol(child.get("name"), methodName);
            //     if(s==null){
            //         reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"The variable with name '"+child.get("name")+"'' is used without being declared"));
            //         return "Undefined";
            //     }
            //     return "TypeInt";
            case "SUM":
                return "TypeInt";
            case "MUL":
                return "TypeInt";
            case "DotExpression":
                if(child.getJmmChild(0).getKind().equals("This")){
                    String name=child.getJmmChild(1).getJmmChild(0).get("name");
                    return symbolTable.getReturnType(name).getName();
                }
                return "Undefined";
        }
        return "Undefined";
    }

    private void checkValidEquality(JmmNode child, String methodName) {
        String firstChildName=child.getJmmChild(0).get("name");
        Symbol firstChildSymbol=getDeclaredSymbol(firstChildName, methodName);
        if(firstChildSymbol==null) return;
        String secondChildKind=child.getJmmChild(1).getKind();
        Boolean firstIsAccessed=false;
        if(secondChildKind.equals("AccessToArray")){
            secondChildKind=child.getJmmChild(2).getKind();
            if(child.getJmmChild(1).getJmmChild(0).getKind().equals("Id")){
                String indexVarName=child.getJmmChild(1).getJmmChild(0).get("name");
                Symbol indexVarSymbol=getDeclaredSymbol(indexVarName, methodName);
                String indexVarType=null;
                if(indexVarSymbol==null){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"The variable with name '"+indexVarName+"' is used without being declared"));
                }
                else{
                    indexVarType=getDeclaredSymbol(indexVarName, methodName).getType().getName();
                    if(!indexVarType.equals("TypeInt")){
                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"Variable '"+firstChildSymbol.getName()+"': "+"array access must be done using a integer type expression. Instead got var '"+indexVarName+"' with type "+indexVarType));
                    };
                }
                
            }
            else if(!child.getJmmChild(1).getJmmChild(0).getKind().equals("IntegerLiteral")){
                reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"Variable '"+firstChildSymbol.getName()+"': "+"array access must be done using a integer type expression. Instead got type "+child.getJmmChild(1).getJmmChild(0).getKind()));
            }
            firstIsAccessed=true;
        }
        if(firstIsAccessed && !firstChildSymbol.getType().isArray()){
            reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"Variable '"+firstChildSymbol.getName()+"': "+"is not an array"));
        }
        switch(secondChildKind){
            case "SUM":
                if(!firstChildSymbol.getType().getName().equals("TypeInt")){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"Variable '"+firstChildSymbol.getName()+"': "+"The result of a sum can't be assigned to a var of type "+firstChildSymbol.getType().getName()));
                }
                else if(!firstIsAccessed && firstChildSymbol.getType().isArray()){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"Variable '"+firstChildSymbol.getName()+"': "+"Array isn't being accessed properly"));
                }
                break;
            case "IntegerLiteral":
                if(!firstChildSymbol.getType().getName().equals("TypeInt")){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"Variable '"+firstChildSymbol.getName()+"': "+"Can't assign an Integer Literal to a var of type "+firstChildSymbol.getType().getName()));
                }
                else if(!firstIsAccessed && firstChildSymbol.getType().isArray()){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"Variable '"+firstChildSymbol.getName()+"': "+"Array isn't being accessed properly"));
                }
                break;
            case "Boolean":
                if(!firstChildSymbol.getType().getName().equals("TypeBoolean")){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"Variable '"+firstChildSymbol.getName()+"': "+"Can't assign an Boolean to a var of type "+firstChildSymbol.getType().getName()));
                }
                else if(!firstIsAccessed && firstChildSymbol.getType().isArray()){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"Variable '"+firstChildSymbol.getName()+"': "+"Array isn't being accessed properly"));
                }
                break;
            case "String":
                if(!firstChildSymbol.getType().getName().equals("TypeString")){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"Variable '"+firstChildSymbol.getName()+"': "+"Can't assign a String to a var of type "+firstChildSymbol.getType().getName()));
                }
                else if(!firstIsAccessed && firstChildSymbol.getType().isArray()){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,-1,"Variable '"+firstChildSymbol.getName()+"': "+"Array isn't being accessed properly"));
                }
                break;
        }
    }

    private void visitMethod(JmmNode methodRoot){
        JmmNode methodBodyNode=null;
        String methodName=methodRoot.get("name");
        for (JmmNode child : methodRoot.getChildren()) {
            if(child.getKind().equals("MethodBody")){
                methodBodyNode=child;
            }
            else if(child.getKind().equals("Return")){
                visitScope(child, methodName);
                checkReturnType(child, methodName);
            }
        }
        visitScope(methodBodyNode, methodName);
    }

    public Boolean visitClass(JmmNode classRoot,Boolean dummy){
        for (JmmNode child : classRoot.getChildren()) {
            if (child.getKind().equals("MainMethod") || child.getKind().equals("NormalMethod")) {
                visitMethod(child);
            }
        }
        return true;
    }

    public List<Report> getReports(){
        return reports;
    };
}
