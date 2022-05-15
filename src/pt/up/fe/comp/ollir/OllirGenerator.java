package pt.up.fe.comp.ollir;

import java.util.stream.Collectors;

import jasmin.sym;
import java_cup.runtime.symbol;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Method;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.exceptions.NotImplementedException; 

public class OllirGenerator extends AJmmVisitor<Integer, Integer> {
    private final StringBuilder code;
    private final SymbolTable symbolTable;

    public String curMethRetType;
    public String methodSignature;
    public Integer varcount = 0;
    public Integer tempCount = 0;
    public Integer loopCount = 0;
    public Boolean visitingIf = false;

    public Symbol twoWaySymbol1 = null;
    public Method twoWayMethod1 = null;
    public Symbol twoWaySymbol2 = null;
    public Method twoWayMethod2 = null;

    public String returnType = "";

    public OllirGenerator(SymbolTable symbolTable){
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;

        addVisit("Start", this::programVisit); 
        addVisit("ClassDeclaration", this::classDeclVisit);
        addVisit("MainMethod", this::methodDeclVisit);
        addVisit("NormalMethod", this::methodDeclVisit);
        addVisit("MethodBody", this::methodBodyVisit);
        addVisit("Equality", this::stmtVisit);
        addVisit("DotExpression", this::stmtVisit);
        // addVisit("IfStatement", this::stmtVisit);
        // addVisit("WhileStatement", this::stmtVisit);
        addVisit("ReturnRule", this::returnVisit); 
        /*
        addVisit("SUM", this::twoWayVisit);
        addVisit("SUB", this::twoWayVisit);
        addVisit("MUL", this::twoWayVisit);
        addVisit("DIV", this::twoWayVisit);
        addVisit("NewObject", this::newVisit);
        */
    }

    public String getCode(){
        return code.toString();
    }

    private Integer programVisit(JmmNode program, Integer dummy){
        for (var importString : symbolTable.getImports()){
            code.append("import ").append(importString).append(";\n");
        }
        code.append("\n");

        for (var child : program.getChildren()){
            visit(child);
        } 
        return 0;
    }

    private Integer classDeclVisit(JmmNode classDecl, Integer dummy){
        code.append("public ").append(symbolTable.getClassName());
        var superClass = symbolTable.getSuper();
        if (!superClass.isEmpty()) code.append(" extends ").append(superClass);
        code.append("{\n");

        for (var child : classDecl.getChildren()){
            visit(child);
        }

        code.append("}\n");
        return 0;
    }


    private Integer methodDeclVisit(JmmNode methodDecl, Integer dummy){
        methodSignature = methodDecl.get("name");
        var isStatic = Boolean.valueOf(methodDecl.get("isStatic"));

        code.append(".method public ");
        if(isStatic){
            code.append("static ");
        }

        code.append(methodSignature + "(");

        var params = symbolTable.getParameters(methodSignature);

        var paramCode = params.stream()
        .map(symbol -> OllirUtils.getCode(symbol))
        .collect(Collectors.joining(", "));
        
        code.append(paramCode);

        code.append(").");

        curMethRetType = OllirUtils.getCode(symbolTable.getReturnType(methodSignature));

        code.append(OllirUtils.getCode(symbolTable.getReturnType(methodSignature)));

        code.append(" {\n");

        for (var child : methodDecl.getChildren()){
            if(child.getKind().equals("MethodBody") || child.getKind().equals("ReturnRule")){
                visit(child);
            }
            
        }

        if(methodSignature.equals("main")) code.append("ret.V;\n");
        code.append("}\n\n");

        return 0;
    }


    private Integer methodBodyVisit(JmmNode methodBody, Integer dummy){

        var stmts = methodBody.getChildren();
        for(var stmt: stmts){
            visit(stmt);
        }

        return 0;
    }

    private String getCode(JmmNode node){
        StringBuilder code = new StringBuilder();
        if(node.getKind().equals("Id"))
            code.append(node.get("name"));
        else if(node.getKind().equals("IntegerLiteral"))
            code.append(node.get("value"));
        else
            code.append(node.getKind());
        code.append(".").append(getType(node));

        return code.toString();
    }

    private String getType(JmmNode node){
        if(node.getKind().equals("IntegerLiteral"))
            return "i32";
        if(node.getKind().equals("This"))
            return "this";
        if(node.getKind().equals("Negation"))
            return getType(node.getJmmChild(0));
        if(node.getKind().equals("DotExpression"))
            return getType(node.getJmmChild(1));
        if(node.getKind().equals("Identifier"))
            return symbolTable.getReturnType(methodSignature).getName();
        if(node.getKind().equals("Id") )
            return OllirUtils.getOllirType(symbolTable.getVariableType(methodSignature, node.get("name")));
        return OllirUtils.getOllirType(symbolTable.getVariableType(methodSignature, node.get("name")));
    }

