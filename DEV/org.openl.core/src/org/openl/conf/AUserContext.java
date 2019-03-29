package org.openl.conf;

import java.util.Objects;

/**
 * The implementation of the hashCode() and equals() for the derives classes.
 *
 * @author sam
 */
public abstract class AUserContext implements IUserContext {
    // Classloader is important part of user context, commonly each executable
    // instance of rules is made in separate classloader that serves an
    // identifier of this instance.
    // For example two files with rules placed into the same folder(common user
    // home) and java beans are shared between these rules,
    // in this case classloader of each rules instance helps to distinguish
    // usercontexts of modules.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof IUserContext)) {
            return false;
        }
        IUserContext c = (IUserContext) obj;

        return Objects.equals(getUserClassLoader(), c.getUserClassLoader()) &&
        Objects.equals(getUserHome(), c.getUserHome());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserClassLoader(), getUserHome());
    }

}
