/*
Copyright (c) 1995-2008 held by the author(inputString).  All rights reserved.

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
(http://www.nps.edu and http://www.movesinstitute.org)
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
package viskit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import javax.swing.JTextArea;
import javax.swing.Timer;

/**
 * JTextAreaOutputStream.java
 * Created on Aug 18, 2008
 *
 * A class to stream text to a jTextArea
 * 
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA, USA
 * www.nps.edu
 *
 * @author mike
 * @version $Id$
 */
public class JTextAreaOutputStream extends ByteArrayOutputStream implements ActionListener
{
  private JTextArea jta;
  private Timer swingTimer;
  private int delay = 125; //250;   // Performance adjuster for slow machines
  
  final public static int OUTPUTLIMIT = 1024 * 1024 * 8; // 8Mb
  final public static int BACKOFFSIZE = 1024 * 16;       // 16Kb, must be less than OUTPUTLIMIT
  
  private final String warningMsg = "Output limit exceeded / previous text deleted.\n" +
                                    "----------------------------------------------\n";
  JTextAreaOutputStream(JTextArea ta, int buffSize)
  {
    super(buffSize);
    jta = ta;
    swingTimer = new Timer(delay, this);
    swingTimer.start();
  }

  public void actionPerformed(ActionEvent e)
  {
    int inputSize = size();
    if (inputSize > 0) {
      
      String inputString = this.toString();  // "this" = this output stream
      reset();
          
      if (jta.getDocument().getLength() > OUTPUTLIMIT) {
        int backoff = Math.max(BACKOFFSIZE, inputSize);
        jta.replaceRange(warningMsg, 0, backoff - 1);
      }
      jta.append(inputString);
      jta.setCaretPosition(jta.getDocument().getLength()-1);
     }
  }

  public void kill()
  {
    swingTimer.stop();
    actionPerformed(null);  // flush last bit
  }
}
