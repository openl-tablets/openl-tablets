package org.openl.rules.webstudio.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.text.MessageFormat;

import org.openl.main.OpenLVersion;
import org.openl.util.Log;
import org.openl.util.StringTool;

import edu.stanford.ejalbert.BrowserLauncher;

/*
 * Created on Jul 11, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */

/**
 * @author snshor
 */
public class StartTomcat {

    private static class BrowserStarter implements Runnable {

        private String browserURL;

        public BrowserStarter(String browserURL) {
            this.browserURL = browserURL;
        }

        public void run() {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                // Do nothing
            }

            try {
                Class<?> desktop = null;
                try {
                    desktop = Class.forName("java.awt.Desktop");
                } catch (ClassNotFoundException cnfe) {
                    // NOTICE: The desktop support is available beginning from
                    // JDK 1.6
                }

                boolean isDesktopSupported = false;

                if (desktop != null) {
                    Method isDesktopSupportedMethod = desktop.getMethod("isDesktopSupported", new Class[] {});

                    isDesktopSupported = (Boolean) isDesktopSupportedMethod.invoke(null, new Object[] {});

                    if (isDesktopSupported) {
                        Method getDesktop = desktop.getMethod("getDesktop", new Class[] {});

                        Object desktopObject = getDesktop.invoke(null, new Object[] {});

                        Method browse = desktop.getMethod("browse", new Class[] { URI.class });

                        browse.invoke(desktopObject, new Object[] { new URI(browserURL) });
                    }
                }
                if (desktop == null || !isDesktopSupported) {
                    BrowserLauncher browserLauncher = new BrowserLauncher();
                    browserLauncher.openURLinBrowser(browserURL);
                }
            } catch (Exception ex) {
                Log.error("Could not start a browser. Error: {0}", ex, ex.getMessage());
            }
        }

    }
    private static final String BROWSER_URL_PROPERTY = "browser.url";

    private static final String DEFAULT_BROWSER_URL = "http://localhost:8080/webstudio/";

    static public final String JAVA_CLASSPATH_PROPERTY = "java.class.path";

    private static String findChome() throws IOException {
        String cpath = System.getProperty(JAVA_CLASSPATH_PROPERTY);

        String[] pathElements = StringTool.tokenize(cpath, File.pathSeparator);

        for (int i = 0; i < pathElements.length; i++) {
            if (pathElements[i].endsWith("bootstrap.jar")) {
                File tomcatHome = new File(pathElements[i]).getCanonicalFile().getParentFile().getParentFile();
                return tomcatHome.toString();
            }
        }

        throw new RuntimeException("Could not find bootstrap.jar in " + JAVA_CLASSPATH_PROPERTY);
    }

    /*
     * C:\3p\jakarta-tomcat-5.0.25\bin>start "Tomcat"
     * "c:\j2sdk1.4.2_04\bin\java"
     * -Djava.endorsed.dirs="C:\3p\jakarta-tomcat-5.0.25\common\endorsed"
     * -classpath
     * "c:\j2sdk1.4.2_04\lib\tools.jar;C:\3p\jakarta-tomcat-5.0.25\bin\bootstrap.jar"
     * -Dcatalina.base="C:\3p\jakarta-tomcat-5.0.25"
     * -Dcatalina.home="C:\3p\jakarta-tomcat-5.0.25"
     * -Djava.io.tmpdir="C:\3p\jakarta-tomcat-5.0.25\temp"
     * org.apache.catalina.startup.Bootstrap start
     *
     */

    private static String getProperty(String[] args, String prefix) {
        if (args != null) {
            for (String parameter : args) {
                if (parameter.startsWith(prefix)) {
                    return parameter.substring(parameter.indexOf('=') + 1).trim();
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {

        // System.out.println("OpenL Tomcat Starter, Version " +
        // OpenLVersion.getVersion() +
        // " Build " + OpenLVersion.getBuild() + "
        // http://openl-tablets.sourceforge.net (c) 2006,2007\n");

        System.out.println("OpenL Tomcat Starter,  Version " + OpenLVersion.getVersion() + " Build "
                + OpenLVersion.getBuild() + " " + OpenLVersion.getURL() + " (c) " + OpenLVersion.getCopyrightYear()
                + "\n");

        Class<?> bootstrap = null;
        try {
            bootstrap = Class.forName("org.apache.catalina.startup.Bootstrap");
        } catch (ClassNotFoundException cnfe) {
            throw new Exception("\n Apache Tomcat bootstrap.jar must be in classpath.");
        }

        String whome = System.getProperty("openl.webstudio.home");

        if (whome == null) {
            whome = getProperty(args, "openl.webstudio.home");
        }
        if (whome == null) {
            whome = "..";
        }

        File webstudioHome = new File(whome);

        if (!webstudioHome.exists()) {
            throw new Exception(
                    MessageFormat
                            .format(
                                    "\nYou did not set up correctly openl.webstudio.home variable. It was \"{0}\".\n Please refer to OpenL Tablets document 'Web Programming and OpenL Tablets'. Chapter - Web Develoment Setup",
                                    whome));
        }
        System.setProperty("openl.webstudio.home", webstudioHome.getCanonicalPath());

        String chome = System.getProperty("catalina.home");

        if (chome == null) {
            chome = getProperty(args, "catalina.home");
        }
        if (chome == null) {
            // chome = "../org.openl.rules.tomcat.lib/apache-tomcat-5.5.17";
            chome = findChome();
        }
        File catalinaHome = new File(chome);

        if (!catalinaHome.exists()) {
            throw new Exception(
                    MessageFormat
                            .format(
                                    "\nYou did not set up correctly catalina.home variable. It was \"{0}\".\n Please refer to OpenL Tablets document 'Web Programming and OpenL Tablets'. Chapter - Web Develoment Setup",
                                    chome));
        }

        System.setProperty("catalina.home", catalinaHome.getCanonicalPath());

        System.out.println("Using tomcat home: " + System.getProperty("catalina.home"));

        String cbase = System.getProperty("catalina.base");

        if (cbase == null) {
            cbase = getProperty(args, "catalina.base");
        }
        if (cbase == null) {
            cbase = ".";
        }

        File catalinaBase = new File(cbase);
        System.setProperty("catalina.base", catalinaBase.getCanonicalPath());

        System.out.println("Using tomcat base: " + System.getProperty("catalina.base"));

        Method main = bootstrap.getMethod("main", new Class[] { String[].class });

        String browserURL = System.getProperty(BROWSER_URL_PROPERTY);
        if (browserURL == null) {
            browserURL = getProperty(args, BROWSER_URL_PROPERTY);
        }
        if (browserURL == null) {
            System.out.println("Using default browser url: " + DEFAULT_BROWSER_URL);
            browserURL = DEFAULT_BROWSER_URL;
        }

        new Thread(new BrowserStarter(browserURL)).start();

        main.invoke(null, new Object[] { new String[] { "start" } });

        // org.apache.catalina.startup.Bootstrap.main(new String[] { "start" });

    }
}
