
package dk.bearware.data;

import android.content.Context;
import android.media.AudioAttributes;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import dk.bearware.gui.LocaleHelper;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TTSWrapper {
    private static final String TAG = "bearware";
    private TextToSpeech tts;
    public static final String defaultEngineName = "com.google.android.tts";
    private Context mContext;
    public boolean useAnnouncements = false;
    private String mCurrentEngineName = defaultEngineName;

    // Queue speeches until TTS engine finishes async initialization
    private boolean mTtsReady = false;
    private final List<String> mPendingSpeeches = new ArrayList<>();

    public TTSWrapper(Context context) {
        this.mContext = context;
        tts = new TextToSpeech(context, this::onTtsInit);
    }

    public TTSWrapper(Context context, String engineName) {
        this.mContext = context;
        tts = new TextToSpeech(context, this::onTtsInit, engineName);
        this.mCurrentEngineName = engineName;
    }

    private void onTtsInit(int status) {
        mTtsReady = (status == TextToSpeech.SUCCESS);
        if (mTtsReady) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            String behavior = prefs.getString(Preferences.PREF_TTS_LANGUAGE_BEHAVIOR, "follow_app");
            
            if (behavior.equals("follow_app")) {
                Locale locale = LocaleHelper.getCurrentLocale(mContext);
                int result = tts.setLanguage(locale);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language " + locale + " is NOT supported or missing data");
                } else {
                    Log.d(TAG, "TTS initialized for language: " + locale);
                }
            } else {
                Log.d(TAG, "TTS initialized using system default language (behavior: " + behavior + ")");
            }

            if (!mPendingSpeeches.isEmpty()) {
                Log.d(TAG, "TTS ready, flushing " + mPendingSpeeches.size() + " pending speeches");
                for (String text : mPendingSpeeches) {
                    tts.speak(text, TextToSpeech.QUEUE_ADD, null, null);
                }
                mPendingSpeeches.clear();
            }
        }
    }

    public void setLanguage(Locale locale) {
        if (tts != null) {
            int result = tts.setLanguage(locale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language " + locale + " is NOT supported or missing data during update");
            } else {
                Log.d(TAG, "TTS updated to language: " + locale);
            }
        }
    }

    public void shutdown() {
        mPendingSpeeches.clear();
        tts.shutdown();
    }

    public void setAccessibilityStream(boolean bEnable) {
        if (tts == null) return;
        try {
            tts.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(bEnable ? AudioAttributes.CONTENT_TYPE_SPEECH : AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .setUsage(bEnable ? AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY : AudioAttributes.USAGE_UNKNOWN)
                .build());
        } catch (Exception e) {
            Log.e(TAG, "Failed to set TTS audio attributes", e);
        }
    }

    public void speak(String text) {
        if (this.useAnnouncements) {
            AccessibilityManager manager = (AccessibilityManager) mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if ((manager != null) && manager.isEnabled()) {
                AccessibilityEvent e = AccessibilityEvent.obtain();
                e.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
                e.getText().add(text);
                manager.sendAccessibilityEvent(e);
            }
        } else {
            if (!mTtsReady) {
                // TTS engine not yet initialized — queue for later
                Log.d(TAG, "TTS not ready, queuing: " + text);
                mPendingSpeeches.add(text);
            } else {
                tts.speak(text, TextToSpeech.QUEUE_ADD, null, null);
            }
        }
    }

    public List<EngineInfo> getEngines() {
        return tts.getEngines();
    }

    public void reinitialize(Context context, String engineName) {
        Log.d(TAG, "Reinitializing TTS engine: " + engineName);
        mTtsReady = false;
        mPendingSpeeches.clear();
        if (tts != null) {
            tts.shutdown();
        }
        this.mContext = context;
        this.mCurrentEngineName = engineName;
        tts = new TextToSpeech(context, this::onTtsInit, engineName);
    }

    public TTSWrapper switchEngine(String engineName) {
        if (engineName.equals(this.mCurrentEngineName)) return this;
        reinitialize(this.mContext, engineName);
        return this;
    }
}