package viskit.model;

import viskit.ModelEvent;
import viskit.VGlobals;
import viskit.mvc.mvcAbstractModel;
import viskit.xsd.assembly.SimkitAssemblyXML2Java;
import viskit.xsd.bindings.assembly.*;

import javax.swing.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

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
  public static final String schemaLoc = "http://diana.gl.nps.navy.mil/Simkit/assembly.xsd";
  private Point pointLess = new Point(100,100);
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

  public void saveModel(File f)
  {

    if(f == null)
      f = currentFile;
     try {
       FileWriter fw = new FileWriter(f);
       Marshaller m = jc.createMarshaller();
       m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,new Boolean(true));
       m.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION,schemaLoc);

       String nm = f.getName();
       int dot=-1;
       if((dot=nm.indexOf('.')) != -1)
         nm = nm.substring(0,dot);

       jaxbRoot.setName(nIe(metaData.name));
       jaxbRoot.setVersion(nIe(metaData.version));
       jaxbRoot.setPackage(nIe(metaData.pkg));
       if(jaxbRoot.getSchedule() == null) {
         jaxbRoot.setSchedule(oFactory.createSchedule());
       }
       jaxbRoot.getSchedule().setStopTime(metaData.stopTime);
       jaxbRoot.getSchedule().setVerbose(""+metaData.verbose);

