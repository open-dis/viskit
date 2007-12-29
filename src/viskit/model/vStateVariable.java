package viskit.model;

import java.util.ArrayList;
import viskit.VGlobals;

/**
 * Represents the information about one state variable. This
 * includes its name, type, initial value, and current value.
 *
 * @author DMcG
 * @version $Id: vStateVariable.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class vStateVariable extends ViskitElement {

    /** Name of the state variable */
    private String variableName;
    /** The variable type. This can be a primitive or a class name. */
    private String variableType;
    /** The above field holds the size within the brackets **/
    private String arrayVariableType;
    /** array size, for (multi-dim) array */
    private String[] arraySize;
    /** Object that represents its current value */
    private Object currentValue;
    private String comment = "";
    private ArrayList<String> descriptionArray = new ArrayList<String>();
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

    /**
     * Returns the name of the state variable.
     *
     * @return name of state variable
     */
    public String getName() {
        return variableName;
    }

    /**
     * Sets the state variable name.
     *
     * @param pVariableName what the state variable name will become
     */
    public void setName(String pVariableName) {
        variableName = pVariableName;
    }

    /**
     * Returns a string representation of the type of the variable. This may
     * be a primitive type (int, double, float, etc) or an Object (String,
     * container, etc.).
     *
     * @return string represenatation of the type of the variable
     */
    public String getType() {
        return variableType;
    }

    /**
     * Sets the type of the state variable. There is no checking that the
     * type is valid; this will happily accept a class name string that
     * does not exist.
     *
     * @param pVariableType represenation of the type of the state variable
     */
    public void setType(String pVariableType) {
        variableType = pVariableType;
        arrayVariableType = VGlobals.instance().stripArraySize(pVariableType);
        arraySize = VGlobals.instance().getArraySize(pVariableType);
    }

    public String getArrayType() {
        return arrayVariableType;
    }

    public String[] getArraySize() {
        return arraySize;
    }

    /**
     * Returns an object that represents the current value of the object.
     */
    public Object getCurrentValue() {
        return currentValue;
    }

    /**
     * Sets the current value of the state variable
     */
    public void setCurrentValue(Object pCurrentValue) {
        currentValue = pCurrentValue;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
