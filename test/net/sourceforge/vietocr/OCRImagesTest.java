/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sourceforge.vietocr;

import java.io.File;
import java.util.List;
import javax.imageio.IIOImage;
import org.junit.*;
import static org.junit.Assert.*;

public class OCRImagesTest {

    String tessPath = System.getProperty("user.dir") + "./tesseract";
    String lang = "vie";
    OCRImageEntity entity;

    public OCRImagesTest() {
        File selectedFile = new File("samples/vietsample1.tif");
        try {
            List<IIOImage> iioImageList = ImageIOHelper.getIIOImageList(selectedFile);
            entity = new OCRImageEntity(iioImageList, -1, null, "vie");
        } catch (Exception e) {
        }
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
     * Test of recognizeText method, of class OCRImages.
     */
    @Test
    public void testRecognizeText() throws Exception {
        System.out.println("recognizeText");
        List<IIOImage> images = entity.getSelectedOimages();
        OCR<IIOImage> instance = new OCRImages(tessPath);
        String expResult = "Đôi Mắt Người Sơn Tây";
        String result = instance.recognizeText(images, lang);
        assertTrue(result.startsWith(expResult));
    }
}
