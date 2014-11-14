package viskit.view.dialog;

import viskit.model.EvGraphNode;
import viskit.model.VInstantiator;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import viskit.Vstatics;
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

    private JLabel handleLab; //,outputLab;
    private JTextField handleField;
    private JCheckBox outputCheck, verboseCheck;
    private InstantiationPanel ip;
    private static EventGraphNodeInspectorDialog dialog;
    private static boolean modified = false;
    private EvGraphNode egNode;
    private Component locationComp;
    private JButton okButt,  canButt;
    private enableApplyButtonListener lis;
    private JPanel buttPan;
    public static String newName;
    public static VInstantiator newInstantiator;
    private JTextField descField;
    private JLabel descLab;

    public static boolean showDialog(JFrame f, Component comp, EvGraphNode parm) {
        try {
            if (dialog == null) {
                dialog = new EventGraphNodeInspectorDialog(f, comp, parm);
            } else {
                dialog.setParams(comp, parm);
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

    private EventGraphNodeInspectorDialog(JFrame parent, Component comp, EvGraphNode lv) throws ClassNotFoundException {
        super(parent, "Event Graph Inspector", true);
        this.egNode = lv;
        this.locationComp = comp;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        lis = new enableApplyButtonListener();

        JPanel content = new JPanel();
        setContentPane(content);
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        handleField = new JTextField();
        Vstatics.clampHeight(handleField);
        handleLab = new JLabel("name", JLabel.TRAILING);
        handleLab.setLabelFor(handleField);
        //outputLab = new JLabel("detailed output",JLabel.TRAILING);
        outputCheck = new JCheckBox("detailed output");

        descField = new JTextField();
        Vstatics.clampHeight(descField);
        descLab = new JLabel("description", JLabel.TRAILING);
        descLab.setLabelFor(descField);
        verboseCheck = new JCheckBox("verbose output");
        
        Vstatics.cloneSize(handleLab, descLab);    // make handle same size
        
        buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        canButt = new JButton("Cancel");
        okButt = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(canButt);
        buttPan.add(okButt);

        placeWidgets();
        fillWidgets();     // put the data into the widgets

        modified = (lv == null);     // if it's a new egNode, they can always accept defaults with no typing
        okButt.setEnabled(lv == null);

        getRootPane().setDefaultButton(canButt);

        pack();     // do this prior to next
        this.setLocationRelativeTo(locationComp);

        // attach listeners
        canButt.addActionListener(new cancelButtonListener());
        okButt.addActionListener(new applyButtonListener());

        handleField.addCaretListener(lis);
        descField.addCaretListener(lis);
        outputCheck.addActionListener(lis);
        verboseCheck.addActionListener(lis);
    }

    public void setParams(Component c, EvGraphNode p) throws ClassNotFoundException {
        egNode = p;
        locationComp = c;

        fillWidgets();
        getContentPane().invalidate();
        modified = (p == null);
        okButt.setEnabled(p == null);

        //getRootPane().setDefaultButton(canButt);
        //pack();
        //this.setLocationRelativeTo(c);
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
        //bcont.add(Box.createHorizontalGlue());
        content.add(bcont);

        JPanel dcont = new JPanel();
        dcont.setLayout(new BoxLayout(dcont, BoxLayout.X_AXIS));
        dcont.add(descLab);
        dcont.add(Box.createHorizontalStrut(5));
        dcont.add(descField);
        dcont.add(Box.createHorizontalStrut(2));
        dcont.add(verboseCheck);
        //dcont.add(Box.createHorizontalGlue());
       
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
            verboseCheck.setSelected(egNode.isVerboseMarked());
            descField.setText(egNode.getDescriptionString());
            //ArrayList alis = egNode.getComments(); //? 
            ip.setData(egNode.getInstantiator());
          } else {
            handleField.setText("egNode name");
            outputCheck.setSelected(false);
            verboseCheck.setSelected(false);
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
            egNode.setVerboseMarked(verboseCheck.isSelected());
        } else {
            newName = nm;
            newInstantiator = ip.getData();
        }
    }

    class cancelButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            modified = false;    // for the caller
            dispose();
        }
    }

    class applyButtonListener implements ActionListener {

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

        public void caretUpdate(CaretEvent event) {
            common();
        }

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
        VInstantiator vi = null;

        if (egNode != null) {
            vi = egNode.getInstantiator();
        } else {
            vi = newInstantiator;
        }
        testLp:
        {
            if (handleField.getText().trim().length() <= 0) {
                break testLp;
            }
            if (!vi.isValid()) {
                break testLp;
            }
            return false; // no blank fields , don't cancel close
        }   // testLp

        // Here if we found a problem
        int ret = JOptionPane.showConfirmDialog(EventGraphNodeInspectorDialog.this, "All fields must be completed. Close anyway?",
                "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ret == JOptionPane.YES_OPTION) {
            return false;  // don't cancel
        } else {
            return true;  // cancel close
        }
    }

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified == true) {
                int ret = JOptionPane.showConfirmDialog(EventGraphNodeInspectorDialog.this, "Apply changes?",
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


