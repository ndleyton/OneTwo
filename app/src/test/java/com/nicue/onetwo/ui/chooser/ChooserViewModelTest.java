package com.nicue.onetwo.ui.chooser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.core.app.ApplicationProvider;

import com.nicue.onetwo.LiveDataTestUtil;
import com.nicue.onetwo.data.settings.SettingsPrefsDataSource;
import com.nicue.onetwo.data.settings.SettingsRepository;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ChooserViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void refreshChoosingOrder_readsPersistedSetting() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        SettingsRepository settingsRepository = new SettingsRepository(new SettingsPrefsDataSource(context));
        settingsRepository.setChooserOrderEnabled(false);

        ChooserViewModel viewModel = new ChooserViewModel(settingsRepository);
        assertFalse(LiveDataTestUtil.getValue(viewModel.getChoosingOrder()));

        settingsRepository.setChooserOrderEnabled(true);
        viewModel.refreshChoosingOrder();

        assertTrue(LiveDataTestUtil.getValue(viewModel.getChoosingOrder()));
    }
}
