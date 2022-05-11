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

    public Symbol twoWaySymbol1 = null;
    public Method twoWayMethod1 = null;
    public Symbol twoWaySymbol2 = null;
    public Method twoWayMethod2 = null;

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

        code.append("}\n");

        return 0;
    }


    private Integer methodBodyVisit(JmmNode methodBody, Integer dummy){

        var stmts = methodBody.getChildren();
        for(var stmt: stmts){
            visit(stmt);
        }

        return 0;
    }

    private String getType(JmmNode node){
        if(node.getKind().equals("IntegerLiteral"))
            return "i32";
        if(node.getKind().equals("Id"))
            return OllirUtils.getOllirType(symbolTable.getVariableType(methodSignature, node.get("name")));
        return OllirUtils.getOllirType(symbolTable.getVariableType(methodSignature, node.get("name")));
    }

    private Integer returnVisit(JmmNode returnStmt, Integer dummy){
        String type = "";
        if(isBinOp(returnStmt.getJmmChild(0))){
            type = OllirUtils.getOllirType(getType(returnStmt.getJmmChild(0).getJmmChild(1)));
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

    private void assignStmtVisit(JmmNode assignStmt){
        int counter = 0;
        if(isBinOp(assignStmt.getJmmChild(1)) && isBinOp(assignStmt.getJmmChild(1).getJmmChild(0))){
            counter = binOpVisit(assignStmt.getJmmChild(1), 0);
        }
        idVisit(assignStmt.getJmmChild(0));

        code.append(" :=.").append(getType(assignStmt.getJmmChild(0))).append(" ");

        int i = 1;
        if(assignStmt.getJmmChild(1).getKind().equals("AccessToArray"))
            i = 2;

        if(counter == 0)
            expressionVisit(assignStmt.getJmmChild(i),0);
        else
            code.append("t").append(tempCount-1 + ".i32 ").append(OllirUtils.getOllirType(assignStmt.getJmmChild(1).getKind()))
                .append(".").append(OllirUtils.getOllirType(assignStmt.getJmmChild(1).getJmmChild(1).getKind())).append(" ")
                .append(expressionVisit(assignStmt.getJmmChild(1).getJmmChild(1), 0));

    }

    Boolean isBinOp(JmmNode op){
        String kind = op.getKind();
        return kind.equals("ANDD") || kind.equals("SUM") || kind.equals("SUB") || kind.equals("LESSTHAN") || kind.equals("MUL") || kind.equals("DIV");
    }

    private Integer binOpVisit(JmmNode binOp, Integer counter){
        if(isBinOp(binOp.getJmmChild(0))){
            counter = binOpVisit(binOp.getJmmChild(0), counter+1);
        }
        if(!isBinOp(binOp.getJmmChild(0)) && !isBinOp(binOp.getJmmChild(1))){
            if(counter != 0){
                code.append("t").append(tempCount).append(".i32 :=.i32 ");
                tempCount++;
                expressionVisit(binOp.getJmmChild(0), 0);
                code.append(" ").append(OllirUtils.getOllirType(binOp.getKind()))
                    .append(".").append(getType(binOp.getJmmChild(0))).append(" ");
                expressionVisit(binOp.getJmmChild(1), 0);
                code.append(";\n");
            }
            else{
                expressionVisit(binOp.getJmmChild(0), 0);
                code.append(" ").append(OllirUtils.getOllirType(binOp.getKind()))
                    .append(".").append(getType(binOp.getJmmChild(0))).append(" ");
                expressionVisit(binOp.getJmmChild(1), 0);
            }
            return counter +1;
        }

        code.append("t").append(tempCount).append(".i32 :=.i32 ");
        code.append("t").append(tempCount-1).append(".i32 ");
        code.append(" ").append(OllirUtils.getOllirType(binOp.getKind()))
            .append(".").append(getType(binOp.getJmmChild(1))).append(" ");
        expressionVisit(binOp.getJmmChild(1), 0);
        code.append(";\n");
        tempCount++;
        return counter;
    }


    private Integer expressionVisit(JmmNode expression, Integer dummy){
        String kind = expression.getKind();

        if(isBinOp(expression)){
            binOpVisit(expression, 0);
            return 0;
        }
        switch(kind){
            case "Id": idVisit(expression); break;
            case "DotExpression": memberCallVisit(expression); break;
            case "This": break;
            case "Boolean": break;
            case "Negation": break;
            case "IntegerLiteral": code.append(expression.get("value")).append(".").append(OllirUtils.getOllirType("TypeInt")); break;
            case "InitializeArray": code.append("new(array, ").append(expression.getJmmChild(0).getJmmChild(0).get("value"))
                                        .append(".").append(OllirUtils.getOllirType("TypeInt")).append(").array.i32"); break;
            case "NewObject": code.append("new").append(expression.getJmmChild(0).get("name"));
            // case "AccessToArray": code.append(arg0)
            default: 
            throw new NotImplementedException("OLLIR: Expression kind not implemented: " + expression.getKind());
        }
        return 0;
    }


    private Integer stmtVisit(JmmNode stmt, Integer dummy){
        String stmtType = stmt.getKind().toString();
        switch(stmtType){
            // case "StatementBlock": break;
            // case "IfStatement": break;
            // case "WhileStatement": break;
            case "Equality": assignStmtVisit(stmt); code.append(";\n"); break; //Assignment
            case "DotExpression": expressionVisit(stmt, dummy); code.append(";\n"); break; 
            default: 
            throw new NotImplementedException("OLLIR: Statement kind not implemented: " + stmt.getKind());
        }
        return 0;
    }

    private void memberCallVisit(JmmNode memberCall){
        visit(memberCall.getJmmChild(0));
        //type missing
        System.out.println("No error1");
        if(memberCall.getJmmChild(0).getKind().equals("This")){
            code.append("invokespecial(").append("this, \"<init>");
        } else {
            code.append("invokestatic(");

            code.append(memberCall.getJmmChild(0).get("name")).append(",\"");
            code.append(memberCall.getJmmChild(1).getJmmChild(0).get("name"));
        }

        System.out.println("No error3");
        // for(int i = 1; i < memberCall.getJmmChild(1).getNumChildren(); i++){
        //     code.append("");
        // } 

        Method met1 = null;
        for (var s : symbolTable.getMethodList()){
            //System.out.println("in for");
            System.out.println(memberCall.getKind() + "-> " + memberCall.getNumChildren());
            System.out.println(memberCall.getJmmChild(1));
            System.out.println(memberCall.getJmmChild(1).getJmmChild(0));
            System.out.println("");
            if (s.getMethodSignature().equals(memberCall.getJmmChild(1).getJmmChild(0).get("name"))){
                //System.out.println("in if: "+ s.getMethodSignature());
                met1 = s;
                break;
            }
        }

        //TODO: Check Type depending on variable assigned (void if none);
        //kind of solved, added every function from the imports as V, don't know if that is correct
        if(met1 != null) code.append("\").").append(OllirUtils.getOllirType(met1.getType().getName()));
        else  code.append("\").V");

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