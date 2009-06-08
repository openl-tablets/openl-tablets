package org.openl.rules.test.liveexcel.formula;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ThreadSafetyTest {

    public static class DeclTestThread implements Runnable {
        public void run() {
            DeclareSearchTest declareSearchTest = new DeclareSearchTest();
            declareSearchTest.testDeclare();
            declareSearchTest.testFunction();
        }
    }

    public static class CalcTestThread implements Runnable {
        public void run() {
            new LiveExcelFunctionTest().testUDF();
        }
    }

    @Test
    public void test() throws InterruptedException {
        List<Thread> threads = new ArrayList<Thread>();
        threads.add(new Thread(new CalcTestThread()));
        threads.add(new Thread(new DeclTestThread()));
        threads.add(new Thread(new CalcTestThread()));
        threads.add(new Thread(new DeclTestThread()));
        threads.add(new Thread(new CalcTestThread()));
        threads.add(new Thread(new DeclTestThread()));
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }
}
