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
    GridRunner runner;
    
    /** Creates a new instance of LocalDriverImpl */
    public LocalDriverImpl() {
        loader = new LocalBootLoader(new URL[]{}, Thread.currentThread().getContextClassLoader(), viskit.VGlobals.instance().getWorkDirectory());
        loader = loader.init();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            Class gridRunnerz = loader.loadClass("viskit.xsd.assembly.GridRunner");
            try {
                runner = (viskit.xsd.assembly.GridRunner) (gridRunnerz.newInstance());
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            } catch (InstantiationException ex) {
                ex.printStackTrace();
            }
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        
    }

    public void clear() throws DoeException {
        runner.clear();
    }

    public int flushQueue() throws DoeException {
        return runner.flushQueue();
    }

    public int getDesignPointCount() throws DoeException {
        return runner.getDesignPointCount();
    }

    public synchronized Hashtable getDesignPointStats(int sampleIndex, int designPtIndex) throws DoeException {
        return runner.getDesignPointStats(sampleIndex,designPtIndex);
    }
    
    public int getRemainingTasks() throws DoeException {
        return runner.getRemainingTasks();
    }

    public synchronized Hashtable getReplicationStats(int sampleIndex, int designPtIndex, int replicationIndex) throws DoeException {
        return runner.getReplicationStats(sampleIndex,designPtIndex,replicationIndex);
        
    }

    public synchronized String getResult(int sample, int designPt) throws DoeException {
        return runner.getResult(sample,designPt);
    }

    public synchronized String getResultByTaskID(int taskID) throws DoeException {
        return runner.getResultByTaskID(taskID);
    }

    public synchronized Vector getTaskQueue() throws DoeException {
        
        return runner.getTaskQueue();
    }

    public String qstat() throws DoeException {
        
        return "See Task Queue";
    }

    public String qstatXML() throws DoeException {
        return "<!--See Task Queue-->";
    }

    public void removeIndexedTask(int sampleIndex, int designPtIndex) throws DoeException {
        runner.removeIndexedTask(sampleIndex,designPtIndex);
    }

    public void removeTask(int jobID, int taskID) throws DoeException {
        runner.removeTask(jobID,taskID);
    }

    public void setAssembly(String assembly) throws DoeException {
        runner.setAssembly(assembly);
    }

    public void addEventGraph(String eventGraph) throws DoeException {
        runner.addEventGraph(eventGraph);
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
        runner.run();
    }
    
}
