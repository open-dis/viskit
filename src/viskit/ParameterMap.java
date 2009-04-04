package viskit;

import java.lang.annotation.*;

/** Used for the Viskit UI population of fields
 * 
 * @since April 3, 2007, 7:47 PM
 * @author Rick Goldberg
 * @version $Id$
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR})
public @interface ParameterMap {
      String[] names();
      String[] types();
}