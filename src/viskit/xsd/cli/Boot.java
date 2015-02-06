/*
 * Boot.java
 *
 * Created on December 21, 2005, 12:00 PM
 */
package viskit.xsd.cli;

import edu.nps.util.LogUtils;
import edu.nps.util.TempFileManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.apache.log4j.Logger;
import viskit.VGlobals;

/**
 * @author Rick Goldberg
 * @version $Id$
 */
public class Boot extends URLClassLoader implements Runnable {
    static Boot bootee;
    static Thread booter;
    static Thread runner;
    static Logger log = LogUtils.getLogger(Boot.class);
    String[] args;
    public URL baseJarURL;

    public Boot(URL[] urls) {
        this(urls,VGlobals.instance().getWorkClassLoader());
    }

    public Boot(URL[] urls, ClassLoader cloader) {
        super(urls,cloader);
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


            } catch (IOException e) {
                log.error(e);
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

            } catch (IllegalArgumentException e) {
                log.error(e);
            } catch (MalformedURLException e) {
                log.error(e);
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
                        log.debug("Found internal jar externalizing " + name);
                        File extJar = TempFileManager.createTempFile(name,".jar");

                        // note this file gets created for the duration of the server, is ok to use deleteOnExit
                        try (FileOutputStream fos = new FileOutputStream(extJar)) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            int c;
                            byte[] buff = new byte[512];
                            while ((c = jis.read(buff)) > -1 ) {
                                baos.write(buff,0,c);
                            }
                            fos.write(baos.toByteArray());
                            fos.flush();
                        }
                        // capture any jars within the jar in the jar...
                        addURL(extJar.toURI().toURL());
                        log.debug("File to new jar " + extJar.getCanonicalPath());
                        log.debug("Added jar " + extJar.toURI().toURL().toString());
                        String systemClassPath = System.getProperty("java.class.path");
                        System.setProperty("java.class.path", systemClassPath+File.pathSeparator+extJar.getCanonicalPath());
                        log.debug("ClassPath " + System.getProperty("java.class.path"));
                    }
                }
            }
        } catch (IOException e) {
            log.error(e);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        log.debug("Finding class " + name);

        Class<?> clz = super.findClass(name);
        resolveClass(clz); // still needed?

        log.debug(clz);

        return clz;
    }

    @Override
    public void run() {
        try {
            Class<?> lclaz = loadClass("viskit.xsd.cli.Launcher");
            Constructor<?> lconstructor = lclaz.getConstructor(new Class<?>[0]);
            Launcher launcher = (Launcher) lconstructor.newInstance(new Object[0]);
            runner = new Thread(launcher);
            runner.setContextClassLoader(this);
            runner.start();
            runner.join();

        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InstantiationException | InterruptedException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
            log.error(e);
        }
    }

    private String getJavaRev() {
        String rev = "java15";
        try {
            ClassLoader cloader = VGlobals.instance().getWorkClassLoader();
            InputStream configIn = cloader.getResourceAsStream("config.properties");
            Properties p = new Properties();
            p.load(configIn);
            String javaVersion = (p.getProperty("JavaVersion") == null) ? "unk" : p.getProperty("JavaVersion");
            if (javaVersion.indexOf("1.4") > 0) {
                rev = "java14";
            }
        } catch (IOException e) {
            log.error(e);
        }
        return rev;
    }

    public static void main(String[] args) throws Exception {
        URL u = Boot.class.getClassLoader().getResource("viskit/xsd/cli/Boot.class");
        log.debug(u);
        URLConnection urlc = u.openConnection();
        if ( urlc instanceof JarURLConnection ) {
            u = ((JarURLConnection)urlc).getJarFileURL();
        }
        log.debug("Booting " + u);
        bootee = new Boot(new URL[] { u });
        bootee.setArgs(args);
        booter = new Thread(bootee);
        booter.start();
    }
}
