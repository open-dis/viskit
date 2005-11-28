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
 * @since Nov 28, 2005
 * @since 10:43:16 AM
 */

package edu.nps.util;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.util.StringTokenizer;


/**
 * This class is based on sample code found on various web sites.
 */
public class CryptoMethods
{
  private static final byte[] PRIVATE_KEY =
      {106,93,45,-24,68,125,-92,103};

  public static String doEncryption(String string, Key key)
  {
    try {
      Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
      desCipher.init(Cipher.ENCRYPT_MODE, key);
      byte[] cleartext = string.getBytes();
      byte[] ciphertext = desCipher.doFinal(cleartext);
      return getString(ciphertext);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static String doDecryption(String string, Key key)
  {
    try {
      Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
      byte[] ciphertext = getBytes(string);
      desCipher.init(Cipher.DECRYPT_MODE, key);
      byte[] cleartext = desCipher.doFinal(ciphertext);

      return new String(cleartext);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static Key getTheKey()
  {
    try {
      byte[] bytes = PRIVATE_KEY;
      DESKeySpec pass = new DESKeySpec(bytes);
      SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
      SecretKey s = skf.generateSecret(pass);
      return s;
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static String getString(byte[] bytes)
  {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < bytes.length; i++) {
      byte b = bytes[i];
      sb.append((int) (0x00FF & b));
      if (i + 1 < bytes.length) {
        sb.append("-");
      }
    }
    return sb.toString();
  }

  private static byte[] getBytes(String str)
  {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    StringTokenizer st = new StringTokenizer(str, "-", false);
    while (st.hasMoreTokens()) {
      int i = Integer.parseInt(st.nextToken());
      bos.write((byte) i);
    }
    return bos.toByteArray();
  }
}