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
  private Vector connections = new Vector();
  private ArrayList constructorArguments = new ArrayList();

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
  public ArrayList getConstructorArguments()
  {
    return constructorArguments;
  }
  public void setConstructorArguments(ArrayList lis)
  {
    constructorArguments = lis;
  }

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
