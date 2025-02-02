package viskit.doe;

import edu.nps.util.Log4jUtilities;
import edu.nps.util.TempFileManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.logging.log4j.Logger;

import viskit.ViskitGlobals;
import viskit.ViskitStatics;

/** LocalBootLoader is similar to Viskit's Vstatics.classForName and implements
 * class loading that can be used in "Local Grid" mode.
 *
 * In "Remote Grid" mode, Grid nodes can't have already loaded classes
 * from the Viskit panel, not unless we serialize the classes and their
 * instances, which could be problematic.
 *
 * So in Remote ( or Regular ) Grid mode, a class loader
 * called Boot loads up all the Event Graphs from XML, the Assembly,
 * and any jars sent via the XML-RPC call, or any jars packaged within
 * the Gridkit deployment jar.
 *
 * In Local mode, all these classes should be loaded by Viskit already,
 * by DOE time, and we'd like to run a DOE with little interaction from
 * Viskit other than the API's already used for Gridkit, so basically
 * 'logging in' to a local service. It should still build up class definitions
 * from the Assembly XML since in each replication a new class definition
 * of the same type is used.
 *
 * Viskit caches all generated classes in the VGlobals workDirectory,
 * however, it also caches the Assembly classes, which need to be
 * "zero turn-around" for each DesignPoint in the experiment, meaning
 * a class loader has to "forget" the Assembly class each time since
 * the parameters are coded into the class and these change per
 * DesignPoint. Since DesignPoint runs can be done in parallel threads,
 * each thread should own its own individual context instance.
 *
 * This class should read from the workDirectory, discard any Assembly
 * classes, jar the remaining ones or otherwise pass the new directory
 * to the super-class which then adds them to the current context
 * in a disposable manner. Aside from the common runtime classes,
 * the current context class loader should already resolve Viskit specific
 * libs, such as simkit.jar. Then LocalBoot should add all jars found
 * in Viskit's lib before loading the pruned workDirectory and then
 * be ready to accept a modified Assembly; in this way one or many
 * threads can be executed, each with their own LocalBoot context.
 *
 * In other words, this is a two stage process, first capture the cache as
 * last left by Viskit and prune it, then spawn as many threads per
 * DesignPoint modified Assemblies, which inherit stage one context but
 * individually create the second stage. Both stages can be handled by
 * the LocalBootLoader class. The zero'th stage is adding all classes
 * required to run Simkit and the ViskitAssembly.
 *
 * Each thread that uses a LocalBootLoader should set the contextClassLoader
 * to be its own LocalBootLoaders's parent's parent; this should enable multiple
 * threads to use class methods in a unique context, e.g. Schedule.reset();
 *
 * In order to do that, as in create a separate context for Simkit per Thread,
 * without running an external Process, everything must be read into a
 * ClassLoader that has the current Viskit running Thread's parent's
 * contextClassLoader, above from where simkit.jar got loaded in, i.e., the
 * stage prior to reading in the lib directory during JVM initialization. Then
 * each new ClassLoader so constructed can have a unique Simkit run
 * simultaneously. Furthermore, it can now have a unique Assembly per Thread
 * as originally expected.
 *
 * @author Rick Goldberg
 * @since December 27, 2006, 11:47 AM
 * @version $Id$
 */
public class LocalBootLoader extends URLClassLoader {

    private final static Logger LOG = Log4jUtilities.getLogger(LocalBootLoader.class);
    
    String[] classPath;
    LocalBootLoader stage1;
    File workDir;
    URL[] extUrls;
    
    boolean allowAssembly = false;
    private boolean reloadSimkit = false;

    /** Creates a new instance of LocalBootLoader
     * @param classes external classpath urls
     * @param parent the parent Classloader to this one
     * @param workDir the current project working directory
     */
    public LocalBootLoader(URL[] classes, ClassLoader parent, File workDir) {
        super(new URL[] {}, parent);
        extUrls = classes;
        this.workDir = workDir;
        LOG.debug(ViskitGlobals.instance().printCallerLog());
    }

