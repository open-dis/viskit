package viskit.doe;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Rick Goldberg
 * @since January 8, 2007, 11:36 AM
 * @version $Id$
 */
public interface DoeRunDriver {

    // TBD: all methods should be throwing Exceptions rather than use the rpc protocol return values
    // this is a remnant of evolving from the underlying handlers and looks tacky.

    void run() throws DoeException;

    /**
     * handler for clearing the experiment from memory,
     * could be used in cases where you want to flush the queue
     * and also the accumulated state so far.
     * @throws DoeException
     */
    void clear() throws DoeException;

    /**
     * handler for clearing the grid queue,
     *
     * @return number of remaining tasks still in the queue
     * that will be terminated.
     * @throws DoeException
     */
    int flushQueue() throws DoeException;

    int getDesignPointCount() throws DoeException;

    Map getDesignPointStats(int sampleIndex, int designPtIndex) throws DoeException;

    /**
     * handler for returning number of remaining tasks in queue.
     *
     * @return number of remaining tasks in the queue still running.
     * @throws DoeException
     */
    int getRemainingTasks() throws DoeException;

    Map getReplicationStats(int sampleIndex, int designPtIndex, int replicationIndex) throws DoeException;

    /**
     * hook to retrieve results from an experimental run.
     * The call is synchronized, the calling client thread
     * which invokes this method on the server thread blocks
     * until a node run invokes the addResult() on a separate
     * server thread for the particular run requested.
     *
     * Any of Async XML-RPC with client callbacks, single threaded
     * in order, or multithreaded any order clients can be used.
     * This server has a maximum of 100 server threads default,
     * so don't send more than that many multithreaded requests
     * unless using Async mode with callbacks.
     *
     * Note: this method times out if a timeout value is set as
     * an attribute of the Experiment. This makes it tunable depending
     * on the expected run time of the bench test by the user. This
     * comes in handy if the client was single threaded in sequence,
     * see TestReader.java in gridkit.tests. If no value for timeout
     * is supplied in the XML-GRD, then it waits indefinitely.
     * @param sample
     * @param designPt
     * @return
     * @throws DoeException
     */
    String getResult(int sample, int designPt) throws DoeException;

    String getResultByTaskID(int taskID) throws DoeException;

    /** Users of the getTaskQueue() through DoeRunDriver, in local
     * mode need a copy, not the same Vector; users of it in remote
     * mode automatically get a copy. Since we don't actually need
     * any of the internals, simple Booleans get copied in here
     *
     * @return a List of queued tasks
     * @throws viskit.doe.DoeException
     */
    List<Object> getTaskQueue() throws DoeException;

    String qstat() throws DoeException;

    String qstatXML() throws DoeException;

    void removeIndexedTask(int sampleIndex, int designPtIndex) throws DoeException;

    void removeTask(int jobID, int taskID) throws DoeException;

    /**
     * hook for gridkit.setAssembly XML-RPC call, used to initialize
     * From DOE panel. Accepts raw XML String of Assembly.
     * @param assembly
     * @throws DoeException
     */
    void setAssembly(String assembly) throws DoeException;

    void addEventGraph(String assembly) throws DoeException;

    void addJar(File jarFile) throws DoeException;

}
