package viskit.view.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import viskit.VGlobals;
import viskit.VStatics;
import viskit.control.EventGraphControllerImpl;

import viskit.model.EventStateTransition;
import viskit.model.ViskitElement;
import viskit.model.vStateVariable;
import viskit.view.ArgumentsPanel;
import viskit.view.LocalVariablesPanel;

/**
 * A dialog class that lets the user add a new parameter to the document.
 * After the user clicks "OK", "Cancel", or closes the dialog via the
 * close box, the caller retrieves the "buttonChosen" variable from
 * the object to determine the choice. If the user clicked "OK", the
 * caller can retrieve various choices from the object.
 *
 * @author DMcG, Mike Bailey
 * @version $Id$
 */
public class EventTransitionDialog extends JDialog {

    private JTextField actionField, arrayIndexField, localAssignmentField, descriptionField;
    private JComboBox<ViskitElement> stateVarsCB;
    private JComboBox<String> localVarsCB;
    private JRadioButton assTo, opOn, assignMethod;
    private static EventTransitionDialog dialog;
    private static boolean modified = false;
    private EventStateTransition param;
    private JButton okButt, canButt;
    private JButton newSVButt;
    private JLabel actionLab;
    private JPanel indexPanel, localMethodCallPanel;

    /** Required to get the EventArgument for indexing a State Variable array */
    private ArgumentsPanel argPanel;

    /** Required to get the type of any declared local variables */
    private LocalVariablesPanel localVariablesPanel;

    public static boolean showDialog(JFrame f, EventStateTransition est, ArgumentsPanel ap, LocalVariablesPanel lvp) {
        if (dialog == null) {
            dialog = new EventTransitionDialog(f, est, ap, lvp);
        } else {
            dialog.setParams(f, est);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private EventTransitionDialog(JFrame parent, EventStateTransition param, ArgumentsPanel ap, LocalVariablesPanel lvp) {

        super(parent, "State Transition", true);
        argPanel = ap;
        localVariablesPanel = lvp;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        JPanel con = new JPanel();
        setContentPane(con);
        con.setLayout(new BoxLayout(con, BoxLayout.Y_AXIS));
        con.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); //createEtchedBorder(EtchedBorder.RAISED));

        con.add(Box.createVerticalStrut(5));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.PAGE_AXIS));

        JLabel commLab = new JLabel("description");
        JLabel localVarAssignLab = new JLabel("local assignment");
        JLabel nameLab = new JLabel("state variable");
        JLabel arrayIdxLab = new JLabel("index variable");

        assTo = new JRadioButton("assign to (\"=\")");
        opOn = new JRadioButton("invoke on (\".\")");
        ButtonGroup bg = new ButtonGroup();
        bg.add(assTo);
        bg.add(opOn);

        actionLab = new JLabel("invoke");
        Dimension dx = actionLab.getPreferredSize();
        actionLab.setText("");
        actionLab.setPreferredSize(dx);
        actionLab.setHorizontalAlignment(JLabel.TRAILING);

        JLabel localMethodLab = new JLabel("invoke local var method");
        JLabel equalsLab = new JLabel("=");

        assignMethod = new JRadioButton("assign method");

        int w = maxWidth(new JComponent[]{commLab, localVarAssignLab, nameLab,
            arrayIdxLab, assTo, opOn, actionLab, assignMethod, localMethodLab});

        stateVarsCB = new JComboBox<>();
        setMaxHeight(stateVarsCB);
        stateVarsCB.setBackground(Color.white);
        newSVButt = new JButton("new");
        descriptionField = new JTextField(25);
        setMaxHeight(descriptionField);
        actionField = new JTextField(25);
        setMaxHeight(actionField);
        arrayIndexField = new JTextField(25);
        setMaxHeight(arrayIndexField);
        localAssignmentField = new JTextField(25);
        localAssignmentField.setToolTipText("Use this field to optionally "
                + "assign a return type for an invoke on \".\" to an already "
                + "declared local variable, or, declare a new one here");
        setMaxHeight(localAssignmentField);

