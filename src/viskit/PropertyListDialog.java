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
 * @version $Id$
 */
public class PropertyListDialog extends JDialog {

    private static PropertyListDialog dialog;
    private static int selection = -1;
    private String[][] pnamesTypes;
    private Component locationComp;
    private JButton okButt,  canButt;
    private JTable table;
    private JPanel buttPan;
    public static String newProperty;

    public static int showDialog(Dialog f, Component comp, String title, String[][] namesTypes) {
        if (dialog == null) {
            dialog = new PropertyListDialog(f, comp, title, namesTypes);
        } else {
            dialog.setParams(comp, namesTypes);
        }

        dialog.setVisible(true);
        // above call blocks
        return selection;
    }

    private PropertyListDialog(Dialog parent, Component comp, String title, String[][] namesTypes) {
        super(parent, title, true);
        this.pnamesTypes = namesTypes;
        this.locationComp = comp;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        table = new JTable();
        table.getSelectionModel().addListSelectionListener(new mySelectionListener());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        canButt = new JButton("Cancel");
        okButt = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(canButt);
        buttPan.add(okButt);
        buttPan.add(Box.createHorizontalStrut(5));

        fillWidgets();     // put the data into the widgets

        if (pnamesTypes != null) {
            selection = 0;
        }    // if it's a new pclNode, they can always accept defaults with no typing
        okButt.setEnabled(pnamesTypes == null);

        getRootPane().setDefaultButton(canButt);

        pack();     // do this prior to next

        this.setLocationRelativeTo(locationComp);

        // attach listeners
        canButt.addActionListener(new cancelButtonListener());
        okButt.addActionListener(new applyButtonListener());
    }

    public void setParams(Component c, String[][] namesTypes) {
        pnamesTypes = namesTypes;
        locationComp = c;

        fillWidgets();

        if (pnamesTypes == null) {
            selection = 0;
        }
        okButt.setEnabled(pnamesTypes == null);

        getRootPane().setDefaultButton(canButt);

        this.setLocationRelativeTo(c);
    }
    
    String[] colNames = {"property name", "property type"};

    private void fillWidgets() {
        if (pnamesTypes != null) {
            DefaultTableModel dtm = new myUneditableTableModel(pnamesTypes, colNames);
            table.setModel(dtm);
            table.setPreferredScrollableViewportSize(new Dimension(400, 200));
        }
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        content.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JScrollPane jsp = new JScrollPane(table);
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
            dispose();
        }
    }

    class applyButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            dispose();
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
                int ret = JOptionPane.showConfirmDialog(PropertyListDialog.this, "Apply changes?",
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


