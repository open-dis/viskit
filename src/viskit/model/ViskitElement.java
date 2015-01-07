package viskit.model;

import java.util.List;

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
abstract public class ViskitElement {

    public Object opaqueViewObject;       // for private use of V
    public Object opaqueModelObject;      // for private use of M
    public Object opaqueControllerObject; // for private use of C

    /** every node or edge has a unique key */
    private static int seqID = 0;
    private Object modelKey = "" + (seqID++);

    protected ViskitElement shallowCopy(ViskitElement newVe) {
        newVe.opaqueControllerObject = this.opaqueControllerObject;
        newVe.opaqueViewObject = this.opaqueViewObject;
        newVe.opaqueModelObject = this.opaqueModelObject;
        newVe.modelKey = this.modelKey;
        return newVe;
    }

    public Object getModelKey() {
        return modelKey;
    }

    /**
     * Returns the name of the node variable.
     *
     * @return name of node variable
     */
    public abstract String getName();

    /**
     * Sets the node variable name.
     *
     * @param name what the node variable name will become
     */
    public abstract void setName(String name);

    /**
     * Returns a string representation of the type of the variable. This may
     * be a primitive type (int, double, float, etc) or an Object (String,
     * container, etc.).
     *
     * @return string representation of the type of the variable
     */
    public abstract String getType();

    /**
     * Sets the type of the node variable. There is no checking that the
     * type is valid; this will happily accept a class name string that
     * does not exist.
     *
     * @param type representation of the type of the state variable
     */
    public abstract void setType(String type);

    public abstract String getArrayType();

    public abstract String getIndexingExpression();

    /**
     * Returns the name of the state variable.
     *
     * @return name of state variable
     */
    public abstract String getStateVarName();

    /**
     * Returns the type of the state variable.
     *
     * @return type of state variable
     */
    public abstract String getStateVarType();

    public abstract String getValue();

    public abstract String getComment();

    public abstract List<String> getDescriptionArray();

    public abstract void setDescriptionArray(List<String> descriptionArray);

    public abstract String getOperationOrAssignment();

    public abstract boolean isOperation();
}
