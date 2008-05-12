/*
 * Taken from viskit.SourceWindow. TBD completely factor out from SourceWindow.
 */
package viskit.util;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 *
 * @author Rick Goldberg
 */
public class CompilerDiagnosticsListener implements DiagnosticListener<JavaFileObject> {

    public StringBuffer messageString;
    public long startOffset = -1;
    public long endOffset = 0;
    public long line;
    public long columnNumber = 0;

    public CompilerDiagnosticsListener(StringBuffer messageString) {
        this.messageString = messageString;
    }

    public void report(Diagnostic<? extends JavaFileObject> message) {
        String msg = message.getMessage(null);
        if (msg.indexOf("should be declared in a file named") > 0) {
            msg = "No Compiler Errors";
            messageString.append(msg).append('\n');
        } else {

            messageString.append("Viskit has detected ").append(msg).append('\n').append("Code: ").append(message.getCode()).append('\n').append("Kind: ").append(message.getKind()).append('\n').append("Line Number: ").append(message.getLineNumber()).append('\n').append("Column Number: ").append(message.getColumnNumber()).append('\n').append("Position: ").append(message.getPosition()).append('\n').append("Start Position: ").append(message.getStartPosition()).append('\n').append("End Position: ").append(message.getEndPosition()).append('\n').append("Source: ").append(message.getSource());
        }

        if (startOffset == -1) {
            startOffset = message.getStartPosition();
        } else {
            startOffset = startOffset < message.getStartPosition() ? startOffset : message.getStartPosition();
        }

        endOffset = message.getEndPosition();
        line = message.getLineNumber();
    }

    public long getStartOffset() {
        return startOffset;
    }

    public long getEndOffset() {
        return endOffset;
    }

    public long getLineNumber() {
        return line;
    }

    public long getColumnNumber() {
        return columnNumber;
    }

}
