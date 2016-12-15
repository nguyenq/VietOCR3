/**
 * Core Java Technologies Tech Tips, February 20, 2003: Providing a Scalable
 * Image Icon http://java.sun.com/developer/JDCTechTips/2003/tt0220.html#2
 */
package net.sourceforge.vietocr.components;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.net.*;
import javax.imageio.IIOImage;

public class ImageIconScalable extends ImageIcon {

    private int width = -1;
    private int height = -1;

    public ImageIconScalable() {
        super();
    }

    public ImageIconScalable(byte imageData[]) {
        super(imageData);
    }

    public ImageIconScalable(byte imageData[], String description) {
        super(imageData, description);
    }

    public ImageIconScalable(Image image) {
        super(image);
    }

    public ImageIconScalable(Image image, String description) {
        super(image, description);
    }

    public ImageIconScalable(String filename) {
        super(filename);
    }

    public ImageIconScalable(String filename, String description) {
        super(filename, description);
    }

    public ImageIconScalable(URL location) {
        super(location);
    }

    public ImageIconScalable(URL location, String description) {
        super(location, description);
    }

    @Override
    public int getIconHeight() {
        int returnValue;
        if (height == -1) {
            returnValue = super.getIconHeight();
        } else {
            returnValue = height;
        }
        return returnValue;
    }

    @Override
    public int getIconWidth() {
        int returnValue;
        if (width == -1) {
            returnValue = super.getIconWidth();
        } else {
            returnValue = width;
        }
        return returnValue;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Image image = this.getImage();
        if (image == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;

        if ((width == -1) && (height == -1)) {
            g2d.drawImage(image, x, y, c);
        } else {
            Image tempImage = rescaleImage((BufferedImage) image, width, height);
            g2d.drawImage(tempImage, x, y, c);
        }
    }

    /**
     * Sets scale.
     *
     * @param width
     * @param height
     */
    public void setScaledSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public ImageIconScalable clone() {
        BufferedImage source = (BufferedImage) this.getImage();
        BufferedImage copy = new BufferedImage(source.getColorModel(), source.copyData(null), source.isAlphaPremultiplied(), null);
        return new ImageIconScalable(copy);
    }

    /**
     * Gets a rotated image.
     *
     * @param angle
     * @return
     */
    public ImageIconScalable getRotatedImageIcon(double angle) {
        double sin = Math.abs(Math.sin(angle));
        double cos = Math.abs(Math.cos(angle));
        int w = this.getIconWidth();
        int h = this.getIconHeight();
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);

        BufferedImage image = (BufferedImage) this.getImage();
        BufferedImage outputImage = new BufferedImage(newW, newH, image.getType());

        Graphics2D g2d = outputImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g2d.setColor(UIManager.getColor("Label.background"));
        g2d.fillRect(0, 0, newW, newH);

        AffineTransform at = AffineTransform.getRotateInstance(angle, newW / 2, newH / 2);
        at.translate((newW - w) / 2, (newH - h) / 2);
        g2d.drawRenderedImage(image, at);
        g2d.dispose();

        return new ImageIconScalable(outputImage);
    }

    /**
     * Resizes an image.
     *
     * @param w - desired width
     * @param h - desired height
     * @return - the new resized image
     */
    public Image getScaledImage(int w, int h) {
        Image image = this.getImage();
        if (image == null) {
            return null;
        }

        return rescaleImage((BufferedImage) image, w, h);
    }

    /**
     * https://github.com/redwarp/9-Patch-Resizer/blob/develop/src/net/redwarp/tool/resizer/worker/ImageScaler.java
     *
     * @param image
     * @param targetWidth
     * @param targetHeight
     * @return
     */
    public BufferedImage rescaleImage(BufferedImage image, int targetWidth, int targetHeight) {
        if (targetWidth == 0) {
            targetWidth = 1;
        }
        if (targetHeight == 0) {
            targetHeight = 1;
        }
        if (targetWidth * 2 < image.getWidth() - 1) {
            BufferedImage tempImage = rescaleImage(image, image.getWidth() / 2, image.getHeight() / 2);
            return rescaleImage(tempImage, targetWidth, targetHeight);
        } else {
            BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, image.getType());
            Graphics2D g2d = outputImage.createGraphics();
            
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.drawImage(image, 0, 0, outputImage.getWidth(), outputImage.getHeight(), null);
            g2d.dispose();
            
            return outputImage;
        }
    }

    /**
     * Gets list of images.
     *
     * @param iioImageList
     * @return
     */
    public static java.util.List<ImageIconScalable> getImageList(java.util.List<IIOImage> iioImageList) {
        try {
            java.util.List<ImageIconScalable> al = new java.util.ArrayList<ImageIconScalable>();
            for (IIOImage iioImage : iioImageList) {
                al.add(new ImageIconScalable((BufferedImage) iioImage.getRenderedImage()));
            }

            return al;
        } catch (Exception e) {
            return null;
        }
    }
}
