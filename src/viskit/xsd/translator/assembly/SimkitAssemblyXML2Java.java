package viskit.xsd.translator.assembly;

import edu.nps.util.LogUtils;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.log4j.Logger;
import viskit.control.AssemblyControllerImpl;
import viskit.VGlobals;
import viskit.xsd.bindings.assembly.*;

/** A generator of source code from Assembly XML
 *
 * @author  Rick Goldberg
 * @since April 1, 2004, 10:09 AM
 * @version $Id$
 */
public class SimkitAssemblyXML2Java {

    SimkitAssembly root;
    InputStream fileInputStream;
    private String fileBaseName;
    File inputFile;
    JAXBContext jaxbCtx;
    static final boolean debug = false;
    static Logger log = LogUtils.getLogger(SimkitAssemblyXML2Java.class);

    /* convenience Strings for formatting */

    final private String sp  = " ";
    final private String sp4 = sp+sp+sp+sp;
    final private String sp8 = sp4+sp4;
    final private String sp12 = sp8+sp4;
    final private String sp16 = sp8+sp8;
    final private String ob  = "{";
    final private String cb  = "}";
    final private String sc  = ";";
    final private String cm  = ",";
    final private String lp  = "(";
    final private String rp  = ")";
    final private String eq  = "=";
    final private String pd  = ".";
    final private String qu  = "\"";
    final private String nw = "new";

    /** Default constructor that creates the JAXBContext */
    public SimkitAssemblyXML2Java() {
        try {
            this.jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
        } catch (JAXBException ex) {
            log.error(ex);
            error(ex.getMessage());
        }
    }

    public SimkitAssemblyXML2Java(SimkitAssembly root) {
        this();
        this.root = root;
    }

    /**
     * Creates a new instance of SimkitAssemblyXML2Java
     * when used from another class.  Instance this
     * with a String for the name of the xmlFile.
     * @param xmlFile the name of the Assembly XML file
     * @throws FileNotFoundException
     */
    public SimkitAssemblyXML2Java(String xmlFile) throws FileNotFoundException {
        this(new File(xmlFile));
    }

    /** Used by Viskit
     * @param f the Assembly File to process
     * @throws FileNotFoundException
     */
    public SimkitAssemblyXML2Java(File f) throws FileNotFoundException {
        this(new FileInputStream(f));
        this.fileBaseName = baseNameOf(f.getName());
        this.inputFile = f;
    }

    public SimkitAssemblyXML2Java(InputStream is) {
        this();
        fileInputStream = is;
    }

    public void unmarshal() {
        try {
            Unmarshaller u = jaxbCtx.createUnmarshaller();

            this.root = (SimkitAssembly) u.unmarshal(fileInputStream);

            // For debugging, make true
            if (debug) {
                marshalRoot();
            }
        } catch (JAXBException ex) {
            log.error(ex);
//            ex.printStackTrace();
        }
    }

    public String getFileBaseName() {
        return fileBaseName;
    }

    public void setFileBaseName(String fileBaseName) {
        this.fileBaseName = fileBaseName;
    }

    public javax.xml.bind.Element unmarshalAny(String bindings) {
        JAXBContext oldCtx = jaxbCtx;
        Unmarshaller u;
        try {
            jaxbCtx = JAXBContext.newInstance(bindings);
            u = jaxbCtx.createUnmarshaller();
            jaxbCtx = oldCtx;
            return (javax.xml.bind.Element) u.unmarshal(fileInputStream);
        } catch (JAXBException e) {
            jaxbCtx = oldCtx;
            return (javax.xml.bind.Element) null;
        }
    }

    public void marshalRoot() {
        try {
            Marshaller m = jaxbCtx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(this.root, System.out);
        } catch (JAXBException ex) {
            log.error(ex);
        }
    }

    public String marshalToString(Object jaxb) {
        Marshaller m;
        String s;
        if ( jaxb == null ) {
            return "<Empty/>";
        }
        if (jaxb instanceof Results) {
            s = "<Result/>";
        } else {
            s = "<Errors/>";
        }
        try {
            m = jaxbCtx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.setProperty(Marshaller.JAXB_FRAGMENT, true);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            m.marshal(jaxb,pw);
            s = sw.toString();
        } catch (JAXBException e) {
            log.error(e);
//            e.printStackTrace();
        }
        return s;
    }

