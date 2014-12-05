package viskit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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