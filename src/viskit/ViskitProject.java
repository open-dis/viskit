/*
Copyright (c) 1995-2009 held by the author(s).  All rights reserved.

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
package viskit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileView;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/** The base class for all Viskit Project management
 * @version $Id: ViskitProject.java 1916 2008-07-04 09:13:41Z tdnorbra $
 * @author abuss
 */
public class ViskitProject {

    public static final Logger log = Logger.getLogger(viskit.ViskitProject.class);

    /** This static variable will be set by the user upon first Viskit startup
     * to determine a project home space on the user's machine.  A default
     * home will be the user's profile, or home directory.
     */
    public static final String DEFAULT_VISKIT_PROJECTS_DIR =
            System.getProperty("user.home") + Vstatics.getFileSeparator() +
            "MyViskitProjects";
    public static String MY_VISKIT_PROJECTS_DIR = DEFAULT_VISKIT_PROJECTS_DIR;

    /** This static variable will be set by the user upon first Viskit startup
     * to determine a project location space on the user's machine.  A default
     * location will be in the user's profile, or home directory.
     */
    public static String DEFAULT_PROJECT_NAME = "DefaultProject";

    public static final String VISKIT_ROOT_NAME = "ViskitProject";
    public static final String PROJECT_FILE_NAME = "viskitProject.xml";
    public static final String EVENT_GRAPH_DIRECTORY_NAME = "EventGraphs";
    public static final String ASSEMBLY_DIRECTORY_NAME = "Assemblies";
    public static final String LIB_DIRECTORY_NAME = "lib";
    public static final String BUILD_DIRECTORY_NAME = "build";
    public static final String SOURCE_DIRECTORY_NAME = "src";
    public static final String CLASSES_DIRECTORY_NAME = "classes";
    public static final String DIST_DIRECTORY_NAME = "dist";
    private File projectRoot;
    private File projectFile;
    private File eventGraphDir;
    private File assemblyDir;
    private File libDir;
    private File buildDir;
    private File srcDir;
    private File classDir;
    private boolean projectFileExists = false;
    private boolean dirty;
    private Document projectDocument;

    public ViskitProject(File projectRoot) {
        if (projectRoot.exists() && !projectRoot.isDirectory()) {
            throw new IllegalArgumentException(
                    "Project root must be directory: " +
                    projectRoot);
        }
        setProjectRoot(projectRoot);
    }

    public boolean initProject() {

        if (!projectRoot.exists()) {
            projectRoot.mkdir();
        }

        setEventGraphDir(new File(projectRoot, EVENT_GRAPH_DIRECTORY_NAME));
        if (!eventGraphDir.exists()) {
            getEventGraphDir().mkdir();
        }

        setAssemblyDir(new File(projectRoot, ASSEMBLY_DIRECTORY_NAME));
        if (!assemblyDir.exists()) {
            getAssemblyDir().mkdir();
        }

        setLibDir(new File(projectRoot, LIB_DIRECTORY_NAME));
        if (!libDir.exists()) {
            getLibDir().mkdir();
        }

        buildDir = new File(projectRoot, BUILD_DIRECTORY_NAME);
        setSrcDir(new File(getBuildDir(), SOURCE_DIRECTORY_NAME));
        setClassDir(new File(getBuildDir(), CLASSES_DIRECTORY_NAME));

        // Start with a fresh build directory
//        if (buildDir.exists()) {
//            clean();
//        }

        // NOTE: if we nuke the buildDir, then we cause ClassNotFoundExceptions
        // down the line.  Each class will be recompiled everytime we open an
        // assembly, or event graph file, so no real need to nuke it.

        // If we already have a project file, then load it.  If not, create it
        setProjectFile(new File(projectRoot, PROJECT_FILE_NAME));
        if (!projectFile.exists()) {
            try {
                getProjectFile().createNewFile();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            projectDocument = createProjectDocument();
            writeProjectFile();
        } else {
            loadProjectFromFile(getProjectFile());
            projectFileExists = true;
        }
        ViskitConfig.instance().setProjectXMLConfig(getProjectFile().getAbsolutePath());
        return projectFileExists;
    }

    protected Document createProjectDocument() {
        Document document = new Document();
        Element root = new Element("ViskitProject");
        root.setAttribute("name", projectRoot.getName());
        document.setRootElement(root);

        Element eventGraphElement = new Element("EventGraphDirectory");
        eventGraphElement.setAttribute("name", EVENT_GRAPH_DIRECTORY_NAME);
        root.addContent(eventGraphElement);

        eventGraphElement = new Element("AssemblyDirectory");
        eventGraphElement.setAttribute("name", ASSEMBLY_DIRECTORY_NAME);
        root.addContent(eventGraphElement);

        eventGraphElement = new Element("LibDirectory");
        eventGraphElement.setAttribute("name", LIB_DIRECTORY_NAME);
        root.addContent(eventGraphElement);

        eventGraphElement = new Element("BuildDirectory");
        eventGraphElement.setAttribute("name", BUILD_DIRECTORY_NAME);
        root.addContent(eventGraphElement);

        Element subElement = new Element("SourceDirectory");
        subElement.setAttribute("name", SOURCE_DIRECTORY_NAME);
        eventGraphElement.addContent(subElement);

        subElement = new Element("ClassesDir");
        subElement.setAttribute("name", CLASSES_DIRECTORY_NAME);
        eventGraphElement.addContent(subElement);

        eventGraphElement = new Element("DistDirectory");
        eventGraphElement.setAttribute("name", DIST_DIRECTORY_NAME);
        root.addContent(eventGraphElement);

        return document;
    }

    protected void writeProjectFile() {
        FileOutputStream fileOutputStream = null;
        try {
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            fileOutputStream = new FileOutputStream(getProjectFile());
            xmlOutputter.output(projectDocument, fileOutputStream);
            projectFileExists = true;
        } catch (FileNotFoundException ex) {
            log.error(ex);
        } catch (IOException ex) {
            log.error(ex);
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException ex) {
                log.error(ex);
            }
        }
    }

