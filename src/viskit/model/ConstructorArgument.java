package viskit.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Apr 1, 2004
 * @since 3:57:26 PM
 * @version $Id$
 *
 * To change this template use File | Settings | File Templates.
 */
public class ConstructorArgument extends ViskitElement {
    private String stateVarType;
    private List<String> descriptionArray = new ArrayList<>();
    private String name;
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String stateVarName;
    private String type;
    private String value;
    private List<String> comments = new ArrayList<>();
    private String comment;

    public List getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String name) {
        this.value = name;
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
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
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
    public String getIndexingExpression() {
        return indexingExpression;
    }

    @Override
    public String getStateVarName() {
        return stateVarName;
    }

    @Override
    public String getComment() {
        return comment;
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
    public String getStateVarType() {
        return stateVarType;
    }
}
