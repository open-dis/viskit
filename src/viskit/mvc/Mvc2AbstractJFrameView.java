package viskit.mvc;

import javax.swing.JFrame;
import viskit.ViskitConfig;
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
public abstract class Mvc2AbstractJFrameView extends JFrame implements Mvc2View, Mvc2ModelListener {

    protected TitleListener titlList;
    protected int titlKey;
    private Mvc2Model model;
    private Mvc2Controller controller;

    public Mvc2AbstractJFrameView(String title) {
        super(title);
    }

    public void registerWithModel() {
        ((Mvc2AbstractModel) model).addModelListener(this);
    }

    /** Sets the frame title listener and key for this frame
     *
     * @param lis the title listener to set
     * @param key the key for this frame's title
     */
    public void setTitleListener(TitleListener lis, int key) {
        titlList = lis;
        titlKey = key;

        showProjectName();
    }

    /**
     * Shows the project name in the frame title bar
     */
    public void showProjectName() {
        String ttl = " Project: " + ViskitConfig.instance().getVal(ViskitConfig.PROJECT_TITLE_NAME);
        if (this.titlList != null) {
            titlList.setTitle(ttl, titlKey);
        }
    }

    @Override
    public Mvc2Controller getController() {
        return controller;
    }

    @Override
    public Mvc2Model getModel() {
        return model;
    }

    @Override
    public void setController(Mvc2Controller controller) {
        this.controller = controller;
    }

    @Override
    public void setModel(Mvc2Model model) {
        this.model = model;
        registerWithModel();
    }

    @Override
    public abstract void modelChanged(Mvc2ModelEvent event);

}
