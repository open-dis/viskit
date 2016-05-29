package viskit.view.dialog;

import edu.nps.util.LogUtilities;
import edu.nps.util.SpringUtilities;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Vector;
import javax.swing.text.JTextComponent;
import org.apache.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.control.AssemblyController;
import viskit.model.AssemblyEdge;
import viskit.model.EventGraphNode;
import viskit.model.PropertyChangeListenerEdge;
import viskit.model.PropertyChangeListenerNode;
import viskit.model.ViskitElement;

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
public class PclEdgeInspectorDialog extends JDialog
{	
    static final Logger LOG = LogUtilities.getLogger(PclEdgeInspectorDialog.class);
	
    private JLabel  sourceLabel,  propertyLabel,  descriptionLabel,  targetLabel;
    private JTextField sourceTF,  propertyTF,     descriptionTF,     targetTF;
    private JPanel propertyTFPanel;
    private static PclEdgeInspectorDialog pclEdgeInspectorDialog;
    private static boolean modified = false;
    private PropertyChangeListenerEdge pclEdge;
    private JButton okButton,  cancelButton;
    private JButton findStateVariableButton;
    private JPanel buttonPanel, contentPanel, containerPanel;
    private enableApplyButtonListener listener;
	
	public static final String ALL_STATE_VARIABLES_NAME        = "(All state variables)";
	public static final String ALL_STATE_VARIABLES_TYPE        = "(any type)";
	public static final String ALL_STATE_VARIABLES_DESCRIPTION = "Property Change events from all state variables get heard";

    private PclEdgeInspectorDialog(JFrame parent, PropertyChangeListenerEdge pclEdge)
	{
        super(parent, "Property Change Connection", true);
        this.pclEdge = pclEdge;
		
		initialize ();

        setParameters(parent, this.pclEdge);
    }
	
	private void initialize ()
	{
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        listener = new enableApplyButtonListener();
        findStateVariableButton = new JButton("...");
        findStateVariableButton.addActionListener(new ChooseStateVariableAction());
        findStateVariableButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
             sourceLabel = new JLabel("from: source event graph", JLabel.TRAILING);
           propertyLabel = new JLabel("state variable",     JLabel.TRAILING); // originally labelled "property" but that seems misleading...
        descriptionLabel = new JLabel("description",        JLabel.TRAILING);
             targetLabel = new JLabel("to: property change listener", JLabel.TRAILING);
			 
		     sourceLabel.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
	       propertyLabel.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
		descriptionLabel.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
	     	 targetLabel.setAlignmentX(JLabel.RIGHT_ALIGNMENT);

             sourceTF = new JTextField();
             targetTF = new JTextField();
           propertyTF = new JTextField();
        descriptionTF = new JTextField(ALL_STATE_VARIABLES_DESCRIPTION + "  "); // initialize width
        propertyTFPanel = new JPanel();

        propertyTFPanel = new JPanel();
        propertyTFPanel.setLayout(new BoxLayout(propertyTFPanel, BoxLayout.X_AXIS));
        propertyTFPanel.add(propertyTF);
        propertyTFPanel.add(findStateVariableButton);
        propertyTF.addCaretListener(listener);
        pairWidgets(  sourceLabel, sourceTF, false);
        pairWidgets(propertyLabel, propertyTFPanel, true);
        pairWidgets(descriptionLabel, descriptionTF, true);
        pairWidgets(  targetLabel, targetTF, false);

           okButton = new JButton("Apply changes");
       cancelButton = new JButton("Cancel");
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createHorizontalStrut(5));

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
            okButton.addActionListener(new applyButtonListener());

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

        containerPanel = new JPanel(new SpringLayout());
        containerPanel.add(sourceLabel);
        containerPanel.add(sourceTF);
        containerPanel.add(propertyLabel);
        containerPanel.add(propertyTFPanel);
        containerPanel.add(targetLabel);
        containerPanel.add(targetTF);
        containerPanel.add(descriptionLabel);
        containerPanel.add(descriptionTF);
        SpringUtilities.makeCompactGrid(containerPanel, 4, 2, 10, 10, 10, 10);
        contentPanel.add(containerPanel);

        contentPanel.add(buttonPanel);
        contentPanel.add(Box.createVerticalStrut(5));
        setContentPane(contentPanel);

