package viskit.model;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: Apr 1, 2004
 * Time: 3:57:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConstructorArgument extends ViskitElement
{
  private String type;
  private String value;
  private ArrayList comments=new ArrayList();

  public ArrayList getComments()
  {
    return comments;
  }

  public void setComments(ArrayList comments)
  {
    this.comments = comments;
  }

  public String getValue()
  {
    return value;
  }

  public void setValue(String name)
  {
    this.value = name;
  }

  public String getType()
  {
    return type;
  }

  public void setType(String type)
  {
    this.type = type;
  }
}
