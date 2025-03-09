package viskit.mvc;

import javax.swing.JFrame;
import viskit.ViskitUserConfiguration;
import viskit.util.TitleListener;

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
public abstract class MvcAbstractViewFrame extends JFrame implements MvcView, MvcModelListener 
{    

    protected TitleListener titleListener;
    protected int titleKey;
    private MvcModel mvcModel;
    private MvcController mvcController;

    public MvcAbstractViewFrame(String title) 
    {
        super(title);
    }

    public void registerWithModel() 
    {
        ((MvcAbstractModel) mvcModel).addModelListener(this);
    }

    /** Sets the frame title listener and key for this frame
     *
     * @param newTitleListener the title listener to set
     * @param keyValue the key for this frame's title
     */
    public void setTitleListener(TitleListener newTitleListener, int keyValue) 
    {
        titleListener = newTitleListener;
        titleKey = keyValue;
    }

    @Override
    public MvcController getController() {
        return mvcController;
    }

    @Override
    public MvcModel getModel() {
        return mvcModel;
    }

    @Override
    public void setController(MvcController mvcController) {
        this.mvcController = mvcController;
    }

    @Override
    public void setModel(MvcModel mvcModel) {
        this.mvcModel = mvcModel;
        registerWithModel();
    }

    @Override
    public abstract void modelChanged(MvcModelEvent mvcModelEvent);

}
