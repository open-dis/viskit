package viskit.view;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import viskit.ViskitGlobals;
import viskit.control.EventGraphController;
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
public class TransitionsPanel extends JPanel {

    private JList<String> eventStateTransitionsList;
    private JButton minusButton, plusButton;
    private MyListModel eventStateTransitionsListModel;
    private JButton editButton;

    public TransitionsPanel() 
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createHorizontalGlue());
        JLabel instructionsLabel = new JLabel("Double click a row to ");
        int bigSz = getFont().getSize();
        instructionsLabel.setFont(getFont().deriveFont(Font.ITALIC, (float) (bigSz - 2)));
        panel.add(instructionsLabel);
        editButton = new JButton("edit.");
        editButton.setFont(getFont().deriveFont(Font.ITALIC, (float) (bigSz - 2)));
        editButton.setBorder(null);
        editButton.setEnabled(false);
        panel.add(editButton);
        panel.add(Box.createHorizontalGlue());
        add(panel);

        eventStateTransitionsListModel = new MyListModel();
        eventStateTransitionsListModel.addElement("1");
        eventStateTransitionsListModel.addElement("2");
        eventStateTransitionsListModel.addElement("3");
        eventStateTransitionsList = new JList<>(eventStateTransitionsListModel);
        eventStateTransitionsList.setVisibleRowCount(3);

        JScrollPane jScrollPane = new JScrollPane(eventStateTransitionsList);
        Dimension dd = jScrollPane.getPreferredSize();
        dd.width = Integer.MAX_VALUE;
        jScrollPane.setMinimumSize(dd);
        add(jScrollPane);

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
        minusButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/minus.png")));
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

        eventStateTransitionsList.addListSelectionListener(new MyListSelectionListener());
        eventStateTransitionsList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2)
                {
                    if (myMouseListener != null) 
                    {
                        int index = eventStateTransitionsList.getSelectedIndex();

                        // Don't fail on ArrayIndexOutOfBoundsException
                        if (index == -1) {
                            return;
                        }
                        ViskitElement eventStateTransition = eventStateTransitionsArrayList.get(index);
                        mouseEvent.setSource(eventStateTransition);
                        myMouseListener.mouseClicked(mouseEvent);
                    }
                }
            }
        });
    }

    // TODO: combine with list model.  List has no clone method implemented
    ArrayList<ViskitElement> eventStateTransitionsArrayList = new ArrayList<>();

    public void setTransitions(List<? extends ViskitElement> eventStateTransitionList) 
    {
        clearTransitions();
        for (ViskitElement eventStateTransition : eventStateTransitionList) {
            addTransition(eventStateTransition);
        }
    }

    // We know this to be an ArrayList<ViskitElement> clone
    @SuppressWarnings("unchecked")
    public List<ViskitElement> getTransitions() {
        return (List<ViskitElement>) eventStateTransitionsArrayList.clone();
    }

    private void addTransition(ViskitElement eventStateTransition) 
    {
        eventStateTransitionsListModel.addElement(transitionString(eventStateTransition));
        eventStateTransitionsArrayList.add(eventStateTransition);
    }

    private String transitionString(ViskitElement eventStateTransition) {
        return eventStateTransition.toString();
    }

    public void clearTransitions() 
    {
        eventStateTransitionsListModel.removeAllElements();
        eventStateTransitionsArrayList.clear();
    }

    /** Used to determine whether to show or hide StateTransitions
     *
     * @return a state transitions string
     */
    public String getString() {
        String stringValue = "";
        for (Enumeration enumerationValue = eventStateTransitionsListModel.elements(); enumerationValue.hasMoreElements();) {
            stringValue += (String) enumerationValue.nextElement();
            stringValue += "\n";
        }

        // lose last cr
        if (stringValue.length() > 0) {
            stringValue = stringValue.substring(0, stringValue.length() - 1);
        }
        return stringValue;
    }
    private ActionListener myPlusListener;

    public void addPlusListener(ActionListener actionListener) {
        myPlusListener = actionListener;
    }
    private ActionListener myMinusListener;

    public void addMinusListener(ActionListener actionListener) {
        myMinusListener = actionListener;
    }

    public void addDoubleClickedListener(MouseListener mouseListener) {
        myMouseListener = mouseListener;
    }
    private MouseListener myMouseListener;

    public void updateTransition(EventStateTransition eventStateTransition) 
    {
        int index = eventStateTransitionsArrayList.indexOf(eventStateTransition);
        eventStateTransitionsListModel.setElementAt(transitionString(eventStateTransition), index);
    }

    class MyListModel extends DefaultListModel<String> {

        MyListModel() {
            super();
        }
    }
    
    private final String DOUBLE_CLICK_MESSAGE = "double click to edit";

    class PlusButtonListener implements ActionListener 
    {
        @Override
        public void actionPerformed(ActionEvent event) 
        {
            if (ViskitGlobals.instance().getStateVariablesComboBoxModel().getSize() <= 0) 
            {
                ((EventGraphController)ViskitGlobals.instance().getEventGraphController()).messageUser(
                    JOptionPane.ERROR_MESSAGE,
                    "Alert",
                    "No state variables have been defined,\n" +
                    "therefore no state transitions are possible.");
                return;
            }
            eventStateTransitionsListModel.addElement(DOUBLE_CLICK_MESSAGE);
            eventStateTransitionsList.setVisibleRowCount(Math.max(eventStateTransitionsListModel.getSize(), 3));
            eventStateTransitionsList.ensureIndexIsVisible(eventStateTransitionsListModel.getSize() - 1);
            eventStateTransitionsList.setSelectedIndex(eventStateTransitionsListModel.getSize() - 1);
            TransitionsPanel.this.invalidate();
            minusButton.setEnabled(true);
            EventStateTransition eventStateTransition = new EventStateTransition();
            eventStateTransitionsArrayList.add(eventStateTransition);
            if (myPlusListener != null) {
                myPlusListener.actionPerformed(event);
            }

            // This does an edit immediately, and doesn't require a separate double click
            if (myMouseListener != null) {
                MouseEvent mouseEvent = new MouseEvent(plusButton, 0, 0, 0, 0, 0, 2, false);   // plusButt temporarily used
                mouseEvent.setSource(eventStateTransition);
                myMouseListener.mouseClicked(mouseEvent);

                // If they cancelled, kill it
                String result = eventStateTransitionsListModel.get(eventStateTransitionsListModel.getSize() - 1);
                if (DOUBLE_CLICK_MESSAGE.equals(result) || result.isEmpty()) 
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
            if (eventStateTransitionsList.getSelectionModel().getValueIsAdjusting()) {
                return;
            }

            int returnValue = JOptionPane.showConfirmDialog(TransitionsPanel.this, "Are you sure?", "Confirm delete", JOptionPane.YES_NO_OPTION);
            if (returnValue != JOptionPane.YES_OPTION) {
                return;
            }

            int[] selectedIndex = eventStateTransitionsList.getSelectedIndices();
            if (selectedIndex.length != 0) {
                for (int i = 0; i < selectedIndex.length; i++) {
                    eventStateTransitionsListModel.remove(selectedIndex[i] - i);
                    eventStateTransitionsArrayList.remove(selectedIndex[i] - i);
                }
            }
            if (eventStateTransitionsList.getModel().getSize() <= 0) {
                minusButton.setEnabled(false);
            }
            eventStateTransitionsList.setVisibleRowCount(Math.max(3, eventStateTransitionsListModel.getSize()));
            TransitionsPanel.this.invalidate();

            if (myMinusListener != null) {
                myMinusListener.actionPerformed(event);
            }
        }
    }

    class MyListSelectionListener implements ListSelectionListener 
    {
        @Override
        public void valueChanged(ListSelectionEvent listSelectionEven)
        {
            if (listSelectionEven.getValueIsAdjusting()) {
                return;
            }
            if (eventStateTransitionsList.getModel().getSize() != 0) {
                boolean b = eventStateTransitionsList.getSelectedValue() != null;
                minusButton.setEnabled(b);
            }
        }
    }
}
