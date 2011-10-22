package net.sourceforge.vietocr.postprocessing;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class ViePPTest {

    public ViePPTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of postProcess method, of class ViePP.
     */
    @Test
    public void testPostProcess() {
        System.out.println("postProcess");
        String text = "uơn";
        ViePP instance = new ViePP();
        String expResult = "ươn";
        String result = instance.postProcess(text);
        assertEquals(expResult, result);
    }
    /**
     * Test of postProcess method, of class ViePP.
     */
    @Test
    public void testPostProcess1() {
        System.out.println("postProcess");
        String text = "ưon";
        ViePP instance = new ViePP();
        String expResult = "ươn";
        String result = instance.postProcess(text);
        assertEquals(expResult, result);
    }
        /**
     * Test of postProcess method, of class ViePP.
     */
    @Test
    public void testPostProcess2() {
        System.out.println("postProcess");
        String text = "‘ê ‘ổ ‘ô";
        ViePP instance = new ViePP();
        String expResult = "ề ‘ổ ồ";
        String result = instance.postProcess(text);
        assertEquals(expResult, result);
    }
        /**
     * Test of postProcess method, of class ViePP.
     */
    @Test
    public void testPostProcess3() {
        System.out.println("postProcess");
        String text = "ê’ ê’n ậ’n";
        ViePP instance = new ViePP();
        String expResult = "ế ến ậ’n";
        String result = instance.postProcess(text);
        assertEquals(expResult, result);
    }
}