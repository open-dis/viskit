package viskit;

import viskit.model.EventLocalVariable;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Apr 8, 2004
 * Time: 8:49:21 AM
 */

public class LocalVariablesPanel extends ViskitTablePanel
{
  private String[] mytitles = {"name","type","initial value","comment"};
  private static int count = 0;
  
  LocalVariablesPanel(int wid)
  {
    this(wid,0);
  }

  LocalVariablesPanel(int wid, int numRows)
  {
    super(wid,numRows);
    init(true);            // separate constructor from initialization
  }

  public String[] getColumnTitles()
  {
    return mytitles;
  }

  public String[] getFields(Object o, int rowNum)
  {
    String[] sa = new String[4];
    sa[0] = ((EventLocalVariable)o).getName();
    sa[1] = ((EventLocalVariable)o).getType();
    sa[2] = ((EventLocalVariable)o).getValue();
    sa[3] = ((EventLocalVariable)o).getComment();
    return sa;
  }

  public Object newRowObject()
  {
    return new EventLocalVariable("locvar_"+count++,"int","0");
  }

  public int getNumVisibleRows()
  {
    return 3;
  }
}