package viskit;

import viskit.model.VInstantiator;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jun 8, 2004
 * Time: 8:31:41 AM
 */

public class InstantiationPanel extends JPanel implements ActionListener, CaretListener
{
  private JLabel typeLab, methodLab;
  private JTextField typeTF;
  private JComboBox methodCB;

  private static final int FF=0,CONSTR=1,FACT=2,ARR=10;

  private JPanel instPane;
  private CardLayout instPaneLayMgr;
  private FFPanel ffPan;
  private ConstrPanel conPan;
  private FactoryPanel factPan;

  private ActionListener modifiedListener;
  private JDialog packMe;
  boolean constructorOnly = false;

/*
  public InstantiationPanel(ActionListener changedListener)
  {
    this(null,changedListener);
  }
*/
  public InstantiationPanel(JDialog ownerDialog,ActionListener changedListener)
  {
    this(ownerDialog,changedListener,false);
  }
  public InstantiationPanel(JDialog ownerDialog, ActionListener changedListener, boolean onlyConstr)
  {
    this(ownerDialog,changedListener,onlyConstr,false);
  }
  public InstantiationPanel(JDialog ownerDialog, ActionListener changedListener, boolean onlyConstr, boolean typeEditable)
  {
    modifiedListener = changedListener;
    packMe = ownerDialog;
    constructorOnly = onlyConstr;

    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

    JPanel topP = new JPanel(new SpringLayout());
    typeLab = new JLabel("type",JLabel.TRAILING);
    typeTF = new JTextField();
    typeTF.setEditable(typeEditable);
    typeLab.setLabelFor(typeTF);

    methodLab = new JLabel("method",JLabel.TRAILING);

    methodCB = new JComboBox(new String[]{"free form","constructor","factory"});
    //or
    JTextField onlyConstrTF = new JTextField("Constructor");
    onlyConstrTF.setEditable(false);

    topP.add(typeLab);
    topP.add(typeTF);
    topP.add(methodLab);
    if(onlyConstr) {
      methodLab.setLabelFor(onlyConstrTF);
      topP.add(onlyConstrTF);
    }
    else {
      methodLab.setLabelFor(methodCB);
      topP.add(methodCB);
    }
    SpringUtilities.makeCompactGrid(topP,2,2,10,10,5,5);
    add(topP);

    instPane = new JPanel();
    instPaneLayMgr = new CardLayout();
    instPane.setLayout(instPaneLayMgr);

    instPane.setBorder(BorderFactory.createEtchedBorder());
    instPane.setAlignmentX(Box.CENTER_ALIGNMENT);

    ffPan    = new FFPanel(this);
    conPan   = new ConstrPanel(this);
    factPan  = new FactoryPanel(this);

/*
    instPaneLayMgr.addLayoutComponent(ffPan,"ffPan");
    instPaneLayMgr.addLayoutComponent(conPan,"conPan");
    instPaneLayMgr.addLayoutComponent(factPan,"factPan");
    //false advertising
*/
    instPane.add(ffPan,"ffPan");
    instPane.add(conPan,"conPan");
    instPane.add(factPan,"factPan");

    add(Box.createVerticalStrut(5));
    add(instPane);

    methodCB.addActionListener(new ActionListener()
    {
      int lastIdx = 0;
      public void actionPerformed(ActionEvent e)
      {
        if(!typeTF.getText().trim().equals(myVi.getType())) {
          String newType = typeTF.getText().trim();
          // update the panels
          try {
            ffPan.setType(newType);
            conPan.setType(newType);
            factPan.setType(newType);
          }
          catch (ClassNotFoundException e1) {
            JOptionPane.showMessageDialog(InstantiationPanel.this, "Unknown type");
            return;
          }
          ffPan.setData(new VInstantiator.FreeF(newType,""));
          conPan.setData(new VInstantiator.Constr(newType,new Vector()));
          factPan.setData(new VInstantiator.Factory(newType,"","",new Vector()));
        }
        int idx = methodCB.getSelectedIndex();
        if(lastIdx != idx)
          if(modifiedListener != null)
            modifiedListener.actionPerformed(new ActionEvent(methodCB,0,"modified"));
        switch(idx)
        {
        case FF:
            instPaneLayMgr.show(instPane,"ffPan");
            ffPan.value.requestFocus();
            ffPan.value.selectAll();
            break;
        case CONSTR:
            instPaneLayMgr.show(instPane,"conPan");
            break;
        case FACT:
            instPaneLayMgr.show(instPane,"factPan");
            factPan.factClassCB.requestFocus();
            //factPan.factClassCB.selectAll();
            break;
        default:
            System.err.println("bad data Instantiation panel");
        }
      }
    });
  }

