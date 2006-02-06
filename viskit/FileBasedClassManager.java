package viskit;

import javax.swing.*;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import viskit.xsd.bindings.eventgraph.*;

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

  private HashMap classMap, fileMap, parameterMap;

  private FileBasedClassManager()
  {
    classMap = new HashMap();
    fileMap = new HashMap();
    parameterMap = new HashMap();
    new Thread(this,"FileBasedClsMgr").start();
  }

  public void addFileClass(Class c)
  {
    classMap.put(c.getName(),c);
  }

  public void removeFileClass(Class c)
  {
    removeFileClass(c.getName());
  }
  private void removeFileClass(String nm)
  {
    classMap.remove(nm);
  }

  public Class getFileClass(String s)
  {
    return (Class)classMap.get(s);
  }

  public void unloadFile(FileBasedAssyNode fban)
  {
    removeFileClass(fban.loadedClass);
    fileMap.remove(fban.loadedClass);
  }

  public FileBasedAssyNode loadFile(File f) throws Throwable
  {
    FileBasedAssyNode fban = null;
    Class fclass = null;
    if (f.getName().toLowerCase().endsWith(".xml")) {
      PkgAndFile paf = AssemblyController.createTemporaryEventGraphClass(f);

      fclass = FindClassesForInterface.classFromFile(paf.f);   // Throwable from here possibly

      fban = new FileBasedAssyNode(paf.f,fclass.getName(),f,paf.pkg);
      
      // since we're here, cache the parameter names
      JAXBContext jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
      Unmarshaller um = jaxbCtx.createUnmarshaller();
      try {
          SimEntityType simEntity =  (SimEntityType) um.unmarshal(f);
          parameterMap.remove(fclass.getName());
          parameterMap.put(fclass.getName(),simEntity.getParameter());
          System.out.println("Put "+fclass.getName()+simEntity.getParameter());
      } catch (Exception e) {;}
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

  public List resolveParameters(String type) {
      return (List) (parameterMap.get(type));
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
