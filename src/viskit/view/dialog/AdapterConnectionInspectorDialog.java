/*
Copyright (c) 1995-2008 held by the author(s).  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other materials provided with the
      distribution.
    * Neither the names of the Naval Postgraduate School (NPS)
      Modeling Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and http://www.movesinstitute.org)
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/
package viskit.view.dialog;

import edu.nps.util.SpringUtilities;
import static edu.nps.util.GenericConversion.toArray;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Vector;
import javax.swing.text.JTextComponent;
import viskit.model.AdapterEdge;
import viskit.model.EvGraphNode;
import viskit.VGlobals;
import viskit.VStatics;
import viskit.control.AssemblyController;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since June 2, 2004
 * @since 9:19:41 AM
 * @version $Id$
 */
public class AdapterConnectionInspectorDialog extends JDialog {

    private JLabel sourceLab,  targetLab,  nameLab,  descLab;
    private JTextField sourceTF,  targetTF,  nameTF,  descTF;
    private JTextField sourceEventTF,  targetEventTF;
    private JLabel sourceEventLab,  targetEventLab;
    private JButton evSourceNavButt,  evTargetNavButt;
    private JPanel sourceEventPan,  targetEventPan;
    private static AdapterConnectionInspectorDialog dialog;
    private static boolean modified = false;
    private EvGraphNode sourceEVG,  targetEVG;
    private AdapterEdge adapterEdge;
    private JButton okButt,  canButt;
    private JPanel buttPan;
    private enableApplyButtonListener lis;
    public static String xnewProperty;
    public static String newTarget,  newTargetEvent,  newSource,  newSourceEvent;

    public static boolean showDialog(JFrame f, AdapterEdge parm) {
        if (dialog == null) {
            dialog = new AdapterConnectionInspectorDialog(f, parm);
        } else {
            dialog.setParams(f, parm);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private AdapterConnectionInspectorDialog(JFrame parent, AdapterEdge ed) {
        super(parent, "Adapter Connection", true);
        adapterEdge = ed;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        lis = new enableApplyButtonListener();

        nameLab = new JLabel("adapter name", JLabel.TRAILING);
        sourceLab = new JLabel("source event graph", JLabel.TRAILING);
        targetLab = new JLabel("target event graph", JLabel.TRAILING);

        nameTF = new JTextField();
        float[] tfColors = nameTF.getBackground().getRGBColorComponents(null);
        Color tfBack = new Color(tfColors[0] * 0.95f, tfColors[1] * 0.95f, tfColors[2] * 0.95f);
        sourceTF = new JTextField();
        targetTF = new JTextField();
        sourceEventTF = new JTextField();
        sourceEventTF.setEditable(false); // events are chosen from list
        sourceEventTF.setBackground(tfBack);
        VStatics.clampHeight(sourceEventTF);
        targetEventTF = new JTextField();
        targetEventTF.setEditable(false); // events are chosen from list
        targetEventTF.setBackground(tfBack);
        VStatics.clampHeight(targetEventTF);

        evSourceNavButt = new JButton("...");
        evSourceNavButt.addActionListener(new findSourceEventsAction());
        evSourceNavButt.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        VStatics.clampHeight(evSourceNavButt, sourceEventTF);
        evTargetNavButt = new JButton("...");
        evTargetNavButt.addActionListener(new findTargetEventsAction());
        evTargetNavButt.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        VStatics.clampHeight(evTargetNavButt, targetEventTF);

        sourceEventLab = new JLabel("source event", JLabel.TRAILING);
        sourceEventPan = new JPanel();
        sourceEventPan.setLayout(new BoxLayout(sourceEventPan, BoxLayout.X_AXIS));
        sourceEventPan.add(sourceEventTF);
        sourceEventPan.add(evSourceNavButt);

        targetEventLab = new JLabel("target event", JLabel.TRAILING);
        targetEventPan = new JPanel();
        targetEventPan.setLayout(new BoxLayout(targetEventPan, BoxLayout.X_AXIS));
        targetEventPan.add(targetEventTF);
        targetEventPan.add(evTargetNavButt);

        descLab = new JLabel("description", JLabel.TRAILING);
        descTF = new JTextField();

        pairWidgets(nameLab, nameTF, true);
        pairWidgets(sourceLab, sourceTF, false);
        pairWidgets(targetLab, targetTF, false);
        pairWidgets(sourceEventLab, sourceEventPan, true);
        pairWidgets(targetEventLab, targetEventPan, true);
        pairWidgets(descLab, descTF, true);

        nameTF.addCaretListener(lis);
        sourceEventTF.addCaretListener(lis);
        targetEventTF.addCaretListener(lis);

        buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        canButt = new JButton("Cancel");
        okButt = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(canButt);
        buttPan.add(okButt);
        buttPan.add(Box.createHorizontalStrut(5));

        fillWidgets();     // put the data into the widgets

        modified = (ed == null);     // if it's a new pclNode, they can always accept defaults with no typing
        okButt.setEnabled((ed == null));

        getRootPane().setDefaultButton(canButt);

        pack();     // do this prior to next

        // Make the first display a minimum of 400 width
        Dimension d = getSize();
        d.width = Math.max(d.width, 400);
        setSize(d);

        this.setLocationRelativeTo(parent);

        // attach listeners
        canButt.addActionListener(new cancelButtonListener());
        okButt.addActionListener(new applyButtonListener());
    }

    private void pairWidgets(JLabel lab, JComponent tf, boolean edit) {
        VStatics.clampHeight(tf);
        lab.setLabelFor(tf);
        if (tf instanceof JTextField) {
            ((JTextComponent) tf).setEditable(edit);
            if (edit) {
                ((JTextComponent) tf).addCaretListener(lis);
            }
        }
    }

    public void setParams(Component c, AdapterEdge ae) {
        adapterEdge = ae;

        fillWidgets();

        modified = (ae == null);
        okButt.setEnabled((ae == null));

        getRootPane().setDefaultButton(canButt);

        this.setLocationRelativeTo(c);
    }

    private void fillWidgets() {
        if (adapterEdge != null) {
            nameTF.setText(adapterEdge.getName());
            sourceEVG = (EvGraphNode) adapterEdge.getFrom();
            sourceTF.setText(sourceEVG.getName() + " (" + sourceEVG.getType() + ")");
            sourceEventTF.setText(adapterEdge.getSourceEvent());
            targetEVG = (EvGraphNode) adapterEdge.getTo();
            targetTF.setText(targetEVG.getName() + " (" + targetEVG.getType() + ")");
            targetEventTF.setText(adapterEdge.getTargetEvent());
            descTF.setText(adapterEdge.getDescriptionString());
        } else {
            sourceTF.setText("");
            sourceEventTF.setText("");
            targetTF.setText("");
            targetEventTF.setText("");
            descTF.setText("");
        }

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

        JPanel cont = new JPanel(new SpringLayout());
        cont.add(nameLab);
        cont.add(nameTF);
        cont.add(sourceLab);
        cont.add(sourceTF);
        cont.add(sourceEventLab);
        cont.add(sourceEventPan);
        cont.add(targetLab);
        cont.add(targetTF);
        cont.add(targetEventLab);
        cont.add(targetEventPan);
        cont.add(descLab);
        cont.add(descTF);

        SpringUtilities.makeCompactGrid(cont, 6, 2, 10, 10, 5, 5);
        content.add(cont);
        content.add(buttPan);
        content.add(Box.createVerticalStrut(5));
        setContentPane(content);
    }

    private void unloadWidgets() {
        if (adapterEdge != null) {
            adapterEdge.setName(nameTF.getText().trim());
            adapterEdge.setSourceEvent(sourceEventTF.getText().trim());
            adapterEdge.setTargetEvent(targetEventTF.getText().trim());
            adapterEdge.setDescriptionString(descTF.getText().trim());
        }
    //todo implement
    //newTarget,newTargetEvent,newSource,newSourceEvent;

    }

    class cancelButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            modified = false;    // for the caller
            dispose();
        }
    }

