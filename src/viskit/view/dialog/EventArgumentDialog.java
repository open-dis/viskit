package viskit.view.dialog;

import viskit.model.EventArgument;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import viskit.VGlobals;

/**
 * A dialog class that lets the user add a new parameter to the document.
 * After the user clicks "OK", "Cancel", or closes the dialog via the
 * close box, the caller retrieves the "buttonChosen" variable from
 * the object to determine the choice. If the user clicked "OK", the
 * caller can retrieve various choices from the object.
 *
 * @author DMcG
 */
public class EventArgumentDialog extends JDialog {

    private JTextField nameField;    // Text field that holds the parameter name
    private JTextField descriptionField;          // Text field that holds the description
    private JComboBox parameterTypeCombo;    // Editable combo box that lets us select a type
    private static EventArgumentDialog dialog;
    private static boolean modified = false;
    private EventArgument myEA;
    private JButton okButt,  canButt;
    public static String newName,  newType,  newDescription;

    public static boolean showDialog(JFrame f, EventArgument parm) {
        if (dialog == null) {
            dialog = new EventArgumentDialog(f, parm);
        } else {
            dialog.setParams(f, parm);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private EventArgumentDialog(JFrame parent, EventArgument param) {
        super(parent, "Event Argument", true);
        this.myEA = param;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        Container cont = getContentPane();
        cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

        panel.add(Box.createVerticalStrut(5));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));

        JLabel nameLab = new JLabel("name");
        JLabel initLab = new JLabel("initial value");
        JLabel typeLab = new JLabel("type");
        JLabel descriptionLabel = new JLabel("description");
        int w = OneLinePanel.maxWidth(new JComponent[]{nameLab, initLab, typeLab, descriptionLabel});

        nameField = new JTextField(15);
        setMaxHeight(nameField);
        descriptionField = new JTextField(25);
        setMaxHeight(descriptionField);
        parameterTypeCombo = VGlobals.instance().getTypeCB();
        setMaxHeight(parameterTypeCombo);

        fieldsPanel.add(new OneLinePanel(nameLab, w, nameField));
        fieldsPanel.add(new OneLinePanel(typeLab, w, parameterTypeCombo));
        fieldsPanel.add(new OneLinePanel(descriptionLabel, w, descriptionField));
        panel.add(fieldsPanel);
        panel.add(Box.createVerticalStrut(5));

        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        canButt = new JButton("Cancel");
        okButt = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(canButt);
        buttPan.add(okButt);
        panel.add(buttPan);
        panel.add(Box.createVerticalGlue());    // takes up space when dialog is expanded vertically
        cont.add(panel);

        // attach listeners
        canButt.addActionListener(new cancelButtonListener());
        okButt.addActionListener(new applyButtonListener());

        enableApplyButtonListener listener = new enableApplyButtonListener();
        this.nameField.addCaretListener(listener);
        this.descriptionField.addCaretListener(listener);
        this.parameterTypeCombo.addActionListener(listener);

        setParams(parent, param);
    }

    private void setMaxHeight(JComponent c) {
        Dimension d = c.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        c.setMaximumSize(d);
    }

    public final void setParams(Component c, EventArgument p) {
        myEA = p;

        fillWidgets();

        modified = (p == null);
        okButt.setEnabled(p == null);

        getRootPane().setDefaultButton(canButt);
        pack();
        setLocationRelativeTo(c);
    }

    private void fillWidgets() {
        if (myEA != null) {
            nameField.setText(myEA.getName());
            parameterTypeCombo.setSelectedItem(myEA.getType());
            if (myEA.getDescription().size() > 0) {
                descriptionField.setText(myEA.getDescription().get(0));
            } else {
                descriptionField.setText("");
            }
        } else {
            nameField.setText("");
            descriptionField.setText("");
        }
    }

    private void unloadWidgets() {
        String ty = (String) parameterTypeCombo.getSelectedItem();
        ty = VGlobals.instance().typeChosen(ty);
        String nm = nameField.getText();
        nm = nm.replaceAll("\\s", "");

        if (myEA != null) {
            myEA.setName(nm);
            myEA.setType(ty);
            myEA.getDescription().clear();
            String cs = descriptionField.getText().trim();
            if (cs.length() > 0) {
                myEA.getDescription().add(0, cs);
            }
        } else {
            newName = nm;
            newType = ty;
            newDescription = descriptionField.getText().trim();
        }
    }

    class cancelButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            modified = false;    // for the caller
            dispose();
        }
    }

    class applyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (modified) {
                unloadWidgets();
            }
            dispose();
        }
    }

    class enableApplyButtonListener implements CaretListener, ActionListener {

        @Override
        public void caretUpdate(CaretEvent event) {
            modified = true;
            okButt.setEnabled(true);
            getRootPane().setDefaultButton(okButt);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            caretUpdate(null);
        }
    }

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int ret = JOptionPane.showConfirmDialog(EventArgumentDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
                    okButt.doClick();
                } else {
                    canButt.doClick();
                }
            } else {
                canButt.doClick();
            }
        }
    }
}
