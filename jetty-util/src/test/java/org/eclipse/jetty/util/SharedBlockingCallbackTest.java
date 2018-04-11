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

package org.eclipse.jetty.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.util.SharedBlockingCallback.Blocker;
import org.junit.jupiter.api.Test;

public class SharedBlockingCallbackTest
{
    final AtomicInteger notComplete = new AtomicInteger();
    final SharedBlockingCallback sbcb= new SharedBlockingCallback()
    {
        @Override
        protected long getIdleTimeout()
        {
            return 150;
        }

        @Override
        protected void notComplete(Blocker blocker)
        {
            super.notComplete(blocker);
            notComplete.incrementAndGet();
        }

    };
    
    public SharedBlockingCallbackTest()
    {
    }
    
    
    @Test
    public void testDone() throws Exception
    { 
        long start;
        try (Blocker blocker=sbcb.acquire())
        {
            blocker.succeeded();
            start=TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
            blocker.block();
        }
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime())-start,lessThan(500L));
        assertEquals(0,notComplete.get());
    }
    
    @Test
    public void testGetDone() throws Exception
    {
        long start;
        try (final Blocker blocker=sbcb.acquire())
        {
            final CountDownLatch latch = new CountDownLatch(1);

            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    latch.countDown();
                    try{TimeUnit.MILLISECONDS.sleep(100);}catch(Exception e){e.printStackTrace();}
                    blocker.succeeded();
                }
            }).start();

            latch.await();
            start=TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
            blocker.block();
        }
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime())-start,greaterThan(10L));
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime())-start,lessThan(1000L));
        assertEquals(0,notComplete.get());
    }
    
    @Test
    public void testFailed() throws Exception
    {
        final Exception ex = new Exception("FAILED");
        long start=Long.MIN_VALUE;
        try
        {
            try (final Blocker blocker=sbcb.acquire())
            {
                blocker.failed(ex);
                blocker.block();
            }
            fail("Should have thrown IOException");
        }
        catch(IOException ee)
        {
            start=TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
            assertEquals(ex,ee.getCause());
        }
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime())-start,lessThan(100L));
        assertEquals(0,notComplete.get());
    }
    
    @Test
    public void testGetFailed() throws Exception
    {
        final Exception ex = new Exception("FAILED");
        long start=Long.MIN_VALUE;
        final CountDownLatch latch = new CountDownLatch(1);

        try
        {
            try (final Blocker blocker=sbcb.acquire())
            {

                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        latch.countDown();
                        try{TimeUnit.MILLISECONDS.sleep(100);}catch(Exception e){e.printStackTrace();}
                        blocker.failed(ex);
                    }
                }).start();

                latch.await();
                start=TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
                blocker.block();
            }
            fail("Should have thrown IOException");
        }
        catch(IOException ee)
        {
            assertEquals(ex,ee.getCause());
        }
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime())-start,greaterThan(10L));
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime())-start,lessThan(1000L));
        assertEquals(0,notComplete.get());
    }


    @Test
    public void testAcquireBlocked() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    try (Blocker blocker=sbcb.acquire())
                    {
                        latch.countDown();
                        TimeUnit.MILLISECONDS.sleep(100);
                        blocker.succeeded();
                        blocker.block();
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }).start();
        
        
        latch.await();
        long start=TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        try (Blocker blocker=sbcb.acquire())
        {
            assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime())-start,greaterThan(10L));
            assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime())-start,lessThan(500L));

            blocker.succeeded();
            blocker.block();
        }
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime())-start,lessThan(600L));
        assertEquals(0,notComplete.get());
    }

    @Test
    public void testBlockerClose() throws Exception
    {
        try (Blocker blocker=sbcb.acquire())
        {
            SharedBlockingCallback.LOG.info("Blocker not complete "+blocker+" warning is expected...");
        }
        
        assertEquals(1,notComplete.get());
    }
    
    @Test
    public void testBlockerTimeout() throws Exception
    {
        Blocker b0=null;
        try
        {
            try (Blocker blocker=sbcb.acquire())
            {
                b0=blocker;
                Thread.sleep(400);
                blocker.block();
            }
            fail("Should have thrown IOException");
        }
        catch(IOException e)
        {
            Throwable cause = e.getCause();
            assertThat(cause,instanceOf(TimeoutException.class));
        }
        
        assertEquals(0,notComplete.get());
        

        try (Blocker blocker=sbcb.acquire())
        {
            assertThat(blocker,not(equalTo(b0)));
            try
            {
                b0.succeeded();
                fail("Should have thrown IllegalStateException");
            }
            catch(Exception e)
            {
                assertThat(e,instanceOf(IllegalStateException.class));
                assertThat(e.getCause(),instanceOf(TimeoutException.class));
            }
            blocker.succeeded();
        }
    }
}