  VInstantiator getData()
  {
    switch(methodCB.getSelectedIndex())
    {
    case FF:
      return ffPan.getData();
    case CONSTR:
      return conPan.getData();
    case FACT:
      return factPan.getData();
    default:
      System.err.println("bad data Inst. panel getData()");
      return null;
    }
  }
  VInstantiator myVi;
  public void setData(VInstantiator vi) throws ClassNotFoundException
  {
    myVi = vi.vcopy();
    String typ = vi.getType();
    typeTF.setText(typ);

    // inform all panels of the type of the object
    conPan.setType(typ);
    factPan.setType(typ);
    ffPan.setType(typ);

    if(vi instanceof VInstantiator.Constr) {
      conPan.setData((VInstantiator.Constr)vi);
      methodCB.setSelectedIndex(CONSTR);
    }
    else if(vi instanceof VInstantiator.Factory) {
      factPan.setData((VInstantiator.Factory)vi);
      methodCB.setSelectedIndex(FACT);
    }
    else if(vi instanceof VInstantiator.FreeF) {
      ffPan.setData((VInstantiator.FreeF)vi);
      methodCB.setSelectedIndex(FF);
    }
    else {
      //assert false: "Internal error InstantianPanel.setData()"
      System.err.println("Internal error InstantiationPanel.setData()");
    }

  }
  
  public void actionPerformed(ActionEvent e)
  {
    if(modifiedListener != null)
      modifiedListener.actionPerformed(null);
  }

  public void caretUpdate(CaretEvent e)
  {
    actionPerformed(null);
  }

  /***********************************************************************/
  class FFPanel extends JPanel implements CaretListener
  {
    private JTextField value;
    private InstantiationPanel ip;
    public FFPanel(InstantiationPanel ip)
    {
      this.ip = ip;
      setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
      setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

      value = new JTextField("");
      value.addCaretListener(this);
      value.setAlignmentX(Box.CENTER_ALIGNMENT);
      Vstatics.clampHeight(value);

      add(value);
      add(Box.createVerticalGlue());
    }
    public void setData(VInstantiator.FreeF viff)
    {
      if(viff == null)
        return;
      value.setText(viff.getValue());
    }
    String typ;
    public void setType(String typ) throws ClassNotFoundException
    {
      this.typ = typ;
      if(Vstatics.classForName(typ) == null)  // just to check exception
        throw new ClassNotFoundException(typ);
    }
    public VInstantiator getData()
    {
      return new VInstantiator.FreeF(typ,value.getText().trim());
    }

    public void caretUpdate(CaretEvent e)
    {
      if(ip.modifiedListener != null)
        ip.modifiedListener.actionPerformed(new ActionEvent(this,0,"Textfield touched"));
    }
  }

  /***********************************************************************/
  class ConstrPanel extends JPanel implements ActionListener,CaretListener
  {
    private Class clazz;
    private JTabbedPane tp;
    private Constructor[] construct;
    private ConstructorPanel[] constructorPanels;

    private String noParamString = "(no parameters)";
    private ImageIcon checkMark;
    private InstantiationPanel ip;

