package viskit.model;

import java.util.ArrayList;
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
public class EvGraphNode extends AssemblyNode {

    protected boolean outputMarked = false;
    protected boolean verboseMarked = false;
    private ArrayList<String> descriptionArray = new ArrayList<String>();
    private boolean operation;
    private String operationOrAssignment;
    private String arrayType;
    private String indexingExpression;
    private String stateVarName;
    private String value;
    private String comment;
    private String stateVarType;

    EvGraphNode(String name, String type) // package access on constructor
    {
        super(name, type);
    }

    public boolean isOutputMarked() {
        return outputMarked;
    }

    public void setOutputMarked(boolean outputMarked) {
        this.outputMarked = outputMarked;
    }

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

    public ArrayList<String> getDescriptionArray() {
        return descriptionArray;
    }

    public void setDescriptionArray(ArrayList<String> descriptionArray) {
        this.descriptionArray = descriptionArray;
    }

    public String getArrayType() {
        return arrayType;
    }

    public String getIndexingExpression() {
        return indexingExpression;
    }

    public String getStateVarName() {
        return stateVarName;
    }

    public String getValue() {
        return value;
    }

    public String getComment() {
        return comment;
    }

    public String getOperationOrAssignment() {
        return operationOrAssignment;
    }

    public boolean isOperation() {
        return operation;
    }

    public String getStateVarType() {
        return stateVarType;
    }
}
