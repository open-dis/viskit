/*
 * LocalTaskQueue.java
 *
 * Created on January 15, 2007, 1:07 PM
 *
 * LocalTaskQueue creates a number of Gridlets and stores
 * them unstarted in its Vectorness, overriding the set
 * and get methods to return boolean values according to
 * each Gridlet's isAlive() status.
 *
 */

package viskit.xsd.assembly;

import java.io.File;
import java.util.Vector;
import viskit.doe.LocalBootLoader;

/**
 *
 * @author Rick Goldberg
 */
public class LocalTaskQueue extends Vector {
    GridRunner gridRunner;
    int totalTasks;
    /** Creates a new instance of LocalTaskQueue, instanced by
     * GridRunner in local mode.
     *
     * Fills a vector with dormant Gridlets. Each Gridlet is
     * a type of Thread that requires its own freshly created
     * ClassLoader. The LocalBootLoader rereads all needed
     * classes, this ensures that each Thread can run with its
     * own static context independently of one another, which
     * is required to run each Simkit simulation independently.
     *
     * Additional classpath beyond the default Viskit classpaths,
     * should have been loaded in the current Thread's context
     * ClassLoader, which is also a LocalBootLoader which 
     * provides the list of "ext" URL's. 
     */
    public LocalTaskQueue(GridRunner gridRunner, File experimentFile, int totalTasks) {
        super(totalTasks);
        for (int i = 0; i<totalTasks; i++) {
            LocalBootLoader parent = (LocalBootLoader)Thread.currentThread().getContextClassLoader();
            LocalBootLoader loader = new LocalBootLoader(parent.getExtUrls(), ClassLoader.getSystemClassLoader(), viskit.VGlobals.instance().getWorkDirectory());
            loader = loader.init();
            Gridlet task;
            Class gridletz;
            try {
                gridletz = loader.loadClass("viskit.xsd.assembly.Gridlet");
                try {
                    task = (Gridlet) gridletz.newInstance();
                    task.setExperimentFile(experimentFile);
                    task.setTaskID(i+1);
                    task.setJobID(0); // tbd, enable multiple jobs
                    task.setTotalTasks(totalTasks);
                    super.add(i,task);
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                } catch (InstantiationException ex) {
                    ex.printStackTrace();
                }
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
            
        }
    }
    
    /** 
     * Activate Gridlet indexed at i-1
     *
     * A result of complimenting the SGE backend, where
     * TaskID's start at 1. //still need to be done here?
     */
    public void activate(int i) {
        ((Thread) super.get(i-1)).start();
    }
    
    public Object get(int i) {
        if (super.get(i) instanceof Thread)
            return (Object)((Boolean) ((Thread)(super.get(i))).isAlive());
        else return super.get(i);
    }
    
    public boolean add(Object o) {
       
        return true;
    }
}
