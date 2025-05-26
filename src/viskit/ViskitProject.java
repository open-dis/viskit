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
package viskit;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import javax.swing.filechooser.FileView;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import viskit.doe.FileHandler;
import viskit.view.dialog.ViskitUserPreferencesDialog;

/** The base class for management of all Viskit Projects
 * @version $Id: ViskitProject.java 1916 2008-07-04 09:13:41Z tdnorbra $
 * @author abuss
 */
public class ViskitProject 
{
    /** These static variable might be set by the user upon first Viskit startup
     * to determine a project home space on the user's machine.  A default
     * home will be the user's working directory where Viskit is installed.
     */
    
    public static final String VISKIT_VERSION                         = "2.0 beta";
    public static final String DEFAULT_VISKIT_PROJECTS_DIRECTORY_NAME = "MyViskitProjects";
    public static final String DEFAULT_VISKIT_PROJECTS_DIRECTORY_PATH =
            System.getProperty("user.home").replaceAll("\\\\", "/") + "/" + DEFAULT_VISKIT_PROJECTS_DIRECTORY_NAME;
    public static final String PROJECT_FILE_NAME           = "viskitProject.xml";
    public static final String VISKIT_PROJECT_ROOT_ELEMENT_NAME       = "ViskitProject";
    public static final String ASSEMBLIES_DIRECTORY_NAME   = "Assemblies";
    public static final String EVENT_GRAPHS_DIRECTORY_NAME = "EventGraphs";

    public static final String BUILD_DIRECTORY_NAME   = "build";
    public static final String CLASSES_DIRECTORY_NAME = "classes";
    public static final String DIST_DIRECTORY_NAME    = "dist";
    public static final String IMAGES_DIRECTORY_NAME  = "images";
    public static final String LIB_DIRECTORY_NAME     = "lib";
    public static final String SOURCE_DIRECTORY_NAME  = "src";

    public static final String VISKIT_CONFIG_DIRECTORY = "config"; // this must match viskit.jar and many other places
    public static final String VISKIT_ICON_FILE_NAME   = "Viskit.ico";
    public static final String VISKIT_SPLASH_FILE_NAME = "ViskitSplash2.png";
    public static final String VISKIT_ICON_SOURCE      = VISKIT_CONFIG_DIRECTORY + "/" + IMAGES_DIRECTORY_NAME + "/" + VISKIT_ICON_FILE_NAME;
    public static final String VISKIT_SPLASH_SOURCE    = VISKIT_CONFIG_DIRECTORY + "/" + IMAGES_DIRECTORY_NAME + "/" + VISKIT_SPLASH_FILE_NAME;
    public static final String ANALYST_REPORTS_DIRECTORY_NAME = "AnalystReports";
    public static final String ANALYST_REPORT_IMAGES_DIRECTORY_NAME = "images";
    public static final String ANALYST_REPORT_CHARTS_DIRECTORY_PATH = ANALYST_REPORT_IMAGES_DIRECTORY_NAME + "/" + "charts";
    public static final String ANALYST_REPORT_ASSEMBLY_IMAGES_DIRECTORY_NAME = ASSEMBLIES_DIRECTORY_NAME;
    public static final String ANALYST_REPORT_EVENT_GRAPH_IMAGES_DIRECTORY_NAME = EVENT_GRAPHS_DIRECTORY_NAME;
    public static final String ANALYST_REPORT_STATISTICS_DIRECTORY_NAME = "statistics";

    static final Logger LOG = LogManager.getLogger();

    /** This static variable will get updated at launch if user's home directory doesn't exist */
    public static String VISKIT_PROJECTS_DIRECTORY = DEFAULT_VISKIT_PROJECTS_DIRECTORY_PATH;

    /** TODO FIX: This static variable will be set by the user upon first Viskit startup
     * to determine a project location space on the user's machine. A default
     * location will be in the user's profile, or home directory.
     */
    public static final String DEFAULT_PROJECT_NAME = "DefaultProject";
    public static final String NEW_PROJECT_NAME     = "MyNewProject";