    /** Create a context with viskit's libs along with the generated
     * event graphs, takes two stages the returned LocalBootLoader can be used as
     * an isolated context, i.e., where static methods and variables from one
     * class don't interfere with one another or between LocalBootLoaders, here
     * by setting threads' contextClassLoaders to their own LocalBootLoaders.
     *
     * @param allowAssembly
     * @return an isolated Context Class Loader instance
     */
    public LocalBootLoader initialize(boolean allowAssembly)
    {
        this.allowAssembly = allowAssembly;
        return LocalBootLoader.this.initialize();
    }

    /** @return a custom ClassLoader */
    public LocalBootLoader initialize() 
    {
        File jar = null, tempUrl;
        String[] tempClasspath;

        // Capture the current runtime classpath
        initializeStage1();

        stage1.allowAssembly = this.allowAssembly;

        // Now add any external classpaths
        for (URL ext : extUrls) {

            // can happen if extraClassPaths.path[@value] is null or erroneous
            if (ext == null) {continue;}

            stage1.addURL(ext);
            tempClasspath = new String[getClassPath().length + 1];
            System.arraycopy(getClassPath(), 0, tempClasspath, 0, classPath.length);
            try {
                tempUrl = new File(ext.toURI());
                tempClasspath[tempClasspath.length - 1] = tempUrl.getPath();
                LOG.debug("Extra path: {}",tempUrl.getPath());
                classPath = tempClasspath;
            } catch (URISyntaxException ex) {
                LOG.error(ex);
            }
        }

        // TODO: Clearly, adding build/classes to the classpath violates the
        // dirty assembly in the classpath issue that we were attempting to
        // mitigate with the buildCleanWorkJar call below, but something doesn't
        // go quite right if we are building a project from scratch where
        // we have no build/classes compiled, and/or have no cached Event Graphs in the
        // viskitProject.xml. So, leaving commeted out for now. Will need
        // to reopen this issue when we need strict design points for DOE.

        // Now add our project's working directory, i.e. build/classes
        try {

            stage1.addURL(getWorkDirectory().toURI().toURL());
            tempClasspath = new String[getClassPath().length + 1];
            System.arraycopy(getClassPath(), 0, tempClasspath, 0, classPath.length);
            try {
                tempClasspath[tempClasspath.length - 1] = getWorkDirectory().getCanonicalPath();
                classPath = tempClasspath;
            } catch (IOException ex) {
                LOG.error(ex);
            }
        } catch (MalformedURLException ex) {
            LOG.error(ex);
        }

//        jar = buildCleanWorkJar(); // See TODO note above

        // stage1 gets dirty during bring up of clean jar <-- Why?
        // reboot it with cleanWorkJar
        LOG.debug("Stage1 reinit");
//        initStage1();
        LOG.debug("Adding cleaned jar "+jar);

        // Now add our tmp jars containing compiled Event Graphs and Assemblies
//        try {
//            if (jar != null)  {
//
//                // If this is the first time through, and no cached Event Graphs, we are
//                // now adding our project's build/classes path here
//                stage1.addURL(jar.toURI().toURL());
//                String[] tmp = new String[getClassPath().length + 1];
//                System.arraycopy(getClassPath(), 0, tmp, 0, getClassPath().length);
//                try {
//                    tmp[tmp.length - 1] = jar.getCanonicalPath();
//                    classPath = tmp;
//                } catch (IOException ex) {
//                    LOG.error(ex);
//                }
//            }
//        } catch (MalformedURLException ex) {
//            LOG.error(ex);
//        }

        // Now normalize all the paths in the classpath variable[]
        tempClasspath = new String[getClassPath().length];
        int idx = 0;
        for (String path : getClassPath()) {
            tempClasspath[idx] = path;
            idx++;
        }

        stage1.classPath = tempClasspath;
        return stage1;
    }

