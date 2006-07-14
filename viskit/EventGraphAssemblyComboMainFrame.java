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
 * @since Sep 22, 2005
 * @since 3:25:11 PM
 */

package viskit;

import edu.nps.util.SysExitHandler;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import viskit.doe.DoeMain;
import viskit.doe.DoeMainFrame;
import viskit.doe.FileHandler;
import viskit.doe.JobLauncherTab;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

public class EventGraphAssemblyComboMainFrame extends JFrame
{
  private JTabbedPane tabbedPane;
  private JTabbedPane runTabbedPane;

  EventGraphViewFrame egFrame;
  AssemblyViewFrame asyFrame;
  InternalAssemblyRunner asyRunComponent;
  JobLauncherTab runGridComponent;

  Action myQuitAction;
  private DoeMain doeMain;
  /** The initial assembly to load. */
  private String initialFile;

  private final int TAB0_EGEDITOR_IDX = 0;
  private final int TAB0_ASSYEDITOR_IDX = 1;
  private final int TAB0_ASSYRUN_SUBTABS_IDX = 2;
  private final int TAB0_ANAL_REPORT_IDX = 3;

  private final int TAB0_COUNT = TAB0_ANAL_REPORT_IDX +1;

  private final int TAB1_LOCALRUN_IDX = 0;
  private final int TAB1_DOE_IDX = 1;
  private final int TAB1_CLUSTERUN_IDX = 2;


