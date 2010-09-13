package org.openl.conf;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.Stack;

import org.openl.ICompileContext;
import org.openl.OpenL;
import org.openl.binding.impl.Binder;
import org.openl.exception.OpenLRuntimeException;
import org.openl.impl.DefaultCompileContext;
import org.openl.syntax.impl.Parser;
import org.openl.util.Log;
import org.openl.vm.SimpleVM;

public abstract class AOpenLBuilder extends BaseOpenLBuilder {

    static class UserContextStack extends ThreadLocal<Stack<IUserContext>> {

        /**
         *
         */

        @Override
        protected Stack<IUserContext> initialValue() {
            return new Stack<IUserContext>();
        }

        public IUserContext pop() {
            return stack().pop();
        }

        public void push(IUserContext ucxt) {
            stack().push(ucxt);
        }

        protected Stack<IUserContext> stack() {
            return get();
        }

        public IUserContext top() {
            return stack().peek();
        }

    }

    static public UserContextStack userCxt = new UserContextStack();

    boolean inheritExtendedConfigurationLoader = false;

    public OpenL build(String openl) throws OpenConfigurationException {
        OpenL op = new OpenL();
        boolean changedClassLoader = false;
        ClassLoader oldClassLoader = null;

        try {
            userCxt.push(getUserEnvironmentContext());

            ClassLoader myClassLoader = myClassLoader();

            oldClassLoader = Thread.currentThread().getContextClassLoader();

            if (oldClassLoader != myClassLoader) {
                Thread.currentThread().setContextClassLoader(myClassLoader);
                changedClassLoader = true;
            }

            UserContext mycxt = new UserContext(myClassLoader, getUserEnvironmentContext().getUserHome());

            NoAntOpenLTask naot = getNoAntOpenLTask();

            naot.setInheritExtendedConfigurationLoader(inheritExtendedConfigurationLoader);
            if (inheritExtendedConfigurationLoader) {
                naot.execute(getUserEnvironmentContext(), getUserEnvironmentContext().getUserHome());
            } else {
                naot.execute(mycxt, getUserEnvironmentContext().getUserHome());
            }

            // OpenLConfiguration conf =
            // (OpenLConfiguration)helper.getConfigurationObject(
            // getAntProjectConfigurationVariable(openl));

            IOpenLConfiguration conf = NoAntOpenLTask.retrieveConfiguration();

            op.setParser(new Parser(conf));

            op.setBinder(new Binder(conf, conf, conf, conf, conf, op));
            op.setVm(new SimpleVM());
            op.setCompileContext(buildCompileContext());
        } catch (Exception ex) {
            throw new OpenLRuntimeException(ex);
        } finally {
            if (changedClassLoader) {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
            userCxt.pop();
        }
        return op;
    }

    private ICompileContext buildCompileContext() {
        ICompileContext compileContext = new DefaultCompileContext();

        IConfigurableResourceContext resourceContext = getResourceContext();

        if (resourceContext != null) {
            String propertyValue = resourceContext.findProperty("validation");

            if (propertyValue != null) {
                Boolean value = Boolean.valueOf(propertyValue);
                compileContext.setValidationEnabled(value);
            }
        }

        return compileContext;
    }

    public abstract NoAntOpenLTask getNoAntOpenLTask();

    /**
     * @param openl
     */
    protected Properties getProperties(String openl) {
        URL url = getResourceContext().findClassPathResource(openl.replace('.', '/') + '/' + openl + ".ant.properties");
        if (url == null) {
            return null;
        }
        InputStream is = null;
        try {
            is = url.openStream();
            Properties p = new Properties();
            p.load(is);
            return p;
        } catch (IOException e) {
            throw new OpenLRuntimeException(e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Throwable t) {
                Log.error("Error closing stream", t);
            }
        }

    }

    ClassLoader myClassLoader() {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        String myName = getClass().getName();
        try {
            oldClassLoader.loadClass(myName);
            return oldClassLoader;
        } catch (ClassNotFoundException e) {
            return getClass().getClassLoader();
        }

    }

    public void setInheritExtendedConfigurationLoader(boolean inheritExtendedConfigurationLoader) {
        this.inheritExtendedConfigurationLoader = inheritExtendedConfigurationLoader;
    }

    public boolean isInheritExtendedConfigurationLoader() {
        return inheritExtendedConfigurationLoader;
    }

}
