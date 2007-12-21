import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.util.Vector;
import org.apache.xmlrpc.*;

public class TestFeeder extends Thread {

	File f;
	FileReader fr;
	PrintWriter out;
	BufferedReader br;
	XmlRpcClientLite rpc;
	
	public static void main(String[] args) {
		TestFeeder t = new TestFeeder(args[0]);
		t.start();
	}
	
	public TestFeeder(String file) {
	    try{
		f = new File(file);
		rpc = new XmlRpcClientLite("localhost",4444);
	
		fr = new FileReader(f);
		br = new BufferedReader(fr);
	
	    } catch (Exception e) { e.printStackTrace(); }

	}

	public void run() {
	    try {
		String line;
		StringWriter data;
		PrintWriter out;

		data = new StringWriter();
		out = new PrintWriter(data);
                
                while( (line = br.readLine()) != null ) {
                    out.println('\t'+line);
		    System.out.println('\t'+line);
                }
                out.close();
		
		//send file to front end
                java.util.Vector parms = new Vector();
                parms.add(data.toString());
                Object o = rpc.execute("experiment.setAssembly", parms);
		System.out.println("setAssembly returned "+o);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            
	}
}