    private Integer returnVisit(JmmNode returnStmt, Integer dummy){
        String type = "";
        if(isBinOp(returnStmt.getJmmChild(0))){
            int i = 1;
            if(returnStmt.getJmmChild(0).getJmmChild(0).getKind().equals("Id"))
                i = 0;

            type = OllirUtils.getOllirType(getType(returnStmt.getJmmChild(0).getJmmChild(i)));

            code.append("t" + tempCount +".").append(type)
            .append(" :=.").append(type).append(" ");
            binOpVisit(returnStmt.getJmmChild(0), 0);
            code.append(";\n");
        }
        code.append("ret.").append(OllirUtils.getOllirType(symbolTable.getReturnType(methodSignature).getName()))
            .append(" ");
        if(isBinOp(returnStmt.getJmmChild(0))){
            code.append("t" + tempCount +"." + type);
        }
        else
            expressionVisit(returnStmt.getJmmChild(0), dummy);
        code.append(";\n");

        return 0;
    }

    private void idVisit(JmmNode id){
        String name = id.get("name");
        Symbol var = symbolTable.getLocalVariable(methodSignature, name);
        if(var == null){
            var = symbolTable.getParam(methodSignature, name);
        }
        if(var == null){
            var = symbolTable.getField(methodSignature, name);
        }
        String type = OllirUtils.getCode(var.getType());
        code.append(name).append(".");
        code.append(type);
    }

    private void assignStmtAux(JmmNode exp){
        boolean flag = false;
        if(exp.getKind().equals("DotExpression"))
            flag = true;
        for(int i = 0; i < exp.getNumChildren(); i++){
            var e = exp.getJmmChild(i);
            System.out.println("child: " + e.getKind());
            if(isBinOp(e) || e.getKind().equals("DotExpression")){
                assignStmtAux(e);
                flag = true;
            }
        }

        if(flag){
            code.append("t" + tempCount + "." + OllirUtils.getOllirType(returnType))
            .append(" :=." +  OllirUtils.getOllirType(returnType) + " ");
            expressionVisit(exp, 0);
        }
        // else if(isBinOp(exp)){
        //     code.append("t" + tempCount + "." + OllirUtils.getOllirType(returnType))
        //     .append(" :=." + OllirUtils.getOllirType(returnType) + " ")
        //     .append("t" +(tempCount-1) + "." + OllirUtils.getOllirType(returnType)+" ")
        //     .append(OllirUtils.getOllirType(exp.getKind()) + ".").append(OllirUtils.getOllirType(returnType)+" ");
        //     if(exp.getJmmChild(1).getKind().equals("DotExpression"))
        //         code.append("t"+tempCount+"."+OllirUtils.getOllirType(returnType) +" ");
        //     else
        //         code.append(expressionVisit(exp.getJmmChild(1), 0));
        // }
        else {
            code.append("t" + tempCount + "." + OllirUtils.getOllirType(returnType))
            .append(" :=." + OllirUtils.getOllirType(returnType) + " ")
            .append(expressionVisit(exp, 0));

        }
        tempCount++;
        code.append(";\n");

        
    }

    private void assignStmtVisit(JmmNode assignStmt){

        returnType = OllirUtils.getOllirType(symbolTable.getVariableType(methodSignature, assignStmt.getJmmChild(0).get("name")));

        assignStmtAux(assignStmt.getJmmChild(1));

        idVisit(assignStmt.getJmmChild(0));

        code.append(" :=.").append(OllirUtils.getOllirType(returnType)).append(" ");

        code.append("t" + (tempCount-1) + ".").append(OllirUtils.getOllirType(returnType)).append(";\n");
        
    }


    private Integer dotExpInAssign(JmmNode dotExp, Integer counter){
        code.append("t").append(tempCount).append(".i32 :=.i32 ");
        tempCount++;
        memberCallVisit(dotExp);
        code.append(";\n");
        return counter+1;
    }

    Boolean isBinOp(JmmNode op){
        String kind = op.getKind();
        return kind.equals("ANDD") || kind.equals("SUM") || kind.equals("SUB") || kind.equals("LESSTHAN") || kind.equals("MUL") || kind.equals("DIV");
    }

    private Integer leftBinOpVisit(JmmNode binOp, Integer dummy){
        code.append(getCode(binOp.getJmmChild(1)) + " ")
            .append(OllirUtils.getOllirType(binOp.getKind())).append(".").append(getType(binOp.getJmmChild(1))+ " ");
        if(binOp.getJmmChild(1).getKind().equals("DotExpression") || isBinOp(binOp.getJmmChild(1)))
            code.append("t"+(tempCount-1)+"."+getType(binOp.getJmmChild(0)));
        else
        code.append(getCode(binOp.getJmmChild(1)));

        return 0;
    }


