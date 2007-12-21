/*
 * SimkitAssemblyXML2Java.java
 *
 * Created on April 1, 2004, 10:09 AM
 *
 */

package viskit.xsd.assembly;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Collections;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.LinkedHashMap;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;

import org.apache.xmlrpc.*;
import org.apache.xmlrpc.secure.*;
import static edu.nps.util.GenericConversion.toArray;
import viskit.xsd.bindings.assembly.*;

/**
 * @author  Rick Goldberg
 * @version $Id: SimkitAssemblyXML2Java.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class SimkitAssemblyXML2Java {

    SimkitAssembly root;
    InputStream fileInputStream;
    String fileBaseName;
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
            this.jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly",this.getClass().getClassLoader());
        } catch (Exception e) {
            try {
                this.jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly",this.getClass().getClassLoader());
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
     *
     */
    
    public SimkitAssemblyXML2Java(String xmlFile) {
        this.fileBaseName = baseNameOf(xmlFile);
        try {
            this.jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly",this.getClass().getClassLoader()); // exp classLoader bit
            this.fileInputStream = Class.forName("viskit.xsd.assembly.SimkitAssemblyXML2Java").getClassLoader().getResourceAsStream(xmlFile);
        } catch ( Exception e ) {
            e.printStackTrace();
        }   
    }

    /** Used by Viskit */
    public SimkitAssemblyXML2Java(File f) throws Exception {
        this.fileBaseName = baseNameOf(f.getName());
        this.inputFile = f;
        this.jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
        this.fileInputStream = new FileInputStream(f);
    }
    
    public SimkitAssemblyXML2Java(InputStream is) throws Exception {
        try {
            this.jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
        } catch (Exception e) {
            this.jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly",this.getClass().getClassLoader());
        }
        this.fileInputStream = is;
    }
    
    public void unmarshal() {
        Unmarshaller u;
        try {
            u = jaxbCtx.createUnmarshaller();
            this.root = (SimkitAssembly) u.unmarshal(fileInputStream);
            this.fileBaseName = root.getName();
        } catch (Exception e) { e.printStackTrace(); }
        
        if ( debug ) {
            marshal();
        }
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
        Marshaller m;
        try {
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly",this.getClass().getClassLoader());
            m = jaxbCtx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                    new Boolean(true));
            m.marshal(this.root,System.out);
            
        } catch (Exception e) { e.printStackTrace(); }
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
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                    new Boolean(true));
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
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                    new Boolean(true));
            m.setProperty(Marshaller.JAXB_FRAGMENT,new Boolean(true));
            //m.setProperty(Marshaller.JAXB_ENCODING,)
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
            marshal((javax.xml.bind.Element)root, (OutputStream)fos);
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
        StringWriter tail = new StringWriter();
        StringWriter entities = new StringWriter();
        StringWriter listeners = new StringWriter();
        StringWriter connectors = new StringWriter();
        StringWriter output = new StringWriter();
        
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
        
        printImports(pw);
        
        pw.println();
        if ( extend.equals("java.lang.Object") ) {
            extend = "";
        } else {
            extend = "extends" + sp + extend + sp;
        }
        pw.println("public class " + name + sp + extend + ob);
        pw.println();
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
    
    void printImports(PrintWriter pw) {
        TreeSet<String> list = new TreeSet<String>();
        List r = this.root.getSimEntity();
        ListIterator ri = r.listIterator();
        
        while ( ri.hasNext() ) {
            traverseForImports(ri.next(), list);    
        }
        
        r = this.root.getPropertyChangeListener();
        ri = r.listIterator();    
        
        while ( ri.hasNext() ) {
            traverseForImports(ri.next(), list);    
        }
        
        String[] excludes = { 
                "byte","byte[]","char","char[]",
                "int","int[]","float","float[]","double","double[]",
                "long","long[]","boolean","boolean[]"
        };
        
        List exList = java.util.Arrays.asList(excludes);
        Iterator it = list.iterator();
        
        while(it.hasNext()) {
            String cls = (String) it.next();
            if ( exList.contains(cls) ) {
                it.remove();
            }
        }
        
        it = list.iterator();

        while (it.hasNext()) {
            String imp = (String) it.next();
            if (!imp.startsWith("java.lang")) {
                pw.println("import" + sp + imp + sc);
            }
        }
    }
    
    String stripBrackets(String type) {
        int brindex = type.indexOf('[');
        if (brindex > 0)
            return new String(type.substring(0, brindex));
        else 
            return type;
    }
    
    void traverseForImports(Object branch, TreeSet<String> tlist) {
        SortedSet<String> list = Collections.synchronizedSortedSet(tlist);
        if ( branch instanceof SimEntity ) {
            String t = stripBrackets(((SimEntity) branch).getType());
            if ( !list.contains(t) ) {
                synchronized(list) {
                    list.add(t);
                }
            }
            List p = ((SimEntity)branch).getParameters();
            ListIterator pi = p.listIterator();
            while ( pi.hasNext() ) {
                traverseForImports(pi.next(), tlist);
            }
        } else if ( branch instanceof FactoryParameter ) {
            FactoryParameter fp = (FactoryParameter)branch;
            String t = stripBrackets(fp.getType());
            if ( !list.contains(t) ) {
                synchronized(list) {
                    list.add(t);
                }
            }
            List p = fp.getParameters();
            ListIterator pi = p.listIterator();
            while ( pi.hasNext() ) {
                traverseForImports(pi.next(), tlist);
            }
        } else if ( branch instanceof MultiParameter ) {
            MultiParameter mp = (MultiParameter)branch;
            String t = stripBrackets(mp.getType());
            if ( !list.contains(t) ) {
                synchronized(list) {
                    list.add(t);
                }
            }
            List p = mp.getParameters();
            ListIterator pi = p.listIterator();
            
            while ( pi.hasNext() ) {
                traverseForImports(pi.next(), tlist);
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
            PropertyChangeListener pcl = (PropertyChangeListener)branch;
            String t = stripBrackets(pcl.getType());
            if ( !list.contains(t) ) {
                synchronized(list) {
                    list.add(t);
                }
            }
            List p = pcl.getParameters();
            ListIterator pi = p.listIterator();
            while ( pi.hasNext() ) {
                traverseForImports(pi.next(), tlist);
            }
        }
    }
    
    void buildEntities(StringWriter entities) {
        PrintWriter pw = new PrintWriter(entities);
        ListIterator li = this.root.getSimEntity().listIterator();
        
        pw.println(sp4+"protected void createSimEntities"+ lp + rp + sp + ob);
        
        while ( li.hasNext() ) {
            
            SimEntity se = (SimEntity) li.next();
            List pl = se.getParameters();
            ListIterator pli = pl.listIterator();
            
            pw.println();
            pw.println(sp8 + "addSimEntity" + lp + sp + qu + se.getName() + qu + cm);
            pw.print(sp12 + nw + sp + se.getType() + lp);
            
            if ( pli.hasNext() ) {
                pw.println();
                while ( pli.hasNext() ) {
                    doParameter(pl, pli.next(), sp16, pw);
                }
                pw.println(sp12 + rp);
            } else pw.println(rp);
            
            pw.println(sp8 + rp + sc);
        }
        
        li = this.root.getSimEventListenerConnection().listIterator();
        
        if ( this.root.getSimEventListenerConnection().size() > 0 )
            pw.println();
        
        while(li.hasNext()) {
            SimEventListenerConnection sect = (SimEventListenerConnection)li.next();
            pw.print(sp8 + "addSimEventListenerConnection" + lp + qu + ((SimEntity)(sect.getListener())).getName() + qu);
            pw.println(cm + qu + ((SimEntity)(sect.getSource())).getName() + qu + rp + sc);
        }
        
        li = this.root.getAdapter().listIterator();
        
        if ( this.root.getAdapter().size() > 0 ) 
            pw.println();
        
        while ( li.hasNext() ) {
            Adapter a = (Adapter) li.next();
            pw.print(sp8 + "addAdapter" + lp + qu + a.getName() + qu + cm );
            pw.print(sp + qu + a.getEventHeard() + qu + cm );
            pw.print(sp + qu + a.getEventSent() + qu + cm );
            pw.print(sp + qu + ((SimEntity)a.getFrom()).getName() + qu + cm );
            pw.println(sp + qu + ((SimEntity)a.getTo()).getName() + qu + rp + sc );
        } 
        
        pw.println();
        pw.println(sp8 + "super" + pd + "createSimEntities"+ lp + rp + sc);
        
        pw.println(sp4 + cb);
        pw.println();
        
    }
    
     /* Build up a parameter up to but not including a trailing comma.
      * _callers_ should check the size of the list to determine if a
      * comma is needed. This may include a closing paren or brace
      * and any nesting. Note a a doParameter may also be a caller
      * of a doParameter, so the comma placement is tricky.
      */
    
    void doParameter(List plist, Object param, String indent, PrintWriter pw) {
        
        if ( param instanceof MultiParameter ) {
            doMultiParameter((MultiParameter)param, indent, pw);
        } else if ( param instanceof FactoryParameter ) {
            doFactoryParameter((FactoryParameter)param, indent, pw);
        } else {
            doTerminalParameter((TerminalParameter)param, indent, pw);
        }
        
        maybeComma(plist, param, pw);
    }
    
    // with newer getSimEntityByName() always returns SimEntity, however
    // parameter may actually call for a subclass.
    String castIfSimEntity(String type) {
        String sret = "";
        try {
            if (
                    (Class.forName("simkit.SimEntityBase")).isAssignableFrom(Class.forName(type))
                    ||
                    (Class.forName("simkit.SimEntity")).isAssignableFrom(Class.forName(type))
                    ) {
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
        List facts = fact.getParameters();
        ListIterator facti = facts.listIterator();
        pw.println(indent + sp4 + castIfSimEntity(fact.getType()) + factory + pd + method + lp);
        while ( facti.hasNext() ) {
            doParameter(facts, facti.next(), indent + sp8, pw);
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
        if (
                type.equals("boolean") |
                type.equals("char") |
                type.equals("double") |
                type.equals("float") |
                type.equals("int") |
                type.equals("long") |
                type.equals("short")
                ) return true;
        else return false;
    }
    
    boolean isString(String type) {
        if (
                type.equals("String") |
                type.equals("java.lang.String")
                ) return true;
        else return false;
    }
    
    boolean isArray(String type) {
        if (
                type.endsWith("]")
                ) return true;
        else return false;
    }
    
    void doMultiParameter(MultiParameter p, String indent, PrintWriter pw) {
        
        List params = p.getParameters();
        ListIterator paramsi = params.listIterator();
        String ptype = p.getType();
        
        if ( isArray(ptype) ) {
            pw.println(indent + sp4 + nw + sp + ptype + ob);
            while ( paramsi.hasNext() ) {
                doParameter(params, paramsi.next(), indent + sp4, pw);
            }
            pw.print(indent + sp4 + cb);
        } else { // some multi param object
            pw.println(indent + sp4 + castIfSimEntity(ptype) + nw + sp + ptype + lp);
            while ( paramsi.hasNext() ) {
                doParameter(params, paramsi.next(), indent + sp4, pw);
            }
            pw.print(indent + sp4 + rp);
        }
        
    }
    
    void maybeComma(List params, Object param, PrintWriter pw) {
        if ( params.size() > 1 && params.indexOf(param) < params.size() - 1 ) {
            pw.println(cm);
        } else pw.println();
    }
    
    void buildListeners(StringWriter listeners) {
        
        PrintWriter pw = new PrintWriter(listeners);
        LinkedHashMap<String, PropertyChangeListener> replicationStats = new LinkedHashMap<String, PropertyChangeListener>();
        LinkedHashMap<String, PropertyChangeListener> designPointStats = new LinkedHashMap<String, PropertyChangeListener>();
        LinkedHashMap<String, PropertyChangeListener> propertyChangeListeners = new LinkedHashMap<String, PropertyChangeListener>();
        LinkedHashMap<String, PropertyChangeListenerConnection> propertyChangeListenerConnections = new LinkedHashMap<String, PropertyChangeListenerConnection>();
        ListIterator li = this.root.getPropertyChangeListener().listIterator();
        
        while ( li.hasNext() ) {
            PropertyChangeListener pcl = (PropertyChangeListener) li.next();
            String pclMode = pcl.getMode();
            
            if ( "replicationStats".equals(pclMode) ) {
                replicationStats.put(pcl.getName(), pcl);
            } else if ( "designPointStats".equals(pclMode) ) {
                designPointStats.put(pcl.getName(), pcl);
            } else {
                propertyChangeListeners.put(pcl.getName(), pcl);
            }
        }
        
        li = this.root.getPropertyChangeListenerConnection().listIterator();
        
        while ( li.hasNext() ) {
            PropertyChangeListenerConnection pclc = (PropertyChangeListenerConnection) li.next();
            propertyChangeListenerConnections.put(((PropertyChangeListener)pclc.getListener()).getName(),pclc);
        }
        
        pw.println(sp4 + "public void createPropertyChangeListeners" + lp + rp + sp + ob);
        
        String[] pcls = toArray(propertyChangeListeners.keySet(), new String[0]);
        for ( int i = 0; i < pcls.length; i++ ) {
            PropertyChangeListener pcl = (PropertyChangeListener) propertyChangeListeners.get(pcls[i]);
            List pl = pcl.getParameters();
            ListIterator pli = pl.listIterator();
            PropertyChangeListenerConnection pclc =
                    (PropertyChangeListenerConnection) propertyChangeListenerConnections.get(pcls[i]);
            pw.println(sp8 + "addPropertyChangeListener" + lp + qu + pcls[i] + qu + cm);
            pw.print(sp12 + nw + sp + pcl.getType() + lp);
            
            if ( pli.hasNext() ) {
                pw.println();
                while ( pli.hasNext() ) {
                    doParameter(pl, pli.next(), sp16, pw);
                }
                pw.println(sp12 + rp);
            } else pw.println(rp);
            pw.println(sp8 + rp + sc);
            pw.print(sp8 + "addPropertyChangeListenerConnection" + lp + qu + pcls[i] + qu + cm + qu + pclc.getProperty() + qu + cm);
            pw.println(qu + ((SimEntity)pclc.getSource()).getName() + qu + rp + sc);
            pw.println();
        }
        pw.println(sp8 + "super" + pd + "createPropertyChangeListeners" + lp + rp + sc);
        pw.println(sp4 + cb);
        pw.println();
        
        
        pw.println(sp4 + "public void createReplicationStats" + lp + rp + sp + ob);
        
        pcls = (String[]) replicationStats.keySet().toArray(new String[0]);
        for ( int i = 0; i < pcls.length; i++ ) {
            PropertyChangeListener pcl = (PropertyChangeListener) replicationStats.get(pcls[i]);
            List pl = pcl.getParameters();
            ListIterator pli = pl.listIterator();
            PropertyChangeListenerConnection pclc =
                    (PropertyChangeListenerConnection) propertyChangeListenerConnections.get(pcls[i]);
            pw.println(sp8 + "addReplicationStats" + lp + qu + pcls[i] + qu + cm);
            pw.print(sp12 + nw + sp + pcl.getType() + lp);
            
            if ( pli.hasNext() ) {
                pw.println();
                while ( pli.hasNext() ) {
                    doParameter(pl, pli.next(), sp16, pw);
                }
                pw.println(sp12 + rp);
            } else pw.println(rp);
            
            pw.println(sp8 + rp + sc);
            pw.println();
            pw.print(sp8 + "addReplicationStatsListenerConnection" + lp + qu + pcls[i] + qu + cm + qu + pclc.getProperty() + qu + cm);
            pw.println(qu + ((SimEntity)pclc.getSource()).getName() + qu + rp + sc);
            pw.println();
        }
        pw.println(sp8 + "super" + pd + "createReplicationStats" + lp + rp + sc);
        pw.println(sp4 + cb);
        pw.println();
        
        pw.println(sp4 + "public void createDesignPointStats" + lp + rp + sp + ob);
        
        pcls = (String[]) designPointStats.keySet().toArray(new String[0]);
        
        for ( int i = 0; i < pcls.length; i++ ) {
            PropertyChangeListener pcl = (PropertyChangeListener) designPointStats.get(pcls[i]);
            List pl = pcl.getParameters();
            ListIterator pli = pl.listIterator();
            PropertyChangeListenerConnection pclc =
                    (PropertyChangeListenerConnection) propertyChangeListenerConnections.get(pcls[i]);
            pw.println(sp8 + "addDesignPointStats" + lp + qu + pcls[i] + qu + cm);
            pw.print(sp12 + nw + sp + pcl.getType() + lp);
            
            if ( pli.hasNext() ) {
                pw.println();
                while ( pli.hasNext() ) {
                    doParameter(pl, pli.next(), sp16, pw);
                }
                pw.println(sp12 + rp);
            } else pw.println(rp);
            
            pw.println(sp8 + rp + sc);
            pw.println();
            pw.print(sp8 + "addDesignPointStatsListenerConnection" + lp + qu + pcls[i] + qu + cm + qu + pclc.getProperty() + qu + cm);
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
        
        ListIterator outputs = this.root.getOutput().listIterator();
        while ( outputs.hasNext() ) {
            Object elem = ((Output)outputs.next()).getEntity();
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
    
    private String baseNameOf( String s ) {
        return s.substring(0,s.indexOf(pd));
    }
    
    boolean compileCode(String fileName) {
        String path = this.root.getPackage();
        File fDest;
        char[] pchars;
        int j;
        pchars = path.toCharArray();
        for (j = 0; j<pchars.length; j++) {
            if ( pchars[j] == '.' ) pchars[j] = File.separatorChar;
        }
        path = new String(pchars);
        try {
            File f = new File(pd + File.separator + path);
            f.mkdirs();
            fDest = new File(path + File.separator + fileName);
            f = new File(fileName);
            f.renameTo(fDest);
        } catch (Exception e) { e.printStackTrace(); }
        return (
                com.sun.tools.javac.Main.compile(
                new String[] {"-verbose","-sourcepath",path,"-d",pd,path+File.separator+fileName}
        ) == 0
                );
    }
    
    void runIt() {
        try {
            File f = new File(pd);
            ClassLoader cl = new URLClassLoader(new URL[] {f.toURI().toURL()});
            Class<?> assembly = cl.loadClass(this.root.getPackage()+pd+fileBaseName);
            Object params[] = { new String[]{} };
            Class classParams[] = { params[0].getClass() };
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
     * args[0] - -f | --file | -p | --port
     * args[1] - filename | port
     * args[2] - -p | --port | -f | --file
     * args[3] - port | filename
     */
    
    public static void main(String[] arg) {
        int port = 0;
        String fileName = null;
        SimkitAssemblyXML2Java sax2j = null;
        List args = java.util.Arrays.asList(arg);
        Iterator it = args.iterator();
        
        while (it.hasNext()) {
            String a = (String)(it.next());
            //FIXME Remove port stuff
            if (a.equals("-p") || a.equals("--port")) {
                if ( it.hasNext() ) {
                    a = (String)(it.next());
                    port = Integer.parseInt(a);
                } else {
                    usage();
                }
            } else if (a.equals("-f") || a.equals("--file")) {
                if ( it.hasNext() ) {
                    fileName = (String)(it.next());
                } else {
                    usage();
                }
            }
        }
        
        if ( port == 0 ) {
            if ( fileName == null ) {
                usage();
            } else {
                sax2j = new SimkitAssemblyXML2Java(fileName); // regular style
                sax2j.unmarshal();
               
            }
            
        } else {
            if ( fileName == null ) {
                sax2j = new SimkitAssemblyXML2Java(); 
            } else {
                sax2j = new SimkitAssemblyXML2Java(fileName); 

            }
        }
        
    }
    
    static void usage() {
        System.out.println("Check args, you need at least a port or a file in grid mode");
        System.out.println("usage: Assembly [-p port | --port port | -f file | --file file]");
        System.exit(1);
    }   
}
