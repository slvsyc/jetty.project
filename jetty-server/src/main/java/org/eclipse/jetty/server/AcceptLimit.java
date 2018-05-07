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

/**
 * <p>A Listener that limits the number of connection acceptances per period of time.</p>
 * <p>
 * If the limit is exceeded, accepting is suspended for the time period.
 * It can be applied to an entire server or to a specific connector by adding it 
 * via {@link Container#addBean(Object)}
 * </p>
 * <p>
 * <b>Usage:</b>
 * <pre>
 *   Server server = new Server();
 *   server.addBean(new AcceptLimit(100,1,TimeUnit.SECOND,server));
 *   ...
 *   server.start();
 * </pre>
 * </p>
 * @see SelectorManager.AcceptListener
 */
@ManagedObject
public class AcceptLimit extends AbstractLifeCycle implements SelectorManager.AcceptListener
{
    private static final Logger LOG = Log.getLogger(AcceptLimit.class);
    
    private final Server _server;
    private final List<AbstractConnector> _connectors = new ArrayList<>();
    private final Set<SelectableChannel> _accepting = new HashSet<>();
    private final long _period;
    private final TimeUnit _units;
    private int _maxAccepts;
    private boolean _limiting = false;
    private long _sinceNanos = 0;

    public AcceptLimit(@Name("maxAccepts") int maxAccepts, @Name("period") long period, @Name("units") TimeUnit units, @Name("server") Server server)
    {
        _server = server;
        _period = period;
        _units = units;
        _maxAccepts = maxAccepts;
    }
    
    public AcceptLimit(@Name("maxAccepts") int maxAccepts, @Name("period") long period, @Name("units") TimeUnit units, @Name("connectors") Connector...connectors)
    {
        _server = null;
        for (Connector c: connectors)
        {
            if (c instanceof AbstractConnector)
                _connectors.add((AbstractConnector)c);
            else
                LOG.warn("Connector {} is not an AbstractConnection. Connections not limited",c);
        }
        _period = period;
        _units = units;
        _maxAccepts = maxAccepts;
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
            LOG.debug("AcceptLimit {} in {} {} for {}",_maxAccepts, _period, _units,_connectors);
        
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
        if (_accepting.size()==0)
            _sinceNanos = System.nanoTime();
        _accepting.add(channel);
        
        if (LOG.isDebugEnabled())
        {
            long period = System.nanoTime()-_sinceNanos;
            LOG.debug("onAccepting {} in {} {} < {} {}",_accepting.size(),_units.convert(period,TimeUnit.NANOSECONDS),_units,_maxAccepts, channel);
        }
    }

    @Override
    public synchronized void onAcceptFailed(SelectableChannel channel, @SuppressWarnings("unused") Throwable cause)
    {
        _accepting.remove(channel);
    }

    @Override
    public synchronized void onAccepted(SelectableChannel channel, @SuppressWarnings("unused") EndPoint endPoint)
    {
        _accepting.remove(channel);
    }
    

}
