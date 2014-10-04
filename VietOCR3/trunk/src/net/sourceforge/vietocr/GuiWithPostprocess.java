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
import java.io.File;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import net.sourceforge.vietocr.postprocessing.Processor;

/**
 * Applies post-OCR processing to correct common OCR errors.
 *
 */
public class GuiWithPostprocess extends GuiWithOCR {

    private final String strDangAmbigsPath = "DangAmbigsPath";
    private final String strDangAmbigs = "DangAmbigs";
    protected String dangAmbigsPath;
    protected boolean dangAmbigsOn;

    public GuiWithPostprocess() {
        dangAmbigsPath = prefs.get(strDangAmbigsPath, new File(baseDir, "data").getPath());
        dangAmbigsOn = prefs.getBoolean(strDangAmbigs, true);
    }

    @Override
    void jMenuItemPostProcessActionPerformed(java.awt.event.ActionEvent evt) {
        if (curLangCode == null) {
            return;
        }

        jLabelStatus.setText(bundle.getString("Correcting_errors..."));
        jProgressBar1.setIndeterminate(true);
        jProgressBar1.setString(bundle.getString("Correcting_errors..."));
        jProgressBar1.setVisible(true);
        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getGlassPane().setVisible(true);
        this.jMenuItemPostProcess.setEnabled(false);

        SwingWorker<String, Void> correctWorker = new SwingWorker<String, Void>() {

            String selectedText;

            @Override
            public String doInBackground() throws Exception {
                selectedText = jTextArea1.getSelectedText();
                return Processor.postProcess((selectedText != null) ? selectedText : jTextArea1.getText(), curLangCode, dangAmbigsPath, dangAmbigsOn);
            }

            @Override
            public void done() {
                jProgressBar1.setIndeterminate(false);

                try {
                    String result = get();

                    if (selectedText != null) {
                        int start = jTextArea1.getSelectionStart();
                        jTextArea1.replaceSelection(result);
                        jTextArea1.select(start, start + result.length());
                    } else {
                        jTextArea1.setText(result);
                    }
                    jLabelStatus.setText(bundle.getString("Correction_completed"));
                    jProgressBar1.setString(bundle.getString("Correction_completed"));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (java.util.concurrent.ExecutionException e) {
                    String why;
                    Throwable cause = e.getCause();
                    if (cause != null) {
                        if (cause instanceof UnsupportedOperationException) {
                            why = String.format(bundle.getString("Post-processing_not_supported_for_language"), jComboBoxLang.getSelectedItem(), curLangCode);
                        } else if (cause instanceof RuntimeException) {
                            why = cause.getMessage();
                        } else {
                            why = cause.getMessage();
                        }
                    } else {
                        why = e.getMessage();
                    }
//                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, why, APP_NAME, JOptionPane.ERROR_MESSAGE);
                    jProgressBar1.setVisible(false);
                } finally {
                    getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    getGlassPane().setVisible(false);
                    jMenuItemPostProcess.setEnabled(true);
                }
            }
        };
        correctWorker.execute();
    }

    @Override
    void quit() {
        prefs.put(strDangAmbigsPath, dangAmbigsPath);
        prefs.putBoolean(strDangAmbigs, dangAmbigsOn);

        super.quit();
    }
}
