package viskit.xsd.translator.assembly;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.control.AssemblyControllerImpl;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.xsd.bindings.assembly.*;
import viskit.xsd.translator.eventgraph.SimkitXML2Java;

/** A generator of source code from Assembly XML
 *
 * @author  Rick Goldberg
 * @since April 1, 2004, 10:09 AM
 * @version $Id$
 */
public class SimkitAssemblyXML2Java
{
    static final Logger LOG = LogManager.getLogger();

    public static final String ASSEMBLY_BINDINGS = "viskit.xsd.bindings.assembly";
    static final boolean DEBUG = false; // TODO: tie to ViskitStatics.debug?

    /* convenience Strings for formatting */
    /** space character */
    final private String sp  = SimkitXML2Java.SP;
    /** 4 space characters */
    final private String sp4 = sp+sp+sp+sp;
    /** 8 space characters */
    final private String sp8 = sp4+sp4;
    /** 12 space characters */
    final private String sp12 = sp8+sp4;
    /** opening brace { */
    final private String ob  = SimkitXML2Java.OB;
    /** closing brace } */
    final private String cb  = SimkitXML2Java.CB;
    /** semicolon ; */
    final private String sc  = SimkitXML2Java.SC;
    /** comma */
    final private String cm  = SimkitXML2Java.CM;
    /** left parenthesis ( */
    final private String lp  = SimkitXML2Java.LP;
    /** right parenthesis ) */
    final private String rp  = SimkitXML2Java.RP;
    /** equal sign = */
    final private String eq  = SimkitXML2Java.EQ;
    /** period . */
    final private String pd  = SimkitXML2Java.PD;
    /** period . */
    final private String pl  = SimkitXML2Java.PL;
    /** quotation mark " */
    final private String qu  = SimkitXML2Java.QU;
    /** "new" literal */
    final private String nw = "new";

    private SimkitAssembly simkitAssemblyRoot;
    InputStream    fileInputStream;
    private String fileBaseName;
    private static JAXBContext jaxbContext;

    /** Default constructor that creates the JAXBContext */
    public SimkitAssemblyXML2Java()
    {
        try {
            if (jaxbContext == null) // avoid JAXBException (perhaps due to concurrency)
                jaxbContext = JAXBContext.newInstance(ASSEMBLY_BINDINGS);
        } 
        catch (JAXBException ex) {
            LOG.error(ex); // TODO failing on retry
        }
    }

    public SimkitAssemblyXML2Java(InputStream is)
    {
        this();
        fileInputStream = is;
    }

    /**
     * Creates a new instance of SimkitAssemblyXML2Java
     * when used from another class.  Instance this
     * with a String for the name of the xmlFile.
     * @param xmlFile the name and path of an Assembly XML file
     * @throws FileNotFoundException
     */
    public SimkitAssemblyXML2Java(String xmlFile) throws FileNotFoundException
    {
        this(ViskitStatics.classForName(SimkitAssemblyXML2Java.class.getName()).getClassLoader().getResourceAsStream(xmlFile));
        setFileBaseName(new File(baseNameOf(xmlFile)).getName());
    }

    /** Used by Viskit
     * @param file the Assembly File to process
     * @throws FileNotFoundException
     */
    public SimkitAssemblyXML2Java(File file) throws FileNotFoundException 
    {
        this(new FileInputStream(file));
        setFileBaseName(baseNameOf(file.getName()));
    }

    public void unmarshal() 
    {
        try {
            Unmarshaller u = jaxbContext.createUnmarshaller();

            this.simkitAssemblyRoot = (SimkitAssembly) u.unmarshal(fileInputStream);

            // For debugging, make DEBUG true
            if (DEBUG) {
                marshalRoot();
            }
        } 
        catch (JAXBException ex) {
            LOG.error(ex);
//            ex.printStackTrace();
        }
    }

    public String getFileBaseName() 
    {
        return fileBaseName;
    }

    public final void setFileBaseName(String fileBaseName) {
        this.fileBaseName = fileBaseName;
    }

