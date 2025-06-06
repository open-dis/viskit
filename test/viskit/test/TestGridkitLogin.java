/*
 * TestGridkitLogin.java
 *
 * Created on January 31, 2006, 5:52 PM
 *
 * Create a login session.
 */
package viskit.test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Vector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlrpc.XmlRpcClientLite;
import org.apache.xmlrpc.XmlRpcException;
import viskit.ViskitGlobals;

/**
 * Test case
 * @author Rick Goldberg
 * @version $Id: TestGridkitLogin.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class TestGridkitLogin extends Thread 
{
    static final Logger LOG = LogManager.getLogger();

    XmlRpcClientLite xmlrpc;

    /**
     * Creates a new instance of TestGridkitLogin
     * @param server
     * @param port
     * @throws java.lang.Exception
     */
    public TestGridkitLogin(String server, int port) throws Exception {
        xmlrpc = new XmlRpcClientLite(server, port);
    }

    @Override
    public void run() {
        Vector<Object> params = new Vector<>();
        String user = "admin";
        String usid;
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
            ret = xmlrpc.execute("gridkit.addUser", params);
            LOG.info("addUser returns " + ret.toString());

            // users are initialized with their usernames a password
            // and should change them asap.
            usid = (String) xmlrpc.execute("gridkit.login", params);
            LOG.info("login returned " + usid);

            // logout this session
            params.clear();
            params.add(usid);
            ret = xmlrpc.execute("gridkit.logout", params);
            LOG.info("logout " + usid + " " + ret);

            // log back in
            params.clear();
            params.add("admin");
            params.add("admin");
            usid = (String) xmlrpc.execute("gridkit.login", params);
            LOG.info("login returned " + usid);

            // change admin's password
            params.clear();
            params.add(usid);
            params.add("admin");
            params.add("hello");
            ret = xmlrpc.execute("gridkit.changePassword", params);
            LOG.info("changePassword returned " + ret);

            // logout again
            params.clear();
            params.add(usid);
            ret = xmlrpc.execute("gridkit.logout", params);
            LOG.info("logout " + usid + " " + ret);

            // test new password with login
            params.clear();
            params.add("admin");
            params.add("hello");
            usid = (String) xmlrpc.execute("gridkit.login", params);
            LOG.info("login returned " + usid);

            // logout again
            params.clear();
            params.add(usid);
            ret = xmlrpc.execute("gridkit.logout", params);
            LOG.info("logout " + usid + " " + ret);

            // now try a bogus password for admin and force an error
            params.clear();
            params.add("admin");
            params.add("bogus");
            usid = (String) xmlrpc.execute("gridkit.login", params);
            LOG.info("bogus login attempt returned " + usid + ((usid.equals("LOGIN-ERROR")) ? " which is cool" : " which is not cool"));

            // now see if bogus usid allows me to create a user
            params.clear();
            params.add(usid);
            params.add("newbie");
            ret = xmlrpc.execute("gridkit.addUser", params);
            LOG.info("bogus addUser attempt returned " + ret + (((Boolean) ret) ? " which is not cool" : " which is cool"));

            // now login as admin to create newbie
            params.clear();
            params.add("admin");
            params.add("hello");
            usid = (String) xmlrpc.execute("gridkit.login", params);
            LOG.info("login returned " + usid);

            // addUser newbie with verified admin usid
            params.clear();
            params.add(usid);
            params.add("newbie");
            ret = xmlrpc.execute("gridkit.addUser", params);
            LOG.info("addUser of newbie returned " + ret);

            // this time don't logout, see if multi session works
            // for newbie to login and changePassword from default
            params.clear();
            params.add("newbie");
            params.add("newbie");
            usid = (String) xmlrpc.execute("gridkit.login", params);
            params.clear();
            params.add(usid);
            params.add("newbie");
            params.add("newpass");
            ret = xmlrpc.execute("gridkit.changePassword", params);
            LOG.info("newbie login and changePassword returned " + ret);

            // now send a jar to newbies new session

            URL u = ViskitGlobals.instance().getViskitApplicationClassLoader().getResource("diskit/DISMover3D.class");
            
            LOG.info("Opening " + u);
            u = new URI((u.getFile().split("!"))[0].trim()).toURL();
            File jar = new File(u.getFile());
            LOG.info("Opening " + jar);
            java.io.InputStream fis = u.openStream();
            long fileSize = fis.available();
            LOG.info("which is " + fileSize + " bytes");
            byte[] buf = new byte[1024];
            int chunks = (int) (fileSize / 1024L + (fileSize % 1024L > 0L ? 0L : -1L));
            LOG.info("into " + chunks + 1 + " of " + buf.length);
            while (chunks > -1) {
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
                        LOG.error("File io error");
                    }
                } else {
                    outBuf = buf;
                }

                LOG.info("read in " + readIn);

                params.add(outBuf);
                params.add(chunks);
                ret = xmlrpc.execute("gridkit.transferJar", params);
                LOG.info("Transferred " + ret + " bytes in chunk # " + chunks);
                chunks--;
            }

        } catch (XmlRpcException | IOException | URISyntaxException ex) {
//            java.util.logging.Logger.getLogger(TestGridkitLogin.class.getName()).log(Level.SEVERE, null, ex);
            LOG.error("Exception {}", ex);
        }

    }

    /**
     * To set up a server, first run the ant gridkit-jar target, then on the
     * command line, navigate to the Viskit base directory and :
     * "java -Djava.endorsed.dirs=dist/lib/endorsed -jar dist/gridkit.jar"
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            TestGridkitLogin test = new TestGridkitLogin(args[0], Integer.parseInt(args[1]));
            test.start();
        } catch (Exception e) {
            LOG.error(e);
        }
    }
}
