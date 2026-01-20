package org.openl.conf;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

/**
 * The class is designed as immutable, but not immutable because contains ClassLoader.
 *
 * @author snshor
 */
public final class UserContext implements IUserContext {

    private final ClassLoader userClassLoader;

    public UserContext(ClassLoader userClassLoader) {
        this.userClassLoader = userClassLoader;
    }

    @Override
    public ClassLoader getUserClassLoader() {
        return userClassLoader;
    }

    private String printClassloader(ClassLoader ucl) {
        if (ucl == null) {
            return "null";
        }
        if (ucl instanceof URLClassLoader) {
            URL[] urls = ((URLClassLoader) ucl).getURLs();
            StringBuilder sb = new StringBuilder();
            sb.append("ClassLoader URLs: ");
            for (URL url : urls) {
                sb.append(url.toExternalForm());
                sb.append(',');
            }
            return sb.toString();
        }
        return ucl.toString();
    }

    @Override
    public String toString() {
        return "cl=" + printClassloader(userClassLoader);
    }

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

        return Objects.equals(getUserClassLoader(), c.getUserClassLoader());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserClassLoader());
    }
}
