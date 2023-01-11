package viskit.model;

import edu.nps.util.LogUtilities;
import org.apache.logging.log4j.Logger;

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
abstract public class AssemblyEdge extends ViskitElement 
{
    static final Logger LOG = LogUtilities.getLogger(AssemblyEdge.class);
	
    private Object to, from; // TODO use proper class, if possible
    private String description = "";

    public Object getToObject() 
	{
        return to;
    }

    public void setToObject(Object toObject) 
	{
        to = toObject;
    }

    public Object getFromObject() 
	{
        return from;
    }

    public void setFromObject(Object fromObject) 
	{
        this.from = fromObject;
    }

	@Override
    public String getDescription()
	{
		if (description == null)
			description = "";
        return description;
    }

	@Override
    public void setDescription(String newDescription)
	{
        description = newDescription;
    }
}
