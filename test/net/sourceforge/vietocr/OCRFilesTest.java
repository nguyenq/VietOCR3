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

public class OCRFilesTest {

    static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
    String tessPath;
    String lang = "vie";
    OCRImageEntity entity;

    public OCRFilesTest() {
        tessPath = WINDOWS? new File(System.getProperty("user.dir"), "tesseract").getPath() : "/usr/local/bin";
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
     * Test of recognizeText method, of class OCRFiles.
     */
    @Test
    public void testRecognizeText() throws Exception {
        System.out.println("recognizeText with Tesseract executable");
        List<File> tiffFiles = entity.getClonedImageFiles();
        OCR<File> instance = new OCRFiles(tessPath);
        String expResult = "Tôi từ chinh chiến cũng ra đi";
        String result = instance.recognizeText(tiffFiles, lang);
        assertTrue(result.toLowerCase().contains(expResult.toLowerCase()));
    }
}