  public EventGraphAssemblyComboMainFrame(String initialFile)
  {
    super("Viskit");

    this.initialFile = initialFile;

    initUI();

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    this.setLocation((d.width - 800) / 2, (d.height - 600) / 2);
    this.setSize(800, 600);

    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new WindowAdapter()
    {
      public void windowClosing(WindowEvent e)
      {
        myQuitAction.actionPerformed(null);
      }
    });
    ImageIcon icon = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/ViskitSplash2.png"));
    this.setIconImage(icon.getImage());

  }

  ArrayList menus = new ArrayList();

  private void initUI()
  {
    VGlobals.instance().setAssemblyQuitHandler(null);
    VGlobals.instance().setEventGraphQuitHandler(null);
    JMenu helpMenu;
    JMenuBar menuBar;

    tabbedPane = new JTabbedPane();
    ChangeListener tabChangeListener = new myTabChangeListener();

    myQuitAction = new QuitAction("Quit");

    // Tabbed event graph editor
    egFrame = VGlobals.instance().initEventGraphViewFrame(true);
    tabbedPane.add(egFrame.getContent(),TAB0_EGEDITOR_IDX);
    tabbedPane.setTitleAt(TAB0_EGEDITOR_IDX,"Event Graph Editor");
    menuBar = egFrame.getMenus();
    menus.add(menuBar);
    doCommonHelp(menuBar);
    jamSettingsHandler(menuBar);
    egFrame.setTitleListener(myTitleListener,TAB0_EGEDITOR_IDX);
    setJMenuBar(menuBar);
    jamQuitHandler(egFrame.getQuitMenuItem(),myQuitAction,egFrame.getMenus());

    // Assembly editor
    asyFrame = VGlobals.instance().initAssemblyViewFrame(true);
    tabbedPane.add(asyFrame.getContent(),TAB0_ASSYEDITOR_IDX);
    tabbedPane.setTitleAt(TAB0_ASSYEDITOR_IDX,"Assembly Editor");
    menuBar = asyFrame.getMenus();
    menus.add(menuBar);
    doCommonHelp(menuBar);
    jamSettingsHandler(menuBar);
    asyFrame.setTitleListener(myTitleListener,TAB0_ASSYEDITOR_IDX);
    jamQuitHandler(asyFrame.getQuitMenuItem(),myQuitAction,asyFrame.getMenus());

    runTabbedPane = new JTabbedPane();
    tabbedPane.add(runTabbedPane,TAB0_ASSYRUN_SUBTABS_IDX);
    tabbedPane.setTitleAt(TAB0_ASSYRUN_SUBTABS_IDX,"Assembly Run");
    menus.add(null); // placeholder

    // Analyst report
    tabbedPane.add(new JLabel("Analyst report goes here"),TAB0_ANAL_REPORT_IDX);
    tabbedPane.setTitleAt(TAB0_ANAL_REPORT_IDX,"Analyst Report");
    menuBar = new JMenuBar(); //todo implement
    menus.add(menuBar);
    doCommonHelp(menuBar);
    jamSettingsHandler(menuBar);
    //todo blah.setTitleListener(myTitleListener,TAB0_ANAL_REPORT_IDX);
    //todo jamQuitHandler....

    // Assembly runner
    asyRunComponent = new InternalAssemblyRunner();
    runTabbedPane.add(asyRunComponent.getContent(),TAB1_LOCALRUN_IDX);
    runTabbedPane.setTitleAt(TAB1_LOCALRUN_IDX,"Local Run");
    menuBar = asyRunComponent.getMenus();
    menus.add(menuBar);
    doCommonHelp(menuBar);
    jamSettingsHandler(menuBar);
    asyRunComponent.setTitleListener(myTitleListener,TAB0_COUNT+TAB1_LOCALRUN_IDX);
    jamQuitHandler(asyRunComponent.getQuitMenuItem(),myQuitAction,asyRunComponent.getMenus());
    AssemblyController controller = ((AssemblyController)asyFrame.getController());
    controller.setInitialFile(initialFile);
    controller.setAssemblyRunner( new ThisAssemblyRunnerPlug());

    // Design of experiments
    doeMain = DoeMain.main2();
    DoeMainFrame doeFrame = doeMain.getMainFrame();
    runTabbedPane.add(doeFrame.getContent(),TAB1_DOE_IDX);
    runTabbedPane.setTitleAt(TAB1_DOE_IDX,"Design of Experiments");
    runTabbedPane.setIconAt(TAB1_DOE_IDX,new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/grid.png")));
    menuBar = doeMain.getMenus();
    if(menuBar == null){
      menuBar = new JMenuBar();
      menuBar.add(new JMenu("File"));
    }
    menus.add(menuBar);
    doCommonHelp(menuBar);
    doeFrame.setTitleListener(myTitleListener,TAB0_COUNT+TAB1_DOE_IDX);
    jamQuitHandler(doeMain.getQuitMenuItem(),myQuitAction,menuBar);

    // Grid run panel
    runGridComponent = new JobLauncherTab(doeMain.getController(),null,null,this);
    doeFrame.getController().setJobLauncher(runGridComponent);
    runTabbedPane.add(runGridComponent.getContent(),TAB1_CLUSTERUN_IDX);
    runTabbedPane.setTitleAt(TAB1_CLUSTERUN_IDX,"LaunchClusterJob");
    runTabbedPane.setIconAt(TAB1_CLUSTERUN_IDX,new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/grid.png")));
    menuBar = new JMenuBar();
    menuBar.add(new JMenu("File"));
    jamQuitHandler(null,myQuitAction,menuBar);
    menus.add(menuBar);
    doCommonHelp(menuBar);
    runGridComponent.setTitleListener(myTitleListener,TAB0_COUNT+TAB1_CLUSTERUN_IDX);

    // Now setup the assembly file change listeners
    ViskitAssemblyController asyCntlr = (ViskitAssemblyController)asyFrame.getController();

    asyCntlr.addAssemblyFileListener(asyCntlr.getAssemblyChangeListener());
    //asyCntlr.addAssemblyFileListener(asyFrame);
    asyCntlr.addAssemblyFileListener(asyRunComponent);
    asyCntlr.addAssemblyFileListener(doeFrame.getController().getOpenAssemblyListener());
    asyCntlr.addAssemblyFileListener(runGridComponent);

    // Now setup the open-event graph listener(s)
    ViskitController cntl = (ViskitController)egFrame.getController();
    cntl.addOpenEventGraphListener(asyCntlr.getOpenEventGraphListener());
    cntl.addOpenEventGraphListener(doeFrame.getController().getOpenEventGraphListener());
    // Start the controllers
    ((ViskitController)egFrame.getController()).begin();

    runLater(3000,new Runnable(){
      public void run(){((AssemblyController)asyFrame.getController()).begin();}
    });

    // Swing:
    getContentPane().add(tabbedPane);

    tabbedPane.addChangeListener(tabChangeListener);
    runTabbedPane.addChangeListener(tabChangeListener);
  }
  private void runLater(final long ms, final Runnable runr)
  {
    if (viskit.Vstatics.debug) System.out.println("Run later: " + runr);
    Thread t = new Thread(new Runnable()
    {
      public void run()
      {
        try {Thread.sleep(ms);}catch (InterruptedException e) {}
        SwingUtilities.invokeLater(runr);
      }
    },"runLater");
    t.setPriority(Thread.NORM_PRIORITY);
    t.start();
  }

  class myTabChangeListener implements ChangeListener
  {
    public void stateChanged(ChangeEvent e)
    {
      int i = tabbedPane.getSelectedIndex();
      if(i == TAB0_ASSYRUN_SUBTABS_IDX)
        i = TAB0_COUNT+runTabbedPane.getSelectedIndex();

      getJMenuBar().remove(hmen);
      JMenuBar newMB = (JMenuBar)menus.get(i);
      newMB.add(hmen);
      setJMenuBar(newMB);
      setTitle((String)titles[i]);
    }
  }

   private JMenu hmen;
  /**
   * Stick the first Help menu we see into all the following ones.
   * @param mb
   */
  private void doCommonHelp(JMenuBar mb)
  {
    for(int i=0;i<mb.getMenuCount();i++) {
      JMenu men = mb.getMenu(i);
      if(men.getText().equalsIgnoreCase("Help")) {
        if(hmen == null)
          hmen = men;
        else
          mb.remove(i);
        return;
      }
    }
  }

  private void jamSettingsHandler(JMenuBar mb)
  {
    for(int i=0;i<mb.getMenuCount();i++) {
      JMenu men = mb.getMenu(i);
      if(men.getText().equalsIgnoreCase("File")) {
        for(int j=0;j<men.getMenuComponentCount();j++) {
          Component c = men.getMenuComponent(j);
          if(c instanceof JMenuItem) {
            JMenuItem jmi = (JMenuItem)c;
            if(jmi.getText().equalsIgnoreCase("settings"))
            {
              jmi.addActionListener(mySettingsHandler);
              return;
            }
          }
        }
      }
    }
  }
  ActionListener mySettingsHandler = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      SettingsDialog.showDialog(EventGraphAssemblyComboMainFrame.this);
    }
  };
  private void jamQuitHandler(JMenuItem mi, Action qa, JMenuBar mb)
  {
    if(mi==null) {
      JMenu m = mb.getMenu(0); // first menu
      if(m == null) {
        m = new JMenu("File");
        mb.add(m);
      }
      m.addSeparator();
      mi = new JMenuItem("Quit");
      m.add(mi);
    }

    ActionListener[] al = mi.getActionListeners();
    for(int i=0; i<al.length;i++)
      mi.removeActionListener(al[i]);

    mi.setAction(qa);
  }
  class QuitAction extends AbstractAction
  {
    public QuitAction(String s)
    {
      super(s);
    }

    public void actionPerformed(ActionEvent e)
    {
      SysExitHandler defaultHandler = VGlobals.instance().getSysExitHandler();
      VGlobals.instance().setSysExitHandler(nullSysExitHandler);

      tabbedPane.setSelectedIndex(TAB0_EGEDITOR_IDX);
      if(((Controller)egFrame.getController()).preQuit()) {
        tabbedPane.setSelectedIndex(TAB0_ASSYEDITOR_IDX);
        if(((ViskitAssemblyController)asyFrame.getController()).preQuit()) {
          tabbedPane.setSelectedIndex(TAB0_ASSYRUN_SUBTABS_IDX);
          runTabbedPane.setSelectedIndex(TAB1_DOE_IDX);
          if(doeMain.getController().preQuit()) {
            //todo other preQuits here if needed

            VGlobals.instance().setSysExitHandler(defaultHandler);    // reset default handler

            ((ViskitController)egFrame.getController()).postQuit();
            ((AssemblyController)asyFrame.getController()).postQuit();
            doeMain.getController().postQuit();
            //todo other postQuits here if needed

            thisClassCleanup();
            if (viskit.Vstatics.debug) System.out.println("in actionPerformed of exit");
            VGlobals.instance().sysExit(0);  // quit application
          }
        }
      }
      // Here if somebody cancelled.
      VGlobals.instance().setSysExitHandler(defaultHandler);
      return;
    }
  }

  private void thisClassCleanup()
  {
    // Lot of hoops to pretty-fy the config xml file
    String uConfig = VGlobals.instance().getUserConfigFile();
    Document doc;
    File f = new File(uConfig);
    try {
      doc = FileHandler.unmarshallJdom(f);
      Format form = Format.getPrettyFormat();
      XMLOutputter xout = new XMLOutputter(form);
      xout.output(doc,new FileWriter(f));
    }
    catch (Exception e) {
      System.out.println("Bad jdom op: "+e.getMessage());
      return;
    }
  }

  private SysExitHandler nullSysExitHandler = new SysExitHandler()
  {
    public void doSysExit(int status)
    {
      // do nothing
    }
  };

  class ThisAssemblyRunnerPlug implements AssemblyRunnerPlug
  {
    public void exec(String[] execStrings, int runnerClassIndex)
    {
      /** The default version of this does a RuntimeExex("java"....) to spawn a new
       * VM.  We want to run the assembly in a new VM, but not the GUI.
       */
      tabbedPane.setSelectedIndex(TAB0_ASSYRUN_SUBTABS_IDX);
      runTabbedPane.setSelectedIndex(TAB1_LOCALRUN_IDX);
      asyRunComponent.initParams(execStrings,runnerClassIndex);
    }
  }

  String[] titles = new String[]{"","","","","","","","","",""};
  TitleListener myTitleListener = new myTitleListener();
  class myTitleListener implements TitleListener
  {
    public void setTitle(String title, int key)
    {
      titles[key] = title;
      int tabIdx = tabbedPane.getSelectedIndex();
      if(tabIdx == TAB0_ASSYRUN_SUBTABS_IDX)
        tabIdx = TAB0_COUNT+runTabbedPane.getSelectedIndex();

      if(tabIdx == key)
        EventGraphAssemblyComboMainFrame.this.setTitle(title);
    }
  }
}