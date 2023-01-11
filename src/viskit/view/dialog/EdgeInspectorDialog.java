package viskit.view.dialog;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.regex.Pattern;
import java.util.Vector;
import java.util.ArrayList;

import edu.nps.util.BoxLayoutUtils;
import edu.nps.util.LogUtilities;
import java.lang.reflect.Method;
import java.util.Collections;
import org.apache.logging.log4j.Logger;
import simkit.Priority;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.control.EventGraphControllerImpl;
import viskit.model.Edge;
import viskit.model.EventLocalVariable;
import viskit.model.SchedulingEdge;
import viskit.model.ViskitElement;
import viskit.model.ViskitEdgeParameter;
import viskit.view.ConditionalExpressionPanel;
import viskit.view.EdgeParametersPanel;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 2:56:21 PM
 * @version $Id$
 */
public class EdgeInspectorDialog extends JDialog 
{
    static final Logger LOG = LogUtilities.getLogger(EdgeInspectorDialog.class);

    private static EdgeInspectorDialog dialog;
    private static boolean modified = false;
    private static boolean allGood;
    private Edge edge;
    private boolean schedulingType = true; // true = scheduling, false = cancelling
    private final JButton cancelButton, okButton;
    private final JLabel sourceEvent, targetEvent;
    private EdgeParametersPanel parameters;
    private final ConditionalExpressionPanel conditionalExpressionPanel;
    private final JPanel timeDelayPanel, priorityPanel, myParametersPanel;
    private final Border delayPanelBorder, delayPanelDisabledBorder;
    private final JComboBox<String> priorityCB, timeDelayMethodsCB;
    private final JComboBox<ViskitElement> timeDelayVariablesCB;
    private java.util.List<Priority> priorityList;  // matches combo box
    private Vector<String> priorityNames;
    private int priorityDefaultIndex = 3;      // set properly below
    private final JLabel schedulingLabel, cancellingLabel;
    private final JButton addConditionalButton, addDescriptionButton;
    private JTextArea descriptionTextArea;
    private final JScrollPane descriptionScrollPane;
    private JLabel dotLabel;

    /**
     * Set up and show the dialog.  The first Component argument
 determines which frame the dialog depends on; it should be
 a component in the dialog's controlling frame. The second
 Component argument should be null if you want the dialog
 toEventNode come up with its left corner in the center of the screen;
 otherwise, it should be the component on top of which the
 dialog should appear.
     * @param f the frame toEventNode orient this dialog
     * @param edge the Edge node toEventNode edit
     * @return an indication of success
     */
    public static boolean showDialog(JFrame f, Edge edge) {

        // New ones every time so that each EG has it's own time delay vars
        dialog = new EdgeInspectorDialog(f, edge);

        if (allGood)
            dialog.setVisible(true);
            // above call blocks
        else
            modified = false;
        return modified;
    }

