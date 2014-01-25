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

import java.awt.Window;
import java.awt.event.*;
import javax.swing.*;

public class GuiWithLaF extends GuiWithUILanguage {

    public GuiWithLaF() {
        initComponents();
    }

    private void initComponents() {
        ActionListener lafLst = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                updateLaF(ae.getActionCommand());
            }
        };

        // build Look and Feel submenu
        ButtonGroup groupLookAndFeel = new ButtonGroup();
        UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
        for (int i = 0; i < lafs.length; i++) {
            JRadioButtonMenuItem lafButton = new JRadioButtonMenuItem(lafs[i].getName());
            lafButton.setActionCommand(lafs[i].getClassName());
            if (UIManager.getLookAndFeel().getClass().getName().equals(lafButton.getActionCommand())) {
                lafButton.setSelected(true);
            }
            lafButton.addActionListener(lafLst);
            groupLookAndFeel.add(lafButton);
            jMenuLookAndFeel.add(lafButton);
        }
    }

    /**
     *  Updates UI component if changes in LAF
     *
     *@param  laf  the look and feel class name
     */
    void updateLaF(String laf) {
        try {
            UIManager.setLookAndFeel(laf);
        } catch (Exception exc) {
            // do nothing
//            exc.printStackTrace();
        }

        for (Window win : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(win);
            win.validate();
        }

        SwingUtilities.updateComponentTreeUI(popup);
        SwingUtilities.updateComponentTreeUI(jFileChooser);
    }
}
