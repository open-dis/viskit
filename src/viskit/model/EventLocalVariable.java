package viskit.model;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.ViskitGlobals;

/**
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Apr 1, 2004
 * @since 3:59:01 PM
 * @version $Id$
 */
public class EventLocalVariable extends ViskitElement 
{
    static final Logger LOG = LogManager.getLogger();
    
    private String value;
    private String[] arraySize;
//    private String description = EMPTY; // obsolete
//    private List<String> descriptionArray = new ArrayList<>(); // obsolete
    private String description = EMPTY;
    private String indexingExpression;
    private boolean operation;
    private String operationOrAssignment;

    public EventLocalVariable(String name, String type, String value) {
        this.name = name;
        setType(type);
        this.value = value;
    }

    @Override
    public String toString() {
        return (type.isEmpty() && name.isEmpty()) ? EMPTY : "(" + type + ") " + name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String newDescription) {
        this.description = newDescription;
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
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public final void setType(String pType) {
        super.setType(pType);
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
