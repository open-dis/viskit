package viskit.util;

import edu.nps.util.LogUtils;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.log4j.Logger;
import viskit.VGlobals;

/** Using the java compiler now part of javax, we no longer have to
 * either ship tools.jar or require a jdk environment variable.
 * This class was based upon {@link viskit.view.SourceWindow}.
 * TBD refactor out from SourceWindow, the main difference being Compiler actually
 * creates .class files, where SourceWindow compiled to memory.
 *
 * @author Rick Goldberg
 * @version $Id$
 */
public class Compiler {

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
     * @return Diagnostic messages from compiler
     */
    public static String invoke(String pkg, String className, String src) {

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StringBuilder diagnosticMessages = new StringBuilder();

        diag = new CompilerDiagnosticsListener(diagnosticMessages);
        StringBuilder classPaths;
        String cp;

        if (pkg != null && !pkg.isEmpty()) {
            pkg += ".";
        }

        try {
            JavaObjectFromString jofs = new JavaObjectFromString(pkg + className, src);
            File workDir = VGlobals.instance().getWorkDirectory();
            StandardJavaFileManager sjfm = compiler.getStandardFileManager(diag, null, null);
            Iterable<? extends JavaFileObject> fileObjects = Arrays.asList(jofs);
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
                diagnosticMessages.append("No Compiler Errors");
            }
        } catch (Exception ex) {
            log.error(ex);
//            log.error("JavaObjectFromString " + pkg + "." + className + "  " + jofs.toString());
//            log.info("Classpath is: " + cp);
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