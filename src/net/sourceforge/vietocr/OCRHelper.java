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
import net.sourceforge.vietocr.postprocessing.*;
import net.sourceforge.vietocr.utilities.Utilities;

public class OCRHelper {

    /**
     * Performs OCR for bulk/batch and console operations.
     *
     * @param imageFile Image file
     * @param outputFile Without extension
     * @param tessPath
     * @param langCode
     * @param pageSegMode
     * @param outputFormat
     * @throws Exception
     */
    public static void performOCR(File imageFile, File outputFile, String tessPath, String langCode, String pageSegMode, String outputFormat) throws Exception {
        File workingTiffFile = null;
        
        try {
            // create parent folder if not yet exists
            File dir = outputFile.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            boolean postprocess = "text+".equals(outputFormat);

            OCR<File> ocrEngine = new OCRFiles(tessPath);
            ocrEngine.setPageSegMode(pageSegMode);
            ocrEngine.setLanguage(langCode);
            ocrEngine.setOutputFormat(outputFormat.replace("+", ""));

            // convert PDF to TIFF
            if (imageFile.getName().toLowerCase().endsWith(".pdf")) {
                workingTiffFile = PdfUtilities.convertPdf2Tiff(imageFile);
                imageFile = workingTiffFile;
            }

            // recognize image file
            ocrEngine.processPages(imageFile, outputFile);

            // post-corrections for text+ output
            if (postprocess) {
                outputFile = new File(outputFile.getPath() + ".txt");
                String result = Utilities.readTextFile(outputFile);

                // postprocess to correct common OCR errors
                result = Processor.postProcess(result, langCode);
                // correct common errors caused by OCR
                result = TextUtilities.correctOCRErrors(result);
                // correct letter cases
                result = TextUtilities.correctLetterCases(result);

                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
                out.write(result);
                out.close();
            }
        } catch (InterruptedException ignore) {
            // ignore
        } finally {
            if (workingTiffFile != null && workingTiffFile.exists()) {
                workingTiffFile.delete();
            }
        }
    }
}
