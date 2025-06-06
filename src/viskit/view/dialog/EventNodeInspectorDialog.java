package viskit.view.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.ViskitGlobals;
import static viskit.ViskitStatics.DESCRIPTION_HINT;
import viskit.model.*;
import viskit.view.ArgumentsPanel;
import viskit.view.CodeBlockPanel;
import viskit.view.LocalVariablesPanel;
import viskit.view.TransitionsPanel;

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
public class EventNodeInspectorDialog extends JDialog
{
    static final Logger LOG = LogManager.getLogger();

    private static EventNodeInspectorDialog dialog;
    private EventNode eventNode;
    private static boolean modified = false;
    private final JTextField eventNameTF;
//    private final JTextField descriptionTF;
//    private final JPanel descriptionPanel;
    private final JTextArea   descriptionTextArea;
    private final JScrollPane descriptionScrollPane;
    private TransitionsPanel stateTransitionsPanel;
    private ArgumentsPanel arguments;
    private LocalVariablesPanel localVariablesPanel;
    private final CodeBlockPanel localCodeBlockPanel;
    private final JButton cancelButton;
    private final JButton okButton;
    private final JButton addDescriptionButton;
    private final JButton addArgumentsButton;
    private final JButton addLocalVariablesButton;
    private final JButton addCodeBlockButton;
    private final JButton addStateTransitionsButton;

    /**
     * Set up and show the dialog.  The first Component argument
     * determines which frame the dialog depends on; it should be
     * a component in the dialog's controlling frame. The second
     * Component argument should be null if you want the dialog
     * to come up with its left corner in the center of the screen;
     * otherwise, it should be the component on top of which the
     * dialog should appear.
     *
     * @param f parent frame
     * @param node EventNode to edit
     * @return whether data was modified, or not
     */
    public static boolean showDialog(JFrame f, EventNode node) 
    {
        if (dialog == null) 
        {
            dialog = new EventNodeInspectorDialog(f, node);
        } 
        else 
        {
            dialog.setParameters(f, node);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }
    private ChangeListener changeListener;

    private EventNodeInspectorDialog(final JFrame frame, EventNode node) 
    {
        super(frame, "Event Node Inspector: " + node.getName(), true);
        this.eventNode = node;

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new CloseListener());

        JPanel eventNodeInspectorPanel = new JPanel();
        setContentPane(eventNodeInspectorPanel);
        eventNodeInspectorPanel.setLayout(new BoxLayout(eventNodeInspectorPanel, BoxLayout.Y_AXIS));
        eventNodeInspectorPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));

        // name
        JPanel eventNamePanel = new JPanel();
        eventNamePanel.setLayout(new BoxLayout(eventNamePanel, BoxLayout.X_AXIS));
        eventNamePanel.setOpaque(false);
        eventNamePanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Event Node name")));
        eventNameTF = new JTextField(30); // This sets the "preferred width" when this dialog is packed
        eventNameTF.setOpaque(true);
        eventNameTF.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        eventNamePanel.add(eventNameTF);
        // make the field only expand horizontally
        Dimension d = eventNamePanel.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        eventNamePanel.setMaximumSize(new Dimension(d));
        eventNodeInspectorPanel.add(eventNamePanel);

//        descriptionPanel = new JPanel();
//        descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.X_AXIS));
//        descriptionPanel.setOpaque(false);
//        descriptionPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("description")));
//        descriptionPanel.setToolTipText(DESCRIPTION_HINT);
        
