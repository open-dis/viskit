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
public class CancelingEdge extends Edge // single "l" is preferred American spelling
{
    static final Logger LOG = LogManager.getLogger();
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String value;
//    private String comment; // obsolete
//    private List<String> descriptionArray = new ArrayList<>(); // obsolete

    CancelingEdge() //package-limited
    {
    }

    @Override
    Object copyShallow() 
    {
        CancelingEdge ce = new CancelingEdge();
        ce.opaqueViewObject = opaqueViewObject;
        ce.setTo(getTo());
        ce.setFrom(getFrom());
        ce.setParameters(getParameters());
        ce.setConditional(getConditional());
        ce.setDelay(getDelay());
        ce.setDescription(getDescription());
        return ce;
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
