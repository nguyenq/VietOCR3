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
import java.util.Arrays;

/**
 * Common image processing routines.
 */
public class ImageHelper {

    public static BufferedImage lookupOp(BufferedImage source, short table[]) {
        int numBands = source.getRaster().getNumBands();
        int numColorComponents = numBands;

        int len = table.length;
        short table_[][] = new short[numBands][len];
        
        if (source.getColorModel().hasAlpha())
            numColorComponents--;
            
        for (int n = 0; n < numColorComponents; n++)
            table_[n] = table;
        
        if (source.getColorModel().hasAlpha()) {
            table_[numBands-1] = new short[len];
            Arrays.fill(table_[numBands-1], (short)(len - 1));
        }
        
        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        
        ShortLookupTable lookupTable = new ShortLookupTable(0, table_);
        return new LookupOp(lookupTable, null).filter(source, target);
    }

    public static BufferedImage gamma(BufferedImage src, double gamma) {
        gamma = 1.0 / gamma;
        short table[] = new short[256];
        for (int i = 0; i < 256; i++) 
            table[i] = (short)(255.0 * Math.pow(i / 255.0, gamma) + 0.5);

        return lookupOp(src, table);
    }

   /**
     * @param source
     * @param scale
     * @param offset
     * @return
     */
    public static BufferedImage rescaleOp(BufferedImage source, float scale, float offset) {
        int numBands = source.getRaster().getNumBands();
        int numColorComponents = numBands;
        float scale_[] = new float[numBands];
        float offset_[] = new float[numBands];
        
        if (source.getColorModel().hasAlpha())
            numColorComponents--;
        
        for (int n = 0; n < numColorComponents; n++) {
            scale_[n] = scale;
            offset_[n] = offset;
        }
        if (source.getColorModel().hasAlpha()) {
            scale_[numBands-1] = 1;
            offset_[numBands-1] = 1;
        }        

        return new RescaleOp(scale_, offset_, null).filter(source, null);
    }
            
    /**
     * Returns the supplied src image brightened by a float value from 0 to 10.
     * Float values below 1.0f actually darken the source image.
     *
     * @param src
     * @param offset
     * @return
     */
    public static BufferedImage brighten(BufferedImage src, float offset) {
        return rescaleOp(src, 1, offset);
    }

    /**
     * Contrasts image.
     *
     * @param src
     * @param scaleFactor
     * @return
     */
    public static BufferedImage contrast(BufferedImage src, float scaleFactor) {
        return rescaleOp(src, scaleFactor, 0);
    }

    // allow a 10px-margin
    private static final int MARGIN = 10;

