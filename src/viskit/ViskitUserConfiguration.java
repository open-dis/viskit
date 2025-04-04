package viskit;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.configuration2.tree.NodeCombiner;
import org.apache.commons.configuration2.tree.NodeModel;
import org.apache.commons.configuration2.tree.UnionCombiner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import static viskit.ViskitStatics.isFileReady;
import viskit.doe.FileHandler;

/**
 * Persistent key-value store for Viskit configuration values.
 * <p>Viskit Discrete Event Simulation (DES) Tool
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu</p>
 *
 * @author Mike Bailey
 * @since Mar 8, 2005
 * @since 11:09:07 AM
 * @version $Id$
 */
public class ViskitUserConfiguration 
{
    static final Logger LOG = LogManager.getLogger();
    
    // lazy loading, not immediate loading which can fail
    private static volatile ViskitUserConfiguration INSTANCE = null;
    
    /** singleton pattern
     * @return singleton instance
     */
    public static ViskitUserConfiguration instance()
    {
        ViskitUserConfiguration INSTANCE = ViskitUserConfiguration.INSTANCE;
        if (INSTANCE == null) { // Check 1
            synchronized (ViskitUserConfiguration.class) {
                INSTANCE = ViskitUserConfiguration.INSTANCE;
                if (INSTANCE == null) { // Check 2
                    ViskitUserConfiguration.INSTANCE = INSTANCE = new ViskitUserConfiguration();
                }
            }
        }
        if (INSTANCE == null)
            LOG.warn ("initial instance creation failed! check logs afterwards for synchronized singleton safety check");
        return INSTANCE;
    }
    
    public static final File VISKIT_CONFIGURATION_DIR = new File(System.getProperty("user.home"), ".viskit");
    public static final File DOT_VISKIT_README        = new File(VISKIT_CONFIGURATION_DIR, "README.md");
    public static final File C_APP_FILE               = new File(VISKIT_CONFIGURATION_DIR, "c_app.xml");
    public static final File C_GUI_FILE               = new File(VISKIT_CONFIGURATION_DIR, "c_gui.xml");
    public static final File VISKIT_LOGS_DIR          = new File("logs");
    public static final File VISKIT_ERROR_LOG         = new File(VISKIT_LOGS_DIR, "error.0.log");

    public static final String GUI_BEANSHELL_ERROR_DIALOG_KEY                 = "gui.beanshellerrordialog";
    public static final String BEANSHELL_WARNING_KEY                          = "app.beanshell.warning";
    public static final String BEANSHELL_ERROR_DIALOG_TITLE_KEY               = GUI_BEANSHELL_ERROR_DIALOG_KEY + ".title";
    public static final String BEANSHELL_ERROR_DIALOG_LABEL_KEY               = GUI_BEANSHELL_ERROR_DIALOG_KEY + ".label";
    public static final String BEANSHELL_ERROR_DIALOG_QUESTION_KEY            = GUI_BEANSHELL_ERROR_DIALOG_KEY + ".question";
    public static final String BEANSHELL_ERROR_DIALOG_SESSIONCHECKBOX_KEY     = GUI_BEANSHELL_ERROR_DIALOG_KEY + ".sessioncheckbox";
    public static final String BEANSHELL_ERROR_DIALOG_PREFERENCESCHECKBOX_KEY = GUI_BEANSHELL_ERROR_DIALOG_KEY + ".preferencescheckbox";
    public static final String BEANSHELL_ERROR_DIALOG_PREFERENCESTOOLTIP_KEY  = GUI_BEANSHELL_ERROR_DIALOG_KEY + ".preferencestooltip";
    
    public static final String PROJECT_HOME_CLEAR_KEY_PREFIX = "app.projecthome";
    public static final String PROJECT_PATH_KEY              = PROJECT_HOME_CLEAR_KEY_PREFIX + ".path[@dir]";
    public static final String PROJECT_NAME_KEY              = PROJECT_HOME_CLEAR_KEY_PREFIX + ".name[@value]";
    public static final String X_CLASSPATHS_CLEAR_KEY        = "extraClassPaths";
    public static final String X_CLASSPATHS_PATH_KEY         = X_CLASSPATHS_CLEAR_KEY + ".path";
    public static final String X_CLASSPATHS_KEY              = X_CLASSPATHS_PATH_KEY + "[@value]";
    public static final String RECENT_EVENTGRAPH_CLEAR_KEY   = "history.EventGraphEditor.Recent";
    public static final String RECENT_ASSEMBLY_CLEAR_KEY     = "history.AssemblyEditor.Recent";
    public static final String RECENT_PROJECT_CLEAR_KEY      = "history.ProjectEditor.Recent";
    public static final String EVENTGRAPH_HISTORY_KEY        = RECENT_EVENTGRAPH_CLEAR_KEY + ".EventGraphFile";
    public static final String ASSEMBLY_HISTORY_KEY          = RECENT_ASSEMBLY_CLEAR_KEY + ".AssemblyFile";
    public static final String PROJECT_HISTORY_KEY           = RECENT_PROJECT_CLEAR_KEY + ".Project";
    
