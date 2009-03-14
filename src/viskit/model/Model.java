package viskit.model;

import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.swing.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import edu.nps.util.FileIO;
import edu.nps.util.TempFileManager;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import viskit.ModelEvent;
import viskit.VGlobals;
import viskit.ViskitController;
import viskit.mvc.mvcAbstractModel;
import viskit.util.XMLValidationTool;
import viskit.xsd.bindings.eventgraph.*;
import viskit.xsd.bindings.eventgraph.Event;

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
public class Model extends mvcAbstractModel implements ViskitModel {

    static Logger log = Logger.getLogger(Model.class);
    JAXBContext jc;
    ObjectFactory oFactory;
    SimEntity jaxbRoot;
    File currentFile;
    HashMap<Event, EventNode> evNodeCache = new HashMap<Event, EventNode>();
    HashMap<Object, Object> edgeCache = new HashMap<Object, Object>();
    Vector<ViskitElement> stateVariables = new Vector<ViskitElement>();
    Vector<ViskitElement> simParameters = new Vector<ViskitElement>();
    private String schemaLoc = XMLValidationTool.EVENT_GRAPH_SCHEMA;    
    private String privateIdxVarPrefix = "_idxvar_";
    private String privateLocVarPrefix = "locvar_";
    private String stateVarPrefix = "state_";
    private GraphMetaData metaData;
    private ViskitController controller;
    private boolean modelDirty = false;
    private boolean numericPriority;
    
    public Model(ViskitController controller) {
        this.controller = controller;        
        metaData = new GraphMetaData(this);
    }