    private Integer rightBinOpVisit(JmmNode binOp, Integer dummy){
        code.append(getCode(binOp.getJmmChild(0)) + " ");
        code.append(OllirUtils.getOllirType(binOp.getKind())).append(".").append(getType(binOp.getJmmChild(0))+ " ");
        if(binOp.getJmmChild(1).getKind().equals("DotExpression") || isBinOp(binOp.getJmmChild(1)))
            code.append("t"+(tempCount-1)+"."+getType(binOp.getJmmChild(0)));
        else
            code.append(getCode(binOp.getJmmChild(1)));
        return 0;
    }

    private Integer binOpVisit(JmmNode binOp, Integer dummy){
        if(isBinOp(binOp.getJmmChild(0)))
            leftBinOpVisit(binOp, dummy);
        else if(isBinOp(binOp.getJmmChild(1)))
            rightBinOpVisit(binOp, dummy);
        else{
            code.append(getCode(binOp.getJmmChild(1)) + " ")
            .append(OllirUtils.getOllirType(binOp.getKind())).append(".").append(getType(binOp.getJmmChild(1))+ " ");
            if(binOp.getJmmChild(0).getKind().equals("DotExpression"))
                code.append("t"+(tempCount-1)+"."+getType(binOp.getJmmChild(1)));
            else
            code.append(getCode(binOp.getJmmChild(0)));
        }
        return 0;
    }

    // private Integer binOpVisit(JmmNode binOp, Integer counter){
    //     int i = 0;
    //     if(isBinOp(binOp.getJmmChild(0))){
    //         counter = binOpVisit(binOp.getJmmChild(0), counter+1);
    //         i = 1;
    //     }
    //     else if(isBinOp(binOp.getJmmChild(1))){
    //         counter = binOpVisit(binOp.getJmmChild(1), counter+1);
    //         i = 0;
    //     }
    //     if(!isBinOp(binOp.getJmmChild(0)) && !isBinOp(binOp.getJmmChild(1))){
    //         if(counter != 0){
    //             //code.append("t").append(tempCount).append(".i32 :=.i32 ");
    //             tempCount++;
    //             expressionVisit(binOp.getJmmChild(0), 0);
    //             code.append(" ").append(OllirUtils.getOllirType(binOp.getKind()))
    //                 .append(".").append(getType(binOp.getJmmChild(0))).append(" ");
    //             if(binOp.getJmmChild(1).getKind().equals("DotExpression"))
    //                 code.append("t"+(tempCount-1) + "." + getType(binOp.getJmmChild(0)));
    //             else
    //                 expressionVisit(binOp.getJmmChild(1), 0);

    //             code.append(";\n");
    //         }
    //         else{
    //             expressionVisit(binOp.getJmmChild(0), 0);
    //             code.append(" ").append(OllirUtils.getOllirType(binOp.getKind()))
    //                 .append(".").append(getType(binOp.getJmmChild(0))).append(" ");
    //             if(binOp.getJmmChild(1).getKind().equals("DotExpression"))
    //                 code.append("t"+(tempCount-1) + "." + getType(binOp.getJmmChild(0)));
    //             else
    //                 expressionVisit(binOp.getJmmChild(1), 0);
    //         }
    //         return counter +1;
    //     }

    //     // code.append("t").append(tempCount).append(".i32 :=.i32 ");
    //     // code.append("t").append(tempCount-1).append(".i32 ");
    //     // code.append(" ").append(OllirUtils.getOllirType(binOp.getKind()))
    //     //     .append(".").append(getType(binOp.getJmmChild(i))).append(" ");
        
        
    //     // expressionVisit(binOp.getJmmChild(i), 0);
    //     //code.append(";\n");
    //     tempCount++;
    //     return counter;
    // }

    private void binOpNoAssign(JmmNode binOp){
        code.append("t").append(tempCount).append(".i32 :=.i32 ");
        tempCount++;
        expressionVisit(binOp.getJmmChild(0), 0);
        code.append(" ").append(OllirUtils.getOllirType(binOp.getKind()))
            .append(".").append(getType(binOp.getJmmChild(0))).append(" ");
        expressionVisit(binOp.getJmmChild(1), 0);
        code.append(";\n");
    }

    private void invertOp(JmmNode binOp){
        String kind = binOp.getKind();
        switch (kind){
            case "LESSTHAN" : code.append(">="); break;
            default:
                throw new NotImplementedException("OLLIR: Operation kind not implemented: " + binOp.getKind());
        }
    }

