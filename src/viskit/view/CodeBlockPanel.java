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
package viskit.view;

import viskit.view.dialog.TextAreaDialog;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;


/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Nov 3, 2005
 * @since 4:06:07 PM
 * @version $Id$
 */
public class CodeBlockPanel extends JPanel {

    private JTextComponent jtc;
    private Window owner;
    private String title;
    private JButton editButt;

    public CodeBlockPanel(Window owner, boolean multilined, String title) {
        this.owner = owner;
        this.title = title;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);

        if (multilined) {
            jtc = new myJTextArea();
        } else {
            jtc = new myJTextField("");
        }
        jtc.setOpaque(true);
        jtc.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        jtc.setToolTipText("bogus");

        jtc.addKeyListener(new KeyListener() {

            public void keyTyped(KeyEvent e) {
                if (updateListener != null) {
                    updateListener.actionPerformed(new ActionEvent(jtc.getText(), 0, ""));
                }
            }

            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
            }
        });


        add(jtc);
        add(Box.createHorizontalStrut(3));
        if (!multilined) {
            Dimension d = getPreferredSize();
            d.width = Integer.MAX_VALUE;
            setMaximumSize(d);
        }
        editButt = new JButton(" ... ");
        editButt.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        editButt.setToolTipText("Click to edit a long code block");
        Dimension dd = new Dimension(editButt.getPreferredSize());
        if (!multilined) {
            dd.height = getPreferredSize().height;
        }
        editButt.setMaximumSize(dd);
        add(editButt);

        editButt.addActionListener(new buttListener());
    }

    /**
     * This sets the preferredSize of the codeblock panel to borders plus
     * number of lines specified
     * @param n
     */
    public void setVisibleLines(int n) {
        if (jtc instanceof JTextArea) {
            ((JTextArea) jtc).setRows(n);
            Dimension d = new Dimension(jtc.getPreferredScrollableViewportSize());
            int ph = Math.max(d.height, editButt.getPreferredSize().height);
            ph += getInsets().top + getInsets().bottom;
            setPreferredSize(new Dimension(getPreferredSize().width, ph));
            invalidate();
        }
    }
    private ActionListener updateListener;

    public void addUpdateListener(ActionListener lis) {
        updateListener = lis;
    }

    public String getData() {
        String s = jtc.getText();
        if (s == null) {
            s = "";
        }
        return s;
    }

    public void setData(String s) {
        jtc.setText(s);
    }

    class buttListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            StringBuffer sb = new StringBuffer(jtc.getText().trim());
            boolean modded;
            if (owner instanceof JDialog) {
                modded = TextAreaDialog.showTitledDialog(title, (JDialog) owner, owner, sb);
            } else {
                modded = TextAreaDialog.showTitledDialog(title, (JFrame) owner, (JFrame) owner, sb);
            }
            if (modded) {
                jtc.setText(sb.toString().trim());
                jtc.setCaretPosition(0);
                if (updateListener != null) {
                    updateListener.actionPerformed(new ActionEvent(jtc.getText(), 0, ""));
                }
            }
        }
    }

    class myJTextArea extends JTextArea implements DocumentListener {

        public myJTextArea() {
            super();
            setPreferredSize(new Dimension(50, 50));
            getDocument().addDocumentListener(this);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            String txt = getText();
            if (txt == null || txt.length() <= 0) {
                return null;
            }
            return "<html><pre>" + txt;
        }

        public void changedUpdate(DocumentEvent documentEvent) {
            if (updateListener != null) {
                updateListener.actionPerformed(new ActionEvent(getText(), 0, ""));
            }
        }

        public void insertUpdate(DocumentEvent documentEvent) {
            if (updateListener != null) {
                updateListener.actionPerformed(new ActionEvent(getText(), 0, ""));
            }
        }

        public void removeUpdate(DocumentEvent documentEvent) {
            if (updateListener != null) {
                updateListener.actionPerformed(new ActionEvent(getText(), 0, ""));
            }
        }
    }

    class myJTextField extends JTextField implements DocumentListener {

        public myJTextField(String s) {
            super(s);
            getDocument().addDocumentListener(this);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            String txt = getText();
            if (txt == null || txt.length() <= 0) {
                return null;
            }
            return "<html><pre>" + txt;
        }

        public void changedUpdate(DocumentEvent documentEvent) {
            if (updateListener != null) {
                updateListener.actionPerformed(new ActionEvent(getText(), 0, ""));
            }
        }

        public void insertUpdate(DocumentEvent documentEvent) {
            if (updateListener != null) {
                updateListener.actionPerformed(new ActionEvent(getText(), 0, ""));
            }
        }

        public void removeUpdate(DocumentEvent documentEvent) {
            if (updateListener != null) {
                updateListener.actionPerformed(new ActionEvent(getText(), 0, ""));
            }
        }
    }
}