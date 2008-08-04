package viskit;

import edu.nps.util.BoxLayoutUtils;
import simkit.Priority;
import viskit.model.*;

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
import java.util.Vector;
import java.util.ArrayList;
import org.apache.log4j.Logger;

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
public class EdgeInspectorDialog extends JDialog {

    static Logger log = Logger.getLogger(EdgeInspectorDialog.class);
    private static EdgeInspectorDialog dialog;
    private Edge edge;
    private static boolean modified = false;
    private boolean schedulingType = true; // true = scheduling, false = cancelling
    private JButton canButt,  okButt;
    private JLabel srcEvent,  targEvent;
    private JTextField delay;
    private EdgeParametersPanel parameters;
    private ConditionalExpressionPanel conditionalExpressionPanel;
    private JPanel timeDelayPanel;
    private Border delayPanBorder,  delayPanDisabledBorder;
    private JPanel priorityPanel;
    private JComboBox priorityCB;
    private ArrayList<Priority> priorityList;  // matches combo box
    private Vector<String> priorityNames;
    private int priorityDefaultIndex = 3;  // set properly below
    private JPanel myParmPanel;
    private JLabel schedulingLabel;
    private JLabel cancellingLabel;
    private JButton addConditionalButton;
    private JButton addDescriptionButton;
    private JTextArea descriptionJta;
    private JScrollPane descriptionJsp;
    
    Vector<ViskitElement> nodeList;
    Model mod; //todo fix
    
