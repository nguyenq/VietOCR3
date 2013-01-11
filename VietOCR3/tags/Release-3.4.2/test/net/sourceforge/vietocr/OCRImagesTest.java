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

    static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
    String tessPath;
    String lang = "vie";
    OCRImageEntity entity;

    public OCRImagesTest() {
        tessPath = WINDOWS? new File(System.getProperty("user.dir"), Gui.TESSERACT_PATH).getPath() : "/usr/local/bin";
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
        System.out.println("recognizeText with Tesseract API");
        List<IIOImage> images = entity.getSelectedOimages();
        OCR<IIOImage> instance = new OCRImages(tessPath);
        String expResult = "Tôi từ chinh chiến cũng ra đi";
        String result = instance.recognizeText(images, lang);
        System.out.println(result);
        assertTrue(result.toLowerCase().contains(expResult.toLowerCase()));
    }
}
