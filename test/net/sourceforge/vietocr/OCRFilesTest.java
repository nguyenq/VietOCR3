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

    String tessPath = System.getProperty("user.dir") + "./tesseract";
    String lang = "vie";
    List<IIOImage> iioImageList;
    OCRImageEntity entity;

    public OCRFilesTest() {
        File selectedFile = new File("samples/vietsample1.tif");
        try {
            iioImageList = ImageIOHelper.getIIOImageList(selectedFile);
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
        System.out.println("recognizeText");
        List<File> tiffFiles = entity.getClonedImageFiles();
        OCR<File> instance = new OCRFiles(tessPath);
        String expResult = "Đôi Mẳt Người Sơn Tây";
        String result = instance.recognizeText(tiffFiles, lang);
        assertTrue(result.startsWith(expResult));
    }
}
