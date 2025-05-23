package viskit.model;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 2:57:37 PM
 * @version $Id$
 */
abstract public class AssemblyEdge extends ViskitElement {

    private Object to;
    private Object from;

    public Object getTo() {
        return to;
    }

    public void setTo(Object toObject) {
        to = toObject;
    }

    public Object getFrom() {
        return from;
    }

    public void setFrom(Object fromObject) {
        from = fromObject;
    }
}
