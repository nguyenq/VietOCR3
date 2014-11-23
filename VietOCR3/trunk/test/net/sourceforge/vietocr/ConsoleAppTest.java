package net.sourceforge.vietocr;

import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConsoleAppTest {
    
    public ConsoleAppTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of main method, of class ConsoleApp.
     */
    @Test
    public void testMain() {
        System.out.println("main");
        String[] args = {"samples/vietsample.tif", "build/test/out", "-l", "vie"};
        ConsoleApp.main(args);
        assertTrue(new File("build/test/out.txt").exists());
    }
    
}
