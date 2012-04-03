package org.openl.rules.ruleservice.databinding;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * 
 * Factory bean for adding binding types from properties to data binding context.
 * 
 * @author Marat Kamalov
 * 
 */
public class RootClassNamesBindingFactoryBean implements FactoryBean<Set<String>> {

    private final Log log = LogFactory.getLog(RootClassNamesBindingFactoryBean.class);

    private String rootClassNames;

    public void setRootClassNames(String rootClassNames) {
        this.rootClassNames = rootClassNames;
    }

    public String getRootClassNames() {
        return rootClassNames;
    }

    @Override
    public Set<String> getObject() throws Exception {
        Set<String> ret = new HashSet<String>();
        if (rootClassNames == null || rootClassNames.trim().length() == 0) {
            return ret;
        }
        String[] rootClasses = rootClassNames.split(",");
        for (String className : rootClasses) {
            if (className != null && className.trim().length() > 0) {
                String trimmedClassName = className.trim();
                ret.add(trimmedClassName);
                if (log.isInfoEnabled()) {
                    log.info(trimmedClassName + " class is added to the root class names list for WS type binding.");
                }
            }
        }

        return Collections.unmodifiableSet(ret);
    }

    @Override
    public Class<?> getObjectType() {
        return Set.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
