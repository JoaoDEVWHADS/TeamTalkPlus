
package dk.bearware.data;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import dk.bearware.TeamTalkBase;
import dk.bearware.gui.BuildConfig;

public class AppInfo {
    public static final String TAG = "bearware";

    public static final String APPNAME_SHORT = "TeamTalk5Plus";
    public static final String APPVERSION_POSTFIX = "";
    public static final String OSTYPE = "Android";
    public static final String FOLDER_NAME = "TeamTalk";

    public static final String WEBLOGIN_BEARWARE_USERNAME = "bearware";
    public static final String WEBLOGIN_BEARWARE_USERNAMEPOSTFIX = "@bearware.dk";

    public static String BEARWARE_REGISTRATION_WEBSITE = "http://www.bearware.dk";

    public static String getVersion(Context context) {
        String version = "";
        try {
            version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            version += " (" + BuildConfig.BUILD_TIME + ")";
        }
        catch(NameNotFoundException e) {
            Log.e(TAG, "Unable to get version information");
        }
        return version;
    }

    public static String getDefautlUrlArgs(Context context) {
        final String TEAMTALK_VERSION = "5.34.5";
        String appversion = getVersion(context);
        return "client=" + APPNAME_SHORT + "&version="
                + appversion + "&dllversion=" + TEAMTALK_VERSION + "&os=" + OSTYPE;
    }

    public static String getServerListURL(Context context, boolean official, boolean unofficial) {
        String urlToRead = "http://www.bearware.dk/teamtalk/tt5servers.php?" +
                getDefautlUrlArgs(context) +
                "&official=" + (official ? "1" : "0") +
                "&unofficial=" + (unofficial ? "1" : "0");
        return urlToRead;
    }

    public static String getUpdateURL(Context context) {
        String urlToRead = "http://www.bearware.dk/teamtalk/tt5update.php?" + getDefautlUrlArgs(context);
        return urlToRead;
    }

    public static String getBearWareTokenUrl(Context context, String username, String password) {
        try {
            username = URLEncoder.encode(username, "UTF-8");
            password = URLEncoder.encode(password, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Failed to encode username/password", e);
        }
        String urlToRead = "https://www.bearware.dk/teamtalk/weblogin.php?" + getDefautlUrlArgs(context) +
                "&service=bearware&action=auth&username=" + username + "&password=" + password;
        return urlToRead;
    }

    public static String getBearWareAccessTokenUrl(Context context, String username, String token, String accesstoken) {
        try {
            username = URLEncoder.encode(username, "UTF-8");
            token = URLEncoder.encode(token, "UTF-8");
            accesstoken = URLEncoder.encode(accesstoken, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Failed to encode authentication parameters", e);
        }
        String urlToRead = "https://www.bearware.dk/teamtalk/weblogin.php?" + getDefautlUrlArgs(context) +
                "&service=bearware&action=clientauth&username=" + username + "&token=" + token +
                "&accesstoken=" + accesstoken;
        return urlToRead;
    }

    public static String getPublishServerUrl(Context context, String username, String token) {
        try {
            username = URLEncoder.encode(username, "UTF-8");
            token = URLEncoder.encode(token, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Failed to encode publish parameters", e);
        }
        return "https://www.bearware.dk/teamtalk/tt5servers.php?" +
                getDefautlUrlArgs(context) +
                "&action=publish&username=" + username + "&token=" + token;
    }

    public static void ensureFoldersExist(android.content.Context context) {
        try {
            java.io.File root = new java.io.File(android.os.Environment.getExternalStorageDirectory(), FOLDER_NAME);
            if (!root.exists()) {
                if (root.mkdirs()) {
                    android.util.Log.d(TAG, "Created app root directory: " + root.getAbsolutePath());
                }
            } else if (!root.isDirectory()) {
                android.util.Log.w(TAG, "Root exists but is not a directory: " + root.getAbsolutePath());
            }

            java.io.File sounds = new java.io.File(root, "Sounds");
            if (!sounds.exists()) {
                if (sounds.mkdirs()) {
                    android.util.Log.d(TAG, "Created sounds directory: " + sounds.getAbsolutePath());
                }
            } else if (!sounds.isDirectory()) {
                android.util.Log.w(TAG, "Sounds path exists but is not a directory: " + sounds.getAbsolutePath());
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to ensure folders exist", e);
        }
    }

}