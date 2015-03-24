package viskit.model;

import java.util.ArrayList;
import java.util.List;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 9:04:09 AM
 * @version $Id$
 */
public class AdapterEdge extends AssemblyEdge {

    private String targetEvent;
    private String sourceEvent;
    private List<String> descriptionArray = new ArrayList<>();
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String value;
    private String comment;

    AdapterEdge() // package-limited
    {
    }

    public String getTargetEvent() {
        return targetEvent;
    }

    public void setTargetEvent(String ev) {
        targetEvent = ev;
    }

    public String getSourceEvent() {
        return sourceEvent;
    }

    public void setSourceEvent(String ev) {
        sourceEvent = ev;
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
    public String getValue() {
        return value;
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
}
