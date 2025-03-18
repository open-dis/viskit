/*
Copyright (c) 1995-2024 held by the author(s).  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other materials provided with the
      distribution.
    * Neither the names of the Naval Postgraduate School (NPS)
      Modeling, Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and https://my.nps.edu/web/moves)
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/
package viskit.doe;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import javax.swing.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import viskit.util.OpenAssembly;
import viskit.util.XMLValidationTool;
import viskit.xsd.bindings.assembly.SimkitAssembly;
import viskit.xsd.bindings.assembly.SimEntity;
import viskit.xsd.bindings.assembly.TerminalParameter;
import viskit.xsd.translator.assembly.SimkitAssemblyXML2Java;

/**
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author  Mike Bailey
 * @since Jul 20, 2005
 * @since 11:44:06 AM
 * @version $Id$
 */
public class FileHandler 
{
    static final Logger LOG = LogManager.getLogger();
    
    private static final String SCHEMA_LOC = XMLValidationTool.ASSEMBLY_SCHEMA;

    public static DoeFileModel openFile(File f) throws Exception {
        SAXBuilder builder;
        Document doc;
        try {
            builder = new SAXBuilder();
            doc = builder.build(f);
        } catch (IOException | JDOMException e) {
            doc = null;
            throw new Exception("Error parsing or finding file " + f.getAbsolutePath());
        }
        return _openFile(doc, f);
    }

    public static DoeFileModel _openFile(Document document, File file) throws Exception {
        Element element = document.getRootElement();
        if (!element.getName().equalsIgnoreCase("SimkitAssembly")) {
            throw new Exception("Root element must be named \"SimkitAssembly\".");
        }

        DoeFileModel doeFileModel = new DoeFileModel();
        doeFileModel.userFile = file;

        doeFileModel.jdomDocument = document;
        doeFileModel.designParameters = getDesignParameters(document);
        doeFileModel.setSimEntities(getSimEntities(document));
        doeFileModel.parameterTable = new ParameterTable(doeFileModel.getSimEntities(), doeFileModel.designParameters);

        return doeFileModel;
    }

    // todo replace above
    public static DoeFileModel _openFileJaxb(SimkitAssembly simkitAssembly, File f) {
        DoeFileModel dfm = new DoeFileModel();
        dfm.userFile = f;
        // todo dfm.jaxbRoot = assembly;
        dfm.designParameters = simkitAssembly.getDesignParameters();
        dfm.setSimEntities(simkitAssembly.getSimEntity());
        dfm.parameterTable = new ParameterTable(dfm.getSimEntities(), dfm.designParameters);

        return dfm;
    }

    public static Document unmarshallJdom(File f) throws IOException, JDOMException {
        SAXBuilder builder = new SAXBuilder();
        return builder.build(f);
    }

    public static void marshallJdom(File of, Document doc, boolean omit) throws IOException, JDOMException {
        XMLOutputter xmlOut = new XMLOutputter();
        Format form = Format.getPrettyFormat();
        
        if (omit)
            form.setOmitDeclaration(true); // lose the <?xml at the top
        
        xmlOut.setFormat(form);

        try (Writer fw = new FileWriter(of)) {
            xmlOut.output(doc, fw);
        }
    }

    public static void marshallJaxb(File of) throws Exception
    {
        JAXBContext jaxbContext = JAXBContext.newInstance(SimkitAssemblyXML2Java.ASSEMBLY_BINDINGS);
        try (Writer fileWriter = new FileWriter(of)) {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, SCHEMA_LOC);
            marshaller.marshal(OpenAssembly.inst().jaxbRoot, fileWriter);
        }
    }

    /** Called from the DoeController. Not currently used
     * 
     * @param file the assembly file to run
     * @param title the title of the run
     * @param mainFrame the parent frame
     */
    public static void runFile(File file, String title, JFrame mainFrame) {
        try {
            JobLauncher jobLauncher = new JobLauncher(true, file.getAbsolutePath(), title, mainFrame); // TODO broken
        } 
        catch (Exception e) {
            LOG.error("runFile(" + file.getAbsolutePath() + ", " + title + ") exception: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    public static void runFile(File fil, String title, JobLauncherTab2 jobLauncher) {
        jobLauncher.setFile(fil.getAbsolutePath(), title);
    }

    // TODO: JDOM v1.1 does not yet support generics
    @SuppressWarnings("unchecked")
    private static List<TerminalParameter> getDesignParameters(Document document) throws Exception {
        Element element = document.getRootElement();
        return element.getChildren("TerminalParameter");
    }

    // TODO: JDOM v1.1 does not yet support generics
    @SuppressWarnings("unchecked")
    private static List<SimEntity> getSimEntities(Document document) throws Exception {
        Element element = document.getRootElement();
        return element.getChildren("SimEntity");
    }
}