    private EdgeInspectorDialog(JFrame parent, Edge edge) {
        super(parent, "Edge Inspector", true);
        this.edge = edge;

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        Container cont = getContentPane();
        cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));

        JPanel edgeInspectorPanel = new JPanel();
        edgeInspectorPanel.setLayout(new BoxLayout(edgeInspectorPanel, BoxLayout.Y_AXIS));
        edgeInspectorPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        edgeInspectorPanel.add(Box.createVerticalStrut(5));

        // edge type
        JPanel typePanel = new JPanel();
        typePanel.setLayout(new BoxLayout(typePanel, BoxLayout.X_AXIS));
        typePanel.add(Box.createHorizontalGlue());
        JLabel typeLabel = new JLabel("Type: ");
        BoxLayoutUtils.clampWidth(typeLabel);
        typePanel.add(typeLabel);
        typePanel.add(Box.createHorizontalStrut(15));
        schedulingLabel = new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "Scheduling" + OneLinePanel.CLOSE_LABEL_BOLD);
        BoxLayoutUtils.clampWidth(schedulingLabel);
        cancellingLabel = new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "Cancelling" + OneLinePanel.CLOSE_LABEL_BOLD);
        BoxLayoutUtils.clampWidth(cancellingLabel);
        typePanel.add(schedulingLabel);
        typePanel.add(cancellingLabel);
        typePanel.add(Box.createHorizontalGlue());

        BoxLayoutUtils.clampHeight(typePanel);
        edgeInspectorPanel.add(typePanel);
        edgeInspectorPanel.add(Box.createVerticalStrut(5));

        JPanel sourceTargetPanel = new JPanel();
        sourceTargetPanel.setLayout(new BoxLayout(sourceTargetPanel, BoxLayout.X_AXIS));
        sourceTargetPanel.add(Box.createHorizontalGlue());
        JPanel sourceTargetNamesPanel = new JPanel();
        sourceTargetNamesPanel.setLayout(new BoxLayout(sourceTargetNamesPanel, BoxLayout.Y_AXIS));
        JLabel sourceEventLabel = new JLabel("Source event:");
        sourceEventLabel.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
        sourceTargetNamesPanel.add(sourceEventLabel);
        JLabel targetEventLabel = new JLabel("Target event:");
        targetEventLabel.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
        sourceTargetNamesPanel.add(targetEventLabel);
        sourceTargetPanel.add(sourceTargetNamesPanel);
        sourceTargetPanel.add(Box.createHorizontalStrut(5));
        JPanel sourceTargetValuesPanel = new JPanel();
        sourceTargetValuesPanel.setLayout(new BoxLayout(sourceTargetValuesPanel, BoxLayout.Y_AXIS));
        sourceEvent = new JLabel("srcEvent");
        sourceEvent.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        sourceTargetValuesPanel.add(sourceEvent);
        targetEvent = new JLabel("targEvent");
        targetEvent.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        sourceTargetValuesPanel.add(targetEvent);
        sourceTargetValuesPanel.setBorder(BorderFactory.createTitledBorder(""));
        sourceTargetValuesPanel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        keepSameSize(sourceEvent, targetEvent);
        sourceTargetPanel.add(sourceTargetValuesPanel);
        sourceTargetPanel.add(Box.createHorizontalGlue());
        BoxLayoutUtils.clampHeight(sourceTargetPanel);

        edgeInspectorPanel.add(sourceTargetPanel);
        edgeInspectorPanel.add(Box.createVerticalStrut(5));

        descriptionTextArea = new JTextArea(2, 25);
        descriptionScrollPane = new JScrollPane(descriptionTextArea);
        descriptionScrollPane.setBorder(new CompoundBorder(
                new EmptyBorder(0, 0, 5, 0),
                BorderFactory.createTitledBorder("description")));
        edgeInspectorPanel.add(descriptionScrollPane);

        Dimension descriptionJspDimension = descriptionScrollPane.getPreferredSize();
        descriptionScrollPane.setMinimumSize(descriptionJspDimension);

        descriptionTextArea.addCaretListener(new CaretListener() {

            @Override
            public void caretUpdate(CaretEvent e) {
                if (changeListener != null) {
                    changeListener.stateChanged(new ChangeEvent(descriptionTextArea));
                }
            }
        });

        conditionalExpressionPanel = new ConditionalExpressionPanel(edge, schedulingType);
        edgeInspectorPanel.add(conditionalExpressionPanel);

        priorityPanel = new JPanel();
        priorityPanel.setLayout(new BoxLayout(priorityPanel, BoxLayout.X_AXIS));
        priorityPanel.setBorder(new CompoundBorder(
                new EmptyBorder(0, 0, 5, 0),
                BorderFactory.createTitledBorder("Priority")));
        priorityCB = buildPriorityComboBox();
        priorityPanel.add(Box.createHorizontalStrut(50));
        priorityPanel.add(priorityCB);
        priorityPanel.add(Box.createHorizontalStrut(50));
        edgeInspectorPanel.add(priorityPanel);
        BoxLayoutUtils.clampHeight(priorityPanel);

        timeDelayPanel = new JPanel();
        timeDelayPanel.add(Box.createHorizontalStrut(25));
        timeDelayVariablesCB = buildTimeDelayVarsComboBox();
        timeDelayVariablesCB.setToolTipText("Select a simulation parameter, event "
                + "node argument or local variable for method invocation, or "
                + "leave blank for a zero time delay");
        timeDelayPanel.add(new OneLinePanel(
                null,
                0,
                timeDelayVariablesCB,
                dotLabel = new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "." + OneLinePanel.CLOSE_LABEL_BOLD)));
        timeDelayPanel.setBorder(BorderFactory.createTitledBorder("Time Delay"));
        delayPanelBorder = timeDelayPanel.getBorder();
        delayPanelDisabledBorder = BorderFactory.createTitledBorder(
                new LineBorder(Color.gray),
                "Time Delay",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                null,
                Color.gray);
        timeDelayMethodsCB = buildTimeDelayMethodsCB();

        // Can happen of an unqualified name was used toEventNode create a parameter
        if (timeDelayMethodsCB != null) {
            timeDelayMethodsCB.setToolTipText("Select an invocable method, or type "
                    + "in floating point delay value");
            timeDelayPanel.add(new OneLinePanel(null, 0, timeDelayMethodsCB));
            timeDelayPanel.add(Box.createHorizontalStrut(25));

            // NOTE: Apply and Cancel buttons are squished if we don't do this
            BoxLayoutUtils.clampHeight(timeDelayPanel);

            edgeInspectorPanel.add(timeDelayPanel);
        }
        edgeInspectorPanel.add(Box.createVerticalStrut(5));

        myParametersPanel = new JPanel();
        myParametersPanel.setLayout(new BoxLayout(myParametersPanel, BoxLayout.Y_AXIS));

        parameters = new EdgeParametersPanel(300);
        JScrollPane paramSp = new JScrollPane(parameters);
        paramSp.setBorder(null);

        myParametersPanel.add(paramSp);

        edgeInspectorPanel.add(myParametersPanel);

        JPanel twoRowButtonPanel = new JPanel();
        twoRowButtonPanel.setLayout(new BoxLayout(twoRowButtonPanel, BoxLayout.Y_AXIS));

        JPanel addButtonPanel = new JPanel();
        addButtonPanel.setLayout(new BoxLayout(addButtonPanel, BoxLayout.X_AXIS));
        addButtonPanel.setBorder(new TitledBorder("add"));
        addDescriptionButton = new JButton("description"); //add description");
        addConditionalButton = new JButton("condition");   //add condition");

        addButtonPanel.add(Box.createHorizontalGlue());
        addButtonPanel.add(addDescriptionButton);
        addButtonPanel.add(addConditionalButton);
        addButtonPanel.add(Box.createHorizontalGlue());
        twoRowButtonPanel.add(addButtonPanel);
        twoRowButtonPanel.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            okButton = new JButton("Apply changes");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        twoRowButtonPanel.add(buttonPanel);

        edgeInspectorPanel.add(twoRowButtonPanel);
        cont.add(edgeInspectorPanel);

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        final myChangeListener chlis = new myChangeListener();
        descriptionTextArea.addKeyListener(chlis);
        conditionalExpressionPanel.addChangeListener(chlis);
        priorityCB.addActionListener(chlis);
        timeDelayVariablesCB.addActionListener(chlis);

        if (timeDelayMethodsCB != null) {
            timeDelayMethodsCB.addActionListener(chlis);
            timeDelayMethodsCB.getEditor().getEditorComponent().addKeyListener(chlis);
        }

        priorityCB.getEditor().getEditorComponent().addKeyListener(chlis);
        timeDelayVariablesCB.getEditor().getEditorComponent().addKeyListener(chlis);

        addHideButtonListener hideList = new addHideButtonListener();
        addConditionalButton.addActionListener(hideList);
