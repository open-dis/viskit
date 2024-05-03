package viskit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Of importance for the java source generation of an Event Graph is the
 * Parameter Map. The Parameter Map is an annotation construct generated and
 * placed in generated source code to depict all described parameters of an
 * event graph which will aid in correct state variable initialization from
 * either the constructor, or the reset event. The Parameter Map is also used to
 * generated correctly parameterized constructors during source code generation,
 * and to construct proper setters and getters for parameters.
 *
 * The Parameter Map is also parsed from source code at runtime to allow Viskit
 * to properly identify constructors of EGs classes on the classpath whether
 * they are from generated source, or contained in third party libraries.  The
 * importance of this feature is that when Assemblies are constructed from
 * iconized EGs, the Assembly can be constructed such that each EG parameter is
 * identified and properly initialized at runtime from generated Assembly source
 * code.
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