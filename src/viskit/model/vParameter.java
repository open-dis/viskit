package viskit.model;

import viskit.ViskitGlobals;

/**
 * Class describes a "vParameter" to an event graph--something
 * that is passed into the event graph at runtime. This has
 * a name and type.
 *
 * @author DMcG
 * @version $Id$
 */
public class vParameter extends ViskitElement {

    private String[] arraySize;
    private String   indexingExpression;
    private boolean  operation;
    private String   operationOrAssignment;
    private String   value       = EMPTY;
    private String   comment     = EMPTY;
    private String   description = new String();

    vParameter(String pName, String pType) //package-accessible
    {
        name = pName;
        setType(pType);
    }

    public vParameter(String pName, String pType, String description) //todo make package-accessible
    {
        this(pName, pType);
        this.description = description;
    }

    @Override
    public String toString() {
        return "(" + type + ") " + name;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public final void setType(String pType) {
        this.type = pType;
        arraySize = ViskitGlobals.instance().getArraySize(pType);
    }

    public String[] getArraySize() {
        return arraySize;
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
