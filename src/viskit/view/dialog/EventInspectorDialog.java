package viskit.view.dialog;

import edu.nps.util.LogUtils;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import viskit.VGlobals;
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
    private Component locationComponent;
    private EventNode node;
    private static boolean modified = false;
    private JTextField name;
    private JTextField description;
    private JPanel descriptionPanel;
    private TransitionsPanel transitions;
    private ArgumentsPanel arguments;
    private LocalVariablesPanel localVariables;
    private CodeBlockPanel codeBlock;
    private JButton cancelButton, okButton;
    private JButton addDescriptionButton, addArgumentsButton, addLocalsButton, addCodeBlockButton, addStateTransitionsButton;

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
    public static boolean showDialog(JFrame f, EventNode node) {
        if (dialog == null) {
            dialog = new EventInspectorDialog(f, node);
        } else {
            dialog.setParams(f, node);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private EventInspectorDialog(JFrame frame, EventNode node) {
        super(frame, "Event Inspector: " + node.getName(), true);
        this.node = node;
        this.locationComponent = frame;

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        JPanel panel = new JPanel();
        setContentPane(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));

        // name
        JPanel namePan = new JPanel();
        namePan.setLayout(new BoxLayout(namePan, BoxLayout.X_AXIS));
        namePan.setOpaque(false);
        namePan.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Event name")));
        name = new JTextField(30); // This sets the "preferred width" when this dialog is packed
        name.setOpaque(true);
        name.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        namePan.add(name);
        // make the field expand only horiz.
        Dimension d = namePan.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        namePan.setMaximumSize(new Dimension(d));
        panel.add(namePan);

        descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.X_AXIS));
        descriptionPanel.setOpaque(false);
        descriptionPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Description")));
        description = new JTextField("");
        description.setOpaque(true);
        description.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        descriptionPanel.add(description);
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

        // Event arguments
        arguments = new ArgumentsPanel(300, 2);
        arguments.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Event arguments")));
        panel.add(arguments);

        // local vars
        localVariables = new LocalVariablesPanel(300, 2);
        localVariables.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Local variables")));
        panel.add(localVariables);

        // code block
        codeBlock = new CodeBlockPanel(this, true, "Event Code Block");
        codeBlock.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("Code block")));
        panel.add(codeBlock);

        // state transitions
        transitions = new TransitionsPanel();
        transitions.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), BorderFactory.createTitledBorder("State transitions")));
        panel.add(transitions);

        // buttons
        JPanel twoRowButtonPanel = new JPanel();
        twoRowButtonPanel.setLayout(new BoxLayout(twoRowButtonPanel, BoxLayout.Y_AXIS));

        JPanel addButtonPanel = new JPanel();
        addButtonPanel.setLayout(new BoxLayout(addButtonPanel, BoxLayout.X_AXIS));
        addButtonPanel.setBorder(new TitledBorder("add"));

        addDescriptionButton = new JButton("description"); //add description");
        addArgumentsButton = new JButton("arguments"); //add arguments");
        addLocalsButton = new JButton("locals"); //add locals");
        addCodeBlockButton = new JButton("code block"); //add code block");
        addStateTransitionsButton = new JButton("state transitions"); //add state transitions");

        //Font defButtFont = addDescriptionButton.getFont();
        //int defButtFontSize = defButtFont.getSize();
        //addDescriptionButton.setFont(defButtFont.deriveFont((float) (defButtFontSize - 4)));
        addArgumentsButton.setFont(addDescriptionButton.getFont());
        addLocalsButton.setFont(addDescriptionButton.getFont());
        addCodeBlockButton.setFont(addDescriptionButton.getFont());
        addStateTransitionsButton.setFont(addDescriptionButton.getFont());

        addButtonPanel.add(Box.createHorizontalGlue());
        addButtonPanel.add(addDescriptionButton);
        addButtonPanel.add(addArgumentsButton);
        addButtonPanel.add(addLocalsButton);
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
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        twoRowButtonPanel.add(buttonPanel);

        panel.add(twoRowButtonPanel);

        fillWidgets();     // put the data into the widgets
        sizeAndPosition();

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        addHideButtListener hideList = new addHideButtListener();
        addDescriptionButton.addActionListener(hideList);
        addArgumentsButton.addActionListener(hideList);
        addLocalsButton.addActionListener(hideList);
        addCodeBlockButton.addActionListener(hideList);
        addStateTransitionsButton.addActionListener(hideList);

        myChangeActionListener myChangeListener = new myChangeActionListener();
        //name.addActionListener(chlis);
        KeyListener klis = new myKeyListener();
        name.addKeyListener(klis);
        description.addKeyListener(klis);
        editDescriptionButton.addActionListener(new commentListener());

        arguments.addPlusListener(myChangeListener);
        arguments.addMinusListener(myChangeListener);
        arguments.addDoubleClickedListener(new ActionListener() {

            // EventArgumentDialog: Event arguments
            @Override
            public void actionPerformed(ActionEvent e) {
                EventArgument ea = (EventArgument) e.getSource();
                boolean modified = EventArgumentDialog.showDialog(
                        (JFrame) locationComponent,
                        ea);
                if (modified) {
                    arguments.updateRow(ea);
                    setModified(modified);
                }
            }
        });

        codeBlock.addUpdateListener(myChangeListener);

        localVariables.addPlusListener(myChangeListener);
        localVariables.addMinusListener(myChangeListener);
        localVariables.addDoubleClickedListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                EventLocalVariable elv = (EventLocalVariable) e.getSource();
                boolean modified = LocalVariableDialog.showDialog(
                        (JFrame) locationComponent,
                        elv);
                if (modified) {
                    localVariables.updateRow(elv);
                    setModified(modified);
                }
            }
        });

        transitions.addPlusListener(myChangeListener);
        transitions.addMinusListener(myChangeListener);
        transitions.addDoubleClickedListener(new MouseAdapter() {

            // EventStateTransitionDialog: State transition
            // bug fix 1183
            @Override
            public void mouseClicked(MouseEvent e) {
                EventStateTransition est = (EventStateTransition) e.getSource();
                boolean modified = EventStateTransitionDialog.showDialog(
                        (JFrame) locationComponent,
                        est,
                        arguments,
                        localVariables);
                if (modified) {
                    transitions.updateTransition(est);
                    setModified(modified);
                }
            }
        });
    }

    private void setModified(boolean f) {
        okButton.setEnabled(f);
        modified = f;
    }

    private void sizeAndPosition() {
        pack();     // do this prior to next

        // little check to add some extra space to always include the node name
        // in title bar w/out dotdotdots
        if (getWidth() < 350) {
            setSize(350, getHeight());
        }
        setLocationRelativeTo(locationComponent);
    }

    public void setParams(Component c, EventNode en) {
        node = en;
        locationComponent = c;

        fillWidgets();
        sizeAndPosition();
    }

    private void fillWidgets() {
        String nmSt = node.getName();
        nmSt = nmSt.replace(' ', '_');
        setTitle("Event Inspector: " + nmSt);
        name.setText(nmSt);

        Dimension d = description.getPreferredSize();
        String s = fillString(node.getComments());
        description.setText(s);
        description.setCaretPosition(0);
        description.setPreferredSize(d);

        hideShowDescription(s != null && !s.isEmpty());

        s = node.getCodeBlock();
        codeBlock.setData(s);
        codeBlock.setVisibleLines(1);
        hideShowCodeBlock(s != null && !s.isEmpty());

        transitions.setTransitions(node.getTransitions());
        s = transitions.getString();
        hideShowStateTransitions(s != null && !s.isEmpty());

        arguments.setData(node.getArguments());
        hideShowArguments(!arguments.isEmpty());

        localVariables.setData(node.getLocalVariables());
        hideShowLocals(!localVariables.isEmpty());

        setModified(false);
        getRootPane().setDefaultButton(cancelButton);
    }

    private void unloadWidgets(EventNode en) {
        if (modified) {
            en.setName(name.getText().trim().replace(' ', '_'));

            en.setTransitions(transitions.getTransitions());

            // Bug 1373: This is how an EventNode will have knowledge
            // of edge parameter additions, or removals
            en.setArguments(arguments.getData());

            // Bug 1373: This is how we will now sync up any SchedulingEdge
            // parameters with corresponding EventNode parameters
            // TODO: Recheck bug and verify this isn't don't elsewhere.  W/O
            // the continue statement, it nukes edge values that were already
            // there if we modify a node
            for (ViskitElement ve : en.getConnections()) {

                // Okay, it's a SchedulingEdge
                if (ve instanceof SchedulingEdge) {

                    // and, this SchedulingEdge is going to this node
                    if (((SchedulingEdge) ve).to.getName().equals(en.getName())) {
                        LogUtils.getLogger(EventInspectorDialog.class).debug("Found the SE's 'to' Node that matches this EventNode");

                        // The lower key values signal when it was connected to
                        // to this event node.  We're interested in the first
                        // SchedulingEdge to this EventNode
                        LogUtils.getLogger(EventInspectorDialog.class).debug("SE ID is: " + ((SchedulingEdge) ve).getModelKey());

                        // If this isn't the first time, then skip over this edge
                        if (!((SchedulingEdge) ve).parameters.isEmpty()) {continue;}

                        // We match EventArgument count to EdgeParameter count
                        // here.
                        for (ViskitElement v : en.getArguments()) {

                            // The user will be able to change any values from
                            // the EdgeInspectorDialog.  Right now, values are
                            // defaulted to zeros.
                            ((SchedulingEdge) ve).parameters.add(new vEdgeParameter("0"));
                        }
                    }
                }
            }
            en.setLocalVariables(new Vector<>(localVariables.getData()));
            en.getComments().clear();
            en.getComments().add(description.getText().trim());
            en.setCodeBLock(codeBlock.getData());
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

    class cancelButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            setModified(false);

            // To start numbering over next time
            VGlobals.instance().getActiveEventGraphModel().resetLVNameGenerator();
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
//                // we cancel out below.  But to do the beanshell parse test, a node needs to be supplied
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
//                    String parseResults = VGlobals.instance().parseCode(evn, ps);
//                    if (parseResults != null) {
//                        boolean ret = BeanshellErrorDialog.showDialog(parseResults, EventInspectorDialog.this);
//                        if (!ret) // don't ignore
//                        {
//                            return;
//                        }
//                    }
//                }

                unloadWidgets(node);
            }
            dispose();
        }

        private void addPotentialLocalIndexVariable(EventNode n, String lvName) {
            Vector<ViskitElement> locVars = n.getLocalVariables();
            locVars.add(new EventLocalVariable(lvName, "int", "0"));
        }
    }

    // begin show/hide support for unused fields
    private void hideShowDescription(boolean show) {
        descriptionPanel.setVisible(show);
        addDescriptionButton.setVisible(!show);
        pack();
    }

    private void hideShowArguments(boolean show) {
        arguments.setVisible(show);
        addArgumentsButton.setVisible(!show);
        pack();
    }

    private void hideShowLocals(boolean show) {
        localVariables.setVisible(show);
        addLocalsButton.setVisible(!show);
        pack();
    }

    private void hideShowCodeBlock(boolean show) {
        codeBlock.setVisible(show);
        addCodeBlockButton.setVisible(!show);
        pack();
    }

    private void hideShowStateTransitions(boolean show) {
        transitions.setVisible(show);
        addStateTransitionsButton.setVisible(!show);
        pack();
    }

    class addHideButtListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(addDescriptionButton)) {
                hideShowDescription(true);
            } else if (e.getSource().equals(addArgumentsButton)) {
                hideShowArguments(true);
            } else if (e.getSource().equals(addLocalsButton)) {
                hideShowLocals(true);
            } else if (e.getSource().equals(addCodeBlockButton)) {
                hideShowCodeBlock(true);
            } else if (e.getSource().equals(addStateTransitionsButton)) {
                hideShowStateTransitions(true);
            }
        }
    }

    // end show/hide support for unused fields
    class myKeyListener extends KeyAdapter {

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

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int ret = JOptionPane.showConfirmDialog(EventInspectorDialog.this, "Apply changes?",
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

    class commentListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            StringBuffer sb = new StringBuffer(EventInspectorDialog.this.description.getText().trim());
            boolean modded = TextAreaDialog.showTitledDialog("Event Description",
                    EventInspectorDialog.this, sb);
            if (modded) {
                EventInspectorDialog.this.description.setText(sb.toString().trim());
                EventInspectorDialog.this.description.setCaretPosition(0);
                setModified(true);
            }
        }
    }
}
