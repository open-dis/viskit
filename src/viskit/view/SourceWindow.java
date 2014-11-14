package viskit.view;

import edu.nps.util.LogUtils;
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
import org.apache.log4j.Logger;
import viskit.ViskitProject;

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
public class SourceWindow extends JFrame {

    static final Logger LOGGER = LogUtils.getLogger(SourceWindow.class);

    public final String src;
    Thread sysOutThread;
    JTextArea jta;
    private static JFileChooser saveChooser;
    private JPanel contentPane;
    private Searcher searcher;
    private Action startAct;
    private Action againAct;

    public SourceWindow(JFrame main, final String className, String source) {

        this.src = source;
        if (saveChooser == null) {
            saveChooser = new JFileChooser();
            saveChooser.setCurrentDirectory(new File(ViskitProject.MY_VISKIT_PROJECTS_DIR));
        }
        contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);
        JPanel con = new JPanel();
        contentPane.add(con, BorderLayout.CENTER);

        con.setLayout(new BoxLayout(con, BoxLayout.Y_AXIS));
        con.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JToolBar tb = new JToolBar();
        JButton fontPlus = new JButton("Larger");
        JButton fontMinus = new JButton("Smaller");
        JButton printB = new JButton("Print");
        JButton searchButt = new JButton("Find"); // this text gets overwritten by action
        JButton againButt = new JButton("Find next");
        tb.add(new JLabel("Font:"));
        tb.add(fontPlus);
        tb.add(fontMinus);
        tb.add(printB);
        tb.addSeparator();
        tb.add(searchButt);
        tb.add(againButt);
        fontPlus.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                jta.setFont(jta.getFont().deriveFont(jta.getFont().getSize2D() + 1.0f));
            }
        });
        fontMinus.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                jta.setFont(jta.getFont().deriveFont(Math.max(jta.getFont().getSize2D() - 1.0f, 1.0f)));
            }
        });

        printB.setEnabled(false); // todo
        printB.setToolTipText("to be implemented");

        contentPane.add(tb, BorderLayout.NORTH);

        jta = new JTextArea(); //src);
        jta.setText(addLineNums(src));
        jta.setCaretPosition(0);

        jta.setEditable(false);
        jta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane jsp = new JScrollPane(jta);
        con.add(jsp);

        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));

        buttPan.add(Box.createHorizontalGlue());

        JButton compileButt = new JButton("Compile test");
        buttPan.add(compileButt);

        JButton saveButt = new JButton("Save source and close");
        buttPan.add(saveButt);

        JButton closeButt = new JButton("Close");
        buttPan.add(closeButt);

        con.add(buttPan);

        setupSearchKeys();
        searchButt.setAction(startAct);
        againButt.setAction(againAct);

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
                jta.requestFocusInWindow();
            }
        });

        closeButt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SourceWindow.this.dispose();
            }
        });

        compileButt.addActionListener(new ActionListener() {

            StringBuffer sb = new StringBuffer();
            BufferedReader br;

            @Override
            public void actionPerformed(ActionEvent e) {

                // An error stream to write additional error info out to
                ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
                viskit.util.Compiler.setOutPutStream(baosOut);

                try {
                    String diagnostic = viskit.util.Compiler.invoke("", className, src);

                    sb.append(baosOut.toString());
                    sb.append(diagnostic);

                    sysOutDialog.showDialog(SourceWindow.this, SourceWindow.this, sb.toString(), getFileName());

                    if (!diagnostic.contains("No Compiler Errors")) {
                        ErrorHighlightPainter errorHighlightPainter = new ErrorHighlightPainter(Color.PINK);
                        int startOffset = (int) viskit.util.Compiler.getDiagnostic().lineNumber * 5 + (int) viskit.util.Compiler.getDiagnostic().startOffset;
                        int endOffset = (int) viskit.util.Compiler.getDiagnostic().lineNumber * 5 + (int) viskit.util.Compiler.getDiagnostic().endOffset;
                        int columnNumber = (int) viskit.util.Compiler.getDiagnostic().columnNumber;
                        if (startOffset == endOffset) {
                            startOffset = endOffset - columnNumber;
                        }
                        SourceWindow.this.jta.getHighlighter().addHighlight(startOffset, endOffset, errorHighlightPainter);
                        SourceWindow.this.jta.getCaret().setBlinkRate(250);
                        SourceWindow.this.jta.getCaret().setVisible(true);
                        SourceWindow.this.jta.setCaretPosition(startOffset);
                    }

                } catch (Exception ex) {
                    LOGGER.error(ex);
                }
            }
        });

        saveButt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
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
                    JOptionPane.showMessageDialog(null, "Exception on source file write" +
                            "\n" + f.getName() +
                            "\n" + ex.getMessage(),
                            "File I/O Error", JOptionPane.ERROR_MESSAGE);
                }
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
     * @return classname+".java"
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
        searcher = new Searcher(jta, contentPane);

        startAct = new AbstractAction(startSearchHandle) {

            @Override
            public void actionPerformed(ActionEvent e) {
                searcher.startSearch();
                jta.requestFocusInWindow();  // to make the selected text show up if button-initiated
            }
        };
        againAct = new AbstractAction(searchAgainHandle) {

            @Override
            public void actionPerformed(ActionEvent e) {
                searcher.searchAgain();
                jta.requestFocusInWindow();  // to make the selected text show up if button-initiated
            }
        };

        // todo contentPane should work here so the focus can be on the bigger button, etc., and
        // the search will still be done.  I'm doing something wrong.
        InputMap iMap = jta/*contentPane*/.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap aMap = jta/*contentPane*/.getActionMap();

        int cntlKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F, cntlKeyMask);
        iMap.put(key, startSearchHandle);
        aMap.put(startSearchHandle, startAct);

        key = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
        iMap.put(key, searchAgainHandle);
        aMap.put(searchAgainHandle, againAct);

        // Mac uses cmd-G
        String vers = System.getProperty("os.name").toLowerCase();
        if (vers.contains("mac")) {
            key = KeyStroke.getKeyStroke(KeyEvent.VK_G, cntlKeyMask);
            iMap.put(key, searchAgainHandle);
        }
    }
}

