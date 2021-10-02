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
import java.util.Arrays;
import javax.imageio.IIOImage;
import net.sourceforge.lept4j.util.LeptUtils;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.util.ImageIOHelper;
import net.sourceforge.tess4j.util.PdfUtilities;
import static net.sourceforge.vietocr.OCR.MINIMUM_DESKEW_THRESHOLD;
import net.sourceforge.vietocr.postprocessing.Processor;
import net.sourceforge.vietocr.postprocessing.TextUtilities;
import net.sourceforge.vietocr.util.Utils;

public class OCRHelper {

    /**
     * Performs OCR for bulk/batch and console operations.
     *
     * @param imageFile Image file
     * @param outputFile Without extension
     * @param tessdataPath path to Tesseract <code>tessdata</code> directory
     * @param langCode language code
     * @param pageSegMode page segmentation mode
     * @param outputFormats formats of output file. Possible values:
     * <code>text</code>, <code>hocr</code>, or <code>pdf</code>
     * @param options Processing options
     * @throws Exception
     */
    public static void performOCR(File imageFile, File outputFile, String tessdataPath, String langCode, String pageSegMode, String outputFormats, ProcessingOptions options) throws Exception {
        // create parent folder if not yet exists
        File dir = outputFile.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }

        OCR<IIOImage> ocrEngine = new OCRImages();
        ocrEngine.setDatapath(tessdataPath);
        ocrEngine.setPageSegMode(pageSegMode);
        ocrEngine.setLanguage(langCode);
        ocrEngine.setOutputFormats(outputFormats);
        ocrEngine.setProcessingOptions(options);

        File workingTiffFile = null;
        File deskewedImageFile = null;
        File linesRemovedImageFile = null;

        try {
            // convert PDF to TIFF
            if (imageFile.getName().toLowerCase().endsWith(".pdf")) {
                workingTiffFile = PdfUtilities.convertPdf2Tiff(imageFile);
                imageFile = workingTiffFile;
            }

            // deskew
            if (options.isDeskew()) {
                deskewedImageFile = ImageIOHelper.deskewImage(imageFile, MINIMUM_DESKEW_THRESHOLD);
                imageFile = deskewedImageFile;
            }

            // remove lines
            if (options.isRemoveLines()) {
                String outfile = LeptUtils.removeLines(imageFile.getPath());
                linesRemovedImageFile = new File(outfile);
                if (linesRemovedImageFile.length() == 0) {
                    linesRemovedImageFile.delete();
                    linesRemovedImageFile = null;
                } else {
                    imageFile = linesRemovedImageFile;
                }
            }

            // recognize image file
            ocrEngine.processPages(imageFile, outputFile);

            // post-corrections for text output
            if (Arrays.asList(outputFormats.split(",")).contains(ITesseract.RenderedFormat.TEXT.name())) {
                if (options.isPostProcessing() || options.isCorrectLetterCases() || options.isRemoveLineBreaks()) {
                    outputFile = new File(outputFile.getPath() + ".txt");
                    String result = Utils.readTextFile(outputFile);

                    // postprocess to correct common OCR errors
                    if (options.isPostProcessing()) {
                        result = Processor.postProcess(result, langCode);
                    }

                    // correct letter cases
                    if (options.isCorrectLetterCases()) {
                        result = TextUtilities.correctLetterCases(result);
                    }

                    // remove line breaks
                    if (options.isRemoveLineBreaks()) {
                        result = net.sourceforge.vietpad.utilities.TextUtilities.removeLineBreaks(result, options.isRemoveHyphens());
                    }

                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
                    out.write(result);
                    out.close();
                }
            }
        } finally {
            if (workingTiffFile != null && workingTiffFile.exists()) {
                workingTiffFile.delete();
            }

            if (deskewedImageFile != null && deskewedImageFile.exists()) {
                deskewedImageFile.delete();
            }

            if (linesRemovedImageFile != null && linesRemovedImageFile.exists()) {
                linesRemovedImageFile.delete();
            }
        }
    }
}
