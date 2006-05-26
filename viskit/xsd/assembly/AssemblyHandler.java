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

package viskit.xsd.assembly;

import java.io.ByteArrayInputStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.spec.PBEKeySpec;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBContext;
import org.apache.xmlrpc.*;
import viskit.xsd.bindings.assembly.*;

/**
 *
 * @author Rick Goldberg
 */

public class AssemblyHandler implements XmlRpcHandler {
    SessionManager sessionManager;
    Hashtable gridRuns;
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
        this.port = port;
        sessionManager = new SessionManager();
        gridRuns = new Hashtable();
        try {
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
        } catch (Exception e) {
            sessionManager.log("Classpath error loading jaxb bindings?");
        }
    } 
      
    /**
     * Implement the XmlRpcHandler interface directly to manually specify available
     * methods. Realize that many users will be calling this all at once, globals strong-bad.
     */
    
    public Object execute(String methodName, Vector arguments) throws java.lang.Exception {
        Object ret;
        String call = new String(methodName);
        String xmlData = new String("empty");
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
                usid = new String((String)arguments.elementAt(0));
            }
        }
        
        
        if (sessionManager.authenticate(usid)) {
            
            GridRunner gridRunner = (GridRunner) gridRuns.get(usid);
            
            if (call.equals("gridkit.setAssembly")) {
                
                xmlData=(String) arguments.elementAt(1);
                ret = gridRunner.setAssembly(xmlData);
                
            } else if (call.equals("gridkit.addEventGraph")) {
                
                xmlData=(String) arguments.elementAt(1);
                ret = gridRunner.addEventGraph(xmlData);
                
            } else if (call.equals("gridkit.transferJar")) {
                // used by DOE to send a chunk of a jar
                // jarTransfer(filename,byte[]). 
                ret = gridRunner.transferJar((String)arguments.elementAt(1),
                        (byte[])arguments.elementAt(2),
                        ((Integer)arguments.elementAt(3)).intValue());
            
            } else if (call.equals("gridkit.getJars")) {
                // used Gridlet to update its own Boot class loader
                ret = gridRunner.getJars();
            
            } else if (call.equals("gridkit.run")) {
                
                ret = gridRunner.run();
                
            } else if (call.equals("gridkit.addResult") ||
                    call.equals("gridkit.addReport")) {
                
                xmlData= (String) arguments.elementAt(1);
                ret = gridRunner.addResult(xmlData);
                
            } else if (call.equals("gridkit.getResult")) {
                
                Integer sample = (Integer) arguments.elementAt(1);
                Integer designPt = (Integer) arguments.elementAt(2);
                ret = gridRunner.getResult(sample.intValue(), designPt.intValue());
                
            } else if (call.equals("gridkit.getResultByTaskID")) {
                
                Integer taskID = (Integer) arguments.elementAt(1);
                ret = gridRunner.getResultByTaskID(taskID.intValue());
                
            } else if (call.equals("gridkit.getDesignPointStats")) {
                
                Integer sample = (Integer) arguments.elementAt(1);
                Integer designPt = (Integer) arguments.elementAt(2);
                ret = gridRunner.getDesignPointStats(sample.intValue(), designPt.intValue());
                
            } else if (call.equals("gridkit.getReplicationStats")) {
                
                Integer sample = (Integer) arguments.elementAt(1);
                Integer designPt = (Integer) arguments.elementAt(2);
                Integer replication = (Integer) arguments.elementAt(3);
                ret = gridRunner.getReplicationStats(sample.intValue(), designPt.intValue(), replication.intValue());
                
            } else if (call.equals("gridkit.addDesignPointStat")) {
                
                Integer sample = (Integer) arguments.elementAt(1);
                Integer designPt = (Integer) arguments.elementAt(2);
                Integer numberOfStats = (Integer) arguments.elementAt(3);
                String stat = (String) arguments.elementAt(4);
                ret = gridRunner.addDesignPointStat(sample.intValue(), designPt.intValue(), numberOfStats.intValue(), stat);
                
            } else if (call.equals("gridkit.addReplicationStat")) {
                
                Integer sample = (Integer) arguments.elementAt(1);
                Integer designPt = (Integer) arguments.elementAt(2);
                Integer replication = (Integer) arguments.elementAt(3);
                String stat = (String) arguments.elementAt(4);
                ret = gridRunner.addReplicationStat(sample.intValue(), designPt.intValue(), replication.intValue(), stat);
                
            } else if (call.equals("gridkit.flushQueue")) {
                
                ret = gridRunner.flushQueue();
                
            } else if (call.equals("gridkit.getRemainingTasks")) {
                
                ret = gridRunner.getRemainingTasks();
                
            } else if (call.equals("gridkit.getTaskQueue")) {
                
                ret = gridRunner.getTaskQueue();
                
            } else if (call.equals("gridkit.clear")) {
                
                ret = gridRunner.clear();
                
            } else if (call.equals("gridkit.removeIndexedTask")) {
                
                Integer sample = (Integer) arguments.elementAt(1);
                Integer designPt = (Integer) arguments.elementAt(2);
                ret = gridRunner.removeTask(sample.intValue(),designPt.intValue());
                
            } else if (call.equals("gridkit.removeTask")) {
                
                Integer jobID = (Integer) arguments.elementAt(1);
                Integer taskID = (Integer) arguments.elementAt(2);
                ret = gridRunner.removeTask(jobID.intValue(),taskID.intValue());
                
            } else if (call.equals("gridkit.setJobID")) {
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
                
            } else if (call.equals("gridkit.addUser")) {
                    // sessionManager will of course check if admin
                    ret = sessionManager.addUser(usid,(String)(arguments.elementAt(1)));
                    
            } else if (call.equals("gridkit.changePassword")) {
                // sessionManager will of course check if either user or admin
                String username = (String) arguments.elementAt(1);
                String newPassword = (String) arguments.elementAt(2);
                
                ret = sessionManager.changePassword(usid, username, newPassword);
                
            } else if (call.equals("gridkit.logout")) {
                
                ret = sessionManager.logout(usid);
                
            } else if (call.equals("gridkit.qstat")) {
                
                ret = gridRunner.qstat();
                
            } else if (call.equals("gridkit.qstatXML")) {
                
                ret = gridRunner.qstatXML();
                
            } else if (call.equals("gridkit.getDesignPointCount")) {
                
                ret = gridRunner.getDesignPointCount();
                
            } else {
                
                ret = new String("No such method \""+methodName+"\"! ");
                
            }
            return ret;
            
        } else {
            return Boolean.FALSE;
        }
    }
    
}
