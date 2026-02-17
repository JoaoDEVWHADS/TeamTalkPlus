package dk.bearware.gui;

import android.app.Application;
import android.content.Context;

public class TeamTalkApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "default"));
    }
}
