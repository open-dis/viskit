/*
 * LocalDriverImpl.java
 *
 * Created on January 8, 2007, 2:17 PM
 *
 * Implements a Local Doe Driver, to be interchangeable with the
 * Remote (Grid Engine) Driver
 */

package viskit.doe;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;
import viskit.xsd.assembly.GridRunner;

/**
 *
 * @author Rick Goldberg
 */
public class LocalDriverImpl implements DoeRunDriver {
    
    LocalBootLoader loader;
    Object runner;
    Class gridRunnerz;
    Hashtable methods;
    
    /** Creates a new instance of LocalDriverImpl */
    public LocalDriverImpl() { // remove if needed in gridkit.jar
        loader = new LocalBootLoader(new URL[]{}, Thread.currentThread().getContextClassLoader(), viskit.VGlobals.instance().getWorkDirectory());
        initGridRunner(loader);
    }
    
    public LocalDriverImpl(URL[] extClassPaths, File workDir) {
        loader = new LocalBootLoader(extClassPaths, Thread.currentThread().getContextClassLoader(), workDir);
        initGridRunner(loader);
    }
    
    void initGridRunner(LocalBootLoader loader) {
        loader = loader.init();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            gridRunnerz = loader.loadClass("viskit.xsd.assembly.GridRunner");
            try {
                
                Constructor constr = gridRunnerz.getConstructor(new Class[]{});
                runner = constr.newInstance(new Object[]{});
                Method[] mthds = gridRunnerz.getMethods();
                methods = new Hashtable();
                for (Method m:mthds) {
                    methods.put(m.getName(),m);
                }
                //runner = (GridRunner) constr.newInstance(new Object[]{});
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            } catch (InstantiationException ex) {
                ex.printStackTrace();
            } catch (InvocationTargetException ex) {
                ex.printStackTrace();
            }
        } catch (SecurityException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
            
            
            
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }
    
    public void clear() throws DoeException {
        try {
            Method clear = gridRunnerz.getMethod("clear",new Class[]{});
            clear.invoke(runner,new Object[]{});
        } catch (Exception ex) {
            throw new DoeException(ex.getMessage());
        }
        //runner.clear();
    }
    
    public int flushQueue() throws DoeException {
        try {
            Method flushQueue = gridRunnerz.getMethod("flushQueue", new Class[]{});
            return (Integer) flushQueue.invoke(runner,(Object[])null);
        } catch (Exception ex) {
            throw new DoeException(ex.getMessage());
        }
        //return runner.flushQueue();
    }
    
    public int getDesignPointCount() throws DoeException {
        try {
            return (Integer) ((Method)methods.get("getDesignPointCount")).invoke(runner,new Object[]{});
        } catch (Exception ex) {
            throw new DoeException(ex.getMessage());
        }
        //return runner.getDesignPointCount();
    }
    
    public synchronized Hashtable getDesignPointStats(int sampleIndex, int designPtIndex) throws DoeException {
        try {
            return (Hashtable) ((Method)methods.get("getDesignPointStats")).invoke(runner,sampleIndex,designPtIndex);
        } catch (Exception ex) {
            throw new DoeException(ex.getMessage());
        }
        //return runner.getDesignPointStats(sampleIndex,designPtIndex);
    }
    
    public int getRemainingTasks() throws DoeException {
        try {
            return (Integer) ((Method)methods.get("getRemainingTasks")).invoke(runner,new Object[]{});
            
        } catch (Exception ex) {
            throw new DoeException(ex.getMessage());
        }
        //return runner.getRemainingTasks();
    }
    
    public synchronized Hashtable getReplicationStats(int sampleIndex, int designPtIndex, int replicationIndex) throws DoeException {
        try {
            return (Hashtable) ((Method)methods.get("getReplicationStats")).invoke(runner,sampleIndex,designPtIndex,replicationIndex);
        } catch (Exception ex) {
            throw new DoeException(ex.getMessage());
        }
        //return runner.getReplicationStats(sampleIndex,designPtIndex,replicationIndex);
        
    }
    
    public synchronized String getResult(int sampleIndex, int designPtIndex) throws DoeException {
        try {
            return (String) ((Method)methods.get("getResult")).invoke(runner,sampleIndex,designPtIndex);
        } catch (Exception ex) {
            throw new DoeException(ex.getMessage());
        }
        //return runner.getResult(sample,designPt);
    }
    
    public synchronized String getResultByTaskID(int taskID) throws DoeException {
        try {
            return (String) ((Method)methods.get("getResultByTaskID")).invoke(runner,taskID);
        } catch (Exception ex) {
            throw new DoeException(ex.getMessage());
        }
        //return runner.getResultByTaskID(taskID);
    }
    
    public synchronized Vector getTaskQueue() throws DoeException {
        try {
            return (Vector) ((Method)methods.get("getTaskQueue")).invoke(runner,new Object[]{});
        } catch (Exception ex) {
            throw new DoeException(ex.getMessage());
        }
        //return runner.getTaskQueue();
    }
    
    public String qstat() throws DoeException {
        
        return "See Task Queue";
    }
    
    public String qstatXML() throws DoeException {
        return "<!--See Task Queue-->";
    }
    
    public void removeIndexedTask(int sampleIndex, int designPtIndex) throws DoeException {
        try {
            ((Method)methods.get("removeIndexedTask")).invoke(runner,sampleIndex,designPtIndex);
        } catch (Exception ex) {
            throw new DoeException(ex.getMessage());
        }
        //runner.removeIndexedTask(sampleIndex,designPtIndex);
    }
    
    public void removeTask(int jobID, int taskID) throws DoeException {
        try {
            ((Method)methods.get("removeTask")).invoke(runner,jobID,taskID);
        } catch (Exception ex) {
            throw new DoeException(ex.getMessage());
        }
        //runner.removeTask(jobID,taskID);
    }
    
    public void setAssembly(String assembly) throws DoeException {
        try {
            ((Method)methods.get("setAssembly")).invoke(runner,assembly);
        } catch (Exception ex) {
            throw new DoeException(ex.getMessage());
        }
        //runner.setAssembly(assembly);
    }
    
    public void addEventGraph(String eventGraph) throws DoeException {
        try {
            ((Method)methods.get("addEventGraph")).invoke(runner,eventGraph);
        } catch (Exception ex) {
            throw new DoeException(ex.getMessage());
        }
        //runner.addEventGraph(eventGraph);
    }
    // tbd, project jars? ie with XML
    public void addJar(File jarFile) throws DoeException {
        try {
            loader.doAddURL(jarFile.toURL());
        } catch (MalformedURLException ex) {
            throw new DoeException(ex.getMessage());
        }
    }
    
    public void run() throws DoeException {
        try {
            ((Method)methods.get("run")).invoke(runner,new Object[]{});
        } catch (Exception ex) {
            throw new DoeException(ex.getMessage());
        }
        //runner.run();
    }
    
}
