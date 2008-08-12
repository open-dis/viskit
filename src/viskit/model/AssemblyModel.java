package viskit.model;

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

import edu.nps.util.FileIO;
import edu.nps.util.TempFileManager;
import org.apache.log4j.Logger;
import viskit.FileBasedAssyNode;
import viskit.ModelEvent;
import viskit.ViskitAssemblyController;
import viskit.mvc.mvcAbstractModel;
import viskit.xsd.bindings.assembly.*;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 17, 2004
 * @since 9:16:44 AM
 * @version $Id$
 */
public class AssemblyModel extends mvcAbstractModel implements ViskitAssemblyModel {
    
    static Logger log = Logger.getLogger(AssemblyModel.class);

    private JAXBContext jc;
    private ObjectFactory oFactory;
    private SimkitAssembly jaxbRoot;
    private File currentFile;
    private boolean modelDirty = false;
    private GraphMetaData metaData;
    
    /** We require specific order on this Map's contents */
    private Map<Object, AssemblyNode> nodeCache;
    private HashMap<Object, Object> assEdgeCache;
    public static final String schemaLoc = "http://diana.nps.edu/Simkit/assembly.xsd";
    private Point pointLess = new Point(100, 100);
    private ViskitAssemblyController controller;

    public AssemblyModel(ViskitAssemblyController cont) {
        controller = cont;
        metaData = new GraphMetaData(this);
        setNodeCache(new LinkedHashMap<Object, AssemblyNode>());
        assEdgeCache = new HashMap<Object, Object>();
    }

