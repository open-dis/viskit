/*
 * ParameterMap.java
 *
 * Created on April 3, 2007, 7:47 PM
 *
 * Annotation type for classes compiled from XML
 */

package viskit;
import java.lang.annotation.*;
/**
 *
 * @author Rick Goldberg
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR})
public @interface ParameterMap {
      String[] names();
      String[] types();
}