class Searcher {

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
            System.err.println(e1.getMessage());
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
class sysOutDialog extends JDialog implements ActionListener {

    private static sysOutDialog dialog;
    private static String value = "";
    private JList<Object> list;
    private JTextArea jta;
    private JScrollPane jsp;

    /**
     * Set up and show the dialog.  The first Component argument
     * determines which frame the dialog depends on; it should be
     * a component in the dialog's controlling frame. The second
     * Component argument should be null if you want the dialog
     * to come up with its left corner in the center of the screen;
     * otherwise, it should be the component on top of which the
     * dialog should appear.
     * @param frameComp
     * @param locationComp
     * @param labelText
     * @param title
     * @return the dialog for this SourceWindow
     */
    public static String showDialog(Component frameComp,
            Component locationComp,
            String labelText,
            String title) {
        Frame frame = JOptionPane.getFrameForComponent(frameComp);
        dialog = new sysOutDialog(frame,
                locationComp,
                labelText,
                title);
        dialog.setVisible(true);
        return value;
    }

    private sysOutDialog(Frame frame,
            Component locationComp,
            String text,
            String title) {
        super(frame, title, true);

        //Create and initialize the buttons.
        JButton cancelButton = new JButton("OK");
        cancelButton.addActionListener(sysOutDialog.this);
        getRootPane().setDefaultButton(cancelButton);

        //main part of the dialog
        jta = new JTextArea(text);
        jta.setCaretPosition(text.length());
        jsp = new JScrollPane(jta);
        jsp.setPreferredSize(new Dimension(frame.getWidth() - 50, frame.getHeight() - 50));
        jsp.setAlignmentX(LEFT_ALIGNMENT);
        jsp.setBorder(BorderFactory.createEtchedBorder());

        //Create a container so that we can add a title around
        //the scroll pane.  Can't add a title directly to the
        //scroll pane because its background would be white.
        //Lay out the label and scroll pane from top to bottom.
        JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        JLabel label = new JLabel("Compiler results");
        label.setLabelFor(list);
        listPane.add(label);
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(jsp); //listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);

        //Put everything together, using the content pane's BorderLayout.
        Container contentPane = getContentPane();
        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);
        pack();
        setLocationRelativeTo(locationComp);
    }

    //Handle clicks on the Set and Cancel buttons.
    @Override
    public void actionPerformed(ActionEvent e) {
        sysOutDialog.dialog.dispose();
    }
}
