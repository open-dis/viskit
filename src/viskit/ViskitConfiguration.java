package viskit;

import edu.nps.util.LogUtilities;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.configuration.*;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import viskit.doe.FileHandler;

/**
 * <p>Visual Simkit (Viskit) Discrete Event Simulation (DES) Tool
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu</p>
 *
 * @author Mike Bailey
 * @since Mar 8, 2005
 * @since 11:09:07 AM
 * @version $Id$
 */
public class ViskitConfiguration {

    public static final String VISKIT_SHORT_APPLICATION_NAME = "Visual Simkit";
    public static final String VISKIT_FULL_APPLICATION_NAME  = "Visual Simkit (Viskit) Analyst Tool for Discrete Event Simulation (DES)";
	
    public static final File VISKIT_CONFIGURATION_DIR      = new File(System.getProperty("user.home"), ".viskit");
    public static final File VISKIT_CONFIGURATION_FILE_OLD = new File(VISKIT_CONFIGURATION_DIR, "vconfig.xml");
    public static final File VISKIT_CONFIGURATION_FILE     = new File(VISKIT_CONFIGURATION_DIR, "viskitConfiguration.xml");
    public static final File VISKIT_README_FILE            = new File(VISKIT_CONFIGURATION_DIR, "README.txt");
    public static final File C_APP_FILE                    = new File(VISKIT_CONFIGURATION_DIR, "c_app.xml");
    public static final File C_GUI_FILE                    = new File(VISKIT_CONFIGURATION_DIR, "c_gui.xml");
    public static final File V_DEBUG_LOG                   = new File(VISKIT_CONFIGURATION_DIR, "debug.log");

    public static final String GUI_BEANSHELL_ERROR_DIALOG = "gui.beanshellerrordialog";
    public static final String BEANSHELL_ERROR_DIALOG_TITLE               = GUI_BEANSHELL_ERROR_DIALOG + ".title";
    public static final String BEANSHELL_ERROR_DIALOG_LABEL               = GUI_BEANSHELL_ERROR_DIALOG + ".label";
    public static final String BEANSHELL_ERROR_DIALOG_QUESTION            = GUI_BEANSHELL_ERROR_DIALOG + ".question";
    public static final String BEANSHELL_ERROR_DIALOG_SESSIONCHECKBOX     = GUI_BEANSHELL_ERROR_DIALOG + ".sessioncheckbox";
    public static final String BEANSHELL_ERROR_DIALOG_PREFERENCESCHECKBOX = GUI_BEANSHELL_ERROR_DIALOG + ".preferencescheckbox";
    public static final String BEANSHELL_ERROR_DIALOG_PREFERENCESTOOLTIP  = GUI_BEANSHELL_ERROR_DIALOG + ".preferencestooltip";
    public static final String BEANSHELL_WARNING      = "app.beanshell.warning";
    public static final String PROJECT_HOME_CLEAR_KEY = "app.projecthome";
    public static final String PROJECT_PATH_KEY        = PROJECT_HOME_CLEAR_KEY + ".path[@dir]";
    public static final String PROJECT_NAME_KEY        = PROJECT_HOME_CLEAR_KEY + ".name[@value]";
    public static final String PROJECT_OPEN_KEY        = PROJECT_HOME_CLEAR_KEY + ".open[@value]";
    public static final String PROJECT_AUTHOR_KEY      = PROJECT_HOME_CLEAR_KEY + ".author[@value]";
    public static final String PROJECT_REVISION_KEY    = PROJECT_HOME_CLEAR_KEY + ".revision[@value]";
    public static final String PROJECT_DESCRIPTION_KEY = PROJECT_HOME_CLEAR_KEY + ".description[@value]";
    public static final String PROJECT_PROPERTIES_EDIT_COMPLETED_KEY = PROJECT_HOME_CLEAR_KEY + ".propertiesEditCancelled[@value]";
    public static final String EXTRA_CLASSPATHS_CLEAR_KEY = "extraClassPaths";
    public static final String EXTRA_CLASSPATHS_PATH_KEY = EXTRA_CLASSPATHS_CLEAR_KEY + ".path";
    public static final String EXTRA_CLASSPATHS_KEY      = EXTRA_CLASSPATHS_PATH_KEY  + "[@value]";
    public static final String RECENT_EVENT_GRAPH_CLEAR_KEY = "history.EventGraphEditor.Recent";
    public static final String RECENT_ASSEMBLY_CLEAR_KEY    = "history.AssemblyEditor.Recent";
    public static final String RECENT_PROJECT_CLEAR_KEY     = "history.ProjectEditor.Recent";
    public static final String EVENTGRAPH_HISTORY_KEY = RECENT_EVENT_GRAPH_CLEAR_KEY + ".EventGraphFile";
    public static final String ASSEMBLY_HISTORY_KEY   = RECENT_ASSEMBLY_CLEAR_KEY    + ".AssemblyFile";
    public static final String PROJECT_HISTORY_KEY    = RECENT_PROJECT_CLEAR_KEY     + ".Project";
    public static final String EVENTGRAPH_EDIT_VISIBLE_KEY = "app.tabs.EventGraphEditor[@visible]";
    public static final String ASSEMBLY_EDIT_VISIBLE_KEY   = "app.tabs.AssemblyEditor[@visible]";
//	@Deprecated
//  public static final String ASSEMBLY_RUN_VISIBLE_KEY    = "app.tabs.AssemblyRun[@visible]";
    public static final String SIMULATION_RUN_VISIBLE_KEY  = "app.tabs.SimulationRun[@visible]";
    public static final String DOE_EDIT_VISIBLE_KEY        = "app.tabs.DesignOfExperiments[@visible]";
    public static final String CLUSTER_RUN_VISIBLE_KEY     = "app.tabs.ClusterRun[@visible]";
    public static final String ANALYST_REPORT_VISIBLE_KEY  = "app.tabs.AnalystReport[@visible]";
    public static final String DEBUG_MESSAGES_KEY = "app.debug";

