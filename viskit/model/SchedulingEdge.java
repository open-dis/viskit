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

public class SchedulingEdge extends Edge
{
  public String priority;

  SchedulingEdge()       // package-limited
  {
    parameters = new ArrayList();
  }
  
  Object copyShallow()
  {
    SchedulingEdge se = new SchedulingEdge();
    se.opaqueViewObject = opaqueViewObject;
    se.to = to;
    se.from = from;
    se.parameters = parameters;
    se.delay = delay;
    se.conditional = conditional;
    se.conditionalsComment = conditionalsComment;
    se.priority = priority;
    return se;
  }
}
