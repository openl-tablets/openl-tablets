package org.openl.rules.ruleservice;

import org.openl.rules.repository.exceptions.RRepositoryException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Application main class. Sets up other classes.
 */
public class RuleServiceStarter {
    public static void main(String[] args) throws RRepositoryException {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("openl-ws.xml");
        RuleService ruleService = (RuleService) applicationContext.getBean("ruleService");
        PeriodicalExecutor executor = new PeriodicalExecutor(ruleService);
        executor.execute();
    }
}
