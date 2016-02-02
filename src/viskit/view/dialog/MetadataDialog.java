package viskit.view.dialog;

import viskit.model.GraphMetadata;

import edu.nps.util.SpringUtilities;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
abstract public class MetadataDialog extends JDialog {

    protected static boolean modified = false;
    protected JComponent runtimePanel;
    private JButton cancelButton;
    private JButton okButton;
    GraphMetadata graphMetadata;
    JTextField nameTf, packageTf, authorTf, versionTf, extendsTf, implementsTf;
    JTextField stopTimeTf;
    JCheckBox verboseCb;
    JTextArea descriptionTextArea;

    public MetadataDialog(JFrame f, GraphMetadata gmd) {
        this(f, gmd, "Event Graph Properties");
    }

    public MetadataDialog(JFrame f, GraphMetadata gmd, String title) {
        super(f, title, true);
        this.graphMetadata = gmd;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        //Create and populate the panel.
        JPanel metaDataDialogPanel = new JPanel();
        setContentPane(metaDataDialogPanel);
        metaDataDialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        metaDataDialogPanel.setLayout(new BoxLayout(metaDataDialogPanel, BoxLayout.Y_AXIS));

        JPanel textFieldPanel = new JPanel(new SpringLayout());
        textFieldPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel("name", JLabel.TRAILING);
        nameTf = new JTextField(20);
        nameLabel.setLabelFor(nameTf);
        textFieldPanel.add(nameLabel);
        textFieldPanel.add(nameTf);

        JLabel packageLabel = new JLabel("package", JLabel.TRAILING);
        packageTf = new JTextField(20);
        packageTf.setToolTipText("Use standard Java dot notation for package naming");
        packageLabel.setLabelFor(packageTf);
        textFieldPanel.add(packageLabel);
        textFieldPanel.add(packageTf);

        JLabel authorLabel = new JLabel("author", JLabel.TRAILING);
        authorTf = new JTextField(20);
        authorLabel.setLabelFor(authorTf);
        textFieldPanel.add(authorLabel);
        textFieldPanel.add(authorTf);

        JLabel verssionLabel = new JLabel("version", JLabel.TRAILING);
        versionTf = new JTextField(20);
        verssionLabel.setLabelFor(versionTf);
        textFieldPanel.add(verssionLabel);
        textFieldPanel.add(versionTf);

        JLabel extendsLabel = new JLabel("extends", JLabel.TRAILING);
        extendsTf = new JTextField(20);
        extendsLabel.setLabelFor(extendsTf);
        textFieldPanel.add(extendsLabel);
        textFieldPanel.add(extendsTf);

        JLabel implementsLabel = new JLabel("implements", JLabel.TRAILING);
        implementsTf = new JTextField(20);
        implementsLabel.setLabelFor(implementsTf);
        textFieldPanel.add(implementsLabel);
        textFieldPanel.add(implementsTf);

        // Lay out the panel.
        SpringUtilities.makeCompactGrid(textFieldPanel,
                6, 2,   //rows, cols
                0, 0,   //initX, initY
                6, 6);  //xPad, yPad

        Dimension d = textFieldPanel.getPreferredSize();
        textFieldPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));
        metaDataDialogPanel.add(textFieldPanel);

        runtimePanel = new JPanel(new SpringLayout());
        runtimePanel.setBorder(BorderFactory.createTitledBorder("Runtime defaults"));

        JLabel stopTimeLabel = new JLabel("stop time", JLabel.TRAILING);
        stopTimeTf = new JTextField(20);
        stopTimeLabel.setLabelFor(stopTimeTf);
        runtimePanel.add(stopTimeLabel);
        runtimePanel.add(stopTimeTf);

        JLabel verboseLabel = new JLabel("verbose output", JLabel.TRAILING);
        verboseCb = new JCheckBox();
        verboseLabel.setLabelFor(verboseCb);
        runtimePanel.add(verboseLabel);
        runtimePanel.add(verboseCb);

        SpringUtilities.makeCompactGrid(runtimePanel,
                2, 2, 6, 6, 6, 6);
        d = runtimePanel.getPreferredSize();
        runtimePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));
        runtimePanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        metaDataDialogPanel.add(runtimePanel);

        JLabel descriptionLabel = new JLabel("Description");
        descriptionLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        metaDataDialogPanel.add(descriptionLabel);
        metaDataDialogPanel.add(Box.createVerticalStrut(5));

        descriptionTextArea = new JTextArea(6, 40);
        descriptionTextArea.setWrapStyleWord(true);
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setBorder(BorderFactory.createEmptyBorder());
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionTextArea);
        descriptionScrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        descriptionScrollPane.setBorder(authorTf.getBorder());

        metaDataDialogPanel.add(descriptionScrollPane);
        metaDataDialogPanel.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

            okButton = new JButton("Apply changes");
        cancelButton = new JButton("Cancel");
        getRootPane().setDefaultButton(okButton);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        metaDataDialogPanel.add(buttonPanel);

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        setGraphMetadata(f, gmd);
    }

    public final void setGraphMetadata(Component c, GraphMetadata gmd) 
	{
        graphMetadata = gmd;

        fillWidgets();

        modified = (gmd == null);
        pack();
        setLocationRelativeTo(c);
    }

    private void fillWidgets() 
	{
        if (graphMetadata == null) {
            graphMetadata = new GraphMetadata();
        }
                     nameTf.setText(graphMetadata.name);
                  packageTf.setText(graphMetadata.packageName);
                   authorTf.setText(graphMetadata.author);
                  versionTf.setText(graphMetadata.version);
        descriptionTextArea.setText(graphMetadata.description);
                  extendsTf.setText(graphMetadata.extendsPackageName);
               implementsTf.setText(graphMetadata.implementsPackageName);
                 stopTimeTf.setText(graphMetadata.stopTime);
                  verboseCb.setSelected(graphMetadata.verbose);
                     nameTf.selectAll();
    }

    private void unloadWidgets() {
        graphMetadata.author = authorTf.getText().trim();
        graphMetadata.description = descriptionTextArea.getText().trim();

        if (this instanceof AssemblyMetadataDialog) {

            // The default names are AssemblyName, or EventGraphName
            if (!graphMetadata.name.contains("Assembly") || graphMetadata.name.equals("AssemblyName"))

                // Note: we need to force "Assembly" in the file name for special recognition
                graphMetadata.name = nameTf.getText().trim() + "Assembly";
            else
                graphMetadata.name = nameTf.getText().trim();
        } else {
            graphMetadata.name = nameTf.getText().trim();
        }
        graphMetadata.packageName = packageTf.getText().trim();
        graphMetadata.version = versionTf.getText().trim();
        graphMetadata.extendsPackageName = extendsTf.getText().trim();
        graphMetadata.implementsPackageName = implementsTf.getText().trim();
        graphMetadata.stopTime = stopTimeTf.getText().trim();
        graphMetadata.verbose = verboseCb.isSelected();
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
                if (nameTf.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(MetadataDialog.this, "Must have a non-zero length name.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    nameTf.requestFocus();
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
