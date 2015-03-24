package viskit.view.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;
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
public class EventStateTransitionDialog extends JDialog {

    private JTextField actionField, arrayIndexField, localAssignmentField, localInvocationField, descriptionField;
    private JComboBox<ViskitElement> stateVarsCB;
    private JComboBox<String> stateTranMethodsCB, localVarMethodsCB;
    private JRadioButton assTo, opOn;
    private static EventStateTransitionDialog dialog;
    private static boolean modified = false;
    private EventStateTransition param;
    private JButton okButt, canButt;
    private JButton newSVButt;
    private JLabel actionLab1, actionLab2, localInvokeDot;
    private JPanel localAssignmentPanel, indexPanel, stateTransInvokePanel, localInvocationPanel;

    /** Required to get the EventArgument for indexing a State Variable array */
    private ArgumentsPanel argPanel;

    /** Required to get the type of any declared local variables */
    private LocalVariablesPanel localVariablesPanel;

    public static boolean showDialog(JFrame f, EventStateTransition est, ArgumentsPanel ap, LocalVariablesPanel lvp) {
        if (dialog == null) {
            dialog = new EventStateTransitionDialog(f, est, ap, lvp);
        } else {
            dialog.setParams(f, est);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private EventStateTransitionDialog(JFrame parent, EventStateTransition param, ArgumentsPanel ap, LocalVariablesPanel lvp) {

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
        JLabel localAssignLab = new JLabel("local assignment");
        JLabel nameLab = new JLabel("state variable");
        JLabel arrayIdxLab = new JLabel("index variable");

        assTo = new JRadioButton("assign to (\"=\")");
        opOn = new JRadioButton("invoke on (\".\")");
        ButtonGroup bg = new ButtonGroup();
        bg.add(assTo);
        bg.add(opOn);

        actionLab1 = new JLabel("invoke");
        Dimension dx = actionLab1.getPreferredSize();
        actionLab1.setText("");
        actionLab1.setPreferredSize(dx);
        actionLab1.setHorizontalAlignment(JLabel.TRAILING);

        actionLab2 = new JLabel("invoke");
        actionLab2.setText("");
        actionLab2.setPreferredSize(dx);
        actionLab2.setHorizontalAlignment(JLabel.LEADING);

        JLabel localInvokeLab = new JLabel("local invocation");
        JLabel methodLab = new JLabel("invoke local var method");
        JLabel stateTranInvokeLab = new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "." + OneLinePanel.CLOSE_LABEL_BOLD);

        stateVarsCB = new JComboBox<>();
        setMaxHeight(stateVarsCB);
        stateVarsCB.setBackground(Color.white);
        newSVButt = new JButton("new");
        descriptionField = new JTextField(25);
        setMaxHeight(descriptionField);
        actionField = new JTextField(15);
        actionField.setToolTipText("Use this field to provide a method "
                + "argument, or a value to assign");
        setMaxHeight(actionField);
        arrayIndexField = new JTextField(5);
        setMaxHeight(arrayIndexField);
        localAssignmentField = new JTextField(15);
        localAssignmentField.setToolTipText("Use this field to optionally "
                + "assign a return type from a state variable to an already "
                + "declared local variable");
        setMaxHeight(localAssignmentField);
        localInvocationField = new JTextField(15);
        localInvocationField.setToolTipText("Use this field to optionally "
                + "invoke a zero parameter void method call to an already "
                + "declared local variable, or an argument");
        setMaxHeight(localInvocationField);

        int w = OneLinePanel.maxWidth(new JComponent[]{commLab, localAssignLab,
            nameLab, arrayIdxLab, assTo, opOn, stateTranInvokeLab,
            localInvokeLab, methodLab});

        fieldsPanel.add(new OneLinePanel(commLab, w, descriptionField));
        fieldsPanel.add(Box.createVerticalStrut(10));

        JSeparator divider = new JSeparator(JSeparator.HORIZONTAL);
        divider.setBackground(Color.blue.brighter());

        fieldsPanel.add(divider);
        fieldsPanel.add(localAssignmentPanel = new OneLinePanel(localAssignLab,
                w,
                localAssignmentField,
                new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "=" + OneLinePanel.CLOSE_LABEL_BOLD)));
        fieldsPanel.add(Box.createVerticalStrut(10));
        fieldsPanel.add(new OneLinePanel(nameLab, w, stateVarsCB, newSVButt));
        fieldsPanel.add(indexPanel = new OneLinePanel(arrayIdxLab, w, arrayIndexField));
        fieldsPanel.add(new OneLinePanel(new JLabel(""), w, assTo));
        fieldsPanel.add(new OneLinePanel(new JLabel(""), w, opOn));

        stateTranMethodsCB = new JComboBox<>();
        stateTranMethodsCB.setToolTipText("Use this to select methods to invoke"
                + " on state variables");
        setMaxHeight(stateTranMethodsCB);
        stateTranMethodsCB.setBackground(Color.white);

        fieldsPanel.add(stateTransInvokePanel = new OneLinePanel(stateTranInvokeLab, w, stateTranMethodsCB));
        fieldsPanel.add(new OneLinePanel(actionLab1, w, actionField, actionLab2));

        divider = new JSeparator(JSeparator.HORIZONTAL);
        divider.setBackground(Color.blue.brighter());

        fieldsPanel.add(divider);
        fieldsPanel.add(Box.createVerticalStrut(10));

        localVarMethodsCB = new JComboBox<>();
        localVarMethodsCB.setToolTipText("Use this to select void return type methods "
                + "for locally declared variables, or arguments");
        setMaxHeight(localVarMethodsCB);
        localVarMethodsCB.setBackground(Color.white);

        fieldsPanel.add(new OneLinePanel(
                localInvokeLab,
                w,
                localInvocationField,
                localInvokeDot = new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "." + OneLinePanel.CLOSE_LABEL_BOLD)));
        fieldsPanel.add(localInvocationPanel = new OneLinePanel(methodLab, w, localVarMethodsCB));

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
        descriptionField.addCaretListener(lis);
        localAssignmentField.addCaretListener(lis);
        arrayIndexField.addCaretListener(lis);
        actionField.addCaretListener(lis);
        localInvocationField.addCaretListener(lis);

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
        stateTranMethodsCB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                okButt.setEnabled(true);
                modified = true;
            }
        });
        localVarMethodsCB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                okButt.setEnabled(true);
                modified = true;
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

        setParams(parent, param);
    }

    private void setMaxHeight(JComponent c) {
        Dimension d = c.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        c.setMaximumSize(d);
    }

    private ComboBoxModel<String> resolveStateTranMethodCalls() {
        Class<?> type;
        String typ;
        java.util.List<Method> methods;
        java.util.List<ViskitElement> types = new ArrayList<>();
        Vector<String> methodNames = new Vector<>();

        // Prevent duplicate types
        for (int i = 0; i < stateVarsCB.getItemCount(); i++) {
            ViskitElement e = stateVarsCB.getItemAt(i);
            typ = e.getType();

            if (VGlobals.instance().isGeneric(typ)) {
                typ = typ.substring(0, typ.indexOf("<"));
            }
            if (VGlobals.instance().isArray(typ)) {
                typ = typ.substring(0, typ.indexOf("["));
            }

            if (types.isEmpty()) {
                types.add(e);
            } else if (!types.get(types.size()-1).getType().contains(typ)) {
                types.add(e);
            }
        }

        Collections.sort(types);

        String className;
        for (ViskitElement e : types) {
            typ = e.getType();

            if (VGlobals.instance().isGeneric(typ)) {
                typ = typ.substring(0, typ.indexOf("<"));
            }
            if (VGlobals.instance().isArray(typ)) {
                typ = typ.substring(0, typ.indexOf("["));
            }
            type = VStatics.classForName(typ);
            methods = Arrays.asList(type.getMethods());

            // Filter out methods of Object, and any
            // methods requiring more then one parameter
            for (Method method : methods) {
                className = method.getDeclaringClass().getName();
                if (className.contains("java.lang.Object")) {continue;}

                if (method.getParameterCount() == 0) {
                    methodNames.add(method.getName() + "()");
                } else if (method.getParameterCount() == 1) {
                    methodNames.add(method.getName() + "(" + method.getParameterTypes()[0].getTypeName() + ")");
                }
            }
        }
        Collections.sort(methodNames);
        return new DefaultComboBoxModel<>(methodNames);
    }

    private ComboBoxModel<String> resolveLocalMethodCalls() {
        Class<?> type;
        Method[] methods;
        String typ;
        java.util.List<ViskitElement> types = localVariablesPanel.getData();
        Vector<String> methodNames = new Vector<>();

        // Enable argument type methods to be invoked as well
        types.addAll(argPanel.getData());

        // Last chance to pull if from a quickly declared local variable
        // NOTE: Not ready for this, but good intention here
//        if (types.isEmpty()) {
//            String[] typs = localAssignmentField.getText().split(" ");
//            if (typs.length > 1) {
//                types = new ArrayList<>();
//                EventLocalVariable v = new EventLocalVariable(typs[1], typs[0], "");
//                types.add(v);
//            }
//        }

        String className;
        for (ViskitElement e : types) {
            typ = e.getType();

            if (VGlobals.instance().isGeneric(typ)) {
                typ = typ.substring(0, typ.indexOf("<"));
            }
            if (VGlobals.instance().isArray(typ)) {
                typ = typ.substring(0, typ.indexOf("["));
            }
            type = VStatics.classForName(typ);
            methods = type.getMethods();

            // Filter out methods of Object, non-void return types and any
            // methods requiring parameters
            for (Method method : methods) {
                className = method.getDeclaringClass().getName();
                if (className.contains("java.lang.Object")) {continue;}
                if (!method.getReturnType().getName().contains("void")) {continue;}
                if (method.getParameterCount() > 0) {continue;}

                if (!methodNames.contains(method.getName() + "()"))
                    methodNames.add(method.getName() + "()");
            }
        }
        Collections.sort(methodNames);
        return new DefaultComboBoxModel<>(methodNames);
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
            localAssignmentField.setText(param.getLocalVariableAssignment());
            setStateVariableCBValue(param);
            String ie = param.getIndexingExpression();
            if (ie == null || ie.isEmpty()) {
                arrayIndexField.setText(indexArg);
            } else {
                arrayIndexField.setText(ie);
            }
            boolean isOp = param.isOperation();
            if (isOp) {
                opOn.setSelected(isOp);
                actionLab1.setText("(");
                actionLab2.setText(" )");

                // Strip out the argument from the method name and its
                // parentheses
                String op = param.getOperationOrAssignment();
                op = op.substring(op.indexOf("("), op.length());
                op = op.replace("(", "");
                op = op.replace(")", "");
                actionField.setText(op);
            } else {
                assTo.setSelected(!isOp);
                actionLab1.setText("=");
                actionLab2.setText("");
                actionField.setText(param.getOperationOrAssignment());
            }
            setStateTranMethodsCBValue(param);

            // We only need the variable, not the method invocation
            String localInvoke = param.getLocalVariableInvocation();
            if (localInvoke != null && !localInvoke.isEmpty())
                localInvocationField.setText(localInvoke.split("\\.")[0]);

            setLocalVariableCBValue(param);
        } else {
            descriptionField.setText("");
            localAssignmentField.setText("");
            stateVarsCB.setSelectedIndex(0);
            arrayIndexField.setText(indexArg);
            stateTranMethodsCB.setSelectedIndex(0);
            actionField.setText("");
            assTo.setSelected(true);
            localInvocationField.setText("");
            localVarMethodsCB.setSelectedIndex(0);
        }

        // We have an indexing argument already set
        String typ = ((vStateVariable) stateVarsCB.getSelectedItem()).getType();
        indexPanel.setVisible(VGlobals.instance().isArray(typ));
        localAssignmentPanel.setVisible(opOn.isSelected());
    }

    private void setStateVariableCBValue(EventStateTransition est) {
        stateVarsCB.setModel(VGlobals.instance().getStateVarsCBModel());
        stateVarsCB.setSelectedIndex(0);
        for (int i = 0; i < stateVarsCB.getItemCount(); i++) {
            vStateVariable sv = (vStateVariable) stateVarsCB.getItemAt(i);
            if (est.getName().equalsIgnoreCase(sv.getName())) {
                stateVarsCB.setSelectedIndex(i);
                return;
            }
        }

        // TODO: determine if this is necessary
//        if (est.getStateVarName().isEmpty()) // for first time
//        {
//            ((EventGraphControllerImpl)VGlobals.instance().getEventGraphController()).messageUser(
//                    JOptionPane.ERROR_MESSAGE,
//                    "Alert",
//                    "State variable " + est.getStateVarName() + "not found.");
//        }
    }

    private void setStateTranMethodsCBValue(EventStateTransition est) {
        stateTranMethodsCB.setModel(resolveStateTranMethodCalls());
        stateTransInvokePanel.setVisible(opOn.isSelected());
        pack();

        if (stateTranMethodsCB.getItemCount() <= 0) {return;}

        stateTranMethodsCB.setSelectedIndex(0);
        String ops = est.getOperationOrAssignment();

        if (opOn.isSelected())
            ops = ops.substring(0, ops.indexOf("("));

        String me;
        for (int i = 0; i < stateTranMethodsCB.getItemCount(); i++) {
            me = stateTranMethodsCB.getItemAt(i);

            if (opOn.isSelected())
                me = me.substring(0, me.indexOf("("));

            if (me.contains(ops)) {
                stateTranMethodsCB.setSelectedIndex(i);
                break;
            }
        }
    }

    private void setLocalVariableCBValue(EventStateTransition est) {
        localVarMethodsCB.setModel(resolveLocalMethodCalls());
        localInvocationPanel.setVisible(opOn.isSelected());
        pack();

        // Check for any local variables first
        if (localVarMethodsCB.getItemCount() <= 0) {return;}

        localVarMethodsCB.setSelectedIndex(0);
        String lVMethodCall = est.getLocalVariableInvocation();
        String call;
        for (int i = 0; i < localVarMethodsCB.getItemCount(); i++) {
            call = localVarMethodsCB.getItemAt(i);
            if (lVMethodCall != null && lVMethodCall.contains(call)) {
                localVarMethodsCB.setSelectedIndex(i);
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

            if (!localAssignmentField.getText().isEmpty())
                param.setLocalVariableAssignment(localAssignmentField.getText().trim());

            param.setName(((vStateVariable) stateVarsCB.getSelectedItem()).getName());
            param.setType(((vStateVariable) stateVarsCB.getSelectedItem()).getType());
            param.setIndexingExpression(arrayIndexField.getText().trim());

            String arg = actionField.getText().trim();
            if (opOn.isSelected()) {
                String methodCall = (String) stateTranMethodsCB.getSelectedItem();

                // Insert the argument
                if (!arg.isEmpty()) {
                    methodCall = methodCall.substring(0, methodCall.indexOf("(") + 1);
                    methodCall += arg + ")";
                }

                param.setOperationOrAssignment(methodCall);
            } else {
                param.setOperationOrAssignment(arg);
            }
            param.setOperation(opOn.isSelected());

            if (!localInvocationField.getText().isEmpty())
                param.setLocalVariableInvocation(localInvocationField.getText().trim()
                        + "."
                        + (String) localVarMethodsCB.getSelectedItem());
        }
    }

    class cancelButtonListener implements ActionListener {

        // bugfix 1183
        @Override
        public void actionPerformed(ActionEvent event) {
            modified = false;    // for the caller
            localAssignmentField.setText("");
            arrayIndexField.setText("");
            actionField.setText("");
            localInvocationField.setText("");
            VGlobals.instance().getActiveEventGraphModel().resetIdxNameGenerator();
            dispose();
        }
    }

    class RadButtListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            modified = true;
            okButt.setEnabled(true);

            Dimension d = actionLab1.getPreferredSize();
            if (assTo.isSelected()) {
                actionLab1.setText("=");
                actionLab2.setText("");
            } else if (opOn.isSelected()) {
                String ty = ((vStateVariable) stateVarsCB.getSelectedItem()).getType();
                if (VGlobals.instance().isPrimitive(ty)) {
                    ((EventGraphControllerImpl)VGlobals.instance().getEventGraphController()).messageUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Java Language Error",
                            "A method may not be invoked on a primitive type.");
                    assTo.setSelected(true);
                } else {
                    actionLab1.setText("(");
                    actionLab2.setText(" )");
                }
            }
            localAssignmentPanel.setVisible(opOn.isSelected());
            stateTransInvokePanel.setVisible(opOn.isSelected());
            actionLab1.setPreferredSize(d);
            actionLab2.setPreferredSize(d);
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
                        int ret = JOptionPane.showConfirmDialog(EventStateTransitionDialog.this,
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
                    int ret = JOptionPane.showConfirmDialog(EventStateTransitionDialog.this,
                            "No transition (action) has been entered." +
                            "\nIgnore and continue?",
                            "Warning",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
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
            localInvocationPanel.setVisible(!localInvocationField.getText().isEmpty());
            localInvokeDot.setEnabled(!localInvocationField.getText().isEmpty());
            modified = true;
            okButt.setEnabled(true);
            getRootPane().setDefaultButton(okButt);
            pack();
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
                int ret = JOptionPane.showConfirmDialog(EventStateTransitionDialog.this, "Apply changes?",
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
