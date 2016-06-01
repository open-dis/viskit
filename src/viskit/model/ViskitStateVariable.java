package viskit.model;

import edu.nps.util.LogUtilities;
import org.apache.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;

/**
 * Represents the information about one state variable. This
 * includes its name, type, initialValue, currentValue and description.
 *
 * @author DMcG
 * @version $Id$
 */
public class ViskitStateVariable extends ViskitElement
{
    static final Logger LOG = LogUtilities.getLogger(ViskitStateVariable.class);
	
    /** array size, for (multi-dim) array */
    private String[] arraySize;

    /** Object that represents its current value */
    private String  value;
    private String  indexingExpression;
    private boolean implicit = false;
    private boolean operation;
    private String  operationOrAssignment;
    private String  comment = EMPTY;
    private String  description = new String();

    /**
     * Constructor
     * @param stateVariableName
     * @param stateVariableType
     */
    ViskitStateVariable(String stateVariableName, String stateVariableType, String initialValue)
	{
        this.name = stateVariableName;
        setType(stateVariableType);
		this.value = initialValue;
		if  (value == null)
			 value = "";
		this.implicit    = false;
        this.description = ViskitStatics.DEFAULT_DESCRIPTION;
    }

//    public ViskitStateVariable(String stateVariableName, String stateVariableType, String initialValue, String description)
//	{
//        this (stateVariableName,stateVariableType, initialValue);
//        this.description  = description; // order is important
//    }

    public ViskitStateVariable(String stateVariableName, String stateVariableType, boolean implicit, String initialValue, String description)
	{
        this (stateVariableName,stateVariableType, initialValue);
		this.implicit = implicit;
        this.description  = description; // order is important
    }

    @Override
    public String toString() 
	{
        return "(" + type + ") " + name;
    }

    @Override
    public final void setType(String newType) 
	{
        type = newType;
        arraySize = ViskitGlobals.instance().getArraySize(newType);
    }

    public String[] getArraySize() 
	{
        return arraySize;
    }

    /**
     * Returns an object that represents the currentValue of the object.
     * @return the currentValue of this state variable
     */
	@Override
    public String getValue() 
	{
        return value;
    }

    /**
     * Sets the currentValue of the state variable
     * @param newValue replacement currentValue to apply
     */
    public void setValue(String newValue) {
        value = newValue;
    }

    @Override
    public String getIndexingExpression() {
        return indexingExpression;
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
    public void setDescription(String newDescription) 
	{
        this.description = newDescription;
    }
	
	/**
	 * "Comment" elements are earlier Viskit constructs and poorly named.
	 * The correct name is "description" for this information item.
	 * If a Comment element is from an earlier model, append the prose as part of new description and then delete.
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

	/**
	 * @return the implicit
	 */
	public boolean isImplicit() {
		return implicit;
	}

	/**
	 * @param implicit the implicit to set
	 */
	public void setImplicit(boolean implicit) {
		this.implicit = implicit;
	}

}
