package viskit.view;

import edu.nps.util.LogUtilities;
import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.List;
import org.apache.log4j.Logger;

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
public final class ConstructorPanel extends JPanel 
{
    static final Logger LOG = LogUtilities.getLogger(ConstructorPanel.class);

    private JButton selectButton;
    private final ActionListener modifiedListener;
    private JPanel selectButtonPanel;
    private final boolean showButton;
    private final ActionListener selectButtonListener;
    private ObjectListPanel objectListPanel;
    private final JDialog parent;

    public ConstructorPanel(ActionListener modifiedListener, boolean showSelectButton, ActionListener selectListener, JDialog parentDialog) 
	{
        this.modifiedListener = modifiedListener;
        showButton = showSelectButton;
        selectButtonListener = selectListener;
        this.parent = parentDialog;
		
		initialize ();
    }
	
	public void initialize ()
	{
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        selectButtonPanel = new JPanel();
        selectButtonPanel.setLayout(new BoxLayout(selectButtonPanel, BoxLayout.X_AXIS));
        selectButtonPanel.add(Box.createHorizontalGlue());
        selectButton = new JButton("Select this constructor");
        selectButtonPanel.add(selectButton);
        selectButtonPanel.add(Box.createHorizontalGlue());
	}
	
	public void hideSelectConstructorButton ()
	{
		selectButton.setVisible(false);
		selectButtonPanel.setVisible(false);
		repaint();
	}
	
	public void showSelectConstructorButton ()
	{
		selectButton.setVisible(true);
		selectButtonPanel.setVisible(true);
		repaint();
	}

    public void setData(List<Object> args) // of VInstantiators
    {
        this.removeAll();
        add(Box.createVerticalGlue());

        objectListPanel = new ObjectListPanel(modifiedListener); // may have to intercept
        objectListPanel.setDialogInfo(parent);
        objectListPanel.setData(args, true);
        if (args.size() > 0) {
            add(objectListPanel);
        } 
		else
		{
            JLabel label = new JLabel("zero argument constructor");
            label.setAlignmentX(Box.CENTER_ALIGNMENT);
            add(label);
        }
        if (showButton) {
            add(Box.createVerticalStrut(5));
            add(Box.createVerticalGlue());

            add(selectButtonPanel);
            add(Box.createVerticalStrut(5));

            if (selectButtonListener != null) {
                selectButton.addActionListener(selectButtonListener);
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

    public void setSelected(boolean tf) {
        if (objectListPanel != null) {
            objectListPanel.setEnabled(tf);
        }  // todo...make this work maybe olp should be built in constructor
    }

    /**
     * @param clazz Class&lt;?&gt;[] array, typically from constructor signature
     * @return String identifying Class's signature
     */
    public static String getSignature(Class<?>[] clazz) {
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