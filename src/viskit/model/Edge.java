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

    public EventNode toEventNode;
    public EventNode fromEventNode;
    public List<ViskitElement> parametersList;
    public String condition;
    public String conditionDescription;
    public String delay;

    abstract Object copyShallow();
}