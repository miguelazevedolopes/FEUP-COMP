package pt.up.fe.comp.analysers;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
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
        Type returnType=symbolTable.getReturnType(methodName);
        Type returnedValueType=resolveType(node.getJmmChild(0), methodName);
        if(returnedValueType==null) return;
        if(!returnType.equals(returnedValueType))
            reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(node.get("line")),Integer.parseInt(node.get("col")), "Return value doesn't match the declared method type. Expected "+returnType+" but got " + returnedValueType));   
    }

    private void visitScope(JmmNode node, String methodName){
        for (JmmNode child : node.getChildren()) {
            String nodeKind=child.getKind();
            if(nodeKind.equals("WhileStatement")||nodeKind.equals("IfStatement")){
                Type statementExpressionType = resolveType(child.getJmmChild(0),methodName);
                if(!statementExpressionType.equals(new Type("TypeBoolean", false))){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Expressions in conditions must return a boolean, instead got "+ statementExpressionType));
                }
                visitScope(child, methodName);
            }
            else if(nodeKind.equals("ReturnRule")){
                visitScope(child, methodName);
                checkReturnType(child, methodName);
            }
            else if(nodeKind.equals("Id")){
                Symbol s=getDeclaredSymbol(child.get("name"), methodName);
                if(s==null){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"The variable with name '"+child.get("name")+"' is used without being declared"));
                }
            }
            else if(nodeKind.equals("Equality")){
                checkValidEquality(child, methodName);
                visitScope(child, methodName);
            }
            else if(nodeKind.equals("SUM")||nodeKind.equals("MUL")||nodeKind.equals("SUB")||nodeKind.equals("DIV")){
                visitScope(child, methodName);
                Type firstOperandType = resolveType(child.getJmmChild(0),methodName);
                Type secondOperandType = resolveType(child.getJmmChild(1),methodName);
                if(!firstOperandType.equals(secondOperandType) || !firstOperandType.equals(new Type("TypeInt", false))){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Invalid operation: "+child.getJmmChild(0).getKind()+" + "+child.getJmmChild(1).getKind()));
                }
            }
            else if(nodeKind.equals("DotExpression")){
                resolveType(child, methodName);
            }
        }
    }

    private Type resolveType(JmmNode child, String methodName) {
        String nodeKind=child.getKind();
        if(nodeKind.equals("Boolean")||nodeKind.equals("ANDD")||nodeKind.equals("Negation")||nodeKind.equals("LESSTHAN")){
            return new Type("TypeBoolean", false);
        }
        else if(nodeKind.equals("String")){
            return new Type("TypeString", false);
        }
        else if(nodeKind.equals("IntegerLiteral")){
            return new Type("TypeInt", false);
        }
        else if(nodeKind.equals("Id")){
            Symbol s=getDeclaredSymbol(child.get("name"), methodName);
            if(s==null){
                reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"The variable with name '"+child.get("name")+"' is used without being declared"));
                return null;
            }
            else if (s.getType().isArray()){
                if(child.getChildren().size()==0 || !child.getJmmChild(0).getKind().equals("AccessToArray")){
                    return s.getType();
                }
                else{
                    JmmNode indexAccessNode=child.getJmmChild(0).getJmmChild(0);
                    Type indexAccessType=resolveType(indexAccessNode, methodName);
                    if(!(indexAccessType.getName().equals("TypeInt")) ){
                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+s.getName()+"': "+"array access must be done using a integer type expression."));
                        return null;
                    }
                    return indexAccessType;
                }
                
            }
            else{
                if(child.getChildren().size()>0 && child.getJmmChild(0).getKind().equals("AccessToArray")){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+s.getName()+"': "+"is not an array"));
                }
                return s.getType();
            }
        }
        else if(nodeKind.equals("SUM")||nodeKind.equals("MUL")||nodeKind.equals("SUB")||nodeKind.equals("DIV")){
            return new Type("TypeInt", false);
        }
        else if(nodeKind.equals("DotExpression")){
            if(child.getJmmChild(0).getKind().equals("This")){
                String methodCallName=child.getJmmChild(1).getJmmChild(0).get("name");
                if(symbolTable.methodExists(methodCallName)){
                    List<JmmNode> args=child.getJmmChild(1).getChildren();
                    List<Symbol> params = symbolTable.getParameters(methodCallName);
                    if((args.size()-1)!=params.size()){
                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : the declared method's signature doesn't match the one being called. Too many/little arguments."));
                        return null;
                    }
                    else{
                        for (int i=1;i<args.size();i++) {
                            if(args.get(i).getKind().equals("Id")){
                                Symbol localVar=getDeclaredSymbol(args.get(i).get("name"),methodName);
                                if(localVar==null){
                                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"The variable with name '"+args.get(i).get("name")+"' is used without being declared"));
                                    return null;
                                }
                            }
                            
                            Type paramType=resolveType(args.get(i), methodName);
                            if(!params.get(i-1).getType().equals(paramType)){
                                if(!(params.get(i-1).getType().getName().equals("TypeInt") && args.get(i).getKind().equals("IntegerLiteral"))){
                                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : the declared method's signature doesn't match the one being called. The param types don't match the ones being passed as argument."));
                                    return null;
                                }
                            }
                            
                        
                        }
                        return symbolTable.getReturnType(methodCallName);
                    }
                }
                else if(symbolTable.getSuper().isEmpty()){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : there is no such method with that signature."));
                    return null;
                }
            }
            else if(child.getJmmChild(0).getKind().equals("Id")){
                if(!symbolTable.getImports().contains(child.getJmmChild(0).get("name"))){
                    Symbol firstIdentifier=getDeclaredSymbol(child.getJmmChild(0).get("name"),methodName);
                    if(firstIdentifier==null){
                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"The variable with name '"+child.getJmmChild(0).get("name")+"' is used without being declared"));
                        return null;
                    }
                    else{
                        if(firstIdentifier.getType().getName().equals(symbolTable.getClassName())){

                            String methodCallName=child.getJmmChild(1).getJmmChild(0).get("name");
                            if(symbolTable.methodExists(methodCallName)){
                                List<JmmNode> args=child.getJmmChild(1).getChildren();
                                List<Symbol> params = symbolTable.getParameters(methodCallName);
                                if((args.size()-1)!=params.size()){
                                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : the declared method's signature doesn't match the one being called. Too many/little arguments."));
                                    return null;
                                }
                                else{
                                    for (int i=1;i<args.size();i++) {
                                        if(args.get(i).getKind().equals("Id")){
                                            Symbol localVar=getDeclaredSymbol(args.get(i).get("name"),methodName);
                                            if(localVar==null){
                                                reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"The variable with name '"+args.get(i).get("name")+"' is used without being declared"));
                                                return null;
                                            }
                                        }
                                        
                                        Type paramType=resolveType(args.get(i), methodName);
                                        if(!params.get(i-1).getType().equals(paramType)){
                                            if(!(params.get(i-1).getType().getName().equals("TypeInt") && args.get(i).getKind().equals("IntegerLiteral"))){
                                                reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : the declared method's signature doesn't match the one being called. The param types don't match the ones being passed as argument."));
                                                return null;
                                            }
                                        }
                                    }
                                    return symbolTable.getReturnType(methodCallName);
                                }
                                
                            }
                            else if(symbolTable.getSuper().isEmpty()){
                                reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : there is no such method with that signature."));
                                return null;
                            }
                            else{
                                return new Type("UndefinedImport", false);
                            }
                        }
                        else if(firstIdentifier.getType().isArray()){
                            if(child.getJmmChild(1).getJmmChild(0).getKind().equals("Length")){
                                return new Type("TypeInt",false);
                            }
                        }
                            
                        
                    }
                }
                else{
                    return new Type("UndefinedImport", false);
                }
            }
            else{
                reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Invalid dot expression."));
                return null;
            }
            return null;
        }
        else if(nodeKind.equals("InitializeArray")){
            return new Type("TypeInt", true);
        }
        else if(nodeKind.equals("NewObject")){
            if(!symbolTable.getClassName().equals(child.getJmmChild(0).get("name"))){
                if(!symbolTable.getImports().contains(child.getJmmChild(0).get("name"))){
                    if(!symbolTable.getSuper().equals(child.getJmmChild(0).get("name"))){
                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Invalid object type: "+child.getJmmChild(0).get("name")));
                        return null;
                    }
                }
            }
            return new Type(child.getJmmChild(0).get("name"),false);
        }
        return null;
    }

    private void checkValidEquality(JmmNode child, String methodName) {
        
        JmmNode firstChild=child.getJmmChild(0);
        JmmNode secondChild=child.getJmmChild(1);

        Type firstChildType = resolveType(firstChild, methodName);
        Type secondChildType=resolveType(secondChild, methodName);
        if (firstChildType==null || secondChildType ==null){
            reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Invalid equality expression."));
            System.out.println(firstChildType);
            System.out.println(secondChildType);
            return;
        }
        if(!firstChildType.equals(secondChildType)){
            if(!secondChildType.getName().equals("UndefinedImport") ){
                if(firstChildType.getName().equals(symbolTable.getSuper()) && secondChildType.getName().equals(symbolTable.getClassName()))
                    return;
                else if(!firstChildType.getName().equals(symbolTable.getClassName()) && symbolTable.getImports().contains(secondChildType.getName())){
                    return;
                }
                reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Can't assign variable of type '" + secondChildType.getName()+ "' to variable of type '"+ firstChildType.getName() + "'"));
            }
        }  
    }

    private void visitMethod(JmmNode methodRoot){
        JmmNode methodBodyNode=null;
        String methodName=methodRoot.get("name");
        for (JmmNode child : methodRoot.getChildren()) {
            if(child.getKind().equals("MethodBody")){
                methodBodyNode=child;
            }
            else if(child.getKind().equals("ReturnRule")){
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
