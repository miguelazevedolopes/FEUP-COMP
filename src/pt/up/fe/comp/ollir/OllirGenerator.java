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

public class OllirGenerator extends AJmmVisitor<Integer, Code> {
    private final StringBuilder code;
    private final SymbolTable symbolTable;
    private final ollirSymbolTable ollirTable;
    private String methodSignature;

    public OllirGenerator(SymbolTable symbolTable){
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;
        this.ollirTable = new ollirSymbolTable();
        addVisit("Start", this::programVisit);
        addVisit("ClassDeclaration", this::classDeclVisit);
        addVisit("MainMethod", this::methodDeclVisit);
        addVisit("NormalMethod", this::methodDeclVisit);
        addVisit("MethodBody", this::methodBodyVisit);
        addVisit("Equality", this::assignStmtVisit);
//        addVisit("DotExpression", this::stmtVisit);
//        addVisit("IfStatement", this::stmtVisit);
//        addVisit("WhileStatement", this::stmtVisit);
        addVisit("SUM", this::binOpVisit);
        addVisit("SUB", this::binOpVisit);
        addVisit("MUL", this::binOpVisit);
        addVisit("DIV", this::binOpVisit);
        addVisit("ANDD", this::binOpVisit);
        addVisit("LESSTHAN", this::binOpVisit);
        addVisit("Id", this::idVisit);
        addVisit("NewObject", this::newObjectVisit);
        addVisit("IntegerLiteral", this::integerLiteralVisit);
        addVisit("InitializeArray", this::initializeArrayVisit);
    }




    public String getCode(){
        return code.toString();
    }

    private Code programVisit(JmmNode program, Integer dummy){
        for (var importString : symbolTable.getImports()){
            code.append("import ").append(importString).append(";\n");
        }
        code.append("\n");

        for (var child : program.getChildren()){
            visit(child);
        }
        return null;
    }

    private Code classDeclVisit(JmmNode classDecl, Integer dummy){
        code.append("public ").append(symbolTable.getClassName());
        var superClass = symbolTable.getSuper();
        if (!superClass.isEmpty()) code.append(" extends ").append(superClass);
        code.append("{\n\n");

        //FIELDS
        for(var field : symbolTable.getFields()){
            code.append(".field private ").append(field.getName());
            if(field.getType().isArray()) code.append(".array");
            code.append(".").append(OllirUtils.getOllirType(field.getType().getName())).append(";\n");
        }
        code.append("\n");

        //CONSTRUCTOR
        code.append(".construct ").append(symbolTable.getClassName()).append("().V{\n\tinvokespecial(this, \"<init>\").V;\n}\n");
        code.append("\n");

        for (var child : classDecl.getChildren()){
            visit(child);
        }

        code.append("}\n");
        return null;
    }

    private Code methodDeclVisit(JmmNode methodDecl, Integer dummy){
        methodSignature = methodDecl.get("name");
        var isStatic = Boolean.valueOf(methodDecl.get("isStatic"));

        code.append(".method public ");
        if(isStatic) code.append("static ");

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
            if(child.getKind().equals("MethodBody") || child.getKind().equals("ReturnRule")){
                visit(child);
            }
        }

        if(methodSignature.equals("main")) code.append("\tret.V;\n");
        code.append("}\n\n");

        return null;
    }


    private Code methodBodyVisit(JmmNode methodBody, Integer dummy){

        var stmts = methodBody.getChildren();
        for(var stmt: stmts){
            visit(stmt);
        }

        return null;
    }


    private Code assignStmtVisit(JmmNode assignStmt, Integer dummy){
        var varname = assignStmt.getJmmChild(0).get("name");
        Code localcode = visit(assignStmt.getJmmChild(1));
        code.append(localcode.prefix);
        code.append(varname); //Var name
        code.append(" =.").append(OllirUtils.getOllirType(symbolTable.getVariableType(methodSignature,varname))); //.i32 .bool etc
        code.append(localcode.code);
        return null;
    }

    private Code binOpVisit(JmmNode node, Integer dummy){

        Code lhs = visit(node.getJmmChild(0));

        Code rhs = visit(node.getJmmChild(1));

        String op = OllirUtils.getOllirType(node.getKind()); //BIN OP TYPE

        Code thisCode = new Code();

        thisCode.prefix = lhs.prefix;

        thisCode.prefix += rhs.prefix;

        //here you can decide if the temporary variable is necessary or not
        //I am considering that I always need a new temp

        String temp = ollirTable.newTemp();

        thisCode.prefix += temp + "=" + lhs.code + op + rhs.code;

        thisCode.code = temp;

        return thisCode;

    }

    private Code visitMethodCall(JmmNode node, Integer dummy){

        String prefixCode = "";

        Code target = visit(node.getJmmChild(0));  //first child is the target object

        prefixCode += target.prefix;

        String methodName = node.get("name");

        //invokevirtual(temp1,"foo"

        String finalcode = "invokevirtual("+target.code+","+methodName;

        for(JmmNode arg : node.getChildren()){ //for each argument

            Code argCode = visit(arg);

            prefixCode += argCode.prefix; //append code of argument prior to invocation

            finalcode += "," + argCode.code; //append temp variable to arguments
        }

        //invokevirtual(temp1,"foo" <(, arg)*>).V

        finalcode += ")."+ symbolTable.getReturnType(methodName);

        //here you can decide if the temporary variable is necessary or not
        //I am considering that I always need a new temp

        String temp = ollirTable.newTemp();

        finalcode = temp + "=" + finalcode;

        Code thisCode = new Code();

        thisCode.code = temp;

        thisCode.prefix = prefixCode;

        return thisCode;

    }

    private Code idVisit(JmmNode node, Integer integer) {
        Code thisCode = new Code();
        var varname = node.get("name");
        thisCode.code = varname + OllirUtils.getOllirType(symbolTable.getVariableType(methodSignature,varname));
        return thisCode;
    }

    private Code newObjectVisit(JmmNode node, Integer dummy) {
        Code thisCode = new Code();
        Code thatCode = visit(node.getJmmChild(0));
        thisCode.prefix = thatCode.prefix;
        thisCode.code = "new(" + thatCode.code +")." + OllirUtils.getOllirType(node.getJmmChild(0).get("name"));
        return thisCode;
    }

    private Code integerLiteralVisit(JmmNode node, Integer integer) {
        Code thisCode = new Code();
        thisCode.code = node.get("value");
        return thisCode;
    }

    private Code initializeArrayVisit(JmmNode node, Integer integer) {
        Code thisCode = new Code();
        Code thatCode = visit(node.getJmmChild(0).getJmmChild(0));
        thisCode.prefix = thatCode.prefix;
        //new(array, t1.i32).array.i32;
        thisCode.code = "new(array, " + thatCode.code + ".i32).array.i32";
        return thisCode;
    }



}