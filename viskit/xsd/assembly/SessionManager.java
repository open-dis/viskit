/*
 * SessionManager.java
 *
 * Created on January 26, 2006, 4:57 PM
 * 
 * Handles login and session cookies, required to
 * authenticate and de-multiplex multi-jobs.
 *
 * A Session is roughly equivalent to an SGE JOB_ID
 * however we don't know that until the jobs are launched
 * and need to associate a user session first.
 *
 * The cookie returned by the login call is a Unique Session ID (USID)
 * that must be used by subsequent calls during that session. An explicit
 * logout will delete a session, however a complete log of the experiment
 * is still stored in the user spool, and as well a log of login activity.
 * (TBD later retrieval post session).
 * Sessions may timeout due to inactivity (TBD).
 */

package viskit.xsd.assembly;

import java.util.Hashtable;
import java.util.List;
import java.util.Iterator;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import viskit.xsd.bindings.assembly.ObjectFactory;
import viskit.xsd.bindings.assembly.PasswordFileType;
import viskit.xsd.bindings.assembly.UserType;

/**
 *
 * @author Rick Goldberg
 */
public class SessionManager {
    private Hashtable sessions;
    private JAXBContext jaxbCtx;
    private static final String WTMP = "/tmp/wtmp.xml";
    private static final String PASSWD = "/tmp/passwd.xml";
    private static final String SALT = "gridkit!";
    
    /** Creates a new instance of SessionManager */
    public SessionManager() {
        sessions = new Hashtable();
        try {
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
        } catch (Exception e) {
            log("Package Error");
        }
        log("SessionManager initialized");
    }
    
    String login(String username, String password) {
 
        try {
            File pwf = new File(PASSWD);
            FileInputStream is = 
                new FileInputStream(PASSWD);
            Unmarshaller u = jaxbCtx.createUnmarshaller();
            PasswordFileType passwd = (PasswordFileType) u.unmarshal(is);
            List users = passwd.getUser();
            String passcrypt = null;
            Iterator it = users.iterator();
            while (it.hasNext()) {
                UserType user = (UserType) it.next();
                if (user.getName().equals(username)) {
                    passcrypt = user.getPassword();
                }
            }
            if (passcrypt != null) {
                // 8-byte Salt
                byte[] salt = SALT.getBytes();
                PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 18);
                SecretKey key = SecretKeyFactory.getInstance(
                    "PBEWithMD5AndDES").generateSecret(keySpec);
                Cipher decipher = Cipher.getInstance(key.getAlgorithm());
                PBEParameterSpec paramSpec = new PBEParameterSpec(salt, 18);
                decipher.init(Cipher.DECRYPT_MODE,key,paramSpec);
                // Decode base64 to get bytes
                byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(passcrypt);
                // Decrypt
                byte[] utf8 = decipher.doFinal(dec);
                // Decode using utf-8, the uid is encrypted with the password
                // to create the database entry for the encrypted pass. decrypting
                // that with the password gives the uid.
                String clear = new String(utf8, "UTF8");
                if (clear.equals(username)) {
                    String cookie = generateCookie(username);
                    if (! cookie.equals("BAD-COOKIE")) {
                        createSession(cookie, username);
                        log("Session created for "+username);
                    }
                    return cookie;
               } else {
                    log("Failed login attempt for "+username);
                    return "BAD-LOGIN";
                }
            }
        } catch (Exception e) {
            log(new java.util.Date().toString()+": Login error");
            return "LOGIN-ERROR";
        }
        
