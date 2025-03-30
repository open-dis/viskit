package viskit.model;

/**
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Apr 1, 2004
 * @since 3:57:26 PM
 * @version $Id$
 */
public class EventArgument extends ViskitElement
{
    private String description;
//    private List<String> descriptionArray = new ArrayList<>(); // obsolete
    private String value;
    private boolean isOperation;
    private String operationOrAssignmentString;
    private String indexingExpression;

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
        return operationOrAssignmentString;
    }

    @Override
    public boolean isOperation() {
        return isOperation;
    }

    @Override
    public boolean isAssignment() {
        return !isOperation;
    }
}
