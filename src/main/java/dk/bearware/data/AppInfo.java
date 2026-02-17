
package dk.bearware.data;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import dk.bearware.TeamTalkBase;

public class AppInfo {
    public static final String TAG = "bearware";

    public static final String APPNAME_SHORT = "TeamTalk5Plus";
    public static final String APPVERSION_POSTFIX = "";
    public static final String OSTYPE = "Android";

    public static final String WEBLOGIN_BEARWARE_USERNAME = "bearware";
    public static final String WEBLOGIN_BEARWARE_USERNAMEPOSTFIX = "@bearware.dk";

    public static String BEARWARE_REGISTRATION_WEBSITE = "http://www.bearware.dk";

    public static String getVersion(Context context) {
        String version = "";
        try {
            version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        }
        catch(NameNotFoundException e) {
            Log.e(TAG, "Unable to get version information");
        }
        return version;
    }

    public static String getDefautlUrlArgs(Context context) {
        final String TEAMTALK_VERSION = TeamTalkBase.getVersion();
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

}