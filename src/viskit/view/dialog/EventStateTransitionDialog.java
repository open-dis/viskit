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
import viskit.control.EventGraphController;

import viskit.model.EventStateTransition;
import viskit.model.ViskitElement;
import viskit.model.ViskitStateVariable;
import viskit.view.ArgumentsPanel;
import viskit.view.LocalVariablesPanel;
import static viskit.ViskitStatics.DESCRIPTION_HINT;

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
public class EventStateTransitionDialog extends JDialog 
{
    private static EventStateTransitionDialog dialog;
    private static boolean modified = false;
    private static boolean allGood;
    private final JTextField actionField;
    private final JTextField arrayIndexField;
    private final JTextField localAssignmentField;

    private final JTextField localInvocationField;
    private JTextField descriptionField;
    private JComboBox<ViskitElement> stateVariablesCB;
    private final JComboBox<String> stateTranitionMethodsCB;
    private final JComboBox<String> localVarMethodsCB;
    private final JRadioButton assignToRadioButton;
    private final JRadioButton invokeOnRadioButton;
    private EventStateTransition parameterEventStateTransition;
    private final JButton okButton;
    private final JButton cancelButton;
    private final JButton newStateVariableButton;
    private final JLabel invokeOnLabel;
    private final JLabel invokeOnLabel2;
    private final JLabel localInvokeDot;
    private final JPanel localAssignmentPanel;
    private final JPanel indexPanel;
    private final JPanel stateTransInvokePanel;
    private final JPanel localInvocationPanel;

    /** Required to get the EventArgument for indexing a State Variable array */
    private final ArgumentsPanel argPanel;

    /** Required to get the type of any declared local variables */
    private final LocalVariablesPanel localVariablesPanel;

    public static boolean showDialog(JFrame frame, EventStateTransition eventStateTransitionParameter,    
                                     ArgumentsPanel argumentsPanel, LocalVariablesPanel localVariablesPanel) 
    {
        if (dialog == null)
            dialog = new EventStateTransitionDialog(frame, eventStateTransitionParameter, argumentsPanel, localVariablesPanel);
        else
            dialog.setParameters(frame, eventStateTransitionParameter);

        if (allGood)
            dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private EventStateTransitionDialog(JFrame parentFrame, EventStateTransition eventStateTransitionParameter,        
                                       ArgumentsPanel argumentsPanel, LocalVariablesPanel localVariablesPanel) {

        super(parentFrame, "State Transition", true);
        argPanel = argumentsPanel;
        this.localVariablesPanel = localVariablesPanel;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        JPanel panel = new JPanel();
        setContentPane(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); //createEtchedBorder(EtchedBorder.RAISED));

        panel.add(Box.createVerticalStrut(5));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.PAGE_AXIS));

        JLabel descriptionLabel = new JLabel("description");
        descriptionLabel.setToolTipText(DESCRIPTION_HINT);
        JLabel localAssignLabel = new JLabel("local assignment");
        JLabel nameLabel = new JLabel("state variable");
        JLabel arrayIndexVariableLabel = new JLabel("index variable");

        assignToRadioButton = new JRadioButton("assign to (\"=\")");
        invokeOnRadioButton = new JRadioButton("invoke on (\".\")");
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(assignToRadioButton);
        buttonGroup.add(invokeOnRadioButton);

        invokeOnLabel = new JLabel("invoke");
        Dimension dx = invokeOnLabel.getPreferredSize();
        invokeOnLabel.setText("");
        invokeOnLabel.setPreferredSize(dx);
        invokeOnLabel.setHorizontalAlignment(JLabel.TRAILING);

        invokeOnLabel2 = new JLabel("invoke");
        invokeOnLabel2.setText("");
        invokeOnLabel2.setPreferredSize(dx);
        invokeOnLabel2.setHorizontalAlignment(JLabel.LEADING);

