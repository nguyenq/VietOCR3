/**
 * Copyright @ 2017 Quan Nguyen
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

public class GuiWithOEM extends GuiWithPSM {
    private final String strOEM = "OcrEngineMode";

    public GuiWithOEM() {
        selectedOEM = prefs.get(strOEM, "3");
        this.jLabelOEMvalue.setText(enumOf(selectedOEM));

        ActionListener oemLst = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                selectedOEM = ae.getActionCommand();
                jLabelOEMvalue.setText(enumOf(selectedOEM));
            }
        };

        // build PageSegMode submenu
        ButtonGroup groupOEM = new ButtonGroup();
        for (OcrEngineMode mode : OcrEngineMode.values()) {
            JRadioButtonMenuItem radioItem = new JRadioButtonMenuItem(mode.getDesc(), mode.getVal().equals(selectedOEM));
            radioItem.setActionCommand(mode.getVal());
            radioItem.addActionListener(oemLst);
            groupOEM.add(radioItem);
            this.jMenuOEM.add(radioItem);
        }
    }

    /**
     * Gets accessible name of OcrEngineMode enum.
     * 
     * @param val
     * @return 
     */
    private String enumOf(String val) {
        return OcrEngineMode.enumOf(val).name().replace("OEM_", "").replace("_", " ");
    }

    @Override
    void quit() {
        prefs.put(strOEM, selectedOEM);

        super.quit();
    }
}
