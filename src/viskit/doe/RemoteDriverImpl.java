/*
 * RemoteDriverImpl.java
 *
 * Created on January 8, 2007, 2:19 PM
 *
 * Implements Remote (Grid) Doe Driver, uses XML-RPC
 * to connect to Gridkit service.
 */

package viskit.doe;

import edu.nps.util.Log4jUtilities;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlrpc.XmlRpcClientLite;
import org.apache.xmlrpc.XmlRpcException;
import viskit.gridlet.SessionManager;
/**
 *
 * @author Rick Goldberg
 */
public class RemoteDriverImpl implements DoeDriver
{
    static final Logger LOG = LogManager.getLogger();
    
    XmlRpcClientLite rpc;
    Vector<Object> args = new Vector<>();
    String usid = SessionManager.LOGIN_ERROR;

    /** Creates a new instance of RemoteDriverImpl
     * @param host
     * @param port
     * @param user
     * @param password
     * @throws viskit.doe.DoeException
     */
    public RemoteDriverImpl(String host, int port, String user, String password) throws DoeException {
        try {
            rpc = new XmlRpcClientLite(host,port);
        } catch (MalformedURLException ex) {
            throw new DoeException("Malformed Url at "+host+":"+port);
        } 
        catch (Exception e) {
            LOG.error("RemoteDriverImpl(" + host + ", " + port + ", " + user + ", " + password + ") exception: " + e.getMessage());
        }
        if (rpc == null) throw new DoeException("Can't connect to Gridkit service at "+host+":"+port);

        login(user,password);
        Runtime.getRuntime().addShutdownHook(new Cleanup());
    }
    
    /** Replacement for Object.finalize() */
    class Cleanup extends Thread {

        @Override
        public void run() {
            try {
                logout();
            } catch (DoeException e) {
                LOG.error(e.getMessage());
            }
        }
    }

    // used to quickly convert args to an args Vector for rpc via varargs
    private Vector<Object> makeArgs(Object... arg) {
        args.clear();
        args.addAll(Arrays.asList(arg));
        return args;
    }

    @Override
    public void run() throws DoeException {
        boolean ret = false;
        try {
            ret = (Boolean) rpc.execute("gridkit.run", makeArgs(usid));
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException(xre.getMessage());
        }
        if (!ret) throw new DoeException("run failed: not set up or logged in?");
    }

    @Override
    public void addUser(String newUser) throws DoeException {
        boolean ret = false;
        try {
            ret = (Boolean) rpc.execute("gridkit.addUser", makeArgs(usid,newUser));
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("addUser failed: "+xre.getMessage());
        }
        if (!ret) throw new DoeException("addUser failed: not admin?");
    }

    @Override
    public void changePassword(String username, String newPassword) throws DoeException {
        boolean ret = false;

        try {
            ret = (Boolean) rpc.execute("gridkit.changePassword", makeArgs(usid,username,newPassword));
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("changePassword failed: "+xre.getMessage());
        }
        if (!ret) throw new DoeException("changePassword failed: not admin?");
    }

