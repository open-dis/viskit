package viskit.view;

import java.io.File;
import java.util.Collection;
import viskit.model.*;

/**
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Mar 18, 2004
 * @since 12:06:11 PM
 * @version $Id$

 The MVC design of Viskit means that the ViskitModel and the ViskitView know about the
 chosen view only as much as is described by this interface.
 */
public interface AssemblyView {

    // permit user to edit existing entities
    boolean doEditPropertyChangeListenerNode(PropertyChangeListenerNode pclNode);

    /** Permits user to edit existing entities
     * @param evNode the event graph node to edit
     * @return an indication of success
     */
    boolean doEditEventGraphNode(EventGraphNode evNode);

    boolean doEditPropertyChangeListenerEdge(PropertyChangeEdge pclEdge);

    boolean doEditAdapterEdge(AdapterEdge aEdge);

    boolean doEditSimEventListenerEdge(SimEventListenerEdge seEdge);

    Object getSelectedPropertyChangeListener();

    Object getSelectedEventGraph();

    /**
     * Add a path to SimEntities in the LEGO tree.  This call will check
     * compilation of Event Graphs
     *
     * @param f the path to evaluate for SimEntites
     * @param b flag to indicate recursion checking of the given path if a known
     * directory
     */
    void addEventGraphsToLegoTree(File f, boolean b);

    void removeEventGraphFromLEGOTree(File f);

    /**
     * Add a path to PropertyChangeListeners in the LEGO tree
     * @param f the path to evaluate for PropertyChangeListeners
     * @param b flag to indicate recursion checking of the given path
     */
    void addPropertyChangeListenersToLegoTree(File f, boolean b);

    /** Not currently used
     *
     * @param f the PCL to remove from the node tree
     */
    void removePropertyChangeListenerFromLEGOTree(File f);

// user-interaction methods moved from AssemblyViewFrame to MainFrame, up higher in hierarchy
//    int genericAsk(String title, String prompt);      // returns JOptionPane constants
//
//    int genericAskYN(String title, String prompt);
//
//    int genericAsk2Buttons(String title, String prompt, String button1, String button2);
//
//    /** A component, e.g., vAMod, wants to say something.
//     *
//     * @param messageType the type of message, i.e. WARN, ERROR, INFO, QUESTION
//     * @param title the title of the message in the dialog frame
//     * @param message the message to transmit
//     */
//    void genericReport(int messageType, String title, String message);
//
//    String promptForStringOrCancel(String title, String message, String initval);

    /** Allow opening of one, or more Assembly files
     *
     * @return one or more chosen Assembly files
     */
    File[] openFilesAsk();

    /** @param lis a list of recently open files
     * @return a recently opened file
     */
    File openRecentFilesAsk(Collection<String> lis);

    /** Saves the current Assembly "as" desired by the user
     *
     * @param suggestedName the package and file name of the Assembly
     * @param suggestUniqueName show Assembly name only
     * @param title for chooser
     * @return a File object of the saved Assembly
     */
    File saveFileAsk(String suggestedName, boolean suggestUniqueName, String title);

    /** Open an already existing Viskit Project */
    void openProject();

    /** Update the name of the Assembly in the component title bar
     * @param s the name of the Assembly
     */
    void setSelectedAssemblyName(String s);

    /**
     * Called by the controller after source has been generated. Show to the
     * user and provide him with the option to save.
     *
     * @param className the name of the source file to show
     * @param s Java source
     * @param filename the source file's name
     */
    void showAndSaveSource(String className, String s, String filename);

    /**
     * Shows the XML representation of the given file
     * @param f the file to display
     */
    void displayXML(File f);

    /** Add an Assembly tab to the Assembly View Editor
     *
     * @param mod the Assembly model to display graphically
     */
    void addTab(AssemblyModel mod);

    /** Remove an Assembly tab from the Assembly View Editor
     *
     * @param mod the Assembly model to remove from view
     */
    void deleteTab(AssemblyModel mod);

    /** @return an array of open ViskitAssemblyModels */
    AssemblyModel[] getOpenAssemblyModels();
}
