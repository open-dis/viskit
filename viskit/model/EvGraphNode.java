package viskit.model;

import viskit.xsd.bindings.eventgraph.Event;
import viskit.xsd.bindings.assembly.SimEntity;

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
public class EvGraphNode extends AssemblyNode
{
  protected boolean outputMarked = false;
  EvGraphNode(String name, String type)      // package access on constructor
  {
    super(name,type);
  }

/*
  public EvGraphNode shallowCopy()
  {
    EvGraphNode en   = (EvGraphNode)super.shallowCopy(new EvGraphNode(name+"-copy",type));
    en.connections = connections;
    en.comments    = comments;
    en.connections = connections;
    return en;
  }
*/
  public boolean isOutputMarked()
  {
    return outputMarked;
  }

  public void setOutputMarked(boolean outputMarked)
  {
    this.outputMarked = outputMarked;
  }

  public String getName()
  {
    /*
    if(this.opaqueModelObject != null)
      return ((Event)opaqueModelObject).getName();
    else
    */
      return super.getName();
  }
  public void setName(String s)
  {
    if(this.opaqueModelObject != null)
      ((SimEntity)opaqueModelObject).setName(s);

    super.setName(s);
  }

}
