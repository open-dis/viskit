package viskit;

import viskit.mvc.mvcAbstractController;
import viskit.model.*;
import viskit.xsd.translator.SimkitXML2Java;
import viskit.xsd.assembly.SimkitAssemblyXML2Java;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.*;

import actions.ActionIntrospector;
import org.jgraph.graph.DefaultGraphCell;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 14, 2004
 * Time: 9:26:02 AM
 */

public class AssemblyController extends mvcAbstractController implements ViskitAssemblyController
{
  Class simEvSrcClass,simEvLisClass,propChgSrcClass,propChgLisClass;

  public void begin()
  //-----------------
  {
    //newEventGraph();
    // The following comments were an attempt to solve classloader issues that needed to be solved
    // a different way
    try {
      simEvSrcClass   = Vstatics.classForName("simkit.SimEventSource");
      simEvLisClass   = Vstatics.classForName("simkit.SimEventListener");
      propChgSrcClass = Vstatics.classForName("simkit.PropertyChangeSource");
      propChgLisClass = Vstatics.classForName("java.beans.PropertyChangeListener");
      if(simEvSrcClass == null | simEvLisClass == null | propChgSrcClass == null | propChgLisClass == null)
        throw new ClassNotFoundException();
    }
    catch (ClassNotFoundException e) {
      ((ViskitAssemblyView)getView()).genericErrorReport("Internal error","simkit.jar not in classpath");
    }

  }

  public void runEventGraphEditor()
  {
    if (VGlobals.instance().getEventGraphEditor() == null)
      VGlobals.instance().buildEventGraphViewFrame();
    VGlobals.instance().runEventGraphView();
  }


  public void quit()
  //----------------
  {
    if (((AssemblyModel)getModel()).isDirty())
      if(askToSaveAndContinue() == false)
        return;
    VGlobals.instance().quitAssemblyEditor();

  }

  File lastFile;
  public void open()
  {
    if (((AssemblyModel)getModel()).isDirty())
      if(askToSaveAndContinue() == false)
        return;

    lastFile = ((ViskitAssemblyView) getView()).openFileAsk();
    if (lastFile != null) {
      ((ViskitAssemblyModel) getModel()).newModel(lastFile);
      ((ViskitAssemblyView) getView()).fileName(lastFile.getName());
      GraphMetaData gmd = ((ViskitAssemblyModel) getModel()).getMetaData();
      ((ViskitAssemblyView) getView()).setStopTime(gmd.stopTime);
      ((ViskitAssemblyView) getView()).setVerbose(gmd.verbose);
    }

  }
  public void save()
  //----------------
  {
    if(lastFile == null)
      saveAs();
    else {
      updateGMD();
      ((ViskitAssemblyModel)getModel()).saveModel(lastFile);
    }
  }

  public void saveAs()
  {
    lastFile = ((ViskitAssemblyView)getView()).saveFileAsk(((ViskitAssemblyModel)getModel()).getMetaData().name);
    if(lastFile != null) {
      updateGMD();
      ((ViskitAssemblyModel)getModel()).saveModel(lastFile);
      ((ViskitAssemblyView)getView()).fileName(lastFile.getName());
    }

  }
  private void updateGMD()
  {
    GraphMetaData gmd = ((ViskitAssemblyModel)getModel()).getMetaData();
    gmd.stopTime = ((ViskitAssemblyView)getView()).getStopTime();
    gmd.verbose = ((ViskitAssemblyView)getView()).getVerbose();
    ((ViskitAssemblyModel)getModel()).changeMetaData(gmd);

  }
  public void newAssembly()
  {
    if (((AssemblyModel)getModel()).isDirty())
      if(askToSaveAndContinue() == false)
        return;

    ((ViskitAssemblyModel) getModel()).newModel(null);
    editGraphMetaData();

  }

