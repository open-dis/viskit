package viskit.model;

import viskit.mvc.mvcAbstractModel;
import viskit.xsd.bindings.assembly.*;
import viskit.ModelEvent;

import javax.swing.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.awt.*;
import java.io.File;
import java.util.HashMap;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 17, 2004
 * Time: 9:16:44 AM
 */
public class AssemblyModel  extends mvcAbstractModel implements ViskitAssemblyModel
{
  private JAXBContext jc;
  private ObjectFactory oFactory;
  private SimkitAssembly jaxbRoot;
  private File currentFile;
  private boolean modelDirty = false;
  private GraphMetaData metaData;
  HashMap nodeCache = new HashMap();
  HashMap assEdgeCache = new HashMap();
  public void init()
  {
    try {
      jc = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
      oFactory = new ObjectFactory();
      jaxbRoot = oFactory.createSimkitAssembly(); // to start with empty graph
    }
    catch (JAXBException e) {
      JOptionPane.showMessageDialog(null,"Exception on JAXBContext instantiation" +
                                 "\n"+ e.getMessage(),
                                 "XML Error",JOptionPane.ERROR_MESSAGE);
    }
  }

  public void newEventGraph(String widgetName, String className, Point  p)
  {
    EvGraphNode node = new EvGraphNode(widgetName,className);
    if (p == null)
      node.setPosition(new Point(100,100));
    else
      node.setPosition(p);

    SimEntity jaxbEG = null;
    try {
      jaxbEG = oFactory.createSimEntity();
    }
    catch (JAXBException e) {
      //assert false : "Model.newEvent, error creating viskit.xsd.bindings.Event.";
      System.err.println("AssemblyModel.newEventGraph, error creating viskit.xsd.bindings.assembly.SimEntity.");
      return;
    }
    jaxbEG.setName(nIe(widgetName));
    node.opaqueModelObject = jaxbEG;
    jaxbEG.setType(className);
    nodeCache.put(jaxbEG,node);   // key = ev
    jaxbRoot.getSimEntity().add(jaxbEG);

    modelDirty = true;
    notifyChanged(new ModelEvent(node,ModelEvent.EVENTGRAPHADDED, "Event graph added to assembly"));

  }

  public void newPropChangeListener(String widgetName, String className, Point p)
  {
    PropChangeListenerNode pcNode = new PropChangeListenerNode(widgetName,className);
    if(p == null)
      pcNode.setPosition(new Point(100,100));
    else
      pcNode.setPosition(p);
    
    PropertyChangeListener jaxbPCL = null;
    try {
      jaxbPCL = oFactory.createPropertyChangeListener();
    }
    catch (JAXBException e) {
      // assert false: "AssemblyModel.newPropChangeListener, error creating viskit.xsd.bindings.assembly.PropChangeListener.";
      System.err.println("AssemblyModel.newPropChangeListener, error creating viskit.xsd.bindings.assembly.PropChangeListener.");
      return;
    }
    jaxbPCL.setName(nIe(widgetName));
    jaxbPCL.setType(className);
    pcNode.opaqueModelObject = jaxbPCL;
    nodeCache.put(jaxbPCL,pcNode);
    jaxbRoot.getSimEntity().add(jaxbPCL);

    modelDirty = true;
    notifyChanged(new ModelEvent(pcNode,ModelEvent.PCLADDED, "Property Change Node added to assembly"));    
  }

