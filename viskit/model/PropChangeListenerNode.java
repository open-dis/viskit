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
public class PropChangeListenerNode extends AssemblyNode
{
  PropChangeListenerNode(String name, String type)      // package access on constructor
  {
    super(name,type);
  }
}
