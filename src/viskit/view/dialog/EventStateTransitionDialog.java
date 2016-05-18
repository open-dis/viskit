package viskit.view.dialog;

import edu.nps.util.LogUtilities;
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
import org.apache.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.control.EventGraphControllerImpl;

import viskit.model.EventStateTransition;
import viskit.model.ViskitElement;
import viskit.model.ViskitStateVariable;
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
public class EventStateTransitionDialog extends JDialog 
{
    static final Logger LOG = LogUtilities.getLogger(EventStateTransitionDialog.class);

    private static EventStateTransitionDialog dialog;
    private static boolean modified = false;
    private static boolean allGood; // proper initialization

    private final JTextField actionTF, arrayIndexTF, localAssignmentTF, localInvocationTF, descriptionTF;
    private JComboBox<ViskitElement> stateVariablesCB;
    private final JComboBox<String> stateTransitionMethodsCB, localVariableMethodsCB;
    private final JRadioButton assignToRB, invokeOnRB;
    private EventStateTransition eventStateTransition;
    private final JButton okButton, cancelButton;
    private JButton newStateVariableButton;
    private final JLabel actionLabel1, actionLabel2, localInvokeDot;
    private final JPanel localAssignmentPanel, indexPanel, stateTransInvokePanel, localInvocationPanel;

    /** Required to get the EventArgument for indexing a State Variable array */
    private final ArgumentsPanel argumentsPanel;

    /** Required to get the type of any declared local variables */
    private final LocalVariablesPanel localVariablesPanel;

    public static boolean showDialog(JFrame parent, String nodeName, EventStateTransition eventStateTransition, ArgumentsPanel argumentsPanel, LocalVariablesPanel localVariablesPanel) {
        if  (dialog == null)
             dialog = new EventStateTransitionDialog(parent, nodeName, eventStateTransition, argumentsPanel, localVariablesPanel);
		else dialog.setParameters(parent, eventStateTransition);

        if (allGood)
            dialog.setVisible(true); // this call blocks

        return modified;
    }

    private EventStateTransitionDialog(JFrame parent, String nodeName, EventStateTransition eventStateTransition, ArgumentsPanel argumentsPanel, LocalVariablesPanel localVariablesPanel)
	{		
        super(parent, "State Transitions for " + nodeName + " Event", true);
        this.argumentsPanel      = argumentsPanel;
        this.localVariablesPanel = localVariablesPanel;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new MyCloseListener());