    private void ifVisit(JmmNode ifStmt){
        int elseCount = 0;

        Boolean oneBool = false;
        if (ifStmt.getJmmChild(0).getNumChildren() < 2){
            oneBool = true;
        }
        else {
            if (isBinOp(ifStmt.getJmmChild(0).getJmmChild(0))){
                binOpNoAssign(ifStmt.getJmmChild(0).getJmmChild(0));
            }  else if(ifStmt.getJmmChild(0).getJmmChild(0).getKind().equals("DotExpression")){
                int t = dotExpInAssign(ifStmt.getJmmChild(0).getJmmChild(0), 0);
            }
            if (isBinOp(ifStmt.getJmmChild(0).getJmmChild(1))){
                binOpNoAssign(ifStmt.getJmmChild(0).getJmmChild(1));
            } else if(ifStmt.getJmmChild(0).getJmmChild(1).getKind().equals("DotExpression")){
                int t = dotExpInAssign(ifStmt.getJmmChild(0).getJmmChild(1), 0);
            }
        }
        code.append("if (");
        if (oneBool) code.append(ifStmt.getJmmChild(0).get("value"));
        else{
            visitingIf = true;
            if(isBinOp(ifStmt.getJmmChild(0).getJmmChild(0))){
                code.append("t").append(tempCount-1).append(".i32 ");
            } else if(ifStmt.getJmmChild(0).getJmmChild(0).getKind().equals("IntegerLiteral")){
                code.append(ifStmt.getJmmChild(0).getJmmChild(0).get("value")).append(".i32 ");
            } else code.append(ifStmt.getJmmChild(0).getJmmChild(0).get("name")).append(".i32 ");
            invertOp(ifStmt.getJmmChild(0));
            code.append(".bool ");
            if(isBinOp(ifStmt.getJmmChild(0).getJmmChild(1))){
                code.append("t").append(tempCount).append(".i32 ");
            } else if(ifStmt.getJmmChild(0).getJmmChild(1).getKind().equals("IntegerLiteral")){
                code.append(ifStmt.getJmmChild(0).getJmmChild(1).get("value")).append(".i32 ");
            } else code.append(ifStmt.getJmmChild(0).getJmmChild(1).get("name")).append(".i32");
            visitingIf = false;
        }
        code.append(") goto else;\n");
        int numChildren = ifStmt.getNumChildren();
        for (int i = 1; i < numChildren; i++){
            if(ifStmt.getJmmChild(i).getKind().equals("StatementBlock")){
                ++elseCount;
                if (elseCount == 2){
                    if(i == numChildren - 1) code.append("endIf:\n");
                    else code.append("endElse:\n");
                    elseCount = 0;
                } else {
                    code.append("else:\n");
                }
            }
            visit(ifStmt.getJmmChild(i));
        }
    }

    private void whileVisit(JmmNode whileStmt){
        ++loopCount;
        code.append("Loop").append(loopCount).append(":\n");
        Boolean oneBool = false;
        if (whileStmt.getJmmChild(0).getNumChildren() < 2){
            oneBool = true;
        }
        else {
            if (isBinOp(whileStmt.getJmmChild(0).getJmmChild(0))){
                binOpNoAssign(whileStmt.getJmmChild(0).getJmmChild(0));
            } else if(whileStmt.getJmmChild(0).getJmmChild(0).getKind().equals("DotExpression")){
                int t = dotExpInAssign(whileStmt.getJmmChild(0).getJmmChild(0), 0);
            }
            if (isBinOp(whileStmt.getJmmChild(0).getJmmChild(1))){
                binOpNoAssign(whileStmt.getJmmChild(0).getJmmChild(1));
            } else if(whileStmt.getJmmChild(0).getJmmChild(1).getKind().equals("DotExpression")){
                int t = dotExpInAssign(whileStmt.getJmmChild(0).getJmmChild(1), 0);
            }
        }

        code.append("if(");
        if (oneBool) code.append(whileStmt.getJmmChild(0).get("value"));
        else{
            visitingIf = true;
            if(isBinOp(whileStmt.getJmmChild(0).getJmmChild(0)) || whileStmt.getJmmChild(0).getJmmChild(0).getKind().equals("DotExpression")){
                code.append("t").append(tempCount-1).append(".i32 ");
            } else if(whileStmt.getJmmChild(0).getJmmChild(0).getKind().equals("IntegerLiteral")){
                code.append(whileStmt.getJmmChild(0).getJmmChild(0).get("value")).append(".i32 ");
            } else code.append(whileStmt.getJmmChild(0).getJmmChild(0).get("name")).append(".i32 ");
            invertOp(whileStmt.getJmmChild(0));
            code.append(".bool ");
            if(isBinOp(whileStmt.getJmmChild(0).getJmmChild(1)) || whileStmt.getJmmChild(0).getJmmChild(1).getKind().equals("DotExpression")){
                code.append("t").append(tempCount).append(".i32");
            } else if(whileStmt.getJmmChild(0).getJmmChild(1).getKind().equals("IntegerLiteral")){
                code.append(whileStmt.getJmmChild(0).getJmmChild(1).get("value")).append(".i32 ");
            } else code.append(whileStmt.getJmmChild(0).getJmmChild(1).get("name")).append(".i32");
            visitingIf = false;
        }

        code.append(") goto end;\n");
        int whileChildren = whileStmt.getNumChildren();
        for (int i = 1; i < whileChildren; i++){
            visit(whileStmt.getJmmChild(i));
        }
        code.append("goto Loop").append(loopCount).append(";\n");
        code.append("end:\n");
    }

