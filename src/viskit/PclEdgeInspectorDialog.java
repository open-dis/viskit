package viskit;

import viskit.model.EvGraphNode;
import viskit.model.PropChangeEdge;
import viskit.model.PropChangeListenerNode;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since June 2, 2004
 * @since 9:19:41 AM
 * @version $Id: PclEdgeInspectorDialog.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class PclEdgeInspectorDialog extends JDialog {

    private JLabel sourceLab,  targetLab,  propertyLab,  descLab;
    private JTextField sourceTF,  targetTF,  propertyTF,  descTF;
    private JPanel propertyTFPan;
    private JLabel emptyLab,  emptyTF;
    private static PclEdgeInspectorDialog dialog;
    private static boolean modified = false;
    private PropChangeEdge pclEdge;
    private Component locationComp;
    private JButton okButt,  canButt;
    private JButton propButt;
    private JPanel buttPan;
    private enableApplyButtonListener lis;
    //public static String newProperty;
    public static boolean showDialog(JFrame f, Component comp, PropChangeEdge parm) {
        if (dialog == null) {
            dialog = new PclEdgeInspectorDialog(f, comp, parm);
        } else {
            dialog.setParams(comp, parm);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private PclEdgeInspectorDialog(JFrame parent, Component comp, PropChangeEdge ed) {
        super(parent, "Property Change Connection", true);
        this.pclEdge = ed;
        this.locationComp = comp;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        lis = new enableApplyButtonListener();
        propButt = new JButton("...");
        propButt.addActionListener(new findPropertiesAction());
        propButt.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        sourceLab = new JLabel("source event graph", JLabel.TRAILING);
        targetLab = new JLabel("property change listener", JLabel.TRAILING);
        propertyLab = new JLabel("property", JLabel.TRAILING);
        emptyLab = new JLabel();
        descLab = new JLabel("description");

        sourceTF = new JTextField();
        targetTF = new JTextField();
        propertyTF = new JTextField();
        descTF = new JTextField();

        emptyTF = new JLabel("(an empty entry signifies ALL properties in source)");
        int fsz = emptyTF.getFont().getSize();
        emptyTF.setFont(emptyTF.getFont().deriveFont(fsz - 2));
        propertyTFPan = new JPanel();
        propertyTFPan.setLayout(new BoxLayout(propertyTFPan, BoxLayout.X_AXIS));
        propertyTFPan.add(propertyTF);
        propertyTFPan.add(propButt);
        pairWidgets(sourceLab, sourceTF, false);
        pairWidgets(targetLab, targetTF, false);
        pairWidgets(propertyLab, propertyTFPan, true);
        propertyTF.addCaretListener(lis);
        pairWidgets(emptyLab, emptyTF, false);
        pairWidgets(descLab, descTF, true);

        buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        canButt = new JButton("Cancel");
        okButt = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(canButt);
        buttPan.add(okButt);
        buttPan.add(Box.createHorizontalStrut(5));

        fillWidgets();     // put the data into the widgets

        modified = (ed == null ? true : false);     // if it's a new pclNode, they can always accept defaults with no typing
        okButt.setEnabled((ed == null ? true : false));

        getRootPane().setDefaultButton(canButt);

        pack();     // do this prior to next

        // Make the first display a minimum of 400 width
        Dimension d = getSize();
        d.width = Math.max(d.width, 400);
        setSize(d);

        this.setLocationRelativeTo(locationComp);

        // attach listeners
        canButt.addActionListener(new cancelButtonListener());
        okButt.addActionListener(new applyButtonListener());
    }

    private void pairWidgets(JLabel lab, JComponent tf, boolean edit) {
        Vstatics.clampHeight(tf);
        lab.setLabelFor(tf);
        if (tf instanceof JTextField) {
            ((JTextField) tf).setEditable(edit);
            if (edit) {
                ((JTextField) tf).addCaretListener(lis);
            }
        }
    }

    public void setParams(Component c, PropChangeEdge p) {
        pclEdge = p;
        locationComp = c;

        fillWidgets();

        modified = (p == null ? true : false);
        okButt.setEnabled((p == null ? true : false));

        getRootPane().setDefaultButton(canButt);

        this.setLocationRelativeTo(c);
    }

    private void fillWidgets() {
        if (pclEdge != null) {
            sourceTF.setText(pclEdge.getFrom().toString());
            targetTF.setText(pclEdge.getTo().toString());
            propertyTF.setText(pclEdge.getProperty());
            descTF.setText(pclEdge.getDescriptionString());
        } else {
            propertyTF.setText("listened-to property");
            sourceTF.setText("unset...shouldn't see this");
            targetTF.setText("unset...shouldn't see this");
            descTF.setText("");
        }

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

        JPanel cont = new JPanel(new SpringLayout());
        cont.add(sourceLab);
        cont.add(sourceTF);
        cont.add(targetLab);
        cont.add(targetTF);
        cont.add(propertyLab);
        cont.add(propertyTFPan);
        cont.add(emptyLab);
        cont.add(emptyTF);
        cont.add(descLab);
        cont.add(descTF);
        SpringUtilities.makeCompactGrid(cont, 5, 2, 10, 10, 5, 5);
        content.add(cont);

        content.add(buttPan);
        content.add(Box.createVerticalStrut(5));
        setContentPane(content);
    }

    private void unloadWidgets() {
        if (pclEdge != null) {
            pclEdge.setProperty(propertyTF.getText().trim());
            pclEdge.setDescriptionString(descTF.getText().trim());
        }
    /*
    else {
    newProperty = propertyTF.getText().trim();
    }
     */
    }

    class cancelButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            modified = false;    // for the caller
            setVisible(false);
        }
    }

    class applyButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            if (modified) {
                unloadWidgets();
            }
            setVisible(false);
        }
    }

    class enableApplyButtonListener implements CaretListener, ActionListener {

        public void caretUpdate(CaretEvent event) {
            modified = true;
            okButt.setEnabled(true);
            getRootPane().setDefaultButton(okButt);
        }

        public void actionPerformed(ActionEvent event) {
            caretUpdate(null);
        }
    }

    // TODO: Fix so that it will show parameterized generic types
    class findPropertiesAction implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            Object o = pclEdge.getFrom();
            String classname = null;
            if (o instanceof EvGraphNode) {
                classname = ((EvGraphNode) o).getType();
            } else if (o instanceof PropChangeListenerNode) {
                classname = ((PropChangeListenerNode) o).getType();
            }

            try {
                ClassLoader cl = VGlobals.instance().getWorkClassLoader();
                Class<?> c = Class.forName(classname, false, cl);
                if (c == null) {
                    throw new ClassNotFoundException(classname + " not found");
                }

                Class stopClass = Class.forName("simkit.BasicSimEntity", false, cl);
                BeanInfo binf = Introspector.getBeanInfo(c, stopClass);
                PropertyDescriptor[] pds = binf.getPropertyDescriptors();
                if (pds == null || pds.length <= 0) {
                    JOptionPane.showMessageDialog(PclEdgeInspectorDialog.this, "No properties found in " + classname + ".\n" +
                            "Enter name manually.");
                    return;
                }
                Vector<String> nams = new Vector<String>();
                Vector<String> typs = new Vector<String>();
                for (int i = 0; i < pds.length; i++) {
                    if (pds[i].getWriteMethod() != null) {
                        // want getters but no setter
                        continue;
                    }
                    nams.add(pds[i].getName());
                    typs.add(pds[i].getPropertyType().getName());
                }
                String[][] nms = new String[nams.size()][2];
                for (int i = 0; i < nams.size(); i++) {
                    nms[i][0] = nams.get(i);
                    nms[i][1] = typs.get(i);
                }
                int which = PropertyListDialog.showDialog(PclEdgeInspectorDialog.this, PclEdgeInspectorDialog.this,
                        classname + " Properties", nms);
                if (which != -1) {
                    modified = true;
                    propertyTF.setText(nms[which][0]);
                }
            } catch (Throwable e1) {
                System.out.println("Exception getting bean properties, PclEdgeInspectorDialog: " + e1.getMessage());
                System.out.println(System.getProperty("java.class.path"));
            }
        }
    }

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified == true) {
                int ret = JOptionPane.showConfirmDialog(PclEdgeInspectorDialog.this, "Apply changes?",
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
