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
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.sourceforge.vietocr.components.ImageIconScalable;
import net.sourceforge.vietocr.utilities.*;

public class GuiWithImage extends GuiWithBulkOCR {

    private final String strScreenshotMode = "ScreenshotMode";
    private static final double MINIMUM_DESKEW_THRESHOLD = 0.05d;

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
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }

    @Override
    void jMenuItemBrightnessActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        SliderDialog dialog = new SliderDialog(this, true);
        if (dialog.showDialog() == JOptionPane.OK_OPTION) {
            imageIcon = new ImageIconScalable(net.sourceforge.vietocr.utilities.ImageHelper.brighten((BufferedImage) iioImageList.get(imageIndex).getRenderedImage(), 1f));
            imageList.set(imageIndex, imageIcon);
            iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
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
        if (dialog.showDialog() == JOptionPane.OK_OPTION) {
            // Do nothing for now.
            // Initial plan was to implement various image manipulation operations
            // (rotate, flip, sharpen, brighten, threshold, clean up,...) here.
        }
    }

    @Override
    void jMenuItemGrayscaleActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        imageIcon = new ImageIconScalable(net.sourceforge.vietocr.utilities.ImageHelper.convertImageToGrayscale((BufferedImage) iioImageList.get(imageIndex).getRenderedImage()));
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
        imageIcon = new ImageIconScalable(net.sourceforge.vietocr.utilities.ImageHelper.convertImageToBinary((BufferedImage) iioImageList.get(imageIndex).getRenderedImage()));
        imageList.set(imageIndex, imageIcon);
        iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
        displayImage();
    }

    @Override
    void jMenuItemInvertedActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            imageIcon = new ImageIconScalable(net.sourceforge.vietocr.utilities.ImageHelper.invertImageColor((BufferedImage) iioImageList.get(imageIndex).getRenderedImage()));
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
    
    }

    @Override
    void jMenuItemSmoothActionPerformed(java.awt.event.ActionEvent evt) {
        if (iioImageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

    }

    @Override
    void quit() {
        prefs.putBoolean(strScreenshotMode, this.jCheckBoxMenuItemScreenshotMode.isSelected());

        super.quit();
    }
}
