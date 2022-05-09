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

    @Test
    public void helloWorld() {
        noErrors(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
    }

    @Test
    public void findMaximum() {
        noErrors(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"));
    }

    // @Test
    // public void lazysort() {
    //     noErrors(SpecsIo.getResource("fixtures/public/Lazysort.jmm"));
    // }

    @Test
    public void life() {
        noErrors(SpecsIo.getResource("fixtures/public/Life.jmm"));
    }

    // @Test
    // public void quickSort() {
    //     noErrors(SpecsIo.getResource("fixtures/public/QuickSort.jmm"));
    // }

    @Test
    public void simple() {
        noErrors(SpecsIo.getResource("fixtures/public/Simple.jmm"));
    }

    @Test
    public void ticTacToe() {
        noErrors(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"));
    }

    @Test
    public void whileAndIf() {
        noErrors(SpecsIo.getResource("fixtures/public/WhileAndIf.jmm"));
    }


}
