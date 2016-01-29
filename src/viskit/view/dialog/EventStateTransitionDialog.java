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
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
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

    private static EventStateTransitionDialog dialog;
    private static boolean modified = false;
    private static boolean allGood;

    private final JTextField actionField, arrayIndexField, localAssignmentField, localInvocationField, descriptionField;
    private JComboBox<ViskitElement> stateVariablesCB;
    private final JComboBox<String> stateTransitionMethodsCB, localVariableMethodsCB;
    private final JRadioButton assignToRB, invokeOnRB;
    private EventStateTransition param;
    private final JButton okButton, cancelButton;
    private JButton newStateVariableButton;
    private final JLabel actionLabel1, actionLabel2, localInvokeDot;
    private final JPanel localAssignmentPanel, indexPanel, stateTransInvokePanel, localInvocationPanel;

    /** Required to get the EventArgument for indexing a State Variable array */
    private final ArgumentsPanel argumentsPanel;

    /** Required to get the type of any declared local variables */
    private final LocalVariablesPanel localVariablesPanel;

    public static boolean showDialog(JFrame f, EventStateTransition est, ArgumentsPanel ap, LocalVariablesPanel lvp) {
        if (dialog == null)
            dialog = new EventStateTransitionDialog(f, est, ap, lvp);
        else
            dialog.setParams(f, est);

        if (allGood)
            dialog.setVisible(true);

        // above call blocks
        return modified;
    }

    private EventStateTransitionDialog(JFrame parent, EventStateTransition param, ArgumentsPanel ap, LocalVariablesPanel lvp) {

        super(parent, "State Transition", true);
        argumentsPanel = ap;
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

        assignToRB = new JRadioButton("assign to (\"=\")");
        invokeOnRB = new JRadioButton("invoke on (\".\")");
        ButtonGroup bg = new ButtonGroup();
        bg.add(assignToRB);
        bg.add(invokeOnRB);

        actionLabel1 = new JLabel("invoke");
        Dimension dx = actionLabel1.getPreferredSize();
        actionLabel1.setText("");
        actionLabel1.setPreferredSize(dx);
        actionLabel1.setHorizontalAlignment(JLabel.TRAILING);

        actionLabel2 = new JLabel("invoke");
        actionLabel2.setText("");
        actionLabel2.setPreferredSize(dx);
        actionLabel2.setHorizontalAlignment(JLabel.LEADING);

        JLabel localInvokeLab = new JLabel("local invocation");
        JLabel methodLab = new JLabel("invoke local var method");
        JLabel stateTranInvokeLab = new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "." + OneLinePanel.CLOSE_LABEL_BOLD);

        stateVariablesCB = new JComboBox<>();
        setMaxHeight(stateVariablesCB);
        stateVariablesCB.setBackground(Color.white);
        newStateVariableButton = new JButton("new");
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
            nameLab, arrayIdxLab, assignToRB, invokeOnRB, stateTranInvokeLab,
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
        fieldsPanel.add(new OneLinePanel(nameLab, w, stateVariablesCB, newStateVariableButton));
        fieldsPanel.add(indexPanel = new OneLinePanel(arrayIdxLab, w, arrayIndexField));
        fieldsPanel.add(new OneLinePanel(new JLabel(""), w, assignToRB));
        fieldsPanel.add(new OneLinePanel(new JLabel(""), w, invokeOnRB));

        stateTransitionMethodsCB = new JComboBox<>();
        stateTransitionMethodsCB.setToolTipText("Use this to select methods to invoke"
                + " on state variables");
        setMaxHeight(stateTransitionMethodsCB);
        stateTransitionMethodsCB.setBackground(Color.white);

        fieldsPanel.add(stateTransInvokePanel = new OneLinePanel(stateTranInvokeLab, w, stateTransitionMethodsCB));
        fieldsPanel.add(new OneLinePanel(actionLabel1, w, actionField, actionLabel2));

        divider = new JSeparator(JSeparator.HORIZONTAL);
        divider.setBackground(Color.blue.brighter());

        fieldsPanel.add(divider);
        fieldsPanel.add(Box.createVerticalStrut(10));

        localVariableMethodsCB = new JComboBox<>();
        localVariableMethodsCB.setToolTipText("Use this to select void return type methods "
                + "for locally declared variables, or arguments");
        setMaxHeight(localVariableMethodsCB);
        localVariableMethodsCB.setBackground(Color.white);

        fieldsPanel.add(new OneLinePanel(
                localInvokeLab,
                w,
                localInvocationField,
                localInvokeDot = new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "." + OneLinePanel.CLOSE_LABEL_BOLD)));
        fieldsPanel.add(localInvocationPanel = new OneLinePanel(methodLab, w, localVariableMethodsCB));

        con.add(fieldsPanel);
        con.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        cancelButton = new JButton("Cancel");
        okButton = new JButton("Apply changes");
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);

        con.add(buttonPanel);
        con.add(Box.createVerticalGlue());    // takes up space when dialog is expanded vertically

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        enableApplyButtonListener lis = new enableApplyButtonListener();
        descriptionField.addCaretListener(lis);
        localAssignmentField.addCaretListener(lis);
        arrayIndexField.addCaretListener(lis);
        actionField.addCaretListener(lis);
        localInvocationField.addCaretListener(lis);

        stateVariablesCB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JComboBox cb = (JComboBox) actionEvent.getSource();
                vStateVariable sv = (vStateVariable) cb.getSelectedItem();
                descriptionField.setText(sv.getComment());
                okButton.setEnabled(true);
                indexPanel.setVisible(ViskitGlobals.instance().isArray(sv.getType()));
                modified = true;
                pack();
            }
        });
        newStateVariableButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String nm = ViskitGlobals.instance().getEventGraphEditor().addStateVariableDialog();
                if (nm != null) {
                    stateVariablesCB.setModel(ViskitGlobals.instance().getStateVarsCBModel());
                    for (int i = 0; i < stateVariablesCB.getItemCount(); i++) {
                        vStateVariable vsv = (vStateVariable) stateVariablesCB.getItemAt(i);
                        if (vsv.getName().contains(nm)) {
                            stateVariablesCB.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }
        });
        stateTransitionMethodsCB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                okButton.setEnabled(true);
                modified = true;
            }
        });
        localVariableMethodsCB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                okButton.setEnabled(true);
                modified = true;
            }
        });

        // to start off:
        if (stateVariablesCB.getItemCount() > 0) {
            descriptionField.setText(((vStateVariable) stateVariablesCB.getItemAt(0)).getComment());
        } else {
            descriptionField.setText("");
        }

        RadButtListener rbl = new RadButtListener();
        assignToRB.addActionListener(rbl);
        invokeOnRB.addActionListener(rbl);

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
        Vector<String> methodNames = new Vector<>();
        java.util.List<ViskitElement> types = new ArrayList<>();

        // Prevent duplicate types
        for (int i = 0; i < stateVariablesCB.getItemCount(); i++) {
            ViskitElement e = stateVariablesCB.getItemAt(i);
            typ = e.getType();

            if (ViskitGlobals.instance().isGeneric(typ)) {
                typ = typ.substring(0, typ.indexOf("<"));
            }
            if (ViskitGlobals.instance().isArray(typ)) {
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

            if (ViskitGlobals.instance().isGeneric(typ)) {
                typ = typ.substring(0, typ.indexOf("<"));
            }
            if (ViskitGlobals.instance().isArray(typ)) {
                typ = typ.substring(0, typ.indexOf("["));
            }

            // Beware of qualified types here
            type = ViskitStatics.classForName(typ);
            methods = Arrays.asList(type.getMethods());

            // Filter out methods of Object, and any
            // methods requiring more then one parameter
            for (Method method : methods) {
                className = method.getDeclaringClass().getName();
                if (className.contains(ViskitStatics.JAVA_LANG_OBJECT)) {continue;}

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
        Vector<String> methodNames = new Vector<>();
        java.util.List<ViskitElement> types = new ArrayList<>(localVariablesPanel.getData());

        // Enable argument type methods to be invoked as well
        types.addAll(argumentsPanel.getData());

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

            if (ViskitGlobals.instance().isGeneric(typ)) {
                typ = typ.substring(0, typ.indexOf("<"));
            }
            if (ViskitGlobals.instance().isArray(typ)) {
                typ = typ.substring(0, typ.indexOf("["));
            }
            type = ViskitStatics.classForName(typ);

            if (type == null) {
                ((EventGraphControllerImpl) ViskitGlobals.instance().getEventGraphController()).messageUser(
                        JOptionPane.WARNING_MESSAGE,
                        typ + " not found on the Classpath",
                        "Please make sure you are using fully qualified java "
                                + "names when referencing a local type");
                return null;
            }
            methods = type.getMethods();

            // Filter out methods of Object, non-void return types and any
            // methods requiring parameters
            for (Method method : methods) {
                className = method.getDeclaringClass().getName();
                if (className.contains(ViskitStatics.JAVA_LANG_OBJECT)) {continue;}
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
        allGood = true;
        param = p;

        fillWidgets();

        if (!allGood) {return;}

        modified = (p == null);
        okButton.setEnabled(p == null);

        getRootPane().setDefaultButton(cancelButton);
        pack(); // do this prior to next
        setLocationRelativeTo(c);
    }

    // bugfix 1183
    private void fillWidgets() {
        String indexArg = "";

        // Conceptually, should only be one indexing argument
        if (!argumentsPanel.isEmpty()) {
            for (ViskitElement ia : argumentsPanel.getData()) {
                indexArg = ia.getName();
                break;
            }
        }

        if (param != null) {
            if (stateVariablesCB.getItemCount() > 0) {
                descriptionField.setText(((vStateVariable) stateVariablesCB.getSelectedItem()).getComment());
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
                invokeOnRB.setSelected(isOp);
                actionLabel1.setText("(");
                actionLabel2.setText(" )");

                // Strip out the argument from the method name and its
                // parentheses
                String op = param.getOperationOrAssignment();
                op = op.substring(op.indexOf("("), op.length());
                op = op.replace("(", "");
                op = op.replace(")", "");
                actionField.setText(op);
            } else {
                assignToRB.setSelected(!isOp);
                actionLabel1.setText("=");
                actionLabel2.setText("");
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
            stateVariablesCB.setSelectedIndex(0);
            arrayIndexField.setText(indexArg);
            stateTransitionMethodsCB.setSelectedIndex(0);
            actionField.setText("");
            assignToRB.setSelected(true);
            localInvocationField.setText("");
            localVariableMethodsCB.setSelectedIndex(0);
        }

        // We have an indexing argument already set
        String typ = ((vStateVariable) stateVariablesCB.getSelectedItem()).getType();
        indexPanel.setVisible(ViskitGlobals.instance().isArray(typ));
        localAssignmentPanel.setVisible(invokeOnRB.isSelected());
    }

    private void setStateVariableCBValue(EventStateTransition est) {
        stateVariablesCB.setModel(ViskitGlobals.instance().getStateVarsCBModel());
        stateVariablesCB.setSelectedIndex(0);
        for (int i = 0; i < stateVariablesCB.getItemCount(); i++) {
            vStateVariable sv = (vStateVariable) stateVariablesCB.getItemAt(i);
            if (est.getName().equalsIgnoreCase(sv.getName())) {
                stateVariablesCB.setSelectedIndex(i);
                return;
            }
        }

        // TODO: determine if this is necessary
//        if (est.getStateVarName().isEmpty()) // for first time
//        {
//            ((EventGraphControllerImpl)ViskitGlobals.instance().getEventGraphController()).messageUser(
//                    JOptionPane.ERROR_MESSAGE,
//                    "Alert",
//                    "State variable " + est.getStateVarName() + "not found.");
//        }
    }

    private void setStateTranMethodsCBValue(EventStateTransition est) {
        stateTransitionMethodsCB.setModel(resolveStateTranMethodCalls());
        stateTransInvokePanel.setVisible(invokeOnRB.isSelected());
        pack();

        if (stateTransitionMethodsCB.getItemCount() <= 0) {return;}

        stateTransitionMethodsCB.setSelectedIndex(0);
        String ops = est.getOperationOrAssignment();

        if (invokeOnRB.isSelected())
            ops = ops.substring(0, ops.indexOf("("));

        String me;
        for (int i = 0; i < stateTransitionMethodsCB.getItemCount(); i++) {
            me = stateTransitionMethodsCB.getItemAt(i);

            if (invokeOnRB.isSelected())
                me = me.substring(0, me.indexOf("("));

            if (me.contains(ops)) {
                stateTransitionMethodsCB.setSelectedIndex(i);
                break;
            }
        }
    }

    private void setLocalVariableCBValue(EventStateTransition est) {

        ComboBoxModel<String> mod = resolveLocalMethodCalls();

        if (mod == null) {
            allGood = false;
            return;
        }

        localVariableMethodsCB.setModel(mod);
        localInvocationPanel.setVisible(invokeOnRB.isSelected());
        pack();

        // Check for any local variables first
        if (localVariableMethodsCB.getItemCount() <= 0) {return;}

        localVariableMethodsCB.setSelectedIndex(0);
        String lVMethodCall = est.getLocalVariableInvocation();
        String call;
        for (int i = 0; i < localVariableMethodsCB.getItemCount(); i++) {
            call = localVariableMethodsCB.getItemAt(i);
            if (lVMethodCall != null && lVMethodCall.contains(call)) {
                localVariableMethodsCB.setSelectedIndex(i);
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
            else
                param.setLocalVariableAssignment("");

            param.setName(((vStateVariable) stateVariablesCB.getSelectedItem()).getName());
            param.setType(((vStateVariable) stateVariablesCB.getSelectedItem()).getType());
            param.setIndexingExpression(arrayIndexField.getText().trim());

            String arg = actionField.getText().trim();
            if (invokeOnRB.isSelected()) {
                String methodCall = (String) stateTransitionMethodsCB.getSelectedItem();

                // Insert the argument
                if (!arg.isEmpty()) {
                    methodCall = methodCall.substring(0, methodCall.indexOf("(") + 1);
                    methodCall += arg + ")";
                }

                param.setOperationOrAssignment(methodCall);
            } else {
                param.setOperationOrAssignment(arg);
            }
            param.setOperation(invokeOnRB.isSelected());

            if (!localInvocationField.getText().isEmpty())
                param.setLocalVariableInvocation(localInvocationField.getText().trim()
                        + "."
                        + (String) localVariableMethodsCB.getSelectedItem());
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
            ViskitGlobals.instance().getActiveEventGraphModel().resetIdxNameGenerator();
            dispose();
        }
    }

    class RadButtListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            modified = true;
            okButton.setEnabled(true);

            Dimension d = actionLabel1.getPreferredSize();
            if (assignToRB.isSelected()) {
                actionLabel1.setText("=");
                actionLabel2.setText("");
            } else if (invokeOnRB.isSelected()) {
                String ty = ((vStateVariable) stateVariablesCB.getSelectedItem()).getType();
                if (ViskitGlobals.instance().isPrimitive(ty)) {
                    ((EventGraphControllerImpl)ViskitGlobals.instance().getEventGraphController()).messageUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Java Language Error",
                            "A method may not be invoked on a primitive type.");
                    assignToRB.setSelected(true);
                } else {
                    actionLabel1.setText("(");
                    actionLabel2.setText(" )");
                }
            }
            localAssignmentPanel.setVisible(invokeOnRB.isSelected());
            stateTransInvokePanel.setVisible(invokeOnRB.isSelected());
            actionLabel1.setPreferredSize(d);
            actionLabel2.setPreferredSize(d);
            pack();
        }
    }

    class applyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (modified) {
                // check for array index
                String typ = ((vStateVariable) stateVariablesCB.getSelectedItem()).getType();
                if (ViskitGlobals.instance().isArray(typ)) {
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
            okButton.setEnabled(true);
            getRootPane().setDefaultButton(okButton);
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
