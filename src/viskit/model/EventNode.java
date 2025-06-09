package viskit.model;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.xsd.bindings.eventgraph.Event;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 9:08:08 AM
 * @version $Id$
 *
 * An event as seen by the model (not the view)
 */
public class EventNode extends ViskitElement 
{
    static final Logger LOG = LogManager.getLogger();
    
    private Vector<ViskitElement> connectionsVector  = new Vector<>();
    private List<ViskitElement> localVariablesList   = new ArrayList<>();
    private List<ViskitElement> stateTransitionsList = new ArrayList<>();
    private List<ViskitElement> argumentsList        = new ArrayList<>();
//    private List<String> comments = new ArrayList<>(); // obsolete
//    private List<String> descriptionArray = new ArrayList<>(); // obsolete
    private Point2D position = new Point2D.Double(0.d, 0.d);
    private String codeBlockString = EMPTY;
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String value;

    EventNode(String name) // package access on constructor
    {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public EventNode shallowCopy()
    {
        EventNode newEventNode = (EventNode) super.shallowCopy(new EventNode(name + "-copy"));
        newEventNode.connectionsVector = connectionsVector;
//        eventNode.comments = comments; // obsolete
//        eventNode.descriptionArray = descriptionArray; // obsolete
        newEventNode.description = description;
        newEventNode.stateTransitionsList = stateTransitionsList;
        newEventNode.localVariablesList = localVariablesList;
        newEventNode.argumentsList = argumentsList;
        newEventNode.codeBlockString = codeBlockString;
        return newEventNode;
    }

    @Override
    public void setName(String s) {
        if (this.opaqueModelObject != null) {
            ((Event) opaqueModelObject).setName(s);
        }
        this.name = s;
    }

    public List<ViskitElement> getArguments() {
        return argumentsList;
    }

    public void setArguments(List<ViskitElement> arguments) {
        this.argumentsList = arguments;
    }

//    public List<String> getComments() {
//        return comments;
//    }
//
//    public void setComments(List<String> comments) {
//        this.comments = comments;
//    }

    public void setCodeBlockString(String newCodeBlockString) {
        this.codeBlockString = newCodeBlockString;
    }

    public String getSourceCodeBlockString() {
        return codeBlockString;
    }

    public Vector<ViskitElement> getConnections() {
        return connectionsVector;
    }

    public void setConnections(Vector<ViskitElement> newConnectionsVector) {
        this.connectionsVector = newConnectionsVector;
    }

    public List<ViskitElement> getLocalVariables() {
        return localVariablesList;
    }

    public void setLocalVariables(List<ViskitElement> newLocalVariablesList) {
        this.localVariablesList = newLocalVariablesList;
    }

    public List<ViskitElement> getStateTransitions() {
        return stateTransitionsList;
    }

    public void setStateTransitions(List<ViskitElement> newStateTransitionsList) {
        this.stateTransitionsList = newStateTransitionsList;
    }

    public Point2D getPosition() {
        return position;
    }

    public void setPosition(Point2D position) {
        this.position = position;
    }

    @Override
    public String getIndexingExpression() {
        return indexingExpression;
    }

    @Override
    public String getValue() {
        return value;
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
    public String getOperationOrAssignment()
    {
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
