/*
 * Created on Jun 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 *
 */
public abstract class AGenericConfiguration extends AConfigurationElement {

    public static class StringProperty {
        String name;
        String value;

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public void setName(String string) {
            name = string;
        }

        public void setValue(String string) {
            value = string;
        }

    }

    // must implement getImplementingClass()
    protected String implementingClassName;
    String classResourse; // existing class
    String fileResource;

    String urlResource;

    List<StringProperty> properties;

    // TODO will do it later
    String classPathResource;

    public void addProperty(StringProperty prop) {
        if (properties == null) {
            properties = new ArrayList<>();
        }
        properties.add(prop);
    }

    public Object createResource(IConfigurableResourceContext cxt) {

        try {

            Class<?> implementingClass = ClassFactory
                .validateClassExistsAndPublic(implementingClassName, cxt.getClassLoader(), getUri());

            Object res = ClassFactory.newInstance(implementingClass, getUri());

            if (classResourse != null) {
                Class<?> resourceClass = ClassFactory
                    .validateClassExistsAndPublic(classResourse, cxt.getClassLoader(), getUri());
                Method m = ClassFactory
                    .validateHasMethod(implementingClass, "setClassResource", new Class[] { Class.class }, getUri());
                m.invoke(res, resourceClass);
            }

            if (fileResource != null) {
                File f = new File(fileResource);
                Method m = ClassFactory
                    .validateHasMethod(implementingClass, "setFile", new Class[] { File.class }, getUri());
                m.invoke(res, f);
            }

            if (urlResource != null) {
                URL url = new URL(urlResource);
                Method m = ClassFactory
                    .validateHasMethod(implementingClass, "setURL", new Class[] { File.class }, getUri());
                m.invoke(res, url);
            }

            if (properties != null) {
                Method m = ClassFactory.validateHasMethod(implementingClass,
                    "setProperty",
                    new Class[] { String.class, String.class },
                    getUri());
                for (StringProperty prop : properties) {
                    m.invoke(res, prop.getName(), prop.getValue());
                }
            }

            return res;
        } catch (Throwable t) {
            throw RuntimeExceptionWrapper.wrap(t);
        }
    }

    public abstract Class<?> getImplementingClass();

    public void setFile(String filename) {
        fileResource = filename;
    }

    public void setImplementingClass(String classname) {
        implementingClassName = classname;
    }

    public void setResourceClass(String classname) {
        classResourse = classname;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.conf.IConfigurationElement#validate(org.openl.conf.IConfigurableResourceContext)
     */
    public void validate(IConfigurableResourceContext cxt) {
        Class<?> implementingClass = ClassFactory
            .validateClassExistsAndPublic(implementingClassName, cxt.getClassLoader(), getUri());
        ClassFactory.validateSuper(implementingClass, getImplementingClass(), getUri());
        ClassFactory.validateHaveNewInstance(implementingClass, getUri());

        if (classResourse != null) {
            ClassFactory.validateClassExistsAndPublic(classResourse, cxt.getClassLoader(), getUri());
            ClassFactory
                .validateHasMethod(implementingClass, "setClassResource", new Class[] { Class.class }, getUri());
        }

        if (fileResource != null) {
            if (!(new File(fileResource)).exists()) {
                throw new OpenConfigurationException("File " + fileResource + " does not exist", getUri(), null);
            }

            ClassFactory.validateHasMethod(implementingClass, "setFile", new Class[] { File.class }, getUri());
        }

        if (urlResource != null) {
            try {
                new URL(urlResource).openConnection();
            } catch (Throwable t) {
                throw new OpenConfigurationException("Can not connect to URL " + urlResource, getUri(), t);
            }

            ClassFactory.validateHasMethod(implementingClass, "setURL", new Class[] { URL.class }, getUri());
        }

        if (properties != null) {
            ClassFactory.validateHasMethod(implementingClass,
                "setProperty",
                new Class[] { String.class, String.class },
                getUri());
        }

    }

}
