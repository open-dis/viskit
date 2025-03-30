package viskit.view.dialog;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import viskit.ViskitGlobals;
import viskit.ViskitUserConfiguration;
import viskit.control.EventGraphController;
import viskit.model.Model;
import viskit.model.ViskitStateVariable;
import static viskit.ViskitStatics.DESCRIPTION_HINT;

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
    static final Logger LOG = LogManager.getLogger();

    private final JTextField stateVariableNameField;    // Text field that holds the parameter name
    private final JTextField descriptionField;          // Text field that holds the comment
    private final JTextField arraySizeField;
    private final JComboBox stateVariableTypeCombo;    // Editable combo box that lets us select a type
    private final JLabel arraySizeLabel;
    private ViskitStateVariable stateVariable;
    private final JButton okButton;
    private final JButton cancelButton;
    public static String newName,  newType,  newComment;
    private final MyFocusListener focusListener;
    private final Component myTypeComponent;       // i.e., the editor of the type JComboBox

    public static boolean showDialog(JFrame f, ViskitStateVariable var) {
        return ViskitSmallDialog.showDialog(StateVariableDialog.class.getName(), f, var);
    }

    protected StateVariableDialog(JFrame parent, Object param) {
        super(parent, "State Variable Declaration", true);

        focusListener = new MyFocusListener();

        Container cont = getContentPane();
        cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));

        JPanel con = new JPanel();
        con.setLayout(new BoxLayout(con, BoxLayout.Y_AXIS));
        con.setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                new EmptyBorder(10, 10, 10, 10)));

        con.add(Box.createVerticalStrut(5));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel("name");
        JLabel initLabel = new JLabel("initial value");
        JLabel typeLabel = new JLabel("type");
        JLabel descriptionLabel = new JLabel("description");
        descriptionLabel.setToolTipText(DESCRIPTION_HINT);
        arraySizeLabel = new JLabel("array size");

        int w = OneLinePanel.maxWidth(new JComponent[] {nameLabel, initLabel, typeLabel, descriptionLabel});

        stateVariableNameField = new JTextField(15);
        stateVariableNameField.addFocusListener(focusListener);
        setMaxHeight(stateVariableNameField);

        descriptionField = new JTextField(25);
        descriptionField.setToolTipText(DESCRIPTION_HINT);
        descriptionField.addFocusListener(focusListener);
        setMaxHeight(descriptionField);

        stateVariableTypeCombo = ViskitGlobals.instance().getViskitTypeComboBox();
        stateVariableTypeCombo.getEditor().getEditorComponent().addFocusListener(focusListener);
        setMaxHeight(stateVariableTypeCombo);

        arraySizeField = new JTextField(15);
        arraySizeField.addFocusListener(focusListener);

        fieldsPanel.add(new OneLinePanel(nameLabel, w, stateVariableNameField));
        fieldsPanel.add(new OneLinePanel(typeLabel, w, stateVariableTypeCombo));
        fieldsPanel.add(new OneLinePanel(arraySizeLabel, w, arraySizeField));
        fieldsPanel.add(new OneLinePanel(descriptionLabel, w, descriptionField));
        con.add(fieldsPanel);
        con.add(Box.createVerticalStrut(5));

        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        cancelButton = new JButton("Cancel");
        okButton = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(okButton);
        buttPan.add(cancelButton);
        con.add(buttPan);
        con.add(Box.createVerticalGlue());    // takes up space when dialog is expanded vertically
        cont.add(con);

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new StateVariableApplyButtonListener());//applyButtonListener());

        enableApplyButtonListener lis = new enableApplyButtonListener(okButton);
        stateVariableNameField.getDocument().addDocumentListener(lis);//addCaretListener(lis);
        descriptionField.getDocument().addDocumentListener(lis);// addCaretListener(lis);
        stateVariableTypeCombo.addActionListener(lis);

        myTypeComponent = stateVariableTypeCombo.getEditor().getEditorComponent();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowClosingListener(this, okButton, cancelButton));

        setParameters(parent, param);
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
    final void setParameters(Component parentComponent, Object newStateVariable) 
    {
        stateVariable = (ViskitStateVariable) newStateVariable;

        fillWidgets();

        modified = (newStateVariable == null);
        okButton.setEnabled(newStateVariable == null);

        if (newStateVariable == null) {
            getRootPane().setDefaultButton(okButton);
        } 
        else {
            getRootPane().setDefaultButton(cancelButton);
        }
        pack();
        setLocationRelativeTo(parentComponent);
    }

    private String stripArraySize(String typeValue) 
    {
        Pattern pattern = Pattern.compile("\\[.*?\\]");
        Matcher matcher = pattern.matcher(typeValue);
        return  matcher.replaceAll("[]");
    }

    private String getArraySize(String typeValue) 
    {
        Pattern pattern = Pattern.compile("\\[.*?\\]");
        Matcher matcher = pattern.matcher(typeValue.trim());
        if (matcher.find()) 
        {
            String foundString = matcher.group();
            return foundString.substring(1, foundString.length() - 1); // strip brackets ()first and last characters
        } 
        else {
            return "";
        }
    }

    private void fillWidgets() 
    {
        boolean isArray;
        String type;
        if (stateVariable != null) 
        {
            stateVariableNameField.setText(stateVariable.getName());
            type = stateVariable.getType();
            stateVariableTypeCombo.setSelectedItem(stripArraySize(type));
            arraySizeField.setText(getArraySize(type));
            descriptionField.setText(stateVariable.getDescription());
            isArray = ViskitGlobals.instance().isArray(stateVariable.getType());
        } 
        else {
            stateVariableNameField.setText(((Model) ViskitGlobals.instance().getEventGraphViewFrame().getModel()).generateStateVariableName()); //"state_"+count++);
            type = (String) stateVariableTypeCombo.getSelectedItem();
            isArray = ViskitGlobals.instance().isArray(type);
            descriptionField.setText("");
            arraySizeField.setText("");
        }
        toggleArraySizeFields(isArray);
        stateVariableNameField.requestFocus();
        stateVariableNameField.selectAll();
    }

    @Override
    void unloadWidgets() 
    {
        String type = (String) stateVariableTypeCombo.getSelectedItem();
        type = ViskitGlobals.instance().typeChosen(type);
        if (ViskitGlobals.instance().isArray(type)) {
            type = type.substring(0, type.indexOf('[') + 1) + arraySizeField.getText().trim() + "]";
        }
        // make sure there are no spaces
        String name = stateVariableNameField.getText();
        name = name.replaceAll("\\s", "");

        if (stateVariable != null) {
            stateVariable.setName(name);
            stateVariable.setType(type);
            stateVariable.setDescription(this.descriptionField.getText().trim());
        } else {
            newName = name;
            newType = type;
            newComment = descriptionField.getText().trim();
        }
    }

    private boolean isGoodArray(String s) 
    {
        s = s.trim();
        int breakIndex = s.indexOf('[');
        if (breakIndex == -1) {
            return true;
        }

        Pattern pattern = Pattern.compile(".+\\[\\s*\\](^\\[\\]){0}$");     // blah[whitsp]<eol>
        Matcher matcher = pattern.matcher(s);
        return matcher.matches();
    }

    private boolean isGenericType(String type) {
        return (type.contains("<K,V>") || type.contains("<E>"));
    }

    class MyFocusListener extends FocusAdapter {

        @Override
        public void focusGained(FocusEvent focusEvent) {
            handleSelect(focusEvent.getComponent());

            if (focusEvent.getOppositeComponent() == myTypeComponent) {
                handleArrayFieldEnable();
            }
        }

        /**
         *  Enable the array size field if the type is an array, and set the focus to the right guy.
         */
        private void handleArrayFieldEnable()
        {
            String s = (String) stateVariableTypeCombo.getEditor().getItem();
            boolean isArray = ViskitGlobals.instance().isArray(s);
            toggleArraySizeFields(isArray);

            // Do this this way to shake out all the pending focus events before twiddling focus.
            if (isArray) {
                arraySizeField.requestFocus();
            } else {
                descriptionField.requestFocus();
            }
        }

        /**
         * select the text in whatever comes in
         * @param component the component containing text
         */
        private void handleSelect(Component component) 
        {
            if (component instanceof ComboBoxEditor) 
            {
                component = ((ComboBoxEditor) component).getEditorComponent();
            }
            else if (component instanceof JTextComponent) {
                ((JTextComponent) component).selectAll();
            } 
            else if (component instanceof ComboBoxEditor) {
                ((ComboBoxEditor) component).selectAll();
            }
        }
    }

    class StateVariableApplyButtonListener implements ActionListener 
    {
        @Override
        public void actionPerformed(ActionEvent event) {
            if (modified) {
                String typ = ((String) stateVariableTypeCombo.getSelectedItem()).trim();
                String nam = stateVariableNameField.getText().trim();
                String arsz = arraySizeField.getText().trim();

                if (nam.length() <= 0 ||
                        typ.length() <= 0 ||
                        (ViskitGlobals.instance().isArray(typ) && arsz.length() <= 0)) {
                    ((EventGraphController)ViskitGlobals.instance().getEventGraphController()).messageUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Data entry error",
                            "Name, type and (if array) array size must be entered.");
                    toggleArraySizeFields(true);
                    arraySizeField.requestFocus();
                    return;
                } else if (ViskitGlobals.instance().isArray(typ) && !isGoodArray(typ)) {
                    ((EventGraphController)ViskitGlobals.instance().getEventGraphController()).messageUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Data entry error",
                            "Use a single trailing pair of empty square brackets\nto signify a one-dimensional array.");
                    return;
                } else if (isGenericType(typ)) {
                    ((EventGraphController)ViskitGlobals.instance().getEventGraphController()).messageUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Data entry error",
                            "Actual Keys, Values or Element types must replace " +
                            "the K,V or E between the <> for Collection Objects.");
                    return;
                }

                /* Do a beanshell test for array declaration
                 * isPrimitive returns false for arrays
                 */
                if (!ViskitGlobals.instance().isPrimitive(typ) && ViskitGlobals.instance().isArray(typ)) {

                    String s = typ + " " + nam + " = new " + typ;
                    s = s.substring(0, s.lastIndexOf('[') + 1) + arsz + "]";          // stick in size

                    if (ViskitUserConfiguration.instance().getValue(ViskitUserConfiguration.BEANSHELL_WARNING_KEY).equalsIgnoreCase("true")) {
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
