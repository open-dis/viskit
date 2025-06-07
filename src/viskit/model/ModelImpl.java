package viskit.model;

import edu.nps.util.TempFileManager;

import java.awt.geom.Point2D;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.ViskitUserConfiguration;
import viskit.control.EventGraphControllerImpl;
import viskit.mvc.MvcAbstractModel;
import viskit.util.XMLValidationTool;
import viskit.xsd.bindings.eventgraph.*;
import viskit.xsd.translator.assembly.SimkitAssemblyXML2Java;
import viskit.xsd.translator.eventgraph.SimkitEventGraphXML2Java;
import viskit.mvc.MvcController;

/**
 * <p>
 * This is the "master" model of an event graph.  It should control the node and
 * edge XML (JAXB) information.  What hasn't been done is to put in accessor
 * methods for the view to read pieces that it needs, say after it receives a
 * "new model" event.
 * </p>
 *
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 2, 2004
 * @since 1:09:38 PM
 * @version $Id$
 */
public class ModelImpl extends MvcAbstractModel implements Model
{
    static final Logger LOG = LogManager.getLogger();
    
    File currentFile;
    private static JAXBContext jaxbContext;
    ObjectFactory         jaxbEventGraphObjectFactory;
    SimEntity             simEntityJaxbRoot;
    Map<Event, EventNode> eventNodeCacheMap           = new HashMap<>();
    Map<Object, Edge>     edgeCacheMap                = new HashMap<>();
    Vector<ViskitElement> stateVariableElements       = new Vector<>();
    Vector<ViskitElement> simulationParameterElements = new Vector<>();

    private final String schemaLocation = XMLValidationTool.EVENT_GRAPH_SCHEMA;
    private final String privateIndexVariablePrefix    = "_index_variable_";
    private final String privateLocalVariablePrefix    = "local_variable_";
    private final String stateVariablePrefix           = "state_variable_";
    private final String DEFAULT_RUN_EVENT_DESCRIPTION = "The Run node initializes state variables and events when simulation execution begins.";
    private final EventGraphControllerImpl eventGraphController;

    private GraphMetadata graphMetadata;
    private boolean modelDirty = false;
    private boolean numericPriority;

    /** Constructor
     * @param newEventGraphController provide corresponding controller */
    public ModelImpl(MvcController newEventGraphController) 
    {
        eventGraphController = (EventGraphControllerImpl) newEventGraphController;
        graphMetadata = new GraphMetadata(this);
        graphMetadata.name                  = ViskitStatics.emptyIfNull(graphMetadata.name);
        graphMetadata.author                = ViskitStatics.emptyIfNull(graphMetadata.author);
        if (graphMetadata.author.isBlank())
            graphMetadata.author = ViskitUserConfiguration.instance().getAnalystName();
        graphMetadata.description           = ViskitStatics.emptyIfNull(graphMetadata.description);
        graphMetadata.version               = ViskitStatics.emptyIfNull(graphMetadata.version);
        graphMetadata.packageName           = ViskitStatics.emptyIfNull(graphMetadata.packageName);
        graphMetadata.extendsPackageName    = ViskitStatics.emptyIfNull(graphMetadata.extendsPackageName);
        graphMetadata.implementsPackageName = ViskitStatics.emptyIfNull(graphMetadata.implementsPackageName);
        graphMetadata.stopTime              = ViskitStatics.emptyIfNull(graphMetadata.stopTime);
    }

