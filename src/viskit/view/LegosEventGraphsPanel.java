package viskit.view;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 14, 2004
 * Time: 9:44:55 AM
 */
public class LegosEventGraphsPanel extends ViskitJarClassTreePanel 
{
  public LegosEventGraphsPanel(LegoTree legoTree) 
  {
    super(legoTree,"Event Graph availability",
                   "Select-drag-drop an Event Graph from tree list onto the canvas",
                       "Add another Event Graph class file, XML file or directory root to this list",
                       "Remove selected Event Graph class file, XML file or directory from this list");
  }
}
