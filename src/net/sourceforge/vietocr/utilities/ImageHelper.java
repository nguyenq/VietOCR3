/**
 * Copyright @ 2008
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
package net.sourceforge.vietocr.utilities;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.*;
import javax.imageio.IIOImage;

/**
 * Common image processing routines.
 */
public class ImageHelper {
    
    private static final int COLOR_WHITE = Color.WHITE.getRGB();

    /**
     * Convenience method that returns a scaled instance of the provided
     * {@code BufferedImage}.
     *
     * @param image the original image to be scaled
     * @param targetWidth the desired width of the scaled instance, in pixels
     * @param targetHeight the desired height of the scaled instance, in pixels
     * @return a scaled version of the original {@code BufferedImage}
     */
    public static BufferedImage getScaledInstance(BufferedImage image, int targetWidth, int targetHeight) {
        int type = (image.getTransparency() == Transparency.OPAQUE)
                ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage tmp = new BufferedImage(targetWidth, targetHeight, type);
        Graphics2D g2 = tmp.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        g2.dispose();
        return tmp;
    }

    /**
     * Convenience method that returns a scaled instance of the provided
     * {@code IIOImage}.
     *
     * @param iioSource the original image to be scaled
     * @param scale the desired scale
     * @return a scaled version of the original {@code IIOImage}
     */
    public static IIOImage getScaledInstance(IIOImage iioSource, float scale) {
        if (!(iioSource.getRenderedImage() instanceof BufferedImage)) {
            throw new IllegalArgumentException("RenderedImage in IIOImage must be BufferedImage");
        }

        if (scale == 1.0) {
            return iioSource;
        }

        BufferedImage source = (BufferedImage) iioSource.getRenderedImage();
        BufferedImage target = getScaledInstance(source, (int) (scale * source.getWidth()), (int) (scale * source.getHeight()));
        return new IIOImage(target, null, null);
    }

    /**
     * A replacement for the standard
     * <code>BufferedImage.getSubimage</code> method.
     *
     * @param image
     * @param x the X coordinate of the upper-left corner of the specified
     * rectangular region
     * @param y the Y coordinate of the upper-left corner of the specified
     * rectangular region
     * @param width the width of the specified rectangular region
     * @param height the height of the specified rectangular region
     * @return a BufferedImage that is the subimage of <code>image</code>.
     */
    public static BufferedImage getSubImage(BufferedImage image, int x, int y, int width, int height) {
        int type = (image.getTransparency() == Transparency.OPAQUE)
                ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage tmp = new BufferedImage(width, height, type);
        Graphics2D g2 = tmp.createGraphics();
        g2.drawImage(image.getSubimage(x, y, width, height), 0, 0, null);
        g2.dispose();
        return tmp;
    }

    /**
     * A simple method to convert an image to binary or B/W image.
     *
     * @param image input image
     * @return a monochrome image
     */
    public static BufferedImage convertImageToBinary(BufferedImage image) {
        BufferedImage tmp = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2 = tmp.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return tmp;
    }
    
    /**
     * A simple method to convert an image to gray scale.
     *
     * @param image input image
     * @return a monochrome image
     */
    public static BufferedImage convertImageToGrayscale(BufferedImage image) {
        BufferedImage tmp = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = tmp.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return tmp;
    }

    private static final short[] invertTable;

    static {
        invertTable = new short[256];
        for (int i = 0; i < 256; i++) {
            invertTable[i] = (short) (255 - i);
        }
    }

    /**
     * Inverts image color.
     *
     * @param image input image
     * @return an inverted-color image
     */
    public static BufferedImage invertImageColor(BufferedImage image) {
        BufferedImage tmp = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        BufferedImageOp invertOp = new LookupOp(new ShortLookupTable(0, invertTable), null);
        return invertOp.filter(image, tmp);
    }

