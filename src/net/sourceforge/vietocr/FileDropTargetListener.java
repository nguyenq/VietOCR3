/**
 * Copyright @ 2007 Quan Nguyen
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

import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.*;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  File Drop Target Listener
 *
 *@author     Quan Nguyen (nguyenq@users.sf.net)
 *@version    1.1, 23 December 2007
 *@see        http://vietpad.sourceforge.net
 */
public class FileDropTargetListener extends DropTargetAdapter {
    private final Gui holder;
    private File droppedFile;
    
    private final static Logger logger = Logger.getLogger(FileDropTargetListener.class.getName());
    
    /**
     *  Constructor for the FileDropTargetListener object
     *
     *
     * @param holder  instance of Gui
     */
    public FileDropTargetListener(Gui holder) {
        this.holder = holder;
    }
    
    /**
     *  Gives visual feedback
     *
     *@param  dtde  the DropTargetDragEvent
     */
    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        if (droppedFile == null) {
            DataFlavor[] flavors = dtde.getCurrentDataFlavors();
            for (DataFlavor flavor : flavors) {
                if (flavor.isFlavorJavaFileListType()) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    return;
                }
            }
        }
        dtde.rejectDrag();
    }
    
    /**
     *  Handles dropped files
     *
     *@param  dtde  the DropTargetDropEvent
     */
    @Override
    public void drop(DropTargetDropEvent dtde) {
        Transferable transferable = dtde.getTransferable();
        DataFlavor[] flavors = transferable.getTransferDataFlavors();
        
        final boolean LINUX = System.getProperty("os.name").equals("Linux");
        
        for (DataFlavor flavor : flavors) {
            try {
                if (flavor.equals(DataFlavor.javaFileListFlavor) || (LINUX && flavor.getPrimaryType().equals("text") && flavor.getSubType().equals("uri-list"))) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    // Missing DataFlavor.javaFileListFlavor on Linux (Bug ID: 4899516)
                    if (flavor.equals(DataFlavor.javaFileListFlavor)) {
                        java.util.List fileList = (java.util.List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        droppedFile = (File) fileList.get(0);
                    } else {
                        // This workaround is for File DnD on Linux
                        String string =
                                transferable.getTransferData(DataFlavor.stringFlavor).toString().replaceAll("\r\n?", "\n");
                        URI uri = new URI(string.substring(0, string.indexOf('\n')));
                        droppedFile = new File(uri);
                    }
                    // Note: On Windows, Java 1.4.2 can't recognize a Unicode file name
                    // (Bug ID 4896217). Fixed in Java 1.5.
                    
                    // Processes one dropped file at a time in a separate thread
                    new Thread() {
                        @Override
                        public void run() {
                            holder.openFile(droppedFile);
                            droppedFile = null;
                        }
                    }.start();
                    dtde.dropComplete(true);
                    return;
                }
            }catch (Exception e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                dtde.rejectDrop();
            }
        }
        dtde.dropComplete(false);
    }
}
