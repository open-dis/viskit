package viskit.view;

import edu.nps.util.LogUtilities;
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
import org.apache.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.model.ViskitElement;
import static viskit.xsd.translator.eventgraph.SimkitEventGraphXML2Java.SP;
import viskit.model.EventGraphModel;

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
public abstract class ViskitTablePanel extends JPanel
{
    static final Logger LOG = LogUtilities.getLogger(ViskitTablePanel.class);

    protected JTable table;
    private JScrollPane jScrollPane;
    private JButton plusButton,  minusButton,  editButton;
    private ThisTableModel thisTableModel;
    private int defaultWidth = 0,  defaultNumberRows = 3;

    // List has no implemented clone method
    private final ArrayList<ViskitElement> shadow = new ArrayList<>();
    private ActionListener myEditListener,  myPlusListener,  myMinusListener;
    private final String  plusToolTip = "Add a row to this table";
    private final String minusToolTip = "Delete the selected row from this table;";
    private boolean plusMinusEnabled = false;
    private boolean shouldDoAddsAndDeletes = true;

    public ViskitTablePanel(int defaultWidth) {
        this.defaultWidth = defaultWidth;
    }

    public ViskitTablePanel(int defaultWidth, int numberRows) {
        this.defaultWidth = defaultWidth;
        this.defaultNumberRows = numberRows;
    }

    public final void init(boolean wantAddDeleteButtons)
	{
        plusMinusEnabled = wantAddDeleteButtons;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // edit instructions line
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createHorizontalGlue());
        JLabel instructions = new JLabel("Double click a row to ");
        int bigFontSize = instructions.getFont().getSize();
        instructions.setFont(getFont().deriveFont(Font.ITALIC, (float) (bigFontSize - 2)));
        panel.add(instructions);
        editButton = new JButton("edit.");
        editButton.setFont(instructions.getFont()); //.deriveFont(Font.ITALIC, (float) (bigSz - 2)));
        editButton.setBorder(null);
        editButton.setEnabled(false);
        editButton.setActionCommand("e");
        panel.add(editButton);
        panel.add(Box.createHorizontalGlue());
        add(panel);

        // the table
        table = new ThisToolTipTable(thisTableModel = new ThisTableModel(getColumnTitles()));
        adjustColumnWidths();
        int rowHeight = table.getRowHeight();
        int defaultHeight = rowHeight * (defaultNumberRows + 1);

        table.setPreferredScrollableViewportSize(new Dimension(defaultWidth, rowHeight * 3));
        table.setMinimumSize(new Dimension(20, rowHeight * 2));
        jScrollPane = new JScrollPane(table);
        jScrollPane.setMinimumSize(new Dimension(defaultWidth, defaultHeight));       // jmb test
        add(jScrollPane);

        ActionListener actionListener = new MyAddDeleteEditHandler();

        if (wantAddDeleteButtons) {// plus, minus and edit buttons
            add(Box.createVerticalStrut(5));
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            buttonPanel.add(Box.createHorizontalGlue());
            // add button
            plusButton = new JButton(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/plus.png")));
            plusButton.setBorder(null);
            plusButton.setText(null);
            plusButton.setToolTipText(getPlusToolTip());
            Dimension preferredButtonSize = plusButton.getPreferredSize();
            plusButton.setMinimumSize(preferredButtonSize);
            plusButton.setMaximumSize(preferredButtonSize);
            plusButton.setActionCommand("p");
            buttonPanel.add(plusButton);
            // delete button
            minusButton = new JButton(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/minus.png")));
            minusButton.setDisabledIcon(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/minusGrey.png")));
            minusButton.setBorder(null);
            minusButton.setText(null);
            minusButton.setToolTipText(getMinusToolTip());
            preferredButtonSize = minusButton.getPreferredSize();
            minusButton.setMinimumSize(preferredButtonSize);
            minusButton.setMaximumSize(preferredButtonSize);
            minusButton.setActionCommand("m");
            minusButton.setEnabled(false);
            buttonPanel.add(minusButton);
            buttonPanel.add(Box.createHorizontalGlue());
            add(buttonPanel);

            // install local add, delete handlers
             plusButton.addActionListener(actionListener);
            minusButton.addActionListener(actionListener);
        }
        // don't let the whole panel get squeezed smaller that what we start out with
        Dimension d = getPreferredSize();
        setMinimumSize(d);

        // install local edit handler
        editButton.addActionListener(actionListener);

        // install the handler to enable delete and edit buttons only on row-select
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent event)
			{
                if (!event.getValueIsAdjusting())
				{
                    boolean yesNo = table.getSelectedRowCount() > 0;
                    if (plusMinusEnabled)
					{
                        minusButton.setEnabled(yesNo);
                    }
                    editButton.setEnabled(yesNo);
                }
            }
        });