    public javax.xml.bind.Element unmarshalAny(String bindings)
    {
        JAXBContext priorJAXBContext = jaxbContext;
        Unmarshaller unmarshaller;
        try {
            jaxbContext = JAXBContext.newInstance(bindings);
            unmarshaller = jaxbContext.createUnmarshaller();
            jaxbContext = priorJAXBContext;
            return (javax.xml.bind.Element) unmarshaller.unmarshal(fileInputStream);
        }
        catch (JAXBException e) {
            jaxbContext = priorJAXBContext;
            return null;
        }
    }

    public void marshalRoot() 
    {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(this.simkitAssemblyRoot, System.out);
        } 
        catch (JAXBException ex) {
            LOG.error(ex);
        }
    }

    public String marshalToString(Object jaxbObject) 
    {
        Marshaller marshaller;
        String stringResult;
        if ( jaxbObject == null ) 
        {
            return "<Empty/>";
        }
        if (jaxbObject instanceof Results)
        {
            stringResult = "<Result/>";
        } 
        else {
            stringResult = "<Errors/>";
        }
        try {
            marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            StringWriter sw = new StringWriter();
            PrintWriter  pw = new PrintWriter(sw);
            marshaller.marshal(jaxbObject,pw);
            stringResult = sw.toString();
        }  
        catch (JAXBException e) {
            LOG.error(e);
//            e.printStackTrace();
        }
        return stringResult;
    }

    public String marshalFragmentToString(Object jaxbObject)
    {
        return marshalToString(jaxbObject);
    }

    public void marshal(File file) 
    {
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(file);
            marshal(simkitAssemblyRoot, fileOutputStream);
        }
        catch (FileNotFoundException e) {
            LOG.error(e);
//            e.printStackTrace();
        }
    }

    public void marshal(Object nodeObject, OutputStream outputStream) 
    {
        Marshaller marshaller;
        try {
            marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(nodeObject,outputStream);
        } catch (JAXBException e) {
            LOG.error(e);
//            e.printStackTrace();
        }
    }

    public SimkitAssembly getAssemblyRoot() {
        return simkitAssemblyRoot;
    }

    public String translateIntoJavaSource() 
    {
        StringBuilder sourceSB    = new StringBuilder();
        StringWriter  headSW      = new StringWriter();
        StringWriter  entitiesSW  = new StringWriter();
        StringWriter  listenersSW = new StringWriter();
        StringWriter  outputSW    = new StringWriter();
        StringWriter  tailSW      = new StringWriter();
        StringWriter  verboseSW   = new StringWriter();

        buildHead     (headSW);
        buildEntities (entitiesSW);
        buildListeners(listenersSW);
        buildOutput   (outputSW);
        buildVerbose  (verboseSW);
        buildTail     (tailSW);

        buildSource(sourceSB, headSW, entitiesSW, listenersSW, outputSW, verboseSW, tailSW);

        return sourceSB.toString();
    }

    void buildHead(StringWriter head) 
    {
        PrintWriter pw = new PrintWriter(head);
        String name           = this.simkitAssemblyRoot.getName();
        String packageName    = this.simkitAssemblyRoot.getPackage();
        String extendsName    = this.simkitAssemblyRoot.getExtend();
        String implementsName = this.simkitAssemblyRoot.getImplement();
        Schedule schedule;

        pw.println("package " + packageName + sc);
        pw.println();

        // Fully qualified names are used, no imports required
//        printImports(pw);
//        pw.println();

        if (extendsName.equals(ViskitStatics.JAVA_LANG_OBJECT)) {
            extendsName = "";
        } 
        else {
            extendsName = "extends" + sp + extendsName + sp;
        }
        if (implementsName != null) {
            implementsName = "implements" + sp + implementsName + sp;
        } 
        else {
            implementsName = "";
        }

        pw.println("public class " + name + sp + extendsName + implementsName);
        pw.println(ob);
        pw.println(sp4 + "public" + sp + name + lp + rp);
        pw.println(sp4 + ob);
        pw.println(sp8 + "super" + lp + rp + sc);
        if ( (schedule = this.simkitAssemblyRoot.getSchedule()) != null ) 
        {
            pw.print(sp8 + "setStopTime");
            pw.println(lp + schedule.getStopTime() + rp + sc);

            pw.print(sp8 + "setVerbose");
            pw.println(lp + schedule.getVerbose() + rp + sc);

            pw.print(sp8 + "setPrintReplicationReports");
            pw.println(lp + schedule.getPrintReplicationReports() + rp + sc);

            pw.print(sp8 + "setPrintSummaryReport");
            pw.println(lp + schedule.getPrintSummaryReport() + rp + sc);

            pw.print(sp8 + "setSaveReplicationData");
            pw.println(lp + schedule.getSaveReplicationData() + rp + sc);

            pw.println();
            pw.println(sp8 + "// let controlling simulation manager determine number of replications planned");
            pw.print  (sp8 + "// setNumberReplicationsPlanned");
            pw.println(lp + schedule.getNumberReplications() + rp + sc);
        }
        pw.println(sp4 + cb);
        pw.println();
    }

    /** Print out required imports to the Assembly
     * @param pw the PrintWriter to write out Java source
     */
    void printImports(PrintWriter pw)
    {
        SortedSet<String> sortedTreeSet = Collections.synchronizedSortedSet(new TreeSet<>());
        List<SimEntity> simEntityList = this.simkitAssemblyRoot.getSimEntity();

        for (SimEntity simEntity : simEntityList) {
            traverseForImports(simEntity, sortedTreeSet);
        }

        List<PropertyChangeListener> pclList = this.simkitAssemblyRoot.getPropertyChangeListener();

        for (PropertyChangeListener pcl : pclList) 
        {
            traverseForImports(pcl, sortedTreeSet);
        }

        String[] excludesArray = {
            "byte", "byte[]", "char", "char[]",
            "int", "int[]", "float", "float[]", "double", "double[]",
            "long", "long[]", "boolean", "boolean[]"
        };

        List<String> excludesList = java.util.Arrays.asList(excludesArray);

        synchronized (sortedTreeSet)
        {
            String className;
            Iterator<String> sortedTreeSetIterator = sortedTreeSet.iterator();
            while (sortedTreeSetIterator.hasNext()) 
            {
                className = sortedTreeSetIterator.next();
                if (excludesList.contains(className)) {
                    sortedTreeSetIterator.remove();
                    LOG.debug("Removed type \"" + className + "\" from the TreeSet");
                }
            }
        }

        for (String imports : sortedTreeSet) {
            if (!imports.startsWith("java.lang")) 
            {
                pw.println("import" + sp + imports + sc);
            }
        }
    }

    /** This method currently unused
     *
     * @param branch
     * @param tlist
     */
    void traverseForImports(Object branch, SortedSet<String> tlist) 
    {
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

    void buildEntities(StringWriter entities)
    {
        PrintWriter pw = new PrintWriter(entities);

        pw.println(sp4 + "@Override");
        pw.println(sp4 + "protected void createSimEntities" + lp + rp + sp);
        pw.println(sp4 + ob);
        List<Object> objectList;

        for (SimEntity simEntity : this.simkitAssemblyRoot.getSimEntity()) 
        {
            objectList = simEntity.getParameters();

            pw.println();
            pw.println(sp8  + "addSimEntity" + lp + sp + qu + simEntity.getName() + qu + cm);
            pw.print  (sp12 + nw + sp + simEntity.getType() + lp);

            if (!objectList.isEmpty()) 
            {
                pw.println();
                for (Object nextObject : objectList) {
                    doParameter(objectList, nextObject, sp12, pw);
                }
                pw.println(sp12 + rp);
            } 
            else {
                pw.println(rp);
            }

            pw.println(sp8 + rp + sc);
        }

        if ( !this.simkitAssemblyRoot.getSimEventListenerConnection().isEmpty() ) {
            pw.println();
        }

        for (SimEventListenerConnection sect : this.simkitAssemblyRoot.getSimEventListenerConnection()) 
        {
            pw.print  (sp8 + "addSimEventListenerConnection" + lp + qu + sect.getListener() + qu);
            pw.println(cm + sp + qu + sect.getSource() + qu + rp + sc);
        }

        if ( !this.simkitAssemblyRoot.getAdapter().isEmpty() ) {
            pw.println();
        }

        for (Adapter a : this.simkitAssemblyRoot.getAdapter()) 
        {
            pw.print(sp8 + "addAdapter" + lp + qu + a.getName() + qu + cm);
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
      * comma is needed. This may include a closing parenthesis or brace
      * and any nesting. Note that a doParameter may also be a caller
      * of a doParameter, so the comma placement is tricky.
      * @param parameterList
      * @param parameter
      * @param indent
      * @param pw
      */
    void doParameter(List<Object> parameterList, Object parameter, String indent, PrintWriter pw)
    {
        if ( parameter instanceof MultiParameter )
        {
            doMultiParameter((MultiParameter) parameter, indent, pw);
        } 
        else if ( parameter instanceof FactoryParameter ) {
            doFactoryParameter((FactoryParameter) parameter, indent, pw);
        } 
        else {
            doTerminalParameter((TerminalParameter) parameter, indent, pw);
        }
        maybeComma(parameterList, parameter, pw);
    }

    // with newer getSimEntityByName() always returns SimEntity, however
    // parameter may actually call for a subclass.
    String castIfSimEntity(String type)
    {
        String resultString = "";
        ClassLoader classLoader = ViskitGlobals.instance().getViskitApplicationClassLoader();
        try {
            if ((Class.forName("simkit.SimEntityBase", true, classLoader)).isAssignableFrom(Class.forName(type, true, classLoader))
                    ||
                    (Class.forName("simkit.SimEntity", true, classLoader)).isAssignableFrom(Class.forName(type))) {
                resultString = lp + type + rp;
            }
        } 
        catch (ClassNotFoundException cnfe) {
            // Do nothing
        }
        return resultString;
    }

    void doFactoryParameter(FactoryParameter factoryParameter, String indent, PrintWriter pw) 
    {
        String factoryName = factoryParameter.getFactory();
        String  methodName = factoryParameter.getMethod();
        List<Object> factoryParameterList = factoryParameter.getParameters();
        pw.println(indent + sp4 + castIfSimEntity(factoryParameter.getType()) + factoryName + pd + methodName + lp);
        for (Object parameterObject : factoryParameterList) {
            doParameter(factoryParameterList, parameterObject, indent + sp4, pw);
        }
        pw.print(indent + sp4 + rp);
    }

    void doTerminalParameter(TerminalParameter terminalParameter, String indent, PrintWriter pw) 
    {

        String type = terminalParameter.getType();
        String value;
        if ( terminalParameter.getLinkRef() != null ) 
        {
            value = ((TerminalParameter) (terminalParameter.getLinkRef())).getValue();
        } 
        else {
            value = terminalParameter.getValue();
        }
        if ( isPrimitive(type) )
        {
            pw.print(indent + sp4 + value);
        } 
        else if ( isString(type) ) {
            pw.print(indent + sp4 + qu + value + qu);
        } 
        else { // some Expression
            pw.print(indent + sp4 + castIfSimEntity(type) + value);
        }
    }

    void doSimpleStringParameter(TerminalParameter terminalParameter, PrintWriter pw) 
    {
        String type  = terminalParameter.getType();
        String value = terminalParameter.getValue();

        if ( isString(type) ) 
        {
            pw.print(qu + value + qu);
        } 
        else {
            error("Should only have a single String parameter for this PropertyChangeListener");
        }
    }

    public boolean isPrimitive(String type) 
    {
        return ViskitGlobals.instance().isPrimitive(type);
    }

    public boolean isString(String type) 
    {
        return type.contains("String");
    }

    public boolean isArray(String type) 
    {
        return ViskitGlobals.instance().isArray(type);
    }

    void doMultiParameter(MultiParameter multiParameter, String indent, PrintWriter pw) 
    {
        List<Object> parameterList = multiParameter.getParameters();
        String ptype = multiParameter.getType();

        if ( isArray(ptype) ) 
        {
            pw.println(indent + sp4 + nw + sp + ptype);
            pw.println(indent + sp4 + ob);
            for (Object parameterObject : parameterList) 
            {
                doParameter(parameterList, parameterObject, indent + sp4, pw);
            }
            pw.print(indent + sp4 + cb);
        } 
        else  // some multi param object
        {
            // Reduce redundant casting
            pw.println(indent + sp4 + /*castIfSimEntity(ptype) +*/ nw + sp + ptype + lp);
            for (Object parameterObject : parameterList) {
                doParameter(parameterList, parameterObject, indent + sp4, pw);
            }
            pw.print(indent + sp4 + rp);
        }
    }

    void maybeComma(List<Object> parameterList, Object parameterObject, PrintWriter pw) 
    {
        if ( parameterList.size() > 1 && parameterList.indexOf(parameterObject) < parameterList.size() - 1 ) 
        {
            pw.println(cm);
        } 
        else {
            pw.println();
        }
    }

    void buildListeners(StringWriter listeners) 
    {
        PrintWriter pw = new PrintWriter(listeners);
        Map<String, PropertyChangeListener> replicationStatisticsHashMap             = new LinkedHashMap<>();
        Map<String, PropertyChangeListener> designPointStatisticsHashMap             = new LinkedHashMap<>();
        Map<String, PropertyChangeListener> pclHashMap                               = new LinkedHashMap<>();
        Map<String, List<PropertyChangeListenerConnection>> pclConnectionListHashMap = new LinkedHashMap<>();

        String pclMode;
        for (PropertyChangeListener pcl : this.simkitAssemblyRoot.getPropertyChangeListener()) 
        {
            pclMode = pcl.getMode();

            if (null != pclMode) // For backwards compatibility
            {
                switch (pclMode) 
                {
                    case "replicationStat":
                    case "replicationStats":
                    case "replicationStatistic":
                    case "replicationStatistics":
                        replicationStatisticsHashMap.put(pcl.getName(), pcl);
                        break;
                    case "designPointStat":
                    case "designPointStats":
                    case "designPointStatistic":
                    case "designPointStatistics":
                        designPointStatisticsHashMap.put(pcl.getName(), pcl);
                        break;
                    default:
                        pclHashMap.put(pcl.getName(), pcl);
                        break;
                }
            }
        }

        String pclConnectionListenerName;
        List<PropertyChangeListenerConnection> pclConnectionList;
        for (PropertyChangeListenerConnection pclConnection : this.simkitAssemblyRoot.getPropertyChangeListenerConnection()) 
        {
            pclConnectionListenerName = pclConnection.getListener();
            pclConnectionList = pclConnectionListHashMap.get(pclConnectionListenerName);
            if (pclConnectionList == null) 
            {
                pclConnectionList = new ArrayList<>();
                pclConnectionListHashMap.put(pclConnectionListenerName, pclConnectionList);
            }
            pclConnectionList.add( pclConnection);
        }

        pw.println(sp4 + "@Override");
        pw.println(sp4 + "public void createPropertyChangeListeners" + lp + rp);
        pw.println(sp4 + ob);

        List<Object> pclParameterList;
        for (PropertyChangeListener pcl : pclHashMap.values()) 
        {
            pclParameterList = pcl.getParameters();
            pw.println(sp8 + "addPropertyChangeListener" + lp + qu + pcl.getName() + qu + cm);
            pw.print(sp12 + nw + sp + pcl.getType() + lp);

            if (!pclParameterList.isEmpty())
            {
                pw.println();
                for (Object pclParameter : pclParameterList) 
                {
                    doParameter(pclParameterList, pclParameter, sp12, pw);
                }
                pw.println(sp12 + rp);
            } 
            else {
                pw.println(rp);
            }
            pw.println(sp8 + rp + sc);
        }

        for (String propertyChangeListener : pclConnectionListHashMap.keySet()) 
        {
            for (PropertyChangeListenerConnection pclConnection : pclConnectionListHashMap.get(propertyChangeListener)) 
            {
                pw.print(sp8 + "addPropertyChangeListenerConnection" + lp + qu + propertyChangeListener + qu + cm + sp + qu + pclConnection.getProperty() + qu + cm + sp);
                pw.println(qu + pclConnection.getSource() + qu + rp + sc);
                pw.println();
            }
        }
        pw.println(sp8 + "super" + pd + "createPropertyChangeListeners" + lp + rp + sc);
        pw.println(sp4 + cb);
        pw.println();

        pw.println(sp4 + "@Override");
        pw.println(sp4 + "public void createReplicationStatistics" + lp + rp);
        pw.println(sp4 + ob);

        PropertyChangeListener pcl;
        List<PropertyChangeListenerConnection> replicationStatisticsPclConnectionList;
        for (String replicationStatisticsPcl : replicationStatisticsHashMap.keySet()) 
        {
            pcl = replicationStatisticsHashMap.get(replicationStatisticsPcl);
            pclParameterList = pcl.getParameters();
            pw.println(sp8 + "addReplicationStatistics" + lp + qu + replicationStatisticsPcl + qu + cm);
            pw.print(sp12 + nw + sp + pcl.getType() + lp);

            if (!pclParameterList.isEmpty()) 
            {
                pw.println();
                for (Object pclParameter : pclParameterList) 
                {
                    doParameter(pclParameterList, pclParameter, sp12, pw);
                }
                pw.println(sp12 + rp);
            } 
            else {
                pw.println(rp);
            }

            pw.println(sp8 + rp + sc);
            pw.println();
            replicationStatisticsPclConnectionList = pclConnectionListHashMap.get(replicationStatisticsPcl);
            if (replicationStatisticsPclConnectionList != null) 
            {
                for (PropertyChangeListenerConnection replicationStatisticsPclConnection : replicationStatisticsPclConnectionList)
                {
                    pw.print(sp8 + "addReplicationStatisticsListenerConnection" + lp + qu + replicationStatisticsPcl + qu + cm + sp + qu + replicationStatisticsPclConnection.getProperty() + qu + cm + sp);
                    pw.println(qu + replicationStatisticsPclConnection.getSource() + qu + rp + sc);
                    pw.println();
                }
            }
        }
        pw.println(sp8 + "super" + pd + "createReplicationStatistics" + lp + rp + sc);
        pw.println(sp4 + cb);
        pw.println();

        pw.println(sp4 + "@Override");
        pw.println(sp4 + "public void createDesignPointStatistics" + lp + rp);
        pw.println(sp4 + ob);

        for (String designPointStatisticsPcl : designPointStatisticsHashMap.keySet())
        {
            pcl = designPointStatisticsHashMap.get(designPointStatisticsPcl);
            pclParameterList = pcl.getParameters();
            pw.println(sp8 + "addDesignPointStatistics" + lp + qu + designPointStatisticsPcl + qu + cm);
            pw.print(sp12 + nw + sp + pcl.getType() + lp);

            if (!pclParameterList.isEmpty()) {
                pw.println();
                for (Object pclParameter : pclParameterList) {
                    doParameter(pclParameterList, pclParameter, sp12, pw);
                }
                pw.println(sp12 + rp);
            } 
            else {
                pw.println(rp);
            }
         }

        pw.println(sp8 + "super" + pd + "createDesignPointStatistics" + lp + rp + sc);

        pw.println(sp4 + cb);
        pw.println();
    }

    String getAssemblyName() 
    {
        String assemblyName = this.simkitAssemblyRoot.getName().substring(0,1); // lower camel case
        assemblyName = assemblyName.toLowerCase();
        assemblyName += this.simkitAssemblyRoot.getName().substring(1,this.simkitAssemblyRoot.getName().length());
        return assemblyName;
    }

    void buildVerbose(StringWriter out)
    {
        PrintWriter pw = new PrintWriter(out);
        List<Verbose> verboseList = this.simkitAssemblyRoot.getVerbose();
        if (!verboseList.isEmpty()) 
        {
            pw.println(sp4 + "// marker for verbose output"); // TODO build code
            pw.println();
        }
    }

    void buildOutput(StringWriter out)
    {
        PrintWriter pw = new PrintWriter(out);

        // override the printInfo method to dump detailed output from the nodes which are marked, if any
        List<Output> outputList = getAssemblyRoot().getOutput();
        if(!outputList.isEmpty()) 
        {
            pw.println(sp4 + "@Override");
            pw.println(sp4 + "public void printInfo()");
            pw.println(sp4 + ob);
            pw.println(sp8 + "System.out.println" + lp + rp + sc);
            pw.println(sp8 + "System.out.println" + lp + qu + "Entity Details" + qu + rp + sc);
            pw.println(sp8 + "System.out.println" + lp + qu + "--------------" + qu + rp + sc);
            dumpEntities(outputList, pw);
            pw.println(sp8 + "System.out.println" + lp + qu + "--------------" + qu + rp + sc);
            pw.println(sp4 + cb);
            pw.println();
        }
    }

    private void dumpEntities(List<Output> outputList, PrintWriter pw)
    {
        List<SimEntity> simEntityList = getAssemblyRoot().getSimEntity();
        List<PropertyChangeListener> pclList = getAssemblyRoot().getPropertyChangeListener();
        Object element;
        String name;
        String description = new String();
        for (Output output : outputList) 
        {
            element = output.getEntity();
            name = "<FIX: Output not of SimEntity or PropertyChangeListener>";

            for (SimEntity simEntity : simEntityList) 
            {
                if (simEntity.getName().equals(element.toString())) 
                {
                    name        = simEntity.getName();
                    description = simEntity.getDescription();
                    break;
                }
            }

            for (PropertyChangeListener pcl : pclList) 
            {
                if (pcl.getName().equals(element.toString())) {
                    name = pcl.getName();
                    description = pcl.getDescription();
                    break;
                }
            }

            if (!name.contains("<FIX:")) {
                pw.println(sp8 + "System.out.println" + lp + "getSimEntityByName" + lp + qu + name + qu + rp + sp + 
                                                        pl + sp + qu + sp + description + qu + rp + sc);
            }
        }
    }

    void buildTail(StringWriter t) 
    {
        PrintWriter pw = new PrintWriter(t);
        String assemblyName = getAssemblyName();

        // The main method doesn't need to dump the outputs, since they are done at object init time now
        pw.println(sp4 + "public static void main(String[] args)");
        pw.println(sp4 + ob);
        pw.println();
        pw.print(sp8 + this.simkitAssemblyRoot.getName() + sp + assemblyName + sp);
        pw.println(eq + sp + nw + sp + this.simkitAssemblyRoot.getName() + lp + rp + sc);

        pw.println(sp8 + nw + sp + "Thread" + lp + assemblyName + rp + pd + "start" + lp + rp + sc);

        pw.println(sp4 + cb);
        pw.println(cb);
    }

    void buildSource(StringBuilder source, StringWriter head, StringWriter entities,
            StringWriter listeners, StringWriter output, StringWriter verbose, StringWriter tail) 
    {
        source.append(head.getBuffer())
              .append(entities.getBuffer())
              .append(listeners.getBuffer());
        source.append(output.getBuffer())
              .append(verbose.getBuffer())
              .append(tail.getBuffer());
    }

    private String baseNameOf(String dotQualifiedName) 
    {
        return dotQualifiedName.substring(0, dotQualifiedName.indexOf(pd));
    }

    void runIt() 
    {
        try {
            File file = new File(pd); // local directory
            ClassLoader classLoader = new URLClassLoader(new URL[] {file.toURI().toURL()});
            Class<?> assemblyClass = classLoader.loadClass(this.simkitAssemblyRoot.getPackage() + pd + getFileBaseName());
            Object params[] = { new String[]{} };
            Class<?> classParams[] = { params[0].getClass() };
            Method mainMethod = assemblyClass.getDeclaredMethod("main", classParams);
            mainMethod.invoke(null, params);
        }
        catch (MalformedURLException | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) 
        {
            error(e.toString());
        }
    }

    /** Report and exit the JVMdescription@param desc a description of the encountered error
     */
    private void error(String description) {
        LOG.error(description);
        System.exit(1); // 1 = error condition
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        String xmlFile = args[0].replaceAll("\\\\", "/");

        LOG.info("Assembly file is: {}", xmlFile);
        LOG.info("Generating Java Source...");

        try (InputStream inputStream = new FileInputStream(xmlFile)) 
        {
            SimkitAssemblyXML2Java simkitAssemblyXML2Java = new SimkitAssemblyXML2Java(inputStream);
            simkitAssemblyXML2Java.unmarshal();
            String dotJava = simkitAssemblyXML2Java.translateIntoJavaSource();
            LOG.info("Done.");

            // also write out the .java to a file and compile it to a .class
            LOG.info("Generating Java Bytecode...");
            if (AssemblyControllerImpl.compileJavaClassFromString(dotJava) != null)
                LOG.info("Done.");
        } 
        catch (IOException ioe) {
            LOG.error(ioe);
        }
    }

} // end class file SimkitAssemblyXML2Java.java
