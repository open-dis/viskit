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
    public String revision = "";  // TODO originally version, make XML consistent
    public String description = ""; // originally captured in "Comment" element(s), now an attribute
    public String stopTime = "100.0";    
    public String extendsPackageName = "";
    public String implementsPackageName = "";
    
    public  boolean verbose = false;
    private boolean project = false;
    private String label;
	private String DEFAULT_PACKAGE_NAME = "test";

    public GraphMetadata()
	{
        initialize ();
    }

    public GraphMetadata(Object caller)
	{
        initialize ();
        
        if (caller instanceof AssemblyModelImpl)
		{
			label   = "assembly";
			project = false;
            name    = "NewAssembly";
            viskit.xsd.bindings.assembly.ObjectFactory objectFactory = new viskit.xsd.bindings.assembly.ObjectFactory();
            SimkitAssembly tmp    = objectFactory.createSimkitAssembly();
               extendsPackageName = tmp.getExtend();
            implementsPackageName = tmp.getImplement();
        } 
		else if (caller instanceof EventGraphModelImpl)
		{
			label   = "event graph";
			project = false;
            name    = "NewEventGraph";
            viskit.xsd.bindings.eventgraph.ObjectFactory objectFactory = new viskit.xsd.bindings.eventgraph.ObjectFactory();
            SimEntity tempSimEntity = objectFactory.createSimEntity();
                 extendsPackageName = tempSimEntity.getExtend();
              implementsPackageName = tempSimEntity.getImplement();
        }
		else // project TODO verify OK
		{
			label   = "Project";
			project = true;
			name 				   = "NewProject";
			packageName 	        = ""; // unused
            extendsPackageName      = ""; // unused
            implementsPackageName   = ""; // unused
        }
    }

    public GraphMetadata(String name, String packageName, String author, String revision, String extendsPackageName, String implementsPackageName, String description, boolean isProject)
	{
        initialize ();
		
        this.name                  = name;
        this.packageName           = packageName;
        this.author                = author;
        this.revision              = revision;
        this.extendsPackageName    = extendsPackageName;
        this.implementsPackageName = implementsPackageName;
		this.description           = description;
		project                    = isProject;
		if (project)
			label   = "project";
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
