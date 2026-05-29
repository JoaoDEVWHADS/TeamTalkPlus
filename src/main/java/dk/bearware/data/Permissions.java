
package dk.bearware.data;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import dk.bearware.gui.R;

@SuppressLint("InlinedApi")
public enum Permissions {

    RECORD_AUDIO(Manifest.permission.RECORD_AUDIO, R.string.permission_audioinput),
    MODIFY_AUDIO_SETTINGS(Manifest.permission.MODIFY_AUDIO_SETTINGS, R.string.permission_audiomodify),
    INTERNET(Manifest.permission.INTERNET, R.string.permission_internet),
    VIBRATE(Manifest.permission.VIBRATE, R.string.permission_vibrate),
    READ_EXTERNAL_STORAGE(Manifest.permission.READ_EXTERNAL_STORAGE, R.string.permission_filetx),
    WRITE_EXTERNAL_STORAGE(Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.permission_filerx),
    READ_MEDIA_IMAGES(Manifest.permission.READ_MEDIA_IMAGES, R.string.permission_imagetx),
    READ_MEDIA_VIDEO(Manifest.permission.READ_MEDIA_VIDEO, R.string.permission_videotx),
    READ_MEDIA_AUDIO(Manifest.permission.READ_MEDIA_AUDIO, R.string.permission_audiotx),
    WAKE_LOCK(Manifest.permission.WAKE_LOCK, R.string.permission_wake_lock),
    READ_PHONE_STATE(Manifest.permission.READ_PHONE_STATE, R.string.permission_read_phone_state),
    BLUETOOTH((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ? Manifest.permission.BLUETOOTH_CONNECT : Manifest.permission.BLUETOOTH, R.string.permission_bluetooth),
    POST_NOTIFICATIONS(Manifest.permission.POST_NOTIFICATIONS, R.string.permission_post_notifications),
    MANAGE_EXTERNAL_STORAGE(Manifest.permission.MANAGE_EXTERNAL_STORAGE, R.string.permission_manage_storage);

    private static final Queue<Permissions> requestsQueue = new ConcurrentLinkedQueue<>();

    private final String id;
    private final int msgResId;

    private boolean pending;

    Permissions(String id, int msgResId) {
        this.id = id;
        this.msgResId = msgResId;
        pending = false;
    }

    public boolean isGranted(@NonNull Context context) {
        if (id.equals(Manifest.permission.POST_NOTIFICATIONS)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return true; // Not required before Android 13
            }
        }
        if (id.equals(Manifest.permission.BLUETOOTH_CONNECT)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return true; // Not required before Android 12
            }
        }
        // Special handling for storage permissions
        if (id.equals(Manifest.permission.READ_EXTERNAL_STORAGE) ||
            id.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // On Android 11+, if we have MANAGE_EXTERNAL_STORAGE, these are effectively granted
                if (Environment.isExternalStorageManager()) {
                    return true;
                }
                // However, we still check standard perms if manager is not granted
            }
        }
        if (id.equals(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return Environment.isExternalStorageManager();
            }
            return true; // Not relevant for < 11
        }
        
        return ContextCompat.checkSelfPermission(context, id) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isPending() {
        return pending;
    }

    public boolean request(@NonNull Activity activity) {
        return request(activity, false);
    }

    public boolean request(@NonNull Activity activity, boolean noWarn) {
        boolean state = isGranted(activity.getBaseContext());
        if (!state) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, id)) {
                boolean busy = false;
                for (Permissions p : values())
                    if (p.pending) {
                        busy = true;
                        break;
                    }
                if (busy) {
                    requestsQueue.offer(this);
                } else {
                    emitRequest(activity);
                }
            } else if (!noWarn) {
                Toast.makeText(activity.getBaseContext(), msgResId, Toast.LENGTH_LONG).show();
            }
        }
        return state;
    }

    public void emitRequest(@NonNull Activity activity) {
        if (id.equals(Manifest.permission.MANAGE_EXTERNAL_STORAGE) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(android.net.Uri.parse("package:" + activity.getPackageName()));
            try {
                activity.startActivity(intent);
            } catch (Exception e) {
                android.util.Log.e("Permissions", "Failed to start manage storage intent, falling back", e);
                intent = new android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivity(intent);
            }
            pending = true;
            return;
        }
        ActivityCompat.requestPermissions(activity, new String[]{id}, ordinal() + 1);
        pending = true;
    }

    public static void requestAll(@NonNull Activity activity) {
        java.util.List<String> list = new java.util.ArrayList<>();
        for (Permissions p : values()) {
            // REQUEST EVERYTHING THAT IS NOT GRANTED
            if (p.isGranted(activity)) continue;

            // Filter by API level for standard dangerous permissions
            if (p == POST_NOTIFICATIONS && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) continue;
            
            // BLUETOOTH_CONNECT (Dangerous API 31+)
            if (idEquals(p.id, Manifest.permission.BLUETOOTH_CONNECT) && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) continue;

            // Media permissions (Android 13+)
            if ((p == READ_MEDIA_AUDIO || p == READ_MEDIA_IMAGES || p == READ_MEDIA_VIDEO) && 
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) continue;
            
            // Legacy/Standard storage permissions
            if (p == READ_EXTERNAL_STORAGE || p == WRITE_EXTERNAL_STORAGE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) continue;
            }

            // MANAGE_EXTERNAL_STORAGE is handled separately via Intent
            if (p == MANAGE_EXTERNAL_STORAGE) continue;
            
            // Skip non-dangerous permissions (granted at install time)
            if (p == INTERNET || p == VIBRATE || p == MODIFY_AUDIO_SETTINGS || p == WAKE_LOCK) continue;

            list.add(p.id);
        }
        
        if (!list.isEmpty()) {
            ActivityCompat.requestPermissions(activity, list.toArray(new String[0]), 1000);
        }
        
        // Handle MANAGE_EXTERNAL_STORAGE separately as it requires Intent on Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !MANAGE_EXTERNAL_STORAGE.isGranted(activity)) {
            MANAGE_EXTERNAL_STORAGE.emitRequest(activity);
        }
    }

    private static boolean idEquals(String id1, String id2) {
        if (id1 == null || id2 == null) return false;
        return id1.equals(id2);
    }

    public static void requestEssential(@NonNull Activity activity) {
        requestAll(activity);
    }

    @Nullable
    public static Permissions onRequestResult(@NonNull Activity activity, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1000) {
            return null;
        }
        Permissions permission = fromRequestCode(requestCode);
        boolean granted = (grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED);
        if (permission != null) {
            permission.pending = false;
            if (!granted) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.id)) {
                    Toast.makeText(activity.getBaseContext(), permission.msgResId, Toast.LENGTH_LONG).show();
                }
                permission = null;
            }
        }
        Permissions next = requestsQueue.poll();
        if (next != null) {
            next.emitRequest(activity);
        }
        return permission;
    }

    @Nullable
    public static Permissions fromRequestCode(int requestCode) {
        return ((requestCode > 0) && (requestCode <= values().length)) ? values()[requestCode - 1] : null;
    }

}