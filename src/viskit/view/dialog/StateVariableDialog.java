package viskit.view.dialog;

import viskit.model.EventGraphModel;
import viskit.model.ViskitStateVariable;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import viskit.ViskitGlobals;
import viskit.ViskitConfiguration;
import viskit.ViskitStatics;
import viskit.control.EventGraphControllerImpl;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @version $Id$
 */
public class StateVariableDialog extends ViskitSmallDialog 
{
    private final JTextField stateVariableNameField; // Text field that holds the parameter name
    private JTextField initialValueField;            // Text field that holds the initial value
    private JTextField descriptionField;             // Text field that holds the description
    private JTextField arraySizeField;
    private final JComboBox<String> stateVariableTypeCombo;    // Editable combo box that lets us select a type
    private final JLabel arraySizeLabel;
    private ViskitStateVariable stateVariable;
    private final JButton okButton,  cancelButton;
    public static String newName,  newType, newValue, newDescription;
    private final myFocusListener focusListener;
    private final Component myTyperComponent;       // i.e., the editor of the type JComboBox

    public static boolean showDialog(JFrame f, ViskitStateVariable var) {
        return ViskitSmallDialog.showDialog(StateVariableDialog.class.getName(), f, var);
    }

    protected StateVariableDialog(JFrame parent, Object param)
	{
        super(parent, "State Variable Declaration Inspector", true);

        focusListener = new myFocusListener();

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                new EmptyBorder(10, 10, 10, 10)));

        content.add(Box.createVerticalStrut(5));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));

        JLabel         nameLabel = new JLabel("name");
        JLabel         typeLabel = new JLabel("type");
        JLabel initialValueLabel = new JLabel("initialization");
        JLabel  descriptionLabel = new JLabel("description");
        arraySizeLabel = new JLabel("array size");

        int width = OneLinePanel.maxWidth(new JComponent[] {nameLabel, typeLabel, initialValueLabel, descriptionLabel});

        stateVariableTypeCombo = ViskitGlobals.instance().getTypeComboBox();
        stateVariableTypeCombo.getEditor().getEditorComponent().addFocusListener(focusListener);
        setMaxHeight(stateVariableTypeCombo);

        arraySizeField = new JTextField(25);
        arraySizeField.addFocusListener(focusListener);

        stateVariableNameField = new JTextField(25);
        stateVariableNameField.addFocusListener(focusListener);
        setMaxHeight(stateVariableNameField);

        initialValueField = new JTextField(25);
        initialValueField.addFocusListener(focusListener);
        setMaxHeight(initialValueField);

        descriptionField = new JTextField(25);
        descriptionField.addFocusListener(focusListener);
        setMaxHeight(descriptionField);

        fieldsPanel.add(new OneLinePanel(        typeLabel, width, stateVariableTypeCombo));
        fieldsPanel.add(new OneLinePanel(   arraySizeLabel, width, arraySizeField));
        fieldsPanel.add(new OneLinePanel(        nameLabel, width, stateVariableNameField));
        fieldsPanel.add(new OneLinePanel(initialValueLabel, width, initialValueField));
        fieldsPanel.add(new OneLinePanel( descriptionLabel, width, descriptionField));
        content.add(fieldsPanel);
        content.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            okButton = new JButton("Apply changes");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        content.add(buttonPanel);
        content.add(Box.createVerticalGlue());    // takes up space when dialog is expanded vertically
        contentPane.add(content);

        // attach listeners
        cancelButton.addActionListener(new CancelButtonListener());
        okButton.addActionListener(new StateVariableApplyButtonListener());//applyButtonListener());

        EnableApplyButtonListener listener = new EnableApplyButtonListener(okButton);
        stateVariableNameField.getDocument().addDocumentListener(listener);//addCaretListener(lis);
             initialValueField.getDocument().addDocumentListener(listener);
              descriptionField.getDocument().addDocumentListener(listener);// addCaretListener(lis);
        stateVariableTypeCombo.addActionListener(listener);

        myTyperComponent = stateVariableTypeCombo.getEditor().getEditorComponent();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowClosingListener(this, okButton, cancelButton));

        setParams(parent, param);
    }

    /** Toggle these fields appropriately
     *
     * @param b if true, then enable
     */
    private void toggleArraySizeFields(boolean b) {
        arraySizeLabel.setEnabled(b);
        arraySizeField.setEnabled(b);
        arraySizeField.setEditable(b);     // grays background if false
    }

    @Override
    final void setParams(Component parentComponent, Object p) 
	{
        stateVariable = (ViskitStateVariable) p;

        fillWidgets();

        modified = (p == null);
        okButton.setEnabled(p == null);

        if (p == null) {
            getRootPane().setDefaultButton(okButton);
        } else {
            getRootPane().setDefaultButton(cancelButton);
        }
        pack();
        setLocationRelativeTo(parentComponent);
    }

    private String stripArraySize(String typ) {
        Pattern p = Pattern.compile("\\[.*?\\]");
        Matcher m = p.matcher(typ);
        return m.replaceAll("[]");
    }

    private String getArraySize(String typ) {
        Pattern p = Pattern.compile("\\[.*?\\]");
        Matcher m = p.matcher(typ);
        if (m.find()) {
            String f = m.group();
            return f.substring(1, f.length() - 1);
        } else {
            return "";
        }
    }

    private void fillWidgets()
	{
        boolean isArray;
        String type;
        if (stateVariable != null)
		{
            type = stateVariable.getType();
            stateVariableTypeCombo.setSelectedItem(stripArraySize(type));
                    arraySizeField.setText       (getArraySize(type));
            stateVariableNameField.setText       (stateVariable.getName());
                 initialValueField.setText       (stateVariable.getValue());
                 initialValueField.setToolTipText(stateVariable.getValue());
                  descriptionField.setText       (stateVariable.getDescription());
                  descriptionField.setToolTipText(stateVariable.getDescription());
            isArray = ViskitGlobals.instance().isArray(stateVariable.getType());
        } 
		else 
		{
            type = (String) stateVariableTypeCombo.getSelectedItem();
            arraySizeField.setText("");
            stateVariableNameField.setText(((EventGraphModel) ViskitGlobals.instance().getEventGraphViewFrame().getModel()).generateStateVariableName()); //"state_"+count++);
            initialValueField.setText("");
            initialValueField.setToolTipText("");
            descriptionField.setText(ViskitStatics.DEFAULT_DESCRIPTION);
            descriptionField.setToolTipText(ViskitStatics.DEFAULT_DESCRIPTION);
            isArray = ViskitGlobals.instance().isArray(type);
        }
        toggleArraySizeFields(isArray);
        stateVariableNameField.requestFocus();
        stateVariableNameField.selectAll();
    }

    @Override
    void unloadWidgets()
	{
        // make sure there are no spaces
        String type = (String) stateVariableTypeCombo.getSelectedItem();
        type = ViskitGlobals.instance().typeChosen(type);
        if (ViskitGlobals.instance().isArray(type)) {
            type = type.substring(0, type.indexOf('[') + 1) + arraySizeField.getText().trim() + "]";
        }
        String name = stateVariableNameField.getText();
        name = name.replaceAll("\\s", "");
		String initialValue = initialValueField.getText().trim();
		String  description = descriptionField.getText().trim();

        if (stateVariable != null)
		{
            stateVariable.setName(name);
            stateVariable.setType(type);
            stateVariable.setValue(initialValue);
            stateVariable.setDescription (description);
        }
		else
		{
            newName        = name;
            newType        = type;
			newValue       = initialValue;
            newDescription = description;
        }
    }

    private boolean isGoodArray(String s) {
        s = s.trim();
        int brkIdx = s.indexOf('[');
        if (brkIdx == -1) {
            return true;
        }

        Pattern p = Pattern.compile(".+\\[\\s*\\](^\\[\\]){0}$");     // blah[whitsp]<eol>
        Matcher m = p.matcher(s);
        return m.matches();
    }

    private boolean isGeneric(String typ) {
        return (typ.contains("<K,V>") || typ.contains("<E>"));
    }

    // Little runnables to move the focus around
    private Runnable sizeFieldFocus = new Runnable() {

        @Override
        public void run() {
            arraySizeField.requestFocus();
        }
    };

    private Runnable descriptionFieldFocus = new Runnable() {

        @Override
        public void run() {
            descriptionField.requestFocus();
        }
    };

    class myFocusListener extends FocusAdapter {

        @Override
        public void focusGained(FocusEvent e) {
            handleSelect(e.getComponent());

            if (e.getOppositeComponent() == myTyperComponent) {
                handleArrayFieldEnable();
            }
        }

        /**
         *  Enable the array size field if the type is an array, and set the focus to the right guy.
         */
        private void handleArrayFieldEnable() {
            String s = (String) stateVariableTypeCombo.getEditor().getItem();
            boolean isAr = ViskitGlobals.instance().isArray(s);
            toggleArraySizeFields(isAr);

            // Do this this way to shake out all the pending focus events before twiddling focus.
            if (isAr) {
                SwingUtilities.invokeLater(sizeFieldFocus);
            } else {
                SwingUtilities.invokeLater(descriptionFieldFocus);
            }
        }

        /**
         * select the text in whatever comes in
         * @param c the component containing text
         */
        private void handleSelect(Component c) {
            if (c instanceof ComboBoxEditor) {
                c = ((ComboBoxEditor) c).getEditorComponent();
            }

            if (c instanceof JTextComponent) {
                ((JTextComponent) c).selectAll();
            } else if (c instanceof ComboBoxEditor) {
                ((ComboBoxEditor) c).selectAll();
            }
        }
    }

    class StateVariableApplyButtonListener implements ActionListener
	{
        @Override
        public void actionPerformed(ActionEvent event)
		{
            if (modified) {
                String typeName = ((String) stateVariableTypeCombo.getSelectedItem()).trim();
                String name = stateVariableNameField.getText().trim();
                String arraySize = arraySizeField.getText().trim();

                if ((name.length() <= 0) || (typeName.length() <= 0) ||
                    (ViskitGlobals.instance().isArray(typeName) && (arraySize.length() <= 0))) 
				{
                    ViskitGlobals.instance().getEventGraphController().messageToUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Data entry error",
                            "Name, type and (if array) array size must be entered."); // TODO better message
                    toggleArraySizeFields(true);
                    arraySizeField.requestFocus();
                    return;
                } 
				else if (ViskitGlobals.instance().isArray(typeName) && !isGoodArray(typeName)) 
				{
                    ViskitGlobals.instance().getEventGraphController().messageToUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Data entry error",
                            "Use a single trailing pair of empty square brackets\nto signify a one-dimensional array.");
                    return;
                } 
				else if (isGeneric(typeName)) 
				{
                    ViskitGlobals.instance().getEventGraphController().messageToUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Data entry error",
                            "Actual Keys, Values or Element types must replace " +
                            "the K,V or E between the <> for Collection Objects.");
                    return;
                }

                /* Do a beanshell test for array declaration
                 * isPrimitive returns false for arrays
                 */
                if (!ViskitGlobals.instance().isPrimitive(typeName) && ViskitGlobals.instance().isArray(typeName)) {

                    String s = typeName + " " + name + " = new " + typeName;
                    s = s.substring(0, s.lastIndexOf('[') + 1) + arraySize + "]";          // stick in size

                    if (ViskitConfiguration.instance().getValue(ViskitConfiguration.BEANSHELL_WARNING).equalsIgnoreCase("true")) {
                        String result = ViskitGlobals.instance().parseCode(null, s);
                        if (result != null) {
                            boolean ret = BeanshellErrorDialog.showDialog(result, StateVariableDialog.this);
                            if (!ret) // don't ignore
                            {
                                return;
                            }
                        }
                    }
                }
                // ok, we passed
                unloadWidgets();
            }
            dispose();
        }
    }
}
