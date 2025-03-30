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
import viskit.ViskitGlobals;
import static viskit.ViskitStatics.DESCRIPTION_HINT;

/**
 * A dialog class that lets the user add a new parameter to the document.
 * After the user clicks "OK", "Cancel", or closes the dialog via the
 * close box, the caller retrieves the "buttonChosen" variable from
 * the object to determine the choice. If the user clicked "OK", the
 * caller can retrieve various choices from the object.
 *
 * @author DMcG
 */
public class EventArgumentDialog extends JDialog 
{
    private final JTextField nameField;    // Text field that holds the parameter name
    private final JTextField descriptionField;          // Text field that holds the description
    private final JComboBox parameterTypeCombo;    // Editable combo box that lets us select a type
    private static EventArgumentDialog eventArgumentDialog;
    private static boolean modified = false;
    private EventArgument myEventArgument;
    private final JButton okButton;
    private final JButton cancelButton;
    public static String newName,  newType,  newDescription; // TODO what happens with these??

    public static boolean showDialog(JFrame frame, EventArgument parm) {
        if (eventArgumentDialog == null) {
            eventArgumentDialog = new EventArgumentDialog(frame, parm);
        } else {
            eventArgumentDialog.setParams(frame, parm);
        }

        eventArgumentDialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private EventArgumentDialog(JFrame parent, EventArgument param) {
        super(parent, "Event Argument", true);
        this.myEventArgument = param;
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
        descriptionLabel.setToolTipText(DESCRIPTION_HINT);
        int w = OneLinePanel.maxWidth(new JComponent[]{nameLab, initLab, typeLab, descriptionLabel});

        nameField = new JTextField(15);
        setMaxHeight(nameField);
        descriptionField = new JTextField(25);
        descriptionField.setToolTipText(DESCRIPTION_HINT);
        setMaxHeight(descriptionField);
        parameterTypeCombo = ViskitGlobals.instance().getViskitTypeComboBox();
        setMaxHeight(parameterTypeCombo);

        fieldsPanel.add(new OneLinePanel(nameLab, w, nameField));
        fieldsPanel.add(new OneLinePanel(typeLab, w, parameterTypeCombo));
        fieldsPanel.add(new OneLinePanel(descriptionLabel, w, descriptionField));
        panel.add(fieldsPanel);
        panel.add(Box.createVerticalStrut(5));

        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        cancelButton = new JButton("Cancel");
        okButton = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(okButton);
        buttPan.add(cancelButton);
        panel.add(buttPan);
        panel.add(Box.createVerticalGlue());    // takes up space when dialog is expanded vertically
        cont.add(panel);

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

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
        myEventArgument = p;

        fillWidgets();

        modified = (p == null);
        okButton.setEnabled(p == null);

        getRootPane().setDefaultButton(cancelButton);
        pack();
        setLocationRelativeTo(c);
    }

    private void fillWidgets() 
    {
        if (myEventArgument != null) {
            nameField.setText(myEventArgument.getName());
            parameterTypeCombo.setSelectedItem(myEventArgument.getType());
            if (!myEventArgument.getDescription().isEmpty()) {
                descriptionField.setText(myEventArgument.getDescription()); // .get(0));
            } 
            else {
                descriptionField.setText("");
            }
        } else {
            nameField.setText("");
            descriptionField.setText("");
        }
    }

    private void unloadWidgets() 
    {
        String name = nameField.getText();
        name = name.replaceAll("\\s", ""); // ensure no spaces
        String type = (String) parameterTypeCombo.getSelectedItem();
        type = ViskitGlobals.instance().typeChosen(type);

        if (myEventArgument != null) 
        {
            myEventArgument.setName(name);
            myEventArgument.setType(type);
            // obsolete
//            myEventArgument.getDescriptionArray().clear();
//            String cs = descriptionField.getText().trim();
//            if (cs.length() > 0) {
//                myEventArgument.getDescriptionArray().add(0, cs);
//            }
            myEventArgument.setDescription(descriptionField.getText().trim());
        } 
        else { // TODO what happens with these??
            newName = name;
            newType = type;
            newDescription = descriptionField.getText().trim();
        }
    }

    class cancelButtonListener implements ActionListener 
    {
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
                int ret = JOptionPane.showConfirmDialog(EventArgumentDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
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
