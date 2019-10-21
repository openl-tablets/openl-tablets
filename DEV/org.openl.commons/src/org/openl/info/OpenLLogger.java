package org.openl.info;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class OpenLLogger {
    private final Logger logger;

    {
        String name = getName();
        logger = LoggerFactory.getLogger("OpenL." + name);
    }

    protected abstract String getName();

    public final void log() {
        if (logger.isInfoEnabled()) {
            try {
                discover();
            } catch (Exception exc) {
                logger.info("##### {} ", exc.toString());
            }
        }
    }

    protected final void log(String text) {
        logger.info(text);
    }

    protected final void log(String text, String... args) {
        logger.info(text, (Object[]) args);
    }

    protected final void log(String text, String arg1) {
        logger.info(text, arg1);
    }

    protected final void log(String text, String arg1, String arg2) {
        logger.info(text, arg1, arg2);
    }

    protected final void log(String text, Object arg1) {
        logSimpleObject(text, arg1);
        logComplexObject(arg1);
    }

    protected final void log(String text, Object arg1, Object arg2) {
        logSimpleObject(text, arg1, arg2);
        logComplexObject(arg2);
    }

    abstract protected void discover() throws Exception;

    private String toString(Object o) {
        if (o == null) {
            return "<null>";
        } else if (o.getClass().isArray()) {
            int length = Array.getLength(o);
            return "<" + o.getClass().getComponentType().getSimpleName() + "[" + length + "]>";
        } else if (o.getClass().isEnum()) {
            return "<" + o.getClass().getName() + "." + ((Enum<?>) o).name() + "]>";
        } else {
            return o.toString();
        }
    }

    private void logSimpleObject(String text, Object arg1, Object arg2) {
        logger.info(text, toString(arg1), toString(arg2));
    }

    private void logSimpleObject(String text, Object arg1) {
        logger.info(text, toString(arg1));
    }

    @SuppressWarnings("rawtypes")
    private void logComplexObject(Object value) {
        if (value instanceof Map) {
            int i = 0;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                logSimpleObject("    '{}' = {}", entry.getKey(), entry.getValue());
                if (i++ >= 50) {
                    log("    #### More than 50 elements");
                    break;
                }
            }
        } else if (value instanceof Collection) {
            int i = 0;
            for (Object item : (Collection) value) {
                logSimpleObject("    [{}] = {}", i++, item);
                if (i >= 10) {
                    log("    #### More than 10 elements");
                    break;
                }
            }
        } else if (!isSimpleType(value)) {
            BeanInfo bi = null;
            try {
                bi = Introspector.getBeanInfo(value.getClass());
            } catch (Exception e) {
                return;
            }
            PropertyDescriptor[] pds = bi.getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                String propName = pd.getName();
                if ("class".equals(propName)) {
                    continue;
                }
                try {
                    Method readMethod = pd.getReadMethod();
                    if (readMethod != null) {
                        Object propValue = readMethod.invoke(value);
                        logSimpleObject("    {} = {}", propName, propValue);
                    } else {
                        log("    {} = <no access>", propName);
                    }
                } catch (Exception ex) {
                    log("    {} = <exception>", propName);
                }
            }
        }
    }

    private boolean isSimpleType(Object o) {
        return o == null || o instanceof Number || o instanceof CharSequence || o instanceof Class<?> || o instanceof URL || o instanceof URI || o instanceof File || o instanceof Path || o instanceof Iterable || o instanceof Map || o
            .getClass()
            .isArray() || o.getClass().isEnum() || o.getClass().isAnnotation() || o.getClass().isInterface() || o
                .getClass()
                .isPrimitive() || o.getClass().getName().startsWith("java") || o.getClass()
                    .getName()
                    .startsWith("org.apache.naming");
    }
}
