package viskit;

import actions.ActionIntrospector;
import edu.nps.util.DirectoryWatch;
import edu.nps.util.FileIO;
import org.apache.commons.configuration.XMLConfiguration;
import org.jgraph.graph.DefaultGraphCell;
import viskit.model.*;
import viskit.mvc.mvcAbstractController;
import viskit.mvc.mvcModel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Mar 2, 2004
 * Time: 12:52:59 PM
 */

/**
 * This is the MVC controller for the Viskit app.  All user inputs come here, and this
 * code decides what to do about it.  To add new events:
 * 1 add a new public Action BLAH field
 * 2 instantiate it in the constructor, mapping it to a handler (name)
 * 3 write the handler
 */

public class Controller extends mvcAbstractController implements ViskitController
/*******************************************************************************/
{

  public Controller()
  //=================
  {
    initConfig();
    initFileWatch();
    this._setFileLists();
  }

  public void begin()
  //-----------------
  {
    // wait for Main to do this after the first window is put up
    // newEventGraph();

    ArrayList lis = getOpenFileList(false);
    if(lis.size() <= 0)
      /*newEventGraph()*/;    // don't default to new event graph
    else {
     for(int i=0;i<lis.size();i++) {
       File f = new File((String)lis.get(i));
       if(f.exists())
         _doOpen(f);
     }
    }
  }

  public void settings()
  {
    // placeholder for multi-tabbed combo app.
  }
  
  public void quit()
  {
    if(preQuit())
      postQuit();
  }

  public boolean preQuit()
  //----------------
  {
    markConfigAllClosed();
    ViskitModel[] modAr = ((ViskitView)getView()).getOpenModels();
    for(int i=0;i<modAr.length;i++) {
      setModel((mvcModel)modAr[i]);
      File f = modAr[i].getLastFile();
      if(f != null)
        markConfigOpen(f);
      if(preClose())
        postClose();
      else
        return false; // cancelled
    }
    return true;
  }

  public void postQuit()
  {
    ((ViskitView)getView()).prepareToQuit();
    VGlobals.instance().quitEventGraphEditor();

  }
  public void newEventGraph()
  //-------------------------
  {
    Model mod = new Model(this);
    mod.init();
    mod.newModel(null);

    // No model set in controller yet...it gets set
    // when TabbedPane changelistener detects a tab change.
    ((ViskitView)getView()).addTab(mod,true);
    editGraphMetaData();

    buildNewNode(new Point(30,30),"Run");   // always start with a run event
    ((ViskitModel)getModel()).setDirty(false); // we're not really dirty yet
  }

/**
 *
 * @return true = continue, false = don't (i.e., we cancelled)
 */
  private boolean askToSaveAndContinue()
  {
    int yn = (((ViskitView) getView()).genericAsk("Question", "Save modified graph?"));

    switch (yn) {
      case JOptionPane.YES_OPTION:
        save();
        if(((Model)getModel()).isDirty())
          return false; // we cancelled
       // else
        return true;
        //break;
      case JOptionPane.NO_OPTION:
        return true;
        //break;
      case JOptionPane.CANCEL_OPTION:
      default:
        return false;
    }
  }

  public void open()
  //----------------
  {
    File file = ((ViskitView)getView()).openFileAsk();
    if (file != null) {
      _doOpen(file);
    }
  }

  private void _doOpen(File file)
  {
    Model mod = new Model(this);
    mod.init();

    ((ViskitView) getView()).addTab(mod,false);

    if (false == mod.newModel(file)) {
      ((ViskitView) getView()).delTab(mod);   // Not a good open, tell view
      return;
    }
    ((ViskitView)getView()).setSelectedEventGraphName(mod.getMetaData().name);
    adjustRecentList(file);

    fileWatchOpen(file);
  }

  private static final int RECENTLISTSIZE = 15;
  private ArrayList recentFileList;

  public void openRecent()
  {
    ArrayList v = getRecentFileList(true); // have a settings panel now ... false);
    if(v.size() <= 0)
      open();
    else {
      File file = ((ViskitView)getView()).openRecentFilesAsk(v);
      if(file != null)
        _doOpen(file);
    }
  }

  // Support for informing listeners about open eventgraphs
  // Methods to implement a scheme where other modules will be informed of file changes //
  // (Would Java Beans do this with more or less effort?

  private DirectoryWatch dirWatch;
  private File watchDir;
  private void initFileWatch()
  {
    try {
      watchDir = File.createTempFile("egs","current");   // actually creates
      String p = watchDir.getAbsolutePath();   // just want the name part of it
      watchDir.delete();        // Don't want the file to be made yet
      watchDir = new File(p);
      watchDir.mkdir();
      watchDir.deleteOnExit();

      dirWatch = new DirectoryWatch(watchDir);
      dirWatch.setLoopSleepTime(1*1000); // 1 secs
      dirWatch.startWatcher();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void fileWatchSave(File f)
  {
    fileWatchOpen(f);
  }
  private void fileWatchOpen(File f)
  {
    String nm = f.getName();
    File ofile = new File(watchDir,nm);
    try {
      FileIO.copyFile(f,ofile,true);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
  private void fileWatchClose(File f)
  {
    String nm = f.getName();
    File ofile = new File(watchDir,nm);
    ofile.delete();
  }

  public void addOpenEventGraphListener(DirectoryWatch.DirectoryChangeListener lis)
  {
    dirWatch.addListener(lis);
  }

  public void removeOpenEventGraphListener(DirectoryWatch.DirectoryChangeListener lis)
  {
    dirWatch.addListener(lis);
  }

  /**
   * If passed file is in the list, move it to the top.  Else insert it;
   * Trim to RECENTLISTSIZE
   * @param file
   */
  private void adjustRecentList(File file)
  {
    String s = file.getAbsolutePath();
    int idx;
    if((idx = recentFileList.indexOf(s)) != -1)
      recentFileList.remove(idx);
    recentFileList.add(0,s);      // to the top

    while(recentFileList.size() > RECENTLISTSIZE)
      recentFileList.remove(recentFileList.size()-1);
    saveHistoryXML(recentFileList);
  }

  private ArrayList openV;

  private void _setFileLists()
  {
    recentFileList = new ArrayList(RECENTLISTSIZE+1);
    openV = new ArrayList(4);
    String[] valueAr = historyConfig.getStringArray(egHistoryKey+"[@value]");
    for (int i = 0; i < valueAr.length; i++) {
      recentFileList.add(valueAr[i]);
      String op = historyConfig.getString(egHistoryKey + "(" + i + ")[@open]");
      if (op != null && (op.toLowerCase().equals("true") || op.toLowerCase().equals("yes")))
        openV.add(valueAr[i]);
    }
  }
  private void saveHistoryXML(ArrayList recentFiles)
  {
    historyConfig.clearTree(egHistoryClearKey);

    for(int i=0;i<recentFiles.size();i++) {
      String value = (String)recentFiles.get(i);
      historyConfig.setProperty(egHistoryKey+"("+i+")[@value]",value);
    }
    historyConfig.getDocument().normalize();
  }
  private void markConfigAllClosed()
  {
    for(int i=0;i<recentFileList.size();i++)
      historyConfig.setProperty(egHistoryKey+"("+i+")[@open]","false");
  }
  private void markConfigOpen(File f)
  {
    int idx = recentFileList.indexOf(f.getAbsolutePath());
    if(idx != -1)
      historyConfig.setProperty(egHistoryKey+"("+idx+")[@open]","true");

    // The open attribute is zeroed out for all recent files the first time a file is opened
  }

  private ArrayList getRecentFileList(boolean refresh)
  {
    if (refresh || recentFileList == null)
      _setFileLists();
    return recentFileList;
  }

  private ArrayList getOpenFileList(boolean refresh)
  {
    if (refresh || openV == null)
      _setFileLists();
    return openV;
  }


  /* a component, e.g., model, wants to say something. */
  public void messageUser(int typ, String msg)    // typ is one of JOptionPane types
  {
    String title;
    switch(typ) {
      case JOptionPane.WARNING_MESSAGE:
        title = "Warning";
        break;
      case JOptionPane.ERROR_MESSAGE:
        title = "Error";
        break;
      case JOptionPane.INFORMATION_MESSAGE:
        title = "Information";
        break;
      case JOptionPane.PLAIN_MESSAGE:
      case JOptionPane.QUESTION_MESSAGE:
      default:
        title = "";
        break;
    }
    ((ViskitView)getView()).genericErrorReport(title,msg);
  }

  public void close()
  {
    if(preClose())
      postClose();
  }

  public boolean preClose()
  {
    Model mod = (Model)getModel();
    if (mod.isDirty())
      if(!askToSaveAndContinue())
        return false;
     return true;
  }

  public void postClose()
  {
    Model mod = (Model)getModel();

    ((ViskitView)getView()).delTab(mod);

    if(mod.getLastFile() != null)
      fileWatchClose(mod.getLastFile());
  }

  public void closeAll()
  {
    ViskitModel[] modAr = ((ViskitView)getView()).getOpenModels();
    for(int i=0;i<modAr.length;i++) {
      setModel((mvcModel)modAr[i]);
      close();
    }
  }

  public void save()
  //----------------
  {
    ViskitModel mod = (ViskitModel)getModel();
    File lastFile = mod.getLastFile();
    if(lastFile == null)
      saveAs();
    else {
      ((ViskitModel)getModel()).saveModel(lastFile);
      fileWatchSave(lastFile);
    }
  }

  public void saveAs()
  //------------------
  {
    ViskitModel mod = (ViskitModel)getModel();
    ViskitView view = (ViskitView)getView();
    GraphMetaData gmd = mod.getMetaData();

    File saveFile = view.saveFileAsk(gmd.name+".xml",false);
    if(saveFile != null) {
      File lastFile = mod.getLastFile();
      if(lastFile != null)
        fileWatchClose(lastFile);

      String n = saveFile.getName();
      if(n.endsWith(".xml") || n.endsWith(".XML"))
        n = n.substring(0,n.length()-4);
      gmd.name = n;
      mod.changeMetaData(gmd); // might have renamed

      mod.saveModel(saveFile);
      view.setSelectedEventGraphName(n);

      fileWatchOpen(saveFile);
      adjustRecentList(saveFile);
    }
  }

  public void newSimParameter()
  //------------------------
  {
    ((ViskitView) getView()).addParameterDialog();

  }
  public void buildNewSimParameter(String name, String type, String initVal, String comment)
  {
    ((ViskitModel)getModel()).newSimParameter(name, type, initVal, comment);
  }

  public void simParameterEdit(vParameter param)
  {
    boolean modified = ((ViskitView) getView()).doEditParameter(param);
    if (modified) {
      ((viskit.model.ViskitModel) getModel()).changeSimParameter(param);
    }
  }
  public void codeBlockEdit(String s)
  {
    ((viskit.model.ViskitModel)getModel()).changeCodeBlock(s);
  }
  public void stateVariableEdit(vStateVariable var)
  {
    boolean modified = ((ViskitView) getView()).doEditStateVariable(var);
    if (modified) {
      ((viskit.model.ViskitModel) getModel()).changeStateVariable(var);
    }
  }

  // Comes in from plus button
  public void newStateVariable()
  {
    ((ViskitView) getView()).addStateVariableDialog();
  }

  // Comes in from view
  public void buildNewStateVariable(String name, String type, String initVal, String comment)
  //----------------------------
  {
    ((viskit.model.ViskitModel)getModel()).newStateVariable(name,type,initVal,comment);
  }

  private Vector selectionVector = new Vector();

  public void selectNodeOrEdge(Vector v)
  //------------------------------------
  {
    selectionVector = v;
    boolean ccbool = (selectionVector.size() > 0 ? true : false);
    ActionIntrospector.getAction(this, "copy").setEnabled(nodeSelected());
    ActionIntrospector.getAction(this, "cut").setEnabled(ccbool);
    ActionIntrospector.getAction(this, "newSelfRefEdge").setEnabled(ccbool);
  }

  private Vector copyVector = new Vector();

  public void copy()
  //----------------
  {
    if(!nodeSelected()) {
      ((ViskitView) getView()).genericErrorReport("Unsupported Action","Edges cannot be copied.");
      return;
    }
    copyVector = (Vector) selectionVector.clone();

    // Paste only works for node, check to enable/disable paste menu item
    handlePasteMenuItem();
  }

  private void handlePasteMenuItem()
  {
    ActionIntrospector.getAction(this,"paste").setEnabled(nodeCopied());
  }

  private boolean nodeCopied()
  {
    return nodeInVector(copyVector);
  }

  private boolean nodeSelected()
  {
    return nodeInVector(selectionVector);
  }

  private boolean nodeInVector(Vector v)
  {
    for (Iterator itr = v.iterator(); itr.hasNext();) {
      Object o = itr.next();
      if(o instanceof EventNode) {
        return true;
      }
    }
    return false;

  }
  public void paste()
  //-----------------
  {
    if (copyVector.size() <= 0)
      return;
    int x=100,y=100; int n=0;
    // We only paste un-attached nodes (at first)
    for(Iterator itr = copyVector.iterator(); itr.hasNext();) {
      Object o = itr.next();
      if(o instanceof Edge)
        continue;
      String nm = ((viskit.model.EventNode)o).getName();
      ((viskit.model.ViskitModel) getModel()).newEvent(nm+"-copy", new Point(x+(20*n),y+(20*n)));
      n++;
    }
  }

  public void cut()
  //---------------
  {
    if (selectionVector != null && selectionVector.size() > 0) {
      // first ask:
      String msg = "";
      int nodeCount = 0;  // different msg for edge delete
      for (Iterator itr = selectionVector.iterator(); itr.hasNext();) {
        Object o = itr.next();
        if(o instanceof EventNode)
          nodeCount++;
        String s = o.toString();
        s = s.replace('\n', ' ');
        msg += ", \n" + s;
      }
      if(msg.length()>3)
        msg = msg.substring(3);  // remove leading stuff

      String specialNodeMsg = (nodeCount > 0 ? "\n(Events remain in paste buffer, but attached edges are permanently deleted.)" : "");
      if (((ViskitView) getView()).genericAsk("Remove element(s)?", "Confirm remove " + msg + "?" + specialNodeMsg)
       == JOptionPane.YES_OPTION) {
        // do edges first?
        copyVector = (Vector) selectionVector.clone();
        //Vector localV = (Vector) selectionVector.clone();   // avoid concurrent update
        for (Iterator itr = copyVector.iterator(); itr.hasNext();) {
          Object elem = itr.next();
          if(elem instanceof Edge) {
            killEdge((Edge)elem);
          }
          else if(elem instanceof EventNode) {
            EventNode en = (EventNode)elem;
            for (Iterator it2 = en.getConnections().iterator(); it2.hasNext();) {
              Edge ed = (Edge) it2.next();
              killEdge(ed);
            }
            ((ViskitModel) getModel()).deleteEvent(en);
          }
        }
      }
      handlePasteMenuItem();
    }
  }

  private void killEdge(Edge e)
  {
    if (e instanceof SchedulingEdge)
      ((ViskitModel) getModel()).deleteEdge((SchedulingEdge) e);
    else
      ((ViskitModel) getModel()).deleteCancelEdge((CancellingEdge) e);
  }

  public void deleteSimParameter(vParameter p)
  {
    ((ViskitModel) getModel()).deleteSimParameter(p);    
  }

  public void deleteStateVariable(vStateVariable var)
  {
    ((ViskitModel)getModel()).deleteStateVariable(var);
  }

  private boolean checkSave()
  {
    if(((ViskitModel)getModel()).isDirty() || ((ViskitModel)getModel()).getLastFile() == null) {
      int ret = JOptionPane.showConfirmDialog(null,"The model will be saved.\nContinue?","Confirm",JOptionPane.YES_NO_OPTION);
      if(ret != JOptionPane.YES_OPTION)
        return false;
      // saveAs();     7 Nov 05
      save();
    }
    return true;
  }
  public void generateJavaClass()
  {
    File lastFile = ((ViskitModel)getModel()).getLastFile();
    if(checkSave() == false || lastFile == null)
      return;
    String source = ((ViskitModel)getModel()).buildJavaSource();
    if(source != null && source.length() > 0)
      ((ViskitView)getView()).showAndSaveSource(source,lastFile.getName());
  }

  public void showXML()
  {
    if(checkSave() == false || ((ViskitModel)getModel()).getLastFile() == null)
      return;

    ((ViskitView)getView()).displayXML(((ViskitModel)getModel()).getLastFile());
  }

  public void runAssemblyEditor()
  {
    if (VGlobals.instance().getAssemblyEditor() == null)
      VGlobals.instance().buildAssemblyViewFrame(false);
    VGlobals.instance().runAssemblyView();
  }

  public void eventList()
  {
    // not used
    System.out.println("EventListAction in " + this);
  }

  private int nodeCount = 0;
  public void newNode()
  //-------------------
  {
    buildNewNode(new Point(100,100));
  }
  public void buildNewNode(Point p)
  //--------------------------
  {
    buildNewNode(p,"evnt_"+nodeCount++);
  }
  public void buildNewNode(Point p,String nm)
  //------------------------------------
  {
    ((viskit.model.ViskitModel) getModel()).newEvent(nm, p);
  }

  public void buildNewArc(Object[] nodes)
  //--------------------------------
  {
    // My node view objects hold node model objects and vice versa
    EventNode src = (EventNode) ((DefaultGraphCell) nodes[0]).getUserObject();
    EventNode tar = (EventNode) ((DefaultGraphCell) nodes[1]).getUserObject();
    ((ViskitModel) getModel()).newEdge(src, tar);
  }

  public void buildNewCancelArc(Object[] nodes)
  //--------------------------------------
  {
    // My node view objects hold node model objects and vice versa
    EventNode src = (EventNode) ((DefaultGraphCell) nodes[0]).getUserObject();
    EventNode tar = (EventNode) ((DefaultGraphCell) nodes[1]).getUserObject();
    ((ViskitModel) getModel()).newCancelEdge(src, tar);
  }

  public void newSelfRefEdge()
  //--------------------------
  {
    if (selectionVector != null && selectionVector.size() > 0) {
      for (Iterator itr = selectionVector.iterator(); itr.hasNext();) {
        Object o = itr.next();
        if(o instanceof EventNode) {
          ((ViskitModel) getModel()).newEdge((EventNode)o,(EventNode)o);
        }
      }
    }
  }

  public void editGraphMetaData()
  //--------------------------
  {
    GraphMetaData gmd = ((ViskitModel)getModel()).getMetaData();
    boolean modified = EvGraphMetaDataDialog.showDialog((EventGraphViewFrame)getView(),(EventGraphViewFrame)getView(),gmd);
    if(modified)
      ((ViskitModel)getModel()).changeMetaData(gmd);

    // update title bar
    ((ViskitView)getView()).setSelectedEventGraphName(gmd.name);
  }
  
  public void nodeEdit(viskit.model.EventNode node)      // shouldn't be required
  //----------------------------------
  {
    boolean done;
    do {
      done=true;
      boolean modified = ((ViskitView) getView()).doEditNode(node);
      if (modified) {
        done = ((viskit.model.ViskitModel) getModel()).changeEvent(node);
      }
    } while(!done);
  }

  public void arcEdit(viskit.model.SchedulingEdge ed)
  //------------------------------------
  {
    boolean modified = ((ViskitView) getView()).doEditEdge(ed);
    if (modified) {
      ((viskit.model.ViskitModel) getModel()).changeEdge(ed);
    }
  }

  public void canArcEdit(viskit.model.CancellingEdge ed)
  //---------------------------------------
  {
    boolean modified = ((ViskitView) getView()).doEditCancelEdge(ed);
    if (modified) {
      ((viskit.model.ViskitModel) getModel()).changeCancelEdge(ed);
    }
  }
  private String imgSaveCount= "";
  private int    imgSaveInt = -1;
  public void captureWindow()
  //-------------------------
  {
    String fileName = "ViskitScreenCapture";
    File lastFile = ((ViskitModel)getModel()).getLastFile();
    if (lastFile != null)
      fileName = lastFile.getName();

    File fil = ((ViskitView)getView()).saveFileAsk(fileName+imgSaveCount+".png",true);
    if(fil == null)
      return;

    final Timer tim = new Timer(100,new timerCallback(fil));
    tim.setRepeats(false);
    tim.start();

    imgSaveCount = ""+ (++imgSaveInt);
  }

  class timerCallback implements ActionListener
  {
    File fil;
    timerCallback(File f)
    {
      fil = f;
    }
    public void actionPerformed(ActionEvent ev)
    {
      // create and save the image
      Component component = (Component) getView();
      Point p = new Point(0, 0);
      SwingUtilities.convertPointToScreen(p, component);
      Rectangle region = component.getBounds();
      region.x = p.x;
      region.y = p.y;
      BufferedImage image = null;
      try {
        image = new Robot().createScreenCapture(region);
        ImageIO.write(image, "png", fil);
      }
      catch (Exception e) {
        e.printStackTrace();
      }

      // display a scaled version
      JFrame frame = new JFrame("Saved as " + fil.getName());
      ImageIcon ii = new ImageIcon(image.getScaledInstance(image.getWidth() * 50 / 100, image.getHeight() * 50 / 100, Image.SCALE_FAST));
      JLabel lab = new JLabel(ii);
      frame.getContentPane().setLayout(new BorderLayout());
      frame.getContentPane().add(lab, BorderLayout.CENTER);
      frame.pack();
      frame.setLocationRelativeTo((Component) getView());
      frame.setVisible(true);
    }
  }

  XMLConfiguration historyConfig;
  String egHistoryKey = "history.EventGraphEditor.Recent.EventGraphFile";
  String egHistoryClearKey = "history.EventGraphEditor.Recent";
  private void initConfig()
  {
    try {
      historyConfig = VGlobals.instance().getHistoryConfig();
    }
    catch (Exception e) {
      System.out.println("Error loading history file: "+e.getMessage());
      System.out.println("Recent file saving disabled");
      historyConfig = null;
    }
  }
}
