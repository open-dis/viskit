package viskit;

import viskit.mvc.mvcAbstractJFrameView;
import viskit.model.Model;

import javax.swing.*;
import java.awt.*;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 10, 2004
 * Time: 2:07:37 PM
 */
public class AssemblyViewFrame extends mvcAbstractJFrameView implements ViskitAssemblyView
{
  private Model model;
  private Controller controller;
  private JSplitPane jsp;
  private Color background = new Color(0xFB,0xFB,0xE5);
  public AssemblyViewFrame(Model model, Controller controller)
  {
    super("Simkit Assembly Editor");
    this.model = model;
    this.controller = controller;

    Container cont = getContentPane();

    buildMenus();
    JPanel canvas = buildCanvas();
    JComponent trees = buildTreePanels();
    buildToolBar();
    JScrollPane leftsp = new JScrollPane(trees);
    leftsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    //leftsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,trees,new JScrollPane(canvas));
    jsp.setOneTouchExpandable(true);
    trees.setMinimumSize(new Dimension(20,20));
    canvas.setMinimumSize(new Dimension(20,20));
    //jsp.setDividerLocation(0.5d);
    cont.add(jsp);

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    setLocation(((d.width - 800) / 2)+30, ((d.height - 600) / 2)+30);
    setSize(800, 600);

  }
  private void buildMenus()
  {

  }
  private JPanel buildCanvas()
  {
    return new JPanel();
  }
  private JSplitPane panJsp;

  private JComponent buildTreePanels()
  {
    LegosTree lTree= new LegosTree();
    LegosPanel lPan = new LegosPanel(lTree);

    PropChangeListenersList pcList = new PropChangeListenersList();
    PropChangeListenersPanel pcPan = new PropChangeListenersPanel(pcList);

    lTree.setBackground(background);
    pcList.setBackground(background);

      panJsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,lPan,pcPan);
/*
  JScrollPane tsp = new JScrollPane(lPan,JScrollPane.VERTICAL_SCROLLBAR_NEVER,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
  JScrollPane psp = new JScrollPane(pcPan,JScrollPane.VERTICAL_SCROLLBAR_NEVER,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
  panJsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,tsp,psp);
  tsp.setMinimumSize(new Dimension(20,20));
  psp.setMinimumSize(new Dimension(20,20));
*/

      panJsp.setBorder(null);
      panJsp.setOneTouchExpandable(true);
      pcPan.setMinimumSize(new Dimension(20,80));
      lPan.setMinimumSize(new Dimension(20,80));

    return panJsp;
  }
  private void buildToolBar()
  {

  }
  private boolean firstShown = false;

  public void setVisible(boolean b)
  {
    super.setVisible(b);
    if(firstShown == false) {
      firstShown = true;
      jsp.setDividerLocation(225);
      panJsp.setDividerLocation(0.5d);
    }
  }

}
