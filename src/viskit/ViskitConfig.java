package viskit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import edu.nps.util.FileIO;
import java.util.Map;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import viskit.doe.FileHandler;

/**
 * Viskit Discrete Event Simulation (DES) Tool
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2005
 * @since 11:09:07 AM
 * @version $Id$
 */
public class ViskitConfig {

    public static final Logger logger = Logger.getLogger(ViskitConfig.class);

    public static final File VISKIT_HOME_DIR = new File(System.getProperty("user.home"), ".viskit");
    public static final File V_CONFIG_FILE = new File(VISKIT_HOME_DIR, "vconfig.xml");
    public static final File C_APP_FILE = new File(VISKIT_HOME_DIR, "c_app.xml");
    public static final File C_GUI_FILE = new File(VISKIT_HOME_DIR, "c_gui.xml");

    public static final String PROJECT_HOME_KEY = "app.projecthome.path[@dir]";
    public static final String PROJECT_NAME_KEY = "app.projecthome.name[@value]";
    public static final String EG_HISTORY_KEY = "history.EventGraphEditor.Recent.EventGraphFile";
    public static final String ASSY_HISTORY_KEY = "history.AssemblyEditor.Recent.AssemblyFile";
    public static final String X_CLASS_PATH_KEY = "extraClassPath.path";
    public static final String X_CLASS_PATH_CLEAR_KEY = "extraClassPath";
    public static final String RECENT_EG_CLEAR_KEY = "history.EventGraphEditor.Recent";
    public static final String RECENT_ASSY_CLEAR_KEY = "history.AssemblyEditor.Recent";
    public static final String EG_VISIBLE_KEY = "app.tabs.EventGraphEditor[@visible]";
    public static final String ASSY_EDIT_VISIBLE_KEY = "app.tabs.AssemblyEditor[@visible]";
    public static final String ASSY_RUN_VISIBLE_KEY = "app.tabs.AssemblyRun[@visible]";
    public static final String ANALYST_RPT_VISIBLE_KEY = "app.tabs.AnalystReport[@visible]";
    public static final String DEBUG_MSGS_KEY = "app.debug";
    public static final String CACHED_EVENTGRAPHS_KEY = "Cached.EventGraphs[@xml]";
    public static final String CACHED_EVENTGRAPHS_CLASS_KEY = "Cached.EventGraphs[@class]";
    public static final String CACHED_WORKING_DIR_KEY = "Cached[@workDir]";
    public static final String CACHED_MISS_FILE_KEY = "Cached.Miss[@file]";
    public static final String CACHED_MISS_DIGEST_KEY = "Cached.Miss[@digest]";
    public static final String CACHED_CLEAR_KEY = "Cached";
    public static final String EG_EDITOR_FRAME_BOUNDS_KEY = "app.EventGraphEditor.FrameBounds";
    public static final String ASSY_EDITOR_FRAME_BOUNDS_KEY = "app.AssemblyEditor.FrameBounds";
    public static final String LOOK_AND_FEEL_KEY = "gui.lookandfeel";    
    public static final String LAF_DEFAULT = "default";
    public static final String LAF_PLATFORM = "platform";
    
    private static ViskitConfig me;
    
    private Map<String, XMLConfiguration> xmlConfigurations;
    private Map<String, String> sessionHM;
    private CombinedConfiguration cc;
    private DefaultConfigurationBuilder builder;
    private XMLConfiguration projectXMLConifg = null;

    static {
        logger.info("Welcome to the Viskit Discrete Event Simulation (DES) suite");
        logger.info("VISKIT_HOME_DIR: " + VISKIT_HOME_DIR + " " + VISKIT_HOME_DIR.exists() + "\n");
    }

    public static synchronized ViskitConfig instance() {
        if (me == null) {
            me = new ViskitConfig();
        }
        return me;
    }

    private ViskitConfig() {
        try {
            if (!VISKIT_HOME_DIR.exists()) {
                VISKIT_HOME_DIR.mkdirs();
                logger.info("Created dir: " + VISKIT_HOME_DIR);
            }
            File vconfigSrc = new File(V_CONFIG_FILE.getName());
            if (!V_CONFIG_FILE.exists()) {
                V_CONFIG_FILE.createNewFile();
                FileIO.copyFile(vconfigSrc, V_CONFIG_FILE, true);
            }
            File cAppSrc = new File(C_APP_FILE.getName());
            if (!C_APP_FILE.exists()) {
                C_APP_FILE.createNewFile();
                FileIO.copyFile(cAppSrc, C_APP_FILE, true);
            }
            File cGuiSrc = new File(C_GUI_FILE.getName());
            if (!C_GUI_FILE.exists()) {
                C_GUI_FILE.createNewFile();
                FileIO.copyFile(cGuiSrc, C_GUI_FILE, true);
            }
        } catch (IOException ex) {
            logger.error(ex);
        }
        setXmlConfigurations(new HashMap<String, XMLConfiguration>());
        sessionHM = new HashMap<String, String>();
        setDefaultConfig();
    }

