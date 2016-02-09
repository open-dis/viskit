package viskit.view.dialog;

import viskit.model.EventGraphModel;
import viskit.model.vStateVariable;

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
import viskit.control.EventGraphControllerImpl;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @version $Id$
 */
public class StateVariableDialog extends ViskitSmallDialog {

    private final JTextField stateVariableNameField;    // Text field that holds the parameter name
    private JTextField descriptionField;          // Text field that holds the description
    private JTextField arraySizeField;
    private final JComboBox<String> stateVariableTypeCombo;    // Editable combo box that lets us select a type
    private final JLabel arrSizeLab;
    private vStateVariable stateVariable;
    private final JButton okButton,  cancelButton;
    public static String newName,  newType,  newDescription;
    private final myFocusListener focusListener;
    private final Component myTyperComponent;       // i.e., the editor of the type JComboBox

    public static boolean showDialog(JFrame f, vStateVariable var) {
        return ViskitSmallDialog.showDialog(StateVariableDialog.class.getName(), f, var);
    }

    protected StateVariableDialog(JFrame parent, Object param) {
        super(parent, "State Variable Declaration Inspector", true);

        focusListener = new myFocusListener();

        Container cont = getContentPane();
        cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));

        JPanel con = new JPanel();
        con.setLayout(new BoxLayout(con, BoxLayout.Y_AXIS));
        con.setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                new EmptyBorder(10, 10, 10, 10)));

        con.add(Box.createVerticalStrut(5));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));

        JLabel nameLab = new JLabel("name");
        JLabel initLab = new JLabel("initial value");
        JLabel typeLab = new JLabel("type");
        JLabel commLab = new JLabel("description");
        arrSizeLab = new JLabel("array size");

        int w = OneLinePanel.maxWidth(new JComponent[] {nameLab, initLab, typeLab, commLab});

        stateVariableNameField = new JTextField(15);
        stateVariableNameField.addFocusListener(focusListener);
        setMaxHeight(stateVariableNameField);

        descriptionField = new JTextField(25);
        descriptionField.addFocusListener(focusListener);
        setMaxHeight(descriptionField);

        stateVariableTypeCombo = ViskitGlobals.instance().getTypeComboBox();
        stateVariableTypeCombo.getEditor().getEditorComponent().addFocusListener(focusListener);
        setMaxHeight(stateVariableTypeCombo);

        arraySizeField = new JTextField(15);
        arraySizeField.addFocusListener(focusListener);

        fieldsPanel.add(new OneLinePanel(nameLab, w, stateVariableNameField));
        fieldsPanel.add(new OneLinePanel(typeLab, w, stateVariableTypeCombo));
        fieldsPanel.add(new OneLinePanel(arrSizeLab, w, arraySizeField));
        fieldsPanel.add(new OneLinePanel(commLab, w, descriptionField));
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
        okButton.addActionListener(new StateVarApplyButtonListener());//applyButtonListener());

        enableApplyButtonListener lis = new enableApplyButtonListener(okButton);
        stateVariableNameField.getDocument().addDocumentListener(lis);//addCaretListener(lis);
        descriptionField.getDocument().addDocumentListener(lis);// addCaretListener(lis);
        stateVariableTypeCombo.addActionListener(lis);

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
        arrSizeLab.setEnabled(b);
        arraySizeField.setEnabled(b);
        arraySizeField.setEditable(b);     // grays background if false
    }

    @Override
    final void setParams(Component c, Object p) {
        stateVariable = (vStateVariable) p;

        fillWidgets();

        modified = (p == null);
        okButton.setEnabled(p == null);

        if (p == null) {
            getRootPane().setDefaultButton(okButton);
        } else {
            getRootPane().setDefaultButton(cancelButton);
        }
        pack();
        setLocationRelativeTo(c);
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

    private void fillWidgets() {
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
		else 
		{
            stateVariableNameField.setText(((EventGraphModel) ViskitGlobals.instance().getEventGraphEditor().getModel()).generateStateVariableName()); //"state_"+count++);
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
    void unloadWidgets() {
        // make sure there are no spaces
        String type = (String) stateVariableTypeCombo.getSelectedItem();
        type = ViskitGlobals.instance().typeChosen(type);
        if (ViskitGlobals.instance().isArray(type)) {
            type = type.substring(0, type.indexOf('[') + 1) + arraySizeField.getText().trim() + "]";
        }
        String name = stateVariableNameField.getText();
        name = name.replaceAll("\\s", "");

        if (stateVariable != null)
		{
            stateVariable.setName(name);
            stateVariable.setType(type);
            stateVariable.setDescription(this.descriptionField.getText().trim());
        }
		else
		{
            newName = name;
            newType = type;
            newDescription = descriptionField.getText().trim();
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

    class StateVarApplyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (modified) {
                String typ = ((String) stateVariableTypeCombo.getSelectedItem()).trim();
                String nam = stateVariableNameField.getText().trim();
                String arsz = arraySizeField.getText().trim();

                if (nam.length() <= 0 ||
                        typ.length() <= 0 ||
                        (ViskitGlobals.instance().isArray(typ) && arsz.length() <= 0)) {
                    ((EventGraphControllerImpl)ViskitGlobals.instance().getEventGraphController()).messageToUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Data entry error",
                            "Name, type and (if array) array size must be entered.");
                    toggleArraySizeFields(true);
                    arraySizeField.requestFocus();
                    return;
                } else if (ViskitGlobals.instance().isArray(typ) && !isGoodArray(typ)) {
                    ((EventGraphControllerImpl)ViskitGlobals.instance().getEventGraphController()).messageToUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Data entry error",
                            "Use a single trailing pair of empty square brackets\nto signify a one-dimensional array.");
                    return;
                } else if (isGeneric(typ)) {
                    ((EventGraphControllerImpl)ViskitGlobals.instance().getEventGraphController()).messageToUser(
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
