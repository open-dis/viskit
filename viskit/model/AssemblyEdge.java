package viskit.model;

import java.util.ArrayList;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Mar 8, 2004
 * Time: 2:57:37 PM
 */

abstract public class AssemblyEdge extends ViskitElement
{
  private Object to; //PropChangeListenerNode to;
  private Object from;
  private String comment;

/*
  private String name;

  public String getName(){return name;}
  public void   setName(String n){name=n;}
*/

  public String getComment(){return comment;}
  public void   setComment(String c){comment=c;}

  public Object getTo(){return to;}
  public void   setTo(Object t){to=t;}

  public Object getFrom(){return from;}
  public void   setFrom(Object f){from=f;}
}
