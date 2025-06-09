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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.ViskitUserConfiguration;
import viskit.util.FileBasedAssemblyNode;
import viskit.control.AssemblyControllerImpl;
import viskit.mvc.MvcAbstractModel;
import viskit.util.XMLValidationTool;
import viskit.xsd.bindings.assembly.*;
import viskit.xsd.translator.assembly.SimkitAssemblyXML2Java;
import static viskit.view.SimulationRunPanel.SIMULATION_STOP_TIME_DEFAULT;

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
    static final Logger LOG = LogManager.getLogger();
    
    private static JAXBContext jaxbContext;
    private viskit.xsd.bindings.assembly.ObjectFactory jaxbAssemblyObjectFactory;
    private SimkitAssembly simkitAssemblyJaxbRoot;
    private String         currentAssemblyModelName = new String();
    private File           currentAssemblyModelFile;
    private String         title = new String();
    private boolean        modelModified = false;
    private GraphMetadata  graphMetadata;

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
            jaxbAssemblyObjectFactory = new viskit.xsd.bindings.assembly.ObjectFactory();
            simkitAssemblyJaxbRoot = jaxbAssemblyObjectFactory.createSimkitAssembly(); // to start with empty graph
        } 
        catch (JAXBException e) {
            ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML Error",
                    "Exception on JAXBContext instantiation" +
                    "\n" + e.getMessage()
                    );
        }
    }

    @Override
    public SimkitAssembly getJaxbRoot() {
        return simkitAssemblyJaxbRoot;
    }

    @Override
    public GraphMetadata getMetadata() {
        return graphMetadata;
    }

    @Override
    public void changeMetadata(GraphMetadata newGraphMetadata)
    {
        this.graphMetadata = newGraphMetadata;
        setModelModified(true);
    }

    public void loadMetadata(GraphMetadata newGraphMetadata)
    {
        this.graphMetadata = newGraphMetadata;
        // we are loading and not changing, and so modified bit is not set
    }

    @Override
    public boolean newAssemblyModel(File newAssemblyModelFile)
    {
        getNodeCache().clear();
        pointLess = new Point2D.Double(30, 60);
        this.notifyChanged(new ModelEvent(this, ModelEvent.NEW_ASSEMBLY_MODEL, "New empty assembly model"));

        if (newAssemblyModelFile == null)
        {
            simkitAssemblyJaxbRoot = jaxbAssemblyObjectFactory.createSimkitAssembly(); // to start with empty graph
        } 
        else 
        {
            try {
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                // Check for inadvertant opening of an Event Graph, tough to do, yet possible (bugfix 1248)
                try {
                    simkitAssemblyJaxbRoot = (SimkitAssembly) unmarshaller.unmarshal(newAssemblyModelFile);
                } 
                catch (ClassCastException cce) {
                    // If we get here, they've tried to load an event graph.
                    ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE,
                            "This is an Event Graph",
                            "Use the Event Graph Editor to" +
                            "\n" + "work with this file: " + newAssemblyModelFile.getName()
                            );
                    return false;
                }

                GraphMetadata myGraphMetadata         = new GraphMetadata(this);
                myGraphMetadata.name                  = ViskitStatics.emptyIfNull(simkitAssemblyJaxbRoot.getName());
                myGraphMetadata.author                = ViskitStatics.emptyIfNull(simkitAssemblyJaxbRoot.getAuthor());
                if (myGraphMetadata.author.isBlank())
                    myGraphMetadata.author = ViskitUserConfiguration.instance().getAnalystName();
                myGraphMetadata.description           = ViskitStatics.emptyIfNull(simkitAssemblyJaxbRoot.getDescription());
                myGraphMetadata.version               = ViskitStatics.emptyIfNull(simkitAssemblyJaxbRoot.getVersion());
                myGraphMetadata.packageName           = ViskitStatics.emptyIfNull(simkitAssemblyJaxbRoot.getPackage());
                myGraphMetadata.extendsPackageName    = ViskitStatics.emptyIfNull(simkitAssemblyJaxbRoot.getExtend());
                myGraphMetadata.implementsPackageName = ViskitStatics.emptyIfNull(simkitAssemblyJaxbRoot.getImplement());

                Schedule schedule = simkitAssemblyJaxbRoot.getSchedule();
                if (schedule != null) {
                    String stopTime = schedule.getStopTime();
                    if (stopTime != null && stopTime.trim().length() > 0) {
                        myGraphMetadata.stopTime = stopTime.trim();
                    }
                    myGraphMetadata.verbose = schedule.getVerbose().equalsIgnoreCase("true");
                }

                loadMetadata(myGraphMetadata); // no change to modified status
                buildEventGraphsFromJaxb(simkitAssemblyJaxbRoot.getSimEntity(), simkitAssemblyJaxbRoot.getOutput(), simkitAssemblyJaxbRoot.getVerbose());
                buildPropertyChangeListenersFromJaxb(simkitAssemblyJaxbRoot.getPropertyChangeListener());
                buildPropertyChangeListenerConnectionsFromJaxb(simkitAssemblyJaxbRoot.getPropertyChangeListenerConnection());
                buildSimEventConnectionsFromJaxb(simkitAssemblyJaxbRoot.getSimEventListenerConnection());
                buildAdapterConnectionsFromJaxb(simkitAssemblyJaxbRoot.getAdapter());
            
                // TODO check that corresponding SimEntity name exists, otherwise erroneous (difficulty implementing xs:IDREF checking)
            } 
            catch (JAXBException e) 
            {
                ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE,
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
        
        setModelModified(false); // important to clear prior values from setup and intitialization
        return true;
    }

    @Override
    public void saveModel(File savableAssemblyModelFile) 
    {
        if (savableAssemblyModelFile == null) {
            savableAssemblyModelFile = currentAssemblyModelFile;
        }

        // Do the marshalling into a temporary file so as to avoid possible
        // deletion of existing file on a marshal error.

        File tempAssemblyMarshallFile;
        FileWriter fileWriter = null;
        try {
            tempAssemblyMarshallFile = TempFileManager.createTempFile("tempAssemblyMarshallFile", ".xml");
            LOG.debug("tempAssemblyMarshallFile=\n      " + tempAssemblyMarshallFile.getAbsolutePath());
        } 
        catch (IOException e) {
            ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE,
                    "I/O Error",
                    "Exception creating temporary file, AssemblyModel.saveModel():" +
                    "\n" + e.getMessage()
                    );
            LOG.error("Exception creating temporary file, AssemblyModel.saveModel():" +
                    "\n" + e.getMessage());
            return;
        }

        try 
        {
            fileWriter = new FileWriter(tempAssemblyMarshallFile);
            Marshaller assemblyMarshaller = jaxbContext.createMarshaller();
            assemblyMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            assemblyMarshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, schemaLocation);

            simkitAssemblyJaxbRoot.setName       (ViskitStatics.nullIfEmpty(graphMetadata.name));
            simkitAssemblyJaxbRoot.setVersion    (ViskitStatics.nullIfEmpty(graphMetadata.version));
            simkitAssemblyJaxbRoot.setAuthor     (ViskitStatics.nullIfEmpty(graphMetadata.author));
            simkitAssemblyJaxbRoot.setDescription(ViskitStatics.nullIfEmpty(graphMetadata.description));
            simkitAssemblyJaxbRoot.setPackage    (ViskitStatics.nullIfEmpty(graphMetadata.packageName));
            simkitAssemblyJaxbRoot.setExtend     (ViskitStatics.nullIfEmpty(graphMetadata.extendsPackageName));
            simkitAssemblyJaxbRoot.setImplement  (ViskitStatics.nullIfEmpty(graphMetadata.implementsPackageName));

            // Saving an Assembly is much easier, all data is kept in JAXB throughout so no conversions are needed
            // sort SimEntity, PropertyChangeListener, SimEventListenerConnection, PropertyChangeListenerConnection, Adapter, Output
            // TODO needed? sort Schedule, Experiment
            
            // sort the List of SimEntity elements by name for consistent file saving, which includes FactoryParameter TerminalParameter Coordinate
            Collections.sort((List<SimEntity>)simkitAssemblyJaxbRoot.getSimEntity(), 
                             new Comparator<SimEntity>() {
                @Override
                public int compare(SimEntity lhs, SimEntity rhs) {
                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    return lhs.getName().compareTo(rhs.getName());
                }
            });
            
            // sort the List of PropertyChangeListener elements by name for consistent file saving, which includes TerminalParameter Coordinate
            Collections.sort((List<PropertyChangeListener>)simkitAssemblyJaxbRoot.getPropertyChangeListener(), 
                             new Comparator<PropertyChangeListener>() {
                @Override
                public int compare(PropertyChangeListener lhs, PropertyChangeListener rhs) {
                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    return lhs.getName().compareTo(rhs.getName());
                }
            });
            
            // sort the List of SimEventListenerConnection elements by source for consistent file saving
            Collections.sort((List<SimEventListenerConnection>)simkitAssemblyJaxbRoot.getSimEventListenerConnection(), 
                             new Comparator<SimEventListenerConnection>() {
                @Override
                public int compare(SimEventListenerConnection lhs, SimEventListenerConnection rhs) {
                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    return lhs.getSource().compareTo(rhs.getSource());
                }
            });
            
            // sort the List of SimEventListenerConnection elements by source for consistent file saving
            Collections.sort((List<PropertyChangeListenerConnection>)simkitAssemblyJaxbRoot.getPropertyChangeListenerConnection(), 
                             new Comparator<PropertyChangeListenerConnection>() {
                @Override
                public int compare(PropertyChangeListenerConnection lhs, PropertyChangeListenerConnection rhs) {
                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    return lhs.getSource().compareTo(rhs.getSource());
                }
            });
            
            // sort the List of SimEventListenerConnection elements by "from" for consistent file saving
            Collections.sort((List<Adapter>)simkitAssemblyJaxbRoot.getAdapter(), 
                             new Comparator<Adapter>() {
                @Override
                public int compare(Adapter lhs, Adapter rhs) {
                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    return lhs.getFrom().compareTo(rhs.getFrom());
                }
            });
            
            // sort the List of SimEventListenerConnection elements by "from" for consistent file saving
            Collections.sort((List<Output>)simkitAssemblyJaxbRoot.getOutput(), 
                             new Comparator<Output>() {
                @Override
                public int compare(Output lhs, Output rhs) {
                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    return lhs.getEntity().compareTo(rhs.getEntity());
                }
            });
            
            // additional specialty values to save
            if (simkitAssemblyJaxbRoot.getSchedule() == null) {
                simkitAssemblyJaxbRoot.setSchedule(jaxbAssemblyObjectFactory.createSchedule());
            }
            if (!graphMetadata.stopTime.equals("")) 
            {
                simkitAssemblyJaxbRoot.getSchedule().setStopTime(graphMetadata.stopTime);
            } 
            else {
                simkitAssemblyJaxbRoot.getSchedule().setStopTime(SIMULATION_STOP_TIME_DEFAULT); // default
            }

            // Schedule needs this value to properly sync with Enable Analyst Reports
            simkitAssemblyJaxbRoot.getSchedule().setSaveReplicationData(String.valueOf(ViskitGlobals.instance().getSimulationRunPanel().analystReportCB.isSelected()));
            simkitAssemblyJaxbRoot.getSchedule().setVerbose(String.valueOf(graphMetadata.verbose));

            assemblyMarshaller.marshal(simkitAssemblyJaxbRoot, fileWriter);

            // OK, made it through the marshal, overwrite the "real" file
            Files.copy(tempAssemblyMarshallFile.toPath(), savableAssemblyModelFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            String modelModifiedStatus;
            if  (isModelModified())
                 modelModifiedStatus =   "saved modified";
            else modelModifiedStatus = "resaved unmodified";
            long bytesCopied = savableAssemblyModelFile.length();
            LOG.info("{} Assembly file, {} bytes\n      {}" , modelModifiedStatus, bytesCopied, savableAssemblyModelFile);

            setModelModified(false);
            currentAssemblyModelFile = savableAssemblyModelFile;
        }
        catch (JAXBException e) {
            ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML I/O Error",
                    "Exception on JAXB marshalling" +
                    "\n" + savableAssemblyModelFile +
                    "\n" + e.getMessage() +
                    "\n(check for blank data fields)"
                    );
        }
        catch (IOException ex) {
            ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE,
                    "File I/O Error",
                    "Exception on writing " + savableAssemblyModelFile.getName() +
                    "\n" + ex.getMessage());
        } 
        finally {
            try {
                if (fileWriter != null)
                    fileWriter.close();
            } 
            catch (IOException ioe) {}
        }
    }

    @Override
    public File getCurrentFile() {
        return currentAssemblyModelFile;
    }
    
    public String getName() 
    {
        if  (      (currentAssemblyModelFile != null) && (currentAssemblyModelFile.getName().contains(".")))
             return currentAssemblyModelFile.getName().substring(0, currentAssemblyModelFile.getName().lastIndexOf(".")); // followed by file suffix xml
        else if    (currentAssemblyModelFile != null)
             return currentAssemblyModelFile.getName();
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
                ViskitGlobals.instance().messageUser(JOptionPane.INFORMATION_MESSAGE,
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
    public void newEventGraph(String widgetName, String className, Point2D p) 
    {
        EventGraphNode node = new EventGraphNode(widgetName, className);
        if (p == null) {
            node.setPosition(pointLess);
        } else {
            node.setPosition(p);
        }

        SimEntity jaxbEventGraph = jaxbAssemblyObjectFactory.createSimEntity();

        jaxbEventGraph.setName(ViskitStatics.nullIfEmpty(widgetName));
        jaxbEventGraph.setType(className);
        node.opaqueModelObject = jaxbEventGraph;

        ViskitModelInstantiator vc = new ViskitModelInstantiator.Constr(jaxbEventGraph.getType(), null);  // null means undefined
        node.setInstantiator(vc);

        if (!nameCheck())
            mangleName(node);

        getNodeCache().put(node.getName(), node);   // key = ev

        simkitAssemblyJaxbRoot.getSimEntity().add(jaxbEventGraph);

        setModelModified(true);
        notifyChanged(new ModelEvent(node, ModelEvent.EVENT_GRAPH_ADDED, "Event graph added to assembly"));
    }

    @Override
    public void redoEventGraph(EventGraphNode node) 
    {
        SimEntity jaxbEventGraph = jaxbAssemblyObjectFactory.createSimEntity();

        jaxbEventGraph.setName(node.getName());
        node.opaqueModelObject = jaxbEventGraph;
        jaxbEventGraph.setType(node.getType());

        getNodeCache().put(node.getName(), node);   // key = ev

        simkitAssemblyJaxbRoot.getSimEntity().add(jaxbEventGraph);

        setModelModified(true);
        notifyChanged(new ModelEvent(node, ModelEvent.REDO_EVENT_GRAPH, "Event Graph redone"));
    }

    @Override
    public void deleteEventGraphNode(EventGraphNode eventGraphNode) 
    {
        SimEntity jaxbEvent = (SimEntity) eventGraphNode.opaqueModelObject;
        getNodeCache().remove(jaxbEvent.getName());
        simkitAssemblyJaxbRoot.getSimEntity().remove(jaxbEvent);

        setModelModified(true);

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(eventGraphNode, ModelEvent.EVENT_GRAPH_DELETED, "Event Graph deleted"));
        else
            notifyChanged(new ModelEvent(eventGraphNode, ModelEvent.UNDO_EVENT_GRAPH, "Event Graph undone"));
    }

    @Override
    public void newPropertyChangeListenerFromXML(String widgetName, FileBasedAssemblyNode node, Point2D point2D) 
    {
        newPropertyChangeListener(widgetName, node.loadedClass, point2D);
    }

    @Override
    public void newPropertyChangeListener(String widgetName, String className, Point2D p)
    {
        PropertyChangeListenerNode propertyChangeListenerNode = new PropertyChangeListenerNode(widgetName, className);
        if (p == null)
            propertyChangeListenerNode.setPosition(pointLess);
        else
            propertyChangeListenerNode.setPosition(p);

        PropertyChangeListener pcl = jaxbAssemblyObjectFactory.createPropertyChangeListener();

        pcl.setName(ViskitStatics.nullIfEmpty(widgetName));
        pcl.setType(className);
        propertyChangeListenerNode.opaqueModelObject = pcl;

        List<Object> parametersList = pcl.getParameters();

        ViskitModelInstantiator viskitModelInstantiatorConstr = new ViskitModelInstantiator.Constr(pcl.getType(), parametersList);
        propertyChangeListenerNode.setInstantiator(viskitModelInstantiatorConstr);

        if (!nameCheck())
            mangleName(propertyChangeListenerNode);

        getNodeCache().put(propertyChangeListenerNode.getName(), propertyChangeListenerNode);   // key = ev

        simkitAssemblyJaxbRoot.getPropertyChangeListener().add(pcl);

        setModelModified(true);
        notifyChanged(new ModelEvent(propertyChangeListenerNode, ModelEvent.PCL_ADDED, "Property Change Node added to assembly"));
    }

    @Override
    public void redoPropertyChangeListener(PropertyChangeListenerNode propertyChangeListenerNode) 
    {
        PropertyChangeListener jaxbPCL = jaxbAssemblyObjectFactory.createPropertyChangeListener();

        jaxbPCL.setName(propertyChangeListenerNode.getName());
        jaxbPCL.setType(propertyChangeListenerNode.getType());

        propertyChangeListenerNode.opaqueModelObject = jaxbPCL;

        simkitAssemblyJaxbRoot.getPropertyChangeListener().add(jaxbPCL);

        setModelModified(true);
        notifyChanged(new ModelEvent(propertyChangeListenerNode, ModelEvent.REDO_PCL, "Property Change Node redone"));
    }

    @Override
    public void deletePropertyChangeListener(PropertyChangeListenerNode pclNode) 
    {
        PropertyChangeListener jaxbPcNode = (PropertyChangeListener) pclNode.opaqueModelObject;
        getNodeCache().remove(pclNode.getName());
        simkitAssemblyJaxbRoot.getPropertyChangeListener().remove(jaxbPcNode);

        setModelModified(true);

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(pclNode, ModelEvent.PCL_DELETED, "Property Change Listener deleted"));
        else
            notifyChanged(new ModelEvent(pclNode, ModelEvent.UNDO_PCL, "Property Change Listener undone"));
    }

    @Override
    public AdapterEdge newAdapterEdge(String adName, AssemblyNode src, AssemblyNode target) 
    {
        AdapterEdge ae = new AdapterEdge();
        ae.setFrom(src);
        ae.setTo(target);
        ae.setName(adName);

        src.getConnections().add(ae);
        target.getConnections().add(ae);

        Adapter jaxbAdapter = jaxbAssemblyObjectFactory.createAdapter();

        ae.opaqueModelObject = jaxbAdapter;
        jaxbAdapter.setTo(target.getName());
        jaxbAdapter.setFrom(src.getName());

        jaxbAdapter.setName(adName);

        simkitAssemblyJaxbRoot.getAdapter().add(jaxbAdapter);

        setModelModified(true);

        this.notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTER_EDGE_ADDED, "Adapter edge added"));
        return ae;
    }

    @Override
    public void redoAdapterEdge(AdapterEdge ae) 
    {
        AssemblyNode src, target;

        src = (AssemblyNode) ae.getFrom();
        target = (AssemblyNode) ae.getTo();

        Adapter jaxbAdapter = jaxbAssemblyObjectFactory.createAdapter();
        ae.opaqueModelObject = jaxbAdapter;
        jaxbAdapter.setTo(target.getName());
        jaxbAdapter.setFrom(src.getName());
        jaxbAdapter.setName(ae.getName());

        simkitAssemblyJaxbRoot.getAdapter().add(jaxbAdapter);

        setModelModified(true);

        this.notifyChanged(new ModelEvent(ae, ModelEvent.REDO_ADAPTER_EDGE, "Adapter edge added"));
    }

    @Override
    public PropertyChangeEdge newPropChangeEdge(AssemblyNode src, AssemblyNode target) 
    {
        PropertyChangeEdge pce = new PropertyChangeEdge();
        pce.setFrom(src);
        pce.setTo(target);

        src.getConnections().add(pce);
        target.getConnections().add(pce);

        PropertyChangeListenerConnection pclc = jaxbAssemblyObjectFactory.createPropertyChangeListenerConnection();

        pce.opaqueModelObject = pclc;

        pclc.setListener(target.getName());
        pclc.setSource(src.getName());

        simkitAssemblyJaxbRoot.getPropertyChangeListenerConnection().add(pclc);
        setModelModified(true);

        this.notifyChanged(new ModelEvent(pce, ModelEvent.PCL_EDGE_ADDED, "PCL edge added"));
        return pce;
    }

    @Override
    public void redoPropertyChangeEdge(PropertyChangeEdge propertyChangeEdge) 
    {
        AssemblyNode source, target;

        source = (AssemblyNode) propertyChangeEdge.getFrom();
        target = (AssemblyNode) propertyChangeEdge.getTo();

        PropertyChangeListenerConnection pclc = jaxbAssemblyObjectFactory.createPropertyChangeListenerConnection();
        propertyChangeEdge.opaqueModelObject = pclc;
        pclc.setListener(target.getName());
        pclc.setSource(source.getName());

        simkitAssemblyJaxbRoot.getPropertyChangeListenerConnection().add(pclc);
        setModelModified(true);

        this.notifyChanged(new ModelEvent(propertyChangeEdge, ModelEvent.REDO_PCL_EDGE, "PCL edge added"));
    }

    @Override
    public void newSimEventListenerEdge(AssemblyNode sourceAssemblyNode, AssemblyNode targetAssemblyNode)
    {
        SimEventListenerEdge simEventListenerEdge = new SimEventListenerEdge();
        simEventListenerEdge.setFrom(sourceAssemblyNode);
        simEventListenerEdge.setTo(targetAssemblyNode);

        sourceAssemblyNode.getConnections().add(simEventListenerEdge);
        targetAssemblyNode.getConnections().add(simEventListenerEdge);

        SimEventListenerConnection simEventListenerConnection = jaxbAssemblyObjectFactory.createSimEventListenerConnection();

        simEventListenerEdge.opaqueModelObject = simEventListenerConnection;

        simEventListenerConnection.setListener(targetAssemblyNode.getName());
        simEventListenerConnection.setSource(sourceAssemblyNode.getName());

        simkitAssemblyJaxbRoot.getSimEventListenerConnection().add(simEventListenerConnection);

        setModelModified(true);
        notifyChanged(new ModelEvent(simEventListenerEdge, ModelEvent.SIM_EVENT_LISTENER_EDGE_ADDED, "SimEventListener edge added"));
    }

    @Override
    public void redoSimEventListenerEdge(SimEventListenerEdge simEventListenerEdge) 
    {
        AssemblyNode sourceAssemblyNode, targetAssemblyNode;

        sourceAssemblyNode = (AssemblyNode) simEventListenerEdge.getFrom();
        targetAssemblyNode = (AssemblyNode) simEventListenerEdge.getTo();

        SimEventListenerConnection simEventListenerConnection = jaxbAssemblyObjectFactory.createSimEventListenerConnection();
        simEventListenerEdge.opaqueModelObject = simEventListenerConnection;
        simEventListenerConnection.setListener(targetAssemblyNode.getName());
        simEventListenerConnection.setSource(sourceAssemblyNode.getName());

        simkitAssemblyJaxbRoot.getSimEventListenerConnection().add(simEventListenerConnection);

        setModelModified(true);
        notifyChanged(new ModelEvent(simEventListenerEdge, ModelEvent.REDO_SIM_EVENT_LISTENER_EDGE, "SimEventListener Edge redone"));
    }

    @Override
    public void deleteProperyChangeEdge(PropertyChangeEdge removablePropertyChangeEdge) 
    {
        PropertyChangeListenerConnection propertyChangeListenerConnection = (PropertyChangeListenerConnection) removablePropertyChangeEdge.opaqueModelObject;

        simkitAssemblyJaxbRoot.getPropertyChangeListenerConnection().remove(propertyChangeListenerConnection);

        setModelModified(true);

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(removablePropertyChangeEdge, ModelEvent.PCL_EDGE_DELETED, "PCL edge deleted"));
        else
            notifyChanged(new ModelEvent(removablePropertyChangeEdge, ModelEvent.UNDO_PCL_EDGE, "PCL edge undone"));
    }

    @Override
    public void deleteSimEventListenerEdge(SimEventListenerEdge simEventListenerEdge) 
    {
        SimEventListenerConnection simEventListenerConnection = (SimEventListenerConnection) simEventListenerEdge.opaqueModelObject;

        simkitAssemblyJaxbRoot.getSimEventListenerConnection().remove(simEventListenerConnection);

        setModelModified(true);

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(simEventListenerEdge, ModelEvent.SIM_EVENT_LISTENER_EDGE_DELETED, "SimEventListener edge deleted"));
        else
            notifyChanged(new ModelEvent(simEventListenerEdge, ModelEvent.UNDO_SIM_EVENT_LISTENER_EDGE, "SimEventListener edge undone"));
    }

    @Override
    public void deleteAdapterEdge(AdapterEdge adapterEdge) 
    {
        Adapter adapterToDelete = (Adapter) adapterEdge.opaqueModelObject;
        simkitAssemblyJaxbRoot.getAdapter().remove(adapterToDelete);

        setModelModified(true);

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(adapterEdge, ModelEvent.ADAPTER_EDGE_DELETED, "Adapter edge deleted"));
        else
            notifyChanged(new ModelEvent(adapterEdge, ModelEvent.UNDO_ADAPTER_EDGE, "Adapter edge undone"));
    }

    @Override
    public void changePclEdge(PropertyChangeEdge pclEdge) 
    {
        PropertyChangeListenerConnection propertyChangeListenerConnection = (PropertyChangeListenerConnection) pclEdge.opaqueModelObject;
        propertyChangeListenerConnection.setProperty(pclEdge.getProperty());
        propertyChangeListenerConnection.setDescription(pclEdge.getDescription());

        setModelModified(true);
        notifyChanged(new ModelEvent(pclEdge, ModelEvent.PCL_EDGE_CHANGED, "PCL edge changed"));
    }

    @Override
    public void changeAdapterEdge(AdapterEdge adapterEdge) 
    {
        EventGraphNode sourceEventGraphNode = (EventGraphNode) adapterEdge.getFrom();
        EventGraphNode targetEventGraphNode = (EventGraphNode) adapterEdge.getTo();

        Adapter jaxbAdapter = (Adapter) adapterEdge.opaqueModelObject;

        jaxbAdapter.setFrom(sourceEventGraphNode.getName());
        jaxbAdapter.setTo  (targetEventGraphNode.getName());

        jaxbAdapter.setEventHeard(adapterEdge.getSourceEvent());
        jaxbAdapter.setEventSent (adapterEdge.getTargetEvent());

        jaxbAdapter.setName(adapterEdge.getName());
        jaxbAdapter.setDescription(adapterEdge.getDescription());

        setModelModified(true);
        notifyChanged(new ModelEvent(adapterEdge, ModelEvent.ADAPTER_EDGE_CHANGED, "Adapter edge changed"));
    }

    @Override
    public void changeSimEventListenerEdge(SimEventListenerEdge newSimEventListenerEdge) 
    {
        EventGraphNode sourceEventGraphNode = (EventGraphNode) newSimEventListenerEdge.getFrom();
        EventGraphNode targetEventGraphNode = (EventGraphNode) newSimEventListenerEdge.getTo();
        SimEventListenerConnection simEventListenerConnection = (SimEventListenerConnection) newSimEventListenerEdge.opaqueModelObject;

        simEventListenerConnection.setListener(targetEventGraphNode.getName());
        simEventListenerConnection.setSource  (sourceEventGraphNode.getName());
        simEventListenerConnection.setDescription(newSimEventListenerEdge.getDescription());

        setModelModified(true);
        notifyChanged(new ModelEvent(newSimEventListenerEdge, ModelEvent.SIM_EVENT_LISTENER_EDGE_CHANGED, "SimEventListenerener edge changed"));
    }

    @Override
    public boolean changePclNode(PropertyChangeListenerNode propertyChangeListenerNode)
    {
        boolean returnValue = true;
        if (!nameCheck()) 
        {
            mangleName(propertyChangeListenerNode);
            returnValue = false;
        }
        PropertyChangeListener jaxbPropertyChangeListener = (PropertyChangeListener) propertyChangeListenerNode.opaqueModelObject;
        jaxbPropertyChangeListener.setName(propertyChangeListenerNode.getName());
        jaxbPropertyChangeListener.setType(propertyChangeListenerNode.getType());
        jaxbPropertyChangeListener.setDescription(propertyChangeListenerNode.getDescription());

        // Modes should be singular.  All new Assemblies will be with singular mode
        if (propertyChangeListenerNode.isSampleStatistics()) {
            if (propertyChangeListenerNode.isClearStatisticsAfterEachRun())
                jaxbPropertyChangeListener.setMode("replicationStat");
            else
                jaxbPropertyChangeListener.setMode("designPointStat");
        }
        
        String isMeanStatistics = propertyChangeListenerNode.isStatisticTypeCount() ? "true" : "false";
        jaxbPropertyChangeListener.setCountStatistics(isMeanStatistics);

        isMeanStatistics = propertyChangeListenerNode.isStatisticTypeMean() ? "true" : "false";
        jaxbPropertyChangeListener.setMeanStatistics(isMeanStatistics);

        double x = propertyChangeListenerNode.getPosition().getX();
        double y = propertyChangeListenerNode.getPosition().getY();
        Coordinate coordinate = jaxbAssemblyObjectFactory.createCoordinate();
        coordinate.setX(String.valueOf(x));
        coordinate.setY(String.valueOf(y));
        propertyChangeListenerNode.getPosition().setLocation(x, y);
        jaxbPropertyChangeListener.setCoordinate(coordinate);

        List<Object> objectList = jaxbPropertyChangeListener.getParameters();
        objectList.clear();

        ViskitModelInstantiator modelInstantiator = propertyChangeListenerNode.getInstantiator();

        // this will be a list of one...a MultiParameter....get its list, but
        // throw away the object itself.  This is because the
        // PropertyChangeListener object serves as "its own" MultiParameter.
        List<Object> jlistt = getJaxbParameterList(modelInstantiator);

        if (jlistt.size() != 1)
            throw new RuntimeException("Design error in AssemblyModel");

        MultiParameter mp = (MultiParameter) jlistt.get(0);

        for (Object o : mp.getParameters()) 
            objectList.add(o);

        setModelModified(true);
        this.notifyChanged(new ModelEvent(propertyChangeListenerNode, ModelEvent.PCL_CHANGED, "Property Change Listener node changed"));
        return returnValue;
    }

    @Override
    public boolean changeEventGraphNode(EventGraphNode eventGraphNode) 
    {
        boolean returnValue = true;
        if (!nameCheck()) {
            mangleName(eventGraphNode);
            returnValue = false;
        }
        SimEntity jaxbSimEntity = (SimEntity) eventGraphNode.opaqueModelObject;

        jaxbSimEntity.setName(eventGraphNode.getName());
        jaxbSimEntity.setType(eventGraphNode.getType());
        jaxbSimEntity.setDescription(eventGraphNode.getDescription());

        double x = eventGraphNode.getPosition().getX();
        double y = eventGraphNode.getPosition().getY();
        Coordinate coordinate = jaxbAssemblyObjectFactory.createCoordinate();
        coordinate.setX(String.valueOf(x));
        coordinate.setY(String.valueOf(y));
        eventGraphNode.getPosition().setLocation(x, y);
        jaxbSimEntity.setCoordinate(coordinate);

        List<Object> parameterList = jaxbSimEntity.getParameters();
        parameterList.clear();

        ViskitModelInstantiator instantiator = eventGraphNode.getInstantiator();

        // this will be a list of one...a MultiParameter....get its list, but
        // throw away the object itself.  This is because the SimEntity object
        // serves as "its own" MultiParameter.
        List<Object> jaxbParameterList = getJaxbParameterList(instantiator);

        if (jaxbParameterList.size() != 1)
            throw new RuntimeException("Design error in AssemblyModel");

        MultiParameter multiParameter = (MultiParameter) jaxbParameterList.get(0);

        for (Object o : multiParameter.getParameters())
            parameterList.add(o);

        if (eventGraphNode.isOutputMarked()) {
            addToOutputList(jaxbSimEntity);
        } else {
            removeFromOutputList(jaxbSimEntity);
        }

        if (eventGraphNode.isVerboseMarked()) {
            addToVerboseList(jaxbSimEntity);
        } else {
            removeFromVerboseList(jaxbSimEntity);
        }

        setModelModified(true);
        this.notifyChanged(new ModelEvent(eventGraphNode, ModelEvent.EVENT_GRAPH_CHANGED, "Event changed"));
        return returnValue;
    }

    private void removeFromOutputList(SimEntity simEntity) 
    {
        List<Output> outputElementList = simkitAssemblyJaxbRoot.getOutput();
        for (Output nextOutputElement : outputElementList) 
        {
            if (nextOutputElement.getEntity().equals(simEntity.getName()))
            {
                outputElementList.remove(nextOutputElement);
                return;
            }
        }
    }

    private void removeFromVerboseList(SimEntity simEntity) 
    {
        List<Verbose> verboseElementList = simkitAssemblyJaxbRoot.getVerbose();
        for (Verbose nextVerboseElement : verboseElementList) {
            if (nextVerboseElement.getEntity().equals(simEntity.getName())) {
                verboseElementList.remove(nextVerboseElement);
                return;
            }
        }
    }

    private void addToOutputList(SimEntity simEntity) 
    {
        List<Output> outputElementList = simkitAssemblyJaxbRoot.getOutput();
        for (Output nextOutputElement : outputElementList) 
        {
            if (nextOutputElement.getEntity().equals(simEntity.getName())) {
                return;
            }
        }
        // not found, so add it
        Output outputElement = jaxbAssemblyObjectFactory.createOutput();
        outputElement.setEntity(simEntity.getName());
        outputElementList.add(outputElement);
    }

    private void addToVerboseList(SimEntity simEntity) 
    {
        List<Verbose> verboseElementList = simkitAssemblyJaxbRoot.getVerbose();
        for (Verbose nextVerboseElement : verboseElementList) {
            if (nextVerboseElement.getEntity().equals(simEntity.getName())) {
                return;
            }
        }
        // not found, so add it
        Verbose verboseElement = jaxbAssemblyObjectFactory.createVerbose();
        verboseElement.setEntity(simEntity.getName());
        verboseElementList.add(verboseElement);
    }

    @Override
    public Vector<String> getDetailedOutputEntityNames()
    {
        Vector<String> namesVector = new Vector<>();
        Object entity;
        for (Output nextOutputElement : simkitAssemblyJaxbRoot.getOutput()) 
        {
            entity = nextOutputElement.getEntity();
            if (entity instanceof SimEntity) 
            {
                namesVector.add(((SimEntity) entity).getName());
            } 
            else if (entity instanceof PropertyChangeListener) 
            {
                namesVector.add(((PropertyChangeListener) entity).getName());
            }
        }
        return namesVector;
    }

    @Override
    public Vector<String> getVerboseOutputEntityNames() 
    {
        Vector<String> namesVector = new Vector<>();
        Object entity;
        for (Verbose nextVerboseElement : simkitAssemblyJaxbRoot.getVerbose()) 
        {
            entity = nextVerboseElement.getEntity();
            if (entity instanceof SimEntity) 
            {
                namesVector.add(((SimEntity) entity).getName());
            } 
            else if (entity instanceof PropertyChangeListener) 
            {
                namesVector.add(((PropertyChangeListener) entity).getName());
            }
        }
        return namesVector;
    }

   private List<Object> getInstantiatorListFromJaxbParameterList(List<Object> parameterList) 
   {
       // To prevent java.util.ConcurrentModificationException
       List<Object> instantiatorList = new ArrayList<>();
        for (Object nextParameterElement : parameterList) 
        {
            instantiatorList.add(buildInstantiatorFromJaxbParameter(nextParameterElement));
        }
        return instantiatorList;
    }

    private ViskitModelInstantiator buildInstantiatorFromJaxbParameter(Object o) 
    {
        if (o instanceof TerminalParameter) {
            return buildFreeFormFromTerminalParameter((TerminalParameter) o);
        }
        if (o instanceof MultiParameter) {           // used for both arrays and Constr arg lists
            MultiParameter mu = (MultiParameter) o;
            return (mu.getType().contains("[")) ? buildArrayFromMultiParameter(mu) : buildConstrFromMultiParameter(mu);
        }
        return (o instanceof FactoryParameter) ? buildFactoryInstFromFactoryParameter((FactoryParameter) o) : null;
    }

    private ViskitModelInstantiator.FreeF buildFreeFormFromTerminalParameter(TerminalParameter tp) {
        return new ViskitModelInstantiator.FreeF(tp.getType(), tp.getValue());
    }

    private ViskitModelInstantiator.Array buildArrayFromMultiParameter(MultiParameter o) {
        return new ViskitModelInstantiator.Array(o.getType(),
                getInstantiatorListFromJaxbParameterList(o.getParameters()));
    }

    private ViskitModelInstantiator.Constr buildConstrFromMultiParameter(MultiParameter o) {
        return new ViskitModelInstantiator.Constr(o.getType(),
                getInstantiatorListFromJaxbParameterList(o.getParameters()));
    }

    private ViskitModelInstantiator.Factory buildFactoryInstFromFactoryParameter(FactoryParameter o) {
        return new ViskitModelInstantiator.Factory(o.getType(),
                o.getFactory(),
                ViskitStatics.RANDOM_VARIATE_FACTORY_DEFAULT_METHOD,
                getInstantiatorListFromJaxbParameterList(o.getParameters()));
    }

    // We know we will get a List<Object> one way or the other
    @SuppressWarnings("unchecked")
    private List<Object> getJaxbParameterList(ViskitModelInstantiator modelInstantiator) 
    {
        Object o = buildParameters(modelInstantiator);
        if (o instanceof List<?>) {
            return (List<Object>) o;
        }

        Vector<Object> v = new Vector<>();
        v.add(o);
        return v;
    }

    private Object buildParameters(Object vi) 
    {
        if (vi instanceof ViskitModelInstantiator.FreeF) {
            return buildTerminalParameterFromFreeF((ViskitModelInstantiator.FreeF) vi);
        } //TerminalParm
        if (vi instanceof ViskitModelInstantiator.Constr) {
            return buildMultiParameterFromConstr((ViskitModelInstantiator.Constr) vi);
        } // List of Parms
        if (vi instanceof ViskitModelInstantiator.Factory) {
            return buildParameterFromFactory((ViskitModelInstantiator.Factory) vi);
        } // FactoryParam
        if (vi instanceof ViskitModelInstantiator.Array) {
            ViskitModelInstantiator.Array via = (ViskitModelInstantiator.Array) vi;

            if (ViskitGlobals.instance().isArray(via.getType()))
                return buildParametersFromArray(via);
            else if (via.getType().contains("..."))
                return buildParamFromVarargs(via);
        } // MultiParam

        //assert false : AssemblyModelImpl.buildJaxbParameter() received null;
        return null;
    }

    private TerminalParameter buildTerminalParameterFromFreeF(ViskitModelInstantiator.FreeF viff) {
        TerminalParameter tp = jaxbAssemblyObjectFactory.createTerminalParameter();

        tp.setType(viff.getType());
        tp.setValue(viff.getValue());
        tp.setName(viff.getName());
        return tp;
    }

    private MultiParameter buildMultiParameterFromConstr(ViskitModelInstantiator.Constr vicon) {
        MultiParameter mp = jaxbAssemblyObjectFactory.createMultiParameter();

        mp.setType(vicon.getType());
        for (Object vi : vicon.getArgs()) {
            mp.getParameters().add(buildParameters(vi));
        }
        return mp;
    }

    private FactoryParameter buildParameterFromFactory(ViskitModelInstantiator.Factory vifact) {
        FactoryParameter factoryParameter = jaxbAssemblyObjectFactory.createFactoryParameter();

        factoryParameter.setType(   vifact.getType());
        factoryParameter.setFactory(vifact.getFactoryClass());

        for (Object vi : vifact.getParams()) {
            factoryParameter.getParameters().add(buildParameters(vi));
        }
        return factoryParameter;
    }

    private MultiParameter buildParametersFromArray(ViskitModelInstantiator.Array viarr) {
        MultiParameter multiParameter = jaxbAssemblyObjectFactory.createMultiParameter();

        multiParameter.setType(viarr.getType());
        for (Object nextInstantiator : viarr.getInstantiators()) 
        {
            multiParameter.getParameters().add(buildParameters(nextInstantiator));
        }
        return multiParameter;
    }

    private TerminalParameter buildParamFromVarargs(ViskitModelInstantiator.Array viarr) {
        return buildTerminalParameterFromFreeF((ViskitModelInstantiator.FreeF) viarr.getInstantiators().get(0));
    }

    private void buildPropertyChangeListenerConnectionsFromJaxb(List<PropertyChangeListenerConnection> propertyChangeListenerConnectionsList) {
        PropertyChangeEdge propertyChangeEdge;
        AssemblyNode toNode, fromNode;
        for (PropertyChangeListenerConnection nextPropertyChangeListenerConnection : propertyChangeListenerConnectionsList) {
            propertyChangeEdge = new PropertyChangeEdge();
            propertyChangeEdge.setProperty(nextPropertyChangeListenerConnection.getProperty());
            propertyChangeEdge.setDescription(ViskitStatics.emptyIfNull(nextPropertyChangeListenerConnection.getDescription()));
              toNode = getNodeCache().get(nextPropertyChangeListenerConnection.getListener());
            fromNode = getNodeCache().get(nextPropertyChangeListenerConnection.getSource());
            propertyChangeEdge.setTo(toNode);
            propertyChangeEdge.setFrom(fromNode);
            propertyChangeEdge.opaqueModelObject = nextPropertyChangeListenerConnection;

            toNode.getConnections().add(propertyChangeEdge);
            fromNode.getConnections().add(propertyChangeEdge);

            this.notifyChanged(new ModelEvent(propertyChangeEdge, ModelEvent.PCL_EDGE_ADDED, "PCL edge added"));
        }
    }

    private void buildSimEventConnectionsFromJaxb(List<SimEventListenerConnection> simevconnsList) {
        SimEventListenerEdge sele;
        AssemblyNode toNode, frNode;
        for (SimEventListenerConnection selc : simevconnsList) {
            sele = new SimEventListenerEdge();
            toNode = getNodeCache().get(selc.getListener());
            frNode = getNodeCache().get(selc.getSource());
            sele.setTo(toNode);
            sele.setFrom(frNode);
            sele.opaqueModelObject = selc;
            sele.setDescription(selc.getDescription());

            toNode.getConnections().add(sele);
            frNode.getConnections().add(sele);
            this.notifyChanged(new ModelEvent(sele, ModelEvent.SIM_EVENT_LISTENER_EDGE_ADDED, "Sim event listener connection added"));
        }
    }

    private void buildAdapterConnectionsFromJaxb(List<Adapter> adaptersList) 
    {
        AdapterEdge adapterEdge;
        AssemblyNode toNode, fromNode;
        String event;
        for (Adapter jaxbAdapter : adaptersList) 
        {
            adapterEdge = new AdapterEdge();
            toNode   = getNodeCache().get(jaxbAdapter.getTo());
            fromNode = getNodeCache().get(jaxbAdapter.getFrom());
            adapterEdge.setTo(toNode);
            adapterEdge.setFrom(fromNode);

            // Handle XML names w/ underscores (XML IDREF issue)
            event = jaxbAdapter.getEventHeard();
            if (event.contains("_"))
                event = event.substring(0, event.indexOf("_"));
            adapterEdge.setSourceEvent(event);

            event = jaxbAdapter.getEventSent();
            if (event.contains("_"))
                event = event.substring(0, event.indexOf("_"));
            adapterEdge.setTargetEvent(event);

            adapterEdge.setName(jaxbAdapter.getName());
            adapterEdge.setDescription(jaxbAdapter.getDescription());
            adapterEdge.opaqueModelObject = jaxbAdapter;

              toNode.getConnections().add(adapterEdge);
            fromNode.getConnections().add(adapterEdge);
            this.notifyChanged(new ModelEvent(adapterEdge, ModelEvent.ADAPTER_EDGE_ADDED, "Adapter connection added"));
        }
    }

    private void buildPropertyChangeListenersFromJaxb(List<PropertyChangeListener> pcLs) {
        for (PropertyChangeListener pcl : pcLs) {
            buildPclNodeFromJaxbPCL(pcl);
        }
    }

    private void buildEventGraphsFromJaxb(List<SimEntity> simEntityList, List<Output> outputList, List<Verbose> verboseList) 
    {
        for (SimEntity nextSimEntity : simEntityList) {
            boolean isOutput = false;
            boolean isVerbose = false;
            // This must be done in this order, because the buildEvgNode...below
            // causes AssembleModel to be reentered, and the outputList gets hit.
            Object simEntity;
            for (Output o : outputList) {
                simEntity = o.getEntity();
                if (simEntity.equals(nextSimEntity.getName())) {
                    isOutput = true;
                    break;
                }
            }

            // Verbose shouldn't be populated since the verbose check box has been disabled
            for (Verbose v : verboseList) {
                simEntity = v.getEntity();
                if (simEntity.equals(nextSimEntity.getName())) {
                    isVerbose = true;
                    break;
                }
            }
            buildEventGraphNodeFromJaxbSimEntity(nextSimEntity, isOutput, isVerbose);
        }
    }

    private PropertyChangeListenerNode buildPclNodeFromJaxbPCL(PropertyChangeListener pcl) {
        PropertyChangeListenerNode pNode = (PropertyChangeListenerNode) getNodeCache().get(pcl.getName());
        if (pNode != null) {
            return pNode;
        }
        pNode = new PropertyChangeListenerNode(pcl.getName(), pcl.getType());

        // For backwards compatibility, bug 706
        pNode.setClearStatisticsAfterEachRun(pcl.getMode().contains("replicationStat"));
        pNode.setStatisticTypeMean(Boolean.parseBoolean(pcl.getMeanStatistics()));
        pNode.setStatisticTypeCount(Boolean.parseBoolean(pcl.getCountStatistics()));
        pNode.setDescription(pcl.getDescription());
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
                getInstantiatorListFromJaxbParameterList(lis));
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

    private EventGraphNode buildEventGraphNodeFromJaxbSimEntity(SimEntity simEntity, boolean isOutputNode, boolean isVerboseNode) {
        EventGraphNode en = (EventGraphNode) getNodeCache().get(simEntity.getName());
        if (en != null) {
            return en;
        }
        en = new EventGraphNode(simEntity.getName(), simEntity.getType());

        Coordinate coor = simEntity.getCoordinate();
        if (coor == null) {
            en.setPosition(pointLess);
            pointLess = new Point2D.Double(pointLess.x + 20, pointLess.y + 20);
        } else {
            en.setPosition(new Point2D.Double(Double.parseDouble(coor.getX()),
                    Double.parseDouble(coor.getY())));
        }

        en.setDescription(simEntity.getDescription());
        en.setOutputMarked(isOutputNode);
        en.setVerboseMarked(isVerboseNode);

        List<Object> lis = simEntity.getParameters();

        ViskitModelInstantiator vc = new ViskitModelInstantiator.Constr(lis, simEntity.getType());
        en.setInstantiator(vc);

        en.opaqueModelObject = simEntity;

        getNodeCache().put(en.getName(), en);   // key = se

        if (!nameCheck()) {
            mangleName(en);
        }
        notifyChanged(new ModelEvent(en, ModelEvent.EVENT_GRAPH_ADDED, "Event added"));

        return en;
    }

    // replaced, see ViskitStatics.nullIfEmpty
