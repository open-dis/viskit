package viskit.model;

import edu.nps.util.LogUtilities;
import org.apache.log4j.Logger;
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
    static final Logger LOG = LogUtilities.getLogger(GraphMetadata.class);

    public String name        = "";
    public String packageName = "";
    public String author      = "";
    public String created     = "";
    public String revision    = "";  // TODO originally "version", make XML consistent
    public String path        = "";
    public String description = ""; // originally captured in "Comment" element(s), now is description attribute
    public String stopTime    = "100.0";    
    public String extendsPackageName    = "";
    public String implementsPackageName = "";
    
    public  boolean pathEditable = false;
    private boolean project      = false;
    public  boolean updated      = false;
    public  boolean verbose      = true; // TODO user preference
    private String  label;
	private String  DEFAULT_PACKAGE_NAME = "examples"; // TODO user preference
	
	public static final String ASSEMBLY    = "assembly";
	public static final String EVENT_GRAPH = "event graph";
	public static final String PROJECT     = "project";

    public GraphMetadata()
	{
        initialize ();
    }

    public GraphMetadata(Object caller)
	{
        initialize ();
        
        if (caller instanceof AssemblyModelImpl)
		{
			label   = ASSEMBLY;
			project = false;
            name    = "NewAssembly";
            viskit.xsd.bindings.assembly.ObjectFactory objectFactory = new viskit.xsd.bindings.assembly.ObjectFactory();
            SimkitAssembly tmp    = objectFactory.createSimkitAssembly();
               extendsPackageName = tmp.getExtend();
            implementsPackageName = tmp.getImplement();
        } 
		else if (caller instanceof EventGraphModelImpl)
		{
			label   = EVENT_GRAPH;
			project = false;
            name    = "NewEventGraph";
            viskit.xsd.bindings.eventgraph.ObjectFactory objectFactory = new viskit.xsd.bindings.eventgraph.ObjectFactory();
            SimEntity tempSimEntity = objectFactory.createSimEntity();
                 extendsPackageName = tempSimEntity.getExtend();
              implementsPackageName = tempSimEntity.getImplement();
        }
		else // project TODO verify OK
		{
			label   = PROJECT;
			project = true;
			name 				    = "NewProject";
			packageName 	        = ""; // unused
            extendsPackageName      = ""; // unused
            implementsPackageName   = ""; // unused
        }
		this.created = ""; // do not insert date into legacy models
		this.updated = false;
    }

    public GraphMetadata(String name, String packageName, String author, String created, String revision, String extendsPackageName, String implementsPackageName, 
			             String path, String description, boolean isProject)
	{
        initialize ();
		
        this.name                  = name;
        this.packageName           = packageName;
        this.author                = author;
        this.created               = created;
        this.revision              = revision;
        this.extendsPackageName    = extendsPackageName;
        this.implementsPackageName = implementsPackageName;
		this.path                  = path;
		this.description           = description;
		project                    = isProject;
		if (project)
			label   = PROJECT;
		this.updated = false;
    } 
	
	private void initialize ()
	{
        packageName = DEFAULT_PACKAGE_NAME;
		
		if (description == null) // when not defined in XML
		{
			 description = new String(); // keep empty since legacy Comment information may get added
		}
	}

	/**
	 * @return the project
	 */
	public boolean isProject() {
		return project;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}
}
