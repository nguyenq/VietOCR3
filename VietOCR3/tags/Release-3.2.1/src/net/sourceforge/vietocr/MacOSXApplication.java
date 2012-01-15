package net.sourceforge.vietocr;

import com.apple.eawt.*;
import com.apple.eawt.AppEvent.*;
import java.io.File;

/**
 *  Mac OS X functionality for VietOCR.
 *
 *@author     Quan Nguyen
 *@modified   February 27, 2011
 */
class MacOSXApplication {

    private final static int ZOOM_LIMIT = 60;
    // http://www.mactech.com/articles/develop/issue_17/Yu_final.html

    Application app = null;

    /**
     *  Constructor for the MacOSXApplication object.
     *
     *@param  vietOCR  calling instance of VietOCR
     */
    public MacOSXApplication(final Gui vietOCR) {
        app = Application.getApplication();

//        vietOCR.setMaximizedBounds(new Rectangle(
//                Math.max(vietOCR.getWidth(), vietOCR.font.getSize() * ZOOM_LIMIT),
//                Integer.MAX_VALUE));
//        vietOCR.scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
//        vietOCR.scrollPane.setBorder(null); // line up scrollbars with grow box
//        vietOCR.m_toolBar.setBorder(BorderFactory.createCompoundBorder(
//                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0.5765F, 0.5765F, 0.5765F)),
//                vietOCR.jToolBar.getBorder()));
        
//        app.setDefaultMenuBar(vietOCR.getJMenuBar());
        app.setAboutHandler(new AboutHandler() {

            @Override
            public void handleAbout(AboutEvent ae) {
                vietOCR.about();
            }
        });

        app.setOpenFileHandler(new OpenFilesHandler() {

            @Override
            public void openFiles(OpenFilesEvent ofe) {
                File droppedFile = ofe.getFiles().get(0);
                if (droppedFile.isFile() && vietOCR.promptToSave()) {
                    vietOCR.openFile(droppedFile);
                }
            }
        });

        app.setPreferencesHandler(new PreferencesHandler() {

            @Override
            public void handlePreferences(PreferencesEvent pe) {
                vietOCR.jMenuItemOptionsActionPerformed(null);
            }
        });

        app.setQuitHandler(new QuitHandler() {

            @Override
            public void handleQuitRequestWith(QuitEvent qe, QuitResponse qr) {
                vietOCR.quit();
            }
        });
    }
}
