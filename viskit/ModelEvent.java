package viskit;

import viskit.mvc.mvcModelEvent;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Mar 2, 2004
 * Time: 1:04:35 PM
 */

/**
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

  // assembly editor:
  public static final int NEWASSEMBLYMODEL = 16;

  public static final int EVENTGRAPHADDED = 17;
  public static final int EVENTGRAPHDELETED = 18;
  public static final int EVENTGRAPHCHANGED = 19;

  public static final int PCLADDED = 20;
  public static final int PCLDELETED = 21;
  public static final int PCLCHANGED = 22;

  public static final int ADAPTEREDGEADDED = 23;
  public static final int ADAPTEREDGEDELETED = 24;
  public static final int ADAPTEREDGECHANGED = 25;

  public static final int SIMEVLISTEDGEADDED = 26;
  public static final int SIMEVLISTEDGEDELETED = 27;
  public static final int SIMEVLISTEDGECHANGED = 28;

  public static final int PCLEDGEADDED = 29;
  public static final int PCLEDGEDELETED = 30;
  public static final int PCLEDGECHANGED = 31;

  public ModelEvent(Object obj, int id, String message)
  {
    super(obj,id,message);
  }
}
