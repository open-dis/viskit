package viskit.mvc;

import javax.swing.JFrame;
import viskit.ViskitConfigurationStore;
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

    protected TitleListener titlelListener;
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
        titlelListener = newTitleListener;
        titleKey = keyValue;

        setTitleApplicationProjectName();
    }

    /**
     * Shows the project name in the frame title bar
     */
    public void setTitleApplicationProjectName()
    {
        String newTitle = new String();
        if      ( ViskitConfigurationStore.PROJECT_TITLE_NAME.toLowerCase().contains("project"))
             newTitle +=         ": " +    ViskitConfigurationStore.instance().getVal(ViskitConfigurationStore.PROJECT_TITLE_NAME);
        else if (!ViskitConfigurationStore.PROJECT_TITLE_NAME.isBlank())
             newTitle += " Project: " + ViskitConfigurationStore.instance().getVal(ViskitConfigurationStore.PROJECT_TITLE_NAME);
        // otherwise value is unchanged;
        
        if (this.titlelListener != null) 
        {
            titlelListener.setTitle(newTitle, titleKey);
        }
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
    public void setController(MvcController controller) {
        this.mvcController = controller;
    }

    @Override
    public void setModel(MvcModel model) {
        this.mvcModel = model;
        registerWithModel();
    }

    @Override
    public abstract void modelChanged(MvcModelEvent event);

}
