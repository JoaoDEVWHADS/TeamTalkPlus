package dk.bearware.gui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GitHubUpdateManager {
    private static final String TAG = "GitHubUpdateManager";
    private static final String REPO_URL = "https://api.github.com/repos/JoaoDEVWHADS/TeamTalkPlus/releases/latest";
    public static final int REQUEST_INSTALL_UNKNOWN_SOURCES = 10001;
    private final android.app.Activity activity;
    private static final String PREF_PENDING_APK = "pending_update_apk";
    private static final String PREF_PENDING_TAG = "pending_update_tag";
    private File pendingApkFile;

    public GitHubUpdateManager(android.app.Activity activity) {
        this.activity = activity;
    }

    public void checkForUpdates() {
        new CheckUpdateTask().execute();
    }

    private class CheckUpdateTask extends AsyncTask<Void, Void, UpdateInfo> {
        @Override
        protected UpdateInfo doInBackground(Void... voids) {
            try {
                URL url = new URL(REPO_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "TeamTalk-Android-Updater");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "GitHub API Response Code: " + responseCode);
                if (responseCode == 200) {
                    InputStream in = new BufferedInputStream(connection.getInputStream());
                    String response = dk.bearware.gui.Utils.readInputStream(in);
                    connection.disconnect();

                    Gson gson = new Gson();
                    JsonObject release = gson.fromJson(response, JsonObject.class);
                    String tagName = release.get("tag_name").getAsString();
                    String downloadUrl = null;

                    JsonArray assets = release.getAsJsonArray("assets");
                    for (int i = 0; i < assets.size(); i++) {
                        JsonObject asset = assets.get(i).getAsJsonObject();
                        if (asset.get("name").getAsString().endsWith(".apk")) {
                            downloadUrl = asset.get("browser_download_url").getAsString();
                            break;
                        }
                    }

                    if (downloadUrl != null) {
                        return new UpdateInfo(tagName, downloadUrl);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking for updates", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(UpdateInfo updateInfo) {
            String pendingTag = activity.getSharedPreferences(TAG, Context.MODE_PRIVATE).getString(PREF_PENDING_TAG, "");
            String pendingPath = activity.getSharedPreferences(TAG, Context.MODE_PRIVATE).getString(PREF_PENDING_APK, "");

            if (updateInfo != null && isNewerVersion(updateInfo.tagName)) {
                if (updateInfo.tagName.equals(pendingTag) && !pendingPath.isEmpty()) {
                    File file = new File(pendingPath);
                    if (file.exists()) {
                        pendingApkFile = file;
                        showUpdateDialog(updateInfo);
                        return;
                    }
                }
                showUpdateDialog(updateInfo);
            } else {
                clearPendingUpdate();
            }
        }
    }

    private boolean isNewerVersion(String latestVersion) {
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            String currentVersion = pInfo.versionName;
            
            // Basic comparison: strip 'v' prefix if present
            String latest = latestVersion.startsWith("v") ? latestVersion.substring(1) : latestVersion;
            String current = currentVersion.startsWith("v") ? currentVersion.substring(1) : currentVersion;

            return compareVersions(latest, current) > 0;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found", e);
        }
        return false;
    }

    private int compareVersions(String v1, String v2) {
        String[] vals1 = v1.split("\\.");
        String[] vals2 = v2.split("\\.");
        int i = 0;
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        if (i < vals1.length && i < vals2.length) {
            try {
                int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
                return Integer.signum(diff);
            } catch (NumberFormatException e) {
                return v1.compareTo(v2);
            }
        }
        return Integer.signum(vals1.length - vals2.length);
    }

    private void showUpdateDialog(final UpdateInfo updateInfo) {
        new AlertDialog.Builder(activity)
                .setTitle(dk.bearware.gui.R.string.update_available_title)
                .setMessage(activity.getString(dk.bearware.gui.R.string.update_available_msg, updateInfo.tagName))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (pendingApkFile != null && pendingApkFile.exists()) {
                            installApk(pendingApkFile);
                        } else {
                            downloadAndInstall(updateInfo.downloadUrl, updateInfo.tagName);
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void downloadAndInstall(String url, String tagName) {
        new DownloadTask(activity, tagName).execute(url);
    }

    private static class UpdateInfo {
        String tagName;
        String downloadUrl;

        UpdateInfo(String tagName, String downloadUrl) {
            this.tagName = tagName;
            this.downloadUrl = downloadUrl;
        }
    }

    private class DownloadTask extends AsyncTask<String, Integer, File> {
        private final Context context;
        private final String tagName;
        private ProgressDialog progressDialog;

        DownloadTask(Context context, String tagName) {
            this.context = context;
            this.tagName = tagName;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(context.getString(dk.bearware.gui.R.string.update_downloading));
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected File doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                int fileLength = connection.getContentLength();
                InputStream input = new BufferedInputStream(connection.getInputStream());
                
                File apkFile = new File(context.getExternalCacheDir(), "update.apk");
                FileOutputStream output = new FileOutputStream(apkFile);

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    if (fileLength > 0) {
                        publishProgress((int) (total * 100 / fileLength));
                    }
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

                return apkFile;
            } catch (Exception e) {
                Log.e(TAG, "Download error", e);
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(File apkFile) {
            progressDialog.dismiss();
            if (apkFile != null) {
                pendingApkFile = apkFile;
                activity.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit()
                        .putString(PREF_PENDING_APK, apkFile.getAbsolutePath())
                        .putString(PREF_PENDING_TAG, tagName)
                        .apply();
                installApk(apkFile);
            } else {
                Toast.makeText(activity, dk.bearware.gui.R.string.update_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void resumeInstallation() {
        if (pendingApkFile != null) {
            installApk(pendingApkFile);
        } else {
            String pendingPath = activity.getSharedPreferences(TAG, Context.MODE_PRIVATE).getString(PREF_PENDING_APK, "");
            if (!pendingPath.isEmpty()) {
                File file = new File(pendingPath);
                if (file.exists()) {
                    pendingApkFile = file;
                    installApk(file);
                }
            }
        }
    }

    private void installApk(File apkFile) {
        if (!apkFile.exists()) return;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri apkUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            apkUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", apkFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            apkUri = Uri.fromFile(apkFile);
        }

        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                // Should ideally show a dialog explaining why we need this, then open settings
                new AlertDialog.Builder(activity)
                    .setTitle(dk.bearware.gui.R.string.unknown_sources_title)
                    .setMessage(dk.bearware.gui.R.string.unknown_sources_msg)
                    .setPositiveButton(dk.bearware.gui.R.string.action_settings_title, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent settingsIntent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                            settingsIntent.setData(Uri.parse("package:" + activity.getPackageName()));
                            activity.startActivityForResult(settingsIntent, REQUEST_INSTALL_UNKNOWN_SOURCES);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
                return;
            }
        }

        activity.startActivity(intent);
    }

    private void clearPendingUpdate() {
        String pendingPath = activity.getSharedPreferences(TAG, Context.MODE_PRIVATE).getString(PREF_PENDING_APK, "");
        if (!pendingPath.isEmpty()) {
            File file = new File(pendingPath);
            if (file.exists()) {
                file.delete();
            }
        }
        clearCachedUpdateApk();
        activity.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit()
                .remove(PREF_PENDING_APK)
                .remove(PREF_PENDING_TAG)
                .apply();
    }

    private void clearCachedUpdateApk() {
        File apkFile = new File(activity.getExternalCacheDir(), "update.apk");
        if (apkFile.exists()) {
            if (apkFile.delete()) {
                Log.d(TAG, "Cached update APK deleted.");
            } else {
                Log.w(TAG, "Failed to delete cached update APK.");
            }
        }
    }
}