    /**
     * Gets an image from Clipboard.
     *
     * @return image
     */
    public static Image getClipboardImage() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            return (Image) clipboard.getData(DataFlavor.imageFlavor);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the supplied src image brightened by a float value from 0 to 10. Float values
     * below 1.0f actually darken the source image.
     * @param src
     * @param offset
     * @return 
     */
    public static BufferedImage brighten(BufferedImage src, float offset) {
        RescaleOp rop = new RescaleOp(1, offset, null);
        return rop.filter(src, null);
    }
    
    /**
     * Contrasts image.
     * @param src
     * @param scaleFactor
     * @return 
     */
    public static BufferedImage contrast(BufferedImage src, float scaleFactor) {
        RescaleOp rop = new RescaleOp(scaleFactor, 0, null);
        return rop.filter(src, null);
    }
    
    // allow a 10px-margin
    private static final int margin = 10;

    /**
     * Auto crops an image.
     * @param source
     * @return 
     */
    public static BufferedImage autoCrop(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();

        int minX = 0;
        int minY = 0;
        int maxX = width;
        int maxY = height;
        
        // Immediately break the loops when encountering a non-white pixel.
        lable1:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (source.getRGB(x, y) != COLOR_WHITE) {
                    minY = y;
                    break lable1;
                }
            }
        }

        lable2:
        for (int x = 0; x < width; x++) {
            for (int y = minY; y < height; y++) {
                if (source.getRGB(x, y) != COLOR_WHITE) {
                    minX = x;
                    break lable2;
                }
            }
        }

        lable3:
        for (int y = height - 1; y >= minY; y--) {
            for (int x = minX; x < width; x++) {
                if (source.getRGB(x, y) != COLOR_WHITE) {
                    maxY = y;
                    break lable3;
                }
            }
        }

        lable4:
        for (int x = width - 1; x >= minX; x--) {
            for (int y = minY; y < maxY; y++) {
                if (source.getRGB(x, y) != COLOR_WHITE) {
                    maxX = x;
                    break lable4;
                }
            }
        }
        
        if ((minX - margin) >= 0) {
            minX -= margin;
        }
        
        if ((minY - margin) >= 0) {
            minY -= margin;
        }
        
        if ((maxX + margin) <= width) {
            maxX += margin;
        }
        
        if ((maxY + margin) <= height) {
            maxY += margin;
        }
        
        // if same size, return the original
        if (minX == 0 && minY == 0 && maxX == width && maxY == height) {
            return source;
        }
        
        int newWidth = maxX - minX + 1;
        int newHeight = maxY - minY + 1;
        
        BufferedImage target = new BufferedImage(newWidth, newHeight, source.getType());

        Graphics g = target.getGraphics();
        g.drawImage(source, 0, 0, target.getWidth(), target.getHeight(),
                minX, minY, maxX, maxY, null);

        g.dispose();

        return target;
    }

    /**
     * Sharpens an image.
     * 
     * @param image
     * @return 
     */
    public static BufferedImage sharpen(BufferedImage image) {
        // A 3x3 kernel that sharpens an image
        Kernel kernel = new Kernel(3, 3,
                new float[] {
                    -1, -1, -1,
                    -1, 9, -1,
                    -1, -1, -1
                });

        BufferedImageOp op = new ConvolveOp(kernel);

        return op.filter(image, null);
    }
    
    /**
     * Smooths or blurs an image.
     * 
     * @param image
     * @return 
     */
    public static BufferedImage smoothen(BufferedImage image) {
        // A 3x3 kernel that smoothens an image
        float data1[] = {
            0.1111f, 0.1111f, 0.1111f,
            0.1111f, 0.1111f, 0.1111f,
            0.1111f, 0.1111f, 0.1111f
        };
        
        Kernel kernel = new Kernel(3, 3, data1);
        BufferedImageOp op = new ConvolveOp(kernel);

        return op.filter(image, null);
    }
    
    /**
     * Clones an image.
     * http://stackoverflow.com/questions/3514158/how-do-you-clone-a-bufferedimage
     * @param bi
     * @return 
     */
    public static BufferedImage cloneImage(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
}
