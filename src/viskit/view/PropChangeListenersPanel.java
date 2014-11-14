package viskit.view;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 14, 2004
 * Time: 10:10:13 AM
 */
public class PropChangeListenersPanel extends ClassPanel {
  public PropChangeListenersPanel(LegosTree ltree) {
    super(ltree,"Property Change Listeners", "Add a property change listener class to this list",
                                             "Remove a property change listener class from this list");
  }
}