    private Integer expressionVisit(JmmNode expression, Integer dummy){
        String kind = expression.getKind();

        if(isBinOp(expression)){
            binOpVisit(expression, 0);
            return 0;
        }
        int trash;
        switch(kind){
            case "Id": idVisit(expression); break;
            case "Identifier": idVisit(expression.getJmmChild(0));
            case "DotExpression": memberCallVisit(expression); break;
            case "Boolean": code.append(expression.get("value")).append(".bool"); break;
            case "Negation": break;
            case "IntegerLiteral": code.append(expression.get("value")).append(".").append(OllirUtils.getOllirType("TypeInt")); break;
            case "InitializeArray": code.append("new(array, ");
                                    expressionVisit(expression.getJmmChild(0),0);
                                    code.append(").array.i32"); break;
            case "NewObject": code.append("new(").append(expression.getJmmChild(0).get("name"))
                                .append(").")
                                .append(OllirUtils.getOllirType(expression.getJmmChild(0).get("name"))); break;
            case "AccessToArray":
                if (expression.getJmmChild(0).getKind().equals("DotExpression")) trash = dotExpInAssign(expression.getJmmChild(0), 0);
                else expressionVisit(expression.getJmmChild(0), 0); break;
            default: 
            throw new NotImplementedException("OLLIR: Expression kind not implemented: " + expression.getKind());
        }
        return 0;
    }


    private Integer stmtVisit(JmmNode stmt, Integer dummy){
        String stmtType = stmt.getKind().toString();
        switch(stmtType){
            // case "StatementBlock": break;
            // case "IfStatement": ifVisit(stmt); break;
            // case "WhileStatement": whileVisit(stmt); break;
            case "Equality": assignStmtVisit(stmt); break; //Assignment
            case "DotExpression":
                expressionVisit(stmt, dummy);
                if (!visitingIf) code.append(";\n");
                break; 
            default: 
            throw new NotImplementedException("OLLIR: Statement kind not implemented: " + stmt.getKind());
        }
        return 0;
    }


    private void memberCallVisit(JmmNode memberCall){
        //type missing
        var childType = memberCall.getJmmChild(1).getJmmChild(0).getKind();
        if(childType.equals("Length"))
        {
            code.append("arraylength(").append(getCode(memberCall.getJmmChild(0)))
                .append(").i32");
            return;
        }
        visit(memberCall.getJmmChild(0));
        
        if(childType.equals("main")){
            code.append("invokestatic(");
        }
        else{
            code.append("invokevirtual(");
        }
        code.append(memberCall.getJmmChild(0).get("name"));
        if(!memberCall.getJmmChild(0).get("name").equals("this")){
            if(!symbolTable.getImports().contains(memberCall.getJmmChild(0).get("name"))){
                code.append(".").append(getType(memberCall.getJmmChild(0)));
            }
            else
                code.append(".").append(memberCall.getJmmChild(0).get("name"));
        }
        code.append(", \"").append(memberCall.getJmmChild(1).getJmmChild(0).get("name"))
            .append("\"");

        
        if(memberCall.getJmmChild(1).getNumChildren()>1){ //Has params
            var children = memberCall.getJmmChild(1).getChildren();
            for( var child : children.subList(1, children.size()-1)){
                code.append(", ").append(getCode(child));
            }
        }

        code.append(").").append(returnType);

    }


    // private Integer twoWayVisit(JmmNode sumStmt, Integer dummy){
    //     var sumChild1 = sumStmt.getJmmChild(0);
    //     var sumChild2 = sumStmt.getJmmChild(1);
        
