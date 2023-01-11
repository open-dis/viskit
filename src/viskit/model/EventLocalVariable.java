package viskit.model;

import edu.nps.util.LogUtilities;
import org.apache.logging.log4j.Logger;
import viskit.ViskitGlobals;

/**
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Apr 1, 2004
 * @since 3:59:01 PM
 * @version $Id$
 */
public class EventLocalVariable extends ViskitElement 
{
    static final Logger LOG = LogUtilities.getLogger(EventLocalVariable.class);

    private String[] arraySize;
    private String   indexingExpression;
    private boolean  operation;
    private String   operationOrAssignment;
    private String   value;
    private String   comment = EMPTY;
    private String   description = new String();

    public EventLocalVariable(String name, String type, String value) {
        this.name = name;
        setType(type);
        this.value = value;
    }

    @Override
    public String toString() {
        return (type.isEmpty() && name.isEmpty()) ? EMPTY : "(" + type + ") " + name;
    }
    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public final void setType(String pType) {
        super.setType(pType);
        arraySize = ViskitGlobals.instance().getArraySize(pType);
    }

    public String[] getArraySize() {
        return arraySize;
    }
   @Override
    public String getIndexingExpression() {
        return indexingExpression;
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
}