    @Override
    public void initialize() 
    {
        try {
            if (jaxbContext == null) // avoid JAXBException (perhaps due to concurrency)
                jaxbContext = JAXBContext.newInstance(SimkitEventGraphXML2Java.EVENT_GRAPH_BINDINGS);
            jaxbEventGraphObjectFactory = new viskit.xsd.bindings.eventgraph.ObjectFactory();
            simEntityJaxbRoot = jaxbEventGraphObjectFactory.createSimEntity(); // to start with empty graph
        } 
        catch (JAXBException e) {
            ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML Error",
                    "Exception on JAXBContext instantiation" +
                    "\n" + e.getMessage());
        }
    }

    @Override
    public boolean isModelDirty() {
        return modelDirty;
    }

    @Override
    public void setModelDirty(boolean newDirtyStatus) 
    {
        modelDirty = newDirtyStatus;
        ViskitGlobals.instance().getEventGraphEditorViewFrame().enableEventGraphMenuItems();
        ViskitGlobals.instance().getAssemblyEditorViewFrame().enableProjectMenuItems(); // enable/disable Save All Models menu item
    }

    @Override
    public GraphMetadata getMetadata() {
        return graphMetadata;
    }

    @Override
    public void changeMetadata(GraphMetadata newGraphMetadata) 
    {
        this.graphMetadata = newGraphMetadata;
        setModelDirty(true);
        notifyChanged(new ModelEvent(newGraphMetadata, ModelEvent.METADATA_CHANGED, "Metadata changed"));
    }

    @Override
    public boolean newModel(File modelFile) 
    {
              stateVariableElements.removeAllElements();
        simulationParameterElements.removeAllElements();
        eventNodeCacheMap.clear();
        edgeCacheMap.clear();

        if (modelFile == null) {
            simEntityJaxbRoot = jaxbEventGraphObjectFactory.createSimEntity(); // to start with empty graph
            notifyChanged(new ModelEvent(this, ModelEvent.NEW_MODEL, "New empty model"));
        } 
        else 
        {
            try {
                currentFile = modelFile;

                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                simEntityJaxbRoot = (SimEntity) unmarshaller.unmarshal(currentFile);

                GraphMetadata myGraphMetadata = new GraphMetadata(this);
                myGraphMetadata.name                  = ViskitStatics.emptyIfNull(simEntityJaxbRoot.getName());
                myGraphMetadata.author                = ViskitStatics.emptyIfNull(simEntityJaxbRoot.getAuthor());
//              if (myGraphMetadata.author.isBlank()) // don't override an already-existing file
//                  myGraphMetadata.author =  ViskitUserConfiguration.instance().getAnalystName();
                myGraphMetadata.version               = ViskitStatics.emptyIfNull(simEntityJaxbRoot.getVersion());
                myGraphMetadata.description           = ViskitStatics.emptyIfNull(simEntityJaxbRoot.getDescription());
                myGraphMetadata.packageName           = ViskitStatics.emptyIfNull(simEntityJaxbRoot.getPackage());
                myGraphMetadata.extendsPackageName    = ViskitStatics.emptyIfNull(simEntityJaxbRoot.getExtend());
                myGraphMetadata.implementsPackageName = ViskitStatics.emptyIfNull(simEntityJaxbRoot.getImplement());
                // obsolete
//                List<String> commentList = simEntityJaxbRoot.getComment();
//                StringBuilder sb = new StringBuilder("");
//                for (String comment : commentList) {
//                    sb.append(comment);
//                    sb.append(" ");
//                }
//                myGraphMetadata.description = sb.toString().trim();
                changeMetadata(myGraphMetadata);

                buildEventsFromJaxb(simEntityJaxbRoot.getEvent());

                // The above change metadata and build set the model dirty
                setModelDirty(false);
                // The following builds just notify

                buildParametersFromJaxb(simEntityJaxbRoot.getParameter());
                buildStateVariablesFromJaxb(simEntityJaxbRoot.getStateVariable());
                buildCodeBlockFromJaxb(simEntityJaxbRoot.getCode());
            } 
            catch (JAXBException ee) 
            {
                // want a clear way to know if they're trying to load an assembly vs. some unspecified XML.
                try {
                    if (jaxbContext == null) // avoid JAXBException (perhaps due to concurrency)
                        jaxbContext = JAXBContext.newInstance(SimkitAssemblyXML2Java.ASSEMBLY_BINDINGS);
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    unmarshaller.unmarshal(modelFile);
                    // If we get here, we've tried to load an assembly into the Event Graph editor
                    ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE,
                            "This file is an Assembly",
                            "Use the Assembly Editor to" +
                            "\n" + "work with this file: " + modelFile.getName()
                            );
                } 
                catch (JAXBException e)
                {
                    ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE,
                            "XML I/O Error",
                            "Exception on JAXB unmarshalling of" +
                            "\n" + modelFile.getName() +
                            "\nError is: " + e.getMessage() +
                            "\nin Model.newModel(File)"
                            );
                }
                return false; // from either error case
            }
        }
        return true;
    }

    @Override
    public boolean save() 
    {
        return saveModel(currentFile);
    }

    @Override
    public boolean saveModel(File modelFile) 
    {
        boolean returnValue;
        if (modelFile == null) {
            modelFile = currentFile; // keep original
        }
        else currentFile = modelFile;

        // Do the marshalling into a temporary file, so as to avoid possible
        // deletion of existing file on a marshal error.

        File tempFile;
        try {
            tempFile = TempFileManager.createTempFile("tempEventGraphMarshal", ".xml");
        } 
        catch (IOException e) 
        {
            ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE,
                    "I/O Error",
                    "Exception creating temporary file, Model.saveModel():" + "\n" + e.getMessage()
                    );
            return false;
        }

        FileWriter fileWriter = null;
        try {
            // save metadata
            fileWriter = new FileWriter(tempFile);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, schemaLocation);

            simEntityJaxbRoot.setName       (ViskitStatics.nullIfEmpty(graphMetadata.name));
            simEntityJaxbRoot.setVersion    (ViskitStatics.nullIfEmpty(graphMetadata.version));
            simEntityJaxbRoot.setAuthor     (ViskitStatics.nullIfEmpty(graphMetadata.author));
            simEntityJaxbRoot.setDescription(ViskitStatics.nullIfEmpty(graphMetadata.description));
            simEntityJaxbRoot.setPackage    (ViskitStatics.nullIfEmpty(graphMetadata.packageName));
            simEntityJaxbRoot.setExtend     (ViskitStatics.nullIfEmpty(graphMetadata.extendsPackageName));
            simEntityJaxbRoot.setImplement  (ViskitStatics.nullIfEmpty(graphMetadata.implementsPackageName));
            
            // TODO do we need to update StateVariables, Parameters, Events?  isn't this handled in subclass?

            simEntityJaxbRoot.getParameter().clear();
            for (ViskitElement nextParameterElement : simulationParameterElements) 
            {
                Parameter nextParameter = new Parameter();
                nextParameter.setName       (nextParameterElement.getName());
                nextParameter.setType       (nextParameterElement.getType());
                nextParameter.setDescription(nextParameterElement.getDescription());
                simEntityJaxbRoot.getParameter().add(nextParameter);
            }
            simEntityJaxbRoot.getStateVariable().clear();
            for (ViskitElement nextStateVariableElement : stateVariableElements) 
            {
                StateVariable nextStateVariable = new StateVariable();
                nextStateVariable.setName       (nextStateVariableElement.getName());
                nextStateVariable.setType       (nextStateVariableElement.getType());
                nextStateVariable.setDescription(nextStateVariableElement.getDescription());
                simEntityJaxbRoot.getStateVariable().add(nextStateVariable);
            }
            
            // TODO Events, StateTransition, Assignment
            
            // Already handled: CodeBlock
            
            // obsolete