    public ConstrPanel(InstantiationPanel ip)
    {
      setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
      this.ip = ip;
      tp = new JTabbedPane();
      checkMark = new ImageIcon(ClassLoader.getSystemResource("viskit/images/checkMark.png"));

      //setup(className);         don't init in constructor
    }
    String typ;
    public void setType(String clName) throws ClassNotFoundException
    {
      typ = clName;
      removeAll();
      tp.removeAll();
      //modifiedListener = changedListener;

      clazz = Vstatics.classForName(clName);
      if(clazz == null)
        throw new ClassNotFoundException(clName);
      construct = clazz.getConstructors();
      constructorPanels = new ConstructorPanel[construct.length];

      if (construct == null || construct.length <= 0) {
        // here if there is no way to directly build an object of this class.
        tp.addTab("Constructor 0", null, new JLabel("Abstract class of some kind.  Use free-form construction."));
      }
      else {
        for (int i = 0; i < construct.length; ++i) {
          constructorPanels[i] = new ConstructorPanel(this,construct.length != 1,this,packMe);
          constructorPanels[i].setData(buildDummyInstantiators(construct[i]));
          String sign = ConstructorPanel.getSignature(construct[i].getParameterTypes());

          if (construct[i].getParameterTypes().length == 0)
            sign = noParamString;

          tp.addTab("Constructor " + i, null, constructorPanels[i], sign);
        }
      }
      add(tp);

      actionPerformed(null);    // set icon for initially selected pane
    }
    private List buildDummyInstantiators(Constructor con)
    {
      Vector v = new Vector();
      Class[] cs = con.getParameterTypes();
      for(int i=0;i<cs.length;i++) {
        if(cs[i].isArray()) {
          VInstantiator.Array va = new VInstantiator.Array(Vstatics.convertClassName(cs[i].getName()),
                                                           new Vector());
          v.add(va);
        }
        else {
          VInstantiator.FreeF vff = new VInstantiator.FreeF(Vstatics.convertClassName(cs[i].getName()),
                                                            "");
          v.add(vff);
        }
      }
      return v;
    }

    public void caretUpdate(CaretEvent e)
    {
      ip.caretUpdate(e);
    }

    public void actionPerformed(ActionEvent e)
    {
      int idx = tp.getSelectedIndex();
      if(construct == null ||construct.length <= 0)               // some classes have no constructors
        return;

      for(int i=0;i<tp.getTabCount();i++) {
        if(i == idx) {
          tp.setIconAt(i,checkMark);
          constructorPanels[i].setBorder(BorderFactory.createLineBorder(Color.red));
          constructorPanels[i].setSelected(true);
        }
        else {
          tp.setIconAt(i,null);
          constructorPanels[i].setBorder(null);
          constructorPanels[i].setSelected(false);
        }
      }
      // tell mommy
      ip.actionPerformed(e);
    }

    public void setData(VInstantiator.Constr vi)
    {
      if(vi == null)
        return;
      List args = vi.getArgs();
      for(int i = 0;i<constructorPanels.length;i++) {    //give to everyone...they must cho
        Constructor con = construct[i];
        Class[] params = con.getParameterTypes();
        noMatch:
        {
          for(int j=0;j<params.length;j++) {
            if(j >= args.size())
              break noMatch;
            VInstantiator argVi = (VInstantiator)args.get(j);
            if(! Vstatics.convertClassName(params[j].getName()).equals(argVi.getType())) {
              break noMatch;
            }
          }
          // here if we matched all the arguments to the constructor
          constructorPanels[i].setData(args);
          tp.setSelectedIndex(i);
          actionPerformed(null);
          return;

        } // nomatch
      } // next Constructor

      // If a new node is dragged onto the canvas, there are no constructor arguments specified.
      // Therefore, it's not an error if we don't find a match.

      //assert false: "Internal error ConstrPanel.setData";
      //System.err.println("assert false: \"Internal error ConstrPanel.setData\"");
    }

    public VInstantiator getData()
    {
      ConstructorPanel cp = (ConstructorPanel)tp.getSelectedComponent();
      return new VInstantiator.Constr(typ,cp.getData());
    }
  }

