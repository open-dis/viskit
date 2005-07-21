/*
Copyright (c) 1995-2005 held by the author(s).  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other materials provided with the
      distribution.
    * Neither the names of the Naval Postgraduate School (NPS)
      Modeling Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and http://www.MovesInstitute.org)
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 21, 2005
 * @since 12:29:08 PM
 */

package viskit.doe;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.util.Vector;
import java.awt.*;

import org.apache.xmlrpc.*;

import javax.swing.*;

public class JobLauncher extends JFrame implements Runnable
{
  String file;
  File f;
  FileReader fr;
  PrintWriter out;
  BufferedReader br;
  XmlRpcClientLite rpc;
  private JTextArea ta;

  String clusterDNS = "cluster.moves.nps.navy.mil";

  public JobLauncher(String file, JFrame mainFrame)
  {
    super("Job "+file);
    this.file = file;
    setBounds(mainFrame.getX()+ 100, mainFrame.getY()+100,400,300);
    ta = new JTextArea();
    JScrollPane jsp = new JScrollPane(ta);
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(jsp,BorderLayout.CENTER);

    setVisible(true);
    new Thread(this).start();
  }
  private void writeStatus(final String s)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        ta.append(s);
      }
    });
  }
  StringWriter data;
  public void run()
  {
    //todo use SimkitAssemblyXML2Java methods
    try {
      f = new File(file);
      writeStatus("Building XML-RPC client to "+clusterDNS+".\n");
      rpc = new XmlRpcClientLite(clusterDNS, 4444);

      writeStatus("Creating java.lang.String from file "+file+"\n");

      fr = new FileReader(f);
      br = new BufferedReader(fr);
      data = new StringWriter();
      out = new PrintWriter(data);
      String line;
      while ((line = br.readLine()) != null) {
        out.println('\t' + line);
        //System.out.println('\t' + line);
      }
      out.close();


    }
    catch (Exception e) {
      e.printStackTrace();
    }


    try {
     //send file to front end
      java.util.Vector parms = new Vector();
      parms.add(data.toString());
      writeStatus("Sending job file to "+clusterDNS+"\n");
      Object o = rpc.execute("experiment.setAssembly", parms);
      writeStatus("setAssembly returned" + o);

    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }
}
