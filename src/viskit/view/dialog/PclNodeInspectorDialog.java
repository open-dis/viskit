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
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.model.PropertyChangeListenerNode;
import static viskit.ViskitStatics.DESCRIPTION_HINT;
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
public class PclNodeInspectorDialog extends JDialog {

    private final JLabel nameLabel;
    private final JLabel typeLabel;
    private final JTextField nameField;    // Text field that holds the parameter name
    private final JTextField typeField;
    private InstantiationPanel ip;
    private Class<?> myClass;
    private static PclNodeInspectorDialog dialog;
    private static boolean modified = false;
    private PropertyChangeListenerNode pclNode;
    private final JButton okButton;
    private final JButton canButt;
    private final enableApplyButtonListener lis;
    JPanel buttPan;
    private final JCheckBox clearStatsCB;
    private final JCheckBox getMeanStatsCB;
    private final JCheckBox getCountStatsCB;
    private final JTextField descriptionTF;
    private final JLabel descriptionLabel;

    public static boolean showDialog(JFrame f, PropertyChangeListenerNode parm) {
        try {
            if (dialog == null) {
                dialog = new PclNodeInspectorDialog(f, parm);
            } else {
                dialog.setParams(f, parm);
            }
        } catch (ClassNotFoundException e) {
            String msg = "An object type specified in this element (probably " + parm.getType() + ") was not found.\n" +
                    "Add the XML or class file defining the element to the proper list at left.";
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE, 
                    "Property Change Listener Definition Not Found", 
                    msg
            );
            dialog = null;
            return false; // unmodified
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private PclNodeInspectorDialog(JFrame parent, PropertyChangeListenerNode lv) throws ClassNotFoundException {
        super(parent, "Property Change Listener (PCL) Inspector", true);
        this.pclNode = lv;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        lis = new enableApplyButtonListener();

        JPanel content = new JPanel();
        setContentPane(content);
        // this is a nop
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                BorderFactory.createLineBorder(Color.green, 2)));

        nameField = new JTextField();
        ViskitStatics.clampHeight(nameField);
        nameField.addCaretListener(lis);
        nameLabel = new JLabel("name", JLabel.TRAILING);
        nameLabel.setLabelFor(nameField);

        descriptionTF = new JTextField();
        descriptionTF.setToolTipText(DESCRIPTION_HINT);     
        ViskitStatics.clampHeight(descriptionTF);
        descriptionTF.addCaretListener(lis);
        descriptionLabel = new JLabel("description", JLabel.TRAILING);
        descriptionLabel.setToolTipText(DESCRIPTION_HINT);     
        descriptionLabel.setLabelFor(descriptionTF);

        typeLabel = new JLabel("type", JLabel.TRAILING);
        typeField = new JTextField();
        ViskitStatics.clampHeight(typeField);
        typeField.setEditable(false);
        typeLabel.setLabelFor(typeField);

        clearStatsCB = new JCheckBox("Clear statistics after each replication");
        clearStatsCB.setSelected(true); // bug 706
        clearStatsCB.setAlignmentX(JCheckBox.CENTER_ALIGNMENT);
        clearStatsCB.addActionListener(lis);

        getMeanStatsCB = new JCheckBox("Obtain mean statistics only");
        getMeanStatsCB.setAlignmentX(JCheckBox.CENTER_ALIGNMENT);
        getMeanStatsCB.addActionListener(new getMeanStatsCBListener());

        getCountStatsCB = new JCheckBox("Obtain raw count statistics only");
        getCountStatsCB.setAlignmentX(JCheckBox.CENTER_ALIGNMENT);
        getCountStatsCB.addActionListener(new getCountStatsCBListener());

        buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        canButt = new JButton("Cancel");
        okButton = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(okButton);
        buttPan.add(canButt);

