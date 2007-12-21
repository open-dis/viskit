/*
 * DoeSessionDriver.java
 *
 * Created on January 8, 2007, 11:57 AM
 *
 */

package viskit.doe;

/**
 *
 * @author Rick Goldberg
 */
public interface DoeSessionDriver {
    /**
     * create a new user identity with default password as
     * uid encrypted with uid. To bootstrap a password file,
     * addUser will check if file exists, enabling if it doesn't
     * without a valid usid, once.
     */
    public void addUser(String newUser) throws DoeException;

    public void changePassword(String username, String newPassword) throws DoeException;

    public void login(String username, String password) throws DoeException;

    public void logout() throws DoeException;

    
}