    public void init() {
        try {
            jc = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
            oFactory = new ObjectFactory();
            jaxbRoot = oFactory.createSimEntity(); // to start with empty graph
        } catch (JAXBException e) {
            JOptionPane.showMessageDialog(null, "Exception on JAXBContext instantiation" +
                    "\n" + e.getMessage(),
                    "XML Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Boolean to signify whether the model has been changed since last disk save.
     *
     * @return true means changes have been made and it needs to be flushed.
     */
    public boolean isDirty() {
        return modelDirty;
    }    

    /**
     * This is to allow the controller to stick in a Run event, but treat the graph as fresh.
     * @param dirt
     */
    public void setDirty(boolean dirt) {
        modelDirty = dirt;
    }

    public GraphMetaData getMetaData() {
        return metaData;
    }

    public void changeMetaData(GraphMetaData gmd) {
        metaData = gmd;
        setDirty(true);
    }

    /**
     * Replace current model with one contained in the passed file.
     *
     * @param f the EventGraph file to check open
     * @return true for good open, else false
     */
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
                mymetaData.projectName =
                        mymetaData.projectName +
                        VGlobals.instance().getCurrentViskitProject().getProjectRoot().getName();
                mymetaData.author = jaxbRoot.getAuthor();
                mymetaData.version = jaxbRoot.getVersion();
                mymetaData.name = jaxbRoot.getName();
                mymetaData.packageName = jaxbRoot.getPackage();
                mymetaData.extendsPackageName = jaxbRoot.getExtend();
                mymetaData.implementsPackageName = jaxbRoot.getImplement();
                List<String> lis = jaxbRoot.getComment();
                StringBuffer sb = new StringBuffer("");
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
                    JOptionPane.showMessageDialog(null, "Use the assembly editor to" +
                            "\n" + "work with this file.",
                            "Wrong File Format", JOptionPane.ERROR_MESSAGE);
                } catch (JAXBException e) {
                    JOptionPane.showMessageDialog(null, "Exception on JAXB unmarshalling" +
                            "\n" + f.getName() +
                            "\n" + e.getMessage() +
                            "\nin Model.newModel(File)",
                            "XML I/O Error", JOptionPane.ERROR_MESSAGE);
                }
                return false;    // from either error case
            }
        }
        currentFile = f;
        setDirty(false);
        return true;
    }

    public void saveModel(File f) {
        if (f == null) {
            f = currentFile;
        }

        // Do the marshalling into a temporary file, so as to avoid possible deletion of existing
        // file on a marshal error.

        File tmpF = null;
        try {
            tmpF = TempFileManager.createTempFile("tmpEGmarshal", ".xml");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Exception creating temporary file, Model.saveModel():" +
                    "\n" + e.getMessage(),
                    "I/O Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            FileWriter fw = new FileWriter(tmpF);
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            m.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, schemaLoc);

            String nm = f.getName();
            int dot = -1;
            if ((dot = nm.indexOf('.')) != -1) {
                nm = nm.substring(0, dot);
            }

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
            fw.close();

            // OK, made it through the marshal, overwrite the "real" file
            FileIO.copyFile(tmpF, f, true);

            setDirty(false);
            currentFile = f;
        } catch (JAXBException e) {
            JOptionPane.showMessageDialog(null, "Exception on JAXB marshalling" +
                    "\n" + f.getName() +
                    "\n" + e.getMessage(),
                    "XML I/O Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Exception on writing " + f.getName() +
                    "\n" + ex.getMessage(),
                    "File I/O Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** @return a File object representing the last one passed to the two methods above */
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

        if (!eventNameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
                    "XML file contains duplicate event name: " + en.getName() +
                    "\nUnique name substituted.");
            mangleNodeName(en);
        }
        notifyChanged(new ModelEvent(en, ModelEvent.EVENTADDED, "Event added"));

        return en;
    }

    private String concatStrings(List<String> lis) {
        StringBuffer sb = new StringBuffer();
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
        int nxt = mangleRandom.nextInt(0x10000); // 4 hex digits
        StringBuffer sb = new StringBuffer(name);
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
        HashSet<String> hs = new HashSet<String>(10);
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
        HashSet<String> hs = new HashSet<String>(10);
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
            node.setPosition(new Point(Integer.parseInt(coor.getX()),
                    Integer.parseInt(coor.getY())));
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

            ArrayList<String> com = new ArrayList<String>();
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
            if (sv.getType().indexOf('[') != -1) {
                String idx = st.getIndex();
                est.setIndexingExpression(idx);
            }

            est.setOperation(st.getOperation() != null);
            if (est.isOperation()) {
                est.setOperationOrAssignment(st.getOperation().getMethod());
            } else {
                est.setOperationOrAssignment(st.getAssignment().getValue());
            }

            ArrayList<String> cmt = new ArrayList<String>();
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
        String s = null;
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
            StringBuffer sb = new StringBuffer();
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
        CancellingEdge ce = new CancellingEdge();
        ce.opaqueModelObject = ed;
        ce.conditional = ed.getCondition();

        List<String> cmt = ed.getComment();
        if (!cmt.isEmpty()) {
            StringBuffer sb = new StringBuffer();
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

        notifyChanged(new ModelEvent(ce, ModelEvent.CANCELLINGEDGEADDED, "Cancelling edge added"));
    }

    private ArrayList<ViskitElement> buildEdgeParmsFromJaxb(List<EdgeParameter> lis) {
        ArrayList<ViskitElement> alis = new ArrayList<ViskitElement>(3);
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
                controller.messageUser(JOptionPane.ERROR_MESSAGE, "XML file contains duplicate state variable name." +
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
                controller.messageUser(JOptionPane.ERROR_MESSAGE, "XML file contains duplicate parameter name." +
                        "\nUnique name substituted.");
                mangleParamName(vp);
            }
            notifyChanged(new ModelEvent(vp, ModelEvent.SIMPARAMETERADDED, "New sim parameter"));
        }
    }

    public Vector<ViskitElement> getAllNodes() {
        return new Vector<ViskitElement>(evNodeCache.values());
    }
    
    // TODO: Known unchecked cast to ViskitElement
    @SuppressWarnings("unchecked")
    public Vector<ViskitElement> getStateVariables() {
        return (Vector<ViskitElement>) stateVariables.clone();
    }

    // TODO: Known unchecked cast to ViskitElement
    @SuppressWarnings("unchecked")
    public Vector<ViskitElement> getSimParameters() {
        return (Vector<ViskitElement>) simParameters.clone();
    }

    // parameter mods
    // --------------
    public void newSimParameter(String nm, String typ, String xinitVal, String comment) {
        setDirty(true);

        vParameter vp = new vParameter(nm, typ, comment);
        simParameters.add(vp);

        if (!stateVarParamNameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
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

    public void deleteSimParameter(vParameter vp) {
        // remove jaxb variable
        jaxbRoot.getParameter().remove(vp.opaqueModelObject);
        setDirty(true);
        simParameters.remove(vp);
        notifyChanged(new ModelEvent(vp, ModelEvent.SIMPARAMETERDELETED, "vParameter deleted"));
    }

    public void changeCodeBlock(String s) {
        jaxbRoot.setCode(s);
        setDirty(true);
    }

    public boolean changeSimParameter(vParameter vp) {
        boolean retcode = true;
        if (!stateVarParamNameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE, "Duplicate parameter name detected: " + vp.getName() +
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
    public void newStateVariable(String name, String type, String xinitVal, String comment) {
        setDirty(true);

        // get the new one here and show it around
        vStateVariable vsv = new vStateVariable(name, type, comment);
        stateVariables.add(vsv);
        if (!stateVarParamNameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE,
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

    public void deleteStateVariable(vStateVariable vsv) {
        // remove jaxb variable
        jaxbRoot.getStateVariable().remove(vsv.opaqueModelObject);
        stateVariables.remove(vsv);
        setDirty(true);
        notifyChanged(new ModelEvent(vsv, ModelEvent.STATEVARIABLEDELETED, "State variable deleted"));
    }

    public boolean changeStateVariable(vStateVariable vsv) {
        boolean retcode = true;
        if (!stateVarParamNameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE, "Duplicate state variable name detected: " + vsv.getName() +
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
    /**
     * Add a new event to the graph with the given label, at the given point
     * @param nodeName the name of the Event Node
     * @param p the (x, y) position of the Event Node
     */
    public void newEvent(String nodeName, Point p) {
        EventNode node = new EventNode(nodeName);
        if (p == null) {
            node.setPosition(new Point(100, 100));
        } else {
            p.x = ((p.x + 5) / 10) * 10;    //round
            p.y = ((p.y + 5) / 10) * 10;
            node.setPosition(p);
        }
        Event jaxbEv = oFactory.createEvent();

        evNodeCache.put(jaxbEv, node);   // key = ev

        if (!eventNameCheck()) {
            mangleNodeName(node);
        }

        jaxbEv.setName(nIe(nodeName));
        node.opaqueModelObject = jaxbEv;
        jaxbRoot.getEvent().add(jaxbEv);

        setDirty(true);
        notifyChanged(new ModelEvent(node, ModelEvent.EVENTADDED, "Event added"));
    }

    /**
     * Delete the referenced event, also deleting attached edges.
     *
     * @param node
     */
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

    public String generateLocalVariableName() {
        String nm = null;
        do {
            nm = privateLocVarPrefix + locVarNameSequence++;
        } while (!isUniqueLVorIdxVname(nm));
        return nm;

    }

    public void resetLVNameGenerator() {
        locVarNameSequence = 0;
    }
    private int idxVarNameSequence = 0;

    public String generateIndexVariableName() {
        String nm = null;
        do {
            nm = privateIdxVarPrefix + idxVarNameSequence++;
        } while (!isUniqueLVorIdxVname(nm));
        return nm;
    }

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
            
            if (sv.getType() != null && sv.getType().indexOf('[') != -1) {
                               
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

    public boolean changeEvent(EventNode node) {
        boolean retcode = true;
        if (!eventNameCheck()) {
            controller.messageUser(JOptionPane.ERROR_MESSAGE, "Duplicate event name detected: " + node.getName() +
                    "\nUnique name substituted.");
            mangleNodeName(node);
            retcode = false;
        }

        Event jaxbEv = (Event) node.opaqueModelObject;

        jaxbEv.setName(node.getName());

        Coordinate coor = oFactory.createCoordinate();

        // rudimentary snap to grid - this works on saved file only, not the live position in the node.  reload to enjoy.
        int GridScale = 10;
        int x = ((node.getPosition().x + GridScale / 2) / GridScale) * GridScale;
        int y = ((node.getPosition().y + GridScale / 2) / GridScale) * GridScale;
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
    
    public void newEdge(EventNode src, EventNode target) {
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
            ArrayList<ViskitElement> edgeParameters = new ArrayList<ViskitElement>(args.size());
            for (int i = 0; i < args.size(); i++) {
                edgeParameters.add(new vEdgeParameter(""));
            }
            se.parameters = edgeParameters;
        }

        se.priority = "DEFAULT";  // set default

        this.edgeCache.put(sch, se);
        setDirty(true);

        this.notifyChanged(new ModelEvent(se, ModelEvent.EDGEADDED, "Edge added"));
    }

    public void newCancelEdge(EventNode src, EventNode target) {
        CancellingEdge ce = new CancellingEdge();
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
            ArrayList<ViskitElement> edgeParameters = new ArrayList<ViskitElement>(args.size());
            for (int i = 0; i < args.size(); i++) {
                edgeParameters.add(new vEdgeParameter(""));
            }
            ce.parameters = edgeParameters;
        }

        this.edgeCache.put(can, ce);
        setDirty(true);

        this.notifyChanged(new ModelEvent(ce, ModelEvent.CANCELLINGEDGEADDED, "Edge added"));
    }

    public void deleteEdge(SchedulingEdge edge) {
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

    public void deleteCancelEdge(CancellingEdge edge) {
        _commonEdgeDelete(edge);

        setDirty(true);
        this.notifyChanged(new ModelEvent(edge, ModelEvent.CANCELLINGEDGEDELETED, "Cancelling edge deleted"));
    }

    public void changeEdge(SchedulingEdge e) {
        Schedule sch = (Schedule) e.opaqueModelObject;
        sch.setCondition(e.conditional);
        sch.getComment().clear();
        sch.getComment().add(e.conditionalDescription);
        sch.setDelay("" + e.delay);

        sch.setEvent((Event) e.to.opaqueModelObject);
        sch.setPriority(e.priority);
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

    public void changeCancelEdge(CancellingEdge e) {
        Cancel can = (Cancel) e.opaqueModelObject;
        can.setCondition(e.conditional);
        can.setEvent((Event) e.to.opaqueModelObject);
        can.getComment().clear();
        can.getComment().add(e.conditionalDescription);

        can.getEdgeParameter().clear();
        for (ViskitElement edgeParameter : e.parameters) {
            EdgeParameter p = oFactory.createEdgeParameter();
            p.setValue(nIe(edgeParameter.getValue()));
            can.getEdgeParameter().add(p);
        }

        setDirty(true);
        this.notifyChanged(new ModelEvent(e, ModelEvent.CANCELLINGEDGECHANGED, "Cancelling edge changed"));
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