        JPanel content = new JPanel();
        setContentPane(content);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); //createEtchedBorder(EtchedBorder.RAISED));

        content.add(Box.createVerticalStrut(5));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.PAGE_AXIS));

        JLabel descriptionLabel     = new JLabel("description");
        JLabel localAssignmentLabel = new JLabel("local assignment");
        JLabel nameLabel            = new JLabel("state variable");
        JLabel indexVariableLabel   = new JLabel("index variable");

        assignToRB = new JRadioButton("assign to (\"=\")");
        invokeOnRB = new JRadioButton("invoke on (\".\")");
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(assignToRB);
        buttonGroup.add(invokeOnRB);

        actionLabel1 = new JLabel("invoke");
        Dimension dx = actionLabel1.getPreferredSize();
        actionLabel1.setText("");
        actionLabel1.setPreferredSize(dx);
        actionLabel1.setHorizontalAlignment(JLabel.TRAILING);

        actionLabel2 = new JLabel("invoke");
        actionLabel2.setText("");
        actionLabel2.setPreferredSize(dx);
        actionLabel2.setHorizontalAlignment(JLabel.LEADING);

        JLabel localInvokeLabel = new JLabel("local invocation");
        JLabel localVariableMethodLabel = new JLabel("invoke local variable method");
        JLabel stateTransitionInvokeLabel = new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "." + OneLinePanel.CLOSE_LABEL_BOLD);

        stateVariablesCB = new JComboBox<>();
        setMaxHeight(stateVariablesCB);
        stateVariablesCB.setBackground(Color.white);
        newStateVariableButton = new JButton("new");
        descriptionTF = new JTextField(25);
        setMaxHeight(descriptionTF);
        actionTF = new JTextField(15);
        actionTF.setToolTipText("Use this field to provide an assignment value or a method argument"); // 
        setMaxHeight(actionTF);
        arrayIndexTF = new JTextField(5);
        setMaxHeight(arrayIndexTF);
        localAssignmentTF = new JTextField(15);
        localAssignmentTF.setToolTipText("Use this field to optionally assign a return type, from a state variable to an already declared local variable");
        setMaxHeight(localAssignmentTF);
        localInvocationTF = new JTextField(15);
        localInvocationTF.setToolTipText("Use this field to optionally invoke a zero parameter void method call to an already-declared local variable, or an argument");
        setMaxHeight(localInvocationTF);

        int width = OneLinePanel.maxWidth(new JComponent[]{descriptionLabel, localAssignmentLabel,
            nameLabel, indexVariableLabel, assignToRB, invokeOnRB, stateTransitionInvokeLabel,
            localInvokeLabel, localVariableMethodLabel});

        fieldsPanel.add(new OneLinePanel(descriptionLabel, width, descriptionTF));
        fieldsPanel.add(Box.createVerticalStrut(10));

        JSeparator divider = new JSeparator(JSeparator.HORIZONTAL);
        divider.setBackground(Color.blue.brighter());

        fieldsPanel.add(divider);
        fieldsPanel.add(localAssignmentPanel = new OneLinePanel(localAssignmentLabel,
                width,
                localAssignmentTF,
                new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "=" + OneLinePanel.CLOSE_LABEL_BOLD)));
        fieldsPanel.add(Box.createVerticalStrut(10));
        fieldsPanel.add(new OneLinePanel(nameLabel, width, stateVariablesCB, newStateVariableButton));
        fieldsPanel.add(indexPanel = new OneLinePanel(indexVariableLabel, width, arrayIndexTF));
        fieldsPanel.add(new OneLinePanel(new JLabel(""), width, assignToRB));
        fieldsPanel.add(new OneLinePanel(new JLabel(""), width, invokeOnRB));

        stateTransitionMethodsCB = new JComboBox<>();
        stateTransitionMethodsCB.setToolTipText("Select methods to invoke on state variables");
        setMaxHeight(stateTransitionMethodsCB);
        stateTransitionMethodsCB.setBackground(Color.white);

        fieldsPanel.add(stateTransInvokePanel = new OneLinePanel(stateTransitionInvokeLabel, width, stateTransitionMethodsCB));
        fieldsPanel.add(new OneLinePanel(actionLabel1, width, actionTF, actionLabel2));

        divider = new JSeparator(JSeparator.HORIZONTAL);
        divider.setBackground(Color.blue.brighter());

        fieldsPanel.add(divider);
        fieldsPanel.add(Box.createVerticalStrut(10));

        localVariableMethodsCB = new JComboBox<>();
        localVariableMethodsCB.setToolTipText("Select void return type methods for locally declared variables, or arguments");
        setMaxHeight(localVariableMethodsCB);
        localVariableMethodsCB.setBackground(Color.white);

        fieldsPanel.add(new OneLinePanel(
                localInvokeLabel,
                width,
                localInvocationTF,
                localInvokeDot = new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "." + OneLinePanel.CLOSE_LABEL_BOLD)));
        fieldsPanel.add(localInvocationPanel = new OneLinePanel(localVariableMethodLabel, width, localVariableMethodsCB));

        content.add(fieldsPanel);
        content.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            okButton = new JButton("Apply changes");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(    okButton);
        buttonPanel.add(cancelButton);

        content.add(buttonPanel);
        content.add(Box.createVerticalGlue());    // takes up space when dialog is expanded vertically

        // attach listeners
            okButton.addActionListener(new  ApplyButtonListener());
        cancelButton.addActionListener(new CancelButtonListener());

        EnableApplyButtonListener enableApplyButtonListener = new EnableApplyButtonListener();
        descriptionTF.addCaretListener(enableApplyButtonListener);
        localAssignmentTF.addCaretListener(enableApplyButtonListener);
        arrayIndexTF.addCaretListener(enableApplyButtonListener);
        actionTF.addCaretListener(enableApplyButtonListener);
        localInvocationTF.addCaretListener(enableApplyButtonListener);

        stateVariablesCB.addActionListener(new ActionListener()
		{
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JComboBox cb = (JComboBox) actionEvent.getSource();
                ViskitStateVariable stateVariable = (ViskitStateVariable) cb.getSelectedItem();
                descriptionTF.setText(stateVariable.getDescription());
                okButton.setEnabled(true);
                indexPanel.setVisible(ViskitGlobals.instance().isArray(stateVariable.getType()));
                modified = true;
                pack();
            }
        });
        newStateVariableButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String name = ViskitGlobals.instance().getEventGraphViewFrame().addStateVariableDialog();
                if (name != null)
				{
                    stateVariablesCB.setModel(ViskitGlobals.instance().getStateVariablesCBModel());
                    for (int i = 0; i < stateVariablesCB.getItemCount(); i++)
					{
                        ViskitStateVariable vsv = (ViskitStateVariable) stateVariablesCB.getItemAt(i);
                        if (vsv.getName().contains(name))
						{
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
        if (stateVariablesCB.getItemCount() > 0)
		{
            descriptionTF.setText(((ViskitStateVariable) stateVariablesCB.getItemAt(0)).getDescription());
        } 
		else 
		{
            descriptionTF.setText(ViskitStatics.DEFAULT_DESCRIPTION);
        }

        RadioButtonListener radioButtonListener = new RadioButtonListener();
        assignToRB.addActionListener(radioButtonListener);
        invokeOnRB.addActionListener(radioButtonListener);

        setParameters(parent, eventStateTransition);
    }

    private void setMaxHeight(JComponent c) {
        Dimension d = c.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        c.setMaximumSize(d);
    }

    private ComboBoxModel<String> resolveStateTransitionMethodCalls() {
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

    private ComboBoxModel<String> resolveLocalMethodCalls()
	{
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
                ((EventGraphControllerImpl) ViskitGlobals.instance().getEventGraphController()).messageToUser(
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

    public final void setParameters(Component c, EventStateTransition pEventStateTransition)
	{
        allGood = true;
        this.eventStateTransition = pEventStateTransition;

        fillWidgets();

        if (!allGood) {return;}

        modified =         (eventStateTransition == null);
        okButton.setEnabled(eventStateTransition == null);

        getRootPane().setDefaultButton(cancelButton);
        pack(); // do this prior to next
        setLocationRelativeTo(c);
    }

    private void fillWidgets()
	{
        String indexArgument = "";

        // Conceptually, should only be one indexing argument
        if (!argumentsPanel.isEmpty())
		{
            for (ViskitElement argument : argumentsPanel.getData())
			{
                indexArgument = argument.getName();
                break;
            }
        }

        if (eventStateTransition != null)
		{
            if (stateVariablesCB.getItemCount() > 0) 
			{
                descriptionTF.setText(((ViskitStateVariable) stateVariablesCB.getSelectedItem()).getDescription());
            } else 
			{
                descriptionTF.setText(ViskitStatics.DEFAULT_DESCRIPTION);
            }
            localAssignmentTF.setText(eventStateTransition.getLocalVariableAssignment());
            setStateVariableCBValue(eventStateTransition);
            String indexingExpression = eventStateTransition.getIndexingExpression();
            if (indexingExpression == null || indexingExpression.isEmpty())
			{
                arrayIndexTF.setText(indexArgument);
            } 
			else 
			{
                arrayIndexTF.setText(indexingExpression);
            }
            boolean isOperation = eventStateTransition.isOperation();
            if (isOperation)
			{
                invokeOnRB.setSelected(isOperation);
                invokeOnRB.setEnabled( isOperation);
                assignToRB.setEnabled(!isOperation);
                actionLabel1.setText("(");
                actionLabel2.setText(" )");

                // Strip out the argument from the method name and its
                // parentheses
                String operation = eventStateTransition.getOperationOrAssignment();
                operation = operation.substring(operation.indexOf("("), operation.length());
                operation = operation.replace("(", "");
                operation = operation.replace(")", "");
                actionTF.setText(operation);
            } 
			else 
			{
                assignToRB.setSelected(!isOperation);
                assignToRB.setEnabled(!isOperation);
                invokeOnRB.setEnabled( isOperation);
                actionLabel1.setText("=");
                actionLabel2.setText("");
                actionTF.setText(eventStateTransition.getOperationOrAssignment());
            }
            setStateTransitionMethodsCBValue(eventStateTransition);

            // We only need the variable, not the method invocation
            String localVariableInvocation = eventStateTransition.getLocalVariableInvocation();
            if (localVariableInvocation != null && !localVariableInvocation.isEmpty())
                localInvocationTF.setText(localVariableInvocation.split("\\.")[0]);

            setLocalVariableCBValue(eventStateTransition);
        } 
		else // eventStateTransition == null
		{
                descriptionTF.setText("");
            localAssignmentTF.setText("");
             stateVariablesCB.setSelectedIndex(0);
                 arrayIndexTF.setText(indexArgument);
     stateTransitionMethodsCB.setSelectedIndex(0);
                     actionTF.setText("");
                   assignToRB.setSelected(true);
            localInvocationTF.setText("");
       localVariableMethodsCB.setSelectedIndex(0);
        }

        // We have an indexing argument already set
        String typeName = ((ViskitStateVariable) stateVariablesCB.getSelectedItem()).getType();
        indexPanel.setVisible(ViskitGlobals.instance().isArray(typeName));
        localAssignmentPanel.setVisible(invokeOnRB.isSelected());
    }

    private void setStateVariableCBValue(EventStateTransition est) {
        stateVariablesCB.setModel(ViskitGlobals.instance().getStateVariablesCBModel());
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
//            ViskitGlobals.instance().getEventGraphController().messageToUser(
//                    JOptionPane.ERROR_MESSAGE,
//                    "Alert",
//                    "State variable " + est.getStateVarName() + "not found.");
//        }
    }

    private void setStateTransitionMethodsCBValue(EventStateTransition est) {
        stateTransitionMethodsCB.setModel(resolveStateTransitionMethodCalls());
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

    private void unloadWidgets()
	{
        if (eventStateTransition != null)
		{
            eventStateTransition.setDescription(descriptionTF.getText().trim());
            eventStateTransition.setLocalVariableAssignment(localAssignmentTF.getText().trim());
            eventStateTransition.setName(((ViskitStateVariable) stateVariablesCB.getSelectedItem()).getName());
            eventStateTransition.setType(((ViskitStateVariable) stateVariablesCB.getSelectedItem()).getType());
            eventStateTransition.setIndexingExpression(arrayIndexTF.getText().trim());

            String arg = actionTF.getText().trim();
            if (invokeOnRB.isSelected()) {
                String methodCall = (String) stateTransitionMethodsCB.getSelectedItem();

                // Insert the argument
                if (!arg.isEmpty()) {
                    methodCall = methodCall.substring(0, methodCall.indexOf("(") + 1);
                    methodCall += arg + ")";
                }

                eventStateTransition.setOperationOrAssignment(methodCall);
            } else {
                eventStateTransition.setOperationOrAssignment(arg);
            }
            eventStateTransition.setOperation(invokeOnRB.isSelected());

            if (!localInvocationTF.getText().isEmpty())
                eventStateTransition.setLocalVariableInvocation(localInvocationTF.getText().trim()
                        + "."
                        + (String) localVariableMethodsCB.getSelectedItem());
        }
    }

    class CancelButtonListener implements ActionListener {

        // bugfix 1183
        @Override
        public void actionPerformed(ActionEvent event) {
            modified = false;    // for the caller
            localAssignmentTF.setText("");
                 arrayIndexTF.setText("");
            actionTF.setText("");
            localInvocationTF.setText("");
            ViskitGlobals.instance().getActiveEventGraphModel().resetIndexVariableNameGenerator();
            dispose();
        }
    }

    class RadioButtonListener implements ActionListener 
	{
        @Override
        public void actionPerformed(ActionEvent e) 
		{
            modified = true;
            okButton.setEnabled(true);
            String typeName = ((ViskitStateVariable) stateVariablesCB.getSelectedItem()).getType();
			invokeOnRB.setEnabled(!ViskitGlobals.instance().isPrimitive(typeName));

            Dimension d = actionLabel1.getPreferredSize();
            if (assignToRB.isSelected())
			{
                actionLabel1.setText("=");
                actionLabel2.setText("");
            }
			else if (invokeOnRB.isSelected())
			{
                if (ViskitGlobals.instance().isPrimitive(typeName))
				{
                    ViskitGlobals.instance().getEventGraphController().messageToUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Java Language Error",
                            "A method cannot be invoked on a primitive type.");
                    assignToRB.setSelected(true);
                } else
				{
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

    class ApplyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (modified)
			{
                // check for array index
                String typeName = ((ViskitStateVariable) stateVariablesCB.getSelectedItem()).getType();
                if (ViskitGlobals.instance().isArray(typeName))
				{
                    if (arrayIndexTF.getText().trim().isEmpty())
					{
                        int returnValue = JOptionPane.showConfirmDialog(EventStateTransitionDialog.this,
                                "Using a state variable which is an array" +
                                "\nrequires an indexing expression.\nIgnore and continue?",
                                "Warning",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        if (returnValue != JOptionPane.YES_OPTION)
						{
                            return;
                        }
                    }
                }
                unloadWidgets();
            }
            dispose();
        }
    }

    class EnableApplyButtonListener implements CaretListener, ActionListener {

        @Override
        public void caretUpdate(CaretEvent event) {
            localInvocationPanel.setVisible(!localInvocationTF.getText().isEmpty());
            localInvokeDot.setEnabled(!localInvocationTF.getText().isEmpty());
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

    class MyCloseListener extends WindowAdapter
	{
        @Override
        public void windowClosing(WindowEvent e)
		{
            if (modified)
			{
                int returnValue = JOptionPane.showConfirmDialog(EventStateTransitionDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (returnValue == JOptionPane.YES_OPTION)
				{
                    okButton.doClick();
                }
				else
				{
                    cancelButton.doClick();
                }
            } else
			{
                cancelButton.doClick();
            }
        }
    }
}
