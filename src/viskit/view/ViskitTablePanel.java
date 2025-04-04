package viskit.view;

import edu.nps.util.Log4jUtilities;
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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.doe.DoeMain;
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
public abstract class ViskitTablePanel extends JPanel
{
    static final Logger LOG = LogManager.getLogger();

    protected JTable table;
    private JScrollPane scrollPane;
    private JButton plusButton,  minusButton,  editButton;
    private ThisTableModel thisTableModel;
    private int defaultWidth = 0,  defaultNumberRows = 3;

    // List has no implemented clone method
    private final ArrayList<ViskitElement> shadow = new ArrayList<>();
    private ActionListener myEditListener,  myPlusButtonListener,  myMinusButtonListener;
    private final String plusButtonToolTip = "Add a row to this table";
    private final String minusButtonToolTip = "Delete the selected row from this table;";
    private boolean plusMinusButtonsEnabled = false;
    private boolean enableAddsAndDeletes = true;

    public ViskitTablePanel(int defaultWidth) {
        this.defaultWidth = defaultWidth;
    }

    public ViskitTablePanel(int defaultWidth, int defaultNumbeRows) {
        this.defaultWidth      = defaultWidth;
        this.defaultNumberRows = defaultNumbeRows;
    }

    protected final void initialize(boolean includeAddDeleteButtons)
    {
        plusMinusButtonsEnabled = includeAddDeleteButtons;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // edit instructions line
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createHorizontalGlue());
        JLabel instructions = new JLabel("Double click a row to ");
        int bigSize = instructions.getFont().getSize();
        instructions.setFont(getFont().deriveFont(Font.ITALIC, (bigSize - 2)));
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
        scrollPane = new JScrollPane(table);
        scrollPane.setMinimumSize(new Dimension(defaultWidth, defaultHeight));       // jmb test
        add(scrollPane);

        ActionListener lis = new MyAddDeleteEditHandler();

        if (includeAddDeleteButtons) {// plus, minus and edit buttons
            add(Box.createVerticalStrut(5));
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            buttonPanel.add(Box.createHorizontalGlue());
            // add button
            plusButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/plus.png")));
            plusButton.setBorder(null);
            plusButton.setText(null);
            plusButton.setToolTipText(getPlusToolTip());
            Dimension dd = plusButton.getPreferredSize();
            plusButton.setMinimumSize(dd);
            plusButton.setMaximumSize(dd);
            plusButton.setActionCommand("p");
            buttonPanel.add(plusButton);
            // delete button
            minusButton =   new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/minus.png")));
            minusButton.setDisabledIcon(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/minusGrey.png")));
            minusButton.setBorder(null);
            minusButton.setText(null);
            minusButton.setToolTipText(getMinusToolTip());
            dd = minusButton.getPreferredSize();
            minusButton.setMinimumSize(dd);
            minusButton.setMaximumSize(dd);
            minusButton.setActionCommand("m");
            minusButton.setEnabled(false);
            buttonPanel.add(minusButton);
            buttonPanel.add(Box.createHorizontalGlue());
            add(buttonPanel);

            // install local add, delete handlers
            plusButton.addActionListener(lis);
            minusButton.addActionListener(lis);
        }
        // don't let the whole panel get squeezed smaller that what we start out with
        Dimension d = getPreferredSize();
        setMinimumSize(d);

        // install local edit handler
        editButton.addActionListener(lis);

        // install the handler to enable delete and edit buttons only on row-select
        table.getSelectionModel().addListSelectionListener((ListSelectionEvent listSelectionEvent) -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {
                boolean hasSelectedRows = table.getSelectedRowCount() > 0;
                if (plusMinusButtonsEnabled) {
                    minusButton.setEnabled(hasSelectedRows);
                }
                editButton.setEnabled(hasSelectedRows);
            }
        });

