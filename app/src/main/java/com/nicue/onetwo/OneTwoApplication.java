package com.nicue.onetwo;

import android.app.Application;

import com.nicue.onetwo.core.AppContainer;

public class OneTwoApplication extends Application {
    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(this);
        appContainer.getSettingsRepository().applyNightMode();
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }

    public void setAppContainerForTesting(AppContainer appContainer) {
        this.appContainer = appContainer;
    }
}
