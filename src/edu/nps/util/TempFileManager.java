package edu.nps.util;

import java.io.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *  Generates and properly cleans up temporary files. Similar to {@link
 *  File#createTempFile(java.lang.String, java.lang.String)}, this class
 *  provides a static method to create temporary files. The temporary files will
 *  be created in a special directory to be cleaned up the next time this class
 *  is loaded by the JVM. This functionality is required because Win32 platforms
 *  will not allow the JVM to delete files that are open. This causes problems
 *  with items such as JARs that get opened by a URLClassLoader and can
 *  therefore not be deleted by the JVM (including deleteOnExit).
 *
 *  The caller should not need to create an instance of this class, although it
 *  is possible. Simply use the static methods to perform the required
 *  operations. Note that all files created by this class should be
 *  considered as deleted at JVM exit (although the actual deletion may be
 *  delayed). If persistent temporary files are required, use {@link
 *  java.io.File} instead.
 *
 *  Refer to Sun bugs 4171239 and 4950148 for more details.
 *
 *  Code adapted from article titled "Clean Up Your Mess: Managing Temp Files in Java Apps"
 *  http://www.devx.com/Java/Article/22018/1954
 *  @author Michael Pilone
 *  @version $Id$
 */
public class TempFileManager {

    static final Logger LOG = LogManager.getLogger();

    /**
     *  Creates a temporary file in the proper directory to allow for cleanup
     *  after execution. This method delegates to {@link
     *  File#createTempFile(java.lang.String, java.lang.String, java.io.File)} so
     *  refer to it for more documentation. Any file created using this method
     *  should be considered as deleted at JVM exit; therefore, do not use this
     *  method to create files that need to be persistent between application
     *  runs.
     *
     * @param  prefix        the prefix string used in generating the file name;
     *      must be at least three characters long
     * @param  suffix        the suffix string to be used in generating the file's
     *      name; may be null, in which case the suffix ".tmp" will be used
     * @return               an abstract pathname denoting a newly created empty
     *      file
     * @throws  IOException  if a file could not be created
     */
    public static File createTempFile(String prefix, String suffix)
            throws IOException {
        // Check to see if you have already initialized a temp directory
        // for this class.
        if (sTmpDir == null) {
            // Initialize your temp directory. You use the java temp directory
            // property, so you are sure to find the files on the next run.
            String tmpDirName = System.getProperty("java.io.tmpdir");
            File tmpDir = File.createTempFile(TEMP_DIR_PREFIX, ".tmp",
                    new File(tmpDirName));

            // Delete the file if one was automatically created by the JVM.
            // You are going to use the name of the file as a directory name,
            // so you do not want the file laying around.
            tmpDir.delete();

            // Create a lock before creating the directory so
            // there is no race condition with another application trying
            // to clean your temp dir.
            File lockFile = new File(tmpDirName, tmpDir.getName() + ".lck");
            lockFile.createNewFile();

            // Set the lock file to delete on exit so it is properly cleaned
            // by the JVM. This will allow the TempFileManager to clean
            // the overall temp directory next time.
            lockFile.deleteOnExit();

            // Make a temp directory that you will use for all future requests.
            if (!tmpDir.mkdirs()) {
                LOG.warn("Unable to create temporary directory:" + tmpDir.getAbsolutePath());
            }

            sTmpDir = tmpDir;
        }

        // Generate a temp file for the user in your temp directory
        // and return it.
        return File.createTempFile(prefix, suffix, sTmpDir);
    }

    /** Used to create a temp directory made from the process above
     *
     * @param f the temp file created from {@link createTempFile}
     * @return a temp directory
     */
    public static File createTempDir(File f) {
        f.delete();
        f.mkdir();
        return f;
    }

    /**
     *  Utility method to load the TempFileManager at any time and allow it to
     *  clean the temporary files that may be left from previous instances
     *
     * @param  args  command line arguments are currently not supported
     */
    public static void main(String[] args) {
        // Although the JVM will load the class in order to
        // run the main method, this gives a little clarity to
        // what is happening and why we want the main method.
        try {
            // This will load the TempFileManager, which will
            // cause the static block to execute, cleaning
            // any old temp files.
            Class.forName(TempFileManager.class.getName());
        } catch (ClassNotFoundException ex) {
            LOG.error(ex);
//            ex.printStackTrace();
        }
    }

    /**
     *  Deletes all of the files in the given directory, recursing into any sub
     *  directories found. Also deletes the root directory.
     *
     * @param  rootDir       the root directory to be recursively deleted
     * @throws  IOException  if any file or directory could not be deleted
     */
    private static void recursiveDelete(File rootDir) throws IOException {
        // Select all the files
        File[] files = rootDir.listFiles();
        for (File file : files) {

            // If the file is a directory, we will
            // recursively call delete on it.
            if (file.isDirectory()) {
                recursiveDelete(file);
            } else {
                // It is just a file so we are safe to
                // delete it
                if (!file.delete()) {
                    LOG.warn("Could not delete: " + file.getAbsolutePath());
                }
            }
        }

        // Finally, delete the root directory now
        // that all of the files in the directory have
        // been properly deleted.
        if (!rootDir.delete()) {
            LOG.warn("Could not delete: " + rootDir.getAbsolutePath());
        }
    }

    /**
     *  The prefix for the temp directory in the system temp directory
     */
    private final static String TEMP_DIR_PREFIX = "tmp-mgr-";
    /**
     *  The temp directory to generate all files in
     */
    private static File sTmpDir = null;

    /**
     *  Static block used to clean up any old temp directories found -- the JVM
     *  will run this block when a class loader loads the class.
     */
    static {
        // Clean up any old temp directories by listing
        // all of the files, using a filter that will
        // return only directories that start with your
        // prefix.
        FileFilter tmpDirFilter =
            (File pathname) -> (pathname.isDirectory() &&
                    pathname.getName().startsWith(TEMP_DIR_PREFIX));

        // Get the system temp directory and filter the files.
        String tmpDirName = System.getProperty("java.io.tmpdir");
        File tmpDir = new File(tmpDirName);
        File[] tmpFiles = tmpDir.listFiles(tmpDirFilter);

        // Find all the files that do not have a lock by
        // checking if the lock file exists.
        for (File tmpFile : tmpFiles) {

            // Create a file to represent the lock and test.
            File lockFile = new File(tmpFile.getParent(), tmpFile.getName() + ".lck");
            if (!lockFile.exists()) {
                // Delete the contents of the directory since
                // it is no longer locked.
                LOG.debug("Deleting old temp directory " + tmpFile);

                try {
                    recursiveDelete(tmpFile);
                } catch (IOException ex) {
                    // Use the logger at a fine level since not being able to delete
                    // the temp directory should not stop the application
                    // from performing correctly. However, if the application
                    // generates a lot of temp files, this could become
                    // a disk space problem and the level should be raised.
                    LOG.error("Unable to delete " + tmpFile.getAbsolutePath());

                    // Print the exception.
                    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
                    ex.printStackTrace(new PrintStream(ostream));

                    LOG.error(ostream.toString());
                }
            }
        }
    }

} // end class file TempFileManager.java