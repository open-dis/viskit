package viskit;

import edu.nps.util.Log4jUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration2.*;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.configuration2.tree.NodeCombiner;
import org.apache.commons.configuration2.tree.NodeModel;
import org.apache.commons.configuration2.tree.UnionCombiner;
import org.apache.logging.log4j.Logger;

import org.jdom.Document;
import org.jdom.JDOMException;

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
public class ViskitConfigurationStore 
{
    
    /** Singleton pattern, with final version applied by NetBeans
     * @see https://stackoverflow.com/questions/2832297/java-singleton-pattern
     * @see https://stackoverflow.com/questions/70689/what-is-an-efficient-way-to-implement-a-singleton-pattern-in-java
     */
    // lazy loading, not immediate loading which can fail
    private static volatile ViskitConfigurationStore INSTANCE = null; 
    
    /** singleton pattern
     * @return singleton instance
     */
    public static ViskitConfigurationStore instance()
    {
        ViskitConfigurationStore INSTANCE = ViskitConfigurationStore.INSTANCE;
        if (INSTANCE == null) { // Check 1
            synchronized (ViskitConfigurationStore.class) {
                INSTANCE = ViskitConfigurationStore.INSTANCE;
                if (INSTANCE == null) { // Check 2
                    ViskitConfigurationStore.INSTANCE = INSTANCE = new ViskitConfigurationStore();
                    // ViskitStatics.incrementViskitConfigurationStoreCreationCount(); // synchronized singleton safety check
                }
            }
        }
//        if (INSTANCE == null) { // Check 1
//            synchronized (ViskitConfigurationStore.class) {
//                if (INSTANCE == null) { // Check 2
//                    INSTANCE = new ViskitConfigurationStore();
//                }
//            }
//        }
        return INSTANCE;
    }
    
    public static final String VISKIT_SHORT_APPLICATION_NAME = "Viskit";
    public static final String VISKIT_FULL_APPLICATION_NAME  = "Visual Simkit (Viskit) Modeling Tool for Discrete Event Simulation (DES) Analysis";
    
    public static final String VISKIT_WELCOME_MESSAGE = "Welcome to " + VISKIT_SHORT_APPLICATION_NAME;

    public static final File VISKIT_CONFIGURATION_DIR = new File(System.getProperty("user.home"), ".viskit");
    public static final File C_APP_FILE = new File(VISKIT_CONFIGURATION_DIR, "c_app.xml");
    public static final File C_GUI_FILE = new File(VISKIT_CONFIGURATION_DIR, "c_gui.xml");
    public static final File VISKIT_LOGS_DIR = new File("logs");
    public static final File VISKIT_ERROR_LOG = new File(VISKIT_LOGS_DIR, "error.log.0");

