import static org.junit.Assert.assertEquals;
import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jasmin.JasminEmitter;
import pt.up.fe.comp.jasmin.OllirToJasmin;
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
        
        jasminResult.compile();
        //return new JasminResult(ollirResult, jasminCode, Collections.emptyList());
    }

    @Test
    public void testRunHelloWorld() {

        String jasminCode = SpecsIo.getResource("fixtures/public/jasmin/HelloWorld.j");
        var output = TestUtils.runJasmin(jasminCode);
        assertEquals("Hello World!\nHello World Again!\n", SpecsStrings.normalizeFileContents(output));
    }
}
