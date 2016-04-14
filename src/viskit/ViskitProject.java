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
package viskit;

import edu.nps.util.LogUtils;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import javax.swing.filechooser.FileView;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import static viskit.ViskitStatics.LOG;

/** The base class for management of all Viskit Projects
 * @version $Id: ViskitProject.java 1916 2008-07-04 09:13:41Z tdnorbra $
 * @author abuss
 */
public class ViskitProject {

    /** This static variable will be set by the user upon first Viskit startup
     * to determine a project home space on the user's machine.  A default
     * home will be the user's working directory where Viskit is installed.
     */
    public static final String DEFAULT_VISKIT_PROJECTS_DIR =
            System.getProperty("user.home").replaceAll("\\\\", "/") + "/.viskit/MyViskitProjects";

    public static String MY_VISKIT_PROJECTS_DIR = DEFAULT_VISKIT_PROJECTS_DIR;

    /** This static variable will be set by the user upon first Viskit startup
     * to determine a project location space on the user's machine.  A default
     * location will be in the user's profile, or home directory.
     */
    public static       String        DEFAULT_PROJECT_NAME = "DefaultProject";

    public static final String               VISKIT_ROOT_NAME = "ViskitProject";     // fixed
    public static final String              PROJECT_FILE_NAME = "viskitProject.xml"; // fixed
    public static final String      ASSEMBLIES_DIRECTORY_NAME = "Assemblies";
    public static final String     EVENTGRAPHS_DIRECTORY_NAME = "EventGraphs";
    public static final String ANALYST_REPORTS_DIRECTORY_NAME = "AnalystReports";

    public static final String   BUILD_DIRECTORY_NAME = "build";
    public static final String CLASSES_DIRECTORY_NAME = "classes";
    public static final String  SOURCE_DIRECTORY_NAME = "src";
    public static final String    DIST_DIRECTORY_NAME = "dist";
    public static final String     LIB_DIRECTORY_NAME = "lib";

    public static final String VISKIT_ICON_FILE_NAME = "Viskit.ico";
    public static final String VISKIT_CONFIGURATION_SUBDIR = "configuration";
    public static final String VISKIT_ICON_SOURCE = VISKIT_CONFIGURATION_SUBDIR + "/" + VISKIT_ICON_FILE_NAME;
    public static final String ANALYST_REPORT_CHARTS_DIRECTORY_NAME = "charts";
    public static final String ANALYST_REPORT_IMAGES_DIRECTORY_NAME = "images";
    public static final String ANALYST_REPORT_ASSEMBLY_IMAGES_DIRECTORY_NAME = ASSEMBLIES_DIRECTORY_NAME;
    public static final String ANALYST_REPORT_EVENT_GRAPH_IMAGES_DIRECTORY_NAME = EVENTGRAPHS_DIRECTORY_NAME;
    public static final String ANALYST_REPORT_STATISTICS_DIRECTORY_NAME = "statistics";

    static Logger log = LogUtils.getLogger(ViskitProject.class);

	private String projectName = ""; // empty string if no project open
    private File projectRoot;
    private File projectFile;
    private File analystReportsDirectory;
    private File analystReportChartsDirectory;
    private File analystReportImagesDirectory;
    private File analystReportAssemblyImagesDirectory;
    private File analystReportEventGraphImagesDirectory;
    private File analystReportStatisticsDir;
    private File assembliesDir;
    private File eventGraphsDirectory;
    private File buildDirectory;
    private File classesDirectory;
    private File srcDirectory;
    private File distDirectory;
    private File libDirectory;
    private boolean projectFileExists = false;
    private boolean dirty;
    private boolean projectOpen = false;
    private Document projectDocument;

    public ViskitProject (File projectRoot)
	{
        if (projectRoot.exists() && !projectRoot.isDirectory()) {
            throw new IllegalArgumentException(
                    "Project root must be directory: " +
                    projectRoot);
        }
        setProjectRoot(projectRoot);
    }

