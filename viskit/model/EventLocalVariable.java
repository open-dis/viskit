package viskit.model;

import viskit.VGlobals;


/**
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: Apr 1, 2004
 * Time: 3:59:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class EventLocalVariable extends ViskitElement
{
  private String type;
  private String name;
  private String value;
  private String comment="";
  private String arrayType;
  private String[] arraySize;

  public EventLocalVariable(String name, String type, String value)
  {
    this.name = name;
    setType(type);
    this.value = value;
  }
  public String getComment()
  {
    return comment;
  }

  public void setComment(String comment)
  {
    this.comment = comment;
  }

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getType()
  {
    return type;
  }

/*
  public void setType(String type)
  {
    this.type = type;
  }
*/

  public String getValue()
  {
    return value;
  }

  public void setValue(String value)
  {
    this.value = value;
  }
  
  public void setType(String pType)
  {
    type = pType;
    arrayType = VGlobals.instance().stripArraySize(pType);
    arraySize = VGlobals.instance().getArraySize(pType);
  }

  public String   getArrayType()       { return arrayType; }
  public String[] getArraySize()       { return arraySize; }


}