    /**
     * Auto crops an image.
     *
     * @param source
     * @param tolerance range from 0.0 to 0.5
     * @return
     */
    public static BufferedImage autoCrop(BufferedImage source, double tolerance) {
        // Get top-left pixel color as the "baseline" for cropping
        int baseColor = source.getRGB(0, 0);

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
                if (colorWithinTolerance(baseColor, source.getRGB(x, y), tolerance)) {
                    minY = y;
                    break lable1;
                }
            }
        }

        lable2:
        for (int x = 0; x < width; x++) {
            for (int y = minY; y < height; y++) {
                if (colorWithinTolerance(baseColor, source.getRGB(x, y), tolerance)) {
                    minX = x;
                    break lable2;
                }
            }
        }

        // Get lower-left pixel color as the "baseline" for cropping
        baseColor = source.getRGB(minX, height - 1);

        lable3:
        for (int y = height - 1; y >= minY; y--) {
            for (int x = minX; x < width; x++) {
                if (colorWithinTolerance(baseColor, source.getRGB(x, y), tolerance)) {
                    maxY = y;
                    break lable3;
                }
            }
        }

        lable4:
        for (int x = width - 1; x >= minX; x--) {
            for (int y = minY; y < maxY; y++) {
                if (colorWithinTolerance(baseColor, source.getRGB(x, y), tolerance)) {
                    maxX = x;
                    break lable4;
                }
            }
        }

        if ((minX - MARGIN) >= 0) {
            minX -= MARGIN;
        }

        if ((minY - MARGIN) >= 0) {
            minY -= MARGIN;
        }

        if ((maxX + MARGIN) < width) {
            maxX += MARGIN;
        }

        if ((maxY + MARGIN) < height) {
            maxY += MARGIN;
        }

        int newWidth = maxX - minX + 1;
        int newHeight = maxY - minY + 1;

        // if same size, return the original
        if (newWidth == width && newHeight == height) {
            return source;
        }

        BufferedImage target = new BufferedImage(newWidth, newHeight, source.getType());

        Graphics g = target.getGraphics();
        g.drawImage(source, 0, 0, target.getWidth(), target.getHeight(),
                minX, minY, maxX + 1, maxY + 1, null);

        g.dispose();

        return target;
    }

    /**
     * Determines color distance.
     * http://stackoverflow.com/questions/10678015/how-to-auto-crop-an-image-white-border-in-java
     *
     * @param a a RGB value
     * @param b a RGB value
     * @param tolerance
     * @return
     */
    private static boolean colorWithinTolerance(int a, int b, double tolerance) {
        int aAlpha = (int) ((a & 0xFF000000) >>> 24); // Alpha level
        int aRed = (int) ((a & 0x00FF0000) >>> 16);   // Red level
        int aGreen = (int) ((a & 0x0000FF00) >>> 8);  // Green level
        int aBlue = (int) (a & 0x000000FF);           // Blue level

        int bAlpha = (int) ((b & 0xFF000000) >>> 24); // Alpha level
        int bRed = (int) ((b & 0x00FF0000) >>> 16);   // Red level
        int bGreen = (int) ((b & 0x0000FF00) >>> 8);  // Green level
        int bBlue = (int) (b & 0x000000FF);           // Blue level

        double distance = Math.sqrt((aAlpha - bAlpha) * (aAlpha - bAlpha)
                + (aRed - bRed) * (aRed - bRed)
                + (aGreen - bGreen) * (aGreen - bGreen)
                + (aBlue - bBlue) * (aBlue - bBlue));

        // 510.0 is the maximum distance between two colors 
        // (0,0,0,0 -> 255,255,255,255)
        double percentAway = distance / 510.0d;

        return (percentAway > tolerance);
    }

    /**
     * Crops an image to a given region.
     *
     * @param src
     * @param rect
     * @return
     */
    public static BufferedImage crop(BufferedImage src, Rectangle rect) {
        BufferedImage dest = new BufferedImage((int)rect.getWidth(), (int)rect.getHeight(), src.getType());
        Graphics g = dest.getGraphics();
        g.drawImage(src, 0, 0, (int)rect.getWidth(), (int)rect.getHeight(), 
                (int)rect.getX(), (int)rect.getY(), (int)(rect.getX() + rect.getWidth()), (int)(rect.getY() + rect.getHeight()), null);
        g.dispose();
        return dest;
    }

    /**
     * Sharpens an image.
     * http://photo.net/bboard/q-and-a-fetch-msg.tcl?msg_id=000Qi5
     *
     * @param image
     * @return
     */
    public static BufferedImage sharpen(BufferedImage image) {
        // A 5x5 kernel that sharpens an image
        float k = 179.0f;
        Kernel kernel = new Kernel(5, 5, new float[] {
                     0/k,   0/k,  -1/k,   0/k,  0/k,
                     0/k,  -8/k, -21/k,  -8/k,  0/k,
                    -1/k, -21/k, 299/k, -21/k, -1/k,
                     0/k,  -8/k, -21/k,  -8/k,  0/k,
                     0/k,   0/k,  -1/k,   0/k,  0/k
                });

        BufferedImageOp op = new ConvolveOp(kernel);

        return op.filter(image, null);
    }

    /**
     * Smooths or blurs an image.
     * http://photo.net/bboard/q-and-a-fetch-msg.tcl?msg_id=000Qi5
     *
     * @param image
     * @return
     */
    public static BufferedImage smoothen(BufferedImage image) {
        // A 5x5 kernel that smoothens an image
        float k = 179.0f;
        Kernel kernel = new Kernel(5, 5, new float[] {
                    0/k,  0/k,  1/k,  0/k, 0/k,
                    0/k,  8/k, 21/k,  8/k, 0/k,
                    1/k, 21/k, 59/k, 21/k, 1/k,
                    0/k,  8/k, 21/k,  8/k, 0/k,
                    0/k,  0/k,  1/k,  0/k, 0/k
                });

        BufferedImageOp op = new ConvolveOp(kernel);

        return op.filter(image, null);
    }
        
    public static int[] gaussianSmooth(int input[], int width, int height, int defaultVal) {
        // sigma = 1.0, kernel size = 5, radius = 2
        int radius = 2;
        double kernel[] = {0.054488684549642945, 0.24420134200323335, 0.40261994689424746, 0.24420134200323335, 0.054488684549642945};
                
        double sum;
        int output[] = new int[width * height];
        Arrays.fill(output, defaultVal);
        
        for (int x = radius; x < width - radius; x++)
            for (int y = radius; y < height - radius; y++) {
                sum = 0.0;
                for(int i = -radius; i <= radius; i++)
                    sum += kernel[i + radius] * input[(y - i) * width + x];
                output[y * width + x] = (int)(sum + 0.5);
            }
        
        for (int x = radius; x < width - radius; x++)
            for (int y = radius; y < height - radius; y++) {
                sum = 0.0;
                for(int i = -radius; i <= radius; i++)
                    sum += kernel[i + radius] * output[y * width + (x - i)];
                output[y * width + x] = (int)(sum + 0.5);
            }

        return output;
    }
    
    public static int[] calculateNewColor(int inputPixel[], int maskVal, int numColorComponents) {
        if (maskVal == 127) // 127: don't change pixel intensity
            return inputPixel;
        
        int numBands = inputPixel.length;
        int outputPixel[] = new int[numBands];
        
        outputPixel[numBands - 1] = inputPixel[numBands - 1];
        for (int n = 0; n < numColorComponents; n++)
            if (maskVal >= 0 && maskVal <= 126)        // 0 - 126: darken a pixel; 0 = max level, 126 = min level
                outputPixel[n] = (int)((maskVal / 126.0) * inputPixel[n] + 0.5);
            else if (maskVal >= 128 && maskVal <= 255) // 128 - 255: lighten a pixel; 128 = min level, 255 = max level
                outputPixel[n] = (int)(255.0 - (255 - inputPixel[n]) * (255 - maskVal) / 127.0 + 0.5);

        return outputPixel;
    }

    /**
     * Bradley adaptive threshold.
     * http://github.com/rmtheis/bradley-adaptive-thresholding
     * 
     * @param source
     * @param blackLevel
     * @param whiteLevel
     * @return 
     */
    public static BufferedImage adaptiveThreshold(BufferedImage source, float blackLevel, float whiteLevel) {
        int width = source.getWidth();
        int height = source.getHeight();
        
        BufferedImage sourceInGray;
        if (source.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            // RGB to Gray conversion with the best performance ???
            sourceInGray = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            Graphics g = sourceInGray.getGraphics();  
            g.drawImage(source, 0, 0, null);  
            g.dispose();  
        }
        else
            sourceInGray = source;
      
        int inputGrayPixelBuffer[] = sourceInGray.getRaster().getPixels(0, 0, width, height, (int[]) null);
        
        // prepare the integral image
        long integralImg[] = new long[width * height];
        for (int i = 0; i < width; i++) {
            long sum = 0;
            for (int j = 0; j < height; j++) {
                int index = j * width + i;
                sum += inputGrayPixelBuffer[index];
                if (i == 0)
                    integralImg[index] = sum;
                else
                    integralImg[index] = integralImg[index-1] + sum;
            }
        }
        
        int x1, y1, x2, y2;
        int s2 = width / (8 * 2);

        int maskArray[] = new int[width * height];
        Arrays.fill(maskArray, 127); // 0 - 126: darken a pixel; 127: don't change; 128 - 255: lighten a pixel

        // perform thresholding
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++) {
                int index = j * width + i;
                // set the SxS region
                x1 = i - s2; x2 = i + s2;
                y1 = j - s2; y2 = j + s2;
                // check the border
                if (x1 < 0)
                    x1 = 0;
                if (x2 >= width)
                    x2 = width - 1;
                if (y1 < 0) 
                    y1 = 0;
                if (y2 >= height)
                    y2 = height - 1;

                long count = (x2 - x1) * (y2 - y1);
                // I(x, y) = s(x2, y2) - s(x1, y2) - s(x2, y1) + s(x1, x1)
                long sum = integralImg[y2 * width + x2] - integralImg[y1 * width + x2] -
                           integralImg[y2 * width + x1] + integralImg[y1 * width + x1];

                if (inputGrayPixelBuffer[index] * count < sum * blackLevel)
                    maskArray[index] = 0;
                else if (inputGrayPixelBuffer[index] * count > sum * whiteLevel)
                    maskArray[index] = 255;
                }
        
        // smooth the mask
        maskArray = gaussianSmooth(maskArray, width, height, 127);
        
        WritableRaster sourceRaster = source.getRaster();
        int numBands = sourceRaster.getNumBands();
        int numColorComponents = numBands;
        if (source.getColorModel().hasAlpha())
            numColorComponents--;
        
        int inputPixelBuffer[] = sourceRaster.getPixels(0, 0, width, height, (int[]) null);
        int outputPixelBuffer[] = new int[width * height * numBands];
        
        // apply the mask
         for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++) {
                int index = j * width + i;
                int pixel[] = new int[numBands];
                System.arraycopy(inputPixelBuffer, index * numBands, pixel, 0, numBands);
                int newColor[] = calculateNewColor(pixel, maskArray[index], numColorComponents);
                System.arraycopy(newColor, 0, outputPixelBuffer, index * numBands, numBands);
            }
       
        BufferedImage target = new BufferedImage(width, height, source.getType());
        target.getRaster().setPixels(0, 0, width, height, outputPixelBuffer);

        return target;
    }

    /**
     * Bilateral filtering an image.
     * http://cybertron.cg.tu-berlin.de/eitz/bilateral_filtering/
     *
     * @param source
     * @param sigmaD
     * @param sigmaR
     * @return
     */
    public static BufferedImage bilateralFiltering(BufferedImage source, double sigmaD, double sigmaR) {
        int width = source.getWidth();
        int height = source.getHeight();
	
        double sigmaMax = Math.max(sigmaD, sigmaR);
        int kernelRadius = (int)Math.ceil(2 * sigmaMax);
        double twoSigmaDSquared = 2 * sigmaD * sigmaD;
        double twoSigmaRSquared = 2 * sigmaR * sigmaR;

	int kernelSize = kernelRadius * 2 + 1;
	double kernelD[][] = new double[kernelSize][kernelSize];
	int center = (kernelSize - 1) / 2;
        for (int x = -center; x < -center + kernelSize; x++)
            for (int y = -center; y < -center + kernelSize; y++)
                kernelD[x + center][y + center] = Math.exp(-(x * x + y * y) / twoSigmaDSquared);
	
	double gaussSimilarity[] = new double[256];
        for (int i = 0; i < 256; i++)
            gaussSimilarity[i] = Math.exp(-i / twoSigmaRSquared);
              
        WritableRaster sourceRaster = source.getRaster();
        BufferedImage target = new BufferedImage(width, height, source.getType());
        WritableRaster targetRaster = target.getRaster();
        int numBands = sourceRaster.getNumBands();
        int inputPixelBuffer[] = sourceRaster.getPixels(0, 0, width, height, (int[]) null);
        int outputPixelBuffer[] = new int[width * height * numBands];
        
        for(int i = 1; i < width - 1; i++)
            for(int j = 1; j < height - 1; j++) {
                int pixelCenter[] = new int[numBands];
                int targetColor[] = new int[numBands];
                System.arraycopy(inputPixelBuffer, (j * width + i) * numBands, pixelCenter, 0, numBands);
                int mMin = Math.max(0, i - kernelRadius);
                int mMax = Math.min(width - 1, i + kernelRadius);
                int nMin = Math.max(0, j - kernelRadius);
                int nMax = Math.min(height - 1, j + kernelRadius);
                int kernelWidth = mMax - mMin + 1;
                int kernelHeight = nMax - nMin + 1;
                int pixelKernelPosBuffer[] = sourceRaster.getPixels(mMin, nMin, kernelWidth, kernelHeight, (int[]) null);
                for (int band = 0; band < numBands; band++) {
                    double weight;
                    double sum = 0;
                    double totalWeight = 0;
                    int intensityCenter = pixelCenter[band];
                    for (int m = mMin; m <= mMax; m++)
                        for (int n = nMin; n <= nMax; n++) {
                            int pos = ((n - nMin) * kernelWidth + (m - mMin)) * numBands + band;
                            int intensityKernelPos = pixelKernelPosBuffer[pos];
                            weight = kernelD[i-m + kernelRadius][j-n + kernelRadius] * 
                                gaussSimilarity[Math.abs(intensityKernelPos - intensityCenter)];
                            totalWeight += weight;
                            sum += weight * intensityKernelPos;
                        }
                    targetColor[band] = (int)Math.floor(sum / totalWeight);
                }
            System.arraycopy(targetColor, 0, outputPixelBuffer, (j * width + i) * numBands, numBands);
            }
        
        targetRaster.setPixels(0, 0, width, height, outputPixelBuffer);
        
        return target;
    }
}