  /***********************************************************************/
  class FactoryPanel extends JPanel
  {
    private InstantiationPanel ip;
    private JLabel factClassLab, factMethodLab;
    private JComboBox factClassCB;
    private JTextField factMethodTF;
    private JButton factMethodButt;
    private JPanel topP;
    private ObjListPanel olp;

    public FactoryPanel(InstantiationPanel ip)
    {
      this.ip = ip;
      setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

      topP = new JPanel(new SpringLayout());
      factClassLab = new JLabel("Factory class",JLabel.TRAILING);
      factClassCB = new JComboBox(new Object[]{"simkit.random.RandomVariateFactory"});
      // this is wierd, I want it's height to be the one for a non-editable CB
    //  factClassCB.setEditable(false);
      factClassCB.setEditable(true);
      Vstatics.clampHeight(factClassCB);
      factClassLab.setLabelFor(factClassCB);

      JLabel dummy = new JLabel("");
      JLabel classHelp = new JLabel("(Press return after entering class name)");
      classHelp.setFont(factClassCB.getFont());
      dummy.setLabelFor(classHelp);

      factMethodLab = new JLabel("Class method",JLabel.TRAILING);
      factMethodTF  = new JTextField();
      Vstatics.clampHeight(factMethodTF);
      factMethodLab.setLabelFor(factMethodTF);
        JPanel tinyP = new JPanel();
        tinyP.setLayout(new BoxLayout(tinyP, BoxLayout.X_AXIS));
        tinyP.add(factMethodTF);
        factMethodButt = new JButton("...");
        factMethodButt.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(0,3,0,3)));
        Vstatics.clampSize(factMethodButt, factMethodTF, factMethodButt);
        tinyP.add(factMethodButt);
      topP.add(factClassLab);
      topP.add(factClassCB);
      topP.add(dummy);
      topP.add(classHelp);
      topP.add(factMethodLab);
      topP.add(tinyP);
      SpringUtilities.makeCompactGrid(topP,3,2,5,5,5,5);

      add(topP);