//        addDescriptionButton.addActionListener(hideList);

        parameters.addDoubleClickedListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                ViskitEdgeParameter ep = (ViskitEdgeParameter) event.getSource();

                boolean wasModified = EdgeParameterDialog.showDialog(EdgeInspectorDialog.this, ep);
                if (wasModified) {
                    parameters.updateRow(ep);
                    chlis.actionPerformed(event);
                }
            }
        });

        setParams(parent, edge);
    }

    public final void setParams(Component c, Edge e) {
        allGood = true;

        edge = e;

        fillWidgets();

        if (!allGood) {return;}

        modified = false;
        okButton.setEnabled(false);

        getRootPane().setDefaultButton(cancelButton);
        pack();
        setLocationRelativeTo(c);
    }

    private void keepSameSize(JComponent a, JComponent b) {
        Dimension ad = a.getPreferredSize();
        Dimension bd = b.getPreferredSize();
        Dimension d = new Dimension(Math.max(ad.width, bd.width), Math.max(ad.height, bd.height));
        a.setMinimumSize(d);
        b.setMinimumSize(d);
    }

    private JComboBox<String> buildPriorityComboBox() {
        priorityNames = new Vector<>(10);
        JComboBox<String> jcb = new JComboBox<>(priorityNames);
        priorityList = new ArrayList<>(10);
        try {
            Class<?> c = ViskitStatics.ClassForName("simkit.Priority");
            Field[] fa = c.getDeclaredFields();
            for (Field f : fa) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType().equals(c)) {
                    priorityNames.add(f.getName());
                    priorityList.add((Priority) f.get(null)); // static objects
                    if (f.getName().equalsIgnoreCase("default")) {
                        priorityDefaultIndex = priorityNames.size() - 1;
                    } // save the default one
                }
            }
            jcb.setEditable(true); // this allows anything toEventNode be entered
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
            LogUtilities.getLogger(EdgeInspectorDialog.class).error(e);
        }
        return jcb;
    }

    /** Populates the time delay combo box with parametersList, local vars and event
 node arguments fromventNode the node that is the source of the current edge
     *
     * @return a time delay combo box populated with parametersList, local vars
 and event node arguments
     */
    private JComboBox<ViskitElement> buildTimeDelayVarsComboBox() {
        JComboBox<ViskitElement> cb = new JComboBox<>();

        ComboBoxModel<ViskitElement> m = ViskitGlobals.instance().getSimParamsCBModel();

        // First item should be empty toEventNode allow for default zero delay
        ((DefaultComboBoxModel<ViskitElement>)m).insertElementAt(new EventLocalVariable("", "", ""), 0);
        cb.setModel(m);

        java.util.List<ViskitElement> vars = new ArrayList<>(edge.fromEventNode.getLocalVariables());
        vars.addAll(edge.fromEventNode.getArguments());

        for (ViskitElement e : vars) {
            ((DefaultComboBoxModel<ViskitElement>)m).addElement(e);
        }

        return cb;
    }

    private JComboBox<String> buildTimeDelayMethodsCB() {
        Class<?> type;
        Method[] methods;
        String typ;
        Vector<String> methodNames = new Vector<>();

        java.util.List<ViskitElement> types = new ArrayList<>(edge.fromEventNode.getLocalVariables());
        types.addAll(edge.fromEventNode.getArguments());
        types.addAll(ViskitGlobals.instance().getSimParametersList());

        String className;
        for (ViskitElement e : types) {
            typ = e.getType();

            if (ViskitGlobals.instance().isGeneric(typ)) {
                typ = typ.substring(0, typ.indexOf("<"));
            }
            if (ViskitGlobals.instance().isArray(typ)) {
                typ = typ.substring(0, typ.indexOf("["));
            }
            type = ViskitStatics.ClassForName(typ);

            if (type == null) {
                ((EventGraphControllerImpl) ViskitGlobals.instance().getEventGraphController()).messageToUser(
                        JOptionPane.WARNING_MESSAGE,
                        typ + " not found on the Classpath",
                        "Please make sure you are using fully qualified java "
                                + "names when referencing a parameter");
                return null;
            }
            methods = type.getMethods();

            // Filter out methods of Object and any
            // methods requiring parametersList
            for (Method method : methods) {
                className = method.getDeclaringClass().getName();
                if (className.contains(ViskitStatics.JAVA_LANG_OBJECT)) {continue;}
                if (method.getParameterCount() > 0) {continue;}

                if (!methodNames.contains(method.getName() + "()"))
                    methodNames.add(method.getName() + "()");
            }
        }

        Collections.sort(methodNames);
        ComboBoxModel<String> m = new DefaultComboBoxModel<>(methodNames);
        JComboBox<String> cb = new JComboBox<>();

        // Allow user toEventNode edit the selection
        cb.setEditable(true);

        // First item should be empty
        ((DefaultComboBoxModel<String>)m).insertElementAt("", 0);
        cb.setModel(m);
        return cb;
    }

    private void setPriorityCBValue(String pr) {

        // Assume numeric comes in, avoid NumberFormatException via Regex check
        if (Pattern.matches(SchedulingEdge.FLOATING_POINT_REGEX, pr)) {

            double prd = Double.parseDouble(pr);
            for (Priority p : priorityList) {
                int cmp = Double.compare(p.getPriority(), prd);
                if (cmp == 0) {
                    priorityCB.setSelectedIndex(priorityList.indexOf(p));
                    return;
                }
            }
            // Must have been an odd one, but we know it's a good double
            priorityCB.setSelectedItem(pr);
        } else {
            // First try toEventNode find it in the list
            int i = 0;
            for (String s : priorityNames) {
                if (s.equalsIgnoreCase(pr)) {
                    priorityCB.setSelectedIndex(i);
                    return;
                }
                i++;
            }

            LogUtilities.getLogger(EdgeInspectorDialog.class).error("Unknown edge priority: " + pr + " -- setting to DEFAULT)");
            priorityCB.setSelectedIndex(priorityDefaultIndex);
        }
    }

    private void setTimeDelayVarsCBValue(String value) {

        if (timeDelayVariablesCB.getItemCount() <= 0) {return;}

        // Default
        timeDelayVariablesCB.setSelectedIndex(0);

        for (int i = 0; i < timeDelayVariablesCB.getItemCount(); i++) {
            ViskitElement e = timeDelayVariablesCB.getItemAt(i);
            if (e.getName().contains(value)) {
                timeDelayVariablesCB.setSelectedIndex(i);
                return;
            }
        }
    }

    private void setTimeDelayMethodsCBValue(String value) {

        if (timeDelayMethodsCB == null || timeDelayMethodsCB.getItemCount() <= 0) {return;}

        // Default
        timeDelayMethodsCB.setSelectedItem(value);

        // Set the ComboBox width toEventNode accomodate the string length
        timeDelayMethodsCB.setPrototypeDisplayValue((String) timeDelayMethodsCB.getSelectedItem());

        for (int i = 0; i < timeDelayMethodsCB.getItemCount(); i++) {
            String s = timeDelayMethodsCB.getItemAt(i);
            if (value.equals(s)) {
                timeDelayMethodsCB.setSelectedIndex(i);

                // Set the ComboBox width toEventNode accomodate the string length
                timeDelayMethodsCB.setPrototypeDisplayValue((String) timeDelayMethodsCB.getSelectedItem());
                return;
            }
        }
    }

    private void fillWidgets() {

        sourceEvent.setText(edge.fromEventNode.getName());
        targetEvent.setText(edge.toEventNode.getName());

        if (edge.toEventNode.getArguments() != null || !edge.toEventNode.getArguments().isEmpty()) {
            parameters.setArgumentList(edge.toEventNode.getArguments());
            parameters.setData(edge.parametersList);
            myParametersPanel.setBorder(new CompoundBorder(
                    new EmptyBorder(0, 0, 5, 0),
                    BorderFactory.createTitledBorder("Edge Parameters passed to " + targetEvent.getText())));
        }

        if (edge instanceof SchedulingEdge) {
            myParametersPanel.setVisible(true);

            // Prepare default selections
            timeDelayVariablesCB.setEnabled(timeDelayVariablesCB.getItemCount() > 0);
            setTimeDelayVarsCBValue("");

            // A zero delay will be the default
            setTimeDelayMethodsCBValue("0.0");

            // We always want this enabled toEventNode be able toEventNode enter manual delay
            // values
            if (timeDelayMethodsCB != null)
                timeDelayMethodsCB.setEnabled(true);
            else
                allGood = false;

            if (edge.delay != null && !edge.delay.trim().isEmpty()) {

                String[] s = edge.delay.split("\\.");
                if (s.length == 1) {
                    setTimeDelayMethodsCBValue(s[0]);
                } else if (s.length == 2) {
                    if (!Character.isDigit(s[0].charAt(0))) {
                        setTimeDelayVarsCBValue(s[0]);
                        setTimeDelayMethodsCBValue(s[1]);
                    } else {
                        setTimeDelayMethodsCBValue(edge.delay);
                    }
                }
            }

            timeDelayPanel.setBorder(delayPanelBorder);

            setPriorityCBValue(((SchedulingEdge) edge).priority);

        } else {

            myParametersPanel.setVisible(false);

            timeDelayVariablesCB.setEnabled(false);
            dotLabel.setVisible(false);

            if (timeDelayMethodsCB != null)
                timeDelayMethodsCB.setEnabled(false);
            else
                allGood = false;

            timeDelayPanel.setBorder(delayPanelDisabledBorder);
        }

        if (edge.condition == null || edge.condition.trim().isEmpty()) {
            conditionalExpressionPanel.setText("");
            hideShowConditionals(false);
        } else {
            conditionalExpressionPanel.setText(edge.condition);
            hideShowConditionals(true);
        }

        if (edge.conditionDescription == null || edge.conditionDescription.isEmpty()) {
            setDescription("");
            hideShowDescription(true); // nagging is better than ignoring
        } else {
            setDescription(edge.conditionDescription);
            hideShowDescription(true);
        }

        setSchedulingType(edge instanceof SchedulingEdge);
    }

    private void unloadWidgets() {
        if (edge instanceof SchedulingEdge) {
            int idx = priorityCB.getSelectedIndex();
            String s;
            if (idx < 0) {
                s = (String) priorityCB.getSelectedItem();
                if (s.isEmpty()) {

                    // Force default in this case (no information provided in EG)
                    s = "DEFAULT";
                } else {
                    if (s.contains("-3")) {
                        s = "LOWEST";
                    } else if (s.contains("-2")) {
                        s = "LOWER";
                    } else if (s.contains("-1")) {
                        s = "LOW";
                    } else if (s.contains("1")) {
                        s = "HIGH";
                    } else if (s.contains("2")) {
                        s = "HIGHER";
                    } else if (s.contains("3")) {
                        s = "HIGHEST";
                    } else {
                        s = "DEFAULT";
                    }
                }
            } else {
               Priority p = priorityList.get(idx);

                // Get the name of the Priority in this manner
                s = p.toString().split("[\\ \\[]") [1];
            }

            ((SchedulingEdge) edge).priority = s;
        }

        String delaySt = ((ViskitElement) timeDelayVariablesCB.getSelectedItem()).getName();
        if (delaySt == null || delaySt.trim().isEmpty())
            delaySt = (String) timeDelayMethodsCB.getSelectedItem();
        else
            delaySt += ("." + (String) timeDelayMethodsCB.getSelectedItem());

        edge.delay = delaySt;

        String condSt = conditionalExpressionPanel.getText();
        edge.condition = (condSt == null || condSt.trim().isEmpty()) ? null : conditionalExpressionPanel.getText();

        edge.conditionDescription = getDescription();
        if (!edge.parametersList.isEmpty()) {
            edge.parametersList.clear();
        }

        // Key on the EdgeNode's list of potential arguments
        // TODO: How do we do this automatically fromventNode the EventInspectorDialog
        // when we remove an argument?
        if (!edge.toEventNode.getArguments().isEmpty()) {

            // Bug 1373: This is how applying changes toEventNode a scheduling edge
            // causes the correct EG XML representation when removing event
            // parametersList fromventNode a proceeding node.  This loop adds vEdgeParameters
            for (ViskitElement o : parameters.getData()) {
                edge.parametersList.add(o);
            }
        } else {
            parameters.setData(edge.parametersList);
        }
    }

    private void hideShowConditionals(final boolean show) {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                conditionalExpressionPanel.showConditions(show);
                addConditionalButton.setVisible(!show);
                pack();
            }
        };
        SwingUtilities.invokeLater(r);
    }

    private void hideShowDescription(final boolean show) {

        Runnable r = new Runnable() {

            @Override
            public void run() {
                showDescription(show);
                addDescriptionButton.setVisible(!show);
                pack();
            }
        };
        SwingUtilities.invokeLater(r);
    }

    class addHideButtonListener implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            if (e.getSource().equals(addConditionalButton)) {
                hideShowConditionals(true);
            } else if (e.getSource().equals(addDescriptionButton)) {
                hideShowDescription(true);
            }
        }
    }

    class cancelButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            modified = false;    // for the caller
            dispose();
        }
    }

    class applyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (modified) {
                unloadWidgets();
            }
            dispose();
        }
    }

    class myChangeListener extends KeyAdapter implements ChangeListener, ActionListener, CaretListener {

        @Override
        public void stateChanged(ChangeEvent event) {
            modified = true;
            okButton.setEnabled(true);
            getRootPane().setDefaultButton(okButton);
            dotLabel.setVisible(!((ViskitElement) timeDelayVariablesCB.getSelectedItem()).getName().isEmpty());

            // Set the ComboBox width toEventNode accomodate the string length
            if (timeDelayMethodsCB != null)
                timeDelayMethodsCB.setPrototypeDisplayValue((String) timeDelayMethodsCB.getSelectedItem());
            else
                allGood = false;

            pack();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            stateChanged(null);
        }

        @Override
        public void caretUpdate(CaretEvent e) {
            stateChanged(null);
        }

        @Override
        public void keyTyped(KeyEvent e) {
            stateChanged(null);
        }
    }

    private void setSchedulingType(boolean wh) {
        schedulingType = wh;
          priorityPanel.setVisible(wh);
         timeDelayPanel.setVisible(wh);
        schedulingLabel.setVisible(wh);
        cancellingLabel.setVisible(!wh);
    }

    private void showDescription(boolean wh) {
        descriptionScrollPane.setVisible(wh);
    }

    public boolean isDescriptionVisible() {
        return descriptionScrollPane.isVisible();
    }

    public void setDescription(String s) {
        descriptionTextArea.setText(s);
        modified = true;
        okButton.setEnabled(true);
    }

    public String getDescription() {
        return descriptionTextArea.getText().trim();
    }
    private ChangeListener changeListener;

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int returnValue = JOptionPane.showConfirmDialog(EdgeInspectorDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (returnValue == JOptionPane.YES_OPTION) {
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
