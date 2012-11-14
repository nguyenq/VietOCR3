/**
 * Copyright 2008 Quan Nguyen
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
import java.awt.Frame;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import net.sourceforge.vietocr.postprocessing.Processor;
import net.sourceforge.vietocr.postprocessing.TextUtilities;

public class GuiWithBulkOCR extends GuiWithPostprocess {

    private OcrWorker ocrWorker;
    private StatusFrame statusFrame;
    private BulkDialog bulkDialog;
    private final String strInputFolder = "InputFolder";
    private final String strBulkOutputFolder = "BulkOutputFolder";
    private String inputFolder;
    private String bulkOutputFolder;

    public GuiWithBulkOCR() {
        inputFolder = prefs.get(strInputFolder, System.getProperty("user.home"));
        bulkOutputFolder = prefs.get(strBulkOutputFolder, System.getProperty("user.home"));
        statusFrame = new StatusFrame();
        statusFrame.setTitle(bundle.getString("statusFrame.Title"));
    }

    @Override
    protected void jMenuItemBulkOCRActionPerformed(java.awt.event.ActionEvent evt) {
        if (ocrWorker != null && !ocrWorker.isDone()) {
            // Cancel current OCR op to begin a new one. You want only one OCR op at a time.
            ocrWorker.cancel(true);
            ocrWorker = null;
            return;
        }

        if (bulkDialog == null) {
            bulkDialog = new BulkDialog(this, true);
        }

        bulkDialog.setImageFolder(inputFolder);
        bulkDialog.setOutputFolder(bulkOutputFolder);

        if (bulkDialog.showDialog() == JOptionPane.OK_OPTION) {
            inputFolder = bulkDialog.getImageFolder();
            bulkOutputFolder = bulkDialog.getBulkOutputFolder();

            jLabelStatus.setText(bundle.getString("OCR_running..."));
            jProgressBar1.setIndeterminate(true);
            jProgressBar1.setString(bundle.getString("OCR_running..."));
            jProgressBar1.setVisible(true);
            getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            getGlassPane().setVisible(true);
            jMenuItemBulkOCR.setText("Cancel Bulk OCR");

            if (!statusFrame.isVisible()) {
                statusFrame.setVisible(true);
            }
            if (statusFrame.getExtendedState() == Frame.ICONIFIED) {
                statusFrame.setExtendedState(Frame.NORMAL);
            }
            statusFrame.toFront();
            
            File[] files = new File(inputFolder).listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().matches(".*\\.(tif|tiff|jpg|jpeg|gif|png|bmp|pdf)$");
                }
            });

            // instantiate SwingWorker for OCR
            ocrWorker = new OcrWorker(files);
            ocrWorker.execute();
        }
    }

    @Override
    void changeUILanguage(final Locale locale) {
        super.changeUILanguage(locale);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                statusFrame.setTitle(bundle.getString("statusFrame.Title"));
                if (bulkDialog != null) {
                    bulkDialog.changeUILanguage(locale);
                }
            }
        });
    }

    private void performOCR(File imageFile) {
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

            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(bulkOutputFolder, imageFile.getName() + ".txt")), UTF8));
            out.write(result);
            out.close();
        } catch (Exception e) {
            statusFrame.getTextArea().append("    **  " + bundle.getString("Cannotprocess") + " " + imageFile.getName() + "  **\n");
        } finally {
            //clean up working files
            if (tempTiffFiles != null) {
                for (File f : tempTiffFiles) {
                    f.delete();
                }
            }
        }
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
                if (!isCancelled()) {
                    publish(file.getName()); // interim result
                    performOCR(file);
                }
            }
            return null;
        }

        @Override
        protected void process(List<String> results) {
            for (String str : results) {
                statusFrame.getTextArea().append(str + "\n");
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
//                ignore.printStackTrace();
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
//                e.printStackTrace();
//                    System.err.println(why);
                jLabelStatus.setText(null);
                jProgressBar1.setString(null);
                JOptionPane.showMessageDialog(null, why, "OCR Operation", JOptionPane.ERROR_MESSAGE);
            } catch (java.util.concurrent.CancellationException e) {
                jLabelStatus.setText("OCR " + bundle.getString("canceled"));
                jProgressBar1.setString("OCR " + bundle.getString("canceled"));
            } finally {
                jMenuItemBulkOCR.setText(bundle.getString("jMenuItemBulkOCR.Text"));
                getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                getGlassPane().setVisible(false);
            }
        }
    }
}
