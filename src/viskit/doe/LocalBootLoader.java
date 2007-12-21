/*
 * LocalBootLoader.java
 *
 * Created on December 27, 2006, 11:47 AM
 *
 * LocalBootLoader is similar to Viskit's Vstatics.classForName and implememnts 
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
 * from the Assembly XML, since each replication, a new class definition
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
 * Each thread that uses a LocalBoot should set the contextClassLoader
 * to be its own LocalBoot's parent's parent; this should enable multiple threads to use
 * class methods in a unique context, eg. Schedule.reset();
 * 
 * In order to do that, as in create a separate context for Simkit per Thread,
 * without running an external Process, everything must be read into a 
 * ClassLoader that has the current Viskit running Thread's parent's 
 * contextClassLoader, above from where simkit.jar got loaded in, ie, the
 * stage prior to reading in the lib directory during JVM initialization. Then
 * each new ClassLoader so constructed can have a unique Simkit run
 * simultaneously. Furthermore, it can now have a unique Assembly per Thread
 * as originally expected.
 */

package viskit.doe;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
//import viskit.VGlobals;

/**
 *
 * @author Rick Goldberg
 */

public class LocalBootLoader extends URLClassLoader {
    String[] classPath;
    LocalBootLoader stage1;
    File workDir;
    URL[] extUrls;

    boolean allowAssembly = false;

    private boolean reloadSimkit = false;
    
    public LocalBootLoader(URL[] classes, ClassLoader parent, File workDir) {
        super(new URL[]{},parent);
        extUrls = classes;
        this.workDir = new File(workDir.toURI());
        //System.out.println("WorkDir: "+workDir);
    }
    
    /** Creates a new instance of LocalBootLoader */
    public void initStage1() { 
        String classPathProp = System.getProperty("java.class.path");
        String pathSep = System.getProperty("path.separator");
        classPath = classPathProp.split(pathSep);
        if(false)for (String line:classPath) {
            System.out.println("classpath: "+line);
        }
        ClassLoader parentClassLoader = getParent();
        //System.out.println("LocalBootLoader initStage1 reboot..."+workDir);
        
        stage1 = new LocalBootLoader( new URL[]{} , parentClassLoader, workDir );
        boolean loop=!allowAssembly;
        
        // if each LocalBootLoader individually has to read from
        // a file, then each instance of the loader will have its own
        // context in terms of static variables from the read-in classes, 
        // eg. simkit.Schedule.reset() in one thread will not reset another.
        // see sample case StaticsTest
        while(loop) {
            try {
                 if (reloadSimkit) {
                    stage1.loadClass("simkit.random.RandomVariate");
                } else {
                    stage1.loadClass("viskit.doe.LocalBootLoader");
                }
                //System.out.println("still found existing viskit context, going up one more...");
                parentClassLoader = parentClassLoader.getParent();
                stage1 = new LocalBootLoader( new URL[]{}, parentClassLoader, workDir );
            } catch ( Exception e ) { // should probably be class not found exception
                loop = false;
            }
        }
        for (String path:classPath) {
            try {
                // build up stage1 libs
                stage1.addURL( new File(path).toURI().toURL() );
                //System.out.println("Added "+ new File(path).toURL().toString() );
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            }
            stage1.classPath = classPath;
        }
        
    }

   
    public void doAddURL(URL u) {
        super.addURL(u);
    }

    public String[] getClassPath() {
        if (false) for (String line:classPath) {
            System.out.println(line);
        }
        return classPath;
    }
    
    public URL[] getExtUrls() {
        return extUrls;
    }
    
    protected void addURL(URL u) {
        super.addURL(u);
        //System.out.println("Adding url "+u);
    }
    
    public File getWorkDir() {
        return workDir;
    }
    
    public boolean getAllowAssemby() {
        return allowAssembly;
    }
    
    public void setAllowAssemby(boolean enable) {
        this.allowAssembly = enable;
    }
    
    public void setReloadSimkit(boolean reload) {
        this.reloadSimkit = reload;
    }
  
    // create a context with viskit's libs along with
    // the generated eventgraphs, takes two stages
    
    // the returned LocalBootLoader can be used
    // as an isolated context, ie, where static methods
    // and variables from one class don't interfere with
    // one another or between LocalBootLoader's, here
    // done by setting threads' contextClassLoaders to
    // their own LocalBootLoaders
    
    public LocalBootLoader init(boolean allowAssembly) {
        this.allowAssembly = allowAssembly;
        return init();
    }
    
