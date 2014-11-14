package viskit.util;

import java.io.IOException;
import java.net.URI;
import javax.tools.SimpleJavaFileObject;

/** This is a helper class for the javax internal compiler
 * @version $Id$
 */
public class JavaObjectFromString extends SimpleJavaFileObject {

    private String contents = null;

    public JavaObjectFromString(String className, String contents) throws Exception {
        super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.contents = contents;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return contents;
    }
}