  private int egNodeCount=0;
  private String shortEgName(String typeName)
  {
    String shortname = "evgr_";
    if(typeName.lastIndexOf('.') != -1)
      shortname = typeName.substring(typeName.lastIndexOf('.')+1) + "_";
    return shortname + egNodeCount++;
  }
  public void newEventGraphNode(String typeName, Point p)
  {
    String shName = shortEgName(typeName);
    ((viskit.model.AssemblyModel) getModel()).newEventGraph(shName, typeName, p);
  }
  public void newFileBasedEventGraphNode(FileBasedAssyNode xnode, Point p)
  {
    String shName = shortEgName(xnode.loadedClass);
    ((viskit.model.ViskitAssemblyModel)getModel()).newEventGraphFromXML(shName,xnode,p);
  }
  private String shortPCLName(String typeName)
  {
    String shortname = "lstnr_";
    if(typeName.lastIndexOf('.') != -1)
      shortname = typeName.substring(typeName.lastIndexOf('.')+1) + "_";

    return shortname + egNodeCount++; // use same counter
  }
  private String shortAdapterName(String typeName)
  {
    String shortname = "adptr_";
    if(typeName.lastIndexOf('.') != -1)
      shortname = typeName.substring(typeName.lastIndexOf('.')+1) + "_";
    return shortname + egNodeCount++; // use same counter
  }
  public void newPropChangeListenerNode(String name, Point p)
  {
    String shName = shortPCLName(name);
    ((viskit.model.AssemblyModel)getModel()).newPropChangeListener(shName,name,p);
  }

  public void newFileBasedPropChangeListenerNode(FileBasedAssyNode xnode, Point p)
  {
    String shName = shortPCLName(xnode.loadedClass);
    ((viskit.model.AssemblyModel)getModel()).newPropChangeListenerFromXML(shName,xnode,p);

  }

