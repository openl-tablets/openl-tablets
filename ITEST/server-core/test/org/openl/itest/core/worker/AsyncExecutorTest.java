package org.openl.itest.core.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class AsyncExecutorTest {

    @Test
    public void testMultithreadExecution() throws InterruptedException {
        final int nThread = 3;
        ThreadInvocationCaptor threadCaptor = new ThreadInvocationCaptor(3, nThread);

        Runnable taskMock = mock(Runnable.class);
        doAnswer(args -> {
            threadCaptor.capture();
            return null;
        }).when(taskMock).run();

        AsyncExecutor executor = new AsyncExecutor(nThread, taskMock);
        executor.start();
        List<Throwable> errors;
        try {
            threadCaptor.await(5, TimeUnit.SECONDS);
        } finally {
            errors = executor.stop();
        }
        assertEquals(0, errors.size());
    }

    @Test
    public void testCriticalError() throws InterruptedException {
        final int nThread = 3;
        final int atLeast = 3;
        ThreadInvocationCaptor threadCaptor = new ThreadInvocationCaptor(atLeast, nThread);

        Runnable taskMock = mock(Runnable.class);
        doAnswer(args -> {
            threadCaptor.capture();
            fail("for test purpose");
            return null;
        }).when(taskMock).run();

        AsyncExecutor executor = new AsyncExecutor(nThread, taskMock);
        executor.start();
        List<Throwable> errors;
        try {
            threadCaptor.await(5, TimeUnit.SECONDS);
        } finally {
            errors = executor.stop();
        }
        assertTrue(errors.size() >= nThread * atLeast);
        assertTrue(errors.stream().allMatch(AssertionError.class::isInstance));
    }

    @Test
    public void testTimeout() throws InterruptedException {
        CountDownLatch waitToStart = new CountDownLatch(1);
        Runnable taskMock = mock(Runnable.class);
        doAnswer(args -> {
            waitToStart.countDown();
            TimeUnit.SECONDS.sleep(10);
            return null;
        }).when(taskMock).run();

        AsyncExecutor executor = new AsyncExecutor(taskMock);
        executor.start();
        waitToStart.await(1, TimeUnit.SECONDS);
        List<Throwable> errors = executor.stop(1, TimeUnit.MILLISECONDS);

        assertEquals(1, errors.size());
        assertTrue(errors.stream().allMatch(InterruptedException.class::isInstance));
        verify(taskMock, times(1)).run();
    }

}
