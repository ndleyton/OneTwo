package com.nicue.onetwo.Utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PoolsTest {

    @Test(expected = IllegalArgumentException.class)
    public void simplePoolRejectsZeroCapacity() {
        new Pools.SimplePool<>(0);
    }

    @Test
    public void simplePoolAcquireReturnsMostRecentlyReleasedInstance() {
        Pools.SimplePool<String> pool = new Pools.SimplePool<>(2);

        assertTrue(pool.release("first"));
        assertTrue(pool.release("second"));

        assertEquals("second", pool.acquire());
        assertEquals("first", pool.acquire());
        assertNull(pool.acquire());
    }

    @Test(expected = IllegalStateException.class)
    public void simplePoolRejectsDuplicateRelease() {
        Pools.SimplePool<Object> pool = new Pools.SimplePool<>(2);
        Object item = new Object();

        pool.release(item);
        pool.release(item);
    }

    @Test
    public void simplePoolReturnsFalseWhenFull() {
        Pools.SimplePool<String> pool = new Pools.SimplePool<>(1);

        assertTrue(pool.release("first"));
        assertFalse(pool.release("second"));
    }

    @Test
    public void synchronizedPoolDelegatesAcquireAndRelease() {
        Pools.SynchronizedPool<Object> pool = new Pools.SynchronizedPool<>(1);
        Object item = new Object();

        assertTrue(pool.release(item));
        assertSame(item, pool.acquire());
        assertNull(pool.acquire());
    }
}
