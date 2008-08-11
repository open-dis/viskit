package viskit;

import java.awt.*;
import java.util.Vector;
import javax.swing.JComponent;

import edu.nps.util.DirectoryWatch;
import viskit.model.*;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 14, 2004
 * @since 9:27:13 AM
 * @version $Id$
 */
public interface ViskitAssemblyController {
    
    /* start app */
    void begin();
    
    // user has clicked a menu item
    void newEventGraphNode();
    void newPropChangeListenerNode();

    // user has established some parameter, model can create object
    void newEventGraphNode(String name, Point p);
    void newFileBasedEventGraphNode(FileBasedAssyNode xnode, Point p);

    void newFileBasedPropChangeListenerNode(FileBasedAssyNode xnode, Point p);

    void newPropChangeListenerNode(String name, Point p);

    void editGraphMetaData();

    void newAssembly();

    void setRunTabbedPane(JComponent runTabbedPane, int idx);

    void runEventGraphEditor();

    void showXML();

    /* a component, e.g., model, wants to say something. */
    void messageUser(int typ, String msg);    // typ is one of JOptionPane types

    void selectNodeOrEdge(Vector v);

    void newAdapterArc(Object[] nodes);

    void newSimEvListArc(Object[] nodes);

    void newPropChangeListArc(Object[] nodes);

    void pcListenerEdit(PropChangeListenerNode pclNode);

    void evGraphEdit(EvGraphNode evNode);

    void pcListenerEdgeEdit(PropChangeEdge pclEdge);

    void adapterEdgeEdit(AdapterEdge edgeObj);

    void simEvListenerEdgeEdit(SimEvListenerEdge edgeObj);

    /* menu selections */
    void copy();

    void cut();        // to remove nodes and edges

    void open();

    void openRecent();

    void openRecentAssembly(String fullPath);
    
    void paste();

    void quit();

    void save();

    void saveAs();

    // Bug fix: 1195
    void close();

    boolean preClose();

    void postClose();

    void settings();

    boolean preQuit();

    void postQuit();

    void addAssemblyFileListener(OpenAssembly.AssyChangeListener lis);

    void removeAssemblyFileListener(OpenAssembly.AssyChangeListener lis);

    OpenAssembly.AssyChangeListener getAssemblyChangeListener();

    DirectoryWatch.DirectoryChangeListener getOpenEventGraphListener();

    void generateJavaSource();

    void compileAssemblyAndPrepSimRunner();

    void initAssemblyRun();

    void export2grid();

    void captureWindow();
    
    void addRecentFileListListener(RecentFileListener lis);
    void removeRecentFileListListener(RecentFileListener lis);
    java.util.List<String> getRecentFileList();
    void clearRecentFileList();

    public static interface RecentFileListener
    {
      public void listChanged();
    }
}