        fieldsPanel.add(new OneLinePanel(commLab, w, descriptionField));
        fieldsPanel.add(Box.createVerticalStrut(10));
        fieldsPanel.add(new OneLinePanel(localVarAssignLab, w, localAssignmentField, equalsLab));

        JSeparator divider = new JSeparator(JSeparator.HORIZONTAL);
        divider.setBackground(Color.blue.brighter());

        fieldsPanel.add(divider);
        fieldsPanel.add(Box.createVerticalStrut(10));
        fieldsPanel.add(new OneLinePanel(nameLab, w, stateVarsCB, newSVButt));
        fieldsPanel.add(indexPanel = new OneLinePanel(arrayIdxLab, w, arrayIndexField));
        fieldsPanel.add(new OneLinePanel(null, w, assTo));
        fieldsPanel.add(new OneLinePanel(null, w, opOn));
        fieldsPanel.add(new OneLinePanel(actionLab, w, actionField));

        divider = new JSeparator(JSeparator.HORIZONTAL);
        divider.setBackground(Color.blue.brighter());

        fieldsPanel.add(divider);
        fieldsPanel.add(Box.createVerticalStrut(10));

        localVarsCB = new JComboBox<>();
        localVarsCB.setToolTipText("Use this to call void return type methods on locally declared variables");
        setMaxHeight(localVarsCB);
        localVarsCB.setBackground(Color.white);

        fieldsPanel.add(new OneLinePanel(null, w, assignMethod));
        fieldsPanel.add(localMethodCallPanel = new OneLinePanel(localMethodLab, w, localVarsCB));

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

        // attach listeners
        canButt.addActionListener(new cancelButtonListener());
        okButt.addActionListener(new applyButtonListener());

        enableApplyButtonListener lis = new enableApplyButtonListener();
        arrayIndexField.addCaretListener(lis);
        actionField.addCaretListener(lis);
        descriptionField.addCaretListener(lis);
        localAssignmentField.addCaretListener(lis);
        stateVarsCB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                vStateVariable sv = (vStateVariable) cb.getSelectedItem();
                descriptionField.setText(sv.getComment());
                okButt.setEnabled(true);
                indexPanel.setVisible(VGlobals.instance().isArray(sv.getType()));
                modified = true;
                pack();
            }
        });
        newSVButt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String nm = VGlobals.instance().getEventGraphEditor().addStateVariableDialog();
                if (nm != null) {
                    stateVarsCB.setModel(VGlobals.instance().getStateVarsCBModel());
                    for (int i = 0; i < stateVarsCB.getItemCount(); i++) {
                        vStateVariable vsv = (vStateVariable) stateVarsCB.getItemAt(i);
                        if (vsv.getName().contains(nm)) {
                            stateVarsCB.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }
        });
        localVarsCB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
