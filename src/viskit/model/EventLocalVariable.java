package viskit.model;

import java.util.ArrayList;
import viskit.VGlobals;

/**
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Apr 1, 2004
 * @since 3:59:01 PM
 * @version $Id: EventLocalVariable.java 1662 2007-12-16 19:44:04Z tdnorbra $
 * To change this template use File | Settings | File Templates.
 */
public class EventLocalVariable extends ViskitElement {

    private String type;
    private String name;
    private String value;
    private String comment = "";
    private String arrayType;
    private String[] arraySize;
    private ArrayList<String> descriptionArray = new ArrayList<String>();
    private String indexingExpression;
    private String stateVarName;
    private boolean operation;
    private String operationOrAssignment;
    private String stateVarType;

    public EventLocalVariable(String name, String type, String value) {
        this.name = name;
        setType(type);
        this.value = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
    
    @Override
    public String getStateVarType() {
        return stateVarType;
    }
}
