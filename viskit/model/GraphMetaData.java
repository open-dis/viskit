package viskit.model;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Apr 12, 2004
 * Time: 3:50:28 PM
 */

public class GraphMetaData
{
  public String name = "EventGraph_name";
  public String pkg = "pkg";
  public String author = "";
  public String version = "1.0";
  public String comment = "";

  public GraphMetaData()
  {
    author = System.getProperty("user.name");
  }
  public GraphMetaData(String n, String p, String a, String v)
  {
    name = n;
    pkg = p;
    author = a;
    version = v;
  }
}
