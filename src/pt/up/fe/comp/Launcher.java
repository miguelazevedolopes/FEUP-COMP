package pt.up.fe.comp;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.jasmin.JasminEmitter;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.ollir.JmmOptimizer;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        //TODO change args to accept -o
        // read the input code
        if (args.length != 1 && args.length!=2) {
            throw new RuntimeException("Expected 1 or 2 arguments, a path to an existing input file.");
        }
        int i = 0;
        boolean optimize = false;
        if(args.length == 2) {
            if(args[0].equals("-o")) optimize = true;
            i = 1;
        }
        File inputFile = new File(args[i]);
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + args[0] + "'.");
        }
        String input = SpecsIo.read(inputFile);

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(input, config);

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());
        
        // Display AST
        System.out.println("AST:\n\n" + parserResult.getRootNode().toTree());

        // Instantiate JmmAnalysis
        JmmAnalyser analyser = new JmmAnalyser();

        // Analysis stage
        JmmSemanticsResult semanticResult = analyser.semanticAnalysis(parserResult);

        // Check if there are parsing errors
        TestUtils.noErrors(semanticResult.getReports());

        JmmOptimizer optimizer = new JmmOptimizer();
        optimizer.optimize = optimize;

        // Analysis stage
        var ollirCode = optimizer.toOllir(semanticResult);

        System.out.println("--------------------------OLLIR CODE START--------------------------\n"+ ollirCode.getOllirCode() );
        System.out.println("--------------------------OLLIR CODE END----------------------------\n");

        JasminBackend jasminBackend = new JasminEmitter();

        var jasminCode = jasminBackend.toJasmin(ollirCode);

        jasminCode.compile(new File("out"));
        jasminCode.run();
        // Check if there are parsing errors
        // TestUtils.noErrors(optimizationResult);
    }

}
