package viskit.model;

import java.util.ArrayList;
import java.util.List;
import viskit.VGlobals;

/**
 * Represents the information about one state variable. This
 * includes its name, type, initial value, and current value.
 *
 * @author DMcG
 * @version $Id$
 */
public class vStateVariable extends ViskitElement {

    /** Name of the state variable */
    private String variableName;

    /** The variable type. This can be a primitive or a class name. */
    private String variableType;

    /** array size, for (multi-dim) array */
    private String[] arraySize;

    /** Object that represents its current value */
    private Object currentValue;
    private String comment = "";
    private List<String> descriptionArray = new ArrayList<>();
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String stateVarName;
    private String value;
    private String stateVarType;

    @Override
    public String toString() {
        return "(" + variableType + ") " + variableName;
    }

    /**
     * Constructor
     * @param pVariableName
     * @param pVariableType
     */
    vStateVariable(String pVariableName, String pVariableType) {
        variableName = pVariableName;
        setType(pVariableType);
        currentValue = null;
    }

    public vStateVariable(String nm, String typ, String comment) {
        this(nm, typ);
        this.comment = comment;
    }

    @Override
    public String getName() {
        return variableName;
    }

    @Override
    public void setName(String pVariableName) {
        variableName = pVariableName;
    }

    @Override
    public String getType() {
        return variableType;
    }

    @Override
    public final void setType(String pVariableType) {
        variableType = pVariableType;
        arraySize = VGlobals.instance().getArraySize(pVariableType);
    }

    public String[] getArraySize() {
        return arraySize;
    }

    /**
     * Returns an object that represents the current value of the object.
     * @return the current value of this state variable
     */
    public Object getCurrentValue() {
        return currentValue;
    }

    /**
     * Sets the current value of the state variable
     * @param pCurrentValue
     */
    public void setCurrentValue(Object pCurrentValue) {
        currentValue = pCurrentValue;
    }

    @Override
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
    public String getValue() {
        return value;
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
