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
import viskit.VStatics;
import viskit.model.PropChangeListenerNode;
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

    private JLabel nameLabel;
    private JLabel typeLab;
    private JTextField nameField;    // Text field that holds the parameter name
    private JTextField typeField;
    private InstantiationPanel ip;
    private Class<?> myClass;
    private static PclNodeInspectorDialog dialog;
    private static boolean modified = false;
    private PropChangeListenerNode pclNode;
    private Component locationComp;
    private JButton okButt, canButt;
    private enableApplyButtonListener lis;
    JPanel buttPan;
    private JCheckBox clearStatsCB, getMeanStatsCB, getCountStatsCB;
    private JTextField descTF;
    private JLabel descLab;

    public static boolean showDialog(JFrame f, Component comp, PropChangeListenerNode parm) {
        try {
            if (dialog == null) {
                dialog = new PclNodeInspectorDialog(f, comp, parm);
            } else {
                dialog.setParams(comp, parm);
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

    private PclNodeInspectorDialog(JFrame parent, Component comp, PropChangeListenerNode lv) throws ClassNotFoundException {
        super(parent, "Property Change Listener (PCL) Inspector", true);
        this.pclNode = lv;
        this.locationComp = comp;
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
        VStatics.clampHeight(nameField);
        nameField.addCaretListener(lis);
        nameLabel = new JLabel("name", JLabel.TRAILING);
        nameLabel.setLabelFor(nameField);

        descTF = new JTextField();
        VStatics.clampHeight(descTF);
        descTF.addCaretListener(lis);
        descLab = new JLabel("description", JLabel.TRAILING);
        descLab.setLabelFor(descTF);

        typeLab = new JLabel("type", JLabel.TRAILING);
        typeField = new JTextField();
        VStatics.clampHeight(typeField);
        typeField.setEditable(false);
        typeLab.setLabelFor(typeField);

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
        okButt = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(canButt);
        buttPan.add(okButt);

        fillWidgets();     // put the data into the widgets

        modified = (lv == null);     // if it's a new pclNode, they can always accept defaults with no typing
        okButt.setEnabled(lv == null);

        getRootPane().setDefaultButton(canButt);

        pack();     // do this prior to next
        this.setLocationRelativeTo(locationComp);

        // attach listeners
        canButt.addActionListener(new cancelButtonListener());
        okButt.addActionListener(new applyButtonListener());
    }

    public void setParams(Component c, PropChangeListenerNode p) throws ClassNotFoundException {
        pclNode = p;
        locationComp = c;

        fillWidgets();

        modified = (p == null);
        okButt.setEnabled(p == null);

        getRootPane().setDefaultButton(canButt);
        pack();
        this.setLocationRelativeTo(c);
    }

    private void fillWidgets() throws ClassNotFoundException {
        if (pclNode != null) {
            myClass = VStatics.classForName(pclNode.getType());
            if (myClass == null) {
                JOptionPane.showMessageDialog(this, "Class " + pclNode.getType() + " not found.");
                return;
            }

            nameField.setText(pclNode.getName());
            typeField.setText(pclNode.getType());
            descTF.setText(pclNode.getDescriptionString());

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

            cont.add(descLab);
            cont.add(descTF);

            cont.add(typeLab);
            cont.add(typeField);
            SpringUtilities.makeCompactGrid(cont, 3, 2, 10, 10, 5, 5);

            content.add(cont);

            /* Put up a "clear statistics after each replication" checkbox if
             * type is descendent of one of these:
             */
            if (pclNode.isSampleStats()) {
                clearStatsCB.setSelected(pclNode.isClearStatsAfterEachRun());
                content.add(clearStatsCB);
                content.add(Box.createVerticalStrut(3));
            }

            getMeanStatsCB.setSelected(pclNode.isGetMean());
            content.add(getMeanStatsCB);
            content.add(Box.createVerticalStrut(3));

            getCountStatsCB.setSelected(pclNode.isGetCount());
            content.add(getCountStatsCB);
            content.add(Box.createVerticalStrut(3));

            content.add(ip);
            content.add(Box.createVerticalStrut(5));
            content.add(buttPan);
            setContentPane(content);
        } else {
            nameField.setText("pclNode name");
        //commentField.setText("comments here");
        }
    }

    private void unloadWidgets() {
        String nm = nameField.getText();
        nm = nm.replaceAll("\\s", "");
        if (pclNode != null) {
            pclNode.setName(nm);
            pclNode.setDescriptionString(descTF.getText().trim());
            pclNode.setInstantiator(ip.getData());
            if (pclNode.isSampleStats()) {
                pclNode.setClearStatsAfterEachRun(clearStatsCB.isSelected());
            }

            pclNode.setGetCount(getCountStatsCB.isSelected());
            pclNode.setGetMean(getMeanStatsCB.isSelected());
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
            okButt.setEnabled(true);
            getRootPane().setDefaultButton(okButt);
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
            okButt.setEnabled(true);
            getRootPane().setDefaultButton(okButt);
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
            okButt.setEnabled(true);
            getRootPane().setDefaultButton(okButt);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            caretUpdate(null);
        }
    }

    class OneLinePanel extends JPanel {

        OneLinePanel(JLabel lab, int w, JComponent comp) {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(Box.createHorizontalStrut(5));
            add(Box.createHorizontalStrut(w - lab.getPreferredSize().width));
            add(lab);
            add(Box.createHorizontalStrut(5));
            add(comp);

            Dimension d = getPreferredSize();
            d.width = Integer.MAX_VALUE;
            setMaximumSize(d);
        }
    }

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int ret = JOptionPane.showConfirmDialog(PclNodeInspectorDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
                    okButt.doClick();
                } else {
                    canButt.doClick();
                }
            } else {
                canButt.doClick();
            }
        }
    }
}