/*
 * LocalTaskQueue.java
 *
 * Created on January 15, 2007, 1:07 PM
 */

package viskit.xsd.assembly;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
//import java.util.Vector;
import viskit.doe.DoeException;
import viskit.doe.LocalBootLoader;

/**
 * LocalTaskQueue creates a number of Gridlets and stores
 * them unstarted in its Vectorness, overriding the set
 * and get methods to return boolean values according to
 * each Gridlet's isAlive() status.
 * @author Rick Goldberg
 * @version $Id: LocalTaskQueue.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class LocalTaskQueue extends ArrayList<Object> {
    GridRunner gridRunner;
    int totalTasks;
    File experimentFile;
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
    public LocalTaskQueue(GridRunner gridRunner, File experimentFile, int totalTasks) throws DoeException {
        this.experimentFile = experimentFile;
        this.totalTasks = totalTasks;
        this.gridRunner = gridRunner;

        for (int i = 0; i < totalTasks; i++) {            
            super.add(new Boolean(true));
        }
          
    }

    /** 
     * Activate Gridlet indexed at i
     *
     */
    public boolean activate(int i) {
        Object o = super.get(i);
        if (o instanceof Thread) {
            if (! ((Thread)o).isAlive() ) {
                ((Thread) super.get(i)).start();
                return true;
            }   
        } else if ((Boolean) o) {
            Object parent = Thread.currentThread().getContextClassLoader();
            Class<?> parentz = parent.getClass();
            try {
                Method getExtUrls = parentz.getMethod("getExtUrls");
                URL[] extUrls = (URL[]) getExtUrls.invoke(parent);
                Method getWorkDir = parentz.getMethod("getWorkDir");
                File workDir = (File) getWorkDir.invoke(parent);
                // really doesn't matter which ClassLoader gets passed to LBL, that's the whole point, it "boots" up from the bottom
                LocalBootLoader loader = new LocalBootLoader(extUrls, (ClassLoader)parent/* ClassLoader.getSystemClassLoader()*/, workDir);
                loader = loader.init();
                Thread.currentThread().setContextClassLoader(loader); // this line is not kidding around!
                Object task;
                Class<?> gridletz;
           
                gridletz = loader.loadClass("viskit.xsd.assembly.Gridlet");
                
                Constructor constr = gridletz.getConstructor(new Class<?>[]{});
                task = constr.newInstance(new Object[]{});
                ((Thread)task).setContextClassLoader(loader);
                ((Thread)task).setPriority(Thread.MAX_PRIORITY);
                //System.out.println("At this point, setting "+task+"'s loader to "+loader);
                //task.setExperimentFile(experimentFile);
                Class<?> fileClass = loader.loadClass("java.io.File");
                Constructor fileConstr = fileClass.getConstructor(java.net.URI.class);
                Object fileObj = fileConstr.newInstance(experimentFile.toURI());
                Method mthd = gridletz.getMethod("setExperimentFile",fileClass);
                mthd.invoke(task,fileObj);
                //task.setTaskID(i+1);
                mthd = gridletz.getMethod("setTaskID",int.class);
                mthd.invoke(task,(i+1));
                //task.setJobID(0); // tbd, enable multiple jobs
                mthd = gridletz.getMethod("setJobID",int.class);
                mthd.invoke(task,0);
                //task.setTotalTasks(totalTasks);
                mthd = gridletz.getMethod("setTotalTasks",int.class);
                mthd.invoke(task,totalTasks);
                
                // gridRunner to be done "retrospectively" on the other side
                // so send as Object
                //task.setGridRunner(gridRunner);
                mthd = gridletz.getMethod("setGridRunner",Object.class);
                mthd.invoke(task,(Object)gridRunner);
                
                super.set(i, task);
                ((Thread) task).start();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
       
    @Override
    public Object get(int i) {
        if (super.get(i) instanceof Thread)
            return new Boolean(Boolean.TRUE);
        
        else return super.get(i);
    }
    
    @Override    
    public boolean add(Object o) {
       
        return true;
    }
    
    @Override
    public int size() {
        return super.size(); // do fries come with that?
    }
    
    @Override
    public String toString() { /// mostly unusable
        String buf ="Task Queue Status:\n";
        StringBuffer sbuf = new StringBuffer(buf);
        
        for(int i = 0; i < size(); i++) {
            String task = "\t";
            if (super.get(i) instanceof Thread) {
                task += 
                        ((Thread)super.get(i)).isAlive()?"TaskID "+(i+1)+"RUNNING":"TaskID "+(i+1)+"WAITING";
            } else {
                task += "TaskID "+ (i=1) + (super.get(i).equals(Boolean.FALSE)?"DONE":"PENDING");
            }
            sbuf.append(task+"\n");
        }

        return buf;
    }
}