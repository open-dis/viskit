package viskit.model;

import edu.nps.util.LogUtils;
import edu.nps.util.TempFileManager;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import javax.swing.JOptionPane;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import viskit.VGlobals;
import viskit.VStatics;
import viskit.util.FileBasedAssyNode;
import viskit.control.AssemblyControllerImpl;
import viskit.mvc.mvcAbstractModel;
import viskit.mvc.mvcController;
import viskit.util.XMLValidationTool;
import viskit.xsd.bindings.assembly.*;
import viskit.xsd.translator.assembly.SimkitAssemblyXML2Java;

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
public class AssemblyModelImpl extends mvcAbstractModel implements AssemblyModel {

    private JAXBContext jc;
    private ObjectFactory oFactory;
    private SimkitAssembly jaxbRoot;
    private File currentFile;
    private boolean modelDirty = false;
    private GraphMetaData metaData;

    /** We require specific order on this Map's contents */
    private Map<String, AssemblyNode> nodeCache;
    private String schemaLoc = XMLValidationTool.ASSEMBLY_SCHEMA;
    private Point2D.Double pointLess;
    private AssemblyControllerImpl controller;

    public AssemblyModelImpl(mvcController cont) {
        pointLess = new Point2D.Double(30, 60);
        controller = (AssemblyControllerImpl) cont;
        metaData = new GraphMetaData(this);
        nodeCache = new LinkedHashMap<>();
    }

