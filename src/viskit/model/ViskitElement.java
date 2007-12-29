package viskit.model;

import java.util.ArrayList;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 19, 2004
 * @since 2:10:07 PM
 * @version $Id: ViskitElement.java 1662 2007-12-16 19:44:04Z tdnorbra $
 *  
 *  Base class for the objects that get passed around between M, V and C.
 */
abstract public class ViskitElement
{
  public Object opaqueViewObject;       // for private use of V
  public Object opaqueModelObject;      // for private use of M
  public Object opaqueControllerObject; // for private use of C

  protected ViskitElement shallowCopy(ViskitElement newVe)      // shallow copy
  {
    newVe.opaqueControllerObject = this.opaqueControllerObject;
    newVe.opaqueViewObject       = this.opaqueViewObject;
    newVe.opaqueModelObject      = this.opaqueModelObject;
    newVe.modelKey               = this.modelKey;
    return newVe;
  }

  // every node or edge has a unique key
  private static int seqID = 0;
  private Object modelKey = "" + (seqID++);

  public Object getModelKey()
  {
    return modelKey;
  }
  
  public abstract String getName();

  public abstract void setName(String name);
  
  public abstract String getType();
  
  public abstract void setType(String type);
  
  public abstract String getArrayType();
  
  public abstract String getIndexingExpression();
  
  public abstract String getStateVarName();
  
  public abstract String getValue();
  
  public abstract String getComment();
  
  public abstract ArrayList<String> getDescriptionArray();

  public abstract void setDescriptionArray(ArrayList<String> descriptionArray);
  
  public abstract String getOperationOrAssignment();
  
  public abstract boolean isOperation();
  
  public abstract String getStateVarType();
}