    //     if(sumStmt.getJmmChild(0).getKind().equals("DotExpression")){
    //         for (var s : symbolTable.getMethodList()){
    //             if (s.getMethodSignature().equals(sumStmt.getJmmChild(0).getJmmChild(1).getJmmChild(0).get("name"))){
    //                 twoWayMethod1 = s;
    //                 break;
    //             }
    //         }
    //     } else{
    //         twoWaySymbol1 = symbolTable.getLocalVariable(methodSignature, sumStmt.getJmmChild(0).get("name"));
    //         if(twoWaySymbol1 == null){
    //             twoWaySymbol1 = symbolTable.getParam(methodSignature, sumStmt.getJmmChild(0).get("name"));
    //         }
    //         if(twoWaySymbol1 == null){
    //             twoWaySymbol1 = symbolTable.getField(methodSignature, sumStmt.getJmmChild(0).get("name"));
    //         }
    //     }

    //     if(sumStmt.getJmmChild(1).getKind().equals("DotExpression")){
    //         for (var s : symbolTable.getMethodList()){
    //             if (s.getMethodSignature().equals(sumStmt.getJmmChild(1).getJmmChild(1).getJmmChild(0).get("name"))){
    //                 twoWayMethod2 = s;
    //                 break;
    //             }
    //         }
    //     } else{
    //         twoWaySymbol2 = symbolTable.getLocalVariable(methodSignature, sumStmt.getJmmChild(1).get("name"));
    //         if(twoWaySymbol2 == null){
    //             twoWaySymbol2 = symbolTable.getParam(methodSignature, sumStmt.getJmmChild(1).get("name"));
    //         }
    //         if(twoWaySymbol2 == null){
    //             twoWaySymbol2 = symbolTable.getField(methodSignature, sumStmt.getJmmChild(1).get("name"));
    //         }
    //     }

    //     code.append("t1.");
    //     if(twoWaySymbol1 != null) code.append(OllirUtils.getOllirType(twoWaySymbol1.getType().getName())).append(" ");
    //     else code.append(OllirUtils.getOllirType(twoWayMethod1.getType().getName())).append(" ");
    //     code.append(":=.");
    //     if(twoWaySymbol1 != null) code.append(OllirUtils.getOllirType(twoWaySymbol1.getType().getName())).append(" ");
    //     else code.append(OllirUtils.getOllirType(twoWayMethod1.getType().getName())).append(" ");
    //     if (sumChild1.getKind().equals("Id")){
    //         code.append(sumChild1.get("name")).append(".");
    //     } else if (sumChild1.getKind().equals("IntegerLiteral")){
    //         code.append(sumChild1.get("value")).append(".");
    //     } else visit(sumChild1);
    //     if(twoWaySymbol1 != null) code.append(OllirUtils.getOllirType(twoWaySymbol1.getType().getName())).append("\n");
    //     else code.append(OllirUtils.getOllirType(twoWayMethod1.getType().getName())).append("\n");        

    //     code.append("t2.");
    //     if(twoWaySymbol2 != null) code.append(OllirUtils.getOllirType(twoWaySymbol2.getType().getName())).append(" ");
    //     else code.append(OllirUtils.getOllirType(twoWayMethod2.getType().getName())).append(" ");
    //     code.append(":=.");
    //     if(twoWaySymbol2 != null) code.append(OllirUtils.getOllirType(twoWaySymbol2.getType().getName())).append(" ");
    //     else code.append(OllirUtils.getOllirType(twoWayMethod2.getType().getName())).append(" ");
    //     if (sumChild2.getKind().equals("Id")){
    //         code.append(sumChild2.get("name")).append(".");
    //     } else if (sumChild2.getKind().equals("IntegerLiteral")){
    //         code.append(sumChild2.get("value")).append(".");
    //     } else visit(sumChild2);
    //     if(twoWaySymbol2 != null) code.append(OllirUtils.getOllirType(twoWaySymbol2.getType().getName())).append("\n");
    //     //else code.append(".").append(OllirUtils.getOllirType(twoWayMethod2.getType().getName())).append("\n"); 

    //     return 0;
    // }

    // private Integer varVisit(JmmNode var, Integer dummy){

    //     code.append(var.get("name")).append(".");

    //     Symbol symbol = symbolTable.getLocalVariable(methodSignature, var.get("name"));
    //     if(symbol == null){
    //         symbol = symbolTable.getParam(methodSignature, var.get("name"));
    //     }
    //     if(symbol == null){
    //         symbol = symbolTable.getField(methodSignature, var.get("name"));
    //     }
    //     String type =OllirUtils.getOllirType(symbol.getType().getName());
    //     code.append(type);
    //     return 0;
    // }