/*
       jaxbRoot.setAuthor(nIe(metaData.author));

       java.util.List clis = jaxbRoot.getComment();
       clis.clear();;
       String cmt = nIe(metaData.comment);
       if(cmt != null)
         clis.add(cmt.trim());
*/

       m.marshal(jaxbRoot,fw);
       fw.close();

       modelDirty = false;
       currentFile = f;
     }
     catch (JAXBException e) {
       JOptionPane.showMessageDialog(null,"Exception on JAXB marshalling" +
                                  "\n"+ f.getName() +
                                  "\n"+ e.getMessage() +
                                  "\n(check for blank data fields)",
                                  "XML I/O Error",JOptionPane.ERROR_MESSAGE);
       return;
     }
     catch (IOException ex) {
       JOptionPane.showMessageDialog(null,"Exception on writing "+ f.getName() +
                                  "\n"+ ex.getMessage(),
                                  "File I/O Error",JOptionPane.ERROR_MESSAGE);
       return;
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
    jaxbRoot.getPropertyChangeListener().add(jaxbPCL);

    modelDirty = true;
    notifyChanged(new ModelEvent(pcNode,ModelEvent.PCLADDED, "Property Change Node added to assembly"));    
  }

  public AdapterEdge newAdapterEdge (EvGraphNode src, EvGraphNode target)
  {
    AdapterEdge ae = new AdapterEdge();
    ae.setFrom(src);
    ae.setTo(target);
    src.getConnections().add(ae);
    target.getConnections().add(ae);

    Adapter jaxbAdapter;
    try {
      jaxbAdapter = oFactory.createAdapter();
    }
    catch (JAXBException e) {
      //assert false : "AssemblyModel.newAdapterEdge, error creating viskit.xsd.bindings.assembly.SimEventListenerConnection.";
      System.err.println("AssemblyModel.newAdapterEdge, error creating viskit.xsd.bindings.assembly.SimEventListenerConnection.");
      return null;
    }
    ae.opaqueModelObject = jaxbAdapter;
    jaxbAdapter.setTo(target.opaqueModelObject);
    jaxbAdapter.setFrom(src.opaqueModelObject);

    jaxbAdapter.setName("requiredadaptername");

    assEdgeCache.put(jaxbAdapter,ae);
    jaxbRoot.getAdapter().add(jaxbAdapter);

    modelDirty = true;

    this.notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGEADDED, "Adapter edge added"));
    return ae;
  }

  public PropChangeEdge newPclEdge(EvGraphNode src, PropChangeListenerNode target)
  {
    PropChangeEdge pce = new PropChangeEdge();
    pce.setFrom(src);
    pce.setTo(target);

    src.getConnections().add(pce);
    target.getConnections().add(pce);

    PropertyChangeListenerConnection pclc;
    try {
      pclc = oFactory.createPropertyChangeListenerConnection();
    }
    catch (JAXBException e) {
      //assert false : "AssemblyModel.newPclEdge, error creating viskit.xsd.bindings.assembly.PropertyChangeListenerConnection.";
      System.err.println("AssemblyModel.newPclEdge, error creating viskit.xsd.bindings.assembly.PropertyChangeListenerConnection.");
      return null;
    }
    pce.opaqueModelObject = pclc;
    PropertyChangeListener targL = (PropertyChangeListener) target.opaqueModelObject;
    pclc.setListener(targL);
    SimEntity sent = (SimEntity) src.opaqueModelObject;
    pclc.setSource(sent);


    assEdgeCache.put(pclc, pce);
    jaxbRoot.getPropertyChangeListenerConnection().add(pclc);
    modelDirty = true;

    this.notifyChanged(new ModelEvent(pce, ModelEvent.PCLEDGEADDED, "PCL edge added"));
    return pce;
  }

  public void newSimEvLisEdge (EvGraphNode src, EvGraphNode target){
    SimEvListenerEdge sele = new SimEvListenerEdge();
    sele.setFrom(src);
    sele.setTo(target);
    src.getConnections().add(sele);
    target.getConnections().add(sele);

    SimEventListenerConnection selc;
    try {
      selc = oFactory.createSimEventListenerConnection();
    }
    catch (JAXBException e) {
      //assert false : "AssemblyModel.newSimEvLisEdge, error creating viskit.xsd.bindings.assembly.SimEventListenerConnection.";
      System.err.println("AssemblyModel.newSimEvLisEdge, error creating viskit.xsd.bindings.assembly.SimEventListenerConnection.");
      return;
    }
    sele.opaqueModelObject = selc;

    selc.setListener(target.opaqueModelObject);
    selc.setSource(src.opaqueModelObject);

    assEdgeCache.put(selc,sele);
    jaxbRoot.getSimEventListenerConnection().add(selc);

    modelDirty = true;
    notifyChanged(new ModelEvent(sele, ModelEvent.SIMEVLISTEDGEADDED, "SimEvList edge added"));
  }

  public void deleteEvGraphNode(EvGraphNode evNode)
  {
    SimEntity jaxbEv = (SimEntity)evNode.opaqueModelObject;
    nodeCache.remove(jaxbEv);
    jaxbRoot.getSimEntity().remove(jaxbEv);

    modelDirty = true;
    this.notifyChanged(new ModelEvent(evNode, ModelEvent.EVENTGRAPHDELETED, "Event graph deleted"));
  }

  public void deletePCLNode(PropChangeListenerNode pclNode)
  {
    PropertyChangeListener jaxbPcNode = (PropertyChangeListener)pclNode.opaqueModelObject;
    nodeCache.remove(pclNode);
    jaxbRoot.getPropertyChangeListener().remove(jaxbPcNode);

    modelDirty = true;
    this.notifyChanged(new ModelEvent(pclNode, ModelEvent.PCLDELETED, "Property Change Listener deleted"));
  }

  /**
   *  Assembly nodes don't hold onto edges.
   */

  public void deletePropChangeEdge(PropChangeEdge pce)
  {
    PropertyChangeListenerConnection pclc  = (PropertyChangeListenerConnection)pce.opaqueModelObject;

    assEdgeCache.remove(pce);
    jaxbRoot.getPropertyChangeListenerConnection().remove(pclc);

    modelDirty = true;
    this.notifyChanged(new ModelEvent(pce, ModelEvent.PCLEDGEDELETED, "PCL edge deleted"));
  }

  public void deleteSimEvLisEdge(SimEvListenerEdge sele)
  {
    SimEventListenerConnection sel_c = (SimEventListenerConnection)sele.opaqueModelObject;

    assEdgeCache.remove(sele);
    jaxbRoot.getSimEventListenerConnection().remove(sel_c);

    modelDirty = true;
    this.notifyChanged(new ModelEvent(sele, ModelEvent.SIMEVLISTEDGEDELETED, "SimEvList edge deleted"));
  }

  public void deleteAdapterEdge(AdapterEdge ae)
  {
    Adapter j_adp = (Adapter)ae.opaqueModelObject;
    assEdgeCache.remove(ae);
    jaxbRoot.getAdapter().remove(j_adp);

    modelDirty = true;
    notifyChanged(new ModelEvent(ae,ModelEvent.ADAPTEREDGEDELETED, "Adapter edge deleted"));
  }

  public void changePclEdge(PropChangeEdge pclEdge)
  {
    PropertyChangeListenerConnection pclc = (PropertyChangeListenerConnection)pclEdge.opaqueModelObject;
    //pclc.setListener(targ.opaqueModelObject);  never changes
    pclc.setProperty(pclEdge.getProperty());

    modelDirty = true;
    notifyChanged(new ModelEvent(pclEdge,ModelEvent.PCLEDGECHANGED, "PCL edge changed"));
  }

  public void changeAdapterEdge(AdapterEdge ae)
  {
    EvGraphNode src = (EvGraphNode)ae.getFrom();
    EvGraphNode targ = (EvGraphNode)ae.getTo();

    Adapter jaxbAE = (Adapter)ae.opaqueModelObject;

    jaxbAE.setFrom((SimEntity)src.opaqueModelObject);
    jaxbAE.setTo((SimEntity)targ.opaqueModelObject);

    jaxbAE.setEventHeard(ae.getSourceEvent());
    jaxbAE.setEventSent(ae.getTargetEvent());

    modelDirty = true;
    notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGECHANGED, "Adapter edge changed"));
  }

  public void changeSimEvEdge(SimEvListenerEdge seEdge)
  {
    EvGraphNode src = (EvGraphNode)seEdge.getFrom();
    EvGraphNode targ = (EvGraphNode)seEdge.getTo();
    SimEventListenerConnection selc = (SimEventListenerConnection)seEdge.opaqueModelObject;

    selc.setListener(targ.opaqueModelObject);
    selc.setSource(src.opaqueModelObject);

    modelDirty = true;
    notifyChanged(new ModelEvent(seEdge,ModelEvent.SIMEVLISTEDGECHANGED, "SimEvListener edge changed"));
  }

  public void changePclNode(PropChangeListenerNode pclNode)
  {
    viskit.xsd.bindings.assembly.PropertyChangeListener jaxBPcl =  (viskit.xsd.bindings.assembly.PropertyChangeListener)pclNode.opaqueModelObject;
    jaxBPcl.setName(pclNode.getName());
    jaxBPcl.setType(pclNode.getType());
    viskit.xsd.bindings.assembly.Coordinate coor = null;
    try {
      coor = oFactory.createCoordinate();
    } catch(JAXBException e) {
      System.err.println("Exc AssemblyModel.changePclNode()");
      return;
    }
    coor.setX(""+pclNode.getPosition().x);
    coor.setY(""+pclNode.getPosition().y);
    jaxBPcl.setCoordinate(coor);


    List lis = jaxBPcl.getParameters();
    lis.clear();

    VInstantiator inst = pclNode.getInstantiator();

    List jlistt = getJaxbParamList(inst);
    for (Iterator itr = jlistt.iterator(); itr.hasNext();) {
      Object o = itr.next();
      lis.add(o);
    }

    modelDirty = true;
    this.notifyChanged(new ModelEvent(pclNode, ModelEvent.PCLCHANGED, "Property Change Listener node changed"));
  }

  public void changeEvGraphNode(EvGraphNode evNode)
  {
    SimEntity jaxbSE = (SimEntity)evNode.opaqueModelObject;

    jaxbSE.setName(evNode.getName());
    jaxbSE.setType(evNode.getType());
    viskit.xsd.bindings.assembly.Coordinate coor = null;
    try {
      coor = oFactory.createCoordinate();
    } catch(JAXBException e) {
      System.err.println("Exc AssemblyModel.changeEvGraphNode()");
      return;
    }
    coor.setX(""+evNode.getPosition().x);
    coor.setY(""+evNode.getPosition().y);
    jaxbSE.setCoordinate(coor);

    List lis = jaxbSE.getParameters();
    lis.clear();

    VInstantiator inst = evNode.getInstantiator();

    List jlistt = getJaxbParamList(inst);
    for (Iterator itr = jlistt.iterator(); itr.hasNext();) {
      Object o = itr.next();
      lis.add(o);
    }
    if(evNode.isOutputMarked())
      addToOutputList(jaxbSE);
    else
      removeFromOutputList(jaxbSE);
    
    modelDirty = true;
    this.notifyChanged(new ModelEvent(evNode, ModelEvent.EVENTGRAPHCHANGED, "Event changed"));
  }

  private void removeFromOutputList(SimEntity se)
  {
    List outTL = jaxbRoot.getOutput();
    for (Iterator itr = outTL.iterator(); itr.hasNext();) {
      Output o = (Output) itr.next();
      if(o.getEntity() == se) {
        outTL.remove(o);
        return;
      }
    }
  }

  private void addToOutputList(SimEntity se)
  {
    List outTL = jaxbRoot.getOutput();
    for (Iterator itr = outTL.iterator(); itr.hasNext();) {
      Output o = (Output) itr.next();
      if(o.getEntity() == se) {;
        return;
      }
    }
    Output op = null;
    try {
      op = oFactory.createOutput();
    }
    catch (JAXBException e) {
      System.out.println("Error in AssemblyModel.addToOutputList "+e);
      return;
    }
    op.setEntity(se);
    outTL.add(op);
  }

  private List getInstantiatorListFromJaxbParmList(List lis)
  {
    Vector v = new Vector();

    for (Iterator itr = lis.iterator(); itr.hasNext();) {
      v.add(buildInstantiatorFromJaxbParameter(itr.next()));
    }
    return v;
  }

  private VInstantiator buildInstantiatorFromJaxbParameter(Object o)
  {
    if(o instanceof TerminalParameter)
      return buildFreeFormFromTermParameter((TerminalParameter)o);
    if(o instanceof MultiParameter)
      return buildArrayFromMultiParameter((MultiParameter)o);
    if(o instanceof FactoryParameter)
      return buildFactoryInstFromFactoryParameter((FactoryParameter)o);
    else
      //assert false: "bad object, buildInstantiatorFromJaxbParameter
      return null;
  }

  private VInstantiator.Array buildArrayFromMultiParameter(MultiParameter o)
  {
    return new VInstantiator.Array(o.getType(),getInstantiatorListFromJaxbParmList(o.getParameters()));
  }

  private VInstantiator.Factory buildFactoryInstFromFactoryParameter(FactoryParameter o)
  {
    return new VInstantiator.Factory(o.getType(),o.getFactory(),
        "getInstance",getInstantiatorListFromJaxbParmList(o.getParameters()));
  }

  private VInstantiator.FreeF buildFreeFormFromTermParameter(TerminalParameter tp)
  {
    return new VInstantiator.FreeF(tp.getType(),tp.getValue());
  }

  private List getJaxbParamList(VInstantiator vi)
  {
    Object o = buildParam(vi);
    if(o instanceof List)
      return (List)o;
    Vector v = new Vector();
    v.add(o);
    return v;
  }

  private Object buildParam(VInstantiator vi)
  {
    if(vi instanceof VInstantiator.FreeF)
      return buildParmFromFreeF((VInstantiator.FreeF)vi);      //TerminalParm
    if(vi instanceof VInstantiator.Constr)
      return buildParmFromConstr((VInstantiator.Constr)vi);     // List of Parms
    if(vi instanceof VInstantiator.Factory)
      return buildParmFromFactory((VInstantiator.Factory)vi);   // FactoryParam
    if(vi instanceof VInstantiator.Array)
      return buildParmFromArray((VInstantiator.Array)vi);       // MultiParam

    //assert false : AssemblyModel.buildJaxbParameter() received null;
    return null; //

  }

  private TerminalParameter buildParmFromFreeF(VInstantiator.FreeF viff)
  {
    TerminalParameter tp = null;
    try {
      tp = oFactory.createTerminalParameter();
    }
    catch (JAXBException e) {
      System.err.println("jaxb error buildParmFromFreeF");
      return null;
    }
    tp.setType(viff.getType());
    tp.setValue(viff.getValue());
    return tp;
  }

  private Object buildParmFromConstr(VInstantiator.Constr vicon)
  {
    Vector v = new Vector();
    for (Iterator itr = vicon.getArgs().iterator(); itr.hasNext();) {
      VInstantiator vi = (VInstantiator) itr.next();
      v.add(buildParam(vi));
    }
    return v;
  }

  private FactoryParameter buildParmFromFactory(VInstantiator.Factory vifact)
  {
    FactoryParameter fp = null;
    try {
      fp = oFactory.createFactoryParameter();
    }
    catch (JAXBException e) {
      System.err.println("jaxb error buildParmFromFactory");
      return null;
    }
    fp.setType(vifact.getType());
    fp.setFactory(vifact.getFactoryClass()+"."+vifact.getMethod()+"()");
    for (Iterator itr = vifact.getParams().iterator(); itr.hasNext();) {
      VInstantiator vi = (VInstantiator) itr.next();
      fp.getParameters().add(buildParam(vi));
    }
    return fp;
  }

  private MultiParameter buildParmFromArray(VInstantiator.Array viarr)
  {
    MultiParameter mp = null;
    try {
      mp = oFactory.createMultiParameter();
    }
    catch (JAXBException e) {
      System.err.println("jaxb error buildParmFromArray");
      return null;

    }
    mp.setType(viarr.getType());
    for (Iterator itr = viarr.getInstantiators().iterator(); itr.hasNext();) {
      VInstantiator vi = (VInstantiator) itr.next();
      mp.getParameters().add(buildParam(vi));
    }
    return mp;
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

      VGlobals.instance().assemblyReset();
      nodeCache.clear();
      assEdgeCache.clear();
      pointLess = new Point(100,100);
      metaData = new GraphMetaData(); //todo need new object?
      metaData.name = "Assembly_name"; // override
      this.notifyChanged(new ModelEvent(this, ModelEvent.NEWASSEMBLYMODEL, "New empty assembly model"));
    }
    else {
      try {
        Unmarshaller u = jc.createUnmarshaller();
        // u.setValidating(true); can't do this, the unmarshaller needs to have this capability..
        // see u.isValidating()
        // Unmarshaller does NOT validate by default
        jaxbRoot = (SimkitAssembly) u.unmarshal(f);
        metaData = new GraphMetaData();
      //  metaData.author = jaxbRoot.getAuthor();
        metaData.version = jaxbRoot.getVersion();
        metaData.name = jaxbRoot.getName();
        metaData.pkg = jaxbRoot.getPackage();

        ScheduleType sch = jaxbRoot.getSchedule();
        if(sch != null) {
          metaData.stopTime = sch.getStopTime();
          metaData.verbose = sch.getVerbose().equalsIgnoreCase("true");
        }
/*        List lis = jaxbRoot.getComment();
        StringBuffer sb = new StringBuffer("");
        for(Iterator itr = lis.iterator(); itr.hasNext();) {
          sb.append((String)itr.next());
          sb.append(" ");
        }

        metaData.comment = sb.toString().trim();
*/
        VGlobals.instance().assemblyReset();
        nodeCache.clear();
        assEdgeCache.clear();
        this.notifyChanged(new ModelEvent(this, ModelEvent.NEWASSEMBLYMODEL, "New model loaded from file"));
        buildEGsFromJaxb(jaxbRoot.getSimEntity(),jaxbRoot.getOutput());
        buildPCLsFromJaxb(jaxbRoot.getPropertyChangeListener());

        buildPCConnectionsFromJaxb(jaxbRoot.getPropertyChangeListenerConnection());
        buildSimEvConnectionsFromJaxb(jaxbRoot.getSimEventListenerConnection());
        buildAdapterConnectionsFromJaxb(jaxbRoot.getAdapter());

      }
      catch (JAXBException e) {
        JOptionPane.showMessageDialog(null,"Exception on JAXB unmarshalling" +
                                   "\n"+ f.getName() +
                                   "\n"+ e.getMessage(),
                                   "XML I/O Error",JOptionPane.ERROR_MESSAGE);

        return;
      }


    }
    currentFile = f;
    modelDirty = false;
  }
  private void buildPCConnectionsFromJaxb(List pcconnsList)
  {
    for (Iterator itr = pcconnsList.iterator(); itr.hasNext();) {
      PropertyChangeListenerConnection pclc = (PropertyChangeListenerConnection) itr.next();
      PropChangeEdge pce = new PropChangeEdge();
      pce.setProperty(pclc.getProperty());
      AssemblyNode toNode = (AssemblyNode)nodeCache.get(pclc.getListener());
      AssemblyNode frNode = (AssemblyNode)nodeCache.get(pclc.getSource());
      pce.setTo(toNode);
      pce.setFrom(frNode);
      pce.opaqueModelObject = pclc;
      toNode.getConnections().add(pce);
      frNode.getConnections().add(pce);

      assEdgeCache.put(pclc, pce);
      this.notifyChanged(new ModelEvent(pce, ModelEvent.PCLEDGEADDED, "PCL edge added"));
    }
  }

  private void buildSimEvConnectionsFromJaxb(List simevconnsList)
  {
    for (Iterator itr = simevconnsList.iterator(); itr.hasNext();) {
      SimEventListenerConnection selc =  (SimEventListenerConnection)itr.next();
      SimEvListenerEdge sele = new SimEvListenerEdge();
      AssemblyNode toNode = (AssemblyNode)nodeCache.get(selc.getListener());
      AssemblyNode frNode = (AssemblyNode)nodeCache.get(selc.getSource());
      sele.setTo(toNode);
      sele.setFrom(frNode);
      sele.opaqueModelObject = selc;
      toNode.getConnections().add(sele);
      frNode.getConnections().add(sele);
      assEdgeCache.put(selc,sele);
      this.notifyChanged(new ModelEvent(sele,ModelEvent.SIMEVLISTEDGEADDED, "Sim event listener connection added"));
    }
  }
  private void buildAdapterConnectionsFromJaxb(List adaptersList)
  {
    for (Iterator itr = adaptersList.iterator(); itr.hasNext();) {
      Adapter jaxbAdapter = (Adapter) itr.next();
      AdapterEdge ae = new AdapterEdge();
      AssemblyNode toNode = (AssemblyNode)nodeCache.get(jaxbAdapter.getTo());
      AssemblyNode frNode = (AssemblyNode)nodeCache.get(jaxbAdapter.getFrom());
      ae.setTo(toNode);
      ae.setFrom(frNode);
      ae.setSourceEvent(jaxbAdapter.getEventSent());
      ae.setTargetEvent(jaxbAdapter.getEventHeard());
      ae.opaqueModelObject = jaxbAdapter;
      toNode.getConnections().add(ae);
      frNode.getConnections().add(ae);
      assEdgeCache.put(jaxbAdapter,ae);
      this.notifyChanged(new ModelEvent(ae,ModelEvent.ADAPTEREDGEADDED, "Adapter connection added"));
    }
  }
  private void buildPCLsFromJaxb(List pcLs)
  {
    for (Iterator itr = pcLs.iterator(); itr.hasNext();) {
      PropertyChangeListener pcl = (PropertyChangeListener) itr.next();
      buildPclNodeFromJaxbPCL(pcl);
    }
  }

  private void buildEGsFromJaxb(List simEntities, List outputList)
  {
    for (Iterator itr = simEntities.iterator(); itr.hasNext();) {
      SimEntity se = (SimEntity) itr.next();
      boolean isOutput=false;

      // This must be done in this order, because the buildEvgNode...below
      // causes AssembleModel to be reentered, and the outputList gets hit.
      for (Iterator outIt = outputList.iterator(); outIt.hasNext();) {
        Output o = (Output) outIt.next();
        SimEntity simE = (SimEntity)o.getEntity();
        if(simE == se) {
          isOutput=true;;
          break;
        }
      }
      buildEvgNodeFromJaxbSimEntity(se,isOutput);
    }
  }

  private PropChangeListenerNode buildPclNodeFromJaxbPCL(PropertyChangeListener pcl)
  {
    PropChangeListenerNode pNode = (PropChangeListenerNode)nodeCache.get(pcl);
    if(pNode != null) {
      return pNode;
    }
    pNode = new PropChangeListenerNode(pcl.getName(),pcl.getType());
    CoordinateType coor = pcl.getCoordinate();
    if(coor == null) {
      pNode.setPosition(pointLess);
      pointLess = new Point(pointLess.x+20,pointLess.y+20);
    }
    else
      pNode.setPosition(new Point(Integer.parseInt(coor.getX()),
                             Integer.parseInt(coor.getY())));
    List lis = pcl.getParameters();
    VInstantiator.Constr vc = new VInstantiator.Constr(pcl.getType(),
                      getInstantiatorListFromJaxbParmList(lis));
    pNode.setInstantiator(vc);

    pNode.opaqueModelObject = pcl;
    nodeCache.put(pcl,pNode);   // key = se

    notifyChanged(new ModelEvent(pNode,ModelEvent.PCLADDED, "PCL added"));

    return pNode;
  }

  private EvGraphNode buildEvgNodeFromJaxbSimEntity(SimEntity se, boolean isOutputNode)
  {
    EvGraphNode en = (EvGraphNode)nodeCache.get(se);
    if(en != null) {
      return en;
    }
    en = new EvGraphNode(se.getName(),se.getType());

    CoordinateType coor = se.getCoordinate();
    if(coor == null) {
      en.setPosition(pointLess);
      pointLess = new Point(pointLess.x+20,pointLess.y+20);
    }
    else
      en.setPosition(new Point(Integer.parseInt(coor.getX()),
                             Integer.parseInt(coor.getY())));

    en.setOutputMarked(isOutputNode);
    List lis = se.getParameters();
    VInstantiator.Constr vc = new VInstantiator.Constr(se.getType(),
                    getInstantiatorListFromJaxbParmList(lis));
    en.setInstantiator(vc);

    en.opaqueModelObject = se;
    nodeCache.put(se,en);   // key = se

    notifyChanged(new ModelEvent(en,ModelEvent.EVENTGRAPHADDED, "Event added"));

    return en;

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

  public String buildJavaSource()
  {
    try {
      SimkitAssemblyXML2Java x2j = new SimkitAssemblyXML2Java(currentFile);
      x2j.unmarshal();
      return x2j.translate();
     }
     catch (Exception e) {
       e.printStackTrace();
     }
     return null;
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
