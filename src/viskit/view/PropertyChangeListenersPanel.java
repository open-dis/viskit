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
public class PropertyChangeListenersPanel extends ViskitJarClassTreePanel
{
  public PropertyChangeListenersPanel(LegoTree legoTree)
  {
    super(legoTree,"Property Change Listener selection, addition", 
                   "Select-drag-drop a Property Change Listener from list onto the canvas",
            "Add another Property Change Listener class to this list",
            "Remove selected Property Change Listener class from this list");
  }
}