/*
 * AssemblyHandler.java
 *
 * Created on January 26, 2006, 4:52 PM
 *
 * Provides XML-RPC handlers for AssemblyServer and SecureAssemblyServer
 *
 * Front end to the Gridkit service. Multiple users and multiple jobs per
 * user supported via login cookies. For each Unique Session ID, a GridRunner
 * is created to manage an array of Gridlet tasks for the SGE job.
 *
 * Also provides admin hooks for creating users and fixing passwords.
 *
 * Assembly and EventGraph XML are sent via XML-RPC from the DOE
 * Panel.
 *
 */
package viskit.gridlet;

import edu.nps.util.LogUtils;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;
import javax.xml.bind.JAXBContext;
import static javax.xml.bind.JAXBContext.newInstance;
import javax.xml.bind.JAXBException;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcHandler;

/**
 *
 * @author Rick Goldberg
 */
public class AssemblyHandler implements XmlRpcHandler {

    static Logger LOG = LogUtils.getLogger(AssemblyHandler.class);
    SessionManager sessionManager;
    Hashtable<String, GridRunner> gridRuns;

    // need to carry the port through to the GridRunners
    // so they can send back their Report stream; note
    // there will be no forward communication on this port
    // as that is being listened to for all other calls from
    // DOE panel. Since the GridRunners launch a number of
    // Gridlets, the usid also has to be carried through
    // so that the correct GridRunner's jaxb are updated
    // during an addReport(), likewise enabling the appropriate
    // getReport() monitor.
    int port;
    JAXBContext jaxbCtx;
    InputStream inputStream;

    public AssemblyHandler(int port) {
        this(port, new SessionManager());
    }

    public AssemblyHandler(int port, SessionManager sessionManager) {
        this.port = port;
        this.sessionManager = sessionManager;
        gridRuns = new Hashtable<>();
        try {
            jaxbCtx = newInstance("viskit.xsd.bindings.assembly");
        } catch (JAXBException e) {
            LOG.error("Classpath error loading jaxb bindings?", e);
        }
    }

