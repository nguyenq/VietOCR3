/**
 * Copyright 2012 Quan Nguyen
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.sourceforge.vietocr;

import java.io.*;
import java.util.List;
import javax.imageio.IIOImage;
import net.sourceforge.vietocr.postprocessing.*;
import net.sourceforge.vietocr.utilities.ImageIOHelper;

public class OCRHelper {

    public static void performOCR(File imageFile, File outputFile, String tessPath, String langCode, String pageSegMode, boolean hocr) throws Exception {
        List<File> tempTiffFiles = null;

        try {
            OCR<File> ocrEngine = new OCRFiles(tessPath);
            ocrEngine.setPageSegMode(pageSegMode);
            ocrEngine.setHocr(hocr);
            List<IIOImage> iioImageList = ImageIOHelper.getIIOImageList(imageFile);
            tempTiffFiles = ImageIOHelper.createTiffFiles(iioImageList, -1);
            String result = ocrEngine.recognizeText(tempTiffFiles, langCode);
            
            // skip post-corrections if hocr output
            if (!hocr) {
                // postprocess to correct common OCR errors
                result = Processor.postProcess(result, langCode);
                // correct common errors caused by OCR
                result = TextUtilities.correctOCRErrors(result);
                // correct letter cases
                result = TextUtilities.correctLetterCases(result);
            }
            
            // create parent folder if not yet exists
            File dir = outputFile.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
            out.write(result);
            out.close();
        } catch (InterruptedException ignore) {
            // ignore
        } finally {
            //clean up working files
            if (tempTiffFiles != null) {
                for (File f : tempTiffFiles) {
                    f.delete();
                }
            }
        }
    }
}
