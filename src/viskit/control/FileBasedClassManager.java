package viskit.control;

import edu.nps.util.GenericConversion;
import edu.nps.util.Log4jUtilities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.math.BigInteger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;

import viskit.util.FileBasedAssemblyNode;
import viskit.util.FindClassesForInterface;

import viskit.ViskitGlobals;

import viskit.ViskitUserConfiguration;

import viskit.ViskitStatics;

import viskit.xsd.bindings.eventgraph.SimEntity;
import viskit.xsd.translator.eventgraph.SimkitEventGraphXML2Java;

/** A custom class manager to support finding Event Graphs and PCLs in *.class form vice
 * XML.  Used to populate the LEGOs tree on the Assembly Editor.
 *
 * <pre>
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * </pre>
 * @author Mike Bailey
 * @since Jul 23, 2004
 * @since 12:53:36 PM
 * @version $Id$
 */
public class FileBasedClassManager {

    static final Logger LOG = LogManager.getLogger();

    // Singleton:
    protected static FileBasedClassManager me;
    private static XMLConfiguration projectXMLConfiguration;
    private final Map<String, Class<?>> classMap;

    public static synchronized FileBasedClassManager instance() {
        if (me == null) {
            me = new FileBasedClassManager();
        }

        // This requires reinitializing everytime this FBM is called
        projectXMLConfiguration = ViskitUserConfiguration.instance().getProjectXMLConfiguration();
        return me;
    }

    private FileBasedClassManager() {
        classMap = Collections.synchronizedMap(new HashMap<>());
    }

    public void addFileClass(Class<?> c) {
        classMap.put(c.getName(), c);
    }

    public void removeFileClass(Class<?> c) {
        removeFileClass(c.getName());
    }

    private void removeFileClass(String nm) {
        classMap.remove(nm);
    }

    public Class<?> getFileClass(String s) {
        return classMap.get(s);
    }

    public void unloadFile(FileBasedAssemblyNode newFileBasedAssemblyNode) {
        removeFileClass(newFileBasedAssemblyNode.loadedClass);
    }
    FileBasedAssemblyNode fileBasedAssemblyNode = null;
    Class<?> fileClass = null;
    JAXBContext   jaxbContext = null;
    Unmarshaller unmarshaller = null;
    PackageAndFile packageAndFile = null;
    File xmlFile = null;
    SimEntity simEntity = null;

    /** Known path for EventGraph Compilation
     *
     * @param file an event graph to compile
     * @param implementsClass to test for extension of simkit.BasicSimEntity
     * @return a node tree for viewing in the Assembly Editor
     * @throws java.lang.Throwable for a problem finding a class
     */
    public FileBasedAssemblyNode loadFile(File file, Class<?> implementsClass) throws Throwable {

        // if it is cached, cacheXML directory exists and will be loaded on start
        if (file.getName().toLowerCase().endsWith(".xml"))
        {
            if (jaxbContext == null) // avoid JAXBException (perhaps due to concurrency)
                jaxbContext = JAXBContext.newInstance(SimkitEventGraphXML2Java.EVENT_GRAPH_BINDINGS);
            unmarshaller = jaxbContext.createUnmarshaller();

            // Did we cacheXML the EventGraph XML and Class?
            if (!isCached(file)) 
            {
                // Make sure it's not a Cached Miss
                if (!isCacheMiss(file)) 
                {
                    // This next step will compile first time found Event Graphs
                    packageAndFile = ((AssemblyControllerImpl) ViskitGlobals.instance().getActiveAssemblyController()).createTemporaryEventGraphClass(file);

                    // Compile fail of an EventGraph, so just return here
                    if (packageAndFile == null) {
                        return null;
                    }

                    // Reset this so that the correct FBAN gets created
                    xmlFile = null;
                    setFileBasedAssemblyNode(file);

                    // TODO: work situation where another build/classes gets added
                    // to the classpath as it won't readily be seen before the
                    // project's build/classes is. This causes ClassNotFoundExceptions
                    if (fileBasedAssemblyNode != null)
                        addCache(file, fileBasedAssemblyNode.classFile);
                    // else TODO
                }
                // It's cached
            } 
            else {
                file = getCachedClass(file);
                xmlFile = getCachedXML(file);
                setFileBasedAssemblyNode(file);
            }
        // Check, but don't cacheXML .class files
        } 
        else if (file.getName().toLowerCase().endsWith(".class")) {
            fileClass = FindClassesForInterface.classFromFile(file, implementsClass);   // Throwable from here possibly
            if (fileClass != null) 
            {
                String packageName = fileClass.getName().substring(0, fileClass.getName().lastIndexOf("."));
                fileBasedAssemblyNode = new FileBasedAssemblyNode(file, fileClass.getName(), packageName);

                // If we have an annotated ParameterMap, then cacheXML it. If not,
                // then treat the fileClass as something that belongs on the
                // extra classpath
                List<Object>[] parameterMapListArray = ViskitStatics.resolveParameters(fileClass);
                if (parameterMapListArray != null && parameterMapListArray.length > 0)
                    ViskitStatics.putParameterList(fileClass.getName(), parameterMapListArray);
            }
        // TODO: Check if this is really necessary and should be dealt with upstream
        } 
        else if (!file.getName().toLowerCase().endsWith(".java")) 
        {
            LOG.warn("Unsupported file type: {}", file);
            return null;
        }
        if (fileClass != null) 
        {
            addFileClass(fileClass);
        } 
        else {
            fileBasedAssemblyNode = null;
        }
        return fileBasedAssemblyNode;
    }