    private File projectDirectory;
    private File projectFile;
    private File analystReportsDirectory;
    private File analystReportChartsDirectory;
    private File analystReportImagesDirectory;
    private File analystReportAssemblyImagesDirectory;
    private File analystReportEventGraphImagesDirectory;
    private File analystReportStatisticsDirectory;
    private File assembliesDirectory;
    private File eventGraphsDirectory;
    private File buildDirectory;
    private File classesDirectory;
    private File srcDirectory;
    private File libDirectory;
    private boolean projectFileExists = false;
    private boolean projectDirty;
    private boolean projectOpen = false;
    private Document projectDocument;

    public ViskitProject(File projectDirectory)
    {
        if (projectDirectory.exists() && !projectDirectory.isDirectory()) 
        {
            LOG.error("Project root must be directory, not\n      "+ projectDirectory.getAbsolutePath());
            return;
        }
        else if (!isProject(projectDirectory))
        {
            LOG.error("Project directory does not contain a '" + PROJECT_FILE_NAME + "' file\n      "+ projectDirectory.getAbsolutePath());
            return;
        }
        setProjectDirectory(projectDirectory);
        
        boolean projectInitializationSuccess = initializeProject();
        LOG.info("ViskitProject " + projectDirectory.getName() + " projectInitializationSuccessful=" + projectInitializationSuccess);
    }

    /** *
     * Initialize current Viskit project
     * @return whether project was successfully initialized
     */
    public final boolean initializeProject() 
    {
        if (projectDirectory == null)
            return false; // possible during startup
        else if (!projectDirectory.exists()) {
             projectDirectory.mkdir();
        }
        setAnalystReportsDirectory(new File(projectDirectory, ANALYST_REPORTS_DIRECTORY_NAME));
        LOG.info("initializeProject() analystReportsDirectory=\n      " + getAnalystReportsDirectory());
        if (!analystReportsDirectory.exists())
        {
            getAnalystReportsDirectory().mkdirs();
            try {
                // copy icon and splash files to AnalystsReport directory to support html report
                Files.copy(new File(VISKIT_ICON_SOURCE).toPath(),   new File(getAnalystReportsDirectory(), VISKIT_ICON_FILE_NAME).toPath());
                Files.copy(new File(VISKIT_SPLASH_SOURCE).toPath(), new File(getAnalystReportsDirectory(), VISKIT_SPLASH_FILE_NAME).toPath());
            }
            catch (IOException ex) {
                LOG.error("initializeProject() exception when setting up analystReportsDirectory {}", ex);
            }
        }

        setAnalystReportImagesDirectory(new File(getAnalystReportsDirectory(), ANALYST_REPORT_IMAGES_DIRECTORY_NAME));

        setAnalystReportAssemblyImagesDirectory(new File(getAnalystReportImagesDirectory(), ANALYST_REPORT_ASSEMBLY_IMAGES_DIRECTORY_NAME));
        if (!analystReportAssemblyImagesDirectory.exists()) {
            getAnalystReportAssemblyImagesDirectory().mkdirs();
        }

        setAnalystReportChartsDirectory(new File(getAnalystReportsDirectory(), ANALYST_REPORT_CHARTS_DIRECTORY_PATH));
        if (!analystReportChartsDirectory.exists()) {
            getAnalystReportChartsDirectory().mkdirs();
        }

        setAnalystReportEventGraphImagesDirectory(new File(getAnalystReportImagesDirectory(), ANALYST_REPORT_EVENT_GRAPH_IMAGES_DIRECTORY_NAME));
        if (!analystReportEventGraphImagesDirectory.exists()) {
            getAnalystReportEventGraphImagesDirectory().mkdirs();
        }

        setAnalystReportStatisticsDirectory(new File(getAnalystReportsDirectory(), ANALYST_REPORT_STATISTICS_DIRECTORY_NAME));
        if (!analystReportStatisticsDirectory.exists()) {
            getAnalystReportStatisticsDirectory().mkdirs();
        }

        setAssembliesDirectory(new File(projectDirectory, ASSEMBLIES_DIRECTORY_NAME));
        if (!assembliesDirectory.exists()) {
            getAssembliesDirectory().mkdir();
        }

        setEventGraphsDirectory(new File(projectDirectory, EVENT_GRAPHS_DIRECTORY_NAME));
        if (!eventGraphsDirectory.exists()) {
            getEventGraphsDirectory().mkdir();
        }

        setBuildDirectory(new File(projectDirectory, BUILD_DIRECTORY_NAME));

        // Start with a fresh build directory
//        if (getBuildDirectory().exists()) {
//            clean();
//        }

        // NOTE: If the project's build directory got nuked and we have
        // cached our Event Graphs and classes with MD5 hash, we'll throw a
        // ClassNotFoundException. Caching of Event Graphs is a convenience for large
        // directories of Event Graphs that take time to compile the first time

        setSrcDirectory(new File(getBuildDirectory(), SOURCE_DIRECTORY_NAME));
        if (!srcDirectory.exists()) {
            getSrcDirectory().mkdirs();
        }

        // packageName "examples" set elsewhere
        setClassesDirectory(new File(getBuildDirectory(), CLASSES_DIRECTORY_NAME));
        if (!classesDirectory.exists()) {
            getClassesDirectory().mkdirs(); // create directory and intermediate subdirectories
        }

        setLibDirectory(new File(projectDirectory, LIB_DIRECTORY_NAME));
        if (!libDirectory.exists()) {
            getLibDirectory().mkdir();
        }
        // TODO copy over logj4 jar

        // If we already have a project file, then load it.  If not, create it
        setProjectFile(new File(projectDirectory, PROJECT_FILE_NAME)); // sets projectFile
        if (!projectFile.exists()) 
        {
            try {
                getProjectFile().createNewFile();
            } 
            catch (IOException e) {
                LOG.error(e.getMessage());
            }
            projectDocument = createProjectDocument();
            writeProjectFile();
        } 
        else {
            loadProjectFromFile(getProjectFile());
        }
        ViskitUserConfiguration.instance().setProjectXMLConfiguration(getProjectFile().getPath()); // TODO problem on re-RUN

        XMLConfiguration config = ViskitUserConfiguration.instance().getProjectXMLConfiguration();
        config.setProperty(ViskitUserConfiguration.VISKIT_PROJECT_NAME, getProjectDirectory().getName());

        setProjectOpen(projectFileExists);
        return projectFileExists;
    }

