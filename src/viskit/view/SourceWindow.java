package viskit.view;

import javax.swing.*;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import viskit.util.Compiler;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Apr 23, 2004
 * @since 3:17:23 PM
 * @version $Id$
 */
@SuppressWarnings("serial")
public class SourceWindow extends JFrame
{
    static final Logger LOG = LogManager.getLogger();

    public final String src;
    JTextArea textArea;
    private static JFileChooser saveChooser;
    private JPanel contentPane;
    private Searcher searcher;
    private Action startAction;
    private Action againAction;

    public SourceWindow(JFrame main, final String className, String source) 
    {
        this.src = source;
        if (saveChooser == null) 
        {
            // remembers user's prior location for next usage
            saveChooser = new JFileChooser();
            saveChooser.setCurrentDirectory(ViskitGlobals.instance().getViskitProject().getSrcDirectory());
        }
        contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);
        JPanel contentPanel = new JPanel();
        contentPane.add(contentPanel, BorderLayout.CENTER);

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JToolBar toolbar = new JToolBar();
        
        JLabel fontLabel = new JLabel("Font:");
        fontLabel.setToolTipText("Font");
        JButton fontPlus       = new JButton("Larger font");
        fontPlus.setToolTipText("Larger font");
        JButton fontMinus      = new JButton("Smaller font");
        fontMinus.setToolTipText("Smaller font");
        JButton printButton    = new JButton("Print");
        printButton.setToolTipText("Print");
        JButton findButton   = new JButton("Find"); // this text gets overwritten by action
        findButton.setToolTipText("Find");
        JButton findNextButton = new JButton("Find next");
        findNextButton.setToolTipText("Find next");
        
//      toolbar.add(fontLabel); // clutter
        toolbar.add(fontPlus);
        toolbar.add(fontMinus);
//      toolbar.add(printButton); // not implemented
        toolbar.addSeparator();
        toolbar.add(findButton);
        toolbar.add(findNextButton);
        fontPlus.addActionListener((ActionEvent e) -> {
            textArea.setFont(textArea.getFont().deriveFont(textArea.getFont().getSize2D() + 1.0f));
        });
        fontMinus.addActionListener((ActionEvent e) -> {
            textArea.setFont(textArea.getFont().deriveFont(Math.max(textArea.getFont().getSize2D() - 1.0f, 1.0f)));
        });

        printButton.setEnabled(false); // todo
        printButton.setToolTipText("to be implemented");

        contentPane.add(toolbar, BorderLayout.NORTH);

        textArea = new JTextArea(); //src);
        textArea.setText(addLineNums(src));
        textArea.setCaretPosition(0);

        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        contentPanel.add(scrollPane);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton compileButton = new JButton("Compile test");
        compileButton.setToolTipText("Compile test");
        buttonPanel.add(compileButton);

        JButton saveButton = new JButton("Save source and close");
        saveButton.setToolTipText("Save source and close");
        buttonPanel.add(saveButton);

        JButton closeButton = new JButton("Close");
        closeButton.setToolTipText("Close");
        buttonPanel.add(closeButton);

        contentPanel.add(buttonPanel);

        setupSearchKeys();
        findButton.setAction(startAction);
        findNextButton.setAction(againAction);

