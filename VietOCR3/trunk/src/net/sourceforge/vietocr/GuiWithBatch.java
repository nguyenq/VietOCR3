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

import java.awt.Cursor;
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

    private OcrWorker ocrWorker;
    private StatusFrame statusFrame;
    private Watcher watcher;
    private boolean executeBatch;
    private BatchDialog batchDialog;
    private final String strImageFolder = "ImageFolder";
    private final String strBulkOutputFolder = "BulkOutputFolder";
    private String imageFolder;
    private String bulkOutputFolder;

    public GuiWithBatch() {
        imageFolder = prefs.get(strImageFolder, System.getProperty("user.home"));
        bulkOutputFolder = prefs.get(strBulkOutputFolder, System.getProperty("user.home"));
        this.jMenuItemBulkOCR.setEnabled(watchEnabled);
        statusFrame = new StatusFrame();
        statusFrame.setTitle(bundle.getString("statusFrame.Title"));

        // watch for new image files
        final Queue<File> queue = new LinkedList<File>();
        watcher = new Watcher(queue, new File(imageFolder));
        watcher.setEnabled(watchEnabled);

        Thread t = new Thread(watcher);
        t.start();

        // autoOCR if there are files in the queue
        Action autoOcrAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final File imageFile = queue.poll();
                performOCR(imageFile);
            }
        };

        new Timer(5000, autoOcrAction).start();
    }

    private void performOCR(final File imageFile) {
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

    @Override
    protected void jMenuItemBulkOCRActionPerformed(java.awt.event.ActionEvent evt) {
        executeBatch ^= true;

        if (!executeBatch) {
            this.jMenuItemBulkOCR.setText(bundle.getString("jMenuItemBulkOCR.Text"));
            // abort currently executing Bulk OCR task.
            // How ???
            return;
        }

        if (batchDialog == null) {
            batchDialog = new BatchDialog(this, true);
        }

        batchDialog.setImageFolder(imageFolder);
        batchDialog.setOutputFolder(bulkOutputFolder);

        if (batchDialog.showDialog() == JOptionPane.OK_OPTION) {
            imageFolder = batchDialog.getImageFolder();
            bulkOutputFolder = batchDialog.getBulkOutputFolder();
            this.jMenuItemBulkOCR.setText("Stop Bulk OCR");

            File[] files = new File(imageFolder).listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().matches(".*\\.(tif|tiff|jpg|jpeg|gif|png|bmp|pdf)$");
                }
            });

            // will need to put this long execution task in a swingwork
            //  execute bulk
            // instantiate SwingWorker for OCR
            ocrWorker = new OcrWorker(files);
            ocrWorker.execute();
//            for (File file : files) {
//                performOCR(file);
//            }
        }
        executeBatch = false;
        this.jMenuItemBulkOCR.setText(bundle.getString("jMenuItemBulkOCR.Text"));
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

    /**
     * A worker class for managing OCR process.
     */
    class OcrWorker extends SwingWorker<Void, String> {

        File[] files;

        OcrWorker(File[] files) {
            this.files = files;
        }

        @Override
        protected Void doInBackground() throws Exception {
            for (File file : files) {
                performOCR(file);
            }
            return null;
        }

        @Override
        protected void process(List<String> results) {
            for (String str : results) {
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
                ignore.printStackTrace();
            } catch (java.util.concurrent.ExecutionException e) {
                String why = null;
                Throwable cause = e.getCause();
                if (cause != null) {
                    if (cause instanceof IOException) {
                        why = bundle.getString("Cannot_find_Tesseract._Please_set_its_path.");
                    } else if (cause instanceof FileNotFoundException) {
                        why = bundle.getString("An_exception_occurred_in_Tesseract_engine_while_recognizing_this_image.");
                    } else if (cause instanceof OutOfMemoryError) {
                        why = cause.getMessage();
                    } else {
                        why = cause.getMessage();
                    }
                } else {
                    why = e.getMessage();
                }
                e.printStackTrace();
//                    System.err.println(why);
                jLabelStatus.setText(null);
                jProgressBar1.setString(null);
                JOptionPane.showMessageDialog(null, why, "OCR Operation", JOptionPane.ERROR_MESSAGE);
            } catch (java.util.concurrent.CancellationException e) {
                jLabelStatus.setText("OCR " + bundle.getString("canceled"));
                jProgressBar1.setString("OCR " + bundle.getString("canceled"));
            } finally {
                getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                getGlassPane().setVisible(false);
            }
        }
    }
}