        // install the double-clicked handler to duplicate action of edit button
        table.addMouseListener(new MouseAdapter() 
		{
            @Override
            public void mouseClicked(MouseEvent event) 
			{
                if (event.getClickCount() == 2) 
				{
                    doEdit();
                }
            }
        });
    }

    // public methods for working with this class
    /**
     * Install external handler for row edit requests.  The row object can be retrieved by
     * ActionEvent.getSource().
     * @param editListener
     */
    public void addDoubleClickedListener(ActionListener editListener) 
	{
        myEditListener = editListener;
    }

    /**
     * Install external handler for row-add requests.
     * @param addListener
     */
    public void addPlusListener(ActionListener addListener) 
	{
        myPlusListener = addListener;
    }

    /**
     * Install external handler for row-delete requests.  The row object can be retrieved by
     * ActionEvent.getSource().
     * @param deleteListener
     */
    public void addMinusListener(ActionListener deleteListener) 
	{
        myMinusListener = deleteListener;
    }

    /**
     * Add a row defined by the argument to the end of the table. 
     * The table data is retrieved via abstract method getFields(o).
     * @param viskitElement the argument for this row
     */
    public void addRow(ViskitElement viskitElement)
	{
        shadow.add(viskitElement);

        Vector<String> rowData = new Vector<>();
        String[] fields = getFields(viskitElement, 0);
        rowData.addAll(Arrays.asList(fields));
        thisTableModel.addRow(rowData);

        adjustColumnWidths();

        // This doesn't work perfectly on the Mac
        JScrollBar verticalScrollBar = jScrollPane.getVerticalScrollBar();
        verticalScrollBar.setValue(verticalScrollBar.getMaximum());
    }

    /**
     * Add a row to the end of the table.  The row object will be built through
     * the abstract method, newRowObject().
     */
    public void addRow()
	{
        addRow(newRowObject());
    }

    /**
     * Remove the row representing the argument from the table.
     * @param viskitElement the element row to remove
     */
    public void removeRow(ViskitElement viskitElement) 
	{
        removeRow(findObjectRow(viskitElement));
    }

    /**
     * Remove the row identified by the passed zero-based row number from the table.
     * @param rowNumber index of the object to remove
     */
    public void removeRow(int rowNumber) 
	{
        ViskitElement e = shadow.remove(rowNumber);
        thisTableModel.removeRow(rowNumber);
    }

    /**
     * Initialize the table with the passed data.
     * @param data
     */
    public void setData(List<? extends ViskitElement> data) 
	{
        shadow.clear();
        thisTableModel.setRowCount(0);

        if (data != null) 
		{
            for (ViskitElement o : data) 
			{
                putARow(o);
            }
        }
        adjustColumnWidths();
    }

    /**
     * Get all the current table data in the form of an array of row objects.
     * @return ArrayList copy of row objects
     */
	@SuppressWarnings("unchecked") // necessary because clone() returns an object
    public ArrayList<ViskitElement> getData() {
        return (ArrayList<ViskitElement>) shadow.clone();
    }

    public boolean isEmpty() {
        return thisTableModel.getRowCount() == 0;
    }

    /**
     * Update the table row, typically after editing, representing the passed rowObject.
     * @param rowObject the element to update a table row with
     */
    public void updateRow(ViskitElement rowObject) {
        int row = findObjectRow(rowObject);

        String[] fields = getFields(rowObject, 0);
        for (int i = 0; i < thisTableModel.getColumnCount(); i++) {
            thisTableModel.setValueAt(fields[i], row, i);
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
     * @param e a row element
     * @param rowNum row number...not used unless EdgeParametersPanel //todo fix
     * @return  String array of fields
     */
    abstract public String[] getFields(ViskitElement e, int rowNum);

    /**
     * Build a new row object
     * @return a new row object
     */
    abstract public ViskitElement newRowObject();

    /**
     * Specify how many rows the table should display at a minimum
     * @return number of rows
     */
    abstract public int getNumberVisibleRows();

    // private methods
    /**
     * If a double-clicked listener has been installed, message it with the row
     * object to be edited.
     */
    private void doEdit() 
	{
        if (myEditListener != null) {
            Object o = shadow.get(table.getSelectedRow());
            ActionEvent ae = new ActionEvent(o, 0, "");
            myEditListener.actionPerformed(ae);
        }
    }

    /**
     * Given a row object, find its row number.
     * @param o row object
     * @return row index
     */
    protected int findObjectRow(Object o) 
	{
        int row = 0;

        // the most probable case
        if (o == shadow.get(table.getSelectedRow())) {
            row = table.getSelectedRow();
        } // else look at all
        else {
            int r;
            for (r = 0; r < shadow.size(); r++) {
                if (o == shadow.get(r)) {
                    row = r;
                }
                break;
            }
            if (r >= thisTableModel.getRowCount()) //assert false: "Bad table processing, ViskitTablePanel.updateRow)
            {
                LOG.error("Bad table processing, ViskitTablePanel.updateRow");
            }  // will die here
        }
        return row;
    }

    /**
     * Set table column widths to the widest element, including header.  Let last column float.
     */
    private void adjustColumnWidths() {
        String[] titles = getColumnTitles();
        FontMetrics fm = table.getFontMetrics(table.getFont());

        for (int c = 0; c < table.getColumnCount(); c++) {
            TableColumn col = table.getColumnModel().getColumn(c);
            int maxWidth = 0;
            int w = fm.stringWidth(titles[c]);
            col.setMinWidth(w);
            if (w > maxWidth) {
                maxWidth = w;
            }
            for (int r = 0; r < table.getRowCount(); r++) {
                String s = (String) thisTableModel.getValueAt(r, c);
                // shouldn't happen, but:
                if (s != null) {
                    w = fm.stringWidth(s);
                    if (w > maxWidth) {
                        maxWidth = w;
                    }
                }
            }
            if (c != table.getColumnCount() - 1) {    // leave the last one alone
                // its important to set maxwidth before preferred width because the latter
                // gets clamped by the former.
                col.setMaxWidth(maxWidth + 5);       // why the fudge?
                col.setPreferredWidth(maxWidth + 5); // why the fudge?
            }
        }
        table.invalidate();
    }

    /**
     * Build a table row based on the passed row object.
     * @param e a ViskitElement to add to the table row
     */
    private void putARow(ViskitElement e) {
        shadow.add(e);

        Vector<String> rowData = new Vector<>();
        String[] fields = getFields(e, shadow.size() - 1);
        rowData.addAll(Arrays.asList(fields));
        thisTableModel.addRow(rowData);
    }

    /**
     * Whether this class should add and delete rows on plus-minus clicks.
     * Else that's left to a listener
     * @param value How to play it
     */
    protected void doAddsAndDeletes(boolean value) 
	{
        shouldDoAddsAndDeletes = value;
    }

    /** The local listener for plus, minus and edit clicks */
    class MyAddDeleteEditHandler implements ActionListener 
	{
        @Override
        public void actionPerformed(ActionEvent event) 
		{
            switch (event.getActionCommand()) 
			{
                case "p": // plus
                    if (myPlusListener != null) 
					{
                        myPlusListener.actionPerformed(event);
                    }
                    if (shouldDoAddsAndDeletes) 
					{
                        addRow();
                    }
                    break;
					
                case "m": // minus
                    int returnValue = JOptionPane.showConfirmDialog(ViskitTablePanel.this, "Are you sure?", "Confirm delete", JOptionPane.YES_NO_OPTION);
                    if (returnValue != JOptionPane.YES_OPTION)
					{
                        return;
                    }
                    if (myMinusListener != null) 
					{
                        event.setSource(shadow.get(table.getSelectedRow()));
                        myMinusListener.actionPerformed(event);
                    }
                    // Begin T/S for Bug 1373.  This process should remove edge
                    // parameters not only from the preceding EdgeInspectorDialog,
                    // but also from the EG XML representation
//                    if (shouldDoAddsAndDeletes) 
//					{
//                        removeRow(table.getSelectedRow()); // apparently redundant, listener removed state variable
//                    }
                    break;
					
                default:
                    doEdit();
                    break;
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
            String tip = null;
            java.awt.Point p = e.getPoint();
            int rowIndex = rowAtPoint(p);
            int colIndex = columnAtPoint(p);

            Object o = getValueAt(rowIndex, colIndex); // tool tip is contents (for long contents)
            if (o != null)
                tip = (String) o;

            return (tip == null || tip.isEmpty()) ? null : tip;
        }
    }
}
