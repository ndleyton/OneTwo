package com.nicue.onetwo.fragments;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DiceFragmentTest {

    @Test
    public void normalizeFacesInputDefaultsBlankAndInvalidValuesToSix() {
        assertEquals("6", DiceFragment.normalizeFacesInput(""));
        assertEquals("6", DiceFragment.normalizeFacesInput("  "));
        assertEquals("6", DiceFragment.normalizeFacesInput("abc"));
    }

    @Test
    public void normalizeFacesInputEnforcesMinimumOfTwo() {
        assertEquals("2", DiceFragment.normalizeFacesInput("0"));
        assertEquals("2", DiceFragment.normalizeFacesInput("1"));
    }

    @Test
    public void normalizeFacesInputReturnsTrimmedValidValue() {
        assertEquals("20", DiceFragment.normalizeFacesInput(" 20 "));
    }
}