        JLabel localInvocationLabel           = new JLabel("local invocation");
        JLabel invokeLocalMethodLable         = new JLabel("invoke method on loca variable");
        JLabel stateTransitionInvocationLabel = new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "." + OneLinePanel.CLOSE_LABEL_BOLD);

        stateVariablesCB = new JComboBox<>();
        setMaxHeight(stateVariablesCB);
        stateVariablesCB.setBackground(Color.white);
        newStateVariableButton = new JButton("new");
        descriptionField = new JTextField(25);
        descriptionField.setToolTipText(DESCRIPTION_HINT);
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

        int w = OneLinePanel.maxWidth(new JComponent[]{descriptionLabel, localAssignLabel,
            nameLabel, arrayIndexVariableLabel, assignToRadioButton, invokeOnRadioButton, stateTransitionInvocationLabel,
            localInvocationLabel, invokeLocalMethodLable});

        fieldsPanel.add(new OneLinePanel(descriptionLabel, w, descriptionField));
        fieldsPanel.add(Box.createVerticalStrut(10));

        JSeparator divider = new JSeparator(JSeparator.HORIZONTAL);
        divider.setBackground(Color.blue.brighter());

        fieldsPanel.add(divider);
        fieldsPanel.add(localAssignmentPanel = new OneLinePanel(localAssignLabel,
                w,
                localAssignmentField,
                new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "=" + OneLinePanel.CLOSE_LABEL_BOLD)));
        fieldsPanel.add(Box.createVerticalStrut(10));
        fieldsPanel.add(new OneLinePanel(nameLabel, w, stateVariablesCB, newStateVariableButton));
        fieldsPanel.add(indexPanel = new OneLinePanel(arrayIndexVariableLabel, w, arrayIndexField));
        fieldsPanel.add(new OneLinePanel(new JLabel(""), w, assignToRadioButton));
        fieldsPanel.add(new OneLinePanel(new JLabel(""), w, invokeOnRadioButton));

        stateTranitionMethodsCB = new JComboBox<>();
        stateTranitionMethodsCB.setToolTipText("Select a method to invoke on a state variable");
        setMaxHeight(stateTranitionMethodsCB);
        stateTranitionMethodsCB.setBackground(Color.white);

        fieldsPanel.add(stateTransInvokePanel = new OneLinePanel(stateTransitionInvocationLabel, w, stateTranitionMethodsCB));
        fieldsPanel.add(new OneLinePanel(invokeOnLabel, w, actionField, invokeOnLabel2));

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
                localInvocationLabel,
                w,
                localInvocationField,
                localInvokeDot = new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "." + OneLinePanel.CLOSE_LABEL_BOLD)));
        fieldsPanel.add(localInvocationPanel = new OneLinePanel(invokeLocalMethodLable, w, localVarMethodsCB));

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

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        enableApplyButtonListener lis = new enableApplyButtonListener();
        descriptionField.addCaretListener(lis);
        localAssignmentField.addCaretListener(lis);
        arrayIndexField.addCaretListener(lis);
        actionField.addCaretListener(lis);
        localInvocationField.addCaretListener(lis);

        stateVariablesCB.addActionListener((ActionEvent e) -> {
            JComboBox cb = (JComboBox) e.getSource();
            ViskitStateVariable stateVariable = (ViskitStateVariable) cb.getSelectedItem();
            descriptionField.setText(stateVariable.getDescription());
            okButton.setEnabled(true);
            indexPanel.setVisible(ViskitGlobals.instance().isArray(stateVariable.getType()));
            modified = true;
            pack();
        });
        newStateVariableButton.addActionListener((ActionEvent e) -> {
            String stateVarjableName = ViskitGlobals.instance().getEventGraphViewFrame().addStateVariableDialog();
            if (stateVarjableName != null) {
                stateVariablesCB.setModel(ViskitGlobals.instance().getStateVariablesComboBoxModel());
                for (int i = 0; i < stateVariablesCB.getItemCount(); i++) {
                    ViskitStateVariable stateVariable = (ViskitStateVariable) stateVariablesCB.getItemAt(i);
                    if (stateVariable.getName().contains(stateVarjableName)) {
                        stateVariablesCB.setSelectedIndex(i);
                        break;
                    }
                }
            }
        });
        stateTranitionMethodsCB.addActionListener((ActionEvent e) -> {
            okButton.setEnabled(true);
            modified = true;
        });
        localVarMethodsCB.addActionListener((ActionEvent e) -> {
            okButton.setEnabled(true);
            modified = true;
        });

        // to start off:
        if (stateVariablesCB.getItemCount() > 0) {
            descriptionField.setText(stateVariablesCB.getItemAt(0).getDescription());
            descriptionField.setToolTipText(DESCRIPTION_HINT);
        } 
        else {
            descriptionField.setText("");
            descriptionField.setToolTipText(DESCRIPTION_HINT);
        }

        RadioButtonListener radioButtonListener = new RadioButtonListener();
        assignToRadioButton.addActionListener(radioButtonListener);
        invokeOnRadioButton.addActionListener(radioButtonListener);

        setParameters(parentFrame, eventStateTransitionParameter);
    }

    private void setMaxHeight(JComponent component) {
        Dimension d = component.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        component.setMaximumSize(d);
    }

    private ComboBoxModel<String> resolveStateTransitionMethodCalls() {
        Class<?> type;
        String newType;
        java.util.List<Method> methods;
        Vector<String> methodNames = new Vector<>();
        java.util.List<ViskitElement> types = new ArrayList<>();

        // Prevent duplicate types
        for (int i = 0; i < stateVariablesCB.getItemCount(); i++) {
            ViskitElement viskitElement = stateVariablesCB.getItemAt(i);
            newType = viskitElement.getType();

            if (ViskitGlobals.instance().isGenericType(newType)) {
                newType = newType.substring(0, newType.indexOf("<"));
            }
            if (ViskitGlobals.instance().isArray(newType)) {
                newType = newType.substring(0, newType.indexOf("["));
            }

            if (types.isEmpty()) {
                types.add(viskitElement);
            } else if (!types.get(types.size()-1).getType().contains(newType)) {
                types.add(viskitElement);
            }
        }

        Collections.sort(types);

        String className;
        for (ViskitElement viskitElement : types) {
            newType = viskitElement.getType();

            if (ViskitGlobals.instance().isGenericType(newType)) {
                newType = newType.substring(0, newType.indexOf("<"));
            }
            if (ViskitGlobals.instance().isArray(newType)) {
                newType = newType.substring(0, newType.indexOf("["));
            }

            // Beware of qualified types here
            type = ViskitStatics.classForName(newType);
            methods = Arrays.asList(type.getMethods());

            // Filter out methods of Object, and any
            // methods requiring more then one parameter
            for (Method method : methods) 
            {
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
        String newType;
        Vector<String> methodNames = new Vector<>();
        java.util.List<ViskitElement> types = new ArrayList<>(localVariablesPanel.getData());

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
            newType = e.getType();

            if (ViskitGlobals.instance().isGenericType(newType)) {
                newType = newType.substring(0, newType.indexOf("<"));
            }
            if (ViskitGlobals.instance().isArray(newType)) {
                newType = newType.substring(0, newType.indexOf("["));
            }
            type = ViskitStatics.classForName(newType);

            if (type == null) {
                ((EventGraphController) ViskitGlobals.instance().getEventGraphController()).messageUser(
                        JOptionPane.WARNING_MESSAGE,
                        newType + " not found on the Classpath",
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

    public final void setParameters(Component component, EventStateTransition eventStateTransitionParameter) {
        allGood = true;
        parameterEventStateTransition = eventStateTransitionParameter;

        fillWidgets();

        if (!allGood) {return;}

        modified = (eventStateTransitionParameter == null);
        okButton.setEnabled(modified);

        getRootPane().setDefaultButton(cancelButton);
        pack(); // do this prior to next
        setLocationRelativeTo(component);
    }

    // bugfix 1183
    private void fillWidgets() 
    {
        String indexArg = "";

        // Conceptually, should only be one indexing argument
        if (!argPanel.isEmpty()) {
            for (ViskitElement ia : argPanel.getData()) {
                indexArg = ia.getName();
                break;
            }
        }

        if (parameterEventStateTransition != null) {
            if (stateVariablesCB.getItemCount() > 0) {
                descriptionField.setText(((ViskitElement) stateVariablesCB.getSelectedItem()).getDescription());
                descriptionField.setToolTipText(DESCRIPTION_HINT);
            } 
            else {
                descriptionField.setText("");
                descriptionField.setToolTipText(DESCRIPTION_HINT);
            }
            localAssignmentField.setText(parameterEventStateTransition.getLocalVariableAssignment());
            setStateVariableCBValue(parameterEventStateTransition);
            String ie = parameterEventStateTransition.getIndexingExpression();
            if (ie == null || ie.isEmpty()) {
                arrayIndexField.setText(indexArg);
            } else {
                arrayIndexField.setText(ie);
            }
            boolean isOp = parameterEventStateTransition.isOperation();
            if (isOp) {
                invokeOnRadioButton.setSelected(isOp);
                invokeOnLabel.setText("(");
                invokeOnLabel2.setText(" )");

                // Strip out the argument from the method name and its
                // parentheses
                String op = parameterEventStateTransition.getOperationOrAssignment();
                op = op.substring(op.indexOf("("), op.length());
                op = op.replace("(", "");
                op = op.replace(")", "");
                actionField.setText(op);
            } else {
                assignToRadioButton.setSelected(!isOp);
                invokeOnLabel.setText("=");
                invokeOnLabel2.setText("");
                actionField.setText(parameterEventStateTransition.getOperationOrAssignment());
            }
            setStateTranMethodsCBValue(parameterEventStateTransition);

            // We only need the variable, not the method invocation
            String localInvoke = parameterEventStateTransition.getLocalVariableInvocation();
            if (localInvoke != null && !localInvoke.isEmpty())
                localInvocationField.setText(localInvoke.split("\\.")[0]);

            setLocalVariableCBValue(parameterEventStateTransition);
        } else {
            descriptionField.setText("");
            localAssignmentField.setText("");
            stateVariablesCB.setSelectedIndex(0);
            arrayIndexField.setText(indexArg);
            stateTranitionMethodsCB.setSelectedIndex(0);
            actionField.setText("");
            assignToRadioButton.setSelected(true);
            localInvocationField.setText("");
            localVarMethodsCB.setSelectedIndex(0);
        }

        // We have an indexing argument already set
        String typ = ((ViskitElement) stateVariablesCB.getSelectedItem()).getType();
        indexPanel.setVisible(ViskitGlobals.instance().isArray(typ));
        localAssignmentPanel.setVisible(invokeOnRadioButton.isSelected());
    }

    private void setStateVariableCBValue(EventStateTransition est) {
        stateVariablesCB.setModel(ViskitGlobals.instance().getStateVariablesComboBoxModel());
        stateVariablesCB.setSelectedIndex(0);
        for (int i = 0; i < stateVariablesCB.getItemCount(); i++) {
            ViskitStateVariable sv = (ViskitStateVariable) stateVariablesCB.getItemAt(i);
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
        stateTranitionMethodsCB.setModel(resolveStateTransitionMethodCalls());
        stateTransInvokePanel.setVisible(invokeOnRadioButton.isSelected());
        pack();

        if (stateTranitionMethodsCB.getItemCount() <= 0) {return;}

        stateTranitionMethodsCB.setSelectedIndex(0);
        String ops = est.getOperationOrAssignment();

        if (invokeOnRadioButton.isSelected())
            ops = ops.substring(0, ops.indexOf("("));

        String me;
        for (int i = 0; i < stateTranitionMethodsCB.getItemCount(); i++) {
            me = stateTranitionMethodsCB.getItemAt(i);

            if (invokeOnRadioButton.isSelected())
                me = me.substring(0, me.indexOf("("));

            if (me.contains(ops)) {
                stateTranitionMethodsCB.setSelectedIndex(i);
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

        localVarMethodsCB.setModel(mod);
        localInvocationPanel.setVisible(invokeOnRadioButton.isSelected());
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

    private void unloadWidgets() 
    {
        if (parameterEventStateTransition != null) 
        {
            // obsolete
//            parameterEventStateTransition.getComments().clear();
//            String cs = descriptionField.getText().trim();
//            if (!cs.isEmpty()) {
//                parameterEventStateTransition.getComments().add(0, cs);
//            }
            parameterEventStateTransition.setDescription(descriptionField.getText().trim());

            if (!localAssignmentField.getText().isEmpty())
                parameterEventStateTransition.setLocalVariableAssignment(localAssignmentField.getText().trim());
            else
                parameterEventStateTransition.setLocalVariableAssignment("");

            parameterEventStateTransition.setName(((ViskitElement) stateVariablesCB.getSelectedItem()).getName());
            parameterEventStateTransition.setType(((ViskitElement) stateVariablesCB.getSelectedItem()).getType());
            parameterEventStateTransition.setIndexingExpression(arrayIndexField.getText().trim());

            String arg = actionField.getText().trim();
            if (invokeOnRadioButton.isSelected()) {
                String methodCall = (String) stateTranitionMethodsCB.getSelectedItem();

                // Insert the argument
                if (!arg.isEmpty()) {
                    methodCall = methodCall.substring(0, methodCall.indexOf("(") + 1);
                    methodCall += arg + ")";
                }

                parameterEventStateTransition.setOperationOrAssignment(methodCall);
            }
            else {
                parameterEventStateTransition.setOperationOrAssignment(arg);
            }
            parameterEventStateTransition.setOperation(invokeOnRadioButton.isSelected());

            if (!localInvocationField.getText().isEmpty())
                parameterEventStateTransition.setLocalVariableInvocation(localInvocationField.getText().trim()
                        + "."
                        + localVarMethodsCB.getSelectedItem());
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

    class RadioButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            modified = true;
            okButton.setEnabled(true);

            Dimension d = invokeOnLabel.getPreferredSize();
            if (assignToRadioButton.isSelected()) {
                invokeOnLabel.setText("=");
                invokeOnLabel2.setText("");
            } else if (invokeOnRadioButton.isSelected()) {
                String ty = ((ViskitElement) stateVariablesCB.getSelectedItem()).getType();
                if (ViskitGlobals.instance().isPrimitive(ty)) {
                    ((EventGraphController)ViskitGlobals.instance().getEventGraphController()).messageUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Java Language Error",
                            "A method may not be invoked on a primitive type.");
                    assignToRadioButton.setSelected(true);
                } else {
                    invokeOnLabel.setText("(");
                    invokeOnLabel2.setText(" )");
                }
            }
            localAssignmentPanel.setVisible(invokeOnRadioButton.isSelected());
            stateTransInvokePanel.setVisible(invokeOnRadioButton.isSelected());
            invokeOnLabel.setPreferredSize(d);
            invokeOnLabel2.setPreferredSize(d);
            pack();
        }
    }

    class applyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (modified) {
                // check for array index
                String typ = ((ViskitElement) stateVariablesCB.getSelectedItem()).getType();
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
