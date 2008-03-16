package viskit.xsd.assembly;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Collections;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import viskit.xsd.bindings.assembly.Adapter;
import viskit.xsd.bindings.assembly.FactoryParameter;
import viskit.xsd.bindings.assembly.MultiParameter;
import viskit.xsd.bindings.assembly.Output;
import viskit.xsd.bindings.assembly.PropertyChangeListener;
import viskit.xsd.bindings.assembly.PropertyChangeListenerConnection;
import viskit.xsd.bindings.assembly.Results;
import viskit.xsd.bindings.assembly.Schedule;
import viskit.xsd.bindings.assembly.SimEntity;
import viskit.xsd.bindings.assembly.SimEventListenerConnection;
import viskit.xsd.bindings.assembly.SimkitAssembly;
import viskit.xsd.bindings.assembly.TerminalParameter;
import static edu.nps.util.GenericConversion.toArray;

/**
 * @author  Rick Goldberg
 * @since April 1, 2004, 10:09 AM
 * @version $Id: SimkitAssemblyXML2Java.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class SimkitAssemblyXML2Java {
    
    static Logger log = Logger.getLogger(SimkitAssemblyXML2Java.class);

    SimkitAssembly root;
    InputStream fileInputStream;
    private String fileBaseName;
    File inputFile;
    JAXBContext jaxbCtx;
    static final boolean debug = false;
 
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
    
    public SimkitAssemblyXML2Java() {
        try {
            this.jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly", this.getClass().getClassLoader());
        } catch (Exception e) {
            try {
                this.jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly", this.getClass().getClassLoader());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public SimkitAssemblyXML2Java(SimkitAssembly root) {
        this();
        this.root = root;
    }
    
    /**
     * Creates a new instance of SimkitAssemblyXML2Java
     * when used from another class, instance this
     * with a String for the name of the xmlFile.
     * @param xmlFile 
     */
    public SimkitAssemblyXML2Java(String xmlFile) {
        try {
            this.fileBaseName = baseNameOf(xmlFile);
            this.jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly", this.getClass().getClassLoader()); // exp classLoader bit
            this.fileInputStream = new FileInputStream(xmlFile);
        } catch (FileNotFoundException ex) {
            log.error(ex);
        } catch (JAXBException ex) {
            log.error(ex);
        }
    }

    /** Used by Viskit
     * @param f
     * @throws java.lang.Exception 
     */
    public SimkitAssemblyXML2Java(File f) throws Exception {
        this.fileBaseName = baseNameOf(f.getName());
        this.inputFile = f;
        this.jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
        this.fileInputStream = new FileInputStream(f);
    }
    
    public SimkitAssemblyXML2Java(InputStream is) {
        try {
            this.jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
            fileInputStream = is;
        } catch (JAXBException ex) {
            log.error(ex);
        }
    }
    
    public void unmarshal() {
        try {
            Unmarshaller u = jaxbCtx.createUnmarshaller();
            this.root = (SimkitAssembly) u.unmarshal(fileInputStream);
            if (debug) {
                marshal();
            }
        } catch (JAXBException ex) {
            log.error(ex);
            ex.printStackTrace();
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
        } catch (Exception e) {
            jaxbCtx = oldCtx;
            return (javax.xml.bind.Element) null;
        }
        
    }
    
    public void marshal() {
        try {
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly", this.getClass().getClassLoader());
            Marshaller m = jaxbCtx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
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
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly",jaxb.getClass().getClassLoader());
            m = jaxbCtx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            m.setProperty(Marshaller.JAXB_FRAGMENT,new Boolean(true));
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            m.marshal(jaxb,pw);
            s = sw.toString();
        } catch (Exception e) { e.printStackTrace(); }
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
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly",jaxb.getClass().getClassLoader());
            m = jaxbCtx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            m.setProperty(Marshaller.JAXB_FRAGMENT,new Boolean(true));
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            m.marshal(jaxb,pw);
            s = sw.toString();
        } catch (Exception e) { e.printStackTrace(); }
        return s;
    }    
    
    public void marshal(File f) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(f);
            marshal((javax.xml.bind.Element) root, (OutputStream)fos);
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    public void marshal(javax.xml.bind.Element node, java.io.OutputStream o) {
        Marshaller m;
        try {
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly",node.getClass().getClassLoader());
            m = jaxbCtx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            m.marshal(node,o);
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    public SimkitAssembly getRoot() {
        return root;
    }
    
    public String translate() {
        
        StringBuffer source = new StringBuffer();
        StringWriter head = new StringWriter();
        StringWriter entities = new StringWriter();
        StringWriter listeners = new StringWriter();
        StringWriter output = new StringWriter();
        StringWriter tail = new StringWriter();
        
        buildHead(head);
        buildEntities(entities);
        buildListeners(listeners);
        buildOutput(output);
        buildTail(tail);
        
        buildSource(source, head, entities, listeners, output, tail);
        
        return source.toString();
    }
    
    void buildHead(StringWriter head) {
        PrintWriter pw = new PrintWriter(head);
        String name = this.root.getName();
        String pkg  = this.root.getPackage();
        String extend = this.root.getExtend();
        Schedule schedule;
        
        pw.println("package " + pkg + sc);
        pw.println();
        
        // Printing imports is not necessary with assemblies
//        printImports(pw);        
//        pw.println();
        
        if ( extend.equals("java.lang.Object") ) {
            extend = "";
        } else {
            extend = "extends" + sp + extend + sp;
        }
        pw.println("public class " + name + sp + extend + ob);
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
    
    // This method currently unused
    void printImports(PrintWriter pw) {
        TreeSet<String> list = new TreeSet<String>();
        List<SimEntity> r = this.root.getSimEntity();
        
        for (SimEntity se : r) {
            traverseForImports(se, list);    
        }
        
        List<PropertyChangeListener> lpcl = this.root.getPropertyChangeListener();
        
        for (PropertyChangeListener pcl : lpcl) {
            traverseForImports(pcl, list);    
        }
        
        String[] excludes = { 
                "byte","byte[]","char","char[]",
                "int","int[]","float","float[]","double","double[]",
                "long","long[]","boolean","boolean[]"
        };
        
        List<String> exList = java.util.Arrays.asList(excludes);
        for (String clazz : list) {
            if ( exList.contains(clazz) ) {
                synchronized(list) {
                    list.remove(clazz);
                }
            }
        }
        for (String imports : list) {
            if (!imports.startsWith("java.lang")) {
                pw.println("import" + sp + imports + sc);
            }
        }
    }
    
    // This method currently unused
    void traverseForImports(Object branch, TreeSet<String> tlist) {
        SortedSet<String> list = Collections.synchronizedSortedSet(tlist);
        if ( branch instanceof SimEntity ) {
            String t = stripBrackets(((SimEntity) branch).getType());
            if ( !list.contains(t) ) {
                synchronized(list) {
                    list.add(t);
                }
            }
            List<Object> p = ((SimEntity) branch).getParameters();
            for (Object o : p) {
                traverseForImports(o, tlist);
            }
        } else if ( branch instanceof FactoryParameter ) {
            FactoryParameter fp = (FactoryParameter) branch;
            String t = stripBrackets(fp.getType());
            if ( !list.contains(t) ) {
                synchronized(list) {
                    list.add(t);
                }
            }
            List<Object> p = fp.getParameters();
            for (Object o : p) {
                traverseForImports(o, tlist);
            }
        } else if ( branch instanceof MultiParameter ) {
            MultiParameter mp = (MultiParameter)branch;
            String t = stripBrackets(mp.getType());
            if ( !list.contains(t) ) {
                synchronized(list) {
                    list.add(t);
                }
            }
            
            List<Object> p = mp.getParameters();            
            for (Object o : p) {
                traverseForImports(o, tlist);
            }
            
        } else if ( branch instanceof TerminalParameter ) {
            TerminalParameter tp = (TerminalParameter)branch;
            String t = stripBrackets(tp.getType());
            if ( !list.contains(t) ){
                synchronized(list) {
                    list.add(t);
                }
            }
        } else if ( branch instanceof PropertyChangeListener ) {
            if ( !list.contains("java.beans.PropertyChangeListener") ) {
                synchronized(list) {
                    list.add("java.beans.PropertyChangeListener");
                }
            }
            PropertyChangeListener pcl = (PropertyChangeListener) branch;
            String t = stripBrackets(pcl.getType());
            if ( !list.contains(t) ) {
                synchronized(list) {
                    list.add(t);
                }
            }
            List<Object> p = pcl.getParameters();
            for (Object o : p) {
                traverseForImports(o, tlist);
            }
        }
    }
    
    // This method currently unused
    String stripBrackets(String type) {
        int brindex = type.indexOf('[');
        if (brindex > 0) {
            return new String(type.substring(0, brindex));
        } else {
            return type;
        }
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
            pw.print(sp8 + "addSimEventListenerConnection" + lp + qu + ((SimEntity) (sect.getListener())).getName() + qu);
            pw.println(cm + qu + ((SimEntity) (sect.getSource())).getName() + qu + rp + sc);
        }
        
        if ( this.root.getAdapter().size() > 0 ) {
            pw.println();
        }
        
        for (Adapter a : this.root.getAdapter()) {
            pw.print(sp8 + "addAdapter" + lp + qu + a.getName() + qu + cm );
            pw.print(sp + qu + a.getEventHeard() + qu + cm );
            pw.print(sp + qu + a.getEventSent() + qu + cm );
            pw.print(sp + qu + ((SimEntity) a.getFrom()).getName() + qu + cm );
            pw.println(sp + qu + ((SimEntity) a.getTo()).getName() + qu + rp + sc );
        } 
        
        pw.println();
        pw.println(sp8 + "super" + pd + "createSimEntities"+ lp + rp + sc);
        
        pw.println(sp4 + cb);
        pw.println();        
    }
    
     /** Build up a parameter up to but not including a trailing comma.
      * _callers_ should check the size of the list to determine if a
      * comma is needed. This may include a closing paren or brace
      * and any nesting. Note a a doParameter may also be a caller
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
            if ((Class.forName("simkit.SimEntityBase")).isAssignableFrom(Class.forName(type))
                    ||
                    (Class.forName("simkit.SimEntity")).isAssignableFrom(Class.forName(type))) {
                sret = new String(lp + type + rp);
            }
        } catch (ClassNotFoundException cnfe) {
            ; //
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
        if (type.equals("boolean") |
            type.equals("char") |
            type.equals("double") |
            type.equals("float") |
            type.equals("int") |
            type.equals("long") |
            type.equals("short")) {
            return true;
        } else {
            return false;
        }
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
            pw.println(indent + sp4 + castIfSimEntity(ptype) + nw + sp + ptype + lp);
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
        LinkedHashMap<String, PropertyChangeListener> replicationStats = new LinkedHashMap<String, PropertyChangeListener>();
        LinkedHashMap<String, PropertyChangeListener> designPointStats = new LinkedHashMap<String, PropertyChangeListener>();
        LinkedHashMap<String, PropertyChangeListener> propertyChangeListeners = new LinkedHashMap<String, PropertyChangeListener>();
        LinkedHashMap<String, PropertyChangeListenerConnection> propertyChangeListenerConnections = new LinkedHashMap<String, PropertyChangeListenerConnection>();
        
        for (PropertyChangeListener pcl : this.root.getPropertyChangeListener()) {
            String pclMode = pcl.getMode();
            
            if ( "replicationStats".equals(pclMode) ) {
                replicationStats.put(pcl.getName(), pcl);
            } else if ( "designPointStats".equals(pclMode) ) {
                designPointStats.put(pcl.getName(), pcl);
            } else {
                propertyChangeListeners.put(pcl.getName(), pcl);
            }
        }
                
        for (PropertyChangeListenerConnection pclc : this.root.getPropertyChangeListenerConnection()) {
            propertyChangeListenerConnections.put(((PropertyChangeListener) pclc.getListener()).getName(),pclc);
        }
        
        pw.println(sp4 + "@Override");
        pw.println(sp4 + "public void createPropertyChangeListeners" + lp + rp + sp + ob);
        
        String[] pcls = toArray(propertyChangeListeners.keySet(), new String[0]);
        for (String propChangeListener : pcls) {
            PropertyChangeListener pcl = propertyChangeListeners.get(propChangeListener);
            List<Object> pl = pcl.getParameters();
            PropertyChangeListenerConnection pclc = propertyChangeListenerConnections.get(propChangeListener);
            pw.println(sp8 + "addPropertyChangeListener" + lp + qu + propChangeListener + qu + cm);
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
            pw.print(sp8 + "addPropertyChangeListenerConnection" + lp + qu + propChangeListener + qu + cm + qu + pclc.getProperty() + qu + cm);
            pw.println(qu + ((SimEntity) pclc.getSource()).getName() + qu + rp + sc);
            pw.println();
        }
        pw.println(sp8 + "super" + pd + "createPropertyChangeListeners" + lp + rp + sc);
        pw.println(sp4 + cb);
        pw.println();
        
        pw.println(sp4 + "@Override");
        pw.println(sp4 + "public void createReplicationStats" + lp + rp + sp + ob);
        
        pcls = replicationStats.keySet().toArray(new String[0]);
        for (String propChangeListener : pcls) {
            PropertyChangeListener pcl = replicationStats.get(propChangeListener);
            List<Object> pl = pcl.getParameters();
            PropertyChangeListenerConnection pclc = propertyChangeListenerConnections.get(propChangeListener);
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
            pw.print(sp8 + "addReplicationStatsListenerConnection" + lp + qu + propChangeListener + qu + cm + qu + pclc.getProperty() + qu + cm);
            pw.println(qu + ((SimEntity)pclc.getSource()).getName() + qu + rp + sc);
            pw.println();
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
            PropertyChangeListenerConnection pclc = propertyChangeListenerConnections.get(propChangeListener);
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
            
            pw.println(sp8 + rp + sc);
            pw.println();
            pw.print(sp8 + "addDesignPointStatsListenerConnection" + lp + qu + propChangeListener + qu + cm + qu + pclc.getProperty() + qu + cm);
            pw.println(qu + ((SimEntity)pclc.getSource()).getName() + qu + rp + sc);
            pw.println();
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
    
    void buildOutput(StringWriter out) {
        PrintWriter pw = new PrintWriter(out);
        
        pw.println(sp4 + "public static void main(String[] args) {");
        pw.print(sp8 + this.root.getName() + sp + nameAsm() + sp);
        pw.println(eq + sp + nw + sp + this.root.getName() + lp + rp + sc);
        
        List<Output> outputs = this.root.getOutput();
        for (Output output : outputs) {
            Object elem = output.getEntity();
            String name = "<FIX: Output not of SimEntity or PropertyChangeListener>";
            
            if ( elem instanceof SimEntity ) {
                name = ((SimEntity)elem).getName();
            } else if ( elem instanceof PropertyChangeListener ) {
                name = ((PropertyChangeListener)elem).getName();
            }
            pw.println(sp8 + "System.out.println" + lp + nameAsm() + pd + "getSimEntityByName" + lp + qu + name + qu + rp + rp + sc);
        }
    }
    
    void buildTail(StringWriter t) {
        
        PrintWriter pw = new PrintWriter(t);
        String nAsm = nameAsm();
        
        pw.println(sp8 + nw + sp + "Thread" + lp + nAsm + rp + pd + "start" + lp + rp + sc);
        
        pw.println();
        pw.println(sp4 + cb);
        pw.println(cb);
    }
    
    void buildSource(StringBuffer source, StringWriter head, StringWriter entities,
            StringWriter listeners, StringWriter output, StringWriter tail ) {
        
        source.append(head.getBuffer()).append(entities.getBuffer()).append(listeners.getBuffer());
        source.append(output.getBuffer()).append(tail.getBuffer());
    }
    
    public void writeOut(String data, java.io.PrintStream out) {
        out.println(data);
    }
    
    private String baseNameOf(String s) {
        return s.substring(0, s.indexOf(pd));
    }
    
    boolean compileCode(String fileName) {
        String fName = this.root.getName();
        if (!fName.equals(fileName)) {
            log.info("Using " + fName);
            fileName = fName + ".java";
        }
        String path = this.root.getPackage();
        File fDest;
        try {
            File f = new File(pd + File.separator + path);
            f.mkdirs();
            fDest = new File(path + File.separator + fileName);
            f = new File(fileName);
            f.renameTo(fDest);
        } catch (Exception e) { e.printStackTrace(); }
        return (com.sun.tools.javac.Main.compile(
                new String[] {
                "-Xlint:unchecked", 
                "-Xlint:deprecation", 
                "-verbose", 
                "-sourcepath", 
                path, 
                "-d", 
                pd, 
                path + File.separator + fileName}) == 0);
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
        } catch (Exception e) { error(e.toString()); }
    }
    
    void error(String desc) {
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println("error :");
        pw.println(desc);
        
        System.err.println(sw.toString());
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
        ListIterator lit = args.listIterator();
        
        for (String a : args) {
            //FIXME Remove port stuff
            if (a.equals("-p") || a.equals("--port")) {
                
                // Dummy forward looking next()
                lit.next();
                if (lit.hasNext()) {
                    a = (String) lit.next();
                    port = Integer.parseInt(a);
                } else {
                    usage();
                }
            } else if (a.equals("-f") || a.equals("--file")) {
                
                // Dummy forward looking next()
                lit.next();
                if (lit.hasNext()) {
                    fileName = (String) lit.next();                    
                    fileName = fileName.replaceAll("\\\\", "/");
                } else {
                    usage();
                }
            }
        }
        
        log.info("XML file is: " + fileName);
        log.info("Generating Java Source...");
        
        if (port == 0) {
            if ( fileName == null ) {
                usage();
            } else {
                sax2j = new SimkitAssemblyXML2Java(fileName); // regular style
                sax2j.unmarshal();               
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
                File baseName = new File(sax2j.baseNameOf(fileName));
                log.info("baseName: " + baseName.getAbsolutePath());
                sax2j.setFileBaseName(baseName.getName());
                sax2j.unmarshal();
            }
        }
        
        String dotJava = sax2j.translate();
        log.info("Done.");

        // also write out the .java to a file and compile it
        // to a .class
        log.info("Generating Java Bytecode...");
        try {
            String path = sax2j.getRoot().getPackage() + "/" + sax2j.getFileBaseName() + ".java";
            File f = new File(path);
            f.getParentFile().mkdir();
            FileOutputStream fout = new FileOutputStream(f);
            PrintStream ps = new PrintStream(fout, true);
            sax2j.writeOut(dotJava, ps);
            if (!sax2j.compileCode(f.getAbsolutePath())) {
                sax2j.error("Compile error " + f);
            } else {
                log.info("Done.");
            }
        } catch (IOException ioe) {
            log.error(ioe);
        }
    }
    
    static void usage() {
        System.out.println("Check args, you need at least a port or a file in grid mode");
        System.out.println("usage: Assembly [-p port | --port port | -f file | --file file]");
        System.exit(1);
    }    
}
