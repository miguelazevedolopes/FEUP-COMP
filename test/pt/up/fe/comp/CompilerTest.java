package pt.up.fe.comp;


import org.junit.Test;
import pt.up.fe.comp.jasmin.JasminEmitter;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.ollir.JmmOptimizer;
import pt.up.fe.specs.util.SpecsIo;

import java.util.HashMap;
import java.util.Map;


public class CompilerTest {

    public void runFile(String filepath){
        String input = SpecsIo.getResource(filepath);
        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(input, config);

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        // Instantiate JmmAnalysis
        JmmAnalyser analyser = new JmmAnalyser();

        // Analysis stage
        JmmSemanticsResult semanticResult = analyser.semanticAnalysis(parserResult);

        // Check if there are parsing errors
        TestUtils.noErrors(semanticResult.getReports());

        JmmOptimizer optimizer = new JmmOptimizer();
        // Analysis stage
        var ollirResult = optimizer.toOllir(semanticResult);

        TestUtils.noErrors(ollirResult.getReports());

        JasminEmitter emitter = new JasminEmitter();

        JasminResult jasminResult = emitter.toJasmin(ollirResult);
        TestUtils.noErrors(jasminResult.getReports());

        jasminResult.run();
    }


    @Test
    public void helloWord(){
        runFile("fixtures/public/HelloWorld.jmm");
    }

    @Test
    public void FindMaximum(){
        runFile("fixtures/public/FindMaximum.jmm");
    }

    @Test
    public void LazySort(){
        runFile("fixtures/public/LazySort.jmm");
    }

    @Test
    public void Life(){
        runFile("fixtures/public/Life.jmm");
    }

    @Test
    public void MonteCarloPi(){
        runFile("fixtures/public/MonteCarloPi.jmm");
    }

    @Test
    public void QuickSort(){
        runFile("fixtures/public/QuickSort.jmm");
    }

    @Test
    public void Simple(){
        runFile("fixtures/public/Simple.jmm");
    }

    @Test
    public void TicTacToe(){
        runFile("fixtures/public/QuickSort.jmm");
    }

    @Test
    public void WhileAndIf(){
        runFile("fixtures/public/QuickSort.jmm");
    }



}
