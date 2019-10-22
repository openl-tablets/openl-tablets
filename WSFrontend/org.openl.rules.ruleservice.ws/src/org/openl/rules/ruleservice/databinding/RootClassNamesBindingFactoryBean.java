package org.openl.rules.ruleservice.databinding;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * Factory bean for adding binding types from properties to data binding context.
 *
 * @author Marat Kamalov
 */
public class RootClassNamesBindingFactoryBean implements FactoryBean<Set<String>> {

    private final Logger log = LoggerFactory.getLogger(RootClassNamesBindingFactoryBean.class);

    private String rootClassNames;

    public void setRootClassNames(String rootClassNames) {
        this.rootClassNames = rootClassNames;
    }

    public String getRootClassNames() {
        return rootClassNames;
    }

    @Override
    public Set<String> getObject() throws Exception {
        Set<String> ret = new HashSet<>();
        if (rootClassNames == null || rootClassNames.trim().length() == 0) {
            return ret;
        }
        String[] rootClasses = rootClassNames.split(",");
        for (String className : rootClasses) {
            if (className != null && className.trim().length() > 0) {
                String trimmedClassName = className.trim();
                ret.add(trimmedClassName);
                log.info("Class '{}' has been added to the root class names list for WS type binding.",
                    trimmedClassName);
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
