package viskit.model;

import viskit.mvc.mvcAbstractModel;
import viskit.xsd.bindings.assembly.*;
import viskit.xsd.assembly.SimkitAssemblyXML2Java;
import viskit.ModelEvent;
import viskit.VGlobals;

import javax.swing.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

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

  /* from other model...*/
  public void saveModel(File f)
  {
/*
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
       jaxbRoot.setAuthor(nIe(metaData.author));
       jaxbRoot.setPackage(nIe(metaData.pkg));

       java.util.List clis = jaxbRoot.getComment();
       clis.clear();;
       String cmt = nIe(metaData.comment);
       if(cmt != null)
         clis.add(cmt.trim());

       m.marshal(jaxbRoot,fw);
       fw.close();

       modelDirty = false;
       currentFile = f;
     }
     catch (JAXBException e) {
       JOptionPane.showMessageDialog(null,"Exception on JAXB marshalling" +
                                  "\n"+ f.getName() +
                                  "\n"+ e.getMessage(),
                                  "XML I/O Error",JOptionPane.ERROR_MESSAGE);
       return;
     }
     catch (IOException ex) {
       JOptionPane.showMessageDialog(null,"Exception on writing "+ f.getName() +
                                  "\n"+ ex.getMessage(),
                                  "File I/O Error",JOptionPane.ERROR_MESSAGE);
       return;
     }
*/
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
    ae.setFrom(src);
    ae.setTo(target);
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
    sele.setFrom(src);
    sele.setTo(target);
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
  public void changePclEdge(PropChangeEdge pclEdge)
  {
    //todo implement
    System.out.println("changePclEdge...todo");
  }

  public void changeAdapterEdge(AdapterEdge aEdge)
  {
    //todo implement
    System.out.println("changeAdapterEdge...todo");
  }

  public void changeSimEvEdge(SimEvListenerEdge seEdge)
  {
    //todo implement
    System.out.println("changeSimEvEdge...todo");
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

    List lis = jaxBPcl.getTerminalParameter();
    lis.clear();;
    for (Iterator itr = pclNode.getConstructorArguments().iterator(); itr.hasNext();) {
      ConstructorArgument ca = (ConstructorArgument) itr.next();
      TerminalParameter tp = null;
      try {
        tp = oFactory.createTerminalParameter();
      }
      catch (JAXBException e) {
        e.printStackTrace();
      }
      tp.setType(ca.getType());
      tp.setValue(ca.getValue());
      lis.add(tp);
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
    lis.clear();;
    for (Iterator itr = evNode.getConstructorArguments().iterator(); itr.hasNext();) {
      ConstructorArgument ca = (ConstructorArgument) itr.next();
      TerminalParameter tp = null;
      try {
        tp = oFactory.createTerminalParameter();
      }
      catch (JAXBException e) {
        e.printStackTrace();
      }
      tp.setType(ca.getType());
      tp.setValue(ca.getValue());
      lis.add(tp);
    }

    modelDirty = true;
    this.notifyChanged(new ModelEvent(evNode, ModelEvent.EVENTGRAPHCHANGED, "Event changed"));
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
/*
        List lis = jaxbRoot.getComment();
        StringBuffer sb = new StringBuffer("");
        for(Iterator itr = lis.iterator(); itr.hasNext();) {
          sb.append((String)itr.next());
          sb.append(" ");
        }

        metaData.comment = sb.toString().trim();
*/
        VGlobals.instance().reset();
        nodeCache.clear();
        assEdgeCache.clear();
        this.notifyChanged(new ModelEvent(this, ModelEvent.NEWMODEL, "New model loaded from file"));
        buildEGsFromJaxb(jaxbRoot.getSimEntity());
        buildPCLsFromJaxb(jaxbRoot.getPropertyChangeListener());
        // may not need these:
        //buildPCConnectionsFromJaxb(jaxbRoot.getPropertyChangeListenerConnection());
        //buildSimEvConnectionsFromJaxb(jaxbRoot.getSimEventListenerConnection());
        //buildAdapterConnectionsFromJaxb(jaxbRoot.getAdapter());
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

  private void buildEGsFromJaxb(List simEntities)
  {
    for (Iterator itr = simEntities.iterator(); itr.hasNext();) {
      SimEntity se = (SimEntity) itr.next();
      EvGraphNode egn = buildEvgNodeFromJaxbSimEntity(se);

      buildEdgesFromEvGraphNode(egn, egn.getConnections());
    }
  }

  private EvGraphNode buildEvgNodeFromJaxbSimEntity(SimEntity se)
  {
    EvGraphNode en = (EvGraphNode)nodeCache.get(se);
    if(en != null) {
      return en;
    }
    en = new EvGraphNode(se.getName(),se.getType());
    en.setName(se.getName());
    CoordinateType coor = se.getCoordinate();
    en.setPosition(new Point(Integer.parseInt(coor.getX()),
                             Integer.parseInt(coor.getY())));

    en.opaqueModelObject = se;
    nodeCache.put(se,en);   // key = se

    notifyChanged(new ModelEvent(en,ModelEvent.EVENTADDED, "Event added"));

    return en;

  }

  private void buildEdgesFromEvGraphNode(EvGraphNode egn, Vector connections)
  {
    for (Iterator itr = connections.iterator(); itr.hasNext();) {
      Object o = itr.next();
      if(o instanceof Adapter) {
        buildAdapterEdgeFromJaxb(egn,(Adapter)o);
      }
      else if (o instanceof PropertyChangeListenerConnection) {
        buildPCEdgeFromJaxb(egn,(PropertyChangeListenerConnection)o);
      }
      else if (o instanceof SimEventListenerConnection) {
        buildSEvLisEdgeFromJaxb(egn,(SimEventListenerConnection)o);
      }
    }
  }

  private AdapterEdge buildAdapterEdgeFromJaxb(EvGraphNode egn, Adapter a)
  {
    AdapterEdge ae = new AdapterEdge();
    ae.opaqueModelObject = a;
    ae.setFrom(egn);
    EvGraphNode targ = buildEvgNodeFromJaxbSimEntity((SimEntity)a.getTo());
    ae.setTo(egn);
    egn.getConnections().add(ae);
    targ.getConnections().add(ae);

    assEdgeCache.put(a,ae);
    modelDirty = true;
    this.notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGEADDED, "Adapter Edge added"));
    return ae;

  }
  private PropChangeEdge buildPCEdgeFromJaxb(EvGraphNode egn, PropertyChangeListenerConnection pclc)
  {
    PropChangeEdge pce = new PropChangeEdge();
    pce.opaqueModelObject = pclc;
    pce.setFrom(egn);
    EvGraphNode targ = buildEvgNodeFromJaxbSimEntity((SimEntity)pclc.getListener());     //todo wrong
    pce.setTo(targ);
    egn.getConnections().add(pce);
    targ.getConnections().add(pce);

    assEdgeCache.put(pclc,pce);
    modelDirty = true;
    this.notifyChanged(new ModelEvent(pce,ModelEvent.PCLEDGEADDED,"PCL Edge added"));
    return pce;
  }
  private SimEvListenerEdge buildSEvLisEdgeFromJaxb(EvGraphNode egn, SimEventListenerConnection selcon)
  {
    SimEvListenerEdge sele = new SimEvListenerEdge();
    sele.opaqueModelObject = selcon;
    sele.setFrom(egn);
    EvGraphNode targ = buildEvgNodeFromJaxbSimEntity((SimEntity)selcon.getListener());
    sele.setTo(targ);
    egn.getConnections().add(sele);
    targ.getConnections().add(sele);

    assEdgeCache.put(selcon,sele);
    modelDirty = true;
    this.notifyChanged(new ModelEvent(sele,ModelEvent.SIMEVLISTEDGEADDED,"SimEvList Edge added"));
    return sele;
  }
  private void buildPCLsFromJaxb(List pcLs)
  {
    //todo implement

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
