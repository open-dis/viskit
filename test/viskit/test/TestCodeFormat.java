/*
 * TestCodeFormat.java
 *
 * Created on November 14, 2005, 1:05 PM
 *
 */
package viskit.test;

import viskit.xsd.bindings.eventgraph.*;
import viskit.xsd.translator.eventgraph.SimkitXML2Java;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import javax.xml.bind.JAXBException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Test case
 * @author Rick Goldberg
 */
public class TestCodeFormat extends Thread 
{
    static final Logger LOG = LogManager.getLogger();

    String testFile;
    JAXBContext jaxbCtx;
    SimkitXML2Java sx2j;
    SimEntity root;
    InputStream inputStream;
    ByteArrayOutputStream bufferOut;
    ByteArrayInputStream bufferIn;

    /** Creates a new instance of TestCodeFormat
     * @param testFile 
     */
    public TestCodeFormat(String testFile) {
        this.testFile = testFile;
        bufferOut = new ByteArrayOutputStream();
        bufferIn = new ByteArrayInputStream(new byte[0]);
        try {
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
        } catch ( JAXBException jaxbe ) {
            jaxbe.printStackTrace(System.err);
        }
    }

    @Override
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
            e.printStackTrace(System.err);
        }
    }

    InputStream openEventGraph(String filename) {
        InputStream is = null;
        try {
	    is = new FileInputStream(filename);
	} catch ( FileNotFoundException e ) {
            System.err.println("Failed to open "+filename);
	    e.printStackTrace(System.err);
	}
        return is;
    }

    void loadEventGraph(InputStream is) {
        Unmarshaller u;
	try {
	    u = jaxbCtx.createUnmarshaller();
	    this.root = (SimEntity) u.unmarshal(is);
	} catch (JAXBException e) { e.printStackTrace(System.err); }

    }

    void showXML(OutputStream out) {
        try {
            Marshaller m = jaxbCtx.createMarshaller();
            // even setting this to false, thereby losing all
            // tabs in the XML, the CDATA still appears to preserve
            // formatting.
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(root,out);
        } catch ( JAXBException e ) {
            e.printStackTrace(System.err);
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
      System.out.println("wd=>>>>>>>>>>> "+System.getProperty("user.dir"));
        TestCodeFormat tcf = new TestCodeFormat(args[0]);
        tcf.start();
    }

}
