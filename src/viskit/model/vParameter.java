package viskit.model;

import java.util.ArrayList;
import viskit.VGlobals;

/**
 * Class describes a "vParameter" to an event graph--something
 * that is passed into the event graph at runtime. This has
 * a name and type.
 *
 * @author DMcG
 * @version $Id: vParameter.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class vParameter extends ViskitElement {

    private String name;
    private String type;
    private String value = "";
    private String comment = "";
    private String arrayType;
    private String[] arraySize;
    private ArrayList<String> descriptionArray = new ArrayList<String>();
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String stateVarName;

    vParameter(String pName, String pType) //package-accessible
    {
        name = pName;
        setType(pType);
    }

    public vParameter(String pName, String pType, String comment) //todo make package-accessible
    {
        this(pName, pType);
        this.comment = comment;
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public String getType() {
        return type;
    }
    
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String cmt) {
        this.comment = cmt;
    }

    public void setType(String pType) {
        type = pType;
        arrayType = VGlobals.instance().stripArraySize(pType);
        arraySize = VGlobals.instance().getArraySize(pType);
    }

    public String getArrayType() {
        return arrayType;
    }

    public String[] getArraySize() {
        return arraySize;
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
    public String getIndexingExpression() {
        return indexingExpression;
    }

    @Override
    public String getStateVarName() {
        return stateVarName;
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
