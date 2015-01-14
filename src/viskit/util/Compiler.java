package viskit.util;

import edu.nps.util.LogUtils;
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
import org.apache.log4j.Logger;
import viskit.VGlobals;

/** Using the java compiler, now part of javax.tools, we no longer have to
 * ship tools.jar from the JDK install.
 * This class was originally based in {@link viskit.view.SourceWindow}.
 *
 * @author Rick Goldberg
 * @version $Id$
 */
public class Compiler {

    /** Diagnostic message when we have a successful compilation */
    public static final String COMPILE_SUCCESS_MESSAGE = "compile successful!";

    static Logger log = LogUtils.getLogger(Compiler.class);

    /** Stream for writing text to an output device */
    private static OutputStream baosOut;

    /** Compiler diagnostic object */
    private static CompilerDiagnosticsListener diag;

    /** Call the java compiler to test compile our event graph java source
     *
     * @param pkg package containing java file
     * @param className name of the java file
     * @param src a string containing the full source code
     * @return diagnostic messages from the compiler
     */
    public static String invoke(String pkg, String className, String src) {

        StringBuilder diagnosticMessages = new StringBuilder();
        StandardJavaFileManager sjfm = null;
        StringBuilder classPaths;
        String cp;

        if (pkg != null && !pkg.isEmpty()) {
            pkg += ".";
        }

        try {

            // NOTE: if the compiler is null, then likely on a Windoze system,
            // the Oracle JRE's java.exe was placed on the Path first before the
            // JDK's java.exe.  If so, correct the Path in Computer ->
            // Properties -> Advanced system settings -> Environment variables
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            diag = new CompilerDiagnosticsListener(diagnosticMessages);
            sjfm = compiler.getStandardFileManager(diag, null, null);

            JavaObjectFromString jofs = new JavaObjectFromString(pkg + className, src);
            Iterable<? extends JavaFileObject> fileObjects = Arrays.asList(jofs);
            File workDir = VGlobals.instance().getWorkDirectory();
            String workDirPath = workDir.getCanonicalPath();

            // This is would be the first instance of obtaining a LBL if
            // beginning fresh, so, it is reset on the first instantiation
            String[] workClassPath = ((viskit.doe.LocalBootLoader) (VGlobals.instance().getWorkClassLoader())).getClassPath();
            int wkpLength = workClassPath.length;
            classPaths = new StringBuilder(wkpLength);

            for (String cPath : workClassPath) {
                classPaths.append(cPath);
                classPaths.append(File.pathSeparator);
            }

            // Get rid of the last ";" or ":" on the cp
            classPaths = classPaths.deleteCharAt(classPaths.lastIndexOf(File.pathSeparator));
            cp = classPaths.toString();
            log.debug("cp is: " + cp);

            String[] options = {
                "-Xlint:unchecked",
                "-Xlint:deprecation",
                "-cp",
                 cp,
                "-d",
                workDirPath
            };
            java.util.List<String> optionsList = Arrays.asList(options);

            if (baosOut == null) {
                baosOut = new ByteArrayOutputStream();
            }

            compiler.getTask(new BufferedWriter(new OutputStreamWriter(baosOut)),
                    sjfm,
                    diag,
                    optionsList,
                    null,
                    fileObjects).call();

            // Check for errors
            if (diagnosticMessages.toString().isEmpty()) {
                diagnosticMessages.append(COMPILE_SUCCESS_MESSAGE);
            }
        } catch (Exception ex) {
            if (ex instanceof NullPointerException) {

                String msg = "Your environment variable for Path likely has the JRE's "
                                + "java.exe in front of the JDK's java.exe.\n"
                                + "Please reset your Path to have the JDK's "
                                + "java.exe first in the Path";

                // Inform the user about the JRE vs. JDK java.exe Path issue
                VGlobals.instance().getAssemblyEditor().genericReport(JOptionPane.INFORMATION_MESSAGE,
                        "Incorrect Path", msg);

                log.error(msg);
            }
            log.error(ex);
//            log.error("JavaObjectFromString " + pkg + "." + className + "  " + jofs.toString());
//            log.info("Classpath is: " + cp);
        } finally {

            if (sjfm != null) {
                try {
                    sjfm.close();
                } catch (IOException ex) {
                    log.error(ex);
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
        return diag;
    }

} // end class file Compiler.java