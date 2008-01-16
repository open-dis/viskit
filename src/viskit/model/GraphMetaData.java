package viskit.model;

import viskit.xsd.bindings.eventgraph.SimEntity;
import viskit.xsd.bindings.assembly.SimkitAssembly;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Apr 12, 2004
 * @since 3:50:28 PM
 * @version $Id: GraphMetaData.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class GraphMetaData {

    public String name = "EventGraphName";
    public String packageName = "";
    public String author = "";
    public String version = "1.0";
    public String description = ""; // originally called "comment"
    public String stopTime = "100.";
    public boolean verbose = false;
    public String extendsPackageName = "";

    public GraphMetaData() {
        author = System.getProperty("user.name");

        /** get defaults from Schema */
        viskit.xsd.bindings.eventgraph.ObjectFactory of =
                new viskit.xsd.bindings.eventgraph.ObjectFactory();
        SimEntity tmp = of.createSimEntity();
        extendsPackageName = tmp.getExtend();
    }

    public GraphMetaData(Object caller) {
        author = System.getProperty("user.name");
        packageName = "test";
        if (caller instanceof AssemblyModel) {
            name = "AssemblyName";
            viskit.xsd.bindings.assembly.ObjectFactory of =
                    new viskit.xsd.bindings.assembly.ObjectFactory();
            SimkitAssembly tmp = of.createSimkitAssembly();
            extendsPackageName = tmp.getExtend();

        } else {

            viskit.xsd.bindings.eventgraph.ObjectFactory of =
                    new viskit.xsd.bindings.eventgraph.ObjectFactory();
            SimEntity tmp = of.createSimEntity();
            extendsPackageName = tmp.getExtend();
        }
    }

    public GraphMetaData(String n, String p, String a, String v, String e) {
        name = n;
        packageName = p;
        author = a;
        version = v;
        extendsPackageName = e;
    }
}
