package pt.up.fe.comp.symbolTable;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Method;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;


public class SymbolTableVisitor extends PreorderJmmVisitor<Boolean, Boolean>{
    
    private final SymbolTable symbolTable;
    private final List<Report> reports;


    public SymbolTableVisitor() {
        this.symbolTable = new SymbolTable();
        this.reports = new ArrayList<>();
        addVisit("ClassDeclaration", this::visitClass);
        addVisit("ImportDeclaration", this::visitImport);

    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public List<Report> getReports() {
        return reports;
    }

    public Boolean visitClass(JmmNode node, Boolean dummy){
        List<JmmNode> children = node.getChildren();
        List<Symbol> fields = new ArrayList<Symbol>();
        for (int i = 0; i < children.size(); i++){
            JmmNode child = children.get(i);
            String childKind = child.getKind();
            if(childKind.contains("Id")){
                symbolTable.setClassName(child.get("name"));
            }
            System.out.println(child);
            while(child.getKind().equals("Var")){
                boolean isArray = false;
                if(child.getJmmChild(0).getNumChildren() > 0){
                    isArray = true;
                }
                System.out.println("Addded field");
                Symbol symbol = new Symbol(new Type(child.getJmmChild(0).getKind(),isArray), child.get("name"));
                fields.add(symbol);

                if(++i >= children.size())
                    break;
                child = children.get(i);
            }
            if (childKind.equals("MainMethod") || childKind.equals("NormalMethod")) {
                visitMethod(child, dummy);
            
            }
        }

        symbolTable.setFields(fields);
        return true;
    }

    public Boolean visitImport(JmmNode node, Boolean dummy){
        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children){
            symbolTable.addImport(child.get("name"));
        }

        return true;

    }

    public Boolean visitMethod(JmmNode node, Boolean dummy){
        List<JmmNode> children = node.getChildren();
        String name = node.get("name");
        String signature = null;
        Type type = null;
        if(name.equals("main")){
            signature = "void";
            type = new Type("void", false);
        }
        List<Symbol> parameters = new ArrayList<Symbol>();
        List<Symbol> localVariables = new ArrayList<Symbol>();
        for (int i = 0; i < children.size(); i++){
            JmmNode child = children.get(i);
            if(child.getKind().contains("Boolean") || child.getKind().contains("Int") || child.getKind().contains("String") || child.getKind().contains("Void")){
                boolean isArray = false;
                if(child.getChildren() == null)
                    isArray = true;
                type = new Type(child.getKind(), isArray);
                signature = child.getKind();

            }

            while(child.getKind().contains("Param")){
                boolean isArray = false;
                if(child.getJmmChild(0).getJmmChild(0) != null){
                    isArray = true;
                }
                Symbol symbol = new Symbol(new Type(child.getJmmChild(0).getKind(),isArray), child.get("name"));
                parameters.add(symbol);

                if(++i >= children.size())
                    break;
                child = children.get(i);
            }
            if(child.getKind().contains("MethodBody")){
                localVariables = visitMethodBody(child, dummy);
            }
        }
        Method method = new Method(signature,parameters,localVariables, type);
        symbolTable.addMethod(method);
        return true;

    }

    public List<Symbol> visitMethodBody(JmmNode node, Boolean dummy){
        List<JmmNode> children = node.getChildren();
        List<Symbol> localVariables = new ArrayList<Symbol>();
        for (int i = 0; i < children.size(); i++){
            JmmNode child = children.get(i);
            while(child.getKind().contains("Var")){
                boolean isArray = false;
                Symbol symbol = new Symbol(new Type(child.getJmmChild(0).getKind(),isArray), child.get("name"));
                localVariables.add(symbol);
                if(++i >= children.size())
                    break;
                child = children.get(i);
            }
        }

        return localVariables;

    }





}
