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

    private final JButton selectConstructorButton;
    private final ActionListener modifiedListener;
    private final JPanel selectedConstructorPanel;
    private final boolean showButton;
    private final ActionListener selectButtonListener;
    private ObjectListPanel objectListPanel;
    private final JDialog parent;

    public ConstructorPanel(ActionListener newModifiedListener, boolean showSelectedButton, ActionListener selectedListener, JDialog parentDialog) 
    {
        modifiedListener = newModifiedListener;
        showButton = showSelectedButton;
        selectButtonListener = selectedListener;
        this.parent = parentDialog;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        selectedConstructorPanel = new JPanel();
        selectedConstructorPanel.setLayout(new BoxLayout(selectedConstructorPanel, BoxLayout.X_AXIS));
        selectedConstructorPanel.add(Box.createHorizontalGlue());
        selectConstructorButton = new JButton("Select this constructor");
        selectConstructorButton.setToolTipText("Selecting a constructor determines which events are passed to the listener, if any");
        selectedConstructorPanel.add(selectConstructorButton);
        selectedConstructorPanel.add(Box.createHorizontalGlue());
    }

    public void setData(List<Object> args) // of VInstantiators
    {
        this.removeAll();
        add(Box.createVerticalGlue());

        objectListPanel = new ObjectListPanel(modifiedListener); // may have to intercept
        objectListPanel.setDialogInfo(parent);
        objectListPanel.setData(args, true);
        if (!args.isEmpty()) {
            add(objectListPanel);
        } else {
            JLabel lab = new JLabel("zero argument constructor");
            lab.setAlignmentX(Box.CENTER_ALIGNMENT);
            add(lab);
        }
        if (showButton) {
            add(Box.createVerticalStrut(5));
            add(Box.createVerticalGlue());

            add(selectedConstructorPanel);
            add(Box.createVerticalStrut(5));

            if (selectButtonListener != null) {
                selectConstructorButton.addActionListener(selectButtonListener);
            }

            setSelected(false);
        } else {
            add(Box.createVerticalGlue());
            setSelected(true);
        }
        revalidate();
    }

    public List<Object> getData() {
        return objectListPanel.getData(); // of VInstantiators
    }

    public void setSelected(boolean enabled) 
    {
        if (objectListPanel != null) {
            objectListPanel.setEnabled(enabled);
        }  // todo...make this work maybe olp should be built in constructor
    }

    /**
     * @param clazz Class&lt;?&gt;[] array, typically from constructor signature
     * @return String identifying Class's signature
     */
    public static String getSignature(Class<?>[] clazz) 
    {
        StringBuilder buffer = new StringBuilder("(");
        for (int i = 0; i < clazz.length; ++i) {
            buffer.append(clazz[i].getName());
            if (i < clazz.length - 1) {
                buffer.append(',');
            }
        }
        buffer.append(')');
        return buffer.toString();
    }
}