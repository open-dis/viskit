package viskit.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.xsd.bindings.assembly.SimEntity;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 9:08:08 AM
 * @version $Id$
 *
 * An event as seen by the model (not the view)
 */
public class EventGraphNode extends AssemblyNode
{
    static final Logger LOG = LogManager.getLogger();
    
    protected boolean outputMarked = false;
    protected boolean verboseMarked = false;
//    private String comment; // obsolete
//    private List<String> descriptionArray = new ArrayList<>(); // obsolete
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String value;

    EventGraphNode(String name, String type) // package access on constructor
    {
        super(name, type);
    }

    /** Enable an extra verbose dump of all entity details
     *
     * @return indication of the detailed checkbox selected for the entity
     * on the EventGraphNodeInspectorDialog
     */
    public boolean isOutputMarked() {
        return outputMarked;
    }

    public void setOutputMarked(boolean outputMarked) {
        this.outputMarked = outputMarked;
    }

    /** NOT USED
     *
     * @return indication of the verbose checkbox selected for the entity
     * on the EventGraphNodeInspectorDialog
     */
    public boolean isVerboseMarked() {
        return verboseMarked;
    }

    public void setVerboseMarked(boolean verboseMarked) {
        this.verboseMarked = verboseMarked;
    }

    @Override
    public void setName(String s) {
        if (this.opaqueModelObject != null) {
            ((SimEntity) opaqueModelObject).setName(s);
        }

        super.setName(s);
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
