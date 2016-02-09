package viskit.model;

import viskit.xsd.bindings.assembly.SimkitAssembly;
import viskit.xsd.bindings.eventgraph.SimEntity;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Apr 12, 2004
 * @since 3:50:28 PM
 * @version $Id$
 */
public class GraphMetadata {

    public String name = "";
    public String packageName = "";
    public String author = "";
    public String version = "1.0";  // TODO date
    public String description = ""; // originally captured in "Comment" element(s), now an attribute
    public String stopTime = "100.0";    
    public String extendsPackageName = "";
    public String implementsPackageName = "";
    
    public boolean verbose = false;

    public GraphMetadata() {
        this(null);
    }

    public GraphMetadata(Object caller) {
        author = System.getProperty("user.name");
        packageName = "test";
        
        if (caller instanceof AssemblyModelImpl)
		{
            name = "NewAssembly";
            viskit.xsd.bindings.assembly.ObjectFactory objectFactory = new viskit.xsd.bindings.assembly.ObjectFactory();
            SimkitAssembly tmp    = objectFactory.createSimkitAssembly();
               extendsPackageName = tmp.getExtend();
            implementsPackageName = tmp.getImplement();
        } 
		else
		{
            name = "NewEventGraph";
            viskit.xsd.bindings.eventgraph.ObjectFactory objectFactory = new viskit.xsd.bindings.eventgraph.ObjectFactory();
            SimEntity tempSimEntity = objectFactory.createSimEntity();
                 extendsPackageName = tempSimEntity.getExtend();
              implementsPackageName = tempSimEntity.getImplement();
        }
		if (description == null) // when not defined in XML
		{
			 description = new String(); // keep empty since legacy Comment information may get added
		}
    }

    public GraphMetadata(String n, String p, String a, String v, String e, String i, String d) {
        name                  = n;
        packageName           = p;
        author                = a;
        version               = v;
        extendsPackageName    = e;
        implementsPackageName = i;
		description           = d;
    }
}
