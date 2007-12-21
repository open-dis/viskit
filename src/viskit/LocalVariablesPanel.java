package viskit;

import viskit.model.EventLocalVariable;
import viskit.model.ViskitElement;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Apr 8, 2004
 * @since 8:49:21 AM
 * @version $Id: LocalVariablesPanel.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class LocalVariablesPanel extends ViskitTablePanel
{
  private String[] mytitles = {"name","type","initial value","description"};
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

  public ViskitElement newRowObject()
  {
    //return new EventLocalVariable("locvar_"+count++,"int","0"); 
    return new EventLocalVariable(VGlobals.instance().getActiveEventGraphModel().generateLocalVariableName(),
                                    "int","0");
  }

  public int getNumVisibleRows()
  {
    return 3;
  }
}