  /**
   *
   * @return true = continue, false = don't (i.e., we cancelled)
   */
    private boolean askToSaveAndContinue()
    {
      int yn = (((ViskitAssemblyView) getView()).genericAsk("Question", "Save current assembly?"));

      switch (yn) {
        case JOptionPane.YES_OPTION:
          save();
          if(((AssemblyModel)getModel()).isDirty())
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

    public void editGraphMetaData()
    //--------------------------
    {
      GraphMetaData gmd = ((ViskitAssemblyModel)getModel()).getMetaData();
      boolean modified = AssemblyMetaDataDialog.showDialog((AssemblyViewFrame)getView(),(AssemblyViewFrame)getView(),gmd);
      if(modified)
        ((ViskitAssemblyModel)getModel()).changeMetaData(gmd);
    }

  public void newAdapterArc(Object[]nodes)
  {
    AssemblyNode oA = (AssemblyNode)((DefaultGraphCell)nodes[0]).getUserObject();
    AssemblyNode oB = (AssemblyNode)((DefaultGraphCell)nodes[1]).getUserObject();

    AssemblyNode[] oArr = null;
    try {
      oArr = checkLegalForSEListenerArc(oA,oB);
    }
    catch (Exception e) {
      ((ViskitAssemblyView)getView()).genericErrorReport("Connection error.","Possible class not found.  All referenced entities must be in a list at left.");
      return;
    }
    if(oArr == null) {
      ((ViskitAssemblyView)getView()).genericErrorReport("Incompatible connection","The nodes must be a SimEventListener and SimEventSource combination.");
      return;
    }
    adapterEdgeEdit(((ViskitAssemblyModel)getModel()).newAdapterEdge(shortAdapterName(""),oArr[0],oArr[1]));

/*
    if(!(oA instanceof EvGraphNode) || !(oB instanceof EvGraphNode))
      ((ViskitAssemblyView)getView()).genericErrorReport("Incompatible connection", "Both nodes must be instances of Event Graphs.");
    else {
      AdapterEdge ae = ((ViskitAssemblyModel)getModel()).newAdapterEdge((EvGraphNode)oA,(EvGraphNode)oB);
      // edit right away
      if(ae != null)     // shouldn't happen
        adapterEdgeEdit(ae);
    }
*/
  }
  public void newSimEvListArc(Object[]nodes)
  {
    AssemblyNode oA = (AssemblyNode)((DefaultGraphCell)nodes[0]).getUserObject();
    AssemblyNode oB = (AssemblyNode)((DefaultGraphCell)nodes[1]).getUserObject();

    AssemblyNode[] oArr = checkLegalForSEListenerArc(oA,oB);

    if(oArr == null) {
      ((ViskitAssemblyView)getView()).genericErrorReport("Incompatible connection","The nodes must be a SimEventListener and SimEventSource combination.");
      return;
    }
    ((ViskitAssemblyModel)getModel()).newSimEvLisEdge(oArr[0],oArr[1]);

/*
    if(!(oA instanceof EvGraphNode) || !(oB instanceof EvGraphNode))
      ((ViskitAssemblyView)getView()).genericErrorReport("Incompatible connection", "Both nodes must be instances of Event Graphs.");
    else
    ((ViskitAssemblyModel)getModel()).newSimEvLisEdge((EvGraphNode)oA,(EvGraphNode)oB);
*/
  }
  public void newPropChangeListArc(Object[]nodes)
  {
    // One and only one has to be a prop change listener
    AssemblyNode oA = (AssemblyNode)((DefaultGraphCell)nodes[0]).getUserObject();
    AssemblyNode oB = (AssemblyNode)((DefaultGraphCell)nodes[1]).getUserObject();

    AssemblyNode[] oArr = checkLegalForPropChangeArc(oA,oB);

    if(oArr == null) {
      ((ViskitAssemblyView)getView()).genericErrorReport("Incompatible connection","The nodes must be a PropertyChangeListener and PropertyChangeSource combination.");
      return;
    }
    pcListenerEdgeEdit(((ViskitAssemblyModel)getModel()).newPclEdge(oArr[0],oArr[1]));

/*
    PropChangeEdge pce = null;
    if(oA instanceof PropChangeListenerNode && !(oB instanceof PropChangeListenerNode)) {
      pce = ((ViskitAssemblyModel)getModel()).newPclEdge((EvGraphNode)oB,(PropChangeListenerNode)oA);
    }
    else if(oB instanceof PropChangeListenerNode && !(oA instanceof PropChangeListenerNode)) {
      pce = ((ViskitAssemblyModel)getModel()).newPclEdge((EvGraphNode)oA,(PropChangeListenerNode)oB);
    }
    else
      ((ViskitAssemblyView)getView()).genericErrorReport("Incompatible connection","One of the two nodes must be an instance of a PropertyChangeListener.");
    // edit right away
    if(pce != null)
      pcListenerEdgeEdit(pce);
*/
  }

  AssemblyNode[] checkLegalForSEListenerArc(AssemblyNode a, AssemblyNode b)
  {
    Class ca = findClass(a);
    Class cb = findClass(b);
    AssemblyNode [] ra = orderSELSrcAndLis(a,b,ca,cb);
    return ra;
  }

  AssemblyNode[] checkLegalForPropChangeArc(AssemblyNode a, AssemblyNode b)
  {
    Class ca = findClass(a);
    Class cb = findClass(b);
    AssemblyNode [] ra = orderPCLSrcAndLis(a,b,ca,cb);
    return ra;

  }
  Class findClass(AssemblyNode o)
  {
    return Vstatics.classForName(o.getType());
  }
  
  AssemblyNode[] orderPCLSrcAndLis(AssemblyNode a, AssemblyNode b, Class ca, Class cb)
  {
    AssemblyNode[] obArr = new AssemblyNode[2];
    if(propChgSrcClass.isAssignableFrom(ca))
      obArr[0] = a;
    else if(propChgSrcClass.isAssignableFrom(cb))
      obArr[0] = b;
    if(propChgLisClass.isAssignableFrom(cb))
      obArr[1] = b;
    else if(propChgLisClass.isAssignableFrom(ca))
      obArr[1] = a;

    if(obArr[0] == null | obArr[1] == null || obArr[0]==obArr[1])
      return null;
    return obArr;
  }

  AssemblyNode[] orderSELSrcAndLis(AssemblyNode a, AssemblyNode b, Class ca, Class cb)
  {
    AssemblyNode[] obArr = new AssemblyNode[2];
    if(simEvSrcClass.isAssignableFrom(ca))
      obArr[0] = a;
    else if(simEvSrcClass.isAssignableFrom(cb))
      obArr[0] = b;
    if(simEvLisClass.isAssignableFrom(cb))
      obArr[1] = b;
    else if(simEvLisClass.isAssignableFrom(ca))
      obArr[1] = a;

    if(obArr[0] == null | obArr[1] == null || obArr[0]==obArr[1])
      return null;
    return obArr;
  }

  public void pcListenerEdit(PropChangeListenerNode pclNode)
  //---------------------------------------
  {
    boolean modified = ((ViskitAssemblyView) getView()).doEditPclNode(pclNode);
    if (modified) {
      ((ViskitAssemblyModel) getModel()).changePclNode(pclNode);
    }
  }

  public void evGraphEdit(EvGraphNode evNode)
  {
    boolean modified = ((ViskitAssemblyView)getView()).doEditEvGraphNode(evNode);
    if(modified) {
      ((ViskitAssemblyModel)getModel()).changeEvGraphNode(evNode);
    }
  }

  public void pcListenerEdgeEdit(PropChangeEdge pclEdge)
  {
    boolean modified = ((ViskitAssemblyView) getView()).doEditPclEdge(pclEdge);
    if (modified) {
      ((ViskitAssemblyModel) getModel()).changePclEdge(pclEdge);
    }
  }

  public void adapterEdgeEdit(AdapterEdge aEdge)
  {
    boolean modified = ((ViskitAssemblyView) getView()).doEditAdapterEdge(aEdge);
    if (modified) {
      ((ViskitAssemblyModel)getModel()).changeAdapterEdge(aEdge);
    }
  }

  public void simEvListenerEdgeEdit(SimEvListenerEdge seEdge)
  {
    boolean modified = ((ViskitAssemblyView)getView()).doEditSimEvListEdge(seEdge);
    if (modified) {
      ((ViskitAssemblyModel)getModel()).changeSimEvEdge(seEdge);
    }
  }
  private Vector selectionVector = new Vector();

  public void selectNodeOrEdge(Vector v)
  //------------------------------------
  {
    selectionVector = v;
    boolean ccbool = (selectionVector.size() > 0 ? true : false);
    ActionIntrospector.getAction(this, "copy").setEnabled(ccbool);
    ActionIntrospector.getAction(this, "cut").setEnabled(ccbool);
  }

  private Vector copyVector = new Vector();

  public void copy()
  //----------------
  {
    if (selectionVector.size() <= 0)
      return;
    copyVector = (Vector) selectionVector.clone();
    ActionIntrospector.getAction(this,"paste").setEnabled(true);
  }
  int copyCount=0;
  public void paste()
  //-----------------
  {
    if (copyVector.size() <= 0)
      return;
    int x=100,y=100; int n=0;
    // We only paste un-attached nodes (at first)
    for(Iterator itr = copyVector.iterator(); itr.hasNext();) {
      Object o = itr.next();
      if(o instanceof AssemblyEdge)
        continue;
      if(o instanceof EvGraphNode) {
        String nm = ((EvGraphNode)o).getName();
        String typ = ((EvGraphNode)o).getType();
        ((ViskitAssemblyModel)getModel()).newEventGraph(nm+"-copy"+copyCount++,typ,new Point(x+(20*n),y+(20*n)));
      }
      else if (o instanceof PropChangeListenerNode) {
        String nm = ((PropChangeListenerNode)o).getName();
        String typ = ((PropChangeListenerNode)o).getType();
        ((ViskitAssemblyModel)getModel()).newPropChangeListener(nm+"-copy"+copyCount++,typ,new Point(x+(20*n),y+(20*n)));
      }
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
        if(o instanceof EvGraphNode || o instanceof PropChangeListenerNode)
          nodeCount++;
        String s = o.toString();
        s = s.replace('\n', ' ');
        msg += ", \n" + s;
      }
      String specialNodeMsg = (nodeCount > 0 ? "\n(All unselected but attached edges will also be deleted.)" : "");
      if (((ViskitAssemblyView) getView()).genericAsk("Delete element(s)?", "Confirm remove" + msg + "?" + specialNodeMsg)
       == JOptionPane.YES_OPTION) {
        // do edges first?
        Vector localV = (Vector) selectionVector.clone();   // avoid concurrent update
        for (Iterator itr = localV.iterator(); itr.hasNext();) {
          Object elem = itr.next();
          if(elem instanceof AssemblyEdge) {
            killEdge((AssemblyEdge)elem);
          }
          else if(elem instanceof EvGraphNode) {
            EvGraphNode en = (EvGraphNode)elem;
            for (Iterator it2 = en.getConnections().iterator(); it2.hasNext();) {
              AssemblyEdge ed = (AssemblyEdge) it2.next();
              killEdge(ed);
            }
            ((ViskitAssemblyModel) getModel()).deleteEvGraphNode(en);
          }
          else if(elem instanceof PropChangeListenerNode) {
            PropChangeListenerNode en = (PropChangeListenerNode)elem;
            for (Iterator it2 = en.getConnections().iterator(); it2.hasNext();) {
              AssemblyEdge ed = (AssemblyEdge) it2.next();
              killEdge(ed);
            }
            ((ViskitAssemblyModel) getModel()).deletePCLNode(en);
          }
        }
      }
    }
  }

  private void killEdge(AssemblyEdge e)
  {
    if (e instanceof AdapterEdge)
      ((ViskitAssemblyModel) getModel()).deleteAdapterEdge((AdapterEdge) e);
    else if (e instanceof PropChangeEdge)
      ((ViskitAssemblyModel) getModel()).deletePropChangeEdge((PropChangeEdge) e);
    else if (e instanceof SimEvListenerEdge)
      ((ViskitAssemblyModel) getModel()).deleteSimEvLisEdge((SimEvListenerEdge) e);
  }


  /********************************/


  /* from menu:*/
  public void generateJavaSource()
  {
    String source = produceJavaClass();
    if(source != null && source.length() > 0)
      ((ViskitAssemblyView)getView()).showAndSaveSource(source);
  }

  private String produceJavaClass()
  {
    if(((ViskitAssemblyModel)getModel()).isDirty()) {
      int ret = JOptionPane.showConfirmDialog(null,"The model will be saved.\nContinue?","Confirm",JOptionPane.YES_NO_OPTION);
      if(ret != JOptionPane.YES_OPTION)
        return null;

      this.saveAs();
    }
    return buildJavaAssemblySource(((ViskitAssemblyModel)getModel()).getFile());
  }

  // above are routines to operate on current assembly

  public static String buildJavaAssemblySource(File f)
  {
    try {
      SimkitAssemblyXML2Java x2j = new SimkitAssemblyXML2Java(f);
      x2j.unmarshal();
      return x2j.translate();
     }
     catch (Exception e) {
       e.printStackTrace();
     }
     return null;
  }
  public static String buildJavaEventGraphSource(File f)
  {
    try {
      SimkitXML2Java x2j = new SimkitXML2Java(f);
      x2j.unmarshal();
      return x2j.translate();
     }
     catch (Exception e) {
       e.printStackTrace();
     }
     return null;
  }
  public static File compileJavaClassFromStringAndHandleDependencies(String src)
  {
    handleFileBasedClasses();
    return compileJavaClassFromString(src);
  }

  public static File compileJavaClassFromString(String src)
  {
    String baseName=null;

    // Find the package subdirectory
    Pattern pat = Pattern.compile("package.+;");
    Matcher mat = pat.matcher(src);
    boolean fnd = mat.find();

    String packagePath = "";
    if(fnd) {
      int st = mat.start();
      int end = mat.end();
      String s = src.substring(st,end);
      s = s.replace(';','/');
      String[] sa = s.split("\\s");
      sa[1] = sa[1].replace('.','/');
      packagePath = sa[1].trim();
    }
    // done finding the package subdir (just to mark the file as "deleteOnExit")

    pat = Pattern.compile("public\\s+class\\s+");
    mat = pat.matcher(src);
    fnd = mat.find();
   // if(fnd) {
      int end = mat.end();
      String s = src.substring(end,end+128).trim();
      String[]sa = s.split("\\s+");

      baseName = sa[0];
   // }
    try {
      //String baseName = currentFile.getName().substring(0,currentFile.getName().indexOf('.'));
      File f = VGlobals.instance().getWorkDirectory();
      f = new File(f,baseName+".java");
      f.createNewFile();
      f.deleteOnExit();

      FileWriter fw = new FileWriter(f);
      fw.write(src);
      fw.flush();
      fw.close();

      //String cp = System.getProperty("java.class.path");
      String cp = getCustomClassPath();
     // cp = f.getParent()+System.getProperty("path.separator") + cp;
      int reti =  com.sun.tools.javac.Main.compile(new String[]{"-verbose", "-classpath",cp,"-d", f.getParent(), f.getCanonicalPath()});
      if(reti == 0)
        return new File(f.getParentFile().getAbsoluteFile(),packagePath+baseName+".class");
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /** do a temporary file gig to point to a java class file which we created from
   * a passed eventgraph xml file.
   * @param xmlFile
   * @return
   */
/*
  public static File xcreateTemporaryEventGraphClass(File xmlFile)
  {
    try {
      System.out.println("xunmarshalling "+xmlFile.getPath());
      dumpFile(xmlFile);
      SimkitXML2Java x2j = new SimkitXML2Java(xmlFile);
      x2j.unmarshal();
      String src = x2j.translate();
      return compileJavaClassFromString(src);
     }
     catch (Exception e) {
       e.printStackTrace();
     }
     return null;
  }
*/
  public static PkgAndFile createTemporaryEventGraphClass(File xmlFile)
  {
    try {
/*
      System.out.println("eg unmarshalling "+xmlFile.getPath());
      dumpFile(xmlFile);
      SimkitXML2Java x2j = new SimkitXML2Java(xmlFile);
      x2j.unmarshal();
      String src = x2j.translate();
*/
      String src = buildJavaEventGraphSource(xmlFile);
      return compileJavaClassAndSetPackage(src);
     }
     catch (Exception e) {
       e.printStackTrace();
     }
     return null;

  }

/*
  private static void dumpFile(File f)
  {
    try {
      BufferedReader br = new BufferedReader(new FileReader(f));
      while(br.ready()) {
        System.out.println(br.readLine());
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
*/

  public static PkgAndFile compileJavaClassAndSetPackage(String source)
  {
    String pkg = null;
    if (source != null && source.length() > 0) {
      Pattern p = Pattern.compile("package.*;");
      Matcher m = p.matcher(source);
      if (m.find()) {
        String nuts = m.group();
        if (nuts.endsWith(";"))
          nuts = nuts.substring(0, nuts.length() - 1);

        String[] sa = nuts.split("\\s");
        pkg = sa[1];
      }
      File f = compileJavaClassFromString(source);
      if (f != null) {
        f.deleteOnExit();
        return new PkgAndFile(pkg, f);
      }
    }
    return null;
  }


  // From menu
  public void runAssembly()
  {
    // These have to be on the classpath:
    // done abovehandleFileBasedClasses();

    String src = produceJavaClass();                   // asks to save
    PkgAndFile paf = compileJavaClassAndSetPackage(src);
    if(paf != null) {
      File f = paf.f;
      String clNam = f.getName().substring(0,f.getName().indexOf('.'));
      clNam = paf.pkg + "." + clNam;

      String classPath = getCustomClassPath();

      String[] execStrings = buildExecStrings(clNam,classPath);

      try {
        Runtime.getRuntime().exec(execStrings);
      }
      catch (IOException e) {
        e.printStackTrace();
      }


    }
    else
      JOptionPane.showMessageDialog(null,"Error on compile");         //todo, more information
  }
  static String getCustomClassPath()
  {
    String classPath = System.getProperty("java.class.path");
    File base = VGlobals.instance().getWorkDirectory();
    if(classPath.indexOf(base.getAbsolutePath()) == -1)
      classPath = base.getAbsolutePath() + Vstatics.getPathSeparator() + classPath;
    return classPath;
  }

  private static void handleFileBasedClasses()
  {
    Collection fileClasses = FileBasedClassManager.inst().getFileLoadedClasses();
    for (Iterator itr = fileClasses.iterator(); itr.hasNext();) {
      FileBasedAssyNode fbn = (FileBasedAssyNode) itr.next();
      if(fbn.isXML) {
        createTemporaryEventGraphClass(fbn.xmlSource);
      }
      else {
        moveClassFileIntoPlace(fbn);
      }
    }
  }

  private static void moveClassFileIntoPlace(FileBasedAssyNode fbn)
  {
    File f = new File(VGlobals.instance().getWorkDirectory(),
        fbn.pkg.replace('.',Vstatics.getFileSeparator().charAt(0)));
    f.mkdir();

    File target = new File(f,fbn.classFile.getName());
    try {
      target.createNewFile();

      BufferedInputStream is = new BufferedInputStream(new FileInputStream(fbn.classFile));
      BufferedOutputStream os= new BufferedOutputStream(new FileOutputStream(target));
      int b;
      while((b = is.read()) != -1)
        os.write(b);
      is.close();
      os.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }

  }

  private String[] buildExecStrings(String className, String classPath)
  {
    Vector v = new Vector();
    String fsep = Vstatics.getFileSeparator();

     StringBuffer sb = new StringBuffer();
     sb.append(System.getProperty("java.home"));
     sb.append(fsep+"bin"+fsep+"java");
    v.add(sb.toString());

    v.add("-cp");
    v.add(classPath);
    v.add("viskit.ExternalAssemblyRunner");
    v.add(className);

    v.add(""+((ViskitAssemblyModel) getModel()).getMetaData().verbose);
    v.add(((ViskitAssemblyModel) getModel()).getMetaData().stopTime);

    Vector vec = ((ViskitAssemblyModel)getModel()).getVerboseEntityNames();
    for (Iterator itr = vec.iterator(); itr.hasNext();) {
      v.add(itr.next());
    }

    String[] ra = new String[v.size()];
    return (String[])v.toArray(ra);
  }
}
class PkgAndFile
{
  String pkg;
  File f;

  PkgAndFile(String pkg, File f)
  {
    this.pkg = pkg;
    this.f = f;
  }
}
