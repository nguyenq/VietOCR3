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

import com.recognition.software.jdeskew.ImageDeskew;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Deque;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.sourceforge.lept4j.Leptonica1;
import net.sourceforge.lept4j.Pix;
import net.sourceforge.lept4j.util.LeptUtils;

import net.sourceforge.tess4j.util.ImageHelper;
import net.sourceforge.vietocr.components.ImageIconScalable;
import net.sourceforge.vietocr.components.JImageLabel;
import net.sourceforge.vietocr.util.FixedSizeStack;

public class GuiWithImage extends GuiWithBulkOCR {

    private final String strScreenshotMode = "ScreenshotMode";
    private static final double MINIMUM_DESKEW_THRESHOLD = 0.05d;
    private BufferedImage originalImage;
    Deque<BufferedImage> stack = new FixedSizeStack<BufferedImage>(10);

    GuiWithImage() {
        this.jCheckBoxMenuItemScreenshotMode.setSelected(prefs.getBoolean(strScreenshotMode, false));
        jLabelScreenshotModeValue.setText(this.jCheckBoxMenuItemScreenshotMode.isSelected() ? "On" : "Off");
    }

    /**
     * Displays image meta information.
     *
     * @param evt
     */
    @Override
    void jMenuItemMetadataActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ImageInfoDialog dialog = new ImageInfoDialog(this, true);
        dialog.setImage(iioImageList.get(imageIndex));
        if (dialog.showDialog() == JOptionPane.OK_OPTION) {
            // Do nothing for now.
            // Initial plan was to implement various image manipulation operations
            // (rotate, flip, sharpen, brighten, threshold, clean up,...) here.
        }
    }

    /**
     * Deskews image.
     *
     * @param evt
     */
    @Override
    void jMenuItemDeskewActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getGlassPane().setVisible(true);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ImageDeskew deskew = new ImageDeskew((BufferedImage) iioImageList.get(imageIndex).getRenderedImage());
                double imageSkewAngle = deskew.getSkewAngle();

                if ((imageSkewAngle > MINIMUM_DESKEW_THRESHOLD || imageSkewAngle < -(MINIMUM_DESKEW_THRESHOLD))) {
                    originalImage = (BufferedImage) iioImageList.get(imageIndex).getRenderedImage();
                    stack.push(originalImage);
                    rotateImage(-imageSkewAngle);
                }
                getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                getGlassPane().setVisible(false);
            }
        });
    }

    @Override
    void jMenuItemAutocropActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getGlassPane().setVisible(true);

        originalImage = (BufferedImage) iioImageList.get(imageIndex).getRenderedImage();
        BufferedImage croppedImage = net.sourceforge.vietocr.util.ImageHelper.autoCrop(originalImage, 0.1);
        // if same image, skip
        if (originalImage != croppedImage) {
            stack.push(originalImage);
            imageIcon = new ImageIconScalable(croppedImage);
            imageList.set(imageIndex, imageIcon);
            iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
            displayImage();
        }

        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        getGlassPane().setVisible(false);
    }

    @Override
    void jMenuItemCropActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Rectangle rect = ((JImageLabel) jImageLabel).getRect();
        if (rect == null) {
            return;
        }

        ImageIcon ii = (ImageIcon) this.jImageLabel.getIcon();
        int offsetX = 0;
        int offsetY = 0;
        if (ii.getIconWidth() < this.jScrollPaneImage.getWidth()) {
            offsetX = (this.jScrollPaneImage.getViewport().getWidth() - ii.getIconWidth()) / 2;
        }
        if (ii.getIconHeight() < this.jScrollPaneImage.getHeight()) {
            offsetY = (this.jScrollPaneImage.getViewport().getHeight() - ii.getIconHeight()) / 2;
        }

        // create a new rectangle with scale factors and offets factored in
        rect = new Rectangle((int) ((rect.x - offsetX) * scaleX), (int) ((rect.y - offsetY) * scaleY), (int) (rect.width * scaleX), (int) (rect.height * scaleY));

        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getGlassPane().setVisible(true);

        originalImage = (BufferedImage) iioImageList.get(imageIndex).getRenderedImage();
        BufferedImage croppedImage = net.sourceforge.vietocr.util.ImageHelper.crop(originalImage, rect);
        stack.push(originalImage);
        imageIcon = new ImageIconScalable(croppedImage);
        imageList.set(imageIndex, imageIcon);
        iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
        displayImage();

        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        getGlassPane().setVisible(false);
    }

    @Override
    void jMenuItemRemoveLinesActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getGlassPane().setVisible(true);

        try {
            originalImage = (BufferedImage) iioImageList.get(imageIndex).getRenderedImage();
            Pix pix = LeptUtils.convertImageToPix(originalImage);
            Pix pix1 = LeptUtils.removeLines(pix); // horizontal lines
            Pix pix2 = Leptonica1.pixRotate90(pix1, 1); // rotate 90 deg CW
            Pix pix3 = LeptUtils.removeLines(pix2); // effectively vertical lines
            Pix pix4 = Leptonica1.pixRotate90(pix3, -1);  // rotate 90 deg CCW
            BufferedImage imageLinesRemoved = LeptUtils.convertPixToImage(pix4);
            LeptUtils.disposePix(pix);
            LeptUtils.disposePix(pix1);
            LeptUtils.disposePix(pix2);
            LeptUtils.disposePix(pix3);
            LeptUtils.disposePix(pix4);
            stack.push(originalImage);
            imageIcon = new ImageIconScalable(imageLinesRemoved);
            imageList.set(imageIndex, imageIcon);
            iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
            displayImage();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, bundle.getString("Image_may_require_conversion_to_grayscale"), APP_NAME, JOptionPane.ERROR_MESSAGE);
        } finally {
            getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            getGlassPane().setVisible(false);
        }
    }

    @Override
    void jMenuItemBrightnessActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        SliderDialog dialog = new SliderDialog(this, true);
        dialog.setLabelText(bundle.getString("Brightness"));
        dialog.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(SliderDialog.VALUE_CHANGED)) {
                    int value = (Integer) evt.getNewValue();
                    imageIcon = new ImageIconScalable(net.sourceforge.vietocr.util.ImageHelper.brighten(originalImage, value));
                    imageList.set(imageIndex, imageIcon);
                    iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
                    displayImage();
                }
            }
        });

        originalImage = (BufferedImage) iioImageList.get(imageIndex).getRenderedImage();
        stack.push(originalImage);
        if (dialog.showDialog() == JOptionPane.CANCEL_OPTION) {
            //restore image
            originalImage = stack.pop();
            imageIcon = new ImageIconScalable(originalImage);
            imageList.set(imageIndex, imageIcon);
            iioImageList.get(imageIndex).setRenderedImage(originalImage);
            displayImage();
        }
    }

    @Override
    void jMenuItemContrastActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        SliderDialog dialog = new SliderDialog(this, true);
        dialog.setLabelText(bundle.getString("Contrast"));
        dialog.setForContrast();
        dialog.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(SliderDialog.VALUE_CHANGED)) {
                    int value = (Integer) evt.getNewValue();
                    imageIcon = new ImageIconScalable(net.sourceforge.vietocr.util.ImageHelper.contrast(originalImage, value * 0.02f));
                    imageList.set(imageIndex, imageIcon);
                    iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
                    displayImage();
                }
            }
        });

        originalImage = (BufferedImage) iioImageList.get(imageIndex).getRenderedImage();
        stack.push(originalImage);
        if (dialog.showDialog() == JOptionPane.CANCEL_OPTION) {
            //restore image
            originalImage = stack.pop();
            imageIcon = new ImageIconScalable(originalImage);
            imageList.set(imageIndex, imageIcon);
            iioImageList.get(imageIndex).setRenderedImage(originalImage);
            displayImage();
        }
    }

    @Override
    void jMenuItemGammaActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        SliderDialog dialog = new SliderDialog(this, true);
        dialog.setLabelText(bundle.getString("Gamma"));
        dialog.setForGamma();
        dialog.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(SliderDialog.VALUE_CHANGED)) {
                    int value = (Integer) evt.getNewValue();
                    imageIcon = new ImageIconScalable(net.sourceforge.vietocr.util.ImageHelper.gamma(originalImage, value * 0.1f));
                    imageList.set(imageIndex, imageIcon);
                    iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
                    displayImage();
                }
            }
        });

        originalImage = (BufferedImage) iioImageList.get(imageIndex).getRenderedImage();
        stack.push(originalImage);
        if (dialog.showDialog() == JOptionPane.CANCEL_OPTION) {
            //restore image
            originalImage = stack.pop();
            imageIcon = new ImageIconScalable(originalImage);
            imageList.set(imageIndex, imageIcon);
            iioImageList.get(imageIndex).setRenderedImage(originalImage);
            displayImage();
        }      
    }

    @Override
    void jMenuItemThresholdActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DoubleSliderDialog dialog = new DoubleSliderDialog(this, true);
        dialog.setLabelText(bundle.getString("Threshold"));
        dialog.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(DoubleSliderDialog.VALUE_CHANGED)) {
                    int value1 = (Integer) evt.getOldValue();
                    int value2 = (Integer) evt.getNewValue();
                    imageIcon = new ImageIconScalable(net.sourceforge.vietocr.util.ImageHelper.adaptiveThreshold(originalImage, 
                            value1 * (0.75f / 100f) + 0.25f, (100 - value2) * (1.65f / 100f) + 0.25f));
                    imageList.set(imageIndex, imageIcon);
                    iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
                    displayImage();
                }
            }
        });

        originalImage = (BufferedImage) iioImageList.get(imageIndex).getRenderedImage();
        stack.push(originalImage);
        if (dialog.showDialog() == JOptionPane.CANCEL_OPTION) {
            //restore image
            originalImage = stack.pop();
            imageIcon = new ImageIconScalable(originalImage);
            imageList.set(imageIndex, imageIcon);
            iioImageList.get(imageIndex).setRenderedImage(originalImage);
            displayImage();
        }
    }
    
    @Override
    void jMenuItemGrayscaleActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        originalImage = (BufferedImage) iioImageList.get(imageIndex).getRenderedImage();
        stack.push(originalImage);
        imageIcon = new ImageIconScalable(ImageHelper.convertImageToGrayscale(ImageHelper.cloneImage(originalImage)));
        imageList.set(imageIndex, imageIcon);
        iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
        displayImage();
    }

    @Override
    void jMenuItemMonochromeActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        originalImage = (BufferedImage) iioImageList.get(imageIndex).getRenderedImage();
        stack.push(originalImage);
        imageIcon = new ImageIconScalable(ImageHelper.convertImageToBinary(ImageHelper.cloneImage(originalImage)));
        imageList.set(imageIndex, imageIcon);
        iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
        displayImage();
    }

    @Override
    void jMenuItemInvertActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            originalImage = (BufferedImage) iioImageList.get(imageIndex).getRenderedImage();
            stack.push(originalImage);
            imageIcon = new ImageIconScalable(ImageHelper.invertImageColor(ImageHelper.cloneImage(originalImage)));
            imageList.set(imageIndex, imageIcon);
            iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
            displayImage();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), APP_NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    void jMenuItemSharpenActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        originalImage = (BufferedImage) iioImageList.get(imageIndex).getRenderedImage();
        stack.push(originalImage);
        imageIcon = new ImageIconScalable(net.sourceforge.vietocr.util.ImageHelper.sharpen(originalImage));
        imageList.set(imageIndex, imageIcon);
        iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
        displayImage();
    }

    @Override
    void jMenuItemSmoothActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        originalImage = (BufferedImage) iioImageList.get(imageIndex).getRenderedImage();
        stack.push(originalImage);
        imageIcon = new ImageIconScalable(net.sourceforge.vietocr.util.ImageHelper.smoothen(originalImage));
        imageList.set(imageIndex, imageIcon);
        iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
        displayImage();
    }

    @Override
    void jMenuItemBilateralFilteringActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getGlassPane().setVisible(true);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                originalImage = (BufferedImage) iioImageList.get(imageIndex).getRenderedImage();
                stack.push(originalImage);
                imageIcon = new ImageIconScalable(net.sourceforge.vietocr.util.ImageHelper.bilateralFiltering(originalImage, 3.0, 3.0));
                imageList.set(imageIndex, imageIcon);
                iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());

                displayImage();
                
                getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                getGlassPane().setVisible(false);
            }
        });
    }

    @Override
    void jMenuItemUndoActionPerformed(java.awt.event.ActionEvent evt) {
        if (stack.isEmpty()) {
            return;
        }
        BufferedImage image = stack.pop();
        imageIcon = new ImageIconScalable(image);
        imageList.set(imageIndex, imageIcon);
        iioImageList.get(imageIndex).setRenderedImage(image);
        displayImage();
    }

    @Override
    void clearStack() {
        stack.clear();
    }

    @Override
    void jCheckBoxMenuItemScreenshotModeActionPerformed(java.awt.event.ActionEvent evt) {
        jLabelScreenshotModeValue.setText(this.jCheckBoxMenuItemScreenshotMode.isSelected() ? "On" : "Off");
    }

    @Override
    void quit() {
        prefs.putBoolean(strScreenshotMode, this.jCheckBoxMenuItemScreenshotMode.isSelected());
        super.quit();
    }
}
