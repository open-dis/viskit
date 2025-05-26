package viskit.util;

import edu.nps.util.Log4jUtilities;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;

import viskit.ViskitGlobals;
import viskit.doe.LocalBootLoader;

/** 
 * Using the java compiler, now part of javax.tools, we no longer have to
 * ship tools.jar from the JDK install.
 * This class was originally based in {@link viskit.view.SourceWindow}.
 *
 * @author Rick Goldberg
 * @version $Id$
 */
public class Compiler 
{
    static final Logger LOG = LogManager.getLogger();

    /** Diagnostic message when we have a successful compilation */
    public static final String COMPILE_SUCCESS_MESSAGE = "compile success!";

    /** Stream for writing text to an output device */
    private static OutputStream baosOut;

    /** Compiler diagnostic object */
    private static CompilerDiagnosticsListener compilerDiagnosticsListener;

    /** Call the java compiler to test compile our event graph java source
     *
     * @param packageName package containing java file
     * @param className name of the java file
     * @param sourceCodeText a string containing the full source code
     * @return diagnostic messages from the compiler
     */
    public static String invoke(String packageName, String className, String sourceCodeText) {

        StringBuilder diagnosticMessages = new StringBuilder();
        StandardJavaFileManager sjfm = null;
        StringBuilder classPaths;
        String classPathsString;

        if (packageName != null && !packageName.isEmpty()) {
            packageName += ".";
        }

        try {

            // NOTE: if the compiler is null, then likely on a Windoze system,
            // the Oracle JRE's java.exe was placed on the Path first before the
            // JDK's java.exe.  If so, correct the Path in Computer ->
            // Properties -> Advanced system settings -> Environment variables
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            compilerDiagnosticsListener = new CompilerDiagnosticsListener(diagnosticMessages);
            sjfm = compiler.getStandardFileManager(compilerDiagnosticsListener, null, null);

            JavaObjectFromString jofs = new JavaObjectFromString(packageName + className, sourceCodeText);
            Iterable<? extends JavaFileObject> fileObjects = Arrays.asList(jofs);
            File workingDirectory = ViskitGlobals.instance().getProjectWorkingDirectory();
            String workingDirectoryPath = workingDirectory.getCanonicalPath();

            // This is would be the first instance of obtaining a LBL if
            // beginning fresh, so, it is reset on the first instantiation
            String[] classPathArray = ((LocalBootLoader) (ViskitGlobals.instance().getViskitApplicationClassLoader())).getClassPath();
            int classPathArrayLength = classPathArray.length;
            classPaths = new StringBuilder(classPathArrayLength);
            
            for (String nextClassPath : classPathArray) {
                classPaths.append(nextClassPath);
                classPaths.append(File.pathSeparator);
            }

            // Get rid of the last ";" or ":" on the cp
            classPaths = classPaths.deleteCharAt(classPaths.lastIndexOf(File.pathSeparator));
            classPathsString = classPaths.toString();
            LOG.debug("{} classPath is: {}", className,classPathsString);

            String[] options = {
                "-Xlint:unchecked",
                "-Xlint:deprecation",
                "-proc:none",
                "-cp",
                 classPathsString,
                "-d",
                workingDirectoryPath
            };
            java.util.List<String> optionsList = Arrays.asList(options);

            if (baosOut == null)
                baosOut = new ByteArrayOutputStream();

            compiler.getTask(new BufferedWriter(new OutputStreamWriter(baosOut)),
                    sjfm,
                    compilerDiagnosticsListener,
                    optionsList,
                    null,
                    fileObjects).call();

            // Check for errors
            if (diagnosticMessages.toString().isEmpty())
                diagnosticMessages.append(COMPILE_SUCCESS_MESSAGE);
        } 
        catch (Exception ex)
        {
            if (ex instanceof NullPointerException) {

                String message = "Your environment variable for Path might have the JRE's "
                                + "java.exe in front of the JDK's java.exe.\n"
                                + "Please reset your Path to have the JDK's "
                                + "java.exe as first entry in the Path";

                // Inform the user about the JRE vs. JDK java.exe Path issue
                ViskitGlobals.instance().getMainFrame().genericReport(
                        JOptionPane.INFORMATION_MESSAGE,
                        "Incorrect Path", 
                        message
                );
                LOG.error(message);
            }
            LOG.error("JavaObjectFromString invoke " + packageName + "." + className + " exception: " + ex.getMessage());
//            LOG.info("Classpath is {}: ", cp);
        } 
        finally {
            if (sjfm != null) {
                try {
                    sjfm.close();
                } catch (IOException ex) {
                    LOG.error(ex);
                }
            }
        }
        return diagnosticMessages.toString();
    }

    /**
     * @param baosOut the ByteArrayOutputStream to set
     */
    public static void setOutPutStream(OutputStream baosOut) {
        Compiler.baosOut = baosOut;
    }

    /**
     * @return the compiler diagnostic tool
     */
    public static CompilerDiagnosticsListener getDiagnostic() {
        return compilerDiagnosticsListener;
    }

} // end class file Compiler.java