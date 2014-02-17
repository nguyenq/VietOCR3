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

import java.io.*;
import java.util.*;
import net.sourceforge.vietocr.utilities.Utilities;

/**
 * Invokes Tesseract executable via command-line.
 */
public class OCRFiles extends OCR<File> {

    private final String LANG_OPTION = "-l";
    private final String PSM_OPTION = "-psm";
    private final String HOCR_OPTION = "hocr";
    private final String PDF_OPTION = "pdf";
    private final String EOL = "\n";
    private final String tessPath;
    final static String OUTPUT_FILE_NAME = "TessOutput";
    final static String TEXTFILE_EXTENSION = ".txt";
    final static String HTMLFILE_EXTENSION = ".html";
    final static String PDFFILE_EXTENSION = ".pdf";

    /**
     * Creates a new instance of OCR
     */
    public OCRFiles(String tessPath) {
        this.tessPath = tessPath;
    }

    /**
     * @param tiffFiles
     * @return
     * @throws java.lang.Exception
     */
    @Override
    public String recognizeText(final List<File> tiffFiles) throws Exception {
        String outputFormat = this.getOutputFormat();
        File tempTessOutputFile = File.createTempFile(OUTPUT_FILE_NAME, outputFormat == HOCR_OPTION ? HTMLFILE_EXTENSION : outputFormat == PDF_OPTION ? PDFFILE_EXTENSION : TEXTFILE_EXTENSION);
        String outputFileName = Utilities.stripExtension(tempTessOutputFile.getPath()); // chop the .html/.txt extension

        List<String> cmd = new ArrayList<String>();
        cmd.add(tessPath + "/tesseract");
        cmd.add(""); // placeholder for inputfile
        cmd.add(outputFileName);
        cmd.add(LANG_OPTION);
        cmd.add(this.getLanguage());
        cmd.add(PSM_OPTION);
        cmd.add(String.valueOf(getPageSegMode()));
        if ("hocr".equals(outputFormat)) {
            cmd.add(HOCR_OPTION);
        } else if ("pdf".equals(outputFormat)) {
            cmd.add(PDF_OPTION);
        }
        
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(tessPath));
        pb.redirectErrorStream(true);

        StringBuilder result = new StringBuilder();

        for (File tiffFile : tiffFiles) {
            cmd.set(1, tiffFile.getPath());
            pb.command(cmd);
            System.out.println(cmd);
            Process process = pb.start();
            // any error message?
            // this has become unneccesary b/c the standard error is already merged with the standard output
//            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream());
//            errorGobbler.start();
            // any output?
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());
            outputGobbler.start();

            int w = process.waitFor();
            System.out.println("Exit value = " + w);

            if (w == 0) {
                if ("hocr".equals(outputFormat) || "pdf".equals(outputFormat)) {
                    tempTessOutputFile.renameTo(tiffFile);
                    
                    return "";
                }
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tempTessOutputFile), "UTF-8"));

                String str;

                while ((str = in.readLine()) != null) {
                    result.append(str).append(EOL);
                }

                int length = result.length();
                if (length >= EOL.length()) {
                    result.setLength(length - EOL.length()); // remove last EOL
                }
                in.close();
            } else {
                tempTessOutputFile.delete();
                String msg = outputGobbler.getMessage(); // get actual message from the engine;
                if (msg.trim().length() == 0) {
                    msg = "Errors occurred.";
                }
                throw new RuntimeException(msg);
            }
        }

        tempTessOutputFile.delete();
        return result.toString();
    }

    @Override
    public void processPages(File inputImage, File outputFile) throws Exception {
        String outputFormat = this.getOutputFormat();
        List<String> cmd = new ArrayList<String>();
        cmd.add(tessPath + "/tesseract");
        cmd.add(inputImage.getPath());
        cmd.add(outputFile.getPath());
        cmd.add(LANG_OPTION);
        cmd.add(this.getLanguage());
        if (!"pdf".equals(outputFormat)) {
            cmd.add(PSM_OPTION);
            cmd.add(String.valueOf(getPageSegMode()));
        }
        
        if ("hocr".equals(outputFormat)) {
            cmd.add(HOCR_OPTION);
        } else if ("pdf".equals(outputFormat)) {
            cmd.add(PDF_OPTION);
        }
        
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(tessPath));
        pb.redirectErrorStream(true);
        pb.command(cmd);
        System.out.println(cmd);
        Process process = pb.start();

        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());
        outputGobbler.start();

        int w = process.waitFor();
        System.out.println("Exit value = " + w);

        if (w == 0) {

        }
    }
}

/**
 * When Runtime.exec() won't.
 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html
 */
class StreamGobbler extends Thread {

    InputStream is;
    StringBuilder outputMessage = new StringBuilder();

    StreamGobbler(InputStream is) {
        this.is = is;
    }

    String getMessage() {
        return outputMessage.toString();
    }

    @Override
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                outputMessage.append(line).append("\n");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
