/**
 * Copyright @ 2008 Quan Nguyen
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

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.DropTarget;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.channels.*;
import java.text.*;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.undo.*;
import net.sourceforge.tess4j.ITessAPI;

import net.sourceforge.tess4j.util.ImageHelper;
import net.sourceforge.tess4j.util.ImageIOHelper;
import net.sourceforge.vietocr.components.*;
import net.sourceforge.vietocr.util.FormLocalizer;
import net.sourceforge.vietocr.util.Utils;
import net.sourceforge.vietpad.components.*;
import net.sourceforge.vietpad.inputmethod.VietKeyListener;

public class Gui extends JFrame {

    public static final String APP_NAME = "VietOCR";
    public static final String TESSERACT_PATH = "tesseract-ocr";
    public static final String TO_BE_IMPLEMENTED = "To be implemented in subclass";
    static final boolean MAC_OS_X = System.getProperty("os.name").startsWith("Mac");
    static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
    static final boolean LINUX = System.getProperty("os.name").equals("Linux");
    protected final File supportDir = new File(System.getProperty("user.home")
            + (MAC_OS_X ? "/Library/Application Support/" + APP_NAME : "/." + APP_NAME.toLowerCase()));
    static final String UTF8 = "UTF-8";
    static final String strUILanguage = "UILanguage";
    static final String TESSDATA = "tessdata";
    private static final String strLookAndFeel = "lookAndFeel";
    private static final String strWindowState = "windowState";
    private static final String strLangCode = "langCode";
    private static final String strTessDir = "TesseractDirectory";
    private static final String strMruList = "MruList";
    private static final String strFrameWidth = "frameWidth";
    private static final String strFrameHeight = "frameHeight";
    private static final String strFrameX = "frameX";
    private static final String strFrameY = "frameY";
    private static final String strCurrentDirectory = "currentDirectory";
    private static final String strOutputDirectory = "outputDirectory";
    private static final String strFontName = "fontName";
    private static final String strFontSize = "fontSize";
    private static final String strFontStyle = "fontStyle";
    private static final String strWordWrap = "wordWrap";
    private static final String strFilterIndex = "filterIndex";
    private static final String strSegmentedRegions = "SegmentedRegions";
    private static final String strSegmentedRegionsPara = "SegmentedRegionsPara";
    private static final String strSegmentedRegionsTextLine = "SegmentedRegionsTextLine";
    private static final String strSegmentedRegionsSymbol = "SegmentedRegionsSymbol";
    private static final String strSegmentedRegionsBlock = "SegmentedRegionsBlock";
    private static final String strSegmentedRegionsWord = "SegmentedRegionsWord";
    public final String EOL = System.getProperty("line.separator");
    static final Preferences prefs = Preferences.userRoot().node("/net/sourceforge/vietocr3");
    protected static String curLangCode = "eng";
    protected static String selectedUILang = "en";
    protected static Properties lookupISO639;
    protected static Properties lookupISO_3_1_Codes;
    private int filterIndex;
    private FileFilter[] fileFilters;
    protected Font font;
    private final Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    protected int imageIndex;
    int imageTotal;
    List<ImageIconScalable> imageList;
    protected List<IIOImage> iioImageList;
    protected String inputfilename;
    protected ResourceBundle bundle;
    private String currentDirectory;
    private String outputDirectory;
    protected String tessPath;
    private String[] installedLanguageCodes;
    protected String[] installedLanguages;
    ImageIconScalable imageIcon;
    boolean isFitImageSelected;
    protected boolean wordWrapOn;
    protected float scaleX = 1f;
    protected float scaleY = 1f;
    int originalW, originalH;
    Point curScrollPos;
    private File textFile;
    private java.util.List<String> mruList = new java.util.ArrayList<String>();
    private String strClearRecentFiles;
    private boolean textChanged = true;
    private RawListener rawListener;
    private final String DATAFILE_SUFFIX = ".traineddata";
    protected final File baseDir = Utils.getBaseDir(Gui.this);
    protected String datapath;
    private static final int FONT_MIN_SIZE = 6;
    private static final int FONT_MAX_SIZE = 50;

    private final static Logger logger = Logger.getLogger(Gui.class.getName());

    /**
     * Creates new form.
     */
    public Gui() {
        try {
            UIManager.setLookAndFeel(prefs.get(strLookAndFeel, UIManager.getSystemLookAndFeelClassName()));
        } catch (Exception e) {
            // keep default LAF
            logger.log(Level.WARNING, e.getMessage(), e);
        }

        bundle = java.util.ResourceBundle.getBundle("net.sourceforge.vietocr.Gui");
        initComponents();

        if (MAC_OS_X) {
            new MacOSXApplication(Gui.this);

            // remove Exit menuitem
            this.jMenuFile.remove(this.jSeparatorExit);
            this.jMenuFile.remove(this.jMenuItemExit);

            // remove About menuitem
            this.jMenuHelp.remove(this.jSeparatorAbout);
            this.jMenuHelp.remove(this.jMenuItemAbout);

            // remove Options menuitem
            this.jMenuSettings.remove(this.jSeparatorOptions);
            this.jMenuSettings.remove(this.jMenuItemOptions);
        }

        getInstalledLanguagePacks();
        populateOCRLanguageBox();

        if (!supportDir.exists()) {
            supportDir.mkdirs();
        }

        KeyEventDispatcher dispatcher = new KeyEventDispatcher() {

            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    if (!jTextArea1.isFocusOwner() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V) { // Don't catch this event inside the JTextArea
                        pasteImage(); // Paste image from clipboard
                        e.consume();
                    } else if (!jTextArea1.isFocusOwner() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_LEFT) {
                        jButtonPrevPage.doClick();
                    } else if (!jTextArea1.isFocusOwner() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        jButtonNextPage.doClick();
                    } else if (e.getKeyCode() == KeyEvent.VK_F7) {
                        jToggleButtonSpellCheck.doClick();
                    } else if (e.isControlDown() && e.isShiftDown() && (e.getKeyCode() == KeyEvent.VK_EQUALS || e.getKeyCode() == KeyEvent.VK_ADD)) {
                        jButtonRotateCW.doClick();
                    } else if (e.isControlDown() && e.isShiftDown() && (e.getKeyCode() == KeyEvent.VK_MINUS || e.getKeyCode() == KeyEvent.VK_SUBTRACT)) {
                        jButtonRotateCCW.doClick();
                    } else if (e.isControlDown() && (e.getKeyCode() == KeyEvent.VK_EQUALS || e.getKeyCode() == KeyEvent.VK_ADD)) {
                        jButtonZoomIn.doClick();
                    } else if (e.isControlDown() && (e.getKeyCode() == KeyEvent.VK_MINUS || e.getKeyCode() == KeyEvent.VK_SUBTRACT)) {
                        jButtonZoomOut.doClick();
                    } else if (e.isControlDown() && (e.getKeyCode() == KeyEvent.VK_1 || e.getKeyCode() == KeyEvent.VK_NUMPAD1)) {
                        jButtonActualSize.doClick();
                    } else if (e.isControlDown() && (e.getKeyCode() == KeyEvent.VK_2 || e.getKeyCode() == KeyEvent.VK_NUMPAD2)) {
                        jButtonFitImage.doClick();
                    } else if (jTextArea1.isFocusOwner() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z) {
                        m_undoAction.actionPerformed(null);
                    } else if (jTextArea1.isFocusOwner() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Y) {
                        m_redoAction.actionPerformed(null);
                    } else if (jTextArea1.isFocusOwner() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_X) {
                        actionCut.actionPerformed(null);
                    }
                }

                return false;
            }
        };
        DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);

