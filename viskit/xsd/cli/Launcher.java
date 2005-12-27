package viskit.xsd.cli;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.net.JarURLConnection;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.StringTokenizer;

public class Launcher extends Thread implements Runnable {
    ClassLoader cloader;
    Hashtable bytes;
    String assembly;
    String assemblyName;
    Hashtable eventGraphs = new Hashtable();
    boolean debug = true;
    
    /**
     *  args -A ssemblyFile [-E ventGraphFile]
     */
    
    public Launcher() { }
    
    public void setup() {
        
        try {
            URL u;
  
            cloader = Thread.currentThread().getContextClassLoader();
            InputStream configIn = cloader.getResourceAsStream("config.properties");
            Properties p = new Properties();
            p.load(configIn);
            
            try {
                u = cloader.getResource(p.getProperty("Assembly"));
                setAssembly(u);
            } catch (Exception e) { // no assembly resource, launch viskit
                launchGUI();
            }
            
            
            StringTokenizer st = new StringTokenizer(p.getProperty("EventGraphs"));
            
            while ( st.hasMoreTokens() ) {
                u = cloader.getResource( st.nextToken() );
                addEventGraph(u);
            }
            
            
                        
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public Launcher(String[] args) {
        int a = 0;
        // I'm in command line mode TBD
        try {
            URL u;
            while(a < args.length - 1) {
                if (args[a].equals("-A")) {
                    u = new URL(args[a+1]);
                    setAssembly(u);
                    a+=2;
                } else if (args[a].equals("-E")) {
                    u = new URL(args[a+1]);
                    addEventGraph(u);
                    a+=2;
                } else {
                    throw new IllegalArgumentException(args[a]+" not a valid arg, exiting");
                }
                
            }
            
            if ( assemblyName == null ) throw new IllegalArgumentException("Must supply fully qualified class name to run");
            if ( assembly == null ) throw new IllegalArgumentException("Must supply at least one assembly URL");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * read in XML to String
     */ 
    public void setAssembly(URL assemblyURL) throws Exception {
        URLConnection urlc = assemblyURL.openConnection();
        InputStream is = urlc.getInputStream();
        int c = 0;
        StringBuffer xml = new StringBuffer();
        while( (c = is.read()) > -1 ) {
            xml.append((char)c);
        }
        assembly = xml.toString();
        if ( debug ) {
            System.out.println("Assembly XML");
            System.out.println(xml.toString());
        }
                
        // get the class name as defined in the XML
        if (debug) System.out.println("Trying ClassLoader "+cloader);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.toString().getBytes());
        
        if ( debug )
        if ( cloader instanceof java.net.URLClassLoader ) {
            URL[] usz = ((java.net.URLClassLoader)cloader).getURLs();
            for ( int i = 0 ; i < usz.length ; i ++ ) System.out.println("URL "+usz[i].toString());
        }
        
        // create a jaxb context to obtain the name field from the assembly
        Class jclz = cloader.loadClass("javax.xml.bind.JAXBContext");
        Method m = jclz.getDeclaredMethod("newInstance", new Class[] {String.class, ClassLoader.class} );
        Object jco = m.invoke(null,new Object[] { "viskit.xsd.bindings.assembly",cloader });
        m = jclz.getDeclaredMethod("createUnmarshaller", new Class[]{} );
        Object umo = m.invoke(jco, new Object[] {});
        
        // unmarshal the xml to java bindings
        jclz = cloader.loadClass("javax.xml.bind.Unmarshaller");
        m = jclz.getDeclaredMethod("unmarshal",  new Class[] { InputStream.class });
        Object assembly = m.invoke( umo, new Object[]{ bais });
        
        // get the root node of the assembly and obtain name of assembly
        jclz = cloader.loadClass("viskit.xsd.bindings.assembly.SimkitAssemblyType");
        m = jclz.getDeclaredMethod("getPackage", new Class[]{});
        assemblyName = (String)m.invoke(assembly, new Object[]{});
        m = jclz.getDeclaredMethod("getName", new Class[]{});
        assemblyName += "."+(String)m.invoke(assembly, new Object[]{});
        
    }
    
    /**
     * read in XML to String
     */ 
    public void addEventGraph(URL eventGraphURL) throws Exception {
        URLConnection urlc = eventGraphURL.openConnection();
        InputStream is = urlc.getInputStream();
        int c = 0;
        StringBuffer xml = new StringBuffer();
        while( (c = is.read()) > -1 ) {
            xml.append((char)c);
        }
        
        // get the class name as defined in the XML
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.toString().getBytes());
        Class jclz = cloader.loadClass("javax.xml.bind.JAXBContext");
        Method m = jclz.getDeclaredMethod("newInstance", new Class[] {String.class, ClassLoader.class} );
        Object jco = m.invoke(null,new Object[] { "viskit.xsd.bindings.eventgraph",cloader });
        m = jclz.getDeclaredMethod("createUnmarshaller", new Class[]{} );
        Object umo = m.invoke(jco, new Object[] {});
        
        jclz = cloader.loadClass("javax.xml.bind.Unmarshaller");
        m = jclz.getDeclaredMethod("unmarshal",  new Class[] { InputStream.class });
        Object eventgraph = m.invoke( umo, new Object[]{ bais });
        
        jclz = cloader.loadClass("viskit.xsd.bindings.eventgraph.SimEntityType");
        m = jclz.getDeclaredMethod("getPackage", new Class[]{});
        String eventGraphName = (String)m.invoke(eventgraph, new Object[]{});
        m = jclz.getDeclaredMethod("getName", new Class[]{});
        eventGraphName += "."+m.invoke(eventgraph, new Object[]{});
        
        eventGraphs.put(eventGraphName,xml.toString());
        if (debug) {
            System.out.println("EventGraph XML");
            System.out.println(xml.toString());
        }
    }
    
    
    public void run() {
        
        try {
            setup();
            exec();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void exec() throws Exception {
        Class xml2jz;
        Class axml2jz;
        Class bshz;
        Class bshcmz;
        Object xml2j;
        Object axml2j;
        Object bsh;
        Object bshcm;
        Object out;
        Method m;
        Constructor c;
        ByteArrayInputStream bais;
        String assemblyJava;
        
        xml2jz = cloader.loadClass("viskit.xsd.translator.SimkitXML2Java");
        axml2jz = cloader.loadClass("viskit.xsd.assembly.SimkitAssemblyXML2Java");
        bshz = cloader.loadClass("bsh.Interpreter");
        bshcmz = cloader.loadClass("bsh.BshClassManager");
        
        c = bshz.getConstructor(new Class[]{});
        bsh = c.newInstance(new Object[]{});
        
        m = bshz.getDeclaredMethod("getClassManager", new Class[]{});
        bshcm = m.invoke(bsh,new Object[]{});
        m = bshcmz.getDeclaredMethod("classExists",  new Class[]{ String.class });
        out = m.invoke(bshcm, new Object[]{"simkit.BasicAssembly"});
        if (debug) System.out.println("Checking if simkit.BasicAssembly exists... "+((Boolean)out).toString());
        
        try {
            Enumeration e = eventGraphs.keys();
            while ( e.hasMoreElements() ){
                String eventGraphName = (String) (e.nextElement());
                String eventGraph = (String)eventGraphs.get(eventGraphName);
                String eventGraphJava;
                
                // unmarshal eventGraph xml
                bais = new ByteArrayInputStream(eventGraph.getBytes());
                c = xml2jz.getConstructor(new Class[]{ InputStream.class });
                out = c.newInstance(new Object[]{ bais });
                m = out.getClass().getDeclaredMethod("unmarshal", new Class[]{});
                m.invoke(out, new Object[]{});
                
                // translate xml to generate java source
                m = out.getClass().getDeclaredMethod("translate", new Class[]{});
                out = m.invoke(out, new Object[]{});
                eventGraphJava = (String)out;
                
                // bsh eval generated java source
                m = bshz.getDeclaredMethod("eval", new Class[]{ String.class });
                m.invoke(bsh, new Object[]{ eventGraphJava });
                
                // sanity check bsh if eventGraph class exists
                m = bshcmz.getDeclaredMethod("classExists",  new Class[]{ String.class });
                out = m.invoke(bshcm, new Object[]{eventGraphName});
                System.out.println("Checking if "+eventGraphName+" exists... "+((Boolean)out).toString());
                
            }
            
            // unmarshal assembly xml
            bais = new ByteArrayInputStream(assembly.getBytes());
            c = axml2jz.getConstructor(new Class[]{ InputStream.class });
            out = c.newInstance(new Object[]{ bais });
            m = axml2jz.getDeclaredMethod("unmarshal", new Class[]{});
            m.invoke(out, new Object[]{});
            
            // translate xml to java source
            m = axml2jz.getDeclaredMethod("translate", new Class[]{});
            out = m.invoke(out,new Object[]{});
            assemblyJava = (String)out;
            
            // bsh eval the generated source
            m = bshz.getDeclaredMethod("eval", new Class[]{ String.class });
            m.invoke(bsh,new Object[]{ assemblyJava });
            
            // sanity check the bsh class loaders if assembly exists 
            m = bshcmz.getDeclaredMethod("classExists",  new Class[]{ String.class });
            out = m.invoke(bshcm, new Object[]{assemblyName});
            System.out.println("Checking if "+assemblyName+" exists... "+((Boolean)out).toString());
            
            // get the assembly class, create instance and thread it
            m = bshcmz.getDeclaredMethod("classForName",  new Class[]{ String.class });
            Class asmz = (Class)m.invoke(bshcm, new Object[]{assemblyName});
            c = asmz.getConstructor(new Class[]{});
            out = c.newInstance(new Object[]{});
            Thread t = new Thread((Runnable)out);
            t.start();
            
            // another one of many ways to do run the resulting assembly
            //m = bshz.getDeclaredMethod("eval", new Class[]{ String.class });
            //m.invoke(bsh,new Object[]{ "Thread t = new Thread(new "+assemblyName+"());"});
            //m = bshz.getDeclaredMethod("get", new Class[]{ String.class });
            //out = m.invoke(bsh, new Object[]{ "t" });
            //((Thread)out).start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        /* 
         * Above is mostly introspection of below, which only works in command line mode 
         * (ie with classpath set normally)
         *
         * to run within a jar of jars it has to be done via introspection. 
         *
        if (bsh.getClassManager().classExists("simkit.BasicAssembly")) System.out.println("simkit.BasicAssembly found");
        else System.out.println("simkit.BasicAssembly not found");
        try {
            bsh.eval("debug();");
            
            // load eventGraph XML as classes
            // since we've overridden the default bsh ClassLoader, need to 
            // add the defined class in this ClassLoader
            Enumeration e = eventGraphs.keys();
            while( e.hasMoreElements() ){
                String eventGraphName = (String) (e.nextElement());
                String eventGraph = (String)eventGraphs.get(eventGraphName);
                bais = new ByteArrayInputStream(eventGraph.getBytes());
                Class clz = Thread.currentThread().getContextClassLoader().loadClass("viskit.xsd.translator.SimkitXML2Java");
                Constructor cnstr = clz.getConstructor( new Class[] { InputStream.class } );
                //xml2j = new SimkitXML2Java(bais);
                xml2j = (SimkitXML2Java)cnstr.newInstance( new Object[] { bais } );
                xml2j.unmarshal();
                bsh.eval(xml2j.translate());
                if (debug) {
                    System.out.println("Bsh eval of:");
                    System.out.println(xml2j.translate());
                }
                
                Class evgc = bsh.getClassManager().classForName(eventGraphName);
                
                // add these, since this is going to be the class loader,
                // and could conflict with the one beanshell last used to
                // eval the string (?)
                eventGraphs.put(eventGraphName, evgc);
                if (debug) System.out.println("Eventgraph class added: "+eventGraphName+ " "+evgc);
            }
            
            // now any and all dependencies are loaded for the assembly
            bais = new ByteArrayInputStream(assembly.getBytes());
            Class clz = Thread.currentThread().getContextClassLoader().loadClass("viskit.xsd.assembly.SimkitAssemblyXML2Java");
            Constructor cnstr = clz.getConstructor( new Class[] { InputStream.class } );
            axml2j = (SimkitAssemblyXML2Java)cnstr.newInstance( new Object[] { bais } );
            
            axml2j.unmarshal();
            bsh.eval(axml2j.translate());
            if (debug) {
                System.out.println("Bsh eval of:");
                System.out.println(axml2j.translate());
            }
            // run the assembly
            BshClassManager bcm = bsh.getClassManager();
            bcm.dump(new java.io.PrintWriter(System.out));
            // according to bsh docs, can't reload classes 
            // if not using the addClassPath method, since
            // Launcher overrides the default bsh loader,
            // don't do the following:
            // bcm.reloadAllClasses();
            // instead, relaunch.
            if (bcm.classExists(assemblyName)) {
                System.out.println("Running Assembly "+assemblyName);
                //bsh.eval(assemblyName+".main(new String[0])");
                bsh.eval("Thread t = new Thread( new "+assemblyName+"() );");
                Thread t = (Thread) bsh.get("t");
                t.start();
            } else {
                System.out.println("Can't find Assembly "+assemblyName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
     */   
    }
    
    void launchGUI() {
        try {
            Class viskitz = cloader.loadClass("viskit.Splash2");
            Method m = viskitz.getDeclaredMethod("main", new Class[]{ String[].class });
            m.invoke(null, new Object[] { new String[]{"viskit.EventGraphAssemblyComboMain"} });
        } catch (Exception e) {
            System.out.println("Package error, can't run Viskit");
            e.printStackTrace();
            System.exit(1);
        }
    }
}