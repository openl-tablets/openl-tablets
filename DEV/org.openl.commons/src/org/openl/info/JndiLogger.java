package org.openl.info;

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
                }

            } catch (Throwable t) {
                log("  {} ! {}", jndiPath, t.toString());
            }
        }
    }
}