    private void setFileBasedAssemblyNode(File file) 
    {
        ClassLoader loader = ViskitGlobals.instance().getViskitApplicationClassLoader();

        // since we're here, cacheXML the parameter names
        try {
            simEntity = (xmlFile == null) ? (SimEntity) unmarshaller.unmarshal(file) : (SimEntity) unmarshaller.unmarshal(xmlFile);

            // NOTE: If the project's build directory got nuked and we have
            // cached our Event Graphs and classes with MD5 hash, we'll throw a
            // ClassNotFoundException.
            // TODO: Check for this and recompile the Event Graphs before loading their classes
            fileClass = loader.loadClass(simEntity.getPackage() + "." + simEntity.getName());

            fileBasedAssemblyNode =  (xmlFile == null) ?
                new FileBasedAssemblyNode(packageAndFile.file, fileClass.getName(), file, packageAndFile.packageName) :
                new FileBasedAssemblyNode(file, fileClass.getName(), xmlFile, simEntity.getPackage());

            List<Object>[] parameterArray = GenericConversion.newListObjectTypeArray(List.class, 1);
            parameterArray[0].addAll(simEntity.getParameter());
            ViskitStatics.putParameterList(fileClass.getName(), parameterArray);

            LOG.debug("Put " + fileClass.getName() + simEntity.getParameter());
        } 
        catch (JAXBException | ClassNotFoundException | NoClassDefFoundError | NullPointerException e) {
            LOG.error("setFileBasedAssemblyNode({}) error: \n{}", file, e);
            e.printStackTrace();; // debug
        }
    }

    /**
     * Cache the Event Graph and its .class file with good MD5 hash
     * @param xmlEventGraphFile the Event Graph to cacheXML
     * @param classFile the compiled version of this Event Graph
     */
    public void addCache(File xmlEventGraphFile, File classFile) 
    {
        // isCached ( itself checks isStale, if so update and return cached false ) if so don't bother adding the same cacheXML
        if (isCached(xmlEventGraphFile)) {
            return;
        }
        try {
            List<String> cache = Arrays.asList(ViskitUserConfiguration.instance().getConfigurationValues(ViskitUserConfiguration.CACHED_EVENTGRAPHS_KEY));
            if (viskit.ViskitStatics.debug) {
                if (cache == null) {
                    LOG.debug("cache " + cache);
                } else {
                    LOG.debug("cache size " + cache.size());
                }
            }

            // TODO: Not used right now, but may be useful for other build/classes paths
//            if (cacheXML.isEmpty()) {
//                String s = ViskitGlobals.instance().getProjectWorkingDirectory().getCanonicalPath().replaceAll("\\\\", "/");
//                if (viskit.ViskitStatics.debug) {
//                    LOG.debug("Cache is empty, creating workDir entry at " + s);
//                }
//                projectXMLConfiguration.setProperty(ViskitUserConfiguration.CACHED_WORKING_DIR_KEY, s);
//            }
            if (viskit.ViskitStatics.debug) {
                LOG.debug("Adding cache " + xmlEventGraphFile + " " + classFile);
            }

            if (cache != null) {
                projectXMLConfiguration.setProperty("Cached.EventGraphs(" + cache.size() + ")[@xml]", xmlEventGraphFile.getCanonicalPath().replaceAll("\\\\", "/"));
                projectXMLConfiguration.setProperty("Cached.EventGraphs(" + cache.size() + ")[@class]", classFile.getCanonicalPath().replaceAll("\\\\", "/"));
                projectXMLConfiguration.setProperty("Cached.EventGraphs(" + cache.size() + ")[@digest]", createMessageDigest(xmlEventGraphFile, classFile));
            }
            // if used to miss, unmiss it
            removeCacheMiss(xmlEventGraphFile);
        } 
        catch (IOException ex) {
            LOG.error(ex);
            ex.printStackTrace(System.err);
        }
    }

