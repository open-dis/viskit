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

import viskit.model.EvGraphNode;
import viskit.model.VInstantiator;
import viskit.VStatics;
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

    private JLabel handleLab; //,outputLab;
    private JTextField handleField;

    // verboseCheck not used, does nothing for Viskit
    private JCheckBox outputCheck /*, verboseCheck*/;
    private InstantiationPanel ip;
    private EvGraphNode egNode;
    private JButton okButt, canButt;
    private enableApplyButtonListener lis;
    private JPanel buttPan;
    private JTextField descField;
    private JLabel descLab;

    public static boolean showDialog(JFrame f, EvGraphNode parm) {
        try {
            if (dialog == null) {
                dialog = new EventGraphNodeInspectorDialog(f, parm);
            } else {
                dialog.setParams(f, parm);
            }
        } catch (ClassNotFoundException e) {
            String msg = "An object type specified in this element (probably " + parm.getType() + ") was not found.\n" +
                    "Add the XML or class file defining the element to the proper list at left.";
            JOptionPane.showMessageDialog(f, msg, "Event Graph Definition Not Found", JOptionPane.ERROR_MESSAGE);
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

    private EventGraphNodeInspectorDialog(JFrame parent, EvGraphNode node) throws ClassNotFoundException {
        super(parent, "Event Graph Inspector", true);
        this.egNode = node;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        lis = new enableApplyButtonListener();

        JPanel content = new JPanel();
        setContentPane(content);
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        handleField = new JTextField();
        VStatics.clampHeight(handleField);
        handleLab = new JLabel("name", JLabel.TRAILING);
        handleLab.setLabelFor(handleField);
        outputCheck = new JCheckBox("detailed output");
        outputCheck.setToolTipText("Enable a list dump of all entity names to the console");

        descField = new JTextField();
        VStatics.clampHeight(descField);
        descLab = new JLabel("description", JLabel.TRAILING);
        descLab.setLabelFor(descField);

        VStatics.cloneSize(handleLab, descLab);    // make handle same size

        buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        canButt = new JButton("Cancel");
        okButt = new JButton("Apply changes");
        okButt.setEnabled(false);
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(canButt);
        buttPan.add(okButt);

        placeWidgets();

        // attach listeners
        canButt.addActionListener(new cancelButtonListener());
        okButt.addActionListener(new applyButtonListener());

        handleField.addCaretListener(lis);
        descField.addCaretListener(lis);
        outputCheck.addActionListener(lis);

        setParams(parent, node);
    }

    public final void setParams(Component c, EvGraphNode p) throws ClassNotFoundException {
        egNode = p;

        fillWidgets();
        getContentPane().invalidate();
        getRootPane().setDefaultButton(canButt);

        pack();     // do this prior to next
        setLocationRelativeTo(c);
    }

    private void placeWidgets()
    {
        JPanel content = (JPanel)getContentPane();
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel bcont = new JPanel();
        bcont.setLayout(new BoxLayout(bcont, BoxLayout.X_AXIS));
        bcont.add(handleLab);
        bcont.add(Box.createHorizontalStrut(5));
        bcont.add(handleField);
        bcont.add(Box.createHorizontalStrut(2));
        bcont.add(outputCheck);
        content.add(bcont);

        JPanel dcont = new JPanel();
        dcont.setLayout(new BoxLayout(dcont, BoxLayout.X_AXIS));
        dcont.add(descLab);
        dcont.add(Box.createHorizontalStrut(5));
        dcont.add(descField);

        content.add(dcont);
        ip = new InstantiationPanel(this, lis, true);

        ip.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                    "Object creation", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));

        ip.setAlignmentX(Box.CENTER_ALIGNMENT);
        content.add(ip);
        content.add(Box.createVerticalStrut(5));
        content.add(buttPan);
    }

    private void fillWidgets() throws ClassNotFoundException {
        if (egNode != null) {
            handleField.setText(egNode.getName());
            outputCheck.setSelected(egNode.isOutputMarked());
            descField.setText(egNode.getDescriptionString());
            ip.setData(egNode.getInstantiator());
        } else {
            handleField.setText("egNode name");
            outputCheck.setSelected(false);
            descField.setText("");
       }
    }

    private void unloadWidgets() {
        String nm = handleField.getText();
        nm = nm.replaceAll("\\s", "");
        if (egNode != null) {
            egNode.setName(nm);
            egNode.setDescriptionString(descField.getText().trim());
            egNode.setInstantiator(ip.getData());
            egNode.setOutputMarked(outputCheck.isSelected());
        } else {
            newName = nm;
            newInstantiator = ip.getData();
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
            okButt.setEnabled(true);
            getRootPane().setDefaultButton(okButt);
        }
    }

    /**
     * Check for blank fields and return true if user wants to cancel close
     * @return true = cancel close
     */
    boolean checkBlankFields() {
        VInstantiator vi;

        if (egNode != null) {
            vi = egNode.getInstantiator();
        } else {
            vi = newInstantiator;
        }
        testLp:
        {
            if (handleField.getText().trim().isEmpty()) {
                break testLp;
            }
            if (!vi.isValid()) {
                break testLp;
            }
            return false; // no blank fields, don't cancel close
        }   // testLp

        // Here if we found a problem
        int ret = JOptionPane.showConfirmDialog(
                EventGraphNodeInspectorDialog.this,
                "All fields must be completed. Close anyway?",
                "Question",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return ret != JOptionPane.YES_OPTION; // don't cancel
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
