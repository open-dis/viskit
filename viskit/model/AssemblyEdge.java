package viskit.model;

import java.util.ArrayList;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Mar 8, 2004
 * Time: 2:57:37 PM
 */

abstract public class AssemblyEdge extends ViskitElement
{
  public Object to; //PropChangeListenerNode to;
  public Object from;
  public ArrayList parameters;
  public String    conditional;
  public String    conditionalsComment;
  public String    delay;
  abstract Object  copyShallow();
}
