package viskit.model;

import viskit.ViskitGlobals;

/**
 * Represents the information about one state variable. This
 * includes its name, type, initial value, current value and description.
 *
 * @author DMcG
 * @version $Id$
 */
public class vStateVariable extends ViskitElement {

    /** array size, for (multi-dim) array */
    private String[] arraySize;

    /** Object that represents its current value */
    private Object  currentValue;
    private String  indexingExpression;
    private boolean operation;
    private String  operationOrAssignment;
    private String  value;
    private String  comment = EMPTY;
    private String  description = new String();

    /**
     * Constructor
     * @param pVariableName
     * @param pVariableType
     */
    vStateVariable(String pVariableName, String pVariableType) {
        name = pVariableName;
        setType(pVariableType);
        currentValue = null;
    }

    public vStateVariable(String name, String type, String description) {
        this(name, type);
        this.description = description;
    }

    @Override
    public String toString() {
        return "(" + type + ") " + name;
    }

    @Override
    public final void setType(String pVariableType) {
        type = pVariableType;
        arraySize = ViskitGlobals.instance().getArraySize(pVariableType);
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
     * @param pCurrentValue replacement value to apply
     */
    public void setCurrentValue(Object pCurrentValue) {
        currentValue = pCurrentValue;
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
    public String getOperationOrAssignment() {
        return operationOrAssignment;
    }

    @Override
    public boolean isOperation() {
        return operation;
    }

	@Override
    public String getDescription() 
	{		
		moveLegacyCommentsToDescription ();
        return description;
    }

    @Override
    public void setDescription(String newDescription) {
        this.description = newDescription;
    }
	
	/**
	 * "Comment" elements are earlier viskit constructs.
	 * If found from an earlier model, append them as part of description and then delete.
	 */
	private void moveLegacyCommentsToDescription ()
	{
		if (description == null)
			description = new String();
		if ((comment != null) && !comment.isEmpty())
		{
			description = comment.trim();
			comment     = "";
		}
	}

    @Deprecated
    @Override
    public String getComment()
	{
		moveLegacyCommentsToDescription ();
        return description;
    }
}
