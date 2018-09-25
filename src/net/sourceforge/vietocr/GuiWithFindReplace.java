/**
 * Copyright @ 2018 Quan Nguyen
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

import java.util.Locale;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import net.sourceforge.vietpad.components.JFindDialog;

public class GuiWithFindReplace extends GuiWithPostprocess {

    private JDialog findDialog;
    private final static Logger logger = Logger.getLogger(GuiWithFindReplace.class.getName());

    @Override
    void jButtonFindActionPerformed(java.awt.event.ActionEvent evt) {
        if (findDialog == null) {
            findDialog = new JFindDialog(this, false, this.jTextArea1);
            findDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            findDialog.setLocationRelativeTo(this);
        }
        findDialog.setVisible(true);
    }

    @Override
    void changeUILanguage(final Locale locale) {
        super.changeUILanguage(locale);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (findDialog != null) {
                    ((JFindDialog) findDialog).changeUILanguage(locale);
                }
            }
        });
    }
}