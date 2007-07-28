package viskit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import javax.swing.*;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import viskit.doe.LocalBootLoader;
import viskit.xsd.bindings.eventgraph.*;
import org.apache.commons.configuration.XMLConfiguration;

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
  private static XMLConfiguration vConfig;
  private HashMap classMap, fileMap;
  static HashMap parameterMap;

  private FileBasedClassManager()
  {
    classMap = new HashMap();
    fileMap = new HashMap();
    parameterMap = new HashMap();
    new Thread(this,"FileBasedClsMgr").start();
    
    try {
      vConfig = VGlobals.instance().getHistoryConfig();
    }
    catch (Exception e) {
      System.out.println("Error loading config file: "+e.getMessage());
      vConfig = null;
    }
  
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

  public FileBasedAssyNode loadFile(File f) throws Throwable {
      FileBasedAssyNode fban = null;
      Class fclass = null;
      // if it is cached, cache directory exists and will be loaded on start
      if (f.getName().toLowerCase().endsWith(".xml")) {
          if (!isCached(f)) {
              PkgAndFile paf = AssemblyController.createTemporaryEventGraphClass(f);
              ClassLoader loader = VGlobals.instance().getWorkClassLoader(true);
              
              // since we're here, cache the parameter names
              JAXBContext jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
              Unmarshaller um = jaxbCtx.createUnmarshaller();
              try {
                  SimEntityType simEntity =  (SimEntityType) um.unmarshal(f);
                  fclass = loader.loadClass(simEntity.getPackage()+"."+simEntity.getName());
                  fban = new FileBasedAssyNode(paf.f,fclass.getName(),f,paf.pkg);
                  List[] pa = new List[] { simEntity.getParameter() };
                  Vstatics.putParameterList(fclass.getName(),pa);
                  if (viskit.Vstatics.debug) System.out.println("Put "+fclass.getName()+simEntity.getParameter());
              } catch (Exception e) { if (viskit.Vstatics.debug) e.printStackTrace(); }
              
              addCache(f,fban.classFile);
              
          } else {
              f = getCached(f);
              File fXml = getCachedXML(f);
              ClassLoader loader = VGlobals.instance().getWorkClassLoader(true);
              JAXBContext jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
              Unmarshaller um = jaxbCtx.createUnmarshaller();
              try {
                  SimEntityType simEntity =  (SimEntityType) um.unmarshal(fXml);
                  fclass = loader.loadClass(simEntity.getPackage()+"."+simEntity.getName());
                  fban = new FileBasedAssyNode(f,fclass.getName(),fXml,simEntity.getPackage());
                  List[] pa = new List[] { simEntity.getParameter() };
                  Vstatics.putParameterList(fclass.getName(),pa);
                  if (viskit.Vstatics.debug) System.out.println("Put "+fclass.getName()+simEntity.getParameter());
              } catch (Exception e) { if (viskit.Vstatics.debug) e.printStackTrace(); }
              
              
              
              if (viskit.Vstatics.debug) System.out.println(f+" loaded "+fclass.getPackage()+"."+fclass);
              //fban = new FileBasedAssyNode(f,fclass.getName(),fclass.getPackage().getName());
          
              //Vstatics.putParameterList(fclass.getName(), listOfParamNames(fclass));
              //loadFile( getCached(f) );
          }
      } else if (f.getName().toLowerCase().endsWith(".class")) {
          fclass = FindClassesForInterface.classFromFile(f);   // Throwable from here possibly
          fban = new FileBasedAssyNode(f,fclass.getName(),fclass.getPackage().getName());
          
          Vstatics.putParameterList(fclass.getName(), listOfParamNames(fclass));
      } else {
          throw new Exception("Unsupported file type.");
      }
      addFileClass(fclass);
      synchronized (fileMap) {
          fileMap.put(fclass.getName(),fban);
      }
      return fban;
  }
  
  private void addCache(File xmlEg,File classFile) {
      try {
          List<String> cache = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs"+"[@xml]"));
          if (viskit.Vstatics.debug) {
              if(cache == null)
                  System.out.println("cache "+cache);
              else 
                  System.out.println("cache size "+cache.size());
          }
          if ( cache.isEmpty() ) {
              if (viskit.Vstatics.debug) { 
                  System.out.println("Cache is empty, creating workDir entry at "+VGlobals.instance().getWorkDirectory().getCanonicalPath());
              }
              vConfig.setProperty("Cached[@workDir]",VGlobals.instance().getWorkDirectory().getCanonicalPath());
          }
          
          vConfig.setProperty("Cached.EventGraphs("+cache.size()+")[@xml]",xmlEg.getCanonicalPath());
          vConfig.setProperty("Cached.EventGraphs("+cache.size()+")[@class]",classFile.getCanonicalPath());
      } catch (IOException ex) {
          ex.printStackTrace();
      }
  }
  
  private boolean isCached(File file) {
      List<String> cache = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs"+"[@xml]"));
      
      try {
          if (viskit.Vstatics.debug) {
              System.out.println("isCached() "+file+" of cacheSize "+cache.size());
              if (cache.contains(file.getCanonicalPath())) {
                  System.out.println("cached true");
              } else {
                  System.out.println("cached false");
              }
          }
          
          return cache.contains(file.getCanonicalPath());
      } catch (IOException ex) {
          return false;
      } catch (Exception e) {
          e.printStackTrace();
          return false;
      }
  }

  // return cached class file given its cached XML file
  private File getCached(File file) {
      List<String> cacheXML = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs"+"[@xml]"));
      List<String> cacheClass = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs"+"[@class]"));
      int index = 0;
      try {
          index = cacheXML.lastIndexOf(file.getCanonicalPath());
          if ( viskit.Vstatics.debug ) {
              System.out.println("getCached index at "+index);
              System.out.println("will return "+cacheClass.get(index));
          }
      } catch (Exception ex) {
          ex.printStackTrace();
      }
      File cachedFile = new File(cacheClass.get(index));
      if ( viskit.Vstatics.debug ) {
              System.out.println("cachedFile index at "+index);
              System.out.println("will return "+cachedFile);
      }
      return cachedFile;
  }
  
  // return XML file given its cached class file
  private File getCachedXML(File file) {
      List<String> cacheXML = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs"+"[@xml]"));
      List<String> cacheClass = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs"+"[@class]"));
      int index = 0;
      try {
          index = cacheClass.lastIndexOf(file.getCanonicalPath());
          if ( viskit.Vstatics.debug ) {
              System.out.println("getCachedXml index at "+index);
              System.out.println("will return "+cacheXML.get(index));
          }
      } catch (Exception ex) {
          ex.printStackTrace();
      }
      File cachedFile = new File(cacheXML.get(index));
      if ( viskit.Vstatics.debug ) {
              System.out.println("cachedFile index at "+index);
              System.out.println("will return "+cachedFile);
      }
      return cachedFile;
  }

  
  
  protected List[] listOfParamNames(Class c) {
      ObjectFactory of = new ObjectFactory();
      Constructor[] constr = c.getConstructors();
      Annotation[] paramAnnots;
      ArrayList[] l = new ArrayList[constr.length];
      for ( int j = 0; j < constr.length; j++ ) {
          Class[] clz = constr[j].getParameterTypes();
          paramAnnots = constr[j].getDeclaredAnnotations();
          if (paramAnnots == null) {
              l[j] = new ArrayList();
              for (int i = 0; i < clz.length; i++) {
                  try {
                      String zName = clz[i].getName();
                      if (zName.indexOf(".class")>0) {
                          zName = zName.split("\\.")[0];
                      }
                      ParameterType p;
                      p = of.createParameter();
                      p.setName(" ");
                      if (viskit.Vstatics.debug) System.out.println("setting type "+zName);
                      p.setType(zName);
                      l[j].add(p);
                  } catch (javax.xml.bind.JAXBException e) {
                      ;
                  }
              }
          } else {
              System.err.println("Enter Annotation");
              if (paramAnnots.length > 1) {
                  throw new RuntimeException("Only one Annotation per constructor");
              }
              l = new ArrayList[1];
              ParameterMap param = constr[j].getAnnotation(viskit.ParameterMap.class);

              if ( param != null ) {
                  String[] names = ((ParameterMap)param).names();
                  String[] types = ((ParameterMap)param).types();
                  if (names.length != types.length) throw new RuntimeException("ParameterMap names and types length mismatch");
                  for ( int i = 0; i < names.length; i ++) {
                        ParameterType p;
                        try {
                            p = of.createParameter();
                            p.setName(names[i]);
                            p.setType(types[i]);
                            l[0].add(p);
                        } catch (JAXBException ex) {
                            ex.printStackTrace();
                        }
                  }
                  
              } else throw new RuntimeException("Only One ParameterMap Annotation used");

              
          }
      }
      return l;
  }
  
  public Collection getFileLoadedClasses()
  {
    Collection c;
    synchronized (fileMap) {
      c = fileMap.values();
    }
    return c;
  }
  
  public File getFile(String className) {
      return ((FileBasedAssyNode) (fileMap.get(className))).xmlSource;
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
