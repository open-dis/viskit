package viskit.model;

import java.util.ArrayList;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 9:04:09 AM
 * @version $Id: CancellingEdge.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class CancellingEdge extends Edge {
    private String type;
    private ArrayList<String> descriptionArray = new ArrayList<String>();
    private String name;

    CancellingEdge() //package-limited
    {
        parameters = new ArrayList<ViskitElement>();
    }

    Object copyShallow() {
        CancellingEdge ce = new CancellingEdge();
        ce.opaqueViewObject = opaqueViewObject;
        ce.to = to;
        ce.from = from;
        ce.parameters = parameters;
        ce.conditional = conditional;
        ce.delay = delay;
        return ce;
    }
    
    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public ArrayList<String> getDescriptionArray() {
        return descriptionArray;
    }

    @Override
    public void setDescriptionArray(ArrayList<String> descriptionArray) {
        this.descriptionArray = descriptionArray;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
