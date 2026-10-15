package org.openl.itest.proxies;

/**
 * A plain Java bean of the application class path. Proxied by class, it is extended by its proxy.
 */
public class JavaGreeting {

    public String greet(String name) {
        return "Hello, " + name;
    }

    /**
     * Package-private: a proxy intercepts it only when the proxy class is defined beside this class, in the same
     * class loader and package.
     */
    String punctuation() {
        return ".";
    }
}
