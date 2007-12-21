/*
Copyright (c) 1995-2005 held by the author(s).  All rights reserved.

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
      (http://www.nps.edu and http://www.MovesInstitute.org)
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

/**
 * Autonomous Underwater Vehicle Workbench
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jul 20, 2005
 * Time: 4:11:55 PM
 */

package viskit.doe;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

public class ParamTable extends JTable
{
  private ParamTableModel ptm;

  ParamTable(List simEntities, List designParms)
  {
    if (simEntities != null)
      setParams(simEntities, designParms);
    else
      setModel(new DefaultTableModel(new Object[][]{{}}, ParamTableModel.columnNames));
    setShowGrid(true);
    setDefaultRenderer(String.class, new myStringClassRenderer());
    setDefaultRenderer(Boolean.class, new myBooleanClassRenderer());
  }

  private void setParams(List simEntities, List designParams)
  {
    ptm = new ParamTableModel(simEntities, designParams);
    setModel(ptm);
    setColumnWidths();
  }
  
  private int[] colWidths= {175,160,125,75,90,90};
  private void setColumnWidths()
  {
    TableColumn column = null;
    for (int i = 0; i < ParamTableModel.NUM_COLS; i++) {
      column = getColumnModel().getColumn(i);
      column.setPreferredWidth(colWidths[i]);
    }
  }
  Color defaultC;
  Color noedit = Color.lightGray;
  Color multiP = new Color(230, 230, 230);

  class myStringClassRenderer extends JLabel implements TableCellRenderer
  {
    public myStringClassRenderer()
    {
      setOpaque(true);
      setFont(ParamTable.this.getFont());
      defaultC = ParamTable.this.getBackground();
      setBorder(new EmptyBorder(0,3,0,0));       // keeps left from being cutoff
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
      Integer rowKey = new Integer(row);
      if (ptm.noEditRows.contains(rowKey) || column == ParamTableModel.TYPE_COL)
        setBackground(noedit);
      else if (ptm.multiRows.contains(rowKey)) {
        if (column == ParamTableModel.NAME_COL)
          setBackground(multiP);
        else
          setBackground(noedit);
      }
      else if (column == ParamTableModel.VALUE_COL || column == ParamTableModel.MIN_COL ||
          column == ParamTableModel.MAX_COL)
        handleFactorSwitchable(row, column);
      else
        setBackground(defaultC);

      setText(value.toString());
      setToolTipText(getTT(column));
      return this;

    }
    private String getTT(int col)
    {
      switch(col) {
        case ParamTableModel.NAME_COL:
          return "Name of variable.  Each variable used in the experiment must have a name.";
        case ParamTableModel.TYPE_COL:
          return "Variable type.";
        case ParamTableModel.VALUE_COL:
          return "Value of variable.  Not used if variable is used in experiment.";
        case ParamTableModel.FACTOR_COL:
          return "Whether this variable is used as an indepedent variable in the experiment.";
        case ParamTableModel.MIN_COL:
          return "Beginning value of independent variable.";
        case ParamTableModel.MAX_COL:
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
    private void handleFactorSwitchable(int row, int col)
    {
      boolean factor = ((Boolean) getValueAt(row, ParamTableModel.FACTOR_COL)).booleanValue();
      if (factor) {
        switch (col) {
          case ParamTableModel.VALUE_COL:
            setBackground(noedit);
            break;
          default:
            setBackground(defaultC);
            break;
        }
      }
      else {
        switch (col) {
          case ParamTableModel.MIN_COL:
          case ParamTableModel.MAX_COL:
            setBackground(noedit);
            break;
          default:
            setBackground(defaultC);
            break;
        }
      }
    }
  }

  /**
   * Booleans are rendered with checkboxes.  That's what we want.  We have our own Renderer
   * so we can control the background colors of the cells.
   */
  class myBooleanClassRenderer extends JCheckBox implements TableCellRenderer
  {
    public myBooleanClassRenderer()
    {
      super();
      setHorizontalAlignment(JLabel.CENTER);
      setBorderPainted(false);

      // This so we can change background of cells depending on the state of the checkboxes.
      addItemListener(new ItemListener()
      {
        public void itemStateChanged(ItemEvent e)
        {
          ListSelectionModel lsm = ParamTable.this.getSelectionModel();
          if (!lsm.isSelectionEmpty()) {
            int selectedRow = lsm.getMinSelectionIndex();
            ((DefaultTableModel)ParamTable.this.getModel()).fireTableRowsUpdated(selectedRow,selectedRow);
          }
        }
      });
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
      Integer rowKey = new Integer(row);
      setEnabled(true);
      if (ptm.noEditRows.contains(rowKey) || column == ParamTableModel.TYPE_COL) {
        setBackground(noedit);
        setEnabled(false);
      }
      else if (ptm.multiRows.contains(rowKey)) {
        setEnabled(false);
        if (column == ParamTableModel.NAME_COL)
          setBackground(multiP);
        else
          setBackground(noedit);
      }
      else
        setBackground(defaultC);

      setSelected((value != null && ((Boolean) value).booleanValue()));
      setToolTipText("Whether this variable is used as an indepedent variable in the experiment.");

      return this;
    }
  }
}