    private Document createProjectDocument() {
        Document document = new Document();

        Element root = new Element(VISKIT_PROJECT_ROOT_ELEMENT_NAME);
        root.setAttribute("name", projectDirectory.getName());
        document.setRootElement(root);

        Element element = new Element(ANALYST_REPORTS_DIRECTORY_NAME);
        element.setAttribute("name", ANALYST_REPORTS_DIRECTORY_NAME);
        root.addContent(element);

        element = new Element("AssembliesDirectory");
        element.setAttribute("name", ASSEMBLIES_DIRECTORY_NAME);
        root.addContent(element);

        element = new Element("EventGraphsDirectory");
        element.setAttribute("name", EVENT_GRAPHS_DIRECTORY_NAME);
        root.addContent(element);

        element = new Element("BuildDirectory");
        element.setAttribute("name", BUILD_DIRECTORY_NAME);
        root.addContent(element);

        Element subElement = new Element("ClassesDirectory");
        subElement.setAttribute("name", CLASSES_DIRECTORY_NAME);
        element.addContent(subElement);

        subElement = new Element("SourceDirectory");
        subElement.setAttribute("name", SOURCE_DIRECTORY_NAME);
        element.addContent(subElement);

        element = new Element("DistDirectory");
        element.setAttribute("name", DIST_DIRECTORY_NAME);
        root.addContent(element);

        element = new Element("LibDirectory");
        element.setAttribute("name", LIB_DIRECTORY_NAME);
        root.addContent(element);

        return document;
    }