    public LocalBootLoader init() {
        File jar;
        //System.out.println("Stage1 start init ");
        initStage1();
        stage1.allowAssembly = this.allowAssembly;
        jar = buildCleanWorkJar();
        
        //stage1 gets dirty during bring up of clean jar
        //reboot it with cleanWorkJar
        //System.out.println("Stage1 reinit ");
        initStage1();
        //System.out.println("Adding cleaned jar "+jar);
        for (URL ext:extUrls) {
            stage1.addURL(ext);
            String[] tmp = new String[classPath.length+1];
            System.arraycopy(classPath,0,tmp,0,classPath.length);
            try {
                tmp[tmp.length-1] = new File(ext.toURI()).getCanonicalPath();
                classPath = tmp;
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (URISyntaxException ex) {
                ex.printStackTrace();
            }
        }
        try {
            stage1.addURL(jar.toURI().toURL());
            String[] tmp = new String[classPath.length+1];
            System.arraycopy(classPath,0,tmp,0,classPath.length);
            try {
                tmp[tmp.length-1] = jar.getCanonicalPath();
                classPath = tmp;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
        stage1.classPath = classPath;
        return stage1;
    }
    
    private File buildCleanWorkJar() {
        File newJar=null;
        try {
            //File currentDir = VGlobals.instance().getWorkDirectory();
            File currentDir = workDir;
            File newDir = File.createTempFile("viskit","doe");
            newDir.delete();
            newDir.mkdir();
            newDir.deleteOnExit();
            // this potentially "dirties" this instance of stage1
            // meaning it could have Assembly classes in it
            stage1.addURL(currentDir.toURI().toURL());
            // make a clean version of the dir in jar form
            // to be added to a newer stage1 (rebooted) instance.
            newJar = makeJarFileFromDir(currentDir,newDir);
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        return newJar;
    }
     
    private File makeJarFileFromDir(File dir2jar,File destDir) {
        File jarOut = dir2jar;
        try {
            jarOut = File.createTempFile("eventGraphs",".jar",destDir);
            FileOutputStream fos = new FileOutputStream(jarOut);
            JarOutputStream jos = new JarOutputStream(fos);
            if ( dir2jar.isDirectory() ) {
                makeJarFileFromDir(dir2jar,dir2jar,jos);
            }
            jos.close();
            
        } 
        catch (java.util.zip.ZipException ze) {return dir2jar;} // could be first time through
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return jarOut;
    }
    
    private void makeJarFileFromDir(File baseDir, File newDir, JarOutputStream jos) {
        File[] dirList = newDir.listFiles();
        FileInputStream fis;
        JarEntry je;
        for ( int i = 0; i < dirList.length; i++ ) {
            if ( dirList[i].isDirectory() ) {
                makeJarFileFromDir(baseDir, dirList[i], jos);
            } else {
                
                String entryName=" ? ";
                String entryClass=entryName;
                try {
                    entryName = dirList[i].getCanonicalPath().substring(baseDir.getCanonicalPath().length() + 1);
                    //System.out.println("JarEntry started out as "+entryName);
                    // WINDOWS bug: jar entries are / not \, but String.replace doesn't seem to work as expected with \\
                    char[] entryChars = new char[entryName.length()];
                    entryName.getChars(0,entryChars.length,entryChars,0);
                    for (int ind = 0; ind < entryName.length(); ind++) {
                        if (entryChars[ind]==File.separatorChar) {
                            entryChars[ind] = '/'; // this should be redundant on non-windows
                        }
                    }
                    entryName = new String(entryChars);
                    //System.out.println("JarEntry name "+entryName);
                } catch (Exception ex) { 
                    ex.printStackTrace();
                }
                entryClass = entryName.replace('/','.');
                //System.out.println("Entry Class "+entryClass);
                if (entryClass.endsWith(".class")) { // else do nothing
                    
                    entryClass = entryClass.substring(0,entryClass.length() - 6);
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
                        
                        Class<?> clz = stage1.loadClass(entryClass);
                        Class<?> vzClz = stage1.loadClass("viskit.xsd.assembly.ViskitAssembly");
                        if (vzClz.isAssignableFrom(clz)) {
                            isEventGraph = false;
                            //System.out.println("caught Assembly: "+entryClass);
                        }
                    } catch (ClassNotFoundException ex) {
                        //ex.printStackTrace();
                    }
                    
                    if (isEventGraph|allowAssembly) try {
                        je = new JarEntry(entryName);
                        jos.putNextEntry(je);
                        fis = new FileInputStream(dirList[i]);
                        byte[] buf = new byte[256];
                        int c;
                        while ( (c = fis.read(buf)) > 0 ) {
                            jos.write(buf,0,c);
                        }
                        jos.closeEntry();
                    } catch (IOException ex) {
                        ex.printStackTrace();
           
                    }
                }
            }
        }
        
    }
   
}
