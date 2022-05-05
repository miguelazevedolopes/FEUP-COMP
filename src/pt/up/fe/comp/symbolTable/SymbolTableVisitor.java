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
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;


public class SymbolTableVisitor extends PreorderJmmVisitor<Boolean, Boolean>{
    
    private final SymbolTableBuilder symbolTable;
    private final List<Report> reports;


    public SymbolTableVisitor() {
        this.symbolTable = new SymbolTableBuilder();
        this.reports = new ArrayList<>();
        addVisit("ClassDeclaration", this::visitClass);
        addVisit("ImportDeclaration", this::visitImport);
    }

    public SymbolTableBuilder getSymbolTable() {
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
            if(child.getKind().contains("Id")){
                symbolTable.setClassName(child.get("name"));
            }            
            if(child.getKind().contains("Extends")){
                symbolTable.setExtends(child.getJmmChild(0).get("name"));
            }
            while(child.getKind().equals("Var")){
                boolean isArray = false;
                if(child.getNumChildren()>0 && child.getJmmChild(0).getNumChildren() > 0){
                    isArray = true;
                }
                Symbol symbol = new Symbol(new Type(child.getJmmChild(0).getKind(),isArray), child.get("name"));
                fields.add(symbol);

                if(++i >= children.size())
                    break;
                child = children.get(i);
            }
            if (child.getKind().equals("MainMethod") || child.getKind().equals("NormalMethod")) {
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
        Type type = null;
        if(symbolTable.hasMethod(name)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1,"Found duplicate method with signature "+name));
            return false;
        }
        if(name.equals("main")){
            type = new Type("void", false);
        }
        else{
            String typeName = node.getChildren().get(0).getKind();
            if(node.getChildren().get(0).getChildren().size()>0){
                type = new Type(typeName, true);
            }
            else{
                type = new Type(typeName, false);
            }
            
        }
        List<Symbol> parameters = new ArrayList<Symbol>();
        List<Symbol> localVariables = new ArrayList<Symbol>();
        for (int i = 0; i < children.size(); i++){
            JmmNode child = children.get(i);
            
            if(child.getKind().contains("Param")){
                boolean isArray = false;
                if(child.getChildren().get(0).getKind().contains("TypeArray")){
                    isArray=true;
                }
                Symbol symbol = new Symbol(new Type(child.getJmmChild(0).getKind(),isArray), child.get("name"));
                parameters.add(symbol);
            }
            if(child.getKind().contains("MethodBody")){
                localVariables = visitMethodBody(child, dummy);
            }
        }
        Method method = new Method(name,parameters,localVariables, type);
        symbolTable.addMethod(method);
        return true;

    }

    public List<Symbol> visitMethodBody(JmmNode node, Boolean dummy){
        List<JmmNode> children = node.getChildren();
        List<Symbol> localVariables = new ArrayList<Symbol>();
        for (int i = 0; i < children.size(); i++){
            JmmNode child = children.get(i);
            if(child.getKind().contains("Var")){
                System.out.println("entrou");
                boolean isArray = false;
                JmmNode varType=child.getJmmChild(0);
                Symbol symbol;
                if(varType.getKind().equals("Id")){
                    symbol = new Symbol(new Type(varType.get("name"),isArray), child.get("name"));
                }
                else{
                    symbol = new Symbol(new Type(varType.getKind(),isArray), child.get("name"));
                }
                localVariables.add(symbol);
            }
        }

        return localVariables;

    }





}
