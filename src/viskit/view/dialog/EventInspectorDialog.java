package viskit.view.dialog;

import edu.nps.util.LogUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.*;
//import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
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
public class EventInspectorDialog extends JDialog {

    private static EventInspectorDialog dialog;
    private EventNode eventNode;
    private static boolean modified = false;
    private JTextField          nameTF;
    private JTextField          descriptionTF;
    private JPanel              descriptionPanel;
    private TransitionsPanel    transitionsPanel;
    private ArgumentsPanel      argumentsPanel;
    private LocalVariablesPanel localVariablesPanel;
    private CodeBlockPanel      codeBlockPanel;
    private JButton cancelButton, okButton;
    private JButton addDescriptionButton, addArgumentsButton, addLocalVariablesButton, addCodeBlockButton, addStateTransitionsButton;

    /**
     * Set up and show the dialog.  The first Component argument
 determines which frame the dialog depends on; it should be
 a component in the dialog's controlling frame. The second
 Component argument should be null if you want the dialog
 toEventNode come up with its left corner in the center of the screen;
 otherwise, it should be the component on top of which the
 dialog should appear.
     *
     * @param f parent frame
     * @param eventNode EventNode toEventNode edit
     * @return whether data was modified, or not
     */
    public static boolean showDialog(JFrame f, EventNode eventNode) {
        if (dialog == null) {
            dialog = new EventInspectorDialog(f, eventNode);
        } else {
            dialog.setParameters(f, eventNode);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private EventInspectorDialog(final JFrame frame, EventNode eventNode)
	{
        super(frame, "Event Inspector: " + eventNode.getName(), true);
        this.eventNode = eventNode;

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        JPanel panel = new JPanel();
        setContentPane(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));

        // name
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
        namePanel.setOpaque(false);
        namePanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Event name")));
        nameTF = new JTextField(30); // This sets the "preferred width" when this dialog is packed
        nameTF.setOpaque(true);
        nameTF.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        namePanel.add(nameTF);
        // make the field expand only horiz.
        Dimension d = namePanel.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        namePanel.setMaximumSize(new Dimension(d));
        panel.add(namePanel);

        descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.X_AXIS));
        descriptionPanel.setOpaque(false);
        descriptionPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Description")));
        descriptionTF = new JTextField("");
        descriptionTF.setOpaque(true);
        descriptionTF.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        descriptionPanel.add(descriptionTF);
        d = descriptionPanel.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        descriptionPanel.setMaximumSize(new Dimension(d));

        JButton editDescriptionButton = new JButton(" ... ");
        editDescriptionButton.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        editDescriptionButton.setToolTipText("Click to edit a long description");
        Dimension dd = editDescriptionButton.getPreferredSize();
        dd.height = d.height;
        editDescriptionButton.setMaximumSize(new Dimension(dd));
        descriptionPanel.add(editDescriptionButton);
        panel.add(descriptionPanel);