    public boolean initializeProject() {

        if (!projectRoot.exists()) {
             projectRoot.mkdir();
        }

        setAnalystReportsDirectory(new File(projectRoot, ANALYST_REPORTS_DIRECTORY_NAME));
        if (!analystReportsDirectory.exists()) {
            getAnalystReportsDirectory().mkdirs();
            try {
                Files.copy(new File(VISKIT_ICON_SOURCE).toPath(), new File(getAnalystReportsDirectory(), VISKIT_ICON_FILE_NAME).toPath());
            } catch (IOException ex) {
                log.error(ex);
            }
        }

        setAnalystReportChartsDirectory(new File(getAnalystReportsDirectory(), ANALYST_REPORT_CHARTS_DIRECTORY_NAME));
        if (!analystReportChartsDirectory.exists()) {
            getAnalystReportChartsDirectory().mkdirs();
        }

        setAnalystReportImagesDirectory(new File(getAnalystReportsDirectory(), ANALYST_REPORT_IMAGES_DIRECTORY_NAME));

        setAnalystReportAssemblyImagesDirectory(new File(getAnalystReportImagesDirectory(), ANALYST_REPORT_ASSEMBLY_IMAGES_DIRECTORY_NAME));
        if (!analystReportAssemblyImagesDirectory.exists()) {
            getAnalystReportAssemblyImagesDirectory().mkdirs();
        }

        setAnalystReportEventGraphImagesDirectory(new File(getAnalystReportImagesDirectory(), ANALYST_REPORT_EVENT_GRAPH_IMAGES_DIRECTORY_NAME));
        if (!analystReportEventGraphImagesDirectory.exists()) {
            getAnalystReportEventGraphImagesDirectory().mkdirs();
        }

        setAnalystReportStatisticsDir(new File(getAnalystReportsDirectory(), ANALYST_REPORT_STATISTICS_DIRECTORY_NAME));
        if (!analystReportStatisticsDir.exists()) {
            getAnalystReportStatisticsDirectory().mkdirs();
        }

        setAssembliesDirectory(new File(projectRoot, ASSEMBLIES_DIRECTORY_NAME));
        if (!assembliesDir.exists()) {
            getAssembliesDirectory().mkdir();
        }

        setEventGraphsDirectory(new File(projectRoot, EVENTGRAPHS_DIRECTORY_NAME));
        if (!eventGraphsDirectory.exists()) {
            getEventGraphsDirectory().mkdir();
        }

        setBuildDirectory(new File(projectRoot, BUILD_DIRECTORY_NAME));

        // Start with a fresh build directory
//        if (getBuildDirectory().exists()) {
//            clean();
//        }

        // NOTE: If the project's build directory got nuked and we have
        // cached our EGs and classes with MD5 hash, we'll throw a
        // ClassNotFoundException.  Caching of EGs is a convenience for large
        // directories of EGs that take time to compile the first time

        setSrcDirectory(new File(getBuildDirectory(), SOURCE_DIRECTORY_NAME));
        if (!srcDirectory.exists()) {
            getSrcDirectory().mkdirs();
        }

        setClassesDirectory(new File(getBuildDirectory(), CLASSES_DIRECTORY_NAME));
        if (!classesDirectory.exists()) {
            getClassesDirectory().mkdirs();
        }

        setLibDirectory(new File(projectRoot, LIB_DIRECTORY_NAME));
        if (!libDirectory.exists()) {
            getLibDirectory().mkdir();
        }

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
        } 
		else
		{
            loadProjectFromFile(getProjectFile());
        }
        ViskitConfiguration.instance().setProjectXMLConfiguration(getProjectFile().getAbsolutePath());
        setProjectOpen(projectFileExists);
        return projectFileExists;
    }
	
	public String getProjectName ()
	{
		return projectName;
	}

	/**
	 * @param newProjectName the projectName to set
	 */
	public void setProjectName(String newProjectName) {
		this.projectName = newProjectName;
	}

    private Document createProjectDocument() {
        Document document = new Document();

        Element root = new Element(VISKIT_ROOT_NAME);
        root.setAttribute("name", projectRoot.getName());
        document.setRootElement(root);

        Element element = new Element(ANALYST_REPORTS_DIRECTORY_NAME);
        element.setAttribute("name", ANALYST_REPORTS_DIRECTORY_NAME);
        root.addContent(element);

        element = new Element("AssembliesDirectory");
        element.setAttribute("name", ASSEMBLIES_DIRECTORY_NAME);
        root.addContent(element);

        element = new Element("EventGraphsDirectory");
        element.setAttribute("name", EVENTGRAPHS_DIRECTORY_NAME);
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
            log.error(ex);
        } finally {
            try {
                if (fileOutputStream != null)
                    fileOutputStream.close();
            } catch (IOException ex) {
                log.error(ex);
            }
        }
    }

    /**
     * Load a Visual Simkit (Viskit) project file
     * @param inputProjectFile a Viskit project file
     */
    private void loadProjectFromFile(File inputProjectFile) {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            projectDocument = saxBuilder.build(inputProjectFile);
            Element root = projectDocument.getRootElement();
            if (!root.getName().equals(VISKIT_ROOT_NAME)) {
                projectDocument = null;
                throw new IllegalArgumentException("Not a Viskit Project File");
            }
            projectFileExists = true;
			projectName = root.getAttribute("name").getValue();
        } catch (JDOMException | IOException ex) {
            log.error(ex);
            throw new RuntimeException(ex);
        }
    }

    /** @return an array of a project's external resources */
    public String[] getProjectContents() {

        // Prevent duplicate entries
        Set<String> cp = new HashSet<>();

        // Find and list JARs and ZIPs, from the project's lib directory, in the extra classpath widget
        try {
            for (File f : getLibDirectory().listFiles()) {
                if ((f.getName().contains(".jar")) || (f.getName().contains(".zip"))) {
                    String file = f.getCanonicalPath().replaceAll("\\\\", "/");
                    log.debug(file);
                    cp.add(file);
                }
            }
            log.debug(getEventGraphsDirectory().getCanonicalPath());

            // Now list any paths outside of the project space, i.e. ${other path}/build/classes
            String[] classPaths = ViskitConfiguration.instance().getConfigurationValues(ViskitConfiguration.EXTRA_CLASSPATHS_KEY);
            for (String classPath : classPaths) {
                cp.add(classPath.replaceAll("\\\\", "/"));
            }

        } catch (IOException ex) {
            log.error(ex);
        } catch (NullPointerException npe) {
            return null;
        }
        return cp.toArray(new String[cp.size()]);
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
        System.out.println("Generate source into " + getSrcDirectory());
    }

    public void compileSource() {
        if (!buildDirectory.exists()) {
            generateSource();
        }
        if (!classesDirectory.exists()) {
            getClassesDirectory().mkdir();
        }
        System.out.println("Compile Source to " + getClassesDirectory());
    }

    public void deleteProject() {
        deleteDirectoryContents(projectRoot);
    }

    public void closeProject()
	{
        ViskitConfiguration viskitConfiguration = ViskitConfiguration.instance();
        viskitConfiguration.getViskitGuiXMLConfiguration().setProperty(ViskitConfiguration.PROJECT_TITLE_NAME, "");
        viskitConfiguration.cleanup();
        viskitConfiguration.removeProjectXMLConfiguration(viskitConfiguration.getProjectXMLConfiguration());
        setProjectOpen(false);
		projectName = "";
		ViskitGlobals.instance().getViskitApplicationFrame().setTitle(projectName);
    }

    /** @return the root directory of this ViskitProject */
    public File getProjectRoot() {
        return projectRoot;
    }

    public final void setProjectRoot(File projectRoot)
	{
		try
		{
			this.projectRoot = projectRoot.getCanonicalFile();
			XMLConfiguration guiConfig = ViskitConfiguration.instance().getViskitGuiXMLConfiguration();
			guiConfig.setProperty(ViskitConfiguration.PROJECT_TITLE_NAME, getProjectRoot().getName());
		}
		catch (Exception e)
		{
			
		}
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public File getBuildDirectory() {
        return buildDirectory;
    }

    /**
     * @param buildDirectory the buildDirectory to set
     */
    public void setBuildDirectory(File buildDirectory) {
        this.buildDirectory = buildDirectory;
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
     * @param analystReportsDirectory the analystReportsDirectory to set
     */
    public void setAnalystReportsDirectory(File analystReportsDirectory) {
        this.analystReportsDirectory = analystReportsDirectory;
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

    public void setLibDirectory(File libDirectory) {
        this.libDirectory = libDirectory;
    }

    public File getAssembliesDirectory() {
        return assembliesDir;
    }

    public void setAssembliesDirectory(File assemblyDir) {
        this.assembliesDir = assemblyDir;
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


    /**
     * @return the analystReportChartsDirectory
     */
    public File getAnalystReportChartsDirectory() {
        return analystReportChartsDirectory;
    }

    /**
     * @param analystReportChartsDirectory the analystReportChartsDirectory to set
     */
    public void setAnalystReportChartsDirectory(File analystReportChartsDirectory) {
        this.analystReportChartsDirectory = analystReportChartsDirectory;
    }

    /**
     * @return the analystReportImagesDir
     */
    public File getAnalystReportImagesDirectory() {
        return analystReportImagesDirectory;
    }

    /**
     * @param analystReportImagesDirectory the analystReportImagesDir to set
     */
    public void setAnalystReportImagesDirectory(File analystReportImagesDirectory) {
        this.analystReportImagesDirectory = analystReportImagesDirectory;
    }

    /**
     * @return the analystReportAssemblyImagesDirectory
     */
    public File getAnalystReportAssemblyImagesDirectory() {
        return analystReportAssemblyImagesDirectory;
    }

    /**
     * @param analystReportAssemblyImagesDirectory the analystReportAssemblyImagesDirectory to set
     */
    public void setAnalystReportAssemblyImagesDirectory(File analystReportAssemblyImagesDirectory) {
        this.analystReportAssemblyImagesDirectory = analystReportAssemblyImagesDirectory;
    }

    /**
     * @return the analystReportEventGraphImagesDirectory
     */
    public File getAnalystReportEventGraphImagesDirectory() {
        return analystReportEventGraphImagesDirectory;
    }

    /**
     * @param analystReportEventGraphImagesDirectory the analystReportEventGraphImagesDirectory to set
     */
    public void setAnalystReportEventGraphImagesDirectory(File analystReportEventGraphImagesDirectory) {
        this.analystReportEventGraphImagesDirectory = analystReportEventGraphImagesDirectory;
    }

    /**
     * @return the analystReportStatisticsDir
     */
    public File getAnalystReportStatisticsDirectory() {
        return analystReportStatisticsDir;
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
		    projectChooser.setDialogTitle("Open Project");

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

    /** User directory chooser to aid in creating a new project path 
     *
     * @param parent the component to center the FileChooser against
     * @param startingDirectoryPath a path to start looking
     * @return a selected file
     */
    public static File newProjectPath(JComponent parent, String startingDirectoryPath) {
        initializeProjectChooser(startingDirectoryPath);

        projectChooser.setDialogTitle("New Viskit Project Directory");
        int ret = projectChooser.showSaveDialog(parent);
        if (ret == JFileChooser.CANCEL_OPTION) {
            return null;
        }
        return projectChooser.getSelectedFile();
    }

    /** User directory chooser to aid in Viskit-specific project directory selection
     *
     * @param parentFrame the component parent for JOptionPane initialization
     * @param startingDirectoryPath a path to start looking from in the chooser
     * @return a path to a valid project directory
     */
    public static File openProjectDirectory(JFrame parentFrame, String startingDirectoryPath) {
        File projectDirectory;
        initializeProjectChooser(startingDirectoryPath);

        projectChooser.setDialogTitle("Open Existing Viskit Project");
        boolean isProjectDirectory, found = false;

        do {
            int returnValue = projectChooser.showOpenDialog(parentFrame);

            // User may have exited the chooser
            if (returnValue == JFileChooser.CANCEL_OPTION) {
                return null;
            }
            projectDirectory = projectChooser.getSelectedFile();

			if (!projectDirectory.isDirectory())
			{
				projectDirectory = projectDirectory.getParentFile();
			}
			File originalProjectDirectory = projectDirectory;

			if (projectDirectory.getName().equals(ASSEMBLIES_DIRECTORY_NAME)      ||
				projectDirectory.getName().equals(EVENTGRAPHS_DIRECTORY_NAME)     ||
				projectDirectory.getName().equals(ANALYST_REPORTS_DIRECTORY_NAME) ||
				projectDirectory.getName().equals(BUILD_DIRECTORY_NAME)           ||
				projectDirectory.getName().equals(CLASSES_DIRECTORY_NAME)         ||
				projectDirectory.getName().equals(SOURCE_DIRECTORY_NAME)          ||
				projectDirectory.getName().equals(DIST_DIRECTORY_NAME)            ||
				projectDirectory.getName().equals(LIB_DIRECTORY_NAME))
			{
				projectDirectory = projectDirectory.getParentFile(); // gracefully support subdirectory mis-selection
				LOG.info("changed project directory to parent: " + projectDirectory.getPath());
			}
			// check for project configuration file
			File viskitProjectFile = new File(projectDirectory.getPath() + File.separator + PROJECT_FILE_NAME);
		
            isProjectDirectory = ((ViskitProjectFileView)projectChooser.getFileView()).isViskitProject(projectDirectory);
            // Give user a chance to select an iconized project directory
            if (!projectDirectory.exists() || !isProjectDirectory || !viskitProjectFile.exists())
			{
				LOG.error("Illegal viskit project directory: " + originalProjectDirectory.getPath());
                Object[] options = {"Select project", "Cancel"};
				String TRY_AGAIN = "Please try another selection...";
                returnValue = JOptionPane.showOptionDialog(parentFrame, "<html><p>Your selection is not a valid Visual Simkit (Viskit) project.</p>" + 
						    "<p>Look for the Viskit icon when choosing the correct directory.)</p>" + "<p>&nbsp;</p>" +
						    "<p>" + TRY_AGAIN + "</p>", TRY_AGAIN,
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);

                if (returnValue != 0) {
                    // 0th choice (Select project)
                    return null; // cancelled
                } // cancelled
            }
			else  found = true;
			
        } while (!found);

        return projectDirectory;
    }

    /**
     * @param analystReportStatisticsDir the analystReportStatisticsDir to set
     */
    public void setAnalystReportStatisticsDir(File analystReportStatisticsDir) {
        this.analystReportStatisticsDir = analystReportStatisticsDir;
    }

    private static class ViskitProjectFileView extends FileView {

        Icon viskitProjIcon;

        public ViskitProjectFileView() {

            // Can't use VGlobals.instance().getWorkClassLoader() b/c it will
            // hang if this is the first frest startup of Viskit
            viskitProjIcon = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/ViskitIcon.gif"));
        }

        @Override
        public Icon getIcon(File f) {
            return isViskitProject(f) ? viskitProjIcon : null;
        }

        /**
         * Report if given directory holds a Viskit Project
         *
         * @param fDir the project directory to test
         * @return true when a viskitProject.xml file is found
         */
        public boolean isViskitProject(File fDir) {

            if ((fDir == null) || !fDir.exists() || !fDir.isDirectory()) {
                return false;
            }

            // http://www.avajava.com/tutorials/lessons/how-do-i-use-a-filenamefilter-to-display-a-subset-of-files-in-a-directory.html
            File[] files = fDir.listFiles(new java.io.FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {

                    // configuration/ contains the template viskitProject.xml file
                    // so, don't show this directory as a potential Viskit project
                    if (dir.getName().equals(VISKIT_CONFIGURATION_SUBDIR)) {
                        return false;
                    }
					if (name.startsWith(".")) {
                        return false; // no hidden files
                    }

                    // Be brutally specific to reduce looking for any *.xml
                    return name.equalsIgnoreCase(PROJECT_FILE_NAME);
                }
            });

            // This can happen on Win machines when parsing "My Computer" directory
            if (files == null) {
                return false;
            }

            // If this List is not empty, we found a project file
            return files.length > 0;
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
