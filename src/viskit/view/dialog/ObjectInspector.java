package viskit.view.dialog;

import edu.nps.util.LogUtilities;
import java.awt.Point;
import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.apache.log4j.Logger;
import viskit.model.ViskitInstantiator;
import viskit.view.InstantiationPanel;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jun 16, 2004
 * Time: 3:27:42 PM
 *
 * @version $Id$
 */
public class ObjectInspector extends JDialog
{
    static final Logger LOG = LogUtilities.getLogger(ObjectInspector.class);
	
  public boolean modified = false;
  private final JButton     okButton = new JButton("Apply changes");
  private final JButton cancelButton = new JButton("Cancel");
  private final JPanel  contentPanel = new JPanel();
  private final JPanel   buttonPanel = new JPanel();
  private final JDialog parent;
  private final Point newLocation = new Point();
  private InstantiationPanel instantiationPanel;
  private EnableApplyButtonListener enableApplyButtonListener;

  public ObjectInspector(JDialog parent)
  {
    super(parent,"Object Inspector",true);
	this.parent = parent;
	
	initialize ();
  }
  
  private void initialize ()
  {
    contentPanel.setLayout(new BoxLayout(contentPanel,BoxLayout.Y_AXIS));
    contentPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    setContentPane(contentPanel);

    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    okButton.setEnabled(false);
    buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);

    // attach listeners
    enableApplyButtonListener = new EnableApplyButtonListener();
    cancelButton.addActionListener(new CancelButtonListener());
        okButton.addActionListener(new ApplyButtonListener());
  }

  public void setType(String typeName)
  {
    contentPanel.removeAll();

    instantiationPanel = new InstantiationPanel(this,enableApplyButtonListener,false,true);  // allow type editing
    instantiationPanel.setBorder(null);

    contentPanel.add(instantiationPanel);
    //contentP.add(Box.createVerticalGlue());
    contentPanel.add(Box.createVerticalStrut(5));
    contentPanel.add(buttonPanel);

    pack();     // do this prior to next

//    setLocationRelativeTo(getParent());
    this.setSize(parent.getWidth(), this.getHeight()); // same width as parent
	newLocation.x = parent.getLocation().x - ((this.getWidth() - parent.getWidth()) / 2); // shift left, center sub-panel
	newLocation.y = parent.getLocation().y + (parent.getHeight()); // shift below parent panel
	this.setLocation(newLocation);
  }

  public void setData(ViskitInstantiator viskitInstantiator) throws ClassNotFoundException
  {
    instantiationPanel.setData(viskitInstantiator);
    pack();
  }

  public ViskitInstantiator getData()
  {
    return instantiationPanel.getData();
  }

  class CancelButtonListener implements ActionListener
  {
    @Override
    public void actionPerformed(ActionEvent event)
    {
      modified = false;    // for the caller
      dispose();
    }
  }

  class ApplyButtonListener implements ActionListener
  {
    @Override
    public void actionPerformed(ActionEvent event)
    {
      dispose();
    }
  }

  class EnableApplyButtonListener implements CaretListener, ActionListener
  {
    @Override
    public void caretUpdate(CaretEvent event)
    {
      modified = true;
      okButton.setEnabled(true);
      getRootPane().setDefaultButton(okButton);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
//      ObjectInspector.this.pack();             // fix for buttons disappearing on bottom // TODO check
      caretUpdate(null);
    }
  }

}