        if (main.isVisible()) {
            this.setSize(main.getWidth() - 200, main.getHeight() - 100);
            this.setLocationRelativeTo(main);
        } else {
            pack();
            Dimension d = getSize();
            d.height = Math.min(d.height, 400);
            d.width = Math.min(d.width, 800);
            setSize(d);
            setLocationRelativeTo(null);
        }
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        //Make textArea get the focus whenever frame is activated.
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowActivated(WindowEvent e) {
                textArea.requestFocusInWindow();
            }
        });

        closeButton.addActionListener((ActionEvent e) -> {
            SourceWindow.this.dispose();
        });

        compileButton.addActionListener(new ActionListener() {

            StringBuffer sb = new StringBuffer();

            @Override
            public void actionPerformed(ActionEvent e) {

                // An error stream to write additional error info out to
                ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
                Compiler.setOutPutStream(baosOut);

                try {
                    String diagnostic = Compiler.invoke("", className, src);

                    sb.append(baosOut.toString());
                    sb.append(diagnostic);

                    SourceCompilationDialog.showDialog(SourceWindow.this, SourceWindow.this, 
                            sb.toString(), "Compilation results for " + getFileName());

                    if (!diagnostic.contains(Compiler.COMPILE_SUCCESS_MESSAGE)) {
                        ErrorHighlightPainter errorHighlightPainter = new ErrorHighlightPainter(Color.PINK);
                        int startOffset = (int) Compiler.getDiagnostic().lineNumber * 5 + (int) Compiler.getDiagnostic().startOffset;
                        int endOffset = (int) Compiler.getDiagnostic().lineNumber * 5 + (int) Compiler.getDiagnostic().endOffset;
                        int columnNumber = (int) Compiler.getDiagnostic().columnNumber;
                        if (startOffset == endOffset) {
                            startOffset = endOffset - columnNumber;
                        }
                        SourceWindow.this.textArea.getHighlighter().addHighlight(startOffset, endOffset, errorHighlightPainter);
                        SourceWindow.this.textArea.getCaret().setBlinkRate(250);
                        SourceWindow.this.textArea.getCaret().setVisible(true);
                        SourceWindow.this.textArea.setCaretPosition(startOffset);
                    }

                } catch (BadLocationException ex) {
                    LOG.error(ex);
                }

                // Reset the message buffer for the next compilation
                sb.setLength(0);
            }
        });

        saveButton.addActionListener((ActionEvent e) -> {
            String fn = getFileName();
            saveChooser.setSelectedFile(new File(saveChooser.getCurrentDirectory(), fn));
            int ret = saveChooser.showSaveDialog(SourceWindow.this);
            if (ret != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File f = saveChooser.getSelectedFile();

            if (f.exists()) {
                int r = JOptionPane.showConfirmDialog(SourceWindow.this, "File exists.  Overwrite?", "Confirm",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (r != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try {
                try (FileWriter fw = new FileWriter(f)) {
                    fw.write(src);
                }
                SourceWindow.this.dispose();
            } catch (IOException ex) {
                ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE,
                        "File I/O Error",
                        "\n" + f.getName() + "\n" + ex.getMessage()
                );
            }
        });
    }

    public class ErrorHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {

        public ErrorHighlightPainter(Color color) {
            super(color);
        }
    }

    private String addLineNums(String src) {
        // Choose the right lineNumber ending
        String le = "\r\n";
        String le2 = "\n";
        String le3 = "\r";

        String[] sa = src.split(le);
        String[] sa2 = src.split(le2);
        String[] sa3 = src.split(le3);

        // Whichever broke the string up into the most pieces is our boy
        // unless the windoze one works
        if (sa.length <= 1) {
            if (sa2.length > sa.length) {
                sa = sa2;
                le = le2;
            }
            if (sa3.length > sa.length) {
                sa = sa3;
                le = le3;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sa.length; i++) {
            String n = "" + (i + 1);
            int diff = 3 - n.length();        // right align number, 3 digits, pad w/ spaces on left
            for (int j = 0; j < diff; j++) {
                sb.append(" ");
            }
            sb.append(n);
            sb.append(": ");
            sb.append(sa[i]);
            sb.append(le);
        }
        return sb.toString();
    }

    /**
     * Get the file name from the class statement
     * @return className+".java"
     */
    private String getFileName() {
        String[] nm = src.split("\\bclass\\b"); // find the class, won't work if there is the word 'class' in top comments
        if (nm.length >= 2) {
            nm = nm[1].split("\\b");            // find the space after the class
            int idx = 0;
            while (idx < nm.length) {
                if (nm[idx] != null && nm[idx].trim().length() > 0) {
                    return nm[idx].trim() + ".java";
                }
                idx++;
            }
        }
        return "unnamed.java";
    }
    private String startSearchHandle = "Find";
    private String searchAgainHandle = "Find next";

    private void setupSearchKeys() {
        searcher = new Searcher(textArea, contentPane);

        startAction = new AbstractAction(startSearchHandle) {

            @Override
            public void actionPerformed(ActionEvent e) {
                searcher.startSearch();
                textArea.requestFocusInWindow();  // to make the selected text show up if button-initiated
            }
        };
        againAction = new AbstractAction(searchAgainHandle) {

            @Override
            public void actionPerformed(ActionEvent e) {
                searcher.searchAgain();
                textArea.requestFocusInWindow();  // to make the selected text show up if button-initiated
            }
        };

        // todo contentPane should work here so the focus can be on the bigger button, etc., and
        // the search will still be done.  I'm doing something wrong.
        InputMap iMap = textArea/*contentPane*/.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap aMap = textArea/*contentPane*/.getActionMap();

        int cntlKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F, cntlKeyMask);
        iMap.put(key, startSearchHandle);
        aMap.put(startSearchHandle, startAction);

        key = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
        iMap.put(key, searchAgainHandle);
        aMap.put(searchAgainHandle, againAction);

        // Mac uses cmd-G
        String vers = ViskitStatics.OPERATING_SYSTEM.toLowerCase();
        if (vers.contains("mac")) {
            key = KeyStroke.getKeyStroke(KeyEvent.VK_G, cntlKeyMask);
            iMap.put(key, searchAgainHandle);
        }
    }
}

class Searcher
{
    static final Logger LOG = LogManager.getLogger();

    JTextComponent jtc;
    Document doc;
    JComponent comp;

    Searcher(JTextComponent jt, JComponent comp) {
        jtc = jt;
        doc = jt.getDocument();
        this.comp = comp;
    }
    Matcher mat;

    void startSearch() {
        String inputValue = JOptionPane.showInputDialog(comp, "Enter search string");
        if (inputValue == null || inputValue.length() <= 0) {
            return;
        }

        try {
            String s = doc.getText(doc.getStartPosition().getOffset(), doc.getEndPosition().getOffset());
            Pattern pat = Pattern.compile(inputValue, Pattern.CASE_INSENSITIVE);
            mat = pat.matcher(s);

            if (!checkAndShow()) {
                mat = null;
            }
        } catch (BadLocationException e1) {
            LOG.error(e1.getMessage());
        }
    }

    boolean checkAndShow() {
        if (mat.find()) {
            jtc.select(mat.start(), mat.end());
            return true;
        }
        jtc.select(0, 0); // none
        return false;
    }

    void searchAgain() {
        if (mat == null) {
            return;
        }

        if (!checkAndShow()) {
            // We found one originally, but must have run out the bottom
            mat.reset();
            checkAndShow();
        }
    }
}

@SuppressWarnings("serial")
class SourceCompilationDialog extends JDialog implements ActionListener 
{
    static final Logger LOG = LogManager.getLogger();
    
    private static SourceCompilationDialog dialog;
    private JList<Object> objectList;
    private final JTextArea textArea;
    private final JScrollPane scrollPane;

    /**
     * Set up and show the dialog.  The first Component argument
     * determines which frame the dialog depends on; it should be
     * a component in the dialog's controlling frame. The second
     * Component argument should be null if you want the dialog
     * to come up with its left corner in the center of the screen;
     * otherwise, it should be the component on top of which the
     * dialog should appear.
     * @param frameComponent
     * @param sourceCompilationText
     * @param labelText
     * @param title
     * @return the dialog for this SourceWindow
     */
    public static String showDialog(Component frameComponent,
            Component locationComponent,
            String sourceCompilationText,
            String title)
    {
        Frame frame = JOptionPane.getFrameForComponent(frameComponent);
        dialog = new SourceCompilationDialog(frame,
                locationComponent,
                sourceCompilationText,
                title);
        dialog.setVisible(true);
        return "";
    }

    private SourceCompilationDialog(Frame frame,
            Component locationComponent,
            String compilerResult,
            String title) 
    {
        super(frame, title, true);

        //Create and initialize the buttons.
        JButton okButton = new JButton("OK");
        okButton.addActionListener(SourceCompilationDialog.this);
        getRootPane().setDefaultButton(okButton);

        textArea = new JTextArea(compilerResult);
        textArea.setCaretPosition(compilerResult.length());
        scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(frame.getWidth() - 50, frame.getHeight() - 50));
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
        scrollPane.setBorder(BorderFactory.createEtchedBorder());

        //Create a container so that we can add a title around
        //the scroll pane.  Can't add a title directly to the
        //scroll pane because its background would be white.
        //Lay out the label and scroll pane from top to bottom.
        JPanel sourceCompilationResultsPanel = new JPanel();
        sourceCompilationResultsPanel.setLayout(new BoxLayout(sourceCompilationResultsPanel, BoxLayout.PAGE_AXIS));
        // TODO: Prepend Compiler test: {file name here} as the panel title
        JLabel sourceCompilationResultsLabel = new JLabel("Compiler results");
        sourceCompilationResultsLabel.setLabelFor(objectList);
        sourceCompilationResultsPanel.add(sourceCompilationResultsLabel);
        sourceCompilationResultsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        sourceCompilationResultsPanel.add(scrollPane); //listScroller);
        sourceCompilationResultsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);

        //Put everything together, using the content pane's BorderLayout.
        Container contentPane = getContentPane();
        contentPane.add(sourceCompilationResultsPanel, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);
        pack();
        setLocationRelativeTo(locationComponent);
        
        if (compilerResult.contains("Kind: ERROR"))
        {
            LOG.error("autogenerated Java compilation error\n" +
                      "====================================\n" + 
                      compilerResult +
                      "====================================");
            sourceCompilationResultsLabel.setForeground(Color.RED.darker());
        }
    }

    //Handle clicks on the Set and Cancel buttons.
    @Override
    public void actionPerformed(ActionEvent e) {
        SourceCompilationDialog.dialog.dispose();
    }
}
