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
import javax.swing.*;
import javax.swing.Timer;
import net.sourceforge.vietocr.utilities.Watcher;

public class GuiWithBatch extends GuiWithSettings {

    private StatusFrame statusFrame;
    private Watcher watcher;

    public GuiWithBatch() {
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
                statusFrame.getTextArea().append("\t** " + bundle.getString("Please_select_a_language.") + " **\n");
//                        queue.clear();
                return;
            }

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        OCRHelper.performOCR(imageFile, new File(outputFolder, imageFile.getName() + ".txt"), tessPath, curLangCode, selectedPSM);
                    } catch (Exception e) {
                        statusFrame.getTextArea().append("\t** " + bundle.getString("Cannotprocess") + " " + imageFile.getName() + " **\n");
                    }
                }
            });
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