//            List<String> clis = simEntityJaxbRoot.getComment();
//            clis.clear();
//            String cmt = nIe(graphMetadata.description);
//            if (cmt != null) {
//                clis.add(cmt.trim());
//            }

            marshaller.marshal(simEntityJaxbRoot, fileWriter);

            // OK, made it through the marshal, overwrite the "real" file
            Files.copy(tempFile.toPath(), currentFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            returnValue = true;
        }
        catch (JAXBException e) 
        {
            ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML I/O Error",
                    "Exception on JAXB marshalling" +
                    "\n" + modelFile.getName() +
                    "\n" + e.getMessage()
                    );
            returnValue = false;
        } 
        catch (IOException ex)
        {
            ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE,
                    "File I/O Error",
                    "Event graph file-save exception" +
                    "\n" + modelFile.getName() +
                    "\n" + ex.getMessage()
                    );
            returnValue = false;
        } 
        finally 
        {
            try 
            {
                if (fileWriter != null)
                    fileWriter.close();
            } 
            catch (IOException ioe) {
                LOG.error("{} saveModel() error closing FileWriter\n      {}\n{}", 
                        this.getClass().getSimpleName(), currentFile.toPath(), ioe);
            }
        }
        if (returnValue)
        {
            String modelModifiedStatus;
            if  (isModelDirty())
                 modelModifiedStatus =    "saved modified";
            else modelModifiedStatus = "re-saved unmodified";
            long bytesSaved = currentFile.length();
            LOG.info("{} Event Graph file, {} bytes\n      {}", modelModifiedStatus, bytesSaved, currentFile.getPath());
            setModelDirty(false);
        }
        else // returnValue == false, problem with saving or compilation
        {
            LOG.error("Event graph file not saved: {}", modelFile.getName());
        }
        return returnValue;
    }

    @Override
    public File getLastFile() {
        return currentFile;
    }

    private void buildEventsFromJaxb(List<Event> eventsList) {
        EventNode eventNode;
        for (Event nextEvent : eventsList) {
            eventNode = buildEventNodeFromJaxbEvent(nextEvent);
            buildEdgesFromJaxb(eventNode, nextEvent.getScheduleOrCancel());
        }
    }

    private EventNode buildEventNodeFromJaxbEvent(Event newEvent) 
    {
        EventNode eventNode = eventNodeCacheMap.get(newEvent);
        if (eventNode != null) {
            return eventNode;
        }
        eventNode = new EventNode(newEvent.getName());
        jaxbEventToEventNode(newEvent, eventNode);
        eventNode.opaqueModelObject = newEvent;

        eventNodeCacheMap.put(newEvent, eventNode);   // key = ev

        // Ensure a unique Event name for XML IDREFs
        if (!noDuplicateNamesCheck()) {
            mangleName(eventNode);
        }
        
        notifyChanged(new ModelEvent(eventNode, ModelEvent.EVENT_ADDED, "Event added"));
        return eventNode;
    }

    private String concatStrings(List<String> stringList) {
        StringBuilder sb = new StringBuilder();
        for (String s : stringList) {
            sb.append(s);
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    /** changes node name to unique value */
    private void mangleName(ViskitElement nodeElement) {
        do {
            nodeElement.setName(AssemblyModelImpl.mangleName(nodeElement.getName()));
        } while (!noDuplicateNamesCheck());
    }

    /** Check for duplicate names
     * @return true if no duplicate name found, false otherwise
     */
    private boolean noDuplicateNamesCheck() 
    {
        Set<String> nameHashSet = new HashSet<>(10);
        for (EventNode eventNode : eventNodeCacheMap.values()) 
        {
            if (!nameHashSet.add(eventNode.getName())) 
            {
                ViskitGlobals.instance().messageUser(JOptionPane.INFORMATION_MESSAGE,
                        "Duplicate Event Name",
                        "Duplicate event node name detected: " + eventNode.getName() +
                        "\nUnique name will be substituted.");
                return false;
            }
        }
        return true;
    }

    /** @return true if a simkit.Priority was found to have a numeric value */
    public boolean isNumericPriority() {
        return numericPriority;
    }

    public void setNumericPriority(boolean newValue) {
        numericPriority = newValue;
    }

    private boolean stateVariableParameterNameCheck() 
    {
        Set<String> stateVariableParameterHashSet = new HashSet<>(10);
        for (ViskitElement stateVariableParameterElement : stateVariableElements) 
        {
            if (!stateVariableParameterHashSet.add(stateVariableParameterElement.getName())) {
                return false;
            }
        }
        for (ViskitElement nextSimulationParameterElement : simulationParameterElements) 
        {
            if (!stateVariableParameterHashSet.add(nextSimulationParameterElement.getName())) {
                return false;
            }
        }
        return true; // all found OK
    }

    private void jaxbEventToEventNode(Event eventElement, EventNode eventNode) 
    {
        if (eventElement == null)
        {
            LOG.error("jaxbEventToEventNode() received null eventElement, no conversion possible");
            return;
        }
        if (eventNode == null)
        {
            LOG.error("jaxbEventToEventNode() received null eventNode, creating a new one");
            eventNode = new EventNode(eventElement.getName());
        }
        eventNode.setName(eventElement.getName());

//        eventNode.getComments().clear();
//        eventNode.getComments().addAll(event.getComment());
        eventNode.setDescription(eventNode.getDescription() + eventElement.getDescription());

        eventNode.getArguments().clear();

        EventArgument eventArgumentElement;
//        List<String> descriptionList;
        for (Argument nextArgument : eventElement.getArgument()) // returns a List
        {
            eventArgumentElement = new EventArgument();
            eventArgumentElement.setName(nextArgument.getName());
            eventArgumentElement.setType(nextArgument.getType());

//            descriptionList = new ArrayList<>();
//            descriptionList.addAll(arg.getComment());
//            eventArgument.setDescriptionArray(descriptionList);
            eventArgumentElement.setDescription(nextArgument.getDescription());
            eventArgumentElement.opaqueModelObject = nextArgument;
            eventNode.getArguments().add(eventArgumentElement);
        }

        eventNode.getLocalVariables().clear();
        EventLocalVariable eventLocalVariableElement;
        for (LocalVariable nextLocalVariableElement : eventElement.getLocalVariable()) 
        {
            if (!nextLocalVariableElement.getName().startsWith(privateIndexVariablePrefix)) // only if it's a "public" one
            {
                eventLocalVariableElement = new EventLocalVariable(nextLocalVariableElement.getName(), nextLocalVariableElement.getType(), nextLocalVariableElement.getValue());
//                eventLocalVariable.setDescription(concatStrings(localVariable.getComment())); // getComment() returns XML comment list
                eventLocalVariableElement.setDescription(nextLocalVariableElement.getDescription());
                eventLocalVariableElement.opaqueModelObject = nextLocalVariableElement;

                eventNode.getLocalVariables().add(eventLocalVariableElement);
            }
        }
        eventNode.setCodeBlockString(eventElement.getCode());
        eventNode.getStateTransitions().clear();

        EventStateTransition eventStateTransition;
        LocalVariableAssignment localVariableAssignmentElement;
        StateVariable stateVariableElement;
        LocalVariableInvocation localVariableInvocationElement;
        String index;
//      List<String> commentList;
        
        for (StateTransition stateTransition : eventElement.getStateTransition()) 
        {
            eventStateTransition = new EventStateTransition();

            localVariableAssignmentElement = stateTransition.getLocalVariableAssignment();
            if (localVariableAssignmentElement != null && localVariableAssignmentElement.getValue() != null && !localVariableAssignmentElement.getValue().isEmpty()) 
            {
                eventStateTransition.setLocalVariableAssignment(localVariableAssignmentElement.getValue());
            }

            stateVariableElement = (StateVariable) stateTransition.getState();
            eventStateTransition.setName(stateVariableElement.getName());
            eventStateTransition.setType(stateVariableElement.getType());

            if (ViskitGlobals.instance().isArray(stateVariableElement.getType())) {
                index = stateTransition.getIndex();
                eventStateTransition.setIndexingExpression(index);
            }
            
            // obsolete
//            commentList = new ArrayList<>(); // from XML
//            commentList.addAll(stateVariable.getComment());
////            eventStateTransition.setComments(comment);
//            eventStateTransition.setDescription(concatStrings(commentList));
            eventStateTransition.setDescription(stateVariableElement.getDescription());

            eventStateTransition.setOperation(stateTransition.getOperation() != null);
            if (eventStateTransition.isOperation()) 
            {
                eventStateTransition.setOperationOrAssignment(stateTransition.getOperation().getMethod());
            } 
            else {
                eventStateTransition.setOperationOrAssignment(stateTransition.getAssignment().getValue());
            }

            localVariableInvocationElement = stateTransition.getLocalVariableInvocation();
            if ((localVariableInvocationElement != null) && 
                (localVariableInvocationElement.getMethod() != null) && 
                !localVariableInvocationElement.getMethod().isEmpty())
                eventStateTransition.setLocalVariableInvocation(stateTransition.getLocalVariableInvocation().getMethod());

            eventStateTransition.opaqueModelObject = stateTransition;
            eventNode.getStateTransitions().add(eventStateTransition);
        }

        Coordinate coordinate = eventElement.getCoordinate();
        if (coordinate != null) //todo lose this after all xmls updated
        {
            eventNode.setPosition(new Point2D.Double(
                    Double.parseDouble(coordinate.getX()),
                    Double.parseDouble(coordinate.getY())));
        }
    }

    /** Schedule and cancel edges */
    private void buildEdgesFromJaxb(EventNode sourceEventNode, List<Object> objectList)
    {
        for (Object nextObject : objectList) 
        {
            if (nextObject instanceof Schedule)
            {
                buildSchedulingEdgeFromJaxb(sourceEventNode, (Schedule) nextObject);
            } 
            else {
                buildCancelingEdgeFromJaxb(sourceEventNode, (Cancel) nextObject);
            }
        }
    }

    private void buildSchedulingEdgeFromJaxb(EventNode sourceEventNode, Schedule scheduleElement) 
    {
        SchedulingEdge schedulingEdge = new SchedulingEdge();
        String s;
        schedulingEdge.opaqueModelObject = scheduleElement;

        schedulingEdge.setFrom(sourceEventNode);
        EventNode targetEventNode = buildEventNodeFromJaxbEvent((Event) scheduleElement.getEvent());
        schedulingEdge.setTo(targetEventNode);

        sourceEventNode.getConnections().add(schedulingEdge);
        targetEventNode.getConnections().add(schedulingEdge);
        schedulingEdge.setConditional(scheduleElement.getCondition());

        // Attempt to avoid NumberFormatException thrown on Double.parseDouble(String s)
        if (Pattern.matches(SchedulingEdge.FLOATING_POINT_REGEX, scheduleElement.getPriority())) 
        {
            s = scheduleElement.getPriority().trim();

            setNumericPriority(true);

            // We have a FP number
            // TODO: Deal with LOWEST or HIGHEST values containing exponents, i.e. (+/-) 1.06E8
            if (s.contains("-3")) {
                s = "LOWEST";
            } else if (s.contains("-2")) {
                s = "LOWER";
            } else if (s.contains("-1")) {
                s = "LOW";
            } else if (s.contains("1")) {
                s = "HIGH";
            } else if (s.contains("2")) {
                s = "HIGHER";
            } else if (s.contains("3")) {
                s = "HIGHEST";
            } else {
                s = "DEFAULT";
            }
        } 
        else {
            // We have an enumeration String
            s = scheduleElement.getPriority();
        }

        schedulingEdge.priority = s;

        // Now set the JAXB Schedule to record the Priority enumeration to overwrite
        // numeric Priority values
        scheduleElement.setPriority(schedulingEdge.priority);

        // obsolete
//        // convert XML Comment to description string
//        List<String> commentList = schedule.getComment();
//        if (!commentList.isEmpty()) {
//            StringBuilder sb = new StringBuilder();
//            for (String comment : commentList) {
//                sb.append(comment);
//                sb.append("  ");
//            }
//            schedulingEdge.conditionalDescription = sb.toString().trim();
//        }
        schedulingEdge.setDescription(ViskitStatics.emptyIfNull(scheduleElement.getDescription()));
        schedulingEdge.setDelay(scheduleElement.getDelay());
        schedulingEdge.setParameters(buildEdgeParametersFromJaxb(scheduleElement.getEdgeParameter()));

        edgeCacheMap.put(scheduleElement, schedulingEdge);

        setModelDirty(true);
        this.notifyChanged(new ModelEvent(schedulingEdge, ModelEvent.EDGE_ADDED, "Edge added"));
    }

    private void buildCancelingEdgeFromJaxb(EventNode sourceEventNode, Cancel cancelElement)
    {
        CancelingEdge cancelingEdge = new CancelingEdge();
        cancelingEdge.opaqueModelObject = cancelElement;
        cancelingEdge.setConditional(cancelElement.getCondition());

        // obsolete
//        List<String> cmt = cancel.getComment();
//        if (!cmt.isEmpty()) {
//            StringBuilder sb = new StringBuilder();
//            for (String comment : cmt) {
//                sb.append(comment);
//                sb.append("  ");
//            }
//            cancelingEdge.conditionalDescription = sb.toString().trim();
//        }
        cancelingEdge.setDescription(ViskitStatics.emptyIfNull(cancelElement.getDescription()));

        cancelingEdge.setParameters(buildEdgeParametersFromJaxb(cancelElement.getEdgeParameter()));

        cancelingEdge.setFrom(sourceEventNode);
        EventNode targetEventNode = buildEventNodeFromJaxbEvent((Event) cancelElement.getEvent());
        cancelingEdge.setTo(targetEventNode);

        sourceEventNode.getConnections().add(cancelingEdge);
        targetEventNode.getConnections().add(cancelingEdge);

        edgeCacheMap.put(cancelElement, cancelingEdge);

//        setModelDirty(true); // likely erroneous, not expecting dirty if loading from file using JAXB
        notifyChanged(new ModelEvent(cancelingEdge, ModelEvent.CANCELING_EDGE_ADDED, "Canceling edge added"));
    }

    private List<ViskitElement> buildEdgeParametersFromJaxb(List<EdgeParameter> edgeParameterElementList) 
    {
        List<ViskitElement> arrayList = new ArrayList<>(3);
        ViskitEdgeParameter edgeParameter;
        for (EdgeParameter nextEdgeParameter : edgeParameterElementList) {
            edgeParameter = new ViskitEdgeParameter(nextEdgeParameter.getValue());
            arrayList.add(edgeParameter);
        }
        return arrayList;
    }

    private void buildCodeBlockFromJaxb(String newCodeBlock) 
    {
        newCodeBlock = (newCodeBlock == null) ? "" : newCodeBlock;
        // TODO where is class variable??
        notifyChanged(new ModelEvent(newCodeBlock, ModelEvent.CODEBLOCK_CHANGED, "Code block changed"));
    }

    private void buildStateVariablesFromJaxb(List<StateVariable> stateVariableElementList) 
    {
        String c;
//        List<String> varCom;
        ViskitStateVariable stateVariable;
        for (StateVariable nextStateVariableElement : stateVariableElementList) 
        {
            // obsolete
//            varCom = nextStateVariable.getComment();
//            c = " ";
//            for (String comment : varCom) {
//                c += comment;
//                c += " ";
//            }
            String description = ViskitStatics.emptyIfNull(nextStateVariableElement.getDescription());
            stateVariable = new ViskitStateVariable(nextStateVariableElement.getName(), nextStateVariableElement.getType(), description.trim());
            stateVariable.opaqueModelObject = nextStateVariableElement;

            stateVariableElements.add(stateVariable);

            if (!stateVariableParameterNameCheck()) {
                mangleName(stateVariable);
            }
            notifyChanged(new ModelEvent(stateVariable, ModelEvent.STATE_VARIABLE_ADDED, "New state variable"));
        }
    }

    private void buildParametersFromJaxb(List<Parameter> parameterList) 
    {
//        List<String> pCom;
        String c;
        ViskitParameter parameter;
        for (Parameter nextParameter : parameterList) 
        {
            // obsolete
//            pCom = nextParameter.getComment();
//            c = " ";
//            for (String comment : pCom) {
//                c += comment;
//                c += " ";
//            }
            String description = ViskitStatics.emptyIfNull(nextParameter.getDescription());
            parameter = new ViskitParameter(nextParameter.getName(), nextParameter.getType(), description.trim());
            parameter.opaqueModelObject = nextParameter;

            simulationParameterElements.add(parameter);

            if (!stateVariableParameterNameCheck()) {
                mangleName(parameter);
            }
            notifyChanged(new ModelEvent(parameter, ModelEvent.SIM_PARAMETER_ADDED, "vParameter added"));
        }
    }

    @Override
    public Vector<ViskitElement> getAllNodes() {
        return new Vector<>(eventNodeCacheMap.values());
    }

    @Override
    public Vector<ViskitElement> getStateVariables() {
        return new Vector<>(stateVariableElements);
    }

    @Override
    public Vector<ViskitElement> getSimulationParameters() {
        return new Vector<>( simulationParameterElements);
    }

    // parameter mods
    // --------------
    @Override
    public void newSimulationParameter(String name, String type, String xinitVal, String description) 
    {
        ViskitParameter viskitParameter = new ViskitParameter(name, type, description);
        simulationParameterElements.add(viskitParameter);

        if (!stateVariableParameterNameCheck()) {
            mangleName(viskitParameter);
        }

        //parameter.setValue(initVal);
        Parameter parameter = this.jaxbEventGraphObjectFactory.createParameter();
        parameter.setName(ViskitStatics.nullIfEmpty(name));
        parameter.setType(ViskitStatics.nullIfEmpty(type));
//        parameter.getComment().add(description);
        parameter.setDescription(description);

        viskitParameter.opaqueModelObject = parameter;

        simEntityJaxbRoot.getParameter().add(parameter);

        setModelDirty(true);
        notifyChanged(new ModelEvent(viskitParameter, ModelEvent.SIM_PARAMETER_ADDED, "new simulation parameter"));
    }

    @Override
    public void deleteSimParameter(ViskitParameter vp) {
        // remove jaxb variable
        Iterator<Parameter> spItr = simEntityJaxbRoot.getParameter().iterator();
        while (spItr.hasNext()) {
            if (spItr.next() == (Parameter) vp.opaqueModelObject) {
                spItr.remove();
                break;
            }
        }
        simulationParameterElements.remove(vp);

        setModelDirty(true);
        notifyChanged(new ModelEvent(vp, ModelEvent.SIM_PARAMETER_DELETED, "vParameter deleted"));
    }

    @Override
    public void changeCodeBlock(String s) {
        simEntityJaxbRoot.setCode(s);
        setModelDirty(true);
    }

    @Override
    public boolean changeSimParameter(ViskitParameter newParameter) 
    {
        boolean returnValue = true;
        if (!stateVariableParameterNameCheck())
        {
            mangleName(newParameter);
            returnValue = false;
        }
        // fill out jaxb variable
        Parameter parameter = (Parameter) newParameter.opaqueModelObject;
        parameter.setName(ViskitStatics.nullIfEmpty(newParameter.getName()));
        //p.setShortName(vp.getName());
        parameter.setType(ViskitStatics.nullIfEmpty(newParameter.getType()));
        // obsolete
//        parameter.getComment().clear();
//        parameter.getComment().add(newParameter.getDescription());
        parameter.setDescription(newParameter.getDescription());

        setModelDirty(true);
        notifyChanged(new ModelEvent(newParameter, ModelEvent.SIM_PARAMETER_CHANGED, "vParameter changed"));
        return returnValue;
    }

    // State variable mods
    // -------------------
    @Override
    public void newStateVariable(String newName, String newType, String xinitVal, String newDescription) 
    {
        // TODO xinitVal unused?
        // get the new one here and show it around
        ViskitStateVariable newStateVariable = new ViskitStateVariable(newName, newType, newDescription);
        stateVariableElements.add(newStateVariable);
        if (!stateVariableParameterNameCheck()) {
            mangleName(newStateVariable);
        }
        StateVariable jaxbStateVariable = this.jaxbEventGraphObjectFactory.createStateVariable();
        jaxbStateVariable.setName(ViskitStatics.nullIfEmpty(newName));
        //s.setShortName(nIe(name));
        jaxbStateVariable.setType(ViskitStatics.nullIfEmpty(newType));
        jaxbStateVariable.setDescription(ViskitStatics.nullIfEmpty(newDescription));

        newStateVariable.opaqueModelObject = jaxbStateVariable;
        simEntityJaxbRoot.getStateVariable().add(jaxbStateVariable);

        setModelDirty(true);
        notifyChanged(new ModelEvent(newStateVariable, ModelEvent.STATE_VARIABLE_ADDED, "State variable added"));
    }

    @Override
    public void deleteStateVariable(ViskitStateVariable stateVariableToDelete) {
        // remove jaxb variable
        Iterator<StateVariable> stateVariableIterator = simEntityJaxbRoot.getStateVariable().iterator();
        while (stateVariableIterator.hasNext()) 
        {
            if (stateVariableIterator.next() == (StateVariable) stateVariableToDelete.opaqueModelObject) {
                stateVariableIterator.remove();
                break;
            }
        }
        stateVariableElements.remove(stateVariableToDelete);

        setModelDirty(true);
        notifyChanged(new ModelEvent(stateVariableToDelete, ModelEvent.STATE_VARIABLE_DELETED, "State variable deleted"));
    }

    @Override
    public boolean changeStateVariable(ViskitStateVariable newStateVariable) 
    {
        boolean returnValue = true;
        if (!stateVariableParameterNameCheck()) {
            mangleName(newStateVariable);
            returnValue = false;
        }
        // fill out jaxb variable
        StateVariable stateVariable = (StateVariable) newStateVariable.opaqueModelObject;
        stateVariable.setName(ViskitStatics.nullIfEmpty(newStateVariable.getName()));
        stateVariable.setType(ViskitStatics.nullIfEmpty(newStateVariable.getType()));
//        stateVariable.getComment().clear();
//        stateVariable.getComment().add(vsv.getDescription());
        stateVariable.setDescription(ViskitStatics.emptyIfNull(newStateVariable.getDescription()));

        setModelDirty(true);
        notifyChanged(new ModelEvent(newStateVariable, ModelEvent.STATE_VARIABLE_CHANGED, "State variable changed"));
        return returnValue;
    }

    // Event (node) mods
    // -----------------

    @Override
    public void newEventNode(String nodeName, Point2D p) 
    {
        nodeName = ViskitStatics.emptyIfNull(nodeName);
        EventNode node = new EventNode(nodeName);
        if (p == null) {
            p = new Point2D.Double(30, 60);
        }
        node.setPosition(p);

        // Ensure a unique Event name
        if (!noDuplicateNamesCheck()) 
        {
            mangleName(node);
        }
        Event jaxbEvent = jaxbEventGraphObjectFactory.createEvent();

        eventNodeCacheMap.put(jaxbEvent, node);   // key = ev

        jaxbEvent.setName(nodeName);

        if (nodeName.equals("Run"))
        {
                 node.setDescription(DEFAULT_RUN_EVENT_DESCRIPTION);
            jaxbEvent.setDescription(DEFAULT_RUN_EVENT_DESCRIPTION);
        }
        node.opaqueModelObject = jaxbEvent;
        simEntityJaxbRoot.getEvent().add(jaxbEvent);

        setModelDirty(true);
        notifyChanged(new ModelEvent(node, ModelEvent.EVENT_ADDED, "Event added"));
    }

    @Override
    public void redoEvent(EventNode node) {
        if (eventNodeCacheMap.containsValue(node))
            return;

        Event jaxbEv = jaxbEventGraphObjectFactory.createEvent();
        eventNodeCacheMap.put(jaxbEv, node);   // key = evnode.opaqueModelObject = jaxbEv;
        jaxbEv.setName(node.getName());
        node.opaqueModelObject = jaxbEv;
        simEntityJaxbRoot.getEvent().add(jaxbEv);

        setModelDirty(true);
        notifyChanged(new ModelEvent(node, ModelEvent.REDO_EVENT_NODE, "Event Node redone"));
    }

    @Override
    public void deleteEvent(EventNode node) {
        Event jaxbEv = (Event) node.opaqueModelObject;
        eventNodeCacheMap.remove(jaxbEv);
        simEntityJaxbRoot.getEvent().remove(jaxbEv);

        setModelDirty(true);
        if (!eventGraphController.isUndo())
            notifyChanged(new ModelEvent(node, ModelEvent.EVENT_DELETED, "Event deleted"));
        else
            notifyChanged(new ModelEvent(node, ModelEvent.UNDO_EVENT_NODE, "Event undone"));
    }

    private StateVariable findStateVariable(String nm) {
        List<StateVariable> lis = simEntityJaxbRoot.getStateVariable();
        for (StateVariable sv : lis) {
            if (sv.getName().equals(nm)) {
                return sv;
            }
        }
        return null;
    }
    private int localVariableNameSequence = 0;

    @Override
    public String generateLocalVariableName() {
        String newName = null;
        do {
            newName = privateLocalVariablePrefix + localVariableNameSequence++;
        } while (!isUniqueLocalVariableIndexName(newName));
        return newName;
    }

    @Override
    public void resetLocalVariableNameGenerator() {
        localVariableNameSequence = 0;
    }
    private int indexVariableNameSequence = 0;

    @Override
    public String generateIndexVariableName() {
        String newName = null;
        do {
            newName = privateIndexVariablePrefix + indexVariableNameSequence++;
        } while (!isUniqueLocalVariableIndexName(newName));
        return newName;
    }

    @Override
    public void resetIndexNameGenerator() {
        indexVariableNameSequence = 0;
    }

    private boolean isUniqueLocalVariableIndexName(String newName) 
    {
        String indexingExpression;
        for (EventNode event : eventNodeCacheMap.values())
        {
            for (ViskitElement localVariableElement : event.getLocalVariables()) 
            {
                if (localVariableElement.getName().equals(newName)) {
                    return false;
                }
            }
            for (ViskitElement nextStateTransitionElement : event.getStateTransitions()) 
            {
                indexingExpression = nextStateTransitionElement.getIndexingExpression();
                if (indexingExpression != null && indexingExpression.equals(newName)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Check if new state variable name is unique
     * @param newName to check
     * @return whether unique
     */
    private boolean isUniqueStateVariableName(String newName)
    {
        for (ViskitElement stateVariableElement : stateVariableElements) 
        {
            if (stateVariableElement.getName().equals(newName)) {
                return false;
            }
        }
        return true; // unique
    }

    @Override
    public String generateStateVariableName() 
    {
        String newName = null;
        int startIndex = 0;
        do {
            newName = stateVariablePrefix + startIndex++;
        } while (!isUniqueStateVariableName(newName));
        return newName;
    }

    /**
     * Here we convert local state transition expressions into JAXB bindings
     *
     * @param targetStateTransitionList List of StateTransitions to populate
     * @param localStateTransitionList List of StateTransitions to transfer to the target
     */
    private void cloneTransitions(List<StateTransition> targetStateTransitionList, List<ViskitElement> localStateTransitionList) 
    {
        targetStateTransitionList.clear();
        StateTransition stateTransition;
        String localVariableAssignmentString, assignmentString, localVariableInvocationString, invocationString;
        LocalVariableAssignment localVariableAssignment;
        StateVariable stateVariable;
        Operation operation;
        Assignment assignment;
        LocalVariableInvocation localVariableInvocation;
        
        for (ViskitElement stateTransitionElement : localStateTransitionList)
        {
            stateTransition = jaxbEventGraphObjectFactory.createStateTransition();

            // Various locally declared variable ops
            localVariableAssignmentString = ((EventStateTransition)stateTransitionElement).getLocalVariableAssignment();
            if (localVariableAssignmentString != null && !localVariableAssignmentString.isEmpty()) {

                assignmentString = ((EventStateTransition)stateTransitionElement).getLocalVariableAssignment();
                if (assignmentString != null && !assignmentString.isEmpty()) {
                    localVariableAssignment = jaxbEventGraphObjectFactory.createLocalVariableAssignment();
                    localVariableAssignment.setValue(assignmentString);
                    stateTransition.setLocalVariableAssignment(localVariableAssignment);
                }
            }
            stateVariable = findStateVariable(stateTransitionElement.getName());

            if (stateVariable == null) {continue;}

            stateTransition.setState(stateVariable);

            if (stateVariable.getType() != null && ViskitGlobals.instance().isArray(stateVariable.getType()))
            {
                // Match the state transition's index to the given index
                stateTransition.setIndex(stateTransitionElement.getIndexingExpression());
            }

            // Not needed.  Duplicative, gives StateTransition same description as StateVariable
//            if  (stateTransition.getDescription() != null)
//            {
//                 stateTransition.setDescription(stateTransitionElement.getDescription());
//            }
//            else stateTransition.setDescription("");

            if (stateTransitionElement.isOperation()) 
            {
                operation = jaxbEventGraphObjectFactory.createOperation();
                operation.setMethod(stateTransitionElement.getOperationOrAssignment());
                stateTransition.setOperation(operation);
            } 
            else {
                assignment = jaxbEventGraphObjectFactory.createAssignment();
                assignment.setValue(stateTransitionElement.getOperationOrAssignment());
                stateTransition.setAssignment(assignment);
            }

            // If we have any void return type, zero parameter methods to
            // call on local vars, or args, do it now
            localVariableInvocationString = ((EventStateTransition)stateTransitionElement).getLocalVariableInvocation();
            if (localVariableInvocationString != null && !localVariableInvocationString.isEmpty())
            {
                invocationString = ((EventStateTransition) stateTransitionElement).getLocalVariableInvocation();
                if (invocationString != null && !invocationString.isEmpty()) {
                    localVariableInvocation = jaxbEventGraphObjectFactory.createLocalVariableInvocation();
                    localVariableInvocation.setMethod(invocationString);
                    stateTransition.setLocalVariableInvocation(localVariableInvocation);
                }
            }
            stateTransitionElement.opaqueModelObject = stateTransition; //replace
            targetStateTransitionList.add(stateTransition);
        }
    }

    // obsolete
//    private void cloneDescription(List<String> targetDescriptionList, List<String> localDescriptionList) {
//        targetDescriptionList.clear();
//        targetDescriptionList.addAll(localDescriptionList);
//    }

    private void cloneDescription(String clonedDescription, String originalLocalDescription) {
        clonedDescription = originalLocalDescription;
    }

    private void cloneArguments(List<Argument> clonedEventArgumentList, List<ViskitElement> originalEventArgumentList) 
    {
        clonedEventArgumentList.clear();
        Argument tempArgumentElement;
        for (ViskitElement eventArgumentElement : originalEventArgumentList) 
        {
            tempArgumentElement = jaxbEventGraphObjectFactory.createArgument();
            tempArgumentElement.setName(ViskitStatics.nullIfEmpty(eventArgumentElement.getName()));
            tempArgumentElement.setType(ViskitStatics.nullIfEmpty(eventArgumentElement.getType()));
//            argument.getComment().clear();
//            argument.getComment().addAll(eventArgumentElement.getDescriptionArray());
            tempArgumentElement.setDescription(eventArgumentElement.getDescription());
            eventArgumentElement.opaqueModelObject = tempArgumentElement; // replace
            clonedEventArgumentList.add(tempArgumentElement);
        }
    }

    private void cloneLocalVariables(List<LocalVariable> clonedLocalVariableList, List<ViskitElement> originalLocalVariableList) 
    {
        clonedLocalVariableList.clear();
        LocalVariable tempLocalVariable;
        for (ViskitElement newLocalVariable : originalLocalVariableList) 
        {
            tempLocalVariable = jaxbEventGraphObjectFactory.createLocalVariable();
            tempLocalVariable.setName(ViskitStatics.nullIfEmpty(newLocalVariable.getName()));
            tempLocalVariable.setType(ViskitStatics.nullIfEmpty(newLocalVariable.getType()));
            tempLocalVariable.setValue(ViskitStatics.nullIfEmpty(newLocalVariable.getValue()));
//            tempLocalVariable.getComment().clear();
//            tempLocalVariable.getComment().add(newLocalVariable.getDescription());
            tempLocalVariable.setDescription(newLocalVariable.getDescription());
            newLocalVariable.opaqueModelObject = tempLocalVariable; //replace
            clonedLocalVariableList.add(tempLocalVariable);
        }
    }

    @Override
    public boolean changeEventNode(EventNode eventNode) 
    {
        boolean returnValue = true;

        // Ensure a unique Event name
        if (!noDuplicateNamesCheck()) 
        {
            mangleName(eventNode);
            returnValue = false;
        }
        Event jaxbEvent = (Event) eventNode.opaqueModelObject;

        jaxbEvent.setName(eventNode.getName());

        double x = eventNode.getPosition().getX();
        double y = eventNode.getPosition().getY();
        Coordinate coordinate = jaxbEventGraphObjectFactory.createCoordinate();
        coordinate.setX(String.valueOf(x));
        coordinate.setY(String.valueOf(y));
        eventNode.getPosition().setLocation(x, y);
        jaxbEvent.setCoordinate(coordinate);

        cloneDescription(jaxbEvent.getDescription(), eventNode.getDescription());
        cloneArguments(jaxbEvent.getArgument(), eventNode.getArguments());
        cloneLocalVariables(jaxbEvent.getLocalVariable(), eventNode.getLocalVariables());
        // following must follow above
        cloneTransitions(jaxbEvent.getStateTransition(), eventNode.getStateTransitions());

        jaxbEvent.setCode(eventNode.getCodeBlockString());

        setModelDirty(true);
        notifyChanged(new ModelEvent(eventNode, ModelEvent.EVENT_CHANGED, "Event changed"));
        return returnValue;
    }

    // Edge mods
    // ---------

    @Override
    public void newSchedulingEdge(EventNode sourceEventNode, EventNode targetEventNode) 
    {
        SchedulingEdge schedulingEdge = new SchedulingEdge();
        schedulingEdge.setFrom(sourceEventNode);
        schedulingEdge.setTo(targetEventNode);
        sourceEventNode.getConnections().add(schedulingEdge);
        targetEventNode.getConnections().add(schedulingEdge);

        Schedule schedule = jaxbEventGraphObjectFactory.createSchedule();

        schedulingEdge.opaqueModelObject = schedule;
        Event targetEvent = (Event) targetEventNode.opaqueModelObject;
        schedule.setEvent(targetEvent);
        Event sourceEvent = (Event) sourceEventNode.opaqueModelObject;
        sourceEvent.getScheduleOrCancel().add(schedule);

        // Put in dummy edge parameters to match the target arguments
        List<ViskitElement> argumentList = targetEventNode.getArguments();
        List<ViskitElement> edgeParameters;
        if (!argumentList.isEmpty()) 
        {
            edgeParameters = new ArrayList<>(argumentList.size());
            for (ViskitElement argument : argumentList)
            {
                edgeParameters.add(new ViskitEdgeParameter(argument.getValue()));
            }
            schedulingEdge.setParameters(edgeParameters);
        }
        schedulingEdge.priority = "DEFAULT";  // set default priority

        edgeCacheMap.put(schedule, schedulingEdge);

        setModelDirty(true);
        notifyChanged(new ModelEvent(schedulingEdge, ModelEvent.EDGE_ADDED, "Scheduling Edge added"));
    }

    @Override
    public void redoSchedulingEdge(Edge edge) {
        if (edgeCacheMap.containsValue(edge))
            return;

        EventNode sourceEventNode, targetEventNode;
        sourceEventNode = (EventNode) ((DefaultMutableTreeNode) edge.getFrom().opaqueViewObject).getUserObject();
        targetEventNode = (EventNode) ((DefaultMutableTreeNode) edge.getTo().opaqueViewObject).getUserObject();
        Schedule schedule = jaxbEventGraphObjectFactory.createSchedule();
        edge.opaqueModelObject = schedule;
        Event targetEvent = (Event) targetEventNode.opaqueModelObject;
        schedule.setEvent(targetEvent);
        Event sourceEvent = (Event) sourceEventNode.opaqueModelObject;
        sourceEvent.getScheduleOrCancel().add(schedule);
        edgeCacheMap.put(schedule, edge);

        setModelDirty(true);
        notifyChanged(new ModelEvent(edge, ModelEvent.REDO_SCHEDULING_EDGE, "Scheduling Edge redone"));
    }

    @Override
    public void newCancelingEdge(EventNode sourceEventNode, EventNode targetEventNode) 
    {
        CancelingEdge cancelingEdge = new CancelingEdge();
        cancelingEdge.setFrom(sourceEventNode);
        cancelingEdge.setTo(targetEventNode);
        sourceEventNode.getConnections().add(cancelingEdge);
        targetEventNode.getConnections().add(cancelingEdge);

        Cancel cancel = jaxbEventGraphObjectFactory.createCancel();

        cancelingEdge.opaqueModelObject = cancel;
        Event targetEvent = (Event) targetEventNode.opaqueModelObject;
        cancel.setEvent(targetEvent);
        Event sourceEvent = (Event) sourceEventNode.opaqueModelObject;
        sourceEvent.getScheduleOrCancel().add(cancel);

        // Put in dummy edge parameters to match the target arguments
        List<ViskitElement> argumentList = targetEventNode.getArguments();
        List<ViskitElement> edgeParameters;
        if (!argumentList.isEmpty()) {
            edgeParameters = new ArrayList<>(argumentList.size());
            for (ViskitElement argument : argumentList) {
                edgeParameters.add(new ViskitEdgeParameter(argument.getValue()));
            }
            cancelingEdge.setParameters(edgeParameters);
        }
        edgeCacheMap.put(cancel, cancelingEdge);

        setModelDirty(true);
        notifyChanged(new ModelEvent(cancelingEdge, ModelEvent.CANCELING_EDGE_ADDED, "Canceling Edge added"));
    }

    @Override
    public void redoCancelingEdge(Edge edge) 
    {
        if (edgeCacheMap.containsValue(edge))
            return;

        EventNode sourceEventNode, targetEventNode;
        sourceEventNode = (EventNode) ((DefaultMutableTreeNode) edge.getFrom().opaqueViewObject).getUserObject();
        targetEventNode = (EventNode) ((DefaultMutableTreeNode) edge.getTo().opaqueViewObject).getUserObject();
        Cancel cancel = jaxbEventGraphObjectFactory.createCancel();
        edge.opaqueModelObject = cancel;
        Event targetEvent = (Event) targetEventNode.opaqueModelObject;
        cancel.setEvent(targetEvent);
        Event sourceEveny = (Event) sourceEventNode.opaqueModelObject;
        sourceEveny.getScheduleOrCancel().add(cancel);
        edgeCacheMap.put(cancel, edge);

        setModelDirty(true);
        notifyChanged(new ModelEvent(edge, ModelEvent.REDO_CANCELING_EDGE, "Canceling Edge redone"));
    }

    @Override
    public void deleteSchedulingEdge(Edge edge) 
    {
        _commonEdgeDelete(edge);

        if (!eventGraphController.isUndo())
            notifyChanged(new ModelEvent(edge, ModelEvent.EDGE_DELETED, "Edge deleted"));
        else
            notifyChanged(new ModelEvent(edge, ModelEvent.UNDO_SCHEDULING_EDGE, "Edge undone"));
    }

    @Override
    public void deleteCancelingEdge(Edge edge)
    {
        _commonEdgeDelete(edge);

        if (!eventGraphController.isUndo())
            notifyChanged(new ModelEvent(edge, ModelEvent.CANCELING_EDGE_DELETED, "Canceling edge deleted"));
        else
            notifyChanged(new ModelEvent(edge, ModelEvent.UNDO_CANCELING_EDGE, "Canceling edge undone"));
    }

    private void _commonEdgeDelete(Edge edge) 
    {
        Object jaxbEdge = edge.opaqueModelObject;

        List<Event> nodeList = simEntityJaxbRoot.getEvent();
        List<Object> edges;
        for (Event nextEvent : nodeList) {
            edges = nextEvent.getScheduleOrCancel();
            edges.remove(jaxbEdge);
        }
        edgeCacheMap.remove(edge);
        setModelDirty(true);
    }

    @Override
    public void changeSchedulingEdge(Edge schedulingEdge) 
    {
        Schedule schedule = (Schedule) schedulingEdge.opaqueModelObject;
        schedule.setCondition(schedulingEdge.getConditional());
//        schedule.getComment().clear();
//        schedule.getComment().add(schedulingEdge.conditionalDescription);
        schedule.setDescription(schedulingEdge.getDescription());
        schedule.setDelay(String.valueOf(schedulingEdge.getDelay()));

        schedule.setEvent(schedulingEdge.getTo().opaqueModelObject);
        schedule.setPriority(((SchedulingEdge)schedulingEdge).priority);
        schedule.getEdgeParameter().clear();

        EdgeParameter edgeParameter;
        // Bug 1373: This is where an edge parameter gets written out to XML
        for (ViskitElement nextEdgeParameter : schedulingEdge.getParameters())
        {
            edgeParameter = jaxbEventGraphObjectFactory.createEdgeParameter();
            edgeParameter.setValue(ViskitStatics.nullIfEmpty(nextEdgeParameter.getValue()));
            schedule.getEdgeParameter().add(edgeParameter);
        }
        setModelDirty(true);
        notifyChanged(new ModelEvent(schedulingEdge, ModelEvent.EDGE_CHANGED, "Edge changed"));
    }

    @Override
    public void changeCancelingEdge(Edge newCancelingEdge )
    {
        Cancel cancel = (Cancel) newCancelingEdge.opaqueModelObject;
        cancel.setCondition(newCancelingEdge.getConditional());
        cancel.setEvent(newCancelingEdge.getTo().opaqueModelObject);
//        cancel.getComment().clear();
//        cancel.getComment().add(newCancellingEdge.conditionalDescription);
        cancel.setDescription(newCancelingEdge.getConditionalDescription());

        cancel.getEdgeParameter().clear();
        EdgeParameter edgeParameter;
        for (ViskitElement nextEdgeParameter : newCancelingEdge.getParameters()) {
            edgeParameter = jaxbEventGraphObjectFactory.createEdgeParameter();
            edgeParameter.setValue(ViskitStatics.nullIfEmpty(nextEdgeParameter.getValue()));
            cancel.getEdgeParameter().add(edgeParameter);
        }
        setModelDirty(true);
        notifyChanged(new ModelEvent(newCancelingEdge, ModelEvent.CANCELING_EDGE_CHANGED, "Canceling edge changed"));
    }

} // end class file ModelImpl.java
