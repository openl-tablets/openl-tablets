package org.openl.rules.ruleservice.databinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class SetToListOfClassesFactoryBean extends AbstractFactoryBean<List<Class<?>>> {

    private final Logger log = LoggerFactory.getLogger(SetToListOfClassesFactoryBean.class);

    private Set<String> setOfClassNames;

    public void setSetOfClassNames(Set<String> setOfClassNames) {
        this.setOfClassNames = setOfClassNames;
    }

    @Override
    protected List<Class<?>> createInstance() throws Exception {
        List<Class<?>> ret = new ArrayList<>();
        if (setOfClassNames != null) {
            for (String clsName : setOfClassNames) {
                try {
                    Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(clsName);
                    ret.add(cls);
                } catch (ClassNotFoundException e) {
                    log.warn(String.format("Failed to load class '%s'.", clsName));
                }
            }
        }
        return ret;
    }

    @Override
    public Class<?> getObjectType() {
        return List.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
