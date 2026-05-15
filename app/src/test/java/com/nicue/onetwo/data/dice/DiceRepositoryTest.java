package com.nicue.onetwo.data.dice;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DiceRepositoryTest {
    private DiceRepository diceRepository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("SHARED_PREFS_FILE", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        diceRepository = new DiceRepository(new DicePrefsDataSource(context));
    }

    @Test
    public void writeAndReadDiceDefinitions_roundTripInPreferences() {
        List<Integer> diceFaces = Arrays.asList(6, 10, 20);
        diceRepository.writeDiceFaces(diceFaces);

        assertEquals(diceFaces, diceRepository.readDiceFaces());
    }
}