    @Override
    public final void login(String username, String password) throws DoeException {
        this.usid = SessionManager.LOGIN_ERROR;

        try {
            usid = (String) rpc.execute("gridkit.login", makeArgs(username,password) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("login failed: "+xre.getMessage());
        }
        if (usid.equals(SessionManager.LOGIN_ERROR)) throw new DoeException("login failed: check username or password");
    }

    @Override
    public void logout() throws DoeException {
        boolean ret = false;

        try {
            ret = (Boolean) rpc.execute("gridkit.logout", makeArgs(usid) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("logout failed: "+xre.getMessage());
        }
        if (!ret) throw new DoeException("logout failed: not logged in?");

    }

    @Override
    public void clear() throws DoeException {
        boolean ret = false;

        try {
            ret = (Boolean) rpc.execute("gridkit.clear", makeArgs(usid) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("clear failed: "+xre.getMessage());
        }
        if (!ret) throw new DoeException("clear failed: not logged in, set up, or running?");

    }

    @Override
    public int flushQueue() throws DoeException {
        int ret = -1;

        try {
            ret = (Integer) rpc.execute("gridkit.flushQueue", makeArgs(usid) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("flushQueue failed: "+xre.getMessage());
        }
        if (ret<0) throw new DoeException("flushQueue failed: not logged in, set up or running?");
        return ret;
    }

    @Override
    public int getDesignPointCount() throws DoeException {
        int ret = -1;

        try {
            ret = (Integer) rpc.execute("gridkit.getDesignPointCount", makeArgs(usid) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("getDesignPointCount failed: "+xre.getMessage());
        }
        if (ret<0) throw new DoeException("getDesignPointCount failed: not logged in, set up or running?");
        return ret;
    }

    @Override
    public synchronized Map getDesignPointStatistics(int sampleIndex, int designPtIndex) throws DoeException {
        Map ret = new Hashtable();

        try {
            ret = (Map) rpc.execute("gridkit.getDesignPointStats", makeArgs(usid,sampleIndex,designPtIndex) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("getDesignPointStats failed: "+xre.getMessage());
        }
        if (ret.isEmpty()) throw new DoeException("getDesignPointStats failed: not logged in, set up or running, or ready with stats yet?");
        return ret;
    }


    @Override
    public int getRemainingTasks() throws DoeException {
        int ret = -1;

        try {
            ret = (Integer) rpc.execute("gridkit.getRemainingTasks", makeArgs(usid) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("getRemainingTasks failed: "+xre.getMessage());
        }
        if (ret<0) throw new DoeException("getRemainingTasks failed: not logged in, set up or running?");
        return ret;
    }

    @Override
    public synchronized Map getReplicationStatistics(int sampleIndex, int designPtIndex, int replicationIndex) throws DoeException {
         Map ret = new Hashtable();

        try {
            ret = (Map) rpc.execute("gridkit.getReplicationStats", makeArgs(usid,sampleIndex,designPtIndex,replicationIndex) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("getReplicationStats failed: "+xre.getMessage());
        }
        if (ret.isEmpty()) throw new DoeException("getReplicationStats failed: not logged in, set up or running, or ready with stats yet?");
        return ret;
    }

    @Override
    public synchronized String getResult(int sample, int designPt) throws DoeException {
        String ret = "";

        try {
            ret = (String) rpc.execute("gridkit.getResult", makeArgs(usid,sample,designPt) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("getResult failed: "+xre.getMessage());
        }
        if (ret.equals("")) throw new DoeException("getResult failed: not logged in, set up or running? (this method blocks until data available)");
        return ret;
    }

    // TODO: XML RPC v2.0 not updated to generics
    @SuppressWarnings("unchecked")
    @Override
    public synchronized String getResultByTaskID(int taskID) throws DoeException {
        String ret = "";

        try {
            ret = (String) rpc.execute("gridkit.getResultByTaskID", makeArgs(usid,taskID) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("getResultByTaskID failed: "+xre.getMessage());
        }
        if (ret.equals("")) throw new DoeException("getResultByTaskID failed: not logged in, set up or running? (this method blocks until data available)");
        return ret;
    }

    // TODO: XML RPC v2.0 not updated to generics
    @SuppressWarnings("unchecked")
    @Override
    public synchronized List<Object> getTaskQueue() throws DoeException {
        Vector<Object> rpcret = new Vector<>();
        List<Object> ret = new ArrayList<>();
        try {
            rpcret = (Vector<Object>) rpc.execute("gridkit.getTaskQueue", makeArgs(usid) );
        } catch (XmlRpcException | IOException ioe) {
            throw new DoeException("getTaskQueue failed: "+ioe.getMessage());
        }
        if (rpcret.isEmpty()) throw new DoeException("getTaskQueue failed: not logged in, set up or running? (this method blocks until data available)");
        ret.addAll(rpcret);
        return ret;
    }

    @Override
    public String qstat() throws DoeException {
        String ret = "";

        try {
            ret = (String) rpc.execute("gridkit.qstat", makeArgs(usid) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("qstat failed: "+xre.getMessage());
        }
        if (ret.equals("")) throw new DoeException("qstat failed: not logged in?");
        return ret;
    }

    @Override
    public String qstatXML() throws DoeException {
        String ret = "";

        try {
            ret = (String) rpc.execute("gridkit.qstatXML", makeArgs(usid) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("qstatXML failed: "+xre.getMessage());
        }
        if (ret.equals("")) throw new DoeException("qstatXML failed: not logged in?");
        return ret;
    }

    @Override
    public void removeIndexedTask(int sampleIndex, int designPtIndex) throws DoeException {
        int ret = -1;

        try {
            ret = (Integer) rpc.execute("gridkit.removeIndexedTask", makeArgs(usid,sampleIndex,designPtIndex) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("removeIndexedTask failed: "+xre.getMessage());
        }

        if (ret<0) throw new DoeException("removeIndexedTask failed: not logged in, set up or running?");


    }

    @Override
    public void removeTask(int jobID, int taskID) throws DoeException {
        int ret = -1;

        try {
            ret = (Integer) rpc.execute("gridkit.removeTask", makeArgs(usid,jobID,taskID) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("removeTask failed: "+xre.getMessage());
        }

        if (ret<0) throw new DoeException("removeTask failed: not logged in, set up or running?");

    }

    @Override
    public void setAssembly(String assemblyXML) throws DoeException {
        boolean ret = false;

        try {
            ret = (Boolean) rpc.execute("gridkit.setAssembly", makeArgs(usid,assemblyXML) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("setAssembly failed: "+xre.getMessage());
        }

        if (!ret) throw new DoeException("setAssembly failed: not logged in?");

    }

    @Override
    public void addEventGraph(String eventGraphXML) throws DoeException {
        boolean ret = false;

        try {
            ret = (Boolean) rpc.execute("gridkit.addEventGraph", makeArgs(usid,eventGraphXML) );
        } catch (XmlRpcException | IOException xre) {
            throw new DoeException("addEventGraph failed: "+xre.getMessage());
        }

        if (!ret) throw new DoeException("addEventGraph failed: not logged in?");

    }

    @Override
    public void addJar(File jarFile) throws DoeException {
        InputStream is = null;
        try {
            is = jarFile.toURI().toURL().openStream();
        } catch (MalformedURLException ex) {
            throw new DoeException( ex.getMessage() );
        } catch (IOException ex) {
            throw new DoeException( ex.getMessage() );
        }
        boolean ret = true;
        byte[] buf = new byte[256];
        int fileSize = (int) jarFile.length();
        int chunks = ( fileSize / buf.length ) + ( (fileSize % buf.length) > 0 ? 0 : -1 );
        int bufRead = 0;

        while (chunks > -1 && bufRead > -1) { // both should > -1 together
            try {
                bufRead = is.read(buf);
                ret &= (Boolean) rpc.execute("gridkit.transferJar", makeArgs(usid,jarFile.getName(), buf, chunks-- ) );
            } catch (XmlRpcException | IOException xre) {
                throw new DoeException("addJar failed: "+xre.getMessage());
            }
        }

        if (!ret) throw new DoeException("addJar failed: not logged in?");
    }

}
