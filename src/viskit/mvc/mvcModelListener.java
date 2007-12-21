package viskit.mvc;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 2, 2004
 * @since 11:29:33 AM
 * @version $Id: mvcModelListener.java 1662 2007-12-16 19:44:04Z tdnorbra $
 *
 * From an article at www.jaydeetechnology.co.uk
 */
public interface mvcModelListener
{
  public void modelChanged(mvcModelEvent event);
}