    public void init() {
        try {
            jc = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
            oFactory = new ObjectFactory();
            jaxbRoot = oFactory.createSimkitAssembly(); // to start with empty graph
        } catch (JAXBException e) {
            JOptionPane.showMessageDialog(null, "Exception on JAXBContext instantiation" +
                    "\n" + e.getMessage(),
                    "XML Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public File getFile() {
        return currentFile;
    }

    public void saveModel(File f) {
        if (f == null) {
            f = currentFile;
        }
        // Do the marshalling into a temporary file, so as to avoid possible deletion of existing
        // file on a marshal error.

        File tmpF = null;
        try {
            tmpF = TempFileManager.createTempFile("tmpAsymarshal", ".xml");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Exception creating temporary file, AssemblyModel.saveModel():" +
                    "\n" + e.getMessage(),
                    "I/O Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            FileWriter fw = new FileWriter(tmpF);
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            m.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, schemaLoc);

            String nm = f.getName();
            int dot = -1;
            if ((dot = nm.indexOf('.')) != -1) {
                nm = nm.substring(0, dot);
            }

            jaxbRoot.setName(nIe(metaData.name));
            jaxbRoot.setVersion(nIe(metaData.version));
            jaxbRoot.setPackage(nIe(metaData.packageName));
            if (jaxbRoot.getSchedule() == null) {
                jaxbRoot.setSchedule(oFactory.createSchedule());
            }
            if (!metaData.stopTime.equals("")) {
                jaxbRoot.getSchedule().setStopTime(metaData.stopTime);
            } else {
                jaxbRoot.getSchedule().setStopTime("100.0");
            }

            jaxbRoot.getSchedule().setVerbose("" + metaData.verbose);

            m.marshal(jaxbRoot, fw);
            fw.close();

            // OK, made it through the marshal, overwrite the "real" file
            FileIO.copyFile(tmpF, f, true);

            modelDirty = false;
            currentFile = f;
        } catch (JAXBException e) {
            JOptionPane.showMessageDialog(null, "Exception on JAXB marshalling" +
                    "\n" + f +
                    "\n" + e.getMessage() +
                    "\n(check for blank data fields)",
                    "XML I/O Error", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Exception on writing " + f.getName() +
                    "\n" + ex.getMessage(),
                    "File I/O Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    /**
     * 
     * @param v
     */
    public void externalClassesChanged(Vector<String> v) {

    }
    private char[] hdigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private String _fourHexDigits(int i) {
        char[] ca = new char[4];
        for (int j = 3; j >= 0; j--) {
            int idx = i & 0xF;
            i >>= 4;
            ca[j] = hdigits[idx];
        }
        return new String(ca);
    }
    Random mangleRandom = new Random();

    private String mangleName(String name) {
        int nxt = mangleRandom.nextInt(0x10000); // 4 hex digits
        StringBuffer sb = new StringBuffer(name);
        if (sb.charAt(sb.length() - 1) == '_') {
            sb.setLength(sb.length() - 6);
        }
        sb.append('_');
        sb.append(_fourHexDigits(nxt));
        sb.append('_');
        return sb.toString();
    }

    private void manglePCLName(PropChangeListenerNode node) {
        do {
            node.setName(mangleName(node.getName()));
        } while (!nameCheck());
    }

    private void mangleEGName(EvGraphNode node) {
        do {
            node.setName(mangleName(node.getName()));
        } while (!nameCheck());
    }

    private boolean nameCheck() {
        HashSet<String> hs = new HashSet<String>(10);
        for (Object o : getNodeCache().values()) {
            AssemblyNode n = (AssemblyNode) o;
            if (!hs.add(n.getName())) {
                return false;
            }
        }
        return true;
    }
    
    public boolean nameExists(String name) {
        for (Object o : getNodeCache().values()) {
            AssemblyNode n = (AssemblyNode) o;
            if(n.getName().equals(name))
                return true;           
        }
        return false;
    }
    
    public void newEventGraphFromXML(String widgetName, FileBasedAssyNode node, Point p) {
        // This is not needed
        //todo yank out all the FileBasedAssyNode stuff
        newEventGraph(widgetName, node.loadedClass, p);
    }

    public void newEventGraph(String widgetName, String className, Point p) {
        EvGraphNode node = new EvGraphNode(widgetName, className);
        if (p == null) {
            node.setPosition(new Point(100, 100));
        } else {
            p.x = ((p.x + 5) / 10) * 10;    // round
            p.y = ((p.y + 5) / 10) * 10;
            node.setPosition(p);
        }

        SimEntity jaxbEG = oFactory.createSimEntity();

        jaxbEG.setName(nIe(widgetName));
        node.opaqueModelObject = jaxbEG;
        jaxbEG.setType(className);

        VInstantiator.Constr vc = new VInstantiator.Constr(jaxbEG.getType(), null);  // null means undefined
        node.setInstantiator(vc);

        getNodeCache().put(jaxbEG, node);   // key = ev

        if (!nameCheck()) {
            mangleEGName(node);
        }

        // TODO: update with generic JWSDP
        jaxbRoot.getSimEntity().add(jaxbEG);

        modelDirty = true;
        notifyChanged(new ModelEvent(node, ModelEvent.EVENTGRAPHADDED, "Event graph added to assembly"));
    }

    public void newPropChangeListener(String widgetName, String className, Point p) {
        PropChangeListenerNode pcNode = new PropChangeListenerNode(widgetName, className);
        if (p == null) {
            pcNode.setPosition(new Point(100, 100));
        } else {
            pcNode.setPosition(p);
        }

        PropertyChangeListener jaxbPCL = oFactory.createPropertyChangeListener();

        jaxbPCL.setName(nIe(widgetName));
        jaxbPCL.setType(className);

        VInstantiator.Constr vc = new VInstantiator.Constr(jaxbPCL.getType(), new Vector<Object>());
        pcNode.setInstantiator(vc);

        pcNode.opaqueModelObject = jaxbPCL;
        if (!nameCheck()) {
            manglePCLName(pcNode);
        }

        // TODO: update with generic JWSDP
        jaxbRoot.getPropertyChangeListener().add(jaxbPCL);

        modelDirty = true;
        notifyChanged(new ModelEvent(pcNode, ModelEvent.PCLADDED, "Property Change Node added to assembly"));
    }

    public void newPropChangeListenerFromXML(String widgetName, FileBasedAssyNode node, Point p) {
        // This is not needed
        //todo yank out all the FileBasedAssyNode stuff
        newPropChangeListener(widgetName, node.loadedClass, p);
    }

    public AdapterEdge newAdapterEdge(String adName, AssemblyNode src, AssemblyNode target) {
        AdapterEdge ae = new AdapterEdge();
        ae.setFrom(src);
        ae.setTo(target);
        ae.setName(adName);

        src.getConnections().add(ae);
        target.getConnections().add(ae);

        Adapter jaxbAdapter = oFactory.createAdapter();

        ae.opaqueModelObject = jaxbAdapter;
        jaxbAdapter.setTo(target.opaqueModelObject);
        jaxbAdapter.setFrom(src.opaqueModelObject);

        jaxbAdapter.setName(adName);

        assEdgeCache.put(jaxbAdapter, ae);
        jaxbRoot.getAdapter().add(jaxbAdapter);

        modelDirty = true;

        this.notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGEADDED, "Adapter edge added"));
        return ae;
    }

    public PropChangeEdge newPclEdge(AssemblyNode src, AssemblyNode target) //EvGraphNode src, PropChangeListenerNode target)
    {
        PropChangeEdge pce = new PropChangeEdge();
        pce.setFrom(src);
        pce.setTo(target);

        src.getConnections().add(pce);
        target.getConnections().add(pce);

        PropertyChangeListenerConnection pclc = oFactory.createPropertyChangeListenerConnection();

        pce.opaqueModelObject = pclc;

        pclc.setListener(target.opaqueModelObject);
        pclc.setSource(src.opaqueModelObject);

        assEdgeCache.put(pclc, pce);
        jaxbRoot.getPropertyChangeListenerConnection().add(pclc);
        modelDirty = true;

        this.notifyChanged(new ModelEvent(pce, ModelEvent.PCLEDGEADDED, "PCL edge added"));
        return pce;
    }

    public void newSimEvLisEdge(AssemblyNode src, AssemblyNode target) //EvGraphNode src, EvGraphNode target){
    {
        SimEvListenerEdge sele = new SimEvListenerEdge();
        sele.setFrom(src);
        sele.setTo(target);

        src.getConnections().add(sele);
        target.getConnections().add(sele);

        SimEventListenerConnection selc = oFactory.createSimEventListenerConnection();

        sele.opaqueModelObject = selc;

        selc.setListener(target.opaqueModelObject);
        selc.setSource(src.opaqueModelObject);

        assEdgeCache.put(selc, sele);
        jaxbRoot.getSimEventListenerConnection().add(selc);

        modelDirty = true;
        notifyChanged(new ModelEvent(sele, ModelEvent.SIMEVLISTEDGEADDED, "SimEvList edge added"));
    }

    public void deleteEvGraphNode(EvGraphNode evNode) {
        SimEntity jaxbEv = (SimEntity) evNode.opaqueModelObject;
        getNodeCache().remove(jaxbEv);
        jaxbRoot.getSimEntity().remove(jaxbEv);

        modelDirty = true;
        this.notifyChanged(new ModelEvent(evNode, ModelEvent.EVENTGRAPHDELETED, "Event graph deleted"));
    }

    public void deletePCLNode(PropChangeListenerNode pclNode) {
        PropertyChangeListener jaxbPcNode = (PropertyChangeListener) pclNode.opaqueModelObject;
        getNodeCache().remove(pclNode);
        jaxbRoot.getPropertyChangeListener().remove(jaxbPcNode);

        modelDirty = true;
        this.notifyChanged(new ModelEvent(pclNode, ModelEvent.PCLDELETED, "Property Change Listener deleted"));
    }

    /**
     * Assembly nodes don't hold onto edges.
     * @param pce 
     */
    public void deletePropChangeEdge(PropChangeEdge pce) {
        PropertyChangeListenerConnection pclc = (PropertyChangeListenerConnection) pce.opaqueModelObject;

        assEdgeCache.remove(pce);
        jaxbRoot.getPropertyChangeListenerConnection().remove(pclc);

        modelDirty = true;
        this.notifyChanged(new ModelEvent(pce, ModelEvent.PCLEDGEDELETED, "PCL edge deleted"));
    }

    public void deleteSimEvLisEdge(SimEvListenerEdge sele) {
        SimEventListenerConnection sel_c = (SimEventListenerConnection) sele.opaqueModelObject;

        assEdgeCache.remove(sele);
        jaxbRoot.getSimEventListenerConnection().remove(sel_c);

        modelDirty = true;
        this.notifyChanged(new ModelEvent(sele, ModelEvent.SIMEVLISTEDGEDELETED, "SimEvList edge deleted"));
    }

    public void deleteAdapterEdge(AdapterEdge ae) {
        Adapter j_adp = (Adapter) ae.opaqueModelObject;
        assEdgeCache.remove(ae);
        jaxbRoot.getAdapter().remove(j_adp);

        modelDirty = true;
        notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGEDELETED, "Adapter edge deleted"));
    }

