package viskit.model;

import java.util.ArrayList;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 2:57:37 PM
 * @version $Id: SimEvListenerEdge.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class SimEvListenerEdge extends AssemblyEdge {
    private String name;
    private String type;
    private ArrayList<String> descriptionArray = new ArrayList<String>();

    SimEvListenerEdge() // package-limited
    {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
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
    
    /*
    Object copyShallow()
    {
    AdapterEdge se = new AdapterEdge();
    se.opaqueViewObject = opaqueViewObject;
    se.to = to;
    se.from = from;
    se.parameters = parameters;
    se.delay = delay;
    se.conditional = conditional;
    se.conditionalsComment = conditionalsComment;
    return se;
    }
     */
}