    /** Builds, or rebuilds a default configuration */
    private void setDefaultConfig() {
        try {
            builder = new DefaultConfigurationBuilder();
            builder.setFile(V_CONFIG_FILE);
            cc = builder.getConfiguration(true);

            // Save off the indiv XML config for each prefix so we can write back
            for (int i = 0; i < cc.getNumberOfConfigurations(); i++) {
                Object obj = cc.getConfiguration(i);
                if (!(obj instanceof XMLConfiguration)) {
                    continue;
                }
                XMLConfiguration xc = (XMLConfiguration) obj;
                xc.setAutoSave(true);
                HierarchicalConfiguration.Node n = xc.getRoot();
                for (Object o : n.getChildren()) {
                    getXmlConfigurations().put(((HierarchicalConfiguration.Node) o).getName(), xc);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Rather screwy.  A decent design would allow the CompositeConfiguration obj
     * to do the saving, but it won't.
     *
     * @param key
     * @param val
     */
    public void setVal(String key, String val) {
        String cfgKey = key.substring(0, key.indexOf('.'));
        XMLConfiguration xc = getXmlConfigurations().get(cfgKey);
        xc.setProperty(key, val);
    }

    public void setSessionVal(String key, String val) {
        sessionHM.put(key, val);
    }

    public String getVal(String key) {
        String retS = sessionHM.get(key);
        if (retS != null && retS.length() > 0) {
            return retS;
        }

        return cc.getString(key);
    }

    public int getConfigValueCount(String key) {
        String[] sa = cc.getStringArray(key);
        return sa.length;
    }

    /** @param f a Viskit project file */
    public void setProjectXMLConfig(String f) {
        try {
            projectXMLConifg = new XMLConfiguration(f);
        } catch (ConfigurationException ce) {
            Vstatics.log.error(ce);
        }
        projectXMLConifg.setAutoSave(true);
        cc.addConfiguration(projectXMLConifg);
    }

    /** @return a specific project's XMLConfiguration */
    public XMLConfiguration getProjectXMLConfig() {
        return projectXMLConifg;
    }

    /** Remove a project's XML configuration upon closing a Viskit project
     * @param projConfig the project configuration to remove
     */
    public void removeProjectXMLConfig(XMLConfiguration projConfig) {
        cc.removeConfiguration(projConfig);
    }

    /** @return the XMLConfiguration for Viskit */
    public XMLConfiguration getViskitConfig() {
        return (XMLConfiguration) cc.getConfiguration("app");
    }

    /** Used to clear all Viskit Configuration information to create a new
     * Viskit Project
     */
    public void clearViskitConfig() {
        setVal(ViskitConfig.PROJECT_HOME_KEY, "");
        setVal(ViskitConfig.PROJECT_NAME_KEY, "");
        getViskitConfig().clearTree(ViskitConfig.RECENT_EG_CLEAR_KEY);
        getViskitConfig().clearTree(ViskitConfig.RECENT_ASSY_CLEAR_KEY);
    }
    
    public void resetViskitConfig() {
        me = null;
    }

    public void cleanup() {
        // Lot of hoops to pretty-fy config xml files
        Document doc;
        Format form = Format.getPrettyFormat();
        XMLOutputter xout = new XMLOutputter(form);
        try {

            // For c_app.xml
            doc = FileHandler.unmarshallJdom(C_APP_FILE);
            xout.output(doc, new FileWriter(C_APP_FILE));

            // For c_gui.xml
            doc = FileHandler.unmarshallJdom(C_GUI_FILE);
            xout.output(doc, new FileWriter(C_GUI_FILE));

            // For vconfig.xml
            doc = FileHandler.unmarshallJdom(V_CONFIG_FILE);
            xout.output(doc, new FileWriter(V_CONFIG_FILE));

            // For the current Viskit project file
            doc = FileHandler.unmarshallJdom(VGlobals.instance().getCurrentViskitProject().getProjectFile());
            xout.output(doc, new FileWriter(VGlobals.instance().getCurrentViskitProject().getProjectFile()));
        } catch (Exception e) {
            Vstatics.log.error("Bad jdom op: " + e.getMessage());
        }
    }

    /**
     * @return the xmlConfigurations
     */
    public Map<String, XMLConfiguration> getXmlConfigurations() {
        return xmlConfigurations;
    }

    /**
     * @param xmlConfigurations the xmlConfigurations to set
     */
    public void setXmlConfigurations(HashMap<String, XMLConfiguration> xmlConfigurations) {
        this.xmlConfigurations = xmlConfigurations;
    }
}
