package viskit.mvc;

import java.awt.event.ActionEvent;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 2, 2004
 * @since 11:01:04 AM
 * @version $Id: mvcModelEvent.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */

/**
 * From an article at www.jaydeetechnology.co.uk
 */

/**
 * Used to notify interested objects of changes in the state of a model
 */

public class mvcModelEvent extends ActionEvent
{
  public mvcModelEvent(Object obj, int id, String message)
  {
    super(obj,id,message);
  }

}
