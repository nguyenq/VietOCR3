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

public class GuiWithPSM extends GuiWithBatch {
    private final String strPSM = "PageSegMode";

    public GuiWithPSM() {
        selectedPSM = prefs.get(strPSM, "3");
        this.jLabelPSMvalue.setText(enumOf(selectedPSM));

        ActionListener psmLst = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                selectedPSM = ae.getActionCommand();
                jLabelPSMvalue.setText(enumOf(selectedPSM));
            }
        };

        // build PageSegMode submenu
        ButtonGroup groupPSM = new ButtonGroup();
        for (PageSegMode mode : PageSegMode.values()) {
            JRadioButtonMenuItem radioItem = new JRadioButtonMenuItem(mode.getDesc(), mode.getVal().equals(selectedPSM));
            radioItem.setActionCommand(mode.getVal());
            radioItem.addActionListener(psmLst);
            groupPSM.add(radioItem);
            this.jMenuPSM.add(radioItem);
        }
    }

    /**
     * Gets accessible name of PageSegMode enum.
     * 
     * @param val
     * @return 
     */
    private String enumOf(String val) {
        return PageSegMode.enumOf(val).name().replace("PSM_", "").replace("_", " ");
    }

    @Override
    void quit() {
        prefs.put(strPSM, selectedPSM);

        super.quit();
    }
}
