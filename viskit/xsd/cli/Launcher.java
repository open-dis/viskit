package viskit.xsd.cli;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.net.JarURLConnection;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.jar.Attributes;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.List;
import java.util.ListIterator;
import viskit.xsd.translator.SimkitXML2Java;
import viskit.xsd.assembly.SimkitAssemblyXML2Java;
import viskit.xsd.bindings.cli.*;
import bsh.Interpreter;
import bsh.BshClassManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

public class Launcher extends ClassLoader implements Runnable {
    Hashtable bytes;
    String assembly;
    String assemblyName;
    Vector eventGraphs;
    boolean debug = true;
    /**
     *  args -A ssemblyFile -F ully qualified classname to run [-E ventGraphFile]*[-J extensionJars]*
     */
    
    // TODO: -P ackage up a self contained jar given above args.
    public Launcher(String[] args) {
        bytes = new Hashtable();
        eventGraphs = new Vector();
        int a = 0;
        
        if ( args.length == 0 ) {
            // I'm in a jar, read config.xml entries
            // looking for
            // 1 AssemblyFile: <filename for Assembly XML>
            // 0 or more EventGraph: <filenames for EventGraphs>
            // 0 or more ExtensionJar: <filenames for Jars>
            // 1 AssemblyClass: <fully qualified class name for Assembly to run>
            // to setAssembly, addEventGraphs, loadJarClasses
            try {
                
                ClassLoader classLoader = Launcher.class.getClassLoader();
                if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
                
                InputStream configIn = classLoader.getResourceAsStream("config.xml");
                JAXBContext jc = JAXBContext.newInstance("viskit.xsd.cli");
                Unmarshaller um = jc.createUnmarshaller();
                ConfigType conf = (ConfigType) um.unmarshal(configIn);
                
                URL u = classLoader.getResource(conf.getAssembly().getFileName());
                setAssembly(u);
                assemblyName = conf.getAssembly().getClassName();
                
                List its = conf.getEventGraph();
                ListIterator li = its.listIterator();
                
                while ( li.hasNext() ) {
                    u = classLoader.getResource( ((EventGraph) li.next()).getFileName() );
                    addEventGraph(u);
                }
                
                its = conf.getExtensionJar();
                li = its.listIterator();
                
                while ( li.hasNext() ) {
                    u = classLoader.getResource( ((ExtensionJar) li.next()).getFileName());
                    loadJarClasses(u);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // I'm in command line mode
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
                    } else if (args[a].equals("-J")){
                        u = new URL(args[a+1]);
                        loadJarClasses(u);
                        a+=2;
                    } else if (args[a].equals("-F")) {
                        assemblyName = args[a+1];
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
        if (debug) {
            System.out.println("Assembly XML");
            System.out.println(xml.toString());
        }
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
        eventGraphs.addElement(xml.toString());
        if (debug) {
            System.out.println("EventGraph XML");
            System.out.println(xml.toString());
        }
    }
    
    
    /**
     *
     * In command line mode, a file url is supplied.
     * In jar execute mode, the url would have been a jar url, and in that
     * case look for internal jars to load as if the -J was on, recursively.
     *
     */
    public void loadJarClasses(URL jarFileURL) throws Exception {
        JarFile jarFile;
        URLConnection urlc = jarFileURL.openConnection();
        if ( urlc instanceof JarURLConnection ) {
            jarFile = ((JarURLConnection)urlc).getJarFile();
        } else {
            jarFile = new JarFile(urlc.getURL().getFile());
        }
        Enumeration e = jarFile.entries();
        while( e.hasMoreElements() ) {
            JarEntry jarEntry = (JarEntry)(e.nextElement());
            InputStream is = jarFile.getInputStream(jarEntry);
            String name = jarEntry.getName();
            if (debug) System.out.println("Try loading class "+name);
            if ( name.endsWith("class"))  {
                name.substring(0, jarEntry.getName().length() - 6);
                name.replace('/', '.');
                byte[] classBytes = new byte[(int)jarEntry.getSize()];
                is.read(classBytes);
                bytes.put(name,classBytes);
                if (debug) System.out.println("Added "+name);
            } else if ( name.endsWith("jar")) {
                if ( urlc instanceof JarURLConnection ) {
                    loadJarClasses(new URL(((JarURLConnection)urlc).getJarFileURL().toString() + name));
                } else {
                    loadJarClasses(new URL(urlc.getURL().toString() + name));
                }
            }
            
        }
    }
    
    public Class findClass(String name) {
        byte[] b = (byte[])bytes.get(name);
        try {
            if ( b != null ) {
                return defineClass(name, b, 0, b.length);
            }
        } catch ( Exception e ) {
            System.out.println("Can't load class "+name+" check to see if it is in the jars");
            e.printStackTrace();
        }
        
        return null;
    }
        
    public void run(){
        Interpreter bsh = new Interpreter();
        SimkitXML2Java xml2j;
        SimkitAssemblyXML2Java axml2j;
        ByteArrayInputStream bais;
        bsh.setClassLoader(this);

        try {
            bsh.eval("debug();");
            bsh.eval("setAccessibility(true);");
            // load eventGraph XML as classes
            Enumeration e = eventGraphs.elements();
            while( e.hasMoreElements() ){
                String eventGraph = (String) (e.nextElement());
                bais = new ByteArrayInputStream(eventGraph.getBytes());
                xml2j = new SimkitXML2Java(bais);
                xml2j.unmarshal();
                bsh.eval(xml2j.translate());
                if (debug) {
                    System.out.println("Bsh eval of:");
                    System.out.println(xml2j.translate());
                }
            }
            
            // now any and all dependencies are loaded for the assembly
            bais = new ByteArrayInputStream(assembly.getBytes());
            axml2j = new SimkitAssemblyXML2Java(bais);
            axml2j.unmarshal();
            bsh.eval(axml2j.translate());
            if (debug) {
                System.out.println("Bsh eval of:");
                System.out.println(axml2j.translate());
            }
            // run the assembly
            BshClassManager bcm = bsh.getClassManager();
            
            bsh.eval(assemblyName+".main(new String[0])");
            //bsh.eval("new Thread( new "+assemblyName+"() ).start();");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    public static void main(String[] args) {
        new Thread(new Launcher(args)) {
            
        }.start();
    }
}