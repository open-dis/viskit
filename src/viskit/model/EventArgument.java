package viskit.model;

import org.apache.logging.log4j.LogManager;
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
    static final Logger LOG = LogManager.getLogger();
   
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
