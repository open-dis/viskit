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

import viskit.ViskitGlobals;
import viskit.ViskitConfigurationStore;
import viskit.control.EventGraphController;
import viskit.model.Model;
import viskit.model.ViskitStateVariable;
import static viskit.view.EventGraphViewFrame.DESCRIPTION_HINT;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @version $Id$
 */
public class StateVariableDialog extends ViskitSmallDialog {

    private final JTextField stateVarNameField;    // Text field that holds the parameter name
    private final JTextField descriptionField;          // Text field that holds the comment
    private final JTextField arraySizeField;
    private final JComboBox stateVarTypeCombo;    // Editable combo box that lets us select a type
    private final JLabel arraySizeLabel;
    private ViskitStateVariable stVar;
    private final JButton okButton;
    private final JButton canButt;
    public static String newName,  newType,  newComment;
    private final myFocusListener focList;
    private final Component myTyperComponent;       // i.e., the editor of the type JComboBox

    public static boolean showDialog(JFrame f, ViskitStateVariable var) {
        return ViskitSmallDialog.showDialog(StateVariableDialog.class.getName(), f, var);
    }

    protected StateVariableDialog(JFrame parent, Object param) {
        super(parent, "State Variable Declaration", true);

        focList = new myFocusListener();

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

        stateVarNameField = new JTextField(15);
        stateVarNameField.addFocusListener(focList);
        setMaxHeight(stateVarNameField);

        descriptionField = new JTextField(25);
        descriptionField.setToolTipText(DESCRIPTION_HINT);
        descriptionField.addFocusListener(focList);
        setMaxHeight(descriptionField);

        stateVarTypeCombo = ViskitGlobals.instance().getTypeCB();
        stateVarTypeCombo.getEditor().getEditorComponent().addFocusListener(focList);
        setMaxHeight(stateVarTypeCombo);

        arraySizeField = new JTextField(15);
        arraySizeField.addFocusListener(focList);

        fieldsPanel.add(new OneLinePanel(nameLabel, w, stateVarNameField));
        fieldsPanel.add(new OneLinePanel(typeLabel, w, stateVarTypeCombo));
        fieldsPanel.add(new OneLinePanel(arraySizeLabel, w, arraySizeField));
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
        okButton.addActionListener(new StateVarApplyButtonListener());//applyButtonListener());

        enableApplyButtonListener lis = new enableApplyButtonListener(okButton);
        stateVarNameField.getDocument().addDocumentListener(lis);//addCaretListener(lis);
        descriptionField.getDocument().addDocumentListener(lis);// addCaretListener(lis);
        stateVarTypeCombo.addActionListener(lis);

        myTyperComponent = stateVarTypeCombo.getEditor().getEditorComponent();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowClosingListener(this, okButton, canButt));

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
    final void setParams(Component c, Object p) {
        stVar = (ViskitStateVariable) p;

        fillWidgets();

        modified = (p == null);
        okButton.setEnabled(p == null);

        if (p == null) {
            getRootPane().setDefaultButton(okButton);
        } else {
            getRootPane().setDefaultButton(canButt);
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
        String ty;
        if (stVar != null) {
            stateVarNameField.setText(stVar.getName());
            ty = stVar.getType();
            stateVarTypeCombo.setSelectedItem(stripArraySize(ty));
            arraySizeField.setText(getArraySize(ty));
            descriptionField.setText(stVar.getComment());
            isArray = ViskitGlobals.instance().isArray(stVar.getType());
        } else {
            stateVarNameField.setText(((Model) ViskitGlobals.instance().getEventGraphViewFrame().getModel()).generateStateVariableName()); //"state_"+count++);
            ty = (String) stateVarTypeCombo.getSelectedItem();
            isArray = ViskitGlobals.instance().isArray(ty);
            descriptionField.setText("");
            arraySizeField.setText("");
        }
        toggleArraySizeFields(isArray);
        stateVarNameField.requestFocus();
        stateVarNameField.selectAll();
    }

    @Override
    void unloadWidgets() {
        // make sure there are no spaces
        String ty = (String) stateVarTypeCombo.getSelectedItem();
        ty = ViskitGlobals.instance().typeChosen(ty);
        if (ViskitGlobals.instance().isArray(ty)) {
            ty = ty.substring(0, ty.indexOf('[') + 1) + arraySizeField.getText().trim() + "]";
        }
        String nm = stateVarNameField.getText();
        nm = nm.replaceAll("\\s", "");

        if (stVar != null) {
            stVar.setName(nm);
            stVar.setType(ty);
            stVar.setComment(this.descriptionField.getText().trim());
        } else {
            newName = nm;
            newType = ty;
            newComment = descriptionField.getText().trim();
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
            String s = (String) stateVarTypeCombo.getEditor().getItem();
            boolean isAr = ViskitGlobals.instance().isArray(s);
            toggleArraySizeFields(isAr);

            // Do this this way to shake out all the pending focus events before twiddling focus.
            if (isAr) {
                arraySizeField.requestFocus();
            } else {
                descriptionField.requestFocus();
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
                String typ = ((String) stateVarTypeCombo.getSelectedItem()).trim();
                String nam = stateVarNameField.getText().trim();
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
                } else if (isGeneric(typ)) {
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

                    if (ViskitConfigurationStore.instance().getVal(ViskitConfigurationStore.BEANSHELL_WARNING_KEY).equalsIgnoreCase("true")) {
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
