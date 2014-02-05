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

import edu.nps.util.LogUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import viskit.xsd.bindings.assembly.ObjectFactory;
import viskit.xsd.bindings.assembly.PasswordFile;
import viskit.xsd.bindings.assembly.User;

/**
 * @version $Id$
 * @author Rick Goldberg
 */
public class SessionManager /* compliments DoeSessionDriver*/ {

    private Hashtable<String, String> sessions;
    private JAXBContext jaxbCtx;

    Logger LOG = LogUtils.getLogger(SessionManager.class);

    private static final String PASSWD = System.getProperty("user.home") + "/.viskit/passwd.xml";
    private static final String SALT = "gridkit!";

    public static String LOGIN_ERROR = "LOGIN-ERROR";

    /** Creates a new instance of SessionManager */
    public SessionManager() { // public?
        sessions = new Hashtable<String, String>();
        try {
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
        } catch (JAXBException e) {
            LOG.error(e);
        }
        LOG.info("SessionManager initialized");
    }

    public String login(String username, String password) {

        try {
            FileInputStream is = new FileInputStream(PASSWD);
            Unmarshaller u = jaxbCtx.createUnmarshaller();
            PasswordFile passwd = (PasswordFile) u.unmarshal(is);
            List<User> users = passwd.getUser();
            String passcrypt = null;
            Iterator<User> it = users.iterator();
            while (it.hasNext()) {
                User user = it.next();
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
                byte[] dec = Base64.decodeBase64(passcrypt.getBytes("UTF-8"));
                // Decrypt
                byte[] utf8 = decipher.doFinal(dec);
                // Decode using utf-8, the uid is encrypted with the password
                // to create the database entry for the encrypted pass. decrypting
                // that with the password gives the uid.
                String clear = new String(utf8, "UTF-8");
                if (clear.equals(username)) {
                    String cookie = generateCookie(username);
                    if (!cookie.equals("BAD-COOKIE")) {
                        createSession(cookie, username);
                        LOG.info("Session created for "+username);
                    }
                    return cookie;
               } else {
                    LOG.warn("Failed login attempt for "+username);
                    return "BAD-LOGIN";
                }
            }
        } catch (FileNotFoundException e) {
            LOG.error(e);
            return LOGIN_ERROR;
        } catch (JAXBException e) {
            LOG.error(e);
            return LOGIN_ERROR;
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e);
            return LOGIN_ERROR;
        } catch (InvalidKeySpecException e) {
            LOG.error(e);
            return LOGIN_ERROR;
        } catch (NoSuchPaddingException e) {
            LOG.error(e);
            return LOGIN_ERROR;
        } catch (InvalidKeyException e) {
            LOG.error(e);
            return LOGIN_ERROR;
        } catch (InvalidAlgorithmParameterException e) {
            LOG.error(e);
            return LOGIN_ERROR;
        } catch (UnsupportedEncodingException e) {
            LOG.error(e);
            return LOGIN_ERROR;
        } catch (IllegalBlockSizeException e) {
            LOG.error(e);
            return LOGIN_ERROR;
        } catch (BadPaddingException e) {
            LOG.error(e);
            return LOGIN_ERROR;
        }

