package viskit.model;

import java.util.ArrayList;
import java.util.List;
import viskit.VGlobals;

/**
 * Class describes a "vParameter" to an event graph--something
 * that is passed into the event graph at runtime. This has
 * a name and type.
 *
 * @author DMcG
 * @version $Id$
 */
public class vParameter extends ViskitElement {

    private String name;
    private String type;
    private String value = "";
    private String comment = "";
    private String[] arraySize;
    private List<String> descriptionArray = new ArrayList<>();
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String stateVarName;
    private String stateVarType;

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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String pName) {
        name = pName;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getComment() {
        return comment;
    }

    public void setComment(String cmt) {
        this.comment = cmt;
    }

    @Override
    public final void setType(String pType) {
        type = pType;
        arraySize = VGlobals.instance().getArraySize(pType);
    }

    public String[] getArraySize() {
        return arraySize;
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

    @Override
    public String getStateVarType() {
        return stateVarType;
    }
}
