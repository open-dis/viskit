package viskit.model;

import edu.nps.util.LogUtilities;
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
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import org.apache.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.util.FileBasedAssemblyNode;
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
public class AssemblyModelImpl extends mvcAbstractModel implements AssemblyModel
{
    static final Logger LOG = LogUtilities.getLogger(AssemblyModelImpl.class);

    private JAXBContext    jaxbContext;
    private ObjectFactory  jaxbObjectFactory;
    private SimkitAssembly jaxbSimkitAssembly;
    private File           currentFile;
    private boolean        modelDirty = false;
    private GraphMetadata  graphMetadata;

    /** We require specific order on this Map's contents */
    private final Map<String, AssemblyNode> nodeCacheMap;
    private final String schemaLocation = XMLValidationTool.ASSEMBLY_SCHEMA;
    private Point2D.Double pointLess;
    private final AssemblyControllerImpl assemblyController;

    public AssemblyModelImpl(mvcController assemblyController)
	{
        pointLess = new Point2D.Double(30, 60);
        this.assemblyController = (AssemblyControllerImpl) assemblyController;
        graphMetadata = new GraphMetadata(this);
		if ((graphMetadata.description == null) || graphMetadata.description.trim().isEmpty())
			 graphMetadata.description = ViskitStatics.DEFAULT_DESCRIPTION;
        nodeCacheMap  = new LinkedHashMap<>();
    }

    public void initialize() 
	{
        try {
            jaxbContext = JAXBContext.newInstance(SimkitAssemblyXML2Java.ASSEMBLY_BINDINGS);
            jaxbObjectFactory = new ObjectFactory();
            jaxbSimkitAssembly = jaxbObjectFactory.createSimkitAssembly(); // to start with empty graph
        } 
		catch (JAXBException e) {
            assemblyController.messageToUser(JOptionPane.ERROR_MESSAGE,
                    "XML Error",
                    "Exception on JAXBContext instantiation" +
                    "\n" + e.getMessage()
                    );
            e.printStackTrace();
        }
    }

    @Override
    public boolean isDirty() 
	{
        return modelDirty;
    }

    @Override
    public void setDirty(boolean dirtyStatus)
	{
        modelDirty = dirtyStatus;
    }

    @Override
    public SimkitAssembly getJaxbRoot()
	{
        return jaxbSimkitAssembly;
    }

    @Override
    public GraphMetadata getMetadata() 
	{
        return graphMetadata;
    }

    @Override
    public void setMetadata(GraphMetadata graphMetadata) 
	{
        this.graphMetadata = graphMetadata;
        setDirty(true);
    }

    @Override
    public boolean newModel(File file)
	{
        getNodeCache().clear();
        pointLess = new Point2D.Double(30, 60);
        this.notifyChanged(new ModelEvent(this, ModelEvent.NEWASSEMBLYMODEL, "New empty assembly model"));

        if (file == null)
		{
            jaxbSimkitAssembly = jaxbObjectFactory.createSimkitAssembly(); // to start with empty graph
        } 
		else 
		{
            try {
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                // Check for inadvertant opening of an EG, tough to do, yet possible (bugfix 1248)
                try {
                    jaxbSimkitAssembly = (SimkitAssembly) unmarshaller.unmarshal(file);
                } 
				catch (ClassCastException cce) {
                    // If we get here, they've likely tried to load an event graph.
                    assemblyController.messageToUser(JOptionPane.ERROR_MESSAGE,
                            "Wrong File Type", // TODO confirm
                            "This file is not an Assembly model!"
                            );
                    return false;
                }

                GraphMetadata myMetadata = new GraphMetadata(this);
                myMetadata.revision      = jaxbSimkitAssembly.getVersion();
                myMetadata.name          = jaxbSimkitAssembly.getName();
                myMetadata.packageName   = jaxbSimkitAssembly.getPackage();
                myMetadata.description   = jaxbSimkitAssembly.getDescription(); // TODO salvage Comment entries in older versions
				if ((myMetadata.description == null) || myMetadata.description.trim().isEmpty())
					 myMetadata.description = ViskitStatics.DEFAULT_DESCRIPTION;

                Schedule schedule = jaxbSimkitAssembly.getSchedule();
                if (schedule != null) 
				{
                    String stopTime = schedule.getStopTime();
                    if (stopTime != null && stopTime.trim().length() > 0)
					{
                        myMetadata.stopTime = stopTime.trim();
                    }
                    myMetadata.verbose = schedule.getVerbose().equalsIgnoreCase("true");
                }

                setMetadata(myMetadata);
                buildEventGraphsFromJaxb(jaxbSimkitAssembly.getSimEntity(), jaxbSimkitAssembly.getOutput(), jaxbSimkitAssembly.getVerbose());
                buildPropertyChangeListenersFromJaxb(jaxbSimkitAssembly.getPropertyChangeListener());
                buildPropertyChangeListenerConnectionsFromJaxb(jaxbSimkitAssembly.getPropertyChangeListenerConnection());
                buildSimEventListenerConnectionsFromJaxb(jaxbSimkitAssembly.getSimEventListenerConnection());
                buildAdapterConnectionsFromJaxb(jaxbSimkitAssembly.getAdapter());
            } 
			catch (JAXBException e)
			{
                assemblyController.messageToUser(JOptionPane.ERROR_MESSAGE,
                        "XML Input/Output Error",
                        "Exception on JAXB unmarshalling of" +
                            "\n" + file.getName() +
                            "\n" + e.getMessage() +
                            "\nin AssemblyModel.newModel(File)"
                            );
				e.printStackTrace();

                return false;
            }
        }

        currentFile = file;
        setDirty(false);
        return true;
    }

