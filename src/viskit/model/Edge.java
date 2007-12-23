package viskit.model;

import java.util.ArrayList;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 2:57:37 PM
 * @version $Id: Edge.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public abstract class Edge extends ViskitElement {

    public EventNode to;
    public EventNode from;
    public ArrayList<ViskitElement> parameters;
    public String conditional;
    public String conditionalDescription;
    public String delay;

    abstract Object copyShallow();
}