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
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Deque;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.sourceforge.tess4j.util.ImageHelper;
import net.sourceforge.vietocr.components.ImageIconScalable;
import net.sourceforge.vietocr.util.FixedSizeStack;

public class GuiWithImage extends GuiWithBulkOCR {

    private final String strScreenshotMode = "ScreenshotMode";
    private static final double MINIMUM_DESKEW_THRESHOLD = 0.05d;
    private BufferedImage originalImage;
    Deque<BufferedImage> stack = new FixedSizeStack<BufferedImage>(10);

    GuiWithImage() {
        this.jCheckBoxMenuItemScreenshotMode.setSelected(prefs.getBoolean(strScreenshotMode, false));
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
        stack.push(originalImage);
        imageIcon = new ImageIconScalable(net.sourceforge.vietocr.util.ImageHelper.autoCrop(originalImage));
        imageList.set(imageIndex, imageIcon);
        iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
        displayImage();
        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        getGlassPane().setVisible(false);
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
    void quit() {
        prefs.putBoolean(strScreenshotMode, this.jCheckBoxMenuItemScreenshotMode.isSelected());

        super.quit();
    }
}
