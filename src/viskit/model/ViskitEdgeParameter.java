package viskit.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Apr 7, 2004
 * @since 3:19:43 PM
 * @version $Id$
 */
public class ViskitEdgeParameter extends ViskitElement 
{
    static final Logger LOG = LogManager.getLogger();
    
    public String bogus; // TODO fix usages

    private String value;
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
//    private String comment; // obsolete
//    private List<String> descriptionArray = new ArrayList<>(); // obsolete
    public ViskitEdgeParameter(String value) {
        this.value = value;
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
