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
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.vietocr.util.Utils;

/**
 * Invokes Tesseract executable via command-line.
 */
public class OCRFiles extends OCR<File> {

    private final String LANG_OPTION = "-l";
    private final String PSM_OPTION = "-psm";
    private final String CONFIGVAR_OPTION = "-c";
    final static String OUTPUT_FILE_NAME = "TessOutput";
    final static String TEXTFILE_EXTENSION = ".txt";
    private final String tessPath;

    /**
     * Creates a new instance of OCR
     *
     * @param tessPath
     */
    public OCRFiles(String tessPath) {
        this.tessPath = tessPath;
    }

    /**
     * Recognizes TIFF files.
     *
     * @param tiffFiles
     * @param inputfilename input filename; not used.
     * @return recognized text
     * @throws Exception
     */
    @Override
    public String recognizeText(List<File> tiffFiles, String inputfilename) throws Exception {
        File tempTessOutputFile = File.createTempFile(OUTPUT_FILE_NAME, TEXTFILE_EXTENSION);
        String outputFileName = Utils.stripExtension(tempTessOutputFile.getPath()); // chop the file extension

        List<String> cmd = new ArrayList<String>();
        cmd.add(tessPath + "/tesseract");
        cmd.add(""); // placeholder for inputfile
        cmd.add(outputFileName);
        cmd.add(LANG_OPTION);
        cmd.add(language);
        cmd.add(PSM_OPTION);
        cmd.add(pageSegMode);
        controlParameters(cmd);

        File configsFilePath = new File(datapath, CONFIG_PATH + CONFIGS_FILE);
        if (configsFilePath.exists()) {
            cmd.add(CONFIGS_FILE);
        }

        ProcessBuilder pb = new ProcessBuilder();
        if (new File(datapath, TESSDATA).exists()) {
            Map<String, String> env = pb.environment();
            env.put("TESSDATA_PREFIX", datapath);
        }

        pb.directory(new File(tessPath));
        pb.redirectErrorStream(true);

        StringBuilder result = new StringBuilder();

        for (File tiffFile : tiffFiles) {
            cmd.set(1, tiffFile.getPath());
            pb.command(cmd);
//            System.out.println(cmd);
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
                String str = Utils.readTextFile(tempTessOutputFile);
                result.append(str);
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

    /**
     * Processes OCR for input file with specified output format.
     *
     * @param inputImage
     * @param outputFile
     * @throws Exception
     */
    @Override
    public void processPages(File inputImage, File outputFile) throws Exception {
        List<String> cmd = new ArrayList<String>();
        cmd.add(tessPath + "/tesseract");
        cmd.add(inputImage.getAbsolutePath());
        cmd.add(outputFile.getAbsolutePath());
        cmd.add(LANG_OPTION);
        cmd.add(language);
        cmd.add(PSM_OPTION);
        cmd.add(pageSegMode);
        controlParameters(cmd);
        File configsFilePath = new File(datapath, CONFIG_PATH + CONFIGS_FILE);
        if (configsFilePath.exists()) {
            cmd.add(CONFIGS_FILE);
        }

        if ("hocr".equals(outputFormat) || "pdf".equals(outputFormat)) {
            cmd.add(outputFormat);
        }

        ProcessBuilder pb = new ProcessBuilder();
//        if (new File(datapath, TESSDATA).exists()) {
//            Map<String, String> env = pb.environment();
//            env.put("TESSDATA_PREFIX", datapath);
//        }
        pb.directory(new File(tessPath));
        pb.redirectErrorStream(true);
        pb.command(cmd);
//        System.out.println(cmd);
        Process process = pb.start();

        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());
        outputGobbler.start();

        int w = process.waitFor();
        System.out.println("Exit value = " + w);

        if (w != 0) {
            String msg = outputGobbler.getMessage(); // get actual message from the engine;
            if (msg.trim().length() == 0) {
                msg = "Errors occurred.";
            }
            throw new RuntimeException(msg);
        }
    }

    /**
     * Reads <code>tessdata/configs/tess_configvars</code> and
     * <code>setVariable</code> on Tesseract engine. This only works for
     * non-init parameters (@see
     * <a href="https://code.google.com/p/tesseract-ocr/wiki/ControlParams">ControlParams</a>).
     *
     * @param instance
     */
    void controlParameters(List<String> cmd) throws Exception {
        File configvarsFilePath = new File(getDatapath(), CONFIG_PATH + CONFIGVARS_FILE); // Note: On Linux, this is under TESSDATA_PREFIX dir 
        if (!configvarsFilePath.exists()) {
            return;
        }

        String str = Utils.readTextFile(configvarsFilePath);

        for (String line : str.split("\n")) {
            if (!line.trim().startsWith("#")) {
                try {
                    String[] keyValuePair = line.trim().split("\\s+");
                    cmd.add(CONFIGVAR_OPTION);
                    cmd.add(keyValuePair[0] + "=" + keyValuePair[1]);
                } catch (Exception e) {
                    //ignore and continue on
                }
            }
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

    private final static Logger logger = Logger.getLogger(StreamGobbler.class.getName());

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
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                outputMessage.append(line).append("\n");
            }
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, ioe.getMessage(), ioe);
        }
    }
}
