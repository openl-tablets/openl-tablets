/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.Stack;

/**
 * The class is designed as immutable, but not immutable because contains
 * ClassLoader.
 * 
 * @author snshor
 * 
 */
public final class UserContext extends AUserContext {

    static ThreadLocal<Stack<IUserContext>> contextStack = new ThreadLocal<Stack<IUserContext>>();

    private ClassLoader userClassLoader;

    private String userHome;

    private Properties userProperties;

    public static IUserContext currentContext() {
        Stack<IUserContext> stack = contextStack.get();
        if (stack == null || stack.size() == 0) {
            return null;
        }
        return stack.peek();
    }

    @Deprecated
    public static IUserContext makeOrLoadContext(ClassLoader cl, String home) {
        return UserContext.getCurrentContextOrCreateNew(cl, home);
    }

    public static IUserContext getCurrentContextOrCreateNew(ClassLoader cl, String home) {
        IUserContext cxt = currentContext();
        if (cxt != null) {
            return cxt;
        }
        return new UserContext(cl, home);
    }

    public static void popCurrentContext() {
        contextStack.get().pop();
    }

    public static void pushCurrentContext(IUserContext cxt) {
        Stack<IUserContext> stack = contextStack.get();
        if (stack == null) {
            stack = new Stack<IUserContext>();
            contextStack.set(stack);
        }
        stack.push(cxt);
    }

    public UserContext(ClassLoader userClassLoader, String userHome) {
        this(userClassLoader, userHome, null);
    }

    public UserContext(ClassLoader userClassLoader, String userHome, Properties userProperties) {
        this.userClassLoader = userClassLoader;
        this.userHome = userHome;
        this.userProperties = userProperties;
    }

    public Object execute(IExecutable exe) {
        try {
            pushCurrentContext(this);
            return exe.execute();
        } finally {
            popCurrentContext();
        }
    }

    public ClassLoader getUserClassLoader() {
        return userClassLoader;
    }

    public String getUserHome() {
        return userHome;
    }

    public Properties getUserProperties() {
        return new Properties(userProperties);
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
        StringBuilder sb = new StringBuilder();
        sb.append("home=").append(userHome).append("cl=").append(printClassloader(userClassLoader));
        return sb.toString();
    }

}
