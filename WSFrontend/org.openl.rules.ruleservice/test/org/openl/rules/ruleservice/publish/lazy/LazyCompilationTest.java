package org.openl.rules.ruleservice.publish.lazy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "ruleservice.datasource.dir=test-resources/LazyCompilationTest",
        "ruleservice.datasource.deploy.clean.datasource=false" })
@ContextConfiguration({ "classpath:openl-ruleservice-beans.xml" })
public class LazyCompilationTest {

    private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

    private static final String SERVICE_NAME = "LazyCompilationTest_multimodule";

    private ApplicationContext applicationContext;

    private volatile boolean failed = false;
    private volatile boolean running = true;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test
    public void lazyCompilationTest() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);

        Thread[] threads = new Thread[MAX_THREADS];
        CountDownLatch countDownLatch = new CountDownLatch(2000);
        for (int i = 0; i < MAX_THREADS; i++) {
            threads[i] = new Thread(new LazyCompilationTestRunnable(frontend, countDownLatch));
            threads[i].start();
        }

        countDownLatch.await();
        running = false;
        for (int i = 0; i < MAX_THREADS; i++) {
            threads[i].join();
        }
        assertFalse(failed);
    }

    public class LazyCompilationTestRunnable implements Runnable {
        private RulesFrontend frontend;
        private CountDownLatch countDownLatch;

        public LazyCompilationTestRunnable(RulesFrontend frontend, CountDownLatch countDownLatch) {
            this.frontend = frontend;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            Random rnd = new Random();
            while (running) {
                int n = rnd.nextInt(9) + 1;
                try {
                    IRulesRuntimeContext cxt = RulesRuntimeContextFactory.buildRulesRuntimeContext();
                    cxt.setLob("module" + n);
                    if (!("module" + n).equals(frontend.execute(SERVICE_NAME, "hello", new Object[] { cxt }))) {
                        failed = true;
                        running = false;
                    }
                } catch (Exception e) {
                    failed = true;
                    running = false;
                }
                countDownLatch.countDown();
            }
        }
    }

}
