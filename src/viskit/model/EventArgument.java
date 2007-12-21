package viskit.model;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Apr 1, 2004
 * @since 3:57:26 PM
 * @version $Id: EventArgument.java 1662 2007-12-16 19:44:04Z tdnorbra $
 * To change this template use File | Settings | File Templates.
 */
public class EventArgument extends ViskitElement {
    private ArrayList<String> descriptionArray = new ArrayList<String>();

    private String type;
    private String name;
    private ArrayList<String> comments = new ArrayList<String>();

    public ArrayList<String> getDescription() {
        return comments;
    }

    public void setComments(ArrayList<String> comments) {
        this.comments = comments;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public ArrayList<String> getDescriptionArray() {
        return descriptionArray;
    }

    @Override
    public void setDescriptionArray(ArrayList<String> descriptionArray) {
        this.descriptionArray = descriptionArray;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
