package viskit;

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

  public static final int SIMPARAMETERADDED = 1;
  public static final int SIMPARAMETERDELETED = 2;
  public static final int SIMPARAMETERCHANGED = 3;

  public static final int STATEVARIABLEADDED = 4;
  public static final int STATEVARIABLEDELETED = 5;
  public static final int STATEVARIABLECHANGED = 6;

  public static final int EVENTADDED = 7;
  public static final int EVENTDELETED = 8;
  public static final int EVENTCHANGED = 9;

  public static final int EDGEADDED = 10;
  public static final int EDGEDELETED = 11;
  public static final int EDGECHANGED = 12;

  public static final int CANCELLINGEDGEADDED = 13;
  public static final int CANCELLINGEDGEDELETED = 14;
  public static final int CANCELLINGEDGECHANGED = 15;

  public static final int CODEBLOCKCHANGED = 16;

  // assembly editor:
  public static final int NEWASSEMBLYMODEL = 17;

  public static final int EVENTGRAPHADDED = 18;
  public static final int EVENTGRAPHDELETED = 19;
  public static final int EVENTGRAPHCHANGED = 20;

  public static final int PCLADDED = 21;
  public static final int PCLDELETED = 22;
  public static final int PCLCHANGED = 23;

  public static final int ADAPTEREDGEADDED = 24;
  public static final int ADAPTEREDGEDELETED = 25;
  public static final int ADAPTEREDGECHANGED = 26;

  public static final int SIMEVLISTEDGEADDED = 27;
  public static final int SIMEVLISTEDGEDELETED = 28;
  public static final int SIMEVLISTEDGECHANGED = 29;

  public static final int PCLEDGEADDED = 30;
  public static final int PCLEDGEDELETED = 31;
  public static final int PCLEDGECHANGED = 32;

  public ModelEvent(Object obj, int id, String message)
  {
    super(obj,id,message);
  }
}
