package viskit;

import viskit.model.*;

import java.io.File;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Mar 18, 2004
 * @since 12:06:11 PM
 * @version $Id$
 *
 * The MVC design of Viskit means that the ViskitModel and the ViskitView know about the
 * chosen view only as much as is described by this interface.
 */
public interface ViskitAssemblyView {

    // permit user to edit existing entities
    boolean doEditPclNode(PropChangeListenerNode pclNode);

    boolean doEditEvGraphNode(EvGraphNode evNode);

    boolean doEditPclEdge(PropChangeEdge pclEdge);

    boolean doEditAdapterEdge(AdapterEdge aEdge);

    boolean doEditSimEvListEdge(SimEvListenerEdge seEdge);

    Object getSelectedPropChangeListener();

    Object getSelectedEventGraph();

    /**
     * Add a path to SimEntities in the LEGO tree
     * @param f the path to evaluate for SimEntites
     * @param b flag to indicate recursion checking of the given path
     */
    void addToEventGraphPallette(File f, boolean b);

    void removeFromEventGraphPallette(File f);

    /**
     * Add a path to PropertyChangeListeners in the LEGO tree
     * @param f the path to evaluate for PropertyChangeListeners
     * @param b flag to indicate recursion checking of the given path
     */
    void addToPropChangePallette(File f, boolean b);

    /** Not currently used
     *
     * @param f the PCL to remove from the node tree
     */
    void removeFromPropChangePallette(File f);

    int genericAsk(String title, String prompt);      // returns JOptionPane constants

    int genericAskYN(String title, String prompt);

    int genericAsk2Butts(String title, String prompt, String button1, String button2);

    void genericErrorReport(String title, String message);

    String promptForStringOrCancel(String title, String message, String initval);
 
    /** Allow opening of one, or more Assembly files
     * 
     * @return one or more chosen Assembly files
     */
    File[] openFilesAsk();

    /** @param lis a list of recently open files
     * @return a recently opened file
     */
    File openRecentFilesAsk(Collection<String> lis);

    File saveFileAsk(String suggName, boolean suggUniqueName);

    /** Open an already existing Viskit Project */
    void openProject();

    /** Update the name of the Assembly in the component title bar
     * @param s the name of the Assembly
     */
    void setSelectedAssemblyName(String s);
    
    void showAndSaveSource(String className, String s);

    void displayXML(File f);
    
    /** Add an Assembly tab to the Assembly View Editor
     * 
     * @param mod the Assembly model to display graphically
     */
    void addTab(ViskitAssemblyModel mod);
    
    /** Remove an Assembly tab from the Assembly View Editor
     * 
     * @param mod the Assembly model to remove from view
     */
    void delTab(ViskitAssemblyModel mod);
    
    /** @return an array of open ViskitAssemblyModels */
    ViskitAssemblyModel[] getOpenModels();
    
    /** Capture Assembly Editor user set Frame bounds */
    void prepareToQuit();
}
