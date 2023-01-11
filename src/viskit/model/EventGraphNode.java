package viskit.model;

import edu.nps.util.LogUtilities;
import org.apache.logging.log4j.Logger;
import viskit.view.dialog.UserPreferencesDialog;
import viskit.xsd.bindings.assembly.SimEntity;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 9:08:08 AM
 * @version $Id$

 An event as seen by the model (not the view)
 */
public class EventGraphNode extends AssemblyNode 
{
    static final Logger LOG = LogUtilities.getLogger(EventGraphNode.class);
	
    protected boolean outputMarked = false;
    protected boolean verboseMarked = false;
    private   boolean operation;
    private   String  operationOrAssignment;
    private   String  indexingExpression;
    private   String  value;
    private   String  comment;
    private   String  description = new String();

    EventGraphNode(String name, String type, String description) // package access on constructor
    {
        super(name, type, description);
    }

    /** Enable an extra verbose dump of all entity details
     *
     * @return indication of the detailed checkbox selected for the entity
     * on the EventGraphNodeInspectorDialog
     */
    public boolean isOutputMarked() {
        return outputMarked;
    }

    public void setOutputMarked(boolean outputMarked) {
        this.outputMarked = outputMarked;
    }

    /** 
     * @return indication of the verbose checkbox selected for the entity
     * on the EventGraphNodeInspectorDialog
     */
    public boolean isVerboseMarked() {
        return verboseMarked;
    }

    public void setVerboseMarked(boolean verboseMarked) {
        this.verboseMarked = verboseMarked;
		// TODO set XML attribute
    }

    @Override
    public void setName(String newName) 
	{
        if (this.opaqueModelObject != null) 
		{
            ((SimEntity) opaqueModelObject).setName(newName);
        }
        super.setName(newName);
    }

    @Override
    public String getIndexingExpression() 
	{
        return indexingExpression;
    }

    @Override
    public String getValue() 
	{
        return value;
    }

    @Override
    public String getOperationOrAssignment() 
	{
        return operationOrAssignment;
    }

    @Override
    public boolean isOperation() 
	{
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
	}

    @Deprecated
    @Override
    public String getComment()
	{
		moveLegacyCommentsToDescription ();
		return description;
    }
}