    private void writeProjectFile() {
        FileOutputStream fileOutputStream = null;
        try {
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            fileOutputStream = new FileOutputStream(getProjectFile());
            xmlOutputter.output(projectDocument, fileOutputStream);
            projectFileExists = true;
        } catch (IOException ex) {
            LOG.error(ex);
        } finally {
            try {
                if (fileOutputStream != null)
                    fileOutputStream.close();
            } catch (IOException ex) {
                LOG.error(ex);
            }
        }
    }

    /**
     * Load a Viskit project file
     * @param inputProjectFile a Viskit project file
     */
    private void loadProjectFromFile(File inputProjectFile) {
        try {
            projectDocument = FileHandler.unmarshallJdom(inputProjectFile);
            Element root = projectDocument.getRootElement();
            if (!root.getName().equals(VISKIT_PROJECT_ROOT_ELEMENT_NAME)) {
                projectDocument = null;
                throw new IllegalArgumentException("Not a Viskit Project File");
            }
            projectFileExists = true;
        } catch (JDOMException | IOException ex) {
            LOG.error(ex);
            throw new RuntimeException(ex);
        }
    }

    /** @return an array of a project's extra classpaths */
    public String[] getProjectAdditionalClasspaths() 
    {
        // Prevent duplicate entries
        Set<String> classPathSet = new HashSet<>();

        // Find and list JARs and ZIPs, from the project's lib directory, in the extra classpath widget
        try {
            String additionalJarZipFilePath;
            for (File additionalFile : getLibDirectory().listFiles()) {
                if  ((additionalFile.getName().toLowerCase().contains(".jar")) || 
                     (additionalFile.getName().toLowerCase().contains(".zip"))) 
                {
                    additionalJarZipFilePath = additionalFile.getCanonicalPath().replaceAll("\\\\", "/");
                    LOG.debug(additionalJarZipFilePath);
                    classPathSet.add(additionalJarZipFilePath);
                }
            }
            LOG.debug(getEventGraphsDirectory().getAbsolutePath());

            // Now list any paths inside/outside of the project space, i.e. ${other path}/build/classes
            String[] extraClassPathsArray = ViskitUserPreferencesDialog.getExtraClassPathArray();
            if (extraClassPathsArray.length > 0)
                classPathSet.addAll(Arrays.asList(extraClassPathsArray));
            LOG.debug("Project classPathSet: {}", classPathSet);

        } catch (IOException | NullPointerException ex) {
            LOG.error(ex);
            return new String[0];
        }
        return classPathSet.toArray(String[]::new);
    }

