package viskit.model;

import org.apache.logging.log4j.LogManager;
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
    static final Logger LOG = LogManager.getLogger();
    
    private String operationOrAssignment = EMPTY;
    private boolean isOperation = false;
    private String indexingExpression = EMPTY;
    private String value;
    // obsolete
//    private String comment;
//    private List<String> descriptionArray = new ArrayList<>();
//    private List<String> comments = new ArrayList<>();
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
    public boolean isAssignment() {
        return !isOperation;
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

    // obsolete
//    @Override
//    public List<String> getDescriptionArray() {
//        return descriptionArray;
//    }
//
//    @Override
//    public void setDescriptionArray(List<String> descriptionArray) {
//        this.descriptionArray = descriptionArray;
//    }
//
//    public List<String> getComments() {
//        return comments;
//    }
//
//    public void setComments(List<String> comments) {
//        this.comments = comments;
//    }
    
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
}