package org.openl.rules.ruleservice.conf;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.conf.RulesBasedServiceConfigurer;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:RulesBasedServiceConfigurerTest/openl-ruleservice-beans.xml" })
public class RulesBasedServiceConfigurerTest implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test
    public void testRulesBasedConfigurerFromFile() {
        RulesBasedServiceConfigurer configurer = applicationContext.getBean(RulesBasedServiceConfigurer.class);
        assertNotNull(configurer);
        RuleServiceLoader loader = applicationContext.getBean(RuleServiceLoader.class);
        assertNotNull(loader);
        assertEquals(1, configurer.getServicesToBeDeployed(loader).size());
        assertEquals(4, configurer.getServicesToBeDeployed(loader).iterator().next().getModules().size());
    }
}
