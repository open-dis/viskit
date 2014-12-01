package viskit.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Apr 1, 2004
 * @since 4:00:29 PM
 * @version $Id$
 */
public class EventStateTransition extends ViskitElement {

    private List<String> descriptionArray = new ArrayList<>();
    private String type;
    private String name;
    private String arrayType;
    private String stateVarName = "";
    private String stateVarType = "";
    private String operationOrAssignment = "";
    private boolean isOperation = false;
    private List<String> comments = new ArrayList<>();
    private String indexingExpression = "";
    private String value;
    private String comment;

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(stateVarName);
        if (stateVarType.indexOf('[') != -1) {
            handleArrayIndexing(sb);
        }

        if (isOperation) {
            sb.append('.');
        } else {
            sb.append('=');
        }
        sb.append(operationOrAssignment);
        return sb.toString();
    }

    private void handleArrayIndexing(StringBuffer sb) {
        if (indexingExpression != null && indexingExpression.length() > 0) {
            sb.append('[');
            sb.append(indexingExpression);
            sb.append(']');
        }
    }

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
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
    public String getStateVarName() {
        return stateVarName;
    }

    public void setStateVarName(String stateVarName) {
        this.stateVarName = stateVarName;
    }

    @Override
    public String getStateVarType() {
        return stateVarType;
    }

    public void setStateVarType(String stateVarType) {
        this.stateVarType = stateVarType;
    }

    @Override
    public String getIndexingExpression() {
        return indexingExpression;
    }

    public void setIndexingExpression(String idxExpr) {
        this.indexingExpression = idxExpr;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public List<String> getDescriptionArray() {
        return descriptionArray;
    }

    @Override
    public void setDescriptionArray(List<String> descriptionArray) {
        this.descriptionArray = descriptionArray;
    }

    @Override
    public String getArrayType() {
        return arrayType;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getComment() {
        return comment;
    }
}