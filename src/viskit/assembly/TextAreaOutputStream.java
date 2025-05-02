/*
Copyright (c) 1995-2025 held by the author(s).  All rights reserved.

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
      Modeling, Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and https://my.nps.edu/web/moves)
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
package viskit.assembly;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.swing.JTextArea;
import javax.swing.Timer;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;

/**
 * TextAreaOutputStream.java
 Created on Aug 18, 2008

 A class to stream text to a jTextArea

 MOVES Institute
 Naval Postgraduate School, Monterey, CA, USA
 www.nps.edu
 *
 * @author mike
 * @version $Id$
 */
public class TextAreaOutputStream extends ByteArrayOutputStream implements ActionListener
{
  public static final int CONSOLE_OUTPUT_LIMIT = 1024 * 1024 * 8; // 8Mb
  public static final int CONSOLE_BACKOFF_SIZE = 1024 * 16;       // 16Kb, must be less than CONSOLE_OUTPUT_LIMIT
  
    static final Logger LOG = LogManager.getLogger();
  
  private final JTextArea jTextArea;
  private final Timer  swingTimer;
  private final int    delay = 125; //250;   // Performance adjuster for slow machines
  private final String CONSOLE_warningMessage = "[Console output limit exceeded, prior text deleted.]\n" +
                                                "----------------------------------------------------\n";
  public TextAreaOutputStream(JTextArea textArea, int bufferSize)
  {
    super(bufferSize);
    jTextArea = textArea;
    swingTimer = new Timer(delay, this);
    swingTimer.start();
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    int inputSize = size();
    if (inputSize > 0)
    {
      String inputString = this.toString();  // "this" = this output stream
      reset();

      if (inputString.length() > CONSOLE_OUTPUT_LIMIT) 
      {
        int backoff = Math.max(CONSOLE_BACKOFF_SIZE, inputSize);
        if (backoff > 1)
        {
            int beginIndex = 0;
            if      (inputString.contains(CONSOLE_warningMessage))
                     beginIndex = inputString.lastIndexOf(CONSOLE_warningMessage) + 4;
            else if (inputString.contains("---+"))
                     beginIndex = inputString.lastIndexOf("---+") + 4;
            jTextArea.replaceRange("\n\n" + CONSOLE_warningMessage, beginIndex, backoff - 1);
            LOG.info("Warning: " + CONSOLE_warningMessage.substring(beginIndex, CONSOLE_warningMessage.indexOf("\n")));
        }
      }
      jTextArea.append(inputString);
      jTextArea.setCaretPosition(jTextArea.getDocument().getLength()-1);
     }
    try {
        close();
    } 
    catch (IOException ex) {
        LOG.error(ex);
    }
  }

  /** swingTimer.stop() */
  public void stop()
  {
    swingTimer.stop();
    actionPerformed(null);  // flush last bit of text
  }
}