    class applyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            String stf = sourceEventTF.getText().trim();
            String ttf = targetEventTF.getText().trim();
            if (stf == null || stf.length() == 0 || ttf == null || ttf.length() == 0) {
                JOptionPane.showMessageDialog(AdapterConnectionInspectorDialog.this, "Source and target events must be entered.");
                return;
            }
            if (modified) {
                unloadWidgets();
            }
            dispose();
        }
    }

    class enableApplyButtonListener implements CaretListener, ActionListener {

        @Override
        public void caretUpdate(CaretEvent event) {
            modified = true;
            okButt.setEnabled(true);
            getRootPane().setDefaultButton(okButt);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            caretUpdate(null);
        }
    }

    class findSourceEventsAction implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            commonFindEvents(sourceEVG, sourceEventTF);
        }
    }

    class findTargetEventsAction implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            commonFindEvents(targetEVG, targetEventTF);
        }
    }

    private void commonFindEvents(EvGraphNode node, JTextField selection) {
        Class<?> c;
        String classname = node.getType();
        try {
            c = VStatics.classForName(classname);
            if (c == null) {
                throw new ClassNotFoundException("classname not found");
            }
            Method[] methods = c.getMethods();
            //assert (methods != null && methods.length > 0);
            Vector<String> evsv = new Vector<>();
            if (methods != null && methods.length > 0) {
                for (Method m : methods) {
                    if (!m.getReturnType().getName().equals("void")) {
                        continue;
                    }
                    if (m.getModifiers() != Modifier.PUBLIC) {
                        continue;
                    }
                    String nm = m.getName();
                    if (!nm.startsWith("do")) {
                        continue;
                    }
                    if (nm.equals("doRun")) {
                        continue;
                    }

                    evsv.add(nm.substring(2));
                }
            }
            if (evsv.size() <= 0) {
                JOptionPane.showMessageDialog(AdapterConnectionInspectorDialog.this, "No events found in " + classname + ".");
                return;
            }
            String[] sa = new String[evsv.size()];
            int which = EventListDialog.showDialog(AdapterConnectionInspectorDialog.this, AdapterConnectionInspectorDialog.this,
                    classname + " Events", toArray(evsv, sa));
            if (which != -1) {
                modified = true;
                selection.setText(evsv.get(which));
            }
        } catch (ClassNotFoundException | SecurityException | HeadlessException t) {
            System.err.println("Error connecting: " + t.getMessage());
        }
//    catch (ClassNotFoundException e) {
//      e.printStackTrace();
//    }
    }

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified == true) {
                int ret = JOptionPane.showConfirmDialog(AdapterConnectionInspectorDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
                    okButt.doClick();
                } else {
                    canButt.doClick();
                }
            } else {
                canButt.doClick();
            }
        }
    }

    void clampHeight(JComponent comp) {
        Dimension d = comp.getPreferredSize();
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));
        comp.setMinimumSize(new Dimension(Integer.MAX_VALUE, d.height));
    }
}
