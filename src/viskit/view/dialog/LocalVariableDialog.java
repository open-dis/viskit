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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
public class LocalVariableDialog extends JDialog 
{
    static final Logger LOG = LogManager.getLogger();
    
    public static String newName,  newType,  newValue,  newDescription;
    
    private final JTextField nameField;    // Text field that holds the parameter name
    private final JTextField valueField;       // Text field that holds the expression
    private final JTextField descriptionField;          // Text field that holds the description
    private final JComboBox typeComboBox;    // Editable combo box that lets us select a type
    private static LocalVariableDialog localVariableDialog;
    private static boolean modified = false;
    private EventLocalVariable eventLocalVariable;
    private final JButton okButton;
    private final JButton cancelButton;

    public static boolean showDialog(JFrame parentFrame, EventLocalVariable parameter) 
    {
        if (localVariableDialog == null) {
            localVariableDialog = new LocalVariableDialog(parentFrame, parameter);
        } 
        else 
        {
            localVariableDialog.setParameters(parentFrame, parameter);
        }

        localVariableDialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private LocalVariableDialog(JFrame parentFrame, EventLocalVariable eventLocalVariable) 
    {
        super(parentFrame, "Local Variable Inspector", true);
        this.eventLocalVariable = eventLocalVariable;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        Container contentPanelContainer = getContentPane();
        contentPanelContainer.setLayout(new BoxLayout(contentPanelContainer, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

        panel.add(Box.createVerticalStrut(5));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel("name");
        JLabel initialValueLabel = new JLabel("initial value");
        JLabel typeLabel = new JLabel("type");
        JLabel descriptionLabel = new JLabel("description");
        descriptionLabel.setToolTipText(DESCRIPTION_HINT);
        int w = OneLinePanel.maxWidth(new JComponent[]{nameLabel, initialValueLabel, typeLabel, descriptionLabel});

        nameField = new JTextField(15);
        setMaxHeight(nameField);
        valueField = new JTextField(25);
        setMaxHeight(valueField);
        descriptionField = new JTextField(25);
        descriptionField.setToolTipText(DESCRIPTION_HINT);
        setMaxHeight(descriptionField);
        typeComboBox = ViskitGlobals.instance().getViskitTypeComboBox();
        setMaxHeight(typeComboBox);

        fieldsPanel.add(new OneLinePanel(nameLabel, w, nameField));
        fieldsPanel.add(new OneLinePanel(typeLabel, w, typeComboBox));
        fieldsPanel.add(new OneLinePanel(initialValueLabel, w, valueField));
        fieldsPanel.add(new OneLinePanel(descriptionLabel, w, descriptionField));
        panel.add(fieldsPanel);
        panel.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        cancelButton = new JButton("Cancel");
            okButton = new JButton("Apply changes");
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel);
        panel.add(Box.createVerticalGlue());    // takes up space when dialog is expanded vertically
        contentPanelContainer.add(panel);

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
            okButton.addActionListener(new applyButtonListener());

        enableApplyButtonListener lis = new enableApplyButtonListener();
        this.nameField.addCaretListener(lis);
        this.descriptionField.addCaretListener(lis);
        this.valueField.addCaretListener(lis);
        this.typeComboBox.addActionListener(lis);

        setParameters(parentFrame, eventLocalVariable);
    }

    private void setMaxHeight(JComponent c) {
        Dimension d = c.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        c.setMaximumSize(d);
    }

    public final void setParameters(Component component, EventLocalVariable eventLocalVariable) {
        eventLocalVariable = eventLocalVariable;

        fillWidgets();

        modified = (eventLocalVariable == null);
        okButton.setEnabled(eventLocalVariable == null);

        getRootPane().setDefaultButton(cancelButton);
        pack();
        setLocationRelativeTo(component);
    }

    private void fillWidgets() 
    {
        if (eventLocalVariable != null) 
        {
            nameField.setText(eventLocalVariable.getName());
            typeComboBox.setSelectedItem(eventLocalVariable.getType());
            valueField.setText(eventLocalVariable.getValue());
            descriptionField.setText(eventLocalVariable.getDescription());
            descriptionField.setToolTipText(DESCRIPTION_HINT);
        } 
        else 
        {
            nameField.setText("local variable name");
            descriptionField.setText("description");
            
        }
    }

    private void unloadWidgets() 
    {
        String type = (String) typeComboBox.getSelectedItem();
        type = ViskitGlobals.instance().typeChosen(type);
        String name = nameField.getText();
        name = name.replaceAll("\\s", ""); // remove spaces from name
        if (eventLocalVariable != null) 
        {
            eventLocalVariable.setName(name);
            eventLocalVariable.setType(type);
            eventLocalVariable.setValue(valueField.getText().trim());
            eventLocalVariable.setDescription(descriptionField.getText().trim());
        } else {
            newName = name;
            newType = type;
            newValue = valueField.getText().trim();
            newDescription = descriptionField.getText().trim();
        }
    }

    class cancelButtonListener implements ActionListener 
    {
        @Override
        public void actionPerformed(ActionEvent event) 
        {
            modified = false;    // for the caller
            dispose();
        }
    }

    class applyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (modified) 
            {
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

    class myCloseListener extends WindowAdapter 
    {
        @Override
        public void windowClosing(WindowEvent e) 
        {
            if (modified) 
            {
                int ret = JOptionPane.showConfirmDialog(LocalVariableDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
                    okButton.doClick();
                } 
                else {
                    cancelButton.doClick();
                }
            } 
            else {
                cancelButton.doClick();
            }
        }
    }
}