package viskit.view.dialog;

import edu.nps.util.LogUtilities;
import viskit.model.ViskitStateVariable;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.ViskitConfiguration;
import viskit.ViskitStatics;
import static viskit.xsd.translator.eventgraph.SimkitEventGraphXML2Java.*;
import viskit.model.EventGraphModel;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @version $Id$
 */
public class StateVariableDialog extends ViskitSmallDialog 
{
    static final Logger LOG = LogUtilities.getLogger(StateVariableDialog.class);
	
    private static ViskitStateVariable viskitStateVariable;
	/** Text field that holds the parameter name */
    private final JTextField stateVariableNameField = new JTextField(25);
	/** Text field that holds the initial value */
    private JTextField initialValueField;
	/** Text field that holds the description */
    private JTextField descriptionField;
	
    private JTextField arraySizeField;
	private final JCheckBox         implicitCheckBox = new JCheckBox();
    private       JComboBox<String> stateVariableTypeComboBox;    // Editable combo box that lets us select a type
    private       Component myTyperComponent;                     // i.e., the editor of the type JComboBox
	
    private final JLabel     implicitLabel = new JLabel("implicit");
    private final JLabel         nameLabel = new JLabel("name");
    private final JLabel         typeLabel = new JLabel("type");
    private final JLabel initialValueLabel = new JLabel("initial value");
    private final JLabel  descriptionLabel = new JLabel("description");    
    private final JLabel    arraySizeLabel = new JLabel("array size");
	
    private final JButton     okButton = new JButton("Apply changes");
    private final JButton cancelButton = new JButton("Cancel");
    public static String newName,  newType, newValue, newDescription;
	public static boolean newImplicit;
    private final myFocusListener focusListener;
    private final EnableApplyButtonListener listener = new EnableApplyButtonListener(okButton);
	
	private final String IMPLICIT_TOOLTIP     = "Implicit state variables cannot be set, instead they are computed from other state variables whenever needed";
	private final String IMPLICIT_VALUE_LABEL = "* implicit *";
	
	private       String priorValue = new String();

    public static boolean showDialog(JFrame f, ViskitStateVariable passedStateVariable) 
	{
		viskitStateVariable = passedStateVariable;
		if (viskitStateVariable == null)
		{
			// creating a new state variable
			viskitStateVariable = new ViskitStateVariable ();
			LOG.info ("creating new stateVariable");
			setNewObjectInitialization(true);
		}
        return showDialog(StateVariableDialog.class.getName(), f, viskitStateVariable); // static invocation
    }

