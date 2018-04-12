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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;


@RunWith(value = Parameterized.class)
public class TrieTest
{
    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        Object[][] data = new Object[][]{
            {new ArrayTrie<Integer>(128)},
            {new TreeTrie<Integer>()},
            {new ArrayTernaryTrie<Integer>(128)}
        };
        return Arrays.asList(data);
    }

    Trie<Integer> trie;
    
    public TrieTest(Trie<Integer> t)
    {
        trie=t;
    }
    
    @BeforeEach
    public void before()
    {
        trie.put("hello",1);
        trie.put("He",2);
        trie.put("HELL",3);
        trie.put("wibble",4);
        trie.put("Wobble",5);
        trie.put("foo-bar",6);
        trie.put("foo+bar",7);
        trie.put("HELL4",8);
        trie.put("",9);
    }

    @Test
    public void testOverflow() throws Exception
    {
        int i=0;
        while (true) 
        {
            if (++i>10000)
                break; // must not be fixed size
            if (!trie.put("prefix" + i, i))
            {
                assertTrue(trie.isFull());
                break;
            }
        }
        
        assertTrue(!trie.isFull() || !trie.put("overflow", 0));
    }
    
    @Test
    public void testKeySet() throws Exception
    {
        assertTrue(trie.keySet().contains("hello"));
        assertTrue(trie.keySet().contains("He"));
        assertTrue(trie.keySet().contains("HELL"));
        assertTrue(trie.keySet().contains("wibble"));
        assertTrue(trie.keySet().contains("Wobble"));
        assertTrue(trie.keySet().contains("foo-bar"));
        assertTrue(trie.keySet().contains("foo+bar"));
        assertTrue(trie.keySet().contains("HELL4"));
        assertTrue(trie.keySet().contains(""));
    }
    
    @Test
    public void testGetString() throws Exception
    {
        assertEquals(1,trie.get("hello").intValue());
        assertEquals(2,trie.get("He").intValue());
        assertEquals(3,trie.get("HELL").intValue());
        assertEquals(4,trie.get("wibble").intValue());
        assertEquals(5,trie.get("Wobble").intValue());
        assertEquals(6,trie.get("foo-bar").intValue());
        assertEquals(7,trie.get("foo+bar").intValue());
        
        assertEquals(1,trie.get("Hello").intValue());
        assertEquals(2,trie.get("HE").intValue());
        assertEquals(3,trie.get("heLL").intValue());
        assertEquals(4,trie.get("Wibble").intValue());
        assertEquals(5,trie.get("wobble").intValue());
        assertEquals(6,trie.get("Foo-bar").intValue());
        assertEquals(7,trie.get("FOO+bar").intValue());
        assertEquals(8,trie.get("HELL4").intValue());
        assertEquals(9,trie.get("").intValue());
        
        assertEquals(null,trie.get("helloworld"));
        assertEquals(null,trie.get("Help"));
        assertEquals(null,trie.get("Blah"));
    }

    @Test
    public void testGetBuffer() throws Exception
    {
        assertEquals(1,trie.get(BufferUtil.toBuffer("xhellox"),1,5).intValue());
        assertEquals(2,trie.get(BufferUtil.toBuffer("xhellox"),1,2).intValue());
        assertEquals(3,trie.get(BufferUtil.toBuffer("xhellox"),1,4).intValue());
        assertEquals(4,trie.get(BufferUtil.toBuffer("wibble"),0,6).intValue());
        assertEquals(5,trie.get(BufferUtil.toBuffer("xWobble"),1,6).intValue());
        assertEquals(6,trie.get(BufferUtil.toBuffer("xfoo-barx"),1,7).intValue());
        assertEquals(7,trie.get(BufferUtil.toBuffer("xfoo+barx"),1,7).intValue());
        
        assertEquals(1,trie.get(BufferUtil.toBuffer("xhellox"),1,5).intValue());
        assertEquals(2,trie.get(BufferUtil.toBuffer("xHELLox"),1,2).intValue());
        assertEquals(3,trie.get(BufferUtil.toBuffer("xhellox"),1,4).intValue());
        assertEquals(4,trie.get(BufferUtil.toBuffer("Wibble"),0,6).intValue());
        assertEquals(5,trie.get(BufferUtil.toBuffer("xwobble"),1,6).intValue());
        assertEquals(6,trie.get(BufferUtil.toBuffer("xFOO-barx"),1,7).intValue());
        assertEquals(7,trie.get(BufferUtil.toBuffer("xFOO+barx"),1,7).intValue());

        assertEquals(null,trie.get(BufferUtil.toBuffer("xHelloworldx"),1,10));
        assertEquals(null,trie.get(BufferUtil.toBuffer("xHelpx"),1,4));
        assertEquals(null,trie.get(BufferUtil.toBuffer("xBlahx"),1,4));
    }
    
    @Test
    public void testGetDirectBuffer() throws Exception
    {
        assertEquals(1,trie.get(BufferUtil.toDirectBuffer("xhellox"),1,5).intValue());
        assertEquals(2,trie.get(BufferUtil.toDirectBuffer("xhellox"),1,2).intValue());
        assertEquals(3,trie.get(BufferUtil.toDirectBuffer("xhellox"),1,4).intValue());
        assertEquals(4,trie.get(BufferUtil.toDirectBuffer("wibble"),0,6).intValue());
        assertEquals(5,trie.get(BufferUtil.toDirectBuffer("xWobble"),1,6).intValue());
        assertEquals(6,trie.get(BufferUtil.toDirectBuffer("xfoo-barx"),1,7).intValue());
        assertEquals(7,trie.get(BufferUtil.toDirectBuffer("xfoo+barx"),1,7).intValue());
        
        assertEquals(1,trie.get(BufferUtil.toDirectBuffer("xhellox"),1,5).intValue());
        assertEquals(2,trie.get(BufferUtil.toDirectBuffer("xHELLox"),1,2).intValue());
        assertEquals(3,trie.get(BufferUtil.toDirectBuffer("xhellox"),1,4).intValue());
        assertEquals(4,trie.get(BufferUtil.toDirectBuffer("Wibble"),0,6).intValue());
        assertEquals(5,trie.get(BufferUtil.toDirectBuffer("xwobble"),1,6).intValue());
        assertEquals(6,trie.get(BufferUtil.toDirectBuffer("xFOO-barx"),1,7).intValue());
        assertEquals(7,trie.get(BufferUtil.toDirectBuffer("xFOO+barx"),1,7).intValue());

        assertEquals(null,trie.get(BufferUtil.toDirectBuffer("xHelloworldx"),1,10));
        assertEquals(null,trie.get(BufferUtil.toDirectBuffer("xHelpx"),1,4));
        assertEquals(null,trie.get(BufferUtil.toDirectBuffer("xBlahx"),1,4));
    }
    

    @Test
    public void testGetBestArray() throws Exception
    {
        assertEquals(1,trie.getBest(StringUtil.getUtf8Bytes("xhelloxxxx"),1,8).intValue());
        assertEquals(2,trie.getBest(StringUtil.getUtf8Bytes("xhelxoxxxx"),1,8).intValue());
        assertEquals(3,trie.getBest(StringUtil.getUtf8Bytes("xhellxxxxx"),1,8).intValue());
        assertEquals(6,trie.getBest(StringUtil.getUtf8Bytes("xfoo-barxx"),1,8).intValue());
        assertEquals(8,trie.getBest(StringUtil.getUtf8Bytes("xhell4xxxx"),1,8).intValue());
        
        assertEquals(1,trie.getBest(StringUtil.getUtf8Bytes("xHELLOxxxx"),1,8).intValue());
        assertEquals(2,trie.getBest(StringUtil.getUtf8Bytes("xHELxoxxxx"),1,8).intValue());
        assertEquals(3,trie.getBest(StringUtil.getUtf8Bytes("xHELLxxxxx"),1,8).intValue());
        assertEquals(6,trie.getBest(StringUtil.getUtf8Bytes("xfoo-BARxx"),1,8).intValue());
        assertEquals(8,trie.getBest(StringUtil.getUtf8Bytes("xHELL4xxxx"),1,8).intValue());
        assertEquals(9,trie.getBest(StringUtil.getUtf8Bytes("xZZZZZxxxx"),1,8).intValue());
    }

    @Test
    public void testGetBestBuffer() throws Exception
    {
        assertEquals(1,trie.getBest(BufferUtil.toBuffer("xhelloxxxx"),1,8).intValue());
        assertEquals(2,trie.getBest(BufferUtil.toBuffer("xhelxoxxxx"),1,8).intValue());
        assertEquals(3,trie.getBest(BufferUtil.toBuffer("xhellxxxxx"),1,8).intValue());
        assertEquals(6,trie.getBest(BufferUtil.toBuffer("xfoo-barxx"),1,8).intValue());
        assertEquals(8,trie.getBest(BufferUtil.toBuffer("xhell4xxxx"),1,8).intValue());
        
        assertEquals(1,trie.getBest(BufferUtil.toBuffer("xHELLOxxxx"),1,8).intValue());
        assertEquals(2,trie.getBest(BufferUtil.toBuffer("xHELxoxxxx"),1,8).intValue());
        assertEquals(3,trie.getBest(BufferUtil.toBuffer("xHELLxxxxx"),1,8).intValue());
        assertEquals(6,trie.getBest(BufferUtil.toBuffer("xfoo-BARxx"),1,8).intValue());
        assertEquals(8,trie.getBest(BufferUtil.toBuffer("xHELL4xxxx"),1,8).intValue());
        assertEquals(9,trie.getBest(BufferUtil.toBuffer("xZZZZZxxxx"),1,8).intValue());
        
        ByteBuffer buffer = (ByteBuffer)BufferUtil.toBuffer("xhelloxxxxxxx").position(2);
        assertEquals(1,trie.getBest(buffer,-1,10).intValue());
    }

    @Test
    public void testGetBestDirectBuffer() throws Exception
    {
        assertEquals(1,trie.getBest(BufferUtil.toDirectBuffer("xhelloxxxx"),1,8).intValue());
        assertEquals(2,trie.getBest(BufferUtil.toDirectBuffer("xhelxoxxxx"),1,8).intValue());
        assertEquals(3,trie.getBest(BufferUtil.toDirectBuffer("xhellxxxxx"),1,8).intValue());
        assertEquals(6,trie.getBest(BufferUtil.toDirectBuffer("xfoo-barxx"),1,8).intValue());
        assertEquals(8,trie.getBest(BufferUtil.toDirectBuffer("xhell4xxxx"),1,8).intValue());
        
        assertEquals(1,trie.getBest(BufferUtil.toDirectBuffer("xHELLOxxxx"),1,8).intValue());
        assertEquals(2,trie.getBest(BufferUtil.toDirectBuffer("xHELxoxxxx"),1,8).intValue());
        assertEquals(3,trie.getBest(BufferUtil.toDirectBuffer("xHELLxxxxx"),1,8).intValue());
        assertEquals(6,trie.getBest(BufferUtil.toDirectBuffer("xfoo-BARxx"),1,8).intValue());
        assertEquals(8,trie.getBest(BufferUtil.toDirectBuffer("xHELL4xxxx"),1,8).intValue());
        assertEquals(9,trie.getBest(BufferUtil.toDirectBuffer("xZZZZZxxxx"),1,8).intValue());
        
        ByteBuffer buffer = (ByteBuffer)BufferUtil.toDirectBuffer("xhelloxxxxxxx").position(2);
        assertEquals(1,trie.getBest(buffer,-1,10).intValue());
    }
    
    @Test 
    public void testFull() throws Exception
    {
       if (!(trie instanceof ArrayTrie<?> || trie instanceof ArrayTernaryTrie<?>))
           return;
       
       assertFalse(trie.put("Large: This is a really large key and should blow the maximum size of the array trie as lots of nodes should already be used.",99));
       testGetString();
       testGetBestArray();
       testGetBestBuffer();
    }
    
    
}
