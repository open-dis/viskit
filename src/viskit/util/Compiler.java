/*
 * Using the java compiler now part of javax, we no longer have to 
 * either ship tools.jar or require a jdk environment variable.
 * This class was taken from viskit.SourceWindow.
 * TBD refactor out from SourceWindow, the main difference being Compiler actaully 
 * creates .class Files, where SourceWindow did in memory.
 */

package viskit.util;

import viskit.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 *
 * @author Rick Goldberg
 */
public class Compiler {
    private static ByteArrayOutputStream baosOut;

    
    /**
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
            baosOut = new ByteArrayOutputStream();
            JavaObjectFromString jofs = null;

            try {
                jofs = new JavaObjectFromString(pkg+"."+className, src);
                File workDir = VGlobals.instance().getWorkDirectory();
                StandardJavaFileManager sjfm = compiler.getStandardFileManager(diag, null, null);
                Iterable<? extends JavaFileObject> fileObjects = Arrays.asList(jofs);
                String workDirPath = workDir.getCanonicalPath();
                String[] workClassPath = ((viskit.doe.LocalBootLoader) (VGlobals.instance().getWorkClassLoader())).getClassPath();
                StringBuffer classPaths = new StringBuffer();
                for (String cPath : workClassPath) {
                    classPaths.append(cPath + File.pathSeparator);
                }
                String[] options = {"-classpath", classPaths.toString(), "-d", workDirPath};
                java.util.List<String> optionsList = Arrays.asList(options);
                compiler.getTask(null, sjfm, diag, optionsList, null, fileObjects).call();
                sb = new StringBuffer();
                sb.append(diag.messageString);
            } catch (Exception ex) {
                VGlobals.log.info("Error: Compile JavaObjectFromString " + pkg+"."+className+" " +" " +jofs.toString());
                ex.printStackTrace();
            }
            return sb.toString();
        } 

}


