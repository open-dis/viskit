/*
 * TestGridkitLogin.java
 *
 * Created on January 31, 2006, 5:52 PM
 *
 * Create a login session.
 */

package viskit.test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Vector;
import org.apache.xmlrpc.XmlRpcClientLite;
/**
 *
 * @author Rick Goldberg
 */
public class TestGridkitLogin extends Thread {

    XmlRpcClientLite xmlrpc;
    /** 
     * Creates a new instance of TestGridkitLogin
     * To set up a server, run the ant gridkit-jar
     * target, then just "java -jar dist/gridkit.jar"
     */
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
            // test can be used to init a passwd.xml file
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
            
            // logout this session
            params.clear();
            params.add(usid);
            ret = xmlrpc.execute("gridkit.logout", params);
            System.out.println("logout "+usid+" "+ret);
            
            // log back in
            params.clear();
            params.add("admin");
            params.add("admin");
            usid = (String)xmlrpc.execute("gridkit.login", params);
            System.out.println("login returned "+usid);
            
            // change admin's password
            params.clear();
            params.add(usid);
            params.add("admin");
            params.add("hello");
            ret = xmlrpc.execute("gridkit.changePassword", params);
            System.out.println("changePassword returned "+ret);
            
            // logout again
            params.clear();
            params.add(usid);
            ret = xmlrpc.execute("gridkit.logout", params);
            System.out.println("logout "+usid+" "+ret);
            
            // test new password with login
            params.clear();
            params.add("admin");
            params.add("hello");
            usid = (String)xmlrpc.execute("gridkit.login", params);
            System.out.println("login returned "+usid);
            
            // logout again
            params.clear();
            params.add(usid);
            ret = xmlrpc.execute("gridkit.logout",params);
            System.out.println("logout "+usid+" "+ret);
            
            // now try a bogus password for admin and force an error
            params.clear();
            params.add("admin");
            params.add("bogus");
            usid = (String)xmlrpc.execute("gridkit.login", params);
            System.out.println("bogus login attempt returned "+usid+((usid.equals("LOGIN-ERROR"))?" which is cool":" which is not cool"));
            
            // now see if bogus usid allows me to create a user
            params.clear();
            params.add(usid);
            params.add("newbie");
            ret = xmlrpc.execute("gridkit.addUser", params);
            System.out.println("bogus addUser attempt returned "+ret+((((Boolean)ret).booleanValue())?" which is not cool":" which is cool"));
            
            // now login as admin to create newbie
            params.clear();
            params.add("admin");
            params.add("hello");
            usid = (String)xmlrpc.execute("gridkit.login", params);
            System.out.println("login returned "+usid);
            
            // addUser newbie with verified admin usid
            params.clear();
            params.add(usid);
            params.add("newbie");
            ret = xmlrpc.execute("gridkit.addUser", params);
            System.out.println("addUser of newbie returned "+ret);
            
            // this time don't logout, see if multi session works
            // for newbie to login and changePassword from default
            params.clear();
            params.add("newbie");
            params.add("newbie");
            usid = (String)xmlrpc.execute("gridkit.login",params);
            params.clear();
            params.add(usid);
            params.add("newbie");
            params.add("newpass");
            ret = xmlrpc.execute("gridkit.changePassword", params);
            System.out.println("newbie login and changePassword returned "+ret);
            
            // now send a jar to newbies new session
            
            URL u = Thread.currentThread().getContextClassLoader().getResource("diskit/DISMover3D.class");
            System.out.println("Opening "+ u);
            u = new URL((u.getFile().split("!"))[0].trim());
            File jar = new File(u.getFile());
            System.out.println("Opening "+jar);
            FileInputStream fis = new FileInputStream(jar);
            long fileSize = jar.length();
            System.out.println("which is "+fileSize+ " bytes");
            byte[] buf = new byte[1024];
            int chunks = (int)(fileSize/1024L + (fileSize % 1024L > 0L ? 0L : -1L));
            System.out.println("into "+chunks+1+ " of "+buf.length);
            while ( chunks > -1 ) {
                params.clear();
                params.add(usid);
                params.add("diskit.jar");
                int readIn = fis.read(buf);
                byte[] outBuf;
                if (readIn < buf.length) {
                    // this effectively trims excess 0's
                    // from last chunk (#0)
                    outBuf = new byte[readIn];
                    System.arraycopy(buf, 0, outBuf, 0, readIn);
                    // but if not #0 io error
                    if (chunks != 0) {
                        System.out.println("File io error");
                    }
                } else {
                    outBuf = buf;
                }
                
                System.out.println("read in "+readIn);

                params.add(outBuf);
                params.add(new Integer(chunks));
                ret = xmlrpc.execute("gridkit.transferJar", params);
                System.out.println("Transferred "+ret+" bytes in chunk # "+chunks);
                chunks --;
            }
            
        } catch (Exception e) { e.printStackTrace(); }
        
    }
    
    public static void main(String[] args) {
        try {
            TestGridkitLogin test = new TestGridkitLogin(args[0], Integer.parseInt(args[1]));
            test.start();
        } catch (Exception e) { e.printStackTrace(); }
    }
    
}
