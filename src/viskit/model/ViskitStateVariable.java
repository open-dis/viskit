package viskit.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;

/**
 * Represents the information about one state variable. This
 * includes its name, type, initial value, and current value.
 *
 * @author DMcG
 * @version $Id$
 */
public class ViskitStateVariable extends ViskitElement
{
    static final Logger LOG = LogManager.getLogger();
    
    /** array size, for (multi-dim) array */
    private String[] arraySize;

    /** Object that represents its current value */
    private Object currentValue;
//    private String comment = EMPTY; // obsolete
//    private List<String> descriptionArray = new ArrayList<>(); // obsolete
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String value;

    /**
     * Constructor
     * @param name of state variable
     * @param type of state variable
     */
    ViskitStateVariable(String name, String type) {
        this.name = name;
        this.type = type;
        currentValue = null;
    }

    /**
     * Constructor
     * @param name of state variable
     * @param type of state variable
     * @param description of state variable
     */
    public ViskitStateVariable(String name, String type, String description) {
        this(name, type);
        this.description = ViskitStatics.emptyIfNull(description);
    }

    @Override
    public String toString() {
        return "(" + type + ") " + name;
    }

    @Override
    public final void setType(String newType) {
        type = newType;
        arraySize = ViskitGlobals.instance().getArraySize(newType);
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
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String newDescription) {
        description = ViskitStatics.emptyIfNull(newDescription);
    }

    // obsolete
//    public void setComment(String comment) {
//        this.comment = comment;
//    }
//
//    @Override
//    public List<String> getDescriptionArray() {
//        return descriptionArray;
//    }
//
//    @Override
//    public void setDescriptionArray(List<String> descriptionArray) {
//        this.descriptionArray = descriptionArray;
//    }

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
    public boolean isAssignment() {
        return !operation;
    }
}
