package viskit.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 9:04:09 AM
 * @version $Id$
 */
public class PropertyChangeEdge extends AssemblyEdge
{
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    static final Logger LOG = LogManager.getLogger();
    
    protected String property;
//    private String comment; // obsolete
//    private List<String> descriptionArray = new ArrayList<>(); // obsolete
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String value;

    PropertyChangeEdge() // package-limited
    {
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String p) {
        property = p;
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
