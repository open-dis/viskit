package viskit.doe;

/**
 *
 * @author Rick Goldberg
 * @since January 8, 2007, 11:57 AM
 */
public interface DoeSessionDriver {

    /**
     * create a new user identity with default password as
     * uid encrypted with uid. To bootstrap a password file,
     * addUser will check if file exists, enabling if it doesn't
     * without a valid usid, once.
     * @param newUser
     * @throws viskit.doe.DoeException
     */
    void addUser(String newUser) throws DoeException;

    void changePassword(String username, String newPassword) throws DoeException;

    void login(String username, String password) throws DoeException;

    void logout() throws DoeException;
}
