/**
 * Copyright @ 2021 Quan Nguyen
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
package net.sourceforge.vietocr.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractButton;
import net.sourceforge.tess4j.ITesseract;

/**
 * Allows only one PDF option selected.
 */
public class OuputFormatCheckBoxActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent event) {
        AbstractButton menuItem = (AbstractButton) event.getSource();
        if (menuItem.getModel().isSelected()) {
            String label = menuItem.getText();
            Container parent = menuItem.getParent();
            if (ITesseract.RenderedFormat.PDF.name().equals(label)) {
                AbstractButton menuItem1 = (AbstractButton) getChild(parent, ITesseract.RenderedFormat.PDF_TEXTONLY.name());
                if (menuItem1 != null) 
                    menuItem1.setSelected(false);
            } else if (ITesseract.RenderedFormat.PDF_TEXTONLY.name().equals(label)) {
                AbstractButton menuItem1 = (AbstractButton) getChild(parent, ITesseract.RenderedFormat.PDF.name());
                if (menuItem1 != null) 
                    menuItem1.setSelected(false);
            }
        }
    }

    private Component getChild(Container container, String text) {
        for (Component child : container.getComponents()) {
            if (text.equals(((AbstractButton) child).getText())) {
                return child;
            }
        }

        return null;
    }
}
