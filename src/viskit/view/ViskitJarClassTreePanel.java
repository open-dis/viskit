package viskit.view;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
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
public class ViskitJarClassTreePanel extends JPanel {

    JButton plusButton, minusButton;
    LegoTree legoTree;
    JFileChooser fileChooser;

    public ViskitJarClassTreePanel(LegoTree legoTree, String title, String titleTooltip, String plusTT, String minusTT) 
    {
        this.legoTree = legoTree;
        this.legoTree.setToolTipText(titleTooltip);
        fileChooser = new JFileChooser(ViskitProject.VISKIT_PROJECTS_DIRECTORY);

        add(Box.createHorizontalStrut(10)); // TODO some spacing before PCL header
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JLabel topLabel = new JLabel(title); // e.g. "Event Graphs" or "Property Change Listeners"
        topLabel.setToolTipText(titleTooltip);
        topLabel.setAlignmentX(Box.CENTER_ALIGNMENT);
        add(topLabel);
        
        JScrollPane scrollPane = new JScrollPane(this.legoTree);
        scrollPane.setToolTipText(titleTooltip);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        plusButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/plus.png")));
        plusButton.setBorder(null);
        plusButton.setText(null);
        plusButton.setToolTipText(plusTT); //"Add a new Event Graph class file or directory root to this list");
        Dimension dd = plusButton.getPreferredSize();
        plusButton.setMinimumSize(dd);
        plusButton.setMaximumSize(dd);
        buttonPanel.add(plusButton);
        buttonPanel.add(Box.createHorizontalStrut(10));

        minusButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/minus.png")));
        minusButton.setDisabledIcon(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/minusGrey.png")));
        minusButton.setBorder(null);
        minusButton.setText(null);
        minusButton.setToolTipText(minusTT); //"Remove event graph class file or directory from this list");
        dd = minusButton.getPreferredSize();
        minusButton.setMinimumSize(dd);
        minusButton.setMaximumSize(dd);
        minusButton.setActionCommand("m");
        //minus.setEnabled(false);
        buttonPanel.add(minusButton);
        buttonPanel.add(Box.createHorizontalGlue());
        add(buttonPanel);

        minusButton.addActionListener((ActionEvent e) -> {
            if (this.legoTree.isSelectionEmpty())
            {
                String treeType;
                if   (title.toLowerCase().contains("event graph"))
                     treeType = "an Event Graph";
                else treeType = "a Property Change Listener";
                String message = "First select before removing " + treeType + " from the tree list";
                ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.WARNING_MESSAGE,
                "Select an item first", message);
            }
            this.legoTree.removeSelected();
        });
        plusButton.addActionListener((ActionEvent e) -> {
            if      (title.toLowerCase().contains("event graph"))
                     fileChooser.setDialogTitle("Open Event Graph File");
            else if (title.toLowerCase().contains("property"))
                     fileChooser.setDialogTitle("Open Property Change Listener File");
            fileChooser.setFileFilter(new ClassTypeFilter(this.legoTree.getTargetClass()));
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            
            int retv = fileChooser.showOpenDialog(ViskitJarClassTreePanel.this);
            if (retv == JFileChooser.APPROVE_OPTION) {
                File[] fileArray = fileChooser.getSelectedFiles();
                for (File file : fileArray) {
                    this.legoTree.addContentRoot(file, file.isDirectory());
                }
            }
        });
    }

    /** Facilitates a class finding utility when a user wishes to manually add a
 LEGO to the LEGO legoTree for Assembly construction
     */
    class ClassTypeFilter extends FileFilter {

        private final Class<?> targetClass;     // looking for classes of this kind (or jars or directories)

        ClassTypeFilter(Class<?> c) {
            this.targetClass = c;
        }

        @Override
        public boolean accept(File f) {
            if (f.isFile()) {
                String lowerCaseName = f.getName().toLowerCase();
                if (lowerCaseName.endsWith(".jar")) {
                    return true;
                }
                if (lowerCaseName.endsWith(".xml")) {
                    return true;
                }
                if (lowerCaseName.endsWith(".class")) {
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
            Class<?> nextClass = null;
            try {
                nextClass = FindClassesForInterface.classFromFile(f, nextClass);
            } catch (Throwable e) {}

            // Here we don't show any classes that are reachable through the viskit classpath..i.e., simkit.jar
            // We want no dups there.

            if (nextClass != null) {
                try {
                    Class.forName(nextClass.getName(), false, null);
                    return null;         // this is the negative case
                } catch (ClassNotFoundException e) {
                }        // positive case
            }
            return nextClass;
        }

        @Override
        public String getDescription() {
            return "Java class files, xml files, jar files or directories";
        }
    }
}
