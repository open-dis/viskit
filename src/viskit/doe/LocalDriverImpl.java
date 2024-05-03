package viskit.doe;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import viskit.VStatics;

/**Implements a Local Doe Driver, to be interchangeable with the remote
 * (Grid Engine) Driver
 *
 * @since January 8, 2007, 2:17 PM
 * @author Rick Goldberg
 * @version $Id$
 */
public class LocalDriverImpl implements DoeRunDriver {

    LocalBootLoader loader;
    Object runner;
    Class<?> gridRunnerz;
    Hashtable<String, Method> methods;

    /** Creates a new instance of LocalDriverImpl */
    public LocalDriverImpl() { // remove if needed in gridkit.jar
        this(new URL[] {}, viskit.VGlobals.instance().getWorkDirectory());
    }

    public LocalDriverImpl(URL[] extClassPaths, File workDir) {
        loader = new LocalBootLoader(extClassPaths, viskit.VGlobals.instance().getWorkClassLoader(), workDir);
        initGridRunner(loader);
    }

    final void initGridRunner(LocalBootLoader loader) {
        loader = loader.init();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            gridRunnerz = loader.loadClass("viskit.gridlet.GridRunner");
            try {
                Class<?> loaderz = loader.loadClass(VStatics.LOCAL_BOOT_LOADER);
                Constructor lconstr = loaderz.getConstructor(URL[].class, ClassLoader.class, File.class);
                Object rloader = lconstr.newInstance(loader.getExtUrls(), ClassLoader.getSystemClassLoader(), loader.getWorkDir());
                Method initr = loaderz.getMethod("init");
                rloader = initr.invoke(rloader);
                Constructor constr = gridRunnerz.getConstructor(loader.loadClass(VStatics.LOCAL_BOOT_LOADER)); //yep
                runner = constr.newInstance(rloader);
                Method[] mthds = gridRunnerz.getMethods();
                methods = new Hashtable<>();
                for(Method m : mthds) {
                    methods.put(m.getName(), m);
                    //System.out.println("put "+m.getName()+" "+m);
                }
                //runner = (GridRunner) constr.newInstance(new Object[]{});
            } catch (IllegalArgumentException | IllegalAccessException | InstantiationException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
        } catch (SecurityException | NoSuchMethodException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void clear() throws DoeException {
        try {
            Method clear = gridRunnerz.getMethod("clear", new Class<?>[]{});
            clear.invoke(runner,new Object[]{});
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            throw new DoeException(ex.getMessage());
        }
        //runner.clear();
    }

    @Override
    public int flushQueue() throws DoeException {
        try {
            Method flushQueue = gridRunnerz.getMethod("flushQueue", new Class<?>[]{});
            return (Integer) flushQueue.invoke(runner,(Object[])null);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            throw new DoeException(ex.getMessage());
        }
        //return runner.flushQueue();
    }

    @Override
    public int getDesignPointCount() throws DoeException {
        try {
            return (Integer) (methods.get("getDesignPointCount")).invoke(runner,new Object[]{});
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new DoeException(ex.getMessage());
        }
        //return runner.getDesignPointCount();
    }

    @Override
    public synchronized Hashtable getDesignPointStats(int sampleIndex, int designPtIndex) throws DoeException {
        try {
            return (Hashtable) methods.get("getDesignPointStats").invoke(runner,sampleIndex,designPtIndex);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new DoeException(ex.getMessage());
        }
        //return runner.getDesignPointStats(sampleIndex,designPtIndex);
    }

    @Override
    public int getRemainingTasks() throws DoeException {
        try {
            return (Integer) methods.get("getRemainingTasks").invoke(runner,new Object[]{});

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new DoeException(ex.getMessage());
        }
        //return runner.getRemainingTasks();
    }

    @Override
    public synchronized Hashtable getReplicationStats(int sampleIndex, int designPtIndex, int replicationIndex) throws DoeException {
        try {
            return (Hashtable) methods.get("getReplicationStats").invoke(runner,sampleIndex,designPtIndex,replicationIndex);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new DoeException(ex.getMessage());
        }
        //return runner.getReplicationStats(sampleIndex,designPtIndex,replicationIndex);

    }

    @Override
    public synchronized String getResult(int sampleIndex, int designPtIndex) throws DoeException {
        try {
            return (String) methods.get("getResult").invoke(runner,sampleIndex,designPtIndex);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new DoeException(ex.getMessage());
        }
        //return runner.getResult(sample,designPt);
    }

    @Override
    public synchronized String getResultByTaskID(int taskID) throws DoeException {
        try {
            return (String) methods.get("getResultByTaskID").invoke(runner,taskID);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new DoeException(ex.getMessage());
        }
        //return runner.getResultByTaskID(taskID);
    }

    @Override
    public synchronized List<Object> getTaskQueue() throws DoeException {
        try {
            List queue = (List) methods.get("getTaskQueue").invoke(runner,new Object[]{});
            List<Object> cloneQueue = new ArrayList<>();
            for (Object queue1 : queue) {
                cloneQueue.add(queue1);
            }
            return cloneQueue;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new DoeException(ex.getMessage());
        }
        //return runner.getTaskQueue();
    }

    @Override
    public String qstat() throws DoeException {
        try {
            return (String) methods.get("qstat").invoke(runner);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            return "See Task Queue";
        }

    }

    @Override
    public String qstatXML() throws DoeException {
        return "<!--See Task Queue-->";
    }

    @Override
    public void removeIndexedTask(int sampleIndex, int designPtIndex) throws DoeException {
        try {
            methods.get("removeIndexedTask").invoke(runner, sampleIndex, designPtIndex);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new DoeException(ex.getMessage());
        }
        //runner.removeIndexedTask(sampleIndex,designPtIndex);
    }

    @Override
    public void removeTask(int jobID, int taskID) throws DoeException {
        try {
            methods.get("removeTask").invoke(runner,jobID,taskID);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new DoeException(ex.getMessage());
        }
        //runner.removeTask(jobID,taskID);
    }

    @Override
    public void setAssembly(String assembly) throws DoeException {
        try {
            methods.get("setAssembly").invoke(runner,assembly);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new DoeException(ex.getMessage());
        }
        //runner.setAssembly(assembly);
    }

    @Override
    public void addEventGraph(String eventGraph) throws DoeException {
        try {
            methods.get("addEventGraph").invoke(runner,eventGraph);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new DoeException(ex.getMessage());
        }
        //runner.addEventGraph(eventGraph);
    }

    // tbd, project jars? ie with XML
    @Override
    public void addJar(File jarFile) throws DoeException {
        try {
            loader.doAddURL(jarFile.toURI().toURL());
        } catch (MalformedURLException ex) {
            throw new DoeException(ex.getMessage());
        }
    }

    @Override
    public void run() throws DoeException {
        try {
            Method runMethod = methods.get("run");
            runMethod.invoke(runner,(Object[])null);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new DoeException(ex.getMessage());
        }
        //runner.run();
    }
}