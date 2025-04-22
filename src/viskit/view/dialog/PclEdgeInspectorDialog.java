package viskit.view.dialog;

import edu.nps.util.Log4jUtilities;
import edu.nps.util.SpringUtilities;

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
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Vector;
import javax.swing.text.JTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.control.AssemblyControllerImpl;
import viskit.model.EventGraphNode;
import viskit.model.PropertyChangeEdge;
import viskit.model.PropertyChangeListenerNode;
import viskit.model.ViskitElement;
import static viskit.ViskitStatics.DESCRIPTION_HINT;

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
public class PclEdgeInspectorDialog extends JDialog
{
    static final Logger LOG = LogManager.getLogger();

    private final JLabel sourceLabel;

    private JLabel targetLabel,  propertyLabel,  descriptionLabel;
    private final JTextField sourceTF;
    private JTextField targetTF,  propertyTF,  descriptionTF;
    private final JPanel propertyTFPanel;
    private final JLabel emptyLab;
    private JLabel emptyTFLabel;
    private static PclEdgeInspectorDialog dialog;
    private static boolean modified = false;
    private PropertyChangeEdge pclEdge;
    private final JButton okButton;
    private JButton cancelButton;
    private final JButton propertyButton;
    private final JPanel buttonPanel;
    private final enableApplyButtonListener lis;

