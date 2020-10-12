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

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sourceforge.vietpad.utilities.TextUtilities;
import net.sourceforge.vietpad.components.FontDialog;

public class GuiWithFormat extends GuiWithImage {

    private final String strSelectedCase = "selectedCase";
    private final String strChangeCaseX = "changeCaseX";
    private final String strChangeCaseY = "changeCaseY";
    private ChangeCaseDialog changeCaseDlg;

    private final static Logger logger = Logger.getLogger(GuiWithFormat.class.getName());

    @Override
    void changeUILanguage(final Locale locale) {
        super.changeUILanguage(locale);

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (changeCaseDlg != null) {
                    changeCaseDlg.changeUILanguage(locale);
                }
            }
        });
    }

    @Override
    void jCheckBoxMenuWordWrapActionPerformed(java.awt.event.ActionEvent evt) {
        this.jTextArea1.setLineWrap(wordWrapOn = jCheckBoxMenuWordWrap.isSelected());
    }

    @Override
    void jMenuItemFontActionPerformed(java.awt.event.ActionEvent evt) {
        FontDialog dlg = new FontDialog(this);
        dlg.setAttributes(font);

        Properties prop = new Properties();

        try {
            File xmlFile = new File(baseDir, "data/pangram.xml");
            prop.loadFromXML(new FileInputStream(xmlFile));
            String pangram = prop.getProperty(curLangCode);
            if (pangram == null || pangram.length() == 0) {
                pangram = prop.getProperty("eng");
            }
            dlg.setPreviewText(pangram);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            JOptionPane.showMessageDialog(null, e.getMessage(), APP_NAME, JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        dlg.setVisible(true);
        if (dlg.succeeded()) {
            jTextArea1.setFont(font = dlg.getFont());
            jTextArea1.validate();
        }
    }

    @Override
    void jMenuItemChangeCaseActionPerformed(java.awt.event.ActionEvent evt) {
        if (changeCaseDlg == null) {
            changeCaseDlg = new ChangeCaseDialog(GuiWithFormat.this, false);
            // non-modal
            changeCaseDlg.setSelectedCase(prefs.get(strSelectedCase, "UPPER CASE"));
            changeCaseDlg.setLocation(
                    prefs.getInt(strChangeCaseX, changeCaseDlg.getX()),
                    prefs.getInt(strChangeCaseY, changeCaseDlg.getY()));
        }
        if (jTextArea1.getSelectedText() == null) {
            jTextArea1.selectAll();
        }
        changeCaseDlg.setVisible(true);
    }

    @Override
    void quit() {
        if (changeCaseDlg != null) {
            prefs.put(strSelectedCase, changeCaseDlg.getSelectedCase());
            prefs.putInt(strChangeCaseX, changeCaseDlg.getX());
            prefs.putInt(strChangeCaseY, changeCaseDlg.getY());
        }
        super.quit();
    }

    /**
     * Changes letter case.
     *
     * @param typeOfCase The type that the case should be changed to
     */
    public void changeCase(String typeOfCase) {
        if (jTextArea1.getSelectedText() == null) {
            jTextArea1.selectAll();

            if (jTextArea1.getSelectedText() == null) {
                return;
            }
        }

        String result = TextUtilities.changeCase(jTextArea1.getSelectedText(), typeOfCase);

        undoSupport.beginUpdate();
        int start = jTextArea1.getSelectionStart();
        jTextArea1.replaceSelection(result);
        jTextArea1.setSelectionStart(start);
        jTextArea1.setSelectionEnd(start + result.length());
        undoSupport.endUpdate();
    }

    /**
     * Removes extra line breaks.
     *
     * @param evt
     */
    @Override
    void jMenuItemRemoveLineBreaksActionPerformed(java.awt.event.ActionEvent evt) {
        if (jTextArea1.getSelectedText() == null) {
            jTextArea1.selectAll();

            if (jTextArea1.getSelectedText() == null) {
                return;
            }
        }
        String result = TextUtilities.removeLineBreaks(jTextArea1.getSelectedText(), options.isRemoveHyphens());

        undoSupport.beginUpdate();
        int start = jTextArea1.getSelectionStart();
        jTextArea1.replaceSelection(result);
        jTextArea1.setSelectionStart(start);
        jTextArea1.setSelectionEnd(start + result.length());
        undoSupport.endUpdate();
    }
}
