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

    private final JTextField                 nameField;    // Text field that holds the parameter name
    private final JTextField          descriptionField;    // Text field that holds the description
    private final JComboBox<String> parameterTypeCombo;    // Editable combo box that lets us select a type
    private static EventArgumentDialog dialog;
    private static boolean modified = false;
    private EventArgument eventArgument;
    private final JButton okButton,  cancelButton;
    public static String newName,  newType,  newDescription;

    public static boolean showDialog(JFrame f, EventArgument eventArg)
	{
        if (dialog == null) {
            dialog = new EventArgumentDialog(f, eventArg);
        } else {
            dialog.setParameters(f, eventArg);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private EventArgumentDialog(JFrame parent, EventArgument eventArgument)
	{
        super(parent, "Event Argument", true);
        this.eventArgument = eventArgument;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

        panel.add(Box.createVerticalStrut(5));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));

        JLabel nameLabel         = new JLabel("name");
        JLabel initialValueLabel = new JLabel("initial value");
        JLabel typeLabel         = new JLabel("type");
        JLabel descriptionLabel  = new JLabel("description");
        int width = OneLinePanel.maxWidth(new JComponent[]{nameLabel, initialValueLabel, typeLabel, descriptionLabel});

        nameField = new JTextField(15);
        setMaxHeight(nameField);
        descriptionField = new JTextField(25);
        setMaxHeight(descriptionField);
        parameterTypeCombo = ViskitGlobals.instance().getTypeComboBox();
        setMaxHeight(parameterTypeCombo);

        fieldsPanel.add(new OneLinePanel(nameLabel, width, nameField));
        fieldsPanel.add(new OneLinePanel(typeLabel, width, parameterTypeCombo));
        fieldsPanel.add(new OneLinePanel(descriptionLabel, width, descriptionField));
        panel.add(fieldsPanel);
        panel.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            okButton = new JButton("Apply changes");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel);
        panel.add(Box.createVerticalGlue());    // takes up space when dialog is expanded vertically
        contentPane.add(panel);

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        enableApplyButtonListener listener = new enableApplyButtonListener();
        this.nameField.addCaretListener(listener);
        this.descriptionField.addCaretListener(listener);
        this.parameterTypeCombo.addActionListener(listener);

        setParameters(parent, eventArgument);
    }

    private void setMaxHeight(JComponent c) {
        Dimension d = c.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        c.setMaximumSize(d);
    }

    public final void setParameters(Component c, EventArgument p) {
        eventArgument = p;

        fillWidgets();

        modified = (p == null);
        okButton.setEnabled(p == null);

        getRootPane().setDefaultButton(cancelButton);
        pack();
        setLocationRelativeTo(c);
    }

    private void fillWidgets()
	{
        if (eventArgument != null) {
            nameField.setText(eventArgument.getName());
            parameterTypeCombo.setSelectedItem(eventArgument.getType());
            descriptionField.setText(eventArgument.getDescription());
        } 
		else
		{
            nameField.setText("");
            parameterTypeCombo.setSelectedIndex(-1);
            descriptionField.setText("");
        }
    }

    private void unloadWidgets() {
        String type = (String) parameterTypeCombo.getSelectedItem();
        type = ViskitGlobals.instance().typeChosen(type);
        String name = nameField.getText();
        name = name.replaceAll("\\s", "");

        if (eventArgument != null)
		{
            eventArgument.setName(name);
            eventArgument.setType(type);
            eventArgument.setDescription(descriptionField.getText().trim());
        } 
		else 
		{
            newName = name;
            newType = type;
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