    public void clean() {
        if (getBuildDirectory().exists()) {
            deleteDirectoryContents(getBuildDirectory());
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
        if (!buildDirectory.exists()) {
            getBuildDirectory().mkdir();
        }
        if (!srcDirectory.exists()) {
            getSrcDirectory().mkdir();
        }
        LOG.info("Generate source into " + getSrcDirectory());
    }

    public void compileSource() {
        if (!buildDirectory.exists()) {
            generateSource();
        }
        if (!classesDirectory.exists()) {
            getClassesDirectory().mkdir();
        }
        LOG.info("Compile Source to " + getClassesDirectory());
    }

    public void deleteProject() {
        deleteDirectoryContents(projectDirectory);
    }

    public void closeProject() 
    {
        ViskitUserConfiguration viskitUserConfiguration = ViskitUserConfiguration.instance();
        viskitUserConfiguration.getViskitGuiConfiguration().setProperty(ViskitUserConfiguration.PROJECT_TITLE_NAME_KEY, "");
        viskitUserConfiguration.cleanup();
        viskitUserConfiguration.removeProjectXMLConfiguration(viskitUserConfiguration.getProjectXMLConfiguration());
        setProjectOpen(false);
        ViskitGlobals.instance().setTitleProjectName("");
    }

    /** @return the root directory of this ViskitProject */
    public File getProjectDirectory() {
        return projectDirectory;
    }

    public final void setProjectDirectory(File projectDirectory)
    {
        if (projectDirectory == null)
        {
            LOG.error("ViskitProject setProjectDirectory received null File projectDirectory");
            return;
        }
        else if (!projectDirectory.exists())
        {
            LOG.error("ViskitProject setProjectDirectory received non-existent File projectDirectory=\n      " + projectDirectory.getAbsolutePath());
            return;
        }
        this.projectDirectory = projectDirectory;
        XMLConfiguration guiConfig = ViskitUserConfiguration.instance().getViskitGuiConfiguration();
        guiConfig.setProperty(ViskitUserConfiguration.PROJECT_TITLE_NAME_KEY, projectDirectory.getName()); // TODO check
    }
    
    public String getProjectDirectoryPath()
    {
        return projectDirectory.getAbsolutePath();
    }

    public boolean isProjectDirty() {
        return projectDirty;
    }

    public void setProjectDirty(boolean newDirtyStatus) {
        this.projectDirty = newDirtyStatus;
    }

    public File getBuildDirectory() {
        return buildDirectory;
    }

    /**
     * @param buildDir the buildDirectory to set
     */
    public void setBuildDirectory(File buildDir) {
        this.buildDirectory = buildDir;
    }

    /**
     * @param candidateProjectDirectory potential project directory of interest
     * @return whether a directory contains a Viskit project file
     */
    public static boolean isProject(File candidateProjectDirectory) {
        if (candidateProjectDirectory == null)
            return false;
        File   viskitProjectFile = new File(candidateProjectDirectory, PROJECT_FILE_NAME);
        return viskitProjectFile.isFile();
    }

    /**
     * @return indication of the projectOpen
     */
    public boolean isProjectOpen() {
        return projectOpen;
    }

    /**
     * @param projectOpen the projectOpen to set
     */
    public void setProjectOpen(boolean projectOpen) {
        this.projectOpen = projectOpen;
    }

    /**
     * @return the analystReportsDirectory
     */
    public File getAnalystReportsDirectory() {
        return analystReportsDirectory;
    }

    /**
     * @param analystReportsDir the analystReportsDirectory to set
     */
    public void setAnalystReportsDirectory(File analystReportsDir) {
        this.analystReportsDirectory = analystReportsDir;
    }

    /** Retrieve the project's src directory (located in build)
     *
     * @return the project's src directory (located in build)
     */
    public File getSrcDirectory() {
        return srcDirectory;
    }

    public void setSrcDirectory(File srcDir) {
        this.srcDirectory = srcDir;
    }

    public File getClassesDirectory() {
        return classesDirectory;
    }

    public void setClassesDirectory(File classDirectory) {
        this.classesDirectory = classDirectory;
    }

    public File getEventGraphsDirectory() {
        return eventGraphsDirectory;
    }

    public void setEventGraphsDirectory(File eventGraphDir) {
        this.eventGraphsDirectory = eventGraphDir;
    }

    public File getLibDirectory() {
        return libDirectory;
    }

    public void setLibDirectory(File libDir) {
        this.libDirectory = libDir;
    }

    public File getAssembliesDirectory() {
        return assembliesDirectory;
    }

    public void setAssembliesDirectory(File assemblyDir) {
        this.assembliesDirectory = assemblyDir;
    }

    /**
     * @return the projectFile
     */
    public File getProjectFile() {
        return projectFile;
    }

    /**
     * @return whether projectFile exists
     */
    public boolean  hasProjectFile() {
        return projectFileExists;
    }

    /**
     * @param projectFile the projectFile to set
     */
    public void setProjectFile(File projectFile) {
        this.projectFile = projectFile;
        if  (projectFile == null)
             projectFileExists = false;
        else projectFileExists = projectFile.exists();
    }

    /**
     * @return the analystReportChartsDirectory
     */
    public File getAnalystReportChartsDirectory() {
        return analystReportChartsDirectory;
    }

    /**
     * @param analystReportChartsDir the analystReportChartsDirectory to set
     */
    public void setAnalystReportChartsDirectory(File analystReportChartsDir) {
        this.analystReportChartsDirectory = analystReportChartsDir;
    }

    /**
     * @return the analystReportImagesDirectory
     */
    public File getAnalystReportImagesDirectory() {
        return analystReportImagesDirectory;
    }

    /**
     * @param analystReportImagesDir the analystReportImagesDirectory to set
     */
    public void setAnalystReportImagesDirectory(File analystReportImagesDir) {
        this.analystReportImagesDirectory = analystReportImagesDir;
    }

    /**
     * @return the analystReportAssemblyImagesDirectory
     */
    public File getAnalystReportAssemblyImagesDirectory() {
        return analystReportAssemblyImagesDirectory;
    }

    /**
     * @param analystReportAssemblyImagesDir the analystReportAssemblyImagesDirectory to set
     */
    public void setAnalystReportAssemblyImagesDirectory(File analystReportAssemblyImagesDir) {
        this.analystReportAssemblyImagesDirectory = analystReportAssemblyImagesDir;
    }

    /**
     * @return the analystReportEventGraphImagesDirectory
     */
    public File getAnalystReportEventGraphImagesDirectory() {
        return analystReportEventGraphImagesDirectory;
    }

    /**
     * @param analystReportEventGraphImagesDir the analystReportEventGraphImagesDirectory to set
     */
    public void setAnalystReportEventGraphImagesDirectory(File analystReportEventGraphImagesDir) {
        this.analystReportEventGraphImagesDirectory = analystReportEventGraphImagesDir;
    }

    /**
     * @return the analystReportStatisticsDirectory
     */
    public File getAnalystReportStatisticsDirectory() {
        return analystReportStatisticsDirectory;
    }

    @Override
    public String toString() {
        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        StringWriter stringWriter = new StringWriter();
        try {
            xmlOutputter.output(projectDocument, stringWriter);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        return stringWriter.toString();
    }

    /** A static JFileChooser that can be used at Viskit init from a clean install */
    private static JFileChooser projectChooser;

    /** When a user selects an iconized Viskit Project directory, then load it */
    private static final PropertyChangeListener myChangeListener = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(evt.getPropertyName())) {

                File file = projectChooser.getSelectedFile();

                if (((ViskitProjectFileView) projectChooser.getFileView()).isViskitProject(file)) {
                    projectChooser.approveSelection();
                }
            }
        }
    };

    private static void initializeProjectChooser(String startPath) {
        if (projectChooser == null) {
            projectChooser = new JFileChooser(startPath);

            projectChooser.addPropertyChangeListener(myChangeListener);

            // show spec. icon for viskit projects
            projectChooser.setFileView(new ViskitProjectFileView());

            // allow only dirs for selection
            projectChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            projectChooser.setMultiSelectionEnabled(false);
            projectChooser.setFileFilter(new ProjectFilter());
            projectChooser.setApproveButtonToolTipText("Open selected project");
        } else {
            projectChooser.setCurrentDirectory(new File(startPath));
        }
    }

    /** Used to aid in new project path creation
     *
     * @param parent the component to center the FileChooser against
     * @param startingDirPath a path to start looking
     * @return a selected file
     */
    public static File newProjectPath(JComponent parent, String startingDirPath) {
        initializeProjectChooser(startingDirPath);

        projectChooser.setDialogTitle("New Viskit Project Directory");
        int ret = projectChooser.showSaveDialog(parent);
        if (ret == JFileChooser.CANCEL_OPTION) {
            return null;
        }
        return projectChooser.getSelectedFile();
    }

    /** Utility method to aid in Viskit specific project directory selection
     *
     * @param parent the component parent for JOptionPane orientation
     * @param defaultDirectoryPath a path to start looking from in the chooser
     * @return a path to a valid project directory
     */
    public static File openProjectDirectory(JComponent parent, String defaultDirectoryPath)
    {
        String initialDirectoryPath = defaultDirectoryPath;
        File projectDirectory = new File (initialDirectoryPath);
        
        // if no user preference directory exists, fall back to MyViskitProjects/DefaultProject
        if (!projectDirectory.exists())
        {
            // likely user has not created their own projects yet, fall back to Viskit's embedded MyViskitProjects
            initialDirectoryPath = DEFAULT_VISKIT_PROJECTS_DIRECTORY_NAME + "/"; // allow user to choose DefaultProject or whatever else is there
        }
        initializeProjectChooser(initialDirectoryPath);

        projectChooser.setDialogTitle("Open existing Viskit Project");
        boolean isProjectDirectory;

        do {
            int returnValue = projectChooser.showOpenDialog(parent);
            // User may have exited the chooser, if so then no file was chosen
            if (returnValue == JFileChooser.CANCEL_OPTION) {
                return null;
            }
            projectDirectory = projectChooser.getSelectedFile();
            isProjectDirectory = ((ViskitProjectFileView)projectChooser.getFileView()).isViskitProject(projectDirectory);

            // Give user a chance to select an iconized project directory
            if (!isProjectDirectory) 
            {
                Object[] options = {"Select project", "Cancel"};
                returnValue = JOptionPane.showOptionDialog(parent, "Your selection is not a valid Viskit project.", "Please try another selection",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);

                if (returnValue != 0) {
                    // 0th choice (Select project)
                    return null; // cancelled
                } // cancelled
            }
        } while (!isProjectDirectory); // continue until user has found a valid directory or else cancels the directory chooser

        return projectDirectory;
    }

    /**
     * @param analystReportStatisticsDir the analystReportStatisticsDirectory to set
     */
    public void setAnalystReportStatisticsDirectory(File analystReportStatisticsDir) {
        this.analystReportStatisticsDirectory = analystReportStatisticsDir;
    }

    private static class ViskitProjectFileView extends FileView {

        Icon viskitProjectIcon;

        public ViskitProjectFileView() {

            // Can't use VGlobals.instance().getViskitApplicationClassLoader() b/c it will
            // hang if this is the first frest startup of Viskit
            viskitProjectIcon = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/ViskitIcon.gif"));
        }

        @Override
        public Icon getIcon(File f) {
            return isViskitProject(f) ? viskitProjectIcon : null;
        }
        
        /**
         * Get project name, which is actually maintained in ViskitGlobals
         * @return current project name
         */
        public String getName() 
        {
            return ViskitGlobals.instance().getProjectName();
        }

        /**
         * Report if given directory holds a Viskit Project
         *
         * @param fileDirectory the project directory to test
         * @return true when a viskitProject.xml file is found
         */
        public boolean isViskitProject(File fileDirectory) {

            if ((fileDirectory == null) || !fileDirectory.exists() || !fileDirectory.isDirectory()) {
                return false;
            }
            
            // http://www.avajava.com/tutorials/lessons/how-do-i-use-a-filenamefilter-to-display-a-subset-of-files-in-a-directory.html
            File[] files = fileDirectory.listFiles((File directory, String name) -> {
                // config/ contains the template viskitProject.xml file
                // so, don't show this directory as a potential Viskit project
                if (directory.getName().equals(VISKIT_CONFIG_DIRECTORY)) {
                    return false;
                }

                // Be strictly specific to reduce looking for any *.xml
                return name.equalsIgnoreCase(PROJECT_FILE_NAME);
            });

//            // This can happen on Win machines when parsing "My Computer" directory
//            if (files == null) {
//                return false;
//            }
//            
            // TODO create project file if needed?

//            File[] filesArray = fileDirectory.listFiles();

            // If this List is not empty, we found a project file
//            return files.length > 0;

            return true;
        }
    }

    private static class ProjectFilter extends javax.swing.filechooser.FileFilter {

        @Override
        public boolean accept(File pathname) {
            return !pathname.getName().contains("svn");
        }

        @Override
        public String getDescription() {
            return "Viskit projects";
        }
    }
}
