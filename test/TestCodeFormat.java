/*
 * TestCodeFormat.java
 *
 * Created on November 14, 2005, 1:05 PM
 *
 */

package viskit.test;
import viskit.xsd.bindings.eventgraph.*;
import viskit.xsd.translator.SimkitXML2Java;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 *
 * @author Rick Goldberg
 */

public class TestCodeFormat extends Thread {

    String testFile;
    JAXBContext jaxbCtx;
    SimkitXML2Java sx2j;
    SimEntity root;
    InputStream inputStream;
    ByteArrayOutputStream bufferOut;
    ByteArrayInputStream bufferIn;
    
    /** Creates a new instance of TestCodeFormat */
    public TestCodeFormat(String testFile) {
        this.testFile = new String(testFile);
        bufferOut = new ByteArrayOutputStream();
        bufferIn = new ByteArrayInputStream(new byte[0]);
        try {
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings");
        } catch ( javax.xml.bind.JAXBException jaxbe ) {
            jaxbe.printStackTrace();
        }
    }
    
    public void run() {
        try {
            inputStream = openEventGraph(testFile);
            loadEventGraph(inputStream);
            showXML(System.out);
            // reopen it 
            inputStream.close();
            inputStream = openEventGraph(testFile);
            showJava(inputStream);
            // add some code block
            modifyEventGraph();
            showXML(System.out);
            System.out.println("Code inserted was:");
            System.out.println(root.getCode());
            showXML(bufferOut);
            byte[] buff = bufferOut.toByteArray();
            bufferIn = new ByteArrayInputStream(buff);
            showJava(bufferIn);
            
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
    
    InputStream openEventGraph(String filename) {
        InputStream is = null;
        try {
	    is = new FileInputStream(filename);
	} catch ( Exception e ) {
            System.out.println("Failed to open "+filename);
	    e.printStackTrace();
	} 
        return is;
    }
    
    void loadEventGraph(InputStream is) {
        Unmarshaller u;
	try {
	    u = jaxbCtx.createUnmarshaller();
	    this.root = (SimEntity) u.unmarshal(is);
	} catch (Exception e) { e.printStackTrace(); }
        
    }
    
    void showXML(OutputStream out) {
        try {
            Marshaller m = jaxbCtx.createMarshaller();
            // even setting this to false, thereby losing all
            // tabs in the XML, the CDATA still appears to preserve
            // formatting.
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            m.marshal(root,out);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        
    }
    
    void showJava(InputStream is) throws Exception {
        sx2j = new SimkitXML2Java(is);
        sx2j.unmarshal();
        System.out.println( sx2j.translate() );
    }
    
    
    // add a Code block via jaxb, which 
    // gives a handy setCode(String code)
    // method rather than having to create
    // a <Code> element then setValue(String)
    // on that.
    // String here is 
    //     "System.out.println("this is a test");
    //      if ( true ) {
    //          System.out.println();
    //      }"
    //  
    void modifyEventGraph() throws Exception {

        root.setCode(""+
                "System.out.println(\"this is a test\");\n" +
                "if ( true ) {\n" +
                "\tSystem.out.println();\n" +
                "}"
        );
        
    }
    
    public static void main(String[] args) {
        TestCodeFormat tcf = new TestCodeFormat(args[0]);
        tcf.start();
    }
    
}
