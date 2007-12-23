package viskit.model;

import java.util.ArrayList;
import viskit.xsd.bindings.assembly.SimEntity;

import java.awt.*;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 9:08:08 AM
 * @version $Id: EvGraphNode.java 1662 2007-12-16 19:44:04Z tdnorbra $
 *
 * An event as seen by the model (not the view)
 */
public class EvGraphNode extends AssemblyNode {

    protected boolean outputMarked = false;
    private ArrayList<String> descriptionArray = new ArrayList<String>();
    private boolean operation;
    private String operationOrAssignment;
    private String arrayType;
    private String indexingExpression;
    private String stateVarName;
    private String value;
    private String comment;

    EvGraphNode(String name, String type) // package access on constructor
    {
        super(name, type);
    }

    /*
    public EvGraphNode shallowCopy()
    {
    EvGraphNode en   = (EvGraphNode)super.shallowCopy(new EvGraphNode(name+"-copy",type));
    en.connections = connections;
    en.comments    = comments;
    en.connections = connections;
    return en;
    }
     */
    public boolean isOutputMarked() {
        return outputMarked;
    }

    public void setOutputMarked(boolean outputMarked) {
        this.outputMarked = outputMarked;
    }

    @Override
    public String getName() {
        /*
        if(this.opaqueModelObject != null)
        return ((Event)opaqueModelObject).getName();
        else
         */
        return super.getName();
    }

    @Override
    public void setName(String s) {
        if (this.opaqueModelObject != null) {
            ((SimEntity) opaqueModelObject).setName(s);
        }

        super.setName(s);
    }

    @Override
    public ArrayList<String> getDescriptionArray() {
        return descriptionArray;
    }

    @Override
    public void setDescriptionArray(ArrayList<String> descriptionArray) {
        this.descriptionArray = descriptionArray;
    }

    @Override
    public String getArrayType() {
        return arrayType;
    }

    @Override
    public String getIndexingExpression() {
        return indexingExpression;
    }

    @Override
    public String getStateVarName() {
        return stateVarName;
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
