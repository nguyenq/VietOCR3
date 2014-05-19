package net.sourceforge.vietocr;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.vietocr.util.Utils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class OCRHelperTest {

    static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
    String tessPath;

    public OCRHelperTest() {
        tessPath = WINDOWS ? new File(System.getProperty("user.dir"), Gui.TESSERACT_PATH).getPath() : "/usr/local/bin";
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
     * Test of performOCR method, of class OCRHelper.
     */
    @Test
    public void testPerformOCR() throws Exception {
        System.out.println("performOCR");
        String langCode = "vie";
        String pageSegMode = "3";
        String outputFormat = "pdf";
        String inputFolder = "C:\\Temp\\Images";
        String outputFolder = "C:\\Temp\\Output";
        List<File> files = new ArrayList<File>();
        Utils.listImageFiles(files, new File(inputFolder));
        
        for (File imageFile : files) {
            try {
                System.out.println("Process " + imageFile.getPath());
                String outputFilename = imageFile.getPath().substring(inputFolder.length() + 1);
                OCRHelper.performOCR(imageFile, new File(outputFolder, outputFilename), tessPath, langCode, pageSegMode, outputFormat);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        
        assertTrue(new File(outputFolder).list().length > 0);
    }
}
