package org.openl.rules.ruleservice.publish.lazy;

import static org.junit.Assert.assertFalse;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "ruleservice.datasource.dir=test-resources/LazyCompilationTest",
        "ruleservice.datasource.deploy.clean.datasource=false" })
@ContextConfiguration({ "classpath:openl-ruleservice-beans.xml" })
public class LazyCompilationTest {

    private static final Logger log = LoggerFactory.getLogger(LazyCompilationTest.class);

    private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

    private static final String SERVICE_NAME = "LazyCompilationTest_multimodule";

    private volatile boolean failed = false;
    private volatile boolean running = true;

    @Autowired
    private RulesFrontend frontend;

    @Test
    public void lazyCompilationTest() throws Exception {

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
        private final RulesFrontend frontend;
        private final CountDownLatch countDownLatch;

        LazyCompilationTestRunnable(RulesFrontend frontend, CountDownLatch countDownLatch) {
            this.frontend = frontend;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            Random rnd = new Random();
            while (running) {
                try {
                    int n = rnd.nextInt(9) + 1;
                    String lob = "module" + n;
                    try {
                        IRulesRuntimeContext cxt = RulesRuntimeContextFactory.buildRulesRuntimeContext();

                        cxt.setLob(lob);
                        Object result = frontend.execute(SERVICE_NAME, "hello", cxt);
                        if (!(lob).equals(result)) {
                            failed = true;
                            running = false;
                        }
                    } catch (Exception e) {
                        log.error("Unexpected exception", e);
                        failed = true;
                        running = false;
                    }
                } finally {
                    countDownLatch.countDown();
                }
            }
            while (countDownLatch.getCount() > 0) {
                failed = true;
                countDownLatch.countDown();
            }
        }
    }

}
