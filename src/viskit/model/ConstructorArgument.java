package viskit.model;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    static final Logger LOG = LogManager.getLogger();
    
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String value;
    private List<String> comments = new ArrayList<>();
//    private String comment; // obsolete
//    private List<String> descriptionArray = new ArrayList<>(); // obsolete

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
