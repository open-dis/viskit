package viskit;

import viskit.model.*;

import java.io.File;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Mar 18, 2004
 * @since 12:06:11 PM
 * @version $Id: ViskitAssemblyView.java 1666 2007-12-17 05:24:41Z tdnorbra $
 *
 * The MVC design of Viskit means that the ViskitModel and the ViskitView know about the
 * chosen view only as much as is described by this interface.
 */
public interface ViskitAssemblyView {

    // permit user to edit existing entities
    public boolean doEditPclNode(PropChangeListenerNode pclNode);

    public boolean doEditEvGraphNode(EvGraphNode evNode);

    public boolean doEditPclEdge(PropChangeEdge pclEdge);

    public boolean doEditAdapterEdge(AdapterEdge aEdge);

    public boolean doEditSimEvListEdge(SimEvListenerEdge seEdge);

    public Object getSelectedPropChangeListener();

    public Object getSelectedEventGraph();

    public void addToEventGraphPallette(File f);

    public void removeFromEventGraphPallette(File f);

    public void addToPropChangePallette(File f);

    public void removeFromPropChangePallette(File f);

    public int genericAsk(String title, String prompt);      // returns JOptionPane constants

    public int genericAskYN(String title, String prompt);

    public int genericAsk2Butts(String title, String prompt, String button1, String button2);

    public void genericErrorReport(String title, String message);

    public String promptForStringOrCancel(String title, String message, String initval);

    public File openFileAsk();

    public File openRecentFilesAsk(Collection<String> lis);

    public File saveFileAsk(String suggName, boolean suggUniqueName);

    public void fileName(String s);    // informative, tells view what we're working on
    //public void    setStopTime(String s);
    //public void    setVerbose(boolean v);
    //public String  getStopTime();
    //public boolean getVerbose();
    public void showAndSaveSource(String className, String s);

    public void displayXML(File f);
}
