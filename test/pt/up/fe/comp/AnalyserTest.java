package pt.up.fe.comp;

import org.junit.Test;
import pt.up.fe.specs.util.SpecsIo;

public class AnalyserTest {
    
    private static void noErrors(String code) {
        var result = TestUtils.analyse(code);
        TestUtils.noErrors(result);
    }


    /* 
     * Code that must be successfully parsed 
     */

    // @Test
    // public void helloWorld() {
    //     noErrors(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
    // }

}
