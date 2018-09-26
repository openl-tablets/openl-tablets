package org.openl.info;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;

final class JndiLogger extends OpenLLogger {

    @Override
    protected String getName() {
        return "jndi";
    }

    @Override
    protected void discover() throws Exception {
        log("JNDI Context:");
        try {
            InitialContext ctx = new InitialContext();
            String path = ctx.getNameInNamespace();
            toMap(ctx, path);
        } catch (NoInitialContextException ex) {
            log("  ##### No initial JNDI context found.");
        }
    }

    private void toMap(Context ctx, String path) throws NamingException {
        NamingEnumeration<NameClassPair> list = ctx.list(path);
        if (!list.hasMoreElements()) {
            log("  {} = [Empty] {}", path, ctx.lookup(path));
            return;
        }
        while (list.hasMoreElements()) {
            NameClassPair next = list.next();
            String name = next.getName();
            String jndiPath = path + name;
            try {
                Object value = ctx.lookup(jndiPath);
                if (value instanceof Context) {
                    toMap(ctx, jndiPath + "/");
                } else {
                    log("  {} = {}", jndiPath, value);

                    if (value instanceof Number || value instanceof CharSequence) {
                        // Nothing
                    } else if (value instanceof Map) {
                        log("  {} = {}", jndiPath, value);
                        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                            log("    '{}' = {}", entry.getKey(), entry.getValue());
                        }
                    } else if (value instanceof Collection) {
                        log("  {} = {}", jndiPath, value.getClass());
                        int i = 0;
                        for (Object item : (Collection) value) {
                            log("    [{}] = {}", i++, item);
                        }
                    } else {
                        BeanInfo bi = Introspector.getBeanInfo(value.getClass());
                        PropertyDescriptor[] pds = bi.getPropertyDescriptors();
                        for (PropertyDescriptor pd : pds) {
                            String propName = pd.getName();
                            try {
                                Method readMethod = pd.getReadMethod();
                                if (readMethod != null) {
                                    Object propValue = readMethod.invoke(value);
                                    log("    {} = {}", propName, propValue);
                                } else {
                                    log("    {} = <no access>", propName);
                                }
                            } catch (Exception ex) {
                                log("    {} = <exception>", propName);
                            }
                        }
                    }
                }

            } catch (Throwable t) {
                log("  {} ! {}", jndiPath, t.toString());
            }
        }
    }
}
