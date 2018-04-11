//
//  ========================================================================
//  Copyright (c) 1995-2018 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.util.thread;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.util.log.StacklessLogging;
import org.hamcrest.Matchers;
import org.junit.After;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;


@RunWith(value = Parameterized.class)
public class SchedulerTest
{
    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        Object[][] data = new Object[][]{
            {new TimerScheduler()},
            {new ScheduledExecutorScheduler()}/*,
            {new ConcurrentScheduler(0)},
            {new ConcurrentScheduler(1500)},
            {new ConcurrentScheduler(executor,1500)}*/
        };
        return Arrays.asList(data);
    }

    private Scheduler _scheduler;

    public SchedulerTest(Scheduler scheduler)
    {
        _scheduler=scheduler;
    }

    @Before
    public void before() throws Exception
    {
        System.gc();
        _scheduler.start();
    }

    @After
    public void after() throws Exception
    {
        _scheduler.stop();
    }

    @Test
    public void testExecution() throws Exception
    {
        final AtomicLong executed = new AtomicLong();
        long expected=System.currentTimeMillis()+1000;
        Scheduler.Task task=_scheduler.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                executed.set(System.currentTimeMillis());
            }
        },1000,TimeUnit.MILLISECONDS);

        Thread.sleep(1500);
        assertFalse(task.cancel());
        assertThat(executed.get(),Matchers.greaterThanOrEqualTo(expected));
        assertThat(expected-executed.get(),Matchers.lessThan(1000L));
    }

    @Test
    public void testTwoExecution() throws Exception
    {
        final AtomicLong executed = new AtomicLong();
        long expected=System.currentTimeMillis()+1000;
        Scheduler.Task task=_scheduler.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                executed.set(System.currentTimeMillis());
            }
        },1000,TimeUnit.MILLISECONDS);

        Thread.sleep(1500);
        assertFalse(task.cancel());
        assertThat(executed.get(),Matchers.greaterThanOrEqualTo(expected));
        assertThat(expected-executed.get(),Matchers.lessThan(1000L));

        final AtomicLong executed1 = new AtomicLong();
        long expected1=System.currentTimeMillis()+1000;
        Scheduler.Task task1=_scheduler.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                executed1.set(System.currentTimeMillis());
            }
        },1000,TimeUnit.MILLISECONDS);

        Thread.sleep(1500);
        assertFalse(task1.cancel());
        assertThat(executed1.get(),Matchers.greaterThanOrEqualTo(expected1));
        assertThat(expected1-executed1.get(),Matchers.lessThan(1000L));
    }

    @Test
    public void testQuickCancel() throws Exception
    {
        final AtomicLong executed = new AtomicLong();
        Scheduler.Task task=_scheduler.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                executed.set(System.currentTimeMillis());
            }
        },2000,TimeUnit.MILLISECONDS);

        Thread.sleep(100);
        assertTrue(task.cancel());
        Thread.sleep(2500);
        assertEquals(0,executed.get());
    }

    @Test
    public void testLongCancel() throws Exception
    {
        final AtomicLong executed = new AtomicLong();
        Scheduler.Task task=_scheduler.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                executed.set(System.currentTimeMillis());
            }
        },2000,TimeUnit.MILLISECONDS);

        Thread.sleep(100);
        assertTrue(task.cancel());
        Thread.sleep(2500);
        assertEquals(0,executed.get());
    }

    @Test
    public void testTaskThrowsException() throws Exception
    {
        try (StacklessLogging stackless = new StacklessLogging(TimerScheduler.class))
        {
            long delay = 500;
            _scheduler.schedule(new Runnable()
            {
                @Override
                public void run()
                {
                    throw new RuntimeException("Thrown by testTaskThrowsException");
                }
            }, delay, TimeUnit.MILLISECONDS);

            TimeUnit.MILLISECONDS.sleep(2 * delay);

            // Check whether after a task throwing an exception, the scheduler is still working

            final CountDownLatch latch = new CountDownLatch(1);
            _scheduler.schedule(new Runnable()
            {
                @Override
                public void run()
                {
                    latch.countDown();
                }
            }, delay, TimeUnit.MILLISECONDS);

            assertTrue(latch.await(2 * delay, TimeUnit.MILLISECONDS));
        }
    }
}
