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

import org.apache.logging.log4j.Logger;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

// Local imports
import edu.nps.util.LogUtils;
import javax.xml.transform.stream.StreamSource;

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
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.util.XMLValidationTool">Terry Norbraten</a>
 */
public class XMLValidationTool {

    public static final String ASSEMBLY_SCHEMA = "http://diana.nps.edu/Simkit/assembly.xsd";
    public static final String EVENT_GRAPH_SCHEMA = "http://diana.nps.edu/Simkit/simkit.xsd";

    /** The locally resolved location for assembly schema */
    public static final String LOCAL_ASSEMBLY_SCHEMA = "xsd/assembly.xsd";

    /** The locally resolved location for simkit schema */
    public static final String LOCAL_EVENT_GRAPH_SCHEMA = "xsd/simkit.xsd";

    static final Logger LOG = LogUtils.getLogger(XMLValidationTool.class);

    private FileWriter fWriter;
    private String xmlFile, schemaFile;
    private boolean valid;

    /**
     * Creates a new instance of XMLValidationTool
     * @param xmlFile the scene file to validate
     * @param schema the XML schema to validate the xmlFile against
     */
    public XMLValidationTool(String xmlFile, String schema) {
        setXmlFile(xmlFile);
        setSchemaFile(schema);

        /* Through trial and error, found how to set this property by
         * deciphering the JAXP debug readout using the -Djaxp.debug=1 JVM arg.
         * Reading the API for SchemaFactory.getInstance(String) helps too.
         */
        System.setProperty("javax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema",
                "org.apache.xerces.jaxp.validation.XMLSchemaFactory");
        LOG.debug("javax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema = " +
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
            schemaDoc = factory.newSchema(new StreamSource(XMLValidationTool.class.getClassLoader().getResourceAsStream(getSchemaFile())));
        } catch (SAXException ex) {
            LOG.fatal("Unable to create Schema object: {}", ex);
        }

        // 3. Get a validator from the schemaFile object.
        if (schemaDoc != null) {
            Validator validator = schemaDoc.newValidator();

            // 4. Designate an error handler and an LSResourceResolver
            validator.setErrorHandler(new MyHandler());

            // 5. Prepare to parse the document to be validated.
            InputSource src = new InputSource(getXmlFile());
            SAXSource source = new SAXSource(src);

            // 6. Parse, validate and report any errors.
            try {
                LOG.info("Validating: " + source.getSystemId());

                // Prepare error errorsLog with current DTG
                File errorsLog = new File(ViskitConfig.VISKIT_LOGS_DIR + "/validationErrors.log");
                errorsLog.setWritable(true, false);

                // New LogUtils.getLogger() each Viskit startup
                if (errorsLog.exists()) {errorsLog.delete();}
                fWriter = new FileWriter(errorsLog, true);
                Calendar cal = Calendar.getInstance();
                fWriter.write("****************************\n");
                fWriter.write(cal.getTime().toString() + "\n");
                fWriter.write("****************************\n\n");

                validator.validate(source);
                valid = true;

            } catch (SAXException ex) {
                LOG.fatal(source.getSystemId() + " is not well-formed XML");
                LOG.fatal(ex);
                valid = false;
            } catch (IOException ex) {
                LOG.fatal(ex);
                valid = false;
            } finally {
                try {
                    // Space between file entries
                    fWriter.write("\n");
                    fWriter.close();
                } catch (IOException ex) {
                    LOG.fatal(ex);
                }
            }
        }
        return valid;
    }

    public String getXmlFile() {
        return xmlFile;
    }

    /** Mutator method to change the X3D file to validate
     * @param file the file to set for this validator to validate
     */
    public final void setXmlFile(String file) {
        this.xmlFile = file;
    }

    public String getSchemaFile() {
        return schemaFile;
    }

    public final void setSchemaFile(String schema) {
        this.schemaFile = schema;
    }

    /** Inner utility class to report errors in <(file: row, column): error>
     * format and to resolve X3D scenes to a local DTD
     */
    class MyHandler implements ErrorHandler {

        private final MessageFormat message = new MessageFormat("({0}: row {1}, column {2}):\n{3}\n");
        private String msg;

        /** Stores the particular message as the result of a SAXParseException
         * encountered.
         * @param ex the particular SAXParseException used to form a message
         */
        private void setMessage(SAXParseException ex) {
            msg = message.format(new Object[]{ex.getSystemId(),
                ex.getLineNumber(),
                ex.getColumnNumber(),
                ex.getMessage()});
        }

        /** Needed to ensure that a batch of file errors get recorded
         * @param level WARNING, ERROR or FATAL error reporting levels
         */
        private void writeMessage(String level) {
            try {
                fWriter.write(level + msg + "\n");
            } catch (IOException ex) {
                LOG.fatal(ex);
            }

            // if we got here, there is something wrong
            valid = false;
        }

        @Override
        public void warning(SAXParseException ex) {
            setMessage(ex);
            writeMessage("Warning: ");
            LOG.warn(msg);
        }

        /** Recoverable errors such as violations of validity constraints are
         * reported here
         * @param ex
         */
        @Override
        public void error(SAXParseException ex) {
            setMessage(ex);
            writeMessage("Error: ");
            LOG.error(msg);
        }

        /**
         * @param ex
         * @throws SAXParseException on fatal errors */
        @Override
        public void fatalError(SAXParseException ex) throws SAXParseException {
            setMessage(ex);
            writeMessage("Fatal: ");
            LOG.fatal(msg);
            throw ex;
        }
    }

} // end class file XMLValidationTool.java
