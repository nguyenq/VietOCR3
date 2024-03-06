package javaswingtips;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.Accessible;
import javax.swing.AbstractAction;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import static javax.swing.JComponent.WHEN_FOCUSED;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.ComboPopup;

/**
 * https://github.com/aterai/java-swing-tips/blob/master/CheckedComboBox/src/java/example/MainPanel.java
 *
 * @param <E>
 */
public class CheckedComboBox<E extends CheckableItem> extends JComboBox<E> {

    private final JPanel panel = new JPanel(new BorderLayout());
    private List<String> selectedItems = new ArrayList<>();

    public CheckedComboBox() {
        super();
    }

    public CheckedComboBox(ComboBoxModel<E> model) {
        super(model);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 28);
    }

    @Override
    public void updateUI() {
        setRenderer(null);
        super.updateUI();

        Accessible a = getAccessibleContext().getAccessibleChild(0);
        if (a instanceof ComboPopup) {
            ((ComboPopup) a).getList().addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    JList<?> list = (JList<?>) e.getComponent();
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        updateItem(list.locationToIndex(e.getPoint()));
                    }
                }
            });
        }

        DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
        JCheckBox check = new JCheckBox();
        check.setOpaque(false);
        setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            panel.removeAll();
            Component c = defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (index < 0) {
                JLabel l = (JLabel) c;
                l.setText(getCheckedItemString());
                l.setOpaque(false);
                l.setForeground(list.getForeground());
                panel.setOpaque(false);
            } else {
                check.setSelected(value.isSelected());
                panel.add(check, BorderLayout.WEST);
                panel.setOpaque(true);
                panel.setBackground(c.getBackground());
            }
            panel.add(c);
            return panel;
        });
        initActionMap();
    }

    protected void initActionMap() {
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
        getInputMap(WHEN_FOCUSED).put(ks, "checkbox-select");
        getActionMap().put("checkbox-select", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Accessible a = getAccessibleContext().getAccessibleChild(0);
                if (a instanceof ComboPopup) {
                    updateItem(((ComboPopup) a).getList().getSelectedIndex());
                }
            }
        });
    }

    protected void updateItem(int index) {
        if (isPopupVisible() && index >= 0) {
            E item = getItemAt(index);
            item.setSelected(!item.isSelected());
            String val = item.toString();
            if (item.isSelected()) {
                selectedItems.add(val);
            } else {
                selectedItems.remove(val);
            }            
            setSelectedIndex(-1);
            setSelectedItem(item);
        }
    }

    protected String getCheckedItemString() {
        return String.join("+", selectedItems);
    }

    public void setSelectedItems(List<String> selectedItems) {
        this.selectedItems = new ArrayList<>(selectedItems);
    }
    
    public List<String> getSelectedItems() {
        return this.selectedItems;
    }
}