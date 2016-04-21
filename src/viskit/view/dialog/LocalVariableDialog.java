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

    private final JTextField            nameField;    // Text field that holds the parameter name
    private final JTextField           initialValueField;    // Text field that holds the expression
    private final JTextField     descriptionField;    // Text field that holds the description
    private final JComboBox<String>  typeComboBox;    // Editable combo box that lets us select a type
    private static LocalVariableDialog dialog;
    private static boolean       modified = false;
    private EventLocalVariable eventLocalVariable;
    private final JButton okButton,  cancelButton;
    public static String newName,  newType,  newValue,  newDescription;

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

    private LocalVariableDialog(JFrame parent, EventLocalVariable eventLocalVariable) {
        super(parent, "Event Local Variable Inspector", true);
        this.eventLocalVariable = eventLocalVariable;
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

        JLabel         nameLabel = new JLabel("name");
        JLabel initialValueLabel = new JLabel("initial value");
        JLabel         typeLabel = new JLabel("type");
        JLabel  descriptionLabel = new JLabel("description");
        int w = OneLinePanel.maxWidth(new JComponent[]{nameLabel, initialValueLabel, typeLabel, descriptionLabel});

        nameField = new JTextField(15);
        setMaxHeight(nameField);
        initialValueField = new JTextField(25);
        setMaxHeight(initialValueField);
        descriptionField = new JTextField(25);
        setMaxHeight(descriptionField);
        typeComboBox = ViskitGlobals.instance().getTypeComboBox();
        setMaxHeight(typeComboBox);
        //typeCombo = new JComboBox();
        //typeCombo.setModel(ViskitGlobals.instance().getTypeCBModel(typeCombo));
        //                                       setMaxHeight(typeCombo);
        //typeCombo.setEditable(true);

        fieldsPanel.add(new OneLinePanel(nameLabel,         w, nameField));
        fieldsPanel.add(new OneLinePanel(typeLabel,         w, typeComboBox));
        fieldsPanel.add(new OneLinePanel(initialValueLabel, w, initialValueField));
        fieldsPanel.add(new OneLinePanel(descriptionLabel,  w, descriptionField));
        con.add(fieldsPanel);
        con.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            okButton = new JButton("Apply changes");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        con.add(buttonPanel);
        con.add(Box.createVerticalGlue());    // takes up space when dialog is expanded vertically
        cont.add(con);

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        enableApplyButtonListener lis = new enableApplyButtonListener();
        this.nameField.addCaretListener(lis);
        this.descriptionField.addCaretListener(lis);
        this.initialValueField.addCaretListener(lis);
        this.typeComboBox.addActionListener(lis);

        setParams(parent, eventLocalVariable);
    }

    private void setMaxHeight(JComponent c) {
        Dimension d = c.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        c.setMaximumSize(d);
    }

    public final void setParams(Component c, EventLocalVariable p) {
        eventLocalVariable = p;

        fillWidgets();

        modified = (p == null);
        okButton.setEnabled(p == null);

        getRootPane().setDefaultButton(cancelButton);
        pack();
        setLocationRelativeTo(c);
    }

    private void fillWidgets()
	{
        if (eventLocalVariable != null)
		{
                    nameField.setText(eventLocalVariable.getName());
            initialValueField.setText(eventLocalVariable.getValue());
             descriptionField.setText(eventLocalVariable.getDescription());
             descriptionField.setToolTipText(eventLocalVariable.getDescription());
                 typeComboBox.setSelectedItem(eventLocalVariable.getType());
        } 
		else
		{
            nameField.setText("eventLocalVariable name");
            descriptionField.setText("description here");
            descriptionField.setToolTipText("description here");
        }
    }

    private void unloadWidgets()
	{
        String ty = (String) typeComboBox.getSelectedItem();
        ty = ViskitGlobals.instance().typeChosen(ty);
        String name = nameField.getText();
        name = name.replaceAll("\\s", "");
        if (eventLocalVariable != null)
		{
            eventLocalVariable.setName(name);
            eventLocalVariable.setType(ty);
            eventLocalVariable.setValue(initialValueField.getText().trim());
            eventLocalVariable.setDescription(descriptionField.getText().trim());
        } 
		else
		{
            newName = name;
            newType = ty;
            newValue = initialValueField.getText().trim();
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
                int returnValue = JOptionPane.showConfirmDialog(LocalVariableDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (returnValue == JOptionPane.YES_OPTION) {
                    okButton.doClick();
                } else {
                    cancelButton.doClick();
                }
            } else {
                cancelButton.doClick();
            }
        }
    }
}