    /**
     * Convenience method for the DOE local driver
     * @param u the URL to add to this ClassLoader
     */
    public void doAddURL(URL u) {
        super.addURL(u);
    }

    /**
     * Convenience method for the DOE local driver
     * @return a URL[] of External ClassPath paths
     */
    public URL[] getExtUrls() {
        return extUrls;
    }

    /** @return a custom String[] with our classpath */
    public String[] getClassPath() {

        // very verbose when "debug" mode
        for (String path : classPath) {
            path = path.replaceAll("\\\\", "/");
            LOG.debug(path);
        }
        LOG.debug("End ClassPath entries\n");
        return classPath;
    }

    /** @return the working class directory for this project */
    public File getWorkDirectory() {
        return workDir;
    }

    /** @return an indication for allowing an Assembly to be jarred up */
    public boolean getAllowAssemby() {
        return allowAssembly;
    }

    /** @param enable if true allows Assembly inclusion in temp jars */
    public void setAllowAssembly(boolean enable) {
        this.allowAssembly = enable;
    }

    /** @param reload if true, reloads the simkit.jar on the java classpath */
    public void setReloadSimkit(boolean reload) {
        this.reloadSimkit = reload;
    }

    /** Creates new instances of the stage1 LocalBootLoader */
    private void initializeStage1() {

        String classPathProp = System.getProperty("java.class.path");
        classPathProp = classPathProp.replaceAll("\\\\", "/");
        String sep = System.getProperty("path.separator");
        StringBuilder cPath = new StringBuilder();

        // Handle case where we launch a viskit-editor.jar with a Class-Path header
        // defined in order to find simkit.jar
        if (!classPathProp.toLowerCase().contains("simkit.jar")) {
            URL url;
            JarURLConnection uc;
            Attributes attr;
            try {
                url = new URI("jar:file:" + classPathProp + "!/").toURL();
                uc = (JarURLConnection) url.openConnection();
                attr = uc.getMainAttributes();
                cPath.append(classPathProp);
                for (String path : attr.getValue(Attributes.Name.CLASS_PATH).split(" ")) {
                    cPath.append(sep);
                    cPath.append(path);
                }
            } catch (IOException | NullPointerException | URISyntaxException ex) {
                LOG.error(ex);
            }

            classPath = cPath.toString().split(sep);

        } else {
            classPath = classPathProp.split(sep);
        }

        ClassLoader parentClassLoader = getParent();

        stage1 = new LocalBootLoader(new URL[] {}, parentClassLoader, getWorkDirectory());
        boolean loop = !allowAssembly;

        // if each LocalBootLoader individually has to read from
        // a file, then each instance of the loader will have its own
        // context in terms of static variables from the read-in classes,
        // e.g. simkit.Schedule.reset() in one thread which will not reset
        // another. See sample case StaticsTest
        while (loop) {
            try {
                if (reloadSimkit) {
                    stage1.loadClass(ViskitStatics.RANDOM_VARIATE_CLASS);
                } else {
                    stage1.loadClass(ViskitStatics.LOCAL_BOOT_LOADER);
                }
                //System.out.println("still found existing viskit context, going up one more...");
                parentClassLoader = parentClassLoader.getParent();
                stage1 = new LocalBootLoader(new URL[] {}, parentClassLoader, getWorkDirectory());
            } catch (ClassNotFoundException e) {
                loop = false;
            }
        }

        // build up stage1 libs
        for (String path : getClassPath()) {
            try {
                stage1.addURL(new File(path).toURI().toURL());
            } catch (MalformedURLException ex) {
                LOG.error(ex);
            }
        }
    }

