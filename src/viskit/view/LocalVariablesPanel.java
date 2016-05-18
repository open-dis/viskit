package viskit.view;

import edu.nps.util.LogUtilities;
import org.apache.log4j.Logger;
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
    static final Logger LOG = LogUtilities.getLogger(LocalVariablesPanel.class);
	
  private String[] mytitles = {"name","type","initial value","description"};

  public LocalVariablesPanel(int wid)
  {
    this(wid,0);
  }

  public LocalVariablesPanel(int wid, int numRows)
  {
    super(wid,numRows);
    init(true);            // separate constructor from initialization
  }

  @Override
  public String[] getColumnTitles()
  {
    return mytitles;
  }

  @Override
  public String[] getFields(ViskitElement viskitElement, int rowNum)
  {
    String[] sa = new String[4];
    sa[0] = viskitElement.getName();
    sa[1] = viskitElement.getType();
    sa[2] = viskitElement.getValue();
    sa[3] = viskitElement.getDescription();
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