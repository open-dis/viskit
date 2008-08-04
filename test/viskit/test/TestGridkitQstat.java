/*
 * TestGridkitQstat.java
 *
 * Created on March 27, 2006, 11:00 AM
 *
 * Check SGE qstat
 */
package viskit.test;

import java.util.Vector;
import org.apache.xmlrpc.XmlRpcClientLite;

/**
 *
 * @author Rick Goldberg
 * @version $Id: TestGridkitQstat.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class TestGridkitQstat extends Thread {

    XmlRpcClientLite xmlrpc;

    /** 
     * Creates a new instance of TestGridkitLogin
     * To set up a server, run the ant gridkit-jar
     * target, then just "java -jar dist/gridkit.jar"
     */
    public TestGridkitQstat(String server, int port) throws Exception {
        xmlrpc = new XmlRpcClientLite(server, port);
    }

    @Override
    public void run() {
        Vector<Object> params = new Vector<Object>();
        String user = "newbie";
        String passwd = "newpass";
        String usid = "";
        Object ret;
        try {
            params.clear();
            params.add(user);
            params.add(passwd);
            usid = (String) xmlrpc.execute("gridkit.login", params);
            System.out.println("login returned " + usid);

            // check queue every 2 seconds for 1 minute
            for (int i = 0; i < 30; i++) {
                params.clear();
                params.add(usid);
                System.out.println("=======================================");
                System.out.println("In console format");
                System.out.println(xmlrpc.execute("gridkit.qstat", params));
                System.out.println("In XML format");
                System.out.println(xmlrpc.execute("gridkit.qstatXML", params));
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    ;
                }
                System.out.println("=======================================");
            }

            // logout this usid
            params.clear();
            params.add(usid);
            ret = xmlrpc.execute("gridkit.logout", params);
            System.out.println("logged out");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        try {
            TestGridkitQstat test = new TestGridkitQstat(args[0], Integer.parseInt(args[1]));
            test.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
