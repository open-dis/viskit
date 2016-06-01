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
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import org.apache.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.control.EventGraphControllerImpl;
import viskit.mvc.mvcAbstractModel;
import viskit.mvc.mvcController;
import viskit.util.XMLValidationTool;
import viskit.xsd.bindings.eventgraph.*;
import viskit.xsd.translator.assembly.SimkitAssemblyXML2Java;
import viskit.xsd.translator.eventgraph.SimkitEventGraphXML2Java;

/**
 * <p>
 This is the "master" model of an event graph.  It should control the node and
 edge XML (JAXB) information.  What hasn't been done is toEventNode put in accessor
 methods for the view toEventNode read pieces that it needs, say after it receives a
 "new model" event.
 </p>
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
public class EventGraphModelImpl extends mvcAbstractModel implements EventGraphModel
{
    static final Logger LOG = LogUtilities.getLogger(EventGraphModelImpl.class);

    JAXBContext   jaxbContext;
    ObjectFactory jaxbObjectFactory;
    SimEntity     jaxbSimEntity;
    File          currentFile;
    Map<Event, EventNode> eventGraphNodeCacheMap= new HashMap<>();
    Map<Object, Edge>     edgeCache             = new HashMap<>();
	
    Vector<ViskitElement> stateTransitions      = new Vector<>(); // TODO
    Vector<ViskitStateVariable> stateVariables  = new Vector<>();
    Vector<ViskitElement> simulationParameters  = new Vector<>();
    private final String  schemaLocation        = XMLValidationTool.EVENT_GRAPH_SCHEMA;
    private final String  indexVariablePrefix   = "_indexVariable_";
    private final String  localVariablePrefix   = "localVariable_";
    private final String  stateVariablePrefix   = "stateVariable_";
    private GraphMetadata graphMetadata;
    private final EventGraphControllerImpl eventGraphController;
    private boolean modelDirty = false;
    private boolean hasNumericPriority;

    public EventGraphModelImpl(mvcController controller)
	{
        this.eventGraphController = (EventGraphControllerImpl) controller;
        graphMetadata = new GraphMetadata(this);
		if ((graphMetadata.description == null) || graphMetadata.description.trim().isEmpty())
			 graphMetadata.description = ViskitStatics.DEFAULT_DESCRIPTION;
    }

    @Override
    public void initialize()
	{
        try {
            jaxbContext       = JAXBContext.newInstance(SimkitEventGraphXML2Java.EVENT_GRAPH_BINDINGS);
            jaxbObjectFactory = new ObjectFactory();
            jaxbSimEntity     = jaxbObjectFactory.createSimEntity(); // toEventNode start with empty graph
        }
		catch (JAXBException e)
		{
            eventGraphController.messageToUser(JOptionPane.ERROR_MESSAGE,
                    "XML Error",
                    "Exception on JAXBContext instantiation" +
                    "\n" + e.getMessage()
                    );
            LOG.error("Exception on JAXBContext instantiation", e);
        }
    }

    @Override
    public boolean isDirty() {
        return modelDirty;
    }

    @Override
    public void setDirty(boolean dirt) {
        modelDirty = dirt;
    }

    @Override
    public GraphMetadata getMetadata() {
        return graphMetadata;
    }

    @Override
    public void setMetadata(GraphMetadata graphMetadata)
	{
        this.graphMetadata = graphMetadata;
        setDirty(true);
        notifyChanged(new ModelEvent(graphMetadata, ModelEvent.METADATA_CHANGED, "Metadata changed"));
    }

    @Override
    public boolean newModel(File file)
	{
              stateVariables.removeAllElements();
        simulationParameters.removeAllElements();
              eventGraphNodeCacheMap.clear();
                   edgeCache.clear();
        this.notifyChanged(new ModelEvent(this, ModelEvent.NEWMODEL, "New event graph model"));

        if (file == null)
		{
            jaxbSimEntity = jaxbObjectFactory.createSimEntity(); // toEventNode start with empty event graph
        } 
		else
		{
            try {
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                jaxbSimEntity = (SimEntity) unmarshaller.unmarshal(file);

                GraphMetadata newMetadata = new GraphMetadata(this);
                newMetadata.author                = jaxbSimEntity.getAuthor();
                newMetadata.created               = jaxbSimEntity.getCreated();
                newMetadata.revision              = jaxbSimEntity.getVersion();
                newMetadata.name                  = jaxbSimEntity.getName();
                newMetadata.packageName           = jaxbSimEntity.getPackage();
                newMetadata.extendsPackageName    = jaxbSimEntity.getExtend();
                newMetadata.implementsPackageName = jaxbSimEntity.getImplement();
				// description follows below
				
                // move <Comment> elements to EventNode description attribute
				List<String> commentList = jaxbSimEntity.getComment();
                StringBuilder sb = new StringBuilder();
                for (String comment : commentList) {
                    sb.append(" ");
                    sb.append(comment);
                }
				if (sb.toString().length() > 0)
				{
					newMetadata.description = (sb.toString()).trim(); // copy over then delete
					jaxbSimEntity.getComment().clear();
				}
				else 
				{
					String newDescription = new String();
					if (jaxbSimEntity.getDescription() != null) // empty XML attribute is null
						newDescription = jaxbSimEntity.getDescription();
					newMetadata.description = newDescription.trim();
				}
				if ((newMetadata.description == null) || newMetadata.description.trim().isEmpty())
					 newMetadata.description = ViskitStatics.DEFAULT_DESCRIPTION;
                setMetadata(newMetadata);

                        buildEventsFromJaxb(jaxbSimEntity.getEvent());
                    buildParametersFromJaxb(jaxbSimEntity.getParameter());
                buildStateVariablesFromJaxb(jaxbSimEntity.getStateVariable());
                     buildCodeBlockFromJaxb(jaxbSimEntity.getCode()); // cannot rename jaxb method name without modifying simkit.xsd assembly.xsd schemas
            } 
			catch (JAXBException ee)
			{
                // want a clear way toEventNode know if they're trying toEventNode load an assembly vs. some unspecified XML.
                try {
                    JAXBContext assemblyJaxbContext = JAXBContext.newInstance(SimkitAssemblyXML2Java.ASSEMBLY_BINDINGS);
                    Unmarshaller unmarshaller = assemblyJaxbContext.createUnmarshaller();
                    unmarshaller.unmarshal(file);
                    // If we get here, likely the user has tried to load an assembly as an EventNode.
                    eventGraphController.messageToUser(JOptionPane.ERROR_MESSAGE,
                            "Wrong File Type", // TODO confirm
                            "File is not an Event Graph." + "\n" + 
                            "Use the assembly editor to work with this file."
                            );
                } 
				catch (JAXBException e)
				{
                    eventGraphController.messageToUser(JOptionPane.ERROR_MESSAGE,
                            "XML validation error",
                            "XML validation error: exception on JAXB unmarshalling of" +
                            "\n" + file.getName() +
                            "\n\nError is: " + e.getMessage() +
                            "\nin EventGraphModelImpl.newModel(File)"
                            );
					LOG.error ("JAXB exception, likely XML validation error: " + e.getMessage(), e);
                }
				LOG.error ("JAXB exception, likely incorrect XML file type error: " + ee.getMessage(), ee);
                return false;    // fromEventNode either error case
            }
        }
        currentFile = file;
		runEventStateTransitionsUpdate ();

        setDirty(false); // required for initial file loading
        return true;
    }

    @Override
    public boolean saveModel(File file)
	{
        boolean returnValue;
        if (file == null) {
            file = currentFile;
        }
		else
		{
			// TODO any handling needed?
		}
		
        // Do the marshalling into a temporary file, so as toEventNode avoid possible
        // deletion of existing file on a marshal error.

        File tempFile;
        FileWriter fileWriter = null;
        try {
            tempFile = TempFileManager.createTempFile("tempEventGraphMarshalOutput", ".xml");
        } 
		catch (IOException e)
		{
            eventGraphController.messageToUser(JOptionPane.ERROR_MESSAGE,
                    "Input/Output (I/O) Error",
                    "Exception creating temporary file tempEventGraphMarshalOutput.xml, EventGraphModelImpl.saveModel():" +
                    "\n" + e.getMessage()
                    );
            return false;
        }

        try {
            fileWriter = new FileWriter(tempFile);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			// https://dersteps.wordpress.com/2012/08/22/enable-jaxb-event-handling-to-catch-errors-as-they-happen
			jaxbMarshaller.setEventHandler(new ValidationEventHandler() 
			{
				@Override
				public boolean handleEvent(ValidationEvent validationEvent)
				{
					LOG.error("Marshaller event handler says: " + validationEvent.getMessage() + 
									   " (Exception: " + validationEvent.getLinkedException() + ")");
					return false;
				}
			});
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, schemaLocation);

            jaxbSimEntity.setName       (ViskitGlobals.nullIfEmpty(graphMetadata.name));
            jaxbSimEntity.setCreated    (ViskitGlobals.nullIfEmpty(graphMetadata.created));
            jaxbSimEntity.setVersion    (ViskitGlobals.nullIfEmpty(graphMetadata.revision));
            jaxbSimEntity.setAuthor     (ViskitGlobals.nullIfEmpty(graphMetadata.author));
            jaxbSimEntity.setPackage    (ViskitGlobals.nullIfEmpty(graphMetadata.packageName));
            jaxbSimEntity.setExtend     (ViskitGlobals.nullIfEmpty(graphMetadata.extendsPackageName));
            jaxbSimEntity.setImplement  (ViskitGlobals.nullIfEmpty(graphMetadata.implementsPackageName));
			jaxbSimEntity.setDescription(ViskitGlobals.nullIfEmpty(graphMetadata.description));

            jaxbMarshaller.marshal(jaxbSimEntity, fileWriter);

            // OK, made it through the marshal, overwrite the "real" file
            Files.copy(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

            setDirty(false);
            currentFile = file;
            returnValue = true;
        } 
		catch (JAXBException e)
		{
            eventGraphController.messageToUser(JOptionPane.ERROR_MESSAGE,
                    "XML Input/Output Error",
                    "Exception on JAXB marshalling" +
                    "\n" + file.getName() +
                    "\n" + e.getMessage()
                    );
            returnValue = false;
            LOG.error("Exception on JAXB marshalling for " + file.getName(), e);
        } 
		catch (IOException ex)
		{
            eventGraphController.messageToUser(JOptionPane.ERROR_MESSAGE,
                    "File Input/Output Error",
                    "Exception on writing " + file.getName() +
                    "\n" + ex.getMessage()
                    );
            returnValue = false;
            LOG.error("Exception on writing " + file.getName() , ex);
        } 
		finally {
            try {
                if (fileWriter != null)
                    fileWriter.close();
            } 
			catch (IOException ioe) {} // ignore
        }
        return returnValue;
    }

    @Override
    public File getCurrentFile() {
        return currentFile;
    }

    private void buildEventsFromJaxb(List<Event> jaxbEventList)
	{
        for (Event jaxbEvent : jaxbEventList)
		{
            EventNode eventNode = buildEventNodeFromJaxbEvent(jaxbEvent);
            buildEdgesFromJaxb(eventNode, jaxbEvent.getScheduleOrCancel());
        }
    }

    private EventNode buildEventNodeFromJaxbEvent(Event jaxbEvent)
	{
        EventNode eventNode = eventGraphNodeCacheMap.get(jaxbEvent);
        if (eventNode != null) {
            return eventNode;
        }
        eventNode = new EventNode(jaxbEvent.getName());
        jaxbEventToEventNode(jaxbEvent, eventNode);
        eventNode.opaqueModelObject = jaxbEvent;

        eventGraphNodeCacheMap.put(jaxbEvent, eventNode);   // key = event

        // Ensure a unique Event name for XML IDREFs
        if (!nameCheck()) {
            mangleName(eventNode);
        }
        notifyChanged(new ModelEvent(eventNode, ModelEvent.EVENT_ADDED, "Event added: " + eventNode.name));

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
    private final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private String _fourHexDigits(int i) {
        char[] ca = new char[4];
        for (int j = 3; j >= 0; j--) {
            int idx = i & 0xF;
            i >>= 4;
            ca[j] = hexDigits[idx];
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

    private void mangleName(ViskitElement node)
	{
        do
		{
            node.setName(EventGraphModelImpl.this.mangleName(node.getName()));
        }
		while (!nameCheck());
    }

    private boolean nameCheck()
	{
        Set<String> stringHashSet = new HashSet<>(10);
        for (EventNode eventNode : eventGraphNodeCacheMap.values())
		{
            if (!stringHashSet.add(eventNode.getName()))
			{
                eventGraphController.messageToUser(JOptionPane.INFORMATION_MESSAGE,
                        "Duplicate Event Name",
                        "Duplicate event name detected: " + eventNode.getName() +
                        "\n\nUnique name will be substituted.");
                return false;
            }
        }
        return true;
    }

    /** @return true if a simkit.Priority was found toEventNode have a numeric value */
    public boolean isNumericPriority() {
        return hasNumericPriority;
    }

    public void setNumericPriority(boolean b) {
        hasNumericPriority = b;
    }

    private boolean stateVariableParameterNameCheck()
	{
        Set<String> stringHashSet = new HashSet<>(10);
        for (ViskitElement stateVariable : stateVariables)
		{
            if (!stringHashSet.add(stateVariable.getName())) {
                return false;
            }
        }
        for (ViskitElement simulationParameter : simulationParameters)
		{
            if (!stringHashSet.add(simulationParameter.getName())) {
                return false;
            }
        }
        return true;
    }

    private void jaxbEventToEventNode(Event jaxbEvent, EventNode eventNode)
	{
        eventNode.setName       (jaxbEvent.getName());
        eventNode.setDescription(jaxbEvent.getDescription());

        eventNode.getArguments().clear();
        for (Argument jaxbArgument : jaxbEvent.getArgument())
		{
            EventArgument eventArgument = new EventArgument();
            eventArgument.setName(jaxbArgument.getName());
            eventArgument.setType(jaxbArgument.getType());

            eventArgument.setDescription(jaxbArgument.getDescription());
            eventArgument.opaqueModelObject = jaxbArgument;
            eventNode.getArguments().add(eventArgument);
        }

        eventNode.getLocalVariables().clear();
        for (LocalVariable localVariable : jaxbEvent.getLocalVariable())
		{
            if (!localVariable.getName().startsWith(indexVariablePrefix)) {    // only if it's a "public" one
                EventLocalVariable eventLocalVariable = new EventLocalVariable(
                        localVariable.getName(), localVariable.getType(), localVariable.getValue());
                eventLocalVariable.setDescription(localVariable.getDescription());
                eventLocalVariable.opaqueModelObject = localVariable;

                eventNode.getLocalVariables().add(eventLocalVariable);
            }
        }

        eventNode.setCodeBLock(jaxbEvent.getCode()); // cannot rename jaxb method name without modifying simkit.xsd assembly.xsd schemas

        eventNode.getStateTransitions().clear();
        for (StateTransition jaxbStateTransition : jaxbEvent.getStateTransition())
		{
            EventStateTransition eventStateTransition = new EventStateTransition();

            LocalVariableAssignment l = jaxbStateTransition.getLocalVariableAssignment();
            if (l != null && l.getValue() != null && !l.getValue().isEmpty()) {
                eventStateTransition.setLocalVariableAssignment(l.getValue());
            }

            StateVariable jaxbStateVariable = (StateVariable) jaxbStateTransition.getState();
            eventStateTransition.setName(jaxbStateVariable.getName());
            eventStateTransition.setType(jaxbStateVariable.getType());
            eventStateTransition.setDescription(jaxbStateVariable.getDescription());

            // bug fix 1183
            if (ViskitGlobals.instance().isArray(jaxbStateVariable.getType())) {
                String idx = jaxbStateTransition.getIndex();
                eventStateTransition.setIndexingExpression(idx);
            }

            eventStateTransition.setOperation(jaxbStateTransition.getOperation() != null);
            if (eventStateTransition.isOperation()) {
                eventStateTransition.setOperationOrAssignment(jaxbStateTransition.getOperation().getMethod());
            } else {
                eventStateTransition.setOperationOrAssignment(jaxbStateTransition.getAssignment().getValue());
            }

            LocalVariableInvocation localVariableInvocatio = jaxbStateTransition.getLocalVariableInvocation();
            if (localVariableInvocatio != null && localVariableInvocatio.getMethod() != null && !localVariableInvocatio.getMethod().isEmpty())
                eventStateTransition.setLocalVariableInvocation(jaxbStateTransition.getLocalVariableInvocation().getMethod());

            eventStateTransition.opaqueModelObject = jaxbStateTransition;
            eventNode.getStateTransitions().add(eventStateTransition);
        }

        Coordinate coord = jaxbEvent.getCoordinate();
        if (coord != null) //todo lose this after all xmls updated
        {
            eventNode.setPosition(new Point2D.Double(
                    Double.parseDouble(coord.getX()),
                    Double.parseDouble(coord.getY())));
        }

    }

    private void buildEdgesFromJaxb(EventNode src, List<Object> lis) {
        for (Object o : lis) {
            if (o instanceof Schedule) {
                buildSchedulingEdgeFromJaxb(src, (Schedule) o);
            } else {
                buildCancellingEdgeFromJaxb(src, (Cancel) o);
            }
        }
    }

    private void buildSchedulingEdgeFromJaxb(EventNode src, Schedule jaxbSchedule)
	{
        SchedulingEdge schedulingEdge = new SchedulingEdge();
        String s;
        schedulingEdge.opaqueModelObject = jaxbSchedule;

        schedulingEdge.fromEventNode = src;
        EventNode target = buildEventNodeFromJaxbEvent((Event) jaxbSchedule.getEvent());
        schedulingEdge.toEventNode = target;
        schedulingEdge.name = "FROM_" + schedulingEdge.fromEventNode.name + "_TO_" + schedulingEdge.toEventNode.name;

        src.getConnections().add(schedulingEdge);
        target.getConnections().add(schedulingEdge);
        schedulingEdge.condition = jaxbSchedule.getCondition();

        // Attempt toEventNode avoid NumberFormatException thrown on Double.parseDouble(String s)
        if (Pattern.matches(SchedulingEdge.FLOATING_POINT_REGEX, jaxbSchedule.getPriority()))
		{
            s = jaxbSchedule.getPriority();

            setNumericPriority(true);

            // We have a FP number
            // TODO: Deal with LOWEST or HIGHEST values containing exponents, i.e. (+/-) 1.06E8
            if        (s.contains("-3")) {
                s = "LOWEST";
            } else if (s.contains("-2")) {
                s = "LOWER";
            } else if (s.contains("-1")) {
                s = "LOW";
            } else if (s.contains("-")) {
                s = "LOWEST";
            } else if (s.contains("1")) {
                s = "HIGH";
            } else if (s.contains("2")) {
                s = "HIGHER";
            } else if (s.contains("3")) {
                s = "HIGHEST";
            } else {
                s = "DEFAULT";
            }
        } else {

            // We have an enumeration String
            s = jaxbSchedule.getPriority();
        }

        schedulingEdge.priority = s;

        // Now set the JAXB Schedule toEventNode record the Priority enumeration toEventNode overwrite
        // numeric Priority values
        jaxbSchedule.setPriority(schedulingEdge.priority);

        schedulingEdge.conditionDescription = jaxbSchedule.getDescription();

        schedulingEdge.delay = jaxbSchedule.getDelay();
        schedulingEdge.parametersList = buildEdgeParametersFromJaxb(jaxbSchedule.getEdgeParameter());

        edgeCache.put(jaxbSchedule, schedulingEdge);

        setDirty(true);

        this.notifyChanged(new ModelEvent(schedulingEdge, ModelEvent.EDGE_ADDED, "Scheduling Edge added: " + schedulingEdge.name));
    }

    private void buildCancellingEdgeFromJaxb(EventNode jaxbSourceNode, Cancel jaxbCancel) {
        CancellingEdge cancellingEdge       = new CancellingEdge();
        cancellingEdge.opaqueModelObject    = jaxbCancel;
        cancellingEdge.condition            = jaxbCancel.getCondition();

        List<String> commentList = jaxbCancel.getComment();
        if (!commentList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String comment : commentList)
			{
                sb.append(comment);
                sb.append(" ");
            }
            cancellingEdge.conditionDescription = (jaxbCancel.getDescription() + sb.toString()).trim();
        }

        cancellingEdge.parametersList = buildEdgeParametersFromJaxb(jaxbCancel.getEdgeParameter());

        cancellingEdge.fromEventNode = jaxbSourceNode;
        EventNode target = buildEventNodeFromJaxbEvent((Event) jaxbCancel.getEvent());
        cancellingEdge.toEventNode = target;

        jaxbSourceNode.getConnections().add(cancellingEdge);
        target.getConnections().add(cancellingEdge);

        edgeCache.put(jaxbCancel, cancellingEdge);
        setDirty(true);

        notifyChanged(new ModelEvent(cancellingEdge, ModelEvent.CANCELLING_EDGE_ADDED, "Cancelling edge added: " + cancellingEdge.name));
    }

    private List<ViskitElement> buildEdgeParametersFromJaxb(List<EdgeParameter> lis) {
        List<ViskitElement> viskitElementList = new ArrayList<>(3);
        for (EdgeParameter ep : lis) {
            ViskitEdgeParameter viskitEdgeParameter = new ViskitEdgeParameter(ep.getValue());
            viskitElementList.add(viskitEdgeParameter);
        }
        return viskitElementList;
    }

    private void buildCodeBlockFromJaxb(String codeBlock)
	{
        codeBlock = (codeBlock == null) ? "" : codeBlock; // ensure non-null

        notifyChanged(new ModelEvent(codeBlock, ModelEvent.CODEBLOCK_CHANGED, "Code block changed"));
    }
	
	/** Initialize state transitions from state variables */
	public void runEventStateTransitionsUpdate ()
	{
        EventNode       runEventNode = null;
		for (ViskitElement eventNode : getAllNodes())
		{
			if (eventNode.name.equals("Run"))
			{
				runEventNode = (EventNode) eventNode;
				break; // found it
			}
		}
		if ((runEventNode == null) && (stateVariables != null) && (stateVariables.size() > 0))
		{
			LOG.info("need to create Run node to support stateVariable initializations");
			eventGraphController.buildNewRunNode();
		}
		if (runEventNode != null)
		{
			ArrayList<ViskitElement> runStateTransitions = runEventNode.getStateTransitions();
			runStateTransitions.clear();
			
			for (ViskitStateVariable stateVariable : stateVariables)
			{
				// add initialization for each state variable.  see EventStateTransition tooltip for example.
				EventStateTransition eventStateTransition = new EventStateTransition();
				eventStateTransition.setName(stateVariable.name);
				eventStateTransition.setType(stateVariable.type);
				eventStateTransition.setOperation(false); // value initialization, TODO method operation; missing from eventStateTransition?
				eventStateTransition.setOperationOrAssignment(stateVariable.getValue());
//				eventStateTransition.setLocalVariableAssignment(stateVariable.name);
				
				runStateTransitions.add(eventStateTransition);
				
//            eventStateTransition.setOperation(jaxbStateTransition.getOperation() != null);
//            if (eventStateTransition.isOperation()) {
//                eventStateTransition.setOperationOrAssignment(jaxbStateTransition.getOperation().getMethod());
//            } else {
//                eventStateTransition.setOperationOrAssignment(jaxbStateTransition.getAssignment().getValue());
//            }
			}
		}
		else
		{
			
		}
	}

    private void buildStateVariablesFromJaxb(List<StateVariable> jaxbStateVariableList)
	{	
        for (StateVariable jaxbStateVariable : jaxbStateVariableList)
		{
			// Comment elements are deprecated, merge any legacy Comment strings as part of description field
            List<String> stateVariableCommentsList = jaxbStateVariable.getComment();
            String comments = " ";
            for (String comment : stateVariableCommentsList) {
                comments += comment;
                comments += " ";
            }
			String description = (jaxbStateVariable.getDescription() + comments).trim();
			
            ViskitStateVariable stateVariable = new ViskitStateVariable(jaxbStateVariable.getName(),
																		jaxbStateVariable.getType(),
																		jaxbStateVariable.isImplicit(),
																		jaxbStateVariable.getValue(),
																		description);
            stateVariable.opaqueModelObject = jaxbStateVariable;

            if (!stateVariableParameterNameCheck()) {
                mangleName(stateVariable);
            }
            stateVariables.add(stateVariable);

            notifyChanged(new ModelEvent(stateVariable, ModelEvent.STATEVARIABLE_ADDED, "New state variable: " + stateVariable.name));
        }
    }

    private void buildParametersFromJaxb(List<Parameter> jaxbParametersList)
	{
        for (Parameter jaxbParameter : jaxbParametersList)
		{
            List<String> jaxbParameterCommentsList = jaxbParameter.getComment();
            String comments = " ";
            for (String comment : jaxbParameterCommentsList) {
                comments += comment;
                comments += " ";
            }
			String description = (jaxbParameter.getDescription() + comments).trim();
			jaxbParameter.getComment().clear();
			
            ViskitParameter vp = new ViskitParameter(jaxbParameter.getName(), jaxbParameter.getType(), description);
			jaxbParameter.getComment().clear();
			
            vp.opaqueModelObject = jaxbParameter;

            if (!stateVariableParameterNameCheck()) {
                mangleName(vp);
            }
            simulationParameters.add(vp);

            notifyChanged(new ModelEvent(vp, ModelEvent.SIMPARAMETER_ADDED, "New vParameter added"));
        }
    }

    @Override
    public Vector<ViskitElement> getAllNodes() {
        return new Vector<ViskitElement>(eventGraphNodeCacheMap.values());
    }

    @SuppressWarnings("unchecked") // TODO: Known unchecked cast toEventNode ViskitElement
    @Override
    public Vector<ViskitElement> getStateVariables() {
        return (Vector<ViskitElement>) stateVariables.clone();
    }

    @SuppressWarnings("unchecked") // TODO: Known unchecked cast toEventNode ViskitElement
    @Override
    public Vector<ViskitElement> getSimulationParameters() {
        return (Vector<ViskitElement>) simulationParameters.clone();
    }

    // parameter mods
    // --------------
    @Override
    public void newSimParameter(String name, String type, String initialValue, String description)
	{
        setDirty(true);

        ViskitParameter vp = new ViskitParameter(name, type, description);
        simulationParameters.add(vp);

        if (!stateVariableParameterNameCheck()) {
            mangleName(vp);
        }

        Parameter jaxbParameter = jaxbObjectFactory.createParameter();
        jaxbParameter.setName(ViskitGlobals.nullIfEmpty(name));
        jaxbParameter.setType(ViskitGlobals.nullIfEmpty(type));
        jaxbParameter.setDescription(ViskitGlobals.nullIfEmpty(description));

        vp.opaqueModelObject = jaxbParameter;

        jaxbSimEntity.getParameter().add(jaxbParameter);

        notifyChanged(new ModelEvent(vp, ModelEvent.SIMPARAMETER_ADDED, "new Sim Parameter: " + name));
    }

    @Override
    public void deleteSimParameter(ViskitParameter vp) {
        // remove jaxb variable
        Iterator<Parameter> spItr = jaxbSimEntity.getParameter().iterator();
        while (spItr.hasNext()) {
            if (spItr.next() == (Parameter) vp.opaqueModelObject) {
                spItr.remove();
                break;
            }
        }
        setDirty(true);
        simulationParameters.remove(vp);
        notifyChanged(new ModelEvent(vp, ModelEvent.SIMPARAMETER_DELETED, "vParameter deleted: " + vp.name));
    }

    @Override
    public String getCodeBlock() 
	{
        return jaxbSimEntity.getCode();
    }

    @Override
    public void changeCodeBlock(String newCodeBlock) 
	{
        jaxbSimEntity.setCode(newCodeBlock);
        setDirty(true);
    }

    @Override
    public boolean changeSimParameter(ViskitParameter vp) {
        boolean success = true;
        if (!stateVariableParameterNameCheck()) {
            mangleName(vp);
            success = false;
        }
        // fill out jaxb variable
        Parameter parameter = (Parameter) vp.opaqueModelObject;
        parameter.setName(ViskitGlobals.nullIfEmpty(vp.getName()));
        //p.setShortName(vp.getName());
        parameter.setType(ViskitGlobals.nullIfEmpty(vp.getType()));
        parameter.setDescription(vp.getDescription());

        setDirty(true);
        notifyChanged(new ModelEvent(vp, ModelEvent.SIMPARAMETER_CHANGED, "simParameter changed: " + vp.getName()));
        return success;
    }

    // State variable mods
    // -------------------
    @Override
    public void newStateVariable(String name, String type, boolean implicit, String initialValue, String description)
	{
        setDirty(true);

        // get the new one here and show it around
        ViskitStateVariable viskitStateVariable = new ViskitStateVariable(name, type, implicit, initialValue, description);
        stateVariables.add(viskitStateVariable);
        if (!stateVariableParameterNameCheck()) {
            mangleName(viskitStateVariable);
        }
        StateVariable stateVariable = jaxbObjectFactory.createStateVariable();
        stateVariable.setName(ViskitGlobals.nullIfEmpty(name));
        stateVariable.setType(ViskitGlobals.nullIfEmpty(type));
        stateVariable.setValue(ViskitGlobals.nullIfEmpty(initialValue));
        stateVariable.setDescription(description);

        viskitStateVariable.opaqueModelObject = stateVariable;
        jaxbSimEntity.getStateVariable().add(stateVariable);
        notifyChanged(new ModelEvent(viskitStateVariable, ModelEvent.STATEVARIABLE_ADDED, "State variable added: " + stateVariable.getName()));
    }

    @Override
    public void deleteStateVariable(ViskitStateVariable vsv)
	{
        // remove jaxb variable
        Iterator<StateVariable> svItr = jaxbSimEntity.getStateVariable().iterator();
        while (svItr.hasNext()) {
            if (svItr.next() == (StateVariable) vsv.opaqueModelObject) {
                svItr.remove();
                break;
            }
        }
        stateVariables.remove(vsv);
        setDirty(true);
        notifyChanged(new ModelEvent(vsv, ModelEvent.STATEVARIABLE_DELETED, "State variable deleted: " + vsv.getName()));
    }

    @Override
    public boolean changeStateVariable(ViskitStateVariable viskitStateVariable)
	{
        boolean success = true;
        if (!stateVariableParameterNameCheck())
		{
            mangleName(viskitStateVariable);
            success = false;
        }
        // fill out jaxb variable
        StateVariable jaxbStateVariable = (StateVariable) viskitStateVariable.opaqueModelObject;
        jaxbStateVariable.setName(ViskitGlobals.nullIfEmpty(viskitStateVariable.getName()));
        jaxbStateVariable.setType(ViskitGlobals.nullIfEmpty(viskitStateVariable.getType()));
		boolean isImplicit = viskitStateVariable.isImplicit();
        jaxbStateVariable.setImplicit(isImplicit);
		if (isImplicit)
		{
			jaxbStateVariable.setValue(ViskitGlobals.nullIfEmpty(viskitStateVariable.getValue()));
		}
		else
		{
			jaxbStateVariable.setValue(ViskitGlobals.nullIfEmpty("")); // no value allowed
		}
        jaxbStateVariable.setDescription(ViskitGlobals.nullIfEmpty(viskitStateVariable.getDescription()));

        setDirty(true);
        notifyChanged(new ModelEvent(viskitStateVariable, ModelEvent.STATEVARIABLE_CHANGED, "State variable changed: " + viskitStateVariable.getName()));
        return success;
    }

    // Event (node) mods
    // -----------------

    @Override
    public EventNode newEventNode(String nodeName, Point2D point)
	{
        EventNode eventNode = new EventNode(nodeName);
        if (point == null) {
            point = new Point2D.Double(30, 60);
        }
        eventNode.setPosition(point);
        Event jaxbEvent = jaxbObjectFactory.createEvent();

        eventGraphNodeCacheMap.put(jaxbEvent, eventNode);   // key = ev

        // Ensure a unique Event name
        if (!nameCheck()) {
            mangleName(eventNode);
        }

        jaxbEvent.setName(ViskitGlobals.nullIfEmpty(eventNode.name));

        if ("Run".equals(ViskitGlobals.nullIfEmpty(nodeName))) {
            jaxbEvent.setDescription("The Run event is fired first to support initialization of all simulation state variables");
        }
        eventNode.opaqueModelObject = jaxbEvent;
        jaxbSimEntity.getEvent().add(jaxbEvent);

        setDirty(true);
        notifyChanged(new ModelEvent(eventNode, ModelEvent.EVENT_ADDED, "Event added: " + eventNode.name));
		
		return eventNode;
    }

    @Override
    public void redoEvent(EventNode node) {
        Event jaxbEvent = jaxbObjectFactory.createEvent();
        eventGraphNodeCacheMap.put(jaxbEvent, node);   // key = evnode.opaqueModelObject = jaxbEv;
        jaxbEvent.setName(node.getName());
        node.opaqueModelObject = jaxbEvent;
        jaxbSimEntity.getEvent().add(jaxbEvent);

        setDirty(true);
        notifyChanged(new ModelEvent(node, ModelEvent.REDO_EVENT_NODE, "Event Node action redone: " + node.getName()));
    }

    @Override
    public void deleteEvent(EventNode node) {
        Event jaxbEvent = (Event) node.opaqueModelObject;
        eventGraphNodeCacheMap.remove(jaxbEvent);
        jaxbSimEntity.getEvent().remove(jaxbEvent);

        setDirty(true);

        if (!eventGraphController.isUndo())
            notifyChanged(new ModelEvent(node, ModelEvent.EVENT_DELETED,   "Event deleted: " + node.getName()));
        else
            notifyChanged(new ModelEvent(node, ModelEvent.UNDO_EVENT_NODE, "Event action undone: " + node.getName()));
    }

    private StateVariable findStateVariable(String name)
	{
        List<StateVariable> jaxbStateVariableList = jaxbSimEntity.getStateVariable();
        for (StateVariable jaxbStateVariable : jaxbStateVariableList) {
            if (jaxbStateVariable.getName().equals(name)) {
                return jaxbStateVariable;
            }
        }
        return null; // jaxbStateVariable not found
    }
    private int localVariableNameSequence = 0;

    @Override
    public String generateLocalVariableName()
	{
        String name = null;
        do {
            name = localVariablePrefix + localVariableNameSequence++;
        } 
		while (!isUniqueLocalVariableOrIndexVname(name));
        return name;
    }

    @Override
    public void resetLocalVariableNameGenerator() {
        localVariableNameSequence = 0;
    }
    private int indexVariableNameSequence = 0;

    @Override
    public String generateIndexVariableName() {
        String name = null;
        do {
            name = indexVariablePrefix + indexVariableNameSequence++;
        } 
		while (!isUniqueLocalVariableOrIndexVname(name));
        return name;
    }

    @Override
    public void resetIndexVariableNameGenerator() {
        indexVariableNameSequence = 0;
    }

    private boolean isUniqueLocalVariableOrIndexVname(String name)
	{
        for (EventNode event : eventGraphNodeCacheMap.values())
		{
            for (ViskitElement localVariable : event.getLocalVariables()) {
                if (localVariable.getName().equals(name)) {
                    return false;
                }
            }
            for (ViskitElement transition : event.getStateTransitions()) {
                String indexingExpression = transition.getIndexingExpression();
                if (indexingExpression != null && indexingExpression.equals(name)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isUniqueStateVariableName(String name) {
        for (ViskitElement stateVariable : stateVariables) {
            if (stateVariable.getName().equals(name)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String generateStateVariableName() {
        String name = null;
        int startNumber = 0;
        do {
            name = stateVariablePrefix + startNumber++;
        }
		while (!isUniqueStateVariableName(name));
        return name;
    }

    /**
     * Here we convert local state transition expressions into JAXB bindings
     *
     * @param jaxbTargetList List of StateTransitions toEventNode populate
     * @param local List of StateTransitions toEventNode transfer toEventNode the target
     */
    private void cloneTransitions(List<StateTransition> jaxbTargetList, List<ViskitElement> local)
	{
        jaxbTargetList.clear();
        for (ViskitElement transition : local)
		{
            StateTransition stateTransition = jaxbObjectFactory.createStateTransition();

            // Various locally declared variable ops
            String localVariableAssignment = ((EventStateTransition)transition).getLocalVariableAssignment();
            if (localVariableAssignment != null && !localVariableAssignment.isEmpty()) {

                String localVariableAssignmentValue = ((EventStateTransition)transition).getLocalVariableAssignment();
                if (localVariableAssignmentValue != null && !localVariableAssignmentValue.isEmpty()) {
                    LocalVariableAssignment jaxbLocalVariableAssignment = jaxbObjectFactory.createLocalVariableAssignment();
                    jaxbLocalVariableAssignment.setValue(localVariableAssignmentValue);
                    stateTransition.setLocalVariableAssignment(jaxbLocalVariableAssignment);
                }
            }
            StateVariable stateVariable = findStateVariable(transition.getName());

            if (stateVariable == null)
			{
				continue; // looping
			}

            stateTransition.setState(stateVariable);

            if (stateVariable.getType() != null && ViskitGlobals.instance().isArray(stateVariable.getType())) {

                // Match the state transition's index toEventNode the given index
                stateTransition.setIndex(transition.getIndexingExpression());
            }

            if (transition.isOperation()) {
                Operation jaxbOperation = jaxbObjectFactory.createOperation();
                jaxbOperation.setMethod(transition.getOperationOrAssignment());
                stateTransition.setOperation(jaxbOperation);
            } else {
                Assignment a = jaxbObjectFactory.createAssignment();
                a.setValue(transition.getOperationOrAssignment());
                stateTransition.setAssignment(a);
            }

            // If we have any void return type, zero parameter methods toEventNode
            // call on local vars, or args, do it now
            String localVariableInvocation = ((EventStateTransition)transition).getLocalVariableInvocation();
            if (localVariableInvocation != null && !localVariableInvocation.isEmpty()) {

                String invoke = ((EventStateTransition) transition).getLocalVariableInvocation();
                if (invoke != null && !invoke.isEmpty()) {
                    LocalVariableInvocation jaxbLocalVariableInvocation = jaxbObjectFactory.createLocalVariableInvocation();
                    jaxbLocalVariableInvocation.setMethod(invoke);
                    stateTransition.setLocalVariableInvocation(jaxbLocalVariableInvocation);
                }
            }
            transition.opaqueModelObject = stateTransition; //replace
            jaxbTargetList.add(stateTransition);
        }
    }
	
	@Deprecated
    private void cloneComments (List<String> targ, List<String> local) {
        targ.clear();
        targ.addAll(local);
    }

    private void cloneArguments(List<Argument> jaxbArgumentList, List<ViskitElement> local) {
        jaxbArgumentList.clear();
        for (ViskitElement eventArguments : local)
		{
            Argument jaxbArgument = jaxbObjectFactory.createArgument();
            jaxbArgument.setName(ViskitGlobals.nullIfEmpty(eventArguments.getName()));
            jaxbArgument.setType(ViskitGlobals.nullIfEmpty(eventArguments.getType()));
            jaxbArgument.setDescription(ViskitGlobals.nullIfEmpty(eventArguments.getDescription()));
            eventArguments.opaqueModelObject = jaxbArgument; // replace
            jaxbArgumentList.add(jaxbArgument);
        }
    }

    private void cloneLocalVariables(List<LocalVariable> jaxbLocalVariableList, List<ViskitElement> local) {
        jaxbLocalVariableList.clear();
        for (ViskitElement eventLocalVariables : local) {
            LocalVariable jaxbLocalVariable = jaxbObjectFactory.createLocalVariable();
            jaxbLocalVariable.setName(ViskitGlobals.nullIfEmpty(eventLocalVariables.getName()));
            jaxbLocalVariable.setType(ViskitGlobals.nullIfEmpty(eventLocalVariables.getType()));
            jaxbLocalVariable.setValue(ViskitGlobals.nullIfEmpty(eventLocalVariables.getValue()));
            jaxbLocalVariable.setDescription(eventLocalVariables.getDescription());
            eventLocalVariables.opaqueModelObject = jaxbLocalVariable; //replace
            jaxbLocalVariableList.add(jaxbLocalVariable);
        }
    }

    @Override
    public boolean changeEvent(EventNode node) {
        boolean success = true;

        // Ensure a unique Event name
        if (!nameCheck()) {
            mangleName(node);
            success = false;
        }
        Event jaxbEvent = (Event) node.opaqueModelObject;

        jaxbEvent.setName(node.getName());

        double x = node.getPosition().getX();
        double y = node.getPosition().getY();
        Coordinate coord = jaxbObjectFactory.createCoordinate();
        coord.setX("" + x);
        coord.setY("" + y);
        node.getPosition().setLocation(x, y);
        jaxbEvent.setCoordinate(coord);

		String descriptionCopy = "";
		if ((node.getDescription() != null))
		       descriptionCopy = node.getDescription().trim(); // copy, not reference
		jaxbEvent.setDescription(descriptionCopy);
        cloneArguments(jaxbEvent.getArgument(), node.getArguments());
        cloneLocalVariables(jaxbEvent.getLocalVariable(), node.getLocalVariables());
        // following must follow above
        cloneTransitions(jaxbEvent.getStateTransition(), node.getStateTransitions());

        jaxbEvent.setCode(node.getCodeBlock());

        setDirty(true);
        notifyChanged(new ModelEvent(node, ModelEvent.EVENT_CHANGED, "Event changed: " + node.getName()));
        return success;
    }

    // Edge mods
    // ---------

    @Override
    public void newSchedulingEdge(EventNode sourceNode, EventNode targetNode)
	{
        SchedulingEdge schedulingEdge = new SchedulingEdge();
        schedulingEdge.fromEventNode = sourceNode;
        schedulingEdge.toEventNode = targetNode;
        sourceNode.getConnections().add(schedulingEdge);
        targetNode.getConnections().add(schedulingEdge);

        Schedule jaxbSchedule = jaxbObjectFactory.createSchedule();

        schedulingEdge.opaqueModelObject = jaxbSchedule;
        Event targetEvent = (Event) targetNode.opaqueModelObject;
        jaxbSchedule.setEvent(targetEvent);
        Event sourceEvent = (Event) sourceNode.opaqueModelObject;
        sourceEvent.getScheduleOrCancel().add(jaxbSchedule);

        // Put in dummy edge parametersList toEventNode match the target arguments
        List<ViskitElement> args = targetNode.getArguments();
        if (!args.isEmpty()) {
            List<ViskitElement> edgeParameters = new ArrayList<>(args.size());
            for (ViskitElement arg : args) {
                edgeParameters.add(new ViskitEdgeParameter(arg.getValue()));
            }
            schedulingEdge.parametersList = edgeParameters;
        }

        schedulingEdge.priority = "DEFAULT";  // set default

        edgeCache.put(jaxbSchedule, schedulingEdge);
        setDirty(true);
        notifyChanged(new ModelEvent(schedulingEdge, ModelEvent.EDGE_ADDED, "Scheduling Edge added: " +
		              sourceNode.getName() + " to " + targetNode.getName()));
    }

    @Override
    public void redoSchedulingEdge(SchedulingEdge jaxbEdge)
	{
        EventNode sourceNode, targetNode;
        sourceNode = (EventNode) ((DefaultMutableTreeNode) jaxbEdge.fromEventNode.opaqueViewObject).getUserObject();
        targetNode = (EventNode) ((DefaultMutableTreeNode) jaxbEdge.toEventNode.opaqueViewObject).getUserObject();
        Schedule jaxbSchedule = jaxbObjectFactory.createSchedule();
        jaxbEdge.opaqueModelObject = jaxbSchedule;
        Event jaxbEvent = (Event) targetNode.opaqueModelObject;
        jaxbSchedule.setEvent(jaxbEvent);
        Event sourceEvent = (Event) sourceNode.opaqueModelObject;
        sourceEvent.getScheduleOrCancel().add(jaxbSchedule);
        edgeCache.put(jaxbSchedule, jaxbEdge);
        setDirty(true);
        notifyChanged(new ModelEvent(jaxbEdge, ModelEvent.REDO_SCHEDULING_EDGE, "Scheduling Edge action redone: " + 
		              sourceNode.getName() + " to " + targetNode.getName()));
    }

    @Override
    public void newCancellingEdge(EventNode sourceNode, EventNode targetNode)
	{
        CancellingEdge cancellingEdge = new CancellingEdge();
        cancellingEdge.fromEventNode = sourceNode;
        cancellingEdge.toEventNode   = targetNode;
        sourceNode.getConnections().add(cancellingEdge);
        targetNode.getConnections().add(cancellingEdge);

        Cancel cancel = jaxbObjectFactory.createCancel();

        cancellingEdge.opaqueModelObject = cancel;
        Event targetEvent = (Event) targetNode.opaqueModelObject;
        cancel.setEvent(targetEvent);
        Event sourceEvent = (Event) sourceNode.opaqueModelObject;
        sourceEvent.getScheduleOrCancel().add(cancel);

        // Put in dummy edge parametersList toEventNode match the target arguments
        List<ViskitElement> args = targetNode.getArguments();
        if (!args.isEmpty()) {
            List<ViskitElement> edgeParameters = new ArrayList<>(args.size());
            for (ViskitElement arg : args) {
                edgeParameters.add(new ViskitEdgeParameter(arg.getValue()));
            }
            cancellingEdge.parametersList = edgeParameters;
        }

        edgeCache.put(cancel, cancellingEdge);
        setDirty(true);
        notifyChanged(new ModelEvent(cancellingEdge, ModelEvent.CANCELLING_EDGE_ADDED, "Cancelling Edge added: " + 
		              sourceNode.getName() + " to " + targetNode.getName()));
    }

    @Override
    public void redoCancellingEdge(CancellingEdge edge)
	{
        EventNode sourceNode, targetNode;
        sourceNode = (EventNode) ((DefaultMutableTreeNode) edge.fromEventNode.opaqueViewObject).getUserObject();
        targetNode = (EventNode) ((DefaultMutableTreeNode) edge.toEventNode.opaqueViewObject).getUserObject();
        Cancel cancel = jaxbObjectFactory.createCancel();
        edge.opaqueModelObject = cancel;
        Event targetEvent = (Event) targetNode.opaqueModelObject;
        cancel.setEvent(targetEvent);
        Event sourceEvent = (Event) sourceNode.opaqueModelObject;
        sourceEvent.getScheduleOrCancel().add(cancel);
        edgeCache.put(cancel, edge);
        setDirty(true);
        notifyChanged(new ModelEvent(edge, ModelEvent.REDO_CANCELLING_EDGE, "Cancelling Edge action redone: " +
		              sourceNode.getName() + " to " + targetNode.getName()));
    }

    @Override
    public void deleteSchedulingEdge(SchedulingEdge edge)
	{
        _commonEdgeDelete(edge);

        if (!eventGraphController.isUndo())
            notifyChanged(new ModelEvent(edge, ModelEvent.EDGE_DELETED, "Scheduling Edge deleted: " + edge.getName()));
        else
            notifyChanged(new ModelEvent(edge, ModelEvent.UNDO_SCHEDULING_EDGE, "Scheduling Edge action undone: " + edge.getName()));
    }

    @Override
    public void deleteCancellingEdge(CancellingEdge edge)
	{
        _commonEdgeDelete(edge);

        if (!eventGraphController.isUndo())
            notifyChanged(new ModelEvent(edge, ModelEvent.CANCELLING_EDGE_DELETED, "Cancelling edge deleted: " + edge.getName()));
        else
            notifyChanged(new ModelEvent(edge, ModelEvent.UNDO_CANCELLING_EDGE, "Cancelling edge action undone: " + edge.getName()));
    }

    private void _commonEdgeDelete(Edge edge)
	{
        Object jaxbEdge = edge.opaqueModelObject;

        List<Event> jaxbEvents = jaxbSimEntity.getEvent();
        for (Event jaxbEvent : jaxbEvents) {
            List<Object> edges = jaxbEvent.getScheduleOrCancel();
            edges.remove(jaxbEdge);
        }

        edgeCache.remove(edge);
        setDirty(true);
    }

    @Override
    public void changeSchedulingEdge(SchedulingEdge schedulingEdge)
	{
        Schedule jaxbSchedule = (Schedule) schedulingEdge.opaqueModelObject;
        jaxbSchedule.setCondition(schedulingEdge.condition);
        jaxbSchedule.setDescription(schedulingEdge.conditionDescription);
        jaxbSchedule.setDelay("" + schedulingEdge.delay);

        jaxbSchedule.setEvent(schedulingEdge.toEventNode.opaqueModelObject);
        jaxbSchedule.setPriority((schedulingEdge).priority);
        jaxbSchedule.getEdgeParameter().clear();

        // Bug 1373: This is where an edge parameter gets written out toEventNode XML
        for (ViskitElement edgeParameter : schedulingEdge.parametersList)
		{
            EdgeParameter jaxbEdgeParameter = jaxbObjectFactory.createEdgeParameter();
            jaxbEdgeParameter.setValue(ViskitGlobals.nullIfEmpty(edgeParameter.getValue()));
            jaxbSchedule.getEdgeParameter().add(jaxbEdgeParameter);
        }

        setDirty(true);
        notifyChanged(new ModelEvent(schedulingEdge, ModelEvent.EDGE_CHANGED, "Edge changed: " + schedulingEdge.name));
    }

    @Override
    public void changeCancellingEdge(CancellingEdge cancellingEdge)
	{
        Cancel jaxbCancel = (Cancel) cancellingEdge.opaqueModelObject;
        jaxbCancel.setCondition(cancellingEdge.condition);
        jaxbCancel.setEvent(cancellingEdge.toEventNode.opaqueModelObject);
        jaxbCancel.setDescription(cancellingEdge.conditionDescription);

        jaxbCancel.getEdgeParameter().clear();
        for (ViskitElement edgeParameter : cancellingEdge.parametersList)
		{
            EdgeParameter jaxbEdgeParameter = jaxbObjectFactory.createEdgeParameter();
            jaxbEdgeParameter.setValue(ViskitGlobals.nullIfEmpty(edgeParameter.getValue()));
            jaxbCancel.getEdgeParameter().add(jaxbEdgeParameter);
        }
        setDirty(true);
        notifyChanged(new ModelEvent(cancellingEdge, ModelEvent.CANCELLING_EDGE_CHANGED, "Cancelling edge changed: " + cancellingEdge.name));
    }
}
