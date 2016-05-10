package viskit.util;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/** Taken from viskit.view.SourceWindow to be more broadly used
 *
 * @author Rick Goldberg
 * @version $Id$
 */
public class CompilerDiagnosticsListener implements DiagnosticListener<JavaFileObject> {

    public long startOffset = -1;
    public long   endOffset = 0;
    public long   lineNumber;
    public long columnNumber = 0;

    private StringBuilder messageString;

    public CompilerDiagnosticsListener(StringBuilder messageString)
	{
        this.messageString = messageString;
    }

    @Override
    public void report(Diagnostic<? extends JavaFileObject> message)
	{
        String msg = message.getMessage(null);

        messageString.append(msg);
        messageString.append('\n');
        messageString.append("Code: ");
        messageString.append(message.getCode());
        messageString.append('\n');
        messageString.append("Kind: ");
        messageString.append(message.getKind());
        messageString.append('\n');
        messageString.append("line ");
        messageString.append(message.getLineNumber());
        messageString.append(" ");
        messageString.append("column ");
        messageString.append(message.getColumnNumber());
        messageString.append(" ");
        messageString.append("(position ");
        messageString.append(message.getPosition());
        messageString.append(" in characters ");
        messageString.append(message.getStartPosition());
        messageString.append("..");
        messageString.append(message.getEndPosition());
        messageString.append(")");
        messageString.append('\n');
        messageString.append("Source: ");
        messageString.append(message.getSource());
        messageString.append('\n');

        if (startOffset == -1) {
            startOffset = message.getStartPosition();
        } else {
            startOffset = startOffset < message.getStartPosition() ? startOffset : message.getStartPosition();
        }

         endOffset = message.getEndPosition();
        lineNumber = message.getLineNumber();
    }

}
