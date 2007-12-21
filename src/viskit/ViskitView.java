package viskit;

import viskit.model.*;

import java.io.File;
import java.util.Collection;

/**
 * The MVC design of Viskit means that the ViskitModel and the ViskitView know about the
 * chosen view only as much as is described by this interface.
 * 
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Mar 18, 2004
 * @since 12:06:11 PM
 * @version $Id: ViskitView.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public interface ViskitView
{
  // permit user to edit existing entities
  public boolean doEditCancelEdge   ( CancellingEdge edge );
  public boolean doEditEdge         ( SchedulingEdge edge );
  public boolean doEditNode         ( EventNode node );
  public boolean doEditParameter    ( vParameter param );
  public boolean doEditStateVariable( vStateVariable var);

  public int     genericAsk             ( String title, String prompt );      // returns JOptionPane constants
  public void    genericErrorReport     ( String title, String message );
  public String  promptForStringOrCancel( String title, String message, String initval);

  public File[]  openFilesAsk();
  public File    openRecentFilesAsk(Collection lis);
  public File    saveFileAsk(String suggName,boolean showUniqueName);

  public void    setSelectedEventGraphName(String s);    // informative, tells view what we're working on //todo make sure this is called with each addTab

  public void    addTab(ViskitModel mod, boolean isNew); // When a tab is added
  public void    delTab(ViskitModel mod); // When a tab is removed
  public ViskitModel[] getOpenModels();

  public void    showAndSaveSource(String className, String s, String filename);
  public void    displayXML(File f);

  public void    prepareToQuit();
  
  // The following 2 may be implemented by the view in someother way that an official GUI Dialog
  public String addParameterDialog();          // returns param name
  public String addStateVariableDialog();      // returns statevar name

}