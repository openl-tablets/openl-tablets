package org.openl.rules.ruleservice.publish;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;



import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.rmi.DefaultRmiHandler;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:DefaultRmiHandlerTest/openl-ruleservice-beans.xml" })
public class DefaultRmiHandlerTest implements ApplicationContextAware{
    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @Test
    public void test() throws Exception {
        Assert.assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        Assert.assertNotNull(serviceManager);
        serviceManager.start();
        
        Registry registry = LocateRegistry.getRegistry(1099);
        DefaultRmiHandler defaultRmiHandler = (DefaultRmiHandler)registry.lookup("DefaultRmiHandlerTest/simpleProject");
        
        Assert.assertNotNull(defaultRmiHandler);
        
        String result = (String) defaultRmiHandler.execute("baseHello", new Class<?>[]{int.class}, new Object[] { new Integer(10) });
        
        Assert.assertEquals("Good Morning", result);
        
    }
}
