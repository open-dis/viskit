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

import viskit.ViskitGlobals;
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
    private static JAXBContext jaxbContext;
    ObjectFactory objectFactory;
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
            objectFactory = new ObjectFactory();
            jaxbRoot = objectFactory.createSimEntity(); // to start with empty graph
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
            jaxbRoot = objectFactory.createSimEntity(); // to start with empty graph
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
                List<String> commentList = jaxbRoot.getComment();
                StringBuilder sb = new StringBuilder("");
                for (String comment : commentList) {
                    sb.append(comment);
                    sb.append(" ");
                }
                myGraphMetadata.description = sb.toString().trim();
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
    public boolean saveModel(File f) {
        boolean retVal;
        if (f == null) {
            f = currentFile;
        }
        currentFile = f;

        // Do the marshalling into a temporary file, so as to avoid possible
        // deletion of existing file on a marshal error.

        File tempFile;
        try {
            tempFile = TempFileManager.createTempFile("tempEventGraphMarshal", ".xml");
        } catch (IOException e) {
            eventGraphController.messageUser(JOptionPane.ERROR_MESSAGE,
                    "I/O Error",
                    "Exception creating temporary file, Model.saveModel():" +
                    "\n" + e.getMessage()
                    );
            return false;
        }

        FileWriter fw = null;
        try {
            fw = new FileWriter(tempFile);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, schemaLocation);

            jaxbRoot.setName(nIe(graphMetadata.name));
            jaxbRoot.setVersion(nIe(graphMetadata.version));
            jaxbRoot.setAuthor(nIe(graphMetadata.author));
            jaxbRoot.setPackage(nIe(graphMetadata.packageName));
            jaxbRoot.setExtend(nIe(graphMetadata.extendsPackageName));
            jaxbRoot.setImplement(nIe(graphMetadata.implementsPackageName));
            List<String> clis = jaxbRoot.getComment();
            clis.clear();
            String cmt = nIe(graphMetadata.description);
            if (cmt != null) {
                clis.add(cmt.trim());
            }

            marshaller.marshal(jaxbRoot, fw);

            // OK, made it through the marshal, overwrite the "real" file
            Files.copy(tempFile.toPath(), currentFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            setDirty(false);
            retVal = true;
        } catch (JAXBException e) {
            eventGraphController.messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML I/O Error",
                    "Exception on JAXB marshalling" +
                    "\n" + f.getName() +
                    "\n" + e.getMessage()
                    );
            retVal = false;
        } catch (IOException ex) {
            eventGraphController.messageUser(JOptionPane.ERROR_MESSAGE,
                    "File I/O Error",
                    "Exception on writing " +
                    "\n" + f.getName() +
                    "\n" + ex.getMessage()
                    );
            retVal = false;
        } finally {
            try {
                if (fw != null)
                    fw.close();
            } catch (IOException ioe) {}
        }
        return retVal;
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
        jaxbEvToNode(ev, en);
        en.opaqueModelObject = ev;

        eventNodeCache.put(ev, en);   // key = ev

        // Ensure a unique Event name for XML IDREFs
        if (!nameCheck()) {
            mangleName(en);
        }
        
        notifyChanged(new ModelEvent(en, ModelEvent.EVENT_ADDED, "Event added"));
        return en;
    }

    private String concatStrings(List<String> lis) {
        StringBuilder sb = new StringBuilder();
        for (String s : lis) {
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

    private void jaxbEvToNode(Event ev, EventNode node) {
        node.setName(ev.getName());

        node.getComments().clear();
        node.getComments().addAll(ev.getComment());

        node.getArguments().clear();

        EventArgument ea;
        List<String> com;
        for (Argument arg : ev.getArgument()) {
            ea = new EventArgument();
            ea.setName(arg.getName());
            ea.setType(arg.getType());

            com = new ArrayList<>();
            com.addAll(arg.getComment());
            ea.setComments(com);
            ea.opaqueModelObject = arg;
            node.getArguments().add(ea);
        }

        node.getLocalVariables().clear();
        EventLocalVariable elv;
        for (LocalVariable lv : ev.getLocalVariable()) {
            if (!lv.getName().startsWith(privateIndexVariablePrefix)) {    // only if it's a "public" one
                elv = new EventLocalVariable(lv.getName(), lv.getType(), lv.getValue());
                elv.setComment(concatStrings(lv.getComment()));
                elv.opaqueModelObject = lv;

                node.getLocalVariables().add(elv);
            }
        }

        node.setCodeBLock(ev.getCode());
        node.getTransitions().clear();

        EventStateTransition est;
        LocalVariableAssignment l;
        StateVariable sv;
        String idx;
        LocalVariableInvocation lvi;
        List<String> cmt;
        for (StateTransition st : ev.getStateTransition()) {

            est = new EventStateTransition();

            l = st.getLocalVariableAssignment();
            if (l != null && l.getValue() != null && !l.getValue().isEmpty()) {
                est.setLocalVariableAssignment(l.getValue());
            }

            sv = (StateVariable) st.getState();
            est.setName(sv.getName());
            est.setType(sv.getType());

            // bug fix 1183
            if (ViskitGlobals.instance().isArray(sv.getType())) {
                idx = st.getIndex();
                est.setIndexingExpression(idx);
            }

            est.setOperation(st.getOperation() != null);
            if (est.isOperation()) {
                est.setOperationOrAssignment(st.getOperation().getMethod());
            } else {
                est.setOperationOrAssignment(st.getAssignment().getValue());
            }

            lvi = st.getLocalVariableInvocation();
            if (lvi != null && lvi.getMethod() != null && !lvi.getMethod().isEmpty())
                est.setLocalVariableInvocation(st.getLocalVariableInvocation().getMethod());

            cmt = new ArrayList<>();
            cmt.addAll(sv.getComment());
            est.setComments(cmt);

            est.opaqueModelObject = st;
            node.getTransitions().add(est);
        }

        Coordinate coor = ev.getCoordinate();
        if (coor != null) //todo lose this after all xmls updated
        {
            node.setPosition(new Point2D.Double(
                    Double.parseDouble(coor.getX()),
                    Double.parseDouble(coor.getY())));
        }
    }

    private void buildEdgesFromJaxb(EventNode src, List<Object> lis) {
        for (Object o : lis) {
            if (o instanceof Schedule) {
                buildScheduleEdgeFromJaxb(src, (Schedule) o);
            } else {
                buildCancelEdgeFromJaxb(src, (Cancel) o);
            }
        }
    }

    private void buildScheduleEdgeFromJaxb(EventNode src, Schedule ed) {
        SchedulingEdge se = new SchedulingEdge();
        String s;
        se.opaqueModelObject = ed;

        se.from = src;
        EventNode target = buildNodeFromJaxbEvent((Event) ed.getEvent());
        se.to = target;

        src.getConnections().add(se);
        target.getConnections().add(se);
        se.conditional = ed.getCondition();

        // Attempt to avoid NumberFormatException thrown on Double.parseDouble(String s)
        if (Pattern.matches(SchedulingEdge.FLOATING_POINT_REGEX, ed.getPriority())) {
            s = ed.getPriority();

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
            s = ed.getPriority();
        }

        se.priority = s;

        // Now set the JAXB Schedule to record the Priority enumeration to overwrite
        // numeric Priority values
        ed.setPriority(se.priority);

        List<String> cmt = ed.getComment();
        if (!cmt.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String comment : cmt) {
                sb.append(comment);
                sb.append("  ");
            }
            se.conditionalDescription = sb.toString().trim();
        }
        se.delay = ed.getDelay();
        se.parameters = buildEdgeParmsFromJaxb(ed.getEdgeParameter());

        edgeCache.put(ed, se);

        setDirty(true);
        this.notifyChanged(new ModelEvent(se, ModelEvent.EDGE_ADDED, "Edge added"));
    }

    private void buildCancelEdgeFromJaxb(EventNode src, Cancel ed) {
        CancelingEdge ce = new CancelingEdge();
        ce.opaqueModelObject = ed;
        ce.conditional = ed.getCondition();

        List<String> cmt = ed.getComment();
        if (!cmt.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String comment : cmt) {
                sb.append(comment);
                sb.append("  ");
            }
            ce.conditionalDescription = sb.toString().trim();
        }

        ce.parameters = buildEdgeParmsFromJaxb(ed.getEdgeParameter());

        ce.from = src;
        EventNode target = buildNodeFromJaxbEvent((Event) ed.getEvent());
        ce.to = target;

        src.getConnections().add(ce);
        target.getConnections().add(ce);

        edgeCache.put(ed, ce);

        setDirty(true);
        notifyChanged(new ModelEvent(ce, ModelEvent.CANCELING_EDGE_ADDED, "Canceling edge added"));
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

    private void buildStateVariablesFromJaxb(List<StateVariable> lis) {
        String c;
        List<String> varCom;
        ViskitStateVariable v;
        for (StateVariable var : lis) {
            varCom = var.getComment();
            c = " ";
            for (String comment : varCom) {
                c += comment;
                c += " ";
            }
            v = new ViskitStateVariable(var.getName(), var.getType(), c.trim());
            v.opaqueModelObject = var;

            stateVariables.add(v);

            if (!stateVariableParameterNameCheck()) {
                mangleName(v);
            }
            notifyChanged(new ModelEvent(v, ModelEvent.STATE_VARIABLE_ADDED, "New state variable"));
        }
    }

    private void buildParametersFromJaxb(List<Parameter> lis) {
        List<String> pCom;
        String c;
        ViskitParameter vp;
        for (Parameter p : lis) {
            pCom = p.getComment();
            c = " ";
            for (String comment : pCom) {
                c += comment;
                c += " ";
            }
            vp = new ViskitParameter(p.getName(), p.getType(), c.trim());
            vp.opaqueModelObject = p;

            simulationParameters.add(vp);

            if (!stateVariableParameterNameCheck()) {
                mangleName(vp);
            }
            notifyChanged(new ModelEvent(vp, ModelEvent.SIM_PARAMETER_ADDED, "vParameter added"));
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
        Parameter parameter = this.objectFactory.createParameter();
        parameter.setName(nIe(name));
        parameter.setType(nIe(type));
        parameter.getComment().add(description);

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
    public boolean changeSimParameter(ViskitParameter vp) {
        boolean retcode = true;
        if (!stateVariableParameterNameCheck()) {
            mangleName(vp);
            retcode = false;
        }
        // fill out jaxb variable
        Parameter p = (Parameter) vp.opaqueModelObject;
        p.setName(nIe(vp.getName()));
        //p.setShortName(vp.getName());
        p.setType(nIe(vp.getType()));
        p.getComment().clear();
        p.getComment().add(vp.getComment());

        setDirty(true);
        notifyChanged(new ModelEvent(vp, ModelEvent.SIM_PARAMETER_CHANGED, "vParameter changed"));
        return retcode;
    }

    // State variable mods
    // -------------------
    @Override
    public void newStateVariable(String name, String type, String xinitVal, String comment) {

        // get the new one here and show it around
        ViskitStateVariable vsv = new ViskitStateVariable(name, type, comment);
        stateVariables.add(vsv);
        if (!stateVariableParameterNameCheck()) {
            mangleName(vsv);
        }
        StateVariable s = this.objectFactory.createStateVariable();
        s.setName(nIe(name));
        //s.setShortName(nIe(name));
        s.setType(nIe(type));
        s.getComment().add(comment);

        vsv.opaqueModelObject = s;
        jaxbRoot.getStateVariable().add(s);

        setDirty(true);
        notifyChanged(new ModelEvent(vsv, ModelEvent.STATE_VARIABLE_ADDED, "State variable added"));
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
    public boolean changeStateVariable(ViskitStateVariable vsv) {
        boolean retcode = true;
        if (!stateVariableParameterNameCheck()) {
            mangleName(vsv);
            retcode = false;
        }
        // fill out jaxb variable
        StateVariable sv = (StateVariable) vsv.opaqueModelObject;
        sv.setName(nIe(vsv.getName()));
        sv.setType(nIe(vsv.getType()));
        sv.getComment().clear();
        sv.getComment().add(vsv.getComment());

        setDirty(true);
        notifyChanged(new ModelEvent(vsv, ModelEvent.STATE_VARIABLE_CHANGED, "State variable changed"));
        return retcode;
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
        Event jaxbEv = objectFactory.createEvent();

        eventNodeCache.put(jaxbEv, node);   // key = ev

        // Ensure a unique Event name
        if (!nameCheck()) {
            mangleName(node);
        }

        jaxbEv.setName(nIe(nodeName));

        if ("Run".equals(nIe(nodeName))) {
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

        Event jaxbEv = objectFactory.createEvent();
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
            for (ViskitElement transition : event.getTransitions()) {
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
            st = objectFactory.createStateTransition();

            // Various locally declared variable ops
            localV = ((EventStateTransition)transition).getLocalVariableAssignment();
            if (localV != null && !localV.isEmpty()) {

                assign = ((EventStateTransition)transition).getLocalVariableAssignment();
                if (assign != null && !assign.isEmpty()) {
                    l = objectFactory.createLocalVariableAssignment();
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
                o = objectFactory.createOperation();
                o.setMethod(transition.getOperationOrAssignment());
                st.setOperation(o);
            } else {
                a = objectFactory.createAssignment();
                a.setValue(transition.getOperationOrAssignment());
                st.setAssignment(a);
            }

            // If we have any void return type, zero parameter methods to
            // call on local vars, or args, do it now
            localI = ((EventStateTransition)transition).getLocalVariableInvocation();
            if (localI != null && !localI.isEmpty()) {

                invoke = ((EventStateTransition) transition).getLocalVariableInvocation();
                if (invoke != null && !invoke.isEmpty()) {
                    lvi = objectFactory.createLocalVariableInvocation();
                    lvi.setMethod(invoke);
                    st.setLocalVariableInvocation(lvi);
                }
            }

            transition.opaqueModelObject = st; //replace
            targ.add(st);
        }
    }

    private void cloneComments(List<String> targ, List<String> local) {
        targ.clear();
        targ.addAll(local);
    }

    private void cloneArguments(List<Argument> targ, List<ViskitElement> local) {
        targ.clear();
        Argument arg;
        for (ViskitElement eventArguments : local) {
            arg = objectFactory.createArgument();
            arg.setName(nIe(eventArguments.getName()));
            arg.setType(nIe(eventArguments.getType()));
            arg.getComment().clear();
            arg.getComment().addAll(eventArguments.getDescriptionArray());
            eventArguments.opaqueModelObject = arg; // replace
            targ.add(arg);
        }
    }

    private void cloneLocalVariables(List<LocalVariable> targ, List<ViskitElement> local) {
        targ.clear();
        LocalVariable lvar;
        for (ViskitElement eventLocalVariables : local) {
            lvar = objectFactory.createLocalVariable();
            lvar.setName(nIe(eventLocalVariables.getName()));
            lvar.setType(nIe(eventLocalVariables.getType()));
            lvar.setValue(nIe(eventLocalVariables.getValue()));
            lvar.getComment().clear();
            lvar.getComment().add(eventLocalVariables.getComment());
            eventLocalVariables.opaqueModelObject = lvar; //replace
            targ.add(lvar);
        }
    }

    @Override
    public boolean changeEvent(EventNode node) {
        boolean retcode = true;

        // Ensure a unique Event name
        if (!nameCheck()) {
            mangleName(node);
            retcode = false;
        }
        Event jaxbEv = (Event) node.opaqueModelObject;

        jaxbEv.setName(node.getName());

        double x = node.getPosition().getX();
        double y = node.getPosition().getY();
        Coordinate coor = objectFactory.createCoordinate();
        coor.setX("" + x);
        coor.setY("" + y);
        node.getPosition().setLocation(x, y);
        jaxbEv.setCoordinate(coor);

        cloneComments(jaxbEv.getComment(), node.getComments());
        cloneArguments(jaxbEv.getArgument(), node.getArguments());
        cloneLocalVariables(jaxbEv.getLocalVariable(), node.getLocalVariables());
        // following must follow above
        cloneTransitions(jaxbEv.getStateTransition(), node.getTransitions());

        jaxbEv.setCode(node.getCodeBlock());

        setDirty(true);
        notifyChanged(new ModelEvent(node, ModelEvent.EVENT_CHANGED, "Event changed"));
        return retcode;
    }

    // Edge mods
    // ---------

    @Override
    public void newSchedulingEdge(EventNode src, EventNode target) {
        SchedulingEdge se = new SchedulingEdge();
        se.from = src;
        se.to = target;
        src.getConnections().add(se);
        target.getConnections().add(se);

        Schedule sch = objectFactory.createSchedule();

        se.opaqueModelObject = sch;
        Event targEv = (Event) target.opaqueModelObject;
        sch.setEvent(targEv);
        Event srcEv = (Event) src.opaqueModelObject;
        srcEv.getScheduleOrCancel().add(sch);

        // Put in dummy edge parameters to match the target arguments
        List<ViskitElement> args = target.getArguments();
        List<ViskitElement> edgeParameters;
        if (!args.isEmpty()) {
            edgeParameters = new ArrayList<>(args.size());
            for (ViskitElement arg : args) {
                edgeParameters.add(new ViskitEdgeParameter(arg.getValue()));
            }
            se.parameters = edgeParameters;
        }

        se.priority = "DEFAULT";  // set default

        edgeCache.put(sch, se);

        setDirty(true);
        notifyChanged(new ModelEvent(se, ModelEvent.EDGE_ADDED, "Scheduling Edge added"));
    }

    @Override
    public void redoSchedulingEdge(Edge ed) {
        if (edgeCache.containsValue(ed))
            return;

        EventNode src, target;
        src = (EventNode) ((DefaultMutableTreeNode) ed.from.opaqueViewObject).getUserObject();
        target = (EventNode) ((DefaultMutableTreeNode) ed.to.opaqueViewObject).getUserObject();
        Schedule sched = objectFactory.createSchedule();
        ed.opaqueModelObject = sched;
        Event targEv = (Event) target.opaqueModelObject;
        sched.setEvent(targEv);
        Event srcEv = (Event) src.opaqueModelObject;
        srcEv.getScheduleOrCancel().add(sched);
        edgeCache.put(sched, ed);

        setDirty(true);
        notifyChanged(new ModelEvent(ed, ModelEvent.REDO_SCHEDULING_EDGE, "Scheduling Edge redone"));
    }

    @Override
    public void newCancelingEdge(EventNode src, EventNode target) {
        CancelingEdge ce = new CancelingEdge();
        ce.from = src;
        ce.to = target;
        src.getConnections().add(ce);
        target.getConnections().add(ce);

        Cancel can = objectFactory.createCancel();

        ce.opaqueModelObject = can;
        Event targEv = (Event) target.opaqueModelObject;
        can.setEvent(targEv);
        Event srcEv = (Event) src.opaqueModelObject;
        srcEv.getScheduleOrCancel().add(can);

        // Put in dummy edge parameters to match the target arguments
        List<ViskitElement> args = target.getArguments();
        List<ViskitElement> edgeParameters;
        if (!args.isEmpty()) {
            edgeParameters = new ArrayList<>(args.size());
            for (ViskitElement arg : args) {
                edgeParameters.add(new ViskitEdgeParameter(arg.getValue()));
            }
            ce.parameters = edgeParameters;
        }

        edgeCache.put(can, ce);

        setDirty(true);
        notifyChanged(new ModelEvent(ce, ModelEvent.CANCELING_EDGE_ADDED, "Canceling Edge added"));
    }

    @Override
    public void redoCancelingEdge(Edge ed) {
        if (edgeCache.containsValue(ed))
            return;

        EventNode src, target;
        src = (EventNode) ((DefaultMutableTreeNode) ed.from.opaqueViewObject).getUserObject();
        target = (EventNode) ((DefaultMutableTreeNode) ed.to.opaqueViewObject).getUserObject();
        Cancel can = objectFactory.createCancel();
        ed.opaqueModelObject = can;
        Event targEv = (Event) target.opaqueModelObject;
        can.setEvent(targEv);
        Event srcEv = (Event) src.opaqueModelObject;
        srcEv.getScheduleOrCancel().add(can);
        edgeCache.put(can, ed);

        setDirty(true);
        notifyChanged(new ModelEvent(ed, ModelEvent.REDO_CANCELING_EDGE, "Canceling Edge redone"));
    }

    @Override
    public void deleteSchedulingEdge(Edge edge) {
        _commonEdgeDelete(edge);

        if (!eventGraphController.isUndo())
            notifyChanged(new ModelEvent(edge, ModelEvent.EDGE_DELETED, "Edge deleted"));
        else
            notifyChanged(new ModelEvent(edge, ModelEvent.UNDO_SCHEDULING_EDGE, "Edge undone"));
    }

    @Override
    public void deleteCancelingEdge(Edge edge) {
        _commonEdgeDelete(edge);

        if (!eventGraphController.isUndo())
            notifyChanged(new ModelEvent(edge, ModelEvent.CANCELING_EDGE_DELETED, "Canceling edge deleted"));
        else
            notifyChanged(new ModelEvent(edge, ModelEvent.UNDO_CANCELING_EDGE, "Canceling edge undone"));
    }

    private void _commonEdgeDelete(Edge edg) {
        Object jaxbEdge = edg.opaqueModelObject;

        List<Event> nodes = jaxbRoot.getEvent();
        List<Object> edges;
        for (Event ev : nodes) {
            edges = ev.getScheduleOrCancel();
            edges.remove(jaxbEdge);
        }

        edgeCache.remove(edg);
        setDirty(true);
    }

    @Override
    public void changeSchedulingEdge(Edge e) {
        Schedule sch = (Schedule) e.opaqueModelObject;
        sch.setCondition(e.conditional);
        sch.getComment().clear();
        sch.getComment().add(e.conditionalDescription);
        sch.setDelay("" + e.delay);

        sch.setEvent(e.to.opaqueModelObject);
        sch.setPriority(((SchedulingEdge)e).priority);
        sch.getEdgeParameter().clear();

        EdgeParameter p;
        // Bug 1373: This is where an edge parameter gets written out to XML
        for (ViskitElement edgeParameter : e.parameters) {
            p = objectFactory.createEdgeParameter();
            p.setValue(nIe(edgeParameter.getValue()));
            sch.getEdgeParameter().add(p);
        }

        setDirty(true);
        notifyChanged(new ModelEvent(e, ModelEvent.EDGE_CHANGED, "Edge changed"));
    }

    @Override
    public void changeCancelingEdge(Edge e) {
        Cancel can = (Cancel) e.opaqueModelObject;
        can.setCondition(e.conditional);
        can.setEvent(e.to.opaqueModelObject);
        can.getComment().clear();
        can.getComment().add(e.conditionalDescription);

        can.getEdgeParameter().clear();
        EdgeParameter p;
        for (ViskitElement edgeParameter : e.parameters) {
            p = objectFactory.createEdgeParameter();
            p.setValue(nIe(edgeParameter.getValue()));
            can.getEdgeParameter().add(p);
        }

        setDirty(true);
        notifyChanged(new ModelEvent(e, ModelEvent.CANCELING_EDGE_CHANGED, "Canceling edge changed"));
    }

    /**
     * "nullIfEmpty" returns the passed string if non-zero length, else null
     * @param s the string to evaluate for nullity
     * @return the passed string if non-zero length, else null
     */
    private String nIe(String s) {
        if (s != null) {
            if (s.isEmpty()) {
                s = null;
            }
        }
        return s;
    }

} // end class file ModelImpl.java