    /** Creates an MD5 message digest composed of file contents.  If contents
     * change there will be a mismatch in the new digest, so delete cacheXML
     * etc.
     * @param files the varargs containing files to evaluate
     * @return a String representation of the message digest
     */
    public String createMessageDigest(File... files) {

        String retVal = "";

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buf;
            for (File file : files) {
                buf = new byte[(int) file.length()];
                try {
                    try (InputStream is = file.toURI().toURL().openStream()) {
                        is.read(buf);
                        md.update(buf);
                    }
                } catch (IOException ex) {

                    // This can happen if the build/classes directory got nuked
                    // so, ignore
//                    LOG.error(ex);
//                    ex.printStackTrace();
                }
            }
            byte[] hash = md.digest();
            if (viskit.ViskitStatics.debug) {
                LOG.debug("hash " + new BigInteger(hash).toString(16) + " " + hash.length);
            }
            retVal = new BigInteger(hash).toString(16);
        } catch (NoSuchAlgorithmException ex) {
            LOG.error(ex);
//            ex.printStackTrace();
        }
        return retVal;
    }

    public boolean isCached(File file) {
        List<String> cacheXML = Arrays.asList(ViskitUserConfiguration.instance().getConfigurationValues(ViskitUserConfiguration.CACHED_EVENTGRAPHS_KEY));
        try {
            String filePath = file.getCanonicalPath().replaceAll("\\\\", "/");
            LOG.debug("isCached() " + file + " of cacheSize " + cacheXML.size());
            LOG.debug("chached " + cacheXML.contains(filePath));
            if (cacheXML.contains(filePath)) {
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
            LOG.error(ex);
//            ex.printStackTrace();
            return false;
        }
    }

    /**
     * @param file XML file cached with its class file
     * @return a cached class file given its cached XML file
     */
    public File getCachedClass(File file) {
        List<String> cacheXML = Arrays.asList(ViskitUserConfiguration.instance().getConfigurationValues(ViskitUserConfiguration.CACHED_EVENTGRAPHS_KEY));
        List<String> cacheClass = Arrays.asList(ViskitUserConfiguration.instance().getConfigurationValues(ViskitUserConfiguration.CACHED_EVENTGRAPHS_CLASS_KEY));
        int index = 0;
        try {
            index = cacheXML.lastIndexOf(file.getCanonicalPath().replaceAll("\\\\", "/"));
            if (viskit.ViskitStatics.debug) {
                LOG.debug("getCached index at {}", index);
                LOG.debug("will return {}", cacheClass.get(index));
            }
        } catch (IOException ex) {
            LOG.error(ex);
//            ex.printStackTrace();
        }
        File cachedFile = new File(cacheClass.get(index));
        if (viskit.ViskitStatics.debug) {
            LOG.debug("cachedFile index at {}", index);
            LOG.debug("will return {}", cachedFile);
        }
        return cachedFile;
    }

    /**
     * @param file cached compiled class file of XML file
     * @return an XML file given its cached class file
     */
    public File getCachedXML(File file) {
        List<String> cacheXML = Arrays.asList(ViskitUserConfiguration.instance().getConfigurationValues(ViskitUserConfiguration.CACHED_EVENTGRAPHS_KEY));
        List<String> cacheClass = Arrays.asList(ViskitUserConfiguration.instance().getConfigurationValues(ViskitUserConfiguration.CACHED_EVENTGRAPHS_CLASS_KEY));
        int index = 0;
        try {
            index = cacheClass.lastIndexOf(file.getCanonicalPath().replaceAll("\\\\", "/"));
            if (viskit.ViskitStatics.debug) {
                LOG.debug("getCachedXml index at " + index);
                LOG.debug("will return " + cacheXML.get(index));
            }
        } catch (IOException ex) {
            LOG.error(ex);
//            ex.printStackTrace();
        }
        File cachedFile = new File(cacheXML.get(index));
        if (viskit.ViskitStatics.debug) {
            LOG.debug("cachedFile index at " + index);
            LOG.debug("will return " + cachedFile);
        }
        return cachedFile;
    }

    /** Delete cacheXML given either xml or class file
     * @param file the XML, or class file to delete from the cacheXML
     */
    public void deleteCache(File file) {
        List<String> cacheXML = Arrays.asList(ViskitUserConfiguration.instance().getConfigurationValues(ViskitUserConfiguration.CACHED_EVENTGRAPHS_KEY));
        List<String> cacheClass = Arrays.asList(ViskitUserConfiguration.instance().getConfigurationValues(ViskitUserConfiguration.CACHED_EVENTGRAPHS_CLASS_KEY));
        String filePath;
        File deletedCache = null;
        try {
            filePath = file.getCanonicalPath().replaceAll("\\\\", "/");

            int index = - 1;
            if (cacheXML.contains(filePath)) {
                index = cacheXML.lastIndexOf(filePath);
                deletedCache = new File(cacheClass.get(index));
            } else if (cacheClass.contains(filePath)) {
                index = cacheClass.lastIndexOf(filePath);
                deletedCache = file;
            }
            if (index >= 0) {
                projectXMLConfiguration.clearProperty("Cached.EventGraphs(" + index + ")[@xml]");
                projectXMLConfiguration.clearProperty("Cached.EventGraphs(" + index + ")[@class]");
                projectXMLConfiguration.clearProperty("Cached.EventGraphs(" + index + ")[@digest]");

                boolean didDelete = false;
                if (deletedCache != null)
                    didDelete = deletedCache.delete();
                if (viskit.ViskitStatics.debug) {
                    LOG.debug(didDelete + ": cachedFile deleted index at " + index);
                }
            }
        } catch (IOException ex) {
            LOG.error(ex);
//            ex.printStackTrace();
        }
    }

    /** Check if digests match as well as being on list, if no digest match
     * file changed since being a miss, so do updates etc.
     * @param file the file to evaluate
     * @return an indication of miss caching
     */
    public boolean isCacheMiss(File file) {
        List<String> cacheMisses = Arrays.asList(ViskitUserConfiguration.instance().getConfigurationValues(ViskitUserConfiguration.CACHED_MISS_FILE_KEY));
        List<String> digests = Arrays.asList(ViskitUserConfiguration.instance().getConfigurationValues(ViskitUserConfiguration.CACHED_MISS_DIGEST_KEY));
        int index;
        try {
            index = cacheMisses.lastIndexOf(file.getCanonicalPath().replaceAll("\\\\", "/"));
            if (index >= 0) {
                String digest = digests.get(index);
                String compare = createMessageDigest(file);
                return digest.equals(compare);
            }
        } catch (IOException ex) {
            LOG.error(ex);
//            ex.printStackTrace();
        }
        return false;
    }

    public void addCacheMiss(File file) {
        deleteCache(file);
        removeCacheMiss(file); // remove any old ones
        int index = ViskitUserConfiguration.instance().getConfigurationValues(ViskitUserConfiguration.CACHED_MISS_FILE_KEY).length;
        try {
            projectXMLConfiguration.addProperty("Cached.Miss(" + index + ")[@file]", file.getCanonicalPath().replaceAll("\\\\", "/"));
            projectXMLConfiguration.addProperty("Cached.Miss(" + index + ")[@digest]", createMessageDigest(file));
        } catch (IOException ex) {
            LOG.error(ex);
//            ex.printStackTrace();
        }
    }

    public void removeCacheMiss(File file) {
        List<String> cacheMisses = Arrays.asList(ViskitUserConfiguration.instance().getConfigurationValues(ViskitUserConfiguration.CACHED_MISS_FILE_KEY));
        int index;
        try {
            if ((index = cacheMisses.lastIndexOf(file.getCanonicalPath().replaceAll("\\\\", "/"))) > -1) {
                projectXMLConfiguration.clearProperty("Cached.Miss(" + index + ")[@file]");
                projectXMLConfiguration.clearProperty("Cached.Miss(" + index + ")[@digest]");
            }
        } catch (IOException ex) {
            LOG.error(ex);
//            ex.printStackTrace();
        }
    }

    /** If either the eventGraphFile changed, or the classFile, the cacheXML is stale
     * @param eventGraphFile the EventGraph file to compare digests with
     * @return an indication Event Graph state change
     */
    public boolean isStale(File eventGraphFile) {
        File classFile = getCachedClass(eventGraphFile);
        List<String> cacheDigest = Arrays.asList(ViskitUserConfiguration.instance().getConfigurationValues(ViskitUserConfiguration.CACHED_DIGEST_KEY));
        List<String> cacheXML = Arrays.asList(ViskitUserConfiguration.instance().getConfigurationValues(ViskitUserConfiguration.CACHED_EVENTGRAPHS_KEY));
        String filePath;
        try {
            filePath = eventGraphFile.getCanonicalPath().replaceAll("\\\\", "/");
            int index = cacheXML.lastIndexOf(filePath);
            if (index >= 0) {
                String cachedDigest = cacheDigest.get(index);
                String compareDigest = createMessageDigest(eventGraphFile, classFile);
                return !cachedDigest.equals(compareDigest);
            }
        } catch (IOException ex) {
            LOG.error(ex);
//            ex.printStackTrace();
        }
        // if eventGraphFile not in cacheXML, it can't be stale
        return false;
    }

} // end class FileBasedClassManager
