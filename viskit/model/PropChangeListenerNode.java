package viskit.model;

import viskit.xsd.bindings.Event;

import java.awt.*;
import java.util.ArrayList;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Mar 8, 2004
 * Time: 9:08:08 AM
 */

/**
 * An event as seen by the model (not the view)
 */
public class PropChangeListenerNode extends ViskitElement
{
  private String name;
  private String type;
  private String paramType;
  private String paramValue;
  private Vector connections = new Vector();

/*
  private Vector    connections = new Vector();
  private ArrayList transitions = new ArrayList();
  private Vector    localVariables = new Vector();
  private ArrayList arguments = new ArrayList();
  */
  private Point     position = new Point(0,0);
  //private ArrayList comments = new ArrayList();


  PropChangeListenerNode(String name, String type)      // package access on constructor
  {
    this.name = name;
    this.type = type;
  }
  public String toString()
  {
    return name;
  }
/*
  public PropChangeListenerNode shallowCopy()
  {
    PropChangeListenerNode en   = (PropChangeListenerNode)super.shallowCopy(new PropChangeListenerNode(name+"-copy",type));
    en.connections = connections;
    en.comments    = comments;
    en.transitions = transitions;
    en.localVariables = localVariables;
    en.arguments   = arguments;
    en.connections = connections;
    return en;
  }
*/
  public void setParamType(String s)
  {
    paramType = s;
  }
  public String getParamType()
  {
    return paramType;
  }
  public void setParamValue(String s)
  {
    paramValue = s;
  }
  public String getParamValue()
  {
    return paramValue;
  }
  public String getName()
  {
    return name;
  }
  public void setName(String s)
  {
    name = s;
  }
  public String getType()
  {
    return type;
  }
  public void setType(String typ)
  {
    type = typ;
  }
  /*public ArrayList getComments()
  {
    return comments;
  }

  public void setComments(ArrayList comments)
  {
    this.comments = comments;
  }
  */

  public Point getPosition()
  {
    return position;
  }

  public void setPosition(Point position)
  {
    this.position = position;
  }
  public Vector getConnections()
  {
    return connections;
  }
}
