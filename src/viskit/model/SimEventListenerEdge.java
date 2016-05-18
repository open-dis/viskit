package viskit.model;

import edu.nps.util.LogUtilities;
import org.apache.log4j.Logger;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 2:57:37 PM
 * @version $Id$
 */
public class SimEventListenerEdge extends AssemblyEdge
{
    static final Logger LOG = LogUtilities.getLogger(SimEventListenerEdge.class);

    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String value;
    private String comment;
    private String description = new String();

    SimEventListenerEdge() // package-limited
    {
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
		if ((comment != null) && !comment.isEmpty())
		{
			description = comment.trim();
			comment     = "";
		}
	}

    @Deprecated
    @Override
    public String getComment()
	{
		moveLegacyCommentsToDescription ();
        return description;
    }

    /*
    Object copyShallow()
    {
    AdapterEdge se = new AdapterEdge();
    se.opaqueViewObject = opaqueViewObject;
    se.to = to;
    se.from = from;
    se.parameters = parameters;
    se.delay = delay;
    se.conditional = conditional;
    se.conditionalsComment = conditionalsComment;
    return se;
    }
     */
}
