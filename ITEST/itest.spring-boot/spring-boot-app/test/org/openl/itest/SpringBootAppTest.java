package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import org.openl.rules.ruleservice.simple.RulesFrontend;

@SpringBootTest(classes = {SpringBootApp.class})
public class SpringBootAppTest {

    @Autowired
    private RulesFrontend frontend;

    @Test
    public void test() {
        assertDoesNotThrow(() -> {
            Assert.notNull(frontend, "frontend cannot be null");
            Assert.notNull(frontend.findServiceByName("deployed-rules"), "Service is not found");
        });
    }

}
