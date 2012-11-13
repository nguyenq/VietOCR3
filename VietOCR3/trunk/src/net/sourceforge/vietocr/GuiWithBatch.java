/**
 * Copyright @ 2012 Quan Nguyen
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

import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;
import javax.imageio.IIOImage;
import javax.swing.*;
import javax.swing.Timer;
import net.sourceforge.vietocr.postprocessing.Processor;
import net.sourceforge.vietocr.postprocessing.TextUtilities;
import net.sourceforge.vietocr.utilities.Watcher;

public class GuiWithBatch extends GuiWithSettings {

    private StatusFrame statusFrame;
    private Watcher watcher;
    private boolean executeBatch;
    private BatchDialog batchDialog;
    private final String strWatchFolder = "WatchFolder";
    private final String strOutputFolder = "OutputFolder";

    public GuiWithBatch() {
        watchFolder = prefs.get(strWatchFolder, System.getProperty("user.home"));
        outputFolder = prefs.get(strOutputFolder, System.getProperty("user.home"));
        this.jMenuItemExecuteBatch.setEnabled(watchEnabled);
        statusFrame = new StatusFrame();
        statusFrame.setTitle(bundle.getString("statusFrame.Title"));

        // watch for new image files
        final Queue<File> queue = new LinkedList<File>();
        watcher = new Watcher(queue, new File(watchFolder));
        watcher.setEnabled(watchEnabled);

        Thread t = new Thread(watcher);
        t.start();

        // autoOCR if there are files in the queue
        Action autoOcrAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final File imageFile = queue.poll();
                if (imageFile != null && imageFile.exists()) {
                    if (!statusFrame.isVisible()) {
                        statusFrame.setVisible(true);
                    }

                    statusFrame.getTextArea().append(imageFile.getPath() + "\n");

                    if (curLangCode == null) {
                        statusFrame.getTextArea().append("    **  " + bundle.getString("Please_select_a_language.") + "  **\n");
//                        queue.clear();
                        return;
                    }

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            List<File> tempTiffFiles = null;

                            try {
                                OCR<File> ocrEngine = new OCRFiles(tessPath);
                                ocrEngine.setPageSegMode(selectedPSM);
                                List<IIOImage> iioImageList = ImageIOHelper.getIIOImageList(imageFile);
                                tempTiffFiles = ImageIOHelper.createTiffFiles(iioImageList, -1);
                                String result = ocrEngine.recognizeText(tempTiffFiles, curLangCode);

                                // postprocess to correct common OCR errors
                                result = Processor.postProcess(result, curLangCode);
                                // correct common errors caused by OCR
                                result = TextUtilities.correctOCRErrors(result);
                                // correct letter cases
                                result = TextUtilities.correctLetterCases(result);

                                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputFolder, imageFile.getName() + ".txt")), UTF8));
                                out.write(result);
                                out.close();
                            } catch (Exception e) {
                                statusFrame.getTextArea().append("    **  " + bundle.getString("Cannotprocess") + " " + imageFile.getName() + "  **\n");
                                e.printStackTrace();
                            } finally {
                                //clean up working files
                                if (tempTiffFiles != null) {
                                    for (File f : tempTiffFiles) {
                                        f.delete();
                                    }
                                }
                            }
                        }
                    });
                }
            }
        };

        new Timer(5000, autoOcrAction).start();
    }

    @Override
    protected void jMenuItemExecuteBatchActionPerformed(java.awt.event.ActionEvent evt) {
        executeBatch ^= true;

        if (!executeBatch) {
            this.jMenuItemExecuteBatch.setText("Execute Batch...");
            
            // abort currently executing OCR task.
            
            
            return;
        }

        if (batchDialog == null) {
            batchDialog = new BatchDialog(this, true);
        }

        batchDialog.setImageFolder(watchFolder);
        batchDialog.setOutputFolder(outputFolder);

        if (batchDialog.showDialog() == JOptionPane.OK_OPTION) {
            watchFolder = batchDialog.getImageFolder();
            outputFolder = batchDialog.getOutputFolder();
            this.jMenuItemExecuteBatch.setText("Stop Batch");
            
            //  execute batch
        }
    }

    @Override
    protected void updateWatch(String watchFolder, boolean watchEnabled) {
        watcher.setPath(new File(watchFolder));
        watcher.setEnabled(watchEnabled);
    }

    @Override
    void changeUILanguage(final Locale locale) {
        super.changeUILanguage(locale);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                statusFrame.setTitle(bundle.getString("statusFrame.Title"));
            }
        });
    }
}
