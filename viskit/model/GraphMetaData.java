package viskit.model;

import viskit.xsd.bindings.eventgraph.SimEntityType;
import viskit.xsd.bindings.assembly.SimkitAssemblyType;

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
  public String stopTime = "100.";
  public boolean verbose = false;
  public String extend = "";

  public GraphMetaData()
  {
    author = System.getProperty("user.name");

    /** get defaults from Schema */
    try {
      viskit.xsd.bindings.eventgraph.ObjectFactory of =
          new viskit.xsd.bindings.eventgraph.ObjectFactory();
      SimEntityType tmp = of.createSimEntityType();
      extend = tmp.getExtend();
    }
    catch (javax.xml.bind.JAXBException e) {
      e.printStackTrace();
    }
  }

  public GraphMetaData(Object caller)
  {
    author = System.getProperty("user.name");

    if (caller instanceof AssemblyModel) {
      try {
        viskit.xsd.bindings.assembly.ObjectFactory of =
            new viskit.xsd.bindings.assembly.ObjectFactory();
        SimkitAssemblyType tmp = of.createSimkitAssemblyType();
        extend = tmp.getExtend();
      }
      catch (javax.xml.bind.JAXBException e) {
        e.printStackTrace();
      }
    }
    else {
      try {
        viskit.xsd.bindings.eventgraph.ObjectFactory of =
            new viskit.xsd.bindings.eventgraph.ObjectFactory();
        SimEntityType tmp = of.createSimEntityType();
        extend = tmp.getExtend();
      }
      catch (javax.xml.bind.JAXBException e) {
        e.printStackTrace();
      }
    }
  }

  public GraphMetaData(String n, String p, String a, String v, String e)
  {
    name = n;
    pkg = p;
    author = a;
    version = v;
    extend = e;
  }
}
