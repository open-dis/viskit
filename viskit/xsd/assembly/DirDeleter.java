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

/**
 *
 * @author rmgoldbe
 */

public class DirDeleter extends Thread {
    private ArrayList dirList = new ArrayList();

    public synchronized void add(File dir) {
        dirList.add(dir);
    }

    public void run() {
        synchronized (this) {
            Iterator iterator = dirList.iterator();
            while (iterator.hasNext()) {
                File dir = (File)iterator.next();
                deleteDirectory(dir);
                iterator.remove();
            }
        }
    }

    private void deleteDirectory(File dir) {
        File[] fileArray = dir.listFiles();

        if (fileArray != null) {
            for (int i = 0; i < fileArray.length; i++) {
                //if (fileArray[i].isDirectory())
                  //deleteDirectory(fileArray[i]);
                //else
                   //fileArray[i].delete();
            }
        }
        //dir.delete();
    }
}

