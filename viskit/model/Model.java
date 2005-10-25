package viskit.model;

import viskit.ModelEvent;
import viskit.VGlobals;
import viskit.ViskitController;
import viskit.mvc.mvcAbstractModel;
import viskit.xsd.bindings.*;
import viskit.xsd.bindings.Event;
import viskit.xsd.translator.SimkitXML2Java;

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
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Mar 2, 2004
 * Time: 1:09:38 PM
 */

/**
 * This is the "master" model of an event graph.  It should hold the node, edge and assembly
 * information.  What hasn't been done is to put in accessor methods for the view to
 * read pieces that it needs, say after it receives a "new model" event.
 */

public class Model extends mvcAbstractModel implements ViskitModel
{
  JAXBContext jc;
  ObjectFactory oFactory;

  SimEntity jaxbRoot;
  File currentFile;

  public static final String schemaLoc = "http://diana.gl.nps.navy.mil/Simkit/simkit.xsd";
  HashMap evNodeCache = new HashMap();
  HashMap edgeCache = new HashMap();
  Vector stateVariables = new Vector();
  Vector simParameters = new Vector();

  private String privateIdxVarPrefix = "_idxvar_";
  private String privateLocVarPrefix = "locvar_";

  private String stateVarPrefix      = "state_";

  private GraphMetaData metaData;

  private ViskitController controller;

