package viskit.model;

import viskit.mvc.MvcModelEvent;

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
 *
 * TODO: Implement enums for this (tdn) 9/14/24
 */
public class ModelEvent extends MvcModelEvent
{
  public static final int NEW_MODEL = 0;

  public static final int SIM_PARAMETER_ADDED    = 1;
  public static final int SIM_PARAMETER_DELETED  = 2;
  public static final int SIM_PARAMETER_CHANGED  = 3;

  public static final int STATE_VARIABLE_ADDED   = 4;
  public static final int STATE_VARIABLE_DELETED = 5;
  public static final int STATE_VARIABLE_CHANGED = 6;

  public static final int EVENT_ADDED            = 7;
  public static final int EVENT_DELETED          = 8;
  public static final int EVENT_CHANGED          = 9;
         
  public static final int EDGE_ADDED             = 10;
  public static final int EDGE_DELETED           = 11;
  public static final int EDGE_CHANGED           = 12;

  public static final int CANCELING_EDGE_ADDED   = 13;
  public static final int CANCELING_EDGE_DELETED = 14;
  public static final int CANCELING_EDGE_CHANGED = 15;

  public static final int CODEBLOCK_CHANGED      = 16;

  public static final int REDO_CANCELING_EDGE    = 34;
  public static final int REDO_SCHEDULING_EDGE   = 35;
  public static final int REDO_EVENT_NODE        = 36;

  public static final int UNDO_CANCELING_EDGE    = 37;
  public static final int UNDO_SCHEDULING_EDGE   = 38;
  public static final int UNDO_EVENT_NODE        = 39;

  // assembly editor:
  public static final int NEW_ASSEMBLY_MODEL     = 17;
  
  public static final int EVENT_GRAPH_ADDED      = 18;
  public static final int EVENT_GRAPH_DELETED    = 19;
  public static final int EVENT_GRAPH_CHANGED    = 20;
  
  public static final int PCL_ADDED              = 21;
  public static final int PCL_DELETED            = 22;
  public static final int PCL_CHANGED            = 23;
  
  public static final int ADAPTER_EDGE_ADDED     = 24;
  public static final int ADAPTER_EDGE_DELETED   = 25;
  public static final int ADAPTER_EDGE_CHANGED   = 26;

  public static final int SIM_EVENT_LISTENER_EDGE_ADDED   = 27;
  public static final int SIM_EVENT_LISTENER_EDGE_DELETED = 28;
  public static final int SIM_EVENT_LISTENER_EDGE_CHANGED = 29;

  public static final int PCL_EDGE_ADDED    = 30;
  public static final int PCL_EDGE_DELETED  = 31;
  public static final int PCL_EDGE_CHANGED  = 32;

  public static final int METADATA_CHANGED  = 33;

  public static final int UNDO_EVENT_GRAPH  = 40;
  public static final int REDO_EVENT_GRAPH  = 41;
  public static final int UNDO_PCL          = 42;
  public static final int REDO_PCL          = 43;
  public static final int UNDO_ADAPTER_EDGE = 44;
  public static final int REDO_ADAPTER_EDGE = 45;
  public static final int UNDO_SIM_EVENT_LISTENER_EDGE = 46;
  public static final int REDO_SIM_EVENT_LISTENER_EDGE = 47;
  public static final int UNDO_PCL_EDGE     = 48;
  public static final int REDO_PCL_EDGE     = 49;

  public ModelEvent(Object obj, int id, String message)
  {
    super(obj,id,message);
  }
}
