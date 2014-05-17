/**
 * Copyright 2012 Quan Nguyen
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
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import net.sourceforge.vietocr.utilities.Utilities;

public class GuiWithBulkOCR extends GuiWithPostprocess {

    private BulkOcrWorker ocrWorker;
    private final StatusFrame statusFrame;
    private BulkDialog bulkDialog;
    private final String strInputFolder = "InputFolder";
    private final String strBulkOutputFolder = "BulkOutputFolder";
    private final String strBulkOutputFormat = "BulkOutputFormat";
    private String inputFolder;
    private String outputFolder;
    private String outputFormat;

    public GuiWithBulkOCR() {
        inputFolder = prefs.get(strInputFolder, System.getProperty("user.home"));
        outputFolder = prefs.get(strBulkOutputFolder, System.getProperty("user.home"));
        outputFormat = prefs.get(strBulkOutputFormat, "txt");
        statusFrame = new StatusFrame();
        statusFrame.setTitle(bundle.getString("bulkStatusFrame.Title"));
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

        bulkDialog.setInputFolder(inputFolder);
        bulkDialog.setOutputFolder(outputFolder);
        bulkDialog.setSelectedOutputFormat(outputFormat);

        if (bulkDialog.showDialog() == JOptionPane.OK_OPTION) {
            inputFolder = bulkDialog.getInputFolder();
            outputFolder = bulkDialog.getOutputFolder();
            outputFormat = bulkDialog.getSelectedOutputFormat();

            jLabelStatus.setText(bundle.getString("OCR_running..."));
            jProgressBar1.setIndeterminate(true);
            jProgressBar1.setString(bundle.getString("OCR_running..."));
            jProgressBar1.setVisible(true);
            getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            getGlassPane().setVisible(true);
            jMenuItemBulkOCR.setText(bundle.getString("Cancel_Bulk_OCR"));

            if (!statusFrame.isVisible()) {
                statusFrame.setVisible(true);
            }
            if (statusFrame.getExtendedState() == Frame.ICONIFIED) {
                statusFrame.setExtendedState(Frame.NORMAL);
            }
            statusFrame.toFront();
            statusFrame.getTextArea().append("\t-- " + bundle.getString("Beginning_of_task") + " --\n");

            List<File> files = new ArrayList<File>();
            Utilities.listImageFiles(files, new File(inputFolder));

            // instantiate SwingWorker for OCR
            ocrWorker = new BulkOcrWorker(files);
            ocrWorker.execute();
        }
    }

    @Override
    void changeUILanguage(final Locale locale) {
        super.changeUILanguage(locale);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                statusFrame.setTitle(bundle.getString("bulkStatusFrame.Title"));
                if (bulkDialog != null) {
                    bulkDialog.changeUILanguage(locale);
                }
            }
        });
    }

    @Override
    void quit() {
        prefs.put(strInputFolder, inputFolder);
        prefs.put(strBulkOutputFolder, outputFolder);
        prefs.put(strBulkOutputFormat, outputFormat);

        super.quit();
    }

    /**
     * A worker class for managing bulk OCR process.
     */
    class BulkOcrWorker extends SwingWorker<Void, String> {

        long startTime;
        List<File> files;

        BulkOcrWorker(List<File> files) {
            this.files = files;
            startTime = System.currentTimeMillis();
        }

        @Override
        protected Void doInBackground() throws Exception {
            for (File imageFile : files) {
                if (!isCancelled()) {
                    publish(imageFile.getPath()); // interim result
                    try {
                        String outputFilename = imageFile.getPath().substring(inputFolder.length() + 1);
                        OCRHelper.performOCR(imageFile, new File(outputFolder, outputFilename), tessPath, curLangCode, selectedPSM, outputFormat);
                    } catch (Exception e) {
                        publish("\t** " + bundle.getString("Cannotprocess") + " " + imageFile.getName() + " **");
                    }
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
                statusFrame.getTextArea().append("\t-- " + bundle.getString("End_of_task") + " --\n");
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
//                    System.err.println(why);
                jLabelStatus.setText(null);
                jProgressBar1.setString(null);
                JOptionPane.showMessageDialog(null, why, "OCR Operation", JOptionPane.ERROR_MESSAGE);
            } catch (java.util.concurrent.CancellationException e) {
                jLabelStatus.setText("OCR " + bundle.getString("canceled"));
                jProgressBar1.setString("OCR " + bundle.getString("canceled"));
                statusFrame.getTextArea().append("\t-- " + bundle.getString("Task_canceled") + " --\n");
            } finally {
                jMenuItemBulkOCR.setText(bundle.getString("jMenuItemBulkOCR.Text"));
                getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                getGlassPane().setVisible(false);
                long millis = System.currentTimeMillis() - startTime;
                String elapsedTime = String.format("%02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toHours(millis),
                        TimeUnit.MILLISECONDS.toMinutes(millis),
                        TimeUnit.MILLISECONDS.toSeconds(millis)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
                statusFrame.getTextArea().append("\t" + bundle.getString("Elapsed_time") + ": " + elapsedTime + "\n");
            }
        }
    }
}