        LOG.info("Unknown user "+username);
        return "UNKNOWN-USER";
    }

    public Boolean logout(String ssid) {
        if (sessions.containsKey(ssid)) {
            String[] session = sessions.get(ssid).trim().split("\\s+");
            long startTime = Long.parseLong(session[session.length - 1]);
            long endTime = new java.util.Date().getTime();
            sessions.remove(ssid);
            LOG.info("Logout "+session[0]+" after "+(endTime-startTime)/1000+" seconds");
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * create a new user identity with default password as
     * uid encrypted with uid. To bootstrap a password file,
     * addUser will check if file exists, enabling if it doesn't
     * without a valid usid, once.
     * @param usid the user ID
     * @param newUser the name of the new user
     * @return an indication of success
     */
    public Boolean addUser(String usid, String newUser) {
        if ( isAdmin(usid) ) {

            File pwd = new File(PASSWD);

            try {
                String passcrypt = null;
                Unmarshaller u = jaxbCtx.createUnmarshaller();
                ObjectFactory of = new ObjectFactory();
                //JAXB feature(?)
                //createPasswordFile returns an Object which
                //when marshalled doesn't contain proper <PasswordFile>
                //tags, but createPasswordFile() does.
                PasswordFile passwd = of.createPasswordFile();

                // TODO: upgrade to generic JWSDP
                List<User> users = passwd.getUser();
                if (pwd.exists()) {
                    FileInputStream is = new FileInputStream(pwd);
                    passwd = (PasswordFile) u.unmarshal(is);
                    is.close();

                    // TODO: upgrade to generic JWSDP
                    users = passwd.getUser();
                    Iterator<User> it = users.iterator();
                    while (it.hasNext()) {
                        User user = it.next();
                        if (user.getName().equals(newUser)) {
                            passcrypt = user.getPassword();
                        }
                    }
                }
                if (passcrypt == null) {
                    // null means this user isn't in the db
                    // initialize a new user with temporary password as
                    // new user id itself
                    User user = (new ObjectFactory()).createUser();
                    user.setName(newUser);
                    LOG.info("Trying to add user "+newUser);
                    // set up crypto stuff
                    byte[] salt = SALT.getBytes();
                    PBEKeySpec keySpec = new PBEKeySpec(newUser.toCharArray(), salt, 18);
                    SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
                    Cipher encipher = Cipher.getInstance(key.getAlgorithm());
                    PBEParameterSpec paramSpec = new PBEParameterSpec(salt, 18);
                    encipher.init(Cipher.ENCRYPT_MODE,key,paramSpec);

                    // encrypt newUser id with newUser id for temporary password token
                    byte[] utf8 = newUser.getBytes("UTF-8");
                    byte[] enc = encipher.doFinal(utf8);

                    // Encode bytes to base64 to get a string and write out to passwd.xml file
                    user.setPassword( new String(new org.apache.commons.codec.binary.Base64().encode(enc)) );
                    users.add(user);

                    // write out to XML user database
                    FileOutputStream fos = new FileOutputStream(pwd);

                    jaxbCtx.createMarshaller().marshal(passwd,fos);
                    fos.flush();
                    fos.close();
                    LOG.info("New user created for "+newUser);
                    return Boolean.TRUE;
                } else {
                    LOG.warn("Add existing user failed for "+newUser);
                    return Boolean.FALSE;
                }
            } catch (JAXBException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (IOException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (NoSuchAlgorithmException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (InvalidKeySpecException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (NoSuchPaddingException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (InvalidKeyException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (InvalidAlgorithmParameterException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (IllegalBlockSizeException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (BadPaddingException e) {
                LOG.error(e);
                return Boolean.FALSE;
            }
        }
        LOG.warn("Attempted add user by non-admin!");
        return Boolean.FALSE;
    }

    public Boolean changePassword(String usid, String username, String newPassword) {
        File pwd = new File(PASSWD);
        if ( getUser(usid).equals(username) || isAdmin(usid) ) {
            try {
                Unmarshaller u = jaxbCtx.createUnmarshaller();
                FileInputStream is = new FileInputStream(pwd);
                PasswordFile passwd = (PasswordFile) u.unmarshal(is);
                is.close();
                List<User> users = passwd.getUser();
                Iterator<User> it = users.iterator();
                while (it.hasNext()) {
                    User user = it.next();
                    if (user.getName().equals(username)) {
                        //TBD could refactor out the crypto stuff
                        byte[] salt = SALT.getBytes();
                        PBEKeySpec keySpec = new PBEKeySpec(newPassword.toCharArray(), salt, 18);
                        SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
                        Cipher encipher = Cipher.getInstance(key.getAlgorithm());
                        PBEParameterSpec paramSpec = new PBEParameterSpec(salt, 18);
                        encipher.init(Cipher.ENCRYPT_MODE,key,paramSpec);

                        // encrypt username id with newPassword for temporary password token
                        byte[] utf8 = username.getBytes("UTF-8");
                        byte[] enc = encipher.doFinal(utf8);

                        // Encode bytes to base64 to get a string and write out to passwd.xml file
                        user.setPassword( new String(new org.apache.commons.codec.binary.Base64().encode(enc)) );
                    }
                }
                FileOutputStream fos = new FileOutputStream(pwd);
                jaxbCtx.createMarshaller().marshal(passwd,fos);
                fos.flush();
                fos.close();
            } catch (JAXBException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (IOException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (NoSuchAlgorithmException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (InvalidKeySpecException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (NoSuchPaddingException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (InvalidKeyException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (InvalidAlgorithmParameterException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (IllegalBlockSizeException e) {
                LOG.error(e);
                return Boolean.FALSE;
            } catch (BadPaddingException e) {
                LOG.error(e);
                return Boolean.FALSE;
            }
            LOG.info("Password changed for "+username);
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    // end of XML-RPC direct back-ends


     private String generateCookie(String username) {
        try {
            byte[] salt = SALT.getBytes();
            // use passcrypt to encrypt date, or hard random if needed
            PBEKeySpec keySpec = new PBEKeySpec(getPasscrypt(username).toCharArray(), salt, 18);
            SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
            Cipher encipher = Cipher.getInstance(key.getAlgorithm());
            PBEParameterSpec paramSpec = new PBEParameterSpec(salt, 18);
            encipher.init(Cipher.ENCRYPT_MODE,key,paramSpec);
            byte[] utf8 = (""+new java.util.Date().getTime()).getBytes("UTF-8");
            byte[] enc = encipher.doFinal(utf8);
            // Encode bytes to base64 to get a String
            return new String(new org.apache.commons.codec.binary.Base64().encode(enc));
        } catch (NoSuchAlgorithmException e) {
            return "BAD-COOKIE";
        } catch (InvalidKeySpecException e) {
            return "BAD-COOKIE";
        } catch (NoSuchPaddingException e) {
            return "BAD-COOKIE";
        } catch (InvalidKeyException e) {
            return "BAD-COOKIE";
        } catch (InvalidAlgorithmParameterException e) {
            return "BAD-COOKIE";
        } catch (UnsupportedEncodingException e) {
            return "BAD-COOKIE";
        } catch (IllegalBlockSizeException e) {
            return "BAD-COOKIE";
        } catch (BadPaddingException e) {
            return "BAD-COOKIE";
        }
    }

    // tag the username with the login time, can auto-logout
    // after inactivity check (TBD). see note about cookie
    // randomness and decide if following is redundant:

    private void createSession(String cookie, String username) {
        sessions.put(cookie, username+" "+new java.util.Date().getTime());
    }

    private String getPasscrypt(String username) {
        try {
            String passcrypt = null;
            Unmarshaller u = jaxbCtx.createUnmarshaller();
            File pwd = new File(PASSWD);
            FileInputStream is = new FileInputStream(pwd);
            PasswordFile passwd = (PasswordFile) u.unmarshal(is);
            List<User> users = passwd.getUser();
            Iterator<User> it = users.iterator();
            while (it.hasNext()) {
                User user = it.next();
                if (user.getName().equals(username)) {
                    passcrypt = user.getPassword();
                }
            }
            return passcrypt;
        } catch (JAXBException e) {
            return e.toString();
        } catch (FileNotFoundException e) {
            return e.toString();
        }
    }

    public boolean authenticate(String usid) {
        return sessions.get(usid) != null || isAdmin(usid);
    }

    boolean isAdmin(String usid) {
        if (getUser(usid).equals("admin")) {
            return true;
        }

        return !new File(PASSWD).exists();
    }

    private String getUser(String usid) {
        if ( sessions.containsKey(usid) ) {
            String[] session = sessions.get(usid).trim().split("\\s+");
            return session[0];
        }
        return "nobody";
    }
}
