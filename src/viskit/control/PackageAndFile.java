package viskit.control;

import edu.nps.util.Log4jUtilities;
import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class to hold java source package and file names
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.control.PackageAndFile">Terry Norbraten, NPS MOVES</a>
 * @version $Id:$
 */
public class PackageAndFile { // TODO consider moving this small class into FileBasedClassManager
    
    static final Logger LOG = LogManager.getLogger();

    public String packageName;
    public File file;

    public PackageAndFile(String packageName, File file) {
        this.packageName = packageName;
        this.file = file;
    }

} // end class file PackageAndFile.java
