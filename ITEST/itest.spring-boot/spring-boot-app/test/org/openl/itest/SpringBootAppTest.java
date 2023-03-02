package org.openl.itest;

import org.junit.jupiter.api.Test;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

@SpringBootTest(classes = { SpringBootApp.class })
public class SpringBootAppTest {

    @Autowired
    private RulesFrontend frontend;

    @Test
    public void test() {
        Assert.notNull(frontend, "frontend cannot be null");
        Assert.notNull(frontend.findServiceByName("deployed-rules"), "Service is not found");
    }

}
