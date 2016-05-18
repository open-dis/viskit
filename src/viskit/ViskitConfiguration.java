package viskit;

import edu.nps.util.LogUtilities;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
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
public class ViskitConfiguration
{
    static final Logger LOG = LogUtilities.getLogger(ViskitConfiguration.class);
	
    public static final String VISKIT_SHORT_APPLICATION_NAME = "Visual Simkit";
    public static final String VISKIT_FULL_APPLICATION_NAME  = "Visual Simkit (Viskit) Analyst Tool for Discrete Event Simulation (DES)";
    public static final String VISKIT_WEBSITE_URL            = "https://eos.nps.edu/Viskit"; // TODO
    public static final String VISKIT_MAILING_LIST           = "viskit@www.movesinstitute.org";
	
    public static final File USER_CONFIGURATION_DIRECTORY= new File(System.getProperty("user.home"), ".viskit");
    public static final File USER_CONFIGURATION_FILE     = new File(USER_CONFIGURATION_DIRECTORY, "viskitConfiguration.xml");
    public static final File USER_CONFIGURATION_FILE_OLD = new File(USER_CONFIGURATION_DIRECTORY, "vconfig.xml");
    public static final File USER_README_FILE            = new File(USER_CONFIGURATION_DIRECTORY, "README.txt");
    public static final File USER_DEBUG_LOG              = new File(USER_CONFIGURATION_DIRECTORY, "debug.log");
    public static final File USER_C_APP_FILE             = new File(USER_CONFIGURATION_DIRECTORY, "c_app.xml");
    public static final File USER_C_GUI_FILE             = new File(USER_CONFIGURATION_DIRECTORY, "c_gui.xml");
    public static final File USER_C_GUI_MAC_FILE         = new File(USER_CONFIGURATION_DIRECTORY, "c_gui_mac.xml");
    public static       File VISKIT_DEBUG_LOG            = new File(USER_CONFIGURATION_DIRECTORY, "debug.log");
	
	// these directory instantiations don't work somehow, reinitialized in constructor
    public static       File VISKIT_CONFIGURATION_DIRECTORY = new File("configuration");
    public static       File VISKIT_CONFIGURATION_FILE      = new File("configuration", "viskitConfiguration.xml");
    public static       File VISKIT_README_FILE             = new File("configuration", "README.txt");
    public static       File VISKIT_C_APP_FILE              = new File("configuration", "c_app.xml");
    public static       File VISKIT_C_GUI_FILE              = new File("configuration", "c_gui.xml");

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

    private static ViskitConfiguration singletonViskitConfiguration;

    private Map<String, XMLConfiguration> xmlConfigurations;
    private Map<String, String>           sessionHashMap;
    private CombinedConfiguration         combinedConfiguration;
    private DefaultConfigurationBuilder   defaultConfigurationBuilder;
    private XMLConfiguration              projectXMLConfiguration = null;

    public static synchronized ViskitConfiguration instance() 
	{
        if (singletonViskitConfiguration == null) // singleton design pattern
		{
            singletonViskitConfiguration = new ViskitConfiguration();
        }
        return singletonViskitConfiguration;
    }

