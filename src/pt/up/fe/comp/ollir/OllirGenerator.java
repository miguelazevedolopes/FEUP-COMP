import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode; 

public class OllirGenerator extends AJmmVisitor<Integer, Integer> {
    private final StringBuilder code;
    private final SymbolTable symbolTable;

    public OllirGenerator(SymbolTable symbolTable){
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;

        addVisit(AstNode.PROGRAM, this::programVisit);
        addVisit(AstNode.CLASS_DECL, this::ClassDeclVisit);
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

     private Integer ClassDeclVisit(JmmNode program, Integer dummy){
         code.append("public ").append(symbolTable.getClassName());
         var superClass = symbolTable.getSuper();
         if (superClass != null) code.append(" extends ").append(superClass);
        code.append("{\n");
         return 0;
     }
}