package viskit.mvc;

import edu.nps.util.LogUtilities;
import javax.swing.JFrame;
import org.apache.log4j.Logger;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Mar 2, 2004
 * Time: 11:33:58 AM
 */

/**
 * From an article at www.jaydeetechnology.co.uk
 */
public abstract class mvcAbstractJFrameView extends JFrame implements mvcView, mvcModelListener
{
    static final Logger LOG = LogUtilities.getLogger(mvcAbstractJFrameView.class);
	
    private mvcModel model;
    private mvcController controller;

    public mvcAbstractJFrameView(String title) 
	{
        super(title);
    }

    public void registerWithModel() 
	{
        ((mvcAbstractModel) model).addModelListener(this);
    }

    @Override
    public mvcController getController() 
	{
        return controller;
    }

    @Override
    public mvcModel getModel() 
	{
        return model;
    }

    @Override
    public void setController(mvcController controller) 
	{
        this.controller = controller;
    }

    @Override
    public void setModel(mvcModel model) 
	{
        this.model = model;
        registerWithModel();
    }

    @Override
    public abstract void modelChanged(mvcModelEvent event);

}
