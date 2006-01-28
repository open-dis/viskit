/*
 * AssemblyServer.java
 *
 * Created on January 26, 2006, 2:41 PM
 *
 */

package viskit.xsd.assembly;
import org.apache.xmlrpc.WebServer;

/**
 *
 * XML-RPC service for Gridkit
 * @author rmgold
 */

public class AssemblyServer extends WebServer {
    public static final int DEFAULT_PORT=4444;
    // keep for backwards compat until tested
    SimkitAssemblyXML2Java inst;
    
    AssemblyServer(SimkitAssemblyXML2Java inst, int port) {
        super(port);
        this.inst=inst;
        addHandler("experiment",inst);
        
    }
    
    // server is typically started here from main();
    AssemblyServer(int port) {
        super(port);
        addHandler("gridkit", new AssemblyHandler(port));
    }
    
    AssemblyServer() {
        this(DEFAULT_PORT);
    }
    
    public static void main(String[] args) {
        //TBD check if -p | --port then usage()
        AssemblyServer assemblyServer = new AssemblyServer(Integer.parseInt(args[1]));
        assemblyServer.start();
    }
}


