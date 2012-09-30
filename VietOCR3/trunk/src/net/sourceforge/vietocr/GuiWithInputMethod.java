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
import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;
import net.sourceforge.vietpad.inputmethod.InputMethods;
import net.sourceforge.vietpad.inputmethod.VietKeyListener;

/**
 * Integrates Vietnamese input methods.
 * 
 */
public class GuiWithInputMethod extends GuiWithFormat {

    private final String strInputMethod = "inputMethod";
    private String selectedInputMethod;

    /**
     * Constructor.
     */
    public GuiWithInputMethod() {
        selectedInputMethod = prefs.get(strInputMethod, "Telex");

        initComponents();
    }

    /**
     * Builds a menu for input method selection.
     */
    private void initComponents() {
        VietKeyListener keyLst = new VietKeyListener(jTextArea1);
        jTextArea1.addKeyListener(keyLst);
        VietKeyListener.setInputMethod(InputMethods.valueOf(selectedInputMethod));
        VietKeyListener.setSmartMark(true);
        VietKeyListener.consumeRepeatKey(true);
        boolean vie = curLangCode.startsWith("vie");
        VietKeyListener.setVietModeEnabled(vie);

        ActionListener imlst = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                selectedInputMethod = ae.getActionCommand();
                VietKeyListener.setInputMethod(InputMethods.valueOf(selectedInputMethod));
            }
        };

        // build Input Method submenu
        ButtonGroup groupInputMethod = new ButtonGroup();
        for (InputMethods im : InputMethods.values()) {
            String inputMethod = im.name();
            JRadioButtonMenuItem radioItem = new JRadioButtonMenuItem(inputMethod, selectedInputMethod.equals(inputMethod));
            radioItem.addActionListener(imlst);
            jMenuInputMethod.add(radioItem);
            groupInputMethod.add(radioItem);
        }
    }

    @Override
    void quit() {
        prefs.put(strInputMethod, selectedInputMethod);

        super.quit();
    }
}
