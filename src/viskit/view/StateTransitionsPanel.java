package viskit.view;

import edu.nps.util.LogUtilities;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.model.EventStateTransition;
import viskit.model.ViskitElement;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 2:56:21 PM
 * @version $Id$
 */
public class StateTransitionsPanel extends JPanel
{
    static final Logger LOG = LogUtilities.getLogger(StateTransitionsPanel.class);

    private JList<String> modelList;
    private JButton minusButton, plusButton;
    private MyListModel model;
    private JButton editButton;
	private boolean enabled = true;
    private JLabel instructionsLabel = new JLabel("Double click a row to ");

    public StateTransitionsPanel()
	{
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(Box.createHorizontalGlue());
        int bigSz = getFont().getSize();
        instructionsLabel.setFont(getFont().deriveFont(Font.ITALIC, (float) (bigSz - 2)));
        p.add(instructionsLabel);
        editButton = new JButton("edit.");
        editButton.setFont(getFont().deriveFont(Font.ITALIC, (float) (bigSz - 2)));
        editButton.setBorder(null);
        editButton.setEnabled(false);
        p.add(editButton);
        p.add(Box.createHorizontalGlue());
        add(p);

        model = new MyListModel();
        model.addElement("1");
        model.addElement("2");
        model.addElement("3");
        model.addElement("4");
        model.addElement("5");
        modelList = new JList<>(model);
        modelList.setVisibleRowCount(5);

        JScrollPane jsp = new JScrollPane(modelList);
        Dimension dd = jsp.getPreferredSize();
        dd.width = Integer.MAX_VALUE;
        jsp.setMinimumSize(dd);
        add(jsp);

        add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        plusButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/plus.png")));
        plusButton.setBorder(null);
        plusButton.setText(null);
        Dimension d = plusButton.getPreferredSize();
        plusButton.setMinimumSize(d);
        plusButton.setMaximumSize(d);
        buttonPanel.add(plusButton);
        minusButton =   new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/minus.png")));
        minusButton.setDisabledIcon(new ImageIcon(ClassLoader.getSystemResource("viskit/images/minusGrey.png")));
        d = plusButton.getPreferredSize();
        minusButton.setMinimumSize(d);
        minusButton.setMaximumSize(d);
        minusButton.setBorder(null);
        minusButton.setText(null);
        minusButton.setActionCommand("m");
        minusButton.setEnabled(false);
        buttonPanel.add(minusButton);
        buttonPanel.add(Box.createHorizontalGlue());
        add(buttonPanel);

        add(Box.createVerticalStrut(5));

        dd = this.getPreferredSize();
        this.setMinimumSize(dd);

        plusButton.addActionListener(new PlusButtonListener());
        minusButton.addActionListener(new MinusButtonListener());

        modelList.addListSelectionListener(new MyListSelectionListener());
        modelList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent event)
			{
                if (event.getClickCount() == 2)
				{
                    if (myMouseListener != null)
					{
                        int index = modelList.getSelectedIndex();

                        // Don't fail on ArrayIndexOutOfBoundsException
                        if (index == -1)
						{
                            return;
                        }
                        ViskitElement est = arrayList.get(index);
                        event.setSource(est);
                        myMouseListener.mouseClicked(event);
                    }
                }
            }
        });
    }

    // TODO: combine with list model.  List has no clone method implemented
    ArrayList<ViskitElement> arrayList = new ArrayList<>();

	@Override
	public void setEnabled (boolean value)
	{
		super.setEnabled(value);
		minusButton.setEnabled(value);
		plusButton.setEnabled(value);
		modelList.setEnabled(value);
		enabled = value;
		if (!value)
		{
			instructionsLabel.setVisible(false);
			       editButton.setVisible(false);
		}
	}
    public void setTransitions(List<? extends ViskitElement> tLis) {
        clearTransitions();
        for (ViskitElement est : tLis) {
            addTransition(est);
        }
    }

    // We know this to be an ArrayList<ViskitElement> clone
    @SuppressWarnings("unchecked")
    public ArrayList<ViskitElement> getTransitions() {
        return (ArrayList<ViskitElement>) arrayList.clone();
    }

    private void addTransition(ViskitElement est) {
        model.addElement(transitionString(est));
        arrayList.add(est);
    }

    private String transitionString(ViskitElement est) {
        return est.toString();
    }

    public void clearTransitions() {
        model.removeAllElements();
        arrayList.clear();
    }

    /** Used to determine whether to show or hide StateTransitions
     *
     * @return a state transitions string
     */
    public String getString() {
        String s = "";
        for (Enumeration<String> en = model.elements(); en.hasMoreElements();)
		{
            s += en.nextElement();
            s += "\n";
        }

        // lose last cr
        if (s.length() > 0) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
    private ActionListener myPlusListener;

    public void addPlusListener(ActionListener actionListener) {
        myPlusListener = actionListener;
    }
    private ActionListener myMinusListener;

    public void addMinusListener(ActionListener actionListener) {
        myMinusListener = actionListener;
    }
    private MouseListener myMouseListener;

    public void addDoubleClickedListener(MouseListener ml) {
        myMouseListener = ml;
    }

    public void updateStateTransition(EventStateTransition eventStateTransition)
	{
        int index = arrayList.indexOf(eventStateTransition);
        model.setElementAt(transitionString(eventStateTransition), index);
    }

    class MyListModel extends DefaultListModel<String> {

        MyListModel() {
            super();
        }
    }

    class PlusButtonListener implements ActionListener
	{
        @Override
        public void actionPerformed(ActionEvent event)
		{
            if (ViskitGlobals.instance().getStateVariablesCBModel().getSize() <= 0) {
                ViskitGlobals.instance().getEventGraphController().messageToUser(
                    JOptionPane.ERROR_MESSAGE,
                    "Alert",
                    "No state variables have been defined," +
                        "\ntherefore no state transitions are possible.");
                return;
            }
			
            model.addElement("double click to edit");
            modelList.setVisibleRowCount(Math.max(model.getSize(), 5));
            modelList.ensureIndexIsVisible(model.getSize() - 1);
            modelList.setSelectedIndex(model.getSize() - 1);
            StateTransitionsPanel.this.invalidate();
            minusButton.setEnabled(true);
            EventStateTransition eventStateTransition = new EventStateTransition();
            arrayList.add(eventStateTransition);
            if (myPlusListener != null) {
                myPlusListener.actionPerformed(event);
            }

            // This does an edit immediately, and doesn't require a separate double click
            if (myMouseListener != null) {
                MouseEvent mouseEvent = new MouseEvent(plusButton, 0, 0, 0, 0, 0, 2, false);   // plusButt temporarily used
                mouseEvent.setSource(eventStateTransition);
                myMouseListener.mouseClicked(mouseEvent);

                // If they cancelled, kill it
                String result = model.get(model.getSize() - 1);
                if ("double click to edit".equals(result) || result.isEmpty())
				{  // remove it
                    ActionEvent ae = new ActionEvent(minusButton, 0, "delete");  // dummy
                    minusButton.getActionListeners()[0].actionPerformed(ae);
                }
            }
        }
    }

    class MinusButtonListener implements ActionListener
	{
        @Override
        public void actionPerformed(ActionEvent event) {
            if (modelList.getSelectionModel().getValueIsAdjusting()) {
                return;
            }

            int reti = JOptionPane.showConfirmDialog(StateTransitionsPanel.this, "Are you sure?", "Confirm delete", JOptionPane.YES_NO_OPTION);
            if (reti != JOptionPane.YES_OPTION) {
                return;
            }

            int[] sel = modelList.getSelectedIndices();
            if (sel.length != 0) {
                for (int i = 0; i < sel.length; i++) {
                    model.remove(sel[i] - i);
                    arrayList.remove(sel[i] - i);
                }
            }
            if (modelList.getModel().getSize() <= 0) {
                minusButton.setEnabled(false);
            }
            modelList.setVisibleRowCount(Math.max(3, model.getSize()));
            StateTransitionsPanel.this.invalidate();

            if (myMinusListener != null) {
                myMinusListener.actionPerformed(event);
            }
        }
    }

    class MyListSelectionListener implements ListSelectionListener
	{
        @Override
        public void valueChanged(ListSelectionEvent event)
		{
			if (!enabled)
			{
				return;
			}
            if (event.getValueIsAdjusting())
			{
                return;
            }
            if (modelList.getModel().getSize() != 0) {
                boolean b = modelList.getSelectedValue() != null;
                minusButton.setEnabled(b);
            }
        }
    }
}
