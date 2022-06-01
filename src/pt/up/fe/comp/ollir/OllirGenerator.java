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
    public Integer tempCount = 1;
    public Integer loopCount = 0;
    public Integer ifCount = 0;
    public Boolean visitingIf = false;
    public Boolean boolInOp = false;

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
        addVisit("IfStatement", this::stmtVisit);
        addVisit("WhileStatement", this::stmtVisit);
        addVisit("ReturnRule", this::returnVisit);

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

    private void varVisit(JmmNode jmmNode) {
        code.append(".field private ");
        code.append(jmmNode.get("name"))
                .append(".")
                .append(OllirUtils.getOllirType(jmmNode.getJmmChild(0).getKind()))
                .append(";\n");
    }

    private Integer classDeclVisit(JmmNode classDecl, Integer dummy){
        code.append("public ").append(symbolTable.getClassName());
        var superClass = symbolTable.getSuper();
        if (!superClass.isEmpty()) code.append(" extends ").append(superClass);
        code.append("{\n");

        for (var child : classDecl.getChildren()){
            if(child.getKind().equals("Var"))
                varVisit(child);
        }
        code.append("\n");
        code.append(".construct ").append(symbolTable.getClassName()).append("().V{\ninvokespecial(this, \"<init>\").V;\n}\n");

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

    private String getBinOpType(JmmNode node){
        if (node.getKind().equals("SUM") || node.getKind().equals("SUB") || node.getKind().equals("MUL") || node.getKind().equals("DIV"))
            return "i32";
        else return "bool";
    }

    private String getType(JmmNode node){
        if(node.getKind().equals("IntegerLiteral"))
            return "i32";
        if(node.getKind().equals("This"))
            return "this";
        if(node.getKind().equals("Negation"))
            return "bool";
        if(node.getKind().equals("DotExpression"))
            return getType(node.getJmmChild(1));
        if(node.getKind().equals("Identifier"))
            return symbolTable.getReturnType(methodSignature).getName();
        if(node.getKind().equals("Id") )
            return OllirUtils.getOllirType(symbolTable.getVariableType(methodSignature, node.get("name")));
        if(isBinOp(node))
            return getBinOpType(node);
        return OllirUtils.getOllirType(symbolTable.getVariableType(methodSignature, node.get("name")));
    }

    private Integer returnVisit(JmmNode returnStmt, Integer dummy){
        returnType = OllirUtils.getOllirType(symbolTable.getReturnType(methodSignature).getName());
        assignStmtAux(returnStmt.getJmmChild(0));
        code.append("ret.").append(OllirUtils.getOllirType(symbolTable.getReturnType(methodSignature).getName()))
            .append(" ");
        if(isBinOp(returnStmt.getJmmChild(0)) || returnStmt.getJmmChild(0).getKind().equals("DotExpression") || returnStmt.getJmmChild(0).getKind().equals("Negation")){
            code.append("t" + (tempCount-1) +"." + returnType );
        }
        else
            expressionVisit(returnStmt.getJmmChild(0), dummy);
        code.append(";\n");

        return 0;
    }

    private void idVisit(JmmNode id){
        String name = id.get("name");
        boolean isParam = false;
        Symbol var = symbolTable.getLocalVariable(methodSignature, name);
        if(var == null){
            var = symbolTable.getParam(methodSignature, name);
            isParam = (var != null);
        }
        if(var == null){
            var = symbolTable.getField(methodSignature, name);
        }
        if(id.getNumChildren()>0 && id.getJmmChild(0).getKind().equals("AccessToArray")){
            code.append(name).append("[t" + (tempCount-1) +".i32].");
            code.append(getType(id.getJmmChild(0).getJmmChild(0)));
        }
        else{
            String type = OllirUtils.getCode(var.getType());
            if(isParam)
                code.append("$" + symbolTable.getParamPos(methodSignature, name) + ".");
            code.append(name).append(".");
            code.append(type);
        }
    }

    private void assignStmtAux(JmmNode exp){
        boolean flag = false;
        if(exp.getKind().equals("DotExpression"))
            flag = true;
        for(int i = 0; i < exp.getNumChildren(); i++){
            var e = exp.getJmmChild(i);
            if(isBinOp(e) || e.getKind().equals("DotExpression") || e.getKind().equals("InitializeArray") || e.getKind().equals("AccessToArray") || e.getKind().equals("Negation")){
                assignStmtAux(e);
                flag = true;
            }
        }
        if(exp.getKind().equals("Id"))
            return;
        code.append("t" + (tempCount) + "." + OllirUtils.getOllirType(returnType))
        .append(" :=." + OllirUtils.getOllirType(returnType) + " ");

        expressionVisit(exp, 0);
        tempCount++;
        code.append(";\n");

        
    }

    private void assignStmtVisit(JmmNode assignStmt){

        returnType = OllirUtils.getOllirType(symbolTable.getVariableType(methodSignature, assignStmt.getJmmChild(0).get("name")));

        assignStmtAux(assignStmt.getJmmChild(1));

        if(assignStmt.getJmmChild(0).getNumChildren()>0)
            assignStmtAux(assignStmt.getJmmChild(0));
        

        idVisit(assignStmt.getJmmChild(0));

        code.append(" :=.").append(OllirUtils.getOllirType(returnType)).append(" ");

        if(assignStmt.getJmmChild(0).getNumChildren()>0)
            code.append("t" + (tempCount-2) + ".").append(OllirUtils.getOllirType(returnType)).append(";\n");
        else
            code.append("t" + (tempCount-1) + ".").append(OllirUtils.getOllirType(returnType)).append(";\n");
    }

    private Integer dotExpInAssign(JmmNode dotExp, Integer counter){
        code.append("t").append(tempCount).append(".i32 ").append(":=.i32 ");
        ++tempCount;
        memberCallVisit(dotExp);
        code.append(";\n");
        return counter+1;
    }

    Boolean isBinOp(JmmNode op){
        String kind = op.getKind();
        return kind.equals("ANDD") || kind.equals("SUM") || kind.equals("SUB") || kind.equals("LESSTHAN") || kind.equals("MUL") || kind.equals("DIV");
    }

    private Integer leftBinOpVisit(JmmNode binOp, Integer dummy){
        if(binOp.getJmmChild(1).getKind().equals("DotExpression") || binOp.getJmmChild(1).getKind().equals("Negation"))
            code.append("t"+(tempCount-1)+"."+getType(binOp.getJmmChild(0)));
        else code.append(getCode(binOp.getJmmChild(1)) + " ");
        code.append(" "+ OllirUtils.getOllirType(binOp.getKind())).append(".").append(getType(binOp.getJmmChild(1))+ " ");
        if(binOp.getJmmChild(1).getKind().equals("DotExpression") || isBinOp(binOp.getJmmChild(1)) || binOp.getJmmChild(1).getKind().equals("Negation"))
            code.append("t"+(tempCount-3)+"."+getType(binOp.getJmmChild(0)));
        else
        code.append(getCode(binOp.getJmmChild(1)));
        code.append(binOp.getJmmChild(1).getKind());
        return 0;
    }

    private Integer rightBinOpVisit(JmmNode binOp, Integer dummy){
        if(binOp.getJmmChild(1).getKind().equals("DotExpression"))
            code.append("t"+(tempCount-1)+"."+getType(binOp.getJmmChild(0)));
        else code.append(getCode(binOp.getJmmChild(0)) + " ");
        code.append(OllirUtils.getOllirType(binOp.getKind())).append(".").append(getType(binOp.getJmmChild(0))+ " ");
        if(binOp.getJmmChild(1).getKind().equals("DotExpression") || isBinOp(binOp.getJmmChild(1)))
            code.append("t"+(tempCount-1)+"."+getType(binOp.getJmmChild(0)));
        else
            code.append(getCode(binOp.getJmmChild(1)));
        return 0;
    }

    private Integer binOpVisit(JmmNode binOp, Integer dummy){
        if(isBinOp(binOp.getJmmChild(0)) || binOp.getJmmChild(0).getKind().equals("Negation"))
            leftBinOpVisit(binOp, dummy);
        else if(isBinOp(binOp.getJmmChild(1))  || binOp.getJmmChild(1).getKind().equals("Negation"))
            rightBinOpVisit(binOp, dummy);
        else{
            if(binOp.getJmmChild(1).getKind().equals("DotExpression"))
                code.append("t"+(tempCount-1)+"."+getType(binOp.getJmmChild(0))+" ");
            else code.append(getCode(binOp.getJmmChild(1)) + " ");
            code.append(OllirUtils.getOllirType(binOp.getKind())).append(".");
            if(boolInOp) code.append("bool ");
            else code.append(getType(binOp.getJmmChild(1))+ " ");
            if(binOp.getJmmChild(0).getKind().equals("DotExpression"))
                code.append("t"+(tempCount-1)+"."+getType(binOp.getJmmChild(1)));
            else
            code.append(getCode(binOp.getJmmChild(0)));
        }
        return 0;
    }

    private void binOpNoAssign(JmmNode binOp){
        if (isBinOp(binOp.getJmmChild(0))) binOpNoAssign(binOp.getJmmChild(0));
        else if (binOp.getJmmChild(0).getKind().equals("Negation")){
            if(isBinOp(binOp.getJmmChild(0).getJmmChild(0))) binOpNoAssign(binOp.getJmmChild(0).getJmmChild(0));
            else expressionVisit(binOp.getJmmChild(0), 0);
        } else if (binOp.getJmmChild(0).getKind().equals("DotExpression")){
            int trash = dotExpInAssign(binOp.getJmmChild(0), 0);
        }
        else {
            code.append("t").append(tempCount).append(".i32 :=.i32 ");
            tempCount++;
            expressionVisit(binOp.getJmmChild(0), 0);
            code.append(" ").append(OllirUtils.getOllirType(binOp.getKind()))
                .append(".");
            if (boolInOp) code.append("bool");
            else code.append(getType(binOp.getJmmChild(0)));
            code.append(" ");
            expressionVisit(binOp.getJmmChild(1), 0);
            code.append(";\n");
        }
    }

    private void invertOp(JmmNode binOp){
        String kind = binOp.getKind();
        switch (kind){
            case "LESSTHAN" : code.append(">="); break;
            case "ANDD" : code.append("||"); break;
            case "DotExpression": expressionVisit(binOp, 0); break;
            case "Negation": code.append(""); break;
            default:
                throw new NotImplementedException("OLLIR: Operation Invertion not implemented: " + binOp.getKind());
        }
    }

    private void outsideIfAuxLeft(JmmNode ifStmt){
        if (isBinOp(ifStmt.getJmmChild(0))){
            String type;
            type = getType(ifStmt.getJmmChild(0));
            code.append("t" + tempCount + ".").append(type)
                .append(" :=.").append(type).append(" ");
            binOpVisit(ifStmt.getJmmChild(0), 0);
            code.append(";\n");
        }  else if(ifStmt.getJmmChild(0).getKind().equals("DotExpression")){
            int t = dotExpInAssign(ifStmt.getJmmChild(0), 0);
        } else if (ifStmt.getJmmChild(0).getKind().equals("Negation")){
            var negChild = ifStmt.getJmmChild(0).getJmmChild(0);
            if(isBinOp(negChild)){
                binOpNoAssign(negChild);
            }
            else {
                code.append("t").append(tempCount).append(".").append(getType(negChild))
                    .append(":=.").append(getType(negChild));
                tempCount++;
                
                expressionVisit(ifStmt.getJmmChild(0), 0);
                code.append(";\n");
            }
        } 
    }

    private void outsideIfAuxRight(JmmNode ifStmt){
        if (isBinOp(ifStmt.getJmmChild(1))){
            String type;
            type = getType(ifStmt.getJmmChild(1));
            code.append("t" + tempCount + ".").append(type)
                .append(" :=.").append(type).append(" ");
            binOpVisit(ifStmt.getJmmChild(1), 0);
            code.append(";\n");
        } else if(ifStmt.getJmmChild(1).getKind().equals("DotExpression")){
            int t = dotExpInAssign(ifStmt.getJmmChild(1), 0);
        } else if (ifStmt.getJmmChild(1).getKind().equals("Negation")){
            var negChild = ifStmt.getJmmChild(1).getJmmChild(0);
            if(isBinOp(negChild)) binOpNoAssign(negChild);
            else {
                code.append("t").append(tempCount-1).append(".").append(getType(negChild))
                    .append(":=.").append(getType(negChild));
                tempCount++;
                
                expressionVisit(ifStmt.getJmmChild(1), 0);
                code.append(";\n");
            }
        }
    }

    private Boolean outsideIfAux(JmmNode ifStmt){
        Boolean oneBool = false;
        int trash;
        if (ifStmt.getJmmChild(0).getNumChildren() == 0){
            oneBool = true;
        } else {
            JmmNode child = ifStmt.getJmmChild(0);
            if(ifStmt.getJmmChild(0).getKind().equals("DotExpression"))
                trash = dotExpInAssign(ifStmt.getJmmChild(0), 0);
            else {
                if (child.getNumChildren() != 0) outsideIfAuxLeft(ifStmt.getJmmChild(0));
                if (child.getNumChildren() > 1) outsideIfAuxRight(ifStmt.getJmmChild(0));
            }
            /* code.append("t").append(tempCount).append(".i32 :=.i32 ");

            if (child.getNumChildren() != 0){
                if (child.getNumChildren() > 1){
                    code.append("t").append(tempCount-2).append(".i32");
                } else code.append("t").append(tempCount-1).append(".i32");
            }
            invertOp(ifStmt.getJmmChild(0));
            code.append(".bool ");

            if (child.getNumChildren() > 1) code.append("t").append(tempCount-1).append(".i32");

            code.append(";\n"); */

        }
        return oneBool;
    }

    private Integer tempToUse(JmmNode ifStmt){
        Integer tempToUse;
        Boolean left = isBinOp(ifStmt.getJmmChild(0).getJmmChild(0)) || ifStmt.getJmmChild(0).getJmmChild(0).getKind().equals("Negation") || ifStmt.getJmmChild(0).getJmmChild(0).getKind().equals("DotExpression");
        Boolean right = isBinOp(ifStmt.getJmmChild(0).getJmmChild(1)) || ifStmt.getJmmChild(0).getJmmChild(1).getKind().equals("Negation") || ifStmt.getJmmChild(0).getJmmChild(1).getKind().equals("DotExpression");
        if(left && right){
            tempToUse = tempCount-2;
        } else tempToUse = tempCount-1;
        return tempToUse;
    }

    private void insideIfParLeft(JmmNode ifStmt){
        var tempToUse = tempToUse(ifStmt);
        if(isBinOp(ifStmt.getJmmChild(0).getJmmChild(0)) || ifStmt.getJmmChild(0).getKind().equals("DotExpression")){
            code.append("t").append(tempToUse).append(".i32 ");
        } else if(ifStmt.getJmmChild(0).getJmmChild(0).getKind().equals("IntegerLiteral")){
            code.append(ifStmt.getJmmChild(0).getJmmChild(0).get("value")).append(".i32 ");
        } else if(ifStmt.getJmmChild(0).getJmmChild(0).getKind().equals("Negation")){
            code.append("t").append(tempToUse).append(".i32 ");
            //expressionVisit(ifStmt.getJmmChild(0).getJmmChild(0), 0);
        /*} else if(ifStmt.getJmmChild(0).getJmmChild(0).getKind().equals("Identifier")){
            expressionVisit(ifStmt.getJmmChild(0).getJmmChild(0), 0); */
        } else if(ifStmt.getJmmChild(0).getJmmChild(0).getKind().equals("DotExpression")) {
            code.append("t").append(tempToUse).append(".i32 ");
        } else code.append(ifStmt.getJmmChild(0).getJmmChild(0).get("name")).append(".i32 ");
    }

    private void insideIfParRight(JmmNode ifStmt){
        if(isBinOp(ifStmt.getJmmChild(0).getJmmChild(1)) || ifStmt.getJmmChild(0).getKind().equals("DotExpression")){
            code.append("t").append(tempCount-1).append(".i32 ");
        } else if(ifStmt.getJmmChild(0).getJmmChild(1).getKind().equals("IntegerLiteral")){
            code.append(ifStmt.getJmmChild(0).getJmmChild(1).get("value")).append(".i32 ");
        } else if(ifStmt.getJmmChild(0).getJmmChild(1).getKind().equals("Negation")){
            code.append("t").append(tempCount-1).append(".i32 ");
            //expressionVisit(ifStmt.getJmmChild(0).getJmmChild(1), 0);
        /* } else if(ifStmt.getJmmChild(0).getJmmChild(1).getKind().equals("Identifier")){
            expressionVisit(ifStmt.getJmmChild(0).getJmmChild(1), 0); */
        } else if(ifStmt.getJmmChild(0).getJmmChild(1).getKind().equals("DotExpression")) {
            code.append("t").append(tempCount-1).append(".i32 ");
        } else code.append(ifStmt.getJmmChild(0).getJmmChild(1).get("name")).append(".i32");
    }

    private void insideIfPar(JmmNode ifStmt){
        visitingIf = true;

        insideIfParLeft(ifStmt);

        invertOp(ifStmt.getJmmChild(0));

        code.append(".bool ");

        insideIfParRight(ifStmt);

        visitingIf = false;
    }

    private void oneBoolTrue(JmmNode oneB){
        if (isBinOp(oneB)){
            boolInOp = true;
            binOpNoAssign(oneB);
            boolInOp = false;
        }
        else if (oneB.getKind().equals("DotExpression")){
            code.append("t").append(tempCount);
        }
        else if (oneB.getKind().equals("Negation")){
            if (oneB.getJmmChild(0).getKind().equals("Boolean")) 
                code.append(oneB.getJmmChild(0).get("value"));
            else if (oneB.getJmmChild(0).getKind().equals("DotExpression"))
                code.append("t").append(tempCount);
            else if (isBinOp(oneB.getJmmChild(0))){
                boolInOp = true;
                binOpVisit(oneB.getJmmChild(0), 0);
                boolInOp = false;
            }
            else code.append(oneB.getJmmChild(0).get("name"));
        }
        else if (oneB.getKind().equals("Boolean")) code.append(oneB.get("value"));
        else if (oneB.getKind().equals("Id")) code.append(oneB.get("name"));
        else if (oneB.getKind().equals("Identifier")) code.append(oneB.get("name"));

        code.append(".bool");
    }

    private void insidePar(JmmNode ifStmt, Boolean oneBool){
        if (oneBool) {
            oneBoolTrue(ifStmt.getJmmChild(0));
        } else if(ifStmt.getJmmChild(0).getKind().equals("Negation")){
            if(ifStmt.getJmmChild(0).getNumChildren() == 1){
                oneBoolTrue(ifStmt.getJmmChild(0));
            } else insideIfPar(ifStmt.getJmmChild(0));
        } else if (ifStmt.getJmmChild(0).getKind().equals("DotExpression")){
            oneBoolTrue(ifStmt.getJmmChild(0));
        } else{
            insideIfPar(ifStmt);
        }
    }

    private void ifVisit(JmmNode ifStmt){
        ++ifCount;
        int elseStmtCount = 0;

        Boolean oneBool = outsideIfAux(ifStmt);

        code.append("if (");

        insidePar(ifStmt, oneBool);
        
        code.append(") goto else").append(ifCount).append(";\n");

        int numChildren = ifStmt.getNumChildren();
        for (int i = 1; i < numChildren; i++){
            if (ifStmt.getJmmChild(i).getKind().equals("StatementBlock") && (i != numChildren - 1))
                code.append("goto endIf").append(ifCount).append(";\n");
            if(ifStmt.getJmmChild(i).getKind().equals("StatementBlock")){
                ++elseStmtCount;
                if (elseStmtCount == 2){
                    if(i == numChildren - 1) code.append("endIf").append(ifCount).append(":\n");
                    else code.append("endElse").append(ifCount).append(":\n");
                    elseStmtCount = 0;
                } else {
                    code.append("else").append(ifCount).append(":\n");
                }
            }
            visit(ifStmt.getJmmChild(i));
        }
    }

    private void whileVisit(JmmNode whileStmt){
        ++loopCount;
        code.append("Loop").append(loopCount).append(":\n");

        Boolean oneBool = outsideIfAux(whileStmt);

        code.append("if(");

        insidePar(whileStmt, oneBool);

        code.append(") goto end").append(loopCount).append(";\n");
        int whileChildren = whileStmt.getNumChildren();
        for (int i = 1; i < whileChildren; i++){
            visit(whileStmt.getJmmChild(i));
        }
        code.append("goto Loop").append(loopCount).append(";\n");
        code.append("end").append(loopCount).append(":\n");
    }

    private Integer newArrayVisit(JmmNode newArr, Integer dummy){
        
        code.append("new(array, ");
        // expressionVisit(newArr.getJmmChild(0),0);
        code.append("t"+(tempCount-1)+".i32");
        code.append(").array.i32");
        return 0;
    }

    private void accessToArrayVisit(JmmNode accessArr, Integer dummy){
        int trash;
        if (accessArr.getJmmChild(0).getKind().equals("DotExpression")){
            if (accessArr.getJmmParent().getKind().equals("InitializeArray")){
                memberCallVisit(accessArr.getJmmChild(0));
            }
            else trash = dotExpInAssign(accessArr.getJmmChild(0), 0);
        }
        else expressionVisit(accessArr.getJmmChild(0), 0); 
    }

    /* private void negationVisit(JmmNode negStmt){
        var toNegate = negStmt.getJmmChild(0); 
        if(toNegate){
        } else if (toNegate.getKind()) {
        }
    } */

    private Integer expressionVisit(JmmNode expression, Integer dummy){
        String kind = expression.getKind();

        if(isBinOp(expression)){
            binOpVisit(expression, 0);
            return 0;
        }
        int trash;
        switch(kind){
            case "Id": idVisit(expression); break;
            case "Identifier": idVisit(expression.getJmmChild(0));  break;
            case "DotExpression": memberCallVisit(expression); break;
            case "Boolean": code.append(expression.get("value")).append(".bool"); break;
            //Negation currently skipping
            case "Negation": negationVisit(expression); break; //TODO negationVisit(expression); break;
            case "IntegerLiteral": code.append(expression.get("value")).append(".").append(OllirUtils.getOllirType("TypeInt")); break;
            case "InitializeArray": newArrayVisit(expression, dummy); break;
            case "NewObject": code.append("new(").append(expression.getJmmChild(0).get("name"))
                                .append(").")
                                .append(OllirUtils.getOllirType(expression.getJmmChild(0).get("name"))); break;
            case "AccessToArray": accessToArrayVisit(expression, 0); break;
            default: throw new NotImplementedException("OLLIR: Expression kind not implemented: " + expression.getKind());
        }
        return 5;
    }


    private void negationVisit(JmmNode expression) {
        if(expression.getJmmChild(0).getKind().equals("Id") || expression.getJmmChild(0).getKind().equals("IntegerLiteral")){
            code.append(expressionVisit(expression.getJmmChild(0), 0));
            code.append(" !.bool ");
            code.append(expressionVisit(expression.getJmmChild(0),0));
        }else{
            code.append("t"+(tempCount-1) +".bool ");
            code.append("!.bool ");
            code.append("t"+(tempCount-1) +".bool ");

        }

    }

    private Integer stmtVisit(JmmNode stmt, Integer dummy){
        String stmtType = stmt.getKind().toString();
        switch(stmtType){
            // case "StatementBlock": break;
            case "IfStatement": ifVisit(stmt); break;
            case "WhileStatement": whileVisit(stmt); break;
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

    private void memberCallSeparationParams(JmmNode memberCall){
        if(memberCall.getJmmChild(1).getNumChildren()>1){ //Has params
            var children = memberCall.getJmmChild(1).getChildren();
            for( var child : children.subList(1, children.size()-1)){
                if (isBinOp(child)){
                    binOpNoAssign(child);
                    code.append("t").append(tempCount).append(".").append(getType(child.getJmmChild(0))).append(":=")
                        .append(".").append(getType(child.getJmmChild(0))).append(" ");
                    
                    if(isBinOp(child.getJmmChild(0))) code.append("t").append(tempCount-2);
                    else if (child.getJmmChild(0).getKind().equals("IntegerLiteral")) code.append(child.getJmmChild(0).get("value"));
                    else code.append(child.getJmmChild(0).get("name"));

                    code.append(".").append(getType(child.getJmmChild(0))).append(" ");
                    code.append(OllirUtils.getOllirType(child.getKind())).append(".").append(getType(child.getJmmChild(1))+ " ");

                    if(isBinOp(child.getJmmChild(1))) code.append("t").append(tempCount-1);
                    else if (child.getJmmChild(1).getKind().equals("IntegerLiteral")) code.append(child.getJmmChild(1).get("value"));
                    else code.append(child.getJmmChild(1).get("name"));
                    code.append(".").append(getType(child.getJmmChild(1))).append(";\n");
                }
            }
           
        }
    }

    private void memberCallVisit(JmmNode memberCall){
        //type missing
        
        memberCallSeparationParams(memberCall);

        var childType = memberCall.getJmmChild(1).getJmmChild(0).getKind();
        if(childType.equals("Length"))
        {
            code.append("arraylength(").append(getCode(memberCall.getJmmChild(0))).append(").i32");
            return;
        }
        visit(memberCall.getJmmChild(0));
        boolean isVirtual = false;
        if(childType.equals("main") || symbolTable.getImports().contains(memberCall.getJmmChild(0).get("name"))){
            code.append("invokestatic(");
        }
        else{
            isVirtual = true;
            code.append("invokevirtual(");
        }
        code.append(memberCall.getJmmChild(0).get("name"));
        if(!memberCall.getJmmChild(0).get("name").equals("this") && isVirtual){
           code.append(".").append(memberCall.getJmmChild(0).get("name"));
        }
        code.append(", \"").append(memberCall.getJmmChild(1).getJmmChild(0).get("name"))
            .append("\"");

        
        if(memberCall.getJmmChild(1).getNumChildren()>1){ //Has params
            var children = memberCall.getJmmChild(1).getChildren();
            for( var child : children.subList(1, children.size()-1)){
                if (isBinOp(child)){
                    code.append(", t").append(tempCount).append(getCode(child.getJmmChild(1)));
                } else code.append(", ").append(getCode(child));
            }
        }
        returnType = "V";
        var type = symbolTable.getReturnType(memberCall.getJmmChild(1).getJmmChild(0).get("name"));
        if(type!= null) returnType = type.getName();
        code.append(").").append(OllirUtils.getOllirType(returnType));

    }



}