package viskit.mvc;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Mar 2, 2004
 * Time: 10:56:30 AM
 */

/**
 * From an article at www.jaydeetechnology.co.uk
 */

/**
 * The view displays information and captures data entered.
 */

public interface MvcView
{
  void setController(MvcController controller);
  void setModel     (MvcModel      model);

  MvcController getController();
  MvcModel      getModel();
}
