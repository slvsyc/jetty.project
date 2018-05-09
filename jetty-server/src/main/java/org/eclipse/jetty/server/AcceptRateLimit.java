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

import java.nio.channels.SelectableChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.SelectorManager;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.Container;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.statistic.RateStatistic;
import org.eclipse.jetty.util.thread.Scheduler;

/**
 * <p>A Listener that limits the rate at which new connections are accepted</p>
 * <p>
 * If the limits are exceeded, accepting is suspended for the time period, so 
 * incoming connections are held in the operating system accept queue (no syn ack sent),
 * where they may either timeout or wait for the server to resume accepting.
 * It can be applied to an entire server or to a specific connector by adding it 
 * via {@link Container#addBean(Object)}
 * </p>
 * <p>
 * <b>Usage:</b>
 * </p>
 * <pre>
 *   Server server = new Server();
 *   server.addBean(new AcceptLimit(100,5,TimeUnit.SECONDS,server));
 *   ...
 *   server.start();
 * </pre>
 * @see SelectorManager.AcceptListener
 */
@ManagedObject
public class AcceptRateLimit extends AbstractLifeCycle implements SelectorManager.AcceptListener, Runnable
{
    private static final Logger LOG = Log.getLogger(AcceptRateLimit.class);
    
    private final Server _server;
    private final List<AbstractConnector> _connectors = new ArrayList<>();
    private final Rate _rate;
    private final int _maxRate;
    private boolean _limiting = false;

    public AcceptRateLimit(@Name("maxRate") int maxRate, @Name("period") long period, @Name("units") TimeUnit units, @Name("server") Server server)
    {
        _server = server;
        _maxRate = maxRate;
        _rate = new Rate(period,units);
    }
    
    public AcceptRateLimit(@Name("maxRate") int maxRate, @Name("period") long period, @Name("units") TimeUnit units, @Name("connectors") Connector...connectors)
    {
        _server = null;
        _maxRate = maxRate;
        _rate = new Rate(period,units); 
        for (Connector c: connectors)
        {
            if (c instanceof AbstractConnector)
                _connectors.add((AbstractConnector)c);
            else
                LOG.warn("Connector {} is not an AbstractConnection. Connections not limited",c);
        }
    }
    
    protected void age(long period, TimeUnit units)
    {
        _rate.age(period,units);
    }
    
    @Override
    protected synchronized void doStart() throws Exception
    {
        if (_server!=null)
        {
            for (Connector c: _server.getConnectors())
            {
                if (c instanceof AbstractConnector)
                    _connectors.add((AbstractConnector)c);
                else
                    LOG.warn("Connector {} is not an AbstractConnection. Connections not limited",c);
            }
        }

        if (LOG.isDebugEnabled())
            LOG.debug("AcceptLimit accept<{} rate<{} in {} for {}", _maxRate, _rate,_connectors);
        
        _limiting = false;
        
        for (AbstractConnector c : _connectors)
            c.addBean(this);
    }

    @Override
    protected synchronized void doStop() throws Exception
    {
        for (AbstractConnector c : _connectors)
            c.removeBean(this);
        if (_server!=null)
            _connectors.clear();   
    }

    protected void limit()
    {
        for (AbstractConnector c : _connectors)
        {
            c.setAccepting(false);
        }
        
        Scheduler scheduler = _connectors.get(0).getScheduler();
        scheduler.schedule(this,_rate.getPeriod(),_rate.getUnits());
    }
    
    protected void unlimit()
    {
        for (AbstractConnector c : _connectors)
        {
            c.setAccepting(true);
        }
    }    

    @Override
    public synchronized void onAccepting(SelectableChannel channel)
    {
        int rate = _rate.record();
        
        if (LOG.isDebugEnabled())
        {   
            LOG.debug("onAccepting rate {}/{} for {} {}",
                    rate,_maxRate,
                    _rate,channel);                    
        }
        
        if (rate>_maxRate)
        {
            if (!_limiting)
            {
                _limiting = true;

                LOG.warn("AcceptLimit rate exceeded {}>{} on {}",
                        rate,_maxRate,
                        _connectors);
                limit();
            }
        }
    }

    @Override
    public synchronized void onAcceptFailed(@SuppressWarnings("unused") SelectableChannel channel, @SuppressWarnings("unused") Throwable cause)
    {
    }

    @Override
    public synchronized void onAccepted(@SuppressWarnings("unused") SelectableChannel channel, @SuppressWarnings("unused") EndPoint endPoint)
    {
    }

    @Override
    public synchronized void run()
    {
        int rate = _rate.getRate();
        if (rate>_maxRate)
        {
            Scheduler scheduler = _connectors.get(0).getScheduler();
            scheduler.schedule(this,_rate.getPeriod(),_rate.getUnits());
            return;
        }
        
        if (_limiting)
        {
            _limiting = false;
            LOG.warn("AcceptLimit OK rate {}<={} on {}",
                    rate,_maxRate,
                    _connectors);
            unlimit();
        }
    }

    private final class Rate extends RateStatistic
    {
        private Rate(long period, TimeUnit units)
        {
            super(period,units);
        }

        @Override
        protected void age(long period, TimeUnit units)
        {
            super.age(period,units);
        }
    }
}
