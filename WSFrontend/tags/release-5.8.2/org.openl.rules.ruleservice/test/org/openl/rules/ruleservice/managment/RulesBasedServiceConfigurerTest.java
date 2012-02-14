package org.openl.rules.ruleservice.managment;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.loader.IRulesLoader;
import org.openl.rules.ruleservice.management.RulesBasedServiceConfigurer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:rules-based-configurer/rules-based-configurer-file.xml" })
public class RulesBasedServiceConfigurerTest  implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test
    public void testRulesBasedConfigurerFromFile(){
        RulesBasedServiceConfigurer configurer = applicationContext.getBean("serviceConfigurer", RulesBasedServiceConfigurer.class);
        IRulesLoader loader = applicationContext.getBean("rulesLoader", IRulesLoader.class);
        assertEquals(configurer.getServicesToBeDeployed(loader).size(),1);
        assertEquals(configurer.getServicesToBeDeployed(loader).get(0).getModulesToLoad().size(),4);
    }
}
