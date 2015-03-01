package viskit.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Apr 7, 2004
 * @since 3:19:43 PM
 * @version $Id$
 */
public class vEdgeParameter extends ViskitElement {

    private String stateVarType;
    private List<String> descriptionArray = new ArrayList<>();
    private String type;
    private String name;
    public String bogus; //todo fix
    private String value;
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String stateVarName;
    private String comment;

    public vEdgeParameter(String value) {
        this.value = value;
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
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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