    public void changePclEdge(PropChangeEdge pclEdge) {
        PropertyChangeListenerConnection pclc = (PropertyChangeListenerConnection) pclEdge.opaqueModelObject;
        pclc.setProperty(pclEdge.getProperty());
        pclc.setDescription(pclEdge.getDescriptionString());

        modelDirty = true;
        notifyChanged(new ModelEvent(pclEdge, ModelEvent.PCLEDGECHANGED, "PCL edge changed"));
    }

    public void changeAdapterEdge(AdapterEdge ae) {
        EvGraphNode src = (EvGraphNode) ae.getFrom();
        EvGraphNode targ = (EvGraphNode) ae.getTo();

        Adapter jaxbAE = (Adapter) ae.opaqueModelObject;

        jaxbAE.setFrom((SimEntity) src.opaqueModelObject);
        jaxbAE.setTo((SimEntity) targ.opaqueModelObject);

        jaxbAE.setEventHeard(ae.getSourceEvent());
        jaxbAE.setEventSent(ae.getTargetEvent());

        jaxbAE.setName(ae.getName());
        jaxbAE.setDescription(ae.getDescriptionString());

        modelDirty = true;
        notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGECHANGED, "Adapter edge changed"));
    }

    public void changeSimEvEdge(SimEvListenerEdge seEdge) {
        EvGraphNode src = (EvGraphNode) seEdge.getFrom();
        EvGraphNode targ = (EvGraphNode) seEdge.getTo();
        SimEventListenerConnection selc = (SimEventListenerConnection) seEdge.opaqueModelObject;

        selc.setListener(targ.opaqueModelObject);
        selc.setSource(src.opaqueModelObject);
        selc.setDescription(seEdge.getDescriptionString());

        modelDirty = true;
        notifyChanged(new ModelEvent(seEdge, ModelEvent.SIMEVLISTEDGECHANGED, "SimEvListener edge changed"));
    }

    public boolean changePclNode(PropChangeListenerNode pclNode) {
        boolean retcode = true;
        if (!nameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "Duplicate name detected: " + pclNode.getName() +
                    "\nUnique name substituted.");
            manglePCLName(pclNode);
            retcode = false;
        }
        PropertyChangeListener jaxBPcl = (PropertyChangeListener) pclNode.opaqueModelObject;
        jaxBPcl.setName(pclNode.getName());
        jaxBPcl.setType(pclNode.getType());
        jaxBPcl.setDescription(pclNode.getDescriptionString());

        if (pclNode.isSampleStats()) {
            if (pclNode.isClearStatsAfterEachRun()) {
                jaxBPcl.setMode("replicationStats");
            } else {
                jaxBPcl.setMode("designPointStats");
            }
        }
        
        String statistics = pclNode.isGetCount() ? "true" : "false";
        jaxBPcl.setCountStatistics(statistics);
        
        statistics = pclNode.isGetMean() ? "true" : "false";
        jaxBPcl.setMeanStatistics(statistics);

        Coordinate coor = oFactory.createCoordinate();

        int GridScale = 10;
        int x = ((pclNode.getPosition().x + GridScale / 2) / GridScale) * GridScale;
        int y = ((pclNode.getPosition().y + GridScale / 2) / GridScale) * GridScale;
        coor.setX("" + x);
        coor.setY("" + y);
        pclNode.getPosition().setLocation(x, y);
        jaxBPcl.setCoordinate(coor);

        List<Object> lis = jaxBPcl.getParameters();
        lis.clear();

        VInstantiator inst = pclNode.getInstantiator();

        List<Object> jlistt = getJaxbParamList(inst);

        // this will be a list of one...a MultiParameter....get its list, but throw away the
        // object itself.  This is because the PropertyChangeListener object serves as "its own" MultiParameter,
        if (jlistt.size() != 1) {
            throw new RuntimeException("Design error in AssemblyModel");
        }

        MultiParameter mp = (MultiParameter) jlistt.get(0);

        for (Object o : mp.getParameters()) {
            lis.add(o);
        }

        modelDirty = true;
        this.notifyChanged(new ModelEvent(pclNode, ModelEvent.PCLCHANGED, "Property Change Listener node changed"));
        return retcode;
    }

    public boolean changeEvGraphNode(EvGraphNode evNode) {
        boolean retcode = true;
        if (!nameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE, "Duplicate name detected: " + evNode.getName() +
                    "\nUnique name substituted.");
            mangleEGName(evNode);
            retcode = false;
        }
        SimEntity jaxbSE = (SimEntity) evNode.opaqueModelObject;

        jaxbSE.setName(evNode.getName());
        jaxbSE.setType(evNode.getType());
        jaxbSE.setDescription(evNode.getDescriptionString());

        Coordinate coor = oFactory.createCoordinate();

        int GridScale = 10;
        int x = ((evNode.getPosition().x + GridScale / 2) / GridScale) * GridScale;
        int y = ((evNode.getPosition().y + GridScale / 2) / GridScale) * GridScale;
        coor.setX("" + x);
        coor.setY("" + y);
        evNode.getPosition().setLocation(x, y);
        jaxbSE.setCoordinate(coor);

        List<Object> lis = jaxbSE.getParameters();
        lis.clear();

        VInstantiator inst = evNode.getInstantiator();

        List<Object> jlistt = getJaxbParamList(inst);

        // this will be a list of one...a MultiParameter....get its list, but throw away the
        // object itself.  This is because the SimEntity object serves as "its own" MultiParameter,
        if (jlistt.size() != 1) {
            throw new RuntimeException("Design error in AssemblyModel");
        }

        MultiParameter mp = (MultiParameter) jlistt.get(0);

        for (Object o : mp.getParameters()) {
            lis.add(o);
        }

        if (evNode.isOutputMarked()) {
            addToOutputList(jaxbSE);
        } else {
            removeFromOutputList(jaxbSE);
        }

        modelDirty = true;
        this.notifyChanged(new ModelEvent(evNode, ModelEvent.EVENTGRAPHCHANGED, "Event changed"));
        return retcode;
    }

    private void removeFromOutputList(SimEntity se) {
        List<Output> outTL = jaxbRoot.getOutput();
        for (Output o : outTL) {
            if (o.getEntity() == se) {
                outTL.remove(o);
                return;
            }
        }
    }

    private void addToOutputList(SimEntity se) {
        List<Output> outTL = jaxbRoot.getOutput();
        for (Output o : outTL) {
            if (o.getEntity() == se) {
                return;
            }
        }
        Output op = oFactory.createOutput();

        op.setEntity(se);

        outTL.add(op);
    }

    public Vector<String> getVerboseEntityNames() {
        Vector<String> v = new Vector<String>();
        for (Output ot : jaxbRoot.getOutput()) {
            Object entity = ot.getEntity();
            if (entity instanceof SimEntity) {
                v.add(((SimEntity) entity).getName());
            } else if (entity instanceof PropertyChangeListener) {
                v.add(((PropertyChangeListener) entity).getName());
            }
        }
        return v;
    }

    private List<Object> getInstantiatorListFromJaxbParmList(List<Object> lis) {

        List<Object> vi = new ArrayList<Object>();

        for (Object o : lis) {
            vi.add(buildInstantiatorFromJaxbParameter(o));
        }
        return vi;
    }

    private List<String> getNamesFromParmList(List<Object> lis) {
        List<String> v = new ArrayList<String>();
        for (Object o : lis) {
            if (o instanceof TerminalParameter) {
                String n = ((TerminalParameter) o).getName();
                v.add(n);
            } else {
                v.add("");
            }
        }
        return v;
    }

    private VInstantiator buildInstantiatorFromJaxbParameter(Object o) {
        if (o instanceof TerminalParameter) {
            return buildFreeFormFromTermParameter((TerminalParameter) o);
        }
        if (o instanceof MultiParameter) {           // used for both arrays and Constr arg lists
            MultiParameter mu = (MultiParameter) o;
            return (mu.getType().indexOf('[') != -1) ? buildArrayFromMultiParameter(mu) : buildConstrFromMultiParameter(mu);
        }
        return (o instanceof FactoryParameter) ? buildFactoryInstFromFactoryParameter((FactoryParameter) o) : null;
    }

    private VInstantiator.Array buildArrayFromMultiParameter(MultiParameter o) {
        return new VInstantiator.Array(o.getType(), getInstantiatorListFromJaxbParmList(o.getParameters()));
    }

    private VInstantiator.Constr buildConstrFromMultiParameter(MultiParameter o) {
        return new VInstantiator.Constr(o.getType(), getInstantiatorListFromJaxbParmList(o.getParameters()));
    }

    private VInstantiator.Factory buildFactoryInstFromFactoryParameter(FactoryParameter o) {
        return new VInstantiator.Factory(o.getType(), o.getFactory(),
                "getInstance", getInstantiatorListFromJaxbParmList(o.getParameters()));
    }

    private VInstantiator.FreeF buildFreeFormFromTermParameter(TerminalParameter tp) {
        return new VInstantiator.FreeF(tp.getType(), tp.getValue());
    }

    // We know we will get a List<Object> one way or the other
    @SuppressWarnings("unchecked")
    private List<Object> getJaxbParamList(VInstantiator vi) {
        Object o = buildParam(vi);
        if (o instanceof List<?>) {
            return (List<Object>) o;
        }

        Vector<Object> v = new Vector<Object>();
        v.add(o);
        return v;
    }

    private Object buildParam(Object vi) {
        if (vi instanceof VInstantiator.FreeF) {
            return buildParmFromFreeF((VInstantiator.FreeF) vi);
        }      //TerminalParm
        if (vi instanceof VInstantiator.Constr) {
            return buildParmFromConstr((VInstantiator.Constr) vi);
        }     // List of Parms
        if (vi instanceof VInstantiator.Factory) {
            return buildParmFromFactory((VInstantiator.Factory) vi);
        }   // FactoryParam
        if (vi instanceof VInstantiator.Array) {
            return buildParmFromArray((VInstantiator.Array) vi);
        }       // MultiParam

        //assert false : AssemblyModel.buildJaxbParameter() received null;
        return null;
    }

    private TerminalParameter buildParmFromFreeF(VInstantiator.FreeF viff) {
        TerminalParameter tp = oFactory.createTerminalParameter();

        tp.setType(viff.getType());
        tp.setValue(viff.getValue());
        tp.setName(viff.getName());
        return tp;
    }

    private MultiParameter buildParmFromConstr(VInstantiator.Constr vicon) {
        MultiParameter mp = oFactory.createMultiParameter();

        mp.setType(vicon.getType());
        for (Object o : vicon.getArgs()) {
            VInstantiator vi = (VInstantiator) o;
            mp.getParameters().add(buildParam(vi));
        }
        return mp;
    }

    private FactoryParameter buildParmFromFactory(VInstantiator.Factory vifact) {
        FactoryParameter fp = oFactory.createFactoryParameter();

        fp.setType(vifact.getType());
        fp.setFactory(vifact.getFactoryClass()); //todo when method supported +"."+vifact.getMethod()+"()");

        for (Object vi : vifact.getParams()) {
            fp.getParameters().add(buildParam(vi));
        }
        return fp;
    }

    private MultiParameter buildParmFromArray(VInstantiator.Array viarr) {
        MultiParameter mp = oFactory.createMultiParameter();

        mp.setType(viarr.getType());

        for (Object vi : viarr.getInstantiators()) {
            mp.getParameters().add(buildParam(vi));
        }
        return mp;
    }

    public boolean newModel(File f) {
        getNodeCache().clear();
        assEdgeCache.clear();
        pointLess = new Point(100, 100);
        this.notifyChanged(new ModelEvent(this, ModelEvent.NEWASSEMBLYMODEL, "New empty assembly model"));
        
        if (f == null) {
            jaxbRoot = oFactory.createSimkitAssembly(); // to start with empty graph
        } else {
            try {
                Unmarshaller u = jc.createUnmarshaller();
                jaxbRoot = (SimkitAssembly) u.unmarshal(f);
                
                GraphMetaData mymetaData = new GraphMetaData(this);
                mymetaData.version = jaxbRoot.getVersion();
                mymetaData.name = jaxbRoot.getName();
                mymetaData.packageName = jaxbRoot.getPackage();

                Schedule sch = jaxbRoot.getSchedule();
                if (sch != null) {
                    String stpTime = sch.getStopTime();
                    if (stpTime != null && stpTime.trim().length() > 0) {
                        mymetaData.stopTime = stpTime.trim();
                    }
                    mymetaData.verbose = sch.getVerbose().equalsIgnoreCase("true");
                }

                changeMetaData(mymetaData);
                buildEGsFromJaxb(jaxbRoot.getSimEntity(), jaxbRoot.getOutput());
                buildPCLsFromJaxb(jaxbRoot.getPropertyChangeListener());
                buildPCConnectionsFromJaxb(jaxbRoot.getPropertyChangeListenerConnection());
                buildSimEvConnectionsFromJaxb(jaxbRoot.getSimEventListenerConnection());
                buildAdapterConnectionsFromJaxb(jaxbRoot.getAdapter());
            } catch (JAXBException e) {
                // want a clear way to know if they're trying to load an event graph
                try {
                    JAXBContext egCtx = JAXBContext.newInstance("viskit.xsd.bindings");

                    Unmarshaller um = egCtx.createUnmarshaller();
                    um.unmarshal(f);
                    // If we get here, they've tried to load an event graph.
                    JOptionPane.showMessageDialog(null, "Use the event graph editor to" +
                            "\n" + "work with this file.",
                            "Wrong File Format", JOptionPane.ERROR_MESSAGE);
                } catch (JAXBException ee) {
                    JOptionPane.showMessageDialog(null, "Exception on JAXB unmarshalling" +
                            "\n" + f.getName() +
                            "\n" + e.getMessage(),
                            "XML I/O Error", JOptionPane.ERROR_MESSAGE);
                }
                return false; // from both exceptions
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Exception constructing assembly" +
                        "\n" + f.getName(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        
        currentFile = f;
        setDirty(false);
        return true;
    }

    private void buildPCConnectionsFromJaxb(List<PropertyChangeListenerConnection> pcconnsList) {
        for (PropertyChangeListenerConnection pclc : pcconnsList) {
            PropChangeEdge pce = new PropChangeEdge();
            pce.setProperty(pclc.getProperty());
            pce.setDescriptionString(pclc.getDescription());
            AssemblyNode toNode = getNodeCache().get(pclc.getListener());
            AssemblyNode frNode = getNodeCache().get(pclc.getSource());
            pce.setTo(toNode);
            pce.setFrom(frNode);
            pce.opaqueModelObject = pclc;

            toNode.getConnections().add(pce);
            frNode.getConnections().add(pce);

            assEdgeCache.put(pclc, pce);
            this.notifyChanged(new ModelEvent(pce, ModelEvent.PCLEDGEADDED, "PCL edge added"));
        }
    }

    private void buildSimEvConnectionsFromJaxb(List<SimEventListenerConnection> simevconnsList) {
        for (SimEventListenerConnection selc : simevconnsList) {
            SimEvListenerEdge sele = new SimEvListenerEdge();
            AssemblyNode toNode = getNodeCache().get(selc.getListener());
            AssemblyNode frNode = getNodeCache().get(selc.getSource());
            sele.setTo(toNode);
            sele.setFrom(frNode);
            sele.opaqueModelObject = selc;
            sele.setDescriptionString(selc.getDescription());

            toNode.getConnections().add(sele);
            frNode.getConnections().add(sele);
            assEdgeCache.put(selc, sele);
            this.notifyChanged(new ModelEvent(sele, ModelEvent.SIMEVLISTEDGEADDED, "Sim event listener connection added"));
        }
    }

    private void buildAdapterConnectionsFromJaxb(List<Adapter> adaptersList) {
        for (Adapter jaxbAdapter : adaptersList) {
            AdapterEdge ae = new AdapterEdge();
            AssemblyNode toNode = getNodeCache().get(jaxbAdapter.getTo());
            AssemblyNode frNode = getNodeCache().get(jaxbAdapter.getFrom());
            ae.setTo(toNode);
            ae.setFrom(frNode);
            ae.setSourceEvent(jaxbAdapter.getEventHeard());
            ae.setTargetEvent(jaxbAdapter.getEventSent());
            ae.setName(jaxbAdapter.getName());
            ae.setDescriptionString(jaxbAdapter.getDescription());
            ae.opaqueModelObject = jaxbAdapter;

            toNode.getConnections().add(ae);
            frNode.getConnections().add(ae);
            assEdgeCache.put(jaxbAdapter, ae);
            this.notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGEADDED, "Adapter connection added"));
        }
    }

    private void buildPCLsFromJaxb(List<PropertyChangeListener> pcLs) {
        for (PropertyChangeListener pcl : pcLs) {
            buildPclNodeFromJaxbPCL(pcl);
        }
    }

    private void buildEGsFromJaxb(List<SimEntity> simEntities, List<Output> outputList) {
        for (SimEntity se : simEntities) {
            boolean isOutput = false;

            // This must be done in this order, because the buildEvgNode...below
            // causes AssembleModel to be reentered, and the outputList gets hit.
            for (Output o : outputList) {
                SimEntity simE = (SimEntity) o.getEntity();
                if (simE == se) {
                    isOutput = true;
                    break;
                }
            }
            buildEvgNodeFromJaxbSimEntity(se, isOutput);
        }
    }

    private PropChangeListenerNode buildPclNodeFromJaxbPCL(PropertyChangeListener pcl) {
        PropChangeListenerNode pNode = (PropChangeListenerNode) getNodeCache().get(pcl);
        if (pNode != null) {
            return pNode;
        }
        pNode = new PropChangeListenerNode(pcl.getName(), pcl.getType());
        pNode.setClearStatsAfterEachRun(pcl.getMode().equals("replicationStats"));
        pNode.setGetMean(Boolean.parseBoolean(pcl.getMeanStatistics()));
        pNode.setGetCount(Boolean.parseBoolean(pcl.getCountStatistics()));
        pNode.setDescriptionString(pcl.getDescription());
        Coordinate coor = pcl.getCoordinate();
        if (coor == null) {
            pNode.setPosition(pointLess);
            pointLess = new Point(pointLess.x + 20, pointLess.y + 20);
        } else {
            pNode.setPosition(new Point(Integer.parseInt(coor.getX()),
                    Integer.parseInt(coor.getY())));
        }
        List<Object> lis = pcl.getParameters();
        VInstantiator.Constr vc = new VInstantiator.Constr(pcl.getType(),
                getInstantiatorListFromJaxbParmList(lis));
        pNode.setInstantiator(vc);

        pNode.opaqueModelObject = pcl;
        log.debug("pNode name: " + pNode.getName());

        getNodeCache().put(pcl, pNode);   // key = se

        if (!nameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML file contains duplicate event name: " + pNode.getName() +
                    "\nUnique name substituted.");
            manglePCLName(pNode);
        }
        notifyChanged(new ModelEvent(pNode, ModelEvent.PCLADDED, "PCL added"));
        return pNode;
    }

    private EvGraphNode buildEvgNodeFromJaxbSimEntity(SimEntity se, boolean isOutputNode) {
        EvGraphNode en = (EvGraphNode) getNodeCache().get(se);
        if (en != null) {
            return en;
        }
        en = new EvGraphNode(se.getName(), se.getType());

        Coordinate coor = se.getCoordinate();
        if (coor == null) {
            en.setPosition(pointLess);
            pointLess = new Point(pointLess.x + 20, pointLess.y + 20);
        } else {
            en.setPosition(new Point(Integer.parseInt(coor.getX()),
                    Integer.parseInt(coor.getY())));
        }

        en.setDescriptionString(se.getDescription());
        en.setOutputMarked(isOutputNode);
        List<Object> lis = se.getParameters();
        VInstantiator.Constr vc = new VInstantiator.Constr(lis, se.getType());
        en.setInstantiator(vc);

        en.opaqueModelObject = se;

        getNodeCache().put(se, en);   // key = se

        if (!nameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML file contains duplicate event name: " + en.getName() +
                    "\nUnique name substituted.");
            mangleEGName(en);
        }
        notifyChanged(new ModelEvent(en, ModelEvent.EVENTGRAPHADDED, "Event added"));

        return en;
    }

    /**
     * Boolean to signify whether the model has been changed since last disk save.
     *
     * @return true means changes have been made and it needs to be flushed.
     */
    public boolean isDirty() {
        return modelDirty;
    }

    public void setDirty(boolean wh) {
        modelDirty = wh;
    }

    public SimkitAssembly getJaxbRoot() {
        return jaxbRoot;
    }

    public GraphMetaData getMetaData() {
        return metaData;
    }

    public void changeMetaData(GraphMetaData gmd) {
        metaData = gmd;
    }

    /**
     * "nullIfEmpty" Return the passed string if non-zero length, else null
     * @param s
     * @return 
     */
    private String nIe(String s) {
        if (s != null) {
            if (s.length() == 0) {
                s = null;
            }
        }
        return s;
    }

    public Map<Object, AssemblyNode> getNodeCache() {
        return nodeCache;
    }

    public void setNodeCache(Map<Object, AssemblyNode> nodeCache) {
        this.nodeCache = nodeCache;
    }
}
