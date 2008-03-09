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
 * Copyright (c) 1995-2007 held by the author(s).  All rights reserved.
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

import java.io.File;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Set of utility methods for cacheing a List<String> of EventGraph paths
 * @version $Id:$
 * <p>
 *   <b>History:</b>
 *   <pre><b>
 *     Date:     17 DEC 2007
 *     Time:     0108Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=viskit.EventGraphCache">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Initial
 * 
 *   </b></pre>
 * </p>
 * @author <a href="mailto:tdnorbra@nps.edu">Terry Norbraten</a>
 */
public class EventGraphCache {

    /** The jdom.Document object of the assembly file */    
    private static Document assemblyDocument;
    
    /**
     * The names and file locations of the the event graph files and image files
     * being linked to in the AnalystReport
     */
    private static LinkedList<String> eventGraphNames  = new LinkedList<String>();
    private static LinkedList<String> eventGraphFiles  = new LinkedList<String>();
    private static LinkedList<String> eventGraphImagePaths = new LinkedList<String>();
    
    static Logger log = Logger.getLogger(EventGraphCache.class);
    
    /**
     * Creates the entity table for this analyst xml object
     *
     * @param assemblyFile the assembly file loaded into the Assembly Runner
     * @return the entityTable for the simConfig portion of the analyst report
     */
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    public static Element makeEntityTable(String assemblyFile) {
        Element entityTable = new Element("EntityTable");

        // Clear the cache if currently full
        if (getEventGraphNames().size() > 0) {
            getEventGraphNames().clear();
        }
        if (getEventGraphFiles().size() > 0) {
            getEventGraphFiles().clear();
        }
        if (getEventGraphImagePaths().size() > 0) {
            getEventGraphImagePaths().clear();
        }
        setAssemblyDocument(loadXML(assemblyFile));

        Element localRootElement = EventGraphCache.getAssemblyDocument().getRootElement();
        List<Element> simEntityList = (List<Element>) localRootElement.getChildren("SimEntity");

        // Conduct a test for a diskit package which is native java
        String isJAVAfile = "diskit";
        for (Element temp : simEntityList) {
            String javaTest = temp.getAttributeValue("type").substring(0, 6);

            // If it's not a java file process it
            if (!javaTest.equals(isJAVAfile)) {
                Element tableEntry = new Element("SimEntity");
                tableEntry.setAttribute("name", temp.getAttributeValue("name"));
                tableEntry.setAttribute("fullyQualifiedName", temp.getAttributeValue("type"));
                saveEventGraphReferences(temp.getAttributeValue("type"));
                entityTable.addContent(tableEntry);
            }
        }
        return entityTable;
    }
    
    /**
     * Loads an XML document file for processing
     *
     * @param xmlFile the location of the file to load as a Document
     * @return the document object of the loaded XML
     */
    public static Document loadXML(String xmlFile) {
        Document doc = null;
        try {
            SAXBuilder builder = new SAXBuilder();
            doc = builder.build(new File(xmlFile));
        } catch (JDOMException ex) {
            log.error(ex);
        } catch (IOException ex) {
            log.error(ex);
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
    private static void saveEventGraphReferences(String fileType) {
        log.debug("Parameter fileType: " + fileType);
        
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
        
        String eventGraphImageDir = System.getProperty("user.dir") + "/AnalystReports/images/EventGraphs/";
        
        String eventGraphName = fileType.substring(idx + 1, fileTypeLength);
        log.debug("Event Graph Name: " + eventGraphName);
        
        String eventGraphDir = "";
        String eventGraphPath = "";
        
        // Locate the URL of the event graph directory
        // TODO: resolve case when several classpaths are registered
        for (URL eventGraphURL : SettingsDialog.getExtraClassPathArraytoURLArray()) {
            eventGraphPath = eventGraphURL.toString();
            
            // Don't care about jar files here
            if (eventGraphPath.contains("jar")) {continue;}
            log.info("Event Graph Path: " + eventGraphPath);
            eventGraphDir = eventGraphPath + fileTypePackageToPath;
        }
        
        eventGraphDir = eventGraphDir.replaceAll("\\\\", "/");
        
        /* This is now a URL, so, we need to strip out the "file:/" header so 
         * that the SAXBuilder won't append the base directory to the URL 
         * causing a fnfe
         */
        eventGraphDir = eventGraphDir.replaceFirst("file:/", "");
        log.debug("Event Graph Directory: " + eventGraphDir);
        
        String eventGraphFile = eventGraphDir + eventGraphName + ".xml";
        log.debug("Event Graph File: " + eventGraphFile);
        
        String imgFile = eventGraphImageDir + fileTypePackageToPath + eventGraphName + ".xml.png";
        imgFile = imgFile.replaceAll("\\\\", "/");
        log.debug("Event Graph Image location: " + imgFile);
                
        if (!eventGraphFiles.contains(eventGraphFile)) {            
            eventGraphNames.add(fileType);
            eventGraphFiles.add(eventGraphFile);
            eventGraphImagePaths.add(imgFile);
        }
    }
    
    /** @param pDoc the JDOM Document to set */
    public static void setAssemblyDocument(Document pDoc) {
        assemblyDocument = pDoc;
    }
    
    /** @return a JDOM document (Assembly XML file) */
    public static Document getAssemblyDocument() {return assemblyDocument;}
    
    public static LinkedList<String> getEventGraphNames() {return eventGraphNames;}
    public static LinkedList<String> getEventGraphFiles() {return eventGraphFiles;}
    public static LinkedList<String> getEventGraphImagePaths() {return eventGraphImagePaths;}
    
    public static void setEventGraphNames(LinkedList<String> pEGNames) {eventGraphNames = pEGNames;}
    public static void setEventGraphFiles(LinkedList<String> pEGFiles) {eventGraphFiles = pEGFiles;}
    public static void setEventGraphImagePaths(LinkedList<String> pEGImagePaths) {eventGraphImagePaths = pEGImagePaths;}
    
} // end class file EventGraphCache.java