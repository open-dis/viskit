package viskit.util;

import viskit.*;
import java.io.File;
import java.util.Arrays;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.log4j.Logger;

/** Using the java compiler now part of javax, we no longer have to 
 * either ship tools.jar or require a jdk environment variable.
 * This class was taken from viskit.SourceWindow.
 * TBD refactor out from SourceWindow, the main difference being Compiler actaully 
 * creates .class Files, where SourceWindow did in memory.
 *
 * @author Rick Goldberg
 */
public class Compiler {

    static Logger log = Logger.getLogger(Compiler.class);
    
    /** Call the java compiler to test compile our event graph java source
     * 
     * @param pkg 
     * @param className
     * @param src
     * @return Diagnostic messages from compiler
     */
    public static String invoke(String pkg, String className, String src) {

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StringBuffer diagnosticMessages = new StringBuffer();
        StringBuffer sb = new StringBuffer();
        CompilerDiagnosticsListener diag = new CompilerDiagnosticsListener(diagnosticMessages);
        JavaObjectFromString jofs = null;
        StringBuffer classPaths = null;
        String cp = null;

        try {
            jofs = new JavaObjectFromString(pkg + "." + className, src);
            File workDir = VGlobals.instance().getWorkDirectory();
            StandardJavaFileManager sjfm = compiler.getStandardFileManager(diag, null, null);
            Iterable<? extends JavaFileObject> fileObjects = Arrays.asList(jofs);
            String workDirPath = workDir.getCanonicalPath();
            
            // TODO: Lessen the amount of resetting the LBL
            String[] workClassPath = ((viskit.doe.LocalBootLoader) (VGlobals.instance().getResetWorkClassLoader(false))).getClassPath();
            classPaths = new StringBuffer();
            for (String cPath : workClassPath) {
                classPaths.append(cPath + File.pathSeparator);                
            }
            cp = classPaths.toString();
            log.debug("cp is: " + cp);
            String[] options = {"-Xlint:unchecked",
                    "-Xlint:deprecation",
                    "-cp", 
                    cp,
                    "-d", 
                    workDirPath};
            java.util.List<String> optionsList = Arrays.asList(options);
            compiler.getTask(null, sjfm, diag, optionsList, null, fileObjects).call();
            sb.append(diag.messageString);
        } catch (Exception ex) {
            VGlobals.log.error("JavaObjectFromString " + pkg + "." + className + "  " + jofs.toString());
            log.error("Classpath is: " + cp);
            ex.printStackTrace();
        }
        return sb.toString();
    }
}


