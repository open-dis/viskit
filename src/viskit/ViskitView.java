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
 * @version $Id$
 */
public interface ViskitView
{
  // permit user to edit existing entities
  boolean doEditCancelEdge   ( CancellingEdge edge );
  boolean doEditEdge         ( SchedulingEdge edge );
  boolean doEditNode         ( EventNode node );
  boolean doEditParameter    ( vParameter param );
  boolean doEditStateVariable( vStateVariable var);

  /**
   * @param title 
   * @param prompt 
   * @return yes, no or cancel constants 
   */
  int     genericAsk             ( String title, String prompt );
  int     genericAskYN           (String title, String msg);
  void    genericErrorReport     ( String title, String message );
  String  promptForStringOrCancel( String title, String message, String initval);

  File[]  openFilesAsk();
  File    openRecentFilesAsk(Collection<String> lis);
  File    saveFileAsk(String suggName,boolean showUniqueName);

  /** Tells view what we're working on
   * @param s the name of the EventGraph
   */
  //todo make sure this is called with each addTab 
  void    setSelectedEventGraphName(String s);    

  void    addTab(ViskitModel mod); // When a tab is added
  void    delTab(ViskitModel mod); // When a tab is removed
  ViskitModel[] getOpenModels();

  void    showAndSaveSource(String className, String s, String filename);
  void    displayXML(File f);

  void    prepareToQuit();
  
  // The following 2 may be implemented by the view in someother way that an official GUI Dialog
  String addParameterDialog();          // returns param name
  String addStateVariableDialog();      // returns statevar name

}