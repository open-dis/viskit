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
    private static final String WTMP = "wtmp.xml";
    private static final String PASSWD = "passwd.xml";
    private static final String SALT = "gridkit!";
    
    /** Creates a new instance of SessionManager */
    public SessionManager() {
        log("SessionManager initialized "+new java.util.Date().toString());
    }
    
    String login(String username, String password) {
        
        InputStream is = 
                Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(PASSWD);
        try {
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
                        log("Session created: "+username+" "+new java.util.Date().toString());
                    }
                    return cookie;
               } else {
                    log("Failed login attempt: "+username+" "+new java.util.Date().toString());
                    return "BAD-LOGIN";
                }
            }
        } catch (Exception e) {
            log("Login error: "+new java.util.Date().toString());
            return "LOGIN-ERROR";
        }
        
        log("Unknown user "+username+": "+new java.util.Date().toString());
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
     * uid encrypted with uid.
     */
   
    
    Boolean addUser(String ssid, String newUser) {
        if ( isAdmin(ssid) ) {
            
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(PASSWD);
            try {
                Unmarshaller u = jaxbCtx.createUnmarshaller();
                PasswordFileType passwd = (PasswordFileType) u.unmarshal(is);
                is.close();
                List users = passwd.getUser();
                String passcrypt = null;
                Iterator it = users.iterator();
                while (it.hasNext()) {
                    UserType user = (UserType) it.next();
                    if (user.getName().equals(newUser)) {
                        passcrypt = user.getPassword();
                    }
                }
                if (passcrypt == null) {
                    // null means this user isn't in the db
                    // initialize a new user with temporary password as
                    // new user id itself
                    UserType user = (new ObjectFactory()).createUserType();
                    user.setName(newUser);
                    
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
                    URLConnection url = Thread.currentThread().getContextClassLoader().getResource(PASSWD).openConnection();
                    OutputStream os = url.getOutputStream();
                    jaxbCtx.createMarshaller().marshal(passwd,os);
                    os.flush();
                    os.close();
                    log("New user created: "+newUser+" "+new java.util.Date().toString());
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
    
    public Boolean changePassword(String usid, String user, String newPassword) {
        if ( getUser(usid).equals(user) || isAdmin(usid) ) {
            // TBD
            log("Password changed: "+user+" "+new java.util.Date().toString());
            return Boolean.TRUE;
        }
        
        return Boolean.FALSE;
    }
  
    // end of XML-RPC direct back-ends
    
    private String getPasscrypt(String username) {
        try {
            String passcrypt = null;
            Unmarshaller u = jaxbCtx.createUnmarshaller();
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(PASSWD);
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
        if ( sessions.get(usid) != null ) {
            return true;
        } else {
            return false;
        }
    }
    
    boolean isAdmin(String usid) {
        if (getUser(usid).equals("admin")) return true;
        
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
        message = "<Log>"+message+"</Log>\n";
        try {
            OutputStream os =
                    Thread.currentThread()
                    .getContextClassLoader()
                    .getResource(WTMP)
                    .openConnection()
                    .getOutputStream();
            os.write(message.getBytes());
            os.close();
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
