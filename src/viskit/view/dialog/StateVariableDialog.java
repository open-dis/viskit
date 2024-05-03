package viskit.view.dialog;

import viskit.model.Model;
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
import viskit.VGlobals;
import viskit.ViskitConfig;
import viskit.control.EventGraphController;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @version $Id$
 */
public class StateVariableDialog extends ViskitSmallDialog {

    private JTextField stateVarNameField;    // Text field that holds the parameter name
    private JTextField commentField;          // Text field that holds the comment
    private JTextField arraySizeField;
    private JComboBox stateVarTypeCombo;    // Editable combo box that lets us select a type
    private JLabel arrSizeLab;
    private vStateVariable stVar;
    private JButton okButt, canButt;
    public static String newName,  newType,  newComment;
    private myFocusListener focList;
    private Component myTyperComponent;       // i.e., the editor of the type JComboBox

    public static boolean showDialog(JFrame f, vStateVariable var) {
        return ViskitSmallDialog.showDialog(StateVariableDialog.class.getName(), f, var);
    }

    protected StateVariableDialog(JFrame parent, Object param) {
        super(parent, "State Variable Declaration Inspector", true);

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

        JLabel nameLab = new JLabel("name");
        JLabel initLab = new JLabel("initial value");
        JLabel typeLab = new JLabel("type");
        JLabel commLab = new JLabel("description");
        arrSizeLab = new JLabel("array size");

        int w = OneLinePanel.maxWidth(new JComponent[] {nameLab, initLab, typeLab, commLab});

        stateVarNameField = new JTextField(15);
        stateVarNameField.addFocusListener(focList);
        setMaxHeight(stateVarNameField);

        commentField = new JTextField(25);
        commentField.addFocusListener(focList);
        setMaxHeight(commentField);

        stateVarTypeCombo = VGlobals.instance().getTypeCB();
        stateVarTypeCombo.getEditor().getEditorComponent().addFocusListener(focList);
        setMaxHeight(stateVarTypeCombo);

        arraySizeField = new JTextField(15);
        arraySizeField.addFocusListener(focList);

        fieldsPanel.add(new OneLinePanel(nameLab, w, stateVarNameField));
        fieldsPanel.add(new OneLinePanel(typeLab, w, stateVarTypeCombo));
        fieldsPanel.add(new OneLinePanel(arrSizeLab, w, arraySizeField));
        fieldsPanel.add(new OneLinePanel(commLab, w, commentField));
        con.add(fieldsPanel);
        con.add(Box.createVerticalStrut(5));

        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        canButt = new JButton("Cancel");
        okButt = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(canButt);
        buttPan.add(okButt);
        con.add(buttPan);
        con.add(Box.createVerticalGlue());    // takes up space when dialog is expanded vertically
        cont.add(con);

        // attach listeners
        canButt.addActionListener(new cancelButtonListener());
        okButt.addActionListener(new StateVarApplyButtonListener());//applyButtonListener());

        enableApplyButtonListener lis = new enableApplyButtonListener(okButt);
        stateVarNameField.getDocument().addDocumentListener(lis);//addCaretListener(lis);
        commentField.getDocument().addDocumentListener(lis);// addCaretListener(lis);
        stateVarTypeCombo.addActionListener(lis);

        myTyperComponent = stateVarTypeCombo.getEditor().getEditorComponent();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowClosingListener(this, okButt, canButt));

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
        stVar = (vStateVariable) p;

        fillWidgets();

        modified = (p == null);
        okButt.setEnabled(p == null);

        if (p == null) {
            getRootPane().setDefaultButton(okButt);
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
            commentField.setText(stVar.getComment());
            isArray = VGlobals.instance().isArray(stVar.getType());
        } else {
            stateVarNameField.setText(((Model) VGlobals.instance().getEventGraphEditor().getModel()).generateStateVariableName()); //"state_"+count++);
            ty = (String) stateVarTypeCombo.getSelectedItem();
            isArray = VGlobals.instance().isArray(ty);
            commentField.setText("");
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
        ty = VGlobals.instance().typeChosen(ty);
        if (VGlobals.instance().isArray(ty)) {
            ty = ty.substring(0, ty.indexOf('[') + 1) + arraySizeField.getText().trim() + "]";
        }
        String nm = stateVarNameField.getText();
        nm = nm.replaceAll("\\s", "");

        if (stVar != null) {
            stVar.setName(nm);
            stVar.setType(ty);
            stVar.setComment(this.commentField.getText().trim());
        } else {
            newName = nm;
            newType = ty;
            newComment = commentField.getText().trim();
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
            boolean isAr = VGlobals.instance().isArray(s);
            toggleArraySizeFields(isAr);

            // Do this this way to shake out all the pending focus events before twiddling focus.
            if (isAr) {
                arraySizeField.requestFocus();
            } else {
                commentField.requestFocus();
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
                        (VGlobals.instance().isArray(typ) && arsz.length() <= 0)) {
                    ((EventGraphController)VGlobals.instance().getEventGraphController()).messageUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Data entry error",
                            "Name, type and (if array) array size must be entered.");
                    toggleArraySizeFields(true);
                    arraySizeField.requestFocus();
                    return;
                } else if (VGlobals.instance().isArray(typ) && !isGoodArray(typ)) {
                    ((EventGraphController)VGlobals.instance().getEventGraphController()).messageUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Data entry error",
                            "Use a single trailing pair of empty square brackets\nto signify a one-dimensional array.");
                    return;
                } else if (isGeneric(typ)) {
                    ((EventGraphController)VGlobals.instance().getEventGraphController()).messageUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Data entry error",
                            "Actual Keys, Values or Element types must replace " +
                            "the K,V or E between the <> for Collection Objects.");
                    return;
                }

                /* Do a beanshell test for array declaration
                 * isPrimitive returns false for arrays
                 */
                if (!VGlobals.instance().isPrimitive(typ) && VGlobals.instance().isArray(typ)) {

                    String s = typ + " " + nam + " = new " + typ;
                    s = s.substring(0, s.lastIndexOf('[') + 1) + arsz + "]";          // stick in size

                    if (ViskitConfig.instance().getVal(ViskitConfig.BEANSHELL_WARNING).equalsIgnoreCase("true")) {
                        String result = VGlobals.instance().parseCode(null, s);
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
