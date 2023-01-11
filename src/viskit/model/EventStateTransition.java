package viskit.model;

import edu.nps.util.LogUtilities;
import java.util.ArrayList;
import org.apache.logging.log4j.Logger;
import viskit.ViskitGlobals;

/**
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Apr 1, 2004
 * @since 4:00:29 PM
 * @version $Id$
 */
public class EventStateTransition extends ViskitElement 
{
    static final Logger LOG = LogUtilities.getLogger(EventStateTransition.class);

    private String  operationOrAssignment = EMPTY;
    private String  indexingExpression = EMPTY;
    private boolean isOperation = false;
    private String  value;
    private ArrayList<String> commentsArrayList = new ArrayList<>();
    private String  comment;
    private String  description = EMPTY;  // instance information
	
    private String localVariableAssignment;
    private String localVariableInvocation;

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        if (localVariableAssignment != null && !localVariableAssignment.isEmpty()) {
            sb.append(localVariableAssignment);
            sb.append(' ');
            sb.append('=');
            sb.append(' ');
        }

        sb.append(name);
        if (ViskitGlobals.instance().isArray(type)) {
            handleArrayIndexing(sb);
        }

        // Prevent a "=" from being appended if empty
        if (operationOrAssignment != null && !operationOrAssignment.isEmpty()) {
            if (isOperation) {
                sb.append('.');
            } else {
                sb.append('=');
            }
            sb.append(operationOrAssignment);
        }

        if (localVariableInvocation != null && !localVariableInvocation.isEmpty()) {
            sb.append('\n');
            sb.append(localVariableInvocation);
        }

        return sb.toString();
    }

    private void handleArrayIndexing(StringBuffer sb) {
        if (indexingExpression != null && !indexingExpression.isEmpty()) {
            sb.append('[');
            sb.append(indexingExpression);
            sb.append(']');
        }
    }

    @Override
    public boolean isOperation() {
        return isOperation;
    }

    public void setOperation(boolean operation) {
        isOperation = operation;
    }

    @Override
    public String getOperationOrAssignment() {
        return operationOrAssignment;
    }

    public void setOperationOrAssignment(String operationOrAssignment) {
        this.operationOrAssignment = operationOrAssignment;
    }

    @Override
    public String getIndexingExpression() {
        return indexingExpression;
    }

    public void setIndexingExpression(String idxExpr) {
        this.indexingExpression = idxExpr;
    }

    @Override
    public String getValue() {
        return value;
    }
    /**
     * @return the localVariableAssignment
     */
    public String getLocalVariableAssignment() {
        return localVariableAssignment;
    }

    /**
     * @param localVariableAssignment the localVariableAssignment to set
     */
    public void setLocalVariableAssignment(String localVariableAssignment) {
        this.localVariableAssignment = localVariableAssignment;
    }

    /**
     * @return the localVariableInvocation
     */
    public String getLocalVariableInvocation() {
        return localVariableInvocation;
    }

    /**
     * @param localVariableInvocation the localVariableInvocation to set
     */
    public void setLocalVariableInvocation(String localVariableInvocation) {
        this.localVariableInvocation = localVariableInvocation;
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