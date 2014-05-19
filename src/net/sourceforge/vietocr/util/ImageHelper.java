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
package net.sourceforge.vietocr.util;

import java.awt.*;
import java.awt.image.*;

/**
 * Common image processing routines.
 */
public class ImageHelper {
    
    private static final int COLOR_WHITE = Color.WHITE.getRGB();

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
                minX, minY, maxX + 1, maxY + 1, null);

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
}
