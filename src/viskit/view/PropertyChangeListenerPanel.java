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
public class PropertyChangeListenerPanel extends ClassDisplayPanel {
  public PropertyChangeListenerPanel(LegoTree ltree) {
    super(ltree,"Property Change Listener Selection", 
			"Select and drag a property change listener (PCL) onto the Assembly Editor", // hint
			"Add a property change listener class to this list",                         //  plus-button tooltip
			"Remove a property change listener class from this list");                   // minus-button tooltip
  }
}