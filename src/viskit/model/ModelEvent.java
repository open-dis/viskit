package viskit.model;

import edu.nps.util.LogUtilities;
import org.apache.logging.log4j.Logger;
import viskit.mvc.mvcModelEvent;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 2, 2004
 * @since 1:04:35 PM
 * @version $Id$
 *
 * This defines every event with which the application Model informs its listeners. Typically
 * this is the view.
 */
public class ModelEvent extends mvcModelEvent
{
    static final Logger LOG = LogUtilities.getLogger(ModelEvent.class);
	
	public static final int NEWMODEL = 0;

	public static final int SIMPARAMETER_ADDED    = 1;
	public static final int SIMPARAMETER_DELETED  = 2;
	public static final int SIMPARAMETER_CHANGED  = 3;

	public static final int STATEVARIABLE_ADDED   = 4;
	public static final int STATEVARIABLE_DELETED = 5;
	public static final int STATEVARIABLE_CHANGED = 6;

	public static final int EVENT_ADDED           = 7;
	public static final int EVENT_DELETED         = 8;
	public static final int EVENT_CHANGED         = 9;

	public static final int EDGE_ADDED            = 10;
	public static final int EDGE_DELETED          = 11;
	public static final int EDGE_CHANGED          = 12;

	public static final int CANCELLING_EDGE_ADDED  = 13;
	public static final int CANCELLING_EDGE_DELETED= 14;
	public static final int CANCELLING_EDGE_CHANGED= 15;

	public static final int CODEBLOCK_CHANGED     = 16;

	public static final int REDO_CANCELLING_EDGE  = 34;
	public static final int REDO_SCHEDULING_EDGE  = 35;
	public static final int REDO_EVENT_NODE       = 36;

	public static final int UNDO_CANCELLING_EDGE  = 37;
	public static final int UNDO_SCHEDULING_EDGE  = 38;
	public static final int UNDO_EVENT_NODE       = 39;

	// assembly editor:
	public static final int NEW_ASSEMBLY_MODEL      = 17;

	public static final int EVENTGRAPH_ADDED      = 18;
	public static final int EVENTGRAPH_DELETED    = 19;
	public static final int EVENTGRAPH_CHANGED    = 20;

	public static final int PCL_ADDED             = 21;
	public static final int PCL_DELETED           = 22;
	public static final int PCL_CHANGED           = 23;

	public static final int ADAPTER_EDGE_ADDED     = 24;
	public static final int ADAPTER_EDGE_DELETED   = 25;
	public static final int ADAPTER_EDGE_CHANGED   = 26;

	public static final int SIMEVENT_LISTENER_EDGE_ADDED   = 27;
	public static final int SIMEVENT_LISTENER_EDGE_DELETED = 28;
	public static final int SIMEVENT_LISTENER_EDGE_CHANGED = 29;

	public static final int PCL_EDGE_ADDED        = 30;
	public static final int PCL_EDGE_DELETED      = 31;
	public static final int PCL_EDGE_CHANGED      = 32;

	public static final int METADATA_CHANGED     = 33;

	public static final int UNDO_EVENT_GRAPH     = 40;
	public static final int REDO_EVENT_GRAPH     = 41;
	public static final int UNDO_PCL             = 42;
	public static final int REDO_PCL             = 43;
	public static final int UNDO_ADAPTER_EDGE    = 44;
	public static final int REDO_ADAPTER_EDGE    = 45;
	public static final int UNDO_SIMEVENT_LISTENER_EDGE = 46;
	public static final int REDO_SIMEVENT_LISTENER_EDGE = 47;
	public static final int UNDO_PCL_EDGE        = 48;
	public static final int REDO_PCL_EDGE        = 49;

	public ModelEvent(Object object, int id, String message)
	{
	  super(object,id,message);
	}
  