    /** A cached path to satisfactorily compiled, or not, XML EventGraphs and their respective .class versions */
    public static final String CACHED_CLEAR_KEY       = "Cached";
    public static final String CACHED_DIGEST_KEY      = CACHED_CLEAR_KEY + ".EventGraphs[@digest]";
    public static final String CACHED_EVENTGRAPHS_KEY = CACHED_CLEAR_KEY + ".EventGraphs[@xml]";
    public static final String CACHED_EVENTGRAPHS_CLASS_KEY = CACHED_CLEAR_KEY + ".EventGraphs[@class]";
    public static final String CACHED_MISS_FILE_KEY   = CACHED_CLEAR_KEY + ".Miss[@file]";
    public static final String CACHED_MISS_DIGEST_KEY = CACHED_CLEAR_KEY + ".Miss[@digest]";

    public static final String APP_MAIN_BOUNDS_KEY    = "app.mainframe.size";
    public static final String LOOK_AND_FEEL_KEY      = "gui.lookandfeel";
    public static final String PROJECT_TITLE_NAME     = "gui.projecttitle.name[@value]";
    public static final String LOOK_AND_FEEL_DEFAULT  = "default";
    public static final String LOOK_AND_FEEL_PLATFORM = "platform";

    private static ViskitConfiguration me;

    static final Logger LOG = LogUtilities.getLogger(ViskitConfiguration.class);

    private Map<String, XMLConfiguration> xmlConfigurations;
    private Map<String, String>           sessionHashMap;
    private CombinedConfiguration         combinedConfiguration;
    private DefaultConfigurationBuilder   defaultConfigurationBuilder;
    private XMLConfiguration              projectXMLConfiguration = null;

    static {
        LOG.info("Welcome to the " + VISKIT_FULL_APPLICATION_NAME);
        LOG.debug("VISKIT_CONFIG_DIR: " + VISKIT_CONFIGURATION_DIR + " (exists=" + VISKIT_CONFIGURATION_DIR.exists() + ")");
    }

    public static synchronized ViskitConfiguration instance() {
        if (me == null) {
            me = new ViskitConfiguration();
        }
        return me;
    }