        // state transitions
        transitionsPanel = new TransitionsPanel();
        transitionsPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("State transitions")));
        panel.add(transitionsPanel);

        // Event arguments
        argumentsPanel = new ArgumentsPanel(300, 2);
        argumentsPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Event arguments")));
        panel.add(argumentsPanel);

        // local variables
        localVariablesPanel = new LocalVariablesPanel(300, 2);
        localVariablesPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Local variables")));
        panel.add(localVariablesPanel);

        // code block
        codeBlockPanel = new CodeBlockPanel(this, true, "Event Code Block");
        codeBlockPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Code block")));
        panel.add(codeBlockPanel);

        // buttons
        JPanel twoRowButtonPanel = new JPanel();
        twoRowButtonPanel.setLayout(new BoxLayout(twoRowButtonPanel, BoxLayout.Y_AXIS));

        JPanel addButtonPanel = new JPanel();
        addButtonPanel.setLayout(new BoxLayout(addButtonPanel, BoxLayout.X_AXIS));
        addButtonPanel.setBorder(new TitledBorder("add"));

        addDescriptionButton = new JButton("description"); //add description");
   addStateTransitionsButton = new JButton("state transitions"); //add state transitions");
          addArgumentsButton = new JButton("arguments"); //add arguments");
     addLocalVariablesButton = new JButton("locals"); //add locals");
          addCodeBlockButton = new JButton("code block"); //add code block");
		  
             addDescriptionButton.setToolTipText(ViskitStatics.DEFAULT_DESCRIPTION);
        addStateTransitionsButton.setToolTipText("Define state transitions for this Event");
               addArgumentsButton.setToolTipText("Define initialization arguments for this Event");
          addLocalVariablesButton.setToolTipText("Define local variables for this Event");
               addCodeBlockButton.setToolTipText("Define a Java source-code block for this Event (advanced feature, not recommended)");

        //Font defButtFont = addDescriptionButton.getFont();
        //int defButtFontSize = defButtFont.getSize();
        //addDescriptionButton.setFont(defButtFont.deriveFont((float) (defButtFontSize - 4)));
        addStateTransitionsButton.setFont(addDescriptionButton.getFont());
               addArgumentsButton.setFont(addDescriptionButton.getFont());
          addLocalVariablesButton.setFont(addDescriptionButton.getFont());
               addCodeBlockButton.setFont(addDescriptionButton.getFont());

        addButtonPanel.add(Box.createHorizontalGlue());
        addButtonPanel.add(addDescriptionButton);
        addButtonPanel.add(addStateTransitionsButton);
        addButtonPanel.add(addArgumentsButton);
        addButtonPanel.add(addLocalVariablesButton);
        addButtonPanel.add(addCodeBlockButton);
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

        panel.add(twoRowButtonPanel);

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        AddShowButtonListener hideList = new AddShowButtonListener();
        addDescriptionButton.addActionListener(hideList);
        addArgumentsButton.addActionListener(hideList);
        addLocalVariablesButton.addActionListener(hideList);
        addCodeBlockButton.addActionListener(hideList);
        addStateTransitionsButton.addActionListener(hideList);

        myChangeActionListener myChangeListener = new myChangeActionListener();
        //name.addActionListener(chlis);
        KeyListener keyListener = new MyKeyListener();
        nameTF.addKeyListener(keyListener);
        descriptionTF.addKeyListener(keyListener);
        editDescriptionButton.addActionListener(new commentListener());

        argumentsPanel.addPlusListener(myChangeListener);
        argumentsPanel.addMinusListener(myChangeListener);
        argumentsPanel.addDoubleClickedListener(new ActionListener() {

            // EventArgumentDialog: Event arguments
            @Override
            public void actionPerformed(ActionEvent actionEvent)
			{
                EventArgument eventArgument = (EventArgument) actionEvent.getSource();
                boolean modified = EventArgumentDialog.showDialog(frame, eventArgument);
                if (modified)
				{
                    argumentsPanel.updateRow(eventArgument);
                    setModified(modified);
                }
            }
        });

        codeBlockPanel.addUpdateListener(myChangeListener);

        localVariablesPanel.addPlusListener(myChangeListener);
        localVariablesPanel.addMinusListener(myChangeListener);
        localVariablesPanel.addDoubleClickedListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                EventLocalVariable elv = (EventLocalVariable) e.getSource();
                boolean modified = LocalVariableDialog.showDialog(frame, elv);
                if (modified) {
                    localVariablesPanel.updateRow(elv);
                    setModified(modified);
                }
            }
        });

        transitionsPanel.addPlusListener(myChangeListener);
        transitionsPanel.addMinusListener(myChangeListener);
        transitionsPanel.addDoubleClickedListener(new MouseAdapter() {

            // EventStateTransitionDialog: State transition
            // bug fix 1183
            @Override
            public void mouseClicked(MouseEvent e) {
                EventStateTransition eventStateTransition = (EventStateTransition) e.getSource();

                // modified comes back true even if a caret was placed in a
                // text box
                boolean modified = EventStateTransitionDialog.showDialog(
                        frame,
						getEventNode().getName(),
                        eventStateTransition,
                        argumentsPanel,
                        localVariablesPanel);
                if (modified)
				{
                    transitionsPanel.updateTransition(eventStateTransition);
                    setModified(modified);
                }
            }
        });

        setParameters(frame, eventNode);
    }

    private void setModified(boolean value)
	{
        okButton.setEnabled(value);
        modified = value;
    }

    private void sizeAndPosition(Component c) {
        pack();     // do this prior toEventNode next

        // little check toEventNode add some extra space toEventNode always include the node name
        // in title bar w/out dotdotdots
        if (getWidth() < 350) {
            setSize(350, getHeight());
        }
        setLocationRelativeTo(c);
    }

    public final void setParameters(Component c, EventNode en) {
        eventNode = en;

        fillWidgets();
        sizeAndPosition(c);
    }

    private void fillWidgets()
	{
        String nameOfStateTransition = eventNode.getName();
        nameOfStateTransition = nameOfStateTransition.replace(' ', '_');
        setTitle("Event Inspector: " + nameOfStateTransition);
        nameTF.setText(nameOfStateTransition);

        Dimension d = descriptionTF.getPreferredSize();
        descriptionTF.setText(eventNode.getDescription());
        descriptionTF.setCaretPosition(0);
        descriptionTF.setPreferredSize(d);

        showDescription(true); // always show descriptions by default, they are important for model definition

        String codeBlockSourceText = eventNode.getCodeBlock();
        codeBlockPanel.setData(codeBlockSourceText);
        codeBlockPanel.setVisibleLines(1);
        showCodeBlock(codeBlockSourceText != null && !codeBlockSourceText.isEmpty());

        transitionsPanel.setTransitions(eventNode.getTransitions());
        codeBlockSourceText = transitionsPanel.getString();
        showStateTransitions(codeBlockSourceText != null && !codeBlockSourceText.isEmpty());

        argumentsPanel.setData(eventNode.getArguments());
        showArguments(!argumentsPanel.isEmpty());

        localVariablesPanel.setData(eventNode.getLocalVariables());
        showLocals(!localVariablesPanel.isEmpty());

        setModified(false);
        getRootPane().setDefaultButton(cancelButton);
    }

    private void unloadWidgets(EventNode eventNode) {
        if (modified) {
            eventNode.setName(nameTF.getText().trim().replace(' ', '_'));

            eventNode.setTransitions(transitionsPanel.getTransitions());

            // Bug 1373: This is how an EventNode will have knowledge
            // of edge parameter additions, or removals
            eventNode.setArguments(argumentsPanel.getData());

            // Bug 1373: This is how we will now sync up any SchedulingEdge
            // parametersList with corresponding EventNode parametersList

            // TODO: Recheck bug and verify this isn't don't elsewhere.  W/O
            // the continue statement, it nukes edge values that were already
            // there if we modify a node
            for (ViskitElement viskitElement : eventNode.getConnections()) {

                // Okay, it's a SchedulingEdge
                if (viskitElement instanceof SchedulingEdge) {

                    // and, this SchedulingEdge is going toEventNode this node
                    if (((SchedulingEdge) viskitElement).toEventNode.getName().equals(eventNode.getName())) {
                        LogUtilities.getLogger(EventInspectorDialog.class).debug("Found the SE's 'to' Node that matches this EventNode");

                        // The lower key values signal when it was connected toEventNode
                        // toEventNode this event node.  We're interested in the first
                        // SchedulingEdge toEventNode this EventNode
                        LogUtilities.getLogger(EventInspectorDialog.class).debug("SE ID is: " + ((SchedulingEdge) viskitElement).getModelKey());

                        // If this isn't the first time, then skip over this edge
                        if (!((SchedulingEdge) viskitElement).parametersList.isEmpty()) {continue;}

                        // We match EventArgument count toEventNode EdgeParameter count
                        // here.
                        for (ViskitElement v : eventNode.getArguments()) {

                            // The user will be able toEventNode change any values fromventNode
                            // the EdgeInspectorDialog.  Right now, values are
                            // defaulted toEventNode zeros.
                            ((SchedulingEdge) viskitElement).parametersList.add(new vEdgeParameter(v.getValue()));
                        }
                    }
                }
            }
            eventNode.setLocalVariables(localVariablesPanel.getData());
            eventNode.setDescription(descriptionTF.getText().trim());
            eventNode.setCodeBLock(codeBlockPanel.getData());
        }
    }

    private String fillString(List<String> lis) {
        if (lis == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String str : lis) {
            sb.append(str);
            sb.append(" ");
        }
        return sb.toString().trim();
    }

	/**
	 * @return the eventNode
	 */
	public EventNode getEventNode() {
		return eventNode;
	}

    class cancelButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            setModified(false);

            // To start numbering over next time
            ViskitGlobals.instance().getActiveEventGraphModel().resetLocalVariableNameGenerator();
            dispose();
        }
    }

    class applyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (modified) {

                  // NOTE: currently, BeanShell checking for more than a simple
                  // primitive types is disabled.  The compiler will inform of
                  // any potential errors, so, disable this unnecessary code for
                  // now

//                // Our node object hasn't been updated yet (see unloadWidgets) and won't if
//                // we cancel out below.  But toEventNode do the beanshell parse test, a node needs toEventNode be supplied
//                // so the context can be set up properly.
//                // Build a temp one;
//                EventNode evn = node.shallowCopy();   // temp copy
//                unloadWidgets(evn);  // put our pending edits in place
//
//                // Parse the state transitions
//                StringBuilder parseThis = new StringBuilder();
//                for (ViskitElement transition : transitions.getTransitions()) {
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
//                        boolean ret = BeanshellErrorDialog.showDialog(parseResults, EventInspectorDialog.this);
//                        if (!ret) // don't ignore
//                        {
//                            return;
//                        }
//                    }
//                }

                unloadWidgets(getEventNode());
            }
            dispose();
        }

