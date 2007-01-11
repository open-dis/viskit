/*
 * LocalDriverImpl.java
 *
 * Created on January 8, 2007, 2:17 PM
 *
 * Implements a Local Doe Driver, to make pluggable with the
 * Remote (Grid) Driver, some methods are essentially NOP
 */

package viskit.doe;

import java.io.File;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

/**
 *
 * @author Rick Goldberg
 */
public class LocalDriverImpl implements DoeRunDriver {
    int POOLSIZE = 4;
    LocalBootLoader loader;
    Hashtable pool = new Hashtable();
    Hashtable queue = new Hashtable();
    
    /** Creates a new instance of LocalDriverImpl */
    public LocalDriverImpl() {
        loader = new LocalBootLoader(new URL[]{}, Thread.currentThread().getContextClassLoader());
    }

    public void clear() throws DoeException {
        ;
    }

    public int flushQueue() throws DoeException {
        return 0;
    }

    public int getDesignPointCount() throws DoeException {
        return 0;
    }

    public synchronized Hashtable getDesignPointStats(int sampleIndex, int designPtIndex) throws DoeException {
        return new Hashtable();
    }
    
    public int getRemainingTasks() throws DoeException {
        return 0;
    }

    public synchronized Hashtable getReplicationStats(int sampleIndex, int designPtIndex, int replicationIndex) throws DoeException {
        return new Hashtable();
        
        
    }

    public synchronized String getResult(int sample, int designPt) throws DoeException {
        return "";
    }

    public synchronized String getResultByTaskID(int taskID) throws DoeException {
        return "";
    }

    public synchronized Vector getTaskQueue() throws DoeException {
        
        return new Vector();
    }

    public String qstat() throws DoeException {
        
        return "";
    }

    public String qstatXML() throws DoeException {
        return "";
    }

    public void removeIndexedTask(int sampleIndex, int designPtIndex) throws DoeException {
        ;
    }

    public void removeTask(int jobID, int taskID) throws DoeException {
        ;
    }

    public void setAssembly(String assembly) throws DoeException {
        ;
    }

    public void addEventGraph(String assembly) throws DoeException {
        ;
    }

    public void addJar(File jarFile) throws DoeException {
        ;
    }

    public void run() throws DoeException {
    }
    
}