    public static final String GUI_BEANSHELL_ERROR_DIALOG_KEY = "gui.beanshellerrordialog";
    public static final String BEANSHELL_ERROR_DIALOG_TITLE_KEY = GUI_BEANSHELL_ERROR_DIALOG_KEY + ".title";
    public static final String BEANSHELL_ERROR_DIALOG_LABEL_KEY = GUI_BEANSHELL_ERROR_DIALOG_KEY + ".label";
    public static final String BEANSHELL_ERROR_DIALOG_QUESTION_KEY = GUI_BEANSHELL_ERROR_DIALOG_KEY + ".question";
    public static final String BEANSHELL_ERROR_DIALOG_SESSIONCHECKBOX_KEY = GUI_BEANSHELL_ERROR_DIALOG_KEY + ".sessioncheckbox";
    public static final String BEANSHELL_ERROR_DIALOG_PREFERENCESCHECKBOX_KEY = GUI_BEANSHELL_ERROR_DIALOG_KEY + ".preferencescheckbox";
    public static final String BEANSHELL_ERROR_DIALOG_PREFERENCESTOOLTIP_KEY = GUI_BEANSHELL_ERROR_DIALOG_KEY + ".preferencestooltip";
    public static final String BEANSHELL_WARNING_KEY = "app.beanshell.warning";
    public static final String PROJECT_HOME_CLEAR_KEY = "app.projecthome";
    public static final String PROJECT_PATH_KEY = PROJECT_HOME_CLEAR_KEY + ".path[@dir]";
    public static final String PROJECT_NAME_KEY = PROJECT_HOME_CLEAR_KEY + ".name[@value]";
    public static final String X_CLASSPATHS_CLEAR_KEY = "extraClassPaths";
    public static final String X_CLASSPATHS_PATH_KEY = X_CLASSPATHS_CLEAR_KEY + ".path";
    public static final String X_CLASSPATHS_KEY = X_CLASSPATHS_PATH_KEY + "[@value]";
    public static final String RECENT_EVENTGRAPH_CLEAR_KEY = "history.EventGraphEditor.Recent";
    public static final String RECENT_ASSEMBLY_CLEAR_KEY = "history.AssemblyEditor.Recent";
    public static final String RECENT_PROJECT_CLEAR_KEY = "history.ProjectEditor.Recent";
    public static final String EVENTGRAPH_HISTORY_KEY = RECENT_EVENTGRAPH_CLEAR_KEY + ".EventGraphFile";
    public static final String ASSEMBLY_HISTORY_KEY = RECENT_ASSEMBLY_CLEAR_KEY + ".AssemblyFile";
    public static final String PROJECT_HISTORY_KEY = RECENT_PROJECT_CLEAR_KEY + ".Project";
    public static final String EVENTGRAPH_EDITOR_VISIBLE_KEY = "app.tabs.EventGraphEditor[@visible]";
    public static final String ASSEMBLY_EDITOR_VISIBLE_KEY = "app.tabs.AssemblyEditor[@visible]";
    public static final String ASSEMBLY_SIMULATION_RUN_VISIBLE_KEY = "app.tabs.AssemblyRun[@visible]";
    public static final String ANALYST_REPORT_VISIBLE_KEY = "app.tabs.AnalystReport[@visible]";
    public static final String DESIGNOFEXPERIMENTS_DOE_EDITOR_VISIBLE_KEY = "app.tabs.DesignOfExperiments[@visible]";
    public static final String CLOUD_SIMULATION_RUN_VISIBLE_KEY = "app.tabs.ClusterRun[@visible]";
    public static final String VERBOSE_DEBUG_MESSAGES_KEY = "app.debug";

    /** A cached path to satisfactorily compiled, or not, XML EventGraphs and their respective .class versions */
    public static final String CACHED_CLEAR_KEY = "Cached";
    public static final String CACHED_DIGEST_KEY = CACHED_CLEAR_KEY + ".EventGraphs[@digest]";
    public static final String CACHED_EVENTGRAPHS_KEY = CACHED_CLEAR_KEY + ".EventGraphs[@xml]";
    public static final String CACHED_EVENTGRAPHS_CLASS_KEY = CACHED_CLEAR_KEY + ".EventGraphs[@class]";
    public static final String CACHED_MISS_FILE_KEY = CACHED_CLEAR_KEY + ".Miss[@file]";
    public static final String CACHED_MISS_DIGEST_KEY = CACHED_CLEAR_KEY + ".Miss[@digest]";

    public static final String APP_MAIN_BOUNDS_KEY = "app.mainframe.size";
    public static final String LOOK_AND_FEEL_KEY = "gui.lookandfeel";
    public static final String PROJECT_TITLE_NAME_KEY = "gui.projecttitle.name[@value]";
    public static final String LOOK_AND_FEEL_DEFAULT = "default";
    public static final String LOOK_AND_FEEL_PLATFORM = "platform";
    
    public static final String VISKIT_PROJECT_NAME = "Project[@name]";

    static final Logger LOG = Log4jUtilities.getLogger(ViskitConfigurationStore.class);

    private final Map<String, XMLConfiguration> xmlConfigurationsMap;
    private final Map<String, String>           sessionHashMap;
    private CombinedConfiguration               projectCombinedConfiguration;
    private XMLConfiguration                    projectXMLConfiguration = null;