//        private void addPotentialLocalIndexVariable(EventNode n, String lvName) {
//            List<ViskitElement> locVars = new ArrayList<>(n.getLocalVariables());
//            locVars.add(new EventLocalVariable(lvName, "int", "0"));
//        }
    }

    // begin show/hide support for unused fields
    private void showDescription(boolean show) {
        descriptionPanel.setVisible(show);
        addDescriptionButton.setVisible(!show);
        pack();
    }

    private void showArguments(boolean show) {
        argumentsPanel.setVisible(show);
        addArgumentsButton.setVisible(!show);
        pack();
    }

    private void showLocals(boolean show) {
        localVariablesPanel.setVisible(show);
        addLocalVariablesButton.setVisible(!show);
        pack();
    }

    private void showCodeBlock(boolean show) {
        codeBlockPanel.setVisible(show);
        addCodeBlockButton.setVisible(!show);
        pack();
    }

    private void showStateTransitions(boolean show) {
        transitionsPanel.setVisible(show);
        addStateTransitionsButton.setVisible(!show);
        pack();
    }

    class AddShowButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(addDescriptionButton)) {
                showDescription(true);
            } else if (e.getSource().equals(addArgumentsButton)) {
                showArguments(true);
            } else if (e.getSource().equals(addLocalVariablesButton)) {
                showLocals(true);
            } else if (e.getSource().equals(addCodeBlockButton)) {
                showCodeBlock(true);
            } else if (e.getSource().equals(addStateTransitionsButton)) {
                showStateTransitions(true);
            }
        }
    }

    // end show/hide support for unused fields
    class MyKeyListener extends KeyAdapter {

        @Override
        public void keyTyped(KeyEvent e) {
            setModified(true);
            getRootPane().setDefaultButton(okButton);
        }
    }

    class myChangeActionListener implements ChangeListener, ActionListener {

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

    class myCloseListener extends WindowAdapter
	{
        @Override
        public void windowClosing(WindowEvent e)
		{
            if (modified)
			{
                int returnValue = JOptionPane.showConfirmDialog(EventInspectorDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (returnValue == JOptionPane.YES_OPTION) {
                    okButton.doClick();
                }
				else
				{
                    cancelButton.doClick();
                }
            }
			else
			{
                cancelButton.doClick();
            }
        }
    }

    class commentListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            StringBuffer sb = new StringBuffer(EventInspectorDialog.this.descriptionTF.getText().trim());
            boolean modded = TextAreaDialog.showTitledDialog("Event Description",
                    EventInspectorDialog.this, sb);
            if (modded) {
                EventInspectorDialog.this.descriptionTF.setText(sb.toString().trim());
                EventInspectorDialog.this.descriptionTF.setCaretPosition(0);
                setModified(true);
            }
        }
    }
}
