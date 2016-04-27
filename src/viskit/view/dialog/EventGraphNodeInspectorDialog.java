package viskit.view.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import viskit.model.EventGraphNode;
import viskit.model.VInstantiator;
import viskit.ViskitStatics;
import viskit.view.InstantiationPanel;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since June 2, 2004
 * @since 9:19:41 AM
 * @version $Id$
 */
public class EventGraphNodeInspectorDialog extends JDialog {

    public static String newName;
    public static VInstantiator newInstantiator;
    private static EventGraphNodeInspectorDialog dialog;
    private static boolean modified = false;

    private final JLabel     handleNameLabel; //,outputLab;
    private final JTextField handleNameTF;

    // verboseCheck not used, does nothing for Viskit
    private final JCheckBox detailedOutputCheckBox /*, verboseCheck*/;
    private InstantiationPanel instantiationPanel;
    private EventGraphNode eventGraphNode;
    private final JButton okButton, cancelButton;
    private final enableApplyButtonListener listener;
    private final JPanel buttonPanel;
    private final JTextField descriptionTF;
    private final JLabel descriptionLabel;

    public static boolean showDialog(JFrame parent, EventGraphNode eventGraphNode) {
        try {
            if (dialog == null) {
                dialog = new EventGraphNodeInspectorDialog(parent, eventGraphNode);
            } else {
                dialog.setParameterWidgets(parent, eventGraphNode);
            }
        } catch (ClassNotFoundException e) {
            String message = "An object type specified in this element (probably " + eventGraphNode.getType() + ") was not found.\n" +
                    "Add the XML or class file defining the element to the proper list at left.";
            JOptionPane.showMessageDialog(parent, message, "Event Graph Definition Not Found", JOptionPane.ERROR_MESSAGE);
            dialog = null;
            return false; // unmodified
        }

        //Having trouble getting this beast to redraw with new data, at least on the Mac.
        //The following little bogosity works, plus the invalidate call down below.
        Dimension d = dialog.getSize();
        dialog.setSize(d.width+1, d.height+1);
        dialog.setSize(d);

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private EventGraphNodeInspectorDialog(JFrame parent, EventGraphNode node) throws ClassNotFoundException
	{
        super(parent, "Assembly Editor: initialize new Event Graph instance", true);
        this.eventGraphNode = node;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        listener = new enableApplyButtonListener();

        JPanel content = new JPanel();
        setContentPane(content);
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        handleNameTF = new JTextField();
        ViskitStatics.clampHeight(handleNameTF);
        handleNameLabel = new JLabel("name", JLabel.TRAILING);
        handleNameLabel.setLabelFor(handleNameTF);
        handleNameLabel.setToolTipText("Unique name for this node (no spaces allowed)");
           handleNameTF.setToolTipText("Unique name for this node (no spaces allowed)");
		
        detailedOutputCheckBox = new JCheckBox("detailed output");
        detailedOutputCheckBox.setToolTipText("Enable a list dump of all entity names to the console"); // TODO improve

        descriptionTF = new JTextField();
        ViskitStatics.clampHeight(descriptionTF);
        descriptionLabel = new JLabel("description", JLabel.TRAILING);
        descriptionLabel.setLabelFor(descriptionTF);
        descriptionLabel.setToolTipText("Describe purpose of this event graph instance");
           descriptionTF.setToolTipText("Describe purpose of this event graph instance");

        ViskitStatics.cloneSize(handleNameLabel, descriptionLabel);    // make handle same size

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            okButton = new JButton("Apply changes");
        cancelButton = new JButton("Cancel");
        okButton.setEnabled(false);
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        placeWidgets(); // Object creation panel

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
            okButton.addActionListener(new applyButtonListener());

         handleNameTF.addCaretListener(listener);
        descriptionTF.addCaretListener(listener);
        detailedOutputCheckBox.addActionListener(listener);

        setParameterWidgets(parent, node);
    }

    public final void setParameterWidgets(Component relatedComponent, EventGraphNode currentEventGraphNode) throws ClassNotFoundException
	{
        eventGraphNode = currentEventGraphNode;

        fillWidgets();
        getContentPane().invalidate();
        getRootPane().setDefaultButton(cancelButton);

        pack();     // do this prior to next
        setLocationRelativeTo(relatedComponent);
    }

	/** Object creation panel */
    private void placeWidgets()
    {
        JPanel content = (JPanel)getContentPane();
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel eventGraphIdentificationPanel = new JPanel();
        eventGraphIdentificationPanel.setLayout(new BoxLayout(eventGraphIdentificationPanel, BoxLayout.X_AXIS));
        eventGraphIdentificationPanel.add(handleNameLabel);
        eventGraphIdentificationPanel.add(Box.createHorizontalStrut(5));
        eventGraphIdentificationPanel.add(handleNameTF);
        eventGraphIdentificationPanel.add(Box.createHorizontalStrut(2));
        eventGraphIdentificationPanel.add(detailedOutputCheckBox);
        content.add(eventGraphIdentificationPanel);

        JPanel descriptionContent = new JPanel();
        descriptionContent.setLayout(new BoxLayout(descriptionContent, BoxLayout.X_AXIS));
        descriptionContent.add(descriptionLabel);
        descriptionContent.add(Box.createHorizontalStrut(5));
        descriptionContent.add(descriptionTF);
        content.add(descriptionContent);

        instantiationPanel = new InstantiationPanel(this, listener, true);

        instantiationPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                    "Object creation: parameter initialization", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));

