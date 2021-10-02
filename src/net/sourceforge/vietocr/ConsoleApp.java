/**
 * Copyright @ 2008 Quan Nguyen
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

import net.sourceforge.vietocr.util.Utils;
import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
//import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.tess4j.ITesseract;

public class ConsoleApp {

    private final static Logger logger = Logger.getLogger(ConsoleApp.class.getName());

    public static void main(String[] args) {
        new ConsoleApp().performOCR(args);
    }

    /**
     * Performs OCR on input image file.
     *
     * @param args
     */
    private void performOCR(String[] args) {
        if (args[0].equals("-?") || args[0].equals("-help") || args.length == 1) {
            System.out.println("Usage: java -jar VietOCR.jar\n"
                    + "       (to launch the program in GUI mode)\n\n"
                    + "   or  java -jar VietOCR.jar imagefile outputfile [-l lang] [--psm pagesegmode] [text|hocr|pdf|pdf_textonly|unlv|box|alto|tsv|lstmbox|wordstrbox] [postprocessing] [correctlettercases] [deskew] [removelines] [removelinebreaks]\n"
                    + "       (to execute the program in command-line mode)");
            return;
        }

        final File imageFile = new File(args[0]);
        final File outputFile = new File(args[1]);

        if (!imageFile.exists()) {
            System.out.println("Input file does not exist.");
//            logger.log(Level.SEVERE, "Input file does not exist.");
            return;
        }

        ProcessingOptions options = new ProcessingOptions();

        Set<String> outputFormatSet = new HashSet<String>();
        List<String> renderers = Arrays.asList(Utils.getNames(ITesseract.RenderedFormat.class));
        String curLangCode = "eng"; //default language
        String psm = "3"; // Fully automatic page segmentation, but no OSD (default)

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            // command-line options
            if ("-l".equals(arg)) {
                if ((i + 1) < args.length) {
                    curLangCode = args[i + 1];
                }
            }

            if ("--psm".equals(arg)) {
                if ((i + 1) < args.length) {
                    psm = args[i + 1];
                    try {
                        short psmval = Short.parseShort(psm);
                        if (psmval > 13) {
                            throw new IllegalArgumentException();
                        }
                    } catch (Exception e) {
                        System.out.println("Invalid input value for PSM.");
//                        logger.log(Level.SEVERE, "Invalid input value for PSM.");
                        return;
                    }
                }
            }

            // parse output formats
            if (renderers.contains(arg.toUpperCase())) {
                outputFormatSet.add(arg.toUpperCase());
            }

            // enable pre-processing
            if ("deskew".equals(arg)) {
                options.setDeskew(true);
            }
            if ("removelines".equals(arg)) {
                options.setRemoveLines(true);
            }

            // enable post-processing
            if ("postprocessing".equals(arg)) {
                options.setPostProcessing(true);
            }
            if ("correctlettercases".equals(arg)) {
                options.setCorrectLetterCases(true);
            }
            if ("removelinebreaks".equals(arg)) {
                options.setRemoveLineBreaks(true);
            }
        }

        if (outputFormatSet.isEmpty()) {
            outputFormatSet.add(ITesseract.RenderedFormat.TEXT.toString());
        }

        String outputFormats = String.join(",", outputFormatSet);

        String tessdataPath = Gui.getDatapath(Utils.getBaseDir(ConsoleApp.this));

        try {
            OCRHelper.performOCR(imageFile, outputFile, tessdataPath, curLangCode, psm, outputFormats, options);
        } catch (Exception e) {
            System.out.println(e.getMessage());
//            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
