package viskit.view.dialog;

import edu.nps.util.LogUtilities;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import org.apache.log4j.Logger;

import viskit.model.EventGraphNode;
import viskit.model.ViskitInstantiator;
import viskit.ViskitStatics;
import static viskit.ViskitStatics.DEFAULT_DESCRIPTION;
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
public class EventGraphNodeInspectorDialog extends JDialog
{
    static final Logger LOG = LogUtilities.getLogger(EventGraphNodeInspectorDialog.class);
	
    public static String newName;
    public static ViskitInstantiator newInstantiator;
    private static EventGraphNodeInspectorDialog dialog;
    private static boolean modified = false;

    private final JPanel        contentPanel = new JPanel();
    private final JPanel         buttonPanel = new JPanel();
    private final JLabel     handleNameLabel = new JLabel("name", JLabel.TRAILING);
    private final JLabel    descriptionLabel = new JLabel("description", JLabel.TRAILING);
    private final JTextField    handleNameTF = new JTextField();
    private final JTextField   descriptionTF = new JTextField();
    private final JButton           okButton = new JButton("Apply changes");
    private final JButton       cancelButton = new JButton("Cancel");
    private final EnableApplyButtonListener listener = new EnableApplyButtonListener();
    private final JCheckBox   detailedOutputCheckBox = new JCheckBox("detailed output");

    private final String NAME_TOOLTIP = "Unique name for this node (no spaces allowed)";
    private InstantiationPanel instantiationPanel;
    private EventGraphNode eventGraphNode;

    public static boolean showDialog(JFrame parent, EventGraphNode eventGraphNode)
	{
        try {
            if (dialog == null)
			{
                dialog = new EventGraphNodeInspectorDialog(parent, eventGraphNode);
            } 
			else 
			{
                dialog.setParameterWidgets(parent, eventGraphNode);
            }
        } 
		catch (ClassNotFoundException e) 
		{
            String message = "An object type specified in this element (probably " + eventGraphNode.getType() + ") was not found.\n" +
                    "Add the XML or class file defining the element to the proper list at left.";
            JOptionPane.showMessageDialog(parent, message, "Event Graph definition not found", JOptionPane.ERROR_MESSAGE);
            dialog = null;
            return false; // unmodified
        }

        //Having trouble getting this beast to redraw with new data, at least on the Mac.
        //The following little bogosity works, plus the invalidate call down below.
        Dimension d = dialog.getSize();
        dialog.setSize(d.width+1, d.height+1);
        dialog.setSize(d);

        dialog.setVisible(true); // this call blocks to show dialog
       
        return modified;
    }

    private EventGraphNodeInspectorDialog(JFrame parent, EventGraphNode node) throws ClassNotFoundException
	{
        super(parent, "Assembly Editor", true); // instance
        EventGraphNodeInspectorDialog.this.eventGraphNode = node;
        EventGraphNodeInspectorDialog.this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        EventGraphNodeInspectorDialog.this.addWindowListener(new MyCloseListener());
		
		initialize ();

        setParameterWidgets(parent, node); // TODO rename setEventGraphInstanceParamaters
    }
	
