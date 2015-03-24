package viskit.model;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 1, 2004
 * @since 9:47:55 AM
 * @version $Id$
 */
public abstract class AssemblyNode extends ViskitElement {

    private Vector<AssemblyEdge> connections = new Vector<>();
    private List<String> comments = new ArrayList<>();
    private Point2D position = new Point2D.Double(0d, 0d);
    private VInstantiator instantiator;
    private String descriptionString = EMPTY;  // instance information

    AssemblyNode(String name, String type) // package access on constructor
    {
        this.name = name;
        this.type = type;
        instantiator = new VInstantiator.FreeF(type, EMPTY);
    }

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    public Vector<AssemblyEdge> getConnections() {
        return connections;
    }

    public void setConnections(Vector<AssemblyEdge> connections) {
        this.connections = connections;
    }

    public Point2D getPosition() {
        return position;
    }

    public void setPosition(Point2D position) {
        this.position = position;
    }

    public VInstantiator getInstantiator() {
        return instantiator;
    }

    public void setInstantiator(VInstantiator instantiator) {
        this.instantiator = instantiator;
    }

    public String getDescriptionString() {
        return descriptionString;
    }

    public void setDescriptionString(String description) {
        this.descriptionString = description;
    }
}