    /** Private constructor cannot be invoked externally */
    private ViskitConfigurationStore() 
    {
        try {
            LOG.info(VISKIT_WELCOME_MESSAGE);
            LOG.debug("VISKIT_CONFIGURATION_DIR: " + VISKIT_CONFIGURATION_DIR + " " + VISKIT_CONFIGURATION_DIR.exists() + "\n");
        
            if (!VISKIT_CONFIGURATION_DIR.exists())
            {
                 VISKIT_CONFIGURATION_DIR.mkdirs();
                 LOG.info("Created dir: {}", VISKIT_CONFIGURATION_DIR);
            }

            File cAppSrc = new File("configuration/" + C_APP_FILE.getName());
            if (!C_APP_FILE.exists())
                Files.copy(cAppSrc.toPath(), C_APP_FILE.toPath());

            File cGuiSrc = new File("configuration/" + C_GUI_FILE.getName());
            if (!C_GUI_FILE.exists())
                Files.copy(cGuiSrc.toPath(), C_GUI_FILE.toPath());

        } 
        catch (IOException ex) {
            LOG.error(ex);
        }
        xmlConfigurationsMap = new HashMap<>();
        sessionHashMap = new HashMap<>();
        setDefaultConfiguration();
    }

    /** Builds, or rebuilds a default configuration */
    private void setDefaultConfiguration() {
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
        } catch (ConfigurationException e) {
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
     * Sets key=value pair in the persistent ViskitConfigurationStore.
     * Rather screwy - a decent design would allow the CombinedConfiguration object
 to do the saving, but it won't.
     *
     * @param key the ViskitConfigurationStore named key to set
     * @param val the value of this key
     */
    public void setVal(String key, String val) {
        String cfgKey = key.substring(0, key.indexOf('.'));
        XMLConfiguration xc = xmlConfigurationsMap.get(cfgKey);
        xc.setProperty(key, val);
    }

    public void setSessionVal(String key, String val) {
        sessionHashMap.put(key, val);
    }

    public String getVal(String key) {
        String retS = sessionHashMap.get(key);
        if (retS != null && retS.length() > 0) {
            return retS;
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
    public XMLConfiguration getProjectXMLConfig() {
        return projectXMLConfiguration;
    }

    /** Remove a project's XML configuration upon closing a Viskit project
     * @param projConfig the project configuration to remove
     */
    public void removeProjectXMLConfig(XMLConfiguration projConfig) {
        projectCombinedConfiguration.removeConfiguration(projConfig);
        xmlConfigurationsMap.remove("proj");
    }

    /** @return the XMLConfiguration for Viskit app */
    public XMLConfiguration getViskitAppConfiguration() {
        return (XMLConfiguration) projectCombinedConfiguration.getConfiguration("app");
    }

    /** @return the XMLConfiguration for Viskit gui */
    public XMLConfiguration getViskitGuiConfig() {
        return (XMLConfiguration) projectCombinedConfiguration.getConfiguration("gui");
    }

    /** Used to clear all Viskit Configuration information to create a new
     * Viskit Project
     */
    public void clearViskitConfig() {
        setVal(ViskitConfigurationStore.PROJECT_PATH_KEY, "");
        setVal(ViskitConfigurationStore.PROJECT_NAME_KEY, "");
        getViskitAppConfiguration().clearTree(ViskitConfigurationStore.RECENT_EVENTGRAPH_CLEAR_KEY);
        getViskitAppConfiguration().clearTree(ViskitConfigurationStore.RECENT_ASSEMBLY_CLEAR_KEY);

        // Retain the recent projects list
    }

//    public void resetViskitConfig()
//    {
//        clearViskitConfig(); // not sure what to do here, what is goal of this method?
////        me = null;
//    }

    public void cleanup() {
        // Lot of hoops to pretty-fy config xml files
        Document doc;
        try {

            // For c_app.xml
            doc = FileHandler.unmarshallJdom(C_APP_FILE);
            FileHandler.marshallJdom(C_APP_FILE, doc, false);

            // For c_gui.xml
            doc = FileHandler.unmarshallJdom(C_GUI_FILE);
            FileHandler.marshallJdom(C_GUI_FILE, doc, false);

            // For the current viskitProject.xml file
            doc = FileHandler.unmarshallJdom(ViskitGlobals.instance().getViskitProject().getProjectFile());
            FileHandler.marshallJdom(ViskitGlobals.instance().getViskitProject().getProjectFile(), doc, false);
        } catch (IOException | JDOMException e) {
            LOG.error("Bad JDOM cleanup {}: ", e);
        }
    }
}
