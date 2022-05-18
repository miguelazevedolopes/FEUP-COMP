import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jasmin.JasminEmitter;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsStrings;

public class JasminTest {
    @Test
    public void testCompileHelloWorld(){

        String ollirCode =  SpecsIo.getResource("fixtures/public/ollir/HelloWorld.ollir");

        OllirResult ollirResult = new OllirResult(ollirCode, null);
        JasminResult jasminResult = new JasminEmitter().toJasmin(ollirResult);

        System.out.println("-----Jasmin Code-----\n\n" + jasminResult.getJasminCode());
        TestUtils.noErrors(jasminResult);
        jasminResult.compile();
        //return new JasminResult(ollirResult, jasminCode, Collections.emptyList());
    }

    @Test
    public void testRunHelloWorld() throws IOException{
        Path path = Paths.get("test/fixtures/public/jasmin/HelloWorld2.j");
        
        String ollirCode =  SpecsIo.getResource("fixtures/public/ollir/HelloWorld.ollir");

        OllirResult ollirResult = new OllirResult(ollirCode, null);
        JasminResult jasminResult = new JasminEmitter().toJasmin(ollirResult);

        Files.writeString(path, jasminResult.getJasminCode());
        
        new JasminResult(SpecsIo.getResource("fixtures/public/jasmin/HelloWorld2.j")).run();

        //return new JasminResult(ollirResult, jasminCode, Collections.emptyList());
    }

    @Test
    public void testRunFac() throws IOException{
        Path path = Paths.get("test/fixtures/public/jasmin/Fac.j");
        
        String ollirCode =  SpecsIo.getResource("fixtures/public/ollir/Fac.ollir");

        OllirResult ollirResult = new OllirResult(ollirCode, null);
        JasminResult jasminResult = new JasminEmitter().toJasmin(ollirResult);

        Files.writeString(path, jasminResult.getJasminCode());
        

        jasminResult.run();

        //return new JasminResult(ollirResult, jasminCode, Collections.emptyList());
    }


    @Test
    public void testRunClass1() throws IOException{
        Path path = Paths.get("test/fixtures/public/jasmin/myclass1.j");
        
        String ollirCode =  SpecsIo.getResource("fixtures/public/ollir/myclass1.ollir");

        OllirResult ollirResult = new OllirResult(ollirCode, null);
        JasminResult jasminResult = new JasminEmitter().toJasmin(ollirResult);

        Files.writeString(path, jasminResult.getJasminCode());
        
        TestUtils.noErrors(jasminResult);
        jasminResult.run();

        //return new JasminResult(ollirResult, jasminCode, Collections.emptyList());
    }
    @Test
    public void OllirToJasminBasic() {
        
        String ollirCode =  SpecsIo.getResource("fixtures/public/cp2/OllirToJasminBasic.ollir");

        OllirResult ollirResult = new OllirResult(ollirCode, null);
        JasminResult jasminResult = new JasminEmitter().toJasmin(ollirResult);

        
        TestUtils.noErrors(jasminResult);
        jasminResult.run();
    }

    @Test
    public void OllirToJasminFields() {
        
        String ollirCode =  SpecsIo.getResource("fixtures/public/cp2/OllirToJasminFields.ollir");

        OllirResult ollirResult = new OllirResult(ollirCode, null);
        JasminResult jasminResult = new JasminEmitter().toJasmin(ollirResult);

        
        TestUtils.noErrors(jasminResult);
        jasminResult.run();
    }

    @Test
    public void testRunJasmin() {

        String jasminCode = SpecsIo.getResource("fixtures/public/jasmin/HelloWorld.j");
        var output = TestUtils.runJasmin(jasminCode);
        assertEquals("Hello World!\nHello World Again!\n", SpecsStrings.normalizeFileContents(output));
    }

    //fixtures/public/cp2/OllirToJasminFields.ollir

    //"fixtures/public/cp2/OllirToJasminBasic.ollir"
}