    /**
     * Implement the XmlRpcHandler interface directly to manually specify available
     * methods. Realize that many users will be calling this all at once, globals strong-bad.
     * @param methodName
     * @param arguments
     * @return
     * @throws java.lang.Exception
     */
    @Override
    public Object execute(String methodName, Vector arguments) throws java.lang.Exception {
        Object ret;
        String call = methodName;
        String xmlData;
        String usid;

        // handle login request. returns a cookie which may or may not be real.
        // if it is, other methods seem to work and they do, if it isn't other
        // methods may or may not seem to work but don't. special case for admin
        // whose cookie is good for addUser() and changePassword()
        if (call.equals("gridkit.login")) {
            String username = (String)arguments.elementAt(0);
            String password = (String)arguments.elementAt(1);
            usid = sessionManager.login(username,password);
            if (sessionManager.authenticate(usid) && !sessionManager.isAdmin(usid)) {
                GridRunner gridRunner = new GridRunner(usid,port);
                gridRuns.put(usid,gridRunner);
            }
            return usid;
        }

        usid = "null";
        if (arguments.size() > 0) {
            if (arguments.elementAt(0)!=null) {
                usid = (String)arguments.elementAt(0);
            }
        }


        if (sessionManager.authenticate(usid)) {

            GridRunner gridRunner = gridRuns.get(usid);

            switch (call) {
                case "gridkit.setAssembly":
                    xmlData=(String) arguments.elementAt(1);
                    ret = gridRunner.setAssembly(xmlData);
                    break;
                case "gridkit.addEventGraph":
                    xmlData=(String) arguments.elementAt(1);
                    ret = gridRunner.addEventGraph(xmlData);
                    break;
                case "gridkit.transferJar":
                    // used by DOE to send a chunk of a jar
                    // jarTransfer(filename,byte[]).
                    ret = gridRunner.transferJar((String)arguments.elementAt(1),
                            (byte[])arguments.elementAt(2), ((Integer)arguments.elementAt(3)));
                    break;
                case "gridkit.getJars":
                    // used Gridlet to update its own Boot class loader
                    ret = gridRunner.getJars();
                    break;
                case "gridkit.run":
                    ret = gridRunner.run();
                    break;
                case "gridkit.addResult":
                case "gridkit.addReport":
                    xmlData= (String) arguments.elementAt(1);
                    ret = gridRunner.addResult(xmlData);
                    break;
                case "gridkit.getResult":
                    {
                        Integer sample = (Integer) arguments.elementAt(1);
                        Integer designPt = (Integer) arguments.elementAt(2);
                        ret = gridRunner.getResult(sample, designPt);
                        break;
                    }
                case "gridkit.getResultByTaskID":
                    {
                        Integer taskID = (Integer) arguments.elementAt(1);
                        ret = gridRunner.getResultByTaskID(taskID);
                        break;
                    }
                case "gridkit.getDesignPointStats":
                    {
                        Integer sample = (Integer) arguments.elementAt(1);
                        Integer designPt = (Integer) arguments.elementAt(2);
                        ret = gridRunner.getDesignPointStats(sample, designPt);
                        break;
                    }
                case "gridkit.getReplicationStats":
                    {
                        Integer sample = (Integer) arguments.elementAt(1);
                        Integer designPt = (Integer) arguments.elementAt(2);
                        Integer replication = (Integer) arguments.elementAt(3);
                        ret = gridRunner.getReplicationStats(sample, designPt, replication);
                        break;
                    }
                case "gridkit.addDesignPointStat":
                    {
                        Integer sample = (Integer) arguments.elementAt(1);
                        Integer designPt = (Integer) arguments.elementAt(2);
                        Integer numberOfStats = (Integer) arguments.elementAt(3);
                        String stat = (String) arguments.elementAt(4);
                        ret = gridRunner.addDesignPointStat(sample, designPt, numberOfStats, stat);
                        break;
                    }
                case "gridkit.addReplicationStat":
                    {
                        Integer sample = (Integer) arguments.elementAt(1);
                        Integer designPt = (Integer) arguments.elementAt(2);
                        Integer replication = (Integer) arguments.elementAt(3);
                        String stat = (String) arguments.elementAt(4);
                        ret = gridRunner.addReplicationStat(sample, designPt, replication, stat);
                        break;
                    }
                case "gridkit.flushQueue":
                    ret = gridRunner.flushQueue();
                    break;
                case "gridkit.getRemainingTasks":
                    ret = gridRunner.getRemainingTasks();
                    break;
                case "gridkit.getTaskQueue":
                    ret = gridRunner.getTaskQueue();
                    break;
                case "gridkit.clear":
                    ret = gridRunner.clear();
                    break;
                case "gridkit.removeIndexedTask":
                    {
                        Integer sample = (Integer) arguments.elementAt(1);
                        Integer designPt = (Integer) arguments.elementAt(2);
                        ret = gridRunner.removeTask(sample, designPt);
                        break;
                    }
                case "gridkit.removeTask":
                    {
                        Integer jobID = (Integer) arguments.elementAt(1);
                        Integer taskID = (Integer) arguments.elementAt(2);
                        ret = gridRunner.removeTask(jobID, taskID);
                        break;
                    }
                case "gridkit.setJobID":
                    {
                        // SGE jobID's are only known to the Gridlets
                        // once they are launched in an array
                        // referenced by jobID.taskID
                        // the USID is something we know ahead of time
                        // and the Gridlet can obtain to use to
                        // up-call its own jobID, to inform the registered
                        // GridRunner which will need to known how to
                        // manage the task via SGE (see wc_job_range_list in
                        // SGE man pages.) Note, only the first Gridlet
                        // from a jobID batch will use this call.
                        Integer jobID = (Integer) arguments.elementAt(1);
                        gridRunner.setJobID(jobID);
                        ret = jobID;
                        break;
                    }
                case "gridkit.addUser":
                    // sessionManager will of course check if admin
                    ret = sessionManager.addUser(usid,(String)(arguments.elementAt(1)));
                    break;
                case "gridkit.changePassword":
                    // sessionManager will of course check if either user or admin
                    String username = (String) arguments.elementAt(1);
                    String newPassword = (String) arguments.elementAt(2);
                    ret = sessionManager.changePassword(usid, username, newPassword);
                    break;
                case "gridkit.logout":
                    ret = sessionManager.logout(usid);
                    break;
                case "gridkit.qstat":
                    ret = gridRunner.qstat();
                    break;
                case "gridkit.qstatXML":
                    ret = gridRunner.qstatXML();
                    break;
                case "gridkit.getDesignPointCount":
                    ret = gridRunner.getDesignPointCount();
                    break;
                default:
                    ret = "No such method \""+methodName+"\"! ";
                    break;
            }
            return ret;

        } else {
            return Boolean.FALSE;
        }
    }

}
