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

import org.jdom.Attribute;
import org.jdom.Element;

import javax.swing.table.DefaultTableModel;
import java.util.*;
import java.util.regex.Pattern;


public class ParamTableModel extends DefaultTableModel
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

  public ParamTableModel(List simEntitiesJDom, List designParamsJDom)
  {
    super(0, 0);

    rows = new Vector();
    prefixes = new Vector();
    for (Iterator itr = simEntitiesJDom.iterator(); itr.hasNext();) {
      Element elm = (Element) itr.next();
      assert elm.getName().equalsIgnoreCase("SimEntity");
      processRow(elm);
    }
    mydata = (Object[][]) rows.toArray(mydata);

    if(designParamsJDom != null) {
      for (Iterator itr = designParamsJDom.iterator(); itr.hasNext();) {
        Element elm = (Element) itr.next();
        assert elm.getName().equalsIgnoreCase("TerminalParameter");
        processDesignParam(elm);
      }
    }
  }

  private void processDesignParam(Element elm)
  {
    Attribute at = elm.getAttribute("type");
    String typ = (at==null?"":at.getValue());
    at = elm.getAttribute("name");
    String nm = (at==null?"":at.getValue());
    assert nm.length()>0:"Terminal param w/out name ref!";
    int row = ((Integer)termHashMap.get(nm)).intValue();
    String txt = elm.getTextTrim();
    Object[] minMax = parseMinMax(txt);
    if(minMax != null) {
      this.setValueAt(""+((Double)minMax[0]).doubleValue(),row,MIN_COL);
      this.setValueAt(""+((Double)minMax[1]).doubleValue(),row,MAX_COL);
    //List lis = elm.getContent();
    //System.out.println(typ+" "+nm +" "+txt);
      setValueAt(new Boolean(true),row,FACTOR_COL); //cb
    }
  }
  
  Object[] parseMinMax(String txt)
  {
    Pattern pat =  Pattern.compile("Double\\s*\\(",Pattern.DOTALL);

    String[] sa = pat.split(txt);
    if(sa.length < 3)

      return null;
    Object[] da = new Object[2];
    da[0] = sa[1].substring(0,sa[1].indexOf(')')).trim();
    da[0] = new Double((String)da[0]);
    da[1] = sa[2].substring(0,sa[2].indexOf(')')).trim();
    da[1] = new Double((String)da[1]);
    return da;
  }


  String currentSEname = "";
  HashMap termHashMap = new HashMap();
  Vector prefixes;
  private void processRow(Element el)
  {
    Object[] oa = new Object[6];

    String nm = el.getName();
    if (nm.equalsIgnoreCase("SimEntity")) {
      Attribute at = el.getAttribute("name");
      currentSEname = (at == null ? "" : at.getValue());
      oa[NAME_COL] = "<html><b>"+currentSEname;
      at = el.getAttribute("type");
      String typ = (at == null ? "" : at.getValue());
      oa[TYPE_COL] = "<html><b>"+loseDots(typ);
      oa[VALUE_COL] = oa[MIN_COL] = oa[MAX_COL] = "";
      oa[FACTOR_COL] = new Boolean(false);
      rows.add(oa);
      noEditRows.add(new Integer(rows.size()-1));
      prefixes.clear();
      prefixes.add(currentSEname);
      List children = el.getChildren();
      int i=1;
      for (Iterator itr = children.iterator(); itr.hasNext();) {
        prefixes.add("."+i++);
        processRow((Element) itr.next());
        prefixes.remove(prefixes.size()-1);
      }
    }
    else if (nm.equalsIgnoreCase("TerminalParameter")) {
      Attribute at = el.getAttribute("nameRef");
      String nam = (at == null ? dumpPrefixes():at.getValue());
      oa[NAME_COL] = nam;
      at = el.getAttribute("type");
      String typ = (at == null ? "" : at.getValue());
      oa[TYPE_COL] = loseDots(typ);
      at = el.getAttribute("value");
      oa[VALUE_COL] = (at == null ? "" : at.getValue());
      oa[MIN_COL] = oa[MAX_COL] = ""; // will be editted or filled in from existing file
      oa[FACTOR_COL] = new Boolean(false);
      rows.add(oa);
      termHashMap.put(nam,new Integer(rows.size()-1));
    }
    else {
      assert nm.equalsIgnoreCase("MultiParameter");
      Attribute at = el.getAttribute("nameRef");
      oa[NAME_COL] = (at == null ? dumpPrefixes() : at.getValue());
      at = el.getAttribute("type");
      oa[TYPE_COL] = loseDots(at == null ? "" : at.getValue());
      oa[VALUE_COL] = oa[MIN_COL] = oa[MAX_COL] = "";
      oa[FACTOR_COL] = new Boolean(false);
      rows.add(oa);
      multiRows.add(new Integer(rows.size()-1));

      List children = el.getChildren();
      int i=1;
      for (Iterator itr = children.iterator(); itr.hasNext();) {
        prefixes.add("."+i++);
        processRow((Element) itr.next());
        prefixes.remove(prefixes.size()-1);
      }
    }
  }
  StringBuffer sb = new StringBuffer();
  private String dumpPrefixes()
  {
    sb.setLength(0);
    for(Iterator itr=prefixes.iterator();itr.hasNext();)
      sb.append(itr.next());
    return sb.toString();
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
        assert false:"Column error in ParamTableModel";
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
    fireTableCellUpdated(row, col);
  }

}