        // Make the first display of this panel a minimum of 600 width
        Dimension d = getSize();
        d.width = Math.max(d.width, 600);
        setSize(d);
	}

    public static boolean showDialog(JFrame parentFrame, PropertyChangeListenerEdge parm)
	{
        if (pclEdgeInspectorDialog == null)
		{ 
            pclEdgeInspectorDialog = new PclEdgeInspectorDialog(parentFrame, parm);
        } 
		else 
		{
            pclEdgeInspectorDialog.setParameters(parentFrame, parm);
        }
        pclEdgeInspectorDialog.setVisible(true); // this call blocks while panel dialog is shown
		
        return modified;
    }

    private void pairWidgets(JLabel label, JComponent textField, boolean editable)
	{
        ViskitStatics.clampHeight(textField);
        label.setLabelFor(textField);
        if (textField instanceof JTextField)
		{
            ((JTextComponent) textField).setEditable(editable);
            if (editable)
			{
                ((JTextComponent) textField).addCaretListener(listener);
            }
        }
    }

    public final void setParameters(Component c, PropertyChangeListenerEdge p)
	{
        pclEdge = p;

        fillWidgets();

        modified = (p == null);
        okButton.setEnabled((p == null));

        getRootPane().setDefaultButton(cancelButton);
        pack();
        setLocationRelativeTo(c);
    }

    private void fillWidgets()
	{
        if (pclEdge != null)
		{
			String fromText, toText;
			try
			{
				String fromClassName    = ((EventGraphNode) pclEdge.getFromObject()).getType();
				if    (fromClassName == null)
					   fromClassName    = "";
				if    (fromClassName.contains("."))
					   fromClassName    = fromClassName.substring(fromClassName.lastIndexOf(".")+1);
				String fromInstanceName = ((EventGraphNode) pclEdge.getFromObject()).getName();
				if    (fromInstanceName == null)
					   fromInstanceName = "";
				if   (!fromInstanceName.toLowerCase().contains(fromClassName.toLowerCase()))
				{
					 fromText = fromClassName + " " + fromInstanceName;
				}
				else fromText =                       fromInstanceName;
			}
			catch (Exception e)
			{
				fromText = pclEdge.getFromObject().toString();
			}
			
			try
			{
				String toClassName    = ((PropertyChangeListenerNode) pclEdge.getToObject()).getType();
				if    (toClassName == null)
					   toClassName    = "";
				if    (toClassName.contains("."))
					   toClassName    = toClassName.substring(toClassName.lastIndexOf(".")+1);
				String toInstanceName = ((PropertyChangeListenerNode) pclEdge.getToObject()).getName();
				if    (toInstanceName == null)
					   toInstanceName = "";
				if   (!toInstanceName.toLowerCase().contains(toClassName.toLowerCase()))
				{
					 toText = toClassName + " " + toInstanceName;
				}
				else toText =                     toInstanceName;
			}
			catch (Exception e)
			{
				toText = pclEdge.getFromObject().toString();
			}
			
			     sourceTF.setText(fromText);
	   		     targetTF.setText(  toText);
               propertyTF.setText(pclEdge.getProperty());
            descriptionTF.setText(pclEdge.getDescription());
			
			if (pclEdge.getProperty().trim().isEmpty())
			{
               propertyTF.setText       (ALL_STATE_VARIABLES_NAME);
               propertyTF.setToolTipText(ALL_STATE_VARIABLES_DESCRIPTION);
            descriptionTF.setText       (ALL_STATE_VARIABLES_DESCRIPTION);
			}
			SpringUtilities.makeCompactGrid(containerPanel, 4, 2, 10, 10, 10, 10);
			pack();
        }
		else // why are we here?  presumably some code failure
		{
                 sourceTF.setText("unset...shouldn't see this");
                 targetTF.setText("unset...shouldn't see this");
                propertyTF.setText("listened-to property");
            descriptionTF.setText("");
        }
    }

    private void unloadWidgets()
	{
        if (pclEdge != null)
		{
			String selectedName = propertyTF.getText().trim();
			if (selectedName.equalsIgnoreCase(ALL_STATE_VARIABLES_NAME))
			{
				 pclEdge.setProperty ("");
			}
			else pclEdge.setProperty (selectedName);
            
            pclEdge.setDescription(descriptionTF.getText().trim());
        }
    }

    class cancelButtonListener implements ActionListener
	{
        @Override
        public void actionPerformed(ActionEvent event)
		{
            modified = false;    // for the caller
            dispose();
        }
    }

    class applyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event)
		{
            if (modified)
			{
                unloadWidgets();
            }
            dispose(); // release screen resources for window
        }
    }

    class enableApplyButtonListener implements CaretListener, ActionListener {

        @Override
        public void caretUpdate(CaretEvent event)
		{
            modified = true;
            okButton.setEnabled(true);
            getRootPane().setDefaultButton(okButton);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            caretUpdate(null);
        }
    }

	/**
	 * Show parameterized generic types for user selection
	 */
    class ChooseStateVariableAction implements ActionListener
	{
        @Override
        public void actionPerformed(ActionEvent e)
		{
            Object objectFromEdge = pclEdge.getFromObject();
            String objectClassName = "";
            if (objectFromEdge instanceof EventGraphNode)
			{
                objectClassName = ((ViskitElement) objectFromEdge).getType();
            } 
			else if (objectFromEdge instanceof PropertyChangeListenerNode)
			{
                objectClassName = ((ViskitElement) objectFromEdge).getType();
            }
            try {
                Class<?> objectBaseClass = ViskitStatics.ClassForName(objectClassName);
                if (objectBaseClass == null)
				{
                    throw new ClassNotFoundException(objectClassName + " not found");
                }
				if (objectClassName.contains("."))
				{
					objectClassName = objectClassName.substring(objectClassName.lastIndexOf(".")+1);
				}
                Class<?> stopClass = ViskitStatics.ClassForName("simkit.BasicSimEntity");
                BeanInfo beanInfo = Introspector.getBeanInfo(objectBaseClass, stopClass);
                PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
                if (propertyDescriptors == null || propertyDescriptors.length <= 0)
				{
                    ((AssemblyController)ViskitGlobals.instance().getAssemblyController()).messageToUser(
                            JOptionPane.INFORMATION_MESSAGE,
                            "No properties found in " + objectClassName,
                            "Enter name manually.");
                    return;
                }
                Vector<String>        nameVector = new Vector<>();
                Vector<String>        typeVector = new Vector<>();
                Vector<String> descriptionVector = new Vector<>();
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors)
				{
                    if (propertyDescriptor.getWriteMethod() != null)
					{
                        continue; // want getters but not setters, go to next property
                    }
                    // Warning: the introspector will return property names in lower case, this error is then fixed
					String correctedCaseName = propertyDescriptor.getName();
					String       description = propertyDescriptor.getShortDescription();
					for (AssemblyEdge connection : ((EventGraphNode) objectFromEdge).getConnections())
					{
						if (connection instanceof PropertyChangeListenerEdge)
						{
							if (correctedCaseName.equalsIgnoreCase(((PropertyChangeListenerEdge)connection).getProperty()))
							{
								correctedCaseName = ((PropertyChangeListenerEdge)connection).getProperty();
								description       = ((PropertyChangeListenerEdge)connection).getDescription();
								break;
							}
						}
						else // unexpected problem
						{
							LOG.error ("Found connection of unexpected class: " + connection.toString());
						}
					}
                           nameVector.add(correctedCaseName);
                    descriptionVector.add(description);

                    if  (propertyDescriptor.getPropertyType() != null)
                         typeVector.add(propertyDescriptor.getPropertyType().getName());
					else typeVector.add("");
                }
				
				// Build display
                String[][] nameTypeDescriptionArray = new String[nameVector.size()+1][3];
				nameTypeDescriptionArray[0][0] = ALL_STATE_VARIABLES_NAME;
				nameTypeDescriptionArray[0][1] = ALL_STATE_VARIABLES_TYPE;
				nameTypeDescriptionArray[0][2] = ALL_STATE_VARIABLES_DESCRIPTION;
				
                for (int i = 0; i < nameVector.size(); i++)
				{
                    nameTypeDescriptionArray[i+1][0] =        nameVector.get(i);
                    nameTypeDescriptionArray[i+1][1] =    typeVector.get(i);
                    nameTypeDescriptionArray[i+1][2] = descriptionVector.get(i);
                }
                int whichChoice = StateVariableListDialog.showDialog(PclEdgeInspectorDialog.this,
                      "State Variables for event graph " + objectClassName,
                      nameTypeDescriptionArray);
                if (whichChoice != StateVariableListDialog.NO_SELECTION)
				{
                    modified = true;
                       propertyTF.setText(nameTypeDescriptionArray[whichChoice][0]);
                    descriptionTF.setText(nameTypeDescriptionArray[whichChoice][2]);
                }
            } 
			catch (ClassNotFoundException | IntrospectionException | HeadlessException e1)
			{
                LOG.error ("Exception getting bean properties, PclEdgeInspectorDialog: " + e1.getMessage());
                LOG.error (System.getProperty("java.class.path"));
            }
        }
    }

    class myCloseListener extends WindowAdapter
	{
        @Override
        public void windowClosing(WindowEvent e)
		{
            if (modified)
			{
                int returnValue = JOptionPane.showConfirmDialog(PclEdgeInspectorDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
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
