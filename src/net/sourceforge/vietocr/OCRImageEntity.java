/**
 * Copyright @ 2009 Quan Nguyen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sourceforge.vietocr;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.IIOImage;

import net.sourceforge.tess4j.util.ImageIOHelper;
import net.sourceforge.tess4j.util.ImageHelper;

public class OCRImageEntity {

    /**
     * input images
     */
    private List<IIOImage> oimages;
    /**
     * input image File
     */
    private File imageFile;
    /**
     * input filename
     */
    private String inputfilename;
    /**
     * index of pages, such as in multi-page TIFF image
     */
    private final int index;
    /**
     * list of list of regions of interest
     */
    private final List<List<Rectangle>> roiss;
    /**
     * double-sided page
     */
    private boolean doublesided;
    /**
     * Language code, which follows ISO 639-3 standard
     */
    private final String lang;
    /**
     * Horizontal Resolution
     */
    private int dpiX;
    /**
     * Vertical Resolution
     */
    private int dpiY;

    /**
     * Constructor.
     *
     * @param oimages a list of <code>IIOImage</code> objects
     * @param inputfilename input filename
     * @param index index of images
     * @param roiss list of lists of the bounding rectangle defines the regions of the
     * image to be recognized. A rectangle of zero dimension or
     * <code>null</code> indicates the whole image.
     * @param doublesided
     * @param lang language code, which follows ISO 639-3 standard
     */
    public OCRImageEntity(List<IIOImage> oimages, String inputfilename, int index, List<List<Rectangle>> roiss, boolean doublesided, String lang) {
        this.oimages = oimages;
        this.inputfilename = inputfilename;
        this.index = index;
        this.roiss = roiss;
        this.doublesided = doublesided;
        this.lang = lang;
    }

//    /**
//     * Constructor.
//     *
//     * @param imageFile an image file
//     * @param index index of images
//     * @param rect the bounding rectangle defines the region of the image to be
//     * recognized. A rectangle of zero dimension or <code>null</code> indicates
//     * the whole image.
//     * @param lang language code, which follows ISO 639-3 standard
//     */
//    public OCRImageEntity(File imageFile, int index, List<Rectangle> rect, String lang) {
//        this.imageFile = imageFile;
//        this.inputfilename = imageFile.getPath();
//        this.index = index;
//        this.rects = rect;
//        this.lang = lang;
//    }

    /**
     * Gets oimages.
     *
     * @return the list of oimages
     */
    public List<IIOImage> getOimages() {
        return oimages;
    }

    /**
     * Gets selected oimages.
     *
     * @return the list of selected oimages
     */
    public List<IIOImage> getSelectedOimages() {
        return index == -1 ? oimages : oimages.subList(index, index + 1);
    }

    /**
     * Gets image file.
     *
     * @return the imageFile
     */
    public File getImageFile() {
        return imageFile;
    }

    /**
     * Gets the index.
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets list of lists of regions of interest.
     *
     * @return the bounding rectangles
     */
    public List<List<Rectangle>> getROIss() {
        if (roiss != null) {
            return roiss; // drawn ROIs on images
        } else if (doublesided) {
            // create ROIs for double-sided pages, which consist of two equal-sized side-by-side rectangles for each page
            List<List<Rectangle>> lists = new ArrayList<>();
            for (IIOImage image : oimages) {
                List<Rectangle> list = new ArrayList<>();
                RenderedImage ri = image.getRenderedImage();
                int width = ri.getWidth();
                int height = ri.getHeight();
                Rectangle rect = new Rectangle(width / 2, height);
                list.add(rect);
                rect = (Rectangle) rect.clone();
                rect.x = width / 2;
                list.add(rect);
                lists.add(list);
            }
            return lists;
        }
        
        return null;
    }

    /**
     * Sets screenshot mode.
     *
     * @param mode true for resampling the input image; false for no
     * manipulation of the image
     */
    public void setScreenshotMode(boolean mode) {
        dpiX = mode ? 300 : 0;
        dpiY = mode ? 300 : 0;
    }

    /**
     * Sets resolution (DPI).
     *
     * @param dpiX horizontal resolution
     * @param dpiY vertical resolution
     */
    public void setResolution(int dpiX, int dpiY) {
        this.dpiX = dpiX;
        this.dpiY = dpiY;
    }

    /**
     * Gets language code.
     *
     * @return the language code
     */
    public String getLanguage() {
        return lang;
    }

    /**
     * Gets input filename.
     *
     * @return the input filename
     */
    public String getInputfilename() {
        return inputfilename;
    }
}