    /**
     * Load an existing Viskit project file
     * @param inputProjectFile an existing Viskit project file
     */
    public void loadProjectFromFile(File inputProjectFile) {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            projectDocument = saxBuilder.build(inputProjectFile);
            Element root = projectDocument.getRootElement();
            if (!root.getName().equals(VISKIT_ROOT_NAME)) {
                throw new IllegalArgumentException("Not a Viskit Project File");
            }
        } catch (JDOMException ex) {
            log.error(ex);
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            log.error(ex);
            throw new RuntimeException(ex);
        }
    }

    public void clean() {
        if (getBuildDir().exists()) {
            deleteDirectoryContents(getBuildDir());
        }
    }

    public static void deleteDirectoryContents(File file) {
        if (file.isFile()) {
            file.delete();
        } else {
            File[] contents = file.listFiles();
            for (File f : contents) {
                deleteDirectoryContents(f);
            }
            file.delete();
        }
    }

    public void generateSource() {
        if (!buildDir.exists()) {
            getBuildDir().mkdir();
        }
        if (!srcDir.exists()) {
            getSrcDir().mkdir();
        }
        System.out.println("Generate source into " + getSrcDir());
    }

    public void compileSource() {
        if (!buildDir.exists()) {
            generateSource();
        }
        if (!classDir.exists()) {
            getClassDir().mkdir();
        }
        System.out.println("Compile Source to " + getClassDir());
    }

    public void deleteProject() {
        deleteDirectoryContents(projectRoot);
    }

    public void closeProject() {
        ViskitConfig vConfig = ViskitConfig.instance();
        vConfig.cleanup();
        vConfig.removeProjectXMLConfig(vConfig.getProjectXMLConfig());
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public void setProjectRoot(File projectRoot) {
        this.projectRoot = projectRoot;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public File getBuildDir() {
        return buildDir;
    }

    public File getSrcDir() {
        return srcDir;
    }

    public void setSrcDir(File srcDir) {
        this.srcDir = srcDir;
    }

    public File getClassDir() {
        return classDir;
    }

    public void setClassDir(File classDir) {
        this.classDir = classDir;
    }

    /** @return an array of a project's external resources */
    public String[] getProjectContents() {

        // Prevent duplicate entries
        Set<String> cp = new HashSet<String>();

        // Always list JARs and ZIPs before EventGraphs
        try {
            for (File f : getLibDir().listFiles()) {
                if ((f.getName().contains(".jar")) || (f.getName().contains(".zip"))) {
                    String file = f.getCanonicalPath().replaceAll("\\\\", "/");
                    log.debug(file);
                    cp.add(file);
                }
            }
            log.debug(getEventGraphDir().getCanonicalPath());
            
            // Now list any paths outside of the project space, i.e. ${other path}/build/classes
            String[] classPaths = ViskitConfig.instance().getConfigValues(ViskitConfig.X_CLASS_PATHS_KEY);
            for (String classPath : classPaths) {
                cp.add(classPath.replaceAll("\\\\", "/"));
            }

            // Then always include our project's EventGraphs path for convenience
            cp.add(getEventGraphDir().getCanonicalPath().replaceAll("\\\\", "/"));

        } catch (IOException ex) {
            log.error(ex);
        }
        return cp.toArray(new String[cp.size()]);
    }

    public File getEventGraphDir() {
        return eventGraphDir;
    }

    public void setEventGraphDir(File eventGraphDir) {
        this.eventGraphDir = eventGraphDir;
    }

    public File getLibDir() {
        return libDir;
    }

    public void setLibDir(File libDir) {
        this.libDir = libDir;
    }

    public File getAssemblyDir() {
        return assemblyDir;
    }

    public void setAssemblyDir(File assemblyDir) {
        this.assemblyDir = assemblyDir;
    }

    @Override
    public String toString() {
        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        StringWriter stringWriter = new StringWriter();
        try {
            xmlOutputter.output(projectDocument, stringWriter);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return stringWriter.toString();
    }

    /**
     * @return the projectFile
     */
    public File getProjectFile() {
        return projectFile;
    }

    /**
     * @param projectFile the projectFile to set
     */
    public void setProjectFile(File projectFile) {
        this.projectFile = projectFile;
    }

    private static JFileChooser projectChooser;

    private static void initializeProjectChooser(String startPath) {
        if (projectChooser == null) {
            projectChooser = new JFileChooser(startPath);
            projectChooser.setDialogType(JFileChooser.OPEN_DIALOG);

            // allow only dirs for selection
            projectChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            // show spec. icon for auvw projects
            projectChooser.setFileView(new ViskitProjectFileView());
            projectChooser.setMultiSelectionEnabled(false);
        }
    }

    /** Not currently used
     * @param parent the component to center the FileChooser against
     * @param startingDirPath a path to start looking
     * @return a selected file
     */
    public static File newProjectPath(JFrame parent, String startingDirPath) {
        initializeProjectChooser(startingDirPath);

        projectChooser.setDialogTitle("New Viskit Project Directory");
        int ret = projectChooser.showSaveDialog(parent);
        if (ret == JFileChooser.CANCEL_OPTION) {
            return null;
        }
        return projectChooser.getSelectedFile();
    }

    /**
     * @param parent the component parent for JOptionPane orientation
     * @param startingDirPath a path to start looking from in dir chooser
     * @return a path to a chosen project directory
     */
    public static File openProjectDir(JFrame parent, String startingDirPath) {
        File projectDir = null;
        boolean isProjectDir = false;

        initializeProjectChooser(startingDirPath);
        projectChooser.setDialogTitle("Open an Existing Viskit Project");

        // TODO: Determine source of annoying delay in showing the dialog
        int ret = projectChooser.showOpenDialog(parent);

        if (ret != JFileChooser.APPROVE_OPTION) {
            return null;
        } // cancelled

        do {

            projectDir = projectChooser.getSelectedFile();

            if (!isViskitProject(projectDir)) {
                Object[] options = {"Select project", "Cancel"};
                int retrn = JOptionPane.showOptionDialog(parent, "Selected directory is not a Viskit project.", "Unable to open project",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);

                if (retrn != 0) {
                    // 0th choice (Select project)
                    return null; // cancelled
                } // cancelled
                isProjectDir = false;
            } else {
                isProjectDir = true;
            }
        } while (!isProjectDir);

        projectChooser = null;

        return projectDir;
    }

    /**
     * @param f the project directory to test
     * @return true when an viskitProject.xml file is found
     */
    public static boolean isViskitProject(File f) {

        boolean foundProjectFile = false;
        if (!f.exists() || !f.isDirectory()) {
            return foundProjectFile;
        }

        File[] children = f.listFiles();

        for (File c : children) {
            if (c.getName().equals(PROJECT_FILE_NAME)) {
                foundProjectFile = true;
            }
        }

        return foundProjectFile;
    }

    private static class ViskitProjectFileView extends FileView {

        Icon viskitProjIcon;

        public ViskitProjectFileView() {
            super();
            viskitProjIcon = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/ViskitIcon.gif"));
        }

        @Override
        public Icon getIcon(File f) {
            return isViskitProject(f) ? viskitProjIcon : null;
        }
    }
}
