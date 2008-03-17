package viskit;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.swing.*;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import viskit.xsd.bindings.eventgraph.*;
import static edu.nps.util.GenericConversion.newListObjectTypeArray;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 23, 2004
 * @since 12:53:36 PM
 * @version $Id: FileBasedClassManager.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class FileBasedClassManager implements Runnable {
    // Singleton:
    private static FileBasedClassManager me;
    private static XMLConfiguration vConfig;
    private HashMap<String, FileBasedAssyNode> fileMap;
    private HashMap<String, Class<?>> classMap;
    static Logger log = Logger.getLogger(FileBasedClassManager.class);

    public static synchronized FileBasedClassManager inst() {
        if (me == null) {
            me = new FileBasedClassManager();
        }
        return me;
    }

    private FileBasedClassManager() {
        classMap = new HashMap<String, Class<?>>();
        fileMap = new HashMap<String, FileBasedAssyNode>();

        try {
            vConfig = VGlobals.instance().getHistoryConfig();
        } catch (Exception e) {
            log.debug("Error loading config file: " + e.getMessage());
            vConfig = null;
        }
    }

    public void addFileClass(Class<?> c) {
        classMap.put(c.getName(), c);
    }

    public void removeFileClass(Class c) {
        removeFileClass(c.getName());
    }

    private void removeFileClass(String nm) {
        classMap.remove(nm);
    }

    public Class getFileClass(String s) {
        return (Class) classMap.get(s);
    }

    public void unloadFile(FileBasedAssyNode fban) {
        removeFileClass(fban.loadedClass);
        fileMap.remove(fban.loadedClass);
    }

    public FileBasedAssyNode loadFile(File f) throws Throwable {
        FileBasedAssyNode fban = null;
        Class fclass = null;
        // if it is cached, cache directory exists and will be loaded on start
        if (f.getName().toLowerCase().endsWith(".xml")) {
            if (!isCached(f)) {
                if (!isCacheMiss(f)) {
                    PkgAndFile paf = AssemblyController.createTemporaryEventGraphClass(f);
                    
                    // Tried to compile an Assembly as an EventGraph, so just return here
                    if (paf == null) {
                        return null;
                    }
                    ClassLoader loader = VGlobals.instance().getWorkClassLoader(true);
                    // since we're here, cache the parameter names
                    JAXBContext jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
                    Unmarshaller um = jaxbCtx.createUnmarshaller();
                    try {
                        SimEntity simEntity = (SimEntity) um.unmarshal(f);
                        fclass = loader.loadClass(simEntity.getPackage() + "." + simEntity.getName());
                        fban = new FileBasedAssyNode(paf.f, fclass.getName(), f, paf.pkg);
                        List<Object>[] pa = newListObjectTypeArray(List.class, 1);
                        pa[0].addAll(simEntity.getParameter());
                        Vstatics.putParameterList(fclass.getName(), pa);

                        if (viskit.Vstatics.debug) {
                            log.debug("Put " + fclass.getName() + simEntity.getParameter());
                        }
                    } catch (Exception e) {
                        if (viskit.Vstatics.debug) {
                            e.printStackTrace();
                        }
                    }
                    addCache(f, fban.classFile);
                }
            } else {
                f = getCachedClass(f);
                File fXml = getCachedXML(f);
                ClassLoader loader = VGlobals.instance().getWorkClassLoader(true);
                JAXBContext jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
                Unmarshaller um = jaxbCtx.createUnmarshaller();
                try {
                    SimEntity simEntity = (SimEntity) um.unmarshal(fXml);
                    fclass = loader.loadClass(simEntity.getPackage() + "." + simEntity.getName());
                    fban = new FileBasedAssyNode(f, fclass.getName(), fXml, simEntity.getPackage());
                    List<Object>[] pa = newListObjectTypeArray(List.class, 1);
                    pa[0].addAll(simEntity.getParameter());
                    Vstatics.putParameterList(fclass.getName(), pa);

                    if (viskit.Vstatics.debug) {
                        log.debug("Put " + fclass.getName() + simEntity.getParameter());
                    }
                } catch (Exception e) {
                    if (viskit.Vstatics.debug) {
                        e.printStackTrace();
                    }
                }
                if (viskit.Vstatics.debug) {
                    log.debug(f + " loaded " + fclass.getPackage() + "." + fclass);
                }
            }
        } else if (f.getName().toLowerCase().endsWith(".class")) {
            fclass = FindClassesForInterface.classFromFile(f);   // Throwable from here possibly
            String pkg = fclass.getName().substring(0, fclass.getName().lastIndexOf("."));
            fban = new FileBasedAssyNode(f, fclass.getName(), pkg);
            Vstatics.putParameterList(fclass.getName(), listOfParamNames(fclass));
        } else {
            if (!f.getName().toLowerCase().endsWith(".java")) {
                throw new Exception("Unsupported file type.");
            }
        }
        if (fclass == null) {
            return (FileBasedAssyNode) null;
        }
        addFileClass(fclass);
        synchronized (fileMap) {
            fileMap.put(fclass.getName(), fban);
        }
        return fban;
    }

    public void addCache(File xmlEg, File classFile) {
        // isCached ( itself checks isStale, if so update and return cached false ) if so don't bother adding the same cache 
        if (isCached(xmlEg)) {
            return;
        }
        try {
            List<String> cache = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs" + "[@xml]"));
            if (viskit.Vstatics.debug) {
                if (cache == null) {
                    log.debug("cache " + cache);
                } else {
                    log.debug("cache size " + cache.size());
                }
            }
            if (cache.isEmpty()) {
                if (viskit.Vstatics.debug) {
                    log.debug("Cache is empty, creating workDir entry at " + VGlobals.instance().getWorkDirectory().getCanonicalPath());
                }
                vConfig.setProperty("Cached[@workDir]", VGlobals.instance().getWorkDirectory().getCanonicalPath());
            }
            if (viskit.Vstatics.debug) {
                log.debug("Adding cache " + xmlEg + " " + classFile);
            }
            vConfig.setProperty("Cached.EventGraphs(" + cache.size() + ")[@xml]", xmlEg.getCanonicalPath());
            vConfig.setProperty("Cached.EventGraphs(" + cache.size() + ")[@class]", classFile.getCanonicalPath());
            vConfig.setProperty("Cached.EventGraphs(" + cache.size() + ")[@digest]", createMessageDigest(xmlEg, classFile));
            // if used to miss, unmiss it
            removeCacheMiss(xmlEg);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** Creates an MD5 message digest composed of files, if either changes
     * there will be a mismatch in the new digest, so delete cache etc.
     * @param files
     * @return 
     */
    public String createMessageDigest(File... files) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            for (File file : files) {
                byte[] buf = new byte[(int) file.length()];
                try {
                    InputStream is = file.toURI().toURL().openStream();
                    is.read(buf);
                    md.update(buf);
                    is.close();
                } catch (MalformedURLException ex) {
                    ex.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            byte[] hash = md.digest();
            if (viskit.Vstatics.debug) {
                log.debug("hash " + new BigInteger(hash).toString(16) + " " + hash.length);
            }
            return new BigInteger(hash).toString(16);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public boolean isCached(File file) {
        List<String> cache = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs[@xml]"));
        try {
            if (viskit.Vstatics.debug) {
                log.debug("isCached() " + file + " of cacheSize " + cache.size());
                if (cache.contains(file.getCanonicalPath())) {
                    log.debug("cached true");
                } else {
                    log.debug("cached false");
                }
            }
            if (cache.contains(file.getCanonicalPath())) {
                if (isStale(file)) {
                    deleteCache(file);
                    return false;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } catch (IOException ex) {
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @param file 
     * @return a cached class file given its cached XML file */
    public File getCachedClass(File file) {
        List<String> cacheXML = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs[@xml]"));
        List<String> cacheClass = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs[@class]"));
        int index = 0;
        try {
            index = cacheXML.lastIndexOf(file.getCanonicalPath());
            if (viskit.Vstatics.debug) {
                log.debug("getCached index at " + index);
                log.debug("will return " + cacheClass.get(index));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        File cachedFile = new File(cacheClass.get(index));
        if (viskit.Vstatics.debug) {
            log.debug("cachedFile index at " + index);
            log.debug("will return " + cachedFile);
        }
        return cachedFile;
    }

    /**
     * @param file 
     * @return an XML file given its cached class file 
     */
    public File getCachedXML(File file) {
        List<String> cacheXML = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs[@xml]"));
        List<String> cacheClass = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs[@class]"));
        int index = 0;
        try {
            index = cacheClass.lastIndexOf(file.getCanonicalPath());
            if (viskit.Vstatics.debug) {
                log.debug("getCachedXml index at " + index);
                log.debug("will return " + cacheXML.get(index));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        File cachedFile = new File(cacheXML.get(index));
        if (viskit.Vstatics.debug) {
            log.debug("cachedFile index at " + index);
            log.debug("will return " + cachedFile);
        }
        return cachedFile;
    }

    // delete cache given either xml or class file
    public void deleteCache(File file) {
        List<String> cacheXML = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs" + "[@xml]"));
        List<String> cacheClass = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs" + "[@class]"));
        String filePath;
        File deletedCache = null;
        try {
            filePath = file.getCanonicalPath();

            int index = - 1;
            if (cacheXML.contains(filePath)) {
                index = cacheXML.lastIndexOf(filePath);
                deletedCache = new File(cacheClass.get(index));
            } else if (cacheClass.contains(file.getCanonicalPath())) {
                index = cacheClass.lastIndexOf(filePath);
                deletedCache = file;
            }
            if (index >= 0) {
                vConfig.clearProperty("Cached.EventGraphs(" + index + ")[@xml]");
                vConfig.clearProperty("Cached.EventGraphs(" + index + ")[@class]");
                vConfig.clearProperty("Cached.EventGraphs(" + index + ")[@digest]");
                boolean didDelete = deletedCache.delete();
                if (viskit.Vstatics.debug) {
                    log.debug(didDelete + ": cachedFile deleted index at " + index);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // check if digests match as well as being on list, if no digest match
    // file changed since being a miss, so do updates etc.
    public boolean isCacheMiss(File file) {
        List<String> cacheMisses = Arrays.asList(vConfig.getStringArray("Cached.Miss[@file]"));
        List<String> digests = Arrays.asList(vConfig.getStringArray("Cached.Miss[@digest]"));
        int index;
        try {
            index = cacheMisses.lastIndexOf(file.getCanonicalPath());
            if (index >= 0) {
                String digest = digests.get(index);
                String compare = createMessageDigest(file);
                if (digest.equals(compare)) {
                    return true;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public void addCacheMiss(File file) {
        deleteCache(file);
        removeCacheMiss(file); // remove any old ones
        int index = vConfig.getStringArray("Cached.Miss[@file]").length;
        try {
            vConfig.addProperty("Cached.Miss(" + index + ")[@file]", file.getCanonicalPath());
            vConfig.addProperty("Cached.Miss(" + index + ")[@digest]", createMessageDigest(file));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void removeCacheMiss(File file) {
        List<String> cacheMisses = Arrays.asList(vConfig.getStringArray("Cached.Miss[@file]"));
        int index;
        try {
            if ((index = cacheMisses.lastIndexOf(file.getCanonicalPath())) > -1) {
                vConfig.clearProperty("Cached.Miss(" + index + ")[@file]");
                vConfig.clearProperty("Cached.Miss(" + index + ")[@digest]");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    /** if either the egFile changed, or the classFile, the cache is stale
     * @param egFile
     * @return 
     */
    public boolean isStale(File egFile) {
        File classFile = getCachedClass(egFile);
        List<String> cacheXML = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs[@xml]"));
        List<String> cacheDigest = Arrays.asList(vConfig.getStringArray("Cached.EventGraphs[@digest]"));
        String filePath;
        try {
            filePath = egFile.getCanonicalPath();
            int index = -1;
            index = cacheDigest.lastIndexOf(filePath);
            if (index >= 0) {
                String cachedDigest = cacheDigest.get(index);
                String compareDigest = createMessageDigest(egFile, classFile);
                return !cachedDigest.equals(compareDigest);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        // if egFile not in cache, it can't be stale
        return false;
    }

    protected List<Object>[] listOfParamNames(Class<?> c) {
        ObjectFactory of = new ObjectFactory();
        Constructor[] constr = c.getConstructors();
        Annotation[] paramAnnots;
        List<Object>[] l = newListObjectTypeArray(ArrayList.class, constr.length);
        for (int j = 0; j < constr.length; j++) {
            Class[] clz = constr[j].getParameterTypes();
            paramAnnots = constr[j].getDeclaredAnnotations();
            if (paramAnnots == null) {
                l[j] = new ArrayList<Object>();
                for (int i = 0; i < clz.length; i++) {
                    String zName = clz[i].getName();
                    if (zName.indexOf(".class") > 0) {
                        zName = zName.split("\\.")[0];
                    }
                    Parameter p = of.createParameter();
                    p.setName(" ");
                    if (viskit.Vstatics.debug) {
                        log.debug("setting type " + zName);
                    }
                    p.setType(zName);
                    l[j].add(p);
                }
            } else {
                System.err.println("Enter Annotation");
                if (paramAnnots.length > 1) {
                    throw new RuntimeException("Only one Annotation per constructor");
                }
                l = newListObjectTypeArray(ArrayList.class, 1);
                ParameterMap param = constr[j].getAnnotation(viskit.ParameterMap.class);

                if (param != null) {
                    String[] names = param.names();
                    String[] types = param.types();
                    if (names.length != types.length) {
                        throw new RuntimeException("ParameterMap names and types length mismatch");
                    }
                    for (int i = 0; i < names.length; i++) {
                        Parameter p = of.createParameter();
                        p.setName(names[i]);
                        p.setType(types[i]);
                        l[0] = new ArrayList<Object>();
                        l[0].add(p);
                    }

                } else {
                    throw new RuntimeException("Only One ParameterMap Annotation used");
                }
            }
        }
        return l;
    }

    public Collection<FileBasedAssyNode> getFileLoadedClasses() {
        Collection<FileBasedAssyNode> c;
        synchronized (fileMap) {
            c = fileMap.values();
        }
        return c;
    }

    public File getFile(String className) {
        return fileMap.get(className).xmlSource;
    }

    public void run() {
        final Vector<String> v = new Vector<String>();

        while (true) { // forever
            v.clear();
            synchronized (fileMap) {
                for (FileBasedAssyNode fban : fileMap.values()) {
                    File f = fban.isXML ? fban.xmlSource : fban.classFile;
                    if (f.lastModified() != fban.lastModified) {
                        v.add(fban.loadedClass);
                        fban.lastModified = f.lastModified();
                    }

                    if (v.size() > 0) {
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                VGlobals.instance().getAssemblyModel().externalClassesChanged(v);
                            }
                        });
                    }
                }
            }
            // Goal: sleep for the max estimated between when a user edits and saves an
            // event graph, and when he switches back and tries to run his assembly
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
    }
}
