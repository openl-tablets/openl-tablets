package org.openl.itest.core.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

public class TaskSchedulerTest {

    private TaskScheduler scheduler;

    @Before
    public void setUp() {
        scheduler = new TaskScheduler();
    }

    @Test
    public void testSubmit() throws InterruptedException {
        ThreadInvocationCaptor threadCaptor = new ThreadInvocationCaptor(1, 1);

        Runnable command1 = mock(Runnable.class);
        doAnswer(args -> {
            threadCaptor.capture();
            return null;
        }).when(command1).run();

        Runnable command2 = mock(Runnable.class);
        doAnswer(args -> {
            threadCaptor.capture();
            return null;
        }).when(command2).run();

        scheduler.schedule(command1, 1, TimeUnit.MILLISECONDS);
        scheduler.schedule(command2, 5, TimeUnit.MILLISECONDS);
        List<Throwable> errors = scheduler.await();
        threadCaptor.await(5, TimeUnit.SECONDS);

        assertTrue(errors.isEmpty());
        verify(command1, times(1)).run();
        verify(command2, times(1)).run();
    }

    @Test
    public void testSubmitNegative() throws InterruptedException {
        ThreadInvocationCaptor threadCaptor = new ThreadInvocationCaptor(2, 1);

        Runnable command1 = mock(Runnable.class);
        doAnswer(args -> {
            threadCaptor.capture();
            return null;
        }).when(command1).run();

        scheduler.schedule(command1, 1, TimeUnit.MILLISECONDS);
        List<Throwable> errors = scheduler.await();
        try {
            threadCaptor.await(1, TimeUnit.SECONDS);
            fail("Must be executed only once!");
        } catch (AssertionError e) {
            assertEquals("Each thread must be executed at least '2' times", e.getMessage());
            verify(command1, times(1)).run();
            assertTrue(errors.isEmpty());
        }
    }

    @Test
    public void testCriticalError() throws InterruptedException {
        ThreadInvocationCaptor threadCaptor = new ThreadInvocationCaptor(1, 1);

        Runnable command1 = mock(Runnable.class);
        doAnswer(args -> {
            threadCaptor.capture();
            fail("for test purpose");
            return null;
        }).when(command1).run();

        scheduler.schedule(command1, 1, TimeUnit.MILLISECONDS);
        List<Throwable> errors = scheduler.await();
        threadCaptor.await(5, TimeUnit.SECONDS);

        assertEquals(1, errors.size());
        assertTrue(errors.stream().allMatch(AssertionError.class::isInstance));
        verify(command1, times(1)).run();
    }

    @Test
    public void testTimeout() throws InterruptedException {
        ThreadInvocationCaptor threadCaptor = new ThreadInvocationCaptor(1, 1);

        Runnable command1 = mock(Runnable.class);
        doAnswer(args -> {
            threadCaptor.capture();
            TimeUnit.SECONDS.sleep(10);
            return null;
        }).when(command1).run();

        scheduler.schedule(command1, 1, TimeUnit.NANOSECONDS);
        List<Throwable> errors = scheduler.await(1, TimeUnit.SECONDS);
        threadCaptor.await(5, TimeUnit.SECONDS);

        assertEquals(1, errors.size());
        assertTrue(errors.stream().allMatch(InterruptedException.class::isInstance));
        verify(command1, times(1)).run();
    }

}
