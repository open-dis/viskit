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
import viskit.ViskitGlobals;

import viskit.model.EventGraphNode;
import viskit.model.ViskitModelInstantiator;
import viskit.ViskitStatics;
import static viskit.view.EventGraphViewFrame.DESCRIPTION_HINT;
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
    public static ViskitModelInstantiator newViskitModelnstantiator;
    private static EventGraphNodeInspectorDialog eventGraphNodeInspectorDialog;
    private static boolean modified = false;

    private final JLabel handleLabel; //,outputLab;
    private final JTextField handleField;

    // verboseCheck not used, does nothing for Viskit
    private final JCheckBox outputCheck /*, verboseCheck*/;
    private InstantiationPanel instantiationPanel;
    private EventGraphNode eventGraphNode;
    private final JButton okButton;
    private final JButton cancelButton;
    private final EnableApplyButtonListener enableApplyButtonListener;
    private final JPanel buttonPanel;
    private final JTextField descriptionField;
    private final JLabel descriptionLabel;

    public static boolean showDialog(JFrame f, EventGraphNode parm) {
        try {
            if (eventGraphNodeInspectorDialog == null) {
                eventGraphNodeInspectorDialog = new EventGraphNodeInspectorDialog(f, parm);
            } else {
                eventGraphNodeInspectorDialog.setParams(f, parm);
            }
        } catch (ClassNotFoundException e) {
            String msg = "An object type specified in this element (probably " + parm.getType() + ") was not found.\n" +
                    "Add the XML or class file defining the element to the proper list at left.";
            ViskitGlobals.instance().getAssemblyEditor().genericReport(JOptionPane.ERROR_MESSAGE, "Event Graph Definition Not Found", msg);
            eventGraphNodeInspectorDialog = null;
            return false; // unmodified
        }

        //Having trouble getting this beast to redraw with new data, at least on the Mac.
        //The following little bogosity works, plus the invalidate call down below.
        Dimension d = eventGraphNodeInspectorDialog.getSize();
        eventGraphNodeInspectorDialog.setSize(d.width+1, d.height+1);
        eventGraphNodeInspectorDialog.setSize(d);

        eventGraphNodeInspectorDialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private EventGraphNodeInspectorDialog(JFrame parent, EventGraphNode node) throws ClassNotFoundException {
        super(parent, "Event Graph Inspector", true);
        this.eventGraphNode = node;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new MyCloseListener());

        enableApplyButtonListener = new EnableApplyButtonListener();

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
        descriptionField.setToolTipText(DESCRIPTION_HINT);
        ViskitStatics.clampHeight(descriptionField);
        descriptionLabel = new JLabel("description", JLabel.TRAILING);
        descriptionLabel.setToolTipText(DESCRIPTION_HINT);
        descriptionLabel.setLabelFor(descriptionField);

        ViskitStatics.cloneSize(handleLabel, descriptionLabel);    // make handle same size

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        cancelButton = new JButton("Cancel");
        okButton = new JButton("Apply changes");
        okButton.setEnabled(false);
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        placeWidgets();

        // attach listeners
        cancelButton.addActionListener(new CancelButtonListener());
        okButton.addActionListener(new ApplyButtonListener());

        handleField.addCaretListener(enableApplyButtonListener);
        descriptionField.addCaretListener(enableApplyButtonListener);
        outputCheck.addActionListener(enableApplyButtonListener);

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
        instantiationPanel = new InstantiationPanel(this, enableApplyButtonListener, true);

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
            handleField.setText("eventGraphNode name");
            outputCheck.setSelected(false);
            descriptionField.setText("");
       }
    }

    private void unloadWidgets() {
        String newName = handleField.getText();
        newName = newName.replaceAll("\\s", "");
        if (eventGraphNode != null) {
            eventGraphNode.setName(newName);
            eventGraphNode.setDescriptionString(descriptionField.getText().trim());
            eventGraphNode.setInstantiator(instantiationPanel.getData());
            eventGraphNode.setOutputMarked(outputCheck.isSelected());
        } else {
            newName = newName;
            newViskitModelnstantiator = instantiationPanel.getData();
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
                if (checkBlankFields()) {
                    return;
                }
            }
            dispose();
        }
    }

    class EnableApplyButtonListener implements CaretListener, ActionListener {

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
        ViskitModelInstantiator viskitModelInstantiator;

        if (eventGraphNode != null) {
            viskitModelInstantiator = eventGraphNode.getInstantiator();
        } else {
            viskitModelInstantiator = newViskitModelnstantiator;
        }
        testLp:
        {
            if (handleField.getText().trim().isEmpty()) {
                break testLp;
            }
            if (!viskitModelInstantiator.isValid()) {
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

    class MyCloseListener extends WindowAdapter {

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
