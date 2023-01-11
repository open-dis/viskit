package viskit.model;

import edu.nps.util.LogUtilities;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.apache.logging.log4j.Logger;
import viskit.xsd.bindings.eventgraph.Event;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 9:08:08 AM
 * @version $Id$
 *
 * An event as seen by the model (not the view)
 */
public class EventNode extends ViskitElement 
{
    static final Logger LOG = LogUtilities.getLogger(EventNode.class);

    private ArrayList<ViskitElement>          argumentsArrayList = new ArrayList<>();
    private Vector<ViskitElement>              connectionsVector = new Vector<>();
    private ArrayList<ViskitElement>     localVariablesArrayList = new ArrayList<>();
    private ArrayList<ViskitElement>   stateTransitionsArrayList = new ArrayList<>();
	
    private Point2D position = new Point2D.Double(0.d, 0.d);
    private String  codeblock = EMPTY;
    private boolean operation;
    private String  operationOrAssignment;
    private String  indexingExpression;
    private String  value;
    private ArrayList<String>          commentsArrayList       = new ArrayList<>(); // usage is deprecated, move information to description field
    private String  comment;
    private String                     description             = new String();

    EventNode(String name) // package access on constructor
    {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public EventNode shallowCopy()
	{
        EventNode eventNode = (EventNode) super.shallowCopy(new EventNode(name + "-copy"));
        eventNode.argumentsArrayList        = argumentsArrayList;
        eventNode.codeblock                 = codeblock;
        eventNode.connectionsVector         = connectionsVector;
        eventNode.localVariablesArrayList   = localVariablesArrayList;
        eventNode.stateTransitionsArrayList = stateTransitionsArrayList;
        eventNode.commentsArrayList         = commentsArrayList;
        eventNode.description               = description;
		
		if (!eventNode.commentsArrayList.isEmpty())
		{
			for (String comment : eventNode.commentsArrayList)
			{
				eventNode.description  += " " + comment;
			}
			eventNode.description = eventNode.description.trim();
			eventNode.commentsArrayList.clear();
		}
        return eventNode;
    }

    @Override
    public void setName(String newName) {
        if (this.opaqueModelObject != null) {
            ((Event) opaqueModelObject).setName(newName);
        }
        this.name = newName;
    }

    public List<ViskitElement> getArguments() {
        return argumentsArrayList;
    }

    public void setArguments(ArrayList<ViskitElement> arguments) {
        this.argumentsArrayList = arguments;
    }

    public void setCodeBLock(String s) {
        this.codeblock = s;
    }

    public String getCodeBlock() {
        return codeblock;
    }

    public Vector<ViskitElement> getConnections() {
        return connectionsVector;
    }

    public void setConnections(Vector<ViskitElement> connections) {
        this.connectionsVector = connections;
    }

    public List<ViskitElement> getLocalVariables() {
        return localVariablesArrayList;
    }

    public void setLocalVariables(ArrayList<ViskitElement> localVariables) {
        this.localVariablesArrayList = localVariables;
    }

    public ArrayList<ViskitElement> getStateTransitions() {
        return stateTransitionsArrayList;
    }

    public void setStateTransitions(ArrayList<ViskitElement> stateTransitions) {
        this.stateTransitionsArrayList = stateTransitions;
    }

    public Point2D getPosition() {
        return position;
    }

    public void setPosition(Point2D position) {
        this.position = position;
    }

    @Override
    public String getIndexingExpression() {
        return indexingExpression;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getOperationOrAssignment() {
        return operationOrAssignment;
    }

    @Override
    public boolean isOperation() {
        return operation;
    }

	/**
	 * @return the description
	 */
	@Override
	public String getDescription()
	{
		moveLegacyCommentsToDescription ();
		return description;
	}

	/**
	 * @param newDescription the description to set
	 */
	@Override
	public void setDescription(String newDescription) 
	{
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
		if ((comment != null) && !comment.isEmpty())
		{
			description = comment.trim();
			comment     = "";
		}
		if ((commentsArrayList != null) && !commentsArrayList.isEmpty())
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

    @Deprecated
    @Override
    public String getComment()
	{
		moveLegacyCommentsToDescription ();
		return description;
    }
}
