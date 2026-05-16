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

    @Test
    public void calculateAdjustedCounterValueAddsAmount() {
        assertEquals(15, CounterFragment.calculateAdjustedCounterValue(10, 5, true));
    }

    @Test
    public void calculateAdjustedCounterValueSubtractsAmount() {
        assertEquals(5, CounterFragment.calculateAdjustedCounterValue(10, 5, false));
    }

    @Test
    public void calculateAdjustedCounterValueClampsToPickerBounds() {
        assertEquals(99999, CounterFragment.calculateAdjustedCounterValue(99990, 20, true));
        assertEquals(0, CounterFragment.calculateAdjustedCounterValue(3, 5, false));
    }
}
