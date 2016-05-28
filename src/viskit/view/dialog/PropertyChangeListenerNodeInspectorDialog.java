package viskit.view.dialog;

import edu.nps.util.SpringUtilities;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import viskit.ViskitStatics;
import viskit.model.PropertyChangeListenerNode;
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
public class PropertyChangeListenerNodeInspectorDialog extends JDialog {

    private final JLabel     nameLabel;
	private final JLabel     typeLabel;
    private final JTextField nameTF;    // Text field that holds the parameter name
    private final JTextField typeNameTF;
    private final JTextField descriptionTF;
    private final JLabel     descriptionLabel;
    private InstantiationPanel instantiationPanel;
    private Class<?> myClass;
    private static PropertyChangeListenerNodeInspectorDialog dialog;
    private static boolean modified = false;
    private PropertyChangeListenerNode propertyChangeListenerNode;
    private final JButton okButton, cancelButton;
    private final EnableApplyButtonListener enableApplyButtonListener;
	JPanel statisticsContainerPanel = new JPanel();
    JPanel buttonPanel              = new JPanel();
    private final JRadioButton getMeanStatisticsRB, getCountStatisticsRB;
    private final JCheckBox    clearStatisticsCB;

    public static boolean showDialog(JFrame f, PropertyChangeListenerNode parm) {
        try {
            if (dialog == null) {
                dialog = new PropertyChangeListenerNodeInspectorDialog(f, parm);
            } else {
                dialog.setParams(f, parm);
            }
        } catch (ClassNotFoundException e) {
            String msg = "An object type specified in this element (probably " + parm.getType() + ") was not found.\n" +
                    "Add the XML or class file defining the element to the proper list at left.";
            JOptionPane.showMessageDialog(f, msg, "Property Change Listener Definition Not Found", JOptionPane.ERROR_MESSAGE);
            dialog = null;
            return false; // unmodified
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private PropertyChangeListenerNodeInspectorDialog(JFrame parent, PropertyChangeListenerNode propertyChangeListenerNode) throws ClassNotFoundException
	{
        super(parent, "Property Change Listener (PCL) Inspector", true);
        this.propertyChangeListenerNode = propertyChangeListenerNode;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new MyCloseListener());

        enableApplyButtonListener = new EnableApplyButtonListener();

        JPanel content = new JPanel();
        setContentPane(content);
        // this is a nop
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                BorderFactory.createLineBorder(Color.green, 2)));

        nameTF = new JTextField();
        ViskitStatics.clampHeight(nameTF);
        nameTF.addCaretListener(enableApplyButtonListener);
        nameLabel = new JLabel("name", JLabel.TRAILING);
        nameLabel.setLabelFor(nameTF);

        descriptionTF = new JTextField();
        ViskitStatics.clampHeight(descriptionTF);
        descriptionTF.addCaretListener(enableApplyButtonListener);
        descriptionLabel = new JLabel("description", JLabel.TRAILING);
        descriptionLabel.setLabelFor(descriptionTF);

        typeLabel  = new JLabel("type", JLabel.TRAILING);
        typeNameTF = new JTextField();
        ViskitStatics.clampHeight(typeNameTF);
        typeNameTF.setEditable(false);
        typeLabel.setLabelFor(typeNameTF);

        statisticsContainerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                    "Statistics configuration", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));

        clearStatisticsCB = new JCheckBox("Clear statistics after each replication");
        clearStatisticsCB.setSelected(true); // bug 706
        clearStatisticsCB.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
        clearStatisticsCB.addActionListener(enableApplyButtonListener);

        getMeanStatisticsRB = new JRadioButton("Obtain mean statistics only");
        getMeanStatisticsRB.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
        getMeanStatisticsRB.addActionListener(new GetMeanStatisticsCBListener());

        getCountStatisticsRB = new JRadioButton("Obtain raw count statistics only");
        getCountStatisticsRB.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
        getCountStatisticsRB.addActionListener(new GetCountStatisticsCBListener());

        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        okButton = new JButton("Apply changes");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // attach listeners
        cancelButton.addActionListener(new CancelButtonListener());
        okButton.addActionListener(new ApplyButtonListener());

        setParams(parent, propertyChangeListenerNode);
    }

    public final void setParams(Component c, PropertyChangeListenerNode p) throws ClassNotFoundException {
        propertyChangeListenerNode = p;

        fillWidgets();

        modified = (p == null);
        okButton.setEnabled(p == null);

        getRootPane().setDefaultButton(cancelButton);
        pack();
        setLocationRelativeTo(c);
    }

    private void fillWidgets() throws ClassNotFoundException
	{
        if (propertyChangeListenerNode != null)
		{
            myClass = ViskitStatics.ClassForName(propertyChangeListenerNode.getType());
            if (myClass == null) {
                JOptionPane.showMessageDialog(this, "Class " + propertyChangeListenerNode.getType() + " not found.");
                return;
            }
                   nameTF.setText(propertyChangeListenerNode.getName());
               typeNameTF.setText(propertyChangeListenerNode.getType());
            descriptionTF.setText(propertyChangeListenerNode.getDescription());

            instantiationPanel = new InstantiationPanel(this, enableApplyButtonListener, true);
            setupInstantiationPanel();
            instantiationPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                    "Listener initialization", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                    BorderFactory.createLineBorder(new Color(0xff, 0xc8, 0xc8), 4)));

            JPanel containerPanel = new JPanel(new SpringLayout());
            containerPanel.add(nameLabel);
            containerPanel.add(nameTF);

            containerPanel.add(descriptionLabel);
            containerPanel.add(descriptionTF);

            containerPanel.add(typeLabel);
            containerPanel.add(typeNameTF);
            SpringUtilities.makeCompactGrid(containerPanel, 3, 2, 10, 10, 5, 5);

            content.add(containerPanel);

            // No need to display mean and count CBs for a SPD PCL
            if (!propertyChangeListenerNode.getType().contains("SimplePropertyDumper"))
			{
                getMeanStatisticsRB.setSelected(propertyChangeListenerNode.isGetMean());
                statisticsContainerPanel.add(getMeanStatisticsRB);
                statisticsContainerPanel.add(Box.createVerticalStrut(3));

                getCountStatisticsRB.setSelected(propertyChangeListenerNode.isGetCount());
                statisticsContainerPanel.add(getCountStatisticsRB);
                statisticsContainerPanel.add(Box.createVerticalStrut(3));
            }
            /* Put up a "clear statistics after each replication" checkbox if
             * type is descendent of one of these:
             */
            if (propertyChangeListenerNode.isSampleStatistics())
			{
                clearStatisticsCB.setSelected(propertyChangeListenerNode.isClearStatisticsAfterEachRun());
                statisticsContainerPanel.add(clearStatisticsCB);
                statisticsContainerPanel.add(Box.createVerticalStrut(3));
            }

            content.add(instantiationPanel);
            content.add(Box.createVerticalStrut(5));
			content.add(statisticsContainerPanel);
            content.add(Box.createVerticalStrut(5));
            content.add(buttonPanel);
            setContentPane(content);
        } else {
            nameTF.setText("pclNode name");
        }
    }

    private void unloadWidgets()
	{
        String name = nameTF.getText();
        name = name.replaceAll("\\s", "");
        if (propertyChangeListenerNode != null)
		{
            propertyChangeListenerNode.setName(name);
            propertyChangeListenerNode.setDescription(descriptionTF.getText().trim());
            propertyChangeListenerNode.setInstantiator(instantiationPanel.getData());
			
            propertyChangeListenerNode.setGetCount(getCountStatisticsRB.isSelected());
            propertyChangeListenerNode.setGetMean (getMeanStatisticsRB.isSelected());
            if (propertyChangeListenerNode.isSampleStatistics())
			{
                propertyChangeListenerNode.setClearStatisticsAfterEachRun(clearStatisticsCB.isSelected());
            }
        }
    }

    /**
     * Initialize the InstantiationsPanel with the data from the pclnode
     * @throws java.lang.ClassNotFoundException
     */
    private void setupInstantiationPanel() throws ClassNotFoundException {
        instantiationPanel.setData(propertyChangeListenerNode.getInstantiator());
    }

    class GetMeanStatisticsCBListener implements CaretListener, ActionListener {
        @Override
        public void caretUpdate(CaretEvent event) {
            modified = true;
            okButton.setEnabled(true);
            getRootPane().setDefaultButton(okButton);
        }
        @Override
        public void actionPerformed(ActionEvent ae) {
            boolean isSelected = getMeanStatisticsRB.isSelected();
            getCountStatisticsRB.setSelected(!isSelected);
            caretUpdate(null);
        }
    }

    class GetCountStatisticsCBListener implements CaretListener, ActionListener {
        @Override
        public void caretUpdate(CaretEvent event) {
            modified = true;
            okButton.setEnabled(true);
            getRootPane().setDefaultButton(okButton);
        }
        @Override
        public void actionPerformed(ActionEvent ae) {
            boolean isSelected = getCountStatisticsRB.isSelected();
            getMeanStatisticsRB.setSelected(!isSelected);
            caretUpdate(null);
        }
    }

    class CancelButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            modified = false;    // for the caller
            dispose();
        }
    }

    class ApplyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (modified) {
                unloadWidgets();
            }
            dispose();
        }
    }

    class EnableApplyButtonListener implements CaretListener, ActionListener {

        @Override
        public void caretUpdate(CaretEvent event) {
            modified = true;
            okButton.setEnabled(true);
            getRootPane().setDefaultButton(okButton);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            caretUpdate(null);
        }
    }

    class MyCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int returnValue = JOptionPane.showConfirmDialog(PropertyChangeListenerNodeInspectorDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (returnValue == JOptionPane.YES_OPTION) {
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
