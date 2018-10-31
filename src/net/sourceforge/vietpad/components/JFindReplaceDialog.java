/**
 *  Copyright 1999-2002 Matthew Robinson and Pavel Vorobiev.
 *  All Rights Reserved.
 *
 *  ===================================================
 *  This program contains code from the book "Swing"
 *  2nd Edition by Matthew Robinson and Pavel Vorobiev
 *  http://www.spindoczine.com/sbe
 *  ===================================================
 */
package net.sourceforge.vietpad.components;

import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import net.sourceforge.vietocr.util.FormLocalizer;
import net.sourceforge.vietpad.inputmethod.VietKeyListener;
import net.sourceforge.vietpad.utilities.VietUtilities;

/**
 * Find and Replace Dialog.
 *
 * @author Quan Nguyen
 * @author Gero Herrmann
 * @version 2.0, 28 September 2018
 * @see "http://vietpad.sourceforge.net"
 */
public class JFindReplaceDialog extends javax.swing.JDialog {

    final boolean LINUX = System.getProperty("os.name").equals("Linux");
    private final JTextComponent txtbox;
    private final JTextField m_txtFind;
    private final JTextField m_txtReplace;
    private Document m_docFind;
    private ResourceBundle bundle;

    static private boolean mouse = false;
    private final static Logger logger = Logger.getLogger(JFindReplaceDialog.class.getName());

    /**
     * Creates new form JFindReplaceDialog
     */
    public JFindReplaceDialog(java.awt.Frame parent, boolean modal, JTextComponent txtbox) {
        super(parent, modal);
        initComponents();
        setLocale(parent.getLocale());
        bundle = ResourceBundle.getBundle("net.sourceforge.vietpad.components.JFindReplaceDialog");
        this.txtbox = txtbox;

        ActionListener findAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                populateComboBox("Find");
                FindNext();
            }
        };

        MouseListener mouseLst = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                mouse = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mouse = false;
            }
        };

        m_txtFind = (JTextField) jComboBoxFind.getEditor().getEditorComponent();
        m_txtFind.getInputMap().put(KeyStroke.getKeyStroke("ctrl H"), "none"); // Workaround Bug ID: 6249912 & 4782077       
        m_txtFind.addActionListener(findAction);
        m_txtFind.addMouseListener(mouseLst);
        m_txtFind.addKeyListener(new VietKeyListener(m_txtFind)); // add Vietnamese input capability
        m_docFind = m_txtFind.getDocument();
        m_txtFind.addCaretListener(new CaretListener() {

            @Override
            public void caretUpdate(CaretEvent e) {
                boolean textExists = m_docFind.getLength() > 0;
                jButtonFindNext.setEnabled(textExists);
                jButtonReplace.setEnabled(textExists);
                jButtonReplaceAll.setEnabled(textExists && (!isMatchRegex() || isMatchDiacritics()));
            }
        });

        m_txtReplace = (JTextField) jComboBoxReplace.getEditor().getEditorComponent();
        m_txtReplace.getInputMap().put(KeyStroke.getKeyStroke("ctrl H"), "none"); // Workaround Bug ID: 6249912 & 4782077
        m_txtReplace.addActionListener(findAction);
        m_txtReplace.addMouseListener(mouseLst);
        m_txtReplace.addKeyListener(new VietKeyListener(m_txtReplace));

        KeyListener findKeyLst = new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    FindNext();
                }
            }
        };

        jButtonFindNext.addActionListener(findAction);
        jButtonFindNext.addKeyListener(findKeyLst);

        ActionListener replaceAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                populateComboBox("Replace");
                Replace();
            }
        };

        KeyListener replaceKeyLst = new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    Replace();
                }
            }
        };

        jButtonReplace.addActionListener(replaceAction);
        jButtonReplace.addKeyListener(replaceKeyLst);

        ActionListener replaceAllAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                populateComboBox("Replace");
//                m_owner.getUndoSupport().beginUpdate();
                ReplaceAll();
