package viskit.view;

import viskit.ViskitGlobals;
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
 * @version $Id$
 */
public class LocalVariablesPanel extends ViskitTablePanel
{
  private final String[] mytitles = {"name","type","initial value","description"};

  public LocalVariablesPanel(int width)
  {
    this(width,0);
  }

  public LocalVariablesPanel(int width, int numberRows)
  {
    super(width,numberRows);
    initialize(true);            // separate constructor from initialization
  }

  @Override
  public String[] getColumnTitles()
  {
    return mytitles;
  }

  @Override
  public String[] getFields(ViskitElement e, int rowNumber)
  {
    String[] sa = new String[4];
    sa[0] = e.getName();
    sa[1] = e.getType();
    sa[2] = e.getValue();
    sa[3] = e.getDescription();
    return sa;
  }

  @Override
  public ViskitElement newRowObject()
  {
    //return new EventLocalVariable("locvar_"+count++,"int","0");
    return new EventLocalVariable(
            ViskitGlobals.instance().getActiveEventGraphModel().generateLocalVariableName(),
            "int",
            "0");
  }

  @Override
  public int getNumberVisibleRows()
  {
    return 3;
  }
}