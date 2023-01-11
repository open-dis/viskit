package viskit.model;

import edu.nps.util.LogUtilities;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Apr 1, 2004
 * @since 3:57:26 PM
 * @version $Id$
 */
public class EventArgument extends ViskitElement 
{
    static final Logger LOG = LogUtilities.getLogger(EventArgument.class);

    private String value;
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private List<String> commentsArrayList = new ArrayList<>();
    private String comment;
    private String description;

    @Override
    public String toString() {
        return "(" + type + ") " + name;
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

    @Override
	@Deprecated
    public String getComment() 
	{
		moveLegacyCommentsToDescription ();
        return description;
    }
}
