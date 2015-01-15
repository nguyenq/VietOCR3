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

import java.awt.Cursor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sourceforge.vietocr.components.ImageIconScalable;
//import net.sourceforge.tess4j.util.ImageIOHelper;
import net.sourceforge.vietocr.wia.*;

import uk.co.mmscomputing.device.scanner.*;
import uk.co.mmscomputing.device.sane.*;

public class GuiWithScan extends GuiWithThumbnail implements ScannerListener {

    Scanner scanner;

    private final static Logger logger = Logger.getLogger(GuiWithScan.class.getName());

    /**
     * Access scanner and scan documents via Windows WIA or Linux Sane.
     *
     */
    @Override
    void jMenuItemScanActionPerformed(java.awt.event.ActionEvent evt) {
        scaleX = scaleY = 1f;

        jLabelStatus.setText(bundle.getString("Scanning..."));
        jProgressBar1.setIndeterminate(true);
        jProgressBar1.setString(bundle.getString("Scanning..."));
        jProgressBar1.setVisible(true);
        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getGlassPane().setVisible(true);
        jMenuItemScan.setEnabled(false);
        jButtonScan.setEnabled(false);

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    if (WINDOWS) {
                        File tempImageFile = File.createTempFile("tmp", WINDOWS ? ".bmp" : ".png");

                        if (tempImageFile.exists()) {
                            tempImageFile.delete();
                        }
                        WiaScannerAdapter adapter = new WiaScannerAdapter(); // with MS WIA
                        // The reason for not using PNG format is that jai-imageio library would throw an "I/O error reading PNG header" error.
                        tempImageFile = adapter.ScanImage(FormatID.wiaFormatBMP, tempImageFile.getCanonicalPath());
                        openFile(tempImageFile);
                        tempImageFile.deleteOnExit();
                    } else { // Linux
                        scanner = Scanner.getDevice();
                        scanner.addListener(GuiWithScan.this);
                        scanner.acquire();
                    }
                } catch (ScannerIOException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    JOptionPane.showMessageDialog(null, e.getMessage(), "Error Scanning Image", JOptionPane.ERROR_MESSAGE);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    JOptionPane.showMessageDialog(null, e.getMessage(), "I/O Error", JOptionPane.ERROR_MESSAGE);
                } catch (WiaOperationException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    JOptionPane.showMessageDialog(null, e.getWIAMessage(), e.getMessage(), JOptionPane.WARNING_MESSAGE);
                } catch (Exception e) {
                    String msg = e.getMessage();
                    if (msg == null || msg.equals("")) {
                        msg = "Scanner Operation Error.";
                    }
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    JOptionPane.showMessageDialog(null, msg, "Scanner Operation Error", JOptionPane.ERROR_MESSAGE);
                } finally {
//                    if (WINDOWS) {
                        scanCompleted();
//                    }
                }
            }
        });
    }

    /**
     * Sane scanning.
     * 
     * @param type
     * @param metadata 
     */
    @Override
    public void update(ScannerIOMetadata.Type type, ScannerIOMetadata metadata) {
        if (type.equals(ScannerIOMetadata.ACQUIRED)) {
            BufferedImage scannedImage = metadata.getImage();

            try {
                iioImageList = ImageIOHelper.getIIOImageList(scannedImage);
                imageList = ImageIconScalable.getImageList(iioImageList);
                loadImage();
                setTitle("Scanned image - " + APP_NAME);
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                JOptionPane.showMessageDialog(null, e.getMessage(), "I/O Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                scanCompleted();
            }
        } else if (type.equals(ScannerIOMetadata.NEGOTIATE)) {
            SaneDevice device = (SaneDevice) metadata.getDevice();
            try {
                device.setResolution(300);
                device.setOption("mode", "True Gray");
                device.setOption("source", "FlatBed");
            } catch (Exception e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        } else if (type.equals(ScannerIOMetadata.STATECHANGE)) {
            System.out.println(metadata.getStateStr());
            if (metadata.getStateStr().equals("CLOSED")) {
                scanCompleted();
            }
        } else if (type.equals(ScannerIOMetadata.EXCEPTION)) {
            logger.log(Level.SEVERE, metadata.getException().getMessage(), metadata.getException());
        }
    }

    void scanCompleted() {
        jLabelStatus.setText(bundle.getString("Scanning_completed"));
        jProgressBar1.setIndeterminate(false);
        jProgressBar1.setString(bundle.getString("Scanning_completed"));
        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        getGlassPane().setVisible(false);
        jMenuItemScan.setEnabled(true);
        jButtonScan.setEnabled(true);
    }
}
