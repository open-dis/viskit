package viskit.view.dialog;

import edu.nps.util.LogUtilities;
import viskit.model.GraphMetadata;

import edu.nps.util.SpringUtilities;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.apache.logging.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Apr 12, 2004
 * @since 3:52:05 PM
 * @version $Id$
 */
abstract public class MetadataDialog extends JDialog // TODO add clear, update buttons
{
    static final Logger LOG = LogUtilities.getLogger(MetadataDialog.class);
	
	private final JPanel metadataDialogPanel = new JPanel();
    protected static boolean modified = false;
    protected static GraphMetadata graphMetadata;
	
    protected JComponent runtimePanel;
    private   JButton    cancelButton;
    private   JButton    okButton;
    private   JTextField nameTF, packageTF, authorTF, createdTF, revisionTF, extendsTF, implementsTF, pathTF;
    private   JTextField stopTimeTF;
    private   JCheckBox  verboseCB;
    private   JTextArea  descriptionTextArea;
	private   String	 currentPath = "";

    public MetadataDialog(JFrame f, GraphMetadata graphMetadata) {
        this(f, graphMetadata, "");
    }

    public MetadataDialog(JFrame frame, GraphMetadata graphMetadata, String title)
	{
        super(frame, title, true);
        this.graphMetadata = graphMetadata;

        setGraphMetadata(frame, graphMetadata);
		
		initialize ();
		
        modified = (graphMetadata == null);
        pack();
        setLocationRelativeTo(frame);
	}
	private void initialize()
	{
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        // Populate the panel.
        setContentPane(metadataDialogPanel);
        metadataDialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        metadataDialogPanel.setLayout(new BoxLayout(metadataDialogPanel, BoxLayout.Y_AXIS));

        JPanel textFieldPanel = new JPanel(new SpringLayout());
        textFieldPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel(graphMetadata.getLabel() + " " + "name", JLabel.TRAILING);
		nameLabel.setToolTipText("name is used for both entity and file name"); // TODO spacification rules
        nameTF = new JTextField(20);
        nameLabel.setLabelFor(nameTF);
        textFieldPanel.add(nameLabel);
        textFieldPanel.add(nameTF);

        JLabel authorLabel = new JLabel("author", JLabel.TRAILING);
        authorTF = new JTextField(20);
        authorLabel.setLabelFor(authorTF);
        textFieldPanel.add(authorLabel);
        textFieldPanel.add(authorTF);

        JLabel createdLabel = new JLabel("created", JLabel.TRAILING);
        createdTF = new JTextField(20);
        createdLabel.setLabelFor(createdTF);
        textFieldPanel.add(createdLabel);
        textFieldPanel.add(createdTF);

        JLabel revisionLabel = new JLabel("revision", JLabel.TRAILING);
        revisionTF = new JTextField(20);
        revisionLabel.setLabelFor(revisionTF);
        textFieldPanel.add(revisionLabel);
        textFieldPanel.add(revisionTF);

        JLabel pathLabel = new JLabel("path", JLabel.TRAILING);
        pathTF = new JTextField(60);
		pathTF.setEditable(false); // usually do not allow editing in this field, since it is derived from project path and package
        pathLabel.setLabelFor(pathTF);
        textFieldPanel.add(pathLabel);
        textFieldPanel.add(pathTF);

		JLabel descriptionLabel = new JLabel("description", JLabel.TRAILING);
		descriptionLabel.setToolTipText("Good descriptions make the purpose of a model understandable and clear");
		descriptionLabel.setVerticalAlignment(JLabel.TOP);

		descriptionTextArea = new JTextArea(6, 40);
		descriptionTextArea.setWrapStyleWord(true);
		descriptionTextArea.setLineWrap(true);
		descriptionTextArea.setBorder(BorderFactory.createEmptyBorder());
		JScrollPane descriptionScrollPane = new JScrollPane(descriptionTextArea);
		descriptionScrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		descriptionScrollPane.setBorder(authorTF.getBorder());

		descriptionLabel.setLabelFor(descriptionScrollPane); // TODO not working
		textFieldPanel.add(descriptionLabel);
		textFieldPanel.add(descriptionScrollPane);
		
		int rowCount = 6;

		if (graphMetadata.isProject())
		{
			nameTF.setEditable(false); // graphMetadata.pathEditable can change this at run time
		}
		else // Event Graph or Assembly
		{
			JLabel packageLabel = new JLabel("package", JLabel.TRAILING);
			packageTF = new JTextField(20);
			String packageTooltip = "Use standard Java dot notation for package naming";
			packageLabel.setToolTipText(packageTooltip);
			   packageTF.setToolTipText(packageTooltip);
			// Listen for changes in the text http://stackoverflow.com/questions/3953208/value-change-listener-to-jtextfield
			packageTF.getDocument().addDocumentListener(new DocumentListener() 
			{
				@Override
				public void changedUpdate(DocumentEvent e) 
				{
					computePathValue();
					refreshPathTF();
				}

				@Override
				public void removeUpdate(DocumentEvent e) 
				{
					computePathValue();
					refreshPathTF();
				}

				@Override
				public void insertUpdate(DocumentEvent e) 
				{
					computePathValue();
					refreshPathTF();
				}
				private void refreshPathTF()
				{
                     pathTF.setText       (currentPath);
                     pathTF.setToolTipText(currentPath); // readability for long value
					 metadataDialogPanel.validate(); // repaint TODO not working :(
					 metadataDialogPanel.repaint();
				}
			});
			packageLabel.setLabelFor(packageTF);
			textFieldPanel.add(packageLabel);
			textFieldPanel.add(packageTF);
		
			JLabel extendsLabel = new JLabel("extends", JLabel.TRAILING);
			extendsTF = new JTextField(20);
			extendsLabel.setLabelFor(extendsTF);
			String extendsTooltip = "Name of inherited parent Java class";
			extendsLabel.setToolTipText(extendsTooltip);
			   extendsTF.setToolTipText(extendsTooltip);
			textFieldPanel.add(extendsLabel);
			textFieldPanel.add(extendsTF);

			JLabel implementsLabel = new JLabel("implements", JLabel.TRAILING);
			implementsTF = new JTextField(20);
			String implementsTooltip = "Names of implemented Java interfaces (comma separated)";
			implementsLabel.setToolTipText(implementsTooltip);
			   implementsTF.setToolTipText(implementsTooltip);
			implementsLabel.setLabelFor(implementsTF);
			textFieldPanel.add(implementsLabel);
			textFieldPanel.add(implementsTF);
			
			rowCount = 9;
		}
		
        // Lay out the panel
        SpringUtilities.makeCompactGrid(textFieldPanel,
                rowCount, 2,   //rows, cols
                0, 0,   //initX, initY
                6, 6);  //xPad, yPad

        Dimension d = textFieldPanel.getPreferredSize();
        textFieldPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));
        metadataDialogPanel.add(textFieldPanel);

        runtimePanel = new JPanel(new SpringLayout());
        runtimePanel.setBorder(BorderFactory.createTitledBorder("Runtime defaults"));

        JLabel stopTimeLabel = new JLabel("stop time", JLabel.TRAILING);
        stopTimeTF = new JTextField(20);
        stopTimeLabel.setLabelFor(stopTimeTF);
        runtimePanel.add(stopTimeLabel);
        runtimePanel.add(stopTimeTF);

        JLabel verboseLabel = new JLabel("verbose output", JLabel.TRAILING);
        verboseCB = new JCheckBox();
        verboseLabel.setLabelFor(verboseCB);
        runtimePanel.add(verboseLabel);
        runtimePanel.add(verboseCB);

        SpringUtilities.makeCompactGrid(runtimePanel,
                2, 2, 6, 6, 6, 6);
        d = runtimePanel.getPreferredSize();
        runtimePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));
        runtimePanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        metadataDialogPanel.add(runtimePanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

            okButton = new JButton("Apply changes");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        metadataDialogPanel.add(buttonPanel);

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());
    }

    public final void setGraphMetadata(Component c, GraphMetadata graphMetadata) 
	{
		this.graphMetadata = graphMetadata;
		
//        fillWidgets();
//
//        modified = (graphMetadata == null);
//        pack();
//        setLocationRelativeTo(c);
    }
	
	private void computePathValue()
	{
		if (ViskitGlobals.instance().getCurrentViskitProject() == null) // safety net
			currentPath = graphMetadata.path;
		else if (this instanceof AssemblyMetadataDialog)
			 currentPath = ViskitGlobals.instance().getCurrentViskitProject().getAssembliesDirectory().getPath();
		else if (this instanceof EventGraphMetadataDialog)
			 currentPath = ViskitGlobals.instance().getCurrentViskitProject().getEventGraphsDirectory().getPath();
		else if ((this instanceof ProjectMetadataDialog) && (ViskitGlobals.instance().getCurrentViskitProject() != null))
			 currentPath = ViskitGlobals.instance().getCurrentViskitProject().getProjectRootDirectory().getPath();
		else if ( this instanceof ProjectMetadataDialog)
			 currentPath = graphMetadata.path;
		// else error
		
		if ((graphMetadata.packageName != null) && (!graphMetadata.packageName.isEmpty()))
		{
			currentPath = currentPath + ViskitStatics.getFileSeparator() + graphMetadata.packageName;
		}
	}

    protected void fillWidgets() 
	{
        getRootPane().setDefaultButton(okButton);
		
		if (graphMetadata == null)
		{
			graphMetadata = new GraphMetadata(); // unexpected error condition
			LOG.error ("MetadataDialog invoked with null graphMetadata");
		}
		
		nameTF.setEditable(graphMetadata.pathEditable);
		pathTF.setEditable(graphMetadata.pathEditable && (this instanceof ProjectMetadataDialog));
		
		if ((graphMetadata.description == null) || graphMetadata.description.trim().isEmpty())
			 graphMetadata.description = ViskitStatics.DEFAULT_DESCRIPTION;
		
		computePathValue();
			
                     nameTF.setText       (graphMetadata.name);
                   authorTF.setText       (graphMetadata.author);
                  createdTF.setText       (graphMetadata.created);
                 revisionTF.setText       (graphMetadata.revision);
                     pathTF.setText       (currentPath);
                     pathTF.setToolTipText(currentPath); // readability for long value
        descriptionTextArea.setText       (graphMetadata.description);
        descriptionTextArea.setToolTipText(graphMetadata.description);
                 stopTimeTF.setText       (graphMetadata.stopTime);
                  verboseCB.setSelected   (graphMetadata.verbose);
                     nameTF.selectAll(); // highlight, usually not editable
					 
		if (!graphMetadata.isProject())
		{
			while (graphMetadata.packageName.endsWith("."))
			{
				   graphMetadata.packageName = graphMetadata.packageName.substring(0,graphMetadata.packageName.length()-1); // strip trailing .
			}
			
                  packageTF.setText(graphMetadata.packageName);
                  extendsTF.setText(graphMetadata.extendsPackageName);
               implementsTF.setText(graphMetadata.implementsPackageName);
		}
    }

    private void unloadWidgets() 
	{
        graphMetadata.author      = authorTF.getText().trim();
        graphMetadata.description = descriptionTextArea.getText().trim();

        if (this instanceof AssemblyMetadataDialog)
		{
            // The default names are AssemblyName, or EventGraphName
            if (!graphMetadata.name.toLowerCase().contains(GraphMetadata.ASSEMBLY) || graphMetadata.name.equals("AssemblyName"))

                // Note: we need to force "Assembly" as part of the file name for special recognition
                graphMetadata.name = nameTF.getText().trim() + "Assembly";
            else
                graphMetadata.name = nameTF.getText().trim();
        } 
		else if (this instanceof EventGraphMetadataDialog)
		{
            graphMetadata.name = nameTF.getText().trim();
        }
        graphMetadata.author      =            authorTF.getText().trim();
        graphMetadata.created     =           createdTF.getText().trim();
        graphMetadata.revision    =          revisionTF.getText().trim();
        graphMetadata.description = descriptionTextArea.getText().trim();
		
		if (graphMetadata.isProject())
		{
			if (graphMetadata.pathEditable)
			{
				graphMetadata.name = nameTF.getText().trim();
				graphMetadata.path = pathTF.getText().trim();
			}
		}
		else // Event Graph or Assembly
		{			
			graphMetadata.packageName           =    packageTF.getText().trim();
			while (graphMetadata.packageName.endsWith("."))
			{
				   graphMetadata.packageName = graphMetadata.packageName.substring(0,graphMetadata.packageName.length()-1); // strip trailing .
			}
			
			graphMetadata.extendsPackageName    =    extendsTF.getText().trim();
			graphMetadata.implementsPackageName = implementsTF.getText().trim();
			graphMetadata.stopTime = stopTimeTF.getText().trim();
			graphMetadata.verbose  =  verboseCB.isSelected();
		}
		nameTF.setEditable(false); // reset; graphMetadata.pathEditable can change this at run time
		
		graphMetadata.updated = true;
		modified = true;
    }

	/**
	 * @return the graphMetadata
	 */
	public static GraphMetadata getGraphMetadata() 
	{
		return graphMetadata;
	}

    class cancelButtonListener implements ActionListener 
	{
        @Override
        public void actionPerformed(ActionEvent event) 
		{
            modified = false;
            dispose();
        }
    }

    class applyButtonListener implements ActionListener 
	{
        @Override
        public void actionPerformed(ActionEvent event) 
		{
            // In this class, if the user hits the apply button, it is assumed that the data has been changed,
            // so the model is marked dirty.  A different, more controlled scheme would be to have change listeners
            // for all the widgets, and only mark dirty if data has been changed.  Else do a string compare between
            // final data and ending data and set modified only if something had actually changed.
            modified = true;
            if (modified) 
			{
                if (nameTF.getText().trim().isEmpty())
				{
                    JOptionPane.showMessageDialog(MetadataDialog.this, "Must have a non-zero-length name.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    nameTF.requestFocus();
                    return;
                }
                // OK, we're good....
                unloadWidgets();
            }
            dispose();
        }
    }

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) 
		{
            if (modified) 
			{
                int returnValue = JOptionPane.showConfirmDialog(MetadataDialog.this, "Apply changes?",
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