    // //this one's done, I think
    // private Integer assignVisit(JmmNode assignStmt, Integer dummy){
    //     Symbol symbol = symbolTable.getLocalVariable(methodSignature, assignStmt.getJmmChild(0).get("name"));
    //     if(symbol == null){
    //         symbol = symbolTable.getParam(methodSignature, assignStmt.getJmmChild(0).get("name"));
    //     }
    //     if(symbol == null){
    //         symbol = symbolTable.getField(methodSignature, assignStmt.getJmmChild(0).get("name"));
    //     }
    //     code.append(OllirUtils.getCode(symbol));
    
    //     code.append(" :=.");
        
    //     code.append(OllirUtils.getOllirType(symbol.getType().getName())).append(" ");

    //     if(assignStmt.getJmmChild(1).getKind().equals("Id")){
    //         Symbol symbol2 = symbolTable.getLocalVariable(methodSignature, assignStmt.getJmmChild(1).get("name"));
    //         if(symbol2 == null){
    //             symbol2 = symbolTable.getParam(methodSignature, assignStmt.getJmmChild(1).get("name"));
    //         }
    //         if(symbol2 == null){
    //             symbol2 = symbolTable.getField(methodSignature, assignStmt.getJmmChild(1).get("name"));
    //         }
    //         code.append(OllirUtils.getCode(symbol2));
    //     }
    //     else if (assignStmt.getJmmChild(1).getKind().equals("IntegerLiteral")){
    //         code.append(assignStmt.getJmmChild(1).get("value")).append(".");
    //         code.append(OllirUtils.getOllirType("TypeInt"));

    //     }
    //     else if (assignStmt.getJmmChild(1).getKind().equals("InitializeArray")){
    //         code.append(assignStmt.getJmmChild(1).getJmmChild(0).getJmmChild(0).get("value")).append(".");
    //         code.append(OllirUtils.getOllirType("TypeIntArray"));

    //     }
    //     else if (assignStmt.getJmmChild(1).getKind().equals("AccessToArray")){
    //         if(assignStmt.getJmmChild(2).getKind().equals("IntegerLiteral")){
    //             code.append(assignStmt.getJmmChild(2).get("value")).append(".");
    //             code.append(OllirUtils.getOllirType("TypeInt"));
    //         }

    //     } else if (assignStmt.getJmmChild(1).getKind().equals("SUM")){
    //         var symbolCode = OllirUtils.getCode(symbol);
    //         var typeCode = OllirUtils.getOllirType(symbol.getType().getName());
    //         code.delete(code.length()-symbolCode.length()-typeCode.length()-5, code.length()-1);
    //         visit(assignStmt.getJmmChild(1));

    //         //TODO: meter os tipos do t1 e t2 na soma
    //         code.append(OllirUtils.getCode(symbol));
    
    //         code.append(" :=.");
        
    //         code.append(OllirUtils.getOllirType(symbol.getType().getName())).append(" ");

    //         code.append("t1.");
    //         if(twoWaySymbol1 != null) code.append(OllirUtils.getOllirType(twoWaySymbol1.getType().getName())).append(" ");
    //         else code.append(OllirUtils.getOllirType(twoWayMethod1.getType().getName())).append(" ");
    //         code.append("+ .");
    //         if(twoWaySymbol2 != null) code.append(OllirUtils.getOllirType(twoWaySymbol2.getType().getName())).append(" ");
    //         else code.append(OllirUtils.getOllirType(twoWayMethod2.getType().getName())).append(" ");
    //         code.append("t2.");
    //         if(twoWaySymbol2 != null) code.append(OllirUtils.getOllirType(twoWaySymbol2.getType().getName())).append(" ");
    //         else code.append(OllirUtils.getOllirType(twoWayMethod2.getType().getName())).append(" ");

    //     }  else if (assignStmt.getJmmChild(1).getKind().equals("MUL")){
    //         var symbolCode = OllirUtils.getCode(symbol);
    //         var typeCode = OllirUtils.getOllirType(symbol.getType().getName());
    //         code.delete(code.length()-symbolCode.length()-typeCode.length()-5, code.length()-1);

    //         visit(assignStmt.getJmmChild(1));

    //         //TODO: meter os tipos do t1 e t2 na soma
    //         code.append(OllirUtils.getCode(symbol));
    
    //         code.append(" :=.");
        
    //         code.append(OllirUtils.getOllirType(symbol.getType().getName())).append(" ");
    //         code.append("t1.");
    //         if(twoWaySymbol1 != null) code.append(OllirUtils.getOllirType(twoWaySymbol1.getType().getName())).append(" ");
    //         else code.append(OllirUtils.getOllirType(twoWayMethod1.getType().getName())).append(" ");
    //         code.append("* .");
    //         if(twoWaySymbol2 != null) code.append(OllirUtils.getOllirType(twoWaySymbol2.getType().getName())).append(" ");
    //         else code.append(OllirUtils.getOllirType(twoWayMethod2.getType().getName())).append(" ");
    //         code.append("t2.");
    //         if(twoWaySymbol2 != null) code.append(OllirUtils.getOllirType(twoWaySymbol2.getType().getName())).append(" ");
    //         else code.append(OllirUtils.getOllirType(twoWayMethod2.getType().getName())).append(" ");

