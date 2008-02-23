package viskit;

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
 * @version $Id:$
 */
public class EventListDialog extends JDialog {

    private static EventListDialog dialog;
    private static int selection = -1;
    private String[] names;
    private Component locationComp;
    private JButton okButt,  canButt;
    private JList list;
    private JPanel buttPan;
    public static String newName;

    public static int showDialog(Dialog f, Component comp, String title, String[] names) {
        if (dialog == null) {
            dialog = new EventListDialog(f, comp, title, names);
        } else {
            dialog.setParams(comp, names);
        }

        dialog.setVisible(true);
        // above call blocks
        return selection;
    }

    private EventListDialog(Dialog parent, Component comp, String title, String[] namesTypes) {
        super(parent, title, true);
        this.names = namesTypes;
        this.locationComp = comp;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        list = new JList();
        list.getSelectionModel().addListSelectionListener(new mySelectionListener());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        canButt = new JButton("Cancel");
        okButt = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(canButt);
        buttPan.add(okButt);
        buttPan.add(Box.createHorizontalStrut(5));

        fillWidgets();     // put the data into the widgets

        if (names != null) {
            selection = 0;
        }    // if it's a new pclNode, they can always accept defaults with no typing
        okButt.setEnabled(names == null);

        getRootPane().setDefaultButton(canButt);

        pack();     // do this prior to next

        this.setLocationRelativeTo(locationComp);

        // attach listeners
        canButt.addActionListener(new cancelButtonListener());
        okButt.addActionListener(new applyButtonListener());
    }

    public void setParams(Component c, String[] names) {
        this.names = names;
        locationComp = c;

        fillWidgets();

        if (names == null) {
            selection = 0;
        }
        okButt.setEnabled(names == null);

        getRootPane().setDefaultButton(canButt);

        this.setLocationRelativeTo(c);
    }
    
    String[] colNames = {"property name", "property type"};

    private void fillWidgets() {
        if (names != null) {
            DefaultListModel dlm = new myUneditableListModel(names);
            //DefaultTableModel dtm = new myUneditableTableModel(names,colNames);
            list.setModel(dlm);
            list.setVisibleRowCount(5);
        //list.setPreferredScrollableViewportSize(new Dimension(400,200));
        }
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        content.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JScrollPane jsp = new JScrollPane(list);
        content.add(jsp, BorderLayout.CENTER);

        content.add(buttPan, BorderLayout.SOUTH);
        //content.add(Box.createVerticalStrut(5));
        setContentPane(content);
    }

    private void unloadWidgets() {
    }

    class cancelButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            selection = -1;
            setVisible(false);
        }
    }

    class applyButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            setVisible(false);
        }
    }

    class mySelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            //Ignore extra messages.
            if (e.getValueIsAdjusting()) {
                return;
            }

            ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            if (lsm.isSelectionEmpty()) {
                return;
            } else {
                selection = lsm.getMinSelectionIndex();
                okButt.setEnabled(true);
                getRootPane().setDefaultButton(okButt);
            }
        }
    }

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (selection != -1) {
                int ret = JOptionPane.showConfirmDialog(EventListDialog.this, "Apply changes?",
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

    class myUneditableListModel extends DefaultListModel {

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