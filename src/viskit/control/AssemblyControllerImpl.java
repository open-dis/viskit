package viskit.control;

import actions.ActionIntrospector;
import edu.nps.util.DirectoryWatch;
import edu.nps.util.LogUtils;
import edu.nps.util.TempFileManager;
import edu.nps.util.ZipUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.jgraph.graph.DefaultGraphCell;
import viskit.util.EventGraphCache;
import viskit.util.FileBasedAssemblyNode;
import viskit.util.OpenAssembly;
import viskit.ViskitGlobals;
import viskit.ViskitConfiguration;
import viskit.ViskitProject;
import viskit.ViskitStatics;
import viskit.assembly.AssemblyRunnerPlug;
import viskit.doe.LocalBootLoader;
import viskit.jgraph.vGraphUndoManager;
import viskit.model.*;
import viskit.mvc.mvcAbstractController;
import viskit.mvc.mvcModel;
import viskit.mvc.mvcRecentFileListener;
import viskit.util.Compiler;
import viskit.util.XMLValidationTool;
import viskit.view.dialog.AssemblyMetadataDialog;
import viskit.view.SimulationRunPanel;
import viskit.view.AssemblyEditViewFrame;
import viskit.view.AssemblyView;
import viskit.view.ViskitApplicationFrame;
import viskit.view.dialog.UserPreferencesDialog;
import viskit.xsd.translator.assembly.SimkitAssemblyXML2Java;
import viskit.xsd.bindings.assembly.SimkitAssembly;
import viskit.xsd.translator.eventgraph.SimkitXML2Java;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 14, 2004
 * @since 9:26:02 AM
 * @version $Id$
 */
public class AssemblyControllerImpl extends mvcAbstractController implements AssemblyController, OpenAssembly.AssemblyChangeListener {

    static final Logger LOG = LogUtils.getLogger(AssemblyControllerImpl.class);
    private static int mutex = 0;
    Class<?> simEvSrcClass, simEvLisClass, propChgSrcClass, propChgLisClass;
    private String initialFilePath;

    /** The handler to run an assembly */
    private AssemblyRunnerPlug runner;

    /** Creates a new instance of AssemblyController */
    public AssemblyControllerImpl() {
        initializeHistoryXMLConfiguration();
    }

    /**
     * Sets an initial assembly file to open upon Viskit startup supplied by the
     * command line
     * @param assemblyPath the assembly file to immediately open upon startup
     */
    public void setInitialFile(String assemblyPath) {
        if (viskit.ViskitStatics.debug) {
            System.out.println("Initial file set: " + assemblyPath);
        }
        initialFilePath = assemblyPath;
    }

    /** This method is for introducing Assemblies to compile from outside of
     * Viskit.  This method is not used from Viskit and is required for third-party access.
     *
     * @param assemblyPath an assembly file to compile
     */
    public void compileAssembly(String assemblyPath) {
        LOG.debug("Compiling assembly: " + assemblyPath);
        File f = new File(assemblyPath);
        initialFilePath = assemblyPath;
        _doOpen(f);
        compileAssemblyAndPrepareSimulationRunner();
    }

    @Override
    public void begin()
	{
		// check if prior project was open, if so then open it
		String projectStatus = "false";
		if (ViskitConfiguration.instance() != null)
			 projectStatus = ViskitConfiguration.instance().getValue(ViskitConfiguration.PROJECT_OPEN_KEY);
		if ((projectStatus != null) && projectStatus.equalsIgnoreCase("true"))
		{
			String projectPath = ViskitConfiguration.instance().getValue(ViskitConfiguration.PROJECT_PATH_KEY) +
								 File.separator +
					             ViskitConfiguration.instance().getValue(ViskitConfiguration.PROJECT_NAME_KEY);
			File projectDirectory = new File (projectPath);
			if (projectDirectory.isDirectory())
				openProject (projectDirectory);
			
			// TODO re-open previously open assemblies and event graphs
		}
		
        // The initialFilePath is set if we have stated a file "arg" upon startup from the command line
        if (initialFilePath != null)
		{
            LOG.debug("Loading initial file: " + initialFilePath);
            compileAssembly(initialFilePath);
        } 
		else 
		{
            List<File> openAssemblyFileList = getOpenAssemblyFileList(false);
            LOG.debug("Inside AssemblyControllerImpl begin() and openAssemblyFileList.size()=" + openAssemblyFileList.size());

            for (File f : openAssemblyFileList)
			{
                _doOpen(f);
            }
        }
        updateAssemblyFileLists();
    }
	
    public List<File> getOpenAssemblyFileList()
	{
		return getOpenAssemblyFileList (true);
	}

    /** Information required by the EventGraphControllerImpl to see if an Assembly
     * file is already open.  Also checked internally by this class.
     * @param refresh flag to refresh the list from viskitConfig.xml
     * @return a final (unmodifiable) reference to the current Assembly open list
     */
    private List<File> getOpenAssemblyFileList(boolean refresh)
	{
        if (refresh || openAssembliesFileList == null)
		{
            updateAssemblyFileLists();
        }
        return openAssembliesFileList;
    }

    private boolean checkSaveIfDirty() {
        if (localDirty) {
            StringBuilder sb = new StringBuilder("<html><center>Execution parameters have been modified.<br>(");

            for (Iterator<OpenAssembly.AssemblyChangeListener> iterator = isLocalDirty.iterator(); iterator.hasNext();) {
                sb.append(iterator.next().getHandle());
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2); // last comma-space
            sb.append(")<br>Choose yes if you want to stop this operation, then manually select<br>the indicated tab(s) to ");
            sb.append("save the execution parameters.");

            int yn = (((AssemblyView) getView()).genericAsk2Buttons("Question", sb.toString(), "Stop and let me save",
                    "Ignore my execution parameter changes"));
            // n == -1 if dialog was just closed
            //   ==  0 for first option
            //   ==  1 for second option

            // be conservative, stop for first 2 choices
            if (yn != 1) {
                return false;
            }
        }
        boolean returnValue = true;
        AssemblyModelImpl assemblyModel = (AssemblyModelImpl) getModel();
        if (assemblyModel != null) {
            if (((AssemblyModel) getModel()).isDirty()) {
                return askToSaveAndContinue();
            }
        }
        return returnValue;  // proceed
    }

    @Override
    public void settings() {
        // placeholder for combo gui
    }

    public final static String OPENASSEMBLY_METHOD = "openAssembly"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void openAssembly () // method name must exactly match preceding string value
	{
        // Don't allow a new assembly to be created if a current project is  not open
        if ((ViskitGlobals.instance().getCurrentViskitProject() == null) ||
			!ViskitGlobals.instance().getCurrentViskitProject().isProjectOpen()) 
		{
			messageToUser (JOptionPane.WARNING_MESSAGE, "No project directory", 
					"<html><p>Assemblies are only opened within an open project.</p>" +
					"<p>Open or create a project first.</p>");
			return;
		}
        File[] files = ((AssemblyView) getView()).openFilesAsk();
        if (files == null)
		{
            return;
        }
        for (File file : files) {
            if (file != null) {
				if (file.getParentFile().getAbsolutePath().startsWith(ViskitGlobals.instance().getCurrentViskitProject().getProjectRootDirectory().getAbsolutePath()))
				{
					_doOpen(file);
					ViskitGlobals.instance().getViskitApplicationFrame().displayAssemblyEditorTab();
				}
				else 
				{
					messageToUser (JOptionPane.WARNING_MESSAGE, "Illegal directory for current project", 
							"<html><p>Open assemblies must be within the currently open project.</p>" +
							"<p>&nbsp</p>" +
							"<p>Current project name: <b>" + ViskitGlobals.instance().getCurrentViskitProject().getProjectName() + "</b></p>" +
							"<p>Current project path: "    + ViskitGlobals.instance().getCurrentViskitProject().getProjectRootDirectory().getAbsolutePath() + "</p>" +
							"<p>&nbsp</p>" +
							"<p>Please choose an assembly in current project, or else open a different project.</p>");
					// TODO offer to copy?
					break;
				}
			}
        }
//		updateAssemblyFileLists(); // update file lists
		ViskitGlobals.instance().getAssemblyEditViewFrame().buildMenus(); // reset
    }

    private void _doOpen(File file) {
        if (!file.exists()) {
            return;
        }

        AssemblyView vaw = (AssemblyView) getView();
        AssemblyModelImpl mod = new AssemblyModelImpl(this);
        mod.initialize();
        vaw.addTab(mod);
        ViskitGlobals.instance().getAssemblyEditViewFrame().getSelectedPane().setToolTipText(mod.getMetadata().description);

        // these may initialize to null on startup, check
        // before doing any openAlready lookups
        AssemblyModel[] openAlready = null;
        if (vaw != null) {
            openAlready = vaw.getOpenAssemblyModelArray();
        }
        boolean isOpenAlready = false;
        if (openAlready != null) {
            for (AssemblyModel model : openAlready) {
                if (model.getLastFile() != null) {
                    String path = model.getLastFile().getAbsolutePath();
                    if (path.equals(file.getAbsolutePath())) {
                        isOpenAlready = true;
                    }
                }
            }
        }

        if (mod.newModel(file) && !isOpenAlready) {

            vaw.setSelectedAssemblyName(mod.getMetadata().name);
            // TODO: Implement an Assembly description block set here

            adjustRecentAssemblyFileSet(file);
            markAssemblyFilesOpened();

            // replaces old fileWatchOpen(file);
            initOpenAssemblyWatch(file, mod.getJaxbRoot());
            openEventGraphs(file);

        } else {
            vaw.deleteTab(mod);
        }

        resetRedoUndoStatus();
        ViskitGlobals.instance().getAssemblyEditViewFrame().buildMenus(); // refresh
    }

    /** Start w/ undo/redo disabled in the Edit Menu after opening a file */
    private void resetRedoUndoStatus() {

        AssemblyEditViewFrame view = (AssemblyEditViewFrame) getView();

        if (view.getCurrentVgraphAssemblyComponentWrapper() != null) {
            vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentVgraphAssemblyComponentWrapper().getUndoManager();
            undoMgr.discardAllEdits();
            updateUndoRedoStatus();
        }
    }