    public static boolean showDialog(JFrame f, PropertyChangeEdge parm) {
        if (dialog == null) {
            dialog = new PclEdgeInspectorDialog(f, parm);
        } else {
            dialog.setParams(f, parm);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private PclEdgeInspectorDialog(JFrame parent, PropertyChangeEdge ed) {
        super(parent, "Property Change Connection", true);
        this.pclEdge = ed;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        lis = new enableApplyButtonListener();
        propertyButton = new JButton("...");
        propertyButton.addActionListener(new findPropertiesAction());
        propertyButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        sourceLabel = new JLabel("source event graph", JLabel.TRAILING);
        targetLabel = new JLabel("property change listener", JLabel.TRAILING);
        propertyLabel = new JLabel("property", JLabel.TRAILING);
        emptyLab = new JLabel();
        descriptionLabel = new JLabel("description", JLabel.TRAILING);
        descriptionLabel.setToolTipText(DESCRIPTION_HINT);  

        sourceTF = new JTextField();
        targetTF = new JTextField();
        propertyTF = new JTextField();
        descriptionTF = new JTextField();
        descriptionTF.setToolTipText(DESCRIPTION_HINT);   

        emptyTFLabel = new JLabel("(an empty entry signifies ALL properties in source)");
        int fsz = emptyTFLabel.getFont().getSize();
        emptyTFLabel.setFont(emptyTFLabel.getFont().deriveFont(fsz - 2));
        propertyTFPanel = new JPanel();
        propertyTFPanel.setLayout(new BoxLayout(propertyTFPanel, BoxLayout.X_AXIS));
        propertyTFPanel.add(propertyTF);
        propertyTFPanel.add(propertyButton);
        pairWidgets(sourceLabel, sourceTF, false);
        pairWidgets(targetLabel, targetTF, false);
        pairWidgets(propertyLabel, propertyTFPanel, true);
        propertyTF.addCaretListener(lis);
        pairWidgets(emptyLab, emptyTFLabel, false);
        pairWidgets(descriptionLabel, descriptionTF, true);

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        cancelButton = new JButton("Cancel");
        okButton = new JButton("Apply changes");
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createHorizontalStrut(5));

        // Make the first display a minimum of 400 width
        Dimension d = getSize();
        d.width = Math.max(d.width, 400);
        setSize(d);

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        setParams(parent, ed);
    }

    private void pairWidgets(JLabel lab, JComponent tf, boolean edit) {
        ViskitStatics.clampHeight(tf);
        lab.setLabelFor(tf);
        if (tf instanceof JTextField) {
            ((JTextComponent) tf).setEditable(edit);
            if (edit) {
                ((JTextComponent) tf).addCaretListener(lis);
            }
        }
    }

    public final void setParams(Component c, PropertyChangeEdge p) {
        pclEdge = p;

        fillWidgets();

        modified = (p == null);
        okButton.setEnabled((p == null));

        getRootPane().setDefaultButton(cancelButton);
        pack();
        setLocationRelativeTo(c);
    }

    private void fillWidgets() {
        if (pclEdge != null) {
            sourceTF.setText(pclEdge.getFrom().toString());
            targetTF.setText(pclEdge.getTo().toString());
            propertyTF.setText(pclEdge.getProperty());
            descriptionTF.setText(pclEdge.getDescription());
        } else {
            propertyTF.setText("listened-to property");
            sourceTF.setText("unset...shouldn't see this");
            targetTF.setText("unset...shouldn't see this");
            descriptionTF.setText("");
        }

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

        JPanel cont = new JPanel(new SpringLayout());
        cont.add(sourceLabel);
        cont.add(sourceTF);
        cont.add(targetLabel);
        cont.add(targetTF);
        cont.add(propertyLabel);
        cont.add(propertyTFPanel);
        cont.add(emptyLab);
        cont.add(emptyTFLabel);
        cont.add(descriptionLabel);
        cont.add(descriptionTF);
        SpringUtilities.makeCompactGrid(cont, 5, 2, 10, 10, 5, 5);
        content.add(cont);

        content.add(buttonPanel);
        content.add(Box.createVerticalStrut(5));
        setContentPane(content);
    }

    private void unloadWidgets() {
        if (pclEdge != null) {
            pclEdge.setProperty(propertyTF.getText().trim());
            pclEdge.setDescription(descriptionTF.getText().trim());
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

    // TODO: Fix so that it will show parameterized generic types
    class findPropertiesAction implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            Object o = pclEdge.getFrom();
            String classname = null;
            if (o instanceof EventGraphNode) {
                classname = ((ViskitElement) o).getType();
            } else if (o instanceof PropertyChangeListenerNode) {
                classname = ((ViskitElement) o).getType();
            }

            try {
                Class<?> c = ViskitStatics.classForName(classname);
                if (c == null) {
                    throw new ClassNotFoundException(classname + " not found");
                }

                Class<?> stopClass = ViskitStatics.classForName("simkit.BasicSimEntity");
                BeanInfo beanInfo = Introspector.getBeanInfo(c, stopClass);
                PropertyDescriptor[] propertyDescriptorArray = beanInfo.getPropertyDescriptors();
                if (propertyDescriptorArray == null || propertyDescriptorArray.length <= 0) {
                    ViskitGlobals.instance().messageUser(
                            JOptionPane.INFORMATION_MESSAGE,
                            "No properties found in " + classname,
                            "Enter name manually.");
                    return;
                }
                Vector<String> nams = new Vector<>();
                Vector<String> typs = new Vector<>();
                for (PropertyDescriptor pd : propertyDescriptorArray) {
                    if (pd.getWriteMethod() != null) {
                        // want getters but no setter
                        continue;
                    }

                    // NOTE: The introspector will return property names in lower case
                    nams.add(pd.getName());

                    if (pd.getPropertyType() != null)
                        typs.add(pd.getPropertyType().getName());
                    else
                        typs.add("");
                }
                String[][] nms = new String[nams.size()][2];
                for (int i = 0; i < nams.size(); i++) {
                    nms[i][0] = nams.get(i);
                    nms[i][1] = typs.get(i);
                }
                int which = PropertyListDialog.showDialog(PclEdgeInspectorDialog.this,
                        classname + " Properties",
                        nms);
                if (which != -1) {
                    modified = true;
                    propertyTF.setText(nms[which][0]);
                }
            } catch (ClassNotFoundException | IntrospectionException | HeadlessException e1) {
                LOG.error("Exception getting bean properties, PclEdgeInspectorDialog: " + e1.getMessage());
                LOG.error(System.getProperty("java.class.path"));
            }
        }
    }

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int ret = JOptionPane.showConfirmDialog(PclEdgeInspectorDialog.this, "Apply changes?",
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
