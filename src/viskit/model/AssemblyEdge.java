package viskit.model;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 2:57:37 PM
 * @version $Id: AssemblyEdge.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
abstract public class AssemblyEdge extends ViskitElement {

    private Object to;
    private Object from;
    private String description = "";

    public Object getTo() {
        return to;
    }

    public void setTo(Object t) {
        to = t;
    }

    public Object getFrom() {
        return from;
    }

    public void setFrom(Object f) {
        from = f;
    }

    public String getDescriptionString() {
        return description;
    }

    public void setDescriptionString(String d) {
        description = d;
    }
}