      factClassCB.addActionListener(new MyClassListener());
      MyCaretListener myCarListener = new MyCaretListener();
      //factClassCB.addCaretListener(myCarListener);
      factMethodButt.addActionListener(new MyChangedListener());
      factMethodTF.addCaretListener(myCarListener);
    }
    class MyChangedListener implements ActionListener
    {
      public void actionPerformed(ActionEvent e)
      {
        if(ip.modifiedListener != null)
          ip.modifiedListener.actionPerformed(new ActionEvent(this,0,"Button pressed"));
      }
    }
    class MyCaretListener implements CaretListener
    {
      public void caretUpdate(CaretEvent e)
      {
        if(ip.modifiedListener != null)
          ip.modifiedListener.actionPerformed(new ActionEvent(this,0,"TF edited pressed"));
      }
    }

    boolean noClassAction = false;
    class MyClassListener implements ActionListener
    {
      public void actionPerformed(ActionEvent e)
      {
        if(noClassAction)
          return;
        Class c = null;
        //String cName = factClassCB.getText().trim();
        String cName = factClassCB.getSelectedItem().toString();
        try {
          c = Vstatics.classForName(cName);
          if(c == null)throw new ClassNotFoundException();
        }
        catch (ClassNotFoundException e1) {
          JOptionPane.showMessageDialog(ip,cName + " class not found");
          factClassCB.requestFocus();
          //factClassCB.selectAll();
          return;
        }

        Method[] statMeths = c.getMethods();
        if(statMeths == null || statMeths.length <= 0) {
          JOptionPane.showMessageDialog(ip,cName + " contains no methods");
          factClassCB.requestFocus();
          //factClassCB.selectAll();
          return;
        }
        Vector vn = new Vector();
        HashMap hm = new HashMap();

        for(int i=0;i<statMeths.length;i++) {
          int mods = statMeths[i].getModifiers();
          Class retCl = statMeths[i].getReturnType();
          if(Modifier.isStatic(mods))
            if(retCl == myObjClass){
              String ts = statMeths[i].toString();
              int strt = ts.lastIndexOf('.',ts.indexOf('(')); // go to ( , back to .
              ts = ts.substring(strt+1,ts.length());
              hm.put(ts,statMeths[i]);
              vn.add(ts);
            }
        }
        if(vn.size() <= 0) {
          JOptionPane.showMessageDialog(ip,"<html><center>"+cName + " contains no static methods<br>returning "+typ+".");
          factClassCB.requestFocus();
          //factClassCB.selectAll();
          return;
        }
        String[] ms = new String[0];
        ms =(String[])vn.toArray(ms);
        Object ret = JOptionPane.showInputDialog(packMe,"Choose method","Factory methods",JOptionPane.PLAIN_MESSAGE,null,
              ms,ms[0]);
        if(ret == null) {
          factClassCB.requestFocus();
          //factClassCB.selectAll();
          return;
        }

        Method m = (Method)hm.get(ret);
        factMethodTF.setText(m.getName());
        factMethodTF.setEnabled(true);
        factMethodLab.setEnabled(true);
        factMethodButt.setEnabled(true);
        Class[] pc = m.getParameterTypes();
        Vector vc = new Vector();
        for(int i=0;i<pc.length;i++) {
          if(pc[i].isArray())
            vc.add(new VInstantiator.Array(Vstatics.convertClassName(pc[i].getName()),new ArrayList()));
          else
            vc.add(new VInstantiator.FreeF(Vstatics.convertClassName(pc[i].getName()),""));
        }

        olp = new ObjListPanel(ip);
        olp.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                        "Method arguments",TitledBorder.CENTER,TitledBorder.DEFAULT_POSITION));
        olp.setDialogInfo(packMe,packMe);
        olp.setData(vc,true);
        add(olp);

        add(Box.createVerticalGlue());

       // if(packMe != null)  // gets done in modified handler
       //   packMe.pack();
        if(ip.modifiedListener != null)
          ip.modifiedListener.actionPerformed(new ActionEvent(this,0,"Factory method chosen"));
      }
    }

    String typ;
    Class myObjClass;
    public void setType(String clName) throws ClassNotFoundException
    {
      typ = clName;
      myObjClass = Vstatics.classForName(typ);
      if (myObjClass == null) {
        throw new ClassNotFoundException(typ);
      }
      factMethodLab.setEnabled(false);
      factMethodTF.setEnabled(false);
      factMethodButt.setEnabled(false);
    }

    public void setData(VInstantiator.Factory vi)
    {
      if(vi == null)
        return;

      removeAll();
      //factClassCB.setText(vi.getFactoryClass());
      noClassAction = true;
      factClassCB.setSelectedItem(vi.getFactoryClass());              // this runs action event
      noClassAction = false;
      factMethodTF.setText(vi.getMethod());
      add(topP);

/*
      if(vi.getParams().size() <= 0) {
        JLabel tempLab = new JLabel("no arguments to chosen factory method",JLabel.CENTER);
        add(tempLab);
        add(Box.createVerticalGlue());
        return;
      }
*/
      olp = new ObjListPanel(ip);
      olp.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                      "Method arguments",TitledBorder.CENTER,TitledBorder.DEFAULT_POSITION));
      olp.setDialogInfo(packMe,packMe);

      olp.setData(vi.getParams(),true);
      add(olp);

      add(Box.createVerticalGlue());
      revalidate();
    }

    public VInstantiator getData()
    {
      //String fc = factClassCB.getText();
      String fc = (String)factClassCB.getSelectedItem();if(fc==null)fc="";else fc=fc.trim();
      String m  = factMethodTF.getText();if(m==null)m ="";else m = m.trim();
      List lis;
      if(olp != null)
        lis = olp.getData();
      else
        lis = new Vector();
      return new VInstantiator.Factory(typ,fc,m,lis);
    }
  }
}