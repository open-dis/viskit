package viskit.view.dialog;

import viskit.model.GraphMetadata;

import edu.nps.util.SpringUtilities;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import static viskit.ViskitStatics.DESCRIPTION_HINT;

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
abstract public class MetadataDialog extends JDialog 
{
    static final Logger LOG = LogManager.getLogger();

    protected static boolean modified = false;
    protected JComponent runtimePanel;
    private JButton cancelButton;
    private JButton     okButton;
    
    GraphMetadata graphMetadata;
    JTextField nameTF, packageTF, authorTF, versionTF, extendsTF, implementsTF;
    JTextField stopTimeTF;
    JCheckBox  verboseCB;
    JTextArea  descriptionTextArea;

    public MetadataDialog(JFrame frame, GraphMetadata graphMetadata) {
        this(frame, graphMetadata, "Event Graph Metadata Properties");
    }

    public MetadataDialog(JFrame frame, GraphMetadata graphMetadata, String title) 
    {
        super(frame, title, true);
        this.graphMetadata = graphMetadata;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        //Create and populate the panel.
        JPanel contentPanel = new JPanel();
        setContentPane(contentPanel);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JPanel textFieldPanel = new JPanel(new SpringLayout());
        textFieldPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel("name", JLabel.TRAILING);
        nameTF = new JTextField(20);
        nameLabel.setLabelFor(nameTF);
        textFieldPanel.add(nameLabel);
        textFieldPanel.add(nameTF);

        JLabel packageLabel = new JLabel("package", JLabel.TRAILING);
        packageTF = new JTextField(20);
        packageTF.setToolTipText("Use standard Java dot notation for package naming, e.g. packageName.className");
        packageLabel.setLabelFor(packageTF);
        textFieldPanel.add(packageLabel);
        textFieldPanel.add(packageTF);

        JLabel authorLabel = new JLabel("author", JLabel.TRAILING);
        authorTF = new JTextField(20);
        authorLabel.setLabelFor(authorTF);
        textFieldPanel.add(authorLabel);
        textFieldPanel.add(authorTF);

        JLabel versionLabel = new JLabel("version", JLabel.TRAILING);
        versionTF = new JTextField(20);
        versionLabel.setLabelFor(versionTF);
        textFieldPanel.add(versionLabel);
        textFieldPanel.add(versionTF);

        JLabel extendsLabel = new JLabel("extends", JLabel.TRAILING);
        extendsTF = new JTextField(20);
        extendsLabel.setLabelFor(extendsTF);
        textFieldPanel.add(extendsLabel);
        textFieldPanel.add(extendsTF);

        JLabel implementsLabel = new JLabel("implements", JLabel.TRAILING);
        implementsTF = new JTextField(20);
        implementsLabel.setLabelFor(implementsTF);
        textFieldPanel.add(implementsLabel);
        textFieldPanel.add(implementsTF);

        // Lay out the panel.
        SpringUtilities.makeCompactGrid(textFieldPanel,
                6, 2,   //rows, cols
                0, 0,   //initX, initY
                6, 6);  //xPad, yPad

        Dimension d = textFieldPanel.getPreferredSize();
        textFieldPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));
        contentPanel.add(textFieldPanel);

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

        contentPanel.add(runtimePanel);

        JLabel descriptionLabel = new JLabel("description");
        descriptionLabel.setToolTipText(DESCRIPTION_HINT);
        descriptionLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        contentPanel.add(descriptionLabel);
        contentPanel.add(Box.createVerticalStrut(5));

        descriptionTextArea = new JTextArea(6, 40);
        descriptionTextArea.setToolTipText(DESCRIPTION_HINT);
        descriptionTextArea.setWrapStyleWord(true);
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setBorder(BorderFactory.createEmptyBorder());
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionTextArea);
        descriptionScrollPane.setToolTipText(DESCRIPTION_HINT);
        descriptionScrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        descriptionScrollPane.setBorder(authorTF.getBorder());

        contentPanel.add(descriptionScrollPane);
        contentPanel.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        cancelButton = new JButton("Cancel");
        okButton = new JButton("Apply changes");
        getRootPane().setDefaultButton(okButton);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        contentPanel.add(buttonPanel);

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
            okButton.addActionListener(new applyButtonListener());

        setParameters(frame, graphMetadata);
    }

    public final void setParameters(Component component, GraphMetadata graphMetadata) {
        this.graphMetadata = graphMetadata;

        fillWidgets();

        modified = (graphMetadata == null);
        pack();
        setLocationRelativeTo(component);
    }

    private void fillWidgets() 
    {
        if (graphMetadata == null) {
            graphMetadata = new GraphMetadata();
        }
        nameTF.setText             (graphMetadata.name);
        packageTF.setText          (graphMetadata.packageName);
        authorTF.setText           (graphMetadata.author);
        versionTF.setText          (graphMetadata.version);
        descriptionTextArea.setText(ViskitStatics.emptyIfNull(graphMetadata.description));
        extendsTF.setText          (graphMetadata.extendsPackageName);
        implementsTF.setText       (graphMetadata.implementsPackageName);
        stopTimeTF.setText         (graphMetadata.stopTime);
        verboseCB.setSelected      (graphMetadata.verbose);
        nameTF.selectAll();
    }

    private void unloadWidgets() 
    {
        // cleanups
        graphMetadata.author      = authorTF.getText().trim();

        if (this instanceof AssemblyMetadataDialog) {

            // The default names are AssemblyName, or EventGraphName
            if (!graphMetadata.name.contains("Assembly") || graphMetadata.name.equals("AssemblyName"))

                // Note: we need to force "Assembly" in the file name for special recognition
                graphMetadata.name = nameTF.getText().trim() + "Assembly";
            else
                graphMetadata.name = nameTF.getText().trim();
        } else {
            graphMetadata.name = nameTF.getText().trim();
        }
        graphMetadata.packageName = packageTF.getText().trim();
        graphMetadata.version = versionTF.getText().trim();
        graphMetadata.description = descriptionTextArea.getText().trim();
        graphMetadata.extendsPackageName = extendsTF.getText().trim();
        graphMetadata.implementsPackageName = implementsTF.getText().trim();
        graphMetadata.stopTime = stopTimeTF.getText().trim();
        graphMetadata.verbose = verboseCB.isSelected();
    }

    class cancelButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            modified = false;
            dispose();
        }
    }

    class applyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            // In this class, if the user hits the apply button, it is assumed that the data has been changed,
            // so the model is marked dirty.  A different, more controlled scheme would be to have change listeners
            // for all the widgets, and only mark dirty if data has been changed.  Else do a string compare between
            // final data and ending data and set modified only if something had actually changed.
            modified = true;
            if (modified) {
                if (nameTF.getText().trim().isEmpty()) {
                    ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE, "Error", "Must have a non-zero length name");
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
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int ret = JOptionPane.showConfirmDialog(MetadataDialog.this, "Apply changes?",
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
}
