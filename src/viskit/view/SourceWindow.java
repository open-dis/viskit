package viskit.view;

import edu.nps.util.LogUtilities;
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
    static final Logger LOGGER = LogUtilities.getLogger(SourceWindow.class);

    public final String sourceCode;
    Thread systemOutThread;
    JTextArea textArea;
    private static JFileChooser saveChooser;
    private JPanel contentPane;
    private Searcher searcher;
    private Action startAction;
    private Action againAction;

    public SourceWindow(JFrame main, final String className, String source)
	{
        this.sourceCode = source;
        if (saveChooser == null)
		{
            saveChooser = new JFileChooser();
		    saveChooser.setDialogTitle("Java Source Code");
			if (ViskitGlobals.instance().getCurrentViskitProject() != null)
				saveChooser.setCurrentDirectory(ViskitGlobals.instance().getCurrentViskitProject().getSrcDirectory());
        }
        contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);
        JPanel containerPanel = new JPanel();
        contentPane.add(containerPanel, BorderLayout.CENTER);

        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));
        containerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JToolBar toolBar = new JToolBar();
        JButton  fontPlusButton = new JButton("Larger");
        JButton fontMinusButton = new JButton("Smaller");
        JButton     printButton = new JButton("Print");
        JButton    searchButton = new JButton("Find"); // this text gets overwritten by action
        JButton     againButton = new JButton("Find next");
        toolBar.add(new JLabel("Font:"));
        toolBar.addSeparator(); // TODO ok?
        toolBar.add(fontPlusButton);
        toolBar.add(fontMinusButton);
        toolBar.addSeparator();
        toolBar.add(searchButton);
        toolBar.add(againButton);
        toolBar.addSeparator();
        toolBar.add(printButton);
        fontPlusButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.setFont(textArea.getFont().deriveFont(textArea.getFont().getSize2D() + 1.0f));
            }
        });
        fontMinusButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.setFont(textArea.getFont().deriveFont(Math.max(textArea.getFont().getSize2D() - 1.0f, 1.0f)));
            }
        });

        printButton.setEnabled(false); // todo
        printButton.setToolTipText("to be implemented");

        contentPane.add(toolBar, BorderLayout.NORTH);

        textArea = new JTextArea();
        textArea.setText(addLineNumbers(sourceCode));
        textArea.setCaretPosition(0);

        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane jScrollPane = new JScrollPane(textArea);
        containerPanel.add(jScrollPane);

        JPanel buttonPanel = new JPanel();
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton xmlButton = new JButton("XML model");
        buttonPanel.add(xmlButton);

        JButton compileButton = new JButton("Compile test");
        buttonPanel.add(compileButton);

        JButton saveButton = new JButton("Save source and close");
        buttonPanel.add(saveButton);

        JButton closeButton = new JButton("Close");
        buttonPanel.add(closeButton);

        containerPanel.add(buttonPanel);

        setupSearchKeys();
        searchButton.setAction(startAction);
         againButton.setAction(againAction);

        if (main.isVisible())
		{
            this.setSize(main.getWidth() - 200, main.getHeight() - 100);
            this.setLocationRelativeTo(main);
        } 
		else 
		{
            pack();
            Dimension d = getSize();
            d.height = Math.min(d.height, 800); // no larger than 800x600
            d.width  = Math.min(d.width,  600);
            setSize(d);
            setLocationRelativeTo(null);
        }
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setModalExclusionType(Dialog.ModalExclusionType.NO_EXCLUDE);

        //Make textArea get the focus whenever frame is activated.
        addWindowListener(new WindowAdapter()
		{
            @Override
            public void windowActivated(WindowEvent e) {
                textArea.requestFocusInWindow();
            }
        });

        closeButton.addActionListener(new ActionListener()
		{
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
		
		xmlButton.addActionListener(new ActionListener()
		{
            @Override
            public void actionPerformed(ActionEvent e)
			{
                try {
					if      (ViskitGlobals.instance().getViskitApplicationFrame().isEventGraphEditorTabSelected())
					{
						     ViskitGlobals.instance().getEventGraphController().showXML();
					}
					else if (ViskitGlobals.instance().getViskitApplicationFrame().isAssemblyEditorTabSelected())
					{
						     ViskitGlobals.instance().getAssemblyController().showXML();
					}
				}
				catch (Exception ex) 
				{
                    LOGGER.error(ex);
                }
            }
        });

        compileButton.addActionListener(new ActionListener()
		{
            StringBuffer sb = new StringBuffer();
            BufferedReader br;

            @Override
            public void actionPerformed(ActionEvent e) {

                // An error stream to write additional error info out to
                ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
                Compiler.setOutputStream(baosOut);

                try {
                    String diagnostic = Compiler.invoke("", className, sourceCode);

                    sb.append(baosOut.toString());
                    sb.append(diagnostic);

                    CompilationOutputDialog.showDialog(SourceWindow.this, SourceWindow.this, sb.toString(), getFileName());

                    if (!diagnostic.contains(Compiler.COMPILE_SUCCESS_MESSAGE))
					{
                        ErrorHighlightPainter errorHighlightPainter = new ErrorHighlightPainter(Color.PINK);
                        int startOffset = (int) Compiler.getDiagnostic().lineNumber * 5 + (int) Compiler.getDiagnostic().startOffset;
                        int   endOffset = (int) Compiler.getDiagnostic().lineNumber * 5 + (int) Compiler.getDiagnostic().endOffset;
                        int columnNumber = (int) Compiler.getDiagnostic().columnNumber;
                        if (startOffset == endOffset) {
                            startOffset = endOffset - columnNumber;
                        }
                        textArea.getHighlighter().addHighlight(startOffset, endOffset, errorHighlightPainter);
                        textArea.getCaret().setBlinkRate(250);
                        textArea.getCaret().setVisible(true);
                        textArea.setCaretPosition(startOffset);
                    }
                } 
				catch (Exception ex) 
				{
                    LOGGER.error(ex);
                }
                // Reset the message buffer for the next compilation
                sb.setLength(0);
            }
        });

        saveButton.addActionListener(new ActionListener()
		{
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
                        fw.write(sourceCode);
                    }
                    dispose();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Exception on source file write" +
                            "\n" + f.getName() +
                            "\n" + ex.getMessage(),
                            "File Input/Output Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    public class ErrorHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {

        public ErrorHighlightPainter(Color color) {
            super(color);
        }
    }

    private String addLineNumbers(String src) {
        // Choose the right lineNumber ending
        String le  = "\r\n";
        String le2 = "\n";
        String le3 = "\r";

        String[] sa  = src.split(le);
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
        String[] nm = sourceCode.split("\\bclass\\b"); // find the class, won't work if there is the word 'class' in top comments
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

        int cntlKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
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
class CompilationOutputDialog extends JDialog implements ActionListener {

    private static CompilationOutputDialog dialog;
    private static String value = "";
    private JList<Object> list;
    private JTextArea textArea;
    private JScrollPane textScrollPane;

    /**
     * Set up and show the dialog.  The first Component argument
     * determines which frame the dialog depends on; it should be
     * a component in the dialog's controlling frame. The second
     * Component argument should be null if you want the dialog
     * to come up with its left corner in the center of the screen;
     * otherwise, it should be the component on top of which the
     * dialog should appear.
     * @param frameComponent
     * @param locationComponent
     * @param labelText
     * @param title
     * @return the dialog for this SourceWindow
     */
    public static String showDialog(Component frameComponent,
            Component locationComponent,
            String labelText,
            String title) {
        Frame frame = JOptionPane.getFrameForComponent(frameComponent);
        dialog = new CompilationOutputDialog(frame,
                locationComponent,
                labelText,
                title);
        dialog.setVisible(true);
        return value;
    }

    private CompilationOutputDialog(Frame frame,
            Component locationComponent,
            String text,
            String title) {
        super(frame, title + " compilation", true);
		
		this.setModal(false);

        //Create and initialize the buttons.
        JButton okButton = new JButton("OK");
        okButton.addActionListener(CompilationOutputDialog.this);
        getRootPane().setDefaultButton(okButton);

        //main part of the dialog
        textArea = new JTextArea(text);
        textArea.setCaretPosition(text.length());
        textScrollPane = new JScrollPane(textArea);
        textScrollPane.setPreferredSize(new Dimension(frame.getWidth() - 50, frame.getHeight() - 50));
        textScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        textScrollPane.setBorder(BorderFactory.createEtchedBorder());

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
        listPane.add(textScrollPane); //listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);

        // Put everything together, using the content pane's BorderLayout.
        Container contentPane = getContentPane();
        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);
        pack();
		Dimension d = locationComponent.getSize(); // match
		d.height = Math.min(d.height, 800); // no larger than 800x600
		d.width  = Math.min(d.width,  600);
		setSize(d);
//        setLocationRelativeTo(locationComp);
		this.setLocation(locationComponent.getLocation().x + (locationComponent.getWidth()), // shift right of center
						 locationComponent.getLocation().y);
    }

    //Handle clicks on the Set and Cancel buttons.
    @Override
    public void actionPerformed(ActionEvent e) {
        CompilationOutputDialog.dialog.dispose();
    }
}
