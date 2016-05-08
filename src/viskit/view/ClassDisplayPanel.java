package viskit.view;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import viskit.ViskitGlobals;
import viskit.util.FindClassesForInterface;
import viskit.ViskitProject;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 14, 2004
 * @since 9:44:55 AM
 * @version $Id$
 */
public class ClassDisplayPanel extends JPanel {

    JButton plusButton, minusButton;
    LegoTree tree;
    JFileChooser jfc;

    public ClassDisplayPanel(LegoTree ltree, String title, String hint, String plusButtonTooltip, String minusButtonTooltip)
	{
        this.tree = ltree;
        jfc = new JFileChooser(ViskitProject.MY_VISKIT_PROJECTS_DIR);
		
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.setAlignmentX(Box.LEFT_ALIGNMENT);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(title); // e.g. "Event Graph Selection"
		titleLabel.setToolTipText(hint);
        titleLabel.setAlignmentX(Box.LEFT_ALIGNMENT);
        buttonPanel.add(titleLabel);
		
        buttonPanel.add(Box.createHorizontalGlue());
		
        JLabel plusMinusButtonLabel = new JLabel("Configuration"); // e.g. "Event Graph Selection"
		plusMinusButtonLabel.setToolTipText("Add or remove files, jars, or directories");
        plusMinusButtonLabel.setAlignmentX(Box.RIGHT_ALIGNMENT);
        buttonPanel.add(plusMinusButtonLabel);
        buttonPanel.add(Box.createHorizontalStrut(4));
		
        plusButton = new JButton(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/plus.png")));
        plusButton.setBorder(null);
        plusButton.setText(null);
        plusButton.setToolTipText(plusButtonTooltip); //"Add event graph class file or directory root to this list");
        Dimension dd = plusButton.getPreferredSize();
        plusButton.setMinimumSize(dd);
        plusButton.setMaximumSize(dd);
        buttonPanel.add(plusButton);
        buttonPanel.add(Box.createHorizontalStrut(10));

        minusButton = new JButton(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/minus.png")));
        minusButton.setDisabledIcon(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/minusGrey.png")));
        minusButton.setBorder(null);
        minusButton.setText(null);
        minusButton.setToolTipText(minusButtonTooltip); //"Remove event graph class file or directory from this list");
        dd = minusButton.getPreferredSize();
        minusButton.setMinimumSize(dd);
        minusButton.setMaximumSize(dd);
        minusButton.setActionCommand("m");
        //minus.setEnabled(false);
        buttonPanel.add(minusButton);
        buttonPanel.add(Box.createHorizontalGlue());
        add(buttonPanel);
		
//		JLabel hintPane = new JLabel ("<html><p align=\"center\">" + hint + "</p></html>");
//        hintPane.setAlignmentX(Box.CENTER_ALIGNMENT);
//        add(hintPane);
        JScrollPane jsp = new JScrollPane(tree);
        jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(jsp);
		
        minusButton.addActionListener(new ActionListener()
		{
            @Override
            public void actionPerformed(ActionEvent e)
			{
                tree.removeSelected();
            }
        });

        plusButton.addActionListener(new ActionListener()
		{
            @Override
            public void actionPerformed(ActionEvent e)
			{
                jfc.setFileFilter(new ClassTypeFilter(tree.getTargetClass()));
                jfc.setAcceptAllFileFilterUsed(false);
                jfc.setMultiSelectionEnabled(true);
                jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

                int returnValue = jfc.showOpenDialog(ClassDisplayPanel.this);
                if (returnValue == JFileChooser.APPROVE_OPTION)
				{
                    File[] selectedFiles = jfc.getSelectedFiles();
                    for (File selectedFile : selectedFiles)
					{
                        tree.addContentRoot(selectedFile, selectedFile.isDirectory());
                    }
                }
            }
        });
    }

    /** Facilitates a class finding utility when a user wishes to manually add a
     * LEGO to the LEGO tree for Assembly construction
     */
    class ClassTypeFilter extends FileFilter {

        private Class<?> targetClass;     // looking for classes of this kind (or jars or directories)

        ClassTypeFilter(Class<?> c) {
            this.targetClass = c;
        }

        @Override
        public boolean accept(File f) {
            if (f.isFile()) {
                String lcnam = f.getName().toLowerCase();
                if (lcnam.endsWith(".jar")) {
                    return true;
                }
                if (lcnam.endsWith(".xml")) {
                    return true;
                }
                if (lcnam.endsWith(".class")) {
                    Class<?> fileClass = getClass(f);
                    if (fileClass == null) {
                        return false;
                    }
                    if (targetClass.isAssignableFrom(fileClass)) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        }

        Class<?> getClass(File f) {
            Class<?> c = null;
            try {
                c = FindClassesForInterface.classFromFile(f, c);
            } catch (Throwable e) {}

            // Here we don't show any classes that are reachable through the viskit classpath..i.e., simkit.jar
            // We want no duplicates there.

            if (c != null) {
                try {
                    Class.forName(c.getName(), false, null);
                    return null;         // this is the negative case
                } catch (ClassNotFoundException e) {
                }        // positive case
            }

            return c;
        }

        @Override
        public String getDescription() {
            return "Java class files, xml files, jar files or directories";
        }
    }
}
