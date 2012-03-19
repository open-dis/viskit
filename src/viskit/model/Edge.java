package viskit.model;

import java.util.List;

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
public abstract class Edge extends ViskitElement {

    public EventNode to;
    public EventNode from;
    public List<ViskitElement> parameters;
    public String conditional;
    public String conditionalDescription;
    public String delay;

    abstract Object copyShallow();
}