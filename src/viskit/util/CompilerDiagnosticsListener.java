package viskit.util;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/** Taken from viskit.view.SourceWindow to be more broadly used
 *
 * @author Rick Goldberg
 * @version $Id:$
 */
public class CompilerDiagnosticsListener implements DiagnosticListener<JavaFileObject> {

    public long startOffset = -1;
    public long endOffset = 0;
    public long lineNumber;
    public long columnNumber = 0;

    private StringBuilder messageString;

    public CompilerDiagnosticsListener(StringBuilder messageString) {
        this.messageString = messageString;
    }

    @Override
    public void report(Diagnostic<? extends JavaFileObject> message) {
        String msg = message.getMessage(null);

        messageString.append("Viskit has detected ").append(msg).append('\n').append("Code: ").append(message.getCode()).append('\n').append("Kind: ").append(message.getKind()).append('\n').append("Line Number: ").append(message.getLineNumber()).append('\n').append("Column Number: ").append(message.getColumnNumber()).append('\n').append("Position: ").append(message.getPosition()).append('\n').append("Start Position: ").append(message.getStartPosition()).append('\n').append("End Position: ").append(message.getEndPosition()).append('\n').append("Source: ").append(message.getSource()).append('\n');

        if (startOffset == -1) {
            startOffset = message.getStartPosition();
        } else {
            startOffset = startOffset < message.getStartPosition() ? startOffset : message.getStartPosition();
        }

        endOffset = message.getEndPosition();
        lineNumber = message.getLineNumber();
    }

}