    //     } else if (assignStmt.getJmmChild(1).getKind().equals("SUB")){
    //         var symbolCode = OllirUtils.getCode(symbol);
    //         var typeCode = OllirUtils.getOllirType(symbol.getType().getName());
    //         code.delete(code.length()-symbolCode.length()-typeCode.length()-5, code.length()-1);

    //         visit(assignStmt.getJmmChild(1));

    //         //TODO: meter os tipos do t1 e t2 na soma
    //         code.append(OllirUtils.getCode(symbol));
    
    //         code.append(" :=.");
        
    //         code.append(OllirUtils.getOllirType(symbol.getType().getName())).append(" ");
    //         code.append("t1.");
    //         if(twoWaySymbol1 != null) code.append(OllirUtils.getOllirType(twoWaySymbol1.getType().getName())).append(" ");
    //         else code.append(OllirUtils.getOllirType(twoWayMethod1.getType().getName())).append(" ");
    //         code.append("- .");
    //         if(twoWaySymbol2 != null) code.append(OllirUtils.getOllirType(twoWaySymbol2.getType().getName())).append(" ");
    //         else code.append(OllirUtils.getOllirType(twoWayMethod2.getType().getName())).append(" ");
    //         code.append("t2.");
    //         if(twoWaySymbol2 != null) code.append(OllirUtils.getOllirType(twoWaySymbol2.getType().getName())).append(" ");
    //         else code.append(OllirUtils.getOllirType(twoWayMethod2.getType().getName())).append(" ");
    //     } else if (assignStmt.getJmmChild(1).getKind().equals("DIV")){
    //         var symbolCode = OllirUtils.getCode(symbol);
    //         var typeCode = OllirUtils.getOllirType(symbol.getType().getName());
    //         code.delete(code.length()-symbolCode.length()-typeCode.length()-5, code.length()-1);

    //         visit(assignStmt.getJmmChild(1));

    //         //TODO: meter os tipos do t1 e t2 na soma
    //         code.append(OllirUtils.getCode(symbol));
    
    //         code.append(" :=.");
        
    //         code.append(OllirUtils.getOllirType(symbol.getType().getName())).append(" ");
    //         code.append("t1.");
    //         if(twoWaySymbol1 != null) code.append(OllirUtils.getOllirType(twoWaySymbol1.getType().getName())).append(" ");
    //         else code.append(OllirUtils.getOllirType(twoWayMethod1.getType().getName())).append(" ");
    //         code.append("/ .");
    //         if(twoWaySymbol2 != null) code.append(OllirUtils.getOllirType(twoWaySymbol2.getType().getName())).append(" ");
    //         else code.append(OllirUtils.getOllirType(twoWayMethod2.getType().getName())).append(" ");
    //         code.append("t2.");
    //         if(twoWaySymbol2 != null) code.append(OllirUtils.getOllirType(twoWaySymbol2.getType().getName())).append(" ");
    //         else code.append(OllirUtils.getOllirType(twoWayMethod2.getType().getName())).append(" ");
    //     }

    //     code.append(";\n");

    //     return 0;

    // }

    // //also done, iirc
    // private Integer newVisit(JmmNode newStmt, Integer dummy){
    //     code.append(".").append(newStmt.getJmmChild(0).get("name")).append(" new(");
    //     code.append(newStmt.getJmmChild(0).get("name"));
    //     for (int i = 1; i < newStmt.getNumChildren(); i++){
    //         code.append(",");
    //         var curstmt = newStmt.getJmmChild(i);
    //         if (curstmt.getKind().equals("Id")){
    //             code.append(curstmt.get("name"));
    //         } else if (curstmt.getKind().equals("IntegerLiteral")){
    //             code.append(curstmt.get("value"));
    //         } else visit(curstmt);
    //     }
    //     code.append(")");
    //     code.append(".").append(newStmt.getJmmChild(0).get("name"));
    //     return 0;
    // }

    // //probs done
    // private Integer returnVisit(JmmNode returnStmt, Integer dummy){
    //     code.append("return stmt supposed here");
    //     //code.append("ret.").append(curMethRetType).append(" ").append(returnStmt.getJmmChild(0).get("name")).append(returnStmt.getJmmChild(0).get("type"));

    //     return 0;
    // }


    



}