        // attach listeners
        canButt.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        setParams(parent, lv);
    }

    public final void setParams(Component c, PropertyChangeListenerNode p) throws ClassNotFoundException {
        pclNode = p;

        fillWidgets();

        modified = (p == null);
        okButton.setEnabled(p == null);

        getRootPane().setDefaultButton(canButt);
        pack();
        setLocationRelativeTo(c);
    }

    private void fillWidgets() throws ClassNotFoundException {
        if (pclNode != null) {
            myClass = ViskitStatics.classForName(pclNode.getType());
            if (myClass == null) {
                JOptionPane.showMessageDialog(this, "Class " + pclNode.getType() + " not found.");
                return;
            }

            nameField.setText(pclNode.getName());
            typeField.setText(pclNode.getType());
            descriptionTF.setText(pclNode.getDescription());

            ip = new InstantiationPanel(this, lis, true);
            setupIP();
            ip.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                    "Object creation", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                    BorderFactory.createLineBorder(new Color(0xff, 0xc8, 0xc8), 4)));

            JPanel cont = new JPanel(new SpringLayout());
            cont.add(nameLabel);
            cont.add(nameField);

            cont.add(descriptionLabel);
            cont.add(descriptionTF);

            cont.add(typeLabel);
            cont.add(typeField);
            SpringUtilities.makeCompactGrid(cont, 3, 2, 10, 10, 5, 5);

            content.add(cont);

            /* Put up a "clear statistics after each replication" checkbox if
             * type is descendent of one of these:
             */
            if (pclNode.isSampleStatistics()) {
                clearStatsCB.setSelected(pclNode.isClearStatisticsAfterEachRun());
                content.add(clearStatsCB);
                content.add(Box.createVerticalStrut(3));
            }

            // No need to display mean and count CBs for a SPD PCL
            if (!pclNode.getType().contains("SimplePropertyDumper")) {
                getMeanStatsCB.setSelected(pclNode.isStatisticTypeMean());
                content.add(getMeanStatsCB);
                content.add(Box.createVerticalStrut(3));

                getCountStatsCB.setSelected(pclNode.isStatisticTypeCount());
                content.add(getCountStatsCB);
                content.add(Box.createVerticalStrut(3));
            }

            content.add(ip);
            content.add(Box.createVerticalStrut(5));
            content.add(buttPan);
            setContentPane(content);
        } else {
            nameField.setText("pclNode name");
        }
    }

    private void unloadWidgets() {
        String nm = nameField.getText();
        nm = nm.replaceAll("\\s", "");
        if (pclNode != null) {
            pclNode.setName(nm);
            pclNode.setDescription(descriptionTF.getText().trim());
            pclNode.setInstantiator(ip.getData());
            if (pclNode.isSampleStatistics()) {
                pclNode.setClearStatisticsAfterEachRun(clearStatsCB.isSelected());
            }

            pclNode.setStatisticTypeCount(getCountStatsCB.isSelected());
            pclNode.setStatisticTypeMean(getMeanStatsCB.isSelected());
        }
    }

    /**
     * Initialize the InstantiationsPanel with the data from the pclnode
     * @throws java.lang.ClassNotFoundException
     */
    private void setupIP() throws ClassNotFoundException {
        ip.setData(pclNode.getInstantiator());
    }

    class getMeanStatsCBListener implements CaretListener, ActionListener {
        @Override
        public void caretUpdate(CaretEvent event) {
            modified = true;
            okButton.setEnabled(true);
            getRootPane().setDefaultButton(okButton);
        }
        @Override
        public void actionPerformed(ActionEvent ae) {
            boolean isSelected = getMeanStatsCB.isSelected();
            getCountStatsCB.setSelected(!isSelected);
            caretUpdate(null);
        }
    }

    class getCountStatsCBListener implements CaretListener, ActionListener {
        @Override
        public void caretUpdate(CaretEvent event) {
            modified = true;
            okButton.setEnabled(true);
            getRootPane().setDefaultButton(okButton);
        }
        @Override
        public void actionPerformed(ActionEvent ae) {
            boolean isSelected = getCountStatsCB.isSelected();
            getMeanStatsCB.setSelected(!isSelected);
            caretUpdate(null);
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
            }
            dispose();
        }
    }

    class enableApplyButtonListener implements CaretListener, ActionListener {

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

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int ret = JOptionPane.showConfirmDialog(PclNodeInspectorDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
                    okButton.doClick();
                } else {
                    canButt.doClick();
                }
            } else {
                canButt.doClick();
            }
        }
    }
}