  public void newAdapterEdge (Object src, Object target)
  {
    AdapterEdge ae = new AdapterEdge();
    ae.from = src;
    ae.to   = target;
    Object srcOMO,targetOMO;
    if(src instanceof EvGraphNode) {
      ((EvGraphNode)src).getConnections().add(ae);
      srcOMO = ((EvGraphNode)src).opaqueModelObject;
    }
    else {
      ((PropChangeListenerNode)src).getConnections().add(ae);
      srcOMO = ((PropChangeListenerNode)src).opaqueModelObject;
    }
    if(target instanceof EvGraphNode) {
      ((EvGraphNode)target).getConnections().add(ae);
      targetOMO = ((EvGraphNode)target).opaqueModelObject;
    }
    else {
      ((PropChangeListenerNode)target).getConnections().add(ae);
      targetOMO = ((PropChangeListenerNode)target).opaqueModelObject;
    }

    SimEventListenerConnection selc; // no such thing as "AdapterConnection"?
    try {
      selc = oFactory.createSimEventListenerConnection();
    }
    catch (JAXBException e) {
      //assert false : "AssemblyModel.newAdapterEdge, error creating viskit.xsd.bindings.assembly.SimEventListenerConnection.";
      System.err.println("AssemblyModel.newAdapterEdge, error creating viskit.xsd.bindings.assembly.SimEventListenerConnection.");
      return;
    }
    ae.opaqueModelObject = selc;

    selc.setListener(targetOMO);
    selc.setSource(srcOMO);

    assEdgeCache.put(selc,ae);
    modelDirty = true;

    this.notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGEADDED, "Adapter edge added"));
  }

  public void newPclEdge(EvGraphNode src, PropChangeListenerNode target)
  {
    PropChangeEdge pce = new PropChangeEdge();
    pce.from = src;
    pce.to = target;

    src.getConnections().add(pce);
    target.getConnections().add(pce);

    PropertyChangeListenerConnection pclc;
    try {
      pclc = oFactory.createPropertyChangeListenerConnection();
    }
    catch (JAXBException e) {
      //assert false : "AssemblyModel.newPclEdge, error creating viskit.xsd.bindings.assembly.PropertyChangeListenerConnection.";
      System.err.println("AssemblyModel.newPclEdge, error creating viskit.xsd.bindings.assembly.PropertyChangeListenerConnection.");
      return;
    }
    pce.opaqueModelObject = pclc;
    PropertyChangeListener targL = (PropertyChangeListener) target.opaqueModelObject;
    pclc.setListener(targL);
    SimEntity sent = (SimEntity) src.opaqueModelObject;
    pclc.setSource(sent);


    assEdgeCache.put(pclc, pce);
    modelDirty = true;

    this.notifyChanged(new ModelEvent(pce, ModelEvent.PCLEDGEADDED, "PCL edge added"));

  }

  public void newSimEvLisEdge (Object src, Object target){
    SimEvListenerEdge sele = new SimEvListenerEdge();
    //AdapterEdge ae = new AdapterEdge();
    sele.from = src;
    sele.to   = target;
    Object srcOMO,targetOMO;
    if(src instanceof EvGraphNode) {
      ((EvGraphNode)src).getConnections().add(sele);
      srcOMO = ((EvGraphNode)src).opaqueModelObject;
    }
    else {
      ((PropChangeListenerNode)src).getConnections().add(sele);
      srcOMO = ((PropChangeListenerNode)src).opaqueModelObject;
    }
    if(target instanceof EvGraphNode) {
      ((EvGraphNode)target).getConnections().add(sele);
      targetOMO = ((EvGraphNode)target).opaqueModelObject;
    }
    else {
      ((PropChangeListenerNode)target).getConnections().add(sele);
      targetOMO = ((PropChangeListenerNode)target).opaqueModelObject;
    }

    SimEventListenerConnection selc; // no such thing as "AdapterConnection"?
    try {
      selc = oFactory.createSimEventListenerConnection();
    }
    catch (JAXBException e) {
      //assert false : "AssemblyModel.newAdapterEdge, error creating viskit.xsd.bindings.assembly.SimEventListenerConnection.";
      System.err.println("AssemblyModel.newSimEvLisEdge, error creating viskit.xsd.bindings.assembly.SimEventListenerConnection.");
      return;
    }
    sele.opaqueModelObject = selc;

    selc.setListener(targetOMO);
    selc.setSource(srcOMO);

    assEdgeCache.put(selc,sele);
    modelDirty = true;

    this.notifyChanged(new ModelEvent(sele, ModelEvent.SIMEVLISTEDGEADDED, "SimEvList edge added"));

  }

  public void changeEvGNode(EvGraphNode node)
  {
    SimEntity jaxbSE = (SimEntity)node.opaqueModelObject;

    jaxbSE.setName(node.getName());




 //todo
/*
    Coordinate coor = null;
    try {
      coor = oFactory.createCoordinate();
    } catch(JAXBException e) {
      System.err.println("Exc Model.changeEvent()");
    }
    coor.setX(""+node.getPosition().x);
    coor.setY(""+node.getPosition().y);
    jaxbSE.set
    jaxbEv.setCoordinate(coor);

    cloneComments(jaxbEv.getComment(),node.getComments());
    cloneArguments(jaxbEv.getArgument(),node.getArguments());
    cloneLocalVariables(jaxbEv.getLocalVariable(),node.getLocalVariables());
    // following must follow above
    cloneTransitions(jaxbEv.getStateTransition(),node.getTransitions(),jaxbEv.getLocalVariable());
*/

    modelDirty = true;
    this.notifyChanged(new ModelEvent(node, ModelEvent.EVENTGRAPHCHANGED, "Event changed"));
  }

  public void newModel(File f)
  {
    if (f == null) {
      try {
        jaxbRoot = oFactory.createSimkitAssembly(); // to start with empty graph
      }
      catch (JAXBException e) {
        JOptionPane.showMessageDialog(null,"Exception on JAXBContext instantiation" +
                                   "\n"+ f.getName() +
                                   "\n"+ e.getMessage(),
                                   "XML I/O Error",JOptionPane.ERROR_MESSAGE);
        return;
      }
/* todo
      VGlobals.instance().reset();
      stateVariables.removeAllElements();
      simParameters.removeAllElements();
      done..evNodeCache.clear();
      done..edgeCache.clear();
      done ..metaData = new GraphMetaData();
      done..this.notifyChanged(new ModelEvent(this, ModelEvent.NEWMODEL, "New empty model"));
*/
      nodeCache.clear();
      assEdgeCache.clear();
      metaData = new GraphMetaData(); //todo need new object?
      metaData.name = "Assembly_name"; // override
      this.notifyChanged(new ModelEvent(this, ModelEvent.NEWASSEMBLYMODEL, "New empty assembly model"));
    }
    else {
/*      try {
        Unmarshaller u = jc.createUnmarshaller();
        // u.setValidating(true); can't do this, the unmarshaller needs to have this capability..
        // see u.isValidating()
        // Unmarshaller does NOT validate by default
        jaxbRoot = (SimEntity) u.unmarshal(f);
        metaData = new GraphMetaData();
        metaData.author = jaxbRoot.getAuthor();
        metaData.version = jaxbRoot.getVersion();
        metaData.name = jaxbRoot.getName();
        metaData.pkg = jaxbRoot.getPackage();
        List lis = jaxbRoot.getComment();
        StringBuffer sb = new StringBuffer("");
        for(Iterator itr = lis.iterator(); itr.hasNext();) {
          sb.append((String)itr.next());
          sb.append(" ");
        }
        metaData.comment = sb.toString().trim();

        VGlobals.instance().reset();
        stateVariables.removeAllElements();
        simParameters.removeAllElements();
        evNodeCache.clear();
        edgeCache.clear();
        this.notifyChanged(new ModelEvent(this, ModelEvent.NEWMODEL, "New model loaded from file"));

        buildEventsFromJaxb(jaxbRoot.getEvent());
        buildParametersFromJaxb(jaxbRoot.getParameter());
        buildStateVariablesFromJaxb(jaxbRoot.getStateVariable());

      }
      catch (JAXBException e) {
        JOptionPane.showMessageDialog(null,"Exception on JAXB unmarshalling" +
                                   "\n"+ f.getName() +
                                   "\n"+ e.getMessage(),
                                   "XML I/O Error",JOptionPane.ERROR_MESSAGE);

        return;
      }

*/
    }
    currentFile = f;
    modelDirty = false;
  }
  /**
    * Boolean to signify whether the model has been changed since last disk save.
    *
    * @return true means changes have been made and it needs to be flushed.
    */
   public boolean isDirty()
   {
     return modelDirty;
   }

  public GraphMetaData getMetaData()
  {
    return metaData;
  }
  public void changeMetaData   (GraphMetaData gmd)
  {
    metaData = gmd;
  }

  /**
   *   "nullIfEmpty" Return the passed string if non-zero length, else null
   */
  private String nIe(String s)
  {
    if(s != null)
      if(s.length() == 0)
        s = null;
    return s;
  }

}