//                JComboBox cb = (JComboBox) e.getSource();
//                String lv = (String) cb.getSelectedItem();
                okButt.setEnabled(true);
                modified = true;
                pack();
            }
        });

        // to start off:
        if (stateVarsCB.getItemCount() > 0) {
            descriptionField.setText(((vStateVariable) stateVarsCB.getItemAt(0)).getComment());
        } else {
            descriptionField.setText("");
        }

        RadButtListener rbl = new RadButtListener();
        assTo.addActionListener(rbl);
        opOn.addActionListener(rbl);
        assignMethod.addActionListener(rbl);

        setParams(parent, param);
    }

    private int maxWidth(JComponent[] c) {
        int tmpw, maxw = 0;
        for (JComponent jc : c) {
            tmpw = jc.getPreferredSize().width;
            if (tmpw > maxw) {
                maxw = tmpw;
            }
        }
        return maxw;
    }

    private void setMaxHeight(JComponent c) {
        Dimension d = c.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        c.setMaximumSize(d);
    }

    private ComboBoxModel<String> resolveLocalMethodCalls() {
        Class<?> type;
        Method[] methods;
        ComboBoxModel<String> methodCB = new DefaultComboBoxModel<>();
        java.util.List<ViskitElement> types = localVariablesPanel.getData();

        // Enable argument type methods to be invokes as well
        if (types.isEmpty()) {
            types = argPanel.getData();
        }
        
        for (ViskitElement e : types) {
            type = VStatics.classForName(e.getType());
            methods = type.getMethods();

            // Filter out methods of Object, non-void return types and any
            // methods requiring parameters
            for (Method method : methods) {
                if (method.toString().contains("java.lang.Object")) {continue;}
                if (!method.toString().contains("void")) {continue;}
                if (method.getParameterCount() > 0) {continue;}
                ((DefaultComboBoxModel<String>)methodCB).addElement(method.getName());
            }
        }
        return methodCB;
    }

    public final void setParams(Component c, EventStateTransition p) {
        param = p;

        fillWidgets();

        modified = (p == null);
        okButt.setEnabled(p == null);

        getRootPane().setDefaultButton(canButt);
        pack(); // do this prior to next
        setLocationRelativeTo(c);
    }

    // bugfix 1183
    private void fillWidgets() {
        String indexArg = "";

        // Conceptually, should only be one indexing argument
        if (!argPanel.isEmpty()) {
            for (ViskitElement ia : argPanel.getData()) {
                indexArg = ia.getName();
                break;
            }
        }
        if (param != null) {
            if (stateVarsCB.getItemCount() > 0) {
                descriptionField.setText(((vStateVariable) stateVarsCB.getSelectedItem()).getComment());
            } else {
                descriptionField.setText("");
            }
            actionField.setText(param.getOperationOrAssignment());
            String ie = param.getIndexingExpression();
            if (ie == null || ie.isEmpty()) {
                arrayIndexField.setText(indexArg);
            } else {
                arrayIndexField.setText(ie);
            }
            boolean isOp = param.isOperation();
            if (isOp) {
                opOn.setSelected(isOp);
                actionLab.setText(".");
            } else {
                assTo.setSelected(!isOp);
                actionLab.setText("=");
            }
            setStateVariableComboBox(param);
            localAssignmentField.setText(param.getLocalVariableAssignment());
            setLocalVariableComboBox(param);
        } else {
            descriptionField.setText("");
            actionField.setText("");
            arrayIndexField.setText(indexArg);
            stateVarsCB.setSelectedIndex(0);
            localAssignmentField.setText("");
            assTo.setSelected(true);
            localVarsCB.setSelectedIndex(0);
        }

        // We have an indexing argument already set
        String typ = ((vStateVariable) stateVarsCB.getSelectedItem()).getType();
        indexPanel.setVisible(VGlobals.instance().isArray(typ));
    }

    private void setStateVariableComboBox(EventStateTransition est) {
        stateVarsCB.setModel(VGlobals.instance().getStateVarsCBModel());
        stateVarsCB.setSelectedIndex(0);
        for (int i = 0; i < stateVarsCB.getItemCount(); i++) {
            vStateVariable sv = (vStateVariable) stateVarsCB.getItemAt(i);
            if (est.getStateVarName().equalsIgnoreCase(sv.getName())) {
                stateVarsCB.setSelectedIndex(i);
                return;
            }
        }

        if (!est.getStateVarName().equals("")) // for first time
        {
            ((EventGraphControllerImpl)VGlobals.instance().getEventGraphController()).messageUser(
                    JOptionPane.ERROR_MESSAGE,
                    "Alert",
                    "State variable " + est.getStateVarName() + "not found.");
        }
    }

    private void setLocalVariableComboBox(EventStateTransition est) {
        localVarsCB.setModel(resolveLocalMethodCalls());

        String lvmc = param.getLocalVariableMethodCall();
        if (lvmc != null) {
            assignMethod.setSelected(!lvmc.isEmpty());
        }
        localMethodCallPanel.setVisible(assignMethod.isSelected() && !localAssignmentField.getText().isEmpty());

        // Check for any local variables first
        if (localVarsCB.getItemCount() <= 0) {return;}
        localVarsCB.setSelectedIndex(0);
        String lVMethodCall = est.getLocalVariableMethodCall();
        for (int i = 0; i < localVarsCB.getItemCount(); i++) {
            String call = localVarsCB.getItemAt(i);
            if (lVMethodCall != null && lVMethodCall.contains(call)) {
                localVarsCB.setSelectedIndex(i);
                break;
            }
        }
    }

    private void unloadWidgets() {
        if (param != null) {
            param.getComments().clear();
            String cs = descriptionField.getText().trim();
            if (!cs.isEmpty()) {
                param.getComments().add(0, cs);
            }
            param.setLocalVariableAssignment(localAssignmentField.getText().trim());
            param.setStateVarName(((vStateVariable) stateVarsCB.getSelectedItem()).getName());
            param.setStateVarType(((vStateVariable) stateVarsCB.getSelectedItem()).getType());
            param.setIndexingExpression(arrayIndexField.getText().trim());
            param.setOperationOrAssignment(actionField.getText().trim());
            param.setOperation(opOn.isSelected());
            param.setLocalVariableMethodCall((String) localVarsCB.getSelectedItem());
            assignMethod.setSelected(param.getLocalVariableMethodCall() != null);
        }
    }

    class cancelButtonListener implements ActionListener {

        // bugfix 1183
        @Override
        public void actionPerformed(ActionEvent event) {
            modified = false;    // for the caller
            localAssignmentField.setText("");
            actionField.setText("");
            VGlobals.instance().getActiveEventGraphModel().resetIdxNameGenerator();
            assignMethod.setSelected(false);
            dispose();
        }
    }

    class RadButtListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            modified = true;
            okButt.setEnabled(true);

            Dimension d = actionLab.getPreferredSize();
            if (assTo.isSelected()) {
                actionLab.setText("=");
            } else if (opOn.isSelected()) {
                String ty = ((vStateVariable) stateVarsCB.getSelectedItem()).getType();
                if (VGlobals.instance().isPrimitive(ty)) {
                    ((EventGraphControllerImpl)VGlobals.instance().getEventGraphController()).messageUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Java Language Error",
                            "A method may not be invoked on a primitive type.");
                    assTo.setSelected(true);
                } else {
                    actionLab.setText(".");
                }
            }
            localMethodCallPanel.setVisible(assignMethod.isSelected() && !localAssignmentField.getText().isEmpty());
            actionLab.setPreferredSize(d);
            pack();
        }
    }

    class applyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (modified) {
                // check for array index
                String typ = ((vStateVariable) stateVarsCB.getSelectedItem()).getType();
                if (VGlobals.instance().isArray(typ)) {
                    if (arrayIndexField.getText().trim().isEmpty()) {
                        int ret = JOptionPane.showConfirmDialog(
                                EventTransitionDialog.this,
                                "Using a state variable which is an array" +
                                "\nrequires an indexing expression.\nIgnore and continue?",
                                "Warning",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        if (ret != JOptionPane.YES_OPTION) {
                            return;
                        }
                    }
                }
                // check for null action
                if (actionField.getText().trim().isEmpty()) {
                    int ret = JOptionPane.showConfirmDialog(EventTransitionDialog.this, "No transition (action) has been entered." +
                            "\nIgnore and continue?",
                            "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (ret != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                unloadWidgets();
            }
            dispose();
        }
    }

    class enableApplyButtonListener implements CaretListener, ActionListener {

        @Override
        public void caretUpdate(CaretEvent event) {
            modified = true;
            okButt.setEnabled(true);
            getRootPane().setDefaultButton(okButt);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            caretUpdate(null);
        }
    }

    class OneLinePanel extends JPanel {

        OneLinePanel(JLabel lab, int w, JComponent comp) {
            this(lab,w,comp,null);
        }

        OneLinePanel(JLabel lab, int w, JComponent comp1, JComponent comp2) {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            if (lab == null) {
                lab = new JLabel("");
            }
            add(Box.createHorizontalStrut(5));
            add(Box.createHorizontalStrut(w - lab.getPreferredSize().width));
            add(lab);
            add(Box.createHorizontalStrut(5));
            add(comp1);

            if (comp2 != null)
                add(comp2);

            Dimension d = getPreferredSize();
            d.width = Integer.MAX_VALUE;
            setMaximumSize(d);
        }
    }

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int ret = JOptionPane.showConfirmDialog(EventTransitionDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
                    okButt.doClick();
                } else {
                    canButt.doClick();
                }
            } else {
                canButt.doClick();
            }
        }
    }
}