//        descriptionTF = new JTextField("");
//        descriptionTF.setToolTipText(DESCRIPTION_HINT);
//        descriptionTF.setOpaque(true);
//        descriptionTF.setLineWrap(true);
//        descriptionTF.setWrapStyleWord(true);
//        descriptionTF.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        
        // TODO not scrolling :(
        descriptionTextArea = new JTextArea(2, 25);
        descriptionTextArea.setText("");
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setWrapStyleWord(true);
        descriptionTextArea.setToolTipText(DESCRIPTION_HINT);
        descriptionScrollPane = new JScrollPane(descriptionTextArea);
        descriptionScrollPane.setToolTipText(DESCRIPTION_HINT);
        descriptionScrollPane.setBorder(new CompoundBorder(
                new EmptyBorder(0, 0, 5, 0),
                BorderFactory.createTitledBorder("description")));
        eventNodeInspectorPanel.add(descriptionScrollPane);
        
        Dimension descriptionScrollPaneDimension = descriptionScrollPane.getPreferredSize();
        descriptionScrollPane.setMinimumSize(descriptionScrollPaneDimension);

        descriptionTextArea.addCaretListener((CaretEvent e) -> {
            if (changeListener != null) {
                changeListener.stateChanged(new ChangeEvent(descriptionTextArea));
            }
        });
        
//        descriptionPanel.add(descriptionScrollPane);
//        d = descriptionPanel.getPreferredSize();
//        d.width = Integer.MAX_VALUE;
//        descriptionPanel.setMaximumSize(new Dimension(d));

//        JButton editDescriptionButton = new JButton(" ... ");
//        editDescriptionButton.setToolTipText(DESCRIPTION_HINT);
//        editDescriptionButton.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
//        editDescriptionButton.setToolTipText("Select to edit a long description");
//        Dimension dd = editDescriptionButton.getPreferredSize();
//        dd.height = d.height;
//        editDescriptionButton.setMaximumSize(new Dimension(dd));
//        descriptionPanel.add(editDescriptionButton);


        // Event node arguments
        arguments = new ArgumentsPanel(300, 2);
        arguments.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Event Node arguments")));
        eventNodeInspectorPanel.add(arguments);

        // local variables
        localVariablesPanel = new LocalVariablesPanel(300, 2);
        localVariablesPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Local variables")));
        localVariablesPanel.setToolTipText("variables with local scope, not globally visible outside of this event node");
        eventNodeInspectorPanel.add(localVariablesPanel);

        // code block
        localCodeBlockPanel = new CodeBlockPanel(this, true, "Event Code Block");
        localCodeBlockPanel.setToolTipText("Use of this code block will cause code to run first" +
                " at the top of the Event's \"do\" method");
        localCodeBlockPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Local code block")));
        eventNodeInspectorPanel.add(localCodeBlockPanel);

        // state transitions
        stateTransitionsPanel = new TransitionsPanel();
        stateTransitionsPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("State transitions")));
        eventNodeInspectorPanel.add(stateTransitionsPanel);

        // buttons
        JPanel twoRowButtonPanel = new JPanel();
        twoRowButtonPanel.setLayout(new BoxLayout(twoRowButtonPanel, BoxLayout.Y_AXIS));

        JPanel addButtonPanel = new JPanel();
        addButtonPanel.setLayout(new BoxLayout(addButtonPanel, BoxLayout.X_AXIS));
        addButtonPanel.setBorder(new TitledBorder("add"));

        addDescriptionButton = new JButton("description"); //add description");
        addArgumentsButton = new JButton("Event node arguments"); //add arguments");
        addArgumentsButton.setToolTipText("argument variables necessary for initializing this event node");
        addLocalVariablesButton = new JButton("Local variables"); //add locals");
        addLocalVariablesButton.setToolTipText("variables with local scope, not globally visible outside of this event node");
        
        addCodeBlockButton = new JButton("Local code block");
        addCodeBlockButton.setToolTipText("initialization source code");
        addStateTransitionsButton = new JButton("state transitions"); //add state transitions");

        //Font defButtFont = addDescriptionButton.getFont();
        //int defButtFontSize = defButtFont.getSize();
        //addDescriptionButton.setFont(defButtFont.deriveFont((float) (defButtFontSize - 4)));
        addArgumentsButton.setFont(addDescriptionButton.getFont());
        addLocalVariablesButton.setFont(addDescriptionButton.getFont());
        addCodeBlockButton.setFont(addDescriptionButton.getFont());
        addStateTransitionsButton.setFont(addDescriptionButton.getFont());

        addButtonPanel.add(Box.createHorizontalGlue());
        addButtonPanel.add(addDescriptionButton);
        addButtonPanel.add(addArgumentsButton);
        addButtonPanel.add(addLocalVariablesButton);
        addButtonPanel.add(addCodeBlockButton);
        addButtonPanel.add(addStateTransitionsButton);
        addButtonPanel.add(Box.createHorizontalGlue());
        twoRowButtonPanel.add(addButtonPanel);
        twoRowButtonPanel.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        cancelButton = new JButton("Cancel");
        okButton = new JButton("Apply changes");
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        twoRowButtonPanel.add(buttonPanel);

        eventNodeInspectorPanel.add(twoRowButtonPanel);

        // attach listeners
        cancelButton.addActionListener(new CancelButtonListener());
        okButton.addActionListener(new ApplyButtonListener());

        AddHideButtonListener hideList = new AddHideButtonListener();
        addDescriptionButton.addActionListener(hideList);
        addArgumentsButton.addActionListener(hideList);
        addLocalVariablesButton.addActionListener(hideList);
        addCodeBlockButton.addActionListener(hideList);
        addStateTransitionsButton.addActionListener(hideList);

        ChangeActionListener myChangeListener = new ChangeActionListener();
        //name.addActionListener(chlis);
        KeyListener keyListener = new KeyListener();
        eventNameTF.addKeyListener(keyListener);
