package viskit;

import viskit.xsd.translator.SimkitXML2Java;
import viskit.model.ViskitAssemblyModel;
import viskit.model.AssemblyModel;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jul 23, 2004
 * Time: 12:53:36 PM
 */

public class FileBasedClassManager implements Runnable
{
  // Singleton:
  private static FileBasedClassManager me;
  public static synchronized FileBasedClassManager inst()
  {
    if(me == null)
      me = new FileBasedClassManager();
    return me;
  }

  private HashMap classMap, fileMap;

  private FileBasedClassManager()
  {
    classMap = new HashMap();
    fileMap = new HashMap();
    new Thread(this).start();
  }

  public void addFileClass(Class c)
  {
    classMap.put(c.getName(),c);
  }

  public void removeFileClass(Class c)
  {
    classMap.remove(c.getName());
  }

  public Class getFileClass(String s)
  {
    return (Class)classMap.get(s);
  }

  public FileBasedAssyNode loadFile(File f) throws Throwable
  {
    FileBasedAssyNode fban = null;
    Class fclass = null;
    if (f.getName().toLowerCase().endsWith(".xml")) {
      PkgAndFile paf = AssemblyController.createTemporaryEventGraphClass(f);

      fclass = FindClassesForInterface.classFromFile(paf.f);   // Throwable from here possibly

      fban = new FileBasedAssyNode(paf.f,fclass.getName(),f,paf.pkg);
    }
    else if (f.getName().toLowerCase().endsWith(".class")) {
      fclass = FindClassesForInterface.classFromFile(f);   // Throwable from here possibly
      fban = new FileBasedAssyNode(f,fclass.getName(),fclass.getPackage().getName());
    }
    else {
      throw new Exception ("Unsupported file type.");
    }
    addFileClass(fclass);
    synchronized (fileMap) {
      fileMap.put(fclass.getName(),fban);
    }
    return fban;
  }

  public Collection getFileLoadedClasses()
  {
    Collection c;
    synchronized (fileMap) {
      c = fileMap.values();
    }
    return c;
  }

  public void run()
  {
    final Vector v = new Vector();

    while(true) { // forever
      v.clear();
      synchronized (fileMap) {
        for (Iterator itr = fileMap.values().iterator(); itr.hasNext();) {
          FileBasedAssyNode fban = (FileBasedAssyNode)itr.next();
          File f = fban.isXML ? fban.xmlSource : fban.classFile;
          if(f.lastModified() != fban.lastModified) {
            v.add(fban.loadedClass);
            fban.lastModified = f.lastModified();
          }

          if(v.size() > 0) {
            SwingUtilities.invokeLater(new Runnable()
            {
              public void run()
              {
                VGlobals.instance().getAssemblyModel().externalClassesChanged(v);
              }
            });
          }
        }
      }
      // Goal: sleep for the max estimated between when a user edits and saves an
      // event graph, and when he switches back and tries to run his assembly
      try {Thread.sleep(5000);}catch (InterruptedException e) {}
    }
  }
}
