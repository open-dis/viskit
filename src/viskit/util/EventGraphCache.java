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
package viskit.util;


import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import viskit.ViskitGlobals;
import viskit.doe.FileHandler;
import static viskit.model.AnalystReportModel.NAME;
import static viskit.model.AnalystReportModel.TYPE;

/**
 * Set of utility methods for caching a List&lt;File&gt; of EventGraph paths
 *
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
public class EventGraphCache 
{
    static final Logger LOG = LogManager.getLogger();;
    
    private static EventGraphCache me;

    /**
     * The names and file locations of the the event graph files and image files
     * being linked to in the AnalystReport
     */
    private final List<String> eventGraphNamesList;
    private final List<File>   eventGraphFilesList;
    private final List<File>   eventGraphImageFilesList;

    /** The jdom.Document object of the assembly file */
    private Document assemblyDocument;
    private Element entityTable;

    public static synchronized EventGraphCache instance() 
    {
        if (me == null) {
            me = new EventGraphCache();
        }
        return me;
    }

    private EventGraphCache() 
    {
        eventGraphNamesList      = new LinkedList<>();
        eventGraphFilesList      = new LinkedList<>();
        eventGraphImageFilesList = new LinkedList<>();
    }

    /**
     * Converts a loaded assembly file into a Document
     *
     * @param assemblyFile the assembly file loaded
     */
    public void setAssemblyFileDocument(File assemblyFile) {
        assemblyDocument = loadXML(assemblyFile);
    }

    /**
     * Creates the entity table for an analyst report xml object. Also aids in
     * opening Event Graph files that are a SimEntity node of an Assembly file
     *
     * @param assemblyFile the assembly file loaded
     */
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    public void makeEntityTable(File assemblyFile) {

        setAssemblyFileDocument(assemblyFile);

        entityTable = new Element("EntityTable");

        // Clear the cache if currently full
        if (!getEventGraphNamesList().isEmpty())
            getEventGraphNamesList().clear();
        if (!getEventGraphFilesList().isEmpty())
            getEventGraphFilesList().clear();
        if (!getEventGraphImageFilesList().isEmpty())
            getEventGraphImageFilesList().clear();

        setEventGraphFiles(ViskitGlobals.instance().getViskitProject().getEventGraphsDirectory());

        Element localRootElement = assemblyDocument.getRootElement();
        List<Element> simEntityList = localRootElement.getChildren("SimEntity");

        Element tableEntry;
        List<Element> entityParams;
        // Only those entities defined via SMAL
        for (Element temp : simEntityList) {
            tableEntry = new Element("SimEntity");
            entityParams = temp.getChildren("MultiParameter");

            for (Element param : entityParams) {
                if (param.getAttributeValue(TYPE).equals("diskit.SMAL.EntityDefinition")) {
                    tableEntry.setAttribute(NAME, temp.getAttributeValue(NAME));
                    tableEntry.setAttribute("fullyQualifiedName", temp.getAttributeValue(TYPE)); // TODO ???
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
        Document doc;
        try {
            doc = FileHandler.unmarshallJdom(xmlFile);
        } catch (JDOMException | IOException ex) {
            LOG.error(ex);
            return null;
        }
        return doc;
    }

    /**
     * Processes the 'type' value from a Viskit assembly, if it is an xml file
     * in the project's Event Graphs directory, and adds it to the list of event graphs
     * with the proper formatting of the file's path
     *
     * @param eventGraphFile the Event Graph file type and name to save
     */
    private void saveEventGraphReferences(File eventGraphFile)
    {
        LOG.debug("Event Graph: {}", eventGraphFile);

        // find the package seperator
        int lastSlashIndex = eventGraphFile.getPath().lastIndexOf(File.separator);
        int lastDotIndex   = eventGraphFile.getPath().lastIndexOf(".");

        String    packageName = eventGraphFile.getParentFile().getName();
        String   assemblyName = ViskitGlobals.instance().getActiveAssemblyModel().getName();
        String eventGraphName = eventGraphFile.getPath().substring(lastSlashIndex + 1, lastDotIndex);
        eventGraphNamesList.add(packageName + "." + eventGraphName);

        LOG.debug("EventGraph Name: {}", eventGraphName);

        String eventGraphImageDirectory =
            ViskitGlobals.instance().getViskitProject().getAnalystReportEventGraphImagesDirectory().getPath();

        String eventGraphImageFileName =  "EventGraph_" + eventGraphName + ".xml.png"; // assemblyName + "_"  + // packageName + "/" + 
        File   eventGraphImageFile = new File(eventGraphImageDirectory, eventGraphImageFileName);
        LOG.debug("Event Graph Image location:\n      {}", eventGraphImageFile.getAbsolutePath());

        eventGraphImageFilesList.add(eventGraphImageFile);
    }

    /** Use recursion to find EventGraph XML files
     *
     * @param dir the path the Event Graphs directory to begin evaluation
     */
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private void setEventGraphFiles(File dir) {
        Element localRootElement;
        List<Element> simEntityList;
        String eventGraphName;
        int position;

        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                setEventGraphFiles(file);
            } else {

                localRootElement = assemblyDocument.getRootElement();
                simEntityList = localRootElement.getChildren("SimEntity");

                // Check all names against the simEntityList obtained from the Assembly
                for (Element entity : simEntityList) {
                    eventGraphName = entity.getAttributeValue(TYPE);

                    position = eventGraphName.lastIndexOf(".");
                    eventGraphName = eventGraphName.substring(position + 1, eventGraphName.length());

                    if (file.getName().equals(eventGraphName + ".xml")) {
                        eventGraphFilesList.add(file);
                        saveEventGraphReferences(file);
                    }
                }
            }
        }
    }

    /** @return a JDOM document (Assembly XML file) */
    public Document getAssemblyDocument() {return assemblyDocument;}

    public List<String> getEventGraphNamesList()      {return eventGraphNamesList;}
    public List<File>   getEventGraphFilesList()      {return eventGraphFilesList;}
    public List<File>   getEventGraphImageFilesList() {return eventGraphImageFilesList;}

    /**
     * @return the entityTable
     */
    public Element getEntityTable() {
        return entityTable;
    }

} // end class file EventGraphCache.java