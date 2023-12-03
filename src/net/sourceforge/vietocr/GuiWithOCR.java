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

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.swing.*;

import net.sourceforge.vietocr.components.JImageLabel;

public class GuiWithOCR extends GuiWithImageOps {

    private OcrWorker ocrWorker;
    protected String selectedPSM = "3"; // 3 - Fully automatic page segmentation, but no OSD (default)
    protected String selectedOEM = "3"; // Default, based on what is available
    protected boolean tessLibEnabled;

    private final static Logger logger = Logger.getLogger(GuiWithOCR.class.getName());

    @Override
    void jMenuItemOCRActionPerformed(java.awt.event.ActionEvent evt) {
        if (jImageLabel.getIcon() == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<Rectangle> rois = ((JImageLabel) jImageLabel).getROIs();

        if (rois != null && !rois.isEmpty()) {
            // ROIs applicable to current page only
            try {
                int index = 0;
                for (Rectangle rect : rois) {
                    if (rect == null) {
                        index++;
                        continue;
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
                    Rectangle recaledRect = new Rectangle((int) ((rect.x - offsetX) * scaleX), (int) ((rect.y - offsetY) * scaleY), (int) (rect.width * scaleX), (int) (rect.height * scaleY));
                    rois.set(index++, recaledRect);
                }

                performOCR(iioImageList, inputfilename, imageIndex, Arrays.asList(rois));
            } catch (RasterFormatException rfe) {
                logger.log(Level.SEVERE, rfe.getMessage(), rfe);
                JOptionPane.showMessageDialog(this, rfe.getMessage(), APP_NAME, JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        } else {
            performOCR(iioImageList, inputfilename, imageIndex, null);
        }
    }

    @Override
    void jMenuItemOCRAllActionPerformed(java.awt.event.ActionEvent evt) {
        if (this.jImageLabel.getIcon() == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_load_an_image."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        this.jButtonOCR.setVisible(false);
        this.jButtonCancelOCR.setVisible(true);
        this.jButtonCancelOCR.setEnabled(true);
        performOCR(iioImageList, inputfilename, -1, null);
    }

    /**
     * Perform OCR on images represented by IIOImage.
     *
     * @param iioImageList list of IIOImage
     * @param inputfilename input filename
     * @param index Index of page to be OCRed: -1 for all pages
     * @param roiss list of list of regions of interest
     */
    void performOCR(final List<IIOImage> iioImageList, final String inputfilename, final int index, final List<List<Rectangle>> roiss) {
        if (curLangCode.trim().length() == 0) {
            JOptionPane.showMessageDialog(this, bundle.getString("Please_select_a_language."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        jLabelStatus.setText(bundle.getString("OCR_running..."));
        jProgressBar1.setIndeterminate(true);
        jProgressBar1.setString(bundle.getString("OCR_running..."));
        jProgressBar1.setVisible(true);
        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getGlassPane().setVisible(true);
        this.jButtonOCR.setEnabled(false);
        this.jMenuItemOCR.setEnabled(false);
        this.jMenuItemOCRAll.setEnabled(false);

        OCRImageEntity entity = new OCRImageEntity(iioImageList, inputfilename, index, roiss, this.jCheckBoxMenuItemDoubleSidedPage.isSelected(), curLangCode);
        entity.setScreenshotMode(this.jCheckBoxMenuItemScreenshotMode.isSelected());

        // instantiate SwingWorker for OCR
        ocrWorker = new OcrWorker(entity);
        ocrWorker.execute();
    }

    @Override
    void jButtonCancelOCRActionPerformed(java.awt.event.ActionEvent evt) {
        if (ocrWorker != null && !ocrWorker.isDone()) {
            // Cancel current OCR op to begin a new one. You want only one OCR op at a time.
            ocrWorker.cancel(true);
            ocrWorker = null;
        }

        this.jButtonCancelOCR.setEnabled(false);
    }

    /**
     * A worker class for managing OCR process.
     */
    class OcrWorker extends SwingWorker<Void, String> {

        OCRImageEntity entity;

        OcrWorker(OCRImageEntity entity) {
            this.entity = entity;
        }

        @Override
        protected Void doInBackground() throws Exception {
            String lang = entity.getLanguage();
            OCR<IIOImage> ocrEngine = new OCRImages(); // for Tess4J
            ocrEngine.setDatapath(datapath);
            ocrEngine.setPageSegMode(selectedPSM);
            ocrEngine.setLanguage(lang);
            List<IIOImage> imageList = entity.getSelectedOimages();
            List<List<Rectangle>> roiss = entity.getROIss();

            for (int i = 0; i < imageList.size(); i++) {
                // Could send all images at once for recognition but doing so would not allow cancellation of long-running task; thus, individual images (pages) are being processed here.
                if (!isCancelled()) {
                    String result = ocrEngine.recognizeText(imageList.subList(i, i + 1), entity.getInputfilename(), roiss == null ? null : roiss.subList(i, i + 1));
                    publish(result); // interim result
                }
            }

            return null;
        }

        @Override
        protected void process(List<String> results) {
            for (String str : results) {
                jTextArea1.append(str);
                jTextArea1.setCaretPosition(jTextArea1.getDocument().getLength());
            }
        }

        @Override
        protected void done() {
            jProgressBar1.setIndeterminate(false);

            try {
                get(); // dummy method
                jLabelStatus.setText(bundle.getString("OCR_completed."));
                jProgressBar1.setString(bundle.getString("OCR_completed."));
            } catch (InterruptedException ignore) {
                logger.log(Level.WARNING, ignore.getMessage(), ignore);
            } catch (java.util.concurrent.ExecutionException e) {
                String why;
                Throwable cause = e.getCause();
                if (cause != null) {
                    if (cause instanceof IOException) {
                        why = bundle.getString("Cannot_find_Tesseract._Please_set_its_path.");
                    } else if (cause instanceof FileNotFoundException) {
                        why = bundle.getString("An_exception_occurred_in_Tesseract_engine_while_recognizing_this_image.");
                    } else if (cause instanceof OutOfMemoryError) {
                        why = cause.getMessage();
                    } else if (cause instanceof ClassCastException) {
                        why = cause.getMessage();
                        why += "\nConsider converting the image to binary or grayscale before OCR again.";
                    } else {
                        why = cause.getMessage();
                    }
                } else {
                    why = e.getMessage();
                }

                logger.log(Level.SEVERE, why, e);
                jLabelStatus.setText(null);
                jProgressBar1.setString(null);
                JOptionPane.showMessageDialog(null, why, "OCR Operation", JOptionPane.ERROR_MESSAGE);
            } catch (java.util.concurrent.CancellationException e) {
                String msg = "OCR " + bundle.getString("canceled");
                logger.log(Level.WARNING, msg);
                jLabelStatus.setText(msg);
                jProgressBar1.setString(msg);
            } finally {
                getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                getGlassPane().setVisible(false);
                jButtonOCR.setVisible(true);
                jButtonOCR.setEnabled(true);
                jMenuItemOCR.setEnabled(true);
                jMenuItemOCRAll.setEnabled(true);
                jButtonCancelOCR.setVisible(false);
            }
        }
    }
}