    public static final String EVENTGRAPH_EDITOR_VISIBLE_KEY              = "app.tabs.EventGraphEditor[@visible]";
    public static final String ASSEMBLY_EDITOR_VISIBLE_KEY                = "app.tabs.AssemblyEditor[@visible]";
    public static final String SIMULATION_RUN_VISIBLE_KEY                 = "app.tabs.AssemblyRun[@visible]";
    public static final String ANALYST_REPORT_VISIBLE_KEY                 = "app.tabs.AnalystReport[@visible]";
    public static final String DOE_EDITOR_VISIBLE_KEY                     = "app.tabs.DesignOfExperiments[@visible]"; // DESIGNOFEXPERIMENTS
    public static final String CLOUD_SIMULATION_RUN_VISIBLE_KEY           = "app.tabs.ClusterRun[@visible]";
    public static final String VERBOSE_DEBUG_MESSAGES_KEY                 = "app.debug";
    
    // https://stackoverflow.com/questions/1005073/initialization-of-an-arraylist-in-one-line
    public static final ArrayList<String> tabVisibilityUserPreferenceKeyList = new ArrayList<>(
           Arrays.asList(EVENTGRAPH_EDITOR_VISIBLE_KEY, 
                             ASSEMBLY_EDITOR_VISIBLE_KEY, 
                              SIMULATION_RUN_VISIBLE_KEY,
                              ANALYST_REPORT_VISIBLE_KEY,
                                  DOE_EDITOR_VISIBLE_KEY, // DESIGNOFEXPERIMENTS
                           CLOUD_SIMULATION_RUN_VISIBLE_KEY,
                                 VERBOSE_DEBUG_MESSAGES_KEY));

    /** A cached path to satisfactorily compiled, or not, XML EventGraphs and their respective .class versions */
    public static final String CACHED_CLEAR_KEY             = "Cached";
    public static final String CACHED_DIGEST_KEY            = CACHED_CLEAR_KEY + ".EventGraphs[@digest]";
    public static final String CACHED_EVENTGRAPHS_KEY       = CACHED_CLEAR_KEY + ".EventGraphs[@xml]";
    public static final String CACHED_EVENTGRAPHS_CLASS_KEY = CACHED_CLEAR_KEY + ".EventGraphs[@class]";
    public static final String CACHED_MISS_FILE_KEY         = CACHED_CLEAR_KEY + ".Miss[@file]";
    public static final String CACHED_MISS_DIGEST_KEY       = CACHED_CLEAR_KEY + ".Miss[@digest]";

    public static final String APP_MAIN_BOUNDS_KEY    = "app.mainframe.size";
    public static final String LOOK_AND_FEEL_KEY      = "gui.lookandfeel";
    public static final String LOOK_AND_FEEL_DEFAULT  = "default";
    public static final String LOOK_AND_FEEL_PLATFORM = "platform";
    
    public static final String USER_SYSTEM            = "SYSTEM"; // user name not yet defined by user
    public static final String USER_NAME_KEY          = "user[@name]";
    public static final String USER_EMAIL_KEY         = "user[@email]";
    public static final String USER_WEBSITE_KEY       = "user[@website]";
    
    public static final String PROJECT_TITLE_NAME_KEY = "gui.projecttitle.name[@value]";
    public static final String VISKIT_PROJECT_NAME    = "Project[@name]";

    private static final Map<String, XMLConfiguration> xmlConfigurationsMap = new HashMap<>();
    private static final Map<String, String>                 sessionHashMap = new HashMap<>();
    private static       CombinedConfiguration         projectCombinedConfiguration;
    private static       XMLConfiguration              projectXMLConfiguration = null;
    
    private static boolean initalized = false;

    /** Private constructor cannot be invoked externally */
    private ViskitUserConfiguration() 
    {
        if (!initalized)
        {     
            LOG.info("initializing ViskitUserConfiguration by reading files in .viskit directory");        
            initialize(); // this should only occur once
        }
        // LOG.info("created ViskitUserConfiguration singleton (if this message occurs again, it is a problem)");   
    }
    
