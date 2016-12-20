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

import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import net.sourceforge.vietpad.utilities.SpellCheckHelper;

public class GuiWithSpellcheck extends GuiWithPSM {

    private int start, end;
    private SpellCheckHelper speller;

    private final static Logger logger = Logger.getLogger(GuiWithSpellcheck.class.getName());

    @Override
    void populatePopupMenuWithSuggestions(Point pointClicked) {
        try {
            popup.removeAll();
            if (this.jToggleButtonSpellCheck.isSelected()) {
                int offset = jTextArea1.viewToModel(pointClicked);
                start = javax.swing.text.Utilities.getWordStart(jTextArea1, offset);
                end = javax.swing.text.Utilities.getWordEnd(jTextArea1, offset);
                String curWord = jTextArea1.getDocument().getText(start, end - start);
                makeSuggestions(curWord);
            }
        } catch (BadLocationException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        } finally {
            // load standard menu items
            repopulatePopupMenu();
        }
    }

    /**
     * Populates suggestions at top of context menu.
     *
     * @param curWord
     */
    void makeSuggestions(final String curWord) {
        if (speller == null || curWord == null || curWord.trim().length() == 0) {
            return;
        }

        List<String> suggests = speller.suggest(curWord);
        if (suggests == null || suggests.isEmpty()) {
            return;
        }

        ActionListener correctLst = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                String selectedSuggestion = ae.getActionCommand();
                if (selectedSuggestion.equals("ignore.word")) {
                    speller.ignoreWord(curWord);
                } else if (selectedSuggestion.equals("add.word")) {
                    speller.addWord(curWord);
                } else {
                    jTextArea1.select(start, end);
                    jTextArea1.replaceSelection(selectedSuggestion);
                }
                speller.spellCheck();
            }
        };

        for (String word : suggests) {
            JMenuItem item = new JMenuItem(word);
            Font itemFont = item.getFont();
            if (itemFont.canDisplayUpTo(word) == -1) {
                item.setFont(itemFont.deriveFont(Font.BOLD));
            } else {
                // use jTextArea's font
                item.setFont(font.deriveFont(Font.BOLD, itemFont.getSize2D()));
            }
            item.addActionListener(correctLst);
            popup.add(item);
        }

        popup.addSeparator();
        JMenuItem item = new JMenuItem(bundle.getString("Ignore_All"));
        item.setActionCommand("ignore.word");
        item.addActionListener(correctLst);
        popup.add(item);
        item = new JMenuItem(bundle.getString("Add_to_Dictionary"));
        item.setActionCommand("add.word");
        item.addActionListener(correctLst);
        popup.add(item);
        popup.addSeparator();
    }

    @Override
    void jToggleButtonSpellCheckActionPerformed(java.awt.event.ActionEvent evt) {
        String localeId = getCurrentLocaleId();
        if (localeId == null) {
            JOptionPane.showMessageDialog(null, "Need to add an entry in data/ISO639-1.xml file.", Gui.APP_NAME, JOptionPane.ERROR_MESSAGE);
            return;
        }

        speller = new SpellCheckHelper(this.jTextArea1, localeId);
        if (this.jToggleButtonSpellCheck.isSelected()) {
            speller.enableSpellCheck();
        } else {
            speller.disableSpellCheck();
        }
        this.jTextArea1.repaint();
    }
}