//                m_owner.getUndoSupport().endUpdate();
            }
        };

        jButtonReplaceAll.addActionListener(replaceAllAction);

        ItemListener focus4Find2 = new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                jButtonFindNext.requestFocusInWindow();
                Object source = e.getItemSelectable();
                if (source == jCheckBoxMatchDiacritics || source == jCheckBoxMatchRegex) {
                    jButtonReplaceAll.setEnabled(m_docFind.getLength() > 0 && (!isMatchRegex() || isMatchDiacritics()));
                }
                if (source == jCheckBoxMatchRegex) {
                    jCheckBoxMatchWholeWord.setEnabled(!isMatchRegex());
                }
            }
        };

        jCheckBoxMatchCase.addItemListener(focus4Find2);
        jCheckBoxMatchWholeWord.addItemListener(focus4Find2);
        jCheckBoxMatchDiacritics.addItemListener(focus4Find2);
        jCheckBoxMatchRegex.addItemListener(focus4Find2);
        jRadioButtonSearchUp.addItemListener(focus4Find2);
        jRadioButtonSearchDown.addItemListener(focus4Find2);

        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent e) {
                if ("focusOwner".equals(e.getPropertyName()) && (e.getNewValue() instanceof JTextField) && !mouse) {
                    // SelectAll causes loss of highlight of selected text in textarea in Linux
                    if (!LINUX) {
                        ((JTextField) e.getNewValue()).selectAll();
                    }
                }
            }
        });
        //  Handle Escape key to hide the dialog
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action escapeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);

        WindowListener flst = new WindowAdapter() {

            @Override
            public void windowActivated(WindowEvent e) {
                // For use with VietIME input method (http://vietime.sf.net)
                getInputContext().selectInputMethod(getOwner().getInputContext().getLocale());
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        m_txtFind.requestFocusInWindow();
                        if (!LINUX) {
                            m_txtFind.selectAll();
                        }
                    }
                });
            }
        };
        addWindowListener(flst);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel2 = new javax.swing.JPanel();
        jPanelSearchBox = new javax.swing.JPanel();
        jLabelSearchFor = new javax.swing.JLabel();
        jLabelReplaceWith = new javax.swing.JLabel();
        jComboBoxFind = new javax.swing.JComboBox<>();
        jComboBoxReplace = new javax.swing.JComboBox<>();
        jPanelOptions = new javax.swing.JPanel();
        jCheckBoxMatchCase = new javax.swing.JCheckBox();
        jCheckBoxMatchWholeWord = new javax.swing.JCheckBox();
        jCheckBoxMatchDiacritics = new javax.swing.JCheckBox();
        jCheckBoxMatchRegex = new javax.swing.JCheckBox();
        jRadioButtonSearchUp = new javax.swing.JRadioButton();
        jRadioButtonSearchDown = new javax.swing.JRadioButton();
        jPanelButton = new javax.swing.JPanel();
        jButtonFindNext = new javax.swing.JButton();
        jButtonReplace = new javax.swing.JButton();
        jButtonReplaceAll = new javax.swing.JButton();
        jButtonClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("net/sourceforge/vietpad/components/JFindReplaceDialog"); // NOI18N
        setTitle(bundle.getString("this.Title")); // NOI18N
        setResizable(false);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));

        jPanelSearchBox.setPreferredSize(new java.awt.Dimension(319, 80));
        java.awt.GridBagLayout jPanelSearchBoxLayout = new java.awt.GridBagLayout();
        jPanelSearchBoxLayout.columnWidths = new int[] {0, 14, 0};
        jPanelSearchBoxLayout.rowHeights = new int[] {0, 6, 0};
        jPanelSearchBox.setLayout(jPanelSearchBoxLayout);

        jLabelSearchFor.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietpad/components/JFindReplaceDialog").getString("jLabelSearchFor.Mnemonic").charAt(0));
        jLabelSearchFor.setLabelFor(jComboBoxFind);
        jLabelSearchFor.setText(bundle.getString("jLabelSearchFor.Text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanelSearchBox.add(jLabelSearchFor, gridBagConstraints);

        jLabelReplaceWith.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietpad/components/JFindReplaceDialog").getString("jLabelReplaceWith.Mnemonic").charAt(0));
        jLabelReplaceWith.setLabelFor(jComboBoxReplace);
        jLabelReplaceWith.setText(bundle.getString("jLabelReplaceWith.Text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanelSearchBox.add(jLabelReplaceWith, gridBagConstraints);

        jComboBoxFind.setEditable(true);
        jComboBoxFind.setPreferredSize(new java.awt.Dimension(220, 24));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanelSearchBox.add(jComboBoxFind, gridBagConstraints);

        jComboBoxReplace.setEditable(true);
        jComboBoxReplace.setPreferredSize(new java.awt.Dimension(220, 24));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanelSearchBox.add(jComboBoxReplace, gridBagConstraints);

        jPanel2.add(jPanelSearchBox);

        jPanelOptions.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("Options"))); // NOI18N
        jPanelOptions.setLayout(new java.awt.GridBagLayout());

        jCheckBoxMatchCase.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietpad/components/JFindReplaceDialog").getString("jCheckBoxMatchCase.Mnemonic").charAt(0));
        jCheckBoxMatchCase.setText(bundle.getString("jCheckBoxMatchCase.Text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanelOptions.add(jCheckBoxMatchCase, gridBagConstraints);

        jCheckBoxMatchWholeWord.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietpad/components/JFindReplaceDialog").getString("jCheckBoxMatchWholeWord.Mnemonic").charAt(0));
        jCheckBoxMatchWholeWord.setText(bundle.getString("jCheckBoxMatchWholeWord.Text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanelOptions.add(jCheckBoxMatchWholeWord, gridBagConstraints);

        jCheckBoxMatchDiacritics.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietpad/components/JFindReplaceDialog").getString("jCheckBoxMatchDiacritics.Mnemonic").charAt(0));
        jCheckBoxMatchDiacritics.setText(bundle.getString("jCheckBoxMatchDiacritics.Text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanelOptions.add(jCheckBoxMatchDiacritics, gridBagConstraints);

        jCheckBoxMatchRegex.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietpad/components/JFindReplaceDialog").getString("jCheckBoxMatchRegex.Mnemonic").charAt(0));
        jCheckBoxMatchRegex.setText(bundle.getString("jCheckBoxMatchRegex.Text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 20, 2, 4);
        jPanelOptions.add(jCheckBoxMatchRegex, gridBagConstraints);

        jRadioButtonSearchUp.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietpad/components/JFindReplaceDialog").getString("jRadioButtonSearchUp.Mnemonic").charAt(0));
        jRadioButtonSearchUp.setText(bundle.getString("jRadioButtonSearchUp.Text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 20, 2, 4);
        jPanelOptions.add(jRadioButtonSearchUp, gridBagConstraints);

        jRadioButtonSearchDown.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietpad/components/JFindReplaceDialog").getString("jRadioButtonSearchDown.Mnemonic").charAt(0));
        jRadioButtonSearchDown.setSelected(true);
        jRadioButtonSearchDown.setText(bundle.getString("jRadioButtonSearchDown.Text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 20, 2, 4);
        jPanelOptions.add(jRadioButtonSearchDown, gridBagConstraints);
        ButtonGroup group = new ButtonGroup();
        group.add(jRadioButtonSearchUp);
        group.add(jRadioButtonSearchDown);

        jPanel2.add(jPanelOptions);

        getContentPane().add(jPanel2, java.awt.BorderLayout.CENTER);

        jPanelButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 3, 50, 7));
        jPanelButton.setLayout(new java.awt.GridLayout(0, 1, 0, 5));

        jButtonFindNext.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietpad/components/JFindReplaceDialog").getString("jButtonFindNext.Mnemonic").charAt(0));
        jButtonFindNext.setText(bundle.getString("jButtonFindNext.Text")); // NOI18N
        jButtonFindNext.setEnabled(false);
        jPanelButton.add(jButtonFindNext);

        jButtonReplace.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietpad/components/JFindReplaceDialog").getString("jButtonReplace.Mnemonic").charAt(0));
        jButtonReplace.setText(bundle.getString("jButtonReplace.Text")); // NOI18N
        jButtonReplace.setEnabled(false);
        jPanelButton.add(jButtonReplace);

        jButtonReplaceAll.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietpad/components/JFindReplaceDialog").getString("jButtonReplaceAll.Mnemonic").charAt(0));
        jButtonReplaceAll.setText(bundle.getString("jButtonReplaceAll.Text")); // NOI18N
        jButtonReplaceAll.setEnabled(false);
        jPanelButton.add(jButtonReplaceAll);

        jButtonClose.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/vietpad/components/JFindReplaceDialog").getString("jButtonClose.Mnemonic").charAt(0));
        jButtonClose.setText(bundle.getString("jButtonClose.Text")); // NOI18N
        jButtonClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCloseActionPerformed(evt);
            }
        });
        jPanelButton.add(jButtonClose);

        getContentPane().add(jPanelButton, java.awt.BorderLayout.EAST);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCloseActionPerformed
        this.setVisible(false);
    }//GEN-LAST:event_jButtonCloseActionPerformed

    /**
     * Populates the combobox with entries from the corresponding text field
     */
    @SuppressWarnings("unchecked")
    void populateComboBox(String button) {
        String text;
        JComboBox comboBox;

        if (button.equals("Find")) {
            text = m_txtFind.getText();
            comboBox = jComboBoxFind;
        } else {
            text = m_txtReplace.getText();
            comboBox = jComboBoxReplace;
        }
        if (text.equals("")) {
            return;
        }

        boolean isEntryExisted = false;

        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (text.equals((String) comboBox.getItemAt(i))) {
                isEntryExisted = true;
                break;
            }
        }
        if (!isEntryExisted) {
            comboBox.insertItemAt(text, 0);
            comboBox.setSelectedIndex(0);
        }
    }

    /**
     * Finds next occurrence of find string.
     *
     * @return
     */
    boolean FindNext() {
        String searchData, strFind;
        if (!jCheckBoxMatchDiacritics.isSelected()) {
            searchData = VietUtilities.stripDiacritics(txtbox.getText());
            strFind = VietUtilities.stripDiacritics(this.m_txtFind.getText());
        } else {
            searchData = txtbox.getText();
            strFind = this.m_txtFind.getText();
        }

        if (jRadioButtonSearchDown.isSelected()) {
            int iStart = txtbox.getSelectionEnd();

            if (jCheckBoxMatchRegex.isSelected() || jCheckBoxMatchWholeWord.isSelected()) {
                if (jCheckBoxMatchWholeWord.isSelected() && jCheckBoxMatchWholeWord.isEnabled()) {
                    strFind = "\\b" + Pattern.quote(strFind) + "\\b";
                }

                try {
                    Pattern regex = Pattern.compile((jCheckBoxMatchCase.isSelected() ? "" : "(?i)") + strFind, Pattern.MULTILINE);
                    Matcher m = regex.matcher(searchData);
                    m.region(iStart, txtbox.getDocument().getLength());
                    if (m.find()) {
                        txtbox.select(m.start(), m.end());
                        return true;
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                    JOptionPane.showMessageDialog(null, e.getMessage(), bundle.getString("Regex_Error"), JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } else {
                while (iStart + strFind.length() <= txtbox.getDocument().getLength()) {
                    if (searchData.regionMatches(!jCheckBoxMatchCase.isSelected(), iStart, strFind, 0, strFind.length())) {
                        txtbox.select(iStart, iStart + strFind.length());
                        return true;
                    }
                    iStart++;
                }
            }
        } else {
            if (jCheckBoxMatchRegex.isSelected() || jCheckBoxMatchWholeWord.isSelected()) {
                if (jCheckBoxMatchWholeWord.isSelected() && jCheckBoxMatchWholeWord.isEnabled()) {
                    strFind = "\\b" + Pattern.quote(strFind) + "\\b";
                }
                int iEnd = txtbox.getSelectionStart();

                try {
                    Pattern regex = Pattern.compile((jCheckBoxMatchCase.isSelected() ? "" : "(?i)") + String.format("%1$s(?!.*%1$s)", strFind), Pattern.MULTILINE | Pattern.DOTALL);
                    Matcher m = regex.matcher(searchData);
                    m.region(0, iEnd);
                    if (m.find()) {
                        txtbox.select(m.start(), m.end());
                        return true;
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                    JOptionPane.showMessageDialog(null, e.getMessage(), bundle.getString("Regex_Error"), JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } else {
                int iStart = txtbox.getSelectionStart() - strFind.length();

                while (iStart >= 0) {
                    if (searchData.regionMatches(!jCheckBoxMatchCase.isSelected(), iStart, strFind, 0, strFind.length())) {
                        txtbox.select(iStart, iStart + strFind.length());
                        return true;
                    }
                    iStart--;
                }
            }
        }

        int n = JOptionPane.showConfirmDialog(this,
                bundle.getString("Cannot_find_\"") + m_txtFind.getText() + "\".\n"
                + bundle.getString("Continue_search_from_") + (jRadioButtonSearchDown.isSelected() ? bundle.getString("beginning") : bundle.getString("end")) + "?",
                this.getTitle(),
                JOptionPane.YES_NO_OPTION);

        if (n == JOptionPane.YES_OPTION) {
            if (jRadioButtonSearchDown.isSelected()) {
                txtbox.setSelectionStart(0);
            } else {
                txtbox.setSelectionStart(txtbox.getDocument().getLength());
            }

            txtbox.select(txtbox.getSelectionStart(), txtbox.getSelectionStart());
            FindNext();
        }
        return false;
    }

    /**
     * Replaces currently selected text with replacement string.
     */
    void Replace() {
        String strFind = this.m_txtFind.getText();
        String selectedText = txtbox.getSelectedText();

        if (selectedText == null) {
            FindNext();
            return;
        }

        if (!jCheckBoxMatchDiacritics.isSelected()) {
            strFind = VietUtilities.stripDiacritics(strFind);
            selectedText = VietUtilities.stripDiacritics(selectedText);
        }

        String strReplace = this.m_txtReplace.getText();
        int start = txtbox.getSelectionStart();
        if (jCheckBoxMatchRegex.isSelected()) {
            try {
                Pattern regex = Pattern.compile((jCheckBoxMatchCase.isSelected() ? "" : "(?i)") + strFind, Pattern.MULTILINE);
                txtbox.replaceSelection(regex.matcher(selectedText).replaceAll(unescape(strReplace)));
            } catch (Exception e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                JOptionPane.showMessageDialog(null, e.getMessage(), bundle.getString("Regex_Error"), JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else if ((jCheckBoxMatchCase.isSelected() && selectedText.compareTo(strFind) == 0) || (!jCheckBoxMatchCase.isSelected() && selectedText.compareToIgnoreCase(strFind) == 0)) {
            txtbox.replaceSelection(strReplace);
        }

        if (!jRadioButtonSearchDown.isSelected()) {
            txtbox.setSelectionStart(start);
            txtbox.setSelectionEnd(start);
        }

        FindNext();
    }

    /**
     * Replaces all occurrences of find string with replacement.
     */
    void ReplaceAll() {
        String strFind = this.m_txtFind.getText();
        String str = txtbox.getText();
        String strTemp;

        if (!jCheckBoxMatchDiacritics.isSelected()) {
            strFind = VietUtilities.stripDiacritics(strFind);
            strTemp = VietUtilities.stripDiacritics(str);
        } else {
            strTemp = str;
        }

        String strReplace = this.m_txtReplace.getText();
        int count = 0;

        if (jCheckBoxMatchRegex.isSelected() || jCheckBoxMatchDiacritics.isSelected()) {
            // only for MatchDiacritics
            String patt = jCheckBoxMatchRegex.isSelected() ? strFind : Pattern.quote(strFind);
            if (jCheckBoxMatchWholeWord.isSelected() && jCheckBoxMatchWholeWord.isEnabled()) {
                patt = "\\b" + patt + "\\b";
            }

            try {
                Pattern regex = Pattern.compile((jCheckBoxMatchCase.isSelected() ? "" : "(?i)") + patt, Pattern.MULTILINE);
                Matcher matcher = regex.matcher(str);
                while (matcher.find()) {
                    count++;
                }
                matcher.reset();
                str = regex.matcher(str).replaceAll(unescape(strReplace));
            } catch (Exception e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                JOptionPane.showMessageDialog(null, e.getMessage(), bundle.getString("Regex_Error"), JOptionPane.ERROR_MESSAGE);
                return;
            }
//        } else if (jCheckBoxMatchCase.isSelected() && jCheckBoxMatchDiacritics.isSelected()) {
//            if (jCheckBoxMatchWholeWord.isSelected()) {
//                count = (str.length() - str.replaceAll("\\b" + strFind + "\\b","").length()) / strFind.length();
//                str = str.replaceAll("\\b" + strFind + "\\b", strReplace);
//            } else {
//                count = (str.length() - str.replaceAll("\\Q" + strFind + "\\E","").length()) / strFind.length();
//                str = str.replaceAll("\\Q" + strFind + "\\E", strReplace);
//            }
        } else {
            StringBuilder strB = new StringBuilder(str);

            try {
                Pattern wholewordPatt = Pattern.compile((jCheckBoxMatchCase.isSelected() ? "" : "(?i)") + "\\b" + strFind + "\\b", Pattern.MULTILINE);
                for (int i = 0; i <= strB.length() - strFind.length();) {
                    if (strTemp.regionMatches(!jCheckBoxMatchCase.isSelected(), i, strFind, 0, strFind.length())) {
                        // match whole word requires extra treatment
                        if (jCheckBoxMatchWholeWord.isSelected()) {
                            Matcher m = wholewordPatt.matcher(strTemp);
                            if (m.find(i)) {
                                if (i != m.start()) {
                                    i++;
                                    continue;
                                }
                            } else {
                                i++;
                                continue;
                            }
                        }

                        strB.delete(i, i + strFind.length());
                        strB.insert(i, strReplace);
                        if (!jCheckBoxMatchDiacritics.isSelected()) {
                            strTemp = VietUtilities.stripDiacritics(strB.toString());
                        } else {
                            strTemp = strB.toString();
                        }
                        i += strReplace.length();
                        count++;
                    } else {
                        i++;
                    }
                }

                str = strB.toString();
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                JOptionPane.showMessageDialog(null, e.getMessage(), bundle.getString("Regex_Error"), JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        if (!str.equals(txtbox.getText())) {
            txtbox.setText(str);
            txtbox.select(0, 0);
//            txtbox.Modified = true;
        }

        // display count of replacements
        warning(String.format(bundle.getString("ReplacedOccurrence"), count));
    }

    private String unescape(String input) {
        return input.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
    }

    /**
     * Display warning message
     *
     * @param message Warning message
     */
    protected void warning(String message) {
        JOptionPane.showMessageDialog(this,
                message, this.getTitle(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * @return the state of matchCase chkbox
     */
    public boolean isMatchCase() {
        return this.jCheckBoxMatchCase.isSelected();
    }

    /**
     * @param matchCase the matchCase to set
     */
    public void setMatchCase(boolean matchCase) {
        this.jCheckBoxMatchCase.setSelected(matchCase);
    }

    /**
     * @return the state of matchDiacritics chkbox
     */
    public boolean isMatchDiacritics() {
        return this.jCheckBoxMatchDiacritics.isSelected();
    }

    /**
     * @param matchDiacritics the matchDiacritics to set
     */
    public void setMatchDiacritics(boolean matchDiacritics) {
        this.jCheckBoxMatchDiacritics.setSelected(matchDiacritics);
    }

    /**
     * @return the state of matchRegex chkbox
     */
    public boolean isMatchRegex() {
        return this.jCheckBoxMatchRegex.isSelected();
    }

    /**
     * @param matchRegex the matchRegex to set
     */
    public void setMatchRegex(boolean matchRegex) {
        this.jCheckBoxMatchRegex.setSelected(matchRegex);
    }

    /**
     * @return the state of matchWholeWord chkbox
     */
    public boolean isMatchWholeWord() {
        return this.jCheckBoxMatchWholeWord.isSelected();
    }

    /**
     * @param matchWholeWord the matchWholeWord to set
     */
    public void setMatchWholeWord(boolean matchWholeWord) {
        this.jCheckBoxMatchWholeWord.setSelected(matchWholeWord);
    }

    public void changeUILanguage(final Locale locale) {
        Locale.setDefault(locale);
        bundle = ResourceBundle.getBundle("net/sourceforge/vietpad/components/JFindReplaceDialog");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                FormLocalizer localizer = new FormLocalizer(JFindReplaceDialog.this, JFindReplaceDialog.class);
                localizer.ApplyCulture(bundle);
            }
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(JFindReplaceDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(JFindReplaceDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(JFindReplaceDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(JFindReplaceDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                JFindReplaceDialog dialog = new JFindReplaceDialog(new javax.swing.JFrame(), true, new javax.swing.JTextArea());
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonClose;
    private javax.swing.JButton jButtonFindNext;
    private javax.swing.JButton jButtonReplace;
    private javax.swing.JButton jButtonReplaceAll;
    private javax.swing.JCheckBox jCheckBoxMatchCase;
    private javax.swing.JCheckBox jCheckBoxMatchDiacritics;
    private javax.swing.JCheckBox jCheckBoxMatchRegex;
    private javax.swing.JCheckBox jCheckBoxMatchWholeWord;
    private javax.swing.JComboBox<String> jComboBoxFind;
    private javax.swing.JComboBox<String> jComboBoxReplace;
    private javax.swing.JLabel jLabelReplaceWith;
    private javax.swing.JLabel jLabelSearchFor;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanelButton;
    private javax.swing.JPanel jPanelOptions;
    private javax.swing.JPanel jPanelSearchBox;
    private javax.swing.JRadioButton jRadioButtonSearchDown;
    private javax.swing.JRadioButton jRadioButtonSearchUp;
    // End of variables declaration//GEN-END:variables
}
