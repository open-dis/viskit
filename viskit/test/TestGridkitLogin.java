/*
 * TestGridkitLogin.java
 *
 * Created on January 31, 2006, 5:52 PM
 *
 * Create a login session.
 */

package viskit.test;

import java.util.Vector;
import org.apache.xmlrpc.XmlRpcClientLite;
/**
 *
 * @author Rick Goldberg
 */
public class TestGridkitLogin extends Thread {

    XmlRpcClientLite xmlrpc;
    /** Creates a new instance of TestGridkitLogin */
    public TestGridkitLogin(String server, int port) throws Exception {
        xmlrpc = new XmlRpcClientLite(server,  port);
    }
    
    public void run() {
        Vector params = new Vector();
        String user = "admin";
        String passwd = "hello";
        String usid = "";
        Object ret;
        params.add(user); // just something to create bogus usid
        params.add(user); // to initialize a password file
        try {
            // test assumes no /tmp/passwd.xml exists!
            // when addUser is called the first time
            // with admin, it initializes the passwd
            // database and creates a temporary password
            // for admin, "admin". This is a required
            // part of installing Gridkit by the admin,
            // before the port is made external.
            ret = xmlrpc.execute("gridkit.addUser",params);
            System.out.println("addUser returns "+ret.toString());
            // users are initialized with their usernames a password
            // and should change them asap.
            usid = (String) xmlrpc.execute("gridkit.login", params);
            System.out.println("login returned "+usid);
            
            params.clear();
            params.add(usid);
            ret = xmlrpc.execute("gridkit.logout", params);
            System.out.println("logout "+usid+" "+ret);
            
        } catch (Exception e) { e.printStackTrace(); }
        
    }
    
    public static void main(String[] args) {
        try {
            TestGridkitLogin test = new TestGridkitLogin(args[0], Integer.parseInt(args[1]));
            test.start();
        } catch (Exception e) { e.printStackTrace(); }
    }
    
}