	private void initialize ()
	{
        setContentPane(contentPanel);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		
        ViskitStatics.clampHeight(  handleNameTF);
        handleNameLabel.setLabelFor(handleNameTF);
        handleNameLabel.setToolTipText(NAME_TOOLTIP);
           handleNameTF.setToolTipText(NAME_TOOLTIP);
		
        detailedOutputCheckBox.setToolTipText("Enable a list dump of all entity names to the console"); // TODO improve

        ViskitStatics.clampHeight(descriptionTF);
        
        descriptionLabel.setLabelFor(descriptionTF);
        descriptionLabel.setToolTipText("Describe purpose of this event graph instance");
           descriptionTF.setToolTipText("Describe purpose of this event graph instance");
        ViskitStatics.cloneSize(handleNameLabel, descriptionLabel);    // make labels same size
		
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(    okButton);
        buttonPanel.add(cancelButton);
		
        JPanel content = (JPanel)getContentPane();
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel eventGraphIdentificationPanel = new JPanel();
        eventGraphIdentificationPanel.setLayout(new BoxLayout(eventGraphIdentificationPanel, BoxLayout.X_AXIS));
        eventGraphIdentificationPanel.add(handleNameLabel);
        eventGraphIdentificationPanel.add(Box.createHorizontalStrut(5));
        eventGraphIdentificationPanel.add(handleNameTF);
        eventGraphIdentificationPanel.add(Box.createHorizontalStrut(2));
        eventGraphIdentificationPanel.add(detailedOutputCheckBox);
        content.add(Box.createVerticalStrut(5));
        content.add(Box.createVerticalGlue());
        content.add(eventGraphIdentificationPanel);

        JPanel descriptionContent = new JPanel();
        descriptionContent.setLayout(new BoxLayout(descriptionContent, BoxLayout.X_AXIS));
        descriptionContent.add(descriptionLabel);
        descriptionContent.add(Box.createHorizontalStrut(5));
        descriptionContent.add(descriptionTF);
        content.add(Box.createVerticalStrut(5));
        content.add(Box.createVerticalGlue());
        content.add(descriptionContent);

        instantiationPanel = new InstantiationPanel(this, listener, true);
        instantiationPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                    /* Event Graph Parameter initialization*/ 
				    "Event Graph parameter initialization", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));

        instantiationPanel.setToolTipText("Event Graph parameter initialization: SimEntity creation for Simulation Run");
        instantiationPanel.setAlignmentX(Box.CENTER_ALIGNMENT);
        content.add(Box.createVerticalStrut(10));
        content.add(instantiationPanel);
        content.add(Box.createVerticalStrut(10));
        content.add(buttonPanel);

        // attach listeners
        cancelButton.addActionListener(new CancelButtonListener());
            okButton.addActionListener(new ApplyButtonListener());

         handleNameTF.addCaretListener(listener);
        descriptionTF.addCaretListener(listener);
        detailedOutputCheckBox.addActionListener(listener);
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

    private void fillWidgets() throws ClassNotFoundException
	{
        if (eventGraphNode != null)
		{
                  handleNameTF.setText    (eventGraphNode.getName());
                 descriptionTF.setText    (eventGraphNode.getDescription());
        detailedOutputCheckBox.setSelected(eventGraphNode.isVerboseMarked());
            instantiationPanel.setData    (eventGraphNode.getInstantiator());
        } 
		else
		{
            handleNameTF.setText("Event Graph node name");
            detailedOutputCheckBox.setSelected(false);
        }
		if (descriptionTF.getText().isEmpty())			
            descriptionTF.setText(DEFAULT_DESCRIPTION); // better to nag than ignore
		descriptionTF.setToolTipText(descriptionTF.getText().trim()); // readability for long descriptions
		
        okButton.setEnabled(false); // disabled until modifications occur
    }

    private void unloadWidgets()
	{
        String name = handleNameTF.getText();
        name = name.replaceAll("\\s", ""); // squeeze out illegal whitespace to ensure legal name
        if (eventGraphNode != null)
		{
            eventGraphNode.setName(name);
            eventGraphNode.setDescription(descriptionTF.getText().trim());
            eventGraphNode.setVerboseMarked(detailedOutputCheckBox.isSelected());
            eventGraphNode.setInstantiator(instantiationPanel.getData());
        } 
		else
		{
            newName = name;
            newInstantiator = instantiationPanel.getData();
        }
    }

    class CancelButtonListener implements ActionListener 
	{
        @Override
        public void actionPerformed(ActionEvent event) 
		{
            modified = false; // for the caller; no changes made
            dispose();        // all done
        }
    }

    class ApplyButtonListener implements ActionListener 
	{
        @Override
        public void actionPerformed(ActionEvent event) 
		{
            if (modified)
			{
                unloadWidgets();
                if (continueClosingDespiteBlankFields())
				{
                    return; // continue working
                }
            }
            dispose(); // release dialog, all done
        }
    }

    class EnableApplyButtonListener implements CaretListener, ActionListener
	{
        @Override
        public void caretUpdate(CaretEvent event) 
		{
            common();
        }

        @Override
        public void actionPerformed(ActionEvent event) 
		{
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
     * @return true means to continue close, despite blank fields remaining
     */
    boolean continueClosingDespiteBlankFields() 
	{
        ViskitInstantiator viskitInstantiator;

        if (eventGraphNode != null) 
		{
            viskitInstantiator = eventGraphNode.getInstantiator();
        } 
		else 
		{
            viskitInstantiator = newInstantiator;
        }
        testLp:
        {
            if (handleNameTF.getText().trim().isEmpty()) 
			{
                break testLp;
            }
            if (!viskitInstantiator.isValid()) 
			{
                break testLp;
            }
            return false; // no blank fields, don't cancel close
        }   // testLp

        // Here if we found an empty field
        int returnValue = JOptionPane.showConfirmDialog(
                EventGraphNodeInspectorDialog.this,
                "All fields will need to be completed. Close anyway?",
                "Only partially complete...",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
		boolean cancellation = (returnValue != JOptionPane.YES_OPTION); // whether or not user cancelled
        return  cancellation;
    }

    class MyCloseListener extends WindowAdapter 
	{
        @Override
        public void windowClosing(WindowEvent e) 
		{
            if (modified) 
			{
                int returnValue = JOptionPane.showConfirmDialog(
                        EventGraphNodeInspectorDialog.this,
                        "Apply changes?",
                        "Question",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (returnValue == JOptionPane.YES_OPTION) 
				{
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
}
