/**
 * Copyright @ 2012 Quan Nguyen
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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.IIOImage;

import net.sourceforge.tess4j.ITesseract.RenderedFormat;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.util.ImageIOHelper;
import net.sourceforge.vietocr.util.Utils;

/**
 * Invokes Tesseract OCR API through JNA-based Tess4J wrapper. This could be
 * faster than the command-line method since it feeds image data directly to the
 * OCR engine without creating intermediate working files (less I/O operations).
 * However, any exception from native code will result in hard crash of the
 * application.
 */
public class OCRImages extends OCR<IIOImage> {

    private final Tesseract instance;

    public OCRImages() {
        instance = new Tesseract();
    }

    /**
     * Recognizes images.
     *
     * @param images as IIOImage
     * @param inputfilename input filename
     * @return recognized text
     * @throws Exception
     */
    @Override
    public String recognizeText(List<IIOImage> images, String inputfilename) throws Exception {
        instance.setDatapath(datapath);
        instance.setLanguage(language);
        instance.setPageSegMode(Integer.parseInt(pageSegMode));
        instance.setOcrEngineMode(Integer.parseInt(ocrEngineMode));
        instance.setHocr(outputFormat.equalsIgnoreCase("hocr"));

        File configsFilePath = new File(datapath, CONFIG_PATH + CONFIGS_FILE);
        if (configsFilePath.exists()) {
            String[] configs = {CONFIGS_FILE};
            instance.setConfigs(Arrays.asList(configs));
        }

        controlParameters(instance);
        String text = instance.doOCR(images, inputfilename, rect);

        return text;
    }

    /**
     * Gets segmented regions.
     *
     * @return segmented regions
     * @throws java.io.IOException
     */
    @Override
    public List<Rectangle> getSegmentedRegions(IIOImage image, int tessPageIteratorLevel) throws Exception {
        instance.setDatapath(datapath);
        return instance.getSegmentedRegions((BufferedImage) image.getRenderedImage(), tessPageIteratorLevel);
    }

    /**
     * Reads <code>tessdata/configs/tess_configvars</code> and
     * <code>setVariable</code> on Tesseract engine. This only works for
     * non-init parameters (@see
     * <a href="https://code.google.com/p/tesseract-ocr/wiki/ControlParams">ControlParams</a>).
     *
     * @param instance
     */
    void controlParameters(Tesseract instance) throws Exception {
        File configvarsFilePath = new File(datapath, CONFIG_PATH + CONFIGVARS_FILE);
        if (!configvarsFilePath.exists()) {
            return;
        }

        String str = Utils.readTextFile(configvarsFilePath);

        for (String line : str.split("\n")) {
            if (!line.trim().startsWith("#")) {
                try {
                    String[] keyValuePair = line.trim().split("\\s+");
                    instance.setTessVariable(keyValuePair[0], keyValuePair[1]);
                } catch (Exception e) {
                    //ignore and continue on
                }
            }
        }
    }

    /**
     * Processes OCR for input file with specified output format.
     *
     * @param imageFile
     * @param outputFile
     * @throws Exception
     */
    @Override
    public void processPages(File imageFile, File outputFile) throws Exception {
        instance.setDatapath(datapath);
        instance.setLanguage(language);
        instance.setPageSegMode(Integer.parseInt(pageSegMode));
        instance.setOcrEngineMode(Integer.parseInt(ocrEngineMode));
        List<RenderedFormat> formats = new ArrayList<RenderedFormat>();
        formats.add(RenderedFormat.valueOf(outputFormat.toUpperCase()));

        File tempImageFile = null;

        if (deskew) {
            tempImageFile = ImageIOHelper.deskewImage(imageFile, MINIMUM_DESKEW_THRESHOLD);
            tempImageFile.deleteOnExit();
        }

        instance.createDocuments(deskew ? tempImageFile.getPath() : imageFile.getPath(), outputFile.getPath(), formats);

        if (tempImageFile != null && tempImageFile.exists()) {
            tempImageFile.delete();
        }
    }
}
