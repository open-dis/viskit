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
    private String description = "";

    public Object getTo() { // TODO use proper class, if possible
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

	@Override
    public String getDescription() {
		if (description == null)
			description = "";
        return description;
    }

	@Override
    public void setDescription(String newDescription) {
        description = newDescription;
    }
}
