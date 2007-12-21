package viskit.model;

import java.awt.*;
import java.io.File;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 18, 2004
 * @since 1:43:07 PM
 * @version $Id: ViskitModel.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */

public interface ViskitModel
{
  /**
   * Separate initialization from object construction.
   */
  public void init();

  /**
   * Messaged by controller when a new Model should be loaded.
   * @param f File representing persistent model representation.  If null, model resets itself to 0 nodes, 0 edges, etc.
   * @return for good open
   */
  public boolean newModel      (File f);

  /**
   * Save existing model to specified file.  If null, save to last file.  If no last file, error.
   * @param f File to save to.
   */
  public void saveModel     (File f);

  /**
   * @return A File object representing the last one passed to the two methods above.
   */
  public File getLastFile();
  /**
   *  Reports saved state of model.  Becomes "clean" after a save.
   */
  public boolean isDirty       ();

  /**
   * This is messaged by the controller, typically after a newModel(f) message.  It is used to inst a vector of all the
   * nodes in the graph.  Since the EventNode object has src and target members, it also serves to inst all the edges.
   * @return Vector of EventNodes.
   */
  public Vector    getAllNodes ();

  /**
   * Messaged by controller to inst all defined StateVariables.
   * @return Vector of StateVariables.
   */
  public Vector    getStateVariables();
  
  /**
   * Messaged by controller to inst all defined simulation parameters.  Order (may be) important (?), ergo ArrayList container.
   * @return Vector of vParameter objects.
   */
  public Vector getSimParameters();


  /**
   * Message by the controller to create JavaSource from the model.
   * @return The generated source as a String, or null if error.
   */
  public String buildJavaSource();


  // todo further comments...
  public void    newEvent      (String nodeName, Point p);
  public void    newEdge       (EventNode src, EventNode target);
  public void    newCancelEdge (EventNode src, EventNode target);

  public void    deleteEvent      (EventNode node);
  public void    deleteEdge       (SchedulingEdge edge);
  public void    deleteCancelEdge (CancellingEdge edge);

  public void    changeEdge       (SchedulingEdge e);
  public void    changeCancelEdge (CancellingEdge e);
  public void    changeMetaData   (GraphMetaData gmd);
  public boolean changeEvent      (EventNode en);

  public void    newStateVariable    (String name, String type, String initVal, String comment);
  public void    newSimParameter     (String name, String type, String initVal, String comment);
  public boolean changeStateVariable (vStateVariable st);
  public boolean changeSimParameter  (vParameter p);
  public void    changeCodeBlock     (String s);
  public void    deleteStateVariable (vStateVariable sv);
  public void    deleteSimParameter  (vParameter p);

  public GraphMetaData getMetaData();

  public void   setDirty (boolean dirty);

  public String generateLocalVariableName();
  public String generateIndexVariableName();

  public void   resetLVNameGenerator();
  public void   resetIdxNameGenerator();
  
  public String generateStateVariableName();
}
