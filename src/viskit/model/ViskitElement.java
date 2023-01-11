package viskit.model;

import edu.nps.util.LogUtilities;
import org.apache.logging.log4j.Logger;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 19, 2004
 * @since 2:10:07 PM
 * @version $Id$
 *
 * Base class for the objects that get passed around between M, V and C.
 */
abstract public class ViskitElement implements Comparable<ViskitElement>
{
    static final Logger LOG = LogUtilities.getLogger(ViskitElement.class);

    /** an object provided for private use of View */
	public Object opaqueViewObject;       
	/** an object provided for private use of Model */
    public Object opaqueModelObject;

    /** NOT USED. an object provided for private use of Controller */
    public Object opaqueControllerObject;

    protected static final String EMPTY = "";
    protected String type       = EMPTY;
    protected String name       = EMPTY;

    /** every node or edge instance has a unique key */
    private static int sequenceID = 0;
    private Object modelKey = EMPTY + (sequenceID++); // TODO placeholder, does type change?  or is it always a String?

    protected ViskitElement shallowCopy(ViskitElement newViskitElement)
	{
        newViskitElement.opaqueControllerObject = this.opaqueControllerObject;
        newViskitElement.opaqueViewObject       = this.opaqueViewObject;
        newViskitElement.opaqueModelObject      = this.opaqueModelObject;
        newViskitElement.modelKey               = this.modelKey;
		
        return newViskitElement;
    }

    public Object getModelKey()
	{
        return modelKey;
    }

    @Override
    public int compareTo(ViskitElement e) {
        return getType().compareTo(e.getType());
    }

    /**
     * Returns the name of the node variable.
     *
     * @return name of node variable
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the node variable name.
     *
     * @param name what the node variable name will become
     */
    public void setName(String name){
        this.name = name;
    }

    /**
     * Returns a string representation of the type of the variable. This may
     * be a primitive type (int, double, float, etc) or an Object (String,
     * container, etc.).
     *
     * @return string representation of the type of the variable
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the node variable. There is no checking that the
     * type is valid; this will happily accept a class name string that
     * does not exist.
     *
     * @param type representation of the type of the state variable
     */
    public void setType(String type) {
        this.type = type;
    }

    public abstract String getIndexingExpression();

    public abstract String getValue();

	@Deprecated
    public abstract String getComment();

    public abstract String getDescription();

    public abstract void setDescription(String description);

    public abstract String getOperationOrAssignment();

    public abstract boolean isOperation();

} // end class file ViskitElement.java