    public final static void initialize()
    {
        try {
            if (!VISKIT_CONFIGURATION_DIR.exists())
            {
                 VISKIT_CONFIGURATION_DIR.mkdirs();
                 LOG.info("Created directory: \n      " + VISKIT_CONFIGURATION_DIR.getAbsolutePath());
            }
            else LOG.debug("VISKIT_CONFIGURATION_DIR: " + VISKIT_CONFIGURATION_DIR + " " + VISKIT_CONFIGURATION_DIR.exists() + "\n");
            
            File c_appXmlSourceFile = new File("configuration/" + C_APP_FILE.getName());
            if (!C_APP_FILE.exists())
                Files.copy(c_appXmlSourceFile.toPath(), C_APP_FILE.toPath());

            File c_guiXmlSourceFile = new File("configuration/" + C_GUI_FILE.getName());
            if (!C_GUI_FILE.exists())
                Files.copy(c_guiXmlSourceFile.toPath(), C_GUI_FILE.toPath());
        } 
        catch (IOException ex) {
            LOG.error(ex);
        }
        setDefaultConfiguration();
        initalized = true;
    }
    public static void logDotViskitConfigurationDirectoryStatus()
    {
        LOG.info("VISKIT_CONFIGURATION_DIR=\n      " + VISKIT_CONFIGURATION_DIR.getAbsolutePath() + " which contains user configuration files\n      " +
                 C_APP_FILE.getAbsolutePath() + " (C_APP_FILE) and " + C_GUI_FILE.getAbsolutePath() + " (C_GUI_FILE)" + "\n");
        isFileReady(C_APP_FILE);
        isFileReady(C_GUI_FILE);
    }

    /** Builds, or rebuilds a default configuration */
    private static void setDefaultConfiguration() 
    {
        try {
            Parameters params = new Parameters();
            FileBasedConfigurationBuilder<XMLConfiguration> builder1
                    = new FileBasedConfigurationBuilder<>(XMLConfiguration.class)
                        .configure(params.xml()
                        .setFileName(C_GUI_FILE.getAbsolutePath()));
            builder1.setAutoSave(true);

            FileBasedConfigurationBuilder<XMLConfiguration> builder2
                    = new FileBasedConfigurationBuilder<>(XMLConfiguration.class)
                        .configure(params.xml()
                        .setFileName(C_APP_FILE.getAbsolutePath()));
            builder2.setAutoSave(true);

            NodeCombiner combiner = new UnionCombiner();
            projectCombinedConfiguration = new CombinedConfiguration(combiner);
            projectCombinedConfiguration.addConfiguration(builder1.getConfiguration(), "gui");
            projectCombinedConfiguration.addConfiguration(builder2.getConfiguration(), "app");
        } 
        catch (ConfigurationException e) {
            LOG.error(e);
        }
        
        // Save off the indiv XML config for each prefix so we can write back
        Object obj;
        XMLConfiguration xc;
        NodeModel m;
        for (String name : projectCombinedConfiguration.getConfigurationNames()) {
            obj = projectCombinedConfiguration.getConfiguration(name);
            if (obj instanceof XMLConfiguration) {
                xc = (XMLConfiguration) obj;
                m = xc.getNodeModel();
                for (ImmutableNode o : m.getInMemoryRepresentation().getChildren()) {
                    xmlConfigurationsMap.put((o.getNodeName()), xc);
                }
            }
        }
    }

    /**
     * Sets key=value pair in the persistent ViskitUserConfiguration.
     *
     * @param key the ViskitUserConfiguration named key to set
     * @param newValue the new value of this key
     */
    public void setValue(String key, String newValue) 
    {
        String configurationKey = key.substring(0, key.indexOf('.'));
        XMLConfiguration xmlConfiguration  = xmlConfigurationsMap.get(configurationKey);
        xmlConfiguration .setProperty(key, newValue);
    }

    public void setSessionValue(String key, String newValue) {
        sessionHashMap.put(key, newValue);
    }

    public String getValue(String key) {
        String returnString = sessionHashMap.get(key);
        if (returnString != null && returnString.length() > 0) {
            return returnString;
        }
        return projectCombinedConfiguration.getString(key);
    }

    public String[] getConfigurationValues(String key) {
        return projectCombinedConfiguration.getStringArray(key);
    }

