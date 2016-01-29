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

import viskit.model.EventGraphNode;
import viskit.model.VInstantiator;
import viskit.ViskitStatics;
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

    private final JLabel handleLabel; //,outputLab;
    private final JTextField handleField;

    // verboseCheck not used, does nothing for Viskit
    private final JCheckBox outputCheck /*, verboseCheck*/;
    private InstantiationPanel instantiationPanel;
    private EventGraphNode eventGraphNode;
    private final JButton okButton, cancelButton;
    private final enableApplyButtonListener listener;
    private final JPanel buttonPanel;
    private final JTextField descriptionField;
    private final JLabel descriptionLabel;

    public static boolean showDialog(JFrame f, EventGraphNode parm) {
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

    private EventGraphNodeInspectorDialog(JFrame parent, EventGraphNode node) throws ClassNotFoundException {
        super(parent, "Event Graph Inspector", true);
        this.eventGraphNode = node;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        listener = new enableApplyButtonListener();

        JPanel content = new JPanel();
        setContentPane(content);
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        handleField = new JTextField();
        ViskitStatics.clampHeight(handleField);
        handleLabel = new JLabel("name", JLabel.TRAILING);
        handleLabel.setLabelFor(handleField);
        outputCheck = new JCheckBox("detailed output");
        outputCheck.setToolTipText("Enable a list dump of all entity names to the console");

        descriptionField = new JTextField();
        ViskitStatics.clampHeight(descriptionField);
        descriptionLabel = new JLabel("description", JLabel.TRAILING);
        descriptionLabel.setLabelFor(descriptionField);

        ViskitStatics.cloneSize(handleLabel, descriptionLabel);    // make handle same size

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        cancelButton = new JButton("Cancel");
        okButton = new JButton("Apply changes");
        okButton.setEnabled(false);
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);

        placeWidgets();

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        handleField.addCaretListener(listener);
        descriptionField.addCaretListener(listener);
        outputCheck.addActionListener(listener);

        setParams(parent, node);
    }

    public final void setParams(Component c, EventGraphNode p) throws ClassNotFoundException {
        eventGraphNode = p;

        fillWidgets();
        getContentPane().invalidate();
        getRootPane().setDefaultButton(cancelButton);

        pack();     // do this prior to next
        setLocationRelativeTo(c);
    }

    private void placeWidgets()
    {
        JPanel content = (JPanel)getContentPane();
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel bcont = new JPanel();
        bcont.setLayout(new BoxLayout(bcont, BoxLayout.X_AXIS));
        bcont.add(handleLabel);
        bcont.add(Box.createHorizontalStrut(5));
        bcont.add(handleField);
        bcont.add(Box.createHorizontalStrut(2));
        bcont.add(outputCheck);
        content.add(bcont);

        JPanel dcont = new JPanel();
        dcont.setLayout(new BoxLayout(dcont, BoxLayout.X_AXIS));
        dcont.add(descriptionLabel);
        dcont.add(Box.createHorizontalStrut(5));
        dcont.add(descriptionField);

        content.add(dcont);
        instantiationPanel = new InstantiationPanel(this, listener, true);

        instantiationPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                    "Object creation", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));

        instantiationPanel.setAlignmentX(Box.CENTER_ALIGNMENT);
        content.add(instantiationPanel);
        content.add(Box.createVerticalStrut(5));
        content.add(buttonPanel);
    }

    private void fillWidgets() throws ClassNotFoundException {
        if (eventGraphNode != null) {
            handleField.setText(eventGraphNode.getName());
            outputCheck.setSelected(eventGraphNode.isOutputMarked());
            descriptionField.setText(eventGraphNode.getDescriptionString());
            instantiationPanel.setData(eventGraphNode.getInstantiator());
        } else {
            handleField.setText("egNode name");
            outputCheck.setSelected(false);
            descriptionField.setText("");
       }
    }

    private void unloadWidgets() {
        String nm = handleField.getText();
        nm = nm.replaceAll("\\s", "");
        if (eventGraphNode != null) {
            eventGraphNode.setName(nm);
            eventGraphNode.setDescriptionString(descriptionField.getText().trim());
            eventGraphNode.setInstantiator(instantiationPanel.getData());
            eventGraphNode.setOutputMarked(outputCheck.isSelected());
        } else {
            newName = nm;
            newInstantiator = instantiationPanel.getData();
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
            okButton.setEnabled(true);
            getRootPane().setDefaultButton(okButton);
        }
    }

    /**
     * Check for blank fields and return true if user wants to cancel close
     * @return true = cancel close
     */
    boolean checkBlankFields() {
        VInstantiator vi;

        if (eventGraphNode != null) {
            vi = eventGraphNode.getInstantiator();
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