    public void init() {
        try {
            jc = JAXBContext.newInstance(SimkitAssemblyXML2Java.ASSEMBLY_BINDINGS);
            oFactory = new ObjectFactory();
            jaxbRoot = oFactory.createSimkitAssembly(); // to start with empty graph
        } catch (JAXBException e) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML Error",
                    "Exception on JAXBContext instantiation" +
                    "\n" + e.getMessage()
                    );
        }
    }

    @Override
    public boolean isDirty() {
        return modelDirty;
    }

    @Override
    public void setDirty(boolean wh) {
        modelDirty = wh;
    }

    @Override
    public SimkitAssembly getJaxbRoot() {
        return jaxbRoot;
    }

    @Override
    public GraphMetaData getMetaData() {
        return metaData;
    }

    @Override
    public void changeMetaData(GraphMetaData gmd) {
        metaData = gmd;
        setDirty(true);
    }

    @Override
    public boolean newModel(File f) {
        getNodeCache().clear();
        pointLess = new Point2D.Double(30, 60);
        this.notifyChanged(new ModelEvent(this, ModelEvent.NEWASSEMBLYMODEL, "New empty assembly model"));

        if (f == null) {
            jaxbRoot = oFactory.createSimkitAssembly(); // to start with empty graph
        } else {
            try {
                Unmarshaller u = jc.createUnmarshaller();

                // Check for inadvertant opening of an EG, tough to do, yet possible (bugfix 1248)
                try {
                    jaxbRoot = (SimkitAssembly) u.unmarshal(f);
                } catch (ClassCastException cce) {
                    // If we get here, they've tried to load an event graph.
                    controller.messageUser(JOptionPane.ERROR_MESSAGE,
                            "Wrong File Format",
                            "Use the event graph editor to" +
                            "\n" + "work with this file."
                            );
                    return false;
                }

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
                buildEGsFromJaxb(jaxbRoot.getSimEntity(), jaxbRoot.getOutput(), jaxbRoot.getVerbose());
                buildPCLsFromJaxb(jaxbRoot.getPropertyChangeListener());
                buildPCConnectionsFromJaxb(jaxbRoot.getPropertyChangeListenerConnection());
                buildSimEvConnectionsFromJaxb(jaxbRoot.getSimEventListenerConnection());
                buildAdapterConnectionsFromJaxb(jaxbRoot.getAdapter());
            } catch (JAXBException e) {
                controller.messageUser(JOptionPane.ERROR_MESSAGE,
                        "XML I/O Error",
                        "Exception on JAXB unmarshalling of" +
                            "\n" + f.getName() +
                            "\n" + e.getMessage() +
                            "\nin AssemblyModel.newModel(File)"
                            );

                return false;
            }
        }

        currentFile = f;
        setDirty(false);
        return true;
    }

    @Override
    public void saveModel(File f) {
        if (f == null) {
            f = currentFile;
        }

        // Do the marshalling into a temporary file so as to avoid possible
        // deletion of existing file on a marshal error.

        File tmpF;
        FileWriter fw = null;
        try {
            tmpF = TempFileManager.createTempFile("tmpAsymarshal", ".xml");
        } catch (IOException e) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "I/O Error",
                    "Exception creating temporary file, AssemblyModel.saveModel():" +
                    "\n" + e.getMessage()
                    );
            return;
        }

        try {
            fw = new FileWriter(tmpF);
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, schemaLoc);

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

            // OK, made it through the marshal, overwrite the "real" file
            Files.copy(tmpF.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);

            modelDirty = false;
            currentFile = f;
        } catch (JAXBException e) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML I/O Error",
                    "Exception on JAXB marshalling" +
                    "\n" + f +
                    "\n" + e.getMessage() +
                    "\n(check for blank data fields)"
                    );
        } catch (IOException ex) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "File I/O Error",
                    "Exception on writing " + f.getName() +
                    "\n" + ex.getMessage());
        } finally {
            try {
                if (fw != null)
                    fw.close();
            } catch (IOException ioe) {}
        }
    }

    @Override
    public File getLastFile() {
        return currentFile;
    }

    @Override
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
        int nxt = mangleRandom.nextInt(0x1_0000); // 4 hex digits
        StringBuilder sb = new StringBuilder(name);
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
        Set<String> hs = new HashSet<>(10);
        for (AssemblyNode n : getNodeCache().values()) {
            if (!hs.add(n.getName())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean nameExists(String name) {
        for (AssemblyNode n : getNodeCache().values()) {
            if (n.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void newEventGraphFromXML(String widgetName, FileBasedAssyNode node, Point2D p) {
        newEventGraph(widgetName, node.loadedClass, p);
    }

    @Override
    public void newEventGraph(String widgetName, String className, Point2D p) {
        EvGraphNode node = new EvGraphNode(widgetName, className);
        if (p == null) {
            node.setPosition(pointLess);
        } else {
            node.setPosition(p);
        }

        SimEntity jaxbEG = oFactory.createSimEntity();

        jaxbEG.setName(nIe(widgetName));
        jaxbEG.setType(className);
        node.opaqueModelObject = jaxbEG;

        VInstantiator vc = new VInstantiator.Constr(jaxbEG.getType(), null);  // null means undefined
        node.setInstantiator(vc);

        if (!nameCheck()) {
            mangleEGName(node);
        }

        getNodeCache().put(node.getName(), node);   // key = ev

        jaxbRoot.getSimEntity().add(jaxbEG);

        modelDirty = true;
        notifyChanged(new ModelEvent(node, ModelEvent.EVENTGRAPHADDED, "Event graph added to assembly"));
    }

    @Override
    public void redoEventGraph(EvGraphNode node) {
        SimEntity jaxbEG = oFactory.createSimEntity();

        jaxbEG.setName(node.getName());
        node.opaqueModelObject = jaxbEG;
        jaxbEG.setType(node.getType());

        getNodeCache().put(node.getName(), node);   // key = ev

        jaxbRoot.getSimEntity().add(jaxbEG);

        modelDirty = true;
        notifyChanged(new ModelEvent(node, ModelEvent.REDO_EVENT_GRAPH, "Event Graph redone"));
    }

    @Override
    public void deleteEvGraphNode(EvGraphNode evNode) {
        SimEntity jaxbEv = (SimEntity) evNode.opaqueModelObject;
        getNodeCache().remove(jaxbEv.getName());
        jaxbRoot.getSimEntity().remove(jaxbEv);

        modelDirty = true;

        if (!controller.isUndo())
            notifyChanged(new ModelEvent(evNode, ModelEvent.EVENTGRAPHDELETED, "Event Graph deleted"));
        else
            notifyChanged(new ModelEvent(evNode, ModelEvent.UNDO_EVENT_GRAPH, "Event Graph undone"));
    }

    @Override
    public void newPropChangeListenerFromXML(String widgetName, FileBasedAssyNode node, Point2D p) {
        newPropChangeListener(widgetName, node.loadedClass, p);
    }

    @Override
    public void newPropChangeListener(String widgetName, String className, Point2D p) {
        PropChangeListenerNode pcNode = new PropChangeListenerNode(widgetName, className);
        if (p == null) {
            pcNode.setPosition(pointLess);
        } else {
            pcNode.setPosition(p);
        }

        PropertyChangeListener pcl = oFactory.createPropertyChangeListener();

        pcl.setName(nIe(widgetName));
        pcl.setType(className);
        pcNode.opaqueModelObject = pcl;

        List<Object> lis = pcl.getParameters();

        VInstantiator vc = new VInstantiator.Constr(pcl.getType(), lis);
        pcNode.setInstantiator(vc);

        if (!nameCheck()) {
            manglePCLName(pcNode);
        }

        getNodeCache().put(pcNode.getName(), pcNode);   // key = ev

        jaxbRoot.getPropertyChangeListener().add(pcl);

        modelDirty = true;
        notifyChanged(new ModelEvent(pcNode, ModelEvent.PCLADDED, "Property Change Node added to assembly"));
    }

    @Override
    public void redoPropChangeListener(PropChangeListenerNode node) {

        PropertyChangeListener jaxbPCL = oFactory.createPropertyChangeListener();

        jaxbPCL.setName(node.getName());
        jaxbPCL.setType(node.getType());

        node.opaqueModelObject = jaxbPCL;

        jaxbRoot.getPropertyChangeListener().add(jaxbPCL);

        modelDirty = true;
        notifyChanged(new ModelEvent(node, ModelEvent.REDO_PCL, "Property Change Node redone"));
    }

    @Override
    public void deletePropChangeListener(PropChangeListenerNode pclNode) {
        PropertyChangeListener jaxbPcNode = (PropertyChangeListener) pclNode.opaqueModelObject;
        getNodeCache().remove(pclNode.getName());
        jaxbRoot.getPropertyChangeListener().remove(jaxbPcNode);

        modelDirty = true;

        if (!controller.isUndo())
            notifyChanged(new ModelEvent(pclNode, ModelEvent.PCLDELETED, "Property Change Listener deleted"));
        else
            notifyChanged(new ModelEvent(pclNode, ModelEvent.UNDO_PCL, "Property Change Listener undone"));
    }

    @Override
    public AdapterEdge newAdapterEdge(String adName, AssemblyNode src, AssemblyNode target) {
        AdapterEdge ae = new AdapterEdge();
        ae.setFrom(src);
        ae.setTo(target);
        ae.setName(adName);

        src.getConnections().add(ae);
        target.getConnections().add(ae);

        Adapter jaxbAdapter = oFactory.createAdapter();

        ae.opaqueModelObject = jaxbAdapter;
        jaxbAdapter.setTo(target.getName());
        jaxbAdapter.setFrom(src.getName());

        jaxbAdapter.setName(adName);

        jaxbRoot.getAdapter().add(jaxbAdapter);

        modelDirty = true;

        this.notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGEADDED, "Adapter edge added"));
        return ae;
    }

    @Override
    public void redoAdapterEdge(AdapterEdge ae) {
        AssemblyNode src, target;

        src = (AssemblyNode) ae.getFrom();
        target = (AssemblyNode) ae.getTo();

        Adapter jaxbAdapter = oFactory.createAdapter();
        ae.opaqueModelObject = jaxbAdapter;
        jaxbAdapter.setTo(target.getName());
        jaxbAdapter.setFrom(src.getName());
        jaxbAdapter.setName(ae.getName());

        jaxbRoot.getAdapter().add(jaxbAdapter);

        modelDirty = true;

        this.notifyChanged(new ModelEvent(ae, ModelEvent.REDO_ADAPTER_EDGE, "Adapter edge added"));
    }

    @Override
    public PropChangeEdge newPropChangeEdge(AssemblyNode src, AssemblyNode target) {
        PropChangeEdge pce = new PropChangeEdge();
        pce.setFrom(src);
        pce.setTo(target);

        src.getConnections().add(pce);
        target.getConnections().add(pce);

        PropertyChangeListenerConnection pclc = oFactory.createPropertyChangeListenerConnection();

        pce.opaqueModelObject = pclc;

        pclc.setListener(target.getName());
        pclc.setSource(src.getName());

        jaxbRoot.getPropertyChangeListenerConnection().add(pclc);
        modelDirty = true;

        this.notifyChanged(new ModelEvent(pce, ModelEvent.PCLEDGEADDED, "PCL edge added"));
        return pce;
    }

    @Override
    public void redoPropChangeEdge(PropChangeEdge pce) {
        AssemblyNode src, target;

        src = (AssemblyNode) pce.getFrom();
        target = (AssemblyNode) pce.getTo();

        PropertyChangeListenerConnection pclc = oFactory.createPropertyChangeListenerConnection();
        pce.opaqueModelObject = pclc;
        pclc.setListener(target.getName());
        pclc.setSource(src.getName());

        jaxbRoot.getPropertyChangeListenerConnection().add(pclc);
        modelDirty = true;

        this.notifyChanged(new ModelEvent(pce, ModelEvent.REDO_PCL_EDGE, "PCL edge added"));
    }

    @Override
    public void newSimEvLisEdge(AssemblyNode src, AssemblyNode target) {
        SimEvListenerEdge sele = new SimEvListenerEdge();
        sele.setFrom(src);
        sele.setTo(target);

        src.getConnections().add(sele);
        target.getConnections().add(sele);

        SimEventListenerConnection selc = oFactory.createSimEventListenerConnection();

        sele.opaqueModelObject = selc;

        selc.setListener(target.getName());
        selc.setSource(src.getName());

        jaxbRoot.getSimEventListenerConnection().add(selc);

        modelDirty = true;
        notifyChanged(new ModelEvent(sele, ModelEvent.SIMEVLISTEDGEADDED, "SimEvList edge added"));
    }

    @Override
    public void redoSimEvLisEdge(SimEvListenerEdge sele) {
        AssemblyNode src, target;

        src = (AssemblyNode) sele.getFrom();
        target = (AssemblyNode) sele.getTo();

        SimEventListenerConnection selc = oFactory.createSimEventListenerConnection();
        sele.opaqueModelObject = selc;
        selc.setListener(target.getName());
        selc.setSource(src.getName());

        jaxbRoot.getSimEventListenerConnection().add(selc);

        modelDirty = true;
        notifyChanged(new ModelEvent(sele, ModelEvent.REDO_SIM_EVENT_LISTENER_EDGE, "SimEvList Edge redone"));
    }

    @Override
    public void deletePropChangeEdge(PropChangeEdge pce) {
        PropertyChangeListenerConnection pclc = (PropertyChangeListenerConnection) pce.opaqueModelObject;

        jaxbRoot.getPropertyChangeListenerConnection().remove(pclc);

        modelDirty = true;

        if (!controller.isUndo())
            notifyChanged(new ModelEvent(pce, ModelEvent.PCLEDGEDELETED, "PCL edge deleted"));
        else
            notifyChanged(new ModelEvent(pce, ModelEvent.UNDO_PCL_EDGE, "PCL edge undone"));
    }

    @Override
    public void deleteSimEvLisEdge(SimEvListenerEdge sele) {
        SimEventListenerConnection sel_c = (SimEventListenerConnection) sele.opaqueModelObject;

        jaxbRoot.getSimEventListenerConnection().remove(sel_c);

        modelDirty = true;

        if (!controller.isUndo())
            notifyChanged(new ModelEvent(sele, ModelEvent.SIMEVLISTEDGEDELETED, "SimEvList edge deleted"));
        else
            notifyChanged(new ModelEvent(sele, ModelEvent.UNDO_SIM_EVENT_LISTENER_EDGE, "SimEvList edge undone"));
    }

    @Override
    public void deleteAdapterEdge(AdapterEdge ae) {
        Adapter j_adp = (Adapter) ae.opaqueModelObject;
        jaxbRoot.getAdapter().remove(j_adp);

        modelDirty = true;

        if (!controller.isUndo())
            notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGEDELETED, "Adapter edge deleted"));
        else
            notifyChanged(new ModelEvent(ae, ModelEvent.UNDO_ADAPTER_EDGE, "Adapter edge undone"));
    }

    @Override
    public void changePclEdge(PropChangeEdge pclEdge) {
        PropertyChangeListenerConnection pclc = (PropertyChangeListenerConnection) pclEdge.opaqueModelObject;
        pclc.setProperty(pclEdge.getProperty());
        pclc.setDescription(pclEdge.getDescriptionString());

        modelDirty = true;
        notifyChanged(new ModelEvent(pclEdge, ModelEvent.PCLEDGECHANGED, "PCL edge changed"));
    }

    @Override
    public void changeAdapterEdge(AdapterEdge ae) {
        EvGraphNode src = (EvGraphNode) ae.getFrom();
        EvGraphNode targ = (EvGraphNode) ae.getTo();

        Adapter jaxbAE = (Adapter) ae.opaqueModelObject;

        jaxbAE.setFrom(src.getName());
        jaxbAE.setTo(targ.getName());

        jaxbAE.setEventHeard(ae.getSourceEvent());
        jaxbAE.setEventSent(ae.getTargetEvent());

        jaxbAE.setName(ae.getName());
        jaxbAE.setDescription(ae.getDescriptionString());

        modelDirty = true;
        notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGECHANGED, "Adapter edge changed"));
    }

    @Override
    public void changeSimEvEdge(SimEvListenerEdge seEdge) {
        EvGraphNode src = (EvGraphNode) seEdge.getFrom();
        EvGraphNode targ = (EvGraphNode) seEdge.getTo();
        SimEventListenerConnection selc = (SimEventListenerConnection) seEdge.opaqueModelObject;

        selc.setListener(targ.getName());
        selc.setSource(src.getName());
        selc.setDescription(seEdge.getDescriptionString());

        modelDirty = true;
        notifyChanged(new ModelEvent(seEdge, ModelEvent.SIMEVLISTEDGECHANGED, "SimEvListener edge changed"));
    }

    @Override
    public boolean changePclNode(PropChangeListenerNode pclNode) {
        boolean retcode = true;
        if (!nameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "Duplicate name detected", pclNode.getName() +
                    "\nUnique name substituted.");
            manglePCLName(pclNode);
            retcode = false;
        }
        PropertyChangeListener jaxBPcl = (PropertyChangeListener) pclNode.opaqueModelObject;
        jaxBPcl.setName(pclNode.getName());
        jaxBPcl.setType(pclNode.getType());
        jaxBPcl.setDescription(pclNode.getDescriptionString());

        // Modes should be singular.  All new Assemblies will be with singular mode
        if (pclNode.isSampleStats()) {
            if (pclNode.isClearStatsAfterEachRun()) {
                jaxBPcl.setMode("replicationStat");
            } else {
                jaxBPcl.setMode("designPointStat");
            }
        }

        String statistics = pclNode.isGetCount() ? "true" : "false";
        jaxBPcl.setCountStatistics(statistics);

        statistics = pclNode.isGetMean() ? "true" : "false";
        jaxBPcl.setMeanStatistics(statistics);

        double x = pclNode.getPosition().getX();
        double y = pclNode.getPosition().getY();
        Coordinate coor = oFactory.createCoordinate();
        coor.setX("" + x);
        coor.setY("" + y);
        pclNode.getPosition().setLocation(x, y);
        jaxBPcl.setCoordinate(coor);

        List<Object> lis = jaxBPcl.getParameters();
        lis.clear();

        VInstantiator inst = pclNode.getInstantiator();

        // this will be a list of one...a MultiParameter....get its list, but
        // throw away the object itself.  This is because the
        // PropertyChangeListener object serves as "its own" MultiParameter.
        List<Object> jlistt = getJaxbParamList(inst);

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

    @Override
    public boolean changeEvGraphNode(EvGraphNode evNode) {
        boolean retcode = true;
        if (!nameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "Duplicate name detected", evNode.getName() +
                    "\nUnique name substituted.");
            mangleEGName(evNode);
            retcode = false;
        }
        SimEntity jaxbSE = (SimEntity) evNode.opaqueModelObject;

        jaxbSE.setName(evNode.getName());
        jaxbSE.setType(evNode.getType());
        jaxbSE.setDescription(evNode.getDescriptionString());

        double x = evNode.getPosition().getX();
        double y = evNode.getPosition().getY();
        Coordinate coor = oFactory.createCoordinate();
        coor.setX("" + x);
        coor.setY("" + y);
        evNode.getPosition().setLocation(x, y);
        jaxbSE.setCoordinate(coor);

        List<Object> lis = jaxbSE.getParameters();
        lis.clear();

        VInstantiator inst = evNode.getInstantiator();

        // this will be a list of one...a MultiParameter....get its list, but
        // throw away the object itself.  This is because the SimEntity object
        // serves as "its own" MultiParameter.
        List<Object> jlistt = getJaxbParamList(inst);

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

        if (evNode.isVerboseMarked()) {
            addToVerboseList(jaxbSE);
        } else {
            removeFromVerboseList(jaxbSE);
        }

        modelDirty = true;
        this.notifyChanged(new ModelEvent(evNode, ModelEvent.EVENTGRAPHCHANGED, "Event changed"));
        return retcode;
    }

    private void removeFromOutputList(SimEntity se) {
        List<Output> outTL = jaxbRoot.getOutput();
        for (Output o : outTL) {
            if (o.getEntity().equals(se.getName())) {
                outTL.remove(o);
                return;
            }
        }
    }

    private void removeFromVerboseList(SimEntity se) {
        List<Verbose> vTL = jaxbRoot.getVerbose();
        for (Verbose v : vTL) {
            if (v.getEntity().equals(se.getName())) {
                vTL.remove(v);
                return;
            }
        }
    }

    private void addToOutputList(SimEntity se) {
        List<Output> outTL = jaxbRoot.getOutput();
        for (Output o : outTL) {
            if (o.getEntity().equals(se.getName())) {
                return;
            }
        }
        Output op = oFactory.createOutput();
        op.setEntity(se.getName());
        outTL.add(op);
    }

    private void addToVerboseList(SimEntity se) {
        List<Verbose> vTL = jaxbRoot.getVerbose();
        for (Verbose v : vTL) {
            if (v.getEntity().equals(se.getName())) {
                return;
            }
        }
        Verbose op = oFactory.createVerbose();
        op.setEntity(se.getName());
        vTL.add(op);
    }

    @Override
    public Vector<String> getDetailedOutputEntityNames() {
        Vector<String> v = new Vector<>();
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

    @Override
    public Vector<String> getVerboseOutputEntityNames() {
        Vector<String> v = new Vector<>();
        for (Verbose ot : jaxbRoot.getVerbose()) {
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

       // To prevent java.util.ConcurrentModificationException
       List<Object> vi = new ArrayList<>();
        for (Object o : lis) {
            vi.add(buildInstantiatorFromJaxbParameter(o));
        }
        return vi;
    }

    private VInstantiator buildInstantiatorFromJaxbParameter(Object o) {
        if (o instanceof TerminalParameter) {
            return buildFreeFormFromTermParameter((TerminalParameter) o);
        }
        if (o instanceof MultiParameter) {           // used for both arrays and Constr arg lists
            MultiParameter mu = (MultiParameter) o;
            return (mu.getType().contains("[")) ? buildArrayFromMultiParameter(mu) : buildConstrFromMultiParameter(mu);
        }
        return (o instanceof FactoryParameter) ? buildFactoryInstFromFactoryParameter((FactoryParameter) o) : null;
    }

    private VInstantiator.FreeF buildFreeFormFromTermParameter(TerminalParameter tp) {
        return new VInstantiator.FreeF(tp.getType(), tp.getValue());
    }

    private VInstantiator.Array buildArrayFromMultiParameter(MultiParameter o) {
        return new VInstantiator.Array(o.getType(),
                getInstantiatorListFromJaxbParmList(o.getParameters()));
    }

    private VInstantiator.Constr buildConstrFromMultiParameter(MultiParameter o) {
        return new VInstantiator.Constr(o.getType(),
                getInstantiatorListFromJaxbParmList(o.getParameters()));
    }

    private VInstantiator.Factory buildFactoryInstFromFactoryParameter(FactoryParameter o) {
        return new VInstantiator.Factory(o.getType(),
                o.getFactory(),
                VStatics.RANDOM_VARIATE_FACTORY_DEFAULT_METHOD,
                getInstantiatorListFromJaxbParmList(o.getParameters()));
    }

    // We know we will get a List<Object> one way or the other
    @SuppressWarnings("unchecked")
    private List<Object> getJaxbParamList(VInstantiator vi) {
        Object o = buildParam(vi);
        if (o instanceof List<?>) {
            return (List<Object>) o;
        }

        Vector<Object> v = new Vector<>();
        v.add(o);
        return v;
    }

    private Object buildParam(Object vi) {
        if (vi instanceof VInstantiator.FreeF) {
            return buildParamFromFreeF((VInstantiator.FreeF) vi);
        } //TerminalParm
        if (vi instanceof VInstantiator.Constr) {
            return buildParamFromConstr((VInstantiator.Constr) vi);
        } // List of Parms
        if (vi instanceof VInstantiator.Factory) {
            return buildParamFromFactory((VInstantiator.Factory) vi);
        } // FactoryParam
        if (vi instanceof VInstantiator.Array) {
            VInstantiator.Array via = (VInstantiator.Array) vi;

            if (VGlobals.instance().isArray(via.getType()))
                return buildParamFromArray(via);
            else if (via.getType().contains("..."))
                return buildParamFromVarargs(via);
        } // MultiParam

        //assert false : AssemblyModelImpl.buildJaxbParameter() received null;
        return null;
    }

    private TerminalParameter buildParamFromFreeF(VInstantiator.FreeF viff) {
        TerminalParameter tp = oFactory.createTerminalParameter();

        tp.setType(viff.getType());
        tp.setValue(viff.getValue());
        tp.setName(viff.getName());
        return tp;
    }

    private MultiParameter buildParamFromConstr(VInstantiator.Constr vicon) {
        MultiParameter mp = oFactory.createMultiParameter();

        mp.setType(vicon.getType());
        for (Object vi : vicon.getArgs()) {
            mp.getParameters().add(buildParam(vi));
        }
        return mp;
    }

    private FactoryParameter buildParamFromFactory(VInstantiator.Factory vifact) {
        FactoryParameter fp = oFactory.createFactoryParameter();

        fp.setType(vifact.getType());
        fp.setFactory(vifact.getFactoryClass());

        for (Object vi : vifact.getParams()) {
            fp.getParameters().add(buildParam(vi));
        }
        return fp;
    }

    private MultiParameter buildParamFromArray(VInstantiator.Array viarr) {
        MultiParameter mp = oFactory.createMultiParameter();

        mp.setType(viarr.getType());
        for (Object vi : viarr.getInstantiators()) {
            mp.getParameters().add(buildParam(vi));
        }
        return mp;
    }

    private TerminalParameter buildParamFromVarargs(VInstantiator.Array viarr) {
        return buildParamFromFreeF((VInstantiator.FreeF) viarr.getInstantiators().get(0));
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
            this.notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGEADDED, "Adapter connection added"));
        }
    }

    private void buildPCLsFromJaxb(List<PropertyChangeListener> pcLs) {
        for (PropertyChangeListener pcl : pcLs) {
            buildPclNodeFromJaxbPCL(pcl);
        }
    }

    private void buildEGsFromJaxb(List<SimEntity> simEntities, List<Output> outputList, List<Verbose> verboseList) {
        for (SimEntity se : simEntities) {
            boolean isOutput = false;
            boolean isVerbose = false;
            // This must be done in this order, because the buildEvgNode...below
            // causes AssembleModel to be reentered, and the outputList gets hit.
            for (Output o : outputList) {
                String simE = o.getEntity();
                if (simE.equals(se.getName())) {
                    isOutput = true;
                    break;
                }
            }

            // Verbose shouldn't populated since the verbose check box has been disabled
            for (Verbose v : verboseList) {
                String simE = v.getEntity();
                if (simE.equals(se.getName())) {
                    isVerbose = true;
                    break;
                }
            }
            buildEvgNodeFromJaxbSimEntity(se, isOutput, isVerbose);
        }
    }

    private PropChangeListenerNode buildPclNodeFromJaxbPCL(PropertyChangeListener pcl) {
        PropChangeListenerNode pNode = (PropChangeListenerNode) getNodeCache().get(pcl.getName());
        if (pNode != null) {
            return pNode;
        }
        pNode = new PropChangeListenerNode(pcl.getName(), pcl.getType());

        // For backwards compatibility, bug 706
        pNode.setClearStatsAfterEachRun(pcl.getMode().contains("replicationStat"));
        pNode.setGetMean(Boolean.parseBoolean(pcl.getMeanStatistics()));
        pNode.setGetCount(Boolean.parseBoolean(pcl.getCountStatistics()));
        pNode.setDescriptionString(pcl.getDescription());
        Coordinate coor = pcl.getCoordinate();
        if (coor == null) {
            pNode.setPosition(pointLess);
            pointLess = new Point2D.Double(pointLess.x + 20, pointLess.y + 20);
        } else {
            pNode.setPosition(new Point2D.Double(Double.parseDouble(coor.getX()),
                    Double.parseDouble(coor.getY())));
        }

        List<Object> lis = pcl.getParameters();
        VInstantiator vc = new VInstantiator.Constr(pcl.getType(),
                getInstantiatorListFromJaxbParmList(lis));
        pNode.setInstantiator(vc);

        pNode.opaqueModelObject = pcl;
        LogUtils.getLogger(AssemblyModelImpl.class).debug("pNode name: " + pNode.getName());

        getNodeCache().put(pNode.getName(), pNode);   // key = se

        if (!nameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML file contains duplicate event name", pNode.getName() +
                    "\nUnique name substituted.");
            manglePCLName(pNode);
        }
        notifyChanged(new ModelEvent(pNode, ModelEvent.PCLADDED, "PCL added"));
        return pNode;
    }

    private EvGraphNode buildEvgNodeFromJaxbSimEntity(SimEntity se, boolean isOutputNode, boolean isVerboseNode) {
        EvGraphNode en = (EvGraphNode) getNodeCache().get(se.getName());
        if (en != null) {
            return en;
        }
        en = new EvGraphNode(se.getName(), se.getType());

        Coordinate coor = se.getCoordinate();
        if (coor == null) {
            en.setPosition(pointLess);
            pointLess = new Point2D.Double(pointLess.x + 20, pointLess.y + 20);
        } else {
            en.setPosition(new Point2D.Double(Double.parseDouble(coor.getX()),
                    Double.parseDouble(coor.getY())));
        }

        en.setDescriptionString(se.getDescription());
        en.setOutputMarked(isOutputNode);
        en.setVerboseMarked(isVerboseNode);

        List<Object> lis = se.getParameters();

        VInstantiator vc = new VInstantiator.Constr(lis, se.getType());
        en.setInstantiator(vc);

        en.opaqueModelObject = se;

        getNodeCache().put(en.getName(), en);   // key = se

        if (!nameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML file contains duplicate event name", en.getName() +
                    "\nUnique name substituted.");
            mangleEGName(en);
        }
        notifyChanged(new ModelEvent(en, ModelEvent.EVENTGRAPHADDED, "Event added"));

        return en;
    }

    /**
     * "nullIfEmpty" Return the passed string if non-zero length, else null
     * @param s the string to check for non-zero length
     * @return the passed string if non-zero length, else null
     */
    private String nIe(String s) {
        if (s != null) {
            if (s.length() == 0) {
                s = null;
            }
        }
        return s;
    }

    public Map<String, AssemblyNode> getNodeCache() {
        return nodeCache;
    }

}
