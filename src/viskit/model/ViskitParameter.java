package viskit.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.ViskitGlobals;

/**
 * Class describes a "vParameter" to an event graph--something
 * that is passed into the event graph at runtime. This has
 * a name and type.
 *
 * @author DMcG
 * @version $Id$
 */
public class ViskitParameter extends ViskitElement
{
    static final Logger LOG = LogManager.getLogger();
    
    private String value = EMPTY;
//    private String comment = EMPTY; // obsolete
//    private List<String> descriptionArray = new ArrayList<>(); // obsolete
    private String description = EMPTY;
    private String[] arraySize;
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;

    ViskitParameter(String pName, String pType) //package-accessible
    {
        name = pName;
        setType(pType);
    }

    public ViskitParameter(String pName, String pType, String description) //todo make package-accessible
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
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String newDescription) {
        description = newDescription;
        if (description == null)
            description = "";
    }

    // obsolete
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
    public boolean isAssignment() {
        return !operation;
    }
}
