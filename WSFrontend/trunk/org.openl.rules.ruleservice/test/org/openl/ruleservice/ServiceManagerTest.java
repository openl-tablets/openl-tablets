package org.openl.ruleservice;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.ruleservice.management.ServiceManager;
import org.openl.ruleservice.simple.RulesFrontend;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:openl-ruleservice-beans.xml"})
public class ServiceManagerTest implements ApplicationContextAware{
    
    private ApplicationContext applicationContext;
    
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @Test
    public void testServiceManager(){
       assertNotNull(applicationContext);
       ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
       assertNotNull(serviceManager);
       serviceManager.start();
       RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
       assertNotNull(frontend);                                         
       Object object = frontend.execute("org.openl.tablets.tutorial4", "vehicleEligibilityScore", new Object[]{new DefaultRulesRuntimeContext(),"Provisional"});
       System.out.println(object instanceof org.openl.meta.DoubleValue);
       org.openl.meta.DoubleValue value = (org.openl.meta.DoubleValue) object;
       assertEquals(50.0, value.getValue(), 0.01);
    }
}
