/*
Copyright (c) 1995-2016 held by the author(s).  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other materials provided with the
      distribution.
    * Neither the names of the Naval Postgraduate School (NPS)
      Modeling Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and http://www.movesinstitute.org)
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/
package viskit.doe;

import edu.nps.util.LogUtilities;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.apache.log4j.Logger;

import viskit.xsd.bindings.assembly.SimEntity;
import viskit.xsd.bindings.assembly.TerminalParameter;

/**
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jul 20, 2005
 * Time: 4:11:55 PM
 */
public class ParameterTable extends JTable 
{
    static final Logger LOG = LogUtilities.getLogger(ParameterTable.class);

    private ParameterTableModel ptm;

    ParameterTable(List<SimEntity> jaxbSimEntities, List<TerminalParameter> designParms) {
        if (jaxbSimEntities != null) {
            setParameters(jaxbSimEntities, designParms);
        } else {
            setModel(new DefaultTableModel(new Object[][]{{}}, ParameterTableModel.columnNames));
        }
        setShowGrid(true);
        setDefaultRenderer(String.class, new myStringClassRenderer());
        setDefaultRenderer(Boolean.class, new myBooleanClassRenderer());
    }

    /**
     *
     * @param jaxbSimEntities
     * @param designParams
     */
    private void setParameters(List<SimEntity> jaxbSimEntities, List<TerminalParameter> designParams) {
        ptm = new ParameterTableModel(jaxbSimEntities, designParams);
        setModel(ptm);
        setColumnWidths();
    }
    private int[] colWidths = {175, 160, 125, 75, 90, 90};

    private void setColumnWidths() {
        TableColumn column;
        for (int i = 0; i < ParameterTableModel.NUM_COLS; i++) {
            column = getColumnModel().getColumn(i);
            column.setPreferredWidth(colWidths[i]);
        }
    }
    Color defaultColor;
    Color noeditColor       = Color.lightGray;
    Color multipleRowsColor = new Color(230, 230, 230);

    class myStringClassRenderer extends JLabel implements TableCellRenderer 
	{
        public myStringClassRenderer() {
            setOpaque(true);
            setFont(ParameterTable.this.getFont());
            defaultColor = ParameterTable.this.getBackground();
            setBorder(new EmptyBorder(0, 3, 0, 0));       // keeps left from being cutoff
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
            Integer rowKey = row;
            if (ptm.noEditRows.contains(rowKey) || column == ParameterTableModel.TYPE_COLUMN) {
                setBackground(noeditColor);
            } else if (ptm.multiRows.contains(rowKey)) {
                if (column == ParameterTableModel.NAME_COLUMN) {
                    setBackground(multipleRowsColor);
                } else {
                    setBackground(noeditColor);
                }
            } else if (column == ParameterTableModel.VALUE_COLUMN || column == ParameterTableModel.MIN_COLUMN ||
                    column == ParameterTableModel.MAX_COLUMN) {
                handleFactorSwitchable(row, column);
            } else {
                setBackground(defaultColor);
            }

            setText(value.toString());
            setToolTipText(getToolTip(column));
            return this;

        }

        private String getToolTip(int col) 
		{
            switch (col) {
                case ParameterTableModel.NAME_COLUMN:
                    return "Name of variable.  Each variable used in the experiment must have a name.";
                case ParameterTableModel.TYPE_COLUMN:
                    return "Variable type.";
                case ParameterTableModel.VALUE_COLUMN:
                    return "Value of variable.  Not used if variable is used in experiment.";
                case ParameterTableModel.FACTOR_COLUMN:
                    return "Whether this variable is used as an indepedent variable in the experiment.";
                case ParameterTableModel.MIN_COLUMN:
                    return "Beginning value of independent variable.";
                case ParameterTableModel.MAX_COLUMN:
                    return "Final value of independent variable.";
                default:
                    return "";
            }
        }

        /**
         * Twiddle the background of the value, min and max cells based on the state of the
         * check box.
         * @param row
         * @param col
         */
        private void handleFactorSwitchable(int row, int col) {
            boolean factor = ((Boolean) getValueAt(row, ParameterTableModel.FACTOR_COLUMN));
            if (factor) {
                switch (col) {
                    case ParameterTableModel.VALUE_COLUMN:
                        setBackground(noeditColor);
                        break;
                    default:
                        setBackground(defaultColor);
                        break;
                }
            } else {
                switch (col) {
                    case ParameterTableModel.MIN_COLUMN:
                    case ParameterTableModel.MAX_COLUMN:
                        setBackground(noeditColor);
                        break;
                    default:
                        setBackground(defaultColor);
                        break;
                }
            }
        }
    }

    /**
     * Booleans are rendered with checkboxes.  That's what we want.  We have our own Renderer
     * so we can control the background colors of the cells.
     */
    class myBooleanClassRenderer extends JCheckBox implements TableCellRenderer {

        public myBooleanClassRenderer() {
            super();
            setHorizontalAlignment(JLabel.CENTER);
            setBorderPainted(false);

            // This so we can change background of cells depending on the state of the checkboxes.
            addItemListener(new ItemListener() {

                @Override
                public void itemStateChanged(ItemEvent e) {
                    ListSelectionModel lsm = ParameterTable.this.getSelectionModel();
                    if (!lsm.isSelectionEmpty()) {
                        int selectedRow = lsm.getMinSelectionIndex();
                        ((DefaultTableModel) ParameterTable.this.getModel()).fireTableRowsUpdated(selectedRow, selectedRow);
                    }
                }
            });
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Integer rowKey = row;
            setEnabled(true);
            if (ptm.noEditRows.contains(rowKey) || column == ParameterTableModel.TYPE_COLUMN) {
                setBackground(noeditColor);
                setEnabled(false);
            } else if (ptm.multiRows.contains(rowKey)) {
                setEnabled(false);
                if (column == ParameterTableModel.NAME_COLUMN) {
                    setBackground(multipleRowsColor);
                } else {
                    setBackground(noeditColor);
                }
            } else {
                setBackground(defaultColor);
            }

            setSelected((value != null && ((Boolean) value)));
            setToolTipText("Whether this variable is used as an indepedent variable in the experiment.");

            return this;
        }
    }
}
