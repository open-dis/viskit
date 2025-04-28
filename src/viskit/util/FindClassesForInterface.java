package viskit.util;

import edu.nps.util.Log4jUtilities;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.doe.LocalBootLoader;

/** A custom class finder to support finding Event Graphs and PCLs in *.class form vice
 * XML. Used to populate the LEGOs tree on the Assembly Editor.
 *
 * @author  ahbuss
 */
public class FindClassesForInterface
{
    static final Logger LOG = LogManager.getLogger();

    /**
     * Added by Mike Bailey
     * @param f Class file to read from
     * @param implementing possibly a class of type simkit.BasicSimEntity
     * @return Class object iif of type simkit.BasicSimEntity
     */
    public static Class<?> classFromFile(File f, Class<?> implementing) {
        Class<?> c = null;
        try {
//            c = classFromFile(f);
            c = ViskitStatics.classForName(f.getPath());

            if (c.isInterface() || !isConcrete(c)) {
                c = null;
            } else if (implementing != null && !implementing.isAssignableFrom(c)) {
                c = null;
            }
        } catch (Throwable t) {
            // do nothing
        }
        return c;
    }

    /**
     * Added by Mike Bailey.  Same test as above.
     * @param questionable the class to evaluate
     * @param target the class assignable from the questionable class
     * @return an indication of success
     */
    public static boolean matchClass(Class<?> questionable, Class<?> target) {
        return (!questionable.isInterface() && target.isAssignableFrom(questionable) && isConcrete(questionable));
    }

    /**
     * Simple method to try to load a .class file
     * @param f the file to evaluate
     * @return the class representation of this file
     * @throws java.lang.Throwable if the class can not be found
     */
    public static Class<?> classFromFile(File f) throws java.lang.Throwable {
        return new MyClassLoader().buildIt(f);
    }

    /**
     * Custom classloader in support of classFromFile
     */
    static class MyClassLoader extends ClassLoader {

        private File f;
        private ByteBuffer buffer;
        private RandomAccessFile classFile;
        private final Map<String, Class<?>> found = new Hashtable<>();

        Class<?> buildIt(File fil) throws java.lang.Throwable {
            f = fil;
            return loadClass(f.getName());
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            Class<?> clazz = found.get(name);
            if (clazz != null) {
                return clazz;
            }
            try {
                classFile = new RandomAccessFile(f, "r");
                FileChannel fc = classFile.getChannel();
                buffer = ByteBuffer.allocate((int) fc.size());
                fc.read(buffer);
            } catch (IOException thr) {
                throw new ClassNotFoundException(thr.getMessage());
            }
            try {
                LOG.debug("Attempting to find " + name);

                clazz = defineClass(null, buffer.array(), 0, buffer.capacity()); // do this to get proper name/pkg
                found.put(name, clazz);

                LOG.debug("Found Class: " + clazz.getName() + "\n");
            } 
            catch (Exception e) {
                LOG.error("findClass(" + name + ") exception: " + e.getMessage());
            } 
            finally {
                try {
                    if (classFile != null)
                        classFile.close();
                } 
                catch (IOException ioe) {
                    LOG.error("findClass(" + name + ") finally block, close file exception: " + ioe.getMessage());
                }
            }
            return clazz;
        }
    }