    /**
     * @param projectXmlConfigurationFilePath a Viskit project file, viskitProject.xml
     */
    public void setProjectXMLConfiguration(String projectXmlConfigurationFilePath) {
        try {
            Parameters params = new Parameters();
            FileBasedConfigurationBuilder<XMLConfiguration> fileBasedConfigurationBuilder
                    = new FileBasedConfigurationBuilder<>(XMLConfiguration.class)
                        .configure(params.xml()
                        .setFileName(projectXmlConfigurationFilePath));
            fileBasedConfigurationBuilder.setAutoSave(true);
            projectXMLConfiguration = fileBasedConfigurationBuilder.getConfiguration();
        } catch (ConfigurationException ex) {
            // TODO seems to fail when creating new project?
            LOG.error(ex);
        }
        if ((projectCombinedConfiguration.getConfiguration("proj") == null) || projectCombinedConfiguration.getConfiguration("proj").isEmpty())
            projectCombinedConfiguration.addConfiguration(projectXMLConfiguration, "proj");
        xmlConfigurationsMap.put("proj", projectXMLConfiguration);
    }

    /** @return the XMLConfiguration for Viskit project */
    public XMLConfiguration getProjectXMLConfiguration() {
        return projectXMLConfiguration;
    }

    /** Remove a project's XML configuration upon closing a Viskit project
     * @param projConfig the project configuration to remove
     */
    public void removeProjectXMLConfiguration(XMLConfiguration projConfig) {
        projectCombinedConfiguration.removeConfiguration(projConfig);
        xmlConfigurationsMap.remove("proj");
    }

    /** @return the c_app.cml XMLConfiguration for Viskit app */
    public XMLConfiguration getViskitAppConfiguration() {
        return (XMLConfiguration) projectCombinedConfiguration.getConfiguration("app");
    }

    /** @return the c_gui.xml XMLConfiguration for Viskit gui */
    public XMLConfiguration getViskitGuiConfiguration() {
        return (XMLConfiguration) projectCombinedConfiguration.getConfiguration("gui");
    }

    /** @return project path for Viskit project directory from user's dot_viskit c_app xml configuration file */
    public String getViskitProjectDirectoryPath() {
        return getValue(ViskitUserConfiguration.PROJECT_PATH_KEY);
    }

    /** @return project path for Viskit project directory from user's dot_viskit c_app xml configuration file */
    public File getViskitProjectDirectory() {
        return new File (getViskitProjectDirectoryPath(), getViskitProjectName());
    }

    /** @return project name from user's dot_viskit c_app xml configuration file */
    public String getViskitProjectName() {
        return getValue(ViskitUserConfiguration.PROJECT_NAME_KEY);
    }

    /** Used to clear all Viskit Configuration information to create a new
     * Viskit Project
     */
    public void clearViskitConfiguration() {
        setValue(ViskitUserConfiguration.PROJECT_PATH_KEY, "");
        setValue(ViskitUserConfiguration.PROJECT_NAME_KEY, "");
        getViskitAppConfiguration().clearTree(ViskitUserConfiguration.RECENT_EVENTGRAPH_CLEAR_KEY);
        getViskitAppConfiguration().clearTree(ViskitUserConfiguration.RECENT_ASSEMBLY_CLEAR_KEY);

        // Retain the recent projects list
    }

//    public void resetViskitConfigurationv()
//    {
//        clearViskitConfiguration(); // not sure what to do here, what is goal of this method?
////        me = null;
//    }

    public void cleanup() {
        // Lot of hoops to pretty-fy config xml files
        Document document;
        try
        {
            // For c_app.xml
            document = FileHandler.unmarshallJdom(C_APP_FILE);
            FileHandler.marshallJdom(C_APP_FILE, document, false);

            // For c_gui.xml
            document = FileHandler.unmarshallJdom(C_GUI_FILE);
            FileHandler.marshallJdom(C_GUI_FILE, document, false);

            // For the current viskitProject.xml file
            File projectFile = ViskitGlobals.instance().getViskitProject().getProjectFile();
            if      (projectFile == null)
                    LOG.error("cleanup() null projectFile ");
            else if (!projectFile.exists())
                     LOG.error("cleanup() failed for nonexistent projectFile\n      {}", projectFile.getAbsolutePath());
            else
            {
                document = FileHandler.unmarshallJdom(projectFile);
                FileHandler.marshallJdom(projectFile, document, false);
            }
        } 
        catch (IOException | JDOMException e) {
            LOG.error("Problem with JDOM cleanup {}: ", e);
        }
    }
}
