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
import viskit.control.EventGraphControllerImpl;
import viskit.mvc.MvcAbstractModel;
import viskit.util.XMLValidationTool;
import viskit.xsd.bindings.eventgraph.*;
import viskit.xsd.translator.assembly.SimkitAssemblyXML2Java;
import viskit.xsd.translator.eventgraph.SimkitXML2Java;
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
    
    private static JAXBContext jaxbContext;
    ObjectFactory jaxbEventGraphObjectFactory;
    SimEntity jaxbRoot;
    File currentFile;
    Map<Event, EventNode> eventNodeCache = new HashMap<>();
    Map<Object, Edge> edgeCache = new HashMap<>();
    Vector<ViskitElement> stateVariables = new Vector<>();
    Vector<ViskitElement> simulationParameters  = new Vector<>();

    private final String schemaLocation = XMLValidationTool.EVENT_GRAPH_SCHEMA;
    private final String privateIndexVariablePrefix = "_index_variable_";
    private final String privateLocalVariablePrefix = "local_variable_";
    private final String stateVariablePrefix = "state_variable_";
    private final EventGraphControllerImpl eventGraphController;

    private GraphMetadata graphMetadata;
    private boolean modelDirty = false;
    private boolean numericPriority;

    public ModelImpl(MvcController newController) {
        eventGraphController = (EventGraphControllerImpl) newController;
        graphMetadata = new GraphMetadata(this);
    }

    @Override
    public void initialize() 
    {
        try {
            if (jaxbContext == null) // avoid JAXBException (perhaps due to concurrency)
                jaxbContext = JAXBContext.newInstance(SimkitXML2Java.EVENT_GRAPH_BINDINGS);
            jaxbEventGraphObjectFactory = new viskit.xsd.bindings.eventgraph.ObjectFactory();
            jaxbRoot = jaxbEventGraphObjectFactory.createSimEntity(); // to start with empty graph
        } 
        catch (JAXBException e) {
            eventGraphController.messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML Error",
                    "Exception on JAXBContext instantiation" +
                    "\n" + e.getMessage());
        }
    }

    @Override
    public boolean isDirty() {
        return modelDirty;
    }

    @Override
    public void setDirty(boolean newDirtyStatus) {
        modelDirty = newDirtyStatus;
    }

    @Override
    public GraphMetadata getMetadata() {
        return graphMetadata;
    }

    @Override
    public void changeMetadata(GraphMetadata newGraphMetadata) {
        this.graphMetadata = newGraphMetadata;
        setDirty(true);
        notifyChanged(new ModelEvent(newGraphMetadata, ModelEvent.METADATA_CHANGED, "Metadata changed"));
    }

    @Override
    public boolean newModel(File f) 
    {
        stateVariables.removeAllElements();
        simulationParameters.removeAllElements();
        eventNodeCache.clear();
        edgeCache.clear();

        if (f == null) {
            jaxbRoot = jaxbEventGraphObjectFactory.createSimEntity(); // to start with empty graph
            notifyChanged(new ModelEvent(this, ModelEvent.NEW_MODEL, "New empty model"));
        } else {
            try {
                currentFile = f;

                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                jaxbRoot = (SimEntity) unmarshaller.unmarshal(currentFile);

                GraphMetadata myGraphMetadata = new GraphMetadata(this);
                myGraphMetadata.author = jaxbRoot.getAuthor();
                myGraphMetadata.version = jaxbRoot.getVersion();
                myGraphMetadata.name = jaxbRoot.getName();
                myGraphMetadata.packageName = jaxbRoot.getPackage();
                myGraphMetadata.extendsPackageName = jaxbRoot.getExtend();
                myGraphMetadata.implementsPackageName = jaxbRoot.getImplement();
                // obsolete
//                List<String> commentList = jaxbRoot.getComment();
//                StringBuilder sb = new StringBuilder("");
//                for (String comment : commentList) {
//                    sb.append(comment);
//                    sb.append(" ");
//                }
//                myGraphMetadata.description = sb.toString().trim();
                myGraphMetadata.description = jaxbRoot.getDescription();
                changeMetadata(myGraphMetadata);

                buildEventsFromJaxb(jaxbRoot.getEvent());

                // The above change metadata and build set the model dirty
                setDirty(false);
                // The following builds just notify

                buildParametersFromJaxb(jaxbRoot.getParameter());
                buildStateVariablesFromJaxb(jaxbRoot.getStateVariable());
                buildCodeBlockFromJaxb(jaxbRoot.getCode());
            } 
            catch (JAXBException ee) {
                // want a clear way to know if they're trying to load an assembly vs. some unspecified XML.
                try {
                    if (jaxbContext == null) // avoid JAXBException (perhaps due to concurrency)
                        jaxbContext = JAXBContext.newInstance(SimkitAssemblyXML2Java.ASSEMBLY_BINDINGS);
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    unmarshaller.unmarshal(f);
                    // If we get here, we've tried to load an assembly.
                    eventGraphController.messageUser(JOptionPane.ERROR_MESSAGE,
                            "This file is an Assembly",
                            "Use the Assembly Editor to" +
                            "\n" + "work with this file: " + f.getName()
                            );
                } 
                catch (JAXBException e)
                {
                    eventGraphController.messageUser(JOptionPane.ERROR_MESSAGE,
                            "XML I/O Error",
                            "Exception on JAXB unmarshalling of" +
                            "\n" + f.getName() +
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
    public boolean saveModel(File modelFile) {
        boolean returnValue;
        if (modelFile == null) {
            modelFile = currentFile;
        }
        currentFile = modelFile;

        // Do the marshalling into a temporary file, so as to avoid possible
        // deletion of existing file on a marshal error.

        File tempFile;
        try {
            tempFile = TempFileManager.createTempFile("tempEventGraphMarshal", ".xml");
        } 
        catch (IOException e) {
            eventGraphController.messageUser(JOptionPane.ERROR_MESSAGE,
                    "I/O Error",
                    "Exception creating temporary file, Model.saveModel():" +
                    "\n" + e.getMessage()
                    );
            return false;
        }

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(tempFile);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, schemaLocation);

            jaxbRoot.setName(ViskitStatics.nullIfEmpty(graphMetadata.name));
            jaxbRoot.setVersion(ViskitStatics.nullIfEmpty(graphMetadata.version));
            jaxbRoot.setAuthor(ViskitStatics.nullIfEmpty(graphMetadata.author));
            jaxbRoot.setPackage(ViskitStatics.nullIfEmpty(graphMetadata.packageName));
            jaxbRoot.setExtend(ViskitStatics.nullIfEmpty(graphMetadata.extendsPackageName));
            jaxbRoot.setImplement(ViskitStatics.nullIfEmpty(graphMetadata.implementsPackageName));
            jaxbRoot.setDescription(ViskitStatics.nullIfEmpty(graphMetadata.description));
            
            // obsolete
//            List<String> clis = jaxbRoot.getComment();
//            clis.clear();
//            String cmt = nIe(graphMetadata.description);
//            if (cmt != null) {
//                clis.add(cmt.trim());
//            }

            marshaller.marshal(jaxbRoot, fileWriter);

            // OK, made it through the marshal, overwrite the "real" file
            Files.copy(tempFile.toPath(), currentFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            setDirty(false);
            returnValue = true;
        }
        catch (JAXBException e) {
            eventGraphController.messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML I/O Error",
                    "Exception on JAXB marshalling" +
                    "\n" + modelFile.getName() +
                    "\n" + e.getMessage()
                    );
            returnValue = false;
        } 
        catch (IOException ex) {
            eventGraphController.messageUser(JOptionPane.ERROR_MESSAGE,
                    "File I/O Error",
                    "Exception on writing " +
                    "\n" + modelFile.getName() +
                    "\n" + ex.getMessage()
                    );
            returnValue = false;
        } 
        finally {
            try {
                if (fileWriter != null)
                    fileWriter.close();
            } 
            catch (IOException ioe) {
                LOG.error("{} saveModel() error closing FileWriter\n      {}\n{}", this.getClass().getSimpleName(), currentFile.toPath(), ioe);
            }
        }
        return returnValue;
    }

    @Override
    public File getLastFile() {
        return currentFile;
    }

    private void buildEventsFromJaxb(List<Event> lis) {
        EventNode en;
        for (Event ev : lis) {
            en = buildNodeFromJaxbEvent(ev);
            buildEdgesFromJaxb(en, ev.getScheduleOrCancel());
        }
    }

    private EventNode buildNodeFromJaxbEvent(Event ev) {
        EventNode en = eventNodeCache.get(ev);
        if (en != null) {
            return en;
        }
        en = new EventNode(ev.getName());
        jaxbEventToNode(ev, en);
        en.opaqueModelObject = ev;

        eventNodeCache.put(ev, en);   // key = ev

        // Ensure a unique Event name for XML IDREFs
        if (!nameCheck()) {
            mangleName(en);
        }
        
        notifyChanged(new ModelEvent(en, ModelEvent.EVENT_ADDED, "Event added"));
        return en;
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

    private void mangleName(ViskitElement node) {
        do {
            node.setName(AssemblyModelImpl.mangleName(node.getName()));
        } while (!nameCheck());
    }

    private boolean nameCheck() {
        Set<String> hs = new HashSet<>(10);
        for (EventNode en : eventNodeCache.values()) {
            if (!hs.add(en.getName())) {
                eventGraphController.messageUser(JOptionPane.INFORMATION_MESSAGE,
                        "Duplicate Event Name",
                        "Duplicate event name detected: " + en.getName() +
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

    public void setNumericPriority(boolean b) {
        numericPriority = b;
    }

    private boolean stateVariableParameterNameCheck() {
        Set<String> hs = new HashSet<>(10);
        for (ViskitElement sv : stateVariables) {
            if (!hs.add(sv.getName())) {
                return false;
            }
        }
        for (ViskitElement sp : simulationParameters) {
            if (!hs.add(sp.getName())) {
                return false;
            }
        }
        return true;
    }

    private void jaxbEventToNode(Event event, EventNode eventNode) {
        eventNode.setName(event.getName());

//        eventNode.getComments().clear();
//        eventNode.getComments().addAll(event.getComment());
        eventNode.setDescription(eventNode.getDescription() + event.getDescription());

        eventNode.getArguments().clear();

        EventArgument eventArgument;
//        List<String> descriptionList;
        for (Argument nextArgument : event.getArgument()) // returns a List
        {
            eventArgument = new EventArgument();
            eventArgument.setName(nextArgument.getName());
            eventArgument.setType(nextArgument.getType());

//            descriptionList = new ArrayList<>();
//            descriptionList.addAll(arg.getComment());
//            eventArgument.setDescriptionArray(descriptionList);
            eventArgument.setDescription(nextArgument.getDescription());
            eventArgument.opaqueModelObject = nextArgument;
            eventNode.getArguments().add(eventArgument);
        }

        eventNode.getLocalVariables().clear();
        EventLocalVariable eventLocalVariable;
        for (LocalVariable localVariable : event.getLocalVariable()) {
            if (!localVariable.getName().startsWith(privateIndexVariablePrefix)) // only if it's a "public" one
            {
                eventLocalVariable = new EventLocalVariable(localVariable.getName(), localVariable.getType(), localVariable.getValue());
//                eventLocalVariable.setDescription(concatStrings(localVariable.getComment())); // getComment() returns XML comment list
                eventLocalVariable.setDescription(localVariable.getDescription());
                eventLocalVariable.opaqueModelObject = localVariable;

                eventNode.getLocalVariables().add(eventLocalVariable);
            }
        }
        eventNode.setCodeBlockString(event.getCode());
        eventNode.getStateTransitions().clear();

        EventStateTransition eventStateTransition;
        LocalVariableAssignment localVariableAssignment;
        StateVariable stateVariable;
        String index;
        LocalVariableInvocation localVariableInvocation;
        List<String> commentList;
        for (StateTransition stateTransition : event.getStateTransition()) 
        {
            eventStateTransition = new EventStateTransition();

            localVariableAssignment = stateTransition.getLocalVariableAssignment();
            if (localVariableAssignment != null && localVariableAssignment.getValue() != null && !localVariableAssignment.getValue().isEmpty()) 
            {
                eventStateTransition.setLocalVariableAssignment(localVariableAssignment.getValue());
            }

            stateVariable = (StateVariable) stateTransition.getState();
            eventStateTransition.setName(stateVariable.getName());
            eventStateTransition.setType(stateVariable.getType());

            // bug fix 1183
            if (ViskitGlobals.instance().isArray(stateVariable.getType())) {
                index = stateTransition.getIndex();
                eventStateTransition.setIndexingExpression(index);
            }

            eventStateTransition.setOperation(stateTransition.getOperation() != null);
            if (eventStateTransition.isOperation()) 
            {
                eventStateTransition.setOperationOrAssignment(stateTransition.getOperation().getMethod());
            } 
            else {
                eventStateTransition.setOperationOrAssignment(stateTransition.getAssignment().getValue());
            }

            localVariableInvocation = stateTransition.getLocalVariableInvocation();
            if (localVariableInvocation != null && localVariableInvocation.getMethod() != null && !localVariableInvocation.getMethod().isEmpty())
                eventStateTransition.setLocalVariableInvocation(stateTransition.getLocalVariableInvocation().getMethod());

            // obsolete
//            commentList = new ArrayList<>(); // from XML
//            commentList.addAll(stateVariable.getComment());
////            eventStateTransition.setComments(comment);
//            eventStateTransition.setDescription(concatStrings(commentList));
            eventStateTransition.setDescription(stateVariable.getDescription());

            eventStateTransition.opaqueModelObject = stateTransition;
            eventNode.getStateTransitions().add(eventStateTransition);
        }

        Coordinate coor = event.getCoordinate();
        if (coor != null) //todo lose this after all xmls updated
        {
            eventNode.setPosition(new Point2D.Double(
                    Double.parseDouble(coor.getX()),
                    Double.parseDouble(coor.getY())));
        }
    }

    /** Schedule and cancel edges */
    private void buildEdgesFromJaxb(EventNode sourceEventNode, List<Object> objectList)
    {
        for (Object nextObject : objectList) 
        {
            if (nextObject instanceof Schedule) {
                buildScheduleEdgeFromJaxb(sourceEventNode, (Schedule) nextObject);
            } else {
                buildCancelEdgeFromJaxb(sourceEventNode, (Cancel) nextObject);
            }
        }
    }

    private void buildScheduleEdgeFromJaxb(EventNode sourceEventNode, Schedule schedule) 
    {
        SchedulingEdge schedulingEdge = new SchedulingEdge();
        String s;
        schedulingEdge.opaqueModelObject = schedule;

        schedulingEdge.from = sourceEventNode;
        EventNode targetEventNode = buildNodeFromJaxbEvent((Event) schedule.getEvent());
        schedulingEdge.to = targetEventNode;

        sourceEventNode.getConnections().add(schedulingEdge);
        targetEventNode.getConnections().add(schedulingEdge);
        schedulingEdge.conditional = schedule.getCondition();

        // Attempt to avoid NumberFormatException thrown on Double.parseDouble(String s)
        if (Pattern.matches(SchedulingEdge.FLOATING_POINT_REGEX, schedule.getPriority())) {
            s = schedule.getPriority();

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
        } else {

            // We have an enumeration String
            s = schedule.getPriority();
        }

        schedulingEdge.priority = s;

        // Now set the JAXB Schedule to record the Priority enumeration to overwrite
        // numeric Priority values
        schedule.setPriority(schedulingEdge.priority);

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
        schedulingEdge.setDescription(schedule.getDescription());
        schedulingEdge.delay = schedule.getDelay();
        schedulingEdge.parameters = buildEdgeParmsFromJaxb(schedule.getEdgeParameter());

        edgeCache.put(schedule, schedulingEdge);

        setDirty(true);
        this.notifyChanged(new ModelEvent(schedulingEdge, ModelEvent.EDGE_ADDED, "Edge added"));
    }

    private void buildCancelEdgeFromJaxb(EventNode src, Cancel cancel) {
        CancelingEdge cancelingEdge = new CancelingEdge();
        cancelingEdge.opaqueModelObject = cancel;
        cancelingEdge.conditional = cancel.getCondition();

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
        cancelingEdge.setDescription(cancel.getDescription());

        cancelingEdge.parameters = buildEdgeParmsFromJaxb(cancel.getEdgeParameter());

        cancelingEdge.from = src;
        EventNode target = buildNodeFromJaxbEvent((Event) cancel.getEvent());
        cancelingEdge.to = target;

        src.getConnections().add(cancelingEdge);
        target.getConnections().add(cancelingEdge);

        edgeCache.put(cancel, cancelingEdge);

        setDirty(true);
        notifyChanged(new ModelEvent(cancelingEdge, ModelEvent.CANCELING_EDGE_ADDED, "Canceling edge added"));
    }

    private List<ViskitElement> buildEdgeParmsFromJaxb(List<EdgeParameter> lis) {
        List<ViskitElement> alis = new ArrayList<>(3);
        ViskitEdgeParameter vep;
        for (EdgeParameter ep : lis) {
            vep = new ViskitEdgeParameter(ep.getValue());
            alis.add(vep);
        }
        return alis;
    }

    private void buildCodeBlockFromJaxb(String code) {
        code = (code == null) ? "" : code;
        notifyChanged(new ModelEvent(code, ModelEvent.CODEBLOCK_CHANGED, "Code block changed"));
    }

    private void buildStateVariablesFromJaxb(List<StateVariable> stateVariableList) 
    {
        String c;
//        List<String> varCom;
        ViskitStateVariable stateVariable;
        for (StateVariable nextStateVariable : stateVariableList) 
        {
            // obsolete
//            varCom = nextStateVariable.getComment();
//            c = " ";
//            for (String comment : varCom) {
//                c += comment;
//                c += " ";
//            }
            String description = nextStateVariable.getDescription();
            stateVariable = new ViskitStateVariable(nextStateVariable.getName(), nextStateVariable.getType(), description.trim());
            stateVariable.opaqueModelObject = nextStateVariable;

            stateVariables.add(stateVariable);

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

            simulationParameters.add(parameter);

            if (!stateVariableParameterNameCheck()) {
                mangleName(parameter);
            }
            notifyChanged(new ModelEvent(parameter, ModelEvent.SIM_PARAMETER_ADDED, "vParameter added"));
        }
    }

    @Override
    public Vector<ViskitElement> getAllNodes() {
        return new Vector<>(eventNodeCache.values());
    }

    @Override
    public Vector<ViskitElement> getStateVariables() {
        return new Vector<>(stateVariables);
    }

    @Override
    public Vector<ViskitElement> getSimulationParameters() {
        return new Vector<>( simulationParameters);
    }

    // parameter mods
    // --------------
    @Override
    public void newSimulationParameter(String name, String type, String xinitVal, String description) 
    {
        ViskitParameter viskitParameter = new ViskitParameter(name, type, description);
        simulationParameters.add(viskitParameter);

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

        jaxbRoot.getParameter().add(parameter);

        setDirty(true);
        notifyChanged(new ModelEvent(viskitParameter, ModelEvent.SIM_PARAMETER_ADDED, "new simulation parameter"));
    }

    @Override
    public void deleteSimParameter(ViskitParameter vp) {
        // remove jaxb variable
        Iterator<Parameter> spItr = jaxbRoot.getParameter().iterator();
        while (spItr.hasNext()) {
            if (spItr.next() == (Parameter) vp.opaqueModelObject) {
                spItr.remove();
                break;
            }
        }
        simulationParameters.remove(vp);

        setDirty(true);
        notifyChanged(new ModelEvent(vp, ModelEvent.SIM_PARAMETER_DELETED, "vParameter deleted"));
    }

    @Override
    public void changeCodeBlock(String s) {
        jaxbRoot.setCode(s);
        setDirty(true);
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

        setDirty(true);
        notifyChanged(new ModelEvent(newParameter, ModelEvent.SIM_PARAMETER_CHANGED, "vParameter changed"));
        return returnValue;
    }

    // State variable mods
    // -------------------
    @Override
    public void newStateVariable(String name, String type, String xinitVal, String description) {

        // get the new one here and show it around
        ViskitStateVariable newStateVariable = new ViskitStateVariable(name, type, description);
        stateVariables.add(newStateVariable);
        if (!stateVariableParameterNameCheck()) {
            mangleName(newStateVariable);
        }
        StateVariable jaxbStateVariable = this.jaxbEventGraphObjectFactory.createStateVariable();
        jaxbStateVariable.setName(ViskitStatics.nullIfEmpty(name));
        //s.setShortName(nIe(name));
        jaxbStateVariable.setType(ViskitStatics.nullIfEmpty(type));
        jaxbStateVariable.setDescription(description);

        newStateVariable.opaqueModelObject = jaxbStateVariable;
        jaxbRoot.getStateVariable().add(jaxbStateVariable);

        setDirty(true);
        notifyChanged(new ModelEvent(newStateVariable, ModelEvent.STATE_VARIABLE_ADDED, "State variable added"));
    }

    @Override
    public void deleteStateVariable(ViskitStateVariable vsv) {
        // remove jaxb variable
        Iterator<StateVariable> svItr = jaxbRoot.getStateVariable().iterator();
        while (svItr.hasNext()) {
            if (svItr.next() == (StateVariable) vsv.opaqueModelObject) {
                svItr.remove();
                break;
            }
        }
        stateVariables.remove(vsv);

        setDirty(true);
        notifyChanged(new ModelEvent(vsv, ModelEvent.STATE_VARIABLE_DELETED, "State variable deleted"));
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
        stateVariable.setDescription(newStateVariable.getDescription());

        setDirty(true);
        notifyChanged(new ModelEvent(newStateVariable, ModelEvent.STATE_VARIABLE_CHANGED, "State variable changed"));
        return returnValue;
    }

    // Event (node) mods
    // -----------------

    @Override
    public void newEvent(String nodeName, Point2D p) {
        EventNode node = new EventNode(nodeName);
        if (p == null) {
            p = new Point2D.Double(30, 60);
        }
        node.setPosition(p);
        Event jaxbEv = jaxbEventGraphObjectFactory.createEvent();

        eventNodeCache.put(jaxbEv, node);   // key = ev

        // Ensure a unique Event name
        if (!nameCheck()) {
            mangleName(node);
        }

        jaxbEv.setName(ViskitStatics.nullIfEmpty(nodeName));

        if ("Run".equals(ViskitStatics.nullIfEmpty(nodeName))) {
            jaxbEv.setDescription("This event is fired first to facilitate "
                    + "initialization of all simulation state variables");
        }
        node.opaqueModelObject = jaxbEv;
        jaxbRoot.getEvent().add(jaxbEv);

        setDirty(true);
        notifyChanged(new ModelEvent(node, ModelEvent.EVENT_ADDED, "Event added"));
    }

    @Override
    public void redoEvent(EventNode node) {
        if (eventNodeCache.containsValue(node))
            return;

        Event jaxbEv = jaxbEventGraphObjectFactory.createEvent();
        eventNodeCache.put(jaxbEv, node);   // key = evnode.opaqueModelObject = jaxbEv;
        jaxbEv.setName(node.getName());
        node.opaqueModelObject = jaxbEv;
        jaxbRoot.getEvent().add(jaxbEv);

        setDirty(true);
        notifyChanged(new ModelEvent(node, ModelEvent.REDO_EVENT_NODE, "Event Node redone"));
    }

    @Override
    public void deleteEvent(EventNode node) {
        Event jaxbEv = (Event) node.opaqueModelObject;
        eventNodeCache.remove(jaxbEv);
        jaxbRoot.getEvent().remove(jaxbEv);

        setDirty(true);
        if (!eventGraphController.isUndo())
            notifyChanged(new ModelEvent(node, ModelEvent.EVENT_DELETED, "Event deleted"));
        else
            notifyChanged(new ModelEvent(node, ModelEvent.UNDO_EVENT_NODE, "Event undone"));
    }

    private StateVariable findStateVariable(String nm) {
        List<StateVariable> lis = jaxbRoot.getStateVariable();
        for (StateVariable sv : lis) {
            if (sv.getName().equals(nm)) {
                return sv;
            }
        }
        return null;
    }
    private int locVarNameSequence = 0;

    @Override
    public String generateLocalVariableName() {
        String nm = null;
        do {
            nm = privateLocalVariablePrefix + locVarNameSequence++;
        } while (!isUniqueLVorIdxVname(nm));
        return nm;
    }

    @Override
    public void resetLVNameGenerator() {
        locVarNameSequence = 0;
    }
    private int idxVarNameSequence = 0;

    @Override
    public String generateIndexVariableName() {
        String nm = null;
        do {
            nm = privateIndexVariablePrefix + idxVarNameSequence++;
        } while (!isUniqueLVorIdxVname(nm));
        return nm;
    }

    @Override
    public void resetIdxNameGenerator() {
        idxVarNameSequence = 0;
    }

    private boolean isUniqueLVorIdxVname(String nm) {
        String ie;
        for (EventNode event : eventNodeCache.values()) {
            for (ViskitElement lv : event.getLocalVariables()) {
                if (lv.getName().equals(nm)) {
                    return false;
                }
            }
            for (ViskitElement transition : event.getStateTransitions()) {
                ie = transition.getIndexingExpression();
                if (ie != null && ie.equals(nm)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isUniqueSVname(String nm) {
        for (ViskitElement sv : stateVariables) {
            if (sv.getName().equals(nm)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String generateStateVariableName() {
        String nm = null;
        int startnum = 0;
        do {
            nm = stateVariablePrefix + startnum++;
        } while (!isUniqueSVname(nm));
        return nm;
    }

    /**
     * Here we convert local state transition expressions into JAXB bindings
     *
     * @param targ List of StateTransitions to populate
     * @param local List of StateTransitions to transfer to the target
     */
    private void cloneTransitions(List<StateTransition> targ, List<ViskitElement> local) {
        targ.clear();
        StateTransition st;
        String localV, assign, localI, invoke;
        LocalVariableAssignment l;
        StateVariable sv;
        Operation o;
        Assignment a;
        LocalVariableInvocation lvi;
        for (ViskitElement transition : local) {
            st = jaxbEventGraphObjectFactory.createStateTransition();

            // Various locally declared variable ops
            localV = ((EventStateTransition)transition).getLocalVariableAssignment();
            if (localV != null && !localV.isEmpty()) {

                assign = ((EventStateTransition)transition).getLocalVariableAssignment();
                if (assign != null && !assign.isEmpty()) {
                    l = jaxbEventGraphObjectFactory.createLocalVariableAssignment();
                    l.setValue(assign);
                    st.setLocalVariableAssignment(l);
                }
            }

            sv = findStateVariable(transition.getName());

            if (sv == null) {continue;}

            st.setState(sv);

            if (sv.getType() != null && ViskitGlobals.instance().isArray(sv.getType())) {

                // Match the state transition's index to the given index
                st.setIndex(transition.getIndexingExpression());
            }

            if (transition.isOperation()) {
                o = jaxbEventGraphObjectFactory.createOperation();
                o.setMethod(transition.getOperationOrAssignment());
                st.setOperation(o);
            } else {
                a = jaxbEventGraphObjectFactory.createAssignment();
                a.setValue(transition.getOperationOrAssignment());
                st.setAssignment(a);
            }

            // If we have any void return type, zero parameter methods to
            // call on local vars, or args, do it now
            localI = ((EventStateTransition)transition).getLocalVariableInvocation();
            if (localI != null && !localI.isEmpty()) {

                invoke = ((EventStateTransition) transition).getLocalVariableInvocation();
                if (invoke != null && !invoke.isEmpty()) {
                    lvi = jaxbEventGraphObjectFactory.createLocalVariableInvocation();
                    lvi.setMethod(invoke);
                    st.setLocalVariableInvocation(lvi);
                }
            }

            transition.opaqueModelObject = st; //replace
            targ.add(st);
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

    private void cloneArguments(List<Argument> clonedEventArgumentList, List<ViskitElement> originalEventArgumentList) {
        clonedEventArgumentList.clear();
        Argument tempArgument;
        for (ViskitElement eventArgumentElement : originalEventArgumentList) 
        {
            tempArgument = jaxbEventGraphObjectFactory.createArgument();
            tempArgument.setName(ViskitStatics.nullIfEmpty(eventArgumentElement.getName()));
            tempArgument.setType(ViskitStatics.nullIfEmpty(eventArgumentElement.getType()));
//            argument.getComment().clear();
//            argument.getComment().addAll(eventArgumentElement.getDescriptionArray());
            tempArgument.setDescription(eventArgumentElement.getDescription());
            eventArgumentElement.opaqueModelObject = tempArgument; // replace
            clonedEventArgumentList.add(tempArgument);
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
        if (!nameCheck()) {
            mangleName(eventNode);
            returnValue = false;
        }
        Event jaxbEvent = (Event) eventNode.opaqueModelObject;

        jaxbEvent.setName(eventNode.getName());

        double x = eventNode.getPosition().getX();
        double y = eventNode.getPosition().getY();
        Coordinate coordinate = jaxbEventGraphObjectFactory.createCoordinate();
        coordinate.setX("" + x);
        coordinate.setY("" + y);
        eventNode.getPosition().setLocation(x, y);
        jaxbEvent.setCoordinate(coordinate);

        cloneDescription(jaxbEvent.getDescription(), eventNode.getDescription());
        cloneArguments(jaxbEvent.getArgument(), eventNode.getArguments());
        cloneLocalVariables(jaxbEvent.getLocalVariable(), eventNode.getLocalVariables());
        // following must follow above
        cloneTransitions(jaxbEvent.getStateTransition(), eventNode.getStateTransitions());

        jaxbEvent.setCode(eventNode.getCodeBlockString());

        setDirty(true);
        notifyChanged(new ModelEvent(eventNode, ModelEvent.EVENT_CHANGED, "Event changed"));
        return returnValue;
    }

    // Edge mods
    // ---------

    @Override
    public void newSchedulingEdge(EventNode sourceEventNode, EventNode targetEventNode) 
    {
        SchedulingEdge schedulingEdge = new SchedulingEdge();
        schedulingEdge.from = sourceEventNode;
        schedulingEdge.to = targetEventNode;
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
            for (ViskitElement argument : argumentList) {
                edgeParameters.add(new ViskitEdgeParameter(argument.getValue()));
            }
            schedulingEdge.parameters = edgeParameters;
        }
        schedulingEdge.priority = "DEFAULT";  // set default priority

        edgeCache.put(schedule, schedulingEdge);

        setDirty(true);
        notifyChanged(new ModelEvent(schedulingEdge, ModelEvent.EDGE_ADDED, "Scheduling Edge added"));
    }

    @Override
    public void redoSchedulingEdge(Edge edge) {
        if (edgeCache.containsValue(edge))
            return;

        EventNode sourceEventNode, targetEventNode;
        sourceEventNode = (EventNode) ((DefaultMutableTreeNode) edge.from.opaqueViewObject).getUserObject();
        targetEventNode = (EventNode) ((DefaultMutableTreeNode) edge.to.opaqueViewObject).getUserObject();
        Schedule schedule = jaxbEventGraphObjectFactory.createSchedule();
        edge.opaqueModelObject = schedule;
        Event targetEvent = (Event) targetEventNode.opaqueModelObject;
        schedule.setEvent(targetEvent);
        Event sourceEvent = (Event) sourceEventNode.opaqueModelObject;
        sourceEvent.getScheduleOrCancel().add(schedule);
        edgeCache.put(schedule, edge);

        setDirty(true);
        notifyChanged(new ModelEvent(edge, ModelEvent.REDO_SCHEDULING_EDGE, "Scheduling Edge redone"));
    }

    @Override
    public void newCancelingEdge(EventNode sourceEventNode, EventNode targetEventNode) 
    {
        CancelingEdge cancelingEdge = new CancelingEdge();
        cancelingEdge.from = sourceEventNode;
        cancelingEdge.to   = targetEventNode;
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
            cancelingEdge.parameters = edgeParameters;
        }
        edgeCache.put(cancel, cancelingEdge);

        setDirty(true);
        notifyChanged(new ModelEvent(cancelingEdge, ModelEvent.CANCELING_EDGE_ADDED, "Canceling Edge added"));
    }

    @Override
    public void redoCancelingEdge(Edge edge) 
    {
        if (edgeCache.containsValue(edge))
            return;

        EventNode sourceEventNode, targetEventNode;
        sourceEventNode = (EventNode) ((DefaultMutableTreeNode) edge.from.opaqueViewObject).getUserObject();
        targetEventNode = (EventNode) ((DefaultMutableTreeNode) edge.to.opaqueViewObject).getUserObject();
        Cancel cancel = jaxbEventGraphObjectFactory.createCancel();
        edge.opaqueModelObject = cancel;
        Event targetEvent = (Event) targetEventNode.opaqueModelObject;
        cancel.setEvent(targetEvent);
        Event sourceEveny = (Event) sourceEventNode.opaqueModelObject;
        sourceEveny.getScheduleOrCancel().add(cancel);
        edgeCache.put(cancel, edge);

        setDirty(true);
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

        List<Event> nodeList = jaxbRoot.getEvent();
        List<Object> edges;
        for (Event nextEvent : nodeList) {
            edges = nextEvent.getScheduleOrCancel();
            edges.remove(jaxbEdge);
        }
        edgeCache.remove(edge);
        setDirty(true);
    }

    @Override
    public void changeSchedulingEdge(Edge schedulingEdge) 
    {
        Schedule schedule = (Schedule) schedulingEdge.opaqueModelObject;
        schedule.setCondition(schedulingEdge.conditional);
//        schedule.getComment().clear();
//        schedule.getComment().add(schedulingEdge.conditionalDescription);
        schedule.setDescription(schedulingEdge.getDescription());
        schedule.setDelay("" + schedulingEdge.delay);

        schedule.setEvent(schedulingEdge.to.opaqueModelObject);
        schedule.setPriority(((SchedulingEdge)schedulingEdge).priority);
        schedule.getEdgeParameter().clear();

        EdgeParameter edgeParameter;
        // Bug 1373: This is where an edge parameter gets written out to XML
        for (ViskitElement nextEdgeParameter : schedulingEdge.parameters)
        {
            edgeParameter = jaxbEventGraphObjectFactory.createEdgeParameter();
            edgeParameter.setValue(ViskitStatics.nullIfEmpty(nextEdgeParameter.getValue()));
            schedule.getEdgeParameter().add(edgeParameter);
        }
        setDirty(true);
        notifyChanged(new ModelEvent(schedulingEdge, ModelEvent.EDGE_CHANGED, "Edge changed"));
    }

    @Override
    public void changeCancelingEdge(Edge newCancellingEdge )
    {
        Cancel cancel = (Cancel) newCancellingEdge.opaqueModelObject;
        cancel.setCondition(newCancellingEdge.conditional);
        cancel.setEvent(newCancellingEdge.to.opaqueModelObject);
//        cancel.getComment().clear();
//        cancel.getComment().add(newCancellingEdge.conditionalDescription);
        cancel.setDescription(newCancellingEdge.conditionalDescription);

        cancel.getEdgeParameter().clear();
        EdgeParameter edgeParameter;
        for (ViskitElement nextEdgeParameter : newCancellingEdge.parameters) {
            edgeParameter = jaxbEventGraphObjectFactory.createEdgeParameter();
            edgeParameter.setValue(ViskitStatics.nullIfEmpty(nextEdgeParameter.getValue()));
            cancel.getEdgeParameter().add(edgeParameter);
        }
        setDirty(true);
        notifyChanged(new ModelEvent(newCancellingEdge, ModelEvent.CANCELING_EDGE_CHANGED, "Canceling edge changed"));
    }

} // end class file ModelImpl.java
