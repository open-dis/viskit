package viskit.model;

import java.util.ArrayList;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Mar 8, 2004
 * Time: 9:04:09 AM
 */

public class PropChangeEdge extends AssemblyEdge
{
  PropChangeEdge()       // package-limited
  {
    setComment("Property change listener connection");
  }

  //protected String type;
  protected String property;
  
  //public String getType(){return type;}
  //public void   setType(String t){type=t;}
  public String getProperty(){return property;}
  public void   setProperty(String p){property=p;}
}
