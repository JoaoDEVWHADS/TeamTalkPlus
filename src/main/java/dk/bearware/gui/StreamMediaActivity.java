
package dk.bearware.gui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import dk.bearware.Codec;
import dk.bearware.TeamTalkBase;
import dk.bearware.VideoCodec;
import dk.bearware.backend.TeamTalkConnection;
import dk.bearware.backend.TeamTalkConnectionListener;
import dk.bearware.backend.TeamTalkService;
import dk.bearware.data.Permissions;

public class StreamMediaActivity
extends AppCompatActivity implements TeamTalkConnectionListener {

    public static final String TAG = "bearware";
    public static final int REQUEST_STREAM_MEDIA = 1;
    private EditText file_path;
    private static final String lastMedia = "last_media_file";
    TeamTalkConnection mConnection;

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
        super.onCreate(savedInstanceState);
        mConnection = new TeamTalkConnection(this);
        setContentView(R.layout.activity_stream_media);
        EdgeToEdgeHelper.enableEdgeToEdge(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_stream_media);
        Utils.announceAccessibilityTitle(this, R.string.title_activity_stream_media);
        file_path = this.findViewById(R.id.file_path_txt);
        file_path.setText(PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString(lastMedia, ""));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.text_message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_settings) {
            return true;
        }
        else if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();        
        if (!mConnection.isBound()) {
            Intent intent = new Intent(getApplicationContext(), TeamTalkService.class);
            if(!bindService(intent, mConnection, Context.BIND_AUTO_CREATE))
                Log.e(TAG, "Failed to connect to TeamTalk service");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mConnection.isBound()) {
            onServiceDisconnected(getService());
            unbindService(mConnection);
            mConnection.setBound(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permissions granted = Permissions.onRequestResult(this, requestCode, grantResults);
        if (granted == null)
            return;
        switch (granted) {
            case READ_EXTERNAL_STORAGE:
            case READ_MEDIA_VIDEO:
            case READ_MEDIA_AUDIO:
                if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) || areMediaPermissionsComplete())
                    mediaSelectionStart();
                break;
        default:
            break;
        }
    }

    @Override
    public void onServiceConnected(TeamTalkService service) {
        Button browse_btn = this.findViewById(R.id.media_file_select_btn);
        Button stream_btn = this.findViewById(R.id.media_file_stream_btn);

        OnClickListener listener = v -> {
            switch(v.getId()) {
                case R.id.media_file_select_btn :
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                    requestMediaPermissions() :
                    Permissions.READ_EXTERNAL_STORAGE.request(this)) {
                        mediaSelectionStart();
                    }
                    break;
                case R.id.media_file_stream_btn :
                    String path = file_path.getText().toString();
                    if(path.isEmpty())
                        return;
                    VideoCodec videocodec = new VideoCodec();
                    videocodec.nCodec = Codec.NO_CODEC;
                    if (!getClient().startStreamingMediaFileToChannel(path, videocodec)) {
                        Toast.makeText(StreamMediaActivity.this,
                        R.string.err_stream_media,
                        Toast.LENGTH_LONG).show();
                    } else {
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
                        editor.putString(lastMedia, path).apply();
                        finish();
                    }
                    break;
            }
        };

        browse_btn.setOnClickListener(listener);
        stream_btn.setOnClickListener(listener);
    }

    @Override
    public void onServiceDisconnected(TeamTalkService service) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_STREAM_MEDIA) && (resultCode == RESULT_OK)) {
            Uri uri = data.getData();
            String path = AbsolutePathHelper.getRealPath(this.getBaseContext(), uri);
            if (path != null) {
                file_path.setText(path);
            } else {
                 new FileCopyingTask().execute(uri);
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void mediaSelectionStart() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        Intent i = Intent.createChooser(intent, "File");
        startActivityForResult(i, REQUEST_STREAM_MEDIA);
    }

    private boolean requestMediaPermissions() {
        boolean video = Permissions.READ_MEDIA_VIDEO.request(this);
        boolean audio = Permissions.READ_MEDIA_AUDIO.request(this);
        return areMediaPermissionsComplete() && (video || audio);
    }

    private boolean areMediaPermissionsComplete() {
        return !(Permissions.READ_MEDIA_VIDEO.isPending() ||
                 Permissions.READ_MEDIA_AUDIO.isPending());
    }

    private class FileCopyingTask extends AsyncTask<Uri, Void, String> {

        @Override
        protected String doInBackground(Uri... uris) {
            Uri uri = uris[0];
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            int columnIndex = ((cursor != null) && cursor.moveToFirst()) ? cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) : -1;
            if (columnIndex >= 0) {
                File transitFile = new File(getCacheDir(), cursor.getString(columnIndex));
                cursor.close();
                try {
                    if (((!transitFile.exists()) || transitFile.delete()) && transitFile.createNewFile()) {
                        transitFile.deleteOnExit();
                    } else {
                        return null;
                    }
                } catch (Exception ex) {
                    return null;
                }
                try (InputStream src = getContentResolver().openInputStream(uri);
                     FileOutputStream dest = new FileOutputStream(transitFile)) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = src.read(buffer)) > 0) {
                        dest.write(buffer, 0, read);
                    }
                } catch (Exception ex) {
                    return null;
                }
                return transitFile.getPath();
            } else if (cursor != null) {
                cursor.close();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String path) {
            if (path != null) {
                file_path.setText(path);
            } else {
                Toast.makeText(StreamMediaActivity.this, R.string.err_resolve_file_path, Toast.LENGTH_LONG).show();
            }
        }

    }

}