    public String marshalFragmentToString(Object jaxb) {
        Marshaller m;
        String s;
        if ( jaxb == null ) {
            return "<Empty/>";
        }
        if (jaxb instanceof Results) {
            s = "<Result/>";
        } else {
            s = "<Errors/>";
        }
        try {
            m = jaxbCtx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.setProperty(Marshaller.JAXB_FRAGMENT, true);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            m.marshal(jaxb,pw);
            s = sw.toString();
        } catch (JAXBException e) {
            log.error(e);
//            e.printStackTrace();
        }
        return s;
    }

    public void marshal(File f) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(f);
            marshal(root, fos);
        } catch (FileNotFoundException e) {
            log.error(e);
//            e.printStackTrace();
        }
    }

    public void marshal(Object node, OutputStream o) {
        Marshaller m;
        try {
            m = jaxbCtx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(node,o);
        } catch (JAXBException e) {
            log.error(e);
//            e.printStackTrace();
        }
    }

    public SimkitAssembly getRoot() {
        return root;
    }

    public String translate() {

        StringBuilder source = new StringBuilder();
        StringWriter head = new StringWriter();
        StringWriter entities = new StringWriter();
        StringWriter listeners = new StringWriter();
        StringWriter output = new StringWriter();
        StringWriter tail = new StringWriter();
        StringWriter verbose = new StringWriter();

        buildHead(head);
        buildEntities(entities);
        buildListeners(listeners);
        buildOutput(output);
        buildVerbose(verbose);
        buildTail(tail);

        buildSource(source, head, entities, listeners, output, verbose, tail);

        return source.toString();
    }

    void buildHead(StringWriter head) {
        PrintWriter pw = new PrintWriter(head);
        String name = this.root.getName();
        String pkg  = this.root.getPackage();
        String extendz = this.root.getExtend();
        String implementz = this.root.getImplement();
        Schedule schedule;

        pw.println("package " + pkg + sc);
        pw.println();

        // Fully qualified names are used, no imports required
//        printImports(pw);
//        pw.println();

        if (extendz.equals("java.lang.Object")) {
            extendz = "";
        } else {
            extendz = "extends" + sp + extendz + sp;
        }
        if (implementz != null) {
            implementz = "implements" + sp + implementz + sp;
        } else {
            implementz = "";
        }

        pw.println("public class " + name + sp + extendz + implementz + ob);
        pw.println();
        pw.println(sp4 + "public" + sp + name + lp + rp + sp + ob);
        pw.println(sp8 + "super" + lp + rp + sc);
        if ( (schedule = this.root.getSchedule()) != null ) {

            pw.print(sp8 + "setStopTime");
            pw.println(lp + schedule.getStopTime() + rp + sc);

            pw.print(sp8 + "setVerbose");
            pw.println(lp + schedule.getVerbose() + rp + sc);

            pw.print(sp8 + "setNumberReplications");
            pw.println(lp + schedule.getNumberReplications() + rp + sc);

            pw.print(sp8 + "setSaveReplicationData");
            pw.println(lp + schedule.getSaveReplicationData() + rp + sc);

            pw.print(sp8 + "setPrintReplicationReports");
            pw.println(lp + schedule.getPrintReplicationReports() + rp + sc);

            pw.print(sp8 + "setPrintSummaryReport");
            pw.println(lp + schedule.getPrintSummaryReport() + rp + sc);
        }

        pw.println(sp4 + cb);
        pw.println();
    }

    /** Print out required imports to the Assembly
     * @param pw the PrintWriter to write out Java source
     */
    void printImports(PrintWriter pw) {
        SortedSet<String> list = Collections.synchronizedSortedSet(new TreeSet<String>());
        List<SimEntity> r = this.root.getSimEntity();

        for (SimEntity se : r) {
            traverseForImports(se, list);
        }

        List<PropertyChangeListener> lpcl = this.root.getPropertyChangeListener();

        for (PropertyChangeListener pcl : lpcl) {
            traverseForImports(pcl, list);
        }

        String[] excludes = {
            "byte", "byte[]", "char", "char[]",
            "int", "int[]", "float", "float[]", "double", "double[]",
            "long", "long[]", "boolean", "boolean[]"
        };

        List<String> exList = java.util.Arrays.asList(excludes);

        synchronized (list) {
            Iterator<String> listI = list.iterator();
            while (listI.hasNext()) {
                String clazz = listI.next();
                if (exList.contains(clazz)) {
                    listI.remove();
                    log.debug("Removed type \"" + clazz + "\" from the TreeSet");
                }
            }
        }

        for (String imports : list) {
            if (!imports.startsWith("java.lang")) {
                pw.println("import" + sp + imports + sc);
            }
        }
    }

    /** This method currently unused
     *
     * @param branch
     * @param tlist
     */
    void traverseForImports(Object branch, SortedSet<String> tlist) {
        if ( branch instanceof SimEntity ) {
            String t = stripBrackets(((SimEntity) branch).getType());
            if (!tlist.contains(t)) {
                tlist.add(t);
            }

            List<Object> p = ((SimEntity) branch).getParameters();
            for (Object o : p) {
                traverseForImports(o, tlist);
            }
        } else if ( branch instanceof FactoryParameter ) {
            FactoryParameter fp = (FactoryParameter) branch;
            String t = stripBrackets(fp.getType());
            if (!tlist.contains(t)) {
                tlist.add(t);
            }

            List<Object> p = fp.getParameters();
            for (Object o : p) {
                traverseForImports(o, tlist);
            }
        } else if ( branch instanceof MultiParameter ) {
            MultiParameter mp = (MultiParameter)branch;
            String t = stripBrackets(mp.getType());
            if (!tlist.contains(t)) {
                tlist.add(t);
            }

            List<Object> p = mp.getParameters();
            for (Object o : p) {
                traverseForImports(o, tlist);
            }
        } else if ( branch instanceof TerminalParameter ) {
            TerminalParameter tp = (TerminalParameter)branch;
            String t = stripBrackets(tp.getType());
            if (!tlist.contains(t)) {
                tlist.add(t);
            }
        } else if ( branch instanceof PropertyChangeListener ) {
            if ( !tlist.contains("java.beans.PropertyChangeListener") ) {
                tlist.add("java.beans.PropertyChangeListener");
            }
            PropertyChangeListener pcl = (PropertyChangeListener) branch;
            String t = stripBrackets(pcl.getType());
            if (!tlist.contains(t)) {
                tlist.add(t);
            }

            List<Object> p = pcl.getParameters();
            for (Object o : p) {
                traverseForImports(o, tlist);
            }
        }
    }

    /** This method currently unused
     * @param type
     * @return
     */
    String stripBrackets(String type) {
        int brindex = type.indexOf('[');
        return (brindex > 0) ? new String(type.substring(0, brindex)): type;
    }

    void buildEntities(StringWriter entities) {
        PrintWriter pw = new PrintWriter(entities);

        pw.println(sp4 + "@Override");
        pw.println(sp4 + "protected void createSimEntities" + lp + rp + sp + ob);

        for (SimEntity se : this.root.getSimEntity()) {
            List<Object> pl = se.getParameters();

            pw.println();
            pw.println(sp8 + "addSimEntity" + lp + sp + qu + se.getName() + qu + cm);
            pw.print(sp12 + nw + sp + se.getType() + lp);

            if (pl.size() > 0) {
                pw.println();
                for (Object o : pl) {
                    doParameter(pl, o, sp16, pw);
                }
                pw.println(sp12 + rp);
            } else {
                pw.println(rp);
            }

            pw.println(sp8 + rp + sc);
        }

        if ( this.root.getSimEventListenerConnection().size() > 0 ) {
            pw.println();
        }

        for (SimEventListenerConnection sect : this.root.getSimEventListenerConnection()) {
            pw.print(sp8 + "addSimEventListenerConnection" + lp + qu + sect.getListener() + qu);
            pw.println(cm + qu + sect.getSource() + qu + rp + sc);
        }

        if ( this.root.getAdapter().size() > 0 ) {
            pw.println();
        }

        for (Adapter a : this.root.getAdapter()) {
            pw.print(sp8 + "addAdapter" + lp + qu + a.getName() + qu + cm );
            pw.print(sp + qu + a.getEventHeard() + qu + cm );
            pw.print(sp + qu + a.getEventSent() + qu + cm );
            pw.print(sp + qu + a.getFrom() + qu + cm );
            pw.println(sp + qu + a.getTo() + qu + rp + sc );
        }

        pw.println();
        pw.println(sp8 + "super" + pd + "createSimEntities"+ lp + rp + sc);

        pw.println(sp4 + cb);
        pw.println();
    }

     /** Build up a parameter up to but not including a trailing comma.
      * _callers_ should check the size of the list to determine if a
      * comma is needed. This may include a closing paren or brace
      * and any nesting. Note a doParameter may also be a caller
      * of a doParameter, so the comma placement is tricky.
      * @param plist
      * @param param
      * @param indent
      * @param pw
      */
    void doParameter(List<Object> plist, Object param, String indent, PrintWriter pw) {

        if ( param instanceof MultiParameter ) {
            doMultiParameter((MultiParameter) param, indent, pw);
        } else if ( param instanceof FactoryParameter ) {
            doFactoryParameter((FactoryParameter) param, indent, pw);
        } else {
            doTerminalParameter((TerminalParameter) param, indent, pw);
        }

        maybeComma(plist, param, pw);
    }

    // with newer getSimEntityByName() always returns SimEntity, however
    // parameter may actually call for a subclass.
    String castIfSimEntity(String type) {
        String sret = "";
        try {
            if ((Class.forName("simkit.SimEntityBase", true, VGlobals.instance().getWorkClassLoader())).isAssignableFrom(Class.forName(type, true, VGlobals.instance().getWorkClassLoader()))
                    ||
                    (Class.forName("simkit.SimEntity", true, VGlobals.instance().getWorkClassLoader())).isAssignableFrom(Class.forName(type))) {
                sret = lp + type + rp;
            }
        } catch (ClassNotFoundException cnfe) {
            // Do nothing
        }
        return sret;
    }

    void doFactoryParameter(FactoryParameter fact, String indent, PrintWriter pw) {
        String factory = fact.getFactory();
        String method = fact.getMethod();
        List<Object> facts = fact.getParameters();
        pw.println(indent + sp4 + castIfSimEntity(fact.getType()) + factory + pd + method + lp);
        for (Object o : facts) {
            doParameter(facts, o, indent + sp8, pw);
        }
        pw.print(indent + sp4 + rp);
    }

    void doTerminalParameter(TerminalParameter term, String indent, PrintWriter pw) {

        String type = term.getType();
        String value = term.getValue();
        if ( term.getLinkRef() != null ) {
            value=((TerminalParameter) (term.getLinkRef())).getValue();
        }
        if ( isPrimitive(type) ) {
            pw.print(indent + sp4 + value);
        } else if ( isString(type) ) {
            pw.print(indent + sp4 + qu + value + qu);
        } else { // some Expression
            pw.print(indent + castIfSimEntity(type) + value);
        }
    }

    void doSimpleStringParameter(TerminalParameter term, PrintWriter pw) {

        String type = term.getType();
        String value = term.getValue();

        if ( isString(type) ) {
            pw.print(qu + value + qu);
        } else {
            error("Should only have a single String parameter for this PropertyChangeListener");
        }
    }

    boolean isPrimitive(String type) {
        return type.equals("boolean") |
               type.equals("char") |
               type.equals("double") |
               type.equals("float") |
               type.equals("int") |
               type.equals("long") |
               type.equals("short");
    }

    boolean isString(String type) {
        return type.equals("String") | type.equals("java.lang.String");
    }

    boolean isArray(String type) {
        return type.endsWith("]");
    }

    void doMultiParameter(MultiParameter p, String indent, PrintWriter pw) {

        List<Object> params = p.getParameters();
        String ptype = p.getType();

        if ( isArray(ptype) ) {
            pw.println(indent + sp4 + nw + sp + ptype + ob);
            for (Object o : params) {
                doParameter(params, o, indent + sp4, pw);
            }
            pw.print(indent + sp4 + cb);
        } else { // some multi param object

            // Reduce redundant casting
            pw.println(indent + sp4 + /*castIfSimEntity(ptype) +*/ nw + sp + ptype + lp);
            for (Object o : params) {
                doParameter(params, o, indent + sp4, pw);
            }
            pw.print(indent + sp4 + rp);
        }
    }

    void maybeComma(List<Object> params, Object param, PrintWriter pw) {
        if ( params.size() > 1 && params.indexOf(param) < params.size() - 1 ) {
            pw.println(cm);
        } else {
            pw.println();
        }
    }

    void buildListeners(StringWriter listeners) {

        PrintWriter pw = new PrintWriter(listeners);
        Map<String, PropertyChangeListener> replicationStats = new LinkedHashMap<>();
        Map<String, PropertyChangeListener> designPointStats = new LinkedHashMap<>();
        Map<String, PropertyChangeListener> propertyChangeListeners = new LinkedHashMap<>();
        Map<String, List<PropertyChangeListenerConnection>> propertyChangeListenerConnections =
                new LinkedHashMap<>();

        for (PropertyChangeListener pcl : this.root.getPropertyChangeListener()) {
            String pclMode = pcl.getMode();

            if (null != pclMode ) // For backwards compatibility
            switch (pclMode) {
                case "replicationStat":
                    replicationStats.put(pcl.getName(), pcl);
                    break;
                case "replicationStats":
                    replicationStats.put(pcl.getName(), pcl);
                    break;
                case "designPointStat":
                    designPointStats.put(pcl.getName(), pcl);
                    break;
                case "designPointStats":
                    designPointStats.put(pcl.getName(), pcl);
                    break;
                default:
                    propertyChangeListeners.put(pcl.getName(), pcl);
                    break;
            }
        }

        for (PropertyChangeListenerConnection pclc : this.root.getPropertyChangeListenerConnection()) {
            String name = pclc.getListener();
            List<PropertyChangeListenerConnection> connections = propertyChangeListenerConnections.get(name);
            if (connections == null) {
                connections = new ArrayList<>();
                propertyChangeListenerConnections.put(name, connections);
            }
            connections.add( pclc);
        }

        pw.println(sp4 + "@Override");
        pw.println(sp4 + "public void createPropertyChangeListeners" + lp + rp + sp + ob);

        for (PropertyChangeListener pcl : propertyChangeListeners.values()) {
                              List<Object> pl = pcl.getParameters();
                  pw.println(sp8 + "addPropertyChangeListener" + lp + qu + pcl.getName() + qu + cm);
                  pw.print(sp12 + nw + sp + pcl.getType() + lp);

                  if (pl.size() > 0) {
                      pw.println();
                      for (Object o : pl) {
                          doParameter(pl, o, sp16, pw);
                      }
                      pw.println(sp12 + rp);
                  } else {
                      pw.println(rp);
                  }
                  pw.println(sp8 + rp + sc);
        }

        for (String propChangeListener : propertyChangeListenerConnections.keySet()) {
            for (PropertyChangeListenerConnection pclc : propertyChangeListenerConnections.get(propChangeListener)) {
                pw.print(sp8 + "addPropertyChangeListenerConnection" + lp + qu + propChangeListener + qu + cm + qu + pclc.getProperty() + qu + cm);
                pw.println(qu + pclc.getSource() + qu + rp + sc);
                pw.println();
            }
        }
        pw.println(sp8 + "super" + pd + "createPropertyChangeListeners" + lp + rp + sc);
        pw.println(sp4 + cb);
        pw.println();

        pw.println(sp4 + "@Override");
        pw.println(sp4 + "public void createReplicationStats" + lp + rp + sp + ob);

        String[] pcls = replicationStats.keySet().toArray(new String[0]);
        for (String propChangeListener : pcls) {
            PropertyChangeListener pcl = replicationStats.get(propChangeListener);
            List<Object> pl = pcl.getParameters();
            pw.println(sp8 + "addReplicationStats" + lp + qu + propChangeListener + qu + cm);
            pw.print(sp12 + nw + sp + pcl.getType() + lp);

            if (pl.size() > 0) {
                pw.println();
                for (Object o : pl) {
                    doParameter(pl, o, sp16, pw);
                }
                pw.println(sp12 + rp);
            } else {
                pw.println(rp);
            }

            pw.println(sp8 + rp + sc);
            pw.println();
            List<PropertyChangeListenerConnection> myConnections =
                    propertyChangeListenerConnections.get(propChangeListener);
            if (myConnections != null) {
                for (PropertyChangeListenerConnection pclc : myConnections) {
                    pw.print(sp8 + "addReplicationStatsListenerConnection" + lp + qu + propChangeListener + qu + cm + qu + pclc.getProperty() + qu + cm);
                    pw.println(qu + pclc.getSource() + qu + rp + sc);
                    pw.println();
                }
            }
        }
        pw.println(sp8 + "super" + pd + "createReplicationStats" + lp + rp + sc);
        pw.println(sp4 + cb);
        pw.println();

        pw.println(sp4 + "@Override");
        pw.println(sp4 + "public void createDesignPointStats" + lp + rp + sp + ob);

        pcls = designPointStats.keySet().toArray(new String[0]);

        for (String propChangeListener : pcls) {
            PropertyChangeListener pcl = designPointStats.get(propChangeListener);
            List<Object> pl = pcl.getParameters();
            pw.println(sp8 + "addDesignPointStats" + lp + qu + propChangeListener + qu + cm);
            pw.print(sp12 + nw + sp + pcl.getType() + lp);

            if (pl.size() > 0) {
                pw.println();
                for (Object o : pl) {
                    doParameter(pl, o, sp16, pw);
                }
                pw.println(sp12 + rp);
            } else {
                pw.println(rp);
            }
         }

        pw.println(sp8 + "super" + pd + "createDesignPointStats" + lp + rp + sc);

        pw.println(sp4 + cb);
        pw.println();
    }

    String nameAsm() {
        String asm = this.root.getName().substring(0,1);
        asm = asm.toLowerCase();
        asm += this.root.getName().substring(1,this.root.getName().length());
        return asm;
    }

    void buildVerbose(StringWriter out)
    {
        PrintWriter pw = new PrintWriter(out);
        List<Verbose> vbose = this.root.getVerbose();
        if(!vbose.isEmpty()) {
            //todo build code
            pw.println(sp4 + "// marker for verbose output");
            pw.println();
        }
    }

    void buildOutput(StringWriter out) {
        PrintWriter pw = new PrintWriter(out);

        // override the printInfo method to dump detailed output from the nodes which are marked, if any
        List<Output> outputs = getRoot().getOutput();
        if(!outputs.isEmpty()) {
            pw.println(sp4 + "@Override");
            pw.println(sp4 + "public void printInfo() {");
            pw.println(sp8 + "System.out.println" + lp + qu + "Entity Details" + qu + rp + sc);
            pw.println(sp8 + "System.out.println" + lp + qu + "--------------" + qu + rp + sc);
            dumpEntities(outputs,pw);
            pw.println(sp8 + "System.out.println" + lp + qu + "--------------" + qu + rp + sc);
            pw.println(sp4 + cb);
            pw.println();
            pw.println();
        }
    }

    private void dumpEntities(List<Output> lis, PrintWriter pw) {
        List<SimEntity> simEntities = getRoot().getSimEntity();
        List<PropertyChangeListener> pcls = getRoot().getPropertyChangeListener();
        for (Output output : lis) {
            Object elem = output.getEntity();
            String name = "<FIX: Output not of SimEntity or PropertyChangeListener>";

            for (SimEntity se : simEntities) {
                if (se.getName().equals(elem.toString())) {
                    name = se.getName();
                    break;
                }
            }

            for (PropertyChangeListener pcl : pcls) {
                if (pcl.getName().equals(elem.toString())) {
                    name = pcl.getName();
                    break;
                }
            }

            if (!name.contains("<FIX:")) {
                pw.println(sp8 + "System.out.println" + lp + "getSimEntityByName" + lp + qu + name + qu + rp + rp + sc);
            }
        }
    }

    void buildTail(StringWriter t) {

        PrintWriter pw = new PrintWriter(t);
        String nAsm = nameAsm();

        // The main method doesn't need to dump the outputs, since their done at object init time now
        pw.println(sp4 + "public static void main(String[] args) {");
        pw.print(sp8 + this.root.getName() + sp + nameAsm() + sp);
        pw.println(eq + sp + nw + sp + this.root.getName() + lp + rp + sc);

        pw.println(sp8 + nw + sp + "Thread" + lp + nAsm + rp + pd + "start" + lp + rp + sc);

        pw.println();
        pw.println(sp4 + cb);
        pw.println(cb);
    }

    void buildSource(StringBuilder source, StringWriter head, StringWriter entities,
            StringWriter listeners, StringWriter output, StringWriter verbose, StringWriter tail ) {

        source.append(head.getBuffer()).  append(entities.getBuffer()).append(listeners.getBuffer());
        source.append(output.getBuffer()).append(verbose.getBuffer()). append(tail.getBuffer());
    }

    private String baseNameOf(String s) {
        return s.substring(0, s.indexOf(pd));
    }

    void runIt() {
        try {
            File f = new File(pd);
            ClassLoader cl = new URLClassLoader(new URL[] {f.toURI().toURL()});
            Class<?> assembly = cl.loadClass(this.root.getPackage() + pd + getFileBaseName());
            Object params[] = { new String[]{} };
            Class<?> classParams[] = { params[0].getClass() };
            Method mainMethod = assembly.getDeclaredMethod("main", classParams);
            mainMethod.invoke(null, params);
        } catch (MalformedURLException | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            error(e.toString());
        }
    }

    /** Report and exit the JVM
     *
     * @param desc a description of the encountered error
     */
    private void error(String desc) {
        log.error(desc);
        System.exit(1);
    }

    /**
     * @param arg the command line arguments
     * arg[0] - -f | --file | -p | --port
     * arg[1] - filename | port
     * arg[2] - -p | --port | -f | --file
     * arg[3] - port | filename
     */
    public static void main(String[] arg) {
        int port = -1;
        String fileName = null;
        SimkitAssemblyXML2Java sax2j = null;
        List<String> args = java.util.Arrays.asList(arg);
        ListIterator<String> lit = args.listIterator();

        for (String a : args) {
            //FIXME Remove port stuff
            switch (a) {
                case "-p":
                case "--port":
                    // Dummy forward looking next()
                    lit.next();
                    if (lit.hasNext()) {
                        a = lit.next();
                        port = Integer.parseInt(a);
                    } else {
                        usage();
                    }
                    break;
                case "-f":
                case "--file":
                    // Dummy forward looking next()
                    lit.next();
                    if (lit.hasNext()) {
                        fileName = lit.next();
                        fileName = fileName.replaceAll("\\\\", "/");
                    } else {
                        usage();
                    }
                    break;
            }
        }

        log.info("Assembly file is: " + fileName);
        log.info("Generating Java Source...");

        if (port == 0) {
            if (fileName == null) {
                usage();
            } else {
                try {
                    sax2j = new SimkitAssemblyXML2Java(fileName); // regular style
                } catch (FileNotFoundException ex) {
                    log.error(ex);
                }
                if (sax2j != null) {
                    sax2j.unmarshal();
                }
            }
        } else {
            if (fileName == null) {
                sax2j = new SimkitAssemblyXML2Java();
            } else {
                InputStream is = null;
                try {
                    is = new FileInputStream(fileName);
                } catch (FileNotFoundException fnfe) {
                    log.error(fnfe);
                }
                sax2j = new SimkitAssemblyXML2Java(is);
            }
        }

        if (sax2j != null) {
            File baseName = new File(sax2j.baseNameOf(fileName));
            sax2j.setFileBaseName(baseName.getName());
            sax2j.unmarshal();
            String dotJava = sax2j.translate();
            log.info("Done.");

            // also write out the .java to a file and compile it
            // to a .class
            log.info("Generating Java Bytecode...");
            if (AssemblyControllerImpl.compileJavaClassFromString(dotJava) != null) {
                log.info("Done.");
            }
        }
    }

    static void usage() {
        System.err.println("Check args, you need at least a port or a file in grid mode");
        System.err.println("usage: Assembly [-p port | --port port | -f file | --file file]");
        System.exit(1);
    }
}
