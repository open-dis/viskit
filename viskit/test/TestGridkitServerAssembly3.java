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
    public static final boolean verbose = false; // turns off console output from simkit
    
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
            
            // send SimpleServer.xml
            args.clear();
            args.add(usid);
            args.add(simpleServer);
            ret = xmlrpc.execute("gridkit.addEventGraph",args);
            System.out.println("addEventGraph returned "+ret);
            
            // send ServerAssembly3.xml now that deps are loaded
            // first make a jaxb tree to add DesignParameters
            SimkitAssemblyXML2Java sax2j = 
                    new SimkitAssemblyXML2Java(
                        (new URL(basedir+"examples/ServerAssembly3.xml")
                            .openStream())
                    );
            sax2j.unmarshal();
            SimkitAssemblyType root = sax2j.getRoot();
            
            // need to set unique names for TerminalParameters
            // will pick out the double from Exponential
            // and the two doubles from Gamma. Refer to 
            // ServerAssembly3.xml for documentation on
            // where they are. Ordinarily, would be done
            // by DOE panel, or by editing the XML directly.
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

            ObjectFactory of = new ObjectFactory();
            // link up the entity's parameter to the
            // range-valued design parameter. once the
            // design parameter receives arguments
            // from the design points during a run
            // then it passes by reference design arguments
            // to the entity's parameter, automatically.
            TerminalParameterType designParam0 = of.createTerminalParameter();
            designParam0.setName("exponential");
            designParam0.setType("double");
            tParamExponential.setLinkRef(designParam0);
            
            DoubleRange drange0 = of.createDoubleRange();
            drange0.setLowValue("1.0");
            drange0.setHighValue("2.0");

            designParam0.setValueRange(drange0);
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
            
            // got Terminals now hook them up
            TerminalParameterType designParam1 = of.createTerminalParameter();
            TerminalParameterType designParam2 = of.createTerminalParameter();
            designParam1.setName("gamma1");
            designParam1.setType("double");
            tParamGamma1.setLinkRef(designParam1); 
            
            DoubleRange drange1 = of.createDoubleRange();
            drange1.setLowValue("2.0");
            drange1.setHighValue("3.0");
            designParam1.setValueRange(drange1);
            
            designParam2.setName("gamma2");
            designParam2.setType("double");
            tParamGamma2.setLinkRef(designParam2);
            
            DoubleRange drange2 = of.createDoubleRange();
            drange2.setLowValue("3.0");
            drange2.setHighValue("4.0");
            designParam2.setValueRange(drange2);
            
            designParams.add(designParam1);
            designParams.add(designParam2);
            
            // quick check dp.getName() returns
            System.out.println("dp0 name "+designParam0.getName());
            System.out.println("dp1 name "+designParam1.getName());
            System.out.println("dp2 name "+designParam2.getName());
            
            
            
            // add the Experiment tag, ordinary Assemblies
            // don't usually have one, but should not make
            // a difference to Viskit if they do, however,
            // Gridkit requires one.
            
            ExperimentType exp = of.createExperiment();
            
            // A Gridkit "Sample" is one Latin Hyper Square
            // randomly drawn from the design parameter space.
            // Setting the number of Samples will create
            // that many NxN squares where N is number of
            // design parameters. Each row of the square
            // represents a design point, which will be
            // used as arguments to the design parameters
            // of the Assembly. Each design point is 
            // run as a single SGE node task, which carries out 
            // so many Replications within the task, ie,
            // a taskID is multiple runs of an Assembly
            // under the same DesignPoint with a different
            // set of random variate values each. So, if 
            // an Assembly has a high degree of built in
            // randomness, fewer Samples might need to be
            // taken and more replications, or if an Assemlby 
            // has no or few randomVariates built in, then 
            // more Samples can be generated. 
            // 
            // The Jitter is meant to be able
            // to create a fuzzy sample near some design
            // space, so that a single Sample can be sampled
            // repeatedly. One way you rotate the bulb, one
            // way you can rotate the socket. (TBD currently
            // you can only rotate the bulb, but the socket
            // is different from room to room. )
            exp.setTotalSamples("5");
            // TBD these MUST be the same, probably can refactor
            // replicationsPerDesignPoint out, for now slave it to
            // what was in Schedule.getNumberReplications(();
            exp.setReplicationsPerDesignPoint(root.getSchedule().getNumberReplications()); 
            exp.setType("latin-hypercube");
            //exp.setDebug("true");
            exp.setJitter("true");
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
            
            
            // synchronous single threaded results, uses
            // a status buffer that locks until results are
            // in, at which point a select can be performed.
            // this saves server thread resources
      
            Vector lastQueue;
            args.clear();
            args.add(usid);
            // this shouldn't block on the very first call, the queue
            // is born dirty. 
            lastQueue = (Vector) xmlrpc.execute("gridkit.getTaskQueue",args);
            
            // initial number of tasks ( can also query getRemainingTasks )
            int tasksRemaining = 5 * 3; 
            
            while ( tasksRemaining > 0 ) {
                // this will block until a task ends which could be
                // because it died, or because it completed, either way
                // check the logs returned by getResults will tell.
                args.clear();
                args.add(usid);
                Vector queue = (Vector) xmlrpc.execute("gridkit.getTaskQueue",args);
                for ( int i = 0; i < queue.size(); i ++ ) {
                    // trick: any change between queries indicates a transition at 
                    // taskID = i (well i+1 really, taskID's in SGE start at 1)
                    if ( !((Boolean) lastQueue.get(i)).equals(((Boolean) queue.get(i))) ) {
                        int sampleIndex = i / 3; // number of designPoints chosed in this experiemnt was 3
                        int designPtIndex = i % 3; // can also just use getResultByTaskID(int)
                        args.clear();
                        args.add(usid);
                        args.add(new Integer(sampleIndex));
                        args.add(new Integer(designPtIndex));
                        // this is reallllly verbose, you may wish to consider
                        // not getting the output logs unless there is a problem
                        // even so at that point the design points can be run
                        // as a regular assembly to reproduce. the complete
                        // logs are so far stored on the server.
                        
                        if (verbose) {
                            ret = xmlrpc.execute("gridkit.getResult",args);
                            System.out.println("Result returned from task "+(i+1));
                            System.out.println(ret);
                        }
                        System.out.println("DesignPointStats from task "+(i+1));
                        ret = xmlrpc.execute("gridkit.getDesignPointStats",args);
                        System.out.println(ret);
                        for (int j = 0; j < Integer.parseInt(exp.getReplicationsPerDesignPoint()); j++) {
                            System.out.println("ReplicationStats from task "+(i+1)+" replication "+j);
                            args.clear();
                            args.add(usid);
                            args.add(new Integer(sampleIndex));
                            args.add(new Integer(designPtIndex));
                            args.add(new Integer(j));
                            ret = xmlrpc.execute("gridkit.getReplicationStats",args);
                            System.out.println(ret);
                        }
                        --tasksRemaining;
                        // could also call to get tasksRemaining:
                        // ((Integer)xmlrpc.execute("gridkit.getRemainingTasks",args)).intValue();
                    }
                }
                lastQueue = queue;
                
            }
            
            args.clear();
            args.add(usid);
            System.out.println("Logging out:  "+xmlrpc.execute("gridkit.logout",args));
            
            System.out.println("Test complete!");
            
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
