package org.openl.rules.ruleservice.conf;

import java.util.Collection;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class LazyInitBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private Collection<String> lazyBeanNames;

    public Collection<String> getLazyBeanNames() {
        return lazyBeanNames;
    }

    public void setLazyBeanNames(Collection<String> lazyBeanNames) {
        this.lazyBeanNames = lazyBeanNames;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            if (lazyBeanNames.contains(beanName)) {
                beanFactory.getBeanDefinition(beanName).setLazyInit(true);
            }
        }
    }
}
