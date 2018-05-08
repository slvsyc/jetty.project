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

package org.eclipse.jetty.util.statistic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

/**
 */
public class RateStatistic
{
    private final Deque<Long> _samples = new ArrayDeque<>();
    private final long _nanoPeriod;
    private final TimeUnit _units;
    private long _max;
    private long _count;

    public RateStatistic(long period, TimeUnit units)
    {
        _nanoPeriod = TimeUnit.NANOSECONDS.convert(period,units);    
        _units = units;
    }
    
    public long getPeriod()
    {
        return _units.convert(_nanoPeriod,TimeUnit.NANOSECONDS);
    }
    
    public TimeUnit getUnits()
    {
        return _units;
    }
    
    /**
     * Resets the statistics.
     */
    public void reset()
    {
        synchronized(this)
        {
            _samples.clear();
            _max = 0;
            _count = 0;
        }
    }

    void update()
    {
        update(System.nanoTime());
    }

    private void update(long now)
    {
        long expire = now - _nanoPeriod;
        Long head = _samples.peekFirst();

        while (head!=null && head.longValue()<expire)
        {
            _samples.removeFirst();
            head = _samples.peekFirst();
        }
    }

    protected void age(long period, TimeUnit units)
    {
        long increment = TimeUnit.NANOSECONDS.convert(period,units);
        synchronized(this)
        {
            int size = _samples.size();
            for (int i=0; i<size; i++)
                _samples.addLast(_samples.removeFirst()-increment);
            update();
        }
    }

    /**
     * Records a sample value.
     */
    public int record()
    {
        synchronized(this)
        {
            _count++;
            
            long now = System.nanoTime();
            _samples.add(now);
            update(now);
            int rate = _samples.size();
            if (rate>_max)
                _max = rate;
            return rate;
        }
    }

    public int getRate()
    {
        synchronized(this)
        {
            update(System.nanoTime());
            return _samples.size();
        }
    }

    /**
     * @return the max value of the recorded samples
     */
    public long getMax()
    {
        synchronized(this)
        {
            return _max;
        }
    }

    /**
     * @return the number of samples recorded
     */
    public long getCount()
    {
        synchronized(this)
        {
            return _count;
        }
    }

    void dump()
    {
        dump(TimeUnit.MINUTES);
    }

    void dump(TimeUnit units)
    {
        synchronized(this)
        {
            long now = System.nanoTime();
            System.err.printf("%s@%x{count=%d,max=%d,rate=%d per %d %s}%n", 
                getClass().getSimpleName(), hashCode(), 
                _count, _max, _samples.size(),
                _units.convert(_nanoPeriod,TimeUnit.NANOSECONDS), _units);
                
            _samples.stream().map(t->units.convert(now-t,TimeUnit.NANOSECONDS)).forEach(System.err::println);
        }
    }

    @Override
    public String toString()
    {
        synchronized(this)
        {
            update(System.nanoTime());
            return String.format("%s@%x{count=%d,max=%d,rate=%d per %d %s}", 
                getClass().getSimpleName(), hashCode(), 
                _count, _max, _samples.size(),
                _units.convert(_nanoPeriod,TimeUnit.NANOSECONDS), _units);
        }
    }
}
