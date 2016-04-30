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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

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
public class StateVariableListDialog extends JDialog {

    private static StateVariableListDialog dialog;
    public  static int NO_SELECTION = -1;
    private static int selection    = NO_SELECTION;
    private String[][] beanParameterNamesTypes;
    private JButton okButton,  cancelButton;
    private JTable table;
    private JPanel buttonPanel;
    public static String newProperty;

    String[] columnNames = {"State Variable", "type", "description"};

    private StateVariableListDialog(Dialog parent, String title, String[][] namesTypes)
	{
        super(parent, title, true);
		
		initialize ();

        setParameters(parent, namesTypes);
    }
	
	private void initialize ()
	{
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
	}

    public static int showDialog(Dialog dialogWindow, String title, String[][] namesTypes)
	{
        if (dialog == null)
		{
            dialog = new StateVariableListDialog(dialogWindow, title, namesTypes);
        } 
		else
		{
            dialog.setParameters(dialogWindow, namesTypes);
        }
        dialog.setVisible(true); // this call blocks
		
        return selection;
    }

    public final void setParameters(Component c, String[][] namesTypes)
	{
        beanParameterNamesTypes = namesTypes;

        fillWidgets();

        if (beanParameterNamesTypes == null)
		{
            selection = 0;
        }
        okButton.setEnabled(beanParameterNamesTypes == null);

        getRootPane().setDefaultButton(cancelButton);
        pack();
        setLocationRelativeTo(c);
    }
	
	// http://stackoverflow.com/questions/17627431/auto-resizing-the-jtable-column-widths
	public void resizeColumnWidth(JTable table) {
		final TableColumnModel columnModel = table.getColumnModel();
		for (int column = 0; column < table.getColumnCount(); column++) {
			int width = 50; // Min width
			for (int row = 0; row < table.getRowCount(); row++) {
				TableCellRenderer renderer = table.getCellRenderer(row, column);
				Component comp = table.prepareRenderer(renderer, row, column);
				width = Math.max(comp.getPreferredSize().width + 1, width);
			}
			columnModel.getColumn(column).setPreferredWidth(width);
		}
	}

    private void fillWidgets()
	{
        if (beanParameterNamesTypes != null)
		{
            DefaultTableModel defaultTableModel = new myUneditableTableModel(beanParameterNamesTypes, columnNames);
            table.setModel(defaultTableModel);
			table.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
            table.setPreferredScrollableViewportSize(new Dimension(800, 200));
			resizeColumnWidth(table);
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

//    private void unloadWidgets ()
//	{
//		// connect via callback if needed
//    }

    class cancelButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            selection = NO_SELECTION;
            dispose();
        }
    }

    class applyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            dispose();
        }
    }

    class mySelectionListener implements ListSelectionListener
	{
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

    class myCloseListener extends WindowAdapter
	{
        @Override
        public void windowClosing(WindowEvent e)
		{
            if (selection != NO_SELECTION)
			{
                int returnValue = JOptionPane.showConfirmDialog(StateVariableListDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (returnValue == JOptionPane.YES_OPTION)
				{
                    okButton.doClick();
                } 
				else
				{
                    cancelButton.doClick();
                }
            } 
			else
			{
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


