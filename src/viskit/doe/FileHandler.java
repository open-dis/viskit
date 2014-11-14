/*
Copyright (c) 1995-2008 held by the author(s).  All rights reserved.

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
import javax.swing.filechooser.FileFilter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

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

/**
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author  Mike Bailey
 * @since Jul 20, 2005
 * @since 11:44:06 AM
 * @version $Id$
 */
public class FileHandler {

    private static String schemaLoc = XMLValidationTool.ASSEMBLY_SCHEMA;

    public static DoeFileModel openFile(File f) throws Exception {
        SAXBuilder builder;
        Document doc;
        try {
            builder = new SAXBuilder();
            doc = builder.build(f);
        } catch (IOException e) {
            doc = null;
            throw new Exception("Error parsing or finding file " + f.getAbsolutePath());
        } catch (JDOMException e) {
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

        DoeFileModel dfm = new DoeFileModel();
        dfm.userFile = f;

        dfm.jdomDocument = doc;
        dfm.designParms = getDesignParams(doc);
        dfm.setSimEntities(getSimEntities(doc));
        dfm.paramTable = new ParamTable(dfm.getSimEntities(), dfm.designParms);

        return dfm;
    }

    // todo replace above
    public static DoeFileModel _openFileJaxb(SimkitAssembly assy, File f) {
        DoeFileModel dfm = new DoeFileModel();
        dfm.userFile = f;
        // todo dfm.jaxbRoot = assy;
        dfm.designParms = assy.getDesignParameters();
        dfm.setSimEntities(assy.getSimEntity());
        dfm.paramTable = new ParamTable(dfm.getSimEntities(), dfm.designParms);

        return dfm;
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

    public static void marshallJaxb(File of) throws Exception {
        JAXBContext jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
        FileOutputStream fos = new FileOutputStream(of);
        Marshaller m = jaxbCtx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, schemaLoc);

        //fillRoot();
        m.marshal(OpenAssembly.inst().jaxbRoot, fos);
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
    private static List<TerminalParameter> getDesignParams(Document doc) throws Exception {
        Element elm = doc.getRootElement();
        return elm.getChildren("TerminalParameter");
    }

    // TODO: JDOM v1.1 does not yet support generics
    @SuppressWarnings("unchecked")
    private static List<SimEntity> getSimEntities(Document doc) throws Exception {
        Element elm = doc.getRootElement();
        return elm.getChildren("SimEntity");
    }

    public static class FileFilterEx extends FileFilter {

        private String[] _extensions;
        private String _msg;
        private boolean _showDirs;

        public FileFilterEx(String extension, String msg) {
            this(extension, msg, false);
        }

        public FileFilterEx(String extension, String msg, boolean showDirectories) {
            this(new String[]{extension}, msg, showDirectories);
        }

        public FileFilterEx(String[] extensions, String msg, boolean showDirectories) {
            this._extensions = extensions;
            this._msg = msg;
            this._showDirs = showDirectories;
        }

        @Override
        public boolean accept(java.io.File f) {
            if (f.isDirectory()) {
                return _showDirs;
            }
            for (String _extension : _extensions) {
                if (f.getName().endsWith(_extension)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return _msg;
        }
    }
}
