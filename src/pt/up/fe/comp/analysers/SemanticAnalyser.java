package pt.up.fe.comp.analysers;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.jmm.analysis.table.Method;
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
            switch(child.getKind()){
                case "WhileStatement":
                    Type statementExpressionType = resolveType(child.getJmmChild(0),methodName);
                    if(!statementExpressionType.equals(new Type("TypeBoolean", false))){
                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Expressions in conditions must return a boolean, instead got "+ statementExpressionType));
                    }
                    visitScope(child, methodName);
                    break;
                case "IfStatement":
                    statementExpressionType = resolveType(child.getJmmChild(0),methodName);
                    if(!statementExpressionType.equals(new Type("TypeBoolean", false))){
                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Expressions in conditions must return a boolean, instead got "+ statementExpressionType));
                    }
                    visitScope(child, methodName);
                    break;    
                case "ReturnRule":
                    visitScope(child, methodName);
                    checkReturnType(child, methodName);
                    break;
                case "Id":
                    Symbol s=getDeclaredSymbol(child.get("name"), methodName);
                    if(s==null){
                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"The variable with name '"+child.get("name")+"' is used without being declared"));
                    }
                    break;
                case "Equality":
                    checkValidEquality(child, methodName);
                    visitScope(child, methodName);
                    break;
                case "SUM":
                    visitScope(child, methodName);
                    Type firstOperandType = resolveType(child.getJmmChild(0),methodName);
                    Type secondOperandType = resolveType(child.getJmmChild(1),methodName);
                    if(!firstOperandType.equals(secondOperandType) || !firstOperandType.equals(new Type("TypeInt", false))){
                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Invalid operation: "+child.getJmmChild(0).getKind()+" + "+child.getJmmChild(1).getKind()));
                    }
                    break;
                case "MUL":
                    visitScope(child, methodName);
                    firstOperandType = resolveType(child.getJmmChild(0),methodName);
                    secondOperandType = resolveType(child.getJmmChild(1),methodName);
                    if(!firstOperandType.equals(secondOperandType) || !(firstOperandType.equals(new Type("TypeInt", false)) || firstOperandType.equals(new Type("TypeString", false)) )){
                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Invalid operation: "+child.getJmmChild(0).getKind()+" + "+child.getJmmChild(1).getKind()));
                    }
                    break;
                case "DotExpression":
                    if(child.getJmmChild(0).getKind().equals("This")){
                        String methodCallName=child.getJmmChild(1).getJmmChild(0).get("name");
                        if(symbolTable.methodExists(methodCallName)){
                            List<JmmNode> args=child.getJmmChild(1).getChildren();
                            List<Symbol> params = symbolTable.getParameters(methodCallName);
                            if((args.size()-1)!=params.size()){
                                reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : the declared method's signature doesn't match the one being called. Too many/little arguments."));
                                
                            }
                            else{
                                for (int i=1;i<args.size();i++) {
                                    if(args.get(i).getKind().equals("Id")){
                                        Symbol localVar=getDeclaredSymbol(args.get(i).get("name"),methodName);
                                        if(localVar==null){
                                            reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"The variable with name '"+args.get(i).get("name")+"' is used without being declared"));
                                        }
                                        else if(!params.get(i-1).getType().getName().equals(localVar.getType().getName())){
                                            reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : the declared method's signature doesn't match the one being called. The param types don't match the ones being passed as argument."));
                                        }
                                    }
                                    else if(args.get(i).getKind().equals("DotExpression")){
                                        Type type=resolveType(args.get(i), methodName);
                                        if(!params.get(i-1).getType().equals(type)){
                                            reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : the declared method's signature doesn't match the one being called. The param types don't match the ones being passed as argument."));
                                        }
                                    }
                                    else if(!params.get(i-1).getType().getName().equals(args.get(i).getKind())){
                                        if(!(params.get(i-1).getType().getName().equals("TypeInt") && args.get(i).getKind().equals("IntegerLiteral")))
                                            reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : the declared method's signature doesn't match the one being called. The param types don't match the ones being passed as argument."));
                                    }
                                }
                            }
                        }
                        else if(symbolTable.getSuper().isEmpty()){
                            reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : there is no such method with that signature."));
                        }
                    }
                    else if(child.getJmmChild(0).getKind().equals("Id")){
                        if(!symbolTable.getImports().contains(child.getJmmChild(0).get("name"))){
                            Symbol firstIdentifier=getDeclaredSymbol(child.getJmmChild(0).get("name"),methodName);
                            if(firstIdentifier==null){
                                reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"The variable with name '"+child.getJmmChild(0).get("name")+"' is used without being declared"));
                            }
                            else{
                                if(firstIdentifier.getType().getName().equals(symbolTable.getClassName())){

                                    String methodCallName=child.getJmmChild(1).getJmmChild(0).get("name");
                                    if(symbolTable.methodExists(methodCallName)){
                                        List<JmmNode> args=child.getJmmChild(1).getChildren();
                                        List<Symbol> params = symbolTable.getParameters(methodCallName);
                                        if((args.size()-1)!=params.size()){
                                            reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : the declared method's signature doesn't match the one being called. Too many/little arguments."));
                                        }
                                        else{
                                            for (int i=1;i<args.size();i++) {
                                                if(args.get(i).getKind().equals("Id")){
                                                    Symbol localVar=getDeclaredSymbol(args.get(i).get("name"),methodName);
                                                    if(localVar==null){
                                                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"The variable with name '"+args.get(i).get("name")+"' is used without being declared"));
                                                    }
                                                    else if(!params.get(i-1).getType().getName().equals(localVar.getType().getName())){
                                                        if(!(params.get(i-1).getType().getName().equals("TypeInt") && args.get(i).getKind().equals("IntegerLiteral")))
                                                            reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : the declared method's signature doesn't match the one being called. The param types don't match the ones being passed as argument."));
                                                    }
                                                }
                                                else if(args.get(i).getKind().equals("DotExpression")){
                                                    Type type=resolveType(args.get(i), methodName);
                                                    if(!params.get(i-1).getType().equals(type)){
                                                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : the declared method's signature doesn't match the one being called. The param types don't match the ones being passed as argument."));
                                                    }
                                                }
                                                else if(!(params.get(i-1).getType().getName().equals(args.get(i).getKind()))){
                                                    if(!(params.get(i-1).getType().getName().equals("TypeInt") && args.get(i).getKind().equals("IntegerLiteral")))
                                                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : the declared method's signature doesn't match the one being called. The param types don't match the ones being passed as argument."));
                                                }
                                            }
                                        }
                                    }
                                    else if(symbolTable.getSuper().isEmpty()){
                                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"'"+methodCallName+ "' : there is no such method with that signature."));
                                    }
                                }
                            }
                        }
                    }
                    
                    
                    break;
                default:
                    break;
            }
        }
    }

    private Type resolveType(JmmNode child, String methodName) {
        switch(child.getKind()){
            case "Boolean":
                return new Type("TypeBoolean", false);
            case "String":
                return new Type("TypeString", false);
            case "IntegerLiteral":
                return new Type("TypeInt", false);
            case "Id":
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
                        if(!(indexAccessNode.getKind().equals("IntegerLiteral") || indexAccessNode.getKind().equals("Id")) ){
                            reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+s.getName()+"': "+"array access must be done using a integer type expression."));
                            return null;
                        }
                        else if(indexAccessNode.getKind().equals("Id")){
                            String indexAccessVarName=indexAccessNode.get("name");
                            Symbol indexAccessSymbol=getDeclaredSymbol(indexAccessVarName, methodName);
                            if(indexAccessSymbol==null){
                                reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"The variable with name '"+indexAccessVarName+"' is used without being declared"));
                                return null;
                            }
                            else{
                                String indexVarType=indexAccessSymbol.getType().getName();
                                if(!indexVarType.equals("TypeInt")){
                                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+indexAccessVarName+"': "+"array access must be done using a integer type expression. Instead got var '"+indexAccessVarName+"' with type "+indexVarType));
                                    return null;
                                };
                            }
                        }
                    }
                    return s.getType();
                }
                else{
                    if(child.getChildren().size()>0 && child.getJmmChild(0).getKind().equals("AccessToArray")){
                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+s.getName()+"': "+"is not an array"));
                    }
                    return s.getType();
                }
            case "SUM":
                return new Type("TypeInt", false);
            case "MUL":
                return new Type("TypeInt", false);
            case "DotExpression":
                String dotExpressionMethodName=child.getJmmChild(1).getJmmChild(0).get("name");
                if(symbolTable.methodExists(dotExpressionMethodName)){
                    return symbolTable.getReturnType(dotExpressionMethodName);
                }
                return null;
            case "ANDD":
                return new Type("TypeBoolean", false);
            case "Negation":
                return new Type("TypeBoolean", false);
            case "LESSTHAN":
                return new Type("TypeBoolean", false);
        }
        return null;
    }

    private void checkValidEquality(JmmNode child, String methodName) {
        
        JmmNode firstChild=child.getJmmChild(0);
        JmmNode secondChild=child.getJmmChild(1);
        Symbol firstChildSymbol=getDeclaredSymbol(firstChild.get("name"), methodName);
        Boolean firstIsAccessed=false;
        if(firstChild.getChildren().size()>0){
            String arrayName=firstChild.get("name");
            Symbol arraySymbol=getDeclaredSymbol(arrayName, methodName);
            if(arraySymbol==null){
                reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"The variable with name '"+arrayName+"' is used without being declared"));
            }

            if(firstChild.getJmmChild(0).getJmmChild(0).getKind().equals("Id")){
                String indexVarName=firstChild.getJmmChild(0).getJmmChild(0).get("name");
                Symbol indexVarSymbol=getDeclaredSymbol(indexVarName, methodName);
                String indexVarType=null;
                if(indexVarSymbol==null){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"The variable with name '"+indexVarName+"' is used without being declared"));
                }
                else{
                    indexVarType=getDeclaredSymbol(indexVarName, methodName).getType().getName();
                    if(!indexVarType.equals("TypeInt")){
                        reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+arrayName+"': "+"array access must be done using a integer type expression. Instead got var '"+indexVarName+"' with type "+indexVarType));
                    };
                }
                
            }
            else if(!firstChild.getJmmChild(0).getJmmChild(0).getKind().equals("IntegerLiteral")){
                reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+arrayName+"': "+"array access must be done using a integer type expression. Instead got type "+child.getJmmChild(1).getJmmChild(0).getKind()));
            }
            firstIsAccessed=true;
        }
        if(firstIsAccessed && !firstChildSymbol.getType().isArray()){
            reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+firstChildSymbol.getName()+"': "+"is not an array"));
        }
        switch(secondChild.getKind()){
            case "SUM":
                if(!firstChildSymbol.getType().getName().equals("TypeInt")){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+firstChildSymbol.getName()+"': "+"The result of a sum can't be assigned to a var of type "+firstChildSymbol.getType().getName()));
                }
                else if(!firstIsAccessed && firstChildSymbol.getType().isArray()){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+firstChildSymbol.getName()+"': "+"Array isn't being accessed properly"));
                }
                break;
            case "IntegerLiteral":
                if(!firstChildSymbol.getType().getName().equals("TypeInt")){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+firstChildSymbol.getName()+"': "+"Can't assign an Integer Literal to a var of type "+firstChildSymbol.getType().getName()));
                }
                else if(!firstIsAccessed && firstChildSymbol.getType().isArray()){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+firstChildSymbol.getName()+"': "+"Array isn't being accessed properly"));
                }
                break;
            case "Boolean":
                if(!firstChildSymbol.getType().getName().equals("TypeBoolean")){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+firstChildSymbol.getName()+"': "+"Can't assign an Boolean to a var of type "+firstChildSymbol.getType().getName()));
                }
                else if(!firstIsAccessed && firstChildSymbol.getType().isArray()){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+firstChildSymbol.getName()+"': "+"Array isn't being accessed properly"));
                }
                break;
            case "String":
                if(!firstChildSymbol.getType().getName().equals("TypeString")){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+firstChildSymbol.getName()+"': "+"Can't assign a String to a var of type "+firstChildSymbol.getType().getName()));
                }
                else if(!firstIsAccessed && firstChildSymbol.getType().isArray()){
                    reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Variable '"+firstChildSymbol.getName()+"': "+"Array isn't being accessed properly"));
                }
                break;
            case "Id":
                Type firstChildType = resolveType(firstChild, methodName);
                Type secondChildType=resolveType(secondChild, methodName);
                if(!firstChildType.getName().equals(secondChildType.getName())){
                    if(!(symbolTable.getImports().contains(firstChildType.getName()) &&symbolTable.getImports().contains(secondChildType.getName()))){
                        if(!symbolTable.getClass().getName().contains(firstChildType.getName()) && ! symbolTable.getSuper().contains(secondChildType.getName())){
                            reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(child.get("line")),Integer.parseInt(child.get("col")),"Can't assign variable of type '" + secondChildType.getName()+ "' to variable of type '"+ firstChildType.getName() + "'"));
                        }
                    }
                }
                break;
            default:
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
