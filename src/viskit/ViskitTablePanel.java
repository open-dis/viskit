package viskit;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import viskit.model.ViskitElement;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * @author Mike Bailey
 * @since Apr 8, 2004
 * @since 8:49:21 AM
 * @version $Id$
 */
public abstract class ViskitTablePanel extends JPanel {
    
    protected JTable tab;
    private JScrollPane jsp;
    private JButton plusButt,  minusButt,  edButt;
    private ThisTableModel mod;
    private int defaultWidth = 0,  defaultNumRows = 3;
    
    // List has no implemented clone method
    private ArrayList<ViskitElement> shadow = new ArrayList<ViskitElement>();
    private ActionListener myEditLis,  myPlusLis,  myMinusLis;
    private String plusToolTip = "Add a row to this table";
    private String minusToolTip = "Delete the selected row from this table;";
    private boolean plusMinusEnabled = false;
    private boolean shouldDoAddsAndDeletes = true;

    public ViskitTablePanel(int defaultWidth) {
        this.defaultWidth = defaultWidth;
    }

    public ViskitTablePanel(int defaultWidth, int numRows) {
        this.defaultWidth = defaultWidth;
        this.defaultNumRows = numRows;
    }

    public void init(boolean wantAddDelButts) {
        plusMinusEnabled = wantAddDelButts;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // edit instructions line
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(Box.createHorizontalGlue());
        JLabel instructions = new JLabel("Double click a row to ");
        int bigSz = instructions.getFont().getSize();
        instructions.setFont(getFont().deriveFont(Font.ITALIC, (float) (bigSz - 2)));
        p.add(instructions);
        edButt = new JButton("edit.");
        edButt.setFont(instructions.getFont()); //.deriveFont(Font.ITALIC, (float) (bigSz - 2)));
        edButt.setBorder(null);
        edButt.setEnabled(false);
        edButt.setActionCommand("e");
        p.add(edButt);
        p.add(Box.createHorizontalGlue());
        add(p);

        // the table
        tab = new ThisToolTipTable(mod = new ThisTableModel(getColumnTitles()));
        adjustColumnWidths();
        int rowHeight = tab.getRowHeight();
        int defaultHeight = rowHeight * (defaultNumRows + 1);

        tab.setPreferredScrollableViewportSize(new Dimension(defaultWidth, rowHeight * 3));
        tab.setMinimumSize(new Dimension(20, rowHeight * 2));
        jsp = new JScrollPane(tab);
        jsp.setMinimumSize(new Dimension(defaultWidth, defaultHeight));       // jmb test
        add(jsp);

        ActionListener lis = new MyAddDelEditHandler();

        if (wantAddDelButts) {// plus, minus and edit buttons
            add(Box.createVerticalStrut(5));
            JPanel buttPan = new JPanel();
            buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
            buttPan.add(Box.createHorizontalGlue());
            // add button
            plusButt = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/plus.png")));
            plusButt.setBorder(null);
            plusButt.setText(null);
            plusButt.setToolTipText(getPlusToolTip());
            Dimension dd = plusButt.getPreferredSize();
            plusButt.setMinimumSize(dd);
            plusButt.setMaximumSize(dd);
            plusButt.setActionCommand("p");
            buttPan.add(plusButt);
            // delete button
            minusButt = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/minus.png")));
            minusButt.setDisabledIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/minusGrey.png")));
            minusButt.setBorder(null);
            minusButt.setText(null);
            minusButt.setToolTipText(getMinusToolTip());
            dd = minusButt.getPreferredSize();
            minusButt.setMinimumSize(dd);
            minusButt.setMaximumSize(dd);
            minusButt.setActionCommand("m");
            minusButt.setEnabled(false);
            buttPan.add(minusButt);
            buttPan.add(Box.createHorizontalGlue());
            add(buttPan);
            
            // install local add, delete handlers
            plusButt.addActionListener(lis);
            minusButt.addActionListener(lis);
        }
        // don't let the whole panel get squeezed smaller that what we start out with
        Dimension d = getPreferredSize();
        setMinimumSize(d);

        // install local edit handler
        edButt.addActionListener(lis);

        // install the handler to enable delete and edit buttons only on row-select
        tab.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    boolean yn = tab.getSelectedRowCount() > 0;
                    if (plusMinusEnabled) {
                        minusButt.setEnabled(yn);
                    }
                    edButt.setEnabled(yn);
                }
            }
        });

        // install the double-clicked handler to duplicate action of edit button
        tab.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    doEdit();
                }
            }
        });
    }

    // public methods for working with this class
    /**
     * Install external handler for row edit requests.  The row object can be retrieved by
     * ActionEvent.getSource().
     * @param edLis
     */
    public void addDoubleClickedListener(ActionListener edLis) {
        myEditLis = edLis;
    }

    /**
     * Install external handler for row-add requests.
     * @param addLis
     */
    public void addPlusListener(ActionListener addLis) {
        myPlusLis = addLis;
    }

    /**
     * Install external handler for row-delete requests.  The row object can be retrieved by
     * ActionEvent.getSource().
     * @param delLis
     */
    public void addMinusListener(ActionListener delLis) {
        myMinusLis = delLis;
    }

    /**
     * Add a row defined by the argument to the end of the table.  The table data
     * will be retrieved through the abstract method, getFields(o).
     * @param o
     */
    public void addRow(ViskitElement o) {
        shadow.add(o);

        Vector<String> rowData = new Vector<String>();
        String[] fields = getFields(o, 0);
        rowData.addAll(Arrays.asList(fields));
        mod.addRow(rowData);

        adjustColumnWidths();

        // This doesn't work perfectly on the Mac
        JScrollBar vsb = jsp.getVerticalScrollBar();
        vsb.setValue(vsb.getMaximum());
    }

    /**
     * Add a row to the end of the table.  The row object will be built through 
     * the abstract method, newRowObject().
     */
    public void addRow() {
        addRow(newRowObject());
    }

    /**
     * Remove the row representing the argument from the table.
     * @param o
     */
    public void removeRow(Object o) {
        removeRow(findObjectRow(o));
    }

    /**
     * Remove the row identified by the passed zero-based row number from the 
     * table.
     * @param r index of the object to remove
     */
    public void removeRow(int r) {
        ViskitElement e = shadow.remove(r);
        mod.removeRow(r);
    }

    /**
     * Initialize the table with the passed data.
     * @param data
     */
    public void setData(List<? extends ViskitElement> data) {
        shadow.clear();
        mod.setRowCount(0);

        if (data != null) {
            for (ViskitElement o : data) {
                putARow(o);
            }
        }
        adjustColumnWidths();
    }

    /**
     * Get all the current table data in the form of an array of row objects.
     * @return ArrayList copy of row objects
     */
    // We know this to be an ArrayList<ViskitElement> clone
    @SuppressWarnings("unchecked")
    public List<ViskitElement> getData() {
        return (List<ViskitElement>) shadow.clone();
    }

    public boolean isEmpty() {
        return mod.getRowCount() == 0;
    }

    /**
     * Update the table row, typically after editing, representing the passed rowObject.
     * @param rowObject
     */
    public void updateRow(Object rowObject) {
        int row = findObjectRow(rowObject);

        String[] fields = getFields(rowObject, 0);
        for (int i = 0; i < mod.getColumnCount(); i++) {
            mod.setValueAt(fields[i], row, i);
        }
        adjustColumnWidths();
    }

    // Protected methods
    protected String getPlusToolTip() {
        return plusToolTip;
    }

    protected String getMinusToolTip() {
        return minusToolTip;
    }

    // Abstract methods
    /**
     * Return the column titles.  This defines the number of columns in the display.
     * @return String array of titles
     */
    abstract public String[] getColumnTitles();
    
    /**
     * Return the fields to be displayed in the table.
     * @param o row object
     * @param rowNum row number...not used unless EdgeParametersPanel //todo fix
     * @return  String array of fields
     */
    abstract public String[] getFields(Object o, int rowNum);
    
    /**
     * Build a new row object
     * @return a new row object
     */
    abstract public ViskitElement newRowObject();
    
    /**
     * Specify how many rows the table should display at a minimum
     * @return number of rows
     */
    abstract public int getNumVisibleRows();
    
    // private methods
    /**
     * If a double-clicked listener has been installed, message it with the row
     * object to be edited.
     */
    private void doEdit() {
        if (myEditLis != null) {
            Object o = shadow.get(tab.getSelectedRow());
            ActionEvent ae = new ActionEvent(o, 0, "");
            myEditLis.actionPerformed(ae);
        }
    }

    /**
     * Given a row object, find its row number.
     * @param o row object
     * @return row index
     */
    protected int findObjectRow(Object o) {
        int row = 0;

        // the most probable case
        if (o == shadow.get(tab.getSelectedRow())) {
            row = tab.getSelectedRow();
        } // else look at all
        else {
            int r;
            for (r = 0; r < shadow.size(); r++) {
                if (o == shadow.get(r)) {
                    row = r;
                }
                break;
            }
            if (r >= mod.getRowCount()) //assert false: "Bad table processing, ViskitTablePanel.updateRow)
            {
                System.err.println("Bad table processing, ViskitTablePanel.updateRow");
            }  // will die here
        }
        return row;
    }

    /**
     * Set table column widths to the widest element, including header.  Let last column float.
     */
    private void adjustColumnWidths() {
        String[] titles = getColumnTitles();
        FontMetrics fm = tab.getFontMetrics(tab.getFont());

        for (int c = 0; c < tab.getColumnCount(); c++) {
            TableColumn col = tab.getColumnModel().getColumn(c);
            int maxWidth = 0;
            int w = fm.stringWidth(titles[c]);
            col.setMinWidth(w);
            if (w > maxWidth) {
                maxWidth = w;
            }
            for (int r = 0; r < tab.getRowCount(); r++) {
                String s = (String) mod.getValueAt(r, c);
                // shouldn't happen, but:
                if (s != null) {
                    w = fm.stringWidth(s);
                    if (w > maxWidth) {
                        maxWidth = w;
                    }
                }
            }
            if (c != tab.getColumnCount() - 1) {    // leave the last one alone
                // its important to set maxwidth before preferred with because the latter
                // gets clamped by the former.
                col.setMaxWidth(maxWidth + 5);       // why the fudge?
                col.setPreferredWidth(maxWidth + 5); // why the fudge?
            }
        }
        tab.invalidate();
    }

    /**
     * Build a table row based on the passed row object.
     * @param o a ViskitElement to add to the table row
     */
    private void putARow(ViskitElement o) {
        shadow.add(o);

        Vector<String> rowData = new Vector<String>();
        String[] fields = getFields(o, shadow.size() - 1);
        rowData.addAll(Arrays.asList(fields));
        mod.addRow(rowData);
    }

    /**
     * Whether this class should add and delete rows on plus-minus clicks.  
     * Else that's left to a listener
     * @param boo How to play it
     */
    protected void doAddsAndDeletes(boolean boo) {
        shouldDoAddsAndDeletes = boo;
    }

    /** The local listener for plus, minus and edit clicks */
    class MyAddDelEditHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (event.getActionCommand().equals("p")) {
                if (myPlusLis != null) {
                    myPlusLis.actionPerformed(event);
                }
                if (shouldDoAddsAndDeletes) {
                    addRow();
                }
            } else if (event.getActionCommand().equals("m")) {
                int reti = JOptionPane.showConfirmDialog(ViskitTablePanel.this, "Are you sure?", "Confirm delete", JOptionPane.YES_NO_OPTION);
                if (reti != JOptionPane.YES_OPTION) {
                    return;
                }

                if (myMinusLis != null) {
                    event.setSource(shadow.get(tab.getSelectedRow()));
                    myMinusLis.actionPerformed(event);
                }
                
                // Begin T/S for Bug 1373.  This process should remove edge 
                // parameters not only from the preceding EdgeInspectorDialog,
                // but also from the EG XML representation
                if (shouldDoAddsAndDeletes) {
                    removeRow(tab.getSelectedRow());
                }
            } else {
                doEdit();
            }
            adjustColumnWidths();
        }
    }

    /**
     * Our table model.  Sub class done only to mark all as read-only.
     */
    class ThisTableModel extends DefaultTableModel {

        ThisTableModel(String[] columnNames) {
            super(columnNames, 0);
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }

    class ThisToolTipTable extends JTable {

        ThisToolTipTable(TableModel tm) {
            super(tm);
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            String tip;
            java.awt.Point p = e.getPoint();
            int rowIndex = rowAtPoint(p);
            int colIndex = columnAtPoint(p);

            tip = getValueAt(rowIndex, colIndex).toString();  // tool tip is contents (for long contents)
            return (tip == null || tip.isEmpty()) ? null : tip;
        }
    }
}