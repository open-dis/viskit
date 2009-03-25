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
package viskit;

import edu.nps.util.LogUtils;
import java.io.File;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Set of utility methods for caching a List<File> of EventGraph paths
 * @version $Id$
 * <p>cacheing
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
 * </p>
 * @author <a href="mailto:tdnorbra@nps.edu">Terry Norbraten</a>
 */
public class EventGraphCache {
    
    /** The jdom.Document object of the assembly file */
    private Document assemblyDocument;

    /**
     * The names and file locations of the the event graph files and image files
     * being linked to in the AnalystReport
     */
    private LinkedList<String> eventGraphNamesList  = new LinkedList<String>();
    private LinkedList<File> eventGraphFilesList  = new LinkedList<File>();
    private LinkedList<String> eventGraphImagePathsList = new LinkedList<String>();

    private final String EVENT_GRAPH_IMAGE_DIR = 
            VGlobals.instance().getCurrentViskitProject().getAnalystReportEventGraphImagesDir() + "/";
    private Element entityTable;
    private static EventGraphCache me;
    
    public static synchronized EventGraphCache instance() {
        if (me == null) {
            me = new EventGraphCache();
        }
        return me;
    }

    private EventGraphCache() {
        eventGraphNamesList  = new LinkedList<String>();
        eventGraphFilesList  = new LinkedList<File>();
        eventGraphImagePathsList = new LinkedList<String>();
    }
    
    /**
     * Creates the entity table for this analyst xml object
     *
     * @param assemblyFile the assembly file loaded into the Assembly Runner
     */
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    public void makeEntityTable(File assemblyFile) {
        setEntityTable(new Element("EntityTable"));

        // Clear the cache if currently full
        if (getEventGraphNamesList().size() > 0) {
            getEventGraphNamesList().clear();
        }
        if (getEventGraphFilesList().size() > 0) {
            getEventGraphFilesList().clear();
        }
        if (getEventGraphImagePathsList().size() > 0) {
            getEventGraphImagePathsList().clear();
        }
        setAssemblyDocument(loadXML(assemblyFile));

        Element localRootElement = getAssemblyDocument().getRootElement();
        List<Element> simEntityList = (List<Element>) localRootElement.getChildren("SimEntity");

        // Conduct a test for a diskit package which is native java
        String isDiskitFile = "diskit";
        for (Element temp : simEntityList) {
            String javaTest = temp.getAttributeValue("type").substring(0, isDiskitFile.length());

            // If it's not a java file process it
            if (!javaTest.equals(isDiskitFile)) {
                Element tableEntry = new Element("SimEntity");
                tableEntry.setAttribute("name", temp.getAttributeValue("name"));
                tableEntry.setAttribute("fullyQualifiedName", temp.getAttributeValue("type"));
                saveEventGraphReferences(temp.getAttributeValue("type"));
                getEntityTable().addContent(tableEntry);
            }
        }

        setEventGraphFiles(VGlobals.instance().getCurrentViskitProject().getEventGraphsDir());
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
        } catch (JDOMException ex) {
            LogUtils.getLogger().error(ex);
        } catch (IOException ex) {
            LogUtils.getLogger().error(ex);
        }
        return doc;
    }

    /**
     * Processes the 'type' value from a Viskit assembly, if it is an xml file, and
     * adds it to the list of event graphs with the proper formatting of the file's
     * path
     *
     * @param fileType the type of XML file being used that includes a package
     * name
     */
    private void saveEventGraphReferences(String fileType) {
        LogUtils.getLogger().debug("Parameter fileType: " + fileType);
        eventGraphNamesList.add(fileType);
        
        // find the package seperator
        char letter;
        int idx = 0;
        int fileTypeLength = fileType.length();
        for (int i = 0; i < fileTypeLength; i++) {
            letter = fileType.charAt(i);
            if (letter == '.') {
                idx = i;
            }
        }

        String fileTypePackageToPath = fileType.substring(0, idx) + "/";
        
        // TODO: Move ARs to project folders.  That's where they should be
        String eventGraphName = fileType.substring(idx + 1, fileTypeLength);
        LogUtils.getLogger().debug("EventGraph Name: " + eventGraphName);
        
        String imgFile = EVENT_GRAPH_IMAGE_DIR + fileTypePackageToPath + eventGraphName + ".xml.png";
        imgFile = imgFile.replaceAll("\\\\", "/");
        LogUtils.getLogger().debug("Event Graph Image location: " + imgFile);
        
        eventGraphImagePathsList.add(imgFile);
    }

    /** Use recursion to find EventGraph XML files
     *
     * @param f the path to begin evaluation
     */
    private void setEventGraphFiles(File f) {

        File[] files = f.listFiles();
        for (File file : files) {        
            if (file.isDirectory()) {
                setEventGraphFiles(file);
            } else {
                
                // Check against the eventGraphNamesList obtained from the Assembly
                for (String eventGraphName : eventGraphNamesList) {
                    eventGraphName = eventGraphName.substring(eventGraphName.lastIndexOf(".") + 1) + ".xml";
                    if (file.getName().equals(eventGraphName)) {
                        eventGraphFilesList.add(file);
                    }
                }
            }
        }
    }

    /** @param pDoc the JDOM Document to set */
    public void setAssemblyDocument(Document pDoc) {
        assemblyDocument = pDoc;
    }

    /** @return a JDOM document (Assembly XML file) */
    public Document getAssemblyDocument() {return assemblyDocument;}

    public LinkedList<String> getEventGraphNamesList() {return eventGraphNamesList;}
    public LinkedList<File> getEventGraphFilesList() {return eventGraphFilesList;}
    public LinkedList<String> getEventGraphImagePathsList() {return eventGraphImagePathsList;}

    /**
     * @return the entityTable
     */
    public Element getEntityTable() {
        return entityTable;
    }

    /**
     * @param entityTable the entityTable to set
     */
    public void setEntityTable(Element entityTable) {
        this.entityTable = entityTable;
    }
    
} // end class file EventGraphCache.java