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
import java.lang.reflect.Method;
import java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import simkit.Priority;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import static viskit.ViskitStatics.DESCRIPTION_HINT;
import viskit.control.EventGraphController;
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
    static final Logger LOG = LogManager.getLogger();

    private static EdgeInspectorDialog edgeInspectorDialog;
    private static boolean modified = false;
    private static boolean allGood;
    private Edge edge;
    private boolean schedulingType = true; // true = scheduling, false = cancelling
    private final JButton cancelButton;
    private final JButton okButton;
    private final JLabel sourceEvent;
    private final JLabel targEetvent;
    private EdgeParametersPanel edgeParametersPanel;
    private final ConditionalExpressionPanel conditionalExpressionPanel;
    private final JPanel timeDelayPanel;
    private final JPanel priorityPanel;
    private final JPanel parameterPanel;
    private final Border delayPanelBorder;
    private final Border delayPanelDisabledBorder;
    private final JComboBox<String> priorityCB;
    private final JComboBox<String> timeDelayMethodsCB;
    private final JComboBox<ViskitElement> timeDelayVarsCB;
    private java.util.List<Priority> priorityList;  // matches combo box
    private Vector<String> priorityNames;
    private int priorityDefaultIndex = 3;      // set properly below
    private final JLabel schedulingLabel;
    private final JLabel cancellingLabel;
    private final JButton addConditionalButton;
    private final JButton addDescriptionButton;
    private JTextArea descriptionTextArea;
    private final JScrollPane descriptionJsp;
    private final JLabel dotLabel;

    /**
     * Set up and show the dialog. The first Component argument
     * determines which frame the dialog depends on; it should be
     * a component in the dialog's controlling frame. The second
     * Component argument should be null if you want the dialog
     * to come up with its left corner in the center of the screen;
     * otherwise, it should be centered on top of the main visible component.
     * 
     * @param f the frame to orient this dialog
     * @param edge the Edge node to edit
     * 
     * @return an indication of success
     */
    public static boolean showDialog(JFrame f, Edge edge) {

        // New ones every time so that each Event Graph has it's own time delay vars
        edgeInspectorDialog = new EdgeInspectorDialog(f, edge);

        if (allGood)
            edgeInspectorDialog.setVisible(true);
            // above call blocks
        else
            modified = false;
        return modified;
    }

    private EdgeInspectorDialog(JFrame parent, Edge edge) {
        super(parent, "Edge Inspector", true);
        this.edge = edge;

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new CloseListener());

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
        cancellingLabel = new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "Canceling" + OneLinePanel.CLOSE_LABEL_BOLD);
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
        targEetvent = new JLabel("targEvent");
        targEetvent.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        sourceTargetValuesPanel.add(targEetvent);
        sourceTargetValuesPanel.setBorder(BorderFactory.createTitledBorder(""));
        sourceTargetValuesPanel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        keepSameSize(sourceEvent, targEetvent);
        sourceTargetPanel.add(sourceTargetValuesPanel);
        sourceTargetPanel.add(Box.createHorizontalGlue());
        BoxLayoutUtils.clampHeight(sourceTargetPanel);

        edgeInspectorPanel.add(sourceTargetPanel);
        edgeInspectorPanel.add(Box.createVerticalStrut(5));

        descriptionTextArea = new JTextArea(2, 25);
        descriptionTextArea.setText("");
        descriptionTextArea.setToolTipText(DESCRIPTION_HINT);
        descriptionJsp = new JScrollPane(descriptionTextArea);
        descriptionJsp.setToolTipText(DESCRIPTION_HINT);
        descriptionJsp.setBorder(new CompoundBorder(
                new EmptyBorder(0, 0, 5, 0),
                BorderFactory.createTitledBorder("description")));
        edgeInspectorPanel.add(descriptionJsp);

        Dimension descriptionJspDimension = descriptionJsp.getPreferredSize();
        descriptionJsp.setMinimumSize(descriptionJspDimension);

        descriptionTextArea.addCaretListener((CaretEvent e) -> {
            if (changeListener != null) {
                changeListener.stateChanged(new ChangeEvent(descriptionTextArea));
            }
        });

        conditionalExpressionPanel = new ConditionalExpressionPanel(edge, schedulingType);
        edgeInspectorPanel.add(conditionalExpressionPanel);

        priorityPanel = new JPanel();
        priorityPanel.setLayout(new BoxLayout(priorityPanel, BoxLayout.X_AXIS));
        priorityPanel.setBorder(new CompoundBorder(
                new EmptyBorder(0, 0, 5, 0),
                BorderFactory.createTitledBorder("Priority"))
        );
        priorityCB = buildPriorityComboBox();
        priorityPanel.add(Box.createHorizontalStrut(50));
        priorityPanel.add(priorityCB);
        priorityPanel.add(Box.createHorizontalStrut(50));
        edgeInspectorPanel.add(priorityPanel);
        BoxLayoutUtils.clampHeight(priorityPanel);

        timeDelayPanel = new JPanel();
        timeDelayPanel.add(Box.createHorizontalStrut(25));
        timeDelayVarsCB = buildTimeDelayVarsComboBox();
        timeDelayVarsCB.setToolTipText("Select a simulation parameter, event "
                + "node argument or local variable for method invocation, or "
                + "leave blank for a zero time delay");
        timeDelayPanel.add(new OneLinePanel(
                null,
                0,
                timeDelayVarsCB,
                dotLabel = new JLabel(OneLinePanel.OPEN_LABEL_BOLD + "." + OneLinePanel.CLOSE_LABEL_BOLD))
        );
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

        // Can happen of an unqualified name was used to create a parameter
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

        parameterPanel = new JPanel();
        parameterPanel.setLayout(new BoxLayout(parameterPanel, BoxLayout.Y_AXIS));

        edgeParametersPanel = new EdgeParametersPanel(300);
        JScrollPane paramSp = new JScrollPane(edgeParametersPanel);
        paramSp.setBorder(null);

        parameterPanel.add(paramSp);

        edgeInspectorPanel.add(parameterPanel);

        JPanel twoRowButtonPanel = new JPanel();
        twoRowButtonPanel.setLayout(new BoxLayout(twoRowButtonPanel, BoxLayout.Y_AXIS));

        JPanel addButtonPanel = new JPanel();
        addButtonPanel.setLayout(new BoxLayout(addButtonPanel, BoxLayout.X_AXIS));
        addButtonPanel.setBorder(new TitledBorder("add"));
        addDescriptionButton = new JButton("description");
        addDescriptionButton.setToolTipText(DESCRIPTION_HINT);        
        addConditionalButton = new JButton("conditional");

        addButtonPanel.add(Box.createHorizontalGlue());
        addButtonPanel.add(addDescriptionButton);
        addButtonPanel.add(addConditionalButton);
        addButtonPanel.add(Box.createHorizontalGlue());
        twoRowButtonPanel.add(addButtonPanel);
        twoRowButtonPanel.add(Box.createVerticalStrut(5));

        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        cancelButton = new JButton("Cancel");
        okButton = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());
        buttPan.add(okButton);
        buttPan.add(cancelButton);
        twoRowButtonPanel.add(buttPan);

        edgeInspectorPanel.add(twoRowButtonPanel);
        cont.add(edgeInspectorPanel);

        // attach listeners
        cancelButton.addActionListener(new CancelButtonListener());
        okButton.addActionListener(new ApplyButtonListener());

        final EdgeInspectorDialogChangeListener chlis = new EdgeInspectorDialogChangeListener();
        descriptionTextArea.addKeyListener(chlis);
        conditionalExpressionPanel.addChangeListener(chlis);
        priorityCB.addActionListener(chlis);
        timeDelayVarsCB.addActionListener(chlis);

        if (timeDelayMethodsCB != null) {
            timeDelayMethodsCB.addActionListener(chlis);
            timeDelayMethodsCB.getEditor().getEditorComponent().addKeyListener(chlis);
        }

        priorityCB.getEditor().getEditorComponent().addKeyListener(chlis);
        timeDelayVarsCB.getEditor().getEditorComponent().addKeyListener(chlis);

        AddHideButtonListener hideList = new AddHideButtonListener();
        addConditionalButton.addActionListener(hideList);
        addDescriptionButton.addActionListener(hideList);

        edgeParametersPanel.addDoubleClickedListener((ActionEvent event) -> {
            ViskitEdgeParameter ep = (ViskitEdgeParameter) event.getSource();
            
            boolean wasModified = EdgeParameterDialog.showDialog(EdgeInspectorDialog.this, ep);
            if (wasModified) {
                edgeParametersPanel.updateRow(ep);
                chlis.actionPerformed(event);
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
            Class<?> c = ViskitStatics.classForName("simkit.Priority");
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
            jcb.setEditable(true); // this allows anything to be entered
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
            LOG.error(e);
        }
        return jcb;
    }

    /** Populates the time delay combo box with parameters, local vars and event
     * node arguments from the node that is the source of the current edge
     *
     * @return a time delay combo box populated with parameters, local vars
     * and event node arguments
     */
    private JComboBox<ViskitElement> buildTimeDelayVarsComboBox() {
        JComboBox<ViskitElement> cb = new JComboBox<>();

        ComboBoxModel<ViskitElement> m = ViskitGlobals.instance().getSimulationParametersComboBoxModel();

        // First item should be empty to allow for default zero delay
        ((MutableComboBoxModel<ViskitElement>)m).insertElementAt(new EventLocalVariable("", "", ""), 0);
        cb.setModel(m);

        java.util.List<ViskitElement> vars = new ArrayList<>(edge.getFrom().getLocalVariables());
        vars.addAll(edge.getFrom().getArguments());

        for (ViskitElement e : vars) {
            ((MutableComboBoxModel<ViskitElement>)m).addElement(e);
        }

        return cb;
    }

    private JComboBox<String> buildTimeDelayMethodsCB() {
        Class<?> type;
        Method[] methods;
        String typ;
        Vector<String> methodNames = new Vector<>();

        java.util.List<ViskitElement> types = new ArrayList<>(edge.getFrom().getLocalVariables());
        types.addAll(edge.getFrom().getArguments());
        types.addAll(ViskitGlobals.instance().getSimulationParametersList());

        String className;
        for (ViskitElement e : types) {
            typ = e.getType();

            if (ViskitGlobals.instance().isGenericType(typ)) {
                typ = typ.substring(0, typ.indexOf("<"));
            }
            if (ViskitGlobals.instance().isArray(typ)) {
                typ = typ.substring(0, typ.indexOf("["));
            }
            type = ViskitStatics.classForName(typ);

            if (type == null) {
                ViskitGlobals.instance().messageUser(
                        JOptionPane.WARNING_MESSAGE,
                        typ + " not found on the Classpath",
                        "Please make sure you are using fully qualified java "
                                + "names when referencing a parameter");
                return null;
            }
            methods = type.getMethods();

            // Filter out methods of Object and any
            // methods requiring parameters
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

        // Allow user to edit the selection
        cb.setEditable(true);

        // First item should be empty
        ((MutableComboBoxModel<String>)m).insertElementAt("", 0);
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
            // First try to find it in the list
            int i = 0;
            for (String s : priorityNames) {
                if (s.equalsIgnoreCase(pr)) {
                    priorityCB.setSelectedIndex(i);
                    return;
                }
                i++;
            }

            LOG.error("Unknown edge priority: " + pr + " -- setting to DEFAULT)");
            priorityCB.setSelectedIndex(priorityDefaultIndex);
        }
    }

    private void setTimeDelayVarsCBValue(String value) {

        if (timeDelayVarsCB.getItemCount() <= 0) {return;}

        // Default
        timeDelayVarsCB.setSelectedIndex(0);

        for (int i = 0; i < timeDelayVarsCB.getItemCount(); i++) {
            ViskitElement e = timeDelayVarsCB.getItemAt(i);
            if (e.getName().contains(value)) {
                timeDelayVarsCB.setSelectedIndex(i);
                return;
            }
        }
    }

    private void setTimeDelayMethodsCBValue(String value) {

        if (timeDelayMethodsCB == null || timeDelayMethodsCB.getItemCount() <= 0) {return;}

        // Default
        timeDelayMethodsCB.setSelectedItem(value);

        // Set the ComboBox width to accomodate the string length
        timeDelayMethodsCB.setPrototypeDisplayValue((String) timeDelayMethodsCB.getSelectedItem());

        for (int i = 0; i < timeDelayMethodsCB.getItemCount(); i++) {
            String s = timeDelayMethodsCB.getItemAt(i);
            if (value.equals(s)) {
                timeDelayMethodsCB.setSelectedIndex(i);

                // Set the ComboBox width to accomodate the string length
                timeDelayMethodsCB.setPrototypeDisplayValue((String) timeDelayMethodsCB.getSelectedItem());
                return;
            }
        }
    }

    private void fillWidgets() {

        sourceEvent.setText(edge.getFrom().getName());
        targEetvent.setText(edge.getTo().getName());

        if (edge.getTo().getArguments() != null || !edge.getTo().getArguments().isEmpty()) {
            edgeParametersPanel.setArgumentList(edge.getTo().getArguments());
            edgeParametersPanel.setData(edge.getParameters());
            parameterPanel.setBorder(new CompoundBorder(
                    new EmptyBorder(0, 0, 5, 0),
                    BorderFactory.createTitledBorder("Edge Parameters passed to " + targEetvent.getText())));
        }

        if (edge instanceof SchedulingEdge) {
            parameterPanel.setVisible(true);

            // Prepare default selections
            timeDelayVarsCB.setEnabled(timeDelayVarsCB.getItemCount() > 0);
            setTimeDelayVarsCBValue("");

            // A zero delay will be the default
            setTimeDelayMethodsCBValue("0.0");

            // We always want this enabled to be able to enter manual delay
            // values
            if (timeDelayMethodsCB != null)
                timeDelayMethodsCB.setEnabled(true);
            else
                allGood = false;

            if (edge.getDelay() != null && !edge.getDelay().isBlank()) {

                String[] s = edge.getDelay().split("\\.");
                if (s.length == 1) {
                    setTimeDelayMethodsCBValue(s[0]);
                } else if (s.length == 2) {
                    if (!Character.isDigit(s[0].charAt(0))) {
                        setTimeDelayVarsCBValue(s[0]);
                        setTimeDelayMethodsCBValue(s[1]);
                    } else {
                        setTimeDelayMethodsCBValue(edge.getDelay());
                    }
                }
            }

            timeDelayPanel.setBorder(delayPanelBorder);

            setPriorityCBValue(((SchedulingEdge) edge).priority);

        } else {

            parameterPanel.setVisible(false);

            timeDelayVarsCB.setEnabled(false);
            dotLabel.setVisible(false);

            if (timeDelayMethodsCB != null)
                timeDelayMethodsCB.setEnabled(false);
            else
                allGood = false;

            timeDelayPanel.setBorder(delayPanelDisabledBorder);
        }

        if (edge.getConditional() == null || edge.getConditional().isBlank()) {
            conditionalExpressionPanel.setText("");
            hideShowConditionals(false);
        } else {
            conditionalExpressionPanel.setText(edge.getConditional());
            hideShowConditionals(true);
        }

        if (edge.getDescription() != null && !edge.getDescription().isBlank()) {
            setDescription(edge.getDescription());
            showHideDescription(false);
        } 
        else {
            setDescription("");
            showHideDescription(true);
        }
        showHideDescription(true); // always show

        setSchedulingType(edge instanceof SchedulingEdge);
    }

    private void unloadWidgets() {
        if (edge instanceof SchedulingEdge) {
            int idx = priorityCB.getSelectedIndex();
            String s;
            if (idx < 0) {
                s = (String) priorityCB.getSelectedItem();
                if (s.isEmpty()) {

                    // Force default in this case (no information provided in Event Graph)
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

        String delaySt = ((ViskitElement) timeDelayVarsCB.getSelectedItem()).getName();
        if (delaySt == null || delaySt.isBlank())
            delaySt = (String) timeDelayMethodsCB.getSelectedItem();
        else
            delaySt += ("." + timeDelayMethodsCB.getSelectedItem());

        edge.setDelay(delaySt);

        String condSt = conditionalExpressionPanel.getText();
        edge.setConditional((condSt == null || condSt.isBlank()) ? null : conditionalExpressionPanel.getText());

        edge.setConditionalDescription(getDescription());
        if (!edge.getParameters().isEmpty()) {
             edge.getParameters().clear();
        }

        // Key on the EdgeNode's list of potential arguments
        // TODO: How do we do this automatically from the EventNodeInspectorDialog
        // when we remove an argument?
        if (!edge.getTo().getArguments().isEmpty()) {

            // Bug 1373: This is how applying changes to a scheduling edge
            // causes the correct Event Graph XML representation when removing event
            // parameters from a proceeding node.  This loop adds vEdgeParameters
            for (ViskitElement element : edgeParametersPanel.getData()) {
                edge.getParameters().add(element);
            }
        } else {
            edgeParametersPanel.setData(edge.getParameters());
        }
    }

    private void hideShowConditionals(boolean show) {
        conditionalExpressionPanel.showConditions(show);
        addConditionalButton.setVisible(!show);
        pack();
    }

    private void showHideDescription(boolean showDescription) {
        showDescription(showDescription);
        addDescriptionButton.setVisible(!showDescription);
        pack();
    }

    class AddHideButtonListener implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            if (e.getSource().equals(addConditionalButton)) {
                hideShowConditionals(true);
            } else if (e.getSource().equals(addDescriptionButton)) {
                showHideDescription(true);
            }
        }
    }

    class CancelButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            modified = false;    // for the caller
            dispose();
        }
    }

    class ApplyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (modified) {
                unloadWidgets();
            }
            dispose();
        }
    }

    class EdgeInspectorDialogChangeListener extends KeyAdapter implements ChangeListener, ActionListener, CaretListener {

        @Override
        public void stateChanged(ChangeEvent event) {
            modified = true;
            okButton.setEnabled(true);
            getRootPane().setDefaultButton(okButton);
            dotLabel.setVisible(!((ViskitElement) timeDelayVarsCB.getSelectedItem()).getName().isEmpty());

            // Set the ComboBox width to accomodate the string length
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

    private void showDescription(boolean visible) {
        descriptionJsp.setVisible(visible);
    }

    public boolean isDescriptionVisible() {
        return descriptionJsp.isVisible();
    }

    public void setDescription(String newDescription) 
    {
        newDescription = ViskitStatics.emptyIfNull(newDescription);
        descriptionTextArea.setText(newDescription);
        modified = true;
        okButton.setEnabled(true);
    }

    public String getDescription() {
        return descriptionTextArea.getText().trim();
    }
    private ChangeListener changeListener;

    class CloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int ret = JOptionPane.showConfirmDialog(EdgeInspectorDialog.this, "Apply changes?",
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
    
} // end class EdgeInspectorDialog
