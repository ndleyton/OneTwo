package com.nicue.onetwo;

import android.os.Bundle;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.splashscreen.SplashScreen;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.nicue.onetwo.core.AppContainer;
import com.nicue.onetwo.data.settings.SettingsRepository;
import com.nicue.onetwo.databinding.ActivityMainBinding;
import com.nicue.onetwo.ui.settings.SettingsFragment;

public class MainActivity extends AppCompatActivity implements SettingsFragment.SettingsApplier {
    private ActivityMainBinding binding;
    private AppBarConfiguration appBarConfiguration;
    private SettingsRepository settingsRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        settingsRepository = getAppContainer().getSettingsRepository();
        settingsRepository.applyNightMode();
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appToolbar.toolbar);

        settingsRepository.applyKeepScreenOn(getWindow());

        NavHostFragment navHostFragment =
                (NavHostFragment)
                        getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            throw new IllegalStateException("NavHostFragment is missing from activity_main");
        }
        NavController navController = navHostFragment.getNavController();
        appBarConfiguration =
                new AppBarConfiguration.Builder(
                                R.id.nav_counter,
                                R.id.nav_dice,
                                R.id.nav_chooser,
                                R.id.nav_timer,
                                R.id.nav_mtg_life,
                                R.id.nav_settings)
                        .setOpenableLayout(binding.drawerLayout)
                        .build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.nvView, navController);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment =
                (NavHostFragment)
                        getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            return super.onSupportNavigateUp();
        }
        NavController navController = navHostFragment.getNavController();
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void applyKeepScreenOn(boolean keepScreenOn) {
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void applyDarkMode(boolean darkModeEnabled) {
        AppCompatDelegate.setDefaultNightMode(
                darkModeEnabled
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private AppContainer getAppContainer() {
        return ((OneTwoApplication) getApplication()).getAppContainer();
    }
}
