package net.sourceforge.vietocr.util;

import net.sourceforge.vietocr.util.ImageHelper;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

public class ImageHelperTest {
    File file = new File("samples/vietsample2.png");
    
    public ImageHelperTest() {
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
     * Test of brighten method, of class ImageHelper.
     */
    @Test
    @Ignore("not ready yet")
    public void testBrighten() {
        System.out.println("brighten");
        BufferedImage src = null;
        float offset = 0.0F;
        BufferedImage expResult = null;
        BufferedImage result = ImageHelper.brighten(src, offset);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of contrast method, of class ImageHelper.
     */
    @Test
    @Ignore("not ready yet")
    public void testContrast() {
        System.out.println("contrast");
        BufferedImage src = null;
        float scaleFactor = 0.0F;
        BufferedImage expResult = null;
        BufferedImage result = ImageHelper.contrast(src, scaleFactor);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of autoCrop method, of class ImageHelper.
     * @throws java.io.IOException
     */
    @Test
    public void testAutoCrop() throws IOException {
        System.out.println("autoCrop");
        BufferedImage source = ImageIO.read(file);
        Rectangle expResult = new Rectangle(2275, 2997);
        BufferedImage result = ImageHelper.autoCrop(source);
        //verify dimension
        assertEquals(expResult, new Rectangle(result.getWidth(), result.getHeight()));
    }

    /**
     * Test of sharpen method, of class ImageHelper.
     */
    @Test
    @Ignore("not ready yet")
    public void testSharpen() {
        System.out.println("sharpen");
        BufferedImage image = null;
        BufferedImage expResult = null;
        BufferedImage result = ImageHelper.sharpen(image);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of smoothen method, of class ImageHelper.
     */
    @Test
    @Ignore("not ready yet")
    public void testSmoothen() {
        System.out.println("smoothen");
        BufferedImage image = null;
        BufferedImage expResult = null;
        BufferedImage result = ImageHelper.smoothen(image);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}
