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

package org.eclipse.jetty.server;


import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.TimerScheduler;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class LowResourcesMonitorTest
{
    QueuedThreadPool _threadPool;
    Server _server;
    ServerConnector _connector;
    LowResourceMonitor _lowResourcesMonitor;
    
    @BeforeEach
    public void before() throws Exception
    {
        _threadPool = new QueuedThreadPool();
        _threadPool.setMaxThreads(50);

        _server = new Server(_threadPool);
        _server.manage(_threadPool);

        _server.addBean(new TimerScheduler());
        
        _connector = new ServerConnector(_server);
        _connector.setPort(0);
        _connector.setIdleTimeout(35000);
        _server.addConnector(_connector);

        _server.setHandler(new DumpHandler());

        _lowResourcesMonitor=new LowResourceMonitor(_server);
        _lowResourcesMonitor.setLowResourcesIdleTimeout(200);
        _lowResourcesMonitor.setMaxConnections(20);
        _lowResourcesMonitor.setPeriod(900);
        _server.addBean(_lowResourcesMonitor);

        _server.start();
    }
    
    @AfterEach
    public void after() throws Exception
    {
        _server.stop();
    }
    
    
    @Test
    public void testLowOnThreads() throws Exception
    {
        Thread.sleep(1200);
        _threadPool.setMaxThreads(_threadPool.getThreads()-_threadPool.getIdleThreads()+10);
        Thread.sleep(1200);
        assertFalse(_lowResourcesMonitor.isLowOnResources());
        
        final CountDownLatch latch = new CountDownLatch(1);
        
        for (int i=0;i<100;i++)
        {
            _threadPool.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        latch.await();
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
        
        Thread.sleep(1200);
        assertTrue(_lowResourcesMonitor.isLowOnResources());
        
        latch.countDown();
        Thread.sleep(1200);
        assertFalse(_lowResourcesMonitor.isLowOnResources());
    }
    

    @Test
    public void testNotAccepting() throws Exception
    {
        _lowResourcesMonitor.setAcceptingInLowResources(false);
        Thread.sleep(1200);
        _threadPool.setMaxThreads(_threadPool.getThreads()-_threadPool.getIdleThreads()+10);
        Thread.sleep(1200);
        assertFalse(_lowResourcesMonitor.isLowOnResources());
        
        for (AbstractConnector c : _server.getBeans(AbstractConnector.class))
            assertThat(c.isAccepting(),Matchers.is(true));
        
        final CountDownLatch latch = new CountDownLatch(1);
        for (int i=0;i<100;i++)
        {
            _threadPool.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        latch.await();
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
        
        Thread.sleep(1200);
        assertTrue(_lowResourcesMonitor.isLowOnResources());
        for (AbstractConnector c : _server.getBeans(AbstractConnector.class))
            assertThat(c.isAccepting(),Matchers.is(false));
        
        latch.countDown();
        Thread.sleep(1200);
        assertFalse(_lowResourcesMonitor.isLowOnResources());
        for (AbstractConnector c : _server.getBeans(AbstractConnector.class))
            assertThat(c.isAccepting(),Matchers.is(true));   
    }
    

    @Disabled("not reliable")
    @Test
    public void testLowOnMemory() throws Exception
    {
        _lowResourcesMonitor.setMaxMemory(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()+(100*1024*1024));
        Thread.sleep(1200);
        assertFalse(_lowResourcesMonitor.isLowOnResources());

        byte[] data = new byte[100*1024*1024];
        Arrays.fill(data,(byte)1);
        int hash = Arrays.hashCode(data);
        assertThat(hash,not(equalTo(0)));

        Thread.sleep(1200);
        assertTrue(_lowResourcesMonitor.isLowOnResources());
        data=null;
        System.gc();
        System.gc();

        Thread.sleep(1200);
        assertFalse(_lowResourcesMonitor.isLowOnResources());
    }
    

    @Test
    public void testMaxConnectionsAndMaxIdleTime() throws Exception
    {
        _lowResourcesMonitor.setMaxMemory(0);
        assertFalse(_lowResourcesMonitor.isLowOnResources());

        Socket[] socket = new Socket[_lowResourcesMonitor.getMaxConnections()+1];
        for (int i=0;i<socket.length;i++)
            socket[i]=new Socket("localhost",_connector.getLocalPort());
        
        Thread.sleep(1200);
        assertTrue(_lowResourcesMonitor.isLowOnResources());

        try(Socket newSocket = new Socket("localhost",_connector.getLocalPort()))
        {
            // wait for low idle time to close sockets, but not new Socket
            Thread.sleep(1200);
            assertFalse(_lowResourcesMonitor.isLowOnResources());

            for (int i=0;i<socket.length;i++)
                assertEquals(-1,socket[i].getInputStream().read());

            newSocket.getOutputStream().write("GET / HTTP/1.0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            assertEquals('H',newSocket.getInputStream().read());
        }
    }
    
    @Test
    public void testMaxLowResourceTime() throws Exception
    {
        _lowResourcesMonitor.setMaxLowResourcesTime(5000);
        assertFalse(_lowResourcesMonitor.isLowOnResources());

        try(Socket socket0 = new Socket("localhost",_connector.getLocalPort()))
        {
            _lowResourcesMonitor.setMaxMemory(1);

            Thread.sleep(2400);
            assertTrue(_lowResourcesMonitor.isLowOnResources());

            try(Socket socket1 = new Socket("localhost",_connector.getLocalPort()))
            {
                Thread.sleep(2400);
                assertTrue(_lowResourcesMonitor.isLowOnResources());
                assertEquals(-1,socket0.getInputStream().read());
                socket1.getOutputStream().write("G".getBytes(StandardCharsets.UTF_8));

                Thread.sleep(2400);
                assertTrue(_lowResourcesMonitor.isLowOnResources());
                socket1.getOutputStream().write("E".getBytes(StandardCharsets.UTF_8));

                Thread.sleep(2400);
                assertTrue(_lowResourcesMonitor.isLowOnResources());
                assertEquals(-1,socket1.getInputStream().read());
            }
        }
    }
}
