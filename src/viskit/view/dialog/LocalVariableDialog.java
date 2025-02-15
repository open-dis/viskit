package viskit.view.dialog;

import viskit.model.EventLocalVariable;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import viskit.ViskitGlobals;
import static viskit.ViskitStatics.DESCRIPTION_HINT;

/**
 * A dialog class that lets the user add a new parameter to the document.
 * After the user clicks "OK", "Cancel", or closes the dialog via the
 * close box, the caller retrieves the "buttonChosen" variable from
 * the object to determine the choice. If the user clicked "OK", the
 * caller can retrieve various choices from the object.
 *
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author DMcG, Mike Bailer
 * @since Apr 12, 2004
 * @since 9:19:41 AM
 * @version $Id$
 */
public class LocalVariableDialog extends JDialog {

    public static String newName,  newType,  newValue,  newComment;
    
    private final JTextField nameField;    // Text field that holds the parameter name
    private final JTextField valueField;       // Text field that holds the expression
    private final JTextField descriptionField;          // Text field that holds the description
    private final JComboBox typeCombo;    // Editable combo box that lets us select a type
    private static LocalVariableDialog dialog;
    private static boolean modified = false;
    private EventLocalVariable locVar;
    private final JButton okButton;
    private final JButton canButt;

    public static boolean showDialog(JFrame f, EventLocalVariable parm) {
        if (dialog == null) {
            dialog = new LocalVariableDialog(f, parm);
        } else {
            dialog.setParams(f, parm);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private LocalVariableDialog(JFrame parent, EventLocalVariable lv) {
        super(parent, "Local Variable Inspector", true);
        this.locVar = lv;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        Container cont = getContentPane();
        cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));

        JPanel con = new JPanel();
        con.setLayout(new BoxLayout(con, BoxLayout.Y_AXIS));
        con.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

        con.add(Box.createVerticalStrut(5));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));

        JLabel nameLab = new JLabel("name");
        JLabel initLab = new JLabel("initial value");
        JLabel typeLab = new JLabel("type");
        JLabel descriptionLabel = new JLabel("description");
        descriptionLabel.setToolTipText(DESCRIPTION_HINT);
        int w = OneLinePanel.maxWidth(new JComponent[]{nameLab, initLab, typeLab, descriptionLabel});

        nameField = new JTextField(15);
        setMaxHeight(nameField);
        valueField = new JTextField(25);
        setMaxHeight(valueField);
        descriptionField = new JTextField(25);
        descriptionField.setToolTipText(DESCRIPTION_HINT);
        setMaxHeight(descriptionField);
        typeCombo = ViskitGlobals.instance().getViskitTypeComboBox();
        setMaxHeight(typeCombo);

        fieldsPanel.add(new OneLinePanel(nameLab, w, nameField));
        fieldsPanel.add(new OneLinePanel(typeLab, w, typeCombo));
        fieldsPanel.add(new OneLinePanel(initLab, w, valueField));
        fieldsPanel.add(new OneLinePanel(descriptionLabel, w, descriptionField));
        con.add(fieldsPanel);
        con.add(Box.createVerticalStrut(5));

        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        canButt = new JButton("Cancel");
        okButton = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(okButton);
        buttPan.add(canButt);
        con.add(buttPan);
        con.add(Box.createVerticalGlue());    // takes up space when dialog is expanded vertically
        cont.add(con);

        // attach listeners
        canButt.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        enableApplyButtonListener lis = new enableApplyButtonListener();
        this.nameField.addCaretListener(lis);
        this.descriptionField.addCaretListener(lis);
        this.valueField.addCaretListener(lis);
        this.typeCombo.addActionListener(lis);

        setParams(parent, lv);
    }

    private void setMaxHeight(JComponent c) {
        Dimension d = c.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        c.setMaximumSize(d);
    }

    public final void setParams(Component c, EventLocalVariable p) {
        locVar = p;

        fillWidgets();

        modified = (p == null);
        okButton.setEnabled(p == null);

        getRootPane().setDefaultButton(canButt);
        pack();
        setLocationRelativeTo(c);
    }

    private void fillWidgets() {
        if (locVar != null) {
            nameField.setText(locVar.getName());
            typeCombo.setSelectedItem(locVar.getType());
            valueField.setText(locVar.getValue());
            descriptionField.setText(locVar.getComment());
        } else {
            nameField.setText("locVar name");
            descriptionField.setText("comments here");
        }
    }

    private void unloadWidgets() {
        String ty = (String) typeCombo.getSelectedItem();
        ty = ViskitGlobals.instance().typeChosen(ty);
        String nm = nameField.getText();
        nm = nm.replaceAll("\\s", "");
        if (locVar != null) {
            locVar.setName(nm);
            locVar.setType(ty);
            locVar.setValue(valueField.getText().trim());
            locVar.setComment(descriptionField.getText().trim());
        } else {
            newName = nm;
            newType = ty;
            newValue = valueField.getText().trim();
            newComment = descriptionField.getText().trim();
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
            okButton.setEnabled(true);
            getRootPane().setDefaultButton(okButton);
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
                int ret = JOptionPane.showConfirmDialog(LocalVariableDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
                    okButton.doClick();
                } else {
                    canButt.doClick();
                }
            } else {
                canButt.doClick();
            }
        }
    }
}