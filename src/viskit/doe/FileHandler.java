/*
Copyright (c) 1995-2016 held by the author(s).  All rights reserved.

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
      Modeling Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and http://www.movesinstitute.org)
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import javax.swing.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

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
public class FileHandler {

    private static String schemaLocation = XMLValidationTool.ASSEMBLY_SCHEMA;

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

    public static DoeFileModel _openFile(Document doc, File f) throws Exception {
        Element elm = doc.getRootElement();
        if (!elm.getName().equalsIgnoreCase("SimkitAssembly")) {
            throw new Exception("Root element must be named \"SimkitAssembly\".");
        }

        DoeFileModel doeFileModel = new DoeFileModel();
        doeFileModel.userFile = f;

        doeFileModel.jdomDocument = doc;
        doeFileModel.designParmeters = getDesignParameters(doc);
        doeFileModel.setSimEntities(getSimEntities(doc));
        doeFileModel.paramTable = new ParamTable(doeFileModel.getSimEntities(), doeFileModel.designParmeters);

        return doeFileModel;
    }

    // todo replace above
    public static DoeFileModel _openFileJaxb(SimkitAssembly assembly, File f) {
        DoeFileModel doeFileModel = new DoeFileModel();
        doeFileModel.userFile = f;
        // todo dfm.jaxbRoot = assembly;
        doeFileModel.designParmeters = assembly.getDesignParameters();
        doeFileModel.setSimEntities(assembly.getSimEntity());
        doeFileModel.paramTable = new ParamTable(doeFileModel.getSimEntities(), doeFileModel.designParmeters);

        return doeFileModel;
    }

    public static Document unmarshallJdom(File f) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        return builder.build(f);
    }

    public static void marshallJdom(File of, Document doc) throws Exception {
        XMLOutputter xmlOut = new XMLOutputter();
        Format form = Format.getPrettyFormat();
        form.setOmitDeclaration(true); // lose the <?xml at the top
        xmlOut.setFormat(form);

        FileOutputStream fow = new FileOutputStream(of);
        xmlOut.output(doc, fow);
    }

    public static void marshallJaxb(File of) throws Exception
	{
        FileOutputStream fos = new FileOutputStream(of);
        JAXBContext jaxbContext    = JAXBContext.newInstance(SimkitAssemblyXML2Java.ASSEMBLY_BINDINGS);
        Marshaller  jaxbMarshaller = jaxbContext.createMarshaller();
		// https://dersteps.wordpress.com/2012/08/22/enable-jaxb-event-handling-to-catch-errors-as-they-happen
		jaxbMarshaller.setEventHandler(new ValidationEventHandler() {
			@Override
			public boolean handleEvent(ValidationEvent validationEvent) {
				System.out.println("Marshaller event handler says: " + validationEvent.getMessage() + 
						           " (Exception: " + validationEvent.getLinkedException() + ")");
				return false;
			}
		});
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, schemaLocation);

        //fillRoot();
        jaxbMarshaller.marshal(OpenAssembly.inst().jaxbRoot, fos);
    }

    public static void runFile(File fil, String title, JFrame mainFrame) {
        try {
            new JobLauncher(true, fil.getAbsolutePath(), title, mainFrame);      // broken
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runFile(File fil, String title, JobLauncherTab2 jobLauncher) {
        jobLauncher.setFile(fil.getAbsolutePath(), title);
    }

    // TODO: JDOM v1.1 does not yet support generics
    @SuppressWarnings("unchecked")
    private static List<TerminalParameter> getDesignParameters(Document doc) throws Exception {
        Element elm = doc.getRootElement();
        return elm.getChildren("TerminalParameter");
    }

    // TODO: JDOM v1.1 does not yet support generics
    @SuppressWarnings("unchecked")
    private static List<SimEntity> getSimEntities(Document doc) throws Exception {
        Element elm = doc.getRootElement();
        return elm.getChildren("SimEntity");
    }
}
