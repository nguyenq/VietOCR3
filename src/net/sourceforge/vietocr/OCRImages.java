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
import java.util.List;
import javax.imageio.IIOImage;
import net.sourceforge.tess4j.Tesseract;

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

    @Override
    public String recognizeText(List<IIOImage> images, String lang) throws Exception {
        instance.setLanguage(lang);
        instance.setPageSegMode(Integer.parseInt(this.getPageSegMode()));
        String text = instance.doOCR(images, rect);

        return text;
    }
}
