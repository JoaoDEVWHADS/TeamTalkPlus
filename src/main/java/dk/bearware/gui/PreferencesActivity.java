
package dk.bearware.gui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

import dk.bearware.ClientEvent;
import dk.bearware.StreamType;
import dk.bearware.TeamTalkBase;
import dk.bearware.User;
import dk.bearware.backend.TeamTalkConnection;
import dk.bearware.backend.TeamTalkConnectionListener;
import dk.bearware.backend.TeamTalkConstants;
import dk.bearware.backend.TeamTalkService;
import dk.bearware.data.Preferences;
import dk.bearware.data.TTSWrapper;

public class PreferencesActivity extends PreferenceActivity implements TeamTalkConnectionListener {

    public static final String TAG = "bearware";

    TeamTalkConnection mConnection;

    static final int ACTIVITY_REQUEST_BEARWAREID = 2;

    private AppCompatDelegate appCompatDelegate = null;

    TeamTalkService getService() {
        return mConnection.getService();
    }

    TeamTalkBase getClient() {
        return getService().getTTInstance();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            Log.e(TAG, "FATAL EXCEPTION IN PREFERENCES ACTIVITY", e);
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, e);
            }
        });

        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        mConnection = new TeamTalkConnection(this);
        EdgeToEdgeHelper.enableEdgeToEdge(this);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getDelegate().onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getDelegate().onStart();

        if (!mConnection.isBound()) {
            Intent intent = new Intent(getApplicationContext(), TeamTalkService.class);
            Log.d(TAG, "Connecting to TeamTalk service");
            if(!bindService(intent, mConnection, Context.BIND_AUTO_CREATE))
                Log.e(TAG, "Failed to connect to TeamTalk service");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
        updateSettings();

        if(mConnection.isBound()) {
            Log.d(TAG, "Disconnecting from TeamTalk service");
            unbindService(mConnection);
            mConnection.setBound(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    @Override
    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    void updateSettings() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        User myself = getService().getUsers().get(getClient().getMyUserID());
        if (myself != null) {
            String nickname = getService().getServerEntry().nickname;
            if (TextUtils.isEmpty(nickname)) {
                nickname = prefs.getString(Preferences.PREF_GENERAL_NICKNAME, "");
            }
            if (!nickname.equals(myself.szNickname)) {
                getClient().doChangeNickname(nickname);
            }
            int statusmode = (myself.nStatusMode & ~(TeamTalkConstants.STATUSMODE_FEMALE | TeamTalkConstants.STATUSMODE_NEUTRAL));
            String statusmsg = getService().getServerEntry().statusmsg;
            
            String genderValue = prefs.getString(Preferences.PREF_GENERAL_GENDER, null);
            int genderValueInt = -1;
            
            if (genderValue != null) {
                try {
                    genderValueInt = Integer.parseInt(genderValue);
                } catch (NumberFormatException e) {
                }
            }
            
            if (genderValueInt == -1) {
                genderValueInt = prefs.getInt("gender_pref_int", -1);
                if (genderValueInt == -1) {
                    if (prefs.contains("gender_checkbox")) {
                        boolean oldFemale = prefs.getBoolean("gender_checkbox", false);
                        genderValueInt = oldFemale ? 1 : 0;
                    } else {
                        genderValueInt = 0;
                    }
                }
                prefs.edit().putString(Preferences.PREF_GENERAL_GENDER, String.valueOf(genderValueInt)).apply();
            }
            
            if (genderValueInt == 1) {
                statusmode |= TeamTalkConstants.STATUSMODE_FEMALE;
            } else if (genderValueInt == 2) {
                statusmode |= TeamTalkConstants.STATUSMODE_NEUTRAL;
            }
            
            getClient().doChangeStatus(statusmode, statusmsg);
        }

        int mf_volume = prefs.getInt(Preferences.PREF_SOUNDSYSTEM_MEDIAFILE_VOLUME, 50);
        mf_volume = Utils.refVolume(mf_volume);
        for(User u: getService().getUsers().values()) {
            getClient().setUserVolume(u.nUserID, StreamType.STREAMTYPE_MEDIAFILE_AUDIO, mf_volume);
            getClient().pumpMessage(ClientEvent.CLIENTEVENT_USER_STATECHANGE, u.nUserID);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
        getDelegate().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        String nameWithDot = fragmentName.replace('$', '.');
        boolean isValid = GeneralPreferenceFragment.class.getName().replace('$', '.').equals(nameWithDot) ||
            SoundEventsPreferenceFragment.class.getName().replace('$', '.').equals(nameWithDot) ||
            ConnectionPreferenceFragment.class.getName().replace('$', '.').equals(nameWithDot) ||
            ServerListPreferenceFragment.class.getName().replace('$', '.').equals(nameWithDot) ||
            TtsPreferenceFragment.class.getName().replace('$', '.').equals(nameWithDot) ||
            SoundSystemPreferenceFragment.class.getName().replace('$', '.').equals(nameWithDot) ||
            DisplayPreferenceFragment.class.getName().replace('$', '.').equals(nameWithDot) ||
            AboutPreferenceFragment.class.getName().replace('$', '.').equals(nameWithDot);
        if (!isValid) {
            Log.w(TAG, "Invalid fragment: " + fragmentName + " (" + nameWithDot + ")");
        }
        return isValid;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ACTIVITY_REQUEST_BEARWAREID && resultCode == RESULT_OK) {

        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
        try {
            String stringValue = value.toString();

            if(preference instanceof ListPreference listPreference) {

                int index = listPreference.findIndexOfValue(stringValue);

                CharSequence[] entries = listPreference.getEntries();
                preference.setSummary(index >= 0 && entries != null && index < entries.length
                    ? entries[index].toString().replace("%", "%%") : null);

            }
        else if(preference instanceof RingtonePreference) {

            if(TextUtils.isEmpty(stringValue)) {

            }
            else {
                Ringtone ringtone = RingtoneManager.getRingtone(
                    preference.getContext(), Uri.parse(stringValue));

                if(ringtone == null) {

                    preference.setSummary(null);
                }
                else {

                    String name = ringtone.getTitle(preference.getContext());
                    preference.setSummary(name);
                }
            }

        }
        else if (preference instanceof CheckBoxPreference) {
            if (preference.getKey().equals(Preferences.PREF_GENERAL_BEARWARE_CHECKED)) {
            }
        }
            else {
                preference.setSummary(stringValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        if (preference == null) return;

        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        String value = "";
        try {
            if (preference.getKey() != null) {
                value = PreferenceManager.getDefaultSharedPreferences(
                    preference.getContext()).getString(preference.getKey(), "");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get string value for pref: " + preference.getKey());
        }
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, value);
    }


    public static class DisplayPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            try {
                addPreferencesFromResource(R.xml.pref_display);

                // Font scale is now a SeekBarPreference (int), handled by its own summary update
                Preference fontScalePref = findPreference("pref_display_font_scale");
                if (fontScalePref instanceof SeekBarPreference) {
                     fontScalePref.setOnPreferenceChangeListener((preference, newValue) -> {
                        if (newValue instanceof Integer) {
                            preference.setSummary(SeekBarPreference.getSummaryFormat((Integer)newValue));
                        }
                        return true;
                    });
                    // Set initial summary
                    int val = PreferenceManager.getDefaultSharedPreferences(getActivity())
                            .getInt("pref_display_font_scale", 100);
                    fontScalePref.setSummary(SeekBarPreference.getSummaryFormat(val));
                }

                bindPreferenceSummaryToValue(findPreference("pref_channel_sort"));

                Preference languagePref = findPreference(Preferences.PREF_LANGUAGE);
                if (languagePref != null) {
                    bindPreferenceSummaryToValue(languagePref);
                    languagePref.setOnPreferenceChangeListener((preference, newValue) -> {
                        String language = (String) newValue;
                        LocaleHelper.setLocale(getActivity(), language);
                        
                        // Restart app to apply changes
                        Intent intent = getActivity().getPackageManager()
                            .getLaunchIntentForPackage(getActivity().getPackageName());
                        if(intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            getActivity().finish();
                        } else {
                            getActivity().recreate();
                        }
                        return true;
                    });
                }
            } catch (Exception e) {
                 Log.e(TAG, "Failed to load Display preferences", e);
                 e.printStackTrace();
            }
        }
    }

    public static class GeneralPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Ensure default values are initialized
            PreferenceManager.setDefaultValues(getActivity(), R.xml.pref_general, false);
            
            addPreferencesFromResource(R.xml.pref_general);

            bindPreferenceSummaryToValue(findPreference(Preferences.PREF_GENERAL_NICKNAME));

            Preference bearwareLogin = findPreference(Preferences.PREF_GENERAL_BEARWARE_CHECKED);
            bearwareLogin.setOnPreferenceChangeListener((preference, o) -> {
                Intent edit = new Intent(getActivity(), WebLoginActivity.class);
                getActivity().startActivityForResult(edit, ACTIVITY_REQUEST_BEARWAREID);
                return true;
            });

            Preference genderPref = findPreference(Preferences.PREF_GENERAL_GENDER);
            if (genderPref != null) {
                bindPreferenceSummaryToValue(genderPref);
            }


        }

        @Override
        public void onResume() {
            super.onResume();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
            String username = prefs.getString(Preferences.PREF_GENERAL_BEARWARE_USERNAME, "");

            CheckBoxPreference preference = (CheckBoxPreference) findPreference(Preferences.PREF_GENERAL_BEARWARE_CHECKED);
            preference.setChecked(username.length() > 0);
        }

    }

    public static class ServerListPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_serverlist);
        }

        @Override
        public void onResume() {
            super.onResume();
        }
    }

    public static class SoundEventsPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_soundevents);

            ListPreference soundPackPref = (ListPreference) findPreference("pref_sound_pack");
            if (soundPackPref != null) {
                populateSoundPacks(soundPackPref);
                soundPackPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        preference.setSummary((String) newValue);
                        return true;
                    }
                });
                soundPackPref.setSummary(soundPackPref.getValue());
            }
        }

        private void populateSoundPacks(ListPreference lp) {
            List<String> entries = new ArrayList<>();
            List<String> entryValues = new ArrayList<>();

            // Default option
            entries.add(getString(R.string.sound_pack_default));
            entryValues.add("Default");

            // Scan /sdcard/TeamTalk/Sounds
            File soundsDir = new File(Environment.getExternalStorageDirectory(), "TeamTalk/Sounds");
            if (soundsDir.exists() && soundsDir.isDirectory()) {
                File[] dirs = soundsDir.listFiles(File::isDirectory);
                if (dirs != null) {
                    for (File dir : dirs) {
                        entries.add(dir.getName());
                        entryValues.add(dir.getName());
                    }
                }
            }

            lp.setEntries(entries.toArray(new String[0]));
            lp.setEntryValues(entryValues.toArray(new String[0]));
        }
    }

    public static class ConnectionPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_connection);
        }
    }

    public static class TtsPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_tts);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
            TTSWrapper tts = new TTSWrapper(getActivity().getBaseContext(), prefs.getString("pref_speech_engine", TTSWrapper.defaultEngineName));
            List<EngineInfo> engines = tts.getEngines();
            ListPreference enginePrefs = (ListPreference) findPreference("pref_speech_engine");
            ArrayList<String> entries = new ArrayList<>();
            ArrayList<String> values = new ArrayList<>();
            for (EngineInfo info : engines) {
                entries.add(info.label);
                values.add(info.name);
            }
            enginePrefs.setEntries(entries.toArray(new CharSequence[engines.size()]));
            enginePrefs.setEntryValues(values.toArray(new CharSequence[engines.size()]));

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                CheckBoxPreference mTtsPref = (CheckBoxPreference) findPreference("pref_a11y_volume");
                PreferenceCategory mTtsCat = (PreferenceCategory) findPreference("tts_def");
                if (mTtsCat != null && mTtsPref != null) {
                    mTtsCat.removePreference(mTtsPref);
                }
            }
        }
    }

    public static class SoundSystemPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_soundsystem);
            bindPreferenceSummaryToValue(findPreference("pref_audio_source"));

            CheckBoxPreference stereoPref = (CheckBoxPreference) findPreference("pref_stereo_input");
            CheckBoxPreference micPref = (CheckBoxPreference) findPreference("use_builtin_mic_checkbox");
            ListPreference sourcePref = (ListPreference) findPreference("pref_audio_source");

            if(micPref != null && sourcePref != null) {
                
                sourcePref.setEnabled(!micPref.isChecked());
                
                micPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean checked = (Boolean) newValue;
                    sourcePref.setEnabled(!checked);
                    return true;
                });
            }

            if(stereoPref != null && micPref != null) {
                 stereoPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean checked = (Boolean) newValue;
                    if(checked) {
                        micPref.setChecked(true);
                        if(sourcePref != null) sourcePref.setEnabled(false);
                    }
                    return true;
                });
            }
        }
    }

    public static class AboutPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_about);
        }
    }

    @Override
    public void onServiceConnected(TeamTalkService service) {
    }

    @Override
    public void onServiceDisconnected(TeamTalkService service) {
    }

    private AppCompatDelegate getDelegate() {
        if (appCompatDelegate == null) {
            appCompatDelegate = AppCompatDelegate.create(this, null);
        }
        return appCompatDelegate;
    }

}