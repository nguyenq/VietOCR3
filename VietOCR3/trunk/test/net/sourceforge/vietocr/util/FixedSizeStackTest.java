package net.sourceforge.vietocr.util;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class FixedSizeStackTest {

    public FixedSizeStackTest() {
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
     * Test of push method, of class FixedSizeStack.
     */
    @Test
    public void testPush() {
        System.out.println("push");
        String expected = "9";
        FixedSizeStack<String> instance = new FixedSizeStack<String>(5);
        for (int i = 0; i < 10; i++) {
            instance.push(String.valueOf(i));
        }

        String actual = instance.pop();
        assertEquals(expected, actual);
    }
}
