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

public class AdapterEdge extends AssemblyEdge
{
  private String targetEvent;
  private String sourceEvent;

  AdapterEdge()       // package-limited
  {
  }
  
  public String getTargetEvent() { return targetEvent;}
  public void   setTargetEvent(String ev) { targetEvent = ev;}
  
  public String getSourceEvent() { return sourceEvent;}
  public void   setSourceEvent(String ev) { sourceEvent = ev;}
}
