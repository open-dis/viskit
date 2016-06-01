package viskit.view.dialog;

import viskit.model.ViskitParameter;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
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
public class ParameterDialog extends JDialog 
{
    private static ParameterDialog dialog;
    private static boolean modified = false;
    public static String newName,  newType,  newDescription;
    private static int count = 0;

    private ViskitParameter parameter;
	Container container;
	private final JPanel contentPanel = new JPanel();
	/** Text field that holds the parameter name */
    private final JTextField parameterNameField = new JTextField(15);
	/** Text field that holds the expression */
    private final JTextField expressionField = new JTextField(25);
	/** Text field that holds the description */
    private final JTextField descriptionField = new JTextField(25);
	/** Editable combo box that lets us select a type */
    private       JComboBox<String> parameterTypeCombo;
    private final JButton     okButton = new JButton("Apply changes");
    private final JButton cancelButton = new JButton("Cancel");

    public static boolean showDialog(JFrame f, ViskitParameter parameter) 
	{
        if (dialog == null) 
		{
            dialog = new ParameterDialog(f, parameter);
        }
		else 
		{
            dialog.setParameters(f, parameter);
        }

        dialog.setVisible(true); // this call blocks

        return modified;
    }

    private ParameterDialog(JFrame parent, ViskitParameter parameter)
	{
        super(parent, "Parameter Inspector", true);
		
		initialize ();

        setParameters(parent, parameter);
	}
	
	private void initialize ()
	{
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new myCloseListener());

        container = getContentPane();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                new EmptyBorder(10, 10, 10, 10)));

        contentPanel.add(Box.createVerticalStrut(5));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));

        JLabel         typeLabel = new JLabel("type");
        JLabel         nameLabel = new JLabel("name");
        JLabel initialValueLabel = new JLabel("initial value"); // not used, force instantiation
        JLabel  descriptionLabel = new JLabel("description");
        int labelWidth = OneLinePanel.maxWidth(new JComponent[]{nameLabel, initialValueLabel, typeLabel, descriptionLabel});

        
        setMaxHeight(parameterNameField);
        
        setMaxHeight(expressionField);
        
        setMaxHeight(descriptionField);

        parameterTypeCombo = ViskitGlobals.instance().getTypeComboBox();
        setMaxHeight(parameterTypeCombo);

        fieldsPanel.add(new OneLinePanel(nameLabel,        labelWidth, parameterNameField));
        fieldsPanel.add(new OneLinePanel(typeLabel,        labelWidth, parameterTypeCombo));
        fieldsPanel.add(new OneLinePanel(descriptionLabel, labelWidth, descriptionField));
        contentPanel.add(fieldsPanel);
        contentPanel.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        contentPanel.add(buttonPanel);
        contentPanel.add(Box.createVerticalGlue());    // takes up space when dialog is expanded vertically
        container.add(contentPanel);

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        enableApplyButtonListener lis = new enableApplyButtonListener();
        parameterNameField.addCaretListener(lis);
        descriptionField.addCaretListener(lis);
        expressionField.addCaretListener(lis);
        parameterTypeCombo.addActionListener(lis);
    }

    private void setMaxHeight(JComponent c) {
        Dimension d = c.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        c.setMaximumSize(d);
    }

    public final void setParameters(Component c, ViskitParameter p) {
        parameter = p;

        fillWidgets();

        okButton.setEnabled((p == null));

        getRootPane().setDefaultButton(cancelButton);
        pack();
        setLocationRelativeTo(c);
    }

    private void fillWidgets() {
        if (parameter != null) {
            parameterNameField.setText(parameter.getName());
            parameterTypeCombo.setSelectedItem(parameter.getType());
            descriptionField.setText(parameter.getDescription());
            descriptionField.setToolTipText(parameter.getDescription());
        } else {
            parameterNameField.setText("NewParameter_" + count++);
            descriptionField.setText("");
            descriptionField.setToolTipText("");
        }
    }

    private void unloadWidgets() 
	{
        String type = (String) parameterTypeCombo.getSelectedItem();
        type = ViskitGlobals.instance().typeChosen(type);
        String name = parameterNameField.getText();
        name = name.replaceAll("\\s", "");
        if (parameter != null) {
            parameter.setName(name);
            //
            if (type.equals("String") || type.equals("Double") || type.equals("Integer")) {
                type = "java.lang." + type;
            }
            parameter.setType(type);
            parameter.setDescription(descriptionField.getText());
        } else {
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
                int returnValue = JOptionPane.showConfirmDialog(ParameterDialog.this, "Apply changes?",
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
