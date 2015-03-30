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
import java.util.logging.Level;
import java.util.logging.Logger;

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
        if (args[0].equals("-?") || args[0].equals("-help") || args.length == 1 || args.length >= 8) {
            System.out.println("Usage: java -jar VietOCR.jar\n"
                    + "       (to launch the program in GUI mode)\n\n"
                    + "   or  java -jar VietOCR.jar imagefile outputfile [-l lang] [-psm pagesegmode] [hocr] [pdf]\n"
                    + "       (to execute the program in command-line mode)");
            return;
        }

        String outputFormat = "text";
        for (String arg : args) {
            if ("hocr".equals(arg)) {
                outputFormat = "hocr";
            } else if ("pdf".equals(arg)) {
                outputFormat = "pdf";
            } else if ("text+".equals(arg)) {
                outputFormat = "text+";
            }
        }

        final File imageFile = new File(args[0]);
        final File outputFile = new File(args[1]);

        if (!imageFile.exists()) {
            System.out.println("Input file does not exist.");
            logger.log(Level.SEVERE, "Input file does not exist.");
            return;
        }

        String curLangCode = "eng"; //default language
        String psm = "3"; // Fully automatic page segmentation, but no OSD (default)

        if ((args.length == 4) || (args.length == 5)) {
            if (args[2].equals("-l")) {
                curLangCode = args[3];
            } else if (args[2].equals("-psm")) {
                psm = args[3];
            }
        } else if ((args.length == 6) || (args.length == 7)) {
            curLangCode = args[3];
            psm = args[5];
            try {
                Integer.parseInt(psm);
            } catch (Exception e) {
                System.out.println("Invalid input value.");
                logger.log(Level.SEVERE, "Invalid input value.");
                return;
            }
        }

        String tessPath;

        File baseDir = Utils.getBaseDir(this);

        if (Gui.WINDOWS) {
            tessPath = new File(baseDir, Gui.TESSERACT_PATH).getPath();
        } else {
            tessPath = Gui.prefs.get("TesseractDirectory", new File(baseDir, Gui.TESSERACT_PATH).getPath());
        }

        try {
            OCRHelper.performOCR(imageFile, outputFile, tessPath, curLangCode, psm, outputFormat);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