        instantiationPanel.setAlignmentX(Box.CENTER_ALIGNMENT);
        content.add(instantiationPanel);
        content.add(Box.createVerticalStrut(5));
        content.add(buttonPanel);
    }

    private void fillWidgets() throws ClassNotFoundException
	{
        if (eventGraphNode != null)
		{
            handleNameTF.setText(eventGraphNode.getName());
            detailedOutputCheckBox.setSelected(eventGraphNode.isOutputMarked());
            descriptionTF.setText(eventGraphNode.getDescription());
            instantiationPanel.setData(eventGraphNode.getInstantiator());
        } 
		else
		{
            handleNameTF.setText("Event graph node name");
            detailedOutputCheckBox.setSelected(false);
        }
		if (descriptionTF.getText().isEmpty())			
            descriptionTF.setText("TODO add description"); // better to nag than ignore
    }

    private void unloadWidgets()
	{
        String name = handleNameTF.getText();
        name = name.replaceAll("\\s", ""); // squeeze out illegal whitespace
        if (eventGraphNode != null)
		{
            eventGraphNode.setName(name);
            eventGraphNode.setDescription(descriptionTF.getText().trim());
            eventGraphNode.setInstantiator(instantiationPanel.getData());
            eventGraphNode.setOutputMarked(detailedOutputCheckBox.isSelected());
        } 
		else
		{
            newName = name;
            newInstantiator = instantiationPanel.getData();
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
                if (checkBlankFields()) {
                    return;
                }
            }
            dispose();
        }
    }

    class enableApplyButtonListener implements CaretListener, ActionListener {

        @Override
        public void caretUpdate(CaretEvent event) {
            common();
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            common();
        }

        private void common()
        {
            modified = true;
            okButton.setEnabled(true);
            getRootPane().setDefaultButton(okButton);
        }
    }

    /**
     * Check for blank fields and return true if user wants to cancel close
     * @return true = cancel close
     */
    boolean checkBlankFields() {
        VInstantiator vi;

        if (eventGraphNode != null) {
            vi = eventGraphNode.getInstantiator();
        } else {
            vi = newInstantiator;
        }
        testLp:
        {
            if (handleNameTF.getText().trim().isEmpty()) {
                break testLp;
            }
            if (!vi.isValid()) {
                break testLp;
            }
            return false; // no blank fields, don't cancel close
        }   // testLp

        // Here if we found a problem
        int returnValue = JOptionPane.showConfirmDialog(
                EventGraphNodeInspectorDialog.this,
                "All fields need to be completed. Close anyway?",
                "Question",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return returnValue != JOptionPane.YES_OPTION; // don't cancel
        // cancel close
    }

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int ret = JOptionPane.showConfirmDialog(
                        EventGraphNodeInspectorDialog.this,
                        "Apply changes?",
                        "Question",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
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
