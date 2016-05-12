package viskit.model;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 1, 2004
 * @since 9:47:55 AM
 * @version $Id$
 */
public abstract class AssemblyNode extends ViskitElement {

    private Vector<AssemblyEdge> connections = new Vector<>();
    private Point2D       position = new Point2D.Double(0d, 0d);
    private ViskitInstantiator instantiator;
    private List<String>  commentsArrayList = new ArrayList<>();
    private String        description = EMPTY;  // instance information

    AssemblyNode(String name, String type, String description) // package access on constructor
    {
        this.name = name;
        this.type = type;
        this.description = description;
    }
    public Vector<AssemblyEdge> getConnections() {
        return connections;
    }

    public void setConnections(Vector<AssemblyEdge> connections) {
        this.connections = connections;
    }

    public Point2D getPosition() {
        return position;
    }

    public void setPosition(Point2D position) {
        this.position = position;
    }

    public ViskitInstantiator getInstantiator() {
        return instantiator;
    }

    public void setInstantiator(ViskitInstantiator instantiator) {
        this.instantiator = instantiator;
    }

	@Override
    public String getDescription() 
	{		
		moveLegacyCommentsToDescription ();
        return description;
    }

    @Override
    public void setDescription(String newDescription) {
        this.description = newDescription;
    }
	
	/**
	 * "Comment" elements are earlier viskit constructs.
	 * If found from an earlier model, append them as part of description and then delete.
	 */
	private void moveLegacyCommentsToDescription ()
	{
		if (description == null)
			description = new String();
		if (!commentsArrayList.isEmpty())
		{
			String result = new String();
			for (String comment : commentsArrayList)
			{
				result += " " + comment;
			}
			description = (description + " " + result).trim();
			commentsArrayList.clear();
		}
	}

    @Override
	@Deprecated
    public String getComment() 
	{
		moveLegacyCommentsToDescription ();
        return description;
    }
}
