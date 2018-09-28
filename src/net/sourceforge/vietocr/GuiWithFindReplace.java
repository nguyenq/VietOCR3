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
import net.sourceforge.vietpad.components.JFindReplaceDialog;

public class GuiWithFindReplace extends GuiWithPostprocess {

    private JDialog frDialog;
    private final static Logger logger = Logger.getLogger(GuiWithFindReplace.class.getName());

    @Override
    void jButtonFindActionPerformed(java.awt.event.ActionEvent evt) {
        if (frDialog == null) {
            frDialog = new JFindReplaceDialog(this, false, this.jTextArea1);
            frDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            frDialog.setLocationRelativeTo(this);
        }
        frDialog.setVisible(true);
    }

    @Override
    void changeUILanguage(final Locale locale) {
        super.changeUILanguage(locale);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (frDialog != null) {
                    ((JFindReplaceDialog) frDialog).changeUILanguage(locale);
                }
            }
        });
    }
}
