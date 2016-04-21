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
public class PropertyListDialog extends JDialog {

    private static PropertyListDialog dialog;
    private static int selection = -1;
    private String[][] pnamesTypes;
    private final JButton okButton,  cancelButton;
    private final JTable table;
    private final JPanel buttonPanel;
    public static String newProperty;

    public static int showDialog(Dialog f, String title, String[][] namesTypes) {
        if (dialog == null) {
            dialog = new PropertyListDialog(f, title, namesTypes);
        } else {
            dialog.setParams(f, namesTypes);
        }

        dialog.setVisible(true);
        // above call blocks
        return selection;
    }

    private PropertyListDialog(Dialog parent, String title, String[][] namesTypes) {
        super(parent, title, true);
        this.pnamesTypes = namesTypes;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        table = new JTable();
        table.getSelectionModel().addListSelectionListener(new mySelectionListener());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            okButton = new JButton("Apply changes");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createHorizontalStrut(5));

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        setParams(parent, namesTypes);
    }

    public final void setParams(Component c, String[][] namesTypes) {
        pnamesTypes = namesTypes;

        fillWidgets();

        if (pnamesTypes == null) {
            selection = 0;
        }
        okButton.setEnabled(pnamesTypes == null);

        getRootPane().setDefaultButton(cancelButton);
        pack();
        setLocationRelativeTo(c);
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

        content.add(buttonPanel, BorderLayout.SOUTH);
        //content.add(Box.createVerticalStrut(5));
        setContentPane(content);
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
        public void valueChanged(ListSelectionEvent e) {
            //Ignore extra messages.
            if (e.getValueIsAdjusting()) {
                return;
            }

            ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            if (!lsm.isSelectionEmpty()) {
                selection = lsm.getMinSelectionIndex();
                okButton.setEnabled(true);
                getRootPane().setDefaultButton(okButton);
            }
        }
    }

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (selection != -1) {
                int returnValue = JOptionPane.showConfirmDialog(PropertyListDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (returnValue == JOptionPane.YES_OPTION) {
                    okButton.doClick();
                } else {
                    cancelButton.doClick();
                }
            } else {
                cancelButton.doClick();
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


