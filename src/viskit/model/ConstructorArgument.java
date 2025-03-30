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
 * TODO: Not currently used
 */
public class ConstructorArgument extends ViskitElement 
{
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String value;
    private List<String> comments = new ArrayList<>();
//    private String comment; // obsolete
//    private List<String> descriptionArray = new ArrayList<>(); // obsolete
    private String description;

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

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getIndexingExpression() {
        return indexingExpression;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String newDescription) {
        description = newDescription;
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

    @Override
    public String getOperationOrAssignment() {
        return operationOrAssignment;
    }

    @Override
    public boolean isOperation() {
        return operation;
    }

    @Override
    public boolean isAssignment() {
        return !operation;
    }
}
