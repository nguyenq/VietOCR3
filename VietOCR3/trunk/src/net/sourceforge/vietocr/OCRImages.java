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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.IIOImage;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.ITesseract.RenderedFormat;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.vietocr.util.Utils;

/**
 * Invokes Tesseract OCR API through JNA-based Tess4J wrapper.<br />This could
 * be faster than the existing method since it feeds image data directly to the
 * OCR engine without creating intermediate working files (less I/O operations).
 * However, any exception from native code will result in hard crash of the
 * application.
 */
public class OCRImages extends OCR<IIOImage> {

    Tesseract instance;
    final String TESSDATA = "tessdata";

    public OCRImages(String tessPath) {
        instance = Tesseract.getInstance();
        instance.setDatapath(new File(tessPath, TESSDATA).getPath());
    }

    /**
     * Recognizes images.
     * 
     * @param images as IIOImage
     * @return recognized text
     * @throws Exception 
     */
    @Override
    public String recognizeText(List<IIOImage> images) throws Exception {
        instance.setLanguage(this.getLanguage());
        instance.setPageSegMode(Integer.parseInt(this.getPageSegMode()));
        instance.setHocr(this.getOutputFormat().equalsIgnoreCase("hocr"));
        String text = instance.doOCR(images, rect);

        return text;
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
        instance.setLanguage(this.getLanguage());
        instance.setPageSegMode(Integer.parseInt(this.getPageSegMode()));
        List<RenderedFormat> formats = new ArrayList<RenderedFormat>();
        formats.add(RenderedFormat.valueOf(this.getOutputFormat().toUpperCase()));
        instance.createDocuments(inputImage.getPath(), Utils.stripExtension(outputFile.getName()), outputFile.getParent(), formats);
    }
}
