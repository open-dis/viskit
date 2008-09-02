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

    void addToEventGraphPallette(File f);

    void removeFromEventGraphPallette(File f);

    void addToPropChangePallette(File f);

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

    File openRecentFilesAsk(Collection<String> lis);

    File saveFileAsk(String suggName, boolean suggUniqueName);

    void fileName(String s);    // informative, tells view what we're working on
    
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
