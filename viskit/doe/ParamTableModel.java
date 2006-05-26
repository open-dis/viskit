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
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jul 20, 2005
 * Time: 4:13:25 PM
 */

package viskit.doe;

import bsh.Interpreter;
import bsh.NameSpace;
import viskit.OpenAssembly;
import viskit.xsd.bindings.assembly.*;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.*;

public class ParamTableModel extends DefaultTableModel implements TableModelListener
{
  public static final int NUM_COLS = 6;

  public static String[] columnNames = {
      "SimEntity/Parameter name",
      "Type",
      "Value",
      "Is factor?",
      "Min",
      "Max"
  };
  public static final int NAME_COL = 0;
  public static final int TYPE_COL = 1;
  public static final int VALUE_COL = 2;
  public static final int FACTOR_COL = 3;
  public static final int MIN_COL = 4;
  public static final int MAX_COL = 5;

  Object[][] mydata = new Object[0][0];
  Vector rows;
  public HashSet noEditRows = new HashSet();
  public HashSet multiRows = new HashSet();
  public boolean dirty = false;


  public ParamTableModel(List simEntitiesJaxb, List designParamsJaxb)
  {
    super(0, 0);

    initBeanShell();
    rows = new Vector();

    int i=0;
    for (Iterator itr = simEntitiesJaxb.iterator(); itr.hasNext();) {
      Object o = itr.next();
      if(!(o instanceof SimEntity))
        System.err.println("Error ParamTableModel(), element not SimEntity");
      processRow(o,"SimEntity_"+i++);
    }
    mydata = (Object[][]) rows.toArray(mydata);
    if(designParamsJaxb != null) {
      for (Iterator itr = designParamsJaxb.iterator(); itr.hasNext();) {
        Object o = itr.next();
        if(!(o instanceof TerminalParameter))
          System.err.println("Error ParamTableModel(), element not TerminalParameter");
        processDesignParam(o);
      }
    }
    dirty=false;
    this.addTableModelListener(this);
  }

  public void tableChanged(TableModelEvent e)
  {
    if (viskit.Vstatics.debug) System.out.println("Sending paramlocally editted from ParamTableModel");
    OpenAssembly.inst().doParamLocallyEditted(dummyListener);
  }

  private void processDesignParam(Object o)
  {
    TerminalParameter tp = (TerminalParameter)o;
    String typ = tp.getType();
    typ = (typ==null?"":typ);

    String nm = tp.getName();
    if(nm.length()<=0)
      System.err.println("Terminal param w/out name ref!");
    
    int row = ((Integer)termHashMap.get(nm)).intValue();

    String val = tp.getValue();
    val = (val==null?"":val);
    setValueAt(val,row,VALUE_COL);

    ValueRangeType vr = (ValueRangeType)tp.getValueRange();

    setValueAt(vr.getLowValue(),row,MIN_COL);
    setValueAt(vr.getHighValue(),row,MAX_COL);

    setValueAt(new Boolean(true),row,FACTOR_COL); //cb
  }

  Interpreter interpreter;
  private void initBeanShell()
  {
    interpreter = new Interpreter();
    interpreter.setStrictJava(true);       // no loose typeing
    NameSpace ns = interpreter.getNameSpace();
    ns.importPackage("simkit.*");
    ns.importPackage("simkit.examples.*");
    ns.importPackage("simkit.random.*");
    ns.importPackage("simkit.smdx.*");
    ns.importPackage("simkit.stat.*");
    ns.importPackage("simkit.util.*");
    ns.importPackage("diskit.*");         // 17 Nov 2004
  }

  HashMap termHashMap = new HashMap();
  ArrayList elementsByRow = new ArrayList();