    /**
     * Set up and show the dialog.  The first Component argument
     * determines which frame the dialog depends on; it should be
     * a component in the dialog's controlling frame. The second
     * Component argument should be null if you want the dialog
     * to come up with its left corner in the center of the screen;
     * otherwise, it should be the component on top of which the
     * dialog should appear.
     * @param f
     * @param comp
     * @param edge
     * @return 
     */
    public static boolean showDialog(JFrame f, Component comp, Edge edge) {
        if (dialog == null) {
            dialog = new EdgeInspectorDialog(f, comp, edge);
        } else {
            dialog.setParams(comp, edge);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }
    
    private EdgeInspectorDialog(JFrame frame, Component locationComp, Edge edge) {
        super(frame, "Edge Inspector", true);
        this.edge = edge;

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        mod = (Model) (VGlobals.instance().getEventGraphEditor().getModel());
        
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
        schedulingLabel = new JLabel("<html><b>Scheduling");
        BoxLayoutUtils.clampWidth(schedulingLabel);
        cancellingLabel = new JLabel("<html><b>Cancelling");
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
        srcEvent = new JLabel("srcEvent");
        srcEvent.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        sourceTargetValuesPanel.add(srcEvent);
        targEvent = new JLabel("targEvent");
        targEvent.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        sourceTargetValuesPanel.add(targEvent);
        sourceTargetValuesPanel.setBorder(BorderFactory.createTitledBorder(""));
        sourceTargetValuesPanel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        keepSameSize(srcEvent, targEvent);
        sourceTargetPanel.add(sourceTargetValuesPanel);
        sourceTargetPanel.add(Box.createHorizontalGlue());
        BoxLayoutUtils.clampHeight(sourceTargetPanel);
        edgeInspectorPanel.add(sourceTargetPanel);

        edgeInspectorPanel.add(Box.createVerticalStrut(5));

        descriptionJta = new JTextArea(2, 25);
        descriptionJsp = new JScrollPane(descriptionJta);
        descriptionJsp.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Description")));
        edgeInspectorPanel.add(descriptionJsp);

        Dimension descriptionJspDimension = descriptionJsp.getPreferredSize();
        descriptionJsp.setMinimumSize(descriptionJspDimension);

        descriptionJta.addCaretListener(new CaretListener() {

            public void caretUpdate(CaretEvent e) {
                if (changeListener != null) {
                    changeListener.stateChanged(new ChangeEvent(descriptionJta));
                }
            }
        });

        KeyListener keyListener = new myKeyListener();
        descriptionJta.addKeyListener(keyListener);

        conditionalExpressionPanel = new ConditionalExpressionPanel(edge, schedulingType);
        edgeInspectorPanel.add(conditionalExpressionPanel);

        priorityPanel = new JPanel();
        priorityPanel.setLayout(new BoxLayout(priorityPanel, BoxLayout.X_AXIS));
        priorityPanel.setOpaque(false);
        priorityPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Priority")));
        priorityCB = buildPriorityComboBox();
        priorityPanel.add(Box.createHorizontalStrut(50)); //102));     // set packed width clamp here
        priorityPanel.add(priorityCB);
        priorityPanel.add(Box.createHorizontalStrut(50)); //102));
        edgeInspectorPanel.add(priorityPanel);
        BoxLayoutUtils.clampHeight(priorityPanel);

        timeDelayPanel = new JPanel();
        timeDelayPanel.setLayout(new BoxLayout(timeDelayPanel, BoxLayout.X_AXIS));
        timeDelayPanel.setOpaque(false);
        timeDelayPanel.setBorder(BorderFactory.createTitledBorder("Time Delay"));
        delay = new JTextField();
        delay.setOpaque(true);
        delay.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        Dimension d = delay.getPreferredSize();      // only expand in horiz direction
        d.width = Integer.MAX_VALUE;
        delay.setMaximumSize(d);
        timeDelayPanel.add(delay);
        delayPanBorder = timeDelayPanel.getBorder();
        delayPanDisabledBorder = BorderFactory.createTitledBorder(new LineBorder(Color.gray), "Time Delay",
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                null, Color.gray);
        edgeInspectorPanel.add(timeDelayPanel);
        edgeInspectorPanel.add(Box.createVerticalStrut(5));

        myParmPanel = new JPanel();
        myParmPanel.setLayout(new BoxLayout(myParmPanel, BoxLayout.Y_AXIS));

        parameters = new EdgeParametersPanel(300);
        JScrollPane paramSp = new JScrollPane(parameters);
        paramSp.setBorder(null);
        paramSp.setOpaque(false);

        myParmPanel.add(paramSp);

        edgeInspectorPanel.add(myParmPanel);

        JPanel twoRowButtonPanel = new JPanel();
        twoRowButtonPanel.setLayout(new BoxLayout(twoRowButtonPanel, BoxLayout.Y_AXIS));

        JPanel addButtonPanel = new JPanel();
        addButtonPanel.setLayout(new BoxLayout(addButtonPanel, BoxLayout.X_AXIS));
        addButtonPanel.setBorder(new TitledBorder("add"));
        addDescriptionButton = new JButton("description"); //add description");
        addConditionalButton = new JButton("conditional"); //add conditional");

//        Font defButtFont = addConditionalButton.getFont();
//        int defButtFontSize = defButtFont.getSize();
//        addConditionalButton.setFont(defButtFont.deriveFont((float) (defButtFontSize - 4)));
//        addDescriptionButton.setFont(addConditionalButton.getFont());
        addButtonPanel.add(Box.createHorizontalGlue());
        addButtonPanel.add(addDescriptionButton);
        addButtonPanel.add(addConditionalButton);
        addButtonPanel.add(Box.createHorizontalGlue());
        twoRowButtonPanel.add(addButtonPanel);
        twoRowButtonPanel.add(Box.createVerticalStrut(5));

        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        canButt = new JButton("Cancel");
        okButt = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());
        buttPan.add(canButt);
        buttPan.add(okButt);
        twoRowButtonPanel.add(buttPan);

        edgeInspectorPanel.add(twoRowButtonPanel);
        cont.add(edgeInspectorPanel);

        fillWidgets();     // put the data into the widgets

        okButt.setEnabled(false);
        getRootPane().setDefaultButton(canButt);

        pack();     // do this prior to next
        this.setLocationRelativeTo(locationComp);

        // attach listeners
        canButt.addActionListener(new cancelButtonListener());
        okButt.addActionListener(new applyButtonListener());

        myChangeListener chlis = new myChangeListener();
        conditionalExpressionPanel.addChangeListener(chlis);
        priorityCB.addActionListener(chlis);
        delay.addCaretListener(chlis);
        priorityCB.getEditor().getEditorComponent().addKeyListener(chlis);

        addHideButtonListener hideList = new addHideButtonListener();
        addConditionalButton.addActionListener(hideList);
        addDescriptionButton.addActionListener(hideList);

        parameters.addDoubleClickedListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                vEdgeParameter ep = (vEdgeParameter) event.getSource();

                boolean wasModified = EdgeParameterDialog.showDialog(EdgeInspectorDialog.this, EdgeInspectorDialog.this, ep);
                if (wasModified) {
                    parameters.updateRow(ep);
                    okButt.setEnabled(true);
                    modified = true;
                }
            }
        });
    }

    public void setParams(Component c, Edge e) {
        edge = e;

        fillWidgets();
        pack();

        modified = false;
        okButt.setEnabled(false);
        getRootPane().setDefaultButton(canButt);

        this.setLocationRelativeTo(c);
    }

    private void keepSameSize(JComponent a, JComponent b) {
        Dimension ad = a.getPreferredSize();
        Dimension bd = b.getPreferredSize();
        Dimension d = new Dimension(Math.max(ad.width, bd.width), Math.max(ad.height, bd.height));
        a.setMinimumSize(d);
        b.setMinimumSize(d);
    }

    private JComboBox buildPriorityComboBox() {
        priorityNames = new Vector<String>(10);
        priorityList = new ArrayList<Priority>(10);
        try {
            Class<?> c = Class.forName("simkit.Priority");
            Field[] fa = c.getDeclaredFields();
            for (Field f : fa) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType().equals(c)) {
                    priorityNames.add(f.getName());
                    priorityList.add((Priority) f.get(null)); // static objects
                    if (f.getName().equalsIgnoreCase("default")) {
                        priorityDefaultIndex = priorityNames.size() - 1;
                    }  // save the default one
                }
            }
            JComboBox jcb = new JComboBox(priorityNames);
            jcb.setEditable(true); // this allows anything to be intered
            return jcb;
        } catch (Exception e) {
            Vstatics.log.error(e);
            return new JComboBox(new String[] {"simkit package not in class path"});
        }
    }

    private void setPriorityCBValue(String pr) {
        try {
            // Assume numeric comes in
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
        } catch (NumberFormatException e) {
            // First try to find it in the list
            int i = 0;
            for (String s : priorityNames) {
                if (s.equalsIgnoreCase(pr)) {
                    priorityCB.setSelectedIndex(i);
                    return;
                }
                i++;
            }

            log.error("Unknown edge priority: " + pr + " -- setting to DEFAULT)");
            priorityCB.setSelectedIndex(priorityDefaultIndex);
        }
    }

    private void fillWidgets() {
        nodeList = mod.getAllNodes();            // todo fix
       //Collections.sort(nodeList);             // todo get working

        srcEvent.setText(edge.from.getName());
        targEvent.setText(edge.to.getName());
        myParmPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), 
                BorderFactory.createTitledBorder("Edge Parameters passed to " + targEvent.getText())));

        if (edge.to.getArguments() == null || edge.to.getArguments().isEmpty()) {
            myParmPanel.setVisible(false);
        } else {
            parameters.setArgumentList(edge.to.getArguments());
            parameters.setData(edge.parameters);
            myParmPanel.setVisible(true);
        }

        if (edge instanceof SchedulingEdge) {
            if (edge.conditional == null || edge.conditional.trim().isEmpty()) {
                conditionalExpressionPanel.setText("true");
                hideShowConditionals(false);
            } else {
                conditionalExpressionPanel.setText(edge.conditional);
                hideShowConditionals(true);
            }
            setDescription(edge.conditionalDescription);

            if (edge.conditionalDescription == null || edge.conditionalDescription.isEmpty()) {
                hideShowDescription(false);
            } else {
                hideShowDescription(true);
            }

            if (edge.delay == null || edge.delay.trim().isEmpty()) {
                delay.setText("0.0");
            } else {
                delay.setText("" + edge.delay);
            }
            delay.setEnabled(true);
            timeDelayPanel.setBorder(delayPanBorder);

            setPriorityCBValue(((SchedulingEdge) edge).priority);

        } else {
            if (edge.conditional == null || edge.conditional.trim().isEmpty()) {
                conditionalExpressionPanel.setText("true");
            } else {
                conditionalExpressionPanel.setText(edge.conditional);
            }

            setDescription(edge.conditionalDescription);

            delay.setText("n/a");
            delay.setEnabled(false);
            timeDelayPanel.setBorder(delayPanDisabledBorder);
        }

        setSchedulingType(edge instanceof SchedulingEdge);
    }

    private void unloadWidgets() {
        if (edge instanceof SchedulingEdge) {
            int idx = priorityCB.getSelectedIndex();
            if (idx < 0) {
                String s = (String) priorityCB.getSelectedItem();
                if (s.isEmpty()) {
                    Priority p = priorityList.get(priorityDefaultIndex);
                    ((SchedulingEdge) edge).priority = "" + p.getPriority();
                } else {
                    ((SchedulingEdge) edge).priority = s;
                }
            } else {
                Priority p = priorityList.get(priorityCB.getSelectedIndex());
                ((SchedulingEdge) edge).priority = "" + p.getPriority();
            }
        }
        String delaySt = delay.getText();
        edge.delay = (delaySt == null || delaySt.trim().isEmpty()) ? "0.0" : delay.getText();
        
        String condSt = conditionalExpressionPanel.getText();
        edge.conditional = (condSt == null || condSt.trim().isEmpty()) ? "true" : conditionalExpressionPanel.getText();
        
        edge.conditionalDescription = getDescription();
        if (!edge.parameters.isEmpty()) {
            edge.parameters.clear();
        }
        
        // Key on the EdgeNode's list of potential arguments
        // TODO: How do we do this automatically from the EventInspectorDialog
        // when we remove an argument?
        if (!edge.to.getArguments().isEmpty()) {

            // Bug 1373: This is how applying changes to a scheduling edge
            // causes the correct EG XML representation when removing event
            // parameters from a proceding node.  This loop adds vEdgeParameters
            for (ViskitElement o : parameters.getData()) {
                edge.parameters.add(o);
            }
        } else {
            parameters.setData(edge.parameters);
        }
    }

    private void hideShowConditionals(boolean show) {
        conditionalExpressionPanel.showConditions(show);
        addConditionalButton.setVisible(!show);
        pack();
    }

    private void hideShowDescription(boolean show) {
        showDescription(show);
        addDescriptionButton.setVisible(!show);
        pack();
    }

    class addHideButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(addConditionalButton)) {
                hideShowConditionals(true);
            } else if (e.getSource().equals(addDescriptionButton)) {
                hideShowDescription(true);
            }
        }
    }

    class reverseButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            EventNode en = edge.from;
            edge.from = edge.to;
            edge.to = en;
            modified = true;
            okButt.setEnabled(true);
            getRootPane().setDefaultButton(okButt);
        }
    }

    class cancelButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            modified = false;    // for the caller
            setVisible(false);
        }
    }

    class applyButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            if (modified) {
                // todo fix beanshell syntax checking.  I don't know if this was ever complete enough.  The example to test it
                // against is the edge from Run to Arrival in examples/ArrivalProcess.  The time delay is "interarrivalTime.generate()",
                // which is good syntax if the state variable "RandomVariate interarrivalTime" is already in the beanshell context,
                // which I don't think it is.  Must make the beanshell error checking smarter. 19 Jan 2007

                /*
                StringBuffer sb = new StringBuffer();
                if(edge instanceof SchedulingEdge) {
                sb.append("double delay = ");
                sb.append(delay.getText());
                sb.append(";\n");
                }
                sb.append("if(");
                sb.append(conditionalExpressionPanel.getText());
                sb.append("){;}");
                if(ViskitConfig.instance().getVal("app.beanshell.warning").equalsIgnoreCase("true")) {
                String parseResults = VGlobals.instance().parseCode(edge.from,sb.toString()); //pre+conditionalExpressionPanel.getText()+post);
                if(parseResults != null) {
                boolean ret = BeanshellErrorDialog.showDialog(parseResults,EdgeInspectorDialog.this);
                if(!ret) // don't ignore
                return;
                //  int ret = JOptionPane.showConfirmDialog(EdgeInspectorDialog.this,"Java language error:\n"+parseResults+"\nIgnore and continue?",
                //                                "Warning",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
                //  if(ret != JOptionPane.YES_OPTION)
                //    return;
                }
                }
                 */
                unloadWidgets();
            }
            setVisible(false);
        }
    }

    class myChangeListener extends KeyAdapter implements ChangeListener, ActionListener, CaretListener {

        public void stateChanged(ChangeEvent event) {
            modified = true;
            okButt.setEnabled(true);
            getRootPane().setDefaultButton(okButt);
        }

        public void actionPerformed(ActionEvent e) {
            stateChanged(null);
        }

        public void caretUpdate(CaretEvent e) {
            stateChanged(null);
        }

        @Override
        public void keyTyped(KeyEvent e) {
            stateChanged(null);
            // TODO:  update OK button when description field modified, rather than waiting to select another field            
        }
    }

    private void setSchedulingType(boolean wh) {
        schedulingType = wh;
        priorityPanel.setVisible(wh);
        timeDelayPanel.setVisible(wh);
        schedulingLabel.setVisible(wh);
        cancellingLabel.setVisible(!wh);
    }

    public void showDescription(boolean wh) {
        descriptionJsp.setVisible(wh);
    }

    public boolean isDescriptionVisible() {
        return descriptionJsp.isVisible();
    }

    public void setDescription(String s) {
        descriptionJta.setText(s);
        modified = true;
        okButt.setEnabled(true);
    }

    public String getDescription() {
        return descriptionJta.getText().trim();
    }
    private ChangeListener changeListener;

    public void addChangeListener(ChangeListener listener) {
        this.changeListener = listener;
    }

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int ret = JOptionPane.showConfirmDialog(EdgeInspectorDialog.this, "Apply changes?",
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

    class myKeyListener extends KeyAdapter {

        @Override
        public void keyTyped(KeyEvent e) {
            modified = true;
            okButt.setEnabled(true);
        }
    }
}