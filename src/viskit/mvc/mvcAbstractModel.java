package viskit.mvc;

import edu.nps.util.LogUtilities;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.model.ModelEvent;

/**
 * Abstract root class of the model hierarchy.  Provides basic notification behavior.
 *
 * From an article at www.jaydeetechnology.co.uk
 *
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 2, 2004 : 11:04:25 AM
 * @version $Id$
 */
public abstract class mvcAbstractModel implements mvcModel 
{
    private final List<mvcModelListener> listenersList;

    static final Logger LOG = LogUtilities.getLogger(mvcAbstractModel.class);

	public mvcAbstractModel() 
	{
		this.listenersList = new ArrayList<>(8);
	}

    @Override
    public void notifyChanged(mvcModelEvent modelEvent)
	{		
		String eventID = Integer.toString(modelEvent.getID());
		if (modelEvent.getID() < 10)
			eventID = " " + eventID; // spacing for readability
		
        for (mvcModelListener modelListener : listenersList) 
		{
            modelListener.modelChanged(modelEvent);
        }
		if (listenersList.isEmpty())
		{
			LOG.error ("MVC modelEvent " + eventID + "=" + ModelEvent.toString(modelEvent.getID()) + " ignored, listeners list is empty (\""                    + modelEvent.getActionCommand() + "\")");
		}
		else if (ViskitGlobals.LOG_EVENT_NOTIFICATIONS == true)
		{
			LOG.info  ("MVC modelEvent " + eventID + "=" + ModelEvent.toString(modelEvent.getID()) + " received by " + listenersList.size() + " listeners (\"" + modelEvent.getActionCommand() + "\")");
		}
    }

    public void addModelListener(mvcModelListener modelListener)
	{
        if (!listenersList.contains(modelListener)) 
		{
            listenersList.add(modelListener);
			LOG.info  ("MVC " + this.getClass() +    ".addModelListener(" + modelListener.getClass() + ")");
        }
    }

    public void removeModelListener(mvcModelListener modelListener)
	{
        listenersList.remove(modelListener);
		LOG.info      ("MVC " + this.getClass() + ".removeModelListener(" + modelListener.getClass() + ")");
    }
}