    /**
     * Make clean jars for each thread each with its own isolated ClassLoader
     * @return a clean jar file for each thread each with its own isolated ClassLoader
     */
    private File buildCleanWorkJar() {
        File newJar = null;
        try {

            // Don't jar up an empty build/classes directory
            if (getWorkDirectory().listFiles().length == 0) {return null;}

            // this potentially "dirties" this instance of stage1
            // meaning it could have Assembly classes in it
            stage1.addURL(getWorkDirectory().toURI().toURL());

            // make a clean version of the file in jar form
            // to be added to a newer stage1 (rebooted) instance.
            newJar = makeJarFileFromDirectory(getWorkDirectory());

        } catch (MalformedURLException ex) {
            LOG.error(ex);
        }

        return newJar;
    }

    private File makeJarFileFromDirectory(File dir2jar) {
        File jarOut = dir2jar;
        JarOutputStream jos = null;
        try {
            jarOut = TempFileManager.createTempFile("eventGraphs", ".jar");
            OutputStream fos = new FileOutputStream(jarOut);
            jos = new JarOutputStream(fos);
            if (dir2jar.isDirectory())
                makeJarFileFromDirectory(dir2jar, dir2jar, jos);

            jos.flush();
        } catch (java.util.zip.ZipException ze) {

            // could be first time through; caused by no entries in the jar
            return dir2jar;
        } catch (IOException ex) {
            LOG.error(ex);
        } finally {
            try {
                if (jos != null)
                   jos.close();
            } catch (IOException ex) {}
        }
        return jarOut;
    }

    private void makeJarFileFromDirectory(File baseDir, File newDir, JarOutputStream jos) {
        File[] dirList = newDir.listFiles();
        InputStream fis = null;
        JarEntry je;
        String entryName;
        String entryClass;
        Class<?> clz, vzClz;
        byte[] buf;
        int c;
        for (File file : dirList) {

            // Recurse until we get to .class files
            if (file.isDirectory()) {
                makeJarFileFromDirectory(baseDir, file, jos);
            } else {

                entryName = file.getParentFile().getName() + "/" + file.getName();
                entryClass = entryName.replace('/', '.');

                //System.out.println("Entry Class "+entryClass);
                String dotClass = ".class";
                if (entryClass.endsWith(dotClass)) { // else do nothing

                    entryClass = entryClass.substring(0, entryClass.indexOf(dotClass));
                    //if (tab!=null) tab.writeStatus("Entry: " + entryClass);
                    // it is possible, if one ran an Assembly before going to the
                    // DoE panel to create an Experiment, that a compiled version of
                    // the Assembly is in the work directory. Since this class has
                    // to change each DesignPoint, the class file is pruned from the
                    // cleaned jar so that the runner thread can regenerate the class
                    // each time, as the original class definition would otherwise be
                    // conflictingly already in the loader. On second pass of the stage1
                    // bring up, this cleaned-up jar is then added to the classpath without
                    // loading the original directory.
                    boolean isEventGraph = true;
                    try {
                        clz = stage1.loadClass(entryClass);
                        vzClz = stage1.loadClass("viskit.assembly.ViskitAssembly");
                        if (vzClz.isAssignableFrom(clz))
                            isEventGraph = false;
                        
                    } catch (ClassNotFoundException ex) {
                        System.err.println("Check that viskit.jar has jaxb bindings, or: " + entryClass);
                        LOG.error(ex);
                    }

                    if (isEventGraph | allowAssembly) {
                        try {
                            je = new JarEntry(entryName);
                            jos.putNextEntry(je);
                            fis = new FileInputStream(file);
                            buf = new byte[256];
                            while ((c = fis.read(buf)) > 0)
                                jos.write(buf, 0, c);
                            
                            jos.closeEntry();
                        } catch (IOException ex) {
                            LOG.error(ex);
                        } finally {
                            try {
                                if (fis != null)
                                    fis.close();
                            } catch (IOException ex) {}
                        }
                    }
                }
            }
        }
    }
    
} // end class file LocalBootLoader.java
