package com.nicue.onetwo.fragments;

import com.nicue.onetwo.ui.counter.CounterFragment;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CounterFragmentTest {

    @Test
    public void sanitizeObjectNameReplacesSingleQuotes() {
        assertEquals("Alice\"s score", CounterFragment.sanitizeObjectName("Alice's score"));
    }

    @Test
    public void parseCountValueReturnsParsedNumber() {
        assertEquals(42, CounterFragment.parseCountValue("42"));
    }

    @Test
    public void parseCountValueFallsBackToZeroForInvalidInput() {
        assertEquals(0, CounterFragment.parseCountValue("abc"));
        assertEquals(0, CounterFragment.parseCountValue(""));
    }
}
