package viskit;

import java.io.*;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jul 7, 2004
 * Time: 11:24:17 AM
 */

/**
 * This is a class to run a java class in a new Java VM.  It is designed to be used in a logical way:
 *  ar = new AssemblyRunner("foobar.class");
 *  ar.setArguments("-arg1 -arg2");
 */
public class AssemblyRunner
{
  String classpath;

  String clsName;
  String arguments;
  Thread thr;
  private Runtime rt = Runtime.getRuntime();
  private String filesep = System.getProperty("file.separator");
  //private String fileext = "";
  private String[] execString;
  private boolean isJar = false;
  private Process proc;
  private PipedReader outPipe;
  private PipedReader errPipe;
  private PipedWriter inpPipe;

  private PipedWriter myOutPipe;
  private PipedWriter myErrPipe;
  private PipedReader myInpPipe;

  public AssemblyRunner(String className)
  {
    if(className.endsWith(".jar"))
      isJar = true;
    //String platform = System.getProperty("os.name");
    //if(platform.indexOf("Win") != -1)
      //fileext=".exe";
    clsName = className;
    classpath = System.getProperty("java.class.path");  //default
    outPipe = new PipedReader();
    errPipe = new PipedReader();
    inpPipe = new PipedWriter();

    myOutPipe = new PipedWriter();
    myErrPipe = new PipedWriter();
    myInpPipe = new PipedReader();
    try {
      outPipe.connect(myOutPipe);
      errPipe.connect(myErrPipe);
      inpPipe.connect(myInpPipe);
    }
    catch (IOException e) {
      System.out.println("can't connect pipes");
    }
  }

  public void runAssembly()
  {
    execString = buildExecString();
    thr = new Thread(new myRunnable());
    thr.start();
  }
  private String[] buildExecString()
  {
    Vector v = new Vector();
     StringBuffer sb = new StringBuffer();
     sb.append(System.getProperty("java.home"));
     sb.append(filesep+"bin"+filesep+"java");
     //sb.append(fileext);
    v.add(sb.toString());
    v.add("-cp");
    v.add(classpath);
    if(isJar)
      v.add("-jar");
    v.add(clsName);
    String[] ra = new String[v.size()];
    return (String[])v.toArray(ra);
  }

  private void closePipes()
  {
/*
    try{myOutPipe.close();}catch(Exception e){}
    try{outPipe.close();}catch(Exception e){}

    try{myErrPipe.close();}catch(Exception e){}
    try{errPipe.close();}catch(Exception e){}

*/
    try{inpPipe.close();}catch(Exception e){}
/*    try{myInpPipe.close();}catch(Exception e){}
*/
  }

  class myRunnable implements Runnable
  {
    public void run()
    {
      //System.out.println("into execer");
      try {
        proc = rt.exec(execString);
        new Thread(new myProcOutReader()).start();
        new Thread(new myProcErrReader()).start();
        new Thread(new myProcInpWriter()).start();
      }
      catch(Exception e) {
        System.out.println("couldn't exec "+execString);
      }
      //System.out.println("out of execer");
    }
  }

  // Thread to read data from the Process "input" stream and write to a PipeWriter connected to the
  // users PipedReader;
  class myProcOutReader implements Runnable
  {
    public void run()
    {
      //System.out.println("into myProcOutReader");
      InputStreamReader isr = new InputStreamReader(proc.getInputStream());
      try {
        while(true) {
          int rd = isr.read();
          if(rd == -1)
            break;
          myOutPipe.write(rd);
        }
      }
      catch (IOException e) {}
      try{
        isr.close();
        closePipes();
      }
      catch(Exception ex){}
      //System.out.println("out of myProcOutReader");
    }
  }
  class myProcErrReader implements Runnable
  {
    public void run()
    {
      //System.out.println("into myProcErrReader");
      InputStreamReader isr = new InputStreamReader(proc.getInputStream());
      try {
        while(true) {
          int rd = isr.read();
          if(rd == -1)
            break;
          myErrPipe.write(rd);
        }
      }
      catch (IOException e) {}
      try {
        isr.close();
        closePipes();
      }
      catch(Exception ex){}
      //System.out.println("out of myProcErrReader");
    }
  }
  class myProcInpWriter implements Runnable
  {
    public void run()
    {
      //System.out.println("into myProcInpWriter");
      OutputStreamWriter osr = new OutputStreamWriter(proc.getOutputStream());
      try {
        while(true) {
          int rd = myInpPipe.read();
          if(rd == -1)
            break;
          inpPipe.write(rd);
        }
      }
      catch (IOException e) {}
      try {
        osr.close();
        closePipes();
      }
      catch(Exception ex){}
      //System.out.println("out of myProcInpWriter");
    }
  }
  public String getClasspath()
  {
    return classpath;
  }

  public void setClasspath(String classpath)
  {
    this.classpath = classpath;
  }
  public void setArguments(String args)
  {
    this.arguments = args;
  }
  public String getArguments()
  {
    return arguments;
  }

  public Reader getSysOutReader()
  {
    return outPipe;
  }
  public Reader getSysErrReader()
  {
    return errPipe;
  }
  public Writer getSysInWriter()
  {
    return inpPipe;
  }
  public String getClassName()
  {
    return clsName;
  }

}