    @Override
    public void saveModel(File file) 
	{
        if (file == null)
		{
            file = currentFile;
        }

        // Do the marshalling into a temporary file, in order to avoid possible
        // deletion of existing file on a marshal error.

        File tempFile;
        FileWriter fileWriter = null;
        try {
            tempFile = TempFileManager.createTempFile("tmpAsymarshal", ".xml");
        } 
		catch (IOException e) 
		{
            assemblyController.messageToUser(JOptionPane.ERROR_MESSAGE,
                    "Input/Output (I/O) Error",
                    "Exception creating temporary file, AssemblyModel.saveModel():" +
                    "\n" + e.getMessage()
                    );
            return;
        }

        try {
            fileWriter = new FileWriter(tempFile);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			// https://dersteps.wordpress.com/2012/08/22/enable-jaxb-event-handling-to-catch-errors-as-they-happen
			jaxbMarshaller.setEventHandler(new ValidationEventHandler() {
				@Override
				public boolean handleEvent(ValidationEvent validationEvent) {
					LOG.error("Marshaller event handler says: " + validationEvent.getMessage() + 
							  " (Exception: " + validationEvent.getLinkedException() + ")");
					return false;
				}
			});
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, schemaLocation);

            jaxbSimkitAssembly.setName(ViskitGlobals.nullIfEmpty(graphMetadata.name));
            jaxbSimkitAssembly.setVersion(ViskitGlobals.nullIfEmpty(graphMetadata.revision));
            jaxbSimkitAssembly.setPackage(ViskitGlobals.nullIfEmpty(graphMetadata.packageName));
            jaxbSimkitAssembly.setDescription(ViskitGlobals.nullIfEmpty(graphMetadata.description));

            if (jaxbSimkitAssembly.getSchedule() == null)
			{
                jaxbSimkitAssembly.setSchedule(jaxbObjectFactory.createSchedule());
            }
            if (!graphMetadata.stopTime.equals(""))
			{
                jaxbSimkitAssembly.getSchedule().setStopTime(graphMetadata.stopTime);
            } 
			else 
			{
                jaxbSimkitAssembly.getSchedule().setStopTime("100.0"); // TODO global default
            }

            jaxbSimkitAssembly.getSchedule().setVerbose("" + graphMetadata.verbose);

            jaxbMarshaller.marshal(jaxbSimkitAssembly, fileWriter);

            // OK, made it through the marshal, overwrite the "real" file
            Files.copy(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

            modelDirty = false;
            currentFile = file;
        } 
		catch (JAXBException e) 
		{
            assemblyController.messageToUser(JOptionPane.ERROR_MESSAGE,
                    "XML Input/Output Error",
                    "Exception on JAXB marshalling" +
                    "\n" + file +
                    "\n" + e.getMessage() +
                    "\n(check for blank data fields)"
                    );
            e.printStackTrace();
        } 
		catch (IOException ex) {
            assemblyController.messageToUser(JOptionPane.ERROR_MESSAGE,
                    "File Input/Output Error",
                    "Exception on writing " + file.getName() +
                    "\n" + ex.getMessage());
            ex.printStackTrace();
        } 
		finally {
            try {
                if (fileWriter != null)
                    fileWriter.close();
            }
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
        }
    }

    @Override
    public File getLastFile()
	{
        return currentFile;
    }

    @Override
    public void externalClassesChanged(Vector<String> v) {

    }
    private final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private String _fourHexDigits(int i)
	{
        char[] ca = new char[4];
        for (int j = 3; j >= 0; j--)
		{
            int idx = i & 0xF;
            i >>= 4;
            ca[j] = hexDigits[idx];
        }
        return new String(ca);
    }
    Random mangleRandom = new Random();

    private String mangleName(String name)
	{
        int nxt = mangleRandom.nextInt(0x1_0000); // 4 hex digits
        StringBuilder sb = new StringBuilder(name);
        if (sb.charAt(sb.length() - 1) == '_')
		{
            sb.setLength(sb.length() - 6);
        }
        sb.append('_');
        sb.append(_fourHexDigits(nxt));
        sb.append('_');
        return sb.toString();
    }

    private void mangleName(ViskitElement node)
	{
        do 
		{
            node.setName(mangleName(node.getName()));
        } 
		while (!nameCheckSatisfactory());
    }

    private boolean nameCheckSatisfactory()
	{
        Set<String> hashSet = new HashSet<>(10);
        for (AssemblyNode assemblyNode : getNodeCache().values())
		{
            if (!hashSet.add(assemblyNode.getName()))
			{
                assemblyController.messageToUser(JOptionPane.INFORMATION_MESSAGE,
                        "XML file contains duplicate event name", assemblyNode.getName() +
                        "\nUnique name substituted."); // TODO check, strictly speaking this is not true yet
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean nameExists(String name)
	{
        for (AssemblyNode assemblyNode : getNodeCache().values())
		{
            if (assemblyNode.getName().equals(name))
			{
                return true;
            }
        }
        return false;
    }

    @Override
    public void newEventGraphFromXML(String widgetName, FileBasedAssemblyNode node, Point2D point, String description)
	{
        newEventGraph(widgetName, node.loadedClass, point, description);
    }

    @Override
    public void newEventGraph(String widgetName, String className, Point2D point, String description)
	{
        EventGraphNode eventGraphNode = new EventGraphNode(widgetName, className, description);
        if (point == null)
		{
            eventGraphNode.setPosition(pointLess);
        } 
		else 
		{
            eventGraphNode.setPosition(point);
        }

        SimEntity jaxbSimEntity = jaxbObjectFactory.createSimEntity();

        jaxbSimEntity.setName(ViskitGlobals.nullIfEmpty(widgetName));
        jaxbSimEntity.setType(className);
        jaxbSimEntity.setDescription(description);
        eventGraphNode.opaqueModelObject = jaxbSimEntity;

        ViskitInstantiator viskitConstructor = new ViskitInstantiator.Construct(jaxbSimEntity.getType(), null);  // null means undefined
        eventGraphNode.setInstantiator(viskitConstructor);
        eventGraphNode.setVerboseMarked(true); // default for new SimEntity.  TODO use user preference

        if (!nameCheckSatisfactory())
		{
            mangleName(eventGraphNode);
        }

        getNodeCache().put(eventGraphNode.getName(), eventGraphNode);   // key = event graph name

        jaxbSimkitAssembly.getSimEntity().add(jaxbSimEntity);

        modelDirty = true;
        notifyChanged(new ModelEvent(eventGraphNode, ModelEvent.EVENTGRAPH_ADDED, "New event graph added to assembly: " + eventGraphNode.getName()));
    }

    @Override
    public void redoEventGraph(EventGraphNode eventGraphNode) 
	{
        SimEntity jaxbSimEntity = jaxbObjectFactory.createSimEntity();

        jaxbSimEntity.setName(eventGraphNode.getName());
        eventGraphNode.opaqueModelObject = jaxbSimEntity;
        jaxbSimEntity.setType(eventGraphNode.getType());

        getNodeCache().put(eventGraphNode.getName(), eventGraphNode);   // key = ev

        jaxbSimkitAssembly.getSimEntity().add(jaxbSimEntity);

        modelDirty = true;
        notifyChanged(new ModelEvent(eventGraphNode, ModelEvent.REDO_EVENT_GRAPH, "Event Graph redone"));
    }

    @Override
    public void deleteEventGraphNode(EventGraphNode eventNode) 
	{
        SimEntity simEntity = (SimEntity) eventNode.opaqueModelObject;
        getNodeCache().remove(simEntity.getName());
        jaxbSimkitAssembly.getSimEntity().remove(simEntity);

        modelDirty = true;

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(eventNode, ModelEvent.EVENTGRAPH_DELETED, "Event Graph deleted"));
        else
            notifyChanged(new ModelEvent(eventNode, ModelEvent.UNDO_EVENT_GRAPH, "Event Graph undone"));
    }

    @Override
    public void newPropertyChangeListenerFromXML(String widgetName, FileBasedAssemblyNode node, Point2D point, String description)
	{
        newPropertyChangeListener(widgetName, node.loadedClass, point, description);
    }

    @Override
    public void newPropertyChangeListener(String widgetName, String className, Point2D point, String description)
	{
        PropertyChangeListenerNode pcNode = new PropertyChangeListenerNode(widgetName, className, description);
        if (point == null) {
            pcNode.setPosition(pointLess);
        } else {
            pcNode.setPosition(point);
        }

        PropertyChangeListener pcl = jaxbObjectFactory.createPropertyChangeListener();

        pcl.setName(ViskitGlobals.nullIfEmpty(widgetName));
        pcl.setType(className);
        pcNode.opaqueModelObject = pcl;

        List<Object> lis = pcl.getParameters();

        ViskitInstantiator vc = new ViskitInstantiator.Construct(pcl.getType(), lis);
        pcNode.setInstantiator(vc);

        if (!nameCheckSatisfactory()) 
		{
            mangleName(pcNode);
        }

        getNodeCache().put(pcNode.getName(), pcNode);   // key = ev

        jaxbSimkitAssembly.getPropertyChangeListener().add(pcl);

        modelDirty = true;
        notifyChanged(new ModelEvent(pcNode, ModelEvent.PCL_ADDED, "Property Change Node added to assembly"));
    }

    @Override
    public void redoPropertyChangeListener(PropertyChangeListenerNode node) 
	{
        PropertyChangeListener jaxbPCL = jaxbObjectFactory.createPropertyChangeListener();

        jaxbPCL.setName(node.getName());
        jaxbPCL.setType(node.getType());

        node.opaqueModelObject = jaxbPCL;

        jaxbSimkitAssembly.getPropertyChangeListener().add(jaxbPCL);

        modelDirty = true;
        notifyChanged(new ModelEvent(node, ModelEvent.REDO_PCL, "Property Change Node redone"));
    }

    @Override
    public void deletePropertyChangeListener(PropertyChangeListenerNode pclNode) 
	{
        PropertyChangeListener jaxbPcNode = (PropertyChangeListener) pclNode.opaqueModelObject;
        getNodeCache().remove(pclNode.getName());
        jaxbSimkitAssembly.getPropertyChangeListener().remove(jaxbPcNode);

        modelDirty = true;

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(pclNode, ModelEvent.PCL_DELETED, "Property Change Listener deleted"));
        else
            notifyChanged(new ModelEvent(pclNode, ModelEvent.UNDO_PCL, "Property Change Listener undone"));
    }

    @Override
    public AdapterEdge newAdapterEdge(String adapterName, AssemblyNode sourceNode, AssemblyNode targetNode)
	{
        AdapterEdge adapterEdge = new AdapterEdge();
        adapterEdge.setFrom(sourceNode);
        adapterEdge.setTo(targetNode);
        adapterEdge.setName(adapterName);

        sourceNode.getConnections().add(adapterEdge);
        targetNode.getConnections().add(adapterEdge);

        Adapter jaxbAdapter = jaxbObjectFactory.createAdapter();

        adapterEdge.opaqueModelObject = jaxbAdapter;
        jaxbAdapter.setTo(targetNode.getName());
        jaxbAdapter.setFrom(sourceNode.getName());

        jaxbAdapter.setName(adapterName);

        jaxbSimkitAssembly.getAdapter().add(jaxbAdapter);
        modelDirty = true;
        this.notifyChanged(new ModelEvent(adapterEdge, ModelEvent.ADAPTEREDGE_ADDED, "new Adapter edge added"));
        return adapterEdge;
    }

    @Override
    public void redoAdapterEdge(AdapterEdge adapterEdge)
	{
        AssemblyNode sourceNode, targetNode;

        sourceNode = (AssemblyNode) adapterEdge.getFrom();
        targetNode = (AssemblyNode) adapterEdge.getTo();

        Adapter jaxbAdapter = jaxbObjectFactory.createAdapter();
        adapterEdge.opaqueModelObject = jaxbAdapter;
        jaxbAdapter.setTo(targetNode.getName());
        jaxbAdapter.setFrom(sourceNode.getName());
        jaxbAdapter.setName(adapterEdge.getName());

        jaxbSimkitAssembly.getAdapter().add(jaxbAdapter);

        modelDirty = true;

        this.notifyChanged(new ModelEvent(adapterEdge, ModelEvent.REDO_ADAPTER_EDGE, "Adapter edge added"));
    }

    @Override
    public PropertyChangeListenerEdge newPropChangeEdge(AssemblyNode sourceNode, AssemblyNode targetNode)
	{
        PropertyChangeListenerEdge propertyChangeListenerEdge = new PropertyChangeListenerEdge();
        propertyChangeListenerEdge.setFrom(sourceNode);
        propertyChangeListenerEdge.setTo(targetNode);

        sourceNode.getConnections().add(propertyChangeListenerEdge);
        targetNode.getConnections().add(propertyChangeListenerEdge);

        PropertyChangeListenerConnection propertyChangeListenerConnection = jaxbObjectFactory.createPropertyChangeListenerConnection();

        propertyChangeListenerEdge.opaqueModelObject = propertyChangeListenerConnection;

        propertyChangeListenerConnection.setListener(targetNode.getName());
        propertyChangeListenerConnection.setSource(sourceNode.getName());

        jaxbSimkitAssembly.getPropertyChangeListenerConnection().add(propertyChangeListenerConnection);
        modelDirty = true;

        this.notifyChanged(new ModelEvent(propertyChangeListenerEdge, ModelEvent.PCLEDGE_ADDED, "PCL edge added"));
        return propertyChangeListenerEdge;
    }

    @Override
    public void redoPropertyChangeListenerEdge(PropertyChangeListenerEdge propertyChangeListenerEdge)
	{
        AssemblyNode sourceNode, targetNode;

        sourceNode = (AssemblyNode) propertyChangeListenerEdge.getFrom();
        targetNode = (AssemblyNode) propertyChangeListenerEdge.getTo();

        PropertyChangeListenerConnection propertyChangeListenerConnection = jaxbObjectFactory.createPropertyChangeListenerConnection();
        propertyChangeListenerEdge.opaqueModelObject = propertyChangeListenerConnection;
        propertyChangeListenerConnection.setListener(targetNode.getName());
        propertyChangeListenerConnection.setSource(sourceNode.getName());

        jaxbSimkitAssembly.getPropertyChangeListenerConnection().add(propertyChangeListenerConnection);
        modelDirty = true;

        this.notifyChanged(new ModelEvent(propertyChangeListenerEdge, ModelEvent.REDO_PCL_EDGE, "PCL edge added"));
    }

    @Override
    public void newSimEventListenerEdge(AssemblyNode sourceNode, AssemblyNode targetNode)
	{
        SimEventListenerEdge simEventListenerEdge = new SimEventListenerEdge();
        simEventListenerEdge.setFrom(sourceNode);
        simEventListenerEdge.setTo(targetNode);

        sourceNode.getConnections().add(simEventListenerEdge);
        targetNode.getConnections().add(simEventListenerEdge);

        SimEventListenerConnection simEventListenerConnection = jaxbObjectFactory.createSimEventListenerConnection();

        simEventListenerEdge.opaqueModelObject = simEventListenerConnection;

        simEventListenerConnection.setListener(targetNode.getName());
        simEventListenerConnection.setSource(sourceNode.getName());

        jaxbSimkitAssembly.getSimEventListenerConnection().add(simEventListenerConnection);

        modelDirty = true;
        notifyChanged(new ModelEvent(simEventListenerEdge, ModelEvent.SIMEVENTLISTEDGE_ADDED, "SimEventListenerEdge added"));
    }

    @Override
    public void redoSimEventListenerEdge(SimEventListenerEdge simEventListenerEdge)
	{
        AssemblyNode sourceNode, targetNode;

        sourceNode = (AssemblyNode) simEventListenerEdge.getFrom();
        targetNode = (AssemblyNode) simEventListenerEdge.getTo();

        SimEventListenerConnection simEventListenerConnection = jaxbObjectFactory.createSimEventListenerConnection();
        simEventListenerEdge.opaqueModelObject = simEventListenerConnection;
        simEventListenerConnection.setListener(targetNode.getName());
        simEventListenerConnection.setSource(sourceNode.getName());

        jaxbSimkitAssembly.getSimEventListenerConnection().add(simEventListenerConnection);

        modelDirty = true;
        notifyChanged(new ModelEvent(simEventListenerEdge, ModelEvent.REDO_SIMEVENT_LISTENER_EDGE, "SimEventListenerEdge redone"));
    }

    @Override
    public void deletePropChangeEdge(PropertyChangeListenerEdge propertyChangeListenerEdge) 
	{
        PropertyChangeListenerConnection propertyChangeListenerConnection = (PropertyChangeListenerConnection) propertyChangeListenerEdge.opaqueModelObject;

        jaxbSimkitAssembly.getPropertyChangeListenerConnection().remove(propertyChangeListenerConnection);

        modelDirty = true;

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(propertyChangeListenerEdge, ModelEvent.PCLEDGE_DELETED, "PCL edge deleted"));
        else
            notifyChanged(new ModelEvent(propertyChangeListenerEdge, ModelEvent.UNDO_PCL_EDGE, "PCL edge undone"));
    }

    @Override
    public void deleteSimEventListenerEdge(SimEventListenerEdge simEventListenerEdge)
	{
        SimEventListenerConnection simEventListenerConnection = (SimEventListenerConnection) simEventListenerEdge.opaqueModelObject;

        jaxbSimkitAssembly.getSimEventListenerConnection().remove(simEventListenerConnection);

        modelDirty = true;

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(simEventListenerEdge, ModelEvent.SIMEVENTLISTEDGE_DELETED, "SimEventListenerEdge deleted"));
        else
            notifyChanged(new ModelEvent(simEventListenerEdge, ModelEvent.UNDO_SIMEVENT_LISTENER_EDGE, "SimEventListenerEdge undone"));
    }

    @Override
    public void deleteAdapterEdge(AdapterEdge adapterEdge)
	{
        Adapter jaxbAdapter = (Adapter) adapterEdge.opaqueModelObject;
        jaxbSimkitAssembly.getAdapter().remove(jaxbAdapter);

        modelDirty = true;

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(adapterEdge, ModelEvent.ADAPTEREDGE_DELETED, "Adapter edge deleted"));
        else
            notifyChanged(new ModelEvent(adapterEdge, ModelEvent.UNDO_ADAPTER_EDGE, "Adapter edge undone"));
    }

    @Override
    public void changePclEdge(PropertyChangeListenerEdge propertyChangeListenerEdge)
	{
        PropertyChangeListenerConnection PropertyChangeListenerConnection = (PropertyChangeListenerConnection) propertyChangeListenerEdge.opaqueModelObject;
        PropertyChangeListenerConnection.setProperty(propertyChangeListenerEdge.getProperty());
        PropertyChangeListenerConnection.setDescription(propertyChangeListenerEdge.getDescription());

        modelDirty = true;
        notifyChanged(new ModelEvent(propertyChangeListenerEdge, ModelEvent.PCLEDGE_CHANGED, "PCL edge changed"));
    }

    @Override
    public void changeAdapterEdge(AdapterEdge adapterEdge)
	{
        EventGraphNode sourceEvent = (EventGraphNode) adapterEdge.getFrom();
        EventGraphNode targetEvent = (EventGraphNode) adapterEdge.getTo();

        Adapter jaxbAE = (Adapter) adapterEdge.opaqueModelObject;

        jaxbAE.setFrom(sourceEvent.getName());
        jaxbAE.setTo(targetEvent.getName());

        jaxbAE.setEventHeard(adapterEdge.getSourceEvent());
        jaxbAE.setEventSent(adapterEdge.getTargetEvent());

        jaxbAE.setName(adapterEdge.getName());
        jaxbAE.setDescription(adapterEdge.getDescription());

        modelDirty = true;
        notifyChanged(new ModelEvent(adapterEdge, ModelEvent.ADAPTEREDGE_CHANGED, "Adapter edge changed"));
    }

    @Override
    public void changeSimEventListenerEdge(SimEventListenerEdge simEventListenerEdge)
	{
        EventGraphNode sourceEvent = (EventGraphNode) simEventListenerEdge.getFrom();
        EventGraphNode targetEvent = (EventGraphNode) simEventListenerEdge.getTo();
        SimEventListenerConnection simEventListenerConnection = (SimEventListenerConnection) simEventListenerEdge.opaqueModelObject;

        simEventListenerConnection.setListener(targetEvent.getName());
        simEventListenerConnection.setSource(sourceEvent.getName());
        simEventListenerConnection.setDescription(simEventListenerEdge.getDescription());

        modelDirty = true;
        notifyChanged(new ModelEvent(simEventListenerEdge, ModelEvent.SIMEVENTLISTEDGE_CHANGED, "SimEvListener edge changed"));
    }

    @Override
    public boolean changePclNode(PropertyChangeListenerNode pclNode) 
	{
        boolean returnValue = true;
        if (!nameCheckSatisfactory())
		{
            mangleName(pclNode);
            returnValue = false;
        }
        PropertyChangeListener propertyChangeListener = (PropertyChangeListener) pclNode.opaqueModelObject;
        propertyChangeListener.setName(pclNode.getName());
        propertyChangeListener.setType(pclNode.getType());
        propertyChangeListener.setDescription(pclNode.getDescription());

        // Modes should be singular.  All new Assemblies will be with singular mode
        if (pclNode.isSampleStatistics())
		{
            if (pclNode.isClearStatisticsAfterEachRun())
			{
                propertyChangeListener.setMode("replicationStat"); // TODO replicationStatistic
            } 
			else 
			{
                propertyChangeListener.setMode("designPointStat"); // TODO designPointStatistic
            }
        }

        String statistics = pclNode.isGetCount() ? "true" : "false";
        propertyChangeListener.setCountStatistics(statistics);

        statistics = pclNode.isGetMean() ? "true" : "false";
        propertyChangeListener.setMeanStatistics(statistics);

        double x = pclNode.getPosition().getX();
        double y = pclNode.getPosition().getY();
        Coordinate coor = jaxbObjectFactory.createCoordinate();
        coor.setX("" + x);
        coor.setY("" + y);
        pclNode.getPosition().setLocation(x, y);
        propertyChangeListener.setCoordinate(coor);

        List<Object> propertyChangeListenerParametersList = propertyChangeListener.getParameters();
        propertyChangeListenerParametersList.clear();

        ViskitInstantiator pclNodeInstantiator = pclNode.getInstantiator();

        // this will be a list of one...a MultiParameter....get its list, but
        // throw away the object itself.  This is because the
        // PropertyChangeListener object serves as "its own" MultiParameter.
        List<Object> jaxbParameterList = getJaxbParameterList(pclNodeInstantiator);

        if (jaxbParameterList.size() != 1)
		{
            throw new RuntimeException("Design error in AssemblyModel");
        }

        MultiParameter multiParameter = (MultiParameter) jaxbParameterList.get(0);

        for (Object o : multiParameter.getParameters())
		{
            propertyChangeListenerParametersList.add(o);
        }
        modelDirty = true;
        this.notifyChanged(new ModelEvent(pclNode, ModelEvent.PCL_CHANGED, "Property Change Listener node changed"));
        return returnValue;
    }

    @Override
    public boolean changeEventGraphNode(EventGraphNode eventNode)
	{
        boolean returnValue = true;
        if (!nameCheckSatisfactory())
		{
            mangleName(eventNode);
            returnValue = false;
        }
        SimEntity jaxbSimEntity = (SimEntity) eventNode.opaqueModelObject;

        jaxbSimEntity.setName       (eventNode.getName());
        jaxbSimEntity.setType       (eventNode.getType());
        jaxbSimEntity.setDescription(eventNode.getDescription());

        double x = eventNode.getPosition().getX();
        double y = eventNode.getPosition().getY();
        Coordinate coordinate = jaxbObjectFactory.createCoordinate();
        coordinate.setX("" + x);
        coordinate.setY("" + y);
        eventNode.getPosition().setLocation(x, y);
        jaxbSimEntity.setCoordinate(coordinate);

        List<Object> parametersList = jaxbSimEntity.getParameters();
        parametersList.clear();

        ViskitInstantiator viskitInstantiator = eventNode.getInstantiator();

        // this will be a list of one...a MultiParameter....get its list, but
        // throw away the object itself.  This is because the SimEntity object
        // serves as "its own" MultiParameter.
        List<Object> parameterList = getJaxbParameterList(viskitInstantiator);

        if (parameterList.size() != 1)
		{
            throw new RuntimeException("Design error in AssemblyModel");
        }

        MultiParameter multiParameter = (MultiParameter) parameterList.get(0);

        for (Object o : multiParameter.getParameters())
		{
            parametersList.add(o);
        }

        if (eventNode.isOutputMarked())
		{
            addToOutputList(jaxbSimEntity);
        } 
		else 
		{
            removeFromOutputList(jaxbSimEntity);
        }

        if (eventNode.isVerboseMarked())
		{
            addToVerboseList(jaxbSimEntity);
        } 
		else
		{
            removeFromVerboseList(jaxbSimEntity);
        }

        modelDirty = true;
        this.notifyChanged(new ModelEvent(eventNode, ModelEvent.EVENTGRAPH_CHANGED, "Event changed"));
        return returnValue;
    }

    private void removeFromOutputList(SimEntity jaxbSimEntity) 
	{
        List<Output> outTL = jaxbSimkitAssembly.getOutput();
        for (Output o : outTL) 
		{
            if (o.getEntity().equals(jaxbSimEntity.getName())) 
			{
                outTL.remove(o);
                return;
            }
        }
    }

    private void removeFromVerboseList(SimEntity jaxbSimEntity)
	{
        List<Verbose> vTL = jaxbSimkitAssembly.getVerbose();
        for (Verbose v : vTL)
		{
            if (v.getEntity().equals(jaxbSimEntity.getName()))
			{
                vTL.remove(v);
                return;
            }
        }
    }

    private void addToOutputList(SimEntity jaxbSimEntity)
	{
        List<Output> jaxbOutputList = jaxbSimkitAssembly.getOutput();
        for (Output o : jaxbOutputList)
		{
            if (o.getEntity().equals(jaxbSimEntity.getName()))
			{
                return; // found it
            }
        }
        Output jaxbOutputObject = jaxbObjectFactory.createOutput();
        jaxbOutputObject.setEntity(jaxbSimEntity.getName());
        jaxbOutputList.add(jaxbOutputObject);
    }

    private void addToVerboseList(SimEntity jaxbSimEntity)
	{
        List<Verbose> verboseList = jaxbSimkitAssembly.getVerbose();
        for (Verbose v : verboseList)
		{
            if (v.getEntity().equals(jaxbSimEntity.getName()))
			{
                return; // found it already there
            }
        }
        Verbose jaxbVerboseObject = jaxbObjectFactory.createVerbose();
        jaxbVerboseObject.setEntity(jaxbSimEntity.getName());
        verboseList.add(jaxbVerboseObject);
    }

    @Override
    public Vector<String> getDetailedOutputEntityNames()
	{
        Vector<String> v = new Vector<>();
        for (Output jaxbOutputObject : jaxbSimkitAssembly.getOutput())
		{
            Object jaxbEntity = jaxbOutputObject.getEntity();
            if (jaxbEntity instanceof SimEntity)
			{
                v.add(((SimEntity) jaxbEntity).getName());
            } 
			else if (jaxbEntity instanceof PropertyChangeListener)
			{
                v.add(((PropertyChangeListener) jaxbEntity).getName());
            }
			else // added missing case, getEntity produces a string
			{
				v.add(jaxbEntity.toString()); // TODO confirm OK
			}
        }
        return v;
    }

    @Override
    public Vector<String> getVerboseOutputEntityNames() {
        Vector<String> v = new Vector<>();
        for (Verbose ot : jaxbSimkitAssembly.getVerbose()) {
            Object jaxbEntity = ot.getEntity();
            if (jaxbEntity instanceof SimEntity) {
                v.add(((SimEntity) jaxbEntity).getName());
            } else if (jaxbEntity instanceof PropertyChangeListener) {
                v.add(((PropertyChangeListener) jaxbEntity).getName());
            }
        }
        return v;
    }

   private List<Object> getInstantiatorListFromJaxbParameterList(List<Object> objectList) {

       // To prevent java.util.ConcurrentModificationException
       List<Object> vi = new ArrayList<>();
        for (Object o : objectList) {
            vi.add(buildInstantiatorFromJaxbParameter(o));
        }
        return vi;
    }

    private ViskitInstantiator buildInstantiatorFromJaxbParameter(Object jaxbObject)
	{
        if (jaxbObject instanceof TerminalParameter)
		{
            return buildFreeFormFromTerminalParameter((TerminalParameter) jaxbObject);
        }
        if (jaxbObject instanceof MultiParameter) // used for both arrays and Constr arg lists
		{          
            MultiParameter multiParameter = (MultiParameter) jaxbObject;
            return (multiParameter.getType().contains("[")) ? buildArrayFromMultiParameter(multiParameter) : buildConstrFromMultiParameter(multiParameter);
        }
		else return (jaxbObject instanceof FactoryParameter) ? buildFactoryInstFromFactoryParameter((FactoryParameter) jaxbObject) : null;
    }

    private ViskitInstantiator.FreeForm buildFreeFormFromTerminalParameter(TerminalParameter terminalParameter)
	{
		ViskitInstantiator.FreeForm viskitInstantiator = new ViskitInstantiator.FreeForm(terminalParameter.getType(), terminalParameter.getName());
		viskitInstantiator.setValue      (terminalParameter.getValue());
		viskitInstantiator.setDescription(terminalParameter.getDescription());
        return viskitInstantiator;
    }

    private ViskitInstantiator.Array buildArrayFromMultiParameter(MultiParameter multiParameter)
	{
        return new ViskitInstantiator.Array(multiParameter.getType(),
                getInstantiatorListFromJaxbParameterList(multiParameter.getParameters()));
    }

    private ViskitInstantiator.Construct buildConstrFromMultiParameter(MultiParameter multiParameter)
	{
        return new ViskitInstantiator.Construct(multiParameter.getType(),
                getInstantiatorListFromJaxbParameterList(multiParameter.getParameters()));
    }

    private ViskitInstantiator.Factory buildFactoryInstFromFactoryParameter(FactoryParameter factoryParameter)
	{
        return new ViskitInstantiator.Factory(factoryParameter.getType(),
                factoryParameter.getFactory(),
                ViskitStatics.RANDOM_VARIATE_FACTORY_DEFAULT_METHOD,
                getInstantiatorListFromJaxbParameterList(factoryParameter.getParameters()));
    }

    // We know we will get a List<Object> one way or the other
    @SuppressWarnings("unchecked")
    private List<Object> getJaxbParameterList(ViskitInstantiator viskitInstantiator)
	{
        Object jaxbObject = buildParameter(viskitInstantiator);
        if (jaxbObject instanceof List<?>) 
		{
            return (List<Object>) jaxbObject;
        }
        Vector<Object> v = new Vector<>();
        v.add(jaxbObject);
        return v;
    }

    private Object buildParameter(Object viskitInstantiator)
	{
        if (viskitInstantiator instanceof ViskitInstantiator.FreeForm) 
		{
            return buildParameterFromFreeForm((ViskitInstantiator.FreeForm) viskitInstantiator);
        } //TerminalParm
		else if (viskitInstantiator instanceof ViskitInstantiator.Construct) 
		{
            return buildParamFromConstr((ViskitInstantiator.Construct) viskitInstantiator);
        } // List of Parmarameters
        else if (viskitInstantiator instanceof ViskitInstantiator.Factory) 
		{
            return buildParamFromFactory((ViskitInstantiator.Factory) viskitInstantiator);
        } // FactoryParam
        else if (viskitInstantiator instanceof ViskitInstantiator.Array)
		{
            ViskitInstantiator.Array viskitInstantiatorArray = (ViskitInstantiator.Array) viskitInstantiator;

            if (ViskitGlobals.instance().isArray(viskitInstantiatorArray.getTypeName()))
                return buildParameterFromArray(viskitInstantiatorArray);
            else if (viskitInstantiatorArray.getTypeName().contains("..."))
                return buildParameterFromVarargs(viskitInstantiatorArray);
        } // MultiParam

        //assert false : AssemblyModelImpl.buildJaxbParameter() received null;
        return null; // TODO error condition?
    }

    private TerminalParameter buildParameterFromFreeForm(ViskitInstantiator.FreeForm viff) {
        TerminalParameter terminalParameter = jaxbObjectFactory.createTerminalParameter();

        terminalParameter.setType(viff.getTypeName());
        terminalParameter.setValue(viff.getValue());
        terminalParameter.setName(viff.getName());
        return terminalParameter;
    }

    private MultiParameter buildParamFromConstr(ViskitInstantiator.Construct vicon) {
        MultiParameter multiParameter = jaxbObjectFactory.createMultiParameter();

        multiParameter.setType(vicon.getTypeName());
        for (Object vi : vicon.getParametersList()) {
            multiParameter.getParameters().add(buildParameter(vi));
        }
        return multiParameter;
    }

    private FactoryParameter buildParamFromFactory(ViskitInstantiator.Factory vifact) {
        FactoryParameter factoryParameter = jaxbObjectFactory.createFactoryParameter();

        factoryParameter.setType(vifact.getTypeName());
        factoryParameter.setFactory(vifact.getFactoryClass());

        for (Object vi : vifact.getParametersList()) {
            factoryParameter.getParameters().add(buildParameter(vi));
        }
        return factoryParameter;
    }

    private MultiParameter buildParameterFromArray(ViskitInstantiator.Array viarr) {
        MultiParameter multiParameter = jaxbObjectFactory.createMultiParameter();

        multiParameter.setType(viarr.getTypeName());
        for (Object vi : viarr.getInstantiators()) {
            multiParameter.getParameters().add(buildParameter(vi));
        }
        return multiParameter;
    }

    private TerminalParameter buildParameterFromVarargs(ViskitInstantiator.Array viskitInstantiatorArray)
	{
        return buildParameterFromFreeForm((ViskitInstantiator.FreeForm) viskitInstantiatorArray.getInstantiators().get(0));
    }

    private void buildPropertyChangeListenerConnectionsFromJaxb(List<PropertyChangeListenerConnection> propertyChangeListenerConnectionList)
	{
        for (PropertyChangeListenerConnection propertyChangeListenerConnection : propertyChangeListenerConnectionList)
		{
            PropertyChangeListenerEdge propertyChangeListenerEdge = new PropertyChangeListenerEdge();
            propertyChangeListenerEdge.setProperty(propertyChangeListenerConnection.getProperty());
            propertyChangeListenerEdge.setDescription(propertyChangeListenerConnection.getDescription());
            AssemblyNode   toNode = getNodeCache().get(propertyChangeListenerConnection.getListener());
            AssemblyNode fromNode = getNodeCache().get(propertyChangeListenerConnection.getSource());
            propertyChangeListenerEdge.opaqueModelObject = propertyChangeListenerConnection;
            propertyChangeListenerEdge.setDescription(propertyChangeListenerConnection.getDescription()); // TODO check
			
			if (toNode != null)
			{
				propertyChangeListenerEdge.setTo(toNode);
                toNode.getConnections().add(propertyChangeListenerEdge);
			}
            if (fromNode != null)
			{
				propertyChangeListenerEdge.setFrom(fromNode);
				fromNode.getConnections().add(propertyChangeListenerEdge);
			}
            this.notifyChanged(new ModelEvent(propertyChangeListenerEdge, ModelEvent.PCLEDGE_ADDED, "PCL edge added"));
        }
    }

    private void buildSimEventListenerConnectionsFromJaxb(List<SimEventListenerConnection> simEventListenerConnectionsList)
	{
        for (SimEventListenerConnection simEventListenerConnection : simEventListenerConnectionsList)
		{
            SimEventListenerEdge simEventListenerEdge = new SimEventListenerEdge();
            AssemblyNode toNode = getNodeCache().get(simEventListenerConnection.getListener());
            AssemblyNode fromNode = getNodeCache().get(simEventListenerConnection.getSource());
            simEventListenerEdge.opaqueModelObject = simEventListenerConnection;
            simEventListenerEdge.setDescription(simEventListenerConnection.getDescription());
			
			if (toNode != null)
			{
				simEventListenerEdge.setTo(toNode);
				toNode.getConnections().add(simEventListenerEdge);
			}
            if (fromNode != null)
			{
				simEventListenerEdge.setFrom(fromNode);
				fromNode.getConnections().add(simEventListenerEdge);
			}
            this.notifyChanged(new ModelEvent(simEventListenerEdge, ModelEvent.SIMEVENTLISTEDGE_ADDED, "Sim event listener connection added"));
        }
    }

    private void buildAdapterConnectionsFromJaxb(List<Adapter> adaptersList)
	{
        for (Adapter jaxbAdapter : adaptersList) {
            AdapterEdge adapterEdge = new AdapterEdge();
            AssemblyNode   toNode = getNodeCache().get(jaxbAdapter.getTo());
            AssemblyNode fromNode = getNodeCache().get(jaxbAdapter.getFrom());
			
			if (toNode != null)
			{
				adapterEdge.setTo  (  toNode);
			}
            if (fromNode != null)
			{
				adapterEdge.setFrom(fromNode);
			}
            // Handle XML names with underscores (note XML IDREF issue)
            String event = jaxbAdapter.getEventHeard();
			if ((event == null) || event.isEmpty())
			{
				LOG.error ("event name not found, buildAdapterConnectionsFromJaxb failed");
				return;
			}
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

			if ( toNode != null)
                  toNode.getConnections().add(adapterEdge);
			if (fromNode != null)
                fromNode.getConnections().add(adapterEdge);
            this.notifyChanged(new ModelEvent(adapterEdge, ModelEvent.ADAPTEREDGE_ADDED, "Adapter connection added"));
        }
    }

    private void buildPropertyChangeListenersFromJaxb(List<PropertyChangeListener> propertyChangeListenerList)
	{
        for (PropertyChangeListener pcl : propertyChangeListenerList)
		{
            buildPropertyChangeListenerNodeFromJaxbPCL(pcl);
        }
    }

    private void buildEventGraphsFromJaxb(List<SimEntity> jaxbSimEntitiesList, List<Output> jaxbOutputList, List<Verbose> jaxbVerboseList)
	{
        for (SimEntity jaxbSimEntity : jaxbSimEntitiesList)
		{
            boolean isOutput  = false;
            boolean isVerbose = false;
            // This must be done in this order, because the buildEvgNode...below
            // causes AssembleModel to be reentered, and the outputList gets hit.
            for (Output jaxbOutput : jaxbOutputList)
			{
                String simEntityName = jaxbOutput.getEntity();
                if (simEntityName.equals(jaxbSimEntity.getName()))
				{
                    isOutput = true;
                    break;
                }
            }
            for (Verbose jaxbVerbose : jaxbVerboseList)
			{
                String simEntityName = jaxbVerbose.getEntity();
                if (simEntityName.equals(jaxbSimEntity.getName()))
				{
                    isVerbose = true;
                    break;
                }
            }
            buildEventGraphNodeFromJaxbSimEntity(jaxbSimEntity, isOutput, isVerbose);
        }
    }

    private PropertyChangeListenerNode buildPropertyChangeListenerNodeFromJaxbPCL(PropertyChangeListener propertyChangeListener)
	{
        PropertyChangeListenerNode propertyChangeListenerNode = (PropertyChangeListenerNode) getNodeCache().get(propertyChangeListener.getName());
        if (propertyChangeListenerNode != null)
		{
            return propertyChangeListenerNode;
        }
        propertyChangeListenerNode = new PropertyChangeListenerNode(propertyChangeListener.getName(), 
				                                                    propertyChangeListener.getType(), 
				                                                    propertyChangeListener.getDescription());

        // For backwards compatibility
        propertyChangeListenerNode.setClearStatisticsAfterEachRun(propertyChangeListener.getMode().contains("replicationStat"));
        propertyChangeListenerNode.setGetMean(Boolean.parseBoolean(propertyChangeListener.getMeanStatistics()));
        propertyChangeListenerNode.setGetCount(Boolean.parseBoolean(propertyChangeListener.getCountStatistics()));
        propertyChangeListenerNode.setDescription(propertyChangeListener.getDescription());
        Coordinate coordinate = propertyChangeListener.getCoordinate();
        if (coordinate == null)
		{
            propertyChangeListenerNode.setPosition(pointLess);
            pointLess = new Point2D.Double(pointLess.x + 20, pointLess.y + 20);
        } 
		else 
		{
            propertyChangeListenerNode.setPosition(new Point2D.Double(Double.parseDouble(coordinate.getX()),
                                                                      Double.parseDouble(coordinate.getY())));
        }

        List<Object> propertyChangeListenerParameters = propertyChangeListener.getParameters();
        ViskitInstantiator viskitInstantiatorConstructor = new ViskitInstantiator.Construct(propertyChangeListener.getType(),
                getInstantiatorListFromJaxbParameterList(propertyChangeListenerParameters));
        propertyChangeListenerNode.setInstantiator(viskitInstantiatorConstructor);

        propertyChangeListenerNode.opaqueModelObject = propertyChangeListener;
        LogUtilities.getLogger(AssemblyModelImpl.class).debug("propertyChangeListenerNode name: " + propertyChangeListenerNode.getName());

        getNodeCache().put(propertyChangeListenerNode.getName(), propertyChangeListenerNode);   // key = se

        if (!nameCheckSatisfactory())
		{
            mangleName(propertyChangeListenerNode);
        }
        notifyChanged(new ModelEvent(propertyChangeListenerNode, ModelEvent.PCL_ADDED, "propertyChangeListener added"));
        return propertyChangeListenerNode;
    }

    private EventGraphNode buildEventGraphNodeFromJaxbSimEntity(SimEntity jaxbSimEntity, boolean isOutputNode, boolean isVerboseNode)
	{
        EventGraphNode eventGraphNode = (EventGraphNode) getNodeCache().get(jaxbSimEntity.getName());
        if (eventGraphNode != null)
		{
            return eventGraphNode;
        }
        eventGraphNode = new EventGraphNode(jaxbSimEntity.getName(), jaxbSimEntity.getType(), ViskitStatics.DEFAULT_DESCRIPTION);

        Coordinate coordinate = jaxbSimEntity.getCoordinate();
        if (coordinate == null) 
		{
            eventGraphNode.setPosition(pointLess);
            pointLess = new Point2D.Double(pointLess.x + 20, pointLess.y + 20);
        } 
		else 
		{
            eventGraphNode.setPosition(new Point2D.Double(Double.parseDouble(coordinate.getX()),
                                                          Double.parseDouble(coordinate.getY())));
        }

        eventGraphNode.setDescription(jaxbSimEntity.getDescription());
        eventGraphNode.setOutputMarked(isOutputNode);
		boolean verbose = false;
		if ((jaxbSimEntity.isVerbose() != null) && (jaxbSimEntity.isVerbose()))
			    verbose = true;
        eventGraphNode.setVerboseMarked(isVerboseNode || verbose);

        List<Object> parameterList = jaxbSimEntity.getParameters();

        ViskitInstantiator viskitInstantiatorConstructor = new ViskitInstantiator.Construct(parameterList, jaxbSimEntity.getType());
        eventGraphNode.setInstantiator(viskitInstantiatorConstructor);

        eventGraphNode.opaqueModelObject = jaxbSimEntity;

        getNodeCache().put(eventGraphNode.getName(), eventGraphNode);   // key = se

        if (!nameCheckSatisfactory())
		{
            mangleName(eventGraphNode);
        }
        notifyChanged(new ModelEvent(eventGraphNode, ModelEvent.EVENTGRAPH_ADDED, "Event added"));

        return eventGraphNode;
    }

    public Map<String, AssemblyNode> getNodeCache() {
        return nodeCacheMap;
    }
}
