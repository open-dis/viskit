package viskit.model;

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
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.util.FileBasedAssemblyNode;
import viskit.control.AssemblyControllerImpl;
import static viskit.model.ViskitModelInstantiator.LOG;
import viskit.mvc.MvcAbstractModel;
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
public class AssemblyModelImpl extends MvcAbstractModel implements AssemblyModel
{
    

    private static JAXBContext jaxbContext;
    private ObjectFactory objectFactory;
    private SimkitAssembly jaxbRoot;
    private File currentAssemblyModelFile;
    private String title = new String();
    private boolean modelDirty = false;
    private GraphMetadata graphMetadata;

    /** We require specific order on this Map's contents */
    private final Map<String, AssemblyNode> nodeCache;
    private final String schemaLocation = XMLValidationTool.ASSEMBLY_SCHEMA;
    private Point2D.Double pointLess;
    private final AssemblyControllerImpl assemblyController;

    public AssemblyModelImpl(AssemblyControllerImpl newAssemblyController)
    {
        pointLess = new Point2D.Double(30, 60);
        assemblyController = newAssemblyController;
        graphMetadata = new GraphMetadata(this);
        nodeCache = new LinkedHashMap<>();
    }

    public void initialize() 
    {
        try {
            if (jaxbContext == null) // avoid JAXBException (perhaps due to concurrency)
                jaxbContext = JAXBContext.newInstance(SimkitAssemblyXML2Java.ASSEMBLY_BINDINGS);
            objectFactory = new ObjectFactory();
            jaxbRoot = objectFactory.createSimkitAssembly(); // to start with empty graph
        } 
        catch (JAXBException e) {
            assemblyController.messageUser(JOptionPane.ERROR_MESSAGE,
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
    public GraphMetadata getMetadata() {
        return graphMetadata;
    }

    @Override
    public void changeMetadata(GraphMetadata newGraphMetadata) {
        this.graphMetadata = newGraphMetadata;
        setDirty(true);
    }

    @Override
    public boolean newAssemblyModel(File newAssemblyModelFile)
    {
        getNodeCache().clear();
        pointLess = new Point2D.Double(30, 60);
        this.notifyChanged(new ModelEvent(this, ModelEvent.NEW_ASSEMBLY_MODEL, "New empty assembly model"));

        if (newAssemblyModelFile == null)
        {
            jaxbRoot = objectFactory.createSimkitAssembly(); // to start with empty graph
        } 
        else 
        {
            try {
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                // Check for inadvertant opening of an Event Graph, tough to do, yet possible (bugfix 1248)
                try {
                    jaxbRoot = (SimkitAssembly) unmarshaller.unmarshal(newAssemblyModelFile);
                } 
                catch (ClassCastException cce) {
                    // If we get here, they've tried to load an event graph.
                    assemblyController.messageUser(JOptionPane.ERROR_MESSAGE,
                            "This is an Event Graph",
                            "Use the Event Graph Editor to" +
                            "\n" + "work with this file: " + newAssemblyModelFile.getName()
                            );
                    return false;
                }

                GraphMetadata myGraphMetadata = new GraphMetadata(this);
                myGraphMetadata.version       = jaxbRoot.getVersion();
                myGraphMetadata.name          = jaxbRoot.getName();
                myGraphMetadata.packageName   = jaxbRoot.getPackage();

                Schedule schedule = jaxbRoot.getSchedule();
                if (schedule != null) {
                    String stopTime = schedule.getStopTime();
                    if (stopTime != null && stopTime.trim().length() > 0) {
                        myGraphMetadata.stopTime = stopTime.trim();
                    }
                    myGraphMetadata.verbose = schedule.getVerbose().equalsIgnoreCase("true");
                }

                changeMetadata(myGraphMetadata);
                buildEventGraphsFromJaxb(jaxbRoot.getSimEntity(), jaxbRoot.getOutput(), jaxbRoot.getVerbose());
                buildPCLsFromJaxb(jaxbRoot.getPropertyChangeListener());
                buildPCConnectionsFromJaxb(jaxbRoot.getPropertyChangeListenerConnection());
                buildSimEvConnectionsFromJaxb(jaxbRoot.getSimEventListenerConnection());
                buildAdapterConnectionsFromJaxb(jaxbRoot.getAdapter());
            } 
            catch (JAXBException e) 
            {
                assemblyController.messageUser(JOptionPane.ERROR_MESSAGE,
                        "XML I/O Error",
                        "Exception on JAXB unmarshalling of" +
                            "\n" + newAssemblyModelFile.getName() +
                            "\n" + e.getMessage() +
                            "\nin AssemblyModel.newModel(File)"
                            );

                return false;
            }
        }
        currentAssemblyModelFile = newAssemblyModelFile;
        if (currentAssemblyModelFile != null)
        {
             title = currentAssemblyModelFile.getName();
             if (title.contains(".xml"))
                 title = title.substring(0,title.indexOf(".xml"));
        }
        else title = "UnsavedAssemblyFile";
        
        setDirty(false);
        return true;
    }

    @Override
    public void saveModel(File savableAssemblyModel) {
        if (savableAssemblyModel == null) {
            savableAssemblyModel = currentAssemblyModelFile;
        }

        // Do the marshalling into a temporary file so as to avoid possible
        // deletion of existing file on a marshal error.

        File tempAssemblyMarshallFile;
        FileWriter fileWriter = null;
        try {
            tempAssemblyMarshallFile = TempFileManager.createTempFile("tempAssemblyMarshallFile", ".xml");
            LOG.info("tempAssemblyMarshallFile=\n   " + tempAssemblyMarshallFile.getAbsolutePath());
        } 
        catch (IOException e) {
            assemblyController.messageUser(JOptionPane.ERROR_MESSAGE,
                    "I/O Error",
                    "Exception creating temporary file, AssemblyModel.saveModel():" +
                    "\n" + e.getMessage()
                    );
            LOG.error("Exception creating temporary file, AssemblyModel.saveModel():" +
                    "\n" + e.getMessage());
            return;
        }

        try {
            fileWriter = new FileWriter(tempAssemblyMarshallFile);
            Marshaller assemblyMarshaller = jaxbContext.createMarshaller();
            assemblyMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            assemblyMarshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, schemaLocation);

            jaxbRoot.setName(nIe(graphMetadata.name));
            jaxbRoot.setVersion(nIe(graphMetadata.version));
            jaxbRoot.setPackage(nIe(graphMetadata.packageName));

            if (jaxbRoot.getSchedule() == null) {
                jaxbRoot.setSchedule(objectFactory.createSchedule());
            }
            if (!graphMetadata.stopTime.equals("")) {
                jaxbRoot.getSchedule().setStopTime(graphMetadata.stopTime);
            } else {
                jaxbRoot.getSchedule().setStopTime("100.0");
            }

            // Schedule needs this value to properly sync with Enable Analyst Reports
            jaxbRoot.getSchedule().setSaveReplicationData(String.valueOf(ViskitGlobals.instance().getSimulationRunPanel().analystReportCB.isSelected()));
            jaxbRoot.getSchedule().setVerbose("" + graphMetadata.verbose);

            assemblyMarshaller.marshal(jaxbRoot, fileWriter);

            // OK, made it through the marshal, overwrite the "real" file
            Files.copy(tempAssemblyMarshallFile.toPath(), savableAssemblyModel.toPath(), StandardCopyOption.REPLACE_EXISTING);

            modelDirty = false;
            currentAssemblyModelFile = savableAssemblyModel;
        }
        catch (JAXBException e) {
            assemblyController.messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML I/O Error",
                    "Exception on JAXB marshalling" +
                    "\n" + savableAssemblyModel +
                    "\n" + e.getMessage() +
                    "\n(check for blank data fields)"
                    );
        }
        catch (IOException ex) {
            assemblyController.messageUser(JOptionPane.ERROR_MESSAGE,
                    "File I/O Error",
                    "Exception on writing " + savableAssemblyModel.getName() +
                    "\n" + ex.getMessage());
        } finally {
            try {
                if (fileWriter != null)
                    fileWriter.close();
            } catch (IOException ioe) {}
        }
    }

    @Override
    public File getCurrentFile() {
        return currentAssemblyModelFile;
    }
    
    public String getName() {
        if  (currentAssemblyModelFile != null)
             return currentAssemblyModelFile.getName().substring(0, currentAssemblyModelFile.getName().indexOf(".xml"));
        else return ""; // unexpected, a name should be present
    }

    @Override
    public void externalClassesChanged(Vector<String> v) {

    }
    public static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String _fourHexDigits(int i) {
        char[] ca = new char[4];
        int idx;
        for (int j = 3; j >= 0; j--) {
            idx = i & 0xF;
            i >>= 4;
            ca[j] = HEX_DIGITS[idx];
        }
        return new String(ca);
    }
    static Random mangleRandom = new Random();

    public static String mangleName(String name) {
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

    private void mangleName(ViskitElement node) {
        do {
            node.setName(mangleName(node.getName()));
        } while (!nameCheck());
    }

    private boolean nameCheck() {
        Set<String> hs = new HashSet<>(10);
        for (AssemblyNode n : getNodeCache().values()) {
            if (!hs.add(n.getName())) {
                assemblyController.messageUser(JOptionPane.INFORMATION_MESSAGE,
                        "XML file contains duplicate event name", n.getName() +
                        "\nUnique name substituted.");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean nameExists(String name) {
        for (AssemblyNode n : getNodeCache().values()) {
            if (n.getName().equals(name))
                return true;
        }
        return false;
    }

    @Override
    public void newEventGraphFromXML(String widgetName, FileBasedAssemblyNode node, Point2D p) {
        newEventGraph(widgetName, node.loadedClass, p);
    }

    @Override
    public void newEventGraph(String widgetName, String className, Point2D p) {
        EventGraphNode node = new EventGraphNode(widgetName, className);
        if (p == null) {
            node.setPosition(pointLess);
        } else {
            node.setPosition(p);
        }

        SimEntity jaxbEventGraph = objectFactory.createSimEntity();

        jaxbEventGraph.setName(nIe(widgetName));
        jaxbEventGraph.setType(className);
        node.opaqueModelObject = jaxbEventGraph;

        ViskitModelInstantiator vc = new ViskitModelInstantiator.Constr(jaxbEventGraph.getType(), null);  // null means undefined
        node.setInstantiator(vc);

        if (!nameCheck())
            mangleName(node);

        getNodeCache().put(node.getName(), node);   // key = ev

        jaxbRoot.getSimEntity().add(jaxbEventGraph);

        modelDirty = true;
        notifyChanged(new ModelEvent(node, ModelEvent.EVENT_GRAPH_ADDED, "Event graph added to assembly"));
    }

    @Override
    public void redoEventGraph(EventGraphNode node) {
        SimEntity jaxbEventGraph = objectFactory.createSimEntity();

        jaxbEventGraph.setName(node.getName());
        node.opaqueModelObject = jaxbEventGraph;
        jaxbEventGraph.setType(node.getType());

        getNodeCache().put(node.getName(), node);   // key = ev

        jaxbRoot.getSimEntity().add(jaxbEventGraph);

        modelDirty = true;
        notifyChanged(new ModelEvent(node, ModelEvent.REDO_EVENT_GRAPH, "Event Graph redone"));
    }

    @Override
    public void deleteEventGraphNode(EventGraphNode eventGraphNode) {
        SimEntity jaxbEvent = (SimEntity) eventGraphNode.opaqueModelObject;
        getNodeCache().remove(jaxbEvent.getName());
        jaxbRoot.getSimEntity().remove(jaxbEvent);

        modelDirty = true;

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(eventGraphNode, ModelEvent.EVENT_GRAPH_DELETED, "Event Graph deleted"));
        else
            notifyChanged(new ModelEvent(eventGraphNode, ModelEvent.UNDO_EVENT_GRAPH, "Event Graph undone"));
    }

    @Override
    public void newPropertyChangeListenerFromXML(String widgetName, FileBasedAssemblyNode node, Point2D point2D) {
        newPropertyChangeListener(widgetName, node.loadedClass, point2D);
    }

    @Override
    public void newPropertyChangeListener(String widgetName, String className, Point2D p) {
        PropertyChangeListenerNode propertyChangeListenerNode = new PropertyChangeListenerNode(widgetName, className);
        if (p == null)
            propertyChangeListenerNode.setPosition(pointLess);
        else
            propertyChangeListenerNode.setPosition(p);

        PropertyChangeListener pcl = objectFactory.createPropertyChangeListener();

        pcl.setName(nIe(widgetName));
        pcl.setType(className);
        propertyChangeListenerNode.opaqueModelObject = pcl;

        List<Object> parametersList = pcl.getParameters();

        ViskitModelInstantiator viskitModelInstantiatorConstr = new ViskitModelInstantiator.Constr(pcl.getType(), parametersList);
        propertyChangeListenerNode.setInstantiator(viskitModelInstantiatorConstr);

        if (!nameCheck())
            mangleName(propertyChangeListenerNode);

        getNodeCache().put(propertyChangeListenerNode.getName(), propertyChangeListenerNode);   // key = ev

        jaxbRoot.getPropertyChangeListener().add(pcl);

        modelDirty = true;
        notifyChanged(new ModelEvent(propertyChangeListenerNode, ModelEvent.PCL_ADDED, "Property Change Node added to assembly"));
    }

    @Override
    public void redoPropertyChangeListener(PropertyChangeListenerNode propertyChangeListenerNode) {

        PropertyChangeListener jaxbPCL = objectFactory.createPropertyChangeListener();

        jaxbPCL.setName(propertyChangeListenerNode.getName());
        jaxbPCL.setType(propertyChangeListenerNode.getType());

        propertyChangeListenerNode.opaqueModelObject = jaxbPCL;

        jaxbRoot.getPropertyChangeListener().add(jaxbPCL);

        modelDirty = true;
        notifyChanged(new ModelEvent(propertyChangeListenerNode, ModelEvent.REDO_PCL, "Property Change Node redone"));
    }

    @Override
    public void deletePropertyChangeListener(PropertyChangeListenerNode pclNode) {
        PropertyChangeListener jaxbPcNode = (PropertyChangeListener) pclNode.opaqueModelObject;
        getNodeCache().remove(pclNode.getName());
        jaxbRoot.getPropertyChangeListener().remove(jaxbPcNode);

        modelDirty = true;

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(pclNode, ModelEvent.PCL_DELETED, "Property Change Listener deleted"));
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

        Adapter jaxbAdapter = objectFactory.createAdapter();

        ae.opaqueModelObject = jaxbAdapter;
        jaxbAdapter.setTo(target.getName());
        jaxbAdapter.setFrom(src.getName());

        jaxbAdapter.setName(adName);

        jaxbRoot.getAdapter().add(jaxbAdapter);

        modelDirty = true;

        this.notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTER_EDGE_ADDED, "Adapter edge added"));
        return ae;
    }

    @Override
    public void redoAdapterEdge(AdapterEdge ae) {
        AssemblyNode src, target;

        src = (AssemblyNode) ae.getFrom();
        target = (AssemblyNode) ae.getTo();

        Adapter jaxbAdapter = objectFactory.createAdapter();
        ae.opaqueModelObject = jaxbAdapter;
        jaxbAdapter.setTo(target.getName());
        jaxbAdapter.setFrom(src.getName());
        jaxbAdapter.setName(ae.getName());

        jaxbRoot.getAdapter().add(jaxbAdapter);

        modelDirty = true;

        this.notifyChanged(new ModelEvent(ae, ModelEvent.REDO_ADAPTER_EDGE, "Adapter edge added"));
    }

    @Override
    public PropertyChangeEdge newPropChangeEdge(AssemblyNode src, AssemblyNode target) {
        PropertyChangeEdge pce = new PropertyChangeEdge();
        pce.setFrom(src);
        pce.setTo(target);

        src.getConnections().add(pce);
        target.getConnections().add(pce);

        PropertyChangeListenerConnection pclc = objectFactory.createPropertyChangeListenerConnection();

        pce.opaqueModelObject = pclc;

        pclc.setListener(target.getName());
        pclc.setSource(src.getName());

        jaxbRoot.getPropertyChangeListenerConnection().add(pclc);
        modelDirty = true;

        this.notifyChanged(new ModelEvent(pce, ModelEvent.PCL_EDGE_ADDED, "PCL edge added"));
        return pce;
    }

    @Override
    public void redoPropertyChangeEdge(PropertyChangeEdge propertyChangeEdge) 
    {
        AssemblyNode source, target;

        source = (AssemblyNode) propertyChangeEdge.getFrom();
        target = (AssemblyNode) propertyChangeEdge.getTo();

        PropertyChangeListenerConnection pclc = objectFactory.createPropertyChangeListenerConnection();
        propertyChangeEdge.opaqueModelObject = pclc;
        pclc.setListener(target.getName());
        pclc.setSource(source.getName());

        jaxbRoot.getPropertyChangeListenerConnection().add(pclc);
        modelDirty = true;

        this.notifyChanged(new ModelEvent(propertyChangeEdge, ModelEvent.REDO_PCL_EDGE, "PCL edge added"));
    }

    @Override
    public void newSimEventListenerEdge(AssemblyNode sourceAssemblyNode, AssemblyNode targetAssemblyNode) {
        SimEventListenerEdge simEventListenerEdge = new SimEventListenerEdge();
        simEventListenerEdge.setFrom(sourceAssemblyNode);
        simEventListenerEdge.setTo(targetAssemblyNode);

        sourceAssemblyNode.getConnections().add(simEventListenerEdge);
        targetAssemblyNode.getConnections().add(simEventListenerEdge);

        SimEventListenerConnection simEventListenerConnection = objectFactory.createSimEventListenerConnection();

        simEventListenerEdge.opaqueModelObject = simEventListenerConnection;

        simEventListenerConnection.setListener(targetAssemblyNode.getName());
        simEventListenerConnection.setSource(sourceAssemblyNode.getName());

        jaxbRoot.getSimEventListenerConnection().add(simEventListenerConnection);

        modelDirty = true;
        notifyChanged(new ModelEvent(simEventListenerEdge, ModelEvent.SIM_EVENT_LISTENER_EDGE_ADDED, "SimEventListener edge added"));
    }

    @Override
    public void redoSimEvLisEdge(SimEventListenerEdge sele) {
        AssemblyNode src, target;

        src = (AssemblyNode) sele.getFrom();
        target = (AssemblyNode) sele.getTo();

        SimEventListenerConnection selc = objectFactory.createSimEventListenerConnection();
        sele.opaqueModelObject = selc;
        selc.setListener(target.getName());
        selc.setSource(src.getName());

        jaxbRoot.getSimEventListenerConnection().add(selc);

        modelDirty = true;
        notifyChanged(new ModelEvent(sele, ModelEvent.REDO_SIM_EVENT_LISTENER_EDGE, "SimEventListener Edge redone"));
    }

    @Override
    public void deletePropChangeEdge(PropertyChangeEdge pce) {
        PropertyChangeListenerConnection pclc = (PropertyChangeListenerConnection) pce.opaqueModelObject;

        jaxbRoot.getPropertyChangeListenerConnection().remove(pclc);

        modelDirty = true;

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(pce, ModelEvent.PCL_EDGE_DELETED, "PCL edge deleted"));
        else
            notifyChanged(new ModelEvent(pce, ModelEvent.UNDO_PCL_EDGE, "PCL edge undone"));
    }

