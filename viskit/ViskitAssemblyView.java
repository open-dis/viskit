package viskit;

import viskit.model.*;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: Mar 18, 2004
 * Time: 12:06:11 PM
 */

/**
 * The MVC design of Viskit means that the ViskitModel and the ViskitView know about the
 * chosen view only as much as is described by this interface.
 */

public interface ViskitAssemblyView
{

  // permit user to edit existing entities
  public boolean doEditPclNode(PropChangeListenerNode pclNode);
  public boolean doEditEvGraphNode(EvGraphNode evNode);

  public boolean doEditPclEdge(PropChangeEdge pclEdge);
  public boolean doEditAdapterEdge(AdapterEdge aEdge);
  public boolean doEditSimEvListEdge(SimEvListenerEdge seEdge);

  public int     genericAsk             ( String title, String prompt );      // returns JOptionPane constants
  public void    genericErrorReport     ( String title, String message );
  public String  promptForStringOrCancel( String title, String message, String initval);

  public File    openFileAsk();
  public File    saveFileAsk(String suggNameNoType);

  public void    fileName(String s);    // informative, tells view what we're working on
  public void    setStopTime(String s);
  public void    setVerbose(boolean v);
  public String  getStopTime();
  public boolean getVerbose();

  public void    showAndSaveSource(String s);

}
