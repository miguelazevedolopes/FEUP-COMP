package pt.up.fe.comp.ollir;

import java.util.stream.Collectors;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class OllirGenerator extends AJmmVisitor<Integer, Code> {
    private final StringBuilder code;
    private final SymbolTable symbolTable;
    private final OllirSymbolTable ollirTable;
    private String methodSignature;

    public OllirGenerator(SymbolTable symbolTable){
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;
        this.ollirTable = new OllirSymbolTable();
        addVisit("Start", this::programVisit);
        addVisit("ClassDeclaration", this::classDeclVisit);
        addVisit("MainMethod", this::methodDeclVisit);
        addVisit("NormalMethod", this::methodDeclVisit);
        addVisit("MethodBody", this::methodBodyVisit);
        addVisit("Equality", this::assignStmtVisit);
        addVisit("DotExpression", this::methodCallVisit);
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
        addVisit("AccessToArray", this::accessArrayVisit);
        addVisit("ReturnRule", this::returnVisit);
        addVisit("Boolean", this::booleanVisit);
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
            if(child.getKind().equals("MethodBody") ){
                visit(child);
            }
            else if(child.getKind().equals("ReturnRule")){
                Code thisCode = visit(child);
                code.append(thisCode.prefix);
                code.append(thisCode.code);
            }
        }

        if(methodSignature.equals("main")) code.append("\tret.V;\n");
        code.append("}\n\n");

        return null;
    }


    private Code methodBodyVisit(JmmNode methodBody, Integer dummy){

        var stmts = methodBody.getChildren();
        for(var stmt: stmts){
            Code thisCode = visit(stmt);
            if (thisCode == null) continue;
            code.append(thisCode.prefix);
            if(stmt.getKind().equals("DotExpression")) continue;
            code.append(thisCode.code);
        }

        return null;
    }


    private Code assignStmtVisit(JmmNode node, Integer dummy){
        var varname = node.getJmmChild(0).get("name");
        Code thatCode = visit(node.getJmmChild(0));
        Code thisCode = visit(node.getJmmChild(1));
        thisCode.prefix += thatCode.prefix;
        var type = OllirUtils.getOllirType(symbolTable.getVariableType(methodSignature,varname));
        thisCode.code = "\t" + thatCode.code  +" :=." + type + " " + thisCode.code + ";\n";
        return thisCode;
    }

    private Code binOpVisit(JmmNode node, Integer dummy){

        Code lhs = visit(node.getJmmChild(0));

        Code rhs = visit(node.getJmmChild(1));

        String op = node.getKind(); //BIN OP TYPE

        Code thisCode = new Code();

        thisCode.prefix = lhs.prefix;
        thisCode.prefix += rhs.prefix;

        //here you can decide if the temporary variable is necessary or not
        //I am considering that I always need a new temp

        String temp = ollirTable.newTemp();

        String type = OllirUtils.getOllirType(node.getKind());
        thisCode.prefix += "\t" + temp + "." + type
                + " :=." + type +" "+ lhs.code + " " + OllirUtils.getCode(op)
                + " " + rhs.code +";\n";

        thisCode.code = temp +"."+type;

        return thisCode;
    }

    private Code methodCallVisit(JmmNode node, Integer dummy){

        if(node.getJmmChild(1).getNumChildren() > 0 && node.getJmmChild(1).getJmmChild(0).getKind().equals("Length"))
        {
            Code thisCode = new Code();
            Code thatCode = visit(node.getJmmChild(0));
            thisCode.prefix = thatCode.prefix;
            String temp = ollirTable.newTemp();
            thisCode.prefix += "\t" + temp + ".i32 :=.i32 " + "arraylength(" + thatCode.code+").i32" + ";\n";
            thisCode.code = temp + ".i32";
            return thisCode;
        }

        Code thisCode = new Code();
        Code thatCode = visit(node.getJmmChild(0));  //first child is the target object

        thisCode.prefix = thatCode.prefix;
        boolean isStatic = symbolTable.getImports().contains(thatCode.code);

        String methodName = node.getJmmChild(1).getJmmChild(0).get("name");

        //invokevirtual(temp1,"foo"
        String finalcode;
        if(isStatic)
            finalcode = "invokestatic("+thatCode.code+", \""+methodName +"\"";
        else
            finalcode = "invokevirtual("+thatCode.code+", \""+methodName + "\"";

        for(int i = 1; i < node.getJmmChild(1).getNumChildren(); i++){  //for each argument
            JmmNode arg = node.getJmmChild(1).getJmmChild(i);
            Code argCode = visit(arg);

            thisCode.prefix += argCode.prefix; //append code of argument prior to invocation

            finalcode += ", " + argCode.code; //append temp variable to arguments
        }

        //invokevirtual(temp1,"foo" <(, arg)*>).V


        var ttype = symbolTable.getReturnType(methodName);
        String type = OllirUtils.getOllirType(ttype == null ? "i32" : ttype.getName());
        if(isStatic) type = "V"; //thatCode.code has import name
        finalcode += ")."+ type;

        //here you can decide if the temporary variable is necessary or not
        //I am considering that I always need a new temp

        String temp = ollirTable.newTemp();

        if(node.getJmmParent().getKind().equals("MethodBody"))
            finalcode = "\t" + finalcode + ";\n";
        else
            finalcode = "\t" + temp +"."+  type+ " :=." + type + " " + finalcode + ";\n";

        thisCode.code = temp + "." + type;

        thisCode.prefix += finalcode;

        return thisCode;

    }

    private Code idVisit(JmmNode node, Integer integer) {
        if(node.getNumChildren()>0 && node.getJmmChild(0).getKind().equals("AccessToArray")){
            return visit(node.getJmmChild(0));
        }
        Code thisCode = new Code();
        var varname = node.get("name");
        //TODO  check if import

        thisCode.code = varname;
        if(symbolTable.isArray(methodSignature,varname)){
            thisCode.code += ".array";
        }
        if(!symbolTable.getImports().contains(varname))
            thisCode.code += "."+ OllirUtils.getOllirType(symbolTable.getVariableType(methodSignature, varname));
        return thisCode;
    }

    private Code newObjectVisit(JmmNode node, Integer dummy) {
        Code thisCode = new Code();
        Code thatCode = visit(node.getJmmChild(0));
        thisCode.prefix = thatCode.prefix;
        thisCode.code = "new(" + thatCode.code.substring(0,thatCode.code.length()-1) +")."
                + OllirUtils.getOllirType(node.getJmmChild(0).get("name"));
        return thisCode;
    }

    private Code integerLiteralVisit(JmmNode node, Integer integer) {
        Code thisCode = new Code();
        String temp = ollirTable.newTemp();
        thisCode.prefix = "\t" + temp +".i32 " + " :=.i32 " + node.get("value") + ".i32" + ";\n";
        thisCode.code = temp + ".i32";
        return thisCode;
    }

    private Code initializeArrayVisit(JmmNode node, Integer integer) {
        Code thisCode = new Code();
        Code thatCode = visit(node.getJmmChild(0).getJmmChild(0));
        thisCode.prefix = thatCode.prefix;
        //new(array, t1.i32).array.i32;
        thisCode.code = "new(array, " + thatCode.code +").array.i32";
        return thisCode;
    }

    private Code accessArrayVisit(JmmNode node, Integer integer) {
        boolean needsTemp = !node.getJmmParent().getJmmParent().getKind().equals("Equality");
        //TODO change thisCode.code
        Code thisCode = new Code();
        Code thatCode = visit(node.getJmmChild(0));
        var parent = node.getJmmParent();
        thisCode.prefix = thatCode.prefix;
        String type = OllirUtils.getOllirType(symbolTable.getVariableType(methodSignature,parent.get("name")));
        if(needsTemp){
            String temp = ollirTable.newTemp();
            thisCode.prefix += "\t" + temp + "." + type + " :=." + type + " " + parent.get("name") + "[" + thatCode.code +"]."
                    + OllirUtils.getOllirType(symbolTable.getVariableType(methodSignature, parent.get("name"))) + ";\n";
            thisCode.code = temp + "." + type;
        }
        else{
            thisCode.code = parent.get("name") + "[" + thatCode.code +"]."
                    + OllirUtils.getOllirType(symbolTable.getVariableType(methodSignature, parent.get("name")));
        }

        return thisCode;
    }

    private Code returnVisit(JmmNode node, Integer integer) {
        Code thisCode = new Code();
        Code thatCode = visit(node.getJmmChild(0));

        thisCode.prefix = thatCode.prefix;
        String type = OllirUtils.getOllirType(symbolTable.getReturnType(methodSignature).getName());
        thisCode.code = "\tret." + type + " " + thatCode.code + ";\n";

        return thisCode;
    }

    private Code booleanVisit(JmmNode node, Integer integer) {
        Code thisCode = new Code();
        thisCode.code = node.get("value") + ".bool";
        return thisCode;
    }





}