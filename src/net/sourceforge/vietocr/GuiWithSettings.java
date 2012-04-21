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

import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.imageio.IIOImage;
import javax.swing.*;
import javax.swing.Timer;
import net.sourceforge.vietocr.utilities.*;
import net.sourceforge.vietocr.postprocessing.*;

public class GuiWithSettings extends GuiWithLaF {

    private final String strWatchFolder = "WatchFolder";
    private final String strOutputFolder = "OutputFolder";
    private final String strWatchEnabled = "WatchEnabled";
    private String watchFolder;
    private String outputFolder;
    private boolean watchEnabled;
    private StatusFrame statusFrame;
    private OptionsDialog optionsDialog;
    private Watcher watcher;

    public GuiWithSettings() {
        watchFolder = prefs.get(strWatchFolder, System.getProperty("user.home"));
        outputFolder = prefs.get(strOutputFolder, System.getProperty("user.home"));
        watchEnabled = prefs.getBoolean(strWatchEnabled, false);

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
                if (imageFile != null) {
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
    void jMenuItemOptionsActionPerformed(java.awt.event.ActionEvent evt) {
        if (optionsDialog == null) {
            optionsDialog = new OptionsDialog(this, true);
        }

        optionsDialog.setWatchFolder(watchFolder);
        optionsDialog.setOutputFolder(outputFolder);
        optionsDialog.setWatchEnabled(watchEnabled);
        optionsDialog.setTessPath(tessPath);
        optionsDialog.setDangAmbigsPath(dangAmbigsPath);
        optionsDialog.setDangAmbigsEnabled(dangAmbigsOn);
        optionsDialog.setCurLangCode(curLangCode);

        if (optionsDialog.showDialog() == JOptionPane.OK_OPTION) {
            watchFolder = optionsDialog.getWatchFolder();
            outputFolder = optionsDialog.getOutputFolder();
            watchEnabled = optionsDialog.isWatchEnabled();

            if (!tessPath.equals(optionsDialog.getTessPath())) {
                tessPath = optionsDialog.getTessPath();
                JOptionPane.showMessageDialog(this, bundle.getString("Please_restart_the_application_for_the_change_to_take_effect."), Gui.APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            }
            dangAmbigsPath = optionsDialog.getDangAmbigsPath();
            dangAmbigsOn = optionsDialog.isDangAmbigsEnabled();

            watcher.setPath(new File(watchFolder));
            watcher.setEnabled(watchEnabled);
        }
    }

    @Override
    void jMenuItemDownloadLangDataActionPerformed(java.awt.event.ActionEvent evt) {
        DownloadDialog dialog = new DownloadDialog(this, true);
        dialog.setVisible(true);
    }

    @Override
    void quit() {
        prefs.put(strWatchFolder, watchFolder);
        prefs.put(strOutputFolder, outputFolder);
        prefs.putBoolean(strWatchEnabled, watchEnabled);
        super.quit();
    }

    @Override
    void changeUILanguage(final Locale locale) {
        super.changeUILanguage(locale);

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (optionsDialog != null) {
                    optionsDialog.changeUILanguage(locale);
                }

                statusFrame.setTitle(bundle.getString("statusFrame.Title"));
            }
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        selectedUILang = prefs.get(strUILanguage, "en");
        Locale.setDefault(getLocale(selectedUILang));

        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                new GuiWithSettings().setVisible(true);
            }
        });
    }
}
