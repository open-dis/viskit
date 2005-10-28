package viskit;
import java.lang.reflect.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

/**
 * @version $Id$
 * @author  ahbuss
 */

public class FindClassesForInterface
{
  /**
   * Added by Mike Bailey
   * @param f Class file to read from
   * @param implementing
   * @return Class object
   */

  public static Class classFromFile(File f, Class implementing)
  {
    Class c = null;
    try {
      c = classFromFile(f);

      if(c.isInterface()  || !isConcrete(c))
        c = null;
      else if (implementing != null && !implementing.isAssignableFrom(c))
        c = null;
    }
    catch (Throwable t){}
    return c;
  }

  /**
   * Added by Mike Bailey.  Same test as above.
   * @param questionable
   * @param target
   */
  public static boolean matchClass(Class questionable, Class target)
  {
    return (!questionable.isInterface() && target.isAssignableFrom(questionable) && isConcrete(questionable));
  }
  /**
   * Simple method to try to load a .class file
   */
  public static Class classFromFile(File f) throws java.lang.Throwable
  {
    return new MyClassLoader().buildIt(f);
  }

  /**
   * Custom classloader in support of classFromFile
   */
  static class MyClassLoader extends ClassLoader
  {
    private File f;

    Class buildIt(File fil) throws java.lang.Throwable
    {
      f = fil;
      return loadClass(f.getName());
    }

    protected Class findClass(String name) throws ClassNotFoundException
    {
      byte[] buf = new byte[128 * 1024];        // todo make dynamically sized
      int num = 0;
      try {
        DataInputStream dis = new DataInputStream(new FileInputStream(f));
        num = dis.read(buf);
      }
/*
      catch (FileNotFoundException e) {
        e.printStackTrace();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
*/
      catch(Throwable thr) {
        throw new ClassNotFoundException(thr.getMessage());
      }
      return defineClass(null, buf, 0, num);
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
    public static List findClasses(JarFile jarFile, Class implementing) {
        ArrayList found = new ArrayList();
        URLClassLoader loader = null;
        try {
            loader = new URLClassLoader(new URL[] {new File(jarFile.getName()).toURL()});
        } catch (MalformedURLException e) {e.printStackTrace(System.err); }
        for (Enumeration entries = jarFile.entries(); entries.hasMoreElements(); ) {
            JarEntry nextEntry = (JarEntry) entries.nextElement();
            if (nextEntry.getName().startsWith("META")) { continue; }
            try {
                Class c = loader.loadClass(getClassName(nextEntry.getName()));
                if (c.isInterface()) { continue; }
                if (implementing.isAssignableFrom(c) && isConcrete(c)) {
                    found.add(c);
                }
            } catch (ClassNotFoundException e) {}

        }
        return found;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Throwable {
        String jarFileName = args.length > 0 ? args[0] : "/R:/Simkit/simkit.jar";
        JarFile jarFile = new JarFile(jarFileName);
        List list = findClasses(jarFile, simkit.BasicSimEntity.class);
        System.out.println(jarFile.getName());
        System.out.println("SimEntity:");
        for (int i = 0; i < list.size(); ++i) {
            System.out.println("\t" + list.get(i));
        }
        list = findClasses(jarFile, java.beans.PropertyChangeListener.class);
        System.out.println("PropertyChangeListener:");
        for (int i = 0; i < list.size(); ++i) {
            System.out.println("\t" + list.get(i));
        }

//        jarFile = new JarFile("R:\\Simkit\\simkit.jar");
        System.out.println("RandomVariates:");
        list = findClasses(jarFile, simkit.random.RandomVariate.class);
        for (int i = 0; i < list.size(); ++i) {
            System.out.println("\t" + list.get(i));
        }
        System.out.println("RandomNumbers:");
        list = findClasses(jarFile, simkit.random.RandomNumber.class);
        for (int i = 0; i < list.size(); ++i) {
            System.out.println("\t" + list.get(i));
        }

        if (true) return;
        ArrayList simEntities = new ArrayList();
        ArrayList propertyChangeListeners = new ArrayList();
        System.out.println(jarFile.getName());
        URLClassLoader loader = new URLClassLoader(new URL[] {new File(jarFile.getName()).toURL()});
        for (Enumeration entries = jarFile.entries(); entries.hasMoreElements(); ) {
            JarEntry nextEntry = (JarEntry) entries.nextElement();
            if (nextEntry.getName().startsWith("META")) { continue; }
//            System.out.println(getClassName(nextEntry.getName()));

            try {
                Class c = loader.loadClass(getClassName(nextEntry.getName()));
                if (c.isInterface()) { continue; }
//                System.out.println (c);
                if (java.beans.PropertyChangeListener.class.isAssignableFrom(c)  && isConcrete(c)) {
                    propertyChangeListeners.add(c);
//                    System.out.println("\tIs PropertyChangeListener!");
                }
                if (simkit.SimEntity.class.isAssignableFrom(c) && isConcrete(c) ) {
                    simEntities.add(c);
//                    System.out.println("\tIs SimEntity!");
                }
            } catch (Throwable t) {
//                System.out.println("\t" + nextEntry + " not loaded");
            }
        }
        System.out.println("SimEntities:");
        for (int i = 0; i < simEntities.size(); ++i) {
            System.out.println(simEntities.get(i));
        }
        System.out.println("PropertyChangeListeners:");
        for (int i = 0; i < propertyChangeListeners.size(); ++i) {
            System.out.println(propertyChangeListeners.get(i));
        }

        if (true) return;
        System.out.println(loader);
        Class c = loader.loadClass("png.PNGChunk");
        System.out.println(c);

        String ps = System.getProperty("path.separator");
        File file = new File("/C:/tmp/MiscTest/png/PNGData.class");
        System.out.println(file.getCanonicalPath() + "[" + file.exists() +"]");
        String fullyQualified = file.getAbsolutePath().substring(0,
        file.getAbsolutePath().lastIndexOf('.')).replaceAll("\\\\", ".");
        System.out.println(getClassName(fullyQualified));
        //        for (int i = 0; i < pieces.length; ++i) {
        //            System.out.println("\t" + pieces[i]);
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
     * Determine if given class can be instantiated (ie is concrete).
     * @return true if class is concrete, false if class is abstract
     * @param c The class to be tested
     */
    public static boolean isConcrete(Class c)  {
        return (c.getModifiers() & Modifier.ABSTRACT) != Modifier.ABSTRACT;
    }

}
