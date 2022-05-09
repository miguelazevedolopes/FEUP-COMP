package pt.up.fe.comp.ollir;

import java.util.stream.Collectors;

import java_cup.runtime.symbol;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode; 

public class OllirGenerator extends AJmmVisitor<Integer, Integer> {
    private final StringBuilder code;
    private final SymbolTable symbolTable;

    public String curMethRetType;
    public String methodSignature;
    public Integer varcount = 0;

    public OllirGenerator(SymbolTable symbolTable){
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;

        addVisit("Start", this::programVisit);
        addVisit("ClassDeclaration", this::classDeclVisit);
        addVisit("MainMethod", this::methodDeclVisit);
        addVisit("NormalMethod", this::methodDeclVisit);
        addVisit("MethodBody", this::methodBodyVisit);
        addVisit("Statement", this::exprStmtVisit);
        addVisit("DotExpression", this::memberCallVisit);
        addVisit("Var", this::varDeclVisit);
        addVisit("SUM", this::sumVisit);
        addVisit("Equality", this::equalVisit);
        addVisit("NewObject", this::newVisit);
        addVisit("ReturnRule", this::returnVisit);
    }

    public String getCode(){
        return code.toString();
    }

    private Integer programVisit(JmmNode program, Integer dummy){
        for (var importString : symbolTable.getImports()){
            code.append("import ").append(importString).append(";\n");
        }

        for (var child : program.getChildren()){
            visit(child);
        } 
        return 0;
    }

    private Integer classDeclVisit(JmmNode classDecl, Integer dummy){
        code.append("public ").append(symbolTable.getClassName());
        var superClass = symbolTable.getSuper();
        if (superClass != null) code.append(" extends ").append(superClass);
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
            if(child.getKind().equals("MethodBody")){
                visit(child);
            }
        }

        code.append("}\n");

        return 0;
    }


    private Integer methodBodyVisit(JmmNode methodBody, Integer dummy){
        int lastParamIndex = -1;
        for(int i = 0; i < methodBody.getNumChildren(); i++){
            if(methodBody.getJmmChild(i).getKind().equals("Param")){
                lastParamIndex = i;
            }
        }
        varcount = 0;
        var stmts = methodBody.getChildren().subList(lastParamIndex +1, methodBody.getNumChildren());
        for(var stmt: stmts){
            //System.out.println("stmt is:" + stmt.getKind());
            visit(stmt);
        }

        return 0;
    }


    private Integer exprStmtVisit(JmmNode exprStmt, Integer dummy){
        visit(exprStmt.getJmmChild(0));
        code.append(";\n");
        return 0;
    }

    private Integer memberCallVisit(JmmNode memberCall, Integer dummy){
        visit(memberCall.getJmmChild(0));
        //type missing
        code.append(".").append(" ");
        code.append("invokevirtual(");
        code.append(memberCall.getJmmChild(0).get("name")).append(".").append(" , "); // type missing after .
        code.append(memberCall.getJmmChild(1).getJmmChild(0).get("name"));
        for(int i = 1; i < memberCall.getJmmChild(1).getNumChildren(); i++){
            code.append("");
        }
        code.append(").").append(" ");//type missing

        return 0;
    }

    //I think this counts as cheating but as long as it works
   private Integer varDeclVisit(JmmNode varDecl, Integer dummy){
        var locals = symbolTable.getLocalVariables(methodSignature);
        var varcountmax = locals.size();
        for (var local : locals){
            if(varcount >= varcountmax) break;
            code.append(local.getName()).append(".");
            if (local.getType().isArray()){
                code.append("array.");
            }
            code.append(local.getType().getName());
            code.append(";\n");
            varcount++;
        }
        
        return 0;
    }

    //needs This to work on the example i was using, else will crash after making symbol table
    private Integer sumVisit(JmmNode sumStmt, Integer dummy){
        /* var sumChild1 = sumStmt.getJmmChild(0);
        var sumChild2 = sumStmt.getJmmChild(1);
        if (sumChild1.getKind().equals("Id")){
            code.append(sumChild1.get("name"));
        } else if (sumChild1.getKind().equals("IntegerLiteral")){
            code.append(sumChild1.get("value"));
        } else visit(sumChild1);

        code.append(" + ");

        if (sumChild2.getKind().equals("Id")){
            code.append(sumChild2.get("name"));
        } else if (sumChild2.getKind().equals("IntegerLiteral")){
            code.append(sumChild2.get("value"));
        } else visit(sumChild2); */

        return 0;
    }

    //this one's done, i think
    private Integer equalVisit(JmmNode equalStmt, Integer dummy){
        code.append(equalStmt.getJmmChild(0).get("name")).append(":=");
        for(int i = 1; i < equalStmt.getNumChildren(); i++){
            var curstmt = equalStmt.getJmmChild(i);
            if (curstmt.getKind().equals("Id")){
                code.append(curstmt.get("name"));
            } else if (curstmt.getKind().equals("IntegerLiteral")){
                code.append(curstmt.get("value"));
            } else visit(curstmt);
        }
        code.append(";\n");
        return 0;
    }

    //also done, iirc
    private Integer newVisit(JmmNode newStmt, Integer dummy){
        code.append(".").append(newStmt.getJmmChild(0).get("name")).append(" new(");
        code.append(newStmt.getJmmChild(0).get("name"));
        for (int i = 1; i < newStmt.getNumChildren(); i++){
            code.append(",");
            var curstmt = newStmt.getJmmChild(i);
            if (curstmt.getKind().equals("Id")){
                code.append(curstmt.get("name"));
            } else if (curstmt.getKind().equals("IntegerLiteral")){
                code.append(curstmt.get("value"));
            } else visit(curstmt);
        }
        code.append(")");
        code.append(".").append(newStmt.getJmmChild(0).get("name"));
        return 0;
    }

    //probs done
    private Integer returnVisit(JmmNode returnStmt, Integer dummy){
        code.append("return stmt supposed here");
        //code.append("ret.").append(curMethRetType).append(" ").append(returnStmt.getJmmChild(0).get("name")).append(returnStmt.getJmmChild(0).get("type"));

        return 0;
    }


    



}