    /**
     * Create a list of the classes (Class objects) implementing
     * a desired interface
     * @param jarFile The jar file to be examined for classes
     * @param implementing The class that classes should implement
     * @return List containing the Class objects implementing the
     * desired interface
     */
    public static List<Class<?>> findClasses(JarFile jarFile, Class<?> implementing) {
        List<Class<?>> found = new ArrayList<>();
        URLClassLoader loader = ((LocalBootLoader) ViskitGlobals.instance().getViskitApplicationClassLoader());
        JarEntry nextEntry;
        Class<?> c;
        for (Enumeration entries = jarFile.entries(); entries.hasMoreElements();) {
            nextEntry = (JarEntry) entries.nextElement();
            if (nextEntry.getName().startsWith("META"))
                continue;
            
            if (loader != null)
                try {
                    c = loader.loadClass(getClassName(nextEntry.getName()));
                    if (c.isInterface())
                        continue;
                    
                    if (implementing.isAssignableFrom(c) && isConcrete(c))
                        found.add(c);
                    
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // do nothing
                }
        }
        return found;
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.Throwable pass along throwable exceptions
     */
    public static void main(String[] args) throws Throwable {
        String jarFileName = args.length > 0 ? args[0] : "/R:/Simkit/simkit.jar";
        JarFile jarFile = new JarFile(jarFileName);
        List list = findClasses(jarFile, simkit.BasicSimEntity.class);
        LOG.info(jarFile.getName());
        LOG.info("SimEntity:");
        for (int i = 0; i < list.size(); ++i) {
            LOG.info("\t" + list.get(i));
        }
        list = findClasses(jarFile, java.beans.PropertyChangeListener.class);
        LOG.info("PropertyChangeListener:");
        for (int i = 0; i < list.size(); ++i) {
            LOG.info("\t" + list.get(i));
        }

//        jarFile = new JarFile("R:\\Simkit\\simkit.jar");
        LOG.info("RandomVariates:");
        list = findClasses(jarFile, simkit.random.RandomVariate.class);
        for (int i = 0; i < list.size(); ++i) {
            LOG.info("\t" + list.get(i));
        }
        LOG.info("RandomNumbers:");
        list = findClasses(jarFile, simkit.random.RandomNumber.class);
        for (int i = 0; i < list.size(); ++i) {
            LOG.info("\t" + list.get(i));
        }

        if (true) {
            return;
        }
        List<Class<?>> simEntityClassList = new ArrayList<>();
        List<Class<?>> propertyChangeListenerClassList = new ArrayList<>();
        LOG.info(jarFile.getName());
        URLClassLoader loader = new URLClassLoader(new URL[]{new File(jarFile.getName()).toURI().toURL()});
        for (Enumeration entries = jarFile.entries(); entries.hasMoreElements();) {
            JarEntry nextEntry = (JarEntry) entries.nextElement();
            if (nextEntry.getName().startsWith("META")) {
                continue;
            }
//            LOG.info(getClassName(nextEntry.getName()));

            try {
                Class<?> c = loader.loadClass(getClassName(nextEntry.getName()));
                if (c.isInterface()) {
                    continue;
                }
//                LOG.debug(c);
                if (java.beans.PropertyChangeListener.class.isAssignableFrom(c) && isConcrete(c)) {
                    propertyChangeListenerClassList.add(c);
//                    LOG.info("\tIs PropertyChangeListener!");
                }
                if (simkit.SimEntity.class.isAssignableFrom(c) && isConcrete(c)) {
                    simEntityClassList.add(c);
//                    LOG.info("\tIs SimEntity!");
                }
            } catch (ClassNotFoundException t) {
//                LOG.info("\t" + nextEntry + " not loaded");
            }
        }
        LOG.info("SimEntities:");
        for (int i = 0; i < simEntityClassList.size(); i++) {
            LOG.info(simEntityClassList.get(i));
        }
        LOG.info("PropertyChangeListeners:");
        for (int i = 0; i < propertyChangeListenerClassList.size(); i++) {
            LOG.info(propertyChangeListenerClassList.get(i));
        }

        if (true) {
            return;
        }
        LOG.info(loader);
        Class<?> c = loader.loadClass("png.PNGChunk");
        LOG.info(c);

        String ps = System.getProperty("path.separator");
        File file = new File("/C:/tmp/MiscTest/png/PNGData.class");
        LOG.info(file.getCanonicalPath() + "[" + file.exists() + "]");
        String fullyQualified = file.getAbsolutePath().substring(0,
                file.getAbsolutePath().lastIndexOf('.')).replaceAll("\\\\", ".");
        LOG.info(getClassName(fullyQualified));
    //        for (int i = 0; i < pieces.length; ++i) {
    //            LOG.info("\t" + pieces[i]);
    //        }
    }

    /**
     * Convert a file name of the bytecodes (".class" file)
     * to the (presumed) class name
     * @param name Name of the file
     * @return name of the class
     */
    public static String getClassName(String name) {
        int index = name.lastIndexOf(".class");
        if (index >= 0) {
            name = name.substring(0, index);
            name = name.replaceAll("/", ".");
        }
        return name;
    }

    /**
     * Determine if given class can be instantiated (i.e. is concrete).
     * @return true if class is concrete, false if class is abstract
     * @param c The class to be tested
     */
    public static boolean isConcrete(Class<?> c) {
        return (c.getModifiers() & Modifier.ABSTRACT) != Modifier.ABSTRACT;
    }
}
