package org.openl.itest.core.worker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class AsyncExecutorTest {

    @Test
    public void testMultithreadExecution() {
        final int nThread = 3;
        ThreadInvocationCaptor threadCaptor = new ThreadInvocationCaptor(3, nThread);

        Runnable taskMock = mock(Runnable.class);
        doAnswer(args -> {
            threadCaptor.capture();
            return null;
        }).when(taskMock).run();

        AsyncExecutor executor = new AsyncExecutor(nThread, taskMock);
        executor.start();
        boolean errors;
        try {
            threadCaptor.await(5);
        } finally {
            errors = executor.stop();
        }
        assertFalse(errors);
    }

    @Test
    public void testCriticalError() {
        final int nThread = 3;
        ThreadInvocationCaptor threadCaptor = new ThreadInvocationCaptor(1, nThread);

        Runnable taskMock = mock(Runnable.class);
        doAnswer(args -> {
            threadCaptor.capture();
            fail("for test purpose");
            return null;
        }).when(taskMock).run();

        AsyncExecutor executor = new AsyncExecutor(nThread, taskMock);
        executor.start();
        boolean errors;
        try {
            threadCaptor.await(5);
        } finally {
            errors = executor.stop();
        }
        assertTrue(errors);
    }

    @Test
    public void testTimeout() {
        CountDownLatch waitToStart = new CountDownLatch(1);
        Runnable taskMock = mock(Runnable.class);
        doAnswer(args -> {
            waitToStart.countDown();
            TimeUnit.SECONDS.sleep(10);
            return null;
        }).when(taskMock).run();

        AsyncExecutor executor = AsyncExecutor.start(taskMock);
        try {
            waitToStart.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        boolean error = executor.stop(1, TimeUnit.SECONDS);

        assertTrue(error);
        verify(taskMock, times(1)).run();
    }

}
