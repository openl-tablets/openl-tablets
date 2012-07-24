package org.openl.rules.ruleservice.publish;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MultipleRuleServicePublisherTest {

    @Test(expected = BeanCreationException.class)
    public void testBeanValidation() throws Exception {
        new ClassPathXmlApplicationContext(
                "classpath:multipleruleservicepublisher/openl-ruleservice-multipleruleservice-validation-fail-beans.xml");
    }

    @Test
    public void test() throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "classpath:multipleruleservicepublisher/openl-ruleservice-multipleruleservice-beans.xml");
        Assert.assertNotNull(applicationContext);
    }

    public static class OtherJavaClassRuleServicePublisher extends
            org.openl.rules.ruleservice.simple.JavaClassRuleServicePublisher {

    }
}