    /** Mark every Assembly file opened as "open" in the app config file */
    private void markAssemblyFilesOpened() {

        // Mark every vAMod opened as "open"
        AssemblyModel[] openAlready = ((AssemblyView) getView()).getOpenAssemblyModelArray();
        for (AssemblyModel vAMod : openAlready) {
            if (vAMod.getLastFile() != null) {
                String modelPath = vAMod.getLastFile().getAbsolutePath().replaceAll("\\\\", "/");
                markAssemblyConfigurationOpen(modelPath);
            }
        }
    }

    @Override
    public void openRecentAssembly(File path) {
        _doOpen(path);
		ViskitGlobals.instance().getAssemblyEditViewFrame().buildMenus(); // reset
    }

    /** Tell the Assembly File listener our new name
     *
     * @param f the XML Assembly file
     * @param jaxbroot the JAXB root of this XML file
     */
    public void initOpenAssemblyWatch(File f, SimkitAssembly jaxbroot) {
        OpenAssembly.inst().setFile(f, jaxbroot);
    }

    /** @return the listener for this AssemblyControllerImpl */
    @Override
    public OpenAssembly.AssemblyChangeListener getAssemblyChangeListener() {
        return assemblyChangeListener;
    }
    private boolean localDirty = false;
    private Set<OpenAssembly.AssemblyChangeListener> isLocalDirty = new HashSet<>();
    OpenAssembly.AssemblyChangeListener assemblyChangeListener = new OpenAssembly.AssemblyChangeListener() {

        @Override
        public void assemblyChanged(int action, OpenAssembly.AssemblyChangeListener source, Object param) {
            switch (action) {
                case JAXB_CHANGED:
                    isLocalDirty.remove(source);
                    if (isLocalDirty.isEmpty()) {
                        localDirty = false;
                    }

                    ((AssemblyModel) getModel()).setDirty(true);
                    break;

                case NEW_ASSEMBLY:
                    isLocalDirty.clear();
                    localDirty = false;
                    break;

                case PARAM_LOCALLY_EDITED:
                    // This gets hit when you type something in the last three tabs
                    isLocalDirty.add(source);
                    localDirty = true;
                    break;

                case CLOSE_ASSEMBLY:

                    // Close any currently open EGs because we don't yet know which ones
                    // to keep open until iterating through each remaining vAMod

                    ((EventGraphController) ViskitGlobals.instance().getEventGraphController()).closeAll();

                    AssemblyModel vAMod = (AssemblyModel) getModel();
                    markAssemblyConfigurationClosed(vAMod.getLastFile());

                    AssemblyView view = (AssemblyView) getView();
                    view.deleteTab(vAMod);

                    // NOTE: This doesn't work quite right.  If no Assembly is open,
                    // then any non-associated EGs that were also open will
                    // annoyingly close from the closeAll call above.  We are
                    // using an open EG cache system that relies on parsing an
                    // Assembly file to find its associated EGs to open
                    if (!isCloseAll()) {

                        AssemblyModel[] modAr = view.getOpenAssemblyModelArray();
                        for (AssemblyModel mod : modAr) {
                            if (!mod.equals(vAMod)) {
                                openEventGraphs(mod.getLastFile());
                            }
                        }
                    }

                    break;

                default:
                    LOG.warn("Program error AssemblyControllerImpl.assemblyChanged");
            }
        }

        @Override
        public String getHandle() {
            return "Assembly Controller";
        }
    };

    @Override
    public String getHandle() {
        return assemblyChangeListener.getHandle();
    }

    @Override
    public void assemblyChanged(int action, OpenAssembly.AssemblyChangeListener source, Object param) {
        assemblyChangeListener.assemblyChanged(action, source, param);
    }

    @Override
    public void addAssemblyFileListener(OpenAssembly.AssemblyChangeListener listener) {
        OpenAssembly.inst().addListener(listener);
    }

    @Override
    public void removeAssemblyFileListener(OpenAssembly.AssemblyChangeListener listener) {
        OpenAssembly.inst().removeListener(listener);
    }

    Set<mvcRecentFileListener> recentAssemblyListeners = new HashSet<>();

    @Override
    public void addRecentAssemblyFileSetListener(mvcRecentFileListener listener) {
        recentAssemblyListeners.add(listener);
    }

    @Override
    public void removeRecentAssemblyFileSetListener(mvcRecentFileListener listener) {
        recentAssemblyListeners.remove(listener);
    }

    /** Here we are informed of open Event Graphs */

    private void notifyRecentAssemblyFileListeners() {
        for (mvcRecentFileListener listener : recentAssemblyListeners) {
            listener.listChanged();
        }
    }

    Set<mvcRecentFileListener> recentProjectListeners = new HashSet<>();

    @Override
    public void addRecentProjectListener(mvcRecentFileListener listener)
	{
        recentProjectListeners.add(listener);
    }

    @Override
    public void removeRecentProjectListener(mvcRecentFileListener listener) {
        recentProjectListeners.remove(listener);
    }

    private void notifyRecentProjectFileListeners() {
        for (mvcRecentFileListener listener : recentProjectListeners)
		{
            listener.listChanged();
        }
    }

    /** Here we are informed of open Event Graphs */
    DirectoryWatch.DirectoryChangeListener egListener = new DirectoryWatch.DirectoryChangeListener() {

        @Override
        public void fileChanged(File file, int action, DirectoryWatch source) {
            // Do nothing.  The DirectoryWatch still tracks temp EGs, but we
            // don't need anything special from this method, yet....
        }
    };

    @Override
    public DirectoryWatch.DirectoryChangeListener getOpenEventGraphListener() {
        return egListener;
    }

    public final static String SAVE_METHOD = "save"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void save () // method name must exactly match preceding string value 
	{
        AssemblyModel mod = (AssemblyModel) getModel();
        if (mod.getLastFile() == null) {
            saveAs();
        } else {
            mod.saveModel(mod.getLastFile());
        }
    }

    public final static String SAVEAS_METHOD = "saveAs"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void saveAs () // method name must exactly match preceding string value
	{
        AssemblyModel model         = (AssemblyModel) getModel();
        AssemblyView  view          = (AssemblyView) getView();
        GraphMetadata graphMetadata = model.getMetadata();

        // Allow the user to type specific package names
        String packageName = graphMetadata.packageName.replace(".", ViskitStatics.getFileSeparator());
        File saveFile = view.saveFileAsk(packageName + ViskitStatics.getFileSeparator() + graphMetadata.name + ".xml", false, "Save Assembly File As");

        if (saveFile != null) {

            String n = saveFile.getName();
            if (n.toLowerCase().endsWith(".xml")) {
                n = n.substring(0, n.length() - 4);
            }
            graphMetadata.name = n;
            model.setMetadata(graphMetadata); // might have renamed

            model.saveModel(saveFile);
            view.setSelectedAssemblyName(graphMetadata.name);
            adjustRecentAssemblyFileSet(saveFile);
            markAssemblyFilesOpened();
        }
    }

    public final static String EDITGRAPHMETADATA_METHOD = "editGraphMetadata"; // must match following method name.  not possible to accomplish this programmatically.
    @Override
    public void editGraphMetadata () // method name must exactly match preceding string value
	{
        AssemblyModel assemblyModel = (AssemblyModel) getModel();
        if (assemblyModel == null) {return;}
        GraphMetadata graphMetadata = assemblyModel.getMetadata();
        boolean modified =
                AssemblyMetadataDialog.showDialog(ViskitGlobals.instance().getAssemblyEditViewFrame(), graphMetadata);
        if (modified) {
            ((AssemblyModel) getModel()).setMetadata(graphMetadata);

            // update title bar
            ((AssemblyView) getView()).setSelectedAssemblyName(graphMetadata.name);
        }
    }
    private int eventGraphNodeCount             = 0;
    private int adapterNodeCount                = 0;
    private int propertyChangeListenerNodeCount = 0;    // A little experiment in class introspection
    private static Field eventGraphNodeCountField;
    private static Field adapterNodeCountField;
    private static Field propertyChangeListenerNodeCountField;

