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
import javax.imageio.IIOImage;

public class OCRHelper {

    /**
     * Performs OCR for bulk/batch and console operations.
     *
     * @param imageFile Image file
     * @param outputFile Without extension
     * @param tessdataPath path to Tesseract <code>tessdata</code> directory
     * @param langCode language code
     * @param pageSegMode page segmentation mode
     * @param outputFormat format of output file. Possible values:
     * <code>text</code>, <code>text+</code> (with post-corrections),
     * <code>hocr</code>
     * @param options Processing options
     * @throws Exception
     */
    public static void performOCR(File imageFile, File outputFile, String tessdataPath, String langCode, String pageSegMode, String outputFormat, ProcessingOptions options) throws Exception {
        // create parent folder if not yet exists
        File dir = outputFile.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }

        OCR<IIOImage> ocrEngine = new OCRImages();
        ocrEngine.setDatapath(tessdataPath);
        ocrEngine.setPageSegMode(pageSegMode);
        ocrEngine.setLanguage(langCode);
        ocrEngine.setOutputFormat(outputFormat);
        ocrEngine.setProcessingOptions(options);

        // recognize image file
        ocrEngine.processPages(imageFile, outputFile);
    }
}