//        descriptionTF.addKeyListener(keyListener);
        descriptionTextArea.addKeyListener(keyListener);
//        editDescriptionButton.addActionListener(new DescriptionListener());

        arguments.addPlusListener(myChangeListener);
        arguments.addMinusListener(myChangeListener);
        arguments.addDoubleClickedListener((ActionEvent e) -> {
            EventArgument ea = (EventArgument) e.getSource();
            boolean modified1 = EventArgumentDialog.showDialog(frame, ea);
            if (modified1) {
                arguments.updateRow(ea);
                setModified(modified1);
            }
        }); // EventArgumentDialog: Event node arguments

        localCodeBlockPanel.addUpdateListener(myChangeListener);

        localVariablesPanel.addPlusListener(myChangeListener);
        localVariablesPanel.addMinusListener(myChangeListener);
        localVariablesPanel.addDoubleClickedListener((ActionEvent actionEvent) -> {
            EventLocalVariable eventLocalVariable = (EventLocalVariable) actionEvent.getSource();
            boolean modified1 = LocalVariableDialog.showDialog(frame, eventLocalVariable);
            if (modified1) {
                localVariablesPanel.updateRow(eventLocalVariable);
                setModified(modified1);
            }
        });

        stateTransitionsPanel.addPlusListener(myChangeListener);
        stateTransitionsPanel.addMinusListener(myChangeListener);
        stateTransitionsPanel.addDoubleClickedListener(new MouseAdapter() 
        {
            // EventStateTransitionDialog: State transition
            // bug fix 1183
            @Override
            public void mouseClicked(MouseEvent mouseEvent) 
            {
                EventStateTransition eventStateTransition = (EventStateTransition) mouseEvent.getSource();

                // modified comes back true even if a caret was placed in a
                // text box
                boolean modified = EventStateTransitionDialog.showDialog(
                        frame,
                        eventStateTransition,
                        arguments,
                        localVariablesPanel);
                if (modified)
                {
                    stateTransitionsPanel.updateTransition(eventStateTransition);
                    setModified(modified);
                }
            }
        });

        setParameters(frame, node);
    }

    private void setModified(boolean f) {
        okButton.setEnabled(f);
        modified = f;
    }

    private void sizeAndPosition(Component c) {
        pack();     // do this prior to next

        // little check to add some extra space to always include the node name
        // in title bar w/out dotdotdots
        if (getWidth() < 350) {
            setSize(350, getHeight());
        }
        setLocationRelativeTo(c);
    }

    public final void setParameters(Component c, EventNode newEventNode) {
        eventNode = newEventNode;

        fillWidgets();
        sizeAndPosition(c);
    }

    private void fillWidgets() 
    {
        String nodeName = eventNode.getName();
        nodeName = nodeName.replace(' ', '_');
        setTitle("Event Node Inspector: " + nodeName);
        eventNameTF.setText(nodeName);

        Dimension d = descriptionTextArea.getPreferredSize();
//        String s = fillString(eventNode.getDescription());
        String s = eventNode.getDescription();
        descriptionTextArea.setText(s);
        descriptionTextArea.setCaretPosition(0);
        descriptionTextArea.setPreferredSize(d);

//      hideShowDescription(s != null && !s.isEmpty());
        hideShowDescription(true); // always show

        s = eventNode.getCodeBlockString();
        localCodeBlockPanel.setData(s);
        localCodeBlockPanel.setVisibleLines(1);
        hideShowCodeBlock(s != null && !s.isEmpty());

        stateTransitionsPanel.setTransitions(eventNode.getStateTransitions());
        s = stateTransitionsPanel.getString();
        hideShowStateTransitions(s != null && !s.isEmpty());

        arguments.setData(eventNode.getArguments());
        hideShowArguments(!arguments.isEmpty());

        localVariablesPanel.setData(eventNode.getLocalVariables());
        hideShowLocals(!localVariablesPanel.isEmpty());

        setModified(false);
        getRootPane().setDefaultButton(cancelButton);
    }

    private void unloadWidgets(EventNode eventNode)
    {
        if (modified) 
        {
            eventNode.setName(eventNameTF.getText().trim().replace(' ', '_'));

            eventNode.setStateTransitions(stateTransitionsPanel.getTransitions());

            // Bug 1373: This is how an EventNode will have knowledge
            // of edge parameter additions, or removals
            eventNode.setArguments(arguments.getData());

            // Bug 1373: This is how we will now sync up any SchedulingEdge
            // parameters with corresponding EventNode parameters

            // TODO: Recheck bug and verify this isn't done elsewhere.  W/O
            // the continue statement, it nukes edge values that were already
            // there if we modify a node
            for (ViskitElement viskitElement : eventNode.getConnections()) {

                // Okay, it's a SchedulingEdge
                if (viskitElement instanceof SchedulingEdge) {

                    // and, this SchedulingEdge is going to this node
                    if (((Edge) viskitElement).getTo().getName().equals(eventNode.getName())) {
                        LOG.debug("Found the SE's 'to' Node that matches this EventNode");

                        // The lower key values signal when it was connected to
                        // to this event node.  We're interested in the first
                        // SchedulingEdge to this EventNode
                        LOG.debug("SchedulingEdge ID is: " + viskitElement.getModelKey());

                        // If this isn't the first time, then skip over this edge
                        if (!((Edge) viskitElement).getParameters().isEmpty()) {continue;}

                        // We match EventArgument count to EdgeParameter count
                        // here.
                        for (ViskitElement element : eventNode.getArguments()) {

                            // The user will be able to change any values from
                            // the EdgeInspectorDialog.  Right now, values are
                            // defaulted to zeros.
                            ((Edge) viskitElement).getParameters().add(new ViskitEdgeParameter(element.getValue()));
                        }
                    }
                }
            }
            eventNode.setLocalVariables(localVariablesPanel.getData());
//            eventNode.getComments().clear();
//            eventNode.getComments().add(descriptionTF.getText().trim());
            eventNode.setDescription(descriptionTextArea.getText().trim());
            eventNode.setCodeBlockString(localCodeBlockPanel.getData());
        }
    }

    private String fillString(List<String> stringList)
    {
        if (stringList == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String nextString : stringList) {
            sb.append(nextString);
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    class CancelButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            setModified(false);

            // To start numbering over next time
            ViskitGlobals.instance().getActiveEventGraphModel().resetLVNameGenerator();
            dispose();
        }
    }

    class ApplyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (modified) {

                  // NOTE: currently, BeanShell checking for more than a simple
                  // primitive types is disabled.  The compiler will inform of
                  // any potential errors, so, disable this unnecessary code for
                  // now

//                // Our node object hasn't been updated yet (see unloadWidgets) and won't if
//                // we cancel out below.  But to do the beanshell parse test, a node needs to be supplied
//                // so the context can be set up properly.
//                // Build a temp one;
//                EventNode evn = node.shallowCopy();   // temp copy
//                unloadWidgets(evn);  // put our pending edits in place
//
//                // Parse the state transitions
//                StringBuilder parseThis = new StringBuilder();
//                for (ViskitElement transition : transitions.getStateTransitions()) {
//                    EventStateTransition est = (EventStateTransition) transition;
//                    parseThis.append(est.toString());
//                    parseThis.append(";");
//                    String idxv = est.getIndexingExpression();
//                    if (idxv != null && !idxv.isEmpty()) {
//                        addPotentialLocalIndexVariable(evn, est.getIndexingExpression());
//                    }
//                }
//
//                String ps = parseThis.toString().trim();
//                if (!ps.isEmpty() && ViskitConfig.instance().getVal(ViskitConfig.BEANSHELL_WARNING).equalsIgnoreCase("true")) {
//                    String parseResults = ViskitGlobals.instance().parseCode(evn, ps);
//                    if (parseResults != null) {
//                        boolean ret = BeanshellErrorDialog.showDialog(parseResults, EventNodeInspectorDialog.this);
//                        if (!ret) // don't ignore
//                        {
//                            return;
//                        }
//                    }
//                }

                unloadWidgets(eventNode); // confirmed values are returned?
            }
            dispose(); // releases window assets; perhaps this is premature>  contained information not being saved...
        }