	/** 
	 * Constructor and initialization
	 */
    private ViskitConfiguration()
	{
        try {
            if (!VISKIT_CONFIGURATION_DIR.exists()) {
                 VISKIT_CONFIGURATION_DIR.mkdirs();
                 LOG.info("Created dir: " + VISKIT_CONFIGURATION_DIR);
            }
            File viskitConfigurationFile = new File("configuration" + File.separator + VISKIT_CONFIGURATION_FILE.getName());
			
			// clear out corrupted files, if found
			if (VISKIT_CONFIGURATION_FILE.length() == 0L)
				VISKIT_CONFIGURATION_FILE.delete();
			if (C_APP_FILE.length() == 0L)
				C_APP_FILE.delete();
			if (C_GUI_FILE.length() == 0L)
				C_GUI_FILE.delete();
			
			// create configuration files, if needed
            if (VISKIT_CONFIGURATION_FILE_OLD.exists() && !VISKIT_CONFIGURATION_FILE.exists())
			{
				LOG.info ("copying original-style " + VISKIT_CONFIGURATION_FILE_OLD + " to " + VISKIT_CONFIGURATION_FILE.toPath());
				Files.copy(VISKIT_CONFIGURATION_FILE_OLD.toPath(), VISKIT_CONFIGURATION_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
            if (!VISKIT_CONFIGURATION_FILE.exists())
			{
                Files.copy(viskitConfigurationFile.toPath(), VISKIT_CONFIGURATION_FILE.toPath());
            }
            File cAppSrc = new File("configuration/" + C_APP_FILE.getName());
            if (!VISKIT_README_FILE.exists())
			{
                Files.copy(VISKIT_README_FILE.toPath(), VISKIT_CONFIGURATION_DIR.toPath());
            }
            File cGuiSrc;
            if (ViskitStatics.OPERATING_SYSTEM.toLowerCase().contains("os x"))
                cGuiSrc = new File("configuration/c_gui_mac.xml");
            else
                cGuiSrc = new File("configuration/" + C_GUI_FILE.getName());

            if (!C_GUI_FILE.exists()) {
                Files.copy(cGuiSrc.toPath(), C_GUI_FILE.toPath());
            }
            if (!C_APP_FILE.exists())
			{
                Files.copy(cAppSrc.toPath(), C_APP_FILE.toPath());
            }
        } 
		catch (IOException ex) {
            LOG.error(ex);
        }
        xmlConfigurations = new HashMap<>();
        sessionHashMap    = new HashMap<>();
        setDefaultConfiguration();
    }

    /** Builds (or rebuilds) a default configuration */
    private void setDefaultConfiguration() {
        try {
            defaultConfigurationBuilder = new DefaultConfigurationBuilder();
            defaultConfigurationBuilder.setFile(VISKIT_CONFIGURATION_FILE);
            try {
                combinedConfiguration = defaultConfigurationBuilder.getConfiguration(true); // TODO silence unhelpful verbose output
            }
			catch (ConfigurationException e)
			{
                LOG.error(e);
            }

            // Save off the individual XML configurations for each prefix so we can write back
            int numberOfConfigurations = combinedConfiguration.getNumberOfConfigurations();
            for (int i = 0; i < numberOfConfigurations; i++)
			{
                Configuration configuration = combinedConfiguration.getConfiguration(i);
                if (!(configuration instanceof XMLConfiguration)) {
                    continue;
                }
                XMLConfiguration xmlConfiguration = (XMLConfiguration) configuration;
                xmlConfiguration.setAutoSave(true);
                HierarchicalConfiguration.Node n = xmlConfiguration.getRoot();
                for (Object childObject : n.getChildren()) {
                    xmlConfigurations.put(((HierarchicalConfiguration.Node) childObject).getName(), xmlConfiguration);
                }
            }
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    /**
     * Rather screwy.  A decent design would allow the CombinedConfiguration object
     * to do the saving, but it won't.
     *
     * @param key   the ViskitConfiguration-named key to set
     * @param value the value of this key
     */
    public void setValue(String key, String value) {
        String configurationKey = key.substring(0, key.indexOf('.'));
        XMLConfiguration xmlConfiguration = xmlConfigurations.get(configurationKey);
		if (xmlConfiguration != null)
            xmlConfiguration.setProperty(key, value);
		else
		{
            LOG.error("ViskitConfiguration error xmlConfiguration.setProperty(" + key + ", " + value + ");");
		}
    }

    public void setSessionValue(String key, String value) {
        sessionHashMap.put(key, value);
    }

    public String getValue(String key) {
        String retrievedValue = sessionHashMap.get(key);
        if (retrievedValue != null && retrievedValue.length() > 0) {
            return retrievedValue;
        }
		if (combinedConfiguration == null)
			 return ""; // safety net
		else return combinedConfiguration.getString(key);
    }

    public String[] getConfigurationValues(String key) {
        return combinedConfiguration.getStringArray(key);
    }

    /** @param projectConfigurationPath a Viskit project file */
    public void setProjectXMLConfigurationPath(String projectConfigurationPath) {
        try {
            projectXMLConfiguration = new XMLConfiguration(projectConfigurationPath);
        } catch (ConfigurationException ce) {
            LOG.error(ce);
        }
        projectXMLConfiguration.setAutoSave(true);
        combinedConfiguration.addConfiguration(projectXMLConfiguration, "project");
        xmlConfigurations.put("project", projectXMLConfiguration);
    }

    /** @return the XMLConfiguration for Viskit project */
    public XMLConfiguration getProjectXMLConfiguration() {
        return projectXMLConfiguration;
    }

    /** Remove a project's XML configuration upon closing a Viskit project
     * @param projectXMLConfiguration the project configuration to remove
     */
    public void removeProjectXMLConfiguration(XMLConfiguration projectXMLConfiguration) {
        combinedConfiguration.removeConfiguration(projectXMLConfiguration);
        xmlConfigurations.remove("project");
    }

    /** @return the XMLConfiguration for Viskit application */
    public XMLConfiguration getViskitApplicationXMLConfiguration() {
        return (XMLConfiguration) combinedConfiguration.getConfiguration("app");
    }

    /** @return the XMLConfiguration for Viskit gui */
    public XMLConfiguration getViskitGuiXMLConfiguration() {
        return (XMLConfiguration) combinedConfiguration.getConfiguration("gui");
    }

    /** Used to clear all Viskit Configuration information to create a new
     * Viskit Project
     */
    public void clearViskitConfiguration()
	{
		String projectHomeDirectory = ViskitConfiguration.instance().getValue(ViskitConfiguration.PROJECT_PATH_KEY);
		if (!projectHomeDirectory.isEmpty())
		{
			File parentFile = new File (projectHomeDirectory);
			setValue(ViskitConfiguration.PROJECT_PATH_KEY,  parentFile.getParent()); // remember something, current parent is good
		}
		else setValue(ViskitConfiguration.PROJECT_PATH_KEY, "");
		
        setValue(ViskitConfiguration.PROJECT_NAME_KEY, "");
        setValue(ViskitConfiguration.PROJECT_OPEN_KEY, "false");
        setValue(ViskitConfiguration.PROJECT_AUTHOR_KEY, "");
        setValue(ViskitConfiguration.PROJECT_REVISION_KEY, "");
        setValue(ViskitConfiguration.PROJECT_DESCRIPTION_KEY, "");
        getViskitApplicationXMLConfiguration().clearTree(ViskitConfiguration.RECENT_EVENT_GRAPH_CLEAR_KEY);
        getViskitApplicationXMLConfiguration().clearTree(ViskitConfiguration.RECENT_ASSEMBLY_CLEAR_KEY);

        // TODO: Other clears?
    }

    public void resetViskitConfiguration() {
        me = null;
    }

    public void cleanup() {
        // Lot of hoops to pretty-fy config xml files
        Document document;
        Format format = Format.getPrettyFormat();
        XMLOutputter xout = new XMLOutputter(format);
        try {

            // For c_app.xml
            document = FileHandler.unmarshallJdom(C_APP_FILE);
            xout.output(document,  new FileWriter(C_APP_FILE));

            // For c_gui.xml
            document = FileHandler.unmarshallJdom(C_GUI_FILE);
            xout.output(document,  new FileWriter(C_GUI_FILE));

            // For vconfig.xml
            document = FileHandler.unmarshallJdom(VISKIT_CONFIGURATION_FILE);
            xout.output(document,  new FileWriter(VISKIT_CONFIGURATION_FILE));

            // For the current Viskit project file
            document = FileHandler.unmarshallJdom(ViskitGlobals.instance().getCurrentViskitProject().getProjectFile());
            xout.output(document,  new FileWriter(ViskitGlobals.instance().getCurrentViskitProject().getProjectFile()));
        } catch (Exception e) {
            LOG.error("Bad jdom cleanup() operation: " + e.getMessage());
        }
    }
}
