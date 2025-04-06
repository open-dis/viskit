package viskit.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.ViskitStatics;
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
public class GraphMetadata 
{
    static final Logger LOG = LogManager.getLogger();
    
    public String name = "";
    public String packageName = "";
    public String author = "";
    public String version = "1.0";
    public String description = ""; // originally called "comment"
    public String stopTime = "100.0";    
    public String extendsPackageName = "";
    public String implementsPackageName = "";
    
    public boolean verbose = false;

    public GraphMetadata() {
        this(null);
    }

    public GraphMetadata(Object caller) 
    {
        author = System.getProperty("user.name"); // TODO make this a viskit user property
        packageName = "test"; // default
        
        if (caller instanceof AssemblyModelImpl) 
        {
            name = "NewAssembly";
            viskit.xsd.bindings.assembly.ObjectFactory jaxbAssemblyObjectFactory = 
                    new viskit.xsd.bindings.assembly.ObjectFactory();
            SimkitAssembly tempAssembly = jaxbAssemblyObjectFactory.createSimkitAssembly();
            extendsPackageName = tempAssembly.getExtend();
            implementsPackageName = tempAssembly.getImplement();
            description = "";
        } 
        else
        {
            name = "NewEventGraphName";
            viskit.xsd.bindings.eventgraph.ObjectFactory jaxbEventGraphObjectFactory =
                    new viskit.xsd.bindings.eventgraph.ObjectFactory();
            SimEntity tempSimEntity = jaxbEventGraphObjectFactory.createSimEntity();
            extendsPackageName = tempSimEntity.getExtend();
            implementsPackageName = tempSimEntity.getImplement();
            description = "";
        }
    }

    public GraphMetadata(String newName, String newPackageName, String newAuthor, String newVersion, 
                         String newDescription, String newExtendsPackageName, String newImplementsPackageName) {
        name = newName;
        packageName = newPackageName;
        author = newAuthor;
        version = newVersion;
        extendsPackageName = newExtendsPackageName;
        implementsPackageName = newImplementsPackageName;
        description = ViskitStatics.emptyIfNull(newDescription);
    }
}
