package pt.up.fe.comp;
import org.specs.comp.ollir.ClassUnit;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;


public class BackendStage implements JasminBackend{


    /**
     * * Converts the OLLIR to Jasmin Bytecodes with optimizations performed at the AST level and at the OLLIR
     * level.<br>
     * 
     * @param ollirResult
     * @return
     */
    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();

        StringBuilder jasminCode = new StringBuilder();

        
        jasminCode.append(".class public ").append(ollirClass.getClassName());
        jasminCode.append("\n.super ");

        return null;
    }
    


    
}
