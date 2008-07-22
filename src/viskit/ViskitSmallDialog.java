package viskit;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;

/** This is a class to help in code reuse.  There are several small Dialogs 
 * which are all used the same way.  This class puts the common code in a single
 * super class.
 * 
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 3, 2004
 * @since: 2:39:34 PM
 * @version $Id$
 */
public abstract class ViskitSmallDialog extends JDialog {

    public static boolean modified = false;
    private static ViskitSmallDialog dialog;

    protected static boolean showDialog(String className, JFrame f, Component comp, Object var) {
        if (dialog == null) {
            try {
                Class[] args = new Class[] {Class.forName("javax.swing.JFrame"),
                    Class.forName("java.awt.Component"),
                    Class.forName("java.lang.Object")
                };
                Class<?> c = Vstatics.classForName("viskit." + className);
                Constructor constr = c.getDeclaredConstructor(args);
                dialog = (ViskitSmallDialog) constr.newInstance(new Object[] {f, comp, var});
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            dialog.setParams(comp, var);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    abstract void setParams(Component comp, Object o);

    abstract void unloadWidgets();

    protected ViskitSmallDialog(JFrame parent, String title, boolean bool) {
        super(parent, title, bool);
    }

    protected void setMaxHeight(JComponent c) {
        Dimension d = c.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        c.setMaximumSize(d);
    }

    protected int maxWidth(JComponent[] c) {
        int tmpw = 0, maxw = 0;
        for (int j = 0; j < c.length; j++) {
            tmpw = c[j].getPreferredSize().width;
            if (tmpw > maxw) {
                maxw = tmpw;
            }
        }
        return maxw;
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

    class enableApplyButtonListener implements ActionListener, DocumentListener {

        private JButton applyButt;

        enableApplyButtonListener(JButton applyButton) {
            this.applyButt = applyButton;
        }

        public void changedUpdate(DocumentEvent e) {
            enableButt();
        }

        public void insertUpdate(DocumentEvent e) {
            enableButt();
        }

        public void removeUpdate(DocumentEvent e) {
            enableButt();
        }

        public void actionPerformed(ActionEvent event) {
            enableButt();
        }

        private void enableButt() {
            modified = true;
            applyButt.setEnabled(true);
            getRootPane().setDefaultButton(applyButt);       // in JDialog
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

    class WindowClosingListener extends WindowAdapter {

        private Component parent;
        private JButton okButt;
        private JButton cancelButt;

        WindowClosingListener(Component parent, JButton okButt, JButton cancelButt) {
            this.parent = parent;
            this.okButt = okButt;
            this.cancelButt = cancelButt;
        }

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified == true) {
                int ret = JOptionPane.showConfirmDialog(parent, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
                    okButt.doClick();
                } else {
                    cancelButt.doClick();
                }
            } else {
                cancelButt.doClick();
            }
        }
    }
}
