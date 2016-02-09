package viskit.model;

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

  public static final int CANCELLINGEDGE_ADDED  = 13;
  public static final int CANCELLINGEDGE_DELETED= 14;
  public static final int CANCELLINGEDGE_CHANGED= 15;

  public static final int CODEBLOCK_CHANGED     = 16;

  public static final int REDO_CANCELLING_EDGE  = 34;
  public static final int REDO_SCHEDULING_EDGE  = 35;
  public static final int REDO_EVENT_NODE       = 36;

  public static final int UNDO_CANCELLING_EDGE  = 37;
  public static final int UNDO_SCHEDULING_EDGE  = 38;
  public static final int UNDO_EVENT_NODE       = 39;

  // assembly editor:
  public static final int NEWASSEMBLYMODEL      = 17;

  public static final int EVENTGRAPH_ADDED      = 18;
  public static final int EVENTGRAPH_DELETED    = 19;
  public static final int EVENTGRAPH_CHANGED    = 20;

  public static final int PCL_ADDED             = 21;
  public static final int PCL_DELETED           = 22;
  public static final int PCL_CHANGED           = 23;

  public static final int ADAPTEREDGE_ADDED     = 24;
  public static final int ADAPTEREDGE_DELETED   = 25;
  public static final int ADAPTEREDGE_CHANGED   = 26;

  public static final int SIMEVENTLISTEDGE_ADDED   = 27;
  public static final int SIMEVENTLISTEDGE_DELETED = 28;
  public static final int SIMEVENTLISTEDGE_CHANGED = 29;

  public static final int PCLEDGE_ADDED        = 30;
  public static final int PCLEDGE_DELETED      = 31;
  public static final int PCLEDGE_CHANGED      = 32;

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
}