        log("Unknown user "+username);
        return "UNKNOWN-USER";
    }
    
    void logout(String ssid) {
        if (sessions.containsKey(ssid)) {
            String[] session = (((String) sessions.get(ssid)).trim()).split("\\s+");
            long startTime = Long.parseLong(session[session.length - 1]);
            long endTime = new java.util.Date().getTime();
            sessions.remove(ssid);
            log("Logout "+session[0]+" after "+(endTime-startTime)/1000+" seconds");
            
        }
    }
       
    private String generateCookie(String username) {
        try {
            byte[] salt = SALT.getBytes();
            // use passcrypt to encrypt date, or hard random if needed
            PBEKeySpec keySpec = new PBEKeySpec(getPasscrypt(username).toCharArray(), salt, 18);
            SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
            Cipher encipher = Cipher.getInstance(key.getAlgorithm());
            PBEParameterSpec paramSpec = new PBEParameterSpec(salt, 18);
            encipher.init(Cipher.ENCRYPT_MODE,key,paramSpec);
            byte[] utf8 = (""+new java.util.Date().getTime()).getBytes("UTF8");
            byte[] enc = encipher.doFinal(utf8);
            // Encode bytes to base64 to get a String
            return new sun.misc.BASE64Encoder().encode(enc);
        } catch (Exception e) {
            return "BAD-COOKIE";
        }
    }
    
    // tag the username with the login time, can auto-logout
    // after inactivity check (TBD). see note about cookie
    // randomness and decide if following is redundant:
    private void createSession(String cookie, String username) {
        sessions.put(cookie, username+" "+new java.util.Date().getTime());
    }
    
                   
    /**
     * create a new user identity with default password as
     * uid encrypted with uid. To bootstrap a password file,
     * addUser will check if file exists, enabling if it doesn't
     * without a valid ssid, once.
     */
   
    
    Boolean addUser(String ssid, String newUser) {
        if ( isAdmin(ssid) ) {
            
            File pwd = new File(PASSWD);
            
            try {
                String passcrypt = null;
                Unmarshaller u = jaxbCtx.createUnmarshaller();

                ObjectFactory of = new ObjectFactory();
                PasswordFileType passwd = of.createPasswordFileType();
                List users = passwd.getUser();
                if (pwd.exists()) {
                    FileInputStream is = new FileInputStream(pwd);
                    passwd = (PasswordFileType) u.unmarshal(is);
                    is.close();
                    users = passwd.getUser();
                    Iterator it = users.iterator();
                    while (it.hasNext()) {
                        UserType user = (UserType) it.next();
                        if (user.getName().equals(newUser)) {
                            passcrypt = user.getPassword();
                        }
                    }
                }
                if (passcrypt == null) {
                    // null means this user isn't in the db
                    // initialize a new user with temporary password as
                    // new user id itself
                    UserType user = (new ObjectFactory()).createUserType();
                    user.setName(newUser);
                    log("Trying to add user "+newUser);
                    // set up crypto stuff
                    byte[] salt = SALT.getBytes();
                    PBEKeySpec keySpec = new PBEKeySpec(newUser.toCharArray(), salt, 18);
                    SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
                    Cipher encipher = Cipher.getInstance(key.getAlgorithm());
                    PBEParameterSpec paramSpec = new PBEParameterSpec(salt, 18);
                    encipher.init(Cipher.ENCRYPT_MODE,key,paramSpec);
                    
                    // encrypt newUser id with newUser id for temporary password token
                    byte[] utf8 = newUser.getBytes("UTF8");
                    byte[] enc = encipher.doFinal(utf8);
                    
                    // Encode bytes to base64 to get a string and write out to passwd.xml file
                    user.setPassword( new sun.misc.BASE64Encoder().encode(enc) );
                    users.add(user);
                    
                    // write out to XML user database
                    FileOutputStream fos = new FileOutputStream(pwd);
                    
                    jaxbCtx.createMarshaller().marshal(passwd,fos);
                    fos.flush();
                    fos.close();
                    log("New user created for "+newUser);
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            } catch (Exception e) {
                return Boolean.FALSE;
            }
            
            
        }
        return Boolean.FALSE;
    }
    
    public Boolean changePassword(String usid, String username, String newPassword) {
        File pwd = new File(PASSWD);
        if ( getUser(usid).equals(username) || isAdmin(usid) ) {
            try {
                Unmarshaller u = jaxbCtx.createUnmarshaller();
                FileInputStream is = new FileInputStream(pwd);
                PasswordFileType passwd = (PasswordFileType) u.unmarshal(is);
                is.close();
                List users = passwd.getUser();
                Iterator it = users.iterator();
                while (it.hasNext()) {
                    UserType user = (UserType) it.next();
                    if (user.getName().equals(username)) {
                        //TBD could refactor out the crypto stuff 
                        byte[] salt = SALT.getBytes();
                        PBEKeySpec keySpec = new PBEKeySpec(newPassword.toCharArray(), salt, 18);
                        SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
                        Cipher encipher = Cipher.getInstance(key.getAlgorithm());
                        PBEParameterSpec paramSpec = new PBEParameterSpec(salt, 18);
                        encipher.init(Cipher.ENCRYPT_MODE,key,paramSpec);
                        
                        // encrypt username id with newPassword for temporary password token
                        byte[] utf8 = username.getBytes("UTF8");
                        byte[] enc = encipher.doFinal(utf8);
                        
                        // Encode bytes to base64 to get a string and write out to passwd.xml file
                        user.setPassword( new sun.misc.BASE64Encoder().encode(enc) );
                    }
                }
                FileOutputStream fos = new FileOutputStream(pwd);
                jaxbCtx.createMarshaller().marshal(passwd,fos);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                log("Error changing password for"+username);
                return Boolean.FALSE;
            }
            log("Password changed for "+username);
            return Boolean.TRUE;
        }
        
        return Boolean.FALSE;
    }
  
    // end of XML-RPC direct back-ends
    
    private String getPasscrypt(String username) {
        try {
            String passcrypt = null;
            Unmarshaller u = jaxbCtx.createUnmarshaller();
            File pwd = new File(PASSWD);
            FileInputStream is = new FileInputStream(pwd);
            PasswordFileType passwd = (PasswordFileType) u.unmarshal(is);
            List users = passwd.getUser();
            Iterator it = users.iterator();
            while (it.hasNext()) {
                UserType user = (UserType) it.next();
                if (user.getName().equals(username)) {
                    passcrypt = user.getPassword();
                }
            }
            return passcrypt;
        } catch (Exception e) {
            return e.toString();
        }
    }
 
    boolean authenticate(String usid) {
        // isAdmin only happens if no password file
        // has yet been created. 
        if ( sessions.get(usid) != null || isAdmin(usid)) {
            return true;
        } else {
            return false;
        }
    }
    
    boolean isAdmin(String usid) {
        System.out.println("isAdmin?"+usid);
        if (getUser(usid).equals("admin")) return true;
        File f = new File(PASSWD);
        System.out.println(f.exists());
        if(!f.exists()) return true; // init passwd with addUser for admin
        return false;
    }
    
    private String getUser(String usid) {
        if ( sessions.containsKey(usid) ) {
            String[] session = ((String)sessions.get(usid)).trim().split("\\s+");
            return session[0];
        }
        return "nobody";
    }
    
    void log(String message) {
        int z;
        String line = "<Log>"+new java.util.Date().toString()+": "+message+"</Log>"+'\n';
        try {
            File f = new File(WTMP);
            if (!f.exists()) {
                f.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(f,true);
            fos.write(line.getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