  public Model(ViskitController controller)
  {
    this.controller = controller;
  }
  public void init()
  {
    try {
      jc = JAXBContext.newInstance("viskit.xsd.bindings");
      oFactory = new ObjectFactory();
      jaxbRoot = oFactory.createSimEntity(); // to start with empty graph
    }
    catch (JAXBException e) {
      JOptionPane.showMessageDialog(null,"Exception on JAXBContext instantiation" +
                                 "\n"+ e.getMessage(),
                                 "XML Error",JOptionPane.ERROR_MESSAGE);
    }
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
  private boolean modelDirty = false;

  /**
   * This is to allow the controller to stick in a Run event, but treat the graph as fresh.
   * @param dirt
   */
  public void setDirty(boolean dirt)
  {
    modelDirty = dirt;
  }
  
  public GraphMetaData getMetaData()
  {
    return metaData;
  }

  public void changeMetaData(GraphMetaData gmd)
  {
    metaData = gmd;
    setDirty(true);
  }
  /**
   * Replace current model with one contained in the passed file.
   *
   * @param f
   * @return true for good open, else false
   */
   public boolean newModel(File f)
  {
    GraphMetaData mymetaData;
    if (f == null) {
      try {
        jaxbRoot = oFactory.createSimEntity(); // to start with empty graph
      }
      catch (JAXBException e) {
        JOptionPane.showMessageDialog(null,"Exception on JAXBContext instantiation" +
                                   "\n"+ f.getName() +
                                   "\n"+ e.getMessage(),
                                   "XML I/O Error",JOptionPane.ERROR_MESSAGE);
        return false;
      }
      VGlobals.instance().reset();
      stateVariables.removeAllElements();
      simParameters.removeAllElements();
      evNodeCache.clear();
      edgeCache.clear();
      mymetaData = new GraphMetaData();
      this.notifyChanged(new ModelEvent(this, ModelEvent.NEWMODEL, "New empty model"));
    }
    else {
      try {
        Unmarshaller u = jc.createUnmarshaller();
        // u.setValidating(true); can't do this, the unmarshaller needs to have this capability..
        // see u.isValidating()
        // Unmarshaller does NOT validate by default
        jaxbRoot = (SimEntity) u.unmarshal(f);
        mymetaData = new GraphMetaData();
        mymetaData.author = jaxbRoot.getAuthor();
        mymetaData.version = jaxbRoot.getVersion();
        mymetaData.name = jaxbRoot.getName();
        mymetaData.pkg = jaxbRoot.getPackage();
        mymetaData.extend = jaxbRoot.getExtend();
        List lis = jaxbRoot.getComment();
        StringBuffer sb = new StringBuffer("");
        for(Iterator itr = lis.iterator(); itr.hasNext();) {
          sb.append((String)itr.next());
          sb.append(" ");
        }
        mymetaData.comment = sb.toString().trim();

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
      catch (JAXBException ee) {
        // want a clear way to know if they're trying to load an assembly vs. some unspecified XML.
        try {
          JAXBContext assyCtx  = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
          Unmarshaller um = assyCtx.createUnmarshaller();
          um.unmarshal(f);
          // If we get here, they've tried to load an assembly.
          JOptionPane.showMessageDialog(null,"Use the assembly editor to"+
                                        "\n"+"work with this file.",
                                        "Wrong File Format",JOptionPane.ERROR_MESSAGE);
        }
        catch(JAXBException e) {
        JOptionPane.showMessageDialog(null,"Exception on JAXB unmarshalling" +
                                   "\n"+ f.getName() +
                                   "\n"+ e.getMessage(),
                                   "XML I/O Error",JOptionPane.ERROR_MESSAGE);
        }
        return false;    // from either error case
      }
    }
    metaData = mymetaData;
    currentFile = f;
    setDirty(false);
    return true;
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
       jaxbRoot.setAuthor(nIe(metaData.author));
       jaxbRoot.setPackage(nIe(metaData.pkg));
       jaxbRoot.setExtend(nIe(metaData.extend));
       List clis = jaxbRoot.getComment();
       clis.clear();;
       String cmt = nIe(metaData.comment);
       if(cmt != null)
         clis.add(cmt.trim());

       m.marshal(jaxbRoot,fw);
       fw.close();

       setDirty(false);
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
  }

  /**
   * @return A File object representing the last one passed to the two methods above.
   */
  public File getLastFile()
  {
    return currentFile;
  }

  private void buildEventsFromJaxb(List lis)
  //----------------------------------------
  {
    for(Iterator itr = lis.iterator(); itr.hasNext();) {
      Event ev = (Event)itr.next();
      EventNode en = buildNodeFromJaxbEvent(ev);

      buildEdgesFromJaxb(en,ev.getScheduleOrCancel());
    }
  }

  private EventNode buildNodeFromJaxbEvent(Event ev)
  {
    EventNode en = (EventNode)evNodeCache.get(ev);
    if(en != null) {
      return en;
    }
    en = new EventNode(ev.getName());
    jaxbEvToNode(ev,en);
    en.opaqueModelObject = ev;
    evNodeCache.put(ev,en);   // key = ev

    if(!eventNameCheck()) {
      controller.messageUser(JOptionPane.ERROR_MESSAGE,
                      "XML file contains duplicate event name: "+en.getName() +
                      "\nUnique name substituted.");
      mangleNodeName(en);
    }
    notifyChanged(new ModelEvent(en,ModelEvent.EVENTADDED, "Event added"));

    return en;
  }

  private String concatStrings(List lis)
  {
    StringBuffer sb = new StringBuffer();
    for(Iterator itr = lis.iterator(); itr.hasNext();) {
      sb.append(((String) itr.next()));
      if(sb.length() > 0 && sb.charAt(sb.length()-1) != ' ')
        sb.append(' ');
    }
    return sb.toString().trim();
  }

  private char[] hdigits = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

  private String _fourHexDigits(int i)
  {
    char[] ca = new char[4];
    for(int j=3;j>=0;j--) {
      int idx = i & 0xF;
      i >>= 4;
      ca[j] = hdigits[idx];
    }
    return new String(ca);
  }

  Random mangleRandom = new Random();

  private String mangleName(String name)
  {
    int nxt = mangleRandom.nextInt(0x10000); // 4 hex digits
    StringBuffer sb = new StringBuffer(name);
    if (sb.charAt(sb.length() - 1) == '_')
      sb.setLength(sb.length() - 6);
    sb.append('_');
    sb.append(_fourHexDigits(nxt));
    sb.append('_');
    return sb.toString();
  }

  private void mangleNodeName(EventNode node)
  {
    do {
      node.setName(mangleName(node.getName()));
    }
    while (!eventNameCheck());
  }

  private void mangleStateVarName(vStateVariable vsv)
  {
    do {
      vsv.setName(mangleName(vsv.getName()));
    }
    while (!stateVarParamNameCheck());
  }

  private void mangleParamName(vParameter vp)
  {
    do {
      vp.setName(mangleName(vp.getName()));
    }
    while (!stateVarParamNameCheck());
  }

  private boolean eventNameCheck()
  {
    HashSet hs = new HashSet(10);
    for(Iterator itr=evNodeCache.values().iterator();itr.hasNext();) {
      EventNode en = (EventNode)itr.next();
      if(!hs.add(en.getName()))
        return false;
    }
    return true;
  }

  private boolean stateVarParamNameCheck()
  {
    HashSet hs = new HashSet(10);
    for(Iterator itr=stateVariables.iterator();itr.hasNext();) {
      vStateVariable vsv = (vStateVariable)itr.next();
      if(!hs.add(vsv.getName()))
        return false;
    }
    for(Iterator itr=simParameters.iterator();itr.hasNext();) {
      vParameter vp = (vParameter)itr.next();
      if(!hs.add(vp.getName()))
        return false;
    }
    return true;
  }

  private void jaxbEvToNode(Event ev, EventNode node)
  {
    node.setName(ev.getName());

    CoordinateType coor = ev.getCoordinate();
    if(coor != null)        //todo lose this after all xmls updated
      node.setPosition(new Point(Integer.parseInt(coor.getX()),
                               Integer.parseInt(coor.getY())));

    node.getComments().clear();
    node.getComments().addAll(ev.getComment());

    node.getLocalVariables().clear();
    for(Iterator itr = ev.getLocalVariable().iterator(); itr.hasNext();) {
      LocalVariable lv = (LocalVariable)itr.next();
      if(!lv.getName().startsWith(privateIdxVarPrefix)) {    // only if it's a "public" one
        EventLocalVariable elv = new EventLocalVariable(
                                 lv.getName(),lv.getType(),lv.getValue());
        elv.setComment(concatStrings(lv.getComment()));
        elv.opaqueModelObject = lv;
        node.getLocalVariables().add(elv);
      }
    }

    node.getArguments().clear();
    for(Iterator itr = ev.getArgument().iterator(); itr.hasNext();) {
      Argument arg = (Argument)itr.next();
      EventArgument ea = new EventArgument();
      ea.setName(arg.getName());
      ea.setType(arg.getType());
      ArrayList com = new ArrayList();
      com.addAll(arg.getComment());
      ea.setComments(com);
      ea.opaqueModelObject = arg;
      node.getArguments().add(ea);
    }

    node.getTransitions().clear();
    for(Iterator itr = ev.getStateTransition().iterator(); itr.hasNext();) {
      EventStateTransition est = new EventStateTransition();
      StateTransition st = (StateTransition)itr.next();
      StateVariable   sv = (StateVariable)st.getState();
      est.setStateVarName(sv.getName());
      est.setStateVarType(sv.getType());

      if(sv.getType().indexOf('[') != -1) {
        Object o = st.getIndex();
        if(o instanceof LocalVariable)
          est.setIndexingExpression(((LocalVariable)o).getName());
        // todo confirm the following
        else if(o instanceof Parameter)
          est.setIndexingExpression(((Parameter)o).getName());
        else if(o instanceof Argument)
          est.setIndexingExpression(((Argument)o).getName());
        else if(o instanceof StateVariable)
          est.setIndexingExpression(((StateVariable)o).getName());
      }

      est.setOperation(st.getOperation() != null);
      if(est.isOperation())
        est.setOperationOrAssignment(st.getOperation().getMethod());
      else
        est.setOperationOrAssignment(st.getAssignment().getValue());
      ArrayList cmt = new ArrayList();
      cmt.addAll(sv.getComment());
      est.setComments(cmt);
      est.opaqueModelObject = st;
      node.getTransitions().add(est);
    }
  }

  private void buildEdgesFromJaxb(EventNode src,List lis)
  {
    for(Iterator itr = lis.iterator(); itr.hasNext();) {
      Object o = itr.next();
      if( o instanceof Schedule) {
        buildScheduleEdgeFromJaxb(src,(Schedule)o);
      }
      else {
        buildCancelEdgeFromJaxb(src,(Cancel)o);
      }
    }
  }

  private SchedulingEdge buildScheduleEdgeFromJaxb(EventNode src, Schedule ed)
  {
    SchedulingEdge se = new SchedulingEdge();
    se.opaqueModelObject = ed;

    se.from = src;
    EventNode target = buildNodeFromJaxbEvent((Event)ed.getEvent());
    se.to = target;
    src.getConnections().add(se);
    target.getConnections().add(se);
    se.conditional = ed.getCondition();

    List cmt = ed.getComment();
    if(!cmt.isEmpty()) {
      StringBuffer sb = new StringBuffer();
      for(Iterator itr = cmt.iterator(); itr.hasNext();) {
        sb.append((String)itr.next());
        sb.append("  ");
      }
      se.conditionalsComment = sb.toString().trim();
    }
    se.delay = ed.getDelay();
    se.parameters = buildEdgeParmsFromJaxb(ed.getEdgeParameter());
    edgeCache.put(ed,se);

    setDirty(true);

    this.notifyChanged(new ModelEvent(se, ModelEvent.EDGEADDED, "Edge added"));

    return se;
  }

  private CancellingEdge buildCancelEdgeFromJaxb(EventNode src, Cancel ed)
  {
    CancellingEdge ce = new CancellingEdge();
    ce.opaqueModelObject = ed;
    ce.conditional = ed.getCondition();

    List cmt = ed.getComment();
    if(!cmt.isEmpty()) {
      StringBuffer sb = new StringBuffer();
      for(Iterator itr = cmt.iterator(); itr.hasNext();) {
        sb.append((String)itr.next());
        sb.append("  ");
      }
      ce.conditionalsComment = sb.toString().trim();
    }

    ce.parameters = buildEdgeParmsFromJaxb(ed.getEdgeParameter());

    ce.from = src;
    EventNode target = buildNodeFromJaxbEvent((Event)ed.getEvent());
    ce.to = target;
    src.getConnections().add(ce);
    target.getConnections().add(ce);

    edgeCache.put(ed,ce);
    setDirty(true);

    notifyChanged(new ModelEvent(ce, ModelEvent.CANCELLINGEDGEADDED, "Cancelling edge added"));
    return ce;
  }
  private ArrayList buildEdgeParmsFromJaxb(List lis)
  {
    ArrayList alis = new ArrayList(3);
    for(Iterator itr = lis.iterator();itr.hasNext();) {
      EdgeParameter ep = (EdgeParameter)itr.next();

      vEdgeParameter vep = new vEdgeParameter((String)ep.getValue()); //,ep.getType());
      alis.add(vep);
    }
    return alis;
  }
  private void buildStateVariablesFromJaxb(List lis)
  {
    for(Iterator itr = lis.iterator(); itr.hasNext();) {
      StateVariable var = (StateVariable)itr.next();
      List varCom = var.getComment();
      String c = " ";
      for(Iterator ii=varCom.iterator();ii.hasNext();) {
        c += (String)ii.next();
        c += " ";
      }
      vStateVariable v = new vStateVariable(var.getName(),var.getType(),c.trim());
      v.opaqueModelObject = var;
      stateVariables.add(v);

      if(!stateVarParamNameCheck()) {
        controller.messageUser(JOptionPane.ERROR_MESSAGE,"XML file contains duplicate state variable name."+
                              "\nUnique name substituted.");
        mangleStateVarName(v);
      }

      notifyChanged(new ModelEvent(v,ModelEvent.STATEVARIABLEADDED,"New state variable"));
    }
  }

  private void buildParametersFromJaxb(List lis)
  {
    for(Iterator itr = lis.iterator(); itr.hasNext();) {
      Parameter p = (Parameter)itr.next();
      List pCom = p.getComment();
      String c = " ";
      for(Iterator ii=pCom.iterator();ii.hasNext();) {
        c += (String)ii.next();
        c += " ";
      }
      vParameter vp = new vParameter(p.getName(), p.getType(),c.trim());
      vp.opaqueModelObject = p;
      simParameters.add(vp);

      if(!stateVarParamNameCheck()) {
        controller.messageUser(JOptionPane.ERROR_MESSAGE,"XML file contains duplicate parameter name."+
                               "\nUnique name substituted.");
        mangleParamName(vp);
      }
      notifyChanged(new ModelEvent(vp,ModelEvent.SIMPARAMETERADDED,"New sim parameter"));
    }
  }

  public Vector getAllNodes()
  {
    return new Vector(evNodeCache.values());
  }

  public Vector getStateVariables()
  {
    return (Vector)stateVariables.clone();
  }

  public Vector getSimParameters()
  {
    return (Vector)simParameters.clone();
  }

  // Source building
  // ---------------
  public String buildJavaSource()
  {
   try {
     SimkitXML2Java x2j = new SimkitXML2Java(currentFile);
     x2j.unmarshal();
     return x2j.translate();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }


  // parameter mods
  // --------------
  public void newSimParameter(String nm, String typ, String xinitVal, String comment)
  {
    setDirty(true);

    vParameter vp = new vParameter(nm,typ,comment);
    simParameters.add(vp);

    if(!stateVarParamNameCheck()) {
      controller.messageUser(JOptionPane.ERROR_MESSAGE,
                      "Duplicate parameter name detected: "+nm +
                      "\nUnique name substituted.");
      mangleParamName(vp);
    }

    //p.setValue(initVal);
    Parameter p = null;
    try {p = this.oFactory.createParameter(); } catch(JAXBException e){ System.out.println("newParmJAXBEX"); }
    p.setName(nIe(nm));
    //p.setShortName(nm);
    p.setType(nIe(typ));
    p.getComment().add(comment);

    vp.opaqueModelObject = p;

    jaxbRoot.getParameter().add(p);

    notifyChanged(new ModelEvent(vp, ModelEvent.SIMPARAMETERADDED, "vParameter added"));
  }

  public void deleteSimParameter(vParameter vp)
  {
    // remove jaxb variable
    jaxbRoot.getParameter().remove(vp.opaqueModelObject);
    setDirty(true);
    simParameters.remove(vp);
    notifyChanged(new ModelEvent(vp, ModelEvent.SIMPARAMETERDELETED, "vParameter deleted"));
  }

  public boolean changeSimParameter(vParameter vp)
  {
    boolean retcode = true;
    if(!stateVarParamNameCheck()) {
      controller.messageUser(JOptionPane.ERROR_MESSAGE,"Duplicate parameter name detected: "+vp.getName()+
                                         "\nUnique name substituted.");
      mangleParamName(vp);
      retcode = false;
    }
    // fill out jaxb variable
    Parameter p = (Parameter)vp.opaqueModelObject;
    p.setName(nIe(vp.getName()));
    //p.setShortName(vp.getName());
    p.setType(nIe(vp.getType()));
    p.getComment().clear();
    p.getComment().add(vp.getComment());

    setDirty(true);
    notifyChanged(new ModelEvent(vp, ModelEvent.SIMPARAMETERCHANGED, "vParameter changed"));
    return retcode;
  }

  // State variable mods
  // -------------------

  public void newStateVariable(String name, String type, String xinitVal, String comment)
  {
    setDirty(true);

    // get the new one here and show it around
    vStateVariable vsv = new vStateVariable(name,type,comment);
    stateVariables.add(vsv);
    if(!stateVarParamNameCheck()) {
      controller.messageUser(JOptionPane.ERROR_MESSAGE,
                      "Duplicate state variable name detected: "+name +
                      "\nUnique name substituted.");
      mangleStateVarName(vsv);
    }
    StateVariable s = null;
    try {s = this.oFactory.createStateVariable(); } catch(JAXBException e){ System.out.println("newStVarJAXBEX"); }
    s.setName(nIe(name));
    //s.setShortName(nIe(name));
    s.setType(nIe(type));
    s.getComment().add(comment);

    vsv.opaqueModelObject = s;
    jaxbRoot.getStateVariable().add(s);
    notifyChanged(new ModelEvent(vsv, ModelEvent.STATEVARIABLEADDED, "State variable added"));
  }

  public void deleteStateVariable(vStateVariable vsv)
  {
    // remove jaxb variable
    jaxbRoot.getStateVariable().remove(vsv.opaqueModelObject);
    stateVariables.remove(vsv);
    setDirty(true);
    notifyChanged(new ModelEvent(vsv, ModelEvent.STATEVARIABLEDELETED, "State variable deleted"));
  }

  public boolean changeStateVariable(vStateVariable vsv)
  {
    boolean retcode = true;
    if(!stateVarParamNameCheck()) {
      controller.messageUser(JOptionPane.ERROR_MESSAGE,"Duplicate state variable name detected: "+vsv.getName()+
                                         "\nUnique name substituted.");
      mangleStateVarName(vsv);
      retcode = false;
    }
    // fill out jaxb variable
    StateVariable sv = (StateVariable)vsv.opaqueModelObject;
    sv.setName(nIe(vsv.getName()));
    //sv.setShortName(nIe(vsv.getName()));
    sv.setType(nIe(vsv.getType()));
    sv.getComment().clear();
    sv.getComment().add(vsv.getComment());

    setDirty(true);
    notifyChanged(new ModelEvent(vsv, ModelEvent.STATEVARIABLECHANGED, "State variable changed"));
    return retcode;
  }

  // Event (node) mods
  // -----------------
  /**
   * Add a new event to the graph with the given label, at the given point
   */
  public void newEvent(String nodeName, Point p)
  {
    EventNode node = new EventNode(nodeName);
    if (p == null)
      node.setPosition(new Point(100,100));
    else {
      p.x = ((p.x+5)/10)*10;    //round
      p.y = ((p.y+5)/10)*10;
      node.setPosition(p);
    }
    Event jaxbEv = null;
    try {
      jaxbEv = oFactory.createEvent();
    }
    catch (JAXBException e) {
      //assert false : "Model.newEvent, error creating viskit.xsd.bindings.Event.";
      System.err.println("Model.newEvent, error creating viskit.xsd.bindings.Event.");
      return;
    }
    evNodeCache.put(jaxbEv,node);   // key = ev

    if(!eventNameCheck()) {
      mangleNodeName(node);
    }

    jaxbEv.setName(nIe(nodeName));
    node.opaqueModelObject = jaxbEv;
    jaxbRoot.getEvent().add(jaxbEv);

    setDirty(true);
    notifyChanged(new ModelEvent(node,ModelEvent.EVENTADDED, "Event added"));
  }

  /**
   * Delete the referenced event, also deleting attached edges.
   *
   * @param node
   */
  public void deleteEvent(EventNode node)
  {
    Event jaxbEv = (Event)node.opaqueModelObject;
    evNodeCache.remove(jaxbEv);
    jaxbRoot.getEvent().remove(jaxbEv);

    setDirty(true);
    this.notifyChanged(new ModelEvent(node, ModelEvent.EVENTDELETED, "Event deleted"));
  }

  private StateVariable findStateVariable(String nm)
  {
    List lis = jaxbRoot.getStateVariable();
    for(Iterator itr = lis.iterator(); itr.hasNext(); ) {
      StateVariable st = (StateVariable)itr.next();
      if(st.getName().equals(nm))
        return st;
    }
    return null;
  }

  private int locVarNameSequence = 0;
  public String generateLocalVariableName()
  {
    String nm = null;
    do {
      nm = privateLocVarPrefix + locVarNameSequence++;
    }while(!isUniqueLVorIdxVname(nm));
    return nm;

  }
  public void resetLVNameGenerator()
  {
    locVarNameSequence = 0;
  }

  private int idxVarNameSequence = 0;

  public String generateIndexVariableName()
  {
    String nm = null;
    do {
      nm = privateIdxVarPrefix + idxVarNameSequence++;
    }while(!isUniqueLVorIdxVname(nm));
    return nm;
  }
  public void resetIdxNameGenerator()
  {
    idxVarNameSequence = 0;
  }

  private boolean isUniqueLVorIdxVname(String nm)
  {
    for (Iterator itr = evNodeCache.values().iterator(); itr.hasNext();) {
      EventNode event = (EventNode) itr.next();
      for (Iterator itt = event.getLocalVariables().iterator(); itt.hasNext();) {
        EventLocalVariable lv = (EventLocalVariable)itt.next();
        if(lv.getName().equals(nm))
          return false;
      }
      for (Iterator it2 = event.getTransitions().iterator();it2.hasNext();) {
        EventStateTransition est = (EventStateTransition)it2.next();
        String ie = est.getIndexingExpression();
        if(ie != null && ie.equals(nm))
          return false;
      }
    }
    return true;
  }
  private boolean isUniqueSVname(String nm)
  {
    for (Iterator itr = stateVariables.iterator(); itr.hasNext();) {
      vStateVariable sv = (vStateVariable)itr.next();
      if(sv.getName().equals(nm))
        return false;
    }
    return true;
  }
  
  public String generateStateVariableName()
  {
    String nm = null;
    int startnum = 0;
    do {
      nm = stateVarPrefix + startnum++;
    }while(!isUniqueSVname(nm));
    return nm;
  }

  /**
   * Here we convert indexing expressions into local variable references
   * @param targ
   * @param local
   */
  private void cloneTransitions(List targ, ArrayList local, List locVarList)
  {
    try {
      targ.clear();
      for(Iterator itr = local.iterator(); itr.hasNext();) {
        EventStateTransition est = (EventStateTransition)itr.next();
        StateTransition st =  oFactory.createStateTransition();
        StateVariable sv = findStateVariable(est.getStateVarName());
        st.setState(sv);
        if(sv.getType() != null && sv.getType().indexOf('[') != -1) {
          // build a local variable

          LocalVariable lvar = oFactory.createLocalVariable();

          lvar.setName(est.getIndexingExpression());
          lvar.setType("int");
          lvar.setValue("0");
          lvar.getComment().clear();
          lvar.getComment().add("used internally");
          locVarList.add(lvar);

          st.setIndex(lvar);
        }
        if(est.isOperation()) {
          Operation o = oFactory.createOperation();
          o.setMethod(est.getOperationOrAssignment());
          st.setOperation(o);
        }
        else {
          Assignment a = oFactory.createAssignment();
          a.setValue(est.getOperationOrAssignment());
          st.setAssignment(a);
        }

        est.opaqueModelObject = st; //replace
        targ.add(st);
      }
    }
    catch (JAXBException e) {
      e.printStackTrace();
    }
  }

  private void cloneComments(List targ, ArrayList local)
  {
    targ.clear();
    targ.addAll(local);
  }

  private void cloneArguments(List targ, ArrayList local)
  {
    try {
      targ.clear();
      for(Iterator itr = local.iterator(); itr.hasNext();) {
        EventArgument ea = (EventArgument)itr.next();
        Argument arg = oFactory.createArgument();
        arg.setName(nIe(ea.getName()));
        arg.setType(nIe(ea.getType()));
        arg.getComment().clear();
        arg.getComment().addAll(ea.getComments());
        ea.opaqueModelObject = arg; // replace
        targ.add(arg);
      }
    }
    catch(JAXBException e) {
      System.err.println("Exc Model.cloneArguments() "+e);
    }
  }

  private void cloneLocalVariables(List targ, Vector local)
  {
    try {
      targ.clear();
      for(Iterator itr = local.iterator(); itr.hasNext();) {
        EventLocalVariable elv = (EventLocalVariable)itr.next();
        LocalVariable lvar = oFactory.createLocalVariable();
        lvar.setName(nIe(elv.getName()));
        lvar.setType(nIe(elv.getType()));
        lvar.setValue(nIe(elv.getValue()));
        lvar.getComment().clear();
        lvar.getComment().add(elv.getComment());
        elv.opaqueModelObject = lvar; //replace
        targ.add(lvar);
      }
    }
    catch(JAXBException e) {
      System.err.println("Exc Model.cloneLocalVariables() "+e);
    }

  }

  public boolean changeEvent(EventNode node)
  {
    boolean retcode = true;
    if(!eventNameCheck()) {
      controller.messageUser(JOptionPane.ERROR_MESSAGE,"Duplicate event name detected: "+node.getName()+
                                   "\nUnique name substituted.");
      mangleNodeName(node);
      retcode = false;
    }

    Event jaxbEv = (Event)node.opaqueModelObject;

    jaxbEv.setName(node.getName());

    Coordinate coor = null;
    try {
      coor = oFactory.createCoordinate();
    } catch(JAXBException e) {
      System.err.println("Exc Model.changeEvent()");
    }
    // rudimentary snap to grid - this works on saved file only, not the live position in the node.  reload to enjoy.
    int GridScale = 10;
    coor.setX(""+(((node.getPosition().x+GridScale/2)/GridScale)*GridScale));
    coor.setY(""+(((node.getPosition().y+GridScale/2)/GridScale)*GridScale));
    jaxbEv.setCoordinate(coor);

    cloneComments(jaxbEv.getComment(),node.getComments());
    cloneArguments(jaxbEv.getArgument(),node.getArguments());
    cloneLocalVariables(jaxbEv.getLocalVariable(),node.getLocalVariables());
    // following must follow above
    cloneTransitions(jaxbEv.getStateTransition(),node.getTransitions(),jaxbEv.getLocalVariable());

    setDirty(true);
    notifyChanged(new ModelEvent(node, ModelEvent.EVENTCHANGED, "Event changed"));
    return retcode;
  }

  // Edge mods
  // ---------
  public void newEdge(EventNode src, EventNode target)
  {
    SchedulingEdge se = new SchedulingEdge();
    se.from = src;
    se.to = target;
    src.getConnections().add(se);
    target.getConnections().add(se);

    Schedule sch;
    try {
      sch = oFactory.createSchedule();
    }
    catch (JAXBException e) {
      //assert false : "Model.newEdge, error creating viskit.xsd.bindings.Schedule.";
      System.err.println("Model.newEdge, error creating viskit.xsd.bindings.Schedule.");
      return;
    }
    se.opaqueModelObject = sch;
    Event targEv = (Event)target.opaqueModelObject;
    sch.setEvent(targEv);
    Event srcEv = (Event)src.opaqueModelObject;
    srcEv.getScheduleOrCancel().add(sch);

    // Put in dummy edge parameters to match the target arguments
    ArrayList args = target.getArguments();
    if(args.size() > 0) {
      ArrayList eps = new ArrayList(args.size());
      for(int i = 0;i<args.size();i++)
        eps.add(new vEdgeParameter(""));
      se.parameters = eps;
    }

    this.edgeCache.put(sch,se);
    setDirty(true);

    this.notifyChanged(new ModelEvent(se, ModelEvent.EDGEADDED, "Edge added"));
  }

  public void newCancelEdge(EventNode src, EventNode target)
  {
    CancellingEdge ce = new CancellingEdge();
    ce.from = src;
    ce.to = target;
    src.getConnections().add(ce);
    target.getConnections().add(ce);

    Cancel can;
    try {
      can = oFactory.createCancel();
    }
    catch (JAXBException e) {
      //assert false : "Model.newEdge, error creating viskit.xsd.bindings.Cancel.";
      System.err.println("Model.newEdge, error creating viskit.xsd.bindings.Cancel.");
      return;
    }
    ce.opaqueModelObject = can;
    Event targEv = (Event)target.opaqueModelObject;
    can.setEvent(targEv);
    Event srcEv = (Event)src.opaqueModelObject;
    srcEv.getScheduleOrCancel().add(can);

    // Put in dummy edge parameters to match the target arguments
    ArrayList args = target.getArguments();
    if(args.size() > 0) {
      ArrayList eps = new ArrayList(args.size());
      for(int i = 0;i<args.size();i++)
        eps.add(new vEdgeParameter(""));
      ce.parameters = eps;
    }


    this.edgeCache.put(can,ce);
    setDirty(true);

    this.notifyChanged(new ModelEvent(ce, ModelEvent.CANCELLINGEDGEADDED, "Edge added"));
  }

  public void deleteEdge(SchedulingEdge edge)
  {
    _commonEdgeDelete(edge);

    setDirty(true);
    this.notifyChanged(new ModelEvent(edge, ModelEvent.EDGEDELETED, "Edge deleted"));
  }

  private void _commonEdgeDelete(Edge edg)
  {
    Object jaxbEdge = edg.opaqueModelObject;

    List nodes = jaxbRoot.getEvent();
    for(Iterator itr = nodes.iterator(); itr.hasNext();) {
      Event ev = (Event)itr.next();
      List edges = ev.getScheduleOrCancel();
      edges.remove(jaxbEdge);
    }

    edgeCache.remove(edg);
  }

  public void deleteCancelEdge(CancellingEdge edge)
  {
    _commonEdgeDelete(edge);

    setDirty(true);
    this.notifyChanged(new ModelEvent(edge, ModelEvent.CANCELLINGEDGEDELETED, "Cancelling edge deleted"));
  }

  public void changeEdge(Edge e)
  {
    Schedule sch = (Schedule)e.opaqueModelObject;
    sch.setCondition(e.conditional);
    sch.getComment().clear();
    sch.getComment().add(e.conditionalsComment);
    sch.setDelay(""+e.delay);

    sch.setEvent((Event)e.to.opaqueModelObject);
    sch.setPriority("0");  // todo implement priority

    sch.getEdgeParameter().clear();
    for(Iterator itr = e.parameters.iterator(); itr.hasNext();) {
      vEdgeParameter vp = (vEdgeParameter)itr.next();
      EdgeParameter p = null;
      try {
        p = oFactory.createEdgeParameter();
      }
      catch (JAXBException e1) {
        //assert false : "Model.changeEdge, jaxb error createing EdgeParameter";
        System.err.println("Model.changeEdge, jaxb error createing EdgeParameter");
        return;
      }
      //p.setType(vp.getType());
      p.setValue(nIe(vp.getValue()));
      sch.getEdgeParameter().add(p);
    }

    setDirty(true);
    this.notifyChanged(new ModelEvent(e, ModelEvent.EDGECHANGED, "Edge changed"));
  }

  public void changeCancelEdge(Edge e)
  {
    Cancel can = (Cancel)e.opaqueModelObject;
    can.setCondition(e.conditional);
    can.setEvent((Event)e.to.opaqueModelObject);
    can.getComment().clear();
    can.getComment().add(e.conditionalsComment);

    can.getEdgeParameter().clear();
     for(Iterator itr = e.parameters.iterator(); itr.hasNext();) {
      vEdgeParameter vp = (vEdgeParameter)itr.next();
      EdgeParameter p = null;
      try {
        p = oFactory.createEdgeParameter();
      }
      catch (JAXBException e1) {
        //assert false : "Model.changeEdge, jaxb error createing EdgeParameter";
        System.err.println("Model.changeEdge, jaxb error createing EdgeParameter");
        return;
      }
      //p.setType(vp.getType());
      p.setValue(nIe(vp.getValue()));
      can.getEdgeParameter().add(p);
    }


    setDirty(true);
    this.notifyChanged(new ModelEvent(e, ModelEvent.CANCELLINGEDGECHANGED, "Cancelling edge changed"));
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
