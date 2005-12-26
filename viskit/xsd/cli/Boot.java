/*
 * Boot.java
 *
 * Created on December 21, 2005, 12:00 PM
 *
 */
package viskit.xsd.cli;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLConnection;
import java.net.JarURLConnection;
import java.net.URLClassLoader;
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


/**
 *
 * @author Rick Goldberg
 */

public class Boot extends URLClassLoader implements Runnable {
    boolean debug = true;
    String[] args;

    public Boot(URL[] urls) {
        super(urls,Thread.currentThread().getContextClassLoader());
        addURL(urls[0]);
    }
    
    
    // TODO: -P ackage up a self contained jar given above args.
    public Boot(String[] args) {
        this(new URL[0]);
        this.args=args;
    }
    
    public void setArgs(String[] args) {
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
    
 
    public void addURL(URL jarURL) {
        URLConnection urlc;
        URL u;
        JarEntry je;
        JarFile jf;
        JarInputStream jis;
        try {
            urlc = jarURL.openConnection();
            jis = new JarInputStream(urlc.getInputStream());
            if ( urlc instanceof JarURLConnection ) {
                System.out.println("Help jar url"); // ? shouldn't be here
                
            } else { // from a file, cache internal jars externally
                super.addURL(jarURL);

                while ( ( je = jis.getNextJarEntry() ) != null ) {
                    String name = je.getName();
                    
                    if ( name.endsWith("jar") ) {
                        System.out.println("Found internal jar externalizing "+name);
                        String tmpDir = System.getProperty("java.io.tmpdir");
                        File extJar = new File(tmpDir, name);
                        FileOutputStream fos = new FileOutputStream(extJar);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int c = 0;
                        while ( (c = jis.read()) > -1 ) {
                            baos.write(c);
                        }
                        fos.write(baos.toByteArray());
                        fos.flush();
                        fos.close();
                        addURL(extJar.toURL());
                        System.out.println("File to new jar "+extJar.getCanonicalPath());
                        System.out.println("Added jar "+extJar.toURL().toString());
                        String systemClassPath = System.getProperty("java.class.path");
                        System.out.println("ClassPath "+systemClassPath+File.pathSeparator+extJar.getCanonicalPath());
                        System.setProperty("java.class.path", systemClassPath+File.pathSeparator+extJar.getCanonicalPath());
                    }
                    
                    
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    protected Class findClass(String name) throws ClassNotFoundException {
        System.out.println("Finding class "+name);
        Class clz = super.findClass(name);
        resolveClass(clz);
        System.out.println(clz);
        return clz;
    }
    
    public void run() {
        //load();
        try {
            Class lclaz = loadClass("viskit.xsd.cli.Launcher");
            Constructor lconstructor = lclaz.getConstructor(new Class[0]);
            Launcher launcher = (Launcher)lconstructor.newInstance(new Object[0]);
            Thread runner = new Thread(launcher);
            runner.setContextClassLoader(this);
            runner.start();
            runner.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public static void main(String[] args) throws Exception {
        URL u = Boot.class.getClassLoader().getResource("viskit/xsd/cli/Boot.class");
        System.out.println(u);
        URLConnection urlc = u.openConnection();
        if ( urlc instanceof JarURLConnection ) {
            u = ((JarURLConnection)urlc).getJarFileURL();
        }
        System.out.println("Booting "+u);
        Boot b = new Boot(new URL[] { u });
        b.setArgs(args);
        new Thread(b).start();
    }
}
