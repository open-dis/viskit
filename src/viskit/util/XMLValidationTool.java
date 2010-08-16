/*
 * File:        XMLValidationTool.java
 *
 * Created on:  02 SEP 08
 *
 * Refenences:  Code borrowed from Elliotte Rusty Harold's IBM article at:
 *              http://www-128.ibm.com/developerworks/xml/library/x-javaxmlvalidapi.html,
 *              and from Olaf Meyer's posting on Google Groups - comp.text.xml 
 *              (ErrorPrinter).
 *
 * Assumptions: Connection to the internet now optional.  XML files will be 
 *              resolved to the Schema in order to shorten internal parsing
 *              validation time.
 */
package viskit.util;

// Standard Library Imports
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

// Applcation Specific Local Imports
import edu.nps.util.LogUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import viskit.ViskitConfig;

/**
 * Utility class to validate XML files against provided Schema and 
 * report errors in &lt;(file: row, column): error&gt; format.
 * @version $Id: XMLValidationTool.java 1213 2008-02-12 03:21:15Z tnorbraten $
 * <p>
 *   <b>History:</b>
 *   <pre><b>
 *     Date:     02 SEP 08
 *     Time:     1910Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=viskit.util.XMLValidationTool">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Initial
 *   </b></pre>
 * </p>
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.util.XMLValidationTool">Terry Norbraten</a>
 */
public class XMLValidationTool {

    public static final String ASSEMBLY_SCHEMA = "http://diana.nps.edu/Simkit/assembly.xsd";
    public static final String EVENT_GRAPH_SCHEMA = "http://diana.nps.edu/Simkit/simkit.xsd";
    
    /** The locally resolved location for assembly.xsd */
    public static final String LOCAL_ASSEMBLY_SCHEMA = 
            System.getProperty("user.dir") + "/Schemas/assembly.xsd";
    
    /** The locally resolved location for simkit.xsd */
    public static final String LOCAL_EVENT_GRAPH_SCHEMA = 
            System.getProperty("user.dir") + "/Schemas/simkit.xsd";
    
    private FileWriter fWriter;
    private File xmlFile, schemaFile;
    private boolean valid = true;

    /**
     * Creates a new instance of XMLValidationTool
     * @param xmlFile the scene file to validate
     * @param schema the XML schema to validate the xmlFile against
     */
    public XMLValidationTool(File xmlFile, File schema) {
        setXmlFile(xmlFile);
        setSchemaFile(schema);

        /* Through trial and error, found how to set this property by 
         * deciphering the JAXP debug readout using the -Djaxp.debug=1 JVM arg.
         * Reading the API for SchemaFactory.getInstance(String) helps too.
         */
        System.setProperty("javax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema",
                "org.apache.xerces.jaxp.validation.XMLSchemaFactory");
        LogUtils.getLogger().debug("javax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema = " +
                System.getProperty("javax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema"));
    }

    /** Will report well-formedness and any validation errors encountered
     * @return true if parsed XML file is well-formed XML 
     */
    public boolean isValidXML() {

        // 1. Lookup a factory for the W3C XML Schema language
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        // 2. Compile the schemaFile.
        // Here the schemaFile is loaded from a java.io.File, but you could use
        // a java.net.URL or a javax.xml.transform.Source instead.
        Schema schemaDoc = null;
        try {
            schemaDoc = factory.newSchema(getSchemaFile());
        } catch (SAXException ex) {
            LogUtils.getLogger().fatal("Unable to create Schema object: " + ex);
        }

        // 3. Get a validator from the schemaFile object.
        Validator validator = schemaDoc.newValidator();

        // 4. Designate an error handler and an LSResourceResolver
        validator.setErrorHandler(new MyHandler());

        // 5. Prepare to parse the document to be validated.
        InputSource src = new InputSource(getXmlFile().getAbsolutePath());
        SAXSource source = new SAXSource(src);

        // 6. Parse, validate and report any errors.
        try {
            LogUtils.getLogger().info("Validating: " + source.getSystemId());

            // Prepare error errorsLog with current DTG
            File errorsLog = new File(ViskitConfig.VISKIT_HOME_DIR + "/validationErrors.log");
            
            // New LogUtils.getLogger() each Viskit startup
            if (errorsLog.exists()) {errorsLog.delete();}
            fWriter = new FileWriter(errorsLog, true);
            Calendar cal = Calendar.getInstance();
            fWriter.write("****************************\n");
            fWriter.write(cal.getTime().toString() + "\n");
            fWriter.write("****************************\n\n");

            validator.validate(source);
            
        } catch (SAXException ex) {
            LogUtils.getLogger().fatal(source.getSystemId() + " is not well-formed XML");
            LogUtils.getLogger().fatal(ex);
        } catch (IOException ex) {
            LogUtils.getLogger().fatal(ex);
        } finally {
            try {
                // Space between file entries
                fWriter.write("\n");
                fWriter.close();
            } catch (IOException ex) {
                LogUtils.getLogger().fatal(ex);
            }
        }
        return valid;
    }

    public File getXmlFile() {
        return xmlFile;
    }

    /** Mutator method to change the X3D file to validate
     * @param file the file to set for this validator to validate
     */
    public void setXmlFile(File file) {
        xmlFile = file;
    }
    
    public File getSchemaFile() {
        return schemaFile;
    }

    public void setSchemaFile(File schema) {
        this.schemaFile = schema;
    }

    /** Inner utility class to report errors in <(file: row, column): error> 
     * format and to resolve X3D scenes to a local DTD
     */
    class MyHandler implements ErrorHandler {

        private MessageFormat message = new MessageFormat("({0}: row {1}, column {2}):\n{3}\n");
        private String msg;

        /** Stores the particular message as the result of a SAXParseException
         * encountered.
         * @param ex the particular SAXParseException used to form a message
         */
        private void setMessage(SAXParseException ex) {
            msg = message.format(new Object[]{ex.getSystemId(), new Integer(ex.getLineNumber()),
                new Integer(ex.getColumnNumber()), ex.getMessage()
            });
        }

        /** Needed to ensure that a batch of file errors get recorded
         * @param level WARNING, ERROR or FATAL error reporting levels
         */
        private void writeMessage(String level) {
            try {
                fWriter.write(level + msg + "\n");
            } catch (IOException ex) {
                LogUtils.getLogger().fatal(ex);
            }
            
            // if we got here, there is something wrong
            valid = false;
        }

        public void warning(SAXParseException ex) {
            setMessage(ex);
            writeMessage("Warning: ");
            LogUtils.getLogger().warn(msg);
        }

        /** Recoverable errors such as violations of validity contraints are
         * reported here
         * @param ex 
         */
        public void error(SAXParseException ex) {
            setMessage(ex);
            writeMessage("Error: ");
            LogUtils.getLogger().error(msg);
        }

        /**
         * @param ex 
         * @throws SAXParseException on fatal errors */
        public void fatalError(SAXParseException ex) throws SAXParseException {
            setMessage(ex);
            writeMessage("Fatal: ");
            LogUtils.getLogger().fatal(msg);
            throw ex;
        }
    }
    
} // end class file XMLValidationTool.java
