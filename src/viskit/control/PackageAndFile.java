package viskit.control;

import java.io.File;

/**
 * Utility class to hold java source package and file names
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.control.PackageAndFile">Terry Norbraten, NPS MOVES</a>
 * @version $Id:$
 */
public class PackageAndFile { // TODO consider moving this small class into FileBasedClassManager

    public String packageName;
    public File file;

    public PackageAndFile(String packageName, File file) {
        this.packageName = packageName;
        this.file = file;
    }

} // end class file PackageAndFile.java
