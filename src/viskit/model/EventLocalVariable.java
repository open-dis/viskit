package viskit.model;

import java.util.ArrayList;
import java.util.List;
import viskit.VGlobals;

/**
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Apr 1, 2004
 * @since 3:59:01 PM
 * @version $Id$
 */
public class EventLocalVariable extends ViskitElement {

    private String value;
    private String comment = EMPTY;
    private String[] arraySize;
    private List<String> descriptionArray = new ArrayList<>();
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
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
        super.setType(pType);
        arraySize = VGlobals.instance().getArraySize(pType);
    }

    public String[] getArraySize() {
        return arraySize;
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
    public String getOperationOrAssignment() {
        return operationOrAssignment;
    }

    @Override
    public boolean isOperation() {
        return operation;
    }
}
