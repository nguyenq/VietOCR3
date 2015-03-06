/**
 * Copyright @ 2014 Quan Nguyen
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.List;
import static javax.swing.Action.LARGE_ICON_KEY;
//import static javax.swing.Action.SHORT_DESCRIPTION;
import javax.swing.*;

import net.sourceforge.vietocr.components.JImageLabel;

public class GuiWithThumbnail extends Gui {

    LoadThumbnailWorker loadWorker;
    ButtonGroup group = new ButtonGroup();

    @Override
    void loadThumbnails() {
        jPanelThumb.removeAll();
        Enumeration<AbstractButton> buttons = group.getElements();
        while (buttons.hasMoreElements()) {
            group.remove(buttons.nextElement());
        }
        loadWorker = new LoadThumbnailWorker();
        loadWorker.execute();
    }

    /**
     * SwingWorker class that loads the images in a background thread and calls
     * publish when a new one is ready to be displayed.
     *
     * Adapted from Java Tutorial's IconDemoApp example.
     */
    private class LoadThumbnailWorker extends SwingWorker<Void, ThumbnailAction> {

        /**
         * Creates thumbnail versions of the target image files.
         */
        @Override
        protected Void doInBackground() throws Exception {
            for (int i = 0; i < imageList.size(); i++) {
                ImageIcon thumbnailIcon = new ImageIcon(imageList.get(i).getScaledImage(85, 110));
                ThumbnailAction thumbAction = new ThumbnailAction(thumbnailIcon, i, bundle.getString("Page_") + (i + 1));
                publish(thumbAction);
            }
            return null;
        }

        /**
         * Loads thumbnails and associated labels into panel.
         */
        @Override
        protected void process(List<ThumbnailAction> chunks) {

            for (ThumbnailAction thumbAction : chunks) {
                jPanelThumb.add(Box.createRigidArea((new Dimension(0, 7))));
                JToggleButton thumbButton = new JToggleButton(thumbAction);
                group.add(thumbButton);

                thumbButton.setMargin(new Insets(0, 0, 0, 0)); // remove paddings
//                Border innerBorder = BorderFactory.createEmptyBorder(0, 0, 1, 0);
//                Border outerBorder = new LineBorder(Color.black, 1, false);
//                Border compoundBorder = BorderFactory.createCompoundBorder(outerBorder, innerBorder);
//                thumbButton.setBorder(compoundBorder);
                thumbButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                jPanelThumb.add(thumbButton);

                JLabel label = new JLabel(String.valueOf(thumbAction.getIndex() + 1));
                label.setAlignmentX(Component.CENTER_ALIGNMENT);
                jPanelThumb.add(label);
            }
            jPanelThumb.revalidate();
        }
    };

    /**
     * Action class that shows the image specified.
     */
    private class ThumbnailAction extends AbstractAction {

        int index;

        /**
         * @param Icon - The thumbnail to show in the button.
         * @param String - The description of the icon.
         */
        public ThumbnailAction(Icon thumb, int index, String desc) {
            this.index = index;

            // The short description becomes the tooltip of a button.
//            putValue(SHORT_DESCRIPTION, desc);

            // The LARGE_ICON_KEY is the key for setting the
            // icon when an Action is applied to a button.
            putValue(LARGE_ICON_KEY, thumb);
        }

        public int getIndex() {
            return index;
        }

        /**
         * Shows the full image in the image panel.
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (imageIndex == index) {
                return;
            }
            imageIndex = index;
            ((JImageLabel) jImageLabel).deselect();
            jLabelStatus.setText(null);
            jProgressBar1.setString(null);
            jProgressBar1.setVisible(false);
            jComboBoxPageNum.setSelectedItem(imageIndex + 1);
        }
    }
}