	public static String toString (int eventCode)
	{
	  switch (eventCode)
	  {
		  case NEWMODEL:
			   return "NEWMODEL";
		  case SIMPARAMETER_ADDED:
			   return "SIMPARAMETER_ADDED";
		  case SIMPARAMETER_DELETED:
			   return "SIMPARAMETER_DELETED";
		  case SIMPARAMETER_CHANGED:
			   return "SIMPARAMETER_CHANGED";

		  case STATEVARIABLE_ADDED:
			   return "STATEVARIABLE_ADDED";
		  case STATEVARIABLE_DELETED:
			   return "STATEVARIABLE_DELETED";
		  case STATEVARIABLE_CHANGED:
			   return "STATEVARIABLE_CHANGED";

		  case EVENT_ADDED:
			   return "EVENT_ADDED";
		  case EVENT_DELETED:
			   return "EVENT_DELETED";
		  case EVENT_CHANGED:
			   return "EVENT_CHANGED";

		  case EDGE_ADDED:
			   return "EDGE_ADDED";
		  case EDGE_DELETED:
			   return "EDGE_DELETED";
		  case EDGE_CHANGED:
			   return "EDGE_CHANGED";

		  case CANCELLING_EDGE_ADDED:
			   return "CANCELLING_EDGE_ADDED";
		  case CANCELLING_EDGE_DELETED:
			   return "CANCELLING_EDGE_DELETED";
		  case CANCELLING_EDGE_CHANGED:
			   return "CANCELLINGE_DGE_CHANGED";

		  case CODEBLOCK_CHANGED:
			   return "CODEBLOCK_CHANGED";

		  case REDO_CANCELLING_EDGE:
			   return "REDO_CANCELLING_EDGE";
		  case REDO_SCHEDULING_EDGE:
			   return "REDO_SCHEDULING_EDGE";
		  case REDO_EVENT_NODE:
			   return "REDO_EVENT_NODE";

		  case UNDO_CANCELLING_EDGE:
			   return "UNDO_CANCELLING_EDGE";
		  case UNDO_SCHEDULING_EDGE:
			   return "UNDO_SCHEDULING_EDGE";
		  case UNDO_EVENT_NODE :
			   return "UNDO_EVENT_NODE";

		  // assembly editor:
		  case NEW_ASSEMBLY_MODEL:
			   return "NEW_ASSEMBLY_MODEL";

		  case EVENTGRAPH_ADDED:
			   return "EVENTGRAPH_ADDED";
		  case EVENTGRAPH_DELETED:
			   return "EVENTGRAPH_DELETED";
		  case EVENTGRAPH_CHANGED:
			   return "EVENTGRAPH_CHANGED";

		  case PCL_ADDED:
			   return "PCL_ADDED";
		  case PCL_DELETED:
			   return "PCL_DELETED";
		  case PCL_CHANGED:
			   return "PCL_CHANGED";

		  case ADAPTER_EDGE_ADDED:
			   return "ADAPTER_EDGE_ADDED";
		  case ADAPTER_EDGE_DELETED:
			   return "ADAPTER_EDGE_DELETED";
		  case ADAPTER_EDGE_CHANGED:
			   return "ADAPTER_EDGE_CHANGED";

		  case SIMEVENT_LISTENER_EDGE_ADDED:
			   return "SIMEVENT_LISTENER_EDGE_ADDED";
		  case SIMEVENT_LISTENER_EDGE_DELETED:
			   return "SIMEVENT_LISTENER_EDGE_DELETED";
		  case SIMEVENT_LISTENER_EDGE_CHANGED:
			   return "SIMEVENT_LISTENER_EDGE_CHANGED";

		  case PCL_EDGE_ADDED:
			   return "PCL_EDGE_ADDED";
		  case PCL_EDGE_DELETED:
			   return "PCL_EDGE_DELETED";
		  case PCL_EDGE_CHANGED:
			   return "PCL_EDGE_CHANGED";

		  case METADATA_CHANGED:
			   return "METADATA_CHANGED";

		  case UNDO_EVENT_GRAPH:
			   return "UNDO_EVENT_GRAPH";
		  case REDO_EVENT_GRAPH:
			   return "REDO_EVENT_GRAPH";
		  case UNDO_PCL:
			   return "UNDO_PCL";
		  case REDO_PCL:
			   return "REDO_PCL";
		  case UNDO_ADAPTER_EDGE:
			   return "UNDO_ADAPTER_EDGE";
		  case REDO_ADAPTER_EDGE:
			   return "REDO_ADAPTER_EDGE";
		  case UNDO_SIMEVENT_LISTENER_EDGE:
			   return "UNDO_SIMEVENT_LISTENER_EDGE";
		  case REDO_SIMEVENT_LISTENER_EDGE:
			   return "REDO_SIMEVENT_LISTENER_EDGE";
		  case UNDO_PCL_EDGE:
			   return "UNDO_PCL_EDGE";
		  case REDO_PCL_EDGE:
			   return "REDO_PCL_EDGE";
		  default:
			   return "ILLEGAL_MODELEVENT_CODE";
		}
	}
}
