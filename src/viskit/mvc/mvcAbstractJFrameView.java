package viskit.mvc;

import javax.swing.JFrame;
import viskit.ViskitConfig;
import viskit.ViskitGlobals;
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
public abstract class mvcAbstractJFrameView extends JFrame implements mvcView, mvcModelListener {

    protected TitleListener titleListener;
    protected int titleKey;
    private mvcModel model;
    private mvcController controller;

    public mvcAbstractJFrameView(String title) {
        super(title);
    }

    public void registerWithModel() {
        ((mvcAbstractModel) model).addModelListener(this);
    }

    /** Sets the frame title listener and key for this frame
     *
     * @param listener the title listener to set
     * @param key the key for this frame's title
     */
    public void setTitleListener(TitleListener listener, int key) {
        titleListener = listener;
        titleKey = key;

        showProjectName();
    }

    /**
     * Shows the project name in the frame title bar
     */
    public void showProjectName() {

        String title = " Viskit: " + ViskitGlobals.instance().getCurrentViskitProject().getProjectName();
				// ViskitConfig.instance().getVal(ViskitConfig.PROJECT_TITLE_NAME);
        if (!title.contains("Project"))
		     title += " Project";
	    setTitle(title);
        if (this.titleListener != null) {
            titleListener.setTitle(title, titleKey);
        }
    }

    @Override
    public mvcController getController() {
        return controller;
    }

    @Override
    public mvcModel getModel() {
        return model;
    }

    @Override
    public void setController(mvcController controller) {
        this.controller = controller;
    }

    @Override
    public void setModel(mvcModel model) {
        this.model = model;
        registerWithModel();
    }

    @Override
    public abstract void modelChanged(mvcModelEvent event);

}
