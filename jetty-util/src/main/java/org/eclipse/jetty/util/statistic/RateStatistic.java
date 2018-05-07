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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 */
public class RateStatistic
{
    private final Deque<Long> _samples = new ArrayDeque<>();
    private long _max;
    private long _count;

    /**
     * Resets the statistics.
     */
    public void reset()
    {
        synchronized(this)
        {
        }
    }

    /**
     * Records a sample value.
     */
    public long record()
    {
        synchronized(this)
        { 
            long now = System.nanoTime();
        }
    }

    /**
     * @return the max value of the recorded samples
     */
    public long getMax()
    {
        return _max.get();
    }

    /**
     * @return the number of samples recorded
     */
    public long getCount()
    {
        return _count.get();
    }


    @Override
    public String toString()
    {
        return String.format("%s@%x{count=%d,mean=%d,total=%d,stddev=%f}", getClass().getSimpleName(), hashCode(), getCount(), getMax(), getTotal(), getStdDev());
    }
}
