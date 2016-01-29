package viskit.view.dialog;

import edu.nps.util.LogUtils;
import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import viskit.ViskitStatics;

/** This is a class to help in code reuse.  There are several small Dialogs
 * which are all used the same way.  This class puts the common code in a single
 * super class.
 *
 * NOTE: This is only working for one class due to the static modifier for the
 * dialog
 *
 * <p>
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu</p>
 *
 * @author Mike Bailey
 * @since May 3, 2004 : 2:39:34 PM
 * @version $Id$
 */
public abstract class ViskitSmallDialog extends JDialog {

    protected static boolean modified = false;
    private static ViskitSmallDialog dialog;

    protected static boolean showDialog(String className, JFrame f, Object var) {
        if (dialog == null) {
            try {
                Class<?>[] args = new Class[] {
                    ViskitStatics.classForName("javax.swing.JFrame"),
                    ViskitStatics.classForName(ViskitStatics.JAVA_LANG_OBJECT)
                };
                Class<?> c = ViskitStatics.classForName(className);
                Constructor<?> constr = c.getDeclaredConstructor(args);
                dialog = (ViskitSmallDialog) constr.newInstance(new Object[] {f, var});
            } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
                LogUtils.getLogger(ViskitSmallDialog.class).error(e);
            }
        } else {
            dialog.setParams(f, var);
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

    class cancelButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            modified = false;    // for the caller
            dispose();
        }
    }

    /** NOT USED */
    class applyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (modified) {
                unloadWidgets();
            }
            dispose();
        }
    }

    class enableApplyButtonListener implements ActionListener, DocumentListener {

        private final JButton applyButton;

        enableApplyButtonListener(JButton applyButton) {
            this.applyButton = applyButton;
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            enableButt();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            enableButt();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            enableButt();
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            enableButt();
        }

        private void enableButt() {
            modified = true;
            applyButton.setEnabled(true);
            getRootPane().setDefaultButton(applyButton);       // in JDialog
        }
    }

    class WindowClosingListener extends WindowAdapter {

        private final Component parent;
        private final JButton okButton;
        private final JButton cancelButton;

        WindowClosingListener(Component parent, JButton okButton, JButton cancelButton) {
            this.parent = parent;
            this.okButton = okButton;
            this.cancelButton = cancelButton;
        }

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int ret = JOptionPane.showConfirmDialog(parent, "Apply changes?",
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