  private void processRow(Object obj, String defaultName)
  {
    Object[] oa = new Object[6];

    if (obj instanceof SimEntity) {
      SimEntity se = (SimEntity)obj;
      String SEname = se.getName();
      if(SEname == null || SEname.length()<=0)
        SEname = defaultName;

      oa[NAME_COL] = "<html><b>"+SEname;

      String typ = se.getType();
      typ = (typ == null ? "" : typ);
      oa[TYPE_COL] = "<html><b>"+loseDots(typ);
      oa[VALUE_COL] = oa[MIN_COL] = oa[MAX_COL] = "";
      oa[FACTOR_COL] = new Boolean(false);
      rows.add(oa);
      elementsByRow.add(obj);

      noEditRows.add(new Integer(rows.size()-1));

      List children = se.getParameters();
      int i=1;
      for (Iterator itr = children.iterator(); itr.hasNext();) {
        processRow(itr.next(), SEname+"_"+i++);
      }
    }
    else if (obj instanceof TerminalParameter) {
      TerminalParameter tp = (TerminalParameter)obj;
      Object nameRefObj = tp.getLinkRef();
      String tpname = defaultName;
      if(nameRefObj != null) {
        if(nameRefObj instanceof String)
          tpname = (String)nameRefObj;
        else if(nameRefObj instanceof TerminalParameter)
          tpname = ((TerminalParameter)nameRefObj).getName();
      }
      else if(tp.getName() != null && tp.getName().length() >0) {
        tpname = tp.getName();
      }
      if(tpname == null)
        tpname = defaultName;

      oa[NAME_COL] = tpname;
      String typ = tp.getType();
      typ = (typ == null ? "" : typ);
      oa[TYPE_COL] = loseDots(typ);
      String value = tp.getValue();
      oa[VALUE_COL] = (value == null ? "" : value);
      oa[MIN_COL] = oa[MAX_COL] = ""; // will be editted or filled in from existing file
      oa[FACTOR_COL] = new Boolean(false);
      rows.add(oa);
      elementsByRow.add(obj);

      termHashMap.put(tpname,new Integer(rows.size()-1));
    }
    else if(obj instanceof MultiParameter) {
      MultiParameter mp = (MultiParameter)obj;
      String MPname = mp.getName();
      if(MPname == null || MPname.length()<=0)
        MPname = defaultName;
      oa[NAME_COL] = MPname;
      String typ = mp.getType();
      oa[TYPE_COL] = loseDots(typ == null ? "" : typ);
      oa[VALUE_COL] = oa[MIN_COL] = oa[MAX_COL] = "";
      oa[FACTOR_COL] = new Boolean(false);
      rows.add(oa);
      elementsByRow.add(obj);

      multiRows.add(new Integer(rows.size()-1));
      List children = mp.getParameters();
      int i=1;
      for (Iterator itr = children.iterator(); itr.hasNext();) {
        processRow(itr.next(),MPname+"_"+i++);
      }
    }
    else if(obj instanceof FactoryParameter) {
      FactoryParameter fp = (FactoryParameter)obj;
      String FPname = fp.getName();
      if(FPname == null || FPname.length()<=0)
        FPname = defaultName;

      oa[NAME_COL] = FPname;
      String typ = fp.getType();
      oa[TYPE_COL] = typ; //loseDots(typ)
      oa[VALUE_COL] = oa[MIN_COL] = oa[MAX_COL] = "";
      oa[FACTOR_COL] = new Boolean(false);
      rows.add(oa);
      elementsByRow.add(obj);

      multiRows.add(new Integer(rows.size()-1));

      List children = fp.getParameters();
      int i=1;
      for(Iterator itr = children.iterator(); itr.hasNext();) {
        processRow(itr.next(),FPname+"_"+i++);
      }
    }
    else if(obj instanceof Coordinate)
      ;
    else
      System.err.println("Error ParamTableModel.processRow, unknown type: "+obj);

  }

  public Object getElementAtRow(int r)
  {
    return elementsByRow.get(r);
  }

  public Object[] getRowData(int r)
  {
    return mydata[r];
  }

  private String loseDots(String typ)
  {
    int dot = typ.lastIndexOf('.');
    if(dot != -1)
      typ = typ.substring(dot+1);
    return typ;
  }

  public int getColumnCount()
  {
    return columnNames.length;
  }

  public int getRowCount()
  {
    return mydata == null ? 0 : mydata.length;
  }

  public String getColumnName(int col)
  {
    return columnNames[col];
  }

  public Object getValueAt(int row, int col)
  {
    return mydata[row][col];
  }

  public Class getColumnClass(int c)
  {
    //return getValueAt(0, c).getClass();
    switch (c) {
      case NAME_COL:
      case TYPE_COL:
      case VALUE_COL:
      case MIN_COL:
      case MAX_COL:
        return String.class;
      case FACTOR_COL:
        return Boolean.class;
      default:
        //assert false:"Column error in ParamTableModel";
        System.err.println("Column error in ParamTableModel");
    }
    return null;
  }

  /*
   * Don't need to implement this method unless your table's
   * editable.
   */
  public boolean isCellEditable(int row, int col)
  {
    if (col == TYPE_COL)
      return false;
    Integer rowKey = new Integer(row);
    if(noEditRows.contains(rowKey))
      return false;
    if(col > TYPE_COL && multiRows.contains(rowKey))
      return false;
    return true;
  }

  /*
   * Don't need to implement this method unless your table's
   * data can change.
   */
  public void setValueAt(Object value, int row, int col)
  {
    mydata[row][col] = value;
    dirty=true;

    fireTableCellUpdated(row, col);
  }

  OpenAssembly.AssyChangeListener dummyListener = new OpenAssembly.AssyChangeListener()
  {
    public void assyChanged(int action, OpenAssembly.AssyChangeListener source)
    {
    }

    public String getHandle()
    {
      return "Design of Experiments";
    }
  };

}