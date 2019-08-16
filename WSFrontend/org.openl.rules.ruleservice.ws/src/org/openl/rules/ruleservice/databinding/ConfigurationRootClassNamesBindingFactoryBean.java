package org.openl.rules.ruleservice.databinding;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.config.AbstractFactoryBean;

public class ConfigurationRootClassNamesBindingFactoryBean extends AbstractFactoryBean<Set<String>> {

    private Set<String>[] sets;

    public ConfigurationRootClassNamesBindingFactoryBean(Set<String>... sets) {
        this.sets = sets;
    }
    
    public ConfigurationRootClassNamesBindingFactoryBean() {
        this.sets = sets;
    }

    @Override
    protected Set<String> createInstance() throws Exception {
        Set<String> ret = new HashSet<>();
        for (Set<String> s : sets) {
            if (s != null) {
                ret.addAll(s);
            }
        }
        return ret;
    }

    @Override
    public Class<?> getObjectType() {
        return Set.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

}
