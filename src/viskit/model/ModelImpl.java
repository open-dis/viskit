package viskit.model;

import edu.nps.util.FileIO;
import edu.nps.util.TempFileManager;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import viskit.control.EventGraphController;
import viskit.mvc.mvcAbstractModel;
import viskit.util.XMLValidationTool;
import viskit.xsd.bindings.eventgraph.*;

/**
 * This is the "master" model of an event graph.  It should hold the node, edge and assembly
 * information.  What hasn't been done is to put in accessor methods for the view to
 * read pieces that it needs, say after it receives a "new model" event.
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
public class ModelImpl extends mvcAbstractModel implements Model {

    JAXBContext jc;
    ObjectFactory oFactory;
    SimEntity jaxbRoot;
    File currentFile;
    Map<Event, EventNode> evNodeCache = new HashMap<>();
    Map<Object, Edge> edgeCache = new HashMap<>();
    Vector<ViskitElement> stateVariables = new Vector<>();
    Vector<ViskitElement> simParameters = new Vector<>();
    private String schemaLoc = XMLValidationTool.EVENT_GRAPH_SCHEMA;
    private String privateIdxVarPrefix = "_idxvar_";
    private String privateLocVarPrefix = "locvar_";
    private String stateVarPrefix = "state_";
    private GraphMetaData metaData;
    private EventGraphController controller;
    private boolean modelDirty = false;
    private boolean numericPriority;

    public ModelImpl(EventGraphController controller) {
        this.controller = controller;
        metaData = new GraphMetaData(this);
    }

    @Override
    public void init() {
        try {
            jc = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
            oFactory = new ObjectFactory();
            jaxbRoot = oFactory.createSimEntity(); // to start with empty graph
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
    public void setDirty(boolean dirt) {
        modelDirty = dirt;
    }

    @Override
    public GraphMetaData getMetaData() {
        return metaData;
    }

    @Override
    public void changeMetaData(GraphMetaData gmd) {
        metaData = gmd;
        setDirty(true);
        notifyChanged(new ModelEvent(gmd, ModelEvent.METADATA_CHANGED, "Metadata changed"));
    }

    @Override
    public boolean newModel(File f) {
        stateVariables.removeAllElements();
        simParameters.removeAllElements();
        evNodeCache.clear();
        edgeCache.clear();
        this.notifyChanged(new ModelEvent(this, ModelEvent.NEWMODEL, "New empty model"));

        if (f == null) {
            jaxbRoot = oFactory.createSimEntity(); // to start with empty graph
        } else {
            try {
                Unmarshaller u = jc.createUnmarshaller();
                jaxbRoot = (SimEntity) u.unmarshal(f);

                GraphMetaData mymetaData = new GraphMetaData(this);
                mymetaData.author = jaxbRoot.getAuthor();
                mymetaData.version = jaxbRoot.getVersion();
                mymetaData.name = jaxbRoot.getName();
                mymetaData.packageName = jaxbRoot.getPackage();
                mymetaData.extendsPackageName = jaxbRoot.getExtend();
                mymetaData.implementsPackageName = jaxbRoot.getImplement();
                List<String> lis = jaxbRoot.getComment();
                StringBuilder sb = new StringBuilder("");
                for (String comment : lis) {
                    sb.append(comment);
                    sb.append(" ");
                }
                mymetaData.description = sb.toString().trim();
                changeMetaData(mymetaData);

                buildEventsFromJaxb(jaxbRoot.getEvent());
                buildParametersFromJaxb(jaxbRoot.getParameter());
                buildStateVariablesFromJaxb(jaxbRoot.getStateVariable());
                buildCodeBlockFromJaxb(jaxbRoot.getCode());
            } catch (JAXBException ee) {
                // want a clear way to know if they're trying to load an assembly vs. some unspecified XML.
                try {
                    JAXBContext assyCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
                    Unmarshaller um = assyCtx.createUnmarshaller();
                    um.unmarshal(f);
                    // If we get here, they've tried to load an assembly.
                    controller.messageUser(JOptionPane.ERROR_MESSAGE,
                            "Wrong File Format",
                            "Use the assembly editor to" +
                            "\n" + "work with this file."
                            );
                } catch (JAXBException e) {
                    controller.messageUser(JOptionPane.ERROR_MESSAGE,
                            "XML I/O Error",
                            "Exception on JAXB unmarshalling of" +
                            "\n" + f.getName() +
                            "\nError is: " + e.getMessage() +
                            "\nin Model.newModel(File)"
                            );
                }
                return false;    // from either error case
            }
        }
        currentFile = f;

        // required for initial file loading
        setDirty(false);
        return true;
    }

    @Override
    public boolean saveModel(File f) {
        boolean retVal;
        if (f == null) {
            f = currentFile;
        }

        // Do the marshalling into a temporary file, so as to avoid possible
        // deletion of existing file on a marshal error.

        File tmpF;
        FileWriter fw = null;
        try {
            tmpF = TempFileManager.createTempFile("tmpEGmarshal", ".xml");
        } catch (IOException e) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "I/O Error",
                    "Exception creating temporary file, Model.saveModel():" +
                    "\n" + e.getMessage()
                    );
            return false;
        }

        try {
            fw = new FileWriter(tmpF);
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, schemaLoc);

            jaxbRoot.setName(nIe(metaData.name));
            jaxbRoot.setVersion(nIe(metaData.version));
            jaxbRoot.setAuthor(nIe(metaData.author));
            jaxbRoot.setPackage(nIe(metaData.packageName));
            jaxbRoot.setExtend(nIe(metaData.extendsPackageName));
            jaxbRoot.setImplement(nIe(metaData.implementsPackageName));
            List<String> clis = jaxbRoot.getComment();
            clis.clear();
            String cmt = nIe(metaData.description);
            if (cmt != null) {
                clis.add(cmt.trim());
            }

            m.marshal(jaxbRoot, fw);

            // OK, made it through the marshal, overwrite the "real" file
            FileIO.copyFile(tmpF, f, true);

            setDirty(false);
            currentFile = f;
            retVal = true;
        } catch (JAXBException e) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML I/O Error",
                    "Exception on JAXB marshalling" +
                    "\n" + f.getName() +
                    "\n" + e.getMessage()
                    );
            retVal = false;
        } catch (IOException ex) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "File I/O Error",
                    "Exception on writing " + f.getName() +
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
        for (Event ev : lis) {
            EventNode en = buildNodeFromJaxbEvent(ev);
            buildEdgesFromJaxb(en, ev.getScheduleOrCancel());
        }
    }

    private EventNode buildNodeFromJaxbEvent(Event ev) {
        EventNode en = evNodeCache.get(ev);
        if (en != null) {
            return en;
        }
        en = new EventNode(ev.getName());
        jaxbEvToNode(ev, en);
        en.opaqueModelObject = ev;

        evNodeCache.put(ev, en);   // key = ev

        // Ensure a unique Event name
        if (!eventNameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "Duplicate Event Name",
                    "XML file contains duplicate event name: " + en.getName() +
                    "\nUnique name substituted.");
            mangleNodeName(en);
        }
        notifyChanged(new ModelEvent(en, ModelEvent.EVENTADDED, "Event added"));

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

    private void mangleNodeName(EventNode node) {
        do {
            node.setName(mangleName(node.getName()));
        } while (!eventNameCheck());
    }

    private void mangleStateVarName(vStateVariable vsv) {
        do {
            vsv.setName(mangleName(vsv.getName()));
        } while (!stateVarParamNameCheck());
    }

    private void mangleParamName(vParameter vp) {
        do {
            vp.setName(mangleName(vp.getName()));
        } while (!stateVarParamNameCheck());
    }

    private boolean eventNameCheck() {
        Set<String> hs = new HashSet<>(10);
        for (EventNode en : evNodeCache.values()) {
            if (!hs.add(en.getName())) {
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

    private boolean stateVarParamNameCheck() {
        Set<String> hs = new HashSet<>(10);
        for (ViskitElement sv : stateVariables) {
            if (!hs.add(sv.getName())) {
                return false;
            }
        }
        for (ViskitElement sp : simParameters) {
            if (!hs.add(sp.getName())) {
                return false;
            }
        }
        return true;
    }

    private void jaxbEvToNode(Event ev, EventNode node) {
        node.setName(ev.getName());

        Coordinate coor = ev.getCoordinate();
        if (coor != null) //todo lose this after all xmls updated
        {
            node.setPosition(new Point2D.Double(Double.parseDouble(coor.getX()),
                    Double.parseDouble(coor.getY())));
        }

        node.getComments().clear();
        node.getComments().addAll(ev.getComment());
        node.setCodeBLock(ev.getCode());
        node.getLocalVariables().clear();

        for (LocalVariable lv : ev.getLocalVariable()) {
            if (!lv.getName().startsWith(privateIdxVarPrefix)) {    // only if it's a "public" one
                EventLocalVariable elv = new EventLocalVariable(
                        lv.getName(), lv.getType(), lv.getValue());
                elv.setComment(concatStrings(lv.getComment()));
                elv.opaqueModelObject = lv;

                node.getLocalVariables().add(elv);
            }
        }

        node.getArguments().clear();
        for (Argument arg : ev.getArgument()) {
            EventArgument ea = new EventArgument();
            ea.setName(arg.getName());
            ea.setType(arg.getType());

            List<String> com = new ArrayList<>();
            com.addAll(arg.getComment());
            ea.setComments(com);
            ea.opaqueModelObject = arg;
            node.getArguments().add(ea);
        }

        node.getTransitions().clear();
        for (StateTransition st : ev.getStateTransition()) {
            EventStateTransition est = new EventStateTransition();
            StateVariable sv = (StateVariable) st.getState();
            est.setStateVarName(sv.getName());
            est.setStateVarType(sv.getType());

            // bug fix 1183
            if (sv.getType().contains("[")) {
                String idx = st.getIndex();
                est.setIndexingExpression(idx);
            }

            est.setOperation(st.getOperation() != null);
            if (est.isOperation()) {
                est.setOperationOrAssignment(st.getOperation().getMethod());
            } else {
                est.setOperationOrAssignment(st.getAssignment().getValue());
            }

            List<String> cmt = new ArrayList<>();
            cmt.addAll(sv.getComment());
            est.setComments(cmt);

            est.opaqueModelObject = st;
            node.getTransitions().add(est);
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

        this.notifyChanged(new ModelEvent(se, ModelEvent.EDGEADDED, "Edge added"));
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

        notifyChanged(new ModelEvent(ce, ModelEvent.CANCELINGEDGEADDED, "Canceling edge added"));
    }

    private List<ViskitElement> buildEdgeParmsFromJaxb(List<EdgeParameter> lis) {
        List<ViskitElement> alis = new ArrayList<>(3);
        for (EdgeParameter ep : lis) {
            vEdgeParameter vep = new vEdgeParameter(ep.getValue());
            alis.add(vep);
        }
        return alis;
    }

    private void buildCodeBlockFromJaxb(String code) {
        code = (code == null) ? "" : code;

        notifyChanged(new ModelEvent(code, ModelEvent.CODEBLOCKCHANGED, "Code block changed"));
    }

    private void buildStateVariablesFromJaxb(List<StateVariable> lis) {
        for (StateVariable var : lis) {
            List<String> varCom = var.getComment();
            String c = " ";
            for (String comment : varCom) {
                c += comment;
                c += " ";
            }
            vStateVariable v = new vStateVariable(var.getName(), var.getType(), c.trim());
            v.opaqueModelObject = var;

            stateVariables.add(v);

            if (!stateVarParamNameCheck()) {
                controller.messageUser(JOptionPane.ERROR_MESSAGE,
                        "Duplicate Variable Name",
                        "XML file contains duplicate state variable name." +
                        "\nUnique name substituted.");
                mangleStateVarName(v);
            }

            notifyChanged(new ModelEvent(v, ModelEvent.STATEVARIABLEADDED, "New state variable"));
        }
    }

    private void buildParametersFromJaxb(List<Parameter> lis) {
        for (Parameter p : lis) {
            List<String> pCom = p.getComment();
            String c = " ";
            for (String comment : pCom) {
                c += comment;
                c += " ";
            }
            vParameter vp = new vParameter(p.getName(), p.getType(), c.trim());
            vp.opaqueModelObject = p;

            simParameters.add(vp);

            if (!stateVarParamNameCheck()) {
                controller.messageUser(JOptionPane.ERROR_MESSAGE,
                        "Duplicate Parameter Name",
                        "XML file contains duplicate parameter name." +
                        "\nUnique name substituted.");
                mangleParamName(vp);
            }
            notifyChanged(new ModelEvent(vp, ModelEvent.SIMPARAMETERADDED, "New sim parameter"));
        }
    }

    @Override
    public Vector<ViskitElement> getAllNodes() {
        return new Vector<ViskitElement>(evNodeCache.values());
    }

    // TODO: Known unchecked cast to ViskitElement
    @SuppressWarnings("unchecked")
    @Override
    public Vector<ViskitElement> getStateVariables() {
        return (Vector<ViskitElement>) stateVariables.clone();
    }

    // TODO: Known unchecked cast to ViskitElement
    @SuppressWarnings("unchecked")
    @Override
    public Vector<ViskitElement> getSimParameters() {
        return (Vector<ViskitElement>) simParameters.clone();
    }

    // parameter mods
    // --------------
    @Override
    public void newSimParameter(String nm, String typ, String xinitVal, String comment) {
        setDirty(true);

        vParameter vp = new vParameter(nm, typ, comment);
        simParameters.add(vp);

        if (!stateVarParamNameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "Duplicate Parameter Name",
                    "Duplicate parameter name detected: " + nm +
                    "\nUnique name substituted.");
            mangleParamName(vp);
        }

        //p.setValue(initVal);
        Parameter p = this.oFactory.createParameter();
        p.setName(nIe(nm));
        //p.setShortName(nm);
        p.setType(nIe(typ));
        p.getComment().add(comment);

        vp.opaqueModelObject = p;

        jaxbRoot.getParameter().add(p);

        notifyChanged(new ModelEvent(vp, ModelEvent.SIMPARAMETERADDED, "vParameter added"));
    }

    @Override
    public void deleteSimParameter(vParameter vp) {
        // remove jaxb variable
        Iterator<Parameter> spItr = jaxbRoot.getParameter().iterator();
        while (spItr.hasNext()) {
            if (spItr.next() == (Parameter) vp.opaqueModelObject) {
                spItr.remove();
                break;
            }
        }
        setDirty(true);
        simParameters.remove(vp);
        notifyChanged(new ModelEvent(vp, ModelEvent.SIMPARAMETERDELETED, "vParameter deleted"));
    }

    @Override
    public void changeCodeBlock(String s) {
        jaxbRoot.setCode(s);
        setDirty(true);
    }

    @Override
    public boolean changeSimParameter(vParameter vp) {
        boolean retcode = true;
        if (!stateVarParamNameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "Duplicate Parameter Name",
                    "Duplicate parameter name detected: " + vp.getName() +
                    "\nUnique name substituted.");
            mangleParamName(vp);
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
        notifyChanged(new ModelEvent(vp, ModelEvent.SIMPARAMETERCHANGED, "vParameter changed"));
        return retcode;
    }

    // State variable mods
    // -------------------
    @Override
    public void newStateVariable(String name, String type, String xinitVal, String comment) {
        setDirty(true);

        // get the new one here and show it around
        vStateVariable vsv = new vStateVariable(name, type, comment);
        stateVariables.add(vsv);
        if (!stateVarParamNameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "Duplicate Variable Name",
                    "Duplicate state variable name detected: " + name +
                    "\nUnique name substituted.");
            mangleStateVarName(vsv);
        }
        StateVariable s = this.oFactory.createStateVariable();
        s.setName(nIe(name));
        //s.setShortName(nIe(name));
        s.setType(nIe(type));
        s.getComment().add(comment);

        vsv.opaqueModelObject = s;
        jaxbRoot.getStateVariable().add(s);
        notifyChanged(new ModelEvent(vsv, ModelEvent.STATEVARIABLEADDED, "State variable added"));
    }

    @Override
    public void deleteStateVariable(vStateVariable vsv) {
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
        notifyChanged(new ModelEvent(vsv, ModelEvent.STATEVARIABLEDELETED, "State variable deleted"));
    }

    @Override
    public boolean changeStateVariable(vStateVariable vsv) {
        boolean retcode = true;
        if (!stateVarParamNameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "Duplicate Variable Name",
                    "Duplicate state variable name detected: " + vsv.getName() +
                    "\nUnique name substituted.");
            mangleStateVarName(vsv);
            retcode = false;
        }
        // fill out jaxb variable
        StateVariable sv = (StateVariable) vsv.opaqueModelObject;
        sv.setName(nIe(vsv.getName()));
        //sv.setShortName(nIe(vsv.getName()));
        sv.setType(nIe(vsv.getType()));
        sv.getComment().clear();
        sv.getComment().add(vsv.getComment());

        setDirty(true);
        notifyChanged(new ModelEvent(vsv, ModelEvent.STATEVARIABLECHANGED, "State variable changed"));
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
        Event jaxbEv = oFactory.createEvent();

        evNodeCache.put(jaxbEv, node);   // key = ev

        // Ensure a unique Event name
        if (!eventNameCheck()) {
            mangleNodeName(node);
        }

        jaxbEv.setName(nIe(nodeName));

        if ("Run".equals(nIe(nodeName))) {
            jaxbEv.setDescription("This event is fired first to facilitate initialization of all simulation state variables");
        }
        node.opaqueModelObject = jaxbEv;
        jaxbRoot.getEvent().add(jaxbEv);

        setDirty(true);
        notifyChanged(new ModelEvent(node, ModelEvent.EVENTADDED, "Event added"));
    }

    @Override
    public void deleteEvent(EventNode node) {
        Event jaxbEv = (Event) node.opaqueModelObject;
        evNodeCache.remove(jaxbEv);
        jaxbRoot.getEvent().remove(jaxbEv);

        setDirty(true);
        this.notifyChanged(new ModelEvent(node, ModelEvent.EVENTDELETED, "Event deleted"));
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
            nm = privateLocVarPrefix + locVarNameSequence++;
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
            nm = privateIdxVarPrefix + idxVarNameSequence++;
        } while (!isUniqueLVorIdxVname(nm));
        return nm;
    }

    @Override
    public void resetIdxNameGenerator() {
        idxVarNameSequence = 0;
    }

    private boolean isUniqueLVorIdxVname(String nm) {
        for (EventNode event : evNodeCache.values()) {
            for (ViskitElement lv : event.getLocalVariables()) {
                if (lv.getName().equals(nm)) {
                    return false;
                }
            }
            for (ViskitElement transition : event.getTransitions()) {
                String ie = transition.getIndexingExpression();
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
            nm = stateVarPrefix + startnum++;
        } while (!isUniqueSVname(nm));
        return nm;
    }

    /**
     * Here we convert indexing expressions into local variable references
     * @param targ
     * @param local
     */
    private void cloneTransitions(List<StateTransition> targ, List<ViskitElement> local) {
        targ.clear();
        for (ViskitElement transition : local) {
            StateTransition st = oFactory.createStateTransition();
            StateVariable sv = findStateVariable(transition.getStateVarName());
            st.setState(sv);

            if (sv.getType() != null && sv.getType().contains("[")) {

                // Match the state transition's index to the given index
                st.setIndex(transition.getIndexingExpression());
            }
            if (transition.isOperation()) {
                Operation o = oFactory.createOperation();
                o.setMethod(transition.getOperationOrAssignment());
                st.setOperation(o);
            } else {
                Assignment a = oFactory.createAssignment();
                a.setValue(transition.getOperationOrAssignment());
                st.setAssignment(a);
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
        for (ViskitElement eventArguments : local) {
            Argument arg = oFactory.createArgument();
            arg.setName(nIe(eventArguments.getName()));
            arg.setType(nIe(eventArguments.getType()));
            arg.getComment().clear();
            arg.getComment().addAll(eventArguments.getDescriptionArray());
            eventArguments.opaqueModelObject = arg; // replace
            targ.add(arg);
        }
    }

    private void cloneLocalVariables(List<LocalVariable> targ, Vector<ViskitElement> local) {
        targ.clear();
        for (ViskitElement eventLocalVariables : local) {
            LocalVariable lvar = oFactory.createLocalVariable();
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
        if (!eventNameCheck()) {
            controller.messageUser(JOptionPane.INFORMATION_MESSAGE,
                    "Duplicate Event Name",
                    "Duplicate event name detected: " + node.getName() +
                    "\nUnique name will be substituted.");
            mangleNodeName(node);
            retcode = false;
        }
        Event jaxbEv = (Event) node.opaqueModelObject;

        jaxbEv.setName(node.getName());

        double x = node.getPosition().getX();
        double y = node.getPosition().getY();
        Coordinate coor = oFactory.createCoordinate();
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
        notifyChanged(new ModelEvent(node, ModelEvent.EVENTCHANGED, "Event changed"));
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

        Schedule sch = oFactory.createSchedule();

        se.opaqueModelObject = sch;
        Event targEv = (Event) target.opaqueModelObject;
        sch.setEvent(targEv);
        Event srcEv = (Event) src.opaqueModelObject;
        srcEv.getScheduleOrCancel().add(sch);

        // Put in dummy edge parameters to match the target arguments
        List<ViskitElement> args = target.getArguments();
        if (args.size() > 0) {
            List<ViskitElement> edgeParameters = new ArrayList<>(args.size());
            for (ViskitElement arg : args) {
                edgeParameters.add(new vEdgeParameter(""));
            }
            se.parameters = edgeParameters;
        }

        se.priority = "DEFAULT";  // set default

        this.edgeCache.put(sch, se);
        setDirty(true);

        this.notifyChanged(new ModelEvent(se, ModelEvent.EDGEADDED, "Edge added"));
    }

    @Override
    public void newCancelingEdge(EventNode src, EventNode target) {
        CancelingEdge ce = new CancelingEdge();
        ce.from = src;
        ce.to = target;
        src.getConnections().add(ce);
        target.getConnections().add(ce);

        Cancel can = oFactory.createCancel();

        ce.opaqueModelObject = can;
        Event targEv = (Event) target.opaqueModelObject;
        can.setEvent(targEv);
        Event srcEv = (Event) src.opaqueModelObject;
        srcEv.getScheduleOrCancel().add(can);

        // Put in dummy edge parameters to match the target arguments
        List<ViskitElement> args = target.getArguments();
        if (args.size() > 0) {
            List<ViskitElement> edgeParameters = new ArrayList<>(args.size());
            for (ViskitElement arg : args) {
                edgeParameters.add(new vEdgeParameter(""));
            }
            ce.parameters = edgeParameters;
        }

        this.edgeCache.put(can, ce);
        setDirty(true);

        this.notifyChanged(new ModelEvent(ce, ModelEvent.CANCELINGEDGEADDED, "Edge added"));
    }

    @Override
    public void deleteSchedulingEdge(Edge edge) {
        _commonEdgeDelete(edge);

        setDirty(true);
        this.notifyChanged(new ModelEvent(edge, ModelEvent.EDGEDELETED, "Edge deleted"));
    }

    private void _commonEdgeDelete(Edge edg) {
        Object jaxbEdge = edg.opaqueModelObject;

        List<Event> nodes = jaxbRoot.getEvent();
        for (Event ev : nodes) {
            List<Object> edges = ev.getScheduleOrCancel();
            edges.remove(jaxbEdge);
        }

        edgeCache.remove(edg);
    }

    @Override
    public void deleteCancelingEdge(Edge edge) {
        _commonEdgeDelete(edge);

        setDirty(true);
        this.notifyChanged(new ModelEvent(edge, ModelEvent.CANCELINGEDGEDELETED, "Canceling edge deleted"));
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

        // Bug 1373: This is where an edge parameter gets written out to XML
        for (ViskitElement edgeParameter : e.parameters) {
            EdgeParameter p = oFactory.createEdgeParameter();
            p.setValue(nIe(edgeParameter.getValue()));
            sch.getEdgeParameter().add(p);
        }

        setDirty(true);
        this.notifyChanged(new ModelEvent(e, ModelEvent.EDGECHANGED, "Edge changed"));
    }

    @Override
    public void changeCancelingEdge(Edge e) {
        Cancel can = (Cancel) e.opaqueModelObject;
        can.setCondition(e.conditional);
        can.setEvent(e.to.opaqueModelObject);
        can.getComment().clear();
        can.getComment().add(e.conditionalDescription);

        can.getEdgeParameter().clear();
        for (ViskitElement edgeParameter : e.parameters) {
            EdgeParameter p = oFactory.createEdgeParameter();
            p.setValue(nIe(edgeParameter.getValue()));
            can.getEdgeParameter().add(p);
        }

        setDirty(true);
        this.notifyChanged(new ModelEvent(e, ModelEvent.CANCELINGEDGECHANGED, "Canceling edge changed"));
    }

    /**
     * "nullIfEmpty" Return the passed string if non-zero length, else null
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
}
