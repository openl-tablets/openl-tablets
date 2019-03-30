package org.openl.types.java;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openl.binding.impl.module.VariableInContextFinder;

/**
 * Most of the time classes are wrapped with JavaOpenClass. When you need to wrap your class with another one with
 * additional functionality, you can extend JavaOpenClass and specify with this annotation that OpenL should use that
 * custom java open class to wrap your class.
 *
 * @author NSamatov
 */
@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface CustomJavaOpenClass {
    /**
     * Custom java open class must have a constructor with signature 'public MyCustomJavaOpenClass(Class<?> c)'
     */
    Class<? extends JavaOpenClass> type();

    /**
     * Custom VariableInContextFinder implementation must have a constructor with signature
     * 'MyVariableInContextFinder(IOpenField localVar, int maxDepthLevel)'
     */
    Class<? extends VariableInContextFinder> variableInContextFinder();
}
