/*
 * TestGridkitServerAssembly3.java
 *
 * Created on February 3, 2006, 5:57 PM
 *
 * Login to AssemblyServer with ID created in the
 * TestGridkitLogin test, send required event
 * graphs, modify the assembly to contain
 * some DesignParameters, and run it. 
 *
 * Data collection will be handled async and
 * used to update local jaxb.
 */

package viskit.test;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.net.URL;
import java.io.File;
import java.util.List;
import java.util.Vector;
import viskit.xsd.assembly.SimkitAssemblyXML2Java;
import viskit.xsd.bindings.assembly.*;
import org.apache.xmlrpc.XmlRpcClientLite;

/**
 *
 * @author Rick Goldberg
 */
public class TestGridkitServerAssembly3 extends Thread {
    XmlRpcClientLite xmlrpc;
    Vector args;
    String usid;
    String basedir;
    ByteArrayOutputStream buffer;
    Object ret;
    
    /** Creates a new instance of TestGridkitServerAssembly3 */
    public TestGridkitServerAssembly3(String server, int port) throws Exception {
        xmlrpc = new XmlRpcClientLite(server,port);
        args = new Vector();
        buffer = new ByteArrayOutputStream();
        
        // calculate the base directory, hack, know examples is next to lib
        // this of course only works in a development workspace
        
        // get the jarurl path to viskit.Main
        URL u = Thread.currentThread().getContextClassLoader().getResource("viskit/Main.class");
        // strip the injar path
        System.out.println(u);
        u = new URL((u.getFile().split("!"))[0].trim());
        String path = u.getFile();
        path.replace('\\','/');
        // gather leading paths
        String[] paths = path.split("/");
        basedir = "file:";
        // since ant's runtime classpath points to the viskit build
        // go back 3
        for ( int i = 0 ; i < paths.length - 3; i++ ) {
            basedir += paths[i]+"/";
            
        }
       
        System.out.println(basedir);
    }
    