//        private void addPotentialLocalIndexVariable(EventNode n, String lvName) {
//            List<ViskitElement> locVars = new ArrayList<>(n.getLocalVariables());
//            locVars.add(new EventLocalVariable(lvName, "int", "0"));
//        }
    }

    // begin show/hide support for unused fields
    /** hide or show description text field
     */
    private void hideShowDescription(boolean show) {
        descriptionScrollPane.setVisible(show);
        addDescriptionButton.setVisible(!show);
        pack();
    }

    private void hideShowArguments(boolean show) {
        arguments.setVisible(show);
        addArgumentsButton.setVisible(!show);
        pack();
    }

    private void hideShowLocals(boolean show) {
        localVariablesPanel.setVisible(show);
        addLocalVariablesButton.setVisible(!show);
        pack();
    }

    private void hideShowCodeBlock(boolean show) {
        localCodeBlockPanel.setVisible(show);
        addCodeBlockButton.setVisible(!show);
        pack();
    }

    private void hideShowStateTransitions(boolean show) {
        stateTransitionsPanel.setVisible(show);
        addStateTransitionsButton.setVisible(!show);
        pack();
    }

    class AddHideButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(addDescriptionButton)) {
                hideShowDescription(true);
            } else if (e.getSource().equals(addArgumentsButton)) {
                hideShowArguments(true);
            } else if (e.getSource().equals(addLocalVariablesButton)) {
                hideShowLocals(true);
            } else if (e.getSource().equals(addCodeBlockButton)) {
                hideShowCodeBlock(true);
            } else if (e.getSource().equals(addStateTransitionsButton)) {
                hideShowStateTransitions(true);
            }
        }
    }

    // end show/hide support for unused fields
    class KeyListener extends KeyAdapter {

        @Override
        public void keyTyped(KeyEvent e) {
            setModified(true);
            getRootPane().setDefaultButton(okButton);
        }
    }

    class ChangeActionListener implements ChangeListener, ActionListener {

        @Override
        public void stateChanged(ChangeEvent event) {
            setModified(true);
            getRootPane().setDefaultButton(okButton);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            stateChanged(null);
        }
    }

    class CloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int ret = JOptionPane.showConfirmDialog(EventNodeInspectorDialog.this, "Apply changes?",
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

    class DescriptionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            StringBuffer sb = new StringBuffer(EventNodeInspectorDialog.this.descriptionTextArea.getText().trim());
            boolean modded = TextAreaDialog.showTitledDialog("Event Description",
                    EventNodeInspectorDialog.this, sb);
            if (modded) {
                EventNodeInspectorDialog.this.descriptionTextArea.setText(sb.toString().trim());
                EventNodeInspectorDialog.this.descriptionTextArea.setCaretPosition(0);
                setModified(true);
            }
        }
    }
}
