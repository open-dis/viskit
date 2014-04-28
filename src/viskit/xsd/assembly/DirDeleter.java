/*
 * DirDeleter.java
 *
 * Created on August 3, 2006, 12:14 PM
 *
 * See attributions in TempDir.java
 */

package viskit.xsd.assembly;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import java.util.List;

/**
 * @deprecated use {@link edu.nps.util.TempFileManager} instead
 * @author rmgoldbe
 */
@Deprecated
public class DirDeleter extends Thread {
    private List<File> dirList = new ArrayList<File>();

    public synchronized void add(File dir) {
        dirList.add(dir);
    }

    @Override
    public void run() {
        synchronized (this) {
            Iterator<File> iterator = dirList.iterator();
            while (iterator.hasNext()) {
                File dir = iterator.next();
                deleteDirectory(dir);
                iterator.remove();
            }
        }
    }

    private void deleteDirectory(File dir) {
        File[] fileArray = dir.listFiles();

        if (fileArray != null) {
            for (File file : fileArray) {
                if (file.isDirectory())
                   deleteDirectory(file);
                else
                   file.delete();
            }
        }
        dir.delete();
    }
}