//    /**
//     * "nullIfEmpty" Return the passed string if non-zero length, else null
//     * @param s the string to check for non-zero length
//     * @return the passed string if non-zero length, else null
//     */
//    private String nIe(String s) {
//        if (s != null) {
//            if (s.length() == 0) {
//                s = null;
//            }
//        }
//        return s;
//    }

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

    /**
     * @return the currentAssemblyModelName
     */
    public String getCurrentAssemblyModelName() 
    {
        if (currentAssemblyModelFile != null)
        {
            currentAssemblyModelName = currentAssemblyModelFile.getName();
            return currentAssemblyModelName;
        }
        else 
        {
            LOG.error("getCurrentAssemblyModelName() found currentAssemblyModelFile is null, no name returned");
            return "";
        }
    }

    /**
     * @return whether model
     */
    @Override
    public boolean isModelModified() {
        return modelModified;
    }

    /**
     * @param modelModified the model to set
     */
    @Override
    public void setModelModified(boolean modelModified)
    {
        this.modelModified = modelModified;
        ViskitGlobals.instance().getAssemblyEditorViewFrame().enableAssemblyMenuItems();
        ViskitGlobals.instance().getAssemblyEditorViewFrame().enableProjectMenuItems(); // enable/disable Save All Models menu item
    }

} // end class AssemblyModelImpl