//        // Assign F7 key to spellcheck
//        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "spellcheck");
//        getRootPane().getActionMap().put("spellcheck", new AbstractAction() {
//
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                jToggleButtonSpellCheck.doClick();
//            }
//        });
    }

    public static String getCurrentLangCode() {
        return curLangCode;
    }

    public static String getCurrentLocaleId() {
        if (lookupISO_3_1_Codes.containsKey(curLangCode)) {
            return lookupISO_3_1_Codes.getProperty(curLangCode);
        } else if (lookupISO_3_1_Codes.containsKey(curLangCode.substring(0, 3))) {
            return lookupISO_3_1_Codes.getProperty(curLangCode.substring(0, 3));
        } else {
            return null;
        }
    }

    @Override
    public List<Image> getIconImages() {
        List<Image> images = new ArrayList<Image>();
        images.add(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/ocr_small.png")).getImage());
        images.add(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/ocr.png")).getImage());
        return images;
    }

    /**
     * Adds Undo support to textarea via context menu.
     */
    private void addUndoSupport() {
        // Undo support
        rawListener = new RawListener();
        this.jTextArea1.getDocument().addUndoableEditListener(rawListener);
        undoSupport.addUndoableEditListener(new SupportListener());
        m_undo.discardAllEdits();
        updateUndoRedo();
        updateCutCopyDelete(false);
    }

    /**
     * Gets Tesseract's installed language data packs.
     */
    private void getInstalledLanguagePacks() {
        if (WINDOWS) {
            tessPath = new File(baseDir, TESSERACT_PATH).getPath();
            datapath = tessPath;
        } else {
            tessPath = prefs.get(strTessDir, "/usr/bin");
            datapath = "/usr/share/tesseract-ocr/";
        }

        lookupISO639 = new Properties();
        lookupISO_3_1_Codes = new Properties();

        try {
            File tessdataDir = new File(tessPath, TESSDATA);
            if (!tessdataDir.exists()) {
                String TESSDATA_PREFIX = System.getenv("TESSDATA_PREFIX");
                if (TESSDATA_PREFIX == null && !WINDOWS) { // if TESSDATA_PREFIX env var not set
                    if (tessPath.equals("/usr/bin")) { // default install path of Tesseract on Linux
                        TESSDATA_PREFIX = "/usr/share/tesseract-ocr/"; // default install path of tessdata on Linux
                    } else {
                        TESSDATA_PREFIX = "/usr/local/share/"; // default make install path of tessdata on Linux
                    }
                }
                tessdataDir = new File(TESSDATA_PREFIX, TESSDATA);
                datapath = TESSDATA_PREFIX;
            }

            installedLanguageCodes = tessdataDir.list(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(DATAFILE_SUFFIX) && !name.equals("osd.traineddata");
                }
            });
            Arrays.sort(installedLanguageCodes, Collator.getInstance());

            File xmlFile = new File(baseDir, "data/ISO639-3.xml");
            lookupISO639.loadFromXML(new FileInputStream(xmlFile));
            xmlFile = new File(baseDir, "data/ISO639-1.xml");
            lookupISO_3_1_Codes.loadFromXML(new FileInputStream(xmlFile));
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            JOptionPane.showMessageDialog(null, e.getMessage(), APP_NAME, JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        } finally {
            if (installedLanguageCodes == null) {
                installedLanguages = new String[0];
            } else {
                installedLanguages = new String[installedLanguageCodes.length];
            }
            for (int i = 0; i < installedLanguages.length; i++) {
                installedLanguageCodes[i] = installedLanguageCodes[i].replace(DATAFILE_SUFFIX, "");
                installedLanguages[i] = lookupISO639.getProperty(installedLanguageCodes[i], installedLanguageCodes[i]);
            }
        }
    }

    /**
     * Populates OCR Language box.
     */
    @SuppressWarnings("unchecked")
    private void populateOCRLanguageBox() {
        if (installedLanguageCodes == null) {
            JOptionPane.showMessageDialog(Gui.this, bundle.getString("Tesseract_is_not_found._Please_specify_its_path_in_Settings_menu."), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DefaultComboBoxModel model = new DefaultComboBoxModel(installedLanguages);
        model.setSelectedItem(null);
        jComboBoxLang.setModel(model);
        jComboBoxLang.setSelectedItem(prefs.get(strLangCode, null));
        final JTextComponent textField = (JTextComponent) jComboBoxLang.getEditor().getEditorComponent();
        textField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                curLangCode = textField.getText();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                curLangCode = textField.getText();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // ignore
            }
        });
    }

    /**
     * Populates MRU List.
     */
    private void populateMRUList() {
        String[] fileNames = prefs.get(strMruList, "").split(File.pathSeparator);
        for (String fileName : fileNames) {
            if (!fileName.equals("")) {
                mruList.add(fileName);
            }
        }
        updateMRUMenu();
    }

    /**
     * Populates PopupMenu with spellcheck suggestions.
     *
     * @param p
     */
    void populatePopupMenuWithSuggestions(Point p) {
        // to be implemented in subclass
    }

    void repopulatePopupMenu() {
        popup.add(m_undoAction);
        popup.add(m_redoAction);
        popup.addSeparator();
        popup.add(actionCut);
        popup.add(actionCopy);
        popup.add(actionPaste);
        popup.add(actionDelete);
        popup.addSeparator();
        popup.add(actionSelectAll);
    }

    private String menuItemStringBuild(String text, String shorcut, int width) {
        AffineTransform at = new AffineTransform();
        FontRenderContext frc = new FontRenderContext(at, true, false);
        Font f = popup.getFont();

        String spaces = "";
        while ((int) (f.getStringBounds(text + spaces + shorcut, frc).getWidth()) < width) {
            spaces += " ";
        }

        return text + spaces + shorcut;
    }

    /**
     * Builds context menu for textarea.
     */
    private void populatePopupMenu() {
        final int POPUP_TEXT_WIDTH = 120;

        m_undoAction = new AbstractAction(menuItemStringBuild(bundle.getString("jMenuItemUndo.Text"), "CTRL+Z", POPUP_TEXT_WIDTH)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    m_undo.undo();
                } catch (CannotUndoException ex) {
                    System.err.println(bundle.getString("Unable_to_undo:_") + ex);
                }
                updateUndoRedo();
            }
        };

        popup.add(m_undoAction);

        m_redoAction = new AbstractAction(menuItemStringBuild(bundle.getString("jMenuItemRedo.Text"), "CTRL+Y", POPUP_TEXT_WIDTH)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    m_undo.redo();
                } catch (CannotRedoException ex) {
                    System.err.println(bundle.getString("Unable_to_redo:_") + ex);
                }
                updateUndoRedo();
            }
        };

        popup.add(m_redoAction);
        popup.addSeparator();

        actionCut = new AbstractAction(menuItemStringBuild(bundle.getString("jMenuItemCut.Text"), "CTRL+X", POPUP_TEXT_WIDTH)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                jTextArea1.cut();
                updatePaste();
            }
        };

        popup.add(actionCut);

        actionCopy = new AbstractAction(menuItemStringBuild(bundle.getString("jMenuItemCopy.Text"), "CTRL+C", POPUP_TEXT_WIDTH)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                jTextArea1.copy();
                updatePaste();
            }
        };

        popup.add(actionCopy);

        actionPaste = new AbstractAction(menuItemStringBuild(bundle.getString("jMenuItemPaste.Text"), "CTRL+V", POPUP_TEXT_WIDTH)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                undoSupport.beginUpdate();
                jTextArea1.paste();
                undoSupport.endUpdate();
            }
        };

        popup.add(actionPaste);

        actionDelete = new AbstractAction(menuItemStringBuild(bundle.getString("jMenuItemDelete.Text"), "Del", POPUP_TEXT_WIDTH)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                jTextArea1.replaceSelection(null);
            }
        };

        popup.add(actionDelete);
        popup.addSeparator();

        actionSelectAll = new AbstractAction(menuItemStringBuild(bundle.getString("jMenuItemSelectAll.Text"), "CTRL+A", POPUP_TEXT_WIDTH)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                jTextArea1.selectAll();
            }
        };

        popup.add(actionSelectAll);
    }

    /**
     * Update MRU Submenu.
     */
    private void updateMRUMenu() {
        this.jMenuRecentFiles.removeAll();

        if (mruList.isEmpty()) {
            this.jMenuRecentFiles.add(bundle.getString("No_Recent_Files"));
        } else {
            Action mruAction = new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    JMenuItem item = (JMenuItem) e.getSource();
                    String fileName = item.getText();

                    if (fileName.equals(strClearRecentFiles)) {
                        mruList.clear();
                        jMenuRecentFiles.removeAll();
                        jMenuRecentFiles.add(bundle.getString("No_Recent_Files"));
                    } else {
                        openFile(new File(fileName));
                    }
                }
            };

            for (String fileName : mruList) {
                JMenuItem item = this.jMenuRecentFiles.add(fileName);
                item.addActionListener(mruAction);
            }
            this.jMenuRecentFiles.addSeparator();
            strClearRecentFiles = bundle.getString("Clear_Recent_Files");
            JMenuItem jMenuItemClear = this.jMenuRecentFiles.add(strClearRecentFiles);
            jMenuItemClear.setMnemonic(bundle.getString("jMenuItemClear.Mnemonic").charAt(0));
            jMenuItemClear.addActionListener(mruAction);
        }
    }

    /**
     * Update MRU List.
     *
     * @param fileName
     */
    private void updateMRUList(String fileName) {
        if (mruList.contains(fileName)) {
            mruList.remove(fileName);
        }
        mruList.add(0, fileName);

        if (mruList.size() > 10) {
            mruList.remove(10);
        }

        updateMRUMenu();
    }

    /**
     * Updates the Undo and Redo actions
     */
    private void updateUndoRedo() {
        m_undoAction.setEnabled(m_undo.canUndo());
        m_redoAction.setEnabled(m_undo.canRedo());
    }

    /**
     * Updates the Cut, Copy, and Delete actions
     *
     * @param isTextSelected whether any text currently selected
     */
    private void updateCutCopyDelete(boolean isTextSelected) {
        actionCut.setEnabled(isTextSelected);
        actionCopy.setEnabled(isTextSelected);
        actionDelete.setEnabled(isTextSelected);
    }

    /**
     * Listens to raw undoable edits
     *
     */
    private class RawListener implements UndoableEditListener {

        /**
         * Description of the Method
         *
         * @param e Description of the Parameter
         */
        @Override
        public void undoableEditHappened(UndoableEditEvent e) {
            undoSupport.postEdit(e.getEdit());
        }
    }

    /**
     * Listens to undoable edits filtered by undoSupport
     *
     */
    private class SupportListener implements UndoableEditListener {

        /**
         * Description of the Method
         *
         * @param e Description of the Parameter
         */
        @Override
        public void undoableEditHappened(UndoableEditEvent e) {
            updateSave(true);
            m_undo.addEdit(e.getEdit());
            updateUndoRedo();
        }
    }

    /**
     * Updates the Paste action
     */
    private void updatePaste() {
        try {
            Transferable clipData = clipboard.getContents(clipboard);
            if (clipData != null) {
                actionPaste.setEnabled(clipData.isDataFlavorSupported(DataFlavor.stringFlavor));
            }
        } catch (OutOfMemoryError e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            JOptionPane.showMessageDialog(this, e.getMessage(), bundle.getString("OutOfMemoryError"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popup = new javax.swing.JPopupMenu();
        jFileChooser = new javax.swing.JFileChooser();
        jPopupMenuSegmentedRegions = new javax.swing.JPopupMenu();
        jCheckBoxMenuItemSymbol = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemWord = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemTextLine = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemPara = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemBlock = new javax.swing.JCheckBoxMenuItem();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanelImage = new javax.swing.JPanel();
        jToolBar2 = new javax.swing.JToolBar();
        jButtonOpen = new javax.swing.JButton();
        jButtonScan = new javax.swing.JButton();
        jButtonPasteImage = new javax.swing.JButton();
        jButtonSave = new javax.swing.JButton();
        jSeparator14 = new javax.swing.JToolBar.Separator();
        jButtonPrevPage = new javax.swing.JButton();
        jButtonNextPage = new javax.swing.JButton();
        jComboBoxPageNum = new javax.swing.JComboBox();
        jLabelPageMax = new javax.swing.JLabel();
        jSeparator7 = new javax.swing.JToolBar.Separator();
        jButtonFitImage = new javax.swing.JButton();
        jButtonActualSize = new javax.swing.JButton();
        jSeparator8 = new javax.swing.JToolBar.Separator();
        jButtonZoomIn = new javax.swing.JButton();
        jButtonZoomOut = new javax.swing.JButton();
        jSeparator9 = new javax.swing.JToolBar.Separator();
        jButtonRotateCCW = new javax.swing.JButton();
        jButtonRotateCW = new javax.swing.JButton();
        jSeparator16 = new javax.swing.JToolBar.Separator();
        jSplitPaneImage = new javax.swing.JSplitPane();
        jScrollPaneThumbnail = new javax.swing.JScrollPane();
        jScrollPaneThumbnail.getVerticalScrollBar().setUnitIncrement(20);
        jPanelThumb = new javax.swing.JPanel();
        jScrollPaneImage = new javax.swing.JScrollPane();
        jScrollPaneImage.getVerticalScrollBar().setUnitIncrement(20);
        jScrollPaneImage.getHorizontalScrollBar().setUnitIncrement(20);
        jImageLabel = new JImageLabel();
        jPanelArrow = new javax.swing.JPanel();
        jButtonCollapseExpand = new javax.swing.JButton();
        jPanelTextArea = new javax.swing.JPanel();
        jToolBar1 = new javax.swing.JToolBar();
        jButtonOCR = new javax.swing.JButton();
        jButtonCancelOCR = new javax.swing.JButton();
        jButtonCancelOCR.setVisible(false);
        jButtonClear = new javax.swing.JButton();
        jSeparator17 = new javax.swing.JToolBar.Separator();
        jToggleButtonSpellCheck = new javax.swing.JToggleButton();
        jButtonPostProcess = new javax.swing.JButton();
        jButtonRemoveLineBreaks = new javax.swing.JButton();
        jSeparator15 = new javax.swing.JToolBar.Separator();
        jLabelLanguage = new javax.swing.JLabel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(4, 0), new java.awt.Dimension(4, 0), new java.awt.Dimension(4, 32767));
        jComboBoxLang = new javax.swing.JComboBox();
        jScrollPaneText = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jTextArea1.addMouseListener(new MouseAdapter() {
            public void mousePressed(final MouseEvent e) {
                if (e.isPopupTrigger()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            populatePopupMenuWithSuggestions(e.getPoint());
                            popup.show(e.getComponent(), e.getX(), e.getY());
                        }
                    });
                }
            }

            public void mouseReleased(MouseEvent e) {
                mousePressed(e);
            }
        });
        jTextArea1.addCaretListener(new CaretListener() {
            public void caretUpdate(CaretEvent e) {
                updateCutCopyDelete(e.getDot() != e.getMark());
            }
        });

        jTextArea1.setCaret(new javax.swing.text.DefaultCaret() {
            public void setSelectionVisible(boolean visible) {
                super.setSelectionVisible(true);
            }
        });

        int blinkRate = 500;
        Object o = UIManager.get("TextArea.caretBlinkRate");
        if ((o != null) && (o instanceof Integer)) {
            Integer rate = (Integer) o;
            blinkRate = rate.intValue();
        }
        jTextArea1.getCaret().setBlinkRate(blinkRate);
        jTextArea1.getDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty, System.getProperty("line.separator"));
        jPanelStatus = new javax.swing.JPanel();
        jLabelStatus = new javax.swing.JLabel();
        jLabelStatus.setVisible(false); // use jProgressBar instead for (more animation) task status
        jProgressBar1 = new javax.swing.JProgressBar();
        jProgressBar1.setVisible(false);
        jSeparatorDim = new javax.swing.JSeparator();
        jLabelDimension = new javax.swing.JLabel();
        jLabelDimensionValue = new javax.swing.JLabel();
        jSeparatorDimEnd = new javax.swing.JSeparator();
        jButtonSegmentedRegions = new javax.swing.JButton();
        jPanelSegmentedRegions = new javax.swing.JPanel();
        jSeparatorSM = new javax.swing.JSeparator();
        jLabelScreenshotMode = new javax.swing.JLabel();
        jLabelScreenshotModeValue = new javax.swing.JLabel();
        jSeparatorPSM = new javax.swing.JSeparator();
        jLabelPSM = new javax.swing.JLabel();
        jLabelPSMvalue = new javax.swing.JLabel();
        jSeparatorEnd = new javax.swing.JSeparator();
        jMenuBar2 = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemOpen = new javax.swing.JMenuItem();
        jMenuItemScan = new javax.swing.JMenuItem();
        jMenuItemSave = new javax.swing.JMenuItem();
        jMenuItemSaveAs = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        jMenuRecentFiles = new javax.swing.JMenu();
        jSeparatorExit = new javax.swing.JPopupMenu.Separator();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenuCommand = new javax.swing.JMenu();
        jMenuItemOCR = new javax.swing.JMenuItem();
        jMenuItemOCRAll = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        jMenuItemBulkOCR = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItemPostProcess = new javax.swing.JMenuItem();
        jMenuImage = new javax.swing.JMenu();
        jMenuItemMetadata = new javax.swing.JMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        jMenuFilter = new javax.swing.JMenu();
        jMenuItemBrightness = new javax.swing.JMenuItem();
        jMenuItemContrast = new javax.swing.JMenuItem();
        jMenuItemGamma = new javax.swing.JMenuItem();
        jMenuItemThreshold = new javax.swing.JMenuItem();
        jMenuItemGrayscale = new javax.swing.JMenuItem();
        jMenuItemMonochrome = new javax.swing.JMenuItem();
        jMenuItemInvert = new javax.swing.JMenuItem();
        jMenuItemSharpen = new javax.swing.JMenuItem();
        jMenuItemSmooth = new javax.swing.JMenuItem();
        jMenuItemBilateralFiltering = new javax.swing.JMenuItem();
        jMenuItemDeskew = new javax.swing.JMenuItem();
        jMenuItemAutocrop = new javax.swing.JMenuItem();
        jMenuItemCrop = new javax.swing.JMenuItem();
        jMenuItemRemoveLines = new javax.swing.JMenuItem();
        jSeparator12 = new javax.swing.JPopupMenu.Separator();
        jMenuItemUndo = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jCheckBoxMenuItemScreenshotMode = new javax.swing.JCheckBoxMenuItem();
        jSeparator18 = new javax.swing.JPopupMenu.Separator();
        jCheckBoxMenuItemSegmentedRegions = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemSegmentedRegions.setSelected(prefs.getBoolean(strSegmentedRegions, false));
        jMenuFormat = new javax.swing.JMenu();
        jCheckBoxMenuWordWrap = new javax.swing.JCheckBoxMenuItem();
        jMenuItemFont = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JPopupMenu.Separator();
        jMenuItemChangeCase = new javax.swing.JMenuItem();
        jMenuItemRemoveLineBreaks = new javax.swing.JMenuItem();
        jMenuSettings = new javax.swing.JMenu();
        jMenuInputMethod = new javax.swing.JMenu();
        jSeparatorInputMethod = new javax.swing.JPopupMenu.Separator();
        jMenuUILang = new javax.swing.JMenu();
        jMenuLookAndFeel = new javax.swing.JMenu();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jMenuItemDownloadLangData = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        jMenuPSM = new javax.swing.JMenu();
        jSeparatorOptions = new javax.swing.JPopupMenu.Separator();
        jMenuItemOptions = new javax.swing.JMenuItem();
        jMenuTools = new javax.swing.JMenu();
        jMenuItemMergeTiff = new javax.swing.JMenuItem();
        jMenuItemSplitTiff = new javax.swing.JMenuItem();
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        jMenuItemMergePdf = new javax.swing.JMenuItem();
        jMenuItemSplitPdf = new javax.swing.JMenuItem();
        jMenuHelp = new javax.swing.JMenu();
        jMenuItemHelp = new javax.swing.JMenuItem();
        jSeparatorAbout = new javax.swing.JPopupMenu.Separator();
        jMenuItemAbout = new javax.swing.JMenuItem();

        currentDirectory = prefs.get(strCurrentDirectory, null);
        outputDirectory = prefs.get(strOutputDirectory, null);
        jFileChooser.setCurrentDirectory(currentDirectory == null ? null : new File(currentDirectory));
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui"); // NOI18N
        jFileChooser.setDialogTitle(bundle.getString("jButtonOpen.ToolTipText")); // NOI18N
        FileFilter allImageFilter = new SimpleFilter("bmp;gif;jpg;jpeg;jp2;png;pnm;pbm;pgm;ppm;tif;tiff;pdf", bundle.getString("All_Image_Files"));
        FileFilter bmpFilter = new SimpleFilter("bmp", "Bitmap");
        FileFilter gifFilter = new SimpleFilter("gif", "GIF");
        FileFilter jpegFilter = new SimpleFilter("jpg;jpeg", "JPEG");
        FileFilter jpeg2000Filter = new SimpleFilter("jp2", "JPEG 2000");
        FileFilter pngFilter = new SimpleFilter("png", "PNG");
        FileFilter pnmFilter = new SimpleFilter("pnm;pbm;pgm;ppm", "PNM");
        FileFilter tiffFilter = new SimpleFilter("tif;tiff", "TIFF");

        FileFilter pdfFilter = new SimpleFilter("pdf", "PDF");
        FileFilter textFilter = new SimpleFilter("txt", bundle.getString("UTF-8_Text"));

        jFileChooser.setAcceptAllFileFilterUsed(false);
        jFileChooser.addChoosableFileFilter(allImageFilter);
        jFileChooser.addChoosableFileFilter(bmpFilter);
        jFileChooser.addChoosableFileFilter(gifFilter);
        jFileChooser.addChoosableFileFilter(jpegFilter);
        jFileChooser.addChoosableFileFilter(jpeg2000Filter);
        jFileChooser.addChoosableFileFilter(pngFilter);
        jFileChooser.addChoosableFileFilter(pnmFilter);
        jFileChooser.addChoosableFileFilter(tiffFilter);
        jFileChooser.addChoosableFileFilter(pdfFilter);
        jFileChooser.addChoosableFileFilter(textFilter);

        filterIndex = prefs.getInt(strFilterIndex, 0);
        fileFilters = jFileChooser.getChoosableFileFilters();
        if (filterIndex < fileFilters.length) {
            jFileChooser.setFileFilter(fileFilters[filterIndex]);
        }

        jCheckBoxMenuItemSymbol.setSelected(prefs.getBoolean(strSegmentedRegionsSymbol, false));
        jCheckBoxMenuItemSymbol.setText(bundle.getString("jCheckBoxMenuItemSymbol.Text")); // NOI18N
        jCheckBoxMenuItemSymbol.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemSymbolActionPerformed(evt);
            }
        });
        jPopupMenuSegmentedRegions.add(jCheckBoxMenuItemSymbol);

        jCheckBoxMenuItemWord.setSelected(prefs.getBoolean(strSegmentedRegionsWord, false));
        jCheckBoxMenuItemWord.setText(bundle.getString("jCheckBoxMenuItemWord.Text")); // NOI18N
        jCheckBoxMenuItemWord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemWordActionPerformed(evt);
            }
        });
        jPopupMenuSegmentedRegions.add(jCheckBoxMenuItemWord);

        jCheckBoxMenuItemTextLine.setSelected(prefs.getBoolean(strSegmentedRegionsTextLine, false));
        jCheckBoxMenuItemTextLine.setText(bundle.getString("jCheckBoxMenuItemTextLine.Text")); // NOI18N
        jCheckBoxMenuItemTextLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemTextLineActionPerformed(evt);
            }
        });
        jPopupMenuSegmentedRegions.add(jCheckBoxMenuItemTextLine);

        jCheckBoxMenuItemPara.setSelected(prefs.getBoolean(strSegmentedRegionsPara, false));
        jCheckBoxMenuItemPara.setText(bundle.getString("jCheckBoxMenuItemPara.Text")); // NOI18N
        jCheckBoxMenuItemPara.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemParaActionPerformed(evt);
            }
        });
        jPopupMenuSegmentedRegions.add(jCheckBoxMenuItemPara);

        jCheckBoxMenuItemBlock.setSelected(prefs.getBoolean(strSegmentedRegionsBlock, false));
        jCheckBoxMenuItemBlock.setText(bundle.getString("jCheckBoxMenuItemBlock.Text")); // NOI18N
        jCheckBoxMenuItemBlock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemBlockActionPerformed(evt);
            }
        });
        jPopupMenuSegmentedRegions.add(jCheckBoxMenuItemBlock);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(APP_NAME);
        setIconImages(getIconImages());
        setLocationByPlatform(true);
        setMinimumSize(new java.awt.Dimension(500, 360));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jSplitPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(-2, 0, 0, 0));
        jSplitPane1.setDividerLocation(600);
        jSplitPane1.setDividerSize(2);

        jPanelImage.setLayout(new java.awt.BorderLayout());

        jToolBar2.setFloatable(false);

        jButtonOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/open.png"))); // NOI18N
        jButtonOpen.setToolTipText(bundle.getString("jButtonOpen.ToolTipText")); // NOI18N
        jButtonOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOpenActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonOpen);

        jButtonScan.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/scan.png"))); // NOI18N
        jButtonScan.setToolTipText(bundle.getString("jButtonScan.ToolTipText")); // NOI18N
        jButtonScan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonScanActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonScan);

        jButtonPasteImage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/image_paste.png"))); // NOI18N
        jButtonPasteImage.setToolTipText(bundle.getString("jButtonPasteImage.ToolTipText")); // NOI18N
        jButtonPasteImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPasteImageActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonPasteImage);

        jButtonSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/save.png"))); // NOI18N
        jButtonSave.setToolTipText(bundle.getString("jButtonSave.ToolTipText")); // NOI18N
        jButtonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonSave);
        jToolBar2.add(jSeparator14);

        jButtonPrevPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/document_page_previous.png"))); // NOI18N
        jButtonPrevPage.setToolTipText(bundle.getString("jButtonPrevPage.ToolTipText")); // NOI18N
        jButtonPrevPage.setEnabled(false);
        jButtonPrevPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPrevPageActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonPrevPage);

        jButtonNextPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/document_page_next.png"))); // NOI18N
        jButtonNextPage.setToolTipText(bundle.getString("jButtonNextPage.ToolTipText")); // NOI18N
        jButtonNextPage.setEnabled(false);
        jButtonNextPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonNextPageActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonNextPage);

        jComboBoxPageNum.setEnabled(false);
        jComboBoxPageNum.setMaximumSize(new java.awt.Dimension(40, 24));
        jComboBoxPageNum.setMinimumSize(new java.awt.Dimension(40, 20));
        jComboBoxPageNum.setPreferredSize(new java.awt.Dimension(40, 20));
        jComboBoxPageNum.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxPageNumActionPerformed(evt);
            }
        });
        jToolBar2.add(jComboBoxPageNum);

        jLabelPageMax.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelPageMax.setText(" / 0");
        jLabelPageMax.setEnabled(false);
        jLabelPageMax.setPreferredSize(new java.awt.Dimension(30, 14));
        jToolBar2.add(Box.createHorizontalStrut(4));
        jToolBar2.add(jLabelPageMax);
        jToolBar2.add(Box.createHorizontalStrut(4));
        jToolBar2.add(jSeparator7);

        jButtonFitImage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/zoom_fit.png"))); // NOI18N
        jButtonFitImage.setToolTipText(bundle.getString("jButtonFitImage.ToolTipText")); // NOI18N
        jButtonFitImage.setEnabled(false);
        jButtonFitImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonFitImageActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonFitImage);

        jButtonActualSize.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/zoom_actual.png"))); // NOI18N
        jButtonActualSize.setToolTipText(bundle.getString("jButtonActualSize.ToolTipText")); // NOI18N
        jButtonActualSize.setEnabled(false);
        jButtonActualSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonActualSizeActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonActualSize);
        jToolBar2.add(jSeparator8);

        jButtonZoomIn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/zoom_in.png"))); // NOI18N
        jButtonZoomIn.setToolTipText(bundle.getString("jButtonZoomIn.ToolTipText")); // NOI18N
        jButtonZoomIn.setEnabled(false);
        jButtonZoomIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonZoomInActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonZoomIn);

        jButtonZoomOut.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/zoom_out.png"))); // NOI18N
        jButtonZoomOut.setToolTipText(bundle.getString("jButtonZoomOut.ToolTipText")); // NOI18N
        jButtonZoomOut.setEnabled(false);
        jButtonZoomOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonZoomOutActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonZoomOut);
        jToolBar2.add(jSeparator9);

        jButtonRotateCCW.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/rotate_ccw.png"))); // NOI18N
        jButtonRotateCCW.setToolTipText(bundle.getString("jButtonRotateCCW.ToolTipText")); // NOI18N
        jButtonRotateCCW.setEnabled(false);
        jButtonRotateCCW.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRotateCCWActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonRotateCCW);

        jButtonRotateCW.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/rotate_cw.png"))); // NOI18N
        jButtonRotateCW.setToolTipText(bundle.getString("jButtonRotateCW.ToolTipText")); // NOI18N
        jButtonRotateCW.setEnabled(false);
        jButtonRotateCW.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRotateCWActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonRotateCW);
        jToolBar2.add(jSeparator16);

        jPanelImage.add(jToolBar2, java.awt.BorderLayout.PAGE_START);

        jSplitPaneImage.setDividerLocation(120);

        jScrollPaneThumbnail.setPreferredSize(new java.awt.Dimension(120, 120));

        jPanelThumb.setLayout(new javax.swing.BoxLayout(jPanelThumb, javax.swing.BoxLayout.PAGE_AXIS));
        jScrollPaneThumbnail.setViewportView(jPanelThumb);

        jSplitPaneImage.setLeftComponent(jScrollPaneThumbnail);

        jScrollPaneImage.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                jScrollPaneImageMouseWheelMoved(evt);
            }
        });

        jImageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jImageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jImageLabelMouseEntered(evt);
            }
        });
        jScrollPaneImage.setViewportView(jImageLabel);

        jSplitPaneImage.setRightComponent(jScrollPaneImage);

        jPanelImage.add(jSplitPaneImage, java.awt.BorderLayout.CENTER);
        jSplitPaneImage.getLeftComponent().setMinimumSize(new Dimension());
        jSplitPaneImage.setDividerLocation(0);
        jSplitPaneImage.setDividerSize(0);

        jPanelArrow.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 5));

        jButtonCollapseExpand.setText("»");
        jButtonCollapseExpand.setToolTipText(bundle.getString("jButtonCollapseExpand.ToolTipText")); // NOI18N
        jButtonCollapseExpand.setContentAreaFilled(false);
        jButtonCollapseExpand.setMargin(new java.awt.Insets(2, 4, 2, 4));
        jButtonCollapseExpand.setPreferredSize(new java.awt.Dimension(26, 23));
        jButtonCollapseExpand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCollapseExpandActionPerformed(evt);
            }
        });
        jPanelArrow.add(jButtonCollapseExpand);

        jPanelImage.add(jPanelArrow, java.awt.BorderLayout.WEST);

        jSplitPane1.setLeftComponent(jPanelImage);

        jPanelTextArea.setLayout(new java.awt.BorderLayout());

        jToolBar1.setRollover(true);

        jButtonOCR.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/ocr.png"))); // NOI18N
        jButtonOCR.setToolTipText(bundle.getString("jButtonOCR.ToolTipText")); // NOI18N
        jButtonOCR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOCRActionPerformed(evt);
            }
        });
        jToolBar1.add(jButtonOCR);

        jButtonCancelOCR.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/cancel.png"))); // NOI18N
        jButtonCancelOCR.setToolTipText(bundle.getString("jButtonCancelOCR.ToolTipText")); // NOI18N
        jButtonCancelOCR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelOCRActionPerformed(evt);
            }
        });
        jToolBar1.add(jButtonCancelOCR);

        jButtonClear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/draw_eraser.png"))); // NOI18N
        jButtonClear.setToolTipText(bundle.getString("jButtonClear.ToolTipText")); // NOI18N
        jButtonClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearActionPerformed(evt);
            }
        });
        jToolBar1.add(jButtonClear);
        jToolBar1.add(jSeparator17);

        jToggleButtonSpellCheck.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/inline_spellcheck.png"))); // NOI18N
        jToggleButtonSpellCheck.setToolTipText(bundle.getString("jToggleButtonSpellCheck.ToolTipText")); // NOI18N
        jToggleButtonSpellCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonSpellCheckActionPerformed(evt);
            }
        });
        jToolBar1.add(jToggleButtonSpellCheck);
        jToggleButtonSpellCheck.getAccessibleContext().setAccessibleName("jToggleButtonSpellCheck");

        jButtonPostProcess.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/gear_in.png"))); // NOI18N
        jButtonPostProcess.setToolTipText(bundle.getString("jButtonPostProcess.ToolTipText")); // NOI18N
        jButtonPostProcess.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPostProcessActionPerformed(evt);
            }
        });
        jToolBar1.add(jButtonPostProcess);

        jButtonRemoveLineBreaks.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fatcow/icons/pilcrow_delete.png"))); // NOI18N
        jButtonRemoveLineBreaks.setToolTipText(bundle.getString("jButtonRemoveLineBreaks.ToolTipText")); // NOI18N
        jButtonRemoveLineBreaks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRemoveLineBreaksActionPerformed(evt);
            }
        });
        jToolBar1.add(jButtonRemoveLineBreaks);
        jToolBar1.add(jSeparator15);
        jToolBar1.add(Box.createHorizontalGlue());

        jLabelLanguage.setLabelFor(jComboBoxLang);
        jLabelLanguage.setText(bundle.getString("jLabelLanguage.Text")); // NOI18N
        jLabelLanguage.setToolTipText(bundle.getString("jLabelLanguage.ToolTipText")); // NOI18N
        jToolBar1.add(jLabelLanguage);
        jToolBar1.add(filler1);

        jComboBoxLang.setEditable(true);
        jComboBoxLang.setMaximumSize(new java.awt.Dimension(100, 24));
        jComboBoxLang.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBoxLangItemStateChanged(evt);
            }
        });
        jToolBar1.add(jComboBoxLang);

        jPanelTextArea.add(jToolBar1, java.awt.BorderLayout.PAGE_START);

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setMargin(new java.awt.Insets(8, 8, 2, 2));
        jTextArea1.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                jTextArea1MouseWheelMoved(evt);
            }
        });
        jTextArea1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jTextArea1MouseEntered(evt);
            }
        });
        jScrollPaneText.setViewportView(jTextArea1);
        wordWrapOn = prefs.getBoolean(strWordWrap, false);
        jTextArea1.setLineWrap(wordWrapOn);
        jCheckBoxMenuWordWrap.setSelected(wordWrapOn);

        font = new Font(
            prefs.get(strFontName, MAC_OS_X ? "Lucida Grande" : "Tahoma"),
            prefs.getInt(strFontStyle, Font.PLAIN),
            prefs.getInt(strFontSize, 12));
        jTextArea1.setFont(font);

        jPanelTextArea.add(jScrollPaneText, java.awt.BorderLayout.CENTER);

        jSplitPane1.setRightComponent(jPanelTextArea);

        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);

        jPanelStatus.setPreferredSize(new java.awt.Dimension(10, 22));
        jPanelStatus.setLayout(new javax.swing.BoxLayout(jPanelStatus, javax.swing.BoxLayout.LINE_AXIS));
        jPanelStatus.add(jLabelStatus);

        jProgressBar1.setMaximumSize(new java.awt.Dimension(146, 17));
        jProgressBar1.setStringPainted(true);
        jPanelStatus.add(jProgressBar1);

        jSeparatorDim.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparatorDim.setMaximumSize(new java.awt.Dimension(2, 14));
        jPanelStatus.add(Box.createHorizontalStrut(250));
        jPanelStatus.add(jSeparatorDim);
        jPanelStatus.add(Box.createHorizontalStrut(4));

        jLabelDimension.setText(bundle.getString("jLabelDimension.Text")); // NOI18N
        jLabelDimension.setToolTipText(bundle.getString("jLabelDimension.ToolTipText")); // NOI18N
        jPanelStatus.add(jLabelDimension);
        jPanelStatus.add(Box.createHorizontalStrut(4));

        jLabelDimensionValue.setText("0 × 0px  0bpp");
        jPanelStatus.add(jLabelDimensionValue);

        jSeparatorDimEnd.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparatorDimEnd.setMaximumSize(new java.awt.Dimension(2, 14));
        jPanelStatus.add(Box.createHorizontalStrut(4));
        jPanelStatus.add(jSeparatorDimEnd);

        jButtonSegmentedRegions.setText(bundle.getString("jButtonSegmentedRegions.Text")); // NOI18N
        jButtonSegmentedRegions.setToolTipText(bundle.getString("jButtonSegmentedRegions.ToolTipText")); // NOI18N
        jButtonSegmentedRegions.setMargin(new java.awt.Insets(2, 10, 2, 6));
        jButtonSegmentedRegions.setVisible(jCheckBoxMenuItemSegmentedRegions.isSelected());
        jButtonSegmentedRegions.setContentAreaFilled(false);
        jButtonSegmentedRegions.setBorderPainted(false);
        jButtonSegmentedRegions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSegmentedRegionsActionPerformed(evt);
            }
        });
        jPanelStatus.add(jButtonSegmentedRegions);
        jPanelStatus.add(Box.createHorizontalGlue());

        jPanelSegmentedRegions.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        jPanelStatus.add(jPanelSegmentedRegions);

        jSeparatorSM.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparatorSM.setMaximumSize(new java.awt.Dimension(2, 14));
        jPanelStatus.add(Box.createHorizontalGlue());
        jPanelStatus.add(Box.createHorizontalStrut(4));
        jPanelStatus.add(jSeparatorSM);
        jPanelStatus.add(Box.createHorizontalStrut(4));

        jLabelScreenshotMode.setText(bundle.getString("jLabelScreenshotMode.Text")); // NOI18N
        jLabelScreenshotMode.setToolTipText(bundle.getString("jLabelScreenshotMode.ToolTipText")); // NOI18N
        jPanelStatus.add(jLabelScreenshotMode);
        jPanelStatus.add(Box.createHorizontalStrut(4));
        jPanelStatus.add(jLabelScreenshotModeValue);

        jSeparatorPSM.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparatorPSM.setMaximumSize(new java.awt.Dimension(2, 14));
        jPanelStatus.add(Box.createHorizontalStrut(4));
        jPanelStatus.add(jSeparatorPSM);
        jPanelStatus.add(Box.createHorizontalStrut(4));

        jLabelPSM.setText("PSM:");
        jLabelPSM.setToolTipText(bundle.getString("jLabelPSM.ToolTipText")); // NOI18N
        jPanelStatus.add(jLabelPSM);
        jPanelStatus.add(Box.createHorizontalStrut(4));
        jPanelStatus.add(jLabelPSMvalue);

        jSeparatorEnd.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparatorEnd.setMaximumSize(new java.awt.Dimension(2, 14));
        jPanelStatus.add(Box.createHorizontalStrut(4));
        jPanelStatus.add(jSeparatorEnd);
        jPanelStatus.add(Box.createHorizontalStrut(16));

        getContentPane().add(jPanelStatus, java.awt.BorderLayout.SOUTH);

        jMenuFile.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuFile.Mnemonic").charAt(0));
        jMenuFile.setText(bundle.getString("jMenuFile.Text")); // NOI18N

        jMenuItemOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemOpen.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemOpen.Mnemonic").charAt(0));
        jMenuItemOpen.setText(bundle.getString("jMenuItemOpen.Text")); // NOI18N
        jMenuItemOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemOpen);

        jMenuItemScan.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemScan.Mnemonic").charAt(0));
        jMenuItemScan.setText(bundle.getString("jMenuItemScan.Text")); // NOI18N
        jMenuItemScan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemScanActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemScan);

        jMenuItemSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemSave.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemSave.Mnemonic").charAt(0));
        jMenuItemSave.setText(bundle.getString("jMenuItemSave.Text")); // NOI18N
        jMenuItemSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSave);

        jMenuItemSaveAs.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemSaveAs.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemSaveAs.Mnemonic").charAt(0));
        jMenuItemSaveAs.setText(bundle.getString("jMenuItemSaveAs.Text")); // NOI18N
        jMenuItemSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveAsActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSaveAs);
        jMenuFile.add(jSeparator4);

        jMenuRecentFiles.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuRecentFiles.Mnemonic").charAt(0));
        jMenuRecentFiles.setText(bundle.getString("jMenuRecentFiles.Text")); // NOI18N
        jMenuFile.add(jMenuRecentFiles);
        jMenuFile.add(jSeparatorExit);

        jMenuItemExit.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemExit.Mnemonic").charAt(0));
        jMenuItemExit.setText(bundle.getString("jMenuItemExit.Text")); // NOI18N
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemExit);

        jMenuBar2.add(jMenuFile);

        jMenuCommand.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuCommand.Mnemonic").charAt(0));
        jMenuCommand.setText(bundle.getString("jMenuCommand.Text")); // NOI18N

        jMenuItemOCR.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemOCR.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemOCR.Mnemonic").charAt(0));
        jMenuItemOCR.setText(bundle.getString("jMenuItemOCR.Text")); // NOI18N
        jMenuItemOCR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOCRActionPerformed(evt);
            }
        });
        jMenuCommand.add(jMenuItemOCR);

        jMenuItemOCRAll.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemOCRAll.Mnemonic").charAt(0));
        jMenuItemOCRAll.setText(bundle.getString("jMenuItemOCRAll.Text")); // NOI18N
        jMenuItemOCRAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOCRAllActionPerformed(evt);
            }
        });
        jMenuCommand.add(jMenuItemOCRAll);
        jMenuCommand.add(jSeparator6);

        jMenuItemBulkOCR.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemBulkOCR.Mnemonic").charAt(0));
        jMenuItemBulkOCR.setText(bundle.getString("jMenuItemBulkOCR.Text")); // NOI18N
        jMenuItemBulkOCR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemBulkOCRActionPerformed(evt);
            }
        });
        jMenuCommand.add(jMenuItemBulkOCR);
        jMenuItemBulkOCR.getAccessibleContext().setAccessibleName("jMenuItemExecuteBatch");

        jMenuCommand.add(jSeparator1);

        jMenuItemPostProcess.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemPostProcess.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemPostProcess.Mnemonic").charAt(0));
        jMenuItemPostProcess.setText(bundle.getString("jMenuItemPostProcess.Text")); // NOI18N
        jMenuItemPostProcess.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPostProcessActionPerformed(evt);
            }
        });
        jMenuCommand.add(jMenuItemPostProcess);

        jMenuBar2.add(jMenuCommand);

        jMenuImage.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuImage.Mnemonic").charAt(0));
        jMenuImage.setText(bundle.getString("jMenuImage.Text")); // NOI18N

        jMenuItemMetadata.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemMetadata.Mnemonic").charAt(0));
        jMenuItemMetadata.setText(bundle.getString("jMenuItemMetadata.Text")); // NOI18N
        jMenuItemMetadata.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemMetadataActionPerformed(evt);
            }
        });
        jMenuImage.add(jMenuItemMetadata);
        jMenuImage.add(jSeparator11);

        jMenuFilter.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuFilter.Mnemonic").charAt(0));
        jMenuFilter.setText(bundle.getString("jMenuFilter.Text")); // NOI18N

        jMenuItemBrightness.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemBrightness.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemBrightness.Mnemonic").charAt(0));
        jMenuItemBrightness.setText(bundle.getString("jMenuItemBrightness.Text")); // NOI18N
        jMenuItemBrightness.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemBrightnessActionPerformed(evt);
            }
        });
        jMenuFilter.add(jMenuItemBrightness);

        jMenuItemContrast.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemContrast.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemContrast.Mnemonic").charAt(0));
        jMenuItemContrast.setText(bundle.getString("jMenuItemContrast.Text")); // NOI18N
        jMenuItemContrast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemContrastActionPerformed(evt);
            }
        });
        jMenuFilter.add(jMenuItemContrast);

        jMenuItemGamma.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemGamma.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemGamma.Mnemonic").charAt(0));
        jMenuItemGamma.setText(bundle.getString("jMenuItemGamma.Text")); // NOI18N
        jMenuItemGamma.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemGammaActionPerformed(evt);
            }
        });
        jMenuFilter.add(jMenuItemGamma);

        jMenuItemThreshold.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemThreshold.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemThreshold.Mnemonic").charAt(0));
        jMenuItemThreshold.setText(bundle.getString("jMenuItemThreshold.Text")); // NOI18N
        jMenuItemThreshold.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemThresholdActionPerformed(evt);
            }
        });
        jMenuFilter.add(jMenuItemThreshold);

        jMenuItemGrayscale.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemGrayscale.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemGrayscale.Mnemonic").charAt(0));
        jMenuItemGrayscale.setText(bundle.getString("jMenuItemGrayscale.Text")); // NOI18N
        jMenuItemGrayscale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemGrayscaleActionPerformed(evt);
            }
        });
        jMenuFilter.add(jMenuItemGrayscale);

        jMenuItemMonochrome.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemMonochrome.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemMonochrome.Mnemonic").charAt(0));
        jMenuItemMonochrome.setText(bundle.getString("jMenuItemMonochrome.Text")); // NOI18N
        jMenuItemMonochrome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemMonochromeActionPerformed(evt);
            }
        });
        jMenuFilter.add(jMenuItemMonochrome);

        jMenuItemInvert.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemInvert.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemInvert.Mnemonic").charAt(0));
        jMenuItemInvert.setText(bundle.getString("jMenuItemInvert.Text")); // NOI18N
        jMenuItemInvert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemInvertActionPerformed(evt);
            }
        });
        jMenuFilter.add(jMenuItemInvert);

        jMenuItemSharpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemSharpen.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemSharpen.Mnemonic").charAt(0));
        jMenuItemSharpen.setText(bundle.getString("jMenuItemSharpen.Text")); // NOI18N
        jMenuItemSharpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSharpenActionPerformed(evt);
            }
        });
        jMenuFilter.add(jMenuItemSharpen);

        jMenuItemSmooth.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemSmooth.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemSmooth.Mnemonic").charAt(0));
        jMenuItemSmooth.setText(bundle.getString("jMenuItemSmooth.Text")); // NOI18N
        jMenuItemSmooth.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSmoothActionPerformed(evt);
            }
        });
        jMenuFilter.add(jMenuItemSmooth);

        jMenuItemBilateralFiltering.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemBilateralFiltering.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemBilateralFiltering.Mnemonic").charAt(0));
        jMenuItemBilateralFiltering.setText(bundle.getString("jMenuItemBilateralFiltering.Text")); // NOI18N
        jMenuItemBilateralFiltering.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemBilateralFilteringActionPerformed(evt);
            }
        });
        jMenuFilter.add(jMenuItemBilateralFiltering);

        jMenuImage.add(jMenuFilter);

        jMenuItemDeskew.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemDeskew.setFont(new java.awt.Font("Noto Sans", 0, 12)); // NOI18N
        jMenuItemDeskew.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemDeskew.Mnemonic").charAt(0));
        jMenuItemDeskew.setText(bundle.getString("jMenuItemDeskew.Text")); // NOI18N
        jMenuItemDeskew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDeskewActionPerformed(evt);
            }
        });
        jMenuImage.add(jMenuItemDeskew);

        jMenuItemAutocrop.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemAutocrop.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemAutocrop.Mnemonic").charAt(0));
        jMenuItemAutocrop.setText(bundle.getString("jMenuItemAutocrop.Text")); // NOI18N
        jMenuItemAutocrop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAutocropActionPerformed(evt);
            }
        });
        jMenuImage.add(jMenuItemAutocrop);

        jMenuItemCrop.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemCrop.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemCrop.Mnemonic").charAt(0));
        jMenuItemCrop.setText(bundle.getString("jMenuItemCrop.Text")); // NOI18N
        jMenuItemCrop.setToolTipText("");
        jMenuItemCrop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCropActionPerformed(evt);
            }
        });
        jMenuImage.add(jMenuItemCrop);

        jMenuItemRemoveLines.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemRemoveLines.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemRemoveLines.Mnemonic").charAt(0));
        jMenuItemRemoveLines.setText(bundle.getString("jMenuItemRemoveLines.Text")); // NOI18N
        jMenuItemRemoveLines.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRemoveLinesActionPerformed(evt);
            }
        });
        jMenuImage.add(jMenuItemRemoveLines);
        jMenuImage.add(jSeparator12);

        jMenuItemUndo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemUndo.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemUndo.Mnemonic").charAt(0));
        jMenuItemUndo.setText(bundle.getString("jMenuItemUndo.Text")); // NOI18N
        jMenuItemUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemUndoActionPerformed(evt);
            }
        });
        jMenuImage.add(jMenuItemUndo);
        jMenuImage.add(jSeparator2);

        jCheckBoxMenuItemScreenshotMode.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jCheckBoxMenuItemScreenshotMode.Mnemonic").charAt(0));
        jCheckBoxMenuItemScreenshotMode.setText(bundle.getString("jCheckBoxMenuItemScreenshotMode.Text")); // NOI18N
        jCheckBoxMenuItemScreenshotMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemScreenshotModeActionPerformed(evt);
            }
        });
        jMenuImage.add(jCheckBoxMenuItemScreenshotMode);
        jMenuImage.add(jSeparator18);

        jCheckBoxMenuItemSegmentedRegions.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jCheckBoxMenuItemSegmentedRegions.Mnemonic").charAt(0));
        jCheckBoxMenuItemSegmentedRegions.setText(bundle.getString("jCheckBoxMenuItemSegmentedRegions.Text")); // NOI18N
        jCheckBoxMenuItemSegmentedRegions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemSegmentedRegionsActionPerformed(evt);
            }
        });
        jMenuImage.add(jCheckBoxMenuItemSegmentedRegions);

        jMenuBar2.add(jMenuImage);

        jMenuFormat.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuFormat.Mnemonic").charAt(0));
        jMenuFormat.setText(bundle.getString("jMenuFormat.Text")); // NOI18N

        jCheckBoxMenuWordWrap.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jCheckBoxMenuWordWrap.Mnemonic").charAt(0));
        jCheckBoxMenuWordWrap.setText(bundle.getString("jCheckBoxMenuWordWrap.Text")); // NOI18N
        jCheckBoxMenuWordWrap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuWordWrapActionPerformed(evt);
            }
        });
        jMenuFormat.add(jCheckBoxMenuWordWrap);

        jMenuItemFont.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemFont.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemFont.Mnemonic").charAt(0));
        jMenuItemFont.setText(bundle.getString("jMenuItemFont.Text")); // NOI18N
        jMenuItemFont.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFontActionPerformed(evt);
            }
        });
        jMenuFormat.add(jMenuItemFont);
        jMenuFormat.add(jSeparator10);

        jMenuItemChangeCase.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemChangeCase.Mnemonic").charAt(0));
        jMenuItemChangeCase.setText(bundle.getString("jMenuItemChangeCase.Text")); // NOI18N
        jMenuItemChangeCase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemChangeCaseActionPerformed(evt);
            }
        });
        jMenuFormat.add(jMenuItemChangeCase);

        jMenuItemRemoveLineBreaks.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemRemoveLineBreaks.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemRemoveLineBreaks.Mnemonic").charAt(0));
        jMenuItemRemoveLineBreaks.setText(bundle.getString("jMenuItemRemoveLineBreaks.Text")); // NOI18N
        jMenuItemRemoveLineBreaks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRemoveLineBreaksActionPerformed(evt);
            }
        });
        jMenuFormat.add(jMenuItemRemoveLineBreaks);

        jMenuBar2.add(jMenuFormat);

        jMenuSettings.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuSettings.Mnemonic").charAt(0));
        jMenuSettings.setText(bundle.getString("jMenuSettings.Text")); // NOI18N

        jMenuInputMethod.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuInputMethod.Mnemonic").charAt(0));
        jMenuInputMethod.setText(bundle.getString("jMenuInputMethod.Text")); // NOI18N
        jMenuSettings.add(jMenuInputMethod);
        jMenuSettings.add(jSeparatorInputMethod);

        jMenuUILang.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuUILang.Mnemonic").charAt(0));
        jMenuUILang.setText(bundle.getString("jMenuUILang.Text")); // NOI18N
        jMenuSettings.add(jMenuUILang);

        jMenuLookAndFeel.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuLookAndFeel.Mnemonic").charAt(0));
        jMenuLookAndFeel.setText(bundle.getString("jMenuLookAndFeel.Text")); // NOI18N
        jMenuSettings.add(jMenuLookAndFeel);
        jMenuSettings.add(jSeparator3);

        jMenuItemDownloadLangData.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemDownloadLangData.Mnemonic").charAt(0));
        jMenuItemDownloadLangData.setText(bundle.getString("jMenuItemDownloadLangData.Text")); // NOI18N
        jMenuItemDownloadLangData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDownloadLangDataActionPerformed(evt);
            }
        });
        jMenuSettings.add(jMenuItemDownloadLangData);
        jMenuSettings.add(jSeparator5);

        jMenuPSM.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuPSM.Mnemonic").charAt(0));
        jMenuPSM.setText(bundle.getString("jMenuPSM.Text")); // NOI18N
        jMenuSettings.add(jMenuPSM);
        jMenuSettings.add(jSeparatorOptions);

        jMenuItemOptions.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemOptions.Mnemonic").charAt(0));
        jMenuItemOptions.setText(bundle.getString("jMenuItemOptions.Text")); // NOI18N
        jMenuItemOptions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOptionsActionPerformed(evt);
            }
        });
        jMenuSettings.add(jMenuItemOptions);

        jMenuBar2.add(jMenuSettings);

        jMenuTools.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuTools.Mnemonic").charAt(0));
        jMenuTools.setText(bundle.getString("jMenuTools.Text")); // NOI18N

        jMenuItemMergeTiff.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemMergeTiff.Mnemonic").charAt(0));
        jMenuItemMergeTiff.setText(bundle.getString("jMenuItemMergeTiff.Text")); // NOI18N
        jMenuItemMergeTiff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemMergeTiffActionPerformed(evt);
            }
        });
        jMenuTools.add(jMenuItemMergeTiff);

        jMenuItemSplitTiff.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemSplitTiff.Mnemonic").charAt(0));
        jMenuItemSplitTiff.setText(bundle.getString("jMenuItemSplitTiff.Text")); // NOI18N
        jMenuItemSplitTiff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSplitTiffActionPerformed(evt);
            }
        });
        jMenuTools.add(jMenuItemSplitTiff);
        jMenuTools.add(jSeparator13);

        jMenuItemMergePdf.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemMergePdf.Mnemonic").charAt(0));
        jMenuItemMergePdf.setText(bundle.getString("jMenuItemMergePdf.Text")); // NOI18N
        jMenuItemMergePdf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemMergePdfActionPerformed(evt);
            }
        });
        jMenuTools.add(jMenuItemMergePdf);

        jMenuItemSplitPdf.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemSplitPdf.Mnemonic").charAt(0));
        jMenuItemSplitPdf.setText(bundle.getString("jMenuItemSplitPdf.Text")); // NOI18N
        jMenuItemSplitPdf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSplitPdfActionPerformed(evt);
            }
        });
        jMenuTools.add(jMenuItemSplitPdf);

        jMenuBar2.add(jMenuTools);

        jMenuHelp.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuHelp.Mnemonic").charAt(0));
        jMenuHelp.setText(bundle.getString("jMenuHelp.Text")); // NOI18N

        jMenuItemHelp.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemHelp.Mnemonic").charAt(0));
        jMenuItemHelp.setText(bundle.getString("jMenuItemHelp.Text")); // NOI18N
        jMenuItemHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemHelpActionPerformed(evt);
            }
        });
        jMenuHelp.add(jMenuItemHelp);
        jMenuHelp.add(jSeparatorAbout);

        jMenuItemAbout.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietocr/Gui").getString("jMenuItemAbout.Mnemonic").charAt(0));
        jMenuItemAbout.setText(bundle.getString("jMenuItemAbout.Text")); // NOI18N
        jMenuItemAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAboutActionPerformed(evt);
            }
        });
        jMenuHelp.add(jMenuItemAbout);

        jMenuBar2.add(jMenuHelp);

        setJMenuBar(jMenuBar2);
        if (LINUX) {
            setPreferredSize(new java.awt.Dimension(1340, 600));
        }

        // DnD support
        new DropTarget(this.jImageLabel, new FileDropTargetListener(Gui.this));
        new DropTarget(this.jTextArea1, new FileDropTargetListener(Gui.this));

        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                updateSave(false);
                setExtendedState(prefs.getInt(strWindowState, Frame.NORMAL));
                populateMRUList();
                populatePopupMenu();
                addUndoSupport();
            }

            @Override
            public void windowActivated(WindowEvent e) {
                jButtonPasteImage.setEnabled(ImageHelper.getClipboardImage() != null);
            }
        });

        setSize(
            snap(prefs.getInt(strFrameWidth, 500), 300, screen.width),
            snap(prefs.getInt(strFrameHeight, 360), 150, screen.height));
        setLocation(
            snap(prefs.getInt(strFrameX, (screen.width - getWidth()) / 2),
                screen.x, screen.x + screen.width - getWidth()),
            snap(prefs.getInt(strFrameY, screen.y + (screen.height - getHeight()) / 3),
                screen.y, screen.y + screen.height - getHeight()));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItemHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemHelpActionPerformed
        final String readme = bundle.getString("readme");
        if (MAC_OS_X) {
            try {
                File helpFile = new File(supportDir, "readme.html");
                copyFileFromJarToSupportDir(helpFile);
                helpFile = new File(supportDir, "readme_lt.html");
                copyFileFromJarToSupportDir(helpFile);
                helpFile = new File(supportDir, "readme_sk.html");
                copyFileFromJarToSupportDir(helpFile);
                helpFile = new File(supportDir, "readme_vi.html");
                copyFileFromJarToSupportDir(helpFile);
                Runtime.getRuntime().exec(new String[]{"open", "-b", "com.apple.helpviewer", readme}, null, supportDir);
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        } else {
            if (helptopicsFrame == null) {
                helptopicsFrame = new JFrame(jMenuItemHelp.getText());
                helptopicsFrame.addWindowListener(new WindowAdapter() {

                    @Override
                    public void windowClosing(WindowEvent e) {
                        super.windowClosing(e);
                        helptopicsFrame.dispose();
                        helptopicsFrame = null;
                    }
                });
                helptopicsFrame.getContentPane().setLayout(new BorderLayout());
                HtmlPane helpPane = new HtmlPane(readme);
                helptopicsFrame.getContentPane().add(helpPane, BorderLayout.CENTER);
                helptopicsFrame.getContentPane().add(helpPane.getStatusBar(), BorderLayout.SOUTH);
                helptopicsFrame.pack();
                helptopicsFrame.setLocation((screen.width - helptopicsFrame.getWidth()) / 2, 40);
            }
            helptopicsFrame.setVisible(true);
            helptopicsFrame.setExtendedState(Frame.NORMAL);
        }
    }//GEN-LAST:event_jMenuItemHelpActionPerformed

    /**
     * Copies resources from Jar to support directory.
     *
     * @param helpFile
     * @throws IOException
     */
    private void copyFileFromJarToSupportDir(File helpFile) throws IOException {
        if (!helpFile.exists()) {
            final ReadableByteChannel input
                    = Channels.newChannel(ClassLoader.getSystemResourceAsStream(helpFile.getName()));
            final FileChannel output = new FileOutputStream(helpFile).getChannel();
            output.transferFrom(input, 0, 1000000L);
            output.close();
            input.close();
        }
    }

    private void jComboBoxLangItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBoxLangItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            if (jComboBoxLang.getSelectedIndex() != -1) {
                curLangCode = installedLanguageCodes[jComboBoxLang.getSelectedIndex()];
            } else {
                curLangCode = jComboBoxLang.getSelectedItem().toString();
            }
            // Hide Viet Input Method submenu if selected OCR Language is not Vietnamese
            boolean vie = curLangCode.contains("vie");
            VietKeyListener.setVietModeEnabled(vie);
            this.jMenuInputMethod.setVisible(vie);
            this.jSeparatorInputMethod.setVisible(vie);

            if (this.jToggleButtonSpellCheck.isSelected()) {
                this.jToggleButtonSpellCheck.doClick();
                this.jToggleButtonSpellCheck.doClick();
            }
        }
    }//GEN-LAST:event_jComboBoxLangItemStateChanged

    void jMenuItemOCRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOCRActionPerformed
        // to be implemented in subclass
    }//GEN-LAST:event_jMenuItemOCRActionPerformed
    void jMenuItemOCRAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOCRAllActionPerformed
        // to be implemented in subclass
    }//GEN-LAST:event_jMenuItemOCRAllActionPerformed
    void jMenuItemPostProcessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemPostProcessActionPerformed
        // to be implemented in subclass
    }//GEN-LAST:event_jMenuItemPostProcessActionPerformed
    void jButtonPrevPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPrevPageActionPerformed
        // to be implemented in subclass
    }//GEN-LAST:event_jButtonPrevPageActionPerformed
    void jButtonNextPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonNextPageActionPerformed
        // to be implemented in subclass
    }//GEN-LAST:event_jButtonNextPageActionPerformed
    void jButtonFitImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonFitImageActionPerformed
        // to be implemented in subclass
    }//GEN-LAST:event_jButtonFitImageActionPerformed
    void jButtonActualSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonActualSizeActionPerformed
        // to be implemented in subclass
    }//GEN-LAST:event_jButtonActualSizeActionPerformed
    void jButtonZoomOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonZoomOutActionPerformed
        // to be implemented in subclass
    }//GEN-LAST:event_jButtonZoomOutActionPerformed
    void jButtonZoomInActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonZoomInActionPerformed
        // to be implemented in subclass
    }//GEN-LAST:event_jButtonZoomInActionPerformed
    private void jButtonOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOpenActionPerformed
        jMenuItemOpenActionPerformed(evt);
    }//GEN-LAST:event_jButtonOpenActionPerformed
    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveActionPerformed
        jMenuItemSaveActionPerformed(evt);
    }//GEN-LAST:event_jButtonSaveActionPerformed
    private void jButtonOCRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOCRActionPerformed
        jMenuItemOCRActionPerformed(evt);
    }//GEN-LAST:event_jButtonOCRActionPerformed
    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearActionPerformed
        if (textFile == null || promptToSave()) {
            this.jTextArea1.setText(null);
            this.jTextArea1.requestFocusInWindow();
            textFile = null;
            updateSave(false);
        }
    }//GEN-LAST:event_jButtonClearActionPerformed
    void jCheckBoxMenuWordWrapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuWordWrapActionPerformed
        // to be implemented in subclass
    }//GEN-LAST:event_jCheckBoxMenuWordWrapActionPerformed
    void jMenuItemFontActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemFontActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemFontActionPerformed
    protected static Locale getLocale(String selectedUILang) {
        return new Locale(selectedUILang);
    }

    private void jMenuItemAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAboutActionPerformed
        about();
    }//GEN-LAST:event_jMenuItemAboutActionPerformed

    /**
     * Displays About box.
     */
    void about() {
        try {
            Properties config = new Properties();
            config.loadFromXML(getClass().getResourceAsStream("config.xml"));
            String version = config.getProperty("Version");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            Date releaseDate = sdf.parse(config.getProperty("ReleaseDate"));

            JOptionPane.showMessageDialog(this, APP_NAME + " " + version + " \u00a9 2007\n"
                    + bundle.getString("program_desc") + "\n"
                    + DateFormat.getDateInstance(DateFormat.LONG).format(releaseDate)
                    + "\nhttp://vietocr.sourceforge.net", jMenuItemAbout.getText(), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
        quit();
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    /**
     * Quits and saves application preferences before exit.
     */
    void quit() {
        if (!promptToSave()) {
            return;
        }
        prefs.put(strUILanguage, selectedUILang);

        if (currentDirectory != null) {
            prefs.put(strCurrentDirectory, currentDirectory);
        }
        if (outputDirectory != null) {
            prefs.put(strOutputDirectory, outputDirectory);
        }

        if (!WINDOWS) {
            prefs.put(strTessDir, tessPath);
        }

        prefs.put(strFontName, font.getName());
        prefs.putInt(strFontSize, font.getSize());
        prefs.putInt(strFontStyle, font.getStyle());
        prefs.put(strLookAndFeel, UIManager.getLookAndFeel().getClass().getName());
        prefs.putInt(strWindowState, getExtendedState());
        if (this.jComboBoxLang.getSelectedIndex() != -1) {
            prefs.put(strLangCode, this.jComboBoxLang.getSelectedItem().toString());
        }

        prefs.putBoolean(strWordWrap, wordWrapOn);

        StringBuilder buf = new StringBuilder();
        for (String item : this.mruList) {
            buf.append(item).append(File.pathSeparatorChar);
        }
        prefs.put(strMruList, buf.toString());

        if (getExtendedState() == NORMAL) {
            prefs.putInt(strFrameHeight, getHeight());
            prefs.putInt(strFrameWidth, getWidth());
            prefs.putInt(strFrameX, getX());
            prefs.putInt(strFrameY, getY());
        }

        prefs.putInt(strFilterIndex, filterIndex);
        prefs.putBoolean(strSegmentedRegions, this.jCheckBoxMenuItemSegmentedRegions.isSelected());
        prefs.putBoolean(strSegmentedRegionsPara, this.jCheckBoxMenuItemPara.isSelected());
        prefs.putBoolean(strSegmentedRegionsSymbol, this.jCheckBoxMenuItemSymbol.isSelected());
        prefs.putBoolean(strSegmentedRegionsTextLine, this.jCheckBoxMenuItemTextLine.isSelected());
        prefs.putBoolean(strSegmentedRegionsBlock, this.jCheckBoxMenuItemBlock.isSelected());
        prefs.putBoolean(strSegmentedRegionsWord, this.jCheckBoxMenuItemWord.isSelected());

        System.exit(0);
    }

    private void jMenuItemOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOpenActionPerformed
        if (jFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentDirectory = jFileChooser.getCurrentDirectory().getPath();
            openFile(jFileChooser.getSelectedFile());

            for (int i = 0; i < fileFilters.length; i++) {
                if (fileFilters[i] == jFileChooser.getFileFilter()) {
                    filterIndex = i;
                    break;
                }
            }
        }
    }//GEN-LAST:event_jMenuItemOpenActionPerformed

    /**
     * Opens image or text file.
     *
     * @param selectedFile
     */
    public void openFile(final File selectedFile) {
        if (!selectedFile.exists()) {
            JOptionPane.showMessageDialog(this, bundle.getString("File_not_exist"), APP_NAME, JOptionPane.ERROR_MESSAGE);
            return;
        }
        // if text file, load it into textarea
        if (selectedFile.getName().endsWith(".txt")) {
            if (!promptToSave()) {
                return;
            }
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        new FileInputStream(selectedFile), "UTF8"));
                this.jTextArea1.read(in, null);
                in.close();
                this.textFile = selectedFile;
                javax.swing.text.Document doc = this.jTextArea1.getDocument();
                if (doc.getText(0, 1).equals("\uFEFF")) {
                    doc.remove(0, 1); // remove BOM
                }
                doc.addUndoableEditListener(rawListener);
                updateMRUList(selectedFile.getPath());
                updateSave(false);
                this.jTextArea1.requestFocusInWindow();
            } catch (Exception e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
            return;
        }

        jLabelStatus.setText(bundle.getString("Loading_image..."));
        jProgressBar1.setIndeterminate(true);
        jProgressBar1.setString(bundle.getString("Loading_image..."));
        jProgressBar1.setVisible(true);
        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getGlassPane().setVisible(true);
        this.jButtonOCR.setEnabled(false);
        this.jMenuItemOCR.setEnabled(false);
        this.jMenuItemOCRAll.setEnabled(false);

        SwingWorker loadWorker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                iioImageList = ImageIOHelper.getIIOImageList(selectedFile);
                imageList = ImageIconScalable.getImageList(iioImageList);
                inputfilename = selectedFile.getPath();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // dummy method                   
                    loadImage();
                    setTitle(selectedFile.getName() + " - " + APP_NAME);
                    updateMRUList(selectedFile.getPath());
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                } catch (java.util.concurrent.ExecutionException e) {
                    String why;
                    Throwable cause = e.getCause();
                    if (cause != null) {
                        if (cause instanceof OutOfMemoryError) {
                            why = bundle.getString("OutOfMemoryError");
                        } else {
                            why = cause.getMessage();
                        }
                    } else {
                        why = e.getMessage();
                    }
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    JOptionPane.showMessageDialog(Gui.this, why, APP_NAME, JOptionPane.ERROR_MESSAGE);
                } finally {
                    jLabelStatus.setText(bundle.getString("Loading_completed"));
                    jProgressBar1.setString(bundle.getString("Loading_completed"));
                    jProgressBar1.setIndeterminate(false);
                    getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    getGlassPane().setVisible(false);
                    jButtonOCR.setEnabled(true);
                    jMenuItemOCR.setEnabled(true);
                    jMenuItemOCRAll.setEnabled(true);
                }
            }
        };

        loadWorker.execute();
    }

    /**
     * Loads image.
     */
    @SuppressWarnings("unchecked")
    void loadImage() {
        if (imageList == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("Cannotloadimage"), APP_NAME, JOptionPane.ERROR_MESSAGE);
            return;
        }

        imageTotal = imageList.size();
        imageIndex = 0;
        scaleX = scaleY = 1f;
        isFitImageSelected = false;

        Integer[] pages = new Integer[imageTotal];
        for (int i = 0; i < imageTotal; i++) {
            pages[i] = i + 1;
        }
        this.jComboBoxPageNum.setModel(new DefaultComboBoxModel(pages));
        this.jComboBoxPageNum.setEnabled(true);
        this.jLabelPageMax.setEnabled(true);
        this.jLabelPageMax.setText(" / " + imageTotal);

        displayImage();
        loadThumbnails();

        // clear undo buffer
        clearStack();

        this.jButtonFitImage.setEnabled(true);
        this.jButtonActualSize.setEnabled(false);
        this.jButtonZoomIn.setEnabled(true);
        this.jButtonZoomOut.setEnabled(true);

        if (imageList.size() == 1) {
            this.jButtonNextPage.setEnabled(false);
            this.jButtonPrevPage.setEnabled(false);
        } else {
            this.jButtonNextPage.setEnabled(true);
            this.jButtonPrevPage.setEnabled(true);
        }

        this.jButtonRotateCCW.setEnabled(true);
        this.jButtonRotateCW.setEnabled(true);

        setButton();
    }

    /**
     * Displays image.
     */
    void displayImage() {
        imageIcon = imageList.get(imageIndex).clone();
        originalW = imageIcon.getIconWidth();
        originalH = imageIcon.getIconHeight();
        this.jLabelDimensionValue.setText(String.format("%s × %spx  %sbpp", originalW, originalH, ((BufferedImage) imageIcon.getImage()).getColorModel().getPixelSize()));

        if (this.isFitImageSelected) {
            // scale image to fit the scrollpane
            Dimension fitSize = fitImagetoContainer(originalW, originalH, jScrollPaneImage.getViewport().getWidth(), jScrollPaneImage.getViewport().getHeight());
            imageIcon.setScaledSize(fitSize.width, fitSize.height);
            setScale(fitSize.width, fitSize.height);
        } else if (Math.abs(scaleX - 1f) > 0.001f) {
            // scale image for zoom
            imageIcon.setScaledSize((int) (originalW / scaleX), (int) (originalH / scaleY));
        }

        jImageLabel.setIcon(imageIcon);
        this.jScrollPaneImage.getViewport().setViewPosition(curScrollPos = new Point());
        ((JImageLabel) jImageLabel).deselect();
        ((JImageLabel) jImageLabel).setSegmentedRegions(null);
        setSegmentedRegions();
//        jImageLabel.revalidate();
    }

    void setSegmentedRegions() {
        if (!this.jCheckBoxMenuItemSegmentedRegions.isSelected() || iioImageList == null || this.jButtonActualSize.isEnabled()) {
            ((JImageLabel) jImageLabel).setSegmentedRegions(null);
            return;
        }

        try {
            OCR<IIOImage> ocrEngine = new OCRImages(tessPath); // for Tess4J
            ocrEngine.setDatapath(datapath);
            HashMap<Color, List<Rectangle>> map = ((JImageLabel) jImageLabel).getSegmentedRegions();
            if (map == null) {
                map = new HashMap<Color, List<Rectangle>>();
            }

            IIOImage image = iioImageList.get(imageIndex);

            List<Rectangle> regions;

            if (jCheckBoxMenuItemBlock.isSelected()) {
                if (!map.containsKey(Color.GRAY)) {
                    regions = ocrEngine.getSegmentedRegions(image, ITessAPI.TessPageIteratorLevel.RIL_BLOCK);
                    map.put(Color.GRAY, regions);
                }
            } else {
                map.remove(Color.GRAY);
            }

            if (jCheckBoxMenuItemPara.isSelected()) {
                if (!map.containsKey(Color.GREEN)) {
                    regions = ocrEngine.getSegmentedRegions(image, ITessAPI.TessPageIteratorLevel.RIL_PARA);
                    map.put(Color.GREEN, regions);
                }
            } else {
                map.remove(Color.GREEN);
            }

            if (jCheckBoxMenuItemTextLine.isSelected()) {
                if (!map.containsKey(Color.RED)) {
                    regions = ocrEngine.getSegmentedRegions(image, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);
                    map.put(Color.RED, regions);
                }
            } else {
                map.remove(Color.RED);
            }

            if (jCheckBoxMenuItemWord.isSelected()) {
                if (!map.containsKey(Color.BLUE)) {
                    regions = ocrEngine.getSegmentedRegions(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);
                    map.put(Color.BLUE, regions);
                }
            } else {
                map.remove(Color.BLUE);
            }

            if (jCheckBoxMenuItemSymbol.isSelected()) {
                if (!map.containsKey(Color.MAGENTA)) {
                    regions = ocrEngine.getSegmentedRegions(image, ITessAPI.TessPageIteratorLevel.RIL_SYMBOL);
                    map.put(Color.MAGENTA, regions);
                }
            } else {
                map.remove(Color.MAGENTA);
            }

            ((JImageLabel) jImageLabel).setSegmentedRegions(map);
            jImageLabel.repaint();
            jImageLabel.revalidate();
        } catch (Exception ex) {
            Logger.getLogger(Gui.class.getName()).log(Level.INFO, null, ex);
        }
    }

    void clearStack() {
        // to be implemented in subclass
    }

    private void jMenuItemSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveActionPerformed
        saveAction();
    }//GEN-LAST:event_jMenuItemSaveActionPerformed

    /**
     * Save file action.
     *
     * @return
     */
    boolean saveAction() {
        if (textFile == null || !textFile.exists()) {
            return saveFileDlg();
        } else {
            return saveTextFile();
        }
    }

    private void jMenuItemSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveAsActionPerformed
        saveFileDlg();
    }//GEN-LAST:event_jMenuItemSaveAsActionPerformed

    /**
     * Displays save file dialog.
     *
     * @return
     */
    boolean saveFileDlg() {
        JFileChooser saveChooser = new JFileChooser(outputDirectory);
        FileFilter textFilter = new SimpleFilter("txt", bundle.getString("UTF-8_Text"));
        saveChooser.addChoosableFileFilter(textFilter);
        saveChooser.setFileFilter(textFilter);
        saveChooser.setDialogTitle(bundle.getString("Save_As"));
        if (textFile != null) {
            saveChooser.setSelectedFile(textFile);
        }

        if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirectory = saveChooser.getCurrentDirectory().getPath();
            File f = saveChooser.getSelectedFile();
            if (saveChooser.getFileFilter() == textFilter) {
                if (!f.getName().endsWith(".txt")) {
                    f = new File(f.getPath() + ".txt");
                }
                if (textFile != null && textFile.getPath().equals(f.getPath())) {
                    if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(
                            Gui.this,
                            String.format(bundle.getString("file_already_exist"), textFile.getName()),
                            bundle.getString("Confirm_Save_As"), JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE)) {
                        return false;
                    }
                } else {
                    textFile = f;
                }
            } else {
                textFile = f;
            }
            return saveTextFile();
        } else {
            return false;
        }
    }

    /**
     * Saves output text file.
     *
     * @return
     */
    boolean saveTextFile() {
        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getGlassPane().setVisible(true);

        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(textFile), UTF8));
            jTextArea1.write(out);
            out.close();
            updateMRUList(textFile.getPath());
            updateSave(false);
        } catch (OutOfMemoryError oome) {
            JOptionPane.showMessageDialog(this, oome.getMessage(), bundle.getString("OutOfMemoryError"), JOptionPane.ERROR_MESSAGE);
        } catch (FileNotFoundException fnfe) {
            showError(fnfe, fnfe.getMessage());
        } catch (Exception ex) {
            showError(ex, ex.getMessage());
        } finally {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    getGlassPane().setVisible(false);
                }
            });
        }

        return true;
    }

    /**
     * Displays a dialog to save changes.
     *
     * @return false if user canceled, true else
     */
    protected boolean promptToSave() {
        if (!textChanged) {
            return true;
        }
        switch (JOptionPane.showConfirmDialog(this,
                String.format(bundle.getString("Do_you_want_to_save_the_changes_to_"),
                        (textFile == null ? bundle.getString("Untitled") : textFile.getName())),
                APP_NAME, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE)) {
            case JOptionPane.YES_OPTION:
                return saveAction();
            case JOptionPane.NO_OPTION:
                return true;
            default:
                return false;
        }
    }

    /**
     * Updates the Save action.
     *
     * @param modified whether file has been modified
     */
    void updateSave(boolean modified) {
        if (textChanged != modified) {
            textChanged = modified;
            this.jButtonSave.setEnabled(modified);
            this.jMenuItemSave.setEnabled(modified);
            rootPane.putClientProperty("windowModified", modified);
            // see http://developer.apple.com/qa/qa2001/qa1146.html
        }
    }

    /**
     * Enables or disables page navigation buttons.
     */
    void setButton() {
        if (imageIndex == 0) {
            this.jButtonPrevPage.setEnabled(false);
        } else {
            this.jButtonPrevPage.setEnabled(true);
        }

        if (imageIndex == imageList.size() - 1) {
            this.jButtonNextPage.setEnabled(false);
        } else {
            this.jButtonNextPage.setEnabled(true);
        }
    }

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        jSplitPane1.setDividerLocation(jSplitPane1.getWidth() / 2);

        if (isFitImageSelected && imageIcon != null) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    ((JImageLabel) jImageLabel).deselect();
                    Dimension fitSize = fitImagetoContainer(originalW, originalH, jScrollPaneImage.getViewport().getWidth(), jScrollPaneImage.getViewport().getHeight());
                    fitImageChange(fitSize.width, fitSize.height);
                    setScale(fitSize.width, fitSize.height);
                }
            });
        }
    }//GEN-LAST:event_formComponentResized

    /**
     * Best fit image height and width calculation algorithm.
     *
     * http://www.karpach.com/Best-fit-calculations-algorithm.htm
     *
     * @param w
     * @param h
     * @param maxWidth
     * @param maxHeight
     */
    Dimension fitImagetoContainer(int w, int h, int maxWidth, int maxHeight) {
        float ratio = (float) w / h;

        w = maxWidth;
        h = (int) Math.floor(maxWidth / ratio);

        if (h > maxHeight) {
            h = maxHeight;
            w = (int) Math.floor(maxHeight * ratio);
        }

        return new Dimension(w, h);
    }

    /**
     * Sets image scale.
     *
     * @param width
     * @param height
     */
    void setScale(int width, int height) {
        scaleX = (float) originalW / width;
        scaleY = (float) originalH / height;
        if (scaleX > scaleY) {
            scaleY = scaleX;
        } else {
            scaleX = scaleY;
        }
    }

    void fitImageChange(final int width, final int height) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                imageIcon.setScaledSize(width, height);
                jScrollPaneImage.getViewport().setViewPosition(curScrollPos);
                jImageLabel.revalidate();
                jScrollPaneImage.repaint();
            }
        });
    }

    void jMenuItemScanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemScanActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemScanActionPerformed
    private void jButtonScanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonScanActionPerformed
        jMenuItemScanActionPerformed(evt);
    }//GEN-LAST:event_jButtonScanActionPerformed

    void jButtonRotateCCWActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRotateCCWActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jButtonRotateCCWActionPerformed

    void jButtonRotateCWActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRotateCWActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jButtonRotateCWActionPerformed

    void jMenuItemOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOptionsActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemOptionsActionPerformed
    void jMenuItemChangeCaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemChangeCaseActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemChangeCaseActionPerformed
    void jMenuItemRemoveLineBreaksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRemoveLineBreaksActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemRemoveLineBreaksActionPerformed
    void jMenuItemMergeTiffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemMergeTiffActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemMergeTiffActionPerformed
    void jMenuItemSplitPdfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSplitPdfActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemSplitPdfActionPerformed
    void jButtonCancelOCRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelOCRActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jButtonCancelOCRActionPerformed
    void jMenuItemMergePdfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemMergePdfActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemMergePdfActionPerformed
    void jMenuItemMetadataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemMetadataActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemMetadataActionPerformed
    void jToggleButtonSpellCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonSpellCheckActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jToggleButtonSpellCheckActionPerformed
    void jMenuItemDownloadLangDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDownloadLangDataActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemDownloadLangDataActionPerformed
    private void jTextArea1MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextArea1MouseEntered
        if (!this.jTextArea1.isFocusOwner() && this.isActive()) {
            this.jTextArea1.requestFocusInWindow();
        }
    }//GEN-LAST:event_jTextArea1MouseEntered
    void jMenuItemDeskewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDeskewActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemDeskewActionPerformed

    private void jImageLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jImageLabelMouseEntered
        if (!this.jImageLabel.isFocusOwner() && this.isActive()) {
            jImageLabel.requestFocusInWindow();
        }
    }//GEN-LAST:event_jImageLabelMouseEntered

    void jMenuItemBulkOCRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemBulkOCRActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemBulkOCRActionPerformed

    void jMenuItemAutocropActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAutocropActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemAutocropActionPerformed

    void jMenuItemBrightnessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemBrightnessActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemBrightnessActionPerformed

    void jMenuItemContrastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemContrastActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemContrastActionPerformed

    void jMenuItemGammaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemGammaActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemGammaActionPerformed

    void jMenuItemThresholdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemThresholdActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemThresholdActionPerformed

    void jMenuItemGrayscaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemGrayscaleActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemGrayscaleActionPerformed

    void jMenuItemMonochromeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemMonochromeActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemMonochromeActionPerformed

    void jMenuItemInvertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemInvertActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemInvertActionPerformed

    void jMenuItemSharpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSharpenActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemSharpenActionPerformed

    void jMenuItemSmoothActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSmoothActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemSmoothActionPerformed

    void jMenuItemBilateralFilteringActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemBilateralFilteringActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemBilateralFilteringActionPerformed

    void jMenuItemUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemUndoActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemUndoActionPerformed

    void jMenuItemSplitTiffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSplitTiffActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemSplitTiffActionPerformed

    private void jButtonCollapseExpandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCollapseExpandActionPerformed
        this.jButtonCollapseExpand.setText(this.jButtonCollapseExpand.getText().equals("»") ? "«" : "»");
        boolean collapsed = this.jButtonCollapseExpand.getText().equals("»");
        this.jSplitPaneImage.setDividerLocation(collapsed ? 0 : this.jSplitPaneImage.getLastDividerLocation());
        this.jSplitPaneImage.setDividerSize(collapsed ? 0 : 5);
    }//GEN-LAST:event_jButtonCollapseExpandActionPerformed

    void jCheckBoxMenuItemScreenshotModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemScreenshotModeActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jCheckBoxMenuItemScreenshotModeActionPerformed

    private void jButtonPasteImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPasteImageActionPerformed
        pasteImage();
    }//GEN-LAST:event_jButtonPasteImageActionPerformed

    private void jButtonPostProcessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPostProcessActionPerformed
        this.jMenuItemPostProcess.doClick();
    }//GEN-LAST:event_jButtonPostProcessActionPerformed

    private void jButtonRemoveLineBreaksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRemoveLineBreaksActionPerformed
        this.jMenuItemRemoveLineBreaks.doClick();
    }//GEN-LAST:event_jButtonRemoveLineBreaksActionPerformed

    private void jComboBoxPageNumActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxPageNumActionPerformed
        int pageNum = Integer.parseInt(this.jComboBoxPageNum.getSelectedItem().toString());
        ((JImageLabel) jImageLabel).deselect();
        imageIndex = pageNum - 1;
        jLabelStatus.setText(null);
        jProgressBar1.setString(null);
        jProgressBar1.setVisible(false);
        displayImage();
        clearStack();
        setButton();
    }//GEN-LAST:event_jComboBoxPageNumActionPerformed

    private void jCheckBoxMenuItemSegmentedRegionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemSegmentedRegionsActionPerformed
        this.jButtonSegmentedRegions.setVisible(this.jCheckBoxMenuItemSegmentedRegions.isSelected());
        setSegmentedRegions();
    }//GEN-LAST:event_jCheckBoxMenuItemSegmentedRegionsActionPerformed

    private void jScrollPaneImageMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_jScrollPaneImageMouseWheelMoved
        if (evt.isControlDown()) {
            final float delta = -0.12f * evt.getWheelRotation();

            if (delta <= 0) {
                // set minimum size to zoom
                if (imageIcon.getIconWidth() < 100) {
                    return;
                }
            } else // set maximum size to zoom
            {
                if (imageIcon.getIconWidth() > 10000) {
                    return;
                }
            }

            ((JImageLabel) jImageLabel).deselect();
            ((JImageLabel) jImageLabel).setSegmentedRegions(null);

            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    int width = imageIcon.getIconWidth();
                    int height = imageIcon.getIconHeight();
                    width += width * delta;
                    height += height * delta;
                    imageIcon.setScaledSize(width, height);

                    jImageLabel.revalidate();
                    jScrollPaneImage.repaint();

                    scaleX = originalW / (float) width;
                    scaleY = originalH / (float) height;
                }
            });

            isFitImageSelected = false;
            this.jButtonActualSize.setEnabled(true);
        }
    }//GEN-LAST:event_jScrollPaneImageMouseWheelMoved

    void jMenuItemRemoveLinesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRemoveLinesActionPerformed
        // to be implemented in subclass
    }//GEN-LAST:event_jMenuItemRemoveLinesActionPerformed

    private void jCheckBoxMenuItemSymbolActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemSymbolActionPerformed
        setSegmentedRegions();
    }//GEN-LAST:event_jCheckBoxMenuItemSymbolActionPerformed

    private void jCheckBoxMenuItemBlockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemBlockActionPerformed
        setSegmentedRegions();
    }//GEN-LAST:event_jCheckBoxMenuItemBlockActionPerformed

    private void jCheckBoxMenuItemParaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemParaActionPerformed
        setSegmentedRegions();
    }//GEN-LAST:event_jCheckBoxMenuItemParaActionPerformed

    private void jCheckBoxMenuItemWordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemWordActionPerformed
        setSegmentedRegions();
    }//GEN-LAST:event_jCheckBoxMenuItemWordActionPerformed

    private void jCheckBoxMenuItemTextLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemTextLineActionPerformed
        setSegmentedRegions();
    }//GEN-LAST:event_jCheckBoxMenuItemTextLineActionPerformed

    private void jButtonSegmentedRegionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSegmentedRegionsActionPerformed
        jPopupMenuSegmentedRegions.show(jButtonSegmentedRegions, 0, 0 - (int) jPopupMenuSegmentedRegions.getPreferredSize().getHeight());
    }//GEN-LAST:event_jButtonSegmentedRegionsActionPerformed

    void jMenuItemCropActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCropActionPerformed
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }//GEN-LAST:event_jMenuItemCropActionPerformed

    private void jTextArea1MouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_jTextArea1MouseWheelMoved
        if (evt.isControlDown()) {
            Font f = jTextArea1.getFont();
            int newSize = f.getSize() - evt.getWheelRotation();
            if (newSize > FONT_MIN_SIZE && newSize < FONT_MAX_SIZE) {
                f = new Font(f.getFontName(), f.getStyle(), newSize);
            }
            jTextArea1.setFont(f);
            jTextArea1.validate();
        } else {
            ((Component) evt.getSource()).getParent().dispatchEvent(evt);
        }
    }//GEN-LAST:event_jTextArea1MouseWheelMoved

    /**
     * Pastes image from clipboard.
     */
    void pasteImage() {
        try {
            Image image = ImageHelper.getClipboardImage();
            if (image != null) {
                File tempFile = File.createTempFile("tmp", ".png");
                ImageIO.write((BufferedImage) image, "png", tempFile);
                openFile(tempFile);
                tempFile.deleteOnExit();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * Loads thumbnails.
     */
    void loadThumbnails() {
        // to be implemented in subclass
    }

    /**
     * Changes locale of UI elements.
     *
     * @param locale
     */
    void changeUILanguage(final Locale locale) {
        if (locale.equals(Locale.getDefault())) {
            return; // no change in locale
        }
        Locale.setDefault(locale);
        bundle = java.util.ResourceBundle.getBundle("net.sourceforge.vietocr.Gui");

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                FormLocalizer localizer = new FormLocalizer(Gui.this, Gui.class);
                localizer.ApplyCulture(bundle);

                if (helptopicsFrame != null) {
                    helptopicsFrame.setTitle(jMenuItemHelp.getText());
                }
                jFileChooser.setDialogTitle(bundle.getString("jButtonOpen.ToolTipText"));
                popup.removeAll();
                populatePopupMenu();
                updateMRUMenu();

                for (Component comp : jMenuUILang.getMenuComponents()) {
                    JMenuItem item = (JMenuItem) comp;
                    Locale locale = new Locale(item.getActionCommand());
                    item.setText(locale.getDisplayLanguage());
                }
            }
        });
    }

    /**
     * Shows a warning message
     *
     * @param e the exception to warn about
     * @param message the message to display
     */
    public void showError(Exception e, String message) {
        logger.log(Level.WARNING, e.getMessage(), e);
        JOptionPane.showMessageDialog(this, message, APP_NAME, JOptionPane.WARNING_MESSAGE);
    }

    private int snap(final int ideal, final int min, final int max) {
        final int TOLERANCE = 0;
        return ideal < min + TOLERANCE ? min : (ideal > max - TOLERANCE ? max : ideal);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        selectedUILang = prefs.get(strUILanguage, "en");
        Locale.setDefault(getLocale(selectedUILang));

        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                new Gui().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler filler1;
    javax.swing.JButton jButtonActualSize;
    protected javax.swing.JButton jButtonCancelOCR;
    private javax.swing.JButton jButtonClear;
    private javax.swing.JButton jButtonCollapseExpand;
    javax.swing.JButton jButtonFitImage;
    private javax.swing.JButton jButtonNextPage;
    protected javax.swing.JButton jButtonOCR;
    private javax.swing.JButton jButtonOpen;
    private javax.swing.JButton jButtonPasteImage;
    private javax.swing.JButton jButtonPostProcess;
    private javax.swing.JButton jButtonPrevPage;
    private javax.swing.JButton jButtonRemoveLineBreaks;
    private javax.swing.JButton jButtonRotateCCW;
    private javax.swing.JButton jButtonRotateCW;
    private javax.swing.JButton jButtonSave;
    protected javax.swing.JButton jButtonScan;
    private javax.swing.JButton jButtonSegmentedRegions;
    javax.swing.JButton jButtonZoomIn;
    javax.swing.JButton jButtonZoomOut;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemBlock;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPara;
    protected javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemScreenshotMode;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemSegmentedRegions;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemSymbol;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemTextLine;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemWord;
    protected javax.swing.JCheckBoxMenuItem jCheckBoxMenuWordWrap;
    protected javax.swing.JComboBox jComboBoxLang;
    protected javax.swing.JComboBox jComboBoxPageNum;
    protected javax.swing.JFileChooser jFileChooser;
    protected javax.swing.JLabel jImageLabel;
    private javax.swing.JLabel jLabelDimension;
    private javax.swing.JLabel jLabelDimensionValue;
    private javax.swing.JLabel jLabelLanguage;
    private javax.swing.JLabel jLabelPSM;
    protected javax.swing.JLabel jLabelPSMvalue;
    private javax.swing.JLabel jLabelPageMax;
    private javax.swing.JLabel jLabelScreenshotMode;
    protected javax.swing.JLabel jLabelScreenshotModeValue;
    protected javax.swing.JLabel jLabelStatus;
    private javax.swing.JMenuBar jMenuBar2;
    private javax.swing.JMenu jMenuCommand;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenu jMenuFilter;
    private javax.swing.JMenu jMenuFormat;
    private javax.swing.JMenu jMenuHelp;
    private javax.swing.JMenu jMenuImage;
    protected javax.swing.JMenu jMenuInputMethod;
    private javax.swing.JMenuItem jMenuItemAbout;
    private javax.swing.JMenuItem jMenuItemAutocrop;
    javax.swing.JMenuItem jMenuItemBilateralFiltering;
    private javax.swing.JMenuItem jMenuItemBrightness;
    protected javax.swing.JMenuItem jMenuItemBulkOCR;
    private javax.swing.JMenuItem jMenuItemChangeCase;
    private javax.swing.JMenuItem jMenuItemContrast;
    private javax.swing.JMenuItem jMenuItemCrop;
    private javax.swing.JMenuItem jMenuItemDeskew;
    private javax.swing.JMenuItem jMenuItemDownloadLangData;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemFont;
    private javax.swing.JMenuItem jMenuItemGamma;
    private javax.swing.JMenuItem jMenuItemGrayscale;
    private javax.swing.JMenuItem jMenuItemHelp;
    private javax.swing.JMenuItem jMenuItemInvert;
    private javax.swing.JMenuItem jMenuItemMergePdf;
    private javax.swing.JMenuItem jMenuItemMergeTiff;
    private javax.swing.JMenuItem jMenuItemMetadata;
    private javax.swing.JMenuItem jMenuItemMonochrome;
    protected javax.swing.JMenuItem jMenuItemOCR;
    protected javax.swing.JMenuItem jMenuItemOCRAll;
    private javax.swing.JMenuItem jMenuItemOpen;
    private javax.swing.JMenuItem jMenuItemOptions;
    protected javax.swing.JMenuItem jMenuItemPostProcess;
    private javax.swing.JMenuItem jMenuItemRemoveLineBreaks;
    private javax.swing.JMenuItem jMenuItemRemoveLines;
    private javax.swing.JMenuItem jMenuItemSave;
    private javax.swing.JMenuItem jMenuItemSaveAs;
    protected javax.swing.JMenuItem jMenuItemScan;
    private javax.swing.JMenuItem jMenuItemSharpen;
    private javax.swing.JMenuItem jMenuItemSmooth;
    private javax.swing.JMenuItem jMenuItemSplitPdf;
    private javax.swing.JMenuItem jMenuItemSplitTiff;
    private javax.swing.JMenuItem jMenuItemThreshold;
    protected javax.swing.JMenuItem jMenuItemUndo;
    protected javax.swing.JMenu jMenuLookAndFeel;
    protected javax.swing.JMenu jMenuPSM;
    private javax.swing.JMenu jMenuRecentFiles;
    private javax.swing.JMenu jMenuSettings;
    private javax.swing.JMenu jMenuTools;
    protected javax.swing.JMenu jMenuUILang;
    private javax.swing.JPanel jPanelArrow;
    private javax.swing.JPanel jPanelImage;
    private javax.swing.JPanel jPanelSegmentedRegions;
    private javax.swing.JPanel jPanelStatus;
    private javax.swing.JPanel jPanelTextArea;
    protected javax.swing.JPanel jPanelThumb;
    private javax.swing.JPopupMenu jPopupMenuSegmentedRegions;
    protected javax.swing.JProgressBar jProgressBar1;
    protected javax.swing.JScrollPane jScrollPaneImage;
    private javax.swing.JScrollPane jScrollPaneText;
    private javax.swing.JScrollPane jScrollPaneThumbnail;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator11;
    private javax.swing.JPopupMenu.Separator jSeparator12;
    private javax.swing.JPopupMenu.Separator jSeparator13;
    private javax.swing.JToolBar.Separator jSeparator14;
    private javax.swing.JToolBar.Separator jSeparator15;
    private javax.swing.JToolBar.Separator jSeparator16;
    private javax.swing.JToolBar.Separator jSeparator17;
    private javax.swing.JPopupMenu.Separator jSeparator18;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JToolBar.Separator jSeparator7;
    private javax.swing.JToolBar.Separator jSeparator8;
    private javax.swing.JToolBar.Separator jSeparator9;
    private javax.swing.JPopupMenu.Separator jSeparatorAbout;
    private javax.swing.JSeparator jSeparatorDim;
    private javax.swing.JSeparator jSeparatorDimEnd;
    private javax.swing.JSeparator jSeparatorEnd;
    private javax.swing.JPopupMenu.Separator jSeparatorExit;
    private javax.swing.JPopupMenu.Separator jSeparatorInputMethod;
    private javax.swing.JPopupMenu.Separator jSeparatorOptions;
    private javax.swing.JSeparator jSeparatorPSM;
    private javax.swing.JSeparator jSeparatorSM;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPaneImage;
    protected javax.swing.JTextArea jTextArea1;
    protected javax.swing.JToggleButton jToggleButtonSpellCheck;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    protected javax.swing.JPopupMenu popup;
    // End of variables declaration//GEN-END:variables
    private final UndoManager m_undo = new UndoManager();
    protected final UndoableEditSupport undoSupport = new UndoableEditSupport();
    private Action m_undoAction, m_redoAction, actionCut, actionCopy, actionPaste, actionDelete, actionSelectAll;
    private final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    private JFrame helptopicsFrame;
}