    @Override
    public void deleteSimEventListenerEdge(SimEventListenerEdge sele) {
        SimEventListenerConnection sel_c = (SimEventListenerConnection) sele.opaqueModelObject;

        jaxbRoot.getSimEventListenerConnection().remove(sel_c);

        modelDirty = true;

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(sele, ModelEvent.SIM_EVENT_LISTENER_EDGE_DELETED, "SimEventListener edge deleted"));
        else
            notifyChanged(new ModelEvent(sele, ModelEvent.UNDO_SIM_EVENT_LISTENER_EDGE, "SimEventListener edge undone"));
    }

    @Override
    public void deleteAdapterEdge(AdapterEdge ae) {
        Adapter j_adp = (Adapter) ae.opaqueModelObject;
        jaxbRoot.getAdapter().remove(j_adp);

        modelDirty = true;

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTER_EDGE_DELETED, "Adapter edge deleted"));
        else
            notifyChanged(new ModelEvent(ae, ModelEvent.UNDO_ADAPTER_EDGE, "Adapter edge undone"));
    }

    @Override
    public void changePclEdge(PropertyChangeEdge pclEdge) {
        PropertyChangeListenerConnection pclc = (PropertyChangeListenerConnection) pclEdge.opaqueModelObject;
        pclc.setProperty(pclEdge.getProperty());
        pclc.setDescription(pclEdge.getDescriptionString());

        modelDirty = true;
        notifyChanged(new ModelEvent(pclEdge, ModelEvent.PCL_EDGE_CHANGED, "PCL edge changed"));
    }

    @Override
    public void changeAdapterEdge(AdapterEdge ae) {
        EventGraphNode src = (EventGraphNode) ae.getFrom();
        EventGraphNode targ = (EventGraphNode) ae.getTo();

        Adapter jaxbAE = (Adapter) ae.opaqueModelObject;

        jaxbAE.setFrom(src.getName());
        jaxbAE.setTo(targ.getName());

        jaxbAE.setEventHeard(ae.getSourceEvent());
        jaxbAE.setEventSent(ae.getTargetEvent());

        jaxbAE.setName(ae.getName());
        jaxbAE.setDescription(ae.getDescriptionString());

        modelDirty = true;
        notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTER_EDGE_CHANGED, "Adapter edge changed"));
    }

    @Override
    public void changeSimEvEdge(SimEventListenerEdge seEdge) {
        EventGraphNode src = (EventGraphNode) seEdge.getFrom();
        EventGraphNode targ = (EventGraphNode) seEdge.getTo();
        SimEventListenerConnection selc = (SimEventListenerConnection) seEdge.opaqueModelObject;

        selc.setListener(targ.getName());
        selc.setSource(src.getName());
        selc.setDescription(seEdge.getDescriptionString());

        modelDirty = true;
        notifyChanged(new ModelEvent(seEdge, ModelEvent.SIM_EVENT_LISTENER_EDGE_CHANGED, "SimEventListenerener edge changed"));
    }

    @Override
    public boolean changePclNode(PropertyChangeListenerNode pclNode) {
        boolean retcode = true;
        if (!nameCheck()) {
            mangleName(pclNode);
            retcode = false;
        }
        PropertyChangeListener jaxBPcl = (PropertyChangeListener) pclNode.opaqueModelObject;
        jaxBPcl.setName(pclNode.getName());
        jaxBPcl.setType(pclNode.getType());
        jaxBPcl.setDescription(pclNode.getDescriptionString());

        // Modes should be singular.  All new Assemblies will be with singular mode
        if (pclNode.isSampleStats()) {
            if (pclNode.isClearStatsAfterEachRun())
                jaxBPcl.setMode("replicationStat");
            else
                jaxBPcl.setMode("designPointStat");
        }

        String statistics = pclNode.isGetCount() ? "true" : "false";
        jaxBPcl.setCountStatistics(statistics);

        statistics = pclNode.isGetMean() ? "true" : "false";
        jaxBPcl.setMeanStatistics(statistics);

        double x = pclNode.getPosition().getX();
        double y = pclNode.getPosition().getY();
        Coordinate coor = objectFactory.createCoordinate();
        coor.setX("" + x);
        coor.setY("" + y);
        pclNode.getPosition().setLocation(x, y);
        jaxBPcl.setCoordinate(coor);

        List<Object> lis = jaxBPcl.getParameters();
        lis.clear();

        ViskitModelInstantiator inst = pclNode.getInstantiator();

        // this will be a list of one...a MultiParameter....get its list, but
        // throw away the object itself.  This is because the
        // PropertyChangeListener object serves as "its own" MultiParameter.
        List<Object> jlistt = getJaxbParamList(inst);

        if (jlistt.size() != 1)
            throw new RuntimeException("Design error in AssemblyModel");

        MultiParameter mp = (MultiParameter) jlistt.get(0);

        for (Object o : mp.getParameters()) 
            lis.add(o);

        modelDirty = true;
        this.notifyChanged(new ModelEvent(pclNode, ModelEvent.PCL_CHANGED, "Property Change Listener node changed"));
        return retcode;
    }

    @Override
    public boolean changeEvGraphNode(EventGraphNode evNode) {
        boolean retcode = true;
        if (!nameCheck()) {
            mangleName(evNode);
            retcode = false;
        }
        SimEntity jaxbSE = (SimEntity) evNode.opaqueModelObject;

        jaxbSE.setName(evNode.getName());
        jaxbSE.setType(evNode.getType());
        jaxbSE.setDescription(evNode.getDescriptionString());

        double x = evNode.getPosition().getX();
        double y = evNode.getPosition().getY();
        Coordinate coor = objectFactory.createCoordinate();
        coor.setX("" + x);
        coor.setY("" + y);
        evNode.getPosition().setLocation(x, y);
        jaxbSE.setCoordinate(coor);

        List<Object> lis = jaxbSE.getParameters();
        lis.clear();

        ViskitModelInstantiator inst = evNode.getInstantiator();

        // this will be a list of one...a MultiParameter....get its list, but
        // throw away the object itself.  This is because the SimEntity object
        // serves as "its own" MultiParameter.
        List<Object> jlistt = getJaxbParamList(inst);

        if (jlistt.size() != 1)
            throw new RuntimeException("Design error in AssemblyModel");

        MultiParameter mp = (MultiParameter) jlistt.get(0);

        for (Object o : mp.getParameters())
            lis.add(o);

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
        this.notifyChanged(new ModelEvent(evNode, ModelEvent.EVENT_GRAPH_CHANGED, "Event changed"));
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
        Output op = objectFactory.createOutput();
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
        Verbose op = objectFactory.createVerbose();
        op.setEntity(se.getName());
        vTL.add(op);
    }

    @Override
    public Vector<String> getDetailedOutputEntityNames() {
        Vector<String> v = new Vector<>();
        Object entity;
        for (Output ot : jaxbRoot.getOutput()) {
            entity = ot.getEntity();
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
        Object entity;
        for (Verbose ot : jaxbRoot.getVerbose()) {
            entity = ot.getEntity();
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

    private ViskitModelInstantiator buildInstantiatorFromJaxbParameter(Object o) {
        if (o instanceof TerminalParameter) {
            return buildFreeFormFromTermParameter((TerminalParameter) o);
        }
        if (o instanceof MultiParameter) {           // used for both arrays and Constr arg lists
            MultiParameter mu = (MultiParameter) o;
            return (mu.getType().contains("[")) ? buildArrayFromMultiParameter(mu) : buildConstrFromMultiParameter(mu);
        }
        return (o instanceof FactoryParameter) ? buildFactoryInstFromFactoryParameter((FactoryParameter) o) : null;
    }

    private ViskitModelInstantiator.FreeF buildFreeFormFromTermParameter(TerminalParameter tp) {
        return new ViskitModelInstantiator.FreeF(tp.getType(), tp.getValue());
    }

    private ViskitModelInstantiator.Array buildArrayFromMultiParameter(MultiParameter o) {
        return new ViskitModelInstantiator.Array(o.getType(),
                getInstantiatorListFromJaxbParmList(o.getParameters()));
    }

    private ViskitModelInstantiator.Constr buildConstrFromMultiParameter(MultiParameter o) {
        return new ViskitModelInstantiator.Constr(o.getType(),
                getInstantiatorListFromJaxbParmList(o.getParameters()));
    }

    private ViskitModelInstantiator.Factory buildFactoryInstFromFactoryParameter(FactoryParameter o) {
        return new ViskitModelInstantiator.Factory(o.getType(),
                o.getFactory(),
                ViskitStatics.RANDOM_VARIATE_FACTORY_DEFAULT_METHOD,
                getInstantiatorListFromJaxbParmList(o.getParameters()));
    }

    // We know we will get a List<Object> one way or the other
    @SuppressWarnings("unchecked")
    private List<Object> getJaxbParamList(ViskitModelInstantiator vi) {
        Object o = buildParam(vi);
        if (o instanceof List<?>) {
            return (List<Object>) o;
        }

        Vector<Object> v = new Vector<>();
        v.add(o);
        return v;
    }

    private Object buildParam(Object vi) {
        if (vi instanceof ViskitModelInstantiator.FreeF) {
            return buildParamFromFreeF((ViskitModelInstantiator.FreeF) vi);
        } //TerminalParm
        if (vi instanceof ViskitModelInstantiator.Constr) {
            return buildParamFromConstr((ViskitModelInstantiator.Constr) vi);
        } // List of Parms
        if (vi instanceof ViskitModelInstantiator.Factory) {
            return buildParamFromFactory((ViskitModelInstantiator.Factory) vi);
        } // FactoryParam
        if (vi instanceof ViskitModelInstantiator.Array) {
            ViskitModelInstantiator.Array via = (ViskitModelInstantiator.Array) vi;

            if (ViskitGlobals.instance().isArray(via.getType()))
                return buildParamFromArray(via);
            else if (via.getType().contains("..."))
                return buildParamFromVarargs(via);
        } // MultiParam

        //assert false : AssemblyModelImpl.buildJaxbParameter() received null;
        return null;
    }

    private TerminalParameter buildParamFromFreeF(ViskitModelInstantiator.FreeF viff) {
        TerminalParameter tp = objectFactory.createTerminalParameter();

        tp.setType(viff.getType());
        tp.setValue(viff.getValue());
        tp.setName(viff.getName());
        return tp;
    }

    private MultiParameter buildParamFromConstr(ViskitModelInstantiator.Constr vicon) {
        MultiParameter mp = objectFactory.createMultiParameter();

        mp.setType(vicon.getType());
        for (Object vi : vicon.getArgs()) {
            mp.getParameters().add(buildParam(vi));
        }
        return mp;
    }

    private FactoryParameter buildParamFromFactory(ViskitModelInstantiator.Factory vifact) {
        FactoryParameter fp = objectFactory.createFactoryParameter();

        fp.setType(vifact.getType());
        fp.setFactory(vifact.getFactoryClass());

        for (Object vi : vifact.getParams()) {
            fp.getParameters().add(buildParam(vi));
        }
        return fp;
    }

    private MultiParameter buildParamFromArray(ViskitModelInstantiator.Array viarr) {
        MultiParameter mp = objectFactory.createMultiParameter();

        mp.setType(viarr.getType());
        for (Object vi : viarr.getInstantiators()) {
            mp.getParameters().add(buildParam(vi));
        }
        return mp;
    }

    private TerminalParameter buildParamFromVarargs(ViskitModelInstantiator.Array viarr) {
        return buildParamFromFreeF((ViskitModelInstantiator.FreeF) viarr.getInstantiators().get(0));
    }

    private void buildPCConnectionsFromJaxb(List<PropertyChangeListenerConnection> pcconnsList) {
        PropertyChangeEdge pce;
        AssemblyNode toNode, frNode;
        for (PropertyChangeListenerConnection pclc : pcconnsList) {
            pce = new PropertyChangeEdge();
            pce.setProperty(pclc.getProperty());
            pce.setDescriptionString(pclc.getDescription());
            toNode = getNodeCache().get(pclc.getListener());
            frNode = getNodeCache().get(pclc.getSource());
            pce.setTo(toNode);
            pce.setFrom(frNode);
            pce.opaqueModelObject = pclc;

            toNode.getConnections().add(pce);
            frNode.getConnections().add(pce);

            this.notifyChanged(new ModelEvent(pce, ModelEvent.PCL_EDGE_ADDED, "PCL edge added"));
        }
    }

    private void buildSimEvConnectionsFromJaxb(List<SimEventListenerConnection> simevconnsList) {
        SimEventListenerEdge sele;
        AssemblyNode toNode, frNode;
        for (SimEventListenerConnection selc : simevconnsList) {
            sele = new SimEventListenerEdge();
            toNode = getNodeCache().get(selc.getListener());
            frNode = getNodeCache().get(selc.getSource());
            sele.setTo(toNode);
            sele.setFrom(frNode);
            sele.opaqueModelObject = selc;
            sele.setDescriptionString(selc.getDescription());

            toNode.getConnections().add(sele);
            frNode.getConnections().add(sele);
            this.notifyChanged(new ModelEvent(sele, ModelEvent.SIM_EVENT_LISTENER_EDGE_ADDED, "Sim event listener connection added"));
        }
    }

    private void buildAdapterConnectionsFromJaxb(List<Adapter> adaptersList) {
        AdapterEdge ae;
        AssemblyNode toNode, frNode;
        String event;
        for (Adapter jaxbAdapter : adaptersList) {
            ae = new AdapterEdge();
            toNode = getNodeCache().get(jaxbAdapter.getTo());
            frNode = getNodeCache().get(jaxbAdapter.getFrom());
            ae.setTo(toNode);
            ae.setFrom(frNode);

            // Handle XML names w/ underscores (XML IDREF issue)
            event = jaxbAdapter.getEventHeard();
            if (event.contains("_"))
                event = event.substring(0, event.indexOf("_"));
            ae.setSourceEvent(event);

            event = jaxbAdapter.getEventSent();
            if (event.contains("_"))
                event = event.substring(0, event.indexOf("_"));
            ae.setTargetEvent(event);

            ae.setName(jaxbAdapter.getName());
            ae.setDescriptionString(jaxbAdapter.getDescription());
            ae.opaqueModelObject = jaxbAdapter;

            toNode.getConnections().add(ae);
            frNode.getConnections().add(ae);
            this.notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTER_EDGE_ADDED, "Adapter connection added"));
        }
    }

    private void buildPCLsFromJaxb(List<PropertyChangeListener> pcLs) {
        for (PropertyChangeListener pcl : pcLs) {
            buildPclNodeFromJaxbPCL(pcl);
        }
    }

    private void buildEventGraphsFromJaxb(List<SimEntity> simEntities, List<Output> outputList, List<Verbose> verboseList) {
        for (SimEntity se : simEntities) {
            boolean isOutput = false;
            boolean isVerbose = false;
            // This must be done in this order, because the buildEvgNode...below
            // causes AssembleModel to be reentered, and the outputList gets hit.
            String simE;
            for (Output o : outputList) {
                simE = o.getEntity();
                if (simE.equals(se.getName())) {
                    isOutput = true;
                    break;
                }
            }

            // Verbose shouldn't be populated since the verbose check box has been disabled
            for (Verbose v : verboseList) {
                simE = v.getEntity();
                if (simE.equals(se.getName())) {
                    isVerbose = true;
                    break;
                }
            }
            buildEvgNodeFromJaxbSimEntity(se, isOutput, isVerbose);
        }
    }

    private PropertyChangeListenerNode buildPclNodeFromJaxbPCL(PropertyChangeListener pcl) {
        PropertyChangeListenerNode pNode = (PropertyChangeListenerNode) getNodeCache().get(pcl.getName());
        if (pNode != null) {
            return pNode;
        }
        pNode = new PropertyChangeListenerNode(pcl.getName(), pcl.getType());

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
        ViskitModelInstantiator vc = new ViskitModelInstantiator.Constr(pcl.getType(),
                getInstantiatorListFromJaxbParmList(lis));
        pNode.setInstantiator(vc);

        pNode.opaqueModelObject = pcl;
        LOG.debug("pNode name: " + pNode.getName());

        getNodeCache().put(pNode.getName(), pNode);   // key = se

        if (!nameCheck()) {
            mangleName(pNode);
        }
        notifyChanged(new ModelEvent(pNode, ModelEvent.PCL_ADDED, "PCL added"));
        return pNode;
    }

    private EventGraphNode buildEvgNodeFromJaxbSimEntity(SimEntity se, boolean isOutputNode, boolean isVerboseNode) {
        EventGraphNode en = (EventGraphNode) getNodeCache().get(se.getName());
        if (en != null) {
            return en;
        }
        en = new EventGraphNode(se.getName(), se.getType());

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

        ViskitModelInstantiator vc = new ViskitModelInstantiator.Constr(lis, se.getType());
        en.setInstantiator(vc);

        en.opaqueModelObject = se;

        getNodeCache().put(en.getName(), en);   // key = se

        if (!nameCheck()) {
            mangleName(en);
        }
        notifyChanged(new ModelEvent(en, ModelEvent.EVENT_GRAPH_ADDED, "Event added"));

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

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

} // end class AssemblyModelImpl
