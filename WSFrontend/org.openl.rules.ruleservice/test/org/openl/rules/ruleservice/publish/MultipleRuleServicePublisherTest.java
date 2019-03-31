package org.openl.rules.ruleservice.publish;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MultipleRuleServicePublisherTest {

    @Test
    public void test() throws Exception {
        ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
            "classpath:MultipleRuleServicePublisherTest.xml");
        Assert.assertNotNull(applicationContext);
        applicationContext.close();
    }

    public static class OtherJavaClassRuleServicePublisher extends org.openl.rules.ruleservice.simple.JavaClassRuleServicePublisher {
    }
}
