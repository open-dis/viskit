package viskit.model;

import java.util.Vector;
import java.util.ArrayList;
import java.awt.*;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jul 1, 2004
 * Time: 9:47:55 AM
 */

abstract public class AssemblyNode extends ViskitElement
{
  private String name;
  private String type;

  private Vector    connections = new Vector();
  private ArrayList comments = new ArrayList();
  private Point     position = new Point(0,0);
  private VInstantiator instantiator;

  AssemblyNode(String name, String type)      // package access on constructor
  {
    this.name = name;
    this.type = type;
    instantiator = new VInstantiator.FreeF(type,"");
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
    this.name = s;
  }
  public String getType()
  {
    return type;
  }
  public void setType(String typ)
  {
   //todo if(this.opaqueModelObject != null)
   //   ((Event)opaqueModelObject).setName(s);

    this.type = typ;

  }

  public ArrayList getComments()
  {
    return comments;
  }

  public void setComments(ArrayList comments)
  {
    this.comments = comments;
  }

  public Vector getConnections()
  {
    return connections;
  }

  public void setConnections(Vector connections)
  {
    this.connections = connections;
  }

  public Point getPosition()
  {
    return position;
  }

  public void setPosition(Point position)
  {
    this.position = position;
  }

  public VInstantiator getInstantiator()
  {
    return instantiator;
  }

  public void setInstantiator(VInstantiator instantiator)
  {
    this.instantiator = instantiator;
  }


}
