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

package org.eclipse.jetty.util.log.jmx;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.Managed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LogMBeanTest
{

    private Managed managed;

    private LogMBean logMBean;

    private static final String MANAGED_CLASS = "Managed";

    @BeforeEach
    public void setUp()
    {
        managed = new Managed();
        logMBean = new LogMBean(managed);
    }

    @Test
    public void testKeySet()
    {
        // given
        assertFalse(logMBean.getLoggers().contains(MANAGED_CLASS), "Managed is not registered with loggers");

        // when
        logMBean.setDebugEnabled(MANAGED_CLASS,true);

        // then
        assertTrue(logMBean.getLoggers().contains(MANAGED_CLASS), "Managed must be registered with loggers");
        assertTrue(logMBean.isDebugEnabled(MANAGED_CLASS), "This must return true as debug is enabled for this class");
    }
}
