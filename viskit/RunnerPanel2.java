/*
Copyright (c) 1995-2005 held by the author(s).  All rights reserved.

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
      (http://www.nps.edu and http://www.MovesInstitute.org)
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

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @author Rick Goldberg
 * @since Jul 17, 2006
 * @since 3:17:07 PM
 */

package viskit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Document;
import java.awt.*;
import java.io.*;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;


/**
 * A VCR-controls and TextArea panel.  Sends Simkit output to TextArea
 */
public class RunnerPanel2 extends JPanel
{ 
  private final int STEPSIZE = 100; // adjusts speed of top/bottom scroll arrows
  private final long PAGESIZE = 4096; // should not change this
  FileChaser fileChaser;
  MappedByteBuffer mbb;
  TextUpdater textUpdater;
  BlockingQueue<ByteBuffer> bq = new LinkedBlockingQueue<ByteBuffer>();
  private FileChannel fc;
  private long fcSize;
  private long posn = 0;
  public boolean dump = true;  
  public boolean search;
  String lineEnd = System.getProperty("line.separator");
  public JScrollPane jsp;
  public JTextArea soutTA, serrTA;
  public JSplitPane xsplPn;
  public JButton vcrStop, vcrPlay, vcrRewind, vcrStep, closeButt;
  public JCheckBox vcrVerbose;
  public JTextField vcrSimTime, vcrStopTime;
  private InputStream outInpStr, errInpStr;
  public JCheckBox saveRepDataCB;
  public JCheckBox printRepReportsCB;
  public JCheckBox searchCB;
  public JDialog searchPopup;
  private JButton searchB;
  private JTextField searchKey;
  public JCheckBox printSummReportsCB;
  public JCheckBox resetSeedCB;
  public JCheckBox analystReportCB;
  public JTextField numRepsTF;
  public JScrollBar bar;
  private JLabel titl;
  public RunnerPanel2(boolean verbose, boolean skipCloseButt)
  {
    this(null,skipCloseButt);
    vcrVerbose.setSelected(verbose);
  }

  public RunnerPanel2(String title, boolean skipCloseButt)
  {
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    if(title != null) {
      titl = new JLabel(title);
      titl.setHorizontalAlignment(JLabel.CENTER);
      add(titl,BorderLayout.NORTH);
    }
    JSplitPane leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    JSplitPane leftSplit;
    JSplitPane rightSplit;

    soutTA = new JTextArea("Assembly output stream:" + lineEnd +
        "----------------------" + lineEnd);
    soutTA.setEditable(true); //false);
    soutTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
    soutTA.setBackground(new Color(0xFB, 0xFB, 0xE5));
    soutTA.setRows(100);
    jsp = new JScrollPane(soutTA);
    bar = jsp.getVerticalScrollBar();
    bar.setUnitIncrement(STEPSIZE);
    bar.addAdjustmentListener(new ScrollAdjustmentListener(soutTA));
    bar.addMouseListener(new FileChaserStopper());
    
    serrTA = new JTextArea("Assembly error stream:" + lineEnd +
        "---------------------" + lineEnd);
    serrTA.setForeground(Color.red);
    serrTA.setEditable(true); //false);
    serrTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
    serrTA.setBackground(new Color(0xFB, 0xFB, 0xE5));
    JScrollPane jspErr = new JScrollPane(serrTA);

    rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, jsp,jspErr);

    JComponent vcrPanel = makeVCRPanel(skipCloseButt);

    leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,false,new JScrollPane(vcrPanel),
        new JLabel(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/NPS-3clr-PMS-vrt-type.png"))));

    leftRightSplit.setLeftComponent(leftSplit);
    leftRightSplit.setRightComponent(rightSplit);
    leftRightSplit.setDividerLocation(240);
    leftSplit.setDividerLocation(255);
    rightSplit.setDividerLocation(350);

    add(leftRightSplit,BorderLayout.CENTER);
  }

