package viskit.view.dialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * A dialog class that lets the user add a new parameter to the document.
 * After the user clicks "OK", "Cancel", or closes the dialog via the
 * close box, the caller retrieves the "buttonChosen" variable from
 * the object to determine the choice. If the user clicked "OK", the
 * caller can retrieve various choices from the object.
 *
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey *
 * @author DMcG
 * @since June 2, 2004
 * @since 9:19:41 AM
 * @version $Id$
 */
public class EventListDialog extends JDialog {

    private static EventListDialog eventListDialog;
    private static int selection = -1;
    private String[] names;
    private final JButton okButton;
    private final JButton cancelButton;
    private final JList<String> list;
    private final JPanel buttonPanel;
    public static String newName;

    public static int showDialog(Dialog f, String title, String[] names)
    {
        if (eventListDialog == null) {
            eventListDialog = new EventListDialog(f, title, names);
        } 
        else {
            eventListDialog.setParameters(f, names);
        }

        eventListDialog.setVisible(true);
        // above call blocks
        return selection;
    }

    private EventListDialog(Dialog parent, String title, String[] names) 
    {
        super(parent, title, true);
        this.names = names;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        list = new JList<>();
        list.getSelectionModel().addListSelectionListener(new mySelectionListener());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        cancelButton = new JButton("Cancel");
            okButton = new JButton("Apply changes");
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createHorizontalStrut(5));

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        setParameters(parent, names);
    }

    public final void setParameters(Component component, String[] names) 
    {
        this.names = names;

        fillWidgets();

        if (names == null) {
            selection = 0;
        }
        okButton.setEnabled(names == null);

        getRootPane().setDefaultButton(cancelButton);
        pack();
        setLocationRelativeTo(component);
    }

    String[] columnNames = {"property name", "property type"};

    private void fillWidgets()
    {
        if (names != null) {
            DefaultListModel<String> defaultListModel = new myUneditableListModel(names);
            //DefaultTableModel dtm = new myUneditableTableModel(names,columnNames);
            list.setModel(defaultListModel);
            list.setVisibleRowCount(5);
        //list.setPreferredScrollableViewportSize(new Dimension(400,200));
        }
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JScrollPane scrollPane = new JScrollPane(list);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        //content.add(Box.createVerticalStrut(5));
        setContentPane(contentPanel);
    }

    private void unloadWidgets() {
    }

    class cancelButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            selection = -1;
            dispose();
        }
    }

    class applyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            dispose();
        }
    }

    class mySelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) 
        {
            //Ignore extra messages.
            if (e.getValueIsAdjusting()) {
                return;
            }

            ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();
            if (!listSelectionModel.isSelectionEmpty()) {

                selection = listSelectionModel.getMinSelectionIndex();
                okButton.setEnabled(true);
                getRootPane().setDefaultButton(okButton);
            }
        }
    }

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) 
        {
            if (selection != -1) 
            {
                int ret = JOptionPane.showConfirmDialog(EventListDialog.this, "Apply changes?",
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

    class myUneditableListModel extends DefaultListModel<String> {

        myUneditableListModel(String[] data) {
            for (int i = 0; i < data.length; i++) {
                add(i, data[i]);
            }
        }
    }

    class myUneditableTableModel extends DefaultTableModel {

        myUneditableTableModel(Object[][] data, Object[] columnNames) {
            super(data, columnNames);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}