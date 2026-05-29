package dk.bearware.gui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

public class LocaleHelper {

    // Use the same key as pref_display.xml
    private static final String SELECTED_LANGUAGE = "pref_language";

    public static Context onAttach(Context context) {
        String lang = getLanguage(context);
        return updateContext(context, lang);
    }

    public static Locale getCurrentLocale(Context context) {
        String lang = getLanguage(context);
        if (lang.equals("default")) {
            // Use the system's preferred locale instead of the potentially overridden process-wide default
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Resources.getSystem().getConfiguration().getLocales().get(0);
            }
            return Resources.getSystem().getConfiguration().locale;
        }
        return createLocale(lang);
    }

    /**
     * Updates the resources of the given context manually. 
     * Useful for Services or other non-Activity contexts.
     */
    public static Context updateContext(Context context, String language) {
        if (language.equals("default")) {
            // Reset to system default by using the system configuration's locale
            Locale systemLocale = getCurrentLocale(context);
            return updateResources(context, systemLocale);
        }
        return updateResources(context, createLocale(language));
    }

    public static String getLanguage(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(SELECTED_LANGUAGE, "default");
    }

    public static void setLocale(Context context, String language) {
        persist(context, language);

        if (language.equals("default")) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            String tag = language.replace("_", "-");
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
        }
    }

    private static void persist(Context context, String language) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(SELECTED_LANGUAGE, language).apply();
    }

    private static Context updateResources(Context context, Locale locale) {
        Locale.setDefault(locale);
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.os.LocaleList localeList = new android.os.LocaleList(locale);
            config.setLocales(localeList);
            config.setLayoutDirection(locale);
            
            // Also attempt to update the application context's resources to keep it in sync
            Context appContext = context.getApplicationContext();
            if (appContext != null && appContext != context) {
                appContext.getResources().updateConfiguration(config, appContext.getResources().getDisplayMetrics());
            }

            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLayoutDirection(locale);
            }
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context;
        }
    }

    static Locale createLocale(String language) {
        String tag = language.replace("_", "-");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Locale.forLanguageTag(tag);
        }
        
        String[] parts = tag.split("-", 2);
        if (parts.length > 1) {
            return new Locale(parts[0], parts[1]);
        }
        if (tag.equalsIgnoreCase("in")) {
            return new Locale("in");
        }
        return new Locale(tag);
    }
}