    static { // do at class initialization time
        try {
                        eventGraphNodeCountField = AssemblyControllerImpl.class.getDeclaredField("eventGraphNodeCount");
                           adapterNodeCountField = AssemblyControllerImpl.class.getDeclaredField("adapterNodeCount");
            propertyChangeListenerNodeCountField = AssemblyControllerImpl.class.getDeclaredField("propertyChangeListenerNodeCount");
        } catch (NoSuchFieldException | SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String shortEventGraphName(String typeName) {
        return shortName(typeName, "evgr_", eventGraphNodeCountField);
    }

    private String shortPropertyChangeListenerName(String typeName) {
        return shortName(typeName, "lstnr_", propertyChangeListenerNodeCountField); // use same counter
    }

    private String shortAdapterName(String typeName) {
        return shortName(typeName, "adptr_", adapterNodeCountField); // use same counter
    }

    private String shortName(String typeName, String prefix, Field intField) {
        String shortname = prefix;
        if (typeName.lastIndexOf('.') != -1) {
            shortname = typeName.substring(typeName.lastIndexOf('.') + 1) + "_";
        }

        // Don't capitalize the first letter
        char[] ca = shortname.toCharArray();
        ca[0] = Character.toLowerCase(ca[0]);
        shortname = new String(ca);

        String retn = shortname;
        try {
            int count = intField.getInt(this);
            // Find a unique name
            AssemblyModel model = (AssemblyModel) getModel();
            do {
                retn = shortname + count++;
            } while (model.nameExists(retn));   // don't force the vAMod to mangle the name
            intField.setInt(this, count);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOG.error(ex);
        }
        return retn;
    }

    public final static String NEWPROJECT_METHOD = "newProject"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void newProject () // method name must exactly match preceding string value
	{
		// if a project is currently open, first ask to confirm
        if ((ViskitGlobals.instance().getCurrentViskitProject() != null) &&
		     ViskitGlobals.instance().getCurrentViskitProject().isProjectOpen() &&
			 confirmProjectClosing())
		{
			// perform project closing before continuing
			ViskitGlobals.instance().getCurrentViskitProject().closeProject();
        }
		
        ViskitConfiguration viskitConfiguration = ViskitConfiguration.instance();
		File projectDirectory = new File (viskitConfiguration.getValue(ViskitConfiguration.PROJECT_PATH_KEY)); // starting point; TODO user preference
//		viskitConfiguration.setValue(ViskitConfiguration.PROJECT_PATH_KEY,        projectDirectory.getPath());
		
		String newProjectPath = viskitConfiguration.getValue(ViskitConfiguration.PROJECT_PATH_KEY);
		File   newProjectRoot = new File (newProjectPath);
		ViskitProject viskitProject = new ViskitProject(newProjectRoot);
		
		do
		{
			// directory chooser for new project
			newProjectRoot = viskitProject.newProjectPath (ViskitGlobals.instance().getEventGraphViewFrame().getContent(), newProjectPath);
			if (newProjectRoot == null)
				return; // no project directory chosen, cancel
			newProjectPath = newProjectRoot.getPath();
			if (newProjectRoot.list().length > 0)
			{
				System.out.println("Directory is not empty!  Please choose or create an empty directory.");
				messageToUser(JOptionPane.ERROR_MESSAGE, "Can't create new Viskit project here", "Please choose or create an empty directory");
			}
		}
		while (newProjectRoot.list().length > 0);
		
		// User dialog: project properties
		viskitConfiguration.setValue(ViskitConfiguration.PROJECT_PATH_KEY,        newProjectPath);
		viskitConfiguration.setValue(ViskitConfiguration.PROJECT_NAME_KEY,        newProjectRoot.getName()); // "*Enter new project name*");
		viskitConfiguration.setValue(ViskitConfiguration.PROJECT_AUTHOR_KEY,      System.getProperty("user.name")); // TODO user preference, default author name
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ViskitGlobals.getDateFormat());
		viskitConfiguration.setValue(ViskitConfiguration.PROJECT_REVISION_KEY,    simpleDateFormat.format(new Date())); // prefer date, version number is acceptable alternative
		viskitConfiguration.setValue(ViskitConfiguration.PROJECT_DESCRIPTION_KEY, "*Enter new project description*");
		
		boolean pathEditable = false; // TODO consider true; if a change-your-mind chooser is added to editProjectProperties panel		
		ViskitGlobals.instance().getEventGraphViewFrame().editProjectProperties(pathEditable); // user panel for Project Properties

		if (viskitConfiguration.getValue(ViskitConfiguration.PROJECT_PROPERTIES_EDIT_COMPLETED_KEY).equals("false"))
			return; // user cancelled Project Properties update, do not continue
		
		if (!viskitConfiguration.getValue(ViskitConfiguration.PROJECT_NAME_KEY).equals(newProjectRoot.getName()))
		{
			// project name changed, TODO may require further error checking
			// simpler, better to restring renaming to Projects menu item
		}
//		viskitProject.cleanAll(); // dangerous to users file system!
		
        viskitProject = new ViskitProject(newProjectRoot); // reset
		boolean initializedOK = viskitProject.initializeProject ();
		if (!initializedOK)
		{
			messageToUser(JOptionPane.ERROR_MESSAGE, "Problem initializing new project", "Please check logs to find or report problem");
			return;
		}
		viskitProject.setProjectRootDirectory (newProjectRoot);
		ViskitProject.MY_VISKIT_PROJECTS_DIR = newProjectRoot.getParent();
		viskitProject.setProjectName       (viskitConfiguration.getValue(ViskitConfiguration.PROJECT_NAME_KEY));
		viskitProject.setProjectOpen(true);
		viskitProject.setProjectAuthor     (viskitConfiguration.getValue(ViskitConfiguration.PROJECT_AUTHOR_KEY));
		viskitProject.setProjectRevision   (viskitConfiguration.getValue(ViskitConfiguration.PROJECT_REVISION_KEY));
		viskitProject.setProjectDescription(viskitConfiguration.getValue(ViskitConfiguration.PROJECT_DESCRIPTION_KEY));
		viskitProject.saveProjectFile ();
		
		ViskitGlobals.setCurrentViskitProject(viskitProject);
        ViskitStatics.setViskitProjectDirectory(projectDirectory);
//		ViskitGlobals.instance().initializeProjectHomeDirectory();
//		ViskitGlobals.instance().createWorkDirectory();

        UserPreferencesDialog.saveExtraClassPathEntries(viskitProject.getProjectClasspathArray());
		
		// For a brand new empty project open a default Event Graph
		File[] eventGraphFileArray = viskitProject.getEventGraphsDirectory().listFiles();
		if (eventGraphFileArray.length == 0)
		{
			((EventGraphController)ViskitGlobals.instance().getEventGraphController()).newEventGraph();
		}
		ViskitGlobals.instance().getEventGraphViewFrame().buildMenus();   // reset
		ViskitGlobals.instance().getAssemblyEditViewFrame().buildMenus(); // reset
        ViskitGlobals.instance().getViskitApplicationFrame().showProjectName();
    }

    public final static String ZIP_AND_MAIL_PROJECT_METHOD = "zipAndMailProject"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void zipAndMailProject () // method name must exactly match preceding string value
	{
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            File projectDirectory;
            File projectZip;
            File logFile;

            @Override
            public Void doInBackground() {

                projectDirectory = ViskitGlobals.instance().getCurrentViskitProject().getProjectRootDirectory();
                projectZip = new File(projectDirectory.getParentFile(), projectDirectory.getName() + ".zip");
                logFile = new File(projectDirectory, "debug.log");

                if (projectZip.exists())
                    projectZip.delete();

                if (logFile.exists())
                    logFile.delete();

                try {

                    // First, copy the debug.log to the project dir
                    Files.copy(ViskitConfiguration.V_DEBUG_LOG.toPath(), logFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    ZipUtils.zipFolder(projectDirectory, projectZip);
                } catch (IOException e) {
                    LOG.error(e);
                }

                return null;
            }

            @Override
            public void done() {
                try {

                    // Waits for the zip process to finish
                    get();

                    URL url = null;
					String urlString = "[not found]";
                    try {
                        url = new URL("mailto:" + ViskitStatics.VISKIT_MAILING_LIST
                                + "?subject=Viskit%20Project%20Submission%20for%20"
                                + projectDirectory.getName() + "&body=see%20attachment");
						urlString =  url.toString();
                    } catch (MalformedURLException e) {
                        LOG.error(e);
                    }

                    String message = "Please navigate to<br/>"
                            + projectZip.getParent()
                            + "<br/>and email the " + projectZip.getName()
                            + " file to "
                            + "<b><a href=\"" + urlString + "\">"
                            + ViskitStatics.VISKIT_MAILING_LIST + "</a></b>"
                            + "<br/><br/>Click the link to open up an email "
                            + "form, then attach the zip file";

                    try {
                        Desktop.getDesktop().open(projectZip.getParentFile());
                    } catch (IOException e) {
                        LOG.error(e);
                    }

                    ViskitStatics.showHyperlinkedDialog((Component) getView(), "Viskit Project: " + projectDirectory.getName(), url, message, false);

                } catch (InterruptedException | ExecutionException e) {
                    LOG.error(e);
                }
            }
        };
        worker.execute();
    }

    /** Common method between the AssemblyView and this AssemblyyController
     *
     * @return indication to continue (true) or cancel (false)
     */
    public boolean confirmProjectClosing ()
	{
        boolean continueClosing = true; // default: continue closing unless user says to cancel
		
		if (ViskitGlobals.instance().getCurrentViskitProject() == null)
		{
			return continueClosing; // project is closed already
		}
		else if (ViskitGlobals.instance().getCurrentViskitProject().isProjectOpen())
		{
            String message = "Are you sure you want to close your current Viskit Project?";
            String title   = "Close Current Project";
			if (ViskitGlobals.instance().getCurrentViskitProject() != null)
				title += ": " + ViskitGlobals.instance().getCurrentViskitProject().getProjectName();
            int responseValue = ((AssemblyView) getView()).genericAskYN(title, message);
            if (responseValue == JOptionPane.YES_OPTION) {
                doProjectCleanup();
            } else {
                continueClosing = false;
            }
			return continueClosing; // user decision
        }
		else return continueClosing; // existing project is closed already
    }

    @Override
    public void doProjectCleanup()
	{
        closeAll();
        ((EventGraphController) ViskitGlobals.instance().getEventGraphController()).closeAll();
        ViskitConfiguration.instance().clearViskitConfiguration();
        clearRecentAssemblyFileList();
        ((EventGraphController) ViskitGlobals.instance().getEventGraphController()).clearRecentEventGraphFileSet();
        ViskitGlobals.instance().getCurrentViskitProject().closeProject();
    }

    @Override
    public void openProject(File projectDirectory)
	{
        ViskitStatics.setViskitProjectDirectory(projectDirectory);
        ViskitGlobals.instance().createWorkDirectory();

        // Add our currently opened project to the recently opened projects list
        adjustRecentProjectFileSet(projectDirectory);
		ViskitGlobals.instance().getEventGraphViewFrame().buildMenus(); // reset
		ViskitGlobals.instance().getAssemblyEditViewFrame().buildMenus();   // reset
		ViskitApplicationFrame viskitApplicationFrame = ViskitGlobals.instance().getViskitApplicationFrame();
		if (!viskitApplicationFrame.isEventGraphEditorTabSelected() && !viskitApplicationFrame.isAssemblyEditorTabSelected())
			 viskitApplicationFrame.displayAssemblyEditorTab(); // select relevant tab
    }

    public final static String NEWASSEMBLY_METHOD = "newAssembly"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void newAssembly () // method name must exactly match preceding string value
	{
        // Don't allow a new assembly to be created if a current project is  not open
        if ((ViskitGlobals.instance().getCurrentViskitProject() == null) ||
			!ViskitGlobals.instance().getCurrentViskitProject().isProjectOpen()) 
		{
			messageToUser (JOptionPane.WARNING_MESSAGE, "No project directory", 
					"<html><p>New assemblies are only created within an open project.</p>" +
					"<p>Open or create a project first.</p>");
			return;
		}
		ViskitGlobals.instance().getViskitApplicationFrame().displayAssemblyEditorTab(); // prerequisite to possible menu

        GraphMetadata priorAssemblyMetadata = null;
        AssemblyModelImpl priorAssemblyModel = (AssemblyModelImpl) getModel();
        if (priorAssemblyModel != null) {
            priorAssemblyMetadata = priorAssemblyModel.getMetadata();
        }

        AssemblyModelImpl assemblyModel = new AssemblyModelImpl(this);
        assemblyModel.initialize();
        assemblyModel.newModel(null); // no file

        // No vAssemblyModel set in controller yet...it gets set
        // when TabbedPane changelistener detects a tab change.
        ((AssemblyView) getView()).addTab(assemblyModel);

        GraphMetadata newAssemblyMetadata = new GraphMetadata(assemblyModel);   // build a new one, specific to Assembly
        if (priorAssemblyMetadata != null) {
            newAssemblyMetadata.packageName = priorAssemblyMetadata.packageName;
        }
		newAssemblyMetadata.description = "TODO: enter a description for this new assembly";

        boolean modified = AssemblyMetadataDialog.showDialog((JFrame) getView(), newAssemblyMetadata);
        if (modified)
		{
            ((AssemblyModel) getModel()).setMetadata(newAssemblyMetadata);

            // update title bar
            ((AssemblyView) getView()).setSelectedAssemblyName(newAssemblyMetadata.name);

            // TODO: Implement this
//            ((AssemblyView)  getView()).setSelectedEventGraphDescription(graphMetadata.description);
        } 
		else 
		{
            ((AssemblyView) getView()).deleteTab(assemblyModel);
        }
		ViskitGlobals.instance().getViskitApplicationFrame().displayAssemblyEditorTab(); // prerequisite to buildMenus
		ViskitGlobals.instance().getViskitApplicationFrame().buildMenus(); // reset
    }

    /**
     * A component wants to say something.
     *
     * @param dialogType the type of dialog popup, i.e. WARN, ERROR, INFO, QUESTION
     * @param title the title of the dialog frame
     * @param message the information to present
     */
    @Override
    public void messageToUser(int dialogType, String title, String message) // dialogType is one of JOptionPane types
    {   AssemblyView view = (AssemblyView) getView();
        if (view != null)
            ((AssemblyView) getView()).genericReport(dialogType, title, message);
        else {
            JOptionPane.showMessageDialog(null, message, title, dialogType);
        }
    }

    @Override
    public void quit()
	{
        if (preQuit())
		{
            postQuit();
        }
    }

    @Override
    public boolean preQuit() {

        // Check for dirty models before exiting, first ask if user wants to save them
        AssemblyModel[] openAssemblyModels = ((AssemblyView) getView()).getOpenAssemblyModelArray();
        for (AssemblyModel assemblyModel : openAssemblyModels)
		{
            setModel((mvcModel) assemblyModel);

            // Check for a canceled exit
            if (!preClose()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void postQuit()
	{
        ViskitGlobals.instance().quitAssemblyEditor();
    }

    private boolean closeAll = false;

    public final static String CLOSEALL_METHOD = "closeAll"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void closeAll () // method name must exactly match preceding string value
	{
        AssemblyModel[] assemblyModelArray = ((AssemblyView) getView()).getOpenAssemblyModelArray();
        for (AssemblyModel assemblyModel : assemblyModelArray) {
            setModel((mvcModel) assemblyModel);
			setCloseAll(true);
            close();
        }
		// included in close(): ViskitGlobals.instance().getEventGraphViewFrame().buildMenus(); // reset
        setCloseAll(false);
    }

    public final static String CLOSE_METHOD = "close"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void close () // method name must exactly match preceding string value
	{
        if (preClose()) {
            postClose();
        }
//		updateAssemblyFileLists(); // save for next time
		ViskitGlobals.instance().getAssemblyEditViewFrame().buildMenus(); // reset
    }

    @Override
    public boolean preClose() {
        AssemblyModel assemblyModel = (AssemblyModel) getModel();
        if (assemblyModel == null) {
            return false;
        }
		openAssembliesFileList.remove(assemblyModel.getLastFile());
        if (assemblyModel.isDirty()) {
            return checkSaveIfDirty();
        }
        return true;
    }

    @Override
    public void postClose() {
        OpenAssembly.inst().doSendCloseAssembly();
    }

    private void markAssemblyConfigurationClosed(File f) {

        // Someone may try to close a file that hasn't been saved
        if (f == null) {return;}

        int idx = 0;
        for (File key : recentAssemblyFileSet) {
            if (key.getPath().contains(f.getName())) {
                historyXMLConfiguration.setProperty(ViskitConfiguration.ASSEMBLY_HISTORY_KEY + "(" + idx + ")[@open]", "false");
            }
            idx++;
        }
    }

    // The open attribute is zeroed out for all recent files the first time a file is opened
    private void markAssemblyConfigurationOpen(String path) {

        int idx = 0;
        for (File tempPath : recentAssemblyFileSet) {

            if (tempPath.getPath().equals(path)) {
                historyXMLConfiguration.setProperty(ViskitConfiguration.ASSEMBLY_HISTORY_KEY + "(" + idx + ")[@open]", "true");
            }
            idx++;
        }
    }

    private final Point nextPoint = new Point(25, 25);
    private Point getNextPoint() {
        nextPoint.x = nextPoint.x >= 200 ? 25 : nextPoint.x + 25;
        nextPoint.y = nextPoint.y >= 200 ? 25 : nextPoint.y + 25;
        return nextPoint;
    }

    public final static String NEWEVENTGRAPH_METHOD = "newEventGraphNode"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void newEventGraphNode () // method name must exactly match preceding string value
    {
        Object o = ((AssemblyView) getView()).getSelectedEventGraph();

        if (o != null) {
            if (o instanceof Class<?>) {
                newEventGraphNode(((Class<?>) o).getName(), getNextPoint());
                return;
            } else if (o instanceof FileBasedAssemblyNode) {
                newFileBasedEventGraphNode((FileBasedAssemblyNode) o, getNextPoint());
                return;
            }
        }
        // Nothing selected or non-leaf
        messageToUser(JOptionPane.ERROR_MESSAGE, "Can't create a new node", "You must first open an Event Graph before adding a new node.");
    }

    @Override
    public void newEventGraphNode(String typeName, Point p) {
        String shName = shortEventGraphName(typeName);
        ((AssemblyModel) getModel()).newEventGraph(shName, typeName, p);
    }

    @Override
    public void newFileBasedEventGraphNode(FileBasedAssemblyNode xnode, Point p) {
        String shName = shortEventGraphName(xnode.loadedClass);
        ((AssemblyModel) getModel()).newEventGraphFromXML(shName, xnode, p);
    }


    public final static String NEWPCLNODE_METHOD = "newPropertyChangeListenerNode"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void newPropertyChangeListenerNode () // method name must exactly match preceding string value
    {
        Object o = ((AssemblyView) getView()).getSelectedPropertyChangeListener();

        if (o != null) {
            if (o instanceof Class<?>) {
                newPropertyChangeListenerNode(((Class<?>) o).getName(), getNextPoint());
                return;
            } else if (o instanceof FileBasedAssemblyNode) {
                newFileBasedPropertyChangeListenerNode((FileBasedAssemblyNode) o, getNextPoint());
                return;
            }
        }
        // If nothing selected or a non-leaf
        messageToUser(JOptionPane.ERROR_MESSAGE, "Can't create", "You must first select a Property Change Listener from the panel on the left.");
    }

    @Override
    public void newPropertyChangeListenerNode(String name, Point p) {
        String shName = shortPropertyChangeListenerName(name);
        ((AssemblyModel) getModel()).newPropertyChangeListener(shName, name, p);
    }

    @Override
    public void newFileBasedPropertyChangeListenerNode(FileBasedAssemblyNode xnode, Point p) {
        String shName = shortPropertyChangeListenerName(xnode.loadedClass);
        ((AssemblyModel) getModel()).newPropertyChangeListenerFromXML(shName, xnode, p);
    }

    /**
     *
     * @return true = continue, false = don't (i.e., we canceled)
     */
    private boolean askToSaveAndContinue() {
        int yn = (((AssemblyView) getView()).genericAsk("Question", "Save modified assembly?"));

        switch (yn) {
            case JOptionPane.YES_OPTION:
                save();

                // TODO: Can't remember why this is here after a save?
                if (((AssemblyModel) getModel()).isDirty()) {
                    return false;
                } // we cancelled
                return true;
            case JOptionPane.NO_OPTION:

                // No need to recompile
                if (((AssemblyModel) getModel()).isDirty()) {
                    ((AssemblyModel) getModel()).setDirty(false);
                }
                return true;
            case JOptionPane.CANCEL_OPTION:
                return false;

            // Something funny if we're here
            default:
                return false;
        }
    }

    @Override
    public void newAdapterArc(Object[] nodes) {
        AssemblyNode oA = (AssemblyNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        AssemblyNode oB = (AssemblyNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();

        AssemblyNode[] oArr;
        try {
            oArr = checkLegalForSimEventListenerArc(oA, oB);
        } catch (Exception e) {
            messageToUser(JOptionPane.ERROR_MESSAGE, "Connection error.", "Possible class not found.  All referenced entities must be in a list at left.");
            return;
        }
        if (oArr == null) {
            messageToUser(JOptionPane.ERROR_MESSAGE, "Incompatible connection", "The nodes must be a SimEventListener and SimEventSource combination.");
            return;
        }
        adapterEdgeEdit(((AssemblyModel) getModel()).newAdapterEdge(shortAdapterName(""), oArr[0], oArr[1]));
    }

    @Override
    public void newSimEvListArc(Object[] nodes) {
        AssemblyNode oA = (AssemblyNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        AssemblyNode oB = (AssemblyNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();

        AssemblyNode[] oArr = checkLegalForSimEventListenerArc(oA, oB);

        if (oArr == null) {
            messageToUser(JOptionPane.ERROR_MESSAGE, "Incompatible connection", "The node connection must be a SimEventListener and SimEventSource combination.");
            return;
        }
        ((AssemblyModel) getModel()).newSimEvLisEdge(oArr[0], oArr[1]);
    }

    @Override
    public void newPropertyChangeListArc(Object[] nodes) {
        // One and only one has to be a prop change listener
        AssemblyNode oA = (AssemblyNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        AssemblyNode oB = (AssemblyNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();

        AssemblyNode[] oArr = checkLegalForPropertyChangeArc(oA, oB);

        if (oArr == null) {
            messageToUser(JOptionPane.ERROR_MESSAGE, "Incompatible connection", "The node connection must be a PropertyChangeListener and PropertyChangeSource combination.");
            return;
        }
        propertyChangeListenerEdgeEdit(((AssemblyModel) getModel()).newPropChangeEdge(oArr[0], oArr[1]));
    }

    AssemblyNode[] checkLegalForSimEventListenerArc(AssemblyNode a, AssemblyNode b) {
        Class<?> ca = findClass(a);
        Class<?> cb = findClass(b);
        return orderSELSrcAndLis(a, b, ca, cb);
    }

    AssemblyNode[] checkLegalForPropertyChangeArc(AssemblyNode a, AssemblyNode b) {
        Class<?> ca = findClass(a);
        Class<?> cb = findClass(b);
        return orderPCLSrcAndLis(a, b, ca, cb);
    }

    Class<?> findClass(AssemblyNode o) {
        return ViskitStatics.classForName(o.getType());
    }

    AssemblyNode[] orderPCLSrcAndLis(AssemblyNode a, AssemblyNode b, Class<?> ca, Class<?> cb) {
        AssemblyNode[] obArr = new AssemblyNode[2];
        // tbd, reloading these classes is needed right now as
        // we don't know if the workClassLoader is the same instance
        // as it used to be when these were originally loaded
        // the tbd here is to see if there can be a shared root loader
        simEvSrcClass = ViskitStatics.classForName("simkit.SimEventSource");
        simEvLisClass = ViskitStatics.classForName("simkit.SimEventListener");
        propChgSrcClass = ViskitStatics.classForName("simkit.PropertyChangeSource");
        propChgLisClass = ViskitStatics.classForName("java.beans.PropertyChangeListener");
        if (propChgSrcClass.isAssignableFrom(ca)) {
            obArr[0] = a;
        } else if (propChgSrcClass.isAssignableFrom(cb)) {
            obArr[0] = b;
        }
        if (propChgLisClass.isAssignableFrom(cb)) {
            obArr[1] = b;
        } else if (propChgLisClass.isAssignableFrom(ca)) {
            obArr[1] = a;
        }

        if (obArr[0] == null || obArr[1] == null || obArr[0] == obArr[1]) {
            return null;
        }
        return obArr;
    }

    AssemblyNode[] orderSELSrcAndLis(AssemblyNode a, AssemblyNode b, Class<?> ca, Class<?> cb) {
        AssemblyNode[] obArr = new AssemblyNode[2];
        simEvSrcClass = ViskitStatics.classForName("simkit.SimEventSource");
        simEvLisClass = ViskitStatics.classForName("simkit.SimEventListener");
        propChgSrcClass = ViskitStatics.classForName("simkit.PropertyChangeSource");
        propChgLisClass = ViskitStatics.classForName("java.beans.PropertyChangeListener");
        if (simEvSrcClass.isAssignableFrom(ca)) {
            obArr[0] = a;
        } else if (simEvSrcClass.isAssignableFrom(cb)) {
            obArr[0] = b;
        }
        if (simEvLisClass.isAssignableFrom(cb)) {
            obArr[1] = b;
        } else if (simEvLisClass.isAssignableFrom(ca)) {
            obArr[1] = a;
        }

        if (obArr[0] == null || obArr[1] == null || obArr[0] == obArr[1]) {
            return null;
        }
        return obArr;
    }

    /**
     *
     * @param pclNode Property Change Listener Node
     */
    @Override
    public void propertyChangeListenerEdit(PropertyChangeListenerNode pclNode) {
        boolean done;
        do {
            done = true;
            boolean modified = ((AssemblyView) getView()).doEditPropertyChangeListenerNode(pclNode);
            if (modified) {
                done = ((AssemblyModel) getModel()).changePclNode(pclNode);
            }
        } while (!done);
    }

    @Override
    public void eventGraphEdit(EventGraphNode eventNode) {
        boolean done;
        do {
            done = true;
            boolean modified = ((AssemblyView) getView()).doEditEventGraphNode(eventNode);
            if (modified) {
                done = ((AssemblyModel) getModel()).changeEvGraphNode(eventNode);
            }
        } while (!done);
    }

    @Override
    public void propertyChangeListenerEdgeEdit(PropertyChangeListenerEdge pclEdge) {
        boolean modified = ((AssemblyView) getView()).doEditPropertyChangeListenerEdge(pclEdge);
        if (modified) {
            ((AssemblyModel) getModel()).changePclEdge(pclEdge);
        }
    }

    @Override
    public void adapterEdgeEdit(AdapterEdge aEdge) {
        boolean modified = ((AssemblyView) getView()).doEditAdapterEdge(aEdge);
        if (modified) {
            ((AssemblyModel) getModel()).changeAdapterEdge(aEdge);
        }
    }

    @Override
    public void simEventListenerEdgeEdit(SimEventListenerEdge seEdge) {
        boolean modified = ((AssemblyView) getView()).doEditSimEventListEdge(seEdge);
        if (modified) {
            ((AssemblyModel) getModel()).changeSimEvEdge(seEdge);
        }
    }

    private Vector<Object> selectionVector = new Vector<>();
    private Vector<Object> copyVector = new Vector<>();

    @Override
    public void selectNodeOrEdge(Vector<Object> v) {
        selectionVector = v;
        boolean selected = nodeOrEdgeSelected();

        // Cut not supported
        ActionIntrospector.getAction(this, "cut").setEnabled(false);
        ActionIntrospector.getAction(this, "remove").setEnabled(selected);
        ActionIntrospector.getAction(this, "copy").setEnabled(selected);
    }

    private boolean nodeCopied() {
        return nodeOrEdgeInVector(copyVector);
    }

    private boolean nodeOrEdgeSelected() {
        return nodeOrEdgeInVector(selectionVector);
    }

    private boolean nodeOrEdgeInVector(Vector v) {
        for (Object o : v) {
            if (o instanceof AssemblyNode) {
                return true;
            }
            if (o instanceof AssemblyEdge) {
                return true;
            }
        }
        return false;
    }

    private boolean doRemove = false;

    public final static String REMOVE_METHOD = "remove"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void remove () // method name must exactly match preceding string value
	{
        if (!selectionVector.isEmpty()) {
            // first ask:
            String msg = "";
            int nodeCount = 0;  // different msg for edge delete
            for (Object o : selectionVector) {
                if (o instanceof AssemblyNode) {
                    nodeCount++;
                }
                String s = o.toString();
                s = s.replace('\n', ' ');
                msg += ", \n" + s;
            }
            String specialNodeMsg = (nodeCount > 0) ? "\n(All unselected but attached edges will also be removed.)" : "";
            doRemove = ((AssemblyView) getView()).genericAsk("Remove element(s)?", "Confirm remove" + msg + "?" + specialNodeMsg) == JOptionPane.YES_OPTION;
            if (doRemove) {
                // do edges first?
                delete();
            }
        }
    }

    public final static String CUT_METHOD = "cut"; // must match following method name.  not possible to accomplish this programmatically.
	/**
	 * Not supported
	 */
    @Override
    public void cut () // method name must exactly match preceding string value
    {
        // Not supported
    }

    public final static String COPY_METHOD = "copy"; // must match following method name.  not possible to accomplish this programmatically.
    @Override
    @SuppressWarnings("unchecked")
    public void copy () // method name must exactly match preceding string value
	{
        if (selectionVector.isEmpty()) {
            messageToUser(JOptionPane.WARNING_MESSAGE,
                    "Unsupported Action",
                    "Edges cannot be copied.");
            return;
        }
        copyVector = (Vector<Object>) selectionVector.clone();

        // Paste only works for a node, check to enable/disable paste menu item
        handlePasteMenuItem();
    }

    private void handlePasteMenuItem() {
        ActionIntrospector.getAction(this, "paste").setEnabled(nodeCopied());
    }

    /** Also acts as a bias for point offset */
    private int copyCount = 0;

    public final static String PASTE_METHOD = "paste"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void paste () // method name must exactly match preceding string value
    {
        if (copyVector.isEmpty()) {
            return;
        }
        int x = 100, y = 100;
        int offset = 20;
        for (Object o : copyVector) {
            if (o instanceof AssemblyEdge) {
                continue;
            }

            Point2D p = new Point(x + (offset * copyCount), y + (offset * copyCount));
            if (o instanceof EventGraphNode) {
                String nm = ((ViskitElement) o).getName();
                String typ = ((ViskitElement) o).getType();
                ((AssemblyModel) getModel()).newEventGraph(nm + "-copy" + copyCount, typ, p);
            } else if (o instanceof PropertyChangeListenerNode) {
                String nm = ((ViskitElement) o).getName();
                String typ = ((ViskitElement) o).getType();
                ((AssemblyModel) getModel()).newPropertyChangeListener(nm + "-copy" + copyCount, typ, p);
            }
            copyCount++;
        }
    }

    /** Permanently delete, or undo selected nodes and attached edges from the cache */
    @SuppressWarnings("unchecked")
    private void delete() {
        Vector<Object> v = (Vector<Object>) selectionVector.clone();   // avoid concurrent update
        for (Object elem : v) {
            if (elem instanceof AssemblyEdge) {
                removeEdge((AssemblyEdge) elem);
            } else if (elem instanceof EventGraphNode) {
                EventGraphNode en = (EventGraphNode) elem;
                for (AssemblyEdge ed : en.getConnections()) {
                    removeEdge(ed);
                }
                ((AssemblyModel) getModel()).deleteEvGraphNode(en);
            } else if (elem instanceof PropertyChangeListenerNode) {
                PropertyChangeListenerNode en = (PropertyChangeListenerNode) elem;
                for (AssemblyEdge ed : en.getConnections()) {
                    removeEdge(ed);
                }
                ((AssemblyModel) getModel()).deletePropertyChangeListener(en);
            }
        }

        // Clear the cache after a delete to prevent unnecessary buildup
        if (!selectionVector.isEmpty())
            selectionVector.clear();
    }

    private void removeEdge(AssemblyEdge e) {
        if (e instanceof AdapterEdge) {
            ((AssemblyModel) getModel()).deleteAdapterEdge((AdapterEdge) e);
        } else if (e instanceof PropertyChangeListenerEdge) {
            ((AssemblyModel) getModel()).deletePropChangeEdge((PropertyChangeListenerEdge) e);
        } else if (e instanceof SimEventListenerEdge) {
            ((AssemblyModel) getModel()).deleteSimEvLisEdge((SimEventListenerEdge) e);
        }
    }

    /** Assume the last selected element is our candidate for a redo */
    private DefaultGraphCell redoGraphCell;

    /** Inform the model that this in an undo, not full delete */
    private boolean isUndo = false;

    /**
     * Informs the model that this is an undo, not a full delete
     * @return true if an undo, not a full delete
     */
    public boolean isUndo() {
        return isUndo;
    }

    public final static String UNDO_METHOD = "undo"; // must match following method name.  not possible to accomplish this programmatically.
    /**
     * Removes the last selected node or edge from the JGraph model
     */
    @Override
    public void undo () // method name must exactly match preceding string value
	{
        isUndo = true;

        AssemblyEditViewFrame view = (AssemblyEditViewFrame) getView();
        vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentVgraphAssemblyComponentWrapper().getUndoManager();

        Object[] roots = view.getCurrentVgraphAssemblyComponentWrapper().getRoots();
        redoGraphCell = (DefaultGraphCell) roots[roots.length - 1];

        // Prevent dups
        if (!selectionVector.contains(redoGraphCell.getUserObject()))
            selectionVector.add(redoGraphCell.getUserObject());

        remove();

        if (!doRemove) {return;}

        try {

            // This will clear the selectionVector via callbacks
            undoMgr.undo(view.getCurrentVgraphAssemblyComponentWrapper().getGraphLayoutCache());
        } catch (CannotUndoException ex) {
            LOG.error("Unable to undo: " + ex);
        } finally {
            updateUndoRedoStatus();
        }
    }

    public final static String REDO_METHOD = "redo"; // must match following method name.  Not possible to accomplish this programmatically.
    /**
     * Replaces the last selected node or edge from the JGraph model
     */
    @Override
    public void redo () // method name must exactly match preceding string value
	{
        // Recreate the JAXB (XML) bindings since the paste function only does
        // nodes and not edges
        if (redoGraphCell instanceof org.jgraph.graph.Edge) { // TODO fix

            // Handles both arcs and self-referential arcs
            if (redoGraphCell.getUserObject() instanceof AdapterEdge) {
                AdapterEdge ed = (AdapterEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoAdapterEdge(ed);
            } else if (redoGraphCell.getUserObject() instanceof PropertyChangeListenerEdge) {
                PropertyChangeListenerEdge ed = (PropertyChangeListenerEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoPropChangeEdge(ed);
            } else {
                SimEventListenerEdge ed = (SimEventListenerEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoSimEvLisEdge(ed);
            }
        } else {

            if (redoGraphCell.getUserObject() instanceof PropertyChangeListenerNode) {
                PropertyChangeListenerNode node = (PropertyChangeListenerNode) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoPropertyChangeListener(node);
            } else {
                EventGraphNode node = (EventGraphNode) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoEventGraph(node);
            }
        }

        AssemblyEditViewFrame view = (AssemblyEditViewFrame) getView();
        vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentVgraphAssemblyComponentWrapper().getUndoManager();
        try {
            undoMgr.redo(view.getCurrentVgraphAssemblyComponentWrapper().getGraphLayoutCache());
        } catch (CannotRedoException ex) {
            LOG.error("Unable to redo: " + ex);
        } finally {
            updateUndoRedoStatus();
        }
    }

    /** Toggles the undo/redo Edit menu items on/off */
    public void updateUndoRedoStatus() {
        AssemblyEditViewFrame view = (AssemblyEditViewFrame) getView();
        vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentVgraphAssemblyComponentWrapper().getUndoManager();

        ActionIntrospector.getAction(this, UNDO_METHOD).setEnabled(undoMgr.canUndo(view.getCurrentVgraphAssemblyComponentWrapper().getGraphLayoutCache()));
        ActionIntrospector.getAction(this, REDO_METHOD).setEnabled(undoMgr.canRedo(view.getCurrentVgraphAssemblyComponentWrapper().getGraphLayoutCache()));

        isUndo = false;
    }

    public final static String SHOWXML_METHOD = "showXML"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void showXML () // method name must exactly match preceding string value
	{
        AssemblyModel vmod = (AssemblyModel) getModel();
        if (!checkSaveForSourceCompile() || vmod.getLastFile() == null) {
            return;
        }

        ((AssemblyView) getView()).displayXML(vmod.getLastFile());
    }

    private boolean checkSaveForSourceCompile() {
        AssemblyModel vmod = (AssemblyModel) getModel();

        // Perhaps a cached file is no longer present in the path
        if (vmod == null) {return false;}
        if (vmod.isDirty() || vmod.getLastFile() == null) {
            int ret = ((AssemblyView) getView()).genericAskYN("Confirm", "The model will be saved.\nContinue?");
            if (ret != JOptionPane.YES_OPTION) {
                return false;
            }
            this.saveAs();
        }
        return true;
    }

    public final static String JAVASOURCE_METHOD = "generateJavaSource"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void generateJavaSource () // method name must exactly match preceding string value
	{
        String source = produceJavaAssemblyClass();
        AssemblyModel vmod = (AssemblyModel) getModel();
        if (source != null && !source.isEmpty()) {
            String className = vmod.getMetadata().packageName + "." + vmod.getMetadata().name;
            ((AssemblyView) getView()).showAndSaveSource(className, source, vmod.getLastFile().getName());
        }
    }

    private String produceJavaAssemblyClass()
	{
        AssemblyModel assemblyModel = (AssemblyModel) getModel();
        if (!checkSaveForSourceCompile() || assemblyModel.getLastFile() == null) {
			// TODO error notification needed, or is it already getting handled?
            return null;
        }
		String sourceCode = buildJavaAssemblySource(assemblyModel.getLastFile());
        return sourceCode;
    }

    /**
     * Builds the actual source code from the Assembly XML after a successful
     * XML validation.  These routines to operate on the current assembly.
     *
     * @param f the Assembly file to produce source from
     * @return a string of Assembly source code
     */
    public String buildJavaAssemblySource(File f) {
        // Must validate XML first and handle any errors before compiling
        XMLValidationTool xmlValidationTool = new XMLValidationTool(f, new File(XMLValidationTool.LOCAL_ASSEMBLY_SCHEMA));

        if (!xmlValidationTool.isValidXML()) {
			String message = f.getName() + " is not valid XML";
			messageToUser(JOptionPane.ERROR_MESSAGE, "Invalid Assembly XML", message);
            LOG.error(message + "\n");
            // TODO: implement a Dialog pointing to the validationErrors.LOG
            return null;
        } else {
            LOG.info(f.getName() + " is valid XML\n");
        }

        SimkitAssemblyXML2Java simkitAssemblyXML2Java = null;
        try {
            simkitAssemblyXML2Java = new SimkitAssemblyXML2Java(f);
            simkitAssemblyXML2Java.unmarshal();
        } catch (FileNotFoundException e) {
			String message = f.getName() + " updated Assembly XML file not found when unmarshalling";
			messageToUser(JOptionPane.ERROR_MESSAGE, "XML file not found", message);
            LOG.error(e);
        }
		if  (simkitAssemblyXML2Java != null)
		  	 return simkitAssemblyXML2Java.translate();
		else 
		{
			String message = f.getName() + " autogenenerated Java file not found";
			messageToUser(JOptionPane.ERROR_MESSAGE, "File not found", message);
			return "Error, no Java source produced";
		}
    }

    /**
     * Build the actual source code from the Event Graph XML after a successful
     * XML validation
     *
     * @param simkitXML2Java the Event Graph initialized translator to produce source with
     * @return a string of Event Graph source code
     */
    public String buildJavaEventGraphSource(SimkitXML2Java simkitXML2Java) {
        String eventGraphSource;

        // Must validate XML first and handle any errors before compiling
        XMLValidationTool xmlValidationTool = 
				      new XMLValidationTool(simkitXML2Java.getEventGraphFile(),
                                            new File(XMLValidationTool.LOCAL_EVENT_GRAPH_SCHEMA));

        if (!xmlValidationTool.isValidXML())
		{
			String message = simkitXML2Java.getEventGraphFile().getName() + " is not valid XML";
			messageToUser(JOptionPane.ERROR_MESSAGE, " Invalid Event Graph XML", message);
			LOG.error(message + "\n");
            return null;
        } else {
            LOG.info(simkitXML2Java.getEventGraphFile() + " is valid XML\n");
        }

        try {
            eventGraphSource = simkitXML2Java.translate();
        } 
		catch (Exception e)
		{
			String message = "Error building Java from " + simkitXML2Java.getFileBaseName() +
                             ":\n" + e.getMessage() + ", erroneous event-graph xml found";
			messageToUser(JOptionPane.ERROR_MESSAGE, "Failed Event Graph conversion to Java", message);
			LOG.error(message + "\n");
			eventGraphSource = "Error, no Java source produced";
        }
        return eventGraphSource;
    }

    /** Create and test compile our EventGraphs and Assemblies from XML
     *
     * @param sourceCode the translated source either from SimkitXML2Java, or SimkitAssemblyXML2Java
     * @return a reference to a successfully compiled *.class file or null if
     * a compile failure occurred
     */
    public static File compileJavaClassFromString(String sourceCode) {
        String baseName;

        // Find the package subdirectory
        Pattern pattern = Pattern.compile("package.+;");
        Matcher matcher = pattern.matcher(sourceCode);
        boolean findResult = matcher.find();

        String packagePath = "";
        String packageName = "";
        if (findResult) {
            int st = matcher.start();
            int end = matcher.end();
            String s = sourceCode.substring(st, end);
            packageName = sourceCode.substring(st, end - 1);
            packageName = packageName.substring("package".length(), packageName.length()).trim();
            s = s.replace(';', File.separatorChar);
            String[] sa = s.split("\\s");
            sa[1] = sa[1].replace('.', File.separatorChar);
            packagePath = sa[1].trim();
        }

        pattern = Pattern.compile("public\\s+class\\s+");
        matcher = pattern.matcher(sourceCode);
        matcher.find();

        int end = matcher.end();
        String s = sourceCode.substring(end, sourceCode.length()).trim();
        String[] sa = s.split("\\s+");

        baseName = sa[0];

        FileWriter fw = null;
        boolean compileSuccess;
        try {

            // Should always have a live ViskitProject
            ViskitProject viskitProject = ViskitGlobals.instance().getCurrentViskitProject();

            // Create, or find the project's java source and package
            File sourcePackage = new File(viskitProject.getSrcDirectory(), packagePath);
            if (!sourcePackage.isDirectory()) {
                 sourcePackage.mkdirs();
            }
            File javaFile = new File(sourcePackage, baseName + ".java");
            javaFile.createNewFile();

            fw = new FileWriter(javaFile);
            fw.write(sourceCode);

            // An error stream to write additional error info out to
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Compiler.setOutputStream(byteArrayOutputStream);

            File classesDirectory = viskitProject.getClassesDirectory();

            LOG.info("Test compiling " + javaFile.getCanonicalPath());

            // This will create a class/package to place the .class file
            String diagnostic = Compiler.invoke(packageName, baseName, sourceCode);
            compileSuccess = diagnostic.equals(Compiler.COMPILE_SUCCESS_MESSAGE);
            if (compileSuccess)
			{
                LOG.info(diagnostic + "\n");
                return new File(classesDirectory, packagePath + baseName + ".class");
            } 
			else
			{
				String message = "Error compiling " + javaFile.getCanonicalPath();
                LOG.error(message + "\n");
                LOG.error(diagnostic + "\n");
                if (!byteArrayOutputStream.toString().isEmpty()) {
                    LOG.error(byteArrayOutputStream.toString() + "\n");
                }
            }
        } catch (IOException ioe) {
            LOG.error(ioe);
        } finally {
            try {
                if (fw != null)
                    fw.close();
            } 
			catch (IOException e) {}
        }
        return null;
    }

    /**
     * Known modelPath for EventGraph compilation.  Called whenever an XML file
     * loads for the first time, or is saved during an analyst editing session.
     *
     * @param xmlFile the EventGraph to package up
     * @return a package and file pair
     */
    public PackageAndFile createTemporaryEventGraphClass(File xmlFile) {
        PackageAndFile paf = null;
        try {
            SimkitXML2Java x2j = new SimkitXML2Java(xmlFile);
            x2j.unmarshal();

            boolean isEventGraph = x2j.getUnMarshalledObject() instanceof viskit.xsd.bindings.eventgraph.SimEntity;
            if (!isEventGraph) {
                LOG.debug("Is an Assembly: " + !isEventGraph);
                return null;
            }
            String sourceCode = buildJavaEventGraphSource(x2j);

            /* Warn that we may have forgotten a parameter required for a super class */
            if (sourceCode == null) {
                String message = xmlFile + " did not compile.\n" +
                        "Please check that you have provided initialization parameters in the \n" +
                        "identical order declared by any super classes";
                LOG.error(xmlFile + " " + message);
                LOG.error(sourceCode);
                messageToUser(JOptionPane.ERROR_MESSAGE, "Source code compilation error", message);
                return null;
            }

            // If using plain Vanilla Viskit, don't compile diskit extended EGs
            // as diskit.jar won't be available
            String[] classPath = ((LocalBootLoader) ViskitGlobals.instance().getWorkClassLoader()).getClassPath();
            boolean foundDiskit = false;
            for (String path : classPath) {
                if (path.contains("diskit.jar")) {
                    foundDiskit = !foundDiskit;
                    break;
                }
            }

            if (sourceCode.contains("diskit") && !foundDiskit) {
                FileBasedClassManager.instance().addCacheMiss(xmlFile);

                // TODO: Need to announce/recommend to the user to place
                // diskit.jar in the classpath, then restart Viskit
            } else {
                paf = compileJavaClassAndSetPackage(sourceCode);
            }
        } catch (FileNotFoundException e) {
            LOG.error("Error creating Java class file from " + xmlFile + ": " + e.getMessage() + "\n");
            FileBasedClassManager.instance().addCacheMiss(xmlFile);
        }
        return paf;
    }

    /** Path for EventGraph and Assembly compilation
     *
     * @param sourceCode the raw source to write to file
     * @return a package and file pair
     */
    private PackageAndFile compileJavaClassAndSetPackage(String sourceCode) {
        String packageName = new String();
        if (sourceCode != null && !sourceCode.isEmpty()) {
            Pattern p = Pattern.compile("package.*;");
            Matcher m = p.matcher(sourceCode);
            if (m.find()) {
                String nuts = m.group();
                if (nuts.endsWith(";")) {
                    nuts = nuts.substring(0, nuts.length() - 1);
                }

                String[] sa = nuts.split("\\s");
                packageName = sa[1];
            }
            File f = compileJavaClassFromString(sourceCode);
            if (f != null)
			{
                return new PackageAndFile(packageName, f);
            }
        }
        return null;
    }

    // From menu
    @Override
    public void export2grid() {
        AssemblyModel model = (AssemblyModel) getModel();
        File tFile;
        try {
            tFile = TempFileManager.createTempFile("ViskitAssy", ".xml");
        } catch (IOException e) {
            messageToUser(JOptionPane.ERROR_MESSAGE, "File System Error", e.getMessage());
            return;
        }
        model.saveModel(tFile);
    //todo switch to DOE
    }
    private String[] executionParameters;

    // Known modelPath for Assembly compilation
    @Override
    public void initializeAssemblyRun()
	{
        String sourceCode = produceJavaAssemblyClass(); // asks to save

        PackageAndFile packageAndFile = compileJavaClassAndSetPackage(sourceCode);
        if (packageAndFile != null) {
            File f = packageAndFile.file;
            String className = f.getName().substring(0, f.getName().indexOf('.'));
            className = packageAndFile.pkg + "." + className;
			
            String classPath = ""; // no longer necessary since we don't invoke Runtime.exec to compile anymore

            executionParameters = buildExecutionParameterArray(className, classPath);
        } else {
            executionParameters = null;
        }
    }
	
	@Override
	public boolean isAssemblyReady ()
	{
		return (executionParameters != null); // if parameters are ready, then Assembly passed all tests
	}

    public final static String PREPARESIMULATIONRUN_METHOD = "compileAssemblyAndPrepareSimulationRunner"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void compileAssemblyAndPrepareSimulationRunner () // method name must exactly match preceding string value
	{

        // Prevent multiple pushes of the initialize Simulation Run button
        mutex++;
        if (mutex > 1) {
            return;
        }

        // Prevent double clicking which will cause potential ClassLoader issues
        Runnable r = new Runnable() {

            @Override
            public void run() {
                ((AssemblyEditViewFrame) getView()).compileInitializeAssemblyButton.setEnabled(false);
            }
        };
        if (SwingUtilities.isEventDispatchThread())
            SwingUtilities.invokeLater(r);
        else {
            try {
                SwingUtilities.invokeAndWait(r);
            } catch (InvocationTargetException | InterruptedException e) {
                LOG.error(e);
            }
        }

        SwingWorker worker = new SwingWorker<Void, Void>()
		{
            @Override
            public Void doInBackground () // TODO why Void arther than void?  SwingWorker convention of some sort
			{
                initializeAssemblyRun(); // generate, compile sourceCode and prepare the executionParameters

                if (executionParameters == null) 
				{
                    if (ViskitGlobals.instance().getActiveAssemblyModel() == null) {
                        messageToUser(JOptionPane.WARNING_MESSAGE,
                            "No Assembly file is active",
                            "Please open an Assembly file before running a simulation");
                    } else {
                        String message = "<html><p>Runtime error: no executionParameters found.</p><br />" + 
								         "<p>Please locate and correct the source of the error in assembly XML for proper compilation.</p>";
                        messageToUser(JOptionPane.WARNING_MESSAGE, "Failed Assembly source generation/compilation", message);
                    }
                } 
				else 
				{
                    // Ensure a cleared Simulation Run panel upon every Assembly compile
                    SimulationRunPanel simulationRunPanel = ViskitGlobals.instance().getSimulationRunPanel();
					simulationRunPanel.setTitle( ((AssemblyModel) getModel()).getMetadata().name );
					simulationRunPanel.initializeRunWidgetsSimulationOutputTA ();

                    save(); // Ensure changes to the Assembly Properties dialog get saved
					
                    runner.exec(executionParameters); // Initializes a fresh class loader
                    
                    executionParameters = null; // reset
                }
                return null;
            }

            @Override
            protected void done() {
                try
				{
                   get(); // Wait for the compile, save and Assembly preparations to finish
                } 
				catch (InterruptedException | ExecutionException e) 
				{
                    LOG.error(e);
//                    e.printStackTrace();
                } 
				finally 
				{
                    ((AssemblyEditViewFrame) getView()).compileInitializeAssemblyButton.setEnabled(true);
                    mutex--; // clear lock
					ViskitGlobals.instance().getViskitApplicationFrame().displaySimulationRunTab(); // send analyst to Simulation Run tab
                }
            }
        };
        worker.execute(); // do it to it
    }

    // No longer invoking a Runtime.Exec for compilation
//    public static final int EXEC_JAVACMD = 0;
//    public static final int EXEC_VMARG0 = 1;
//    public static final int EXEC_VMARG1 = 2;
//    public static final int EXEC_VMARG3 = 3;
//    public static final int EXEC_DASH_CP = 4;
//    public static final int EXEC_CLASSPATH = 5;
    public static final int EXEC_TARGET_CLASS_NAME = 6;
    public static final int EXEC_VERBOSE_SWITCH = 7;
    public static final int EXEC_STOPTIME_SWITCH = 8;
//    public static final int EXEC_FIRST_ENTITY_NAME = 9;

    /** Prepare for the compilation of the loaded assembly file from java source.
     * Maintain the above static enumerations to match the order below.
     * @param className the name of the Assembly file to compile
     * @param classPath the current ClassLoader context
     * @return operation-system specific array of execution parameters as String array
     */
    private String[] buildExecutionParameterArray(String className, String classPath)
	{
        Vector<String> v = new Vector<>();
        String fileSeparator = ViskitStatics.getFileSeparator();

        StringBuilder invocation = new StringBuilder();
        invocation.append(System.getProperty("java.home"));
        invocation.append(fileSeparator);
        invocation.append("bin");
        invocation.append(fileSeparator);
        invocation.append("java");
        v.add(invocation.toString());// 0
		// execution parameters, TODO consider including these properties as user preferences
        v.add("-Xss2m");                                                // 1
        v.add("-Xincgc");                                               // 2
        v.add("-Xmx512m");                                              // 3
        v.add("-cp");                                                   // 4
        v.add(classPath);                                               // 5
        v.add(className);                                               // 6

        v.add("" + ((AssemblyModel) getModel()).getMetadata().verbose); // 7
        v.add(((AssemblyModel) getModel()).getMetadata().stopTime);     // 8

        Vector<String> detailedOutputEntityNames = ((AssemblyModel) getModel()).getDetailedOutputEntityNames();
        for (String entityName : detailedOutputEntityNames) {
            v.add(entityName);                                          // 9+
        }
        String[] returnStringArray = new String[v.size()];
        return v.toArray(returnStringArray);
    }
    private String imageSaveCount = "";
    private int imageSaveInt = -1;

    public final static String IMAGECAPTURE_METHOD = "windowImageCapture"; // must match following method name.  not possible to accomplish this programmatically.
    @Override
    public void windowImageCapture () // method name must exactly match preceding string value
	{
        AssemblyModel vmod = (AssemblyModel) getModel();
        String fileName = "AssemblyDiagram"; // default, replaced by filename
        if (vmod.getLastFile() != null) {
            fileName = vmod.getLastFile().getName();
			if (fileName.endsWith(".xml"))
				fileName = fileName.substring (0, fileName.indexOf(".xml"));
        }
        File imageFile = ((AssemblyView) getView()).saveFileAsk(fileName + imageSaveCount + ".png", true, "Save Assembly Diagram Image");
        if (imageFile == null) {
            return;
        }
        final Timer timer = new Timer(100, new timerCallback(imageFile, true));
        timer.setRepeats(false);
        timer.start();

        imageSaveCount = "" + (++imageSaveInt);
    }

    /** Provides an automatic capture of the currently loaded Assembly and stores
     * it to a specified location for inclusion in the generated Analyst Report
     *
     * @param assemblyImage an image file to write the .png
     */
    public void captureAssemblyImage(File assemblyImage) {

        // Don't display an extra frame while taking snapshots
        final Timer timer = new Timer(100, new timerCallback(assemblyImage, false));
        timer.setRepeats(false);
        timer.start();
    }

    public boolean isCloseAll() {
        return closeAll;
    }

    public void setCloseAll(boolean closeAll) {
        this.closeAll = closeAll;
    }

    class timerCallback implements ActionListener {

        File fil;
        boolean display;

        /**
         * Constructor for this timerCallBack
         * @param f the file to write an image to
         * @param b if true, display the image
         */
        timerCallback(File f, boolean b) {
            fil = f;
            display = b;
        }

        @Override
        public void actionPerformed(ActionEvent ev)
		{
            // create and save the image
            AssemblyEditViewFrame assemblyEditViewFrame = (AssemblyEditViewFrame) getView();

            // Get only the jgraph part
            Component component = assemblyEditViewFrame.getCurrentJgraphComponent();
            if (component == null) {return;}
            if (component instanceof JScrollPane) {
                component = ((JScrollPane) component).getViewport().getView();
            }
            Rectangle rectangle = component.getBounds();
            Image image = new BufferedImage(rectangle.width, rectangle.height, BufferedImage.TYPE_3BYTE_BGR);

            // Tell the jgraph component to draw into memory
            component.paint(image.getGraphics());

            try {
                ImageIO.write((RenderedImage)image, "png", fil); // write to file
            } catch (IOException e) {
                LOG.error(e);
            }

            // display a scaled version
            if (display) {
                final JFrame frame = new JFrame("Saved as " + fil.getName());
                Icon imageIcon = new ImageIcon(image);
                JLabel lab = new JLabel(imageIcon);
                frame.getContentPane().setLayout(new BorderLayout());
                frame.getContentPane().add(lab, BorderLayout.CENTER);
                frame.pack();
                frame.setLocationRelativeTo((Component) getView());

                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        frame.setVisible(true);
                    }
                };
                SwingUtilities.invokeLater(r);
            }
        }
    }

    /** Override the default AssemblyRunnerPlug
     *
     * @param plug the AssemblyRunnerPlug to set
     */
    public void setAssemblyRunner(AssemblyRunnerPlug plug) {
        runner = plug;
    }

    /** Opens each Event Graph associated with this Assembly
     * @param f the Assembly File to open EventGraphs for (not used)
     */
    private void openEventGraphs(File f) {
        File tempFile = null;
        try {
            List<File> eventGraphFilesList = EventGraphCache.instance().getEventGraphFilesList();
            for (File file : eventGraphFilesList)
			{
                tempFile = file;

                // _doOpen checks if a tab is already opened
                ((EventGraphControllerImpl) ViskitGlobals.instance().getEventGraphController())._doOpen(file);
            }
			ViskitGlobals.instance().getViskitApplicationFrame().displayAnalystReportTab(); // ensure correct context when building menus
			ViskitGlobals.instance().getEventGraphViewFrame().buildMenus(); // reset
        } catch (Exception ex) {
            LOG.error("Opening EventGraph file: " + tempFile + " caused error: " + ex);
            messageToUser(JOptionPane.WARNING_MESSAGE,
                    "EventGraph Opening Error",
                    "EventGraph file: " + tempFile + "\nencountered error: " + ex + " while loading."
                    );
			ex.printStackTrace();
            closeAll();
        }
    }

    /** Recent open file support */
    private static final int RECENTLISTSIZE = 15;
    private final Set<File> recentAssemblyFileSet = new LinkedHashSet<>(RECENTLISTSIZE + 1);
    private final Set<File> recentProjectFileSet  = new LinkedHashSet<>(RECENTLISTSIZE + 1);

    /**
     * If passed file is in the list, move it to the top.  Else insert it;
     * Trim to RECENTLISTSIZE
     * @param file an assembly file to add to the list
     */
    private void adjustRecentAssemblyFileSet(File file)
	{
        for (Iterator<File> iterator = recentAssemblyFileSet.iterator(); iterator.hasNext();)
		{
            File f = iterator.next();
            if (file.getPath().equals(f.getPath())) {
                iterator.remove();
                break;
            }
        }
        recentAssemblyFileSet.add(file); // to the top
        notifyRecentAssemblyFileListeners();
        saveAssemblyHistoryXML(recentAssemblyFileSet);
    }

    /**
     * If passed file is in the list, move it to the top.  Else insert it;
     * Trim to RECENTLISTSIZE
     * @param file a project file to add to the list
     */
    public void adjustRecentProjectFileSet(File file)
	{
		if (file == null)
			return;
		
        for (Iterator<File> iterator = recentProjectFileSet.iterator(); 
				            iterator.hasNext();)
		{
            File f = iterator.next();
            if (file.getPath().equals(f.getPath())) 
			{
                iterator.remove();
                break;
            }
        }
        recentProjectFileSet.add(file); // to the top
        notifyRecentProjectFileListeners(); // TODO infinite loop?
        saveProjectHistoryXML(recentProjectFileSet);
    }

    private List<File> openAssembliesFileList = new ArrayList<>(4);

    @SuppressWarnings("unchecked")
    public void updateAssemblyFileLists ()
	{
        if (historyXMLConfiguration == null) 
		{
			initializeHistoryXMLConfiguration();
		}
        List<String> assemblyFilePathList = historyXMLConfiguration.getList(ViskitConfiguration.ASSEMBLY_HISTORY_KEY + "[@value]");
//        LOG.debug("recordAssemblyFiles() assemblyFilePathList.size()=" + assemblyFilePathList.size());
        int index = 0;
        for (String assemblyFilePath : assemblyFilePathList)
		{
            if (recentAssemblyFileSet.add(new File(assemblyFilePath))) // returns true if file added
			{
                String openValue = historyXMLConfiguration.getString(ViskitConfiguration.ASSEMBLY_HISTORY_KEY + "(" + index + ")[@open]");

                if (openValue != null && (openValue.toLowerCase().equals("true") || openValue.toLowerCase().equals("yes"))) {
                    openAssembliesFileList.add(new File(assemblyFilePath));
                }
                notifyRecentAssemblyFileListeners();
            }
            index++;
        }
        saveAssemblyHistoryXML(recentAssemblyFileSet);
    }

    @SuppressWarnings("unchecked")
    public void updateProjectFileLists()
	{
        if (historyXMLConfiguration == null) 
		{
			initializeHistoryXMLConfiguration();
		}
        List<String> projectHistoryList = historyXMLConfiguration.getList(ViskitConfiguration.PROJECT_HISTORY_KEY + "[@value]");
        LOG.debug("recordProjectFiles projectHistoryList.size()=" + projectHistoryList.size());
        for (String project : projectHistoryList)
		{
            adjustRecentProjectFileSet(new File(project));
        }
		// TODO save?
    }

    private void saveAssemblyHistoryXML(Set<File> recentFiles)
	{
        historyXMLConfiguration.clearTree(ViskitConfiguration.RECENT_ASSEMBLY_CLEAR_KEY);
        int index = 0;

        // The value's modelPath is already delimited with "/"
        for (File value : recentFiles) {
            historyXMLConfiguration.setProperty(ViskitConfiguration.ASSEMBLY_HISTORY_KEY + "(" + index + ")[@value]", value.getPath());
            index++;
        }
        historyXMLConfiguration.getDocument().normalize();
    }

    /** Always keep our project historyXMLConfiguration until a user clears it manually
     *
     * @param recentFiles a Set of recently opened projects
     */
    private void saveProjectHistoryXML(Set<File> recentFiles) {
        int ix = 0;
        for (File value : recentFiles) {
            historyXMLConfiguration.setProperty(ViskitConfiguration.PROJECT_HISTORY_KEY + "(" + ix + ")[@value]", value.getPath());
            ix++;
        }
        historyXMLConfiguration.getDocument().normalize();
    }

    @Override
    public void clearRecentAssemblyFileList()
	{
        recentAssemblyFileSet.clear();
        notifyRecentAssemblyFileListeners();
        saveAssemblyHistoryXML(recentAssemblyFileSet);
		ViskitGlobals.instance().getAssemblyEditViewFrame().buildMenus(); // reset
    }

    @Override
    public Set<File> getRecentAssemblyFileSet()
	{
        return getRecentAssemblyFileSet(true); // typically must refresh to see changes
    }

    private Set<File> getRecentAssemblyFileSet(boolean refresh)
	{
        if (refresh || recentAssemblyFileSet == null) {
            updateAssemblyFileLists();
        }
        return recentAssemblyFileSet;
    }

    @Override
    public void clearRecentProjectFileSet()
	{
        recentProjectFileSet.clear();
        saveProjectHistoryXML(recentProjectFileSet);
        notifyRecentProjectFileListeners();
		ViskitGlobals.instance().getEventGraphViewFrame().buildMenus(); // reset
    }

    @Override
    public Set<File> getRecentProjectFileSet()
	{
        return getRecentProjectFileSet(false); // don't refresh or infinite loop occurs
    }

    private Set<File> getRecentProjectFileSet(boolean refresh)
	{
        if (refresh || recentProjectFileSet == null)
		{
            updateProjectFileLists();
        }
        return recentProjectFileSet;
    }

    XMLConfiguration historyXMLConfiguration;

    private void initializeHistoryXMLConfiguration() {
        try {
            historyXMLConfiguration = ViskitConfiguration.instance().getViskitApplicationXMLConfiguration();
        } catch (Exception e) {
            LOG.error("Error loading recent history file: " + e.getMessage());
            LOG.warn ("Error, recent history file disabled");
            historyXMLConfiguration = null;
        }
    }
}