        // install the double-clicked handler to duplicate action of edit button
        table.addMouseListener(new MouseAdapter() {

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
     * @param newEditListener
     */
    public void addDoubleClickedListener(ActionListener newEditListener) {
        myEditListener = newEditListener;
    }

    /**
     * Install external handler for row-add requests.
     * @param addLis
     */
    public void addPlusListener(ActionListener addLis) {
        myPlusButtonListener = addLis;
    }

    /**
     * Install external handler for row-delete requests.  The row object can be retrieved by
     * ActionEvent.getSource().
     * @param delLis
     */
    public void addMinusListener(ActionListener delLis) {
        myMinusButtonListener = delLis;
    }

    /**
     * Add a row defined by the argument to the end of the table.  The table data
     * will be retrieved through the abstract method, getFields(o).
     * @param e the argument for this row
     */
    public void addRow(ViskitElement e) {
        shadow.add(e);

        Vector<String> rowData = new Vector<>();
        String[] fields = getFields(e, 0);
        rowData.addAll(Arrays.asList(fields));
        thisTableModel.addRow(rowData);

        adjustColumnWidths();

        // This doesn't work perfectly on the Mac
        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
        scrollBar.setValue(scrollBar.getMaximum());
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
     * @param targetViskitElement the element row to remove
     */
    public void removeRow(ViskitElement targetViskitElement) {
        removeRow(findObjectRow(targetViskitElement));
    }

    /**
     * Remove the row identified by the passed zero-based row number from the
     * table.
     * @param rowIndex index of the object to remove
     */
    public void removeRow(int rowIndex) {
        ViskitElement selectedRowViskitElement = shadow.remove(rowIndex);
        thisTableModel.removeRow(rowIndex);
    }

    /**
     * Initialize the table with the passed data.
     * @param data
     */
    public void setData(List<? extends ViskitElement> data) {
        shadow.clear();
        thisTableModel.setRowCount(0); // reset

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
        return thisTableModel.getRowCount() == 0;
    }

    /**
     * Update the table row, typically after editing, representing the passed rowObject.
     * @param rowObject the element to update a table row with
     */
    public void updateRow(ViskitElement rowObject) {
        int row = findObjectRow(rowObject);

        String[] rowFieldsStringArray = getFields(rowObject, 0);
        for (int i = 0; i < thisTableModel.getColumnCount(); i++) {
            thisTableModel.setValueAt(rowFieldsStringArray[i], row, i);
        }
        adjustColumnWidths();
    }

    // Protected methods
    protected String getPlusToolTip() {
        return plusButtonToolTip;
    }

    protected String getMinusToolTip() {
        return minusButtonToolTip;
    }

    // Abstract methods
    /**
     * Return the column titles.  This defines the number of columns in the display.
     * @return String array of titles
     */
    abstract public String[] getColumnTitles();

    /**
     * Return the fields to be displayed in the table.
     * @param viskitElement a row element
     * @param rowNumber row number...not used unless EdgeParametersPanel //todo fix
     * @return  String array of fields
     */
    abstract public String[] getFields(ViskitElement viskitElement, int rowNumber);

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
            Object selectedRowObject = shadow.get(table.getSelectedRow());
            ActionEvent actionEvent = new ActionEvent(selectedRowObject, 0, "");
            myEditListener.actionPerformed(actionEvent);
        }
    }

    /**
     * Given a row object, find its row number.
     * @param rowObjectOfInterest row object
     * @return row index
     */
    protected int findObjectRow(Object rowObjectOfInterest) {
        int rowIndex = 0;

        // the most probable case
        if (rowObjectOfInterest == shadow.get(table.getSelectedRow())) {
            rowIndex = table.getSelectedRow();
        } // else look at all
        else {
            int rowOfInterest;
            for (rowOfInterest = 0; rowOfInterest < shadow.size(); rowOfInterest++) {
                if (rowObjectOfInterest == shadow.get(rowOfInterest)) {
                    rowIndex = rowOfInterest;
                }
                break;
            }
            // safety check
            if (rowOfInterest >= thisTableModel.getRowCount()) //assert false: "Bad table processing, ViskitTablePanel.updateRow)
            {
                LOG.error("Bad table processing, ViskitTablePanel.updateRow");
            }  // will die here
        }
        return rowIndex;
    }

    /**
     * Set table column widths to the widest element, including header.  Let last column float.
     */
    private void adjustColumnWidths()
    {
        String[] titles = getColumnTitles();
        FontMetrics fontMetrics = table.getFontMetrics(table.getFont());

        for (int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {
            TableColumn tableColumn = table.getColumnModel().getColumn(columnIndex);
            int maxWidth = 0;
            int titleStringWidth = fontMetrics.stringWidth(titles[columnIndex]);
            tableColumn.setMinWidth(titleStringWidth);
            if (titleStringWidth > maxWidth) {
                maxWidth = titleStringWidth;
            }
            for (int rowIndex = 0; rowIndex < table.getRowCount(); rowIndex++) {
                String cellStringValue = (String) thisTableModel.getValueAt(rowIndex, columnIndex);
                // shouldn't happen, but:
                if (cellStringValue != null) {
                    titleStringWidth = fontMetrics.stringWidth(cellStringValue);
                    if (titleStringWidth > maxWidth) {
                        maxWidth = titleStringWidth;
                    }
                }
            }
            if (columnIndex != table.getColumnCount() - 1) {    // leave the last one alone
                // its important to set maxwidth before preferred width because the latter
                // gets clamped by the former.
                tableColumn.setMaxWidth(maxWidth + 5);       // why the fudge?
                tableColumn.setPreferredWidth(maxWidth + 5); // why the fudge?
            }
        }
        table.invalidate();
    }

    /**
     * Build a table row based on the passed row object.
     * @param newViskitElement a ViskitElement to add to the table row
     */
    private void putARow(ViskitElement newViskitElement) {
        shadow.add(newViskitElement);

        Vector<String> rowData = new Vector<>();
        String[] fields = getFields(newViskitElement, shadow.size() - 1);
        rowData.addAll(Arrays.asList(fields));
        thisTableModel.addRow(rowData);
    }

    /**
     * Whether this class should add and delete rows on plus-minus clicks.
     * Else that's left to a listener
     * @param newValue How to play it
     */
    protected void setEnableAddsAndDeletes(boolean newValue) {
        enableAddsAndDeletes = newValue;
    }

    /** The local listener for plus, minus and edit clicks */
    class MyAddDeleteEditHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            switch (actionEvent.getActionCommand()) {
                case "p":
                    if (myPlusButtonListener != null) {
                        myPlusButtonListener.actionPerformed(actionEvent);
                    }
                    if (enableAddsAndDeletes) {
                        addRow();
                    }
                    break;
                case "m":
                    int returnValue = JOptionPane.showConfirmDialog(ViskitTablePanel.this, "Are you sure?", "Confirm delete", JOptionPane.YES_NO_OPTION);
                    if (returnValue != JOptionPane.YES_OPTION) {
                        return;
                    }
                    if (myMinusButtonListener != null) {
                        actionEvent.setSource(shadow.get(table.getSelectedRow()));
                        myMinusButtonListener.actionPerformed(actionEvent);
                    }

                    // Begin T/S for Bug 1373.  This process should remove edge
                    // parameters not only from the preceding EdgeInspectorDialog,
                    // but also from the EG XML representation
                    if (enableAddsAndDeletes) {
                        removeRow(table.getSelectedRow());
                    }
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
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    class ThisToolTipTable extends JTable {

        ThisToolTipTable(TableModel tableModel) {
            super(tableModel);
        }

        @Override
        public String getToolTipText(MouseEvent mouseEvent) {
            String toolTipText = null;
            java.awt.Point p = mouseEvent.getPoint();
            int rowIndex = rowAtPoint(p);
            int colIndex = columnAtPoint(p);

            Object cellObject = getValueAt(rowIndex, colIndex); // tool tip is contents (for long contents)
            if (cellObject != null)
                toolTipText = (String) cellObject;

            return (toolTipText == null || toolTipText.isEmpty()) ? null : toolTipText;
        }
    }
}
