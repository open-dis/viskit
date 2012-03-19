/*
 * Boot.java
 *
 * Created on December 21, 2005, 12:00 PM
 */
package viskit.xsd.cli;

import edu.nps.util.TempFileManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @author Rick Goldberg
 * @version $Id$
 */
public class Boot extends URLClassLoader implements Runnable {
    static Boot bootee;
    static Thread booter;
    static Thread runner;
    private static final boolean debug = true;
    String[] args;
    public URL baseJarURL;

    public Boot(URL[] urls) {
        super(urls,Thread.currentThread().getContextClassLoader());
        addURL(urls[0]);
        baseJarURL = urls[0];
    }
    
    public Boot(URL[] urls, ClassLoader cloader) {
        super(urls,cloader);
        addURL(urls[0]);
        baseJarURL = urls[0];
    }
    
    // TODO: -P ackage up a self contained jar given above args.
    public Boot(String[] args) {
        this(new URL[0]);
        setArgs(args);
    }
    
    public final void setArgs(final String[] args) {
        this.args=args;
    }
    
    // don't need this anymore for jar in jar boot 
    // leave here for command line mode tbd
    public void load() {
        
        int a = 0;
        
        if ( args.length == 0 ) {
            // I'm in a jar, read config entries
            // looking for
            // 1 AssemblyFile: <filename for Assembly XML>
            // 0 or more EventGraph: <filenames for EventGraphs>
            try {
                URL u;
                
                InputStream configIn = getResourceAsStream("config.properties");
                Properties p = new Properties();
                p.load(configIn);
                
                StringTokenizer st = new StringTokenizer(p.getProperty("Jars"));
                    
                while ( st.hasMoreTokens() ) {
                    u = getResource( st.nextToken() );
                    System.out.println("Jar loading: "+u.toString());
                    addURL(u);
                }                

                
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // I'm in command line mode
            try {
                URL u;
                while(a < args.length - 1) {
                    if ( (args[a].equals("-J")) || (args[a].equals("-j")) ){
                        u = new URL(args[a+1]);
                        addURL(u);
                        a+=2;
                    } else {
                        throw new IllegalArgumentException(args[a]+" not a valid arg, exiting");
                    }
                    
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // provided mainly for Gridlets
    // to install 3rd pty jars
    public void addJar(URL jarURL) {
        addURL(jarURL);
    }

    @Override
    public final void addURL(URL jarURL) {
        URLConnection urlc;
        JarEntry je;
        JarInputStream jis;
        try {
            urlc = jarURL.openConnection();
            jis = new JarInputStream(urlc.getInputStream());
            if ( urlc instanceof JarURLConnection ) {
                // ? shouldn't be here, but save for 
                // other possible configurations,
                // would require a full blown resource manager
                // and class loader to make use of 
                // bytecode within jars within jars,
                // as no such URL can be formed, but
                // one could read in bytes and define
                // resources. the advantage of this would
                // only be that no temp files would be
                // needed to externalize the jars.
                System.out.println("Help jar url"); 
            } else { // from a file, cache internal jars externally
                //if ( !((jarURL.toString()).indexOf("tools") > 0))
                //first time through would be the internal url which
                //should be hopefully invalid
                    super.addURL(jarURL);

                while ( ( je = jis.getNextJarEntry() ) != null ) {
                    String name = je.getName();
                    
                    // test for which javac to pack
                    // note this depends on the builder to rename the tools.jars appropriately!
                    
                    if ( name.endsWith("jar") ) {
                        if (debug) {
                            System.out.println("Found internal jar externalizing " + name);
                        }
                        File extJar;
                        extJar = TempFileManager.createTempFile(name,".jar");
                        // note this file gets created for the duration of the server, is ok to use deleteOnExit
                        FileOutputStream fos = new FileOutputStream(extJar);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int c;
                        byte[] buff = new byte[512];
                        while ((c = jis.read(buff)) > -1 ) {
                            baos.write(buff,0,c);
                        }
                        fos.write(baos.toByteArray());
                        fos.flush();
                        fos.close();
                        // capture any jars within the jar in the jar...
                        addURL(extJar.toURI().toURL());
                        if (debug) {
                            System.out.println("File to new jar " + extJar.getCanonicalPath());
                        }
                        if (debug) {
                            System.out.println("Added jar " + extJar.toURI().toURL().toString());
                        }
                        String systemClassPath = System.getProperty("java.class.path");
                        System.setProperty("java.class.path", systemClassPath+File.pathSeparator+extJar.getCanonicalPath());
                        if (debug) {
                            System.out.println("ClassPath " + System.getProperty("java.class.path"));
                        }                        
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected Class findClass(String name) throws ClassNotFoundException {
        if (debug) {
            System.out.println("Finding class " + name);
        }
        Class clz = super.findClass(name);
        resolveClass(clz); // still needed?
        if (debug) {
            System.out.println(clz);
        }
        return clz;
    }
    
    @Override
    public void run() {
        try {
            Class<?> lclaz = loadClass("viskit.xsd.cli.Launcher");
            Constructor lconstructor = lclaz.getConstructor(new Class[0]);
            Launcher launcher = (Launcher) lconstructor.newInstance(new Object[0]);
            runner = new Thread(launcher);
            runner.setContextClassLoader(this);
            runner.start();
            runner.join();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String getJavaRev() {
        String rev = "java15";
        try {
            ClassLoader cloader = Thread.currentThread().getContextClassLoader();
            InputStream configIn = cloader.getResourceAsStream("config.properties");
            Properties p = new Properties();
            p.load(configIn);
            String javaVersion = (p.getProperty("JavaVersion") == null) ? "unk" : p.getProperty("JavaVersion");
            if (javaVersion.indexOf("1.4") > 0) {
                rev = "java14";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rev;
    }
    
    public static void main(String[] args) throws Exception {
        URL u = Boot.class.getClassLoader().getResource("viskit/xsd/cli/Boot.class");
        if (debug) {
            System.out.println(u);
        }
        URLConnection urlc = u.openConnection();
        if ( urlc instanceof JarURLConnection ) {
            u = ((JarURLConnection)urlc).getJarFileURL();
        }
        if (debug) {
            System.out.println("Booting " + u);
        }
        bootee = new Boot(new URL[] { u });
        bootee.setArgs(args);
        booter = new Thread(bootee);
        booter.start();
    }
}
