package viskit.view;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jun 8, 2004
 * @since 8:33:17 AM
 * @version $Id$
 */
public class ConstructorPanel extends JPanel {

    private JButton selectButt;
    private ActionListener modLis;
    private JPanel selectButtPan;
    private boolean showButt;
    private ActionListener selectButtListener;
    private ObjListPanel olp;
    private JDialog parent;

    public ConstructorPanel(ActionListener modifiedListener, boolean showSelectButt, ActionListener selectListener, JDialog parent) {
        modLis = modifiedListener;
        showButt = showSelectButt;
        selectButtListener = selectListener;
        this.parent = parent;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        selectButtPan = new JPanel();
        selectButtPan.setLayout(new BoxLayout(selectButtPan, BoxLayout.X_AXIS));
        selectButtPan.add(Box.createHorizontalGlue());
        selectButt = new JButton("Select this constructor");
        selectButtPan.add(selectButt);
        selectButtPan.add(Box.createHorizontalGlue());
    }

    public void setData(List<Object> args) // of VInstantiators
    {
        this.removeAll();
        add(Box.createVerticalGlue());

        olp = new ObjListPanel(modLis); // may have to intercept
        olp.setDialogInfo(parent);
        olp.setData(args, true);
        if (args.size() > 0) {
            add(olp);
        } else {
            JLabel lab = new JLabel("zero argument constructor");
            lab.setAlignmentX(Box.CENTER_ALIGNMENT);
            add(lab);
        }
        if (showButt) {
            add(Box.createVerticalStrut(5));
            add(Box.createVerticalGlue());

            add(selectButtPan);
            add(Box.createVerticalStrut(5));

            if (selectButtListener != null) {
                selectButt.addActionListener(selectButtListener);
            }

            setSelected(false);
        } else {
            add(Box.createVerticalGlue());
            setSelected(true);
        }
        validate();
    }

    public List<Object> getData() {
        return olp.getData(); // of VInstantiators
    }

    public void setSelected(boolean tf) {
        if (olp != null) {
            olp.setEnabled(tf);
        }  // todo...make this work maybe olp should be built in constructor
    }

    /**
     * @param clazz Class&lt;?&gt;[] array, typically from constructor signature
     * @return String identifying Class's signature
     */
    public static String getSignature(Class<?>[] clazz) {
        StringBuilder buf = new StringBuilder("(");
        for (int i = 0; i < clazz.length; ++i) {
            buf.append(clazz[i].getName());
            if (i < clazz.length - 1) {
                buf.append(',');
            }
        }
        buf.append(')');
        return buf.toString();
    }
}