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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;

public class GuiWithUILanguage extends GuiWithInputMethod {

    public GuiWithUILanguage() {

        initComponents();
    }

    private void initComponents() {

        ActionListener uiLangLst = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (!selectedUILang.equals(ae.getActionCommand())) {
                    selectedUILang = ae.getActionCommand();
                    changeUILanguage(getLocale(selectedUILang));
                }
            }
        };

        // build UI Language submenu
        ButtonGroup groupUILang = new ButtonGroup();
        String[] uiLangs = getInstalledUILangs();
        for (int i = 0; i < uiLangs.length; i++) {
            Locale locale = new Locale(uiLangs[i]);
            JRadioButtonMenuItem uiLangButton = new JRadioButtonMenuItem(locale.getDisplayLanguage(), selectedUILang.equals(locale.getLanguage()));
            uiLangButton.setActionCommand(locale.getLanguage());
            uiLangButton.addActionListener(uiLangLst);
            groupUILang.add(uiLangButton);
            jMenuUILang.add(uiLangButton);
        }
    }

    private String[] getInstalledUILangs() {
        String[] locales = {"en", "it", "lt", "sk", "vi"};
        return locales;
    }
}