    public void run() {
        try {
            // login: must have run TestGridkitLogin to have set up 
            // test account.
            args.clear();
            args.add("newbie");
            args.add("newpass");
            usid = (String)xmlrpc.execute("gridkit.login",args);
            // send ArrivalProcess.xml
            String arrivalProcess;
            URL u = new URL(basedir+"examples/ArrivalProcess.xml");
            InputStream is = u.openStream();
            byte[] buf = new byte[256];
            int readIn;
            while ( (readIn = is.read(buf)) > 0 ) {
                buffer.write(buf,0, readIn);
            }
            arrivalProcess = new String(buffer.toByteArray());
            
            System.out.println(arrivalProcess);
            
            args.clear();
            args.add(usid);
            args.add(arrivalProcess);
            ret = xmlrpc.execute("gridkit.addEventGraph",args);
            System.out.println("addEventGraph returned "+ret);
            
            // send SimpleServer.xml
            String simpleServer;
            u = new URL(basedir+"examples/SimpleServer.xml");
            is = u.openStream();
            buffer = new ByteArrayOutputStream();
            while ( (readIn = is.read(buf)) > 0 ) {
                buffer.write(buf,0, readIn);
            }
            simpleServer = new String(buffer.toByteArray());
            
            System.out.println(simpleServer);
            
            
            args.clear();
            args.add(usid);
            args.add(simpleServer);
            ret = xmlrpc.execute("gridkit.addEventGraph",args);
            System.out.println("addEventGraph returned "+ret);
            
            // send ServerAssembly3.xml now that deps are loaded
            // first make a jaxb tree to add DesignParameters
            SimkitAssemblyXML2Java sax2j = new SimkitAssemblyXML2Java((new URL(basedir+"examples/ServerAssembly3.xml").openStream()));
            sax2j.unmarshal();
            SimkitAssemblyType root = sax2j.getRoot();
            
            // need to set unique names for TerminalParameters
            // will pick out the double from Exponential
            // and the two doubles from Gamma
            SimEntityType arrival, server;
            List entities = root.getSimEntity();
            List designParams = root.getDesignParameters();
            
            arrival = (SimEntityType) entities.get(0);
            server = (SimEntityType) entities.get(1);
            
            List params = arrival.getParameters();
            // arrival's first param is Factory
            FactoryParameterType factParam = (FactoryParameterType) params.get(0);
            // inner multi param contains the double 
            params = factParam.getParameters();
            // inside this one
            MultiParameterType mParam = (MultiParameterType) params.get(1);
            // is the final primitive type
            params = mParam.getParameters();
            // one more deep
            mParam = (MultiParameterType) params.get(0);
            params = mParam.getParameters();
            TerminalParameterType tParamExponential = (TerminalParameterType) params.get(0);
            // now give this one a unique name
            //tParamExponential.setName("exponential");
            // add this one's reference to the DesignParameters
            ObjectFactory of = new ObjectFactory();
            // WARNING: jaxb has some funny business with droping the outter
            // tags of an Element that was created with a of.createXYZType()
            // vs. of.createXYZ(), which works as expected. This may be frustrating.
            TerminalParameterType designParam0 = of.createTerminalParameter();
            designParam0.setName("exponential");
            designParam0.setType("double");
            tParamExponential.setNameRef(designParam0);
            List content = designParam0.getContent();
            content.add("public Double[] exponential() { return new Double[] {new Double(1.0),new Double(2.0)}; }"); // TBD this gets modernized soon
            designParams.add(designParam0);
            
            // now for server's 
            params = server.getParameters();
            factParam = (FactoryParameterType) params.get(0);
            params = factParam.getParameters();
            MultiParameterType mParamParent = (MultiParameterType) params.get(1);
            params = mParamParent.getParameters();

            // one more deep
            mParam = (MultiParameterType) params.get(0);
            params = mParam.getParameters();
            TerminalParameterType tParamGamma1 = (TerminalParameterType) params.get(0);
            // back up
            params = mParamParent.getParameters();
            mParam = (MultiParameterType) params.get(1);
            params = mParam.getParameters();
            TerminalParameterType tParamGamma2 = (TerminalParameterType) params.get(0);
            
            // got Terminals now
            //tParamGamma1.setName("gamma1");
            //tParamGamma2.setName("gamma2");
            TerminalParameterType designParam1 = of.createTerminalParameter();
            TerminalParameterType designParam2 = of.createTerminalParameter();
            designParam1.setName("gamma1");
            designParam1.setType("double");
            tParamGamma1.setNameRef(designParam1); // hook them up
            
            // ugh.. this will change, getting rid of scripted design params. maybe even nameRefs.
            content = designParam1.getContent();
            content.add("public Double[] gamma1() { return new Double[] {new Double(2.0),new Double(3.0)}; }");
            designParam2.setName("gamma2");
            designParam2.setType("double");
            tParamGamma2.setNameRef(designParam2);
            content = designParam2.getContent();
            content.add("public Double[] gamma2() { return new Double[] {new Double(3.0),new Double(4.0)}; }");
            designParams.add(designParam1);
            designParams.add(designParam2);
            
            // quick check dp.getName() returns
            System.out.println("dp0 name "+designParam0.getName());
            System.out.println("dp1 name "+designParam1.getName());
            System.out.println("dp2 name "+designParam2.getName());
            
            
            
            // add the Experiment tag
            
            ExperimentType exp = of.createExperiment();
            exp.setTotalSamples("5");
            exp.setReplicationsPerDesignPoint("10");
            root.setExperiment(exp);
            
            // editing XML directly would have been less involved
            // but similar steps would likely take place in the DOE editor
            
            // create XML String from modified Assembly
            String serverAssembly3exp = sax2j.marshalToString(root);
            System.out.println(serverAssembly3exp);
            
            // send it to server
            args.clear();
            args.add(usid);
            args.add(serverAssembly3exp);
            ret = xmlrpc.execute("gridkit.setAssembly", args);
            System.out.println("setAssembly returned "+ret);
            
            // run it
            args.clear();
            args.add(usid);
            ret = xmlrpc.execute("gridkit.run", args);
            System.out.println("run returned "+ ret);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public static void main(String[] args) {
        try {
            TestGridkitServerAssembly3 test = new TestGridkitServerAssembly3(args[0], Integer.parseInt(args[1]));
            test.start();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
