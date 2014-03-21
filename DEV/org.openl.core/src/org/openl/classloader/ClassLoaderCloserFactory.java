package org.openl.classloader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.jar.JarFile;

/**
 * @author nsamatov.
 */
public final class ClassLoaderCloserFactory {
    public static ClassLoaderCloser getClassLoaderCloser() {
        return ClassLoaderCloserHolder.instance;
    }

    private ClassLoaderCloserFactory() {
    }

    private static class ClassLoaderCloserHolder {
        private static final ClassLoaderCloser instance;

        static {
            Log log = LogFactory.getLog(ClassLoaderCloserFactory.class);
            ClassLoaderCloser closer;
            try {
                Method method = URLClassLoader.class.getMethod("close");
                closer = Modifier.isPublic(method.getModifiers()) ? new Java7ClassLoaderCloser(method) : getOlderClassLoaderCloser(log);
            } catch (NoSuchMethodException e) {
                closer = getOlderClassLoaderCloser(log);
            } catch (SecurityException t) {
                if (log.isErrorEnabled()) {
                    log.error(t.getMessage(), t);
                }
                closer = new DummyClassLoaderCloser();
            }
            if (log.isInfoEnabled()) {
                log.info(String.format("ClassLoaderCloser implementation: %s", closer.getClass().getSimpleName()));
            }
            instance = closer;
        }

        private static ClassLoaderCloser getOlderClassLoaderCloser(Log log) {
            ClassLoaderCloser closer;
            String vendor = getVendor(log);
            if (log.isInfoEnabled()) {
                log.info(String.format("Your java vendor: %s", vendor));
            }

            boolean isSun = vendor != null && (vendor.startsWith("Sun") || vendor.startsWith("Oracle"));
            if (isSun) {
                closer = new SunJava6ClassLoaderCloser();
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(String.format("Your java vendor '%s' isn't supported. Some jars can be locked.", vendor));
                }
                closer = new DummyClassLoaderCloser();
            }
            return closer;
        }

        private static String getVendor(Log log) {
            String vendor = null;
            try {
                vendor = System.getProperty("java.vendor");
            } catch (SecurityException se) {
                if (log.isErrorEnabled()) {
                    log.error(se.getMessage(), se);
                }
            }
            return vendor;
        }
    }

    private static abstract class BaseClassLoaderCloser implements ClassLoaderCloser {
        @Override
        public final void close(ClassLoader classLoader) {
            if (classLoader instanceof URLClassLoader) {
                closeClassLoader((URLClassLoader) classLoader);

                if (classLoader instanceof OpenLBundleClassLoader) {
                    ((OpenLBundleClassLoader) classLoader).closeBundleClassLoaders();
                }
            }
        }

        protected abstract void closeClassLoader(URLClassLoader classLoader);
    }

    private static class Java7ClassLoaderCloser extends BaseClassLoaderCloser {
        private final Log log = LogFactory.getLog(Java7ClassLoaderCloser.class);
        private final Method close;

        private Java7ClassLoaderCloser(Method close) {
            this.close = close;
        }

        @Override
        protected void closeClassLoader(URLClassLoader classLoader) {
            try {
                close.invoke(classLoader);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(String.format("Can't close ClassLoader '%s': %s", classLoader, e.getMessage()), e);
                }
            }
        }
    }

    /**
     * Simple workaround for java 6
     *
     * @see <a href="http://management-platform.blogspot.ru/2009/01/classloaders-keeping-jar-files-open.html">http://management-platform.blogspot.ru/2009/01/classloaders-keeping-jar-files-open.html</a>
     * @see <a href="http://snipplr.com/view/24224/class-loader-which-close-opened-jar-files/">http://snipplr.com/view/24224/class-loader-which-close-opened-jar-files/</a>
     */
    private static class SunJava6ClassLoaderCloser extends BaseClassLoaderCloser {
        private final Log log = LogFactory.getLog(SunJava6ClassLoaderCloser.class);

        @Override
        protected void closeClassLoader(URLClassLoader classLoader) {
            try {
                Class clazz = URLClassLoader.class;
                Field ucp = clazz.getDeclaredField("ucp");
                ucp.setAccessible(true);
                Object sunMiscURLClassPath = ucp.get(classLoader);
                Field loaders = sunMiscURLClassPath.getClass().getDeclaredField("loaders");
                loaders.setAccessible(true);
                Object collection = loaders.get(sunMiscURLClassPath);
                for (Object sunMiscURLClassPathJarLoader : ((Collection) collection).toArray()) {
                    try {
                        Field loader = sunMiscURLClassPathJarLoader.getClass().getDeclaredField("jar");
                        loader.setAccessible(true);
                        Object jarFile = loader.get(sunMiscURLClassPathJarLoader);
                        ((JarFile) jarFile).close();
                    } catch (NoSuchFieldException ignore) {
                        // If we got this far, this is probably not a JAR loader so skip it
                    } catch (Throwable t) {
                        if (log.isErrorEnabled()) {
                            log.error(t.getMessage(), t);
                        }
                    }
                }
            } catch (Throwable t) {
                // Probably not a SUN/Oracle VM
                if (log.isErrorEnabled()) {
                    log.error(t.getMessage(), t);
                }
            }
        }
    }

    private static class DummyClassLoaderCloser extends BaseClassLoaderCloser {

        @Override
        protected void closeClassLoader(URLClassLoader classLoader) {
            // Do nothing
        }
    }
}
