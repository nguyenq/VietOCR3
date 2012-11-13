/**
 * Copyright
 *
 * @ 2008 Quan Nguyen
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

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
        if (bulkDialog == null) {
            bulkDialog = new BulkDialog(this, true);
        }

        bulkDialog.setImageFolder(inputFolder);
        bulkDialog.setOutputFolder(bulkOutputFolder);

        if (bulkDialog.showDialog() == JOptionPane.OK_OPTION) {
            inputFolder = bulkDialog.getImageFolder();
            bulkOutputFolder = bulkDialog.getBulkOutputFolder();
            this.jMenuItemBulkOCR.setText("Stop Bulk OCR");

            File[] files = new File(inputFolder).listFiles(new FilenameFilter() {
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
        }
        this.jMenuItemBulkOCR.setText(bundle.getString("jMenuItemBulkOCR.Text"));
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
//                performOCR(file);
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
