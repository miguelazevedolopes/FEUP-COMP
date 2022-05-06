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
        var methodSignature = methodDecl.get("name");
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

        // var stmts = methodBody.getChildren().subList(lastParamIndex +1, methodBody.getNumChildren());
        // System.out.println(stmts);
        // for(var stmt: stmts){
        //     visit(stmt);
        // }

        return 0;
    }


    private Integer exprStmtVisit(JmmNode exprStmt, Integer dummy){
        visit(exprStmt.getJmmChild(0));
        code.append(";\n");
        return 0;
    }

    private Integer memberCallVisit(JmmNode memberCall, Integer dummy){
        visit(memberCall.getJmmChild(0));
        code.append(".").append(memberCall.getJmmChild(1)).append("(");

        return 0;
    }



}