	/** 
	 * Constructor and initialization
	 */
    private ViskitConfiguration()
	{
        try {
            if (!USER_CONFIGURATION_DIRECTORY.exists())
			{
                USER_CONFIGURATION_DIRECTORY.mkdirs();
				LOG.info("Welcome to the " + VISKIT_FULL_APPLICATION_NAME);
				LOG.info(VISKIT_WEBSITE_URL);
                LOG.info("Created USER_CONFIGURATION_DIRECTORY: " + USER_CONFIGURATION_DIRECTORY + " (exists=" + USER_CONFIGURATION_DIRECTORY.exists() + ")");
            }
			else
			{
				LOG.info("Welcome to the " + VISKIT_FULL_APPLICATION_NAME);
				LOG.info(VISKIT_WEBSITE_URL);
				LOG.info("Checked USER_CONFIGURATION_DIRECTORY: " + USER_CONFIGURATION_DIRECTORY + " (exists=" + USER_CONFIGURATION_DIRECTORY.exists() + ")");
			}
			
			// clear out corrupted files (if found) since they can be problematic
			if (USER_CONFIGURATION_FILE.exists() && USER_CONFIGURATION_FILE.length() == 0L)
				USER_CONFIGURATION_FILE.delete();
			if (USER_CONFIGURATION_FILE_OLD.exists() && USER_CONFIGURATION_FILE_OLD.length() == 0L) // otherwise left alone
				USER_CONFIGURATION_FILE_OLD.delete();
			if (USER_C_APP_FILE.exists() && USER_C_APP_FILE.length() == 0L)
				USER_C_APP_FILE.delete();
			if (USER_C_GUI_FILE.exists() && USER_C_GUI_FILE.length() == 0L)
				USER_C_GUI_FILE.delete();
			if (VISKIT_DEBUG_LOG.exists() && VISKIT_DEBUG_LOG.length() == 0L)
				VISKIT_DEBUG_LOG.delete();
			
			// keep original approach for comparison purposes
            File c_app_SourceFile = new File("configuration" + File.separator + USER_C_APP_FILE.getName());
            File c_gui_SourceFile;
            if  (ViskitStatics.OPERATING_SYSTEM.toLowerCase().contains("os x"))
                 c_gui_SourceFile = new File("configuration" + File.separator + USER_C_GUI_MAC_FILE.getName());
			else c_gui_SourceFile = new File("configuration" + File.separator + USER_C_GUI_FILE.getName());

            if (!USER_C_GUI_FILE.exists() && c_gui_SourceFile.exists())
			{
                Files.copy(c_gui_SourceFile.toPath(), USER_C_GUI_FILE.toPath());
            }
            if (!USER_C_APP_FILE.exists() && c_app_SourceFile.exists())
			{
                Files.copy(c_app_SourceFile.toPath(), USER_C_APP_FILE.toPath());
            }
			
			// create user configuration files, if needed
            if (USER_CONFIGURATION_FILE_OLD.exists() && !USER_CONFIGURATION_FILE.exists())
			{
				LOG.info ("copying original-style " + USER_CONFIGURATION_FILE_OLD + " to " + USER_CONFIGURATION_FILE.toPath());
				Files.copy(USER_CONFIGURATION_FILE_OLD.toPath(), USER_CONFIGURATION_FILE.toPath());
			}
            if (!USER_CONFIGURATION_FILE.exists())
			{
				if (VISKIT_CONFIGURATION_FILE.exists()) // TODO fix directory
					Files.copy(VISKIT_CONFIGURATION_FILE.toPath(), USER_CONFIGURATION_FILE.toPath());
				else
					LOG.error ("Configuration file not found:" + VISKIT_CONFIGURATION_FILE.toPath());
            }
            if (!USER_README_FILE.exists())
			{
				if (VISKIT_README_FILE.exists())
					Files.copy(VISKIT_README_FILE.toPath(),        USER_README_FILE.toPath());
				else
					LOG.error ("Configuration file not found:" + VISKIT_README_FILE.toPath());
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
    private void setDefaultConfiguration()
	{
        try {
            defaultConfigurationBuilder = new DefaultConfigurationBuilder();
            defaultConfigurationBuilder.setFile(USER_CONFIGURATION_FILE);
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
                if (!(configuration instanceof XMLConfiguration)) // safety check
				{
                    continue; // looping
                }
                XMLConfiguration xmlConfiguration = (XMLConfiguration) configuration;
                xmlConfiguration.setAutoSave(true);
                HierarchicalConfiguration.Node n = xmlConfiguration.getRoot();
                for (Object childObject : n.getChildren()) 
				{
                    xmlConfigurations.put(((HierarchicalConfiguration.Node) childObject).getName(), xmlConfiguration);
                }
            }
        } 
		catch (Exception e) 
		{
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
    public XMLConfiguration getViskitApplicationXMLConfiguration()
	{
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
        singletonViskitConfiguration = null;
    }

    public void cleanup() {
        // Lot of hoops to pretty-fy config xml files
        Document document;
        Format format = Format.getPrettyFormat();
        XMLOutputter xout = new XMLOutputter(format);
        try {

            // For c_app.xml
            document = FileHandler.unmarshallJdom(USER_C_APP_FILE);
            xout.output(document,  new FileWriter(USER_C_APP_FILE));

            // For c_gui.xml
            document = FileHandler.unmarshallJdom(USER_C_GUI_FILE);
            xout.output(document,  new FileWriter(USER_C_GUI_FILE));

            // For vconfig.xml
            document = FileHandler.unmarshallJdom(USER_CONFIGURATION_FILE);
            xout.output(document,  new FileWriter(USER_CONFIGURATION_FILE));

            // For the current Viskit project file
            document = FileHandler.unmarshallJdom(ViskitGlobals.instance().getCurrentViskitProject().getProjectFile());
            xout.output(document,  new FileWriter(ViskitGlobals.instance().getCurrentViskitProject().getProjectFile()));
        } catch (Exception e) {
            LOG.error("Bad jdom cleanup() operation: " + e.getMessage());
        }
    }
}