    protected StateVariableDialog(JFrame parent, Object parameter)
	{
        super(parent, "State Variable Declaration", true); // Inspector
		
		initialize ();

        focusListener = new myFocusListener();

        setParameters(parent, parameter);
	}
	private void initialize ()
	{
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                new EmptyBorder(10, 10, 10, 10)));

        content.add(Box.createVerticalStrut(5));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));

        int width = OneLinePanel.maxWidth(new JComponent[] {implicitLabel, nameLabel, typeLabel, initialValueLabel, descriptionLabel});
		
		implicitLabel.setToolTipText(IMPLICIT_TOOLTIP);
		implicitCheckBox.setToolTipText(IMPLICIT_TOOLTIP);
		implicitCheckBox.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent e) 
			{
				boolean implicitSelected = implicitCheckBox.isSelected();
				initialValueField.setEnabled(!implicitSelected);
				if (implicitSelected)
				{
					priorValue = initialValueField.getText(); // remember in case user changes mind
					if (priorValue.equals(IMPLICIT_VALUE_LABEL) || priorValue.trim().isEmpty())
						priorValue = "TODO"; // force setting a value
					initialValueField.setText(IMPLICIT_VALUE_LABEL); // no value allowed
					initialValueField.setToolTipText(IMPLICIT_TOOLTIP);
				}
				else // no longer implicit
				{
					if (priorValue.equals(IMPLICIT_VALUE_LABEL) || priorValue.trim().isEmpty())
						priorValue = "TODO"; // force setting a value
					initialValueField.setText(priorValue); // restore (if user changed mind)
					initialValueField.setToolTipText(priorValue); // readability
				}
				modified = true;
				okButton.setEnabled(true);
				getRootPane().setDefaultButton(okButton);  // in JDialog
			}
		});

        stateVariableNameField.addFocusListener(focusListener);
        setMaxHeight(stateVariableNameField);

        stateVariableTypeComboBox = ViskitGlobals.instance().getTypeComboBox();
        stateVariableTypeComboBox.getEditor().getEditorComponent().addFocusListener(focusListener);
        setMaxHeight(stateVariableTypeComboBox);

        arraySizeField = new JTextField(25);
        arraySizeField.addFocusListener(focusListener);
		

        initialValueField = new JTextField(25);
        initialValueField.addFocusListener(focusListener);
        setMaxHeight(initialValueField);

        descriptionField = new JTextField(25); // readability
        descriptionField.addFocusListener(focusListener);
        setMaxHeight(descriptionField);

        fieldsPanel.add(new OneLinePanel(    implicitLabel, width, implicitCheckBox));
        fieldsPanel.add(new OneLinePanel(        nameLabel, width, stateVariableNameField));
        fieldsPanel.add(new OneLinePanel(        typeLabel, width, stateVariableTypeComboBox));
        fieldsPanel.add(new OneLinePanel(   arraySizeLabel, width, arraySizeField));
        fieldsPanel.add(new OneLinePanel(initialValueLabel, width, initialValueField));
        fieldsPanel.add(new OneLinePanel( descriptionLabel, width, descriptionField));
        content.add(fieldsPanel);
        content.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(    okButton);
        buttonPanel.add(cancelButton);
        content.add(buttonPanel);
        content.add(Box.createVerticalGlue());    // takes up space when dialog is expanded vertically
        contentPane.add(content);

        myTyperComponent = stateVariableTypeComboBox.getEditor().getEditorComponent();

        // attach listeners
        cancelButton.addActionListener(new CancelButtonListener());
        okButton.addActionListener(new StateVariableApplyButtonListener());//applyButtonListener());

        stateVariableNameField.getDocument().addDocumentListener(listener);// addCaretListener(lis);
             initialValueField.getDocument().addDocumentListener(listener);
              descriptionField.getDocument().addDocumentListener(listener);// addCaretListener(lis);
        stateVariableTypeComboBox.addActionListener(listener);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowClosingListener(this, okButton, cancelButton));
    }

    /** Toggle these fields appropriately
     *
     * @param b if true, then enable
     */
    private void toggleArraySizeFields(boolean b) {
        arraySizeLabel.setEnabled(b);
        arraySizeField.setEnabled(b);
        arraySizeField.setEditable(b);     // grays background if false
    }

    @Override
    final void setParameters(Component parentComponent, Object p) 
	{
        viskitStateVariable = (ViskitStateVariable) p;

        fillWidgets();
        pack();
        setLocationRelativeTo(parentComponent);
    }

    private String stripArraySize(String typ) 
	{
        Pattern p = Pattern.compile("\\[.*?\\]");
        Matcher m = p.matcher(typ);
        return m.replaceAll("[]");
    }

    private String getArraySize(String typ)
	{
        Pattern p = Pattern.compile("\\[.*?\\]");
        Matcher m = p.matcher(typ);
        if (m.find())
		{
            String f = m.group();
            return f.substring(1, f.length() - 1);
        } 
		else 
		{
            return "";
        }
    }

    private void fillWidgets()
	{
        boolean isArray;
        String type;
        if (viskitStateVariable != null)
		{
            type = viskitStateVariable.getType();
         stateVariableTypeComboBox.setSelectedItem(stripArraySize(type));
         stateVariableTypeComboBox.setToolTipText (stripArraySize(type));          // readability
            stateVariableNameField.setText       (viskitStateVariable.getName());
                    arraySizeField.setText       (getArraySize(type));
                 initialValueField.setText       (viskitStateVariable.getValue());
                 initialValueField.setToolTipText(viskitStateVariable.getValue());       // readability
                  descriptionField.setText       (viskitStateVariable.getDescription());
                  descriptionField.setToolTipText(viskitStateVariable.getDescription()); // readability
				  implicitCheckBox.setSelected(viskitStateVariable.isImplicit() || viskitStateVariable.getValue().equals(IMPLICIT_VALUE_LABEL)); // safety net
            isArray = ViskitGlobals.instance().isArray(viskitStateVariable.getType());
        } 
		else // initialize stateVariable
		{
            type = (String) stateVariableTypeComboBox.getSelectedItem();
			                stateVariableTypeComboBox.setToolTipText ("");
            stateVariableNameField.setText(((EventGraphModel) ViskitGlobals.instance().getEventGraphViewFrame().getModel()).generateStateVariableName()); //"state_"+count++);
                    arraySizeField.setText("");
                 initialValueField.setText("");
                 initialValueField.setToolTipText("");
                  descriptionField.setText       (ViskitStatics.DEFAULT_DESCRIPTION);
                  descriptionField.setToolTipText(ViskitStatics.DEFAULT_DESCRIPTION);
				  implicitCheckBox.setSelected(false);
            isArray = ViskitGlobals.instance().isArray(type);
        }
		if (implicitCheckBox.isSelected())
		{
			initialValueField.setText(IMPLICIT_VALUE_LABEL);
			initialValueField.setToolTipText(IMPLICIT_TOOLTIP);
			initialValueField.setEnabled(false);
		}
		else
		{
			initialValueField.setToolTipText(initialValueField.getText()); // readability
			initialValueField.setEnabled(true);
		}
		descriptionField.setToolTipText(descriptionField.getText()); // readability
		
        modified = (viskitStateVariable == null) || isNewObjectInitialization(); // enable okButton if a new object is being started.
		
		// safety net case: value indicates implicit while boolean does not
		if (!viskitStateVariable.isImplicit() && viskitStateVariable.getValue().equals(IMPLICIT_VALUE_LABEL))
		{
			 viskitStateVariable.setImplicit(true); // override
			 modified = true;
			 okButton.setEnabled(true); // prompt user to review and approve (also see setSelected above)
			 LOG.error ("Unexpected found value='" + IMPLICIT_VALUE_LABEL + "' when implicit='false', displaying implicit='true' instead for user approval");
			 // design decision: don't popup a question panel since analyst might not know
		}
		okButton.setEnabled(modified);
        if (modified) 
		{
            getRootPane().setDefaultButton(okButton);
        } 
		else 
		{
            getRootPane().setDefaultButton(cancelButton);
        }
		
        toggleArraySizeFields(isArray);
        stateVariableNameField.requestFocus();
        stateVariableNameField.selectAll();
    }

    @Override
    void unloadWidgets()
	{
        // make sure there are no spaces
        String type = (String) stateVariableTypeComboBox.getSelectedItem();
        type = ViskitGlobals.instance().typeChosen(type);
        if (ViskitGlobals.instance().isArray(type)) 
		{
            type = type.substring(0, type.indexOf('[') + 1) + arraySizeField.getText().trim() + "]";
        }
        String name = stateVariableNameField.getText();
        name = name.replaceAll("\\s", "");
		boolean    implicit = implicitCheckBox.isSelected();
		String initialValue = initialValueField.getText().trim();
		String  description = descriptionField.getText().trim();

        if (viskitStateVariable != null)
		{
            viskitStateVariable.setName(name);
            viskitStateVariable.setType(type);
            viskitStateVariable.setImplicit(implicit);
            viskitStateVariable.setValue(initialValue);
            viskitStateVariable.setDescription (description);
        }
		
		// when creating a new state variable, save these new values so that invoking code can retrieve them
		newName        = name;
		newType        = type;
		newImplicit    = implicit;
		newValue       = initialValue;
		newDescription = description;
		
		// create implicit code block if not found
		EventGraphModel eventGraphModel = ((EventGraphModel)ViskitGlobals.instance().getEventGraphController().getModel());
		
		boolean implicitCodeBlockComputeMethodFound = 
			(eventGraphModel                != null) &&
			(eventGraphModel.getCodeBlock() != null) &&
			!eventGraphModel.getCodeBlock().contains("compute_" + name);
		
		//  implicit state variables get a code block - add if not already present
		if (implicit && implicitCodeBlockComputeMethodFound)
		{
			String newCodeBlock = "";
			if ((eventGraphModel != null) && (eventGraphModel.getCodeBlock() != null))
			{
				newCodeBlock = eventGraphModel.getCodeBlock();
			}
			
			if (newCodeBlock.trim().length() > 0)
			    newCodeBlock += "\n\n";
			
			// similar code block found in SimkitEventGraphXML2Java.buildStateVariableAccessor()
			newCodeBlock +=
				JDO + SP + "Implicit state variable computation" + SP + JDC + "\n" +
				"private void compute_" + name + SP + "()" + "\n" +
				OB + "\n" +
				SP_4 + name + SP + EQ + SP + "__TODO__" + "; // insert computation code here" + "\n" +
				CB + "\n";
			eventGraphModel.changeCodeBlock(newCodeBlock);
			ViskitGlobals.instance().getEventGraphViewFrame().refreshCodeBlock(newCodeBlock);
		}
		else if (implicitCodeBlockComputeMethodFound) // but not implicit
		{
			String   title = "Unnecessary code block found";
			String message = "State variable " + name + " is not implicit, remove unnecessary method from Event Graph code block: " + "private void compute_" + name + SP + "()";
			ViskitGlobals.instance().getEventGraphController().messageToUser(
					JOptionPane.ERROR_MESSAGE, title, message);
			LOG.error (title + ", " + message); // TODO add test to future diagnostics stylesheet
			// TODO add similar test when removing a state variable
		}
    }

    private boolean isGoodArray(String s) {
        s = s.trim();
        int brkIdx = s.indexOf('[');
        if (brkIdx == -1) {
            return true;
        }

        Pattern p = Pattern.compile(".+\\[\\s*\\](^\\[\\]){0}$");     // blah[whitsp]<eol>
        Matcher m = p.matcher(s);
        return m.matches();
    }

    private boolean isGeneric(String typ) {
        return (typ.contains("<K,V>") || typ.contains("<E>"));
    }

    // Little runnables to move the focus around
    private final Runnable sizeFieldFocus = new Runnable()
	{
        @Override
        public void run() {
            arraySizeField.requestFocus();
        }
    };

    private final Runnable descriptionFieldFocus = new Runnable() 
	{
        @Override
        public void run() {
            descriptionField.requestFocus();
        }
    };

    class myFocusListener extends FocusAdapter 
	{
        @Override
        public void focusGained(FocusEvent e)
		{
            handleSelect(e.getComponent());

            if (e.getOppositeComponent() == myTyperComponent) 
			{
                handleArrayFieldEnable();
            }
        }

        /**
         *  Enable the array size field if the type is an array, and set the focus to the right guy.
         */
        private void handleArrayFieldEnable() 
		{
            String s = (String) stateVariableTypeComboBox.getEditor().getItem();
            boolean isAr = ViskitGlobals.instance().isArray(s);
            toggleArraySizeFields(isAr);

            // Do this this way to shake out all the pending focus events before twiddling focus.
            if (isAr) {
                SwingUtilities.invokeLater(sizeFieldFocus);
            } else {
                SwingUtilities.invokeLater(descriptionFieldFocus);
            }
        }

        /**
         * select the text in whatever comes in
         * @param c the component containing text
         */
        private void handleSelect(Component c) 
		{
            if (c instanceof ComboBoxEditor) 
			{
                c = ((ComboBoxEditor) c).getEditorComponent();
            }

            if (c instanceof JTextComponent) {
                ((JTextComponent) c).selectAll();
            } else if (c instanceof ComboBoxEditor) {
                ((ComboBoxEditor) c).selectAll();
            }
        }
    }

    class StateVariableApplyButtonListener implements ActionListener
	{
        @Override
        public void actionPerformed(ActionEvent event)
		{
            if (modified) {
                String typeName = ((String) stateVariableTypeComboBox.getSelectedItem()).trim();
                String name = stateVariableNameField.getText().trim();
                String arraySize = arraySizeField.getText().trim();

                if ((name.length() <= 0) || (typeName.length() <= 0) ||
                    (ViskitGlobals.instance().isArray(typeName) && (arraySize.length() <= 0))) 
				{
                    ViskitGlobals.instance().getEventGraphController().messageToUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Data entry error",
                            "Name, type and (if array) array size must be entered."); // TODO better message
                    toggleArraySizeFields(true);
                    arraySizeField.requestFocus();
                    return;
                } 
				else if (ViskitGlobals.instance().isArray(typeName) && !isGoodArray(typeName)) 
				{
                    ViskitGlobals.instance().getEventGraphController().messageToUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Data entry error",
                            "Use a single trailing pair of empty square brackets\nto signify a one-dimensional array.");
                    return;
                } 
				else if (isGeneric(typeName)) 
				{
                    ViskitGlobals.instance().getEventGraphController().messageToUser(
                            JOptionPane.ERROR_MESSAGE,
                            "Data entry error",
                            "Actual Keys, Values or Element types must replace " +
                            "the K,V or E between the <> for Collection Objects.");
                    return;
                }

                /* Do a beanshell test for array declaration
                 * isPrimitive returns false for arrays
                 */
                if (!ViskitGlobals.instance().isPrimitive(typeName) && ViskitGlobals.instance().isArray(typeName)) {

                    String s = typeName + " " + name + " = new " + typeName;
                    s = s.substring(0, s.lastIndexOf('[') + 1) + arraySize + "]";          // stick in size

                    if (ViskitConfiguration.instance().getValue(ViskitConfiguration.BEANSHELL_WARNING).equalsIgnoreCase("true")) {
                        String result = ViskitGlobals.instance().parseCode(null, s);
                        if (result != null) {
                            boolean ret = BeanshellErrorDialog.showDialog(result, StateVariableDialog.this);
                            if (!ret) // don't ignore
                            {
                                return;
                            }
                        }
                    }
                }
                // ok, we passed
                unloadWidgets();
            }
            dispose();
        }
    }
}
