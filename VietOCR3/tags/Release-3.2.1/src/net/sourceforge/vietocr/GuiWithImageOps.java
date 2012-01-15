/**
 * Copyright @ 2008 Quan Nguyen
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

import java.awt.image.BufferedImage;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.sourceforge.vietocr.components.JImageLabel;

public class GuiWithImageOps extends GuiWithScan {

    private final float ZOOM_FACTOR = 1.25f;

    @Override
    void jButtonPrevPageActionPerformed(java.awt.event.ActionEvent evt) {
        ((JImageLabel) jImageLabel).deselect();
        imageIndex--;
        if (imageIndex < 0) {
            imageIndex = 0;
        } else {
            this.jLabelStatus.setText(null);
            jProgressBar1.setString(null);
            jProgressBar1.setVisible(false);
            displayImage();
        }
        setButton();
    }

    @Override
    void jButtonNextPageActionPerformed(java.awt.event.ActionEvent evt) {
        ((JImageLabel) jImageLabel).deselect();
        imageIndex++;
        if (imageIndex > imageTotal - 1) {
            imageIndex = imageTotal - 1;
        } else {
            this.jLabelStatus.setText(null);
            jProgressBar1.setString(null);
            jProgressBar1.setVisible(false);
            displayImage();
        }
        setButton();
    }

    @Override
    void jButtonFitImageActionPerformed(java.awt.event.ActionEvent evt) {
        this.jButtonFitImage.setEnabled(false);
        this.jButtonActualSize.setEnabled(true);
        this.jButtonZoomIn.setEnabled(false);
        this.jButtonZoomOut.setEnabled(false);
        ((JImageLabel) jImageLabel).deselect();
        curScrollPos = this.jScrollPane2.getViewport().getViewPosition();
        scaleX = (float) originalW / (float) this.jScrollPane2.getWidth();
        scaleY = (float) originalH / (float) this.jScrollPane2.getHeight();
        fitImageChange(this.jScrollPane2.getWidth(), this.jScrollPane2.getHeight());
        isFitImageSelected = true;
    }

    @Override
    void jButtonActualSizeActionPerformed(java.awt.event.ActionEvent evt) {
        this.jButtonFitImage.setEnabled(true);
        this.jButtonActualSize.setEnabled(false);
        this.jButtonZoomIn.setEnabled(true);
        this.jButtonZoomOut.setEnabled(true);
        ((JImageLabel) jImageLabel).deselect();
        fitImageChange(originalW, originalH);
        scaleX = scaleY = 1f;
        isFitImageSelected = false;
    }

    @Override
    void jButtonZoomOutActionPerformed(java.awt.event.ActionEvent evt) {
        ((JImageLabel) jImageLabel).deselect();
        doChange(false);
        isFitImageSelected = false;
    }

    @Override
    void jButtonZoomInActionPerformed(java.awt.event.ActionEvent evt) {
        ((JImageLabel) jImageLabel).deselect();
        doChange(true);
        isFitImageSelected = false;
    }

    void doChange(final boolean isZoomIn) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                int width = imageIcon.getIconWidth();
                int height = imageIcon.getIconHeight();

                if (isZoomIn) {
                    imageIcon.setScaledSize((int) (width * ZOOM_FACTOR), (int) (height * ZOOM_FACTOR));
                } else {
                    imageIcon.setScaledSize((int) (width / ZOOM_FACTOR), (int) (height / ZOOM_FACTOR));
                }
                jImageLabel.revalidate();
                jScrollPane2.repaint();

                if (isZoomIn) {
                    scaleX /= ZOOM_FACTOR;
                    scaleY /= ZOOM_FACTOR;
                } else {
                    scaleX *= ZOOM_FACTOR;
                    scaleY *= ZOOM_FACTOR;
                }
            }
        });
    }

    @Override
    void jButtonRotateCCWActionPerformed(java.awt.event.ActionEvent evt) {
        rotateImage(270d);
    }

    @Override
    void jButtonRotateCWActionPerformed(java.awt.event.ActionEvent evt) {
        rotateImage(90d);
    }

    void rotateImage(double angle) {
        try {
            imageIcon = imageList.get(imageIndex).getRotatedImageIcon(Math.toRadians(angle));
            imageList.set(imageIndex, imageIcon); // persist the rotated image
            iioImageList.get(imageIndex).setRenderedImage((BufferedImage) imageIcon.getImage());
            displayImage();
        } catch (OutOfMemoryError oome) {
            JOptionPane.showMessageDialog(this, oome.getMessage(), bundle.getString("OutOfMemoryError"), JOptionPane.ERROR_MESSAGE);
        }
    }
}
