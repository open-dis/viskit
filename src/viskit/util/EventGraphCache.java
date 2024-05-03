/*
 * Program:      Viskit Discrete Event Simulation (DES) Tool
 *
 * Author(s):    Terry Norbraten
 *               http://www.nps.edu and http://www.movesinstitute.org
 *
 * Created:      16 DEC 2007
 *
 * Filename:     EventGraphCache.java
 *
 * Compiler:     JDK1.6
 * O/S:          Windows XP Home Ed. (SP2)
 *
 * Description:  Set of utility methods for cacheing a List<String> of
 *               EventGraph paths
 *
 * References:
 *
 * URL:
 *
 * Requirements: 1)
 *
 * Assumptions:  1)
 *
 * TODO:
 *
 * Copyright (c) 1995-2009 held by the author(s).  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer
 *       in the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the names of the Naval Postgraduate School (NPS)
 *       Modeling Virtual Environments and Simulation (MOVES) Institute
 *       (http://www.nps.edu and http://www.movesinstitute.org)
 *       nor the names of its contributors may be used to endorse or
 *       promote products derived from this software without specific
 *       prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package viskit.util;

import edu.nps.util.LogUtils;
import java.io.File;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import viskit.VGlobals;

/**
 * Set of utility methods for caching a List&lt;File&gt; of EventGraph paths
 * @version $Id$
 * <p>
 *   <b>History:</b>
 *   <pre><b>
 *     Date:     17 DEC 2007
 *     Time:     0108Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=viskit.EventGraphCache">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Initial
 *
 *     Date:     24 JUN 2008
 *     Time:     1832Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=viskit.EventGraphCache">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Twas bad using Strings to hold file/directory path info. Now
 *                  using File and URL objects to better deal with whitespace in
 *                  a directory/file path name
 *
 *     Date:     23 JUL 2008
 *     Time:     0930Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=viskit.EventGraphCache">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Made a singleton to deal with new project creation tasks
 *   </b></pre>
 *
 * @author <a href="mailto:tdnorbra@nps.edu">Terry Norbraten</a>
 */
public class EventGraphCache {

    static Logger log = LogUtils.getLogger(EventGraphCache.class);

    /** The jdom.Document object of the assembly file */
    private Document assemblyDocument;

    /**
     * The names and file locations of the the event graph files and image files
     * being linked to in the AnalystReport
     */
    private List<String> eventGraphNamesList;
    private List<File> eventGraphFilesList;
    private List<File> eventGraphImageFilesList;

    private final String EVENT_GRAPH_IMAGE_DIR =
            VGlobals.instance().getCurrentViskitProject().getAnalystReportEventGraphImagesDir().getPath();
    private Element entityTable;
    private static EventGraphCache me;

    public static synchronized EventGraphCache instance() {
        if (me == null) {
            me = new EventGraphCache();
        }
        return me;
    }

    private EventGraphCache() {
        eventGraphNamesList  = new LinkedList<>();
        eventGraphFilesList  = new LinkedList<>();
        eventGraphImageFilesList = new LinkedList<>();
    }

    /**
     * Converts a loaded assy file into a Document
     *
     * @param assyFile the assembly file loaded
     */
    public void setAssemblyFileDocument(File assyFile) {
        assemblyDocument = loadXML(assyFile);
    }

    /**
     * Creates the entity table for an analyst report xml object.  Also aids in
     * opening EG files that are a SimEntity node of an Assy file
     *
     * @param assyFile the assembly file loaded
     */
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    public void makeEntityTable(File assyFile) {

        setAssemblyFileDocument(assyFile);

        entityTable = new Element("EntityTable");

        // Clear the cache if currently full
        if (!getEventGraphNamesList().isEmpty()) {
            getEventGraphNamesList().clear();
        }
        if (!getEventGraphFilesList().isEmpty()) {
            getEventGraphFilesList().clear();
        }
        if (!getEventGraphImageFilesList().isEmpty()) {
            getEventGraphImageFilesList().clear();
        }

        setEventGraphFiles(VGlobals.instance().getCurrentViskitProject().getEventGraphsDir());

        Element localRootElement = assemblyDocument.getRootElement();
        List<Element> simEntityList = localRootElement.getChildren("SimEntity");

        // Only those entities defined via SMAL
        for (Element temp : simEntityList) {
            Element tableEntry = new Element("SimEntity");
            List<Element> entityParams = temp.getChildren("MultiParameter");

            for (Element param : entityParams) {
                if (param.getAttributeValue("type").equals("diskit.SMAL.EntityDefinition")) {
                    tableEntry.setAttribute("name", temp.getAttributeValue("name"));
                    tableEntry.setAttribute("fullyQualifiedName", temp.getAttributeValue("type"));
                    getEntityTable().addContent(tableEntry);
                }
            }
        }

    }

    /**
     * Loads an XML document file for processing
     *
     * @param xmlFileName the location of the file to load as a Document
     * @return the document object of the loaded XML
     */
    public Document loadXML(String xmlFileName) {
        return loadXML(new File(xmlFileName));
    }

    public Document loadXML(File xmlFile) {
        Document doc = null;
        try {
            SAXBuilder builder = new SAXBuilder();
            doc = builder.build(xmlFile);
        } catch (JDOMException | IOException ex) {
            log.error(ex);
        }
        return doc;
    }

    /**
     * Processes the 'type' value from a Viskit assembly, if it is an xml file
     * in the project's EGs directory, and adds it to the list of event graphs
     * with the proper formatting of the file's path
     *
     * @param egFile the EG file type and name to save
     */
    private void saveEventGraphReferences(File egFile) {
        log.debug("EG: " + egFile);

        // find the package seperator
        int lastSlash = egFile.getPath().lastIndexOf(File.separator);
        int pos = egFile.getPath().lastIndexOf(".");

        String pkg = egFile.getParentFile().getName();
        String egName = egFile.getPath().substring(lastSlash + 1, pos);
        eventGraphNamesList.add(pkg + "." + egName);

        log.debug("EventGraph Name: " + egName);

        File imgFile = new File(EVENT_GRAPH_IMAGE_DIR + "/" + pkg + "/" + egName + ".xml.png");
        log.debug("Event Graph Image location: " + imgFile);

        eventGraphImageFilesList.add(imgFile);
    }

    /** Use recursion to find EventGraph XML files
     *
     * @param dir the path the EGs directory to begin evaluation
     */
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private void setEventGraphFiles(File dir) {

        Element localRootElement;
        List<Element> simEntityList;
        String egName;
        int pos;

        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                setEventGraphFiles(file);
            } else {

                localRootElement = assemblyDocument.getRootElement();
                simEntityList = localRootElement.getChildren("SimEntity");

                // Check all names against the simEntityList obtained from the Assembly
                for (Element entity : simEntityList) {
                    egName = entity.getAttributeValue("type");

                    pos = egName.lastIndexOf(".");
                    egName = egName.substring(pos + 1, egName.length());

                    if (file.getName().equals(egName + ".xml")) {
                        eventGraphFilesList.add(file);
                        saveEventGraphReferences(file);
                    }
                }
            }
        }
    }

    /** @return a JDOM document (Assembly XML file) */
    public Document getAssemblyDocument() {return assemblyDocument;}

    public List<String> getEventGraphNamesList() {return eventGraphNamesList;}
    public List<File> getEventGraphFilesList() {return eventGraphFilesList;}
    public List<File> getEventGraphImageFilesList() {return eventGraphImageFilesList;}

    /**
     * @return the entityTable
     */
    public Element getEntityTable() {
        return entityTable;
    }

} // end class file EventGraphCache.java