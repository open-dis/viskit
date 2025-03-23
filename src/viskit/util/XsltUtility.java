package viskit.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** This class was written by CDR Duane Davis for the AUV Workbench.
 * It was copied to this application to perform handy XSLT conversions.
 *
 * @author Duane Davis
 * @since March 11, 2004, 4:55 PM
 * @version $Id$
 */
public class XsltUtility 
{
    static final Logger LOG = LogManager.getLogger();

    /**
     * Runs an XSL Transformation on an XML file and writes the result to another file
     *
     * @param inputFilePath XML file to be transformed
     * @param outputFilePath output file for transformation results
     * @param xsltStylesheetFilePath XSLT to utilize for transformation
     *
     * @return the resulting transformed XML file
     */
    public static boolean runXsltStylesheet(String inputFilePath, String outputFilePath, String xsltStylesheetFilePath) 
    {
        File xsltStylesheetFile = new File(xsltStylesheetFilePath);
        LOG.info("runXslt() commence stylesheet conversion\n      {}\n      {}\n      {}",
                inputFilePath, outputFilePath,  // TODO actual absolute paths from File
                xsltStylesheetFile.getAbsolutePath());
        try {
            // Force Xalan for this TransformerFactory
            System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
            TransformerFactory factory = TransformerFactory.newInstance();

            // Look in the viskit.jar file for this XSLT
            Templates template = factory.newTemplates(new StreamSource(XsltUtility.class.getClassLoader().getResourceAsStream(xsltStylesheetFilePath)));
            Transformer xFormer = template.newTransformer();
            Source source = new StreamSource(new FileInputStream(inputFilePath));
            Result result = new StreamResult(new FileOutputStream(outputFilePath));
            xFormer.transform(source, result);
        } 
        catch (FileNotFoundException e) {
            LOG.error("Unable to load file for XSL Transformation\n" +
                    "   Input file : " + inputFilePath + "\n" +
                    "   Output file: " + outputFilePath + "\n" +
                    "   XSLT file  : " + xsltStylesheetFilePath);
            return false;
        }
        catch (TransformerConfigurationException e) {
            LOG.error("Unable to configure transformer for XSL Transformation");
            return false;
        } 
        catch (TransformerException e) {
            LOG.error("Exception during XSL Transformation");
            return false;
        }
        return true;
    }
} // end class file XsltUtility.java