  private JPanel makeVCRPanel(boolean skipCloseButt)
  {
    JPanel flowPan = new JPanel(new FlowLayout(FlowLayout.LEFT));

    JLabel vcrSimTimeLab = new JLabel("Sim start time:");
    // TODO:  is this start time or current time of sim?
    // TODO:  is this used elsewhere, or else can it simply be removed?
    vcrSimTime = new JTextField(10);
    vcrSimTime.setEditable(false);
    Vstatics.clampSize(vcrSimTime, vcrSimTime, vcrSimTime);
    JPanel labTF = new JPanel();
    labTF.setLayout(new BoxLayout(labTF,BoxLayout.X_AXIS));
    labTF.add(vcrSimTimeLab);
    labTF.add(vcrSimTime);
    labTF.add(Box.createHorizontalStrut(10));
    flowPan.add(labTF);

    JLabel vcrStopTimeLabel = new JLabel("Sim stop time:");
    vcrStopTimeLabel.setToolTipText("Stop current replication once simulation stop time reached");
    vcrStopTime = new JTextField(10);
    Vstatics.clampSize(vcrStopTime, vcrStopTime, vcrStopTime);
    labTF = new JPanel();
    labTF.setLayout(new BoxLayout(labTF,BoxLayout.X_AXIS));
    labTF.add(vcrStopTimeLabel);
    labTF.add(vcrStopTime);
    labTF.add(Box.createHorizontalStrut(10));
    flowPan.add(labTF);

    numRepsTF = new JTextField(10);
    Vstatics.clampSize(numRepsTF,numRepsTF,numRepsTF);
    JLabel numRepsLab = new JLabel("# replications:");
    labTF = new JPanel();
    labTF.setLayout(new BoxLayout(labTF,BoxLayout.X_AXIS));
    labTF.add(numRepsLab);
    labTF.add(numRepsTF);
    labTF.add(Box.createHorizontalStrut(10));
    flowPan.add(labTF);

    vcrVerbose = new JCheckBox("Verbose output", false);
    vcrVerbose.setToolTipText("Enable or disable verbose simulation output");
    flowPan.add(vcrVerbose);

    closeButt = new JButton("Close");
    closeButt.setToolTipText("Close this window");
    if(!skipCloseButt) {
      flowPan.add(closeButt);
    }


    saveRepDataCB = new JCheckBox("Save replication data");
    flowPan.add(saveRepDataCB);
    printRepReportsCB = new JCheckBox("Print replication reports");
    flowPan.add(printRepReportsCB);
    printSummReportsCB = new JCheckBox("Print summary reports       ");
    flowPan.add(printSummReportsCB);
    analystReportCB = new JCheckBox("Enable Analyst Reports");
    flowPan.add(analystReportCB);
    resetSeedCB = new JCheckBox("Reset seed each rerun");
    flowPan.add(resetSeedCB);
    searchB = new JButton("Search...");
    searchKey = new JTextField();
    flowPan.add(searchB);
    searchB.addActionListener(
           new Searcher(this)
    
    );

    JPanel buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan,BoxLayout.X_AXIS));

    vcrStop = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/Stop24.gif")));
    vcrStop.setToolTipText("Stop the simulation run");
    vcrStop.setEnabled(false);
    vcrStop.setBorder(BorderFactory.createEtchedBorder());
    vcrStop.setText(null);
    //vcrStop.addActionListener(new FileChaserStopper());
    buttPan.add(vcrStop);

    vcrRewind = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/Rewind24.gif")));
    vcrRewind.setToolTipText("Reset the simulation run");
    vcrRewind.setEnabled(false);
    vcrRewind.setBorder(BorderFactory.createEtchedBorder());
    vcrRewind.setText(null);
    if(!skipCloseButt)
      buttPan.add(vcrRewind);

    vcrPlay = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/Play24.gif")));
    vcrPlay.setToolTipText("Begin or resume the simulation run");
    if(skipCloseButt)
      vcrPlay.setToolTipText("Begin the simulation run");
    vcrPlay.setBorder(BorderFactory.createEtchedBorder());
    vcrPlay.setText(null);
    buttPan.add(vcrPlay);

    vcrStep = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/StepForward24.gif")));
    vcrStep.setToolTipText("Step the simulation");
    vcrStep.setBorder(BorderFactory.createEtchedBorder());
    vcrStep.setText(null);
    if(!skipCloseButt)
      buttPan.add(vcrStep);

    buttPan.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    flowPan.setAlignmentX(JComponent.LEFT_ALIGNMENT);

    flowPan.setPreferredSize(new Dimension(vcrPlay.getPreferredSize()));

    flowPan.add(buttPan);
    return flowPan;
  }

 
  public void setFileChannel(FileChannel fc) {
      this.fc = fc;
  }
  
  // launch a thread to process dumps to textarea
  // and one to feed it until interrupted
  // by user scrolling action, or stop button
  // TBD: eventually temp filesystem space is coated in
  // dumps, this thread may wait until the entire 
  // dump is streamed out, which can take a few 
  // seconds. Viskit should remove dump files on exit.
  public void wakeUpTextUpdater(FileInputStream fis) {
      System.runFinalization();
      System.gc();
      fc = fis.getChannel();
      //System.err.println("Wakeup TextUpdater");
      dump = true;
      bq = new LinkedBlockingQueue<ByteBuffer>();
      if (textUpdater != null) {
          textUpdater.cancel(true);
      }
      if (fileChaser != null) {
          fileChaser.cancel(true);
      }
      soutTA.replaceRange("",0,soutTA.getText().length());
      textUpdater = new TextUpdater(soutTA,bq);
      textUpdater.execute();
      fileChaser = new FileChaser(fc,bq);
      fileChaser.execute();
  }
  
 
  
  class FileChaser extends SwingWorker<Void,Void> {
      private BlockingQueue<ByteBuffer> bq;
      private FileChannel fc;
      private long fileSz;
      public FileChaser(FileChannel fc, BlockingQueue<ByteBuffer> bq) {
          this.fc = fc;
          this.bq = bq;
          posn = 0;
          //System.err.println("FileChaser started "+new java.util.Date());
      }
      
      public void stop() {
          //System.err.println("FileChaser stopped "+new java.util.Date());
          try {
              Thread.sleep(100);
          } catch (InterruptedException ex) {
              ex.printStackTrace();
          }
          dump = false;
          vcrPlay.setEnabled(true);
          vcrStop.setEnabled(false);
          System.runFinalization();
          System.gc();
      }
      
      private void update(long pageSize) {
          //System.err.println("FileChaser pageSize: "+pageSize+ " posn: "+ posn + " fc size: "+fileSz);
          if (pageSize > 0)
          try {
              mbb = fc.map(FileChannel.MapMode.READ_ONLY,posn,pageSize);
              mbb.load();
              bq.add(mbb);
          } catch (IOException ex) {
              ;//ex.printStackTrace();
          } else {
              stop();
          }
      } 
      
      public Void doInBackground() {
          //System.err.println(fc +" dump "+dump);
          while (fc != null && fc.isOpen() && dump) {
              fileSz = 0;
              try {
                  fileSz = fc.size();
              } catch (IOException ex) {
                  ex.printStackTrace();
              }
              if (fileSz > 0) {
                  if (posn + PAGESIZE < fileSz) {
                      update(PAGESIZE);
                      posn += PAGESIZE;
                  } else {
                      long ps = fileSz - posn;
                      update(ps);
                      posn += ps;
                  }
              }
              try {
                  Thread.sleep(100); // this 
              } catch (InterruptedException ex) {
                  ex.printStackTrace();
              }
          }
          return null;
      }
  }
  
  class Search extends SwingWorker<Void,Void> {
      String key ="";
      SearchHighlightPainter shp = new SearchHighlightPainter(Color.GRAY);
      public Search(String key) {
          this.key = key;
      }
      protected Void doInBackground() throws Exception {
          search(key);
          return null;
      }
      public void search(String key) {
          String page;
          long pageSize;
          long fileSz = 0;
          int offset = 0;
          fileChaser.stop();
          
          try {
              fileSz = fc.size();
              search = true;
              if (posn == fileSz) posn = 0;
          } catch (IOException ex) {
              ex.printStackTrace();
          }
          
          
          if (search && posn < fileSz) { // change to while if entire file is desired
              
              if (fileSz > 0) {
                  
                  boolean notFound = true;
                  while (notFound && posn < fileSz) {
                      if (posn + PAGESIZE < fileSz) {
                          pageSize = PAGESIZE;
                      } else {
                          pageSize = fileSz - posn;
                      }
                      //System.err.println("Search posn: "+posn+" fileSz: "+fileSz+" pageSize: "+pageSize);
                      page = getPage(pageSize);
                      if (page != null) {
                          soutTA.getHighlighter().removeAllHighlights();
                          soutTA.setText(page);
                          offset = page.indexOf(key);
                          if (offset>=0) {
                              try {
                                  int height = soutTA.getFontMetrics(soutTA.getFont()).getHeight();
                                  soutTA.getHighlighter().addHighlight(offset,offset+key.length(),shp);
                                  String pre = page.substring(0,offset);
                                  String[] preLines = pre.split("\\n");
                                  height *= preLines.length;
                                  Rectangle b = soutTA.getBounds();
                                  Rectangle v = soutTA.getVisibleRect();
                                  int maxHeight = b.height - v.height/2;
                                  int minHeight = v.height/2;
                                  if (height > b.height - minHeight){
                                      height = maxHeight;
                                  } else if ( height < minHeight) {
                                      height = minHeight;
                                  }
                                  b.setSize(v.getSize());
                                  b.setLocation(0,height - minHeight);
                                  soutTA.scrollRectToVisible(b);
                                  soutTA.getFontMetrics(soutTA.getFont()).getHeight();
                              } catch (BadLocationException ex) {
                                  ex.printStackTrace();
                              }
                              notFound = false;
                              posn += offset+key.length();
                          } else {
                              posn += pageSize;
                          }
                          if (posn >= fileSz) {
                              int result = JOptionPane.showConfirmDialog(null,"Continue from beginning?","End of output",JOptionPane.OK_CANCEL_OPTION);
                              if (result == JOptionPane.OK_OPTION) {
                                  notFound = true;
                                  posn = 0; // wrap around
                              } else {
                                  notFound = false;
                              }
                          }
                      }
                  }
                  
                  
              }
          }
          try {
              Thread.sleep(10); // long enough to let the scrollbar bob
          } catch (InterruptedException ex) {
              ex.printStackTrace();
          }
          search = false; // unblock scrollbar
      }
      
      class SearchHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
          public SearchHighlightPainter(Color color) {
              super(color);
          }
      }
  }
  
  private String getPage(long pageSize) {
      int mark = -1;
      String page = null;
      byte[] chbuf = new byte[(int)PAGESIZE];
      
      MappedByteBuffer bb;
      try {
          //fc = fc.position(posn);
          bb = fc.map(FileChannel.MapMode.READ_ONLY, posn, pageSize);
          bb.load();
          if (bb.capacity() > 0) {
              // check if cliped on bounds, this condition should only
              // happen once at the end, which probably is not aligned
              // with PAGESIZE
              if (bb.capacity() < PAGESIZE) {
                  byte[] tmpChbuf = new byte[bb.capacity()]; // same as pageSize
                  bb.get(tmpChbuf);
                  page = new String(tmpChbuf,"UTF8");
              } else {
                  bb.get(chbuf);
                  page = new String(chbuf,"UTF8");
              }
          }
          ///System.err.println(page);
          return page;
      } catch (IOException ex) {
          ex.printStackTrace();
      }
      
      return null;
  }
  
  public class Searcher implements ActionListener {
      RunnerPanel2 runner;
      public Searcher(RunnerPanel2 runner) {
          this.runner = runner;
      }
      public void actionPerformed(ActionEvent e) {
          Object[] msg = {
              "Enter search key...", searchKey
          };
          int result = JOptionPane.showConfirmDialog(searchB,
                  msg, "Enter search key...",
                  JOptionPane.OK_CANCEL_OPTION,
                  JOptionPane.PLAIN_MESSAGE);
          while(result == JOptionPane.OK_OPTION) {
              String key = searchKey.getText();
              if(!key.equals("")) {
                  Search searcher = new Search(key);
                  runner.search = true;
                  searcher.execute();
                  try { // to give repaint some time b4 popup
                      Thread.sleep(50);
                  } catch (InterruptedException ex) {
                      ex.printStackTrace();
                  }
                  result = JOptionPane.showConfirmDialog(searchB,
                          msg, "Enter search key...",
                          JOptionPane.OK_CANCEL_OPTION,
                          JOptionPane.PLAIN_MESSAGE);
              } else {
                  runner.search = false;// nothing entered
              }
          }
          runner.search = false;// cancelled
          
      }
  }

  class FileChaserStopper implements MouseListener,ActionListener {
        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            fileChaser.stop();
            bar.removeMouseListener(this);
            bar.addMouseListener(new MousePressListener());
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void actionPerformed(ActionEvent e) {
            fileChaser.stop();
        }
      
  }
  
  class MousePressListener implements MouseListener {
        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            bar.getModel().setValueIsAdjusting(true);
        }

        public void mouseReleased(MouseEvent e) {
            bar.getModel().setValueIsAdjusting(false);
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }
      
  }
  

  class ScrollAdjustmentListener implements AdjustmentListener {
      public JTextArea ta;
      
      int line = 0;
      // the following are for debugging
      long lastPosn = -1;
      long lastLastPosn = -2;
      int lastLine = 0;
      int lastLastLine = 0;
      
      public ScrollAdjustmentListener(JTextArea ta) {
          this.ta = ta;
      }
      
      public void adjustmentValueChanged(AdjustmentEvent e) {
          int type = e.getAdjustmentType();
          long fcSize = 0;
          if(!(dump|search))try {             
              fcSize = fc.size();

              // the scroll events will always be TRACK, if there are two consecutive TRACKs with the
              // same value, the event was a BLOCK so shift up or down a PAGESIZE depending on
              // the lastLastLine value.
              
              if (type == AdjustmentEvent.TRACK) {
                  lastLastLine = lastLine;
                  lastLine = line;
                  line = e.getValue();
                  line = bar.getValue();
                      long pageSize = PAGESIZE;
                      long tmpPosn = posn;
                      //System.err.println("line: "+line + " lastLine: " + lastLine + " lastLastLine: "+lastLastLine+" max: "+bar.getMaximum()+" min: "+bar.getMinimum()+" ext: "+bar.getModel());
                      if (line == bar.getModel().getMaximum() - bar.getModel().getExtent()) {
                          if (posn > fcSize - PAGESIZE) {
                              pageSize = fcSize - posn;   
                          } else {
                              posn += PAGESIZE;
                          }
                          //
                          // lastLine at 0 means scroll bobbed down durning update and got second event
                          // autogenerated, skip it
                          if (posn < fcSize && !(lastLine == 0)) { 
                            if ( pageSize>0 ) {
                                update(pageSize);
                                bar.setValue(line-1);
                            }
                          } else { 
                              posn = tmpPosn;
                          } 
                          
                          if (lastLine == 0) {
                            if (posn != 0)  
                              bar.setValue(1);
                            else 
                              bar.setValue(0);
                          }
                          
                      }
                      if (line == 0 && posn !=0) {
                          posn -= PAGESIZE;
                          if (posn < 0) {
                              posn = 0;
                              bar.setValue(0);
                              update(PAGESIZE);
                          } else {
                              if (posn < fcSize) {
                                  //System.err.println("Going Up "+(line-bar.getMinimum()));
                                  update(PAGESIZE);                                 
                                  bar.setValue(1);
                              } else {
                                  posn = lastPosn;
                              }
                          }
                      }
                      lastPosn = posn;
                      lastLastPosn = lastPosn; // for debugging
                      //System.err.println("posn :"+posn+" lastPosn: "+lastPosn+" lastLastPosn: "+lastLastPosn+" fcSize: "+fcSize);
                  /*    
                  } else {
                      System.err.println("mbb was null");
                  }*/
              } else {
                  System.err.println(e);
              }
              
          } catch (IOException ioe) {
              // remove verbose after testing, file just not ready
              ioe.printStackTrace();
              ;
          } catch (Exception ex) {
              // remove verbose after testing, file just not ready
              ex.printStackTrace();
          }
      
      }

      private void update(long pageSize) {
          try {
              mbb = fc.map(FileChannel.MapMode.READ_ONLY,posn,pageSize);
              mbb.load();
              //System.err.println("update pageSize: "+pageSize+" at posn: "+posn+" with mbb: "+mbb);
              bq.clear();
              bq.add(mbb);
          } catch (IOException ex) {
              ;//ex.printStackTrace();
          }
      } 
        
  }

  class TextUpdater extends SwingWorker<Void,ByteBuffer> {
      JTextArea ta;
      BlockingQueue<ByteBuffer> bq;
      String page = "";
      byte[] chbuf = new byte[(int)PAGESIZE];
      public TextUpdater(JTextArea ta, BlockingQueue<ByteBuffer> bq) {
          this.ta = ta;
          this.bq = bq;
      }
      
      protected Void doInBackground() throws Exception {
          ByteBuffer cbuf;
          while ((cbuf = bq.take()) != null) {
              publish(cbuf);
          }
          return null;
      }
      
      public void process(java.util.List<ByteBuffer> cbufs) {
          for (ByteBuffer bb:cbufs) {
                try {
                    int lastValue = bar.getModel().getValue();
                    if (bb.capacity() > 0) {
                        // check if cliped on bounds, this condition should only
                        // happen once at the end, which probably is not aligned
                        // with PAGESIZE
                        if (bb.capacity() < PAGESIZE) {
                            byte[] tmpChbuf = new byte[bb.capacity()];
                            bb.get(tmpChbuf);
                            page = new String(tmpChbuf,"UTF8");
                            ta.append(page);
                        } else {
                            bb.get(chbuf);
                            page = new String(chbuf,"UTF8");
                            ta.setText(page);
                        }
                    }
                    
                    bar.getModel().setValue(lastValue);
                } catch (Exception e) {e.printStackTrace();} // could? happen first time; remv verbose ltr
                
          }
          cbufs.clear();
      }
  }
}
