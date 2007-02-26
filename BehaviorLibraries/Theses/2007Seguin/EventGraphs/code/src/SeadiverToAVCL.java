/*
 * SeadiverToAVCL.java
 *
 * Created on January 3, 2007, 4:08 PM
 *
 * @author John Seguin
 * @version $Id$
 *
 */
package seadiver;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.xml.sax.*;

public class SeadiverToAVCL {

    private Document waypointFile;
    private Node root;
    private NodeList parent;
    private Node wpElement;
    private Node wpt;
    private Element xyPosition;
    private Element depth;
    private Element setPosition;
    private Element thrusters;
    private Element makeKnots;
    private static String targetFileName;
    

    private int moverID;

    /** Creates a new instance of SeadiverToAVCL */
    public SeadiverToAVCL(int mover) {
        setMoverID(mover);
    }
   
    public void setInputFile(String inputPathAndName) {
        waypointFile = parseFile(inputPathAndName);
        root = waypointFile.getDocumentElement();
        parent = waypointFile.getElementsByTagName("UUVCommandScript");
        wpElement = parent.item(0);
    }
    
    public void setOutputFile(String outputPathAndName) {
        String temp = outputPathAndName;

        int i = temp.indexOf(".");

        this.targetFileName = temp.substring(0,i) + getMoverID() + ".xml";
    }
    
    private void setMoverID(int mover) {
        this.moverID = mover;
    }

    public void createWPelement(String xValue, String yValue) {
        wpt = waypointFile.createElement("Waypoint");
        xyPosition = waypointFile.createElement("XYPosition");
        xyPosition.setAttribute("x", xValue);
        xyPosition.setAttribute("y", yValue);
        wpElement.appendChild(wpt);
        wpt.appendChild(xyPosition);
        wpt.appendChild(depth);

    }

    public void createSetPositionElement(String xValue, String yValue, double dpth) {
        String depthString = String.valueOf(dpth);
        setPosition = waypointFile.createElement("SetPosition");
        xyPosition = waypointFile.createElement("XYPosition");
        xyPosition.setAttribute("x", xValue);
        xyPosition.setAttribute("y", yValue);
        setPosition.appendChild(xyPosition);
        depth = waypointFile.createElement("Depth");
        depth.setAttribute("value", depthString);
        setPosition.appendChild(depth);
        wpElement.appendChild(setPosition);
    }

    public void createThrusterElement() {
        thrusters = waypointFile.createElement("Thrusters");
        thrusters.setAttribute("value", "false");
        wpElement.appendChild(thrusters);
    }

    public void createMakeKnotsElement(String speed) {
        makeKnots = waypointFile.createElement("MakeKnots");
        makeKnots.setAttribute("speedOverGround", "false");
        makeKnots.setAttribute("value", speed);
        wpElement.appendChild(makeKnots);
    }

    public void generateAVCL(){
        saveXMLDocument(targetFileName, waypointFile);
//        System.out.println("targetFileName is: " + targetFileName);
    }

     private Document parseFile(String fileName) {
         //System.out.println("Parsing XML file... " + fileName);
         DocumentBuilder docBuilder;
         Document doc = null;
         DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
         docBuilderFactory.setIgnoringElementContentWhitespace(true);
         try {
             docBuilder = docBuilderFactory.newDocumentBuilder();
         }
         catch (ParserConfigurationException e) {
             System.out.println("Wrong parser configuration: " + e.getMessage());
             return null;
         }
         File sourceFile = new File(fileName);
         try {
             doc = docBuilder.parse(sourceFile);
         }
         catch (SAXException e) {
             System.out.println("Wrong XML file structure: " + e.getMessage());
             return null;
         }
         catch (IOException e) {
             System.out.println("Could not read source file: " + e.getMessage());
         }
         //System.out.println("XML file parsed");
         return doc;
     }

     /** Saves XML Document into XML file.
      * @param fileName XML file name
      * @param doc XML document to save
      * @return <B>true</B> if method success <B>false</B> otherwise
      */
     private boolean saveXMLDocument(String fileName, Document doc) {
         //System.out.println("Saving XML file... " + fileName);
         // open output stream where XML Document will be saved
         File xmlOutputFile = new File(fileName);
         FileOutputStream fos;
         Transformer transformer;
         try {
             fos = new FileOutputStream(xmlOutputFile);
         }
         catch (FileNotFoundException e) {
             System.out.println("Error occured: " + e.getMessage());
             return false;
         }
         // Use a Transformer for output
         TransformerFactory transformerFactory = TransformerFactory.newInstance();
         try {
             transformer = transformerFactory.newTransformer();
             transformer.setOutputProperty(OutputKeys.INDENT, "yes");
             transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
         }
         catch (TransformerConfigurationException e) {
             System.out.println("Transformer configuration error: " + e.getMessage());
             return false;
         }
         DOMSource source = new DOMSource(doc);
         StreamResult result = new StreamResult(fos);
         // transform source into result will do save
         try {
             transformer.transform(source, result);
         }
         catch (TransformerException e) {
             System.out.println("Error transform: " + e.getMessage());
         }
         //System.out.println("XML file saved.");
         return true;
     }

    private int getMoverID() {
        return this.moverID;
    }

    

}
