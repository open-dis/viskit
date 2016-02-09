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
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
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
public class AssemblyModelImpl extends mvcAbstractModel implements AssemblyModel {

    private JAXBContext    jaxbContext;
    private ObjectFactory  jaxbObjectFactory;
    private SimkitAssembly jaxbSimkitAssembly;
    private File currentFile;
    private boolean modelDirty = false;
    private GraphMetadata metadata;

    /** We require specific order on this Map's contents */
    private final Map<String, AssemblyNode> nodeCache;
    private final String schemaLocation = XMLValidationTool.ASSEMBLY_SCHEMA;
    private Point2D.Double pointLess;
    private final AssemblyControllerImpl assemblyController;

    public AssemblyModelImpl(mvcController cont) {
        pointLess = new Point2D.Double(30, 60);
        assemblyController = (AssemblyControllerImpl) cont;
        metadata = new GraphMetadata(this);
        nodeCache = new LinkedHashMap<>();
    }

    public void initialize() {
        try {
            jaxbContext = JAXBContext.newInstance(SimkitAssemblyXML2Java.ASSEMBLY_BINDINGS);
            jaxbObjectFactory = new ObjectFactory();
            jaxbSimkitAssembly = jaxbObjectFactory.createSimkitAssembly(); // to start with empty graph
        } catch (JAXBException e) {
            assemblyController.messageToUser(JOptionPane.ERROR_MESSAGE,
                    "XML Error",
                    "Exception on JAXBContext instantiation" +
                    "\n" + e.getMessage()
                    );
            e.printStackTrace();
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
        return jaxbSimkitAssembly;
    }

    @Override
    public GraphMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(GraphMetadata gmd) {
        metadata = gmd;
        setDirty(true);
    }

    @Override
    public boolean newModel(File f) {
        getNodeCache().clear();
        pointLess = new Point2D.Double(30, 60);
        this.notifyChanged(new ModelEvent(this, ModelEvent.NEWASSEMBLYMODEL, "New empty assembly model"));

        if (f == null) {
            jaxbSimkitAssembly = jaxbObjectFactory.createSimkitAssembly(); // to start with empty graph
        } else {
            try {
                Unmarshaller u = jaxbContext.createUnmarshaller();

                // Check for inadvertant opening of an EG, tough to do, yet possible (bugfix 1248)
                try {
                    jaxbSimkitAssembly = (SimkitAssembly) u.unmarshal(f);
                } catch (ClassCastException cce) {
                    // If we get here, they've tried to load an event graph.
                    assemblyController.messageToUser(JOptionPane.ERROR_MESSAGE,
                            "Wrong File Type", // TODO confirm
                            "Use the event graph editor to" +
                            "\n" + "work with this file."
                            );
                    return false;
                }

                GraphMetadata myMetadata = new GraphMetadata(this);
                myMetadata.version       = jaxbSimkitAssembly.getVersion();
                myMetadata.name          = jaxbSimkitAssembly.getName();
                myMetadata.packageName   = jaxbSimkitAssembly.getPackage();
                myMetadata.description   = jaxbSimkitAssembly.getDescription(); // TODO salvage Comment entries in older versions

                Schedule schedule = jaxbSimkitAssembly.getSchedule();
                if (schedule != null) {
                    String stopTime = schedule.getStopTime();
                    if (stopTime != null && stopTime.trim().length() > 0) {
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
                            "\n" + f.getName() +
                            "\n" + e.getMessage() +
                            "\nin AssemblyModel.newModel(File)"
                            );
				e.printStackTrace();

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
            assemblyController.messageToUser(JOptionPane.ERROR_MESSAGE,
                    "Input/Output (I/O) Error",
                    "Exception creating temporary file, AssemblyModel.saveModel():" +
                    "\n" + e.getMessage()
                    );
            return;
        }

        try {
            fw = new FileWriter(tmpF);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			// https://dersteps.wordpress.com/2012/08/22/enable-jaxb-event-handling-to-catch-errors-as-they-happen
			jaxbMarshaller.setEventHandler(new ValidationEventHandler() {
				@Override
				public boolean handleEvent(ValidationEvent validationEvent) {
					System.out.println("Marshaller event handler says: " + validationEvent.getMessage() + 
									   " (Exception: " + validationEvent.getLinkedException() + ")");
					return false;
				}
			});
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, schemaLocation);

            jaxbSimkitAssembly.setName(nullIfEmpty(metadata.name));
            jaxbSimkitAssembly.setVersion(nullIfEmpty(metadata.version));
            jaxbSimkitAssembly.setPackage(nullIfEmpty(metadata.packageName));
            jaxbSimkitAssembly.setDescription(nullIfEmpty(metadata.description));

            if (jaxbSimkitAssembly.getSchedule() == null) {
                jaxbSimkitAssembly.setSchedule(jaxbObjectFactory.createSchedule());
            }
            if (!metadata.stopTime.equals("")) {
                jaxbSimkitAssembly.getSchedule().setStopTime(metadata.stopTime);
            } else {
                jaxbSimkitAssembly.getSchedule().setStopTime("100.0");
            }

            jaxbSimkitAssembly.getSchedule().setVerbose("" + metadata.verbose);

            jaxbMarshaller.marshal(jaxbSimkitAssembly, fw);

            // OK, made it through the marshal, overwrite the "real" file
            Files.copy(tmpF.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);

            modelDirty = false;
            currentFile = f;
        } catch (JAXBException e) {
            assemblyController.messageToUser(JOptionPane.ERROR_MESSAGE,
                    "XML Input/Output Error",
                    "Exception on JAXB marshalling" +
                    "\n" + f +
                    "\n" + e.getMessage() +
                    "\n(check for blank data fields)"
                    );
            e.printStackTrace();
        } catch (IOException ex) {
            assemblyController.messageToUser(JOptionPane.ERROR_MESSAGE,
                    "File Input/Output Error",
                    "Exception on writing " + f.getName() +
                    "\n" + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                if (fw != null)
                    fw.close();
            } catch (IOException ioe) {
				ioe.printStackTrace();
			}
        }
    }

    @Override
    public File getLastFile() {
        return currentFile;
    }

    @Override
    public void externalClassesChanged(Vector<String> v) {

    }
    private final char[] hdigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

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

    private void mangleName(ViskitElement node) {
        do {
            node.setName(AssemblyModelImpl.this.mangleName(node.getName()));
        } while (!nameCheck());
    }

    private boolean nameCheck() {
        Set<String> hs = new HashSet<>(10);
        for (AssemblyNode n : getNodeCache().values()) {
            if (!hs.add(n.getName())) {
                assemblyController.messageToUser(JOptionPane.INFORMATION_MESSAGE,
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
            if (n.getName().equals(name)) {
                return true;
            }
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

        SimEntity jaxbSimEntity = jaxbObjectFactory.createSimEntity();

        jaxbSimEntity.setName(nullIfEmpty(widgetName));
        jaxbSimEntity.setType(className);
        node.opaqueModelObject = jaxbSimEntity;

        VInstantiator vc = new VInstantiator.Constr(jaxbSimEntity.getType(), null);  // null means undefined
        node.setInstantiator(vc);

        if (!nameCheck()) {
            mangleName(node);
        }

        getNodeCache().put(node.getName(), node);   // key = ev

        jaxbSimkitAssembly.getSimEntity().add(jaxbSimEntity);

        modelDirty = true;
        notifyChanged(new ModelEvent(node, ModelEvent.EVENTGRAPH_ADDED, "Event graph added to assembly: " + node.getName()));
    }

    @Override
    public void redoEventGraph(EventGraphNode node) {
        SimEntity jaxbEG = jaxbObjectFactory.createSimEntity();

        jaxbEG.setName(node.getName());
        node.opaqueModelObject = jaxbEG;
        jaxbEG.setType(node.getType());

        getNodeCache().put(node.getName(), node);   // key = ev

        jaxbSimkitAssembly.getSimEntity().add(jaxbEG);

        modelDirty = true;
        notifyChanged(new ModelEvent(node, ModelEvent.REDO_EVENT_GRAPH, "Event Graph redone"));
    }

    @Override
    public void deleteEvGraphNode(EventGraphNode eventNode) {
        SimEntity jaxbEv = (SimEntity) eventNode.opaqueModelObject;
        getNodeCache().remove(jaxbEv.getName());
        jaxbSimkitAssembly.getSimEntity().remove(jaxbEv);

        modelDirty = true;

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(eventNode, ModelEvent.EVENTGRAPH_DELETED, "Event Graph deleted"));
        else
            notifyChanged(new ModelEvent(eventNode, ModelEvent.UNDO_EVENT_GRAPH, "Event Graph undone"));
    }

    @Override
    public void newPropertyChangeListenerFromXML(String widgetName, FileBasedAssemblyNode node, Point2D p) {
        newPropertyChangeListener(widgetName, node.loadedClass, p);
    }

    @Override
    public void newPropertyChangeListener(String widgetName, String className, Point2D p) {
        PropertyChangeListenerNode pcNode = new PropertyChangeListenerNode(widgetName, className);
        if (p == null) {
            pcNode.setPosition(pointLess);
        } else {
            pcNode.setPosition(p);
        }

        PropertyChangeListener pcl = jaxbObjectFactory.createPropertyChangeListener();

        pcl.setName(nullIfEmpty(widgetName));
        pcl.setType(className);
        pcNode.opaqueModelObject = pcl;

        List<Object> lis = pcl.getParameters();

        VInstantiator vc = new VInstantiator.Constr(pcl.getType(), lis);
        pcNode.setInstantiator(vc);

        if (!nameCheck()) {
            mangleName(pcNode);
        }

        getNodeCache().put(pcNode.getName(), pcNode);   // key = ev

        jaxbSimkitAssembly.getPropertyChangeListener().add(pcl);

        modelDirty = true;
        notifyChanged(new ModelEvent(pcNode, ModelEvent.PCL_ADDED, "Property Change Node added to assembly"));
    }

    @Override
    public void redoPropertyChangeListener(PropertyChangeListenerNode node) {

        PropertyChangeListener jaxbPCL = jaxbObjectFactory.createPropertyChangeListener();

        jaxbPCL.setName(node.getName());
        jaxbPCL.setType(node.getType());

        node.opaqueModelObject = jaxbPCL;

        jaxbSimkitAssembly.getPropertyChangeListener().add(jaxbPCL);

        modelDirty = true;
        notifyChanged(new ModelEvent(node, ModelEvent.REDO_PCL, "Property Change Node redone"));
    }

    @Override
    public void deletePropertyChangeListener(PropertyChangeListenerNode pclNode) {
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
    public AdapterEdge newAdapterEdge(String adName, AssemblyNode src, AssemblyNode target) {
        AdapterEdge ae = new AdapterEdge();
        ae.setFrom(src);
        ae.setTo(target);
        ae.setName(adName);

        src.getConnections().add(ae);
        target.getConnections().add(ae);

        Adapter jaxbAdapter = jaxbObjectFactory.createAdapter();

        ae.opaqueModelObject = jaxbAdapter;
        jaxbAdapter.setTo(target.getName());
        jaxbAdapter.setFrom(src.getName());

        jaxbAdapter.setName(adName);

        jaxbSimkitAssembly.getAdapter().add(jaxbAdapter);

        modelDirty = true;

        this.notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGE_ADDED, "Adapter edge added"));
        return ae;
    }

    @Override
    public void redoAdapterEdge(AdapterEdge ae) {
        AssemblyNode src, target;

        src = (AssemblyNode) ae.getFrom();
        target = (AssemblyNode) ae.getTo();

        Adapter jaxbAdapter = jaxbObjectFactory.createAdapter();
        ae.opaqueModelObject = jaxbAdapter;
        jaxbAdapter.setTo(target.getName());
        jaxbAdapter.setFrom(src.getName());
        jaxbAdapter.setName(ae.getName());

        jaxbSimkitAssembly.getAdapter().add(jaxbAdapter);

        modelDirty = true;

        this.notifyChanged(new ModelEvent(ae, ModelEvent.REDO_ADAPTER_EDGE, "Adapter edge added"));
    }

    @Override
    public PropertyChangeListenerEdge newPropChangeEdge(AssemblyNode src, AssemblyNode target) {
        PropertyChangeListenerEdge pce = new PropertyChangeListenerEdge();
        pce.setFrom(src);
        pce.setTo(target);

        src.getConnections().add(pce);
        target.getConnections().add(pce);

        PropertyChangeListenerConnection pclc = jaxbObjectFactory.createPropertyChangeListenerConnection();

        pce.opaqueModelObject = pclc;

        pclc.setListener(target.getName());
        pclc.setSource(src.getName());

        jaxbSimkitAssembly.getPropertyChangeListenerConnection().add(pclc);
        modelDirty = true;

        this.notifyChanged(new ModelEvent(pce, ModelEvent.PCLEDGE_ADDED, "PCL edge added"));
        return pce;
    }

    @Override
    public void redoPropChangeEdge(PropertyChangeListenerEdge pce) {
        AssemblyNode src, target;

        src = (AssemblyNode) pce.getFrom();
        target = (AssemblyNode) pce.getTo();

        PropertyChangeListenerConnection pclc = jaxbObjectFactory.createPropertyChangeListenerConnection();
        pce.opaqueModelObject = pclc;
        pclc.setListener(target.getName());
        pclc.setSource(src.getName());

        jaxbSimkitAssembly.getPropertyChangeListenerConnection().add(pclc);
        modelDirty = true;

        this.notifyChanged(new ModelEvent(pce, ModelEvent.REDO_PCL_EDGE, "PCL edge added"));
    }

    @Override
    public void newSimEvLisEdge(AssemblyNode src, AssemblyNode target) {
        SimEventListenerEdge sele = new SimEventListenerEdge();
        sele.setFrom(src);
        sele.setTo(target);

        src.getConnections().add(sele);
        target.getConnections().add(sele);

        SimEventListenerConnection selc = jaxbObjectFactory.createSimEventListenerConnection();

        sele.opaqueModelObject = selc;

        selc.setListener(target.getName());
        selc.setSource(src.getName());

        jaxbSimkitAssembly.getSimEventListenerConnection().add(selc);

        modelDirty = true;
        notifyChanged(new ModelEvent(sele, ModelEvent.SIMEVENTLISTEDGE_ADDED, "SimEvList edge added"));
    }

    @Override
    public void redoSimEvLisEdge(SimEventListenerEdge sele) {
        AssemblyNode src, target;

        src = (AssemblyNode) sele.getFrom();
        target = (AssemblyNode) sele.getTo();

        SimEventListenerConnection selc = jaxbObjectFactory.createSimEventListenerConnection();
        sele.opaqueModelObject = selc;
        selc.setListener(target.getName());
        selc.setSource(src.getName());

        jaxbSimkitAssembly.getSimEventListenerConnection().add(selc);

        modelDirty = true;
        notifyChanged(new ModelEvent(sele, ModelEvent.REDO_SIMEVENT_LISTENER_EDGE, "SimEvList Edge redone"));
    }

    @Override
    public void deletePropChangeEdge(PropertyChangeListenerEdge pce) {
        PropertyChangeListenerConnection pclc = (PropertyChangeListenerConnection) pce.opaqueModelObject;

        jaxbSimkitAssembly.getPropertyChangeListenerConnection().remove(pclc);

        modelDirty = true;

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(pce, ModelEvent.PCLEDGE_DELETED, "PCL edge deleted"));
        else
            notifyChanged(new ModelEvent(pce, ModelEvent.UNDO_PCL_EDGE, "PCL edge undone"));
    }

    @Override
    public void deleteSimEvLisEdge(SimEventListenerEdge sele) {
        SimEventListenerConnection sel_c = (SimEventListenerConnection) sele.opaqueModelObject;

        jaxbSimkitAssembly.getSimEventListenerConnection().remove(sel_c);

        modelDirty = true;

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(sele, ModelEvent.SIMEVENTLISTEDGE_DELETED, "SimEvList edge deleted"));
        else
            notifyChanged(new ModelEvent(sele, ModelEvent.UNDO_SIMEVENT_LISTENER_EDGE, "SimEvList edge undone"));
    }

    @Override
    public void deleteAdapterEdge(AdapterEdge ae) {
        Adapter j_adp = (Adapter) ae.opaqueModelObject;
        jaxbSimkitAssembly.getAdapter().remove(j_adp);

        modelDirty = true;

        if (!assemblyController.isUndo())
            notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGE_DELETED, "Adapter edge deleted"));
        else
            notifyChanged(new ModelEvent(ae, ModelEvent.UNDO_ADAPTER_EDGE, "Adapter edge undone"));
    }

    @Override
    public void changePclEdge(PropertyChangeListenerEdge propertyChangeListenerEdge) {
        PropertyChangeListenerConnection PropertyChangeListenerConnection = (PropertyChangeListenerConnection) propertyChangeListenerEdge.opaqueModelObject;
        PropertyChangeListenerConnection.setProperty(propertyChangeListenerEdge.getProperty());
        PropertyChangeListenerConnection.setDescription(propertyChangeListenerEdge.getDescription());

        modelDirty = true;
        notifyChanged(new ModelEvent(propertyChangeListenerEdge, ModelEvent.PCLEDGE_CHANGED, "PCL edge changed"));
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
        jaxbAE.setDescription(ae.getDescription());

        modelDirty = true;
        notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGE_CHANGED, "Adapter edge changed"));
    }

    @Override
    public void changeSimEvEdge(SimEventListenerEdge seEdge) {
        EventGraphNode src = (EventGraphNode) seEdge.getFrom();
        EventGraphNode targ = (EventGraphNode) seEdge.getTo();
        SimEventListenerConnection selc = (SimEventListenerConnection) seEdge.opaqueModelObject;

        selc.setListener(targ.getName());
        selc.setSource(src.getName());
        selc.setDescription(seEdge.getDescription());

        modelDirty = true;
        notifyChanged(new ModelEvent(seEdge, ModelEvent.SIMEVENTLISTEDGE_CHANGED, "SimEvListener edge changed"));
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
        jaxBPcl.setDescription(pclNode.getDescription());

        // Modes should be singular.  All new Assemblies will be with singular mode
        if (pclNode.isSampleStatistics()) {
            if (pclNode.isClearStatisticsAfterEachRun()) {
                jaxBPcl.setMode("replicationStat"); // TODO replicationStatistic
            } else {
                jaxBPcl.setMode("designPointStat"); // TODO designPointStatistic
            }
        }

        String statistics = pclNode.isGetCount() ? "true" : "false";
        jaxBPcl.setCountStatistics(statistics);

        statistics = pclNode.isGetMean() ? "true" : "false";
        jaxBPcl.setMeanStatistics(statistics);

        double x = pclNode.getPosition().getX();
        double y = pclNode.getPosition().getY();
        Coordinate coor = jaxbObjectFactory.createCoordinate();
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
        this.notifyChanged(new ModelEvent(pclNode, ModelEvent.PCL_CHANGED, "Property Change Listener node changed"));
        return retcode;
    }

    @Override
    public boolean changeEvGraphNode(EventGraphNode eventNode) {
        boolean retcode = true;
        if (!nameCheck()) {
            mangleName(eventNode);
            retcode = false;
        }
        SimEntity jaxbSE = (SimEntity) eventNode.opaqueModelObject;

        jaxbSE.setName(eventNode.getName());
        jaxbSE.setType(eventNode.getType());
        jaxbSE.setDescription(eventNode.getDescription());

        double x = eventNode.getPosition().getX();
        double y = eventNode.getPosition().getY();
        Coordinate coor = jaxbObjectFactory.createCoordinate();
        coor.setX("" + x);
        coor.setY("" + y);
        eventNode.getPosition().setLocation(x, y);
        jaxbSE.setCoordinate(coor);

        List<Object> lis = jaxbSE.getParameters();
        lis.clear();

        VInstantiator inst = eventNode.getInstantiator();

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

        if (eventNode.isOutputMarked()) {
            addToOutputList(jaxbSE);
        } else {
            removeFromOutputList(jaxbSE);
        }

        if (eventNode.isVerboseMarked()) {
            addToVerboseList(jaxbSE);
        } else {
            removeFromVerboseList(jaxbSE);
        }

        modelDirty = true;
        this.notifyChanged(new ModelEvent(eventNode, ModelEvent.EVENTGRAPH_CHANGED, "Event changed"));
        return retcode;
    }

    private void removeFromOutputList(SimEntity se) {
        List<Output> outTL = jaxbSimkitAssembly.getOutput();
        for (Output o : outTL) {
            if (o.getEntity().equals(se.getName())) {
                outTL.remove(o);
                return;
            }
        }
    }

    private void removeFromVerboseList(SimEntity se) {
        List<Verbose> vTL = jaxbSimkitAssembly.getVerbose();
        for (Verbose v : vTL) {
            if (v.getEntity().equals(se.getName())) {
                vTL.remove(v);
                return;
            }
        }
    }

    private void addToOutputList(SimEntity se) {
        List<Output> outTL = jaxbSimkitAssembly.getOutput();
        for (Output o : outTL) {
            if (o.getEntity().equals(se.getName())) {
                return;
            }
        }
        Output op = jaxbObjectFactory.createOutput();
        op.setEntity(se.getName());
        outTL.add(op);
    }

    private void addToVerboseList(SimEntity se) {
        List<Verbose> vTL = jaxbSimkitAssembly.getVerbose();
        for (Verbose v : vTL) {
            if (v.getEntity().equals(se.getName())) {
                return;
            }
        }
        Verbose op = jaxbObjectFactory.createVerbose();
        op.setEntity(se.getName());
        vTL.add(op);
    }

    @Override
    public Vector<String> getDetailedOutputEntityNames() {
        Vector<String> v = new Vector<>();
        for (Output ot : jaxbSimkitAssembly.getOutput()) {
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
        for (Verbose ot : jaxbSimkitAssembly.getVerbose()) {
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
                ViskitStatics.RANDOM_VARIATE_FACTORY_DEFAULT_METHOD,
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

            if (ViskitGlobals.instance().isArray(via.getType()))
                return buildParamFromArray(via);
            else if (via.getType().contains("..."))
                return buildParamFromVarargs(via);
        } // MultiParam

        //assert false : AssemblyModelImpl.buildJaxbParameter() received null;
        return null;
    }

    private TerminalParameter buildParamFromFreeF(VInstantiator.FreeF viff) {
        TerminalParameter tp = jaxbObjectFactory.createTerminalParameter();

        tp.setType(viff.getType());
        tp.setValue(viff.getValue());
        tp.setName(viff.getName());
        return tp;
    }

    private MultiParameter buildParamFromConstr(VInstantiator.Constr vicon) {
        MultiParameter mp = jaxbObjectFactory.createMultiParameter();

        mp.setType(vicon.getType());
        for (Object vi : vicon.getArgs()) {
            mp.getParameters().add(buildParam(vi));
        }
        return mp;
    }

    private FactoryParameter buildParamFromFactory(VInstantiator.Factory vifact) {
        FactoryParameter fp = jaxbObjectFactory.createFactoryParameter();

        fp.setType(vifact.getType());
        fp.setFactory(vifact.getFactoryClass());

        for (Object vi : vifact.getParams()) {
            fp.getParameters().add(buildParam(vi));
        }
        return fp;
    }

    private MultiParameter buildParamFromArray(VInstantiator.Array viarr) {
        MultiParameter mp = jaxbObjectFactory.createMultiParameter();

        mp.setType(viarr.getType());
        for (Object vi : viarr.getInstantiators()) {
            mp.getParameters().add(buildParam(vi));
        }
        return mp;
    }

    private TerminalParameter buildParamFromVarargs(VInstantiator.Array viarr) {
        return buildParamFromFreeF((VInstantiator.FreeF) viarr.getInstantiators().get(0));
    }

    private void buildPropertyChangeListenerConnectionsFromJaxb(List<PropertyChangeListenerConnection> pcconnsList) {
        for (PropertyChangeListenerConnection pclc : pcconnsList) {
            PropertyChangeListenerEdge pce = new PropertyChangeListenerEdge();
            pce.setProperty(pclc.getProperty());
            pce.setDescription(pclc.getDescription());
            AssemblyNode toNode = getNodeCache().get(pclc.getListener());
            AssemblyNode frNode = getNodeCache().get(pclc.getSource());
            pce.setTo(toNode);
            pce.setFrom(frNode);
            pce.opaqueModelObject = pclc;

            toNode.getConnections().add(pce);
            frNode.getConnections().add(pce);

            this.notifyChanged(new ModelEvent(pce, ModelEvent.PCLEDGE_ADDED, "PCL edge added"));
        }
    }

    private void buildSimEventListenerConnectionsFromJaxb(List<SimEventListenerConnection> simEventListenerConnectionsList) {
        for (SimEventListenerConnection simEventListenerConnection : simEventListenerConnectionsList) {
            SimEventListenerEdge simEventListenerEdge = new SimEventListenerEdge();
            AssemblyNode toNode = getNodeCache().get(simEventListenerConnection.getListener());
            AssemblyNode fromNode = getNodeCache().get(simEventListenerConnection.getSource());
            simEventListenerEdge.setTo(toNode);
            simEventListenerEdge.setFrom(fromNode);
            simEventListenerEdge.opaqueModelObject = simEventListenerConnection;
            simEventListenerEdge.setDescription(simEventListenerConnection.getDescription());

            toNode.getConnections().add(simEventListenerEdge);
            fromNode.getConnections().add(simEventListenerEdge);
            this.notifyChanged(new ModelEvent(simEventListenerEdge, ModelEvent.SIMEVENTLISTEDGE_ADDED, "Sim event listener connection added"));
        }
    }

    private void buildAdapterConnectionsFromJaxb(List<Adapter> adaptersList)
	{
        for (Adapter jaxbAdapter : adaptersList) {
            AdapterEdge ae = new AdapterEdge();
            AssemblyNode toNode = getNodeCache().get(jaxbAdapter.getTo());
            AssemblyNode frNode = getNodeCache().get(jaxbAdapter.getFrom());
            ae.setTo(toNode);
            ae.setFrom(frNode);
            // Handle XML names with underscores (XML IDREF issue)
            String event = jaxbAdapter.getEventHeard();
            if (event.contains("_"))
                event = event.substring(0, event.indexOf("_"));
            ae.setSourceEvent(event);

            event = jaxbAdapter.getEventSent();
            if (event.contains("_"))
                event = event.substring(0, event.indexOf("_"));
            ae.setTargetEvent(event);
			
            ae.setName(jaxbAdapter.getName());
            ae.setDescription(jaxbAdapter.getDescription());
            ae.opaqueModelObject = jaxbAdapter;

            toNode.getConnections().add(ae);
            frNode.getConnections().add(ae);
            this.notifyChanged(new ModelEvent(ae, ModelEvent.ADAPTEREDGE_ADDED, "Adapter connection added"));
        }
    }

    private void buildPropertyChangeListenersFromJaxb(List<PropertyChangeListener> pcLs)
	{
        for (PropertyChangeListener pcl : pcLs) {
            buildPropertyChangeListenerNodeFromJaxbPCL(pcl);
        }
    }

    private void buildEventGraphsFromJaxb(List<SimEntity> jaxbSimEntitiesList, List<Output> jaxbOutputList, List<Verbose> jaxbVerboseList)
	{
        for (SimEntity jaxbSimEntity : jaxbSimEntitiesList) {
            boolean isOutput  = false;
            boolean isVerbose = false;
            // This must be done in this order, because the buildEvgNode...below
            // causes AssembleModel to be reentered, and the outputList gets hit.
            for (Output jaxbOutput : jaxbOutputList) {
                String simE = jaxbOutput.getEntity();
                if (simE.equals(jaxbSimEntity.getName())) {
                    isOutput = true;
                    break;
                }
            }
            for (Verbose jaxbVerbose : jaxbVerboseList) {
                String simE = jaxbVerbose.getEntity();
                if (simE.equals(jaxbSimEntity.getName())) {
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
        if (propertyChangeListenerNode != null) {
            return propertyChangeListenerNode;
        }
        propertyChangeListenerNode = new PropertyChangeListenerNode(propertyChangeListener.getName(), propertyChangeListener.getType());

        // For backwards compatibility, bug 706
        propertyChangeListenerNode.setClearStatisticsAfterEachRun(propertyChangeListener.getMode().contains("replicationStat"));
        propertyChangeListenerNode.setGetMean(Boolean.parseBoolean(propertyChangeListener.getMeanStatistics()));
        propertyChangeListenerNode.setGetCount(Boolean.parseBoolean(propertyChangeListener.getCountStatistics()));
        propertyChangeListenerNode.setDescription(propertyChangeListener.getDescription());
        Coordinate coor = propertyChangeListener.getCoordinate();
        if (coor == null) {
            propertyChangeListenerNode.setPosition(pointLess);
            pointLess = new Point2D.Double(pointLess.x + 20, pointLess.y + 20);
        } else {
            propertyChangeListenerNode.setPosition(new Point2D.Double(Double.parseDouble(coor.getX()),
                    Double.parseDouble(coor.getY())));
        }

        List<Object> lis = propertyChangeListener.getParameters();
        VInstantiator vc = new VInstantiator.Constr(propertyChangeListener.getType(),
                getInstantiatorListFromJaxbParmList(lis));
        propertyChangeListenerNode.setInstantiator(vc);

        propertyChangeListenerNode.opaqueModelObject = propertyChangeListener;
        LogUtils.getLogger(AssemblyModelImpl.class).debug("propertyChangeListenerNode name: " + propertyChangeListenerNode.getName());

        getNodeCache().put(propertyChangeListenerNode.getName(), propertyChangeListenerNode);   // key = se

        if (!nameCheck()) {
            mangleName(propertyChangeListenerNode);
        }
        notifyChanged(new ModelEvent(propertyChangeListenerNode, ModelEvent.PCL_ADDED, "propertyChangeListener added"));
        return propertyChangeListenerNode;
    }

    private EventGraphNode buildEventGraphNodeFromJaxbSimEntity(SimEntity simEntity, boolean isOutputNode, boolean isVerboseNode)
	{
        EventGraphNode eventGraphNode = (EventGraphNode) getNodeCache().get(simEntity.getName());
        if (eventGraphNode != null) {
            return eventGraphNode;
        }
        eventGraphNode = new EventGraphNode(simEntity.getName(), simEntity.getType());

        Coordinate coor = simEntity.getCoordinate();
        if (coor == null) {
            eventGraphNode.setPosition(pointLess);
            pointLess = new Point2D.Double(pointLess.x + 20, pointLess.y + 20);
        } else {
            eventGraphNode.setPosition(new Point2D.Double(Double.parseDouble(coor.getX()),
                    Double.parseDouble(coor.getY())));
        }

        eventGraphNode.setDescription(simEntity.getDescription());
        eventGraphNode.setOutputMarked(isOutputNode);
        eventGraphNode.setVerboseMarked(isVerboseNode);

        List<Object> parameterList = simEntity.getParameters();

        VInstantiator vc = new VInstantiator.Constr(parameterList, simEntity.getType());
        eventGraphNode.setInstantiator(vc);

        eventGraphNode.opaqueModelObject = simEntity;

        getNodeCache().put(eventGraphNode.getName(), eventGraphNode);   // key = se

        if (!nameCheck()) {
            mangleName(eventGraphNode);
        }
        notifyChanged(new ModelEvent(eventGraphNode, ModelEvent.EVENTGRAPH_ADDED, "Event added"));

        return eventGraphNode;
    }

    /**
     * "nullIfEmpty" Return the passed string if non-zero length, else null
     * @param s the string to check for non-zero length
     * @return the passed string if non-zero length, else null
     */
    private String nullIfEmpty(String s) {
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
