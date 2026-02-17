
package dk.bearware.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.ListFragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;

import dk.bearware.Channel;
import dk.bearware.ClientFlag;
import dk.bearware.ClientStatistics;
import dk.bearware.RemoteFile;
import dk.bearware.ServerProperties;
import dk.bearware.SoundDeviceConstants;
import dk.bearware.SoundLevel;
import dk.bearware.Subscription;
import dk.bearware.TeamTalkBase;
import dk.bearware.TextMessage;
import dk.bearware.TextMsgType;
import dk.bearware.User;
import dk.bearware.UserAccount;
import dk.bearware.UserRight;
import dk.bearware.UserState;
import dk.bearware.UserType;
import dk.bearware.backend.OnVoiceTransmissionToggleListener;
import dk.bearware.backend.TeamTalkConnection;
import dk.bearware.backend.TeamTalkConnectionListener;
import dk.bearware.backend.TeamTalkConstants;
import dk.bearware.backend.TeamTalkService;
import dk.bearware.data.FileListAdapter;
import dk.bearware.data.MediaAdapter;
import dk.bearware.data.MyTextMessage;
import dk.bearware.data.Permissions;
import dk.bearware.data.Preferences;
import dk.bearware.data.ServerEntry;
import dk.bearware.data.TTSWrapper;
import dk.bearware.data.TextMessageAdapter;
import dk.bearware.events.ClientEventListener;
import dk.bearware.utils.PrefsHelper;

public class MainActivity
extends AppCompatActivity
        implements TeamTalkConnectionListener,
        OnItemClickListener,
        OnItemLongClickListener,
        OnMenuItemClickListener,
        SensorEventListener,
        OnVoiceTransmissionToggleListener,
        ClientEventListener.OnConnectionLostListener,
        ClientEventListener.OnCmdProcessingListener,
        ClientEventListener.OnCmdMyselfLoggedInListener,
        ClientEventListener.OnCmdMyselfLoggedOutListener,
        ClientEventListener.OnCmdMyselfKickedFromChannelListener,
        ClientEventListener.OnCmdUserUpdateListener,
        ClientEventListener.OnCmdUserLeftChannelListener,
        ClientEventListener.OnCmdChannelNewListener,
        ClientEventListener.OnCmdUserTextMessageListener,
        ClientEventListener.OnCmdUserJoinedChannelListener,
        ClientEventListener.OnCmdChannelRemoveListener,
        ClientEventListener.OnCmdChannelUpdateListener,
        ClientEventListener.OnCmdUserLoggedOutListener,
        ClientEventListener.OnCmdUserLoggedInListener,
        ClientEventListener.OnCmdFileRemoveListener,
        ClientEventListener.OnUserStateChangeListener,
        ClientEventListener.OnVoiceActivationListener,
        ClientEventListener.OnCmdFileNewListener {

    private static final int MIC_INPUT_DEFAULT = 0;
    private static final int MIC_INPUT_INTERNAL = 1;
    private static final int MIC_INPUT_EXTERNAL = 2;
    private int currentMicInput = MIC_INPUT_DEFAULT;
    
    private ImageButton micInputButton;
    private BroadcastReceiver headsetReceiver;


    SectionsPagerAdapter mSectionsPagerAdapter;

    ViewPager mViewPager;
    TabLayout mTabLayout;

    public static final String TAG = "bearware";

    private static final String MSG_NOTIFICATION_CHANNEL_ID = "TT_PM";

    public final int REQUEST_EDITCHANNEL = 1,
                     REQUEST_NEWCHANNEL = 2,
                     REQUEST_EDITUSER = 3,
                     REQUEST_SELECT_FILE = 4;

    TeamTalkConnection mConnection;

    Channel curchannel;

    Channel mychannel;

    SparseArray<CmdComplete> activecmds = new SparseArray<>();

    ChannelListAdapter channelsAdapter;
    FileListAdapter filesAdapter;
    TextMessageAdapter textmsgAdapter;
    TextMessageAdapter channelChatAdapter, globalChatAdapter, eventHistoryAdapter;
    PrivateConversationsAdapter privateConversationsAdapter;
    OnlineUsersAdapter onlineUsersAdapter;
    MediaAdapter mediaAdapter;
    TTSWrapper ttsWrapper = null;
    AccessibilityAssistant accessibilityAssistant;
    AudioManager audioManager;
    SoundPool audioIcons;
    NotificationManager notificationManager;
    WakeLock wakeLock, proximityWakeLock;
    boolean restarting;
    SensorManager mSensorManager;
    Sensor mSensor;
    boolean isProximitySensorRegistered = false;
    Map<Integer, User> users = new HashMap<>();
    Map<Integer, Integer> prevChannels = new HashMap<>();
    java.util.Set<Integer> userIdsWithMessages = new java.util.TreeSet<>();

    static final String MESSAGE_NOTIFICATION_TAG = "incoming_message";

    final int SOUND_VOICETXON = 1,
              SOUND_VOICETXOFF = 2,
              SOUND_USERMSG = 3,
              SOUND_CHANMSG = 4,
              SOUND_BCASTMSG = 5,
              SOUND_SERVERLOST = 6,
              SOUND_FILESUPDATE = 7,
              SOUND_VOXENABLE = 8,
              SOUND_VOXDISABLE = 9,
              SOUND_VOXON = 10,
              SOUND_VOXOFF = 11,
              SOUND_TXREADY = 12,
              SOUND_TXSTOP = 13,
              SOUND_USERJOIN = 14,
              SOUND_USERLEFT = 15,
              SOUND_USERLOGGEDIN = 16,
              SOUND_USERLOGGEDOFF = 17,
              SOUND_INTERCEPTON = 18,
              SOUND_INTERCEPTOFF = 19,
              SOUND_CHANMSGSENT = 20;

    SparseIntArray sounds = new SparseIntArray();

    private Context ctx;
    private PrefsHelper prefs;

    public ChannelListAdapter getChannelsAdapter() {
        return channelsAdapter;
    }

    public FileListAdapter getFilesAdapter() {
        return filesAdapter;
    }

    public TextMessageAdapter getTextMessagesAdapter() {
        return textmsgAdapter;
    }

    public MediaAdapter getMediaAdapter() {
        return mediaAdapter;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = getApplicationContext();
        prefs = new PrefsHelper(ctx);

        mConnection = new TeamTalkConnection(this);
        setContentView(R.layout.activity_main);
        EdgeToEdgeHelper.enableEdgeToEdge(this);

        String serverName = getIntent().getStringExtra(ServerEntry.KEY_SERVERNAME);
        if ((serverName != null) && !serverName.isEmpty())
            setTitle(serverName);
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayHomeAsUpEnabled(true);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        restarting = (savedInstanceState != null);
        accessibilityAssistant = new AccessibilityAssistant(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        wakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":TeamTalk5");
        proximityWakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG + ":TeamTalk5");
        wakeLock.setReferenceCounted(false);
        proximityWakeLock.setReferenceCounted(false);

        channelsAdapter = new ChannelListAdapter(this.getBaseContext());
        filesAdapter = new FileListAdapter(this, this, accessibilityAssistant);
        textmsgAdapter = new TextMessageAdapter(this, accessibilityAssistant);
        channelChatAdapter = new TextMessageAdapter(this, accessibilityAssistant);
        channelChatAdapter.setFilterMsgType(TextMsgType.MSGTYPE_CHANNEL);
        globalChatAdapter = new TextMessageAdapter(this, accessibilityAssistant);
        globalChatAdapter.setFilterMsgType(TextMsgType.MSGTYPE_BROADCAST);
        eventHistoryAdapter = new TextMessageAdapter(this, accessibilityAssistant);
        
        java.util.Set<Integer> excludedTypes = new java.util.HashSet<>();
        excludedTypes.add(TextMsgType.MSGTYPE_CHANNEL);
        excludedTypes.add(TextMsgType.MSGTYPE_BROADCAST);
        eventHistoryAdapter.setExcludedMsgTypes(excludedTypes);

        privateConversationsAdapter = new PrivateConversationsAdapter(this);
        mediaAdapter = new MediaAdapter(this.getBaseContext());

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mTabLayout = findViewById(R.id.tab_layout);

        mViewPager = findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(mSectionsPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
        mViewPager.setCurrentItem(mSectionsPagerAdapter.getPositionForId(SectionsPagerAdapter.CHANNELS_PAGE));

        setupButtons();
        
        micInputButton = findViewById(R.id.mic_input_switch);
        micInputButton.setOnClickListener(v -> showMicInputSelectionDialog());

        headsetReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
                    int state = intent.getIntExtra("state", -1);
                    if (state == 1) {
                         micInputButton.setVisibility(View.VISIBLE);
                    } else {
                         micInputButton.setVisibility(View.GONE);
                         // Revert to default if headset unplugged
                         setAudioInputDevice(MIC_INPUT_DEFAULT); 
                    }
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final MediaPlayer mMediaPlayer;
            mMediaPlayer = MediaPlayer.create(ctx, R.raw.silence);
            mMediaPlayer.setOnCompletionListener(mediaPlayer -> mMediaPlayer.release());
            mMediaPlayer.start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        UserAccount myuseraccount = new UserAccount();
        getClient().getMyUserAccount(myuseraccount);

        int currentPage = mSectionsPagerAdapter.getIdForPosition(mViewPager.getCurrentItem());

        boolean uploadRight = (myuseraccount.uUserRights & UserRight.USERRIGHT_UPLOAD_FILES) != UserRight.USERRIGHT_NONE;
        boolean isEditable = curchannel != null;
        boolean isJoinable = curchannel != null && getClient().getMyChannelID() != curchannel.nChannelID && curchannel.nMaxUsers > 0;
        boolean isLeaveable = getClient().getMyChannelID() > 0;
        boolean isMyChannel = curchannel != null && getClient().getMyChannelID() == curchannel.nChannelID;

        boolean inChannelsTab = currentPage == SectionsPagerAdapter.CHANNELS_PAGE;
        
        boolean isOperator = false;
        if (curchannel != null && getClient().isChannelOperator(getClient().getMyUserID(), curchannel.nChannelID)) {
             isOperator = true;
        }

        boolean canBan = (myuseraccount.uUserRights & UserRight.USERRIGHT_BAN_USERS) != UserRight.USERRIGHT_NONE;
        boolean canMove = (myuseraccount.uUserRights & UserRight.USERRIGHT_MOVE_USERS) != UserRight.USERRIGHT_NONE;
        boolean canCreateChannel = (myuseraccount.uUserRights & UserRight.USERRIGHT_MODIFY_CHANNELS) != UserRight.USERRIGHT_NONE ||
                                   (myuseraccount.uUserRights & UserRight.USERRIGHT_CREATE_TEMPORARY_CHANNEL) != UserRight.USERRIGHT_NONE;
        
        menu.findItem(R.id.action_edit).setEnabled(isEditable).setVisible(isEditable && inChannelsTab);
        menu.findItem(R.id.action_join).setEnabled(isJoinable).setVisible(isJoinable && inChannelsTab);
        menu.findItem(R.id.action_leave).setEnabled(isLeaveable).setVisible(isLeaveable && inChannelsTab);
        menu.findItem(R.id.action_move).setEnabled(canMove && isLeaveable).setVisible(canMove && isLeaveable && inChannelsTab && curchannel != null);
        menu.findItem(R.id.action_banned_users).setEnabled((isOperator || canBan) && isLeaveable).setVisible((isOperator || canBan) && isLeaveable && inChannelsTab && curchannel != null);
        menu.findItem(R.id.action_newchannel).setVisible(canCreateChannel && inChannelsTab && curchannel != null);

        boolean inFilesTab = currentPage == SectionsPagerAdapter.FILES_PAGE;
        menu.findItem(R.id.action_upload).setEnabled(uploadRight).setVisible(uploadRight && inFilesTab);

        boolean inMediaTab = currentPage == SectionsPagerAdapter.MEDIA_PAGE;
        menu.findItem(R.id.action_stream).setEnabled(isMyChannel).setVisible(isMyChannel && inMediaTab);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        switch(item.getItemId()) {
            case R.id.action_join : {
                if (curchannel != null)
                    joinChannel(curchannel);
            }
            break;
            case R.id.action_leave : {
                    leaveChannel();
            }
            break;
            case R.id.action_move : {
                Intent intent = new Intent(this, dk.bearware.gui.MoveUsersActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.action_banned_users : {
                Intent intent = new Intent(this, dk.bearware.gui.ChannelBannedUsersActivity.class);
                if (curchannel != null) {
                    intent.putExtra("channel_id", curchannel.nChannelID);
                }
                startActivity(intent);
            }
            break;
            case R.id.action_upload : {
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                    requestMediaPermissions() :
                    Permissions.READ_EXTERNAL_STORAGE.request(this)) {
                    fileSelectionStart();
                }
            }
            break;
            case R.id.action_stream : {
                int flags = getClient().getFlags();
                if ((flags & ClientFlag.CLIENT_STREAM_AUDIO) == ClientFlag.CLIENT_STREAM_AUDIO || (flags & ClientFlag.CLIENT_STREAM_VIDEO) == ClientFlag.CLIENT_STREAM_VIDEO) {
                    getClient().stopStreamingMediaFileToChannel();
                } else {
                    Intent intent = new Intent(MainActivity.this, StreamMediaActivity.class);
                    startActivity(intent);
                }
            }
            break;
            case R.id.action_edit : {
                if (curchannel != null)
                    editChannelProperties(curchannel);
            }
            break;

            case R.id.action_newchannel : {
                Intent intent = new Intent(MainActivity.this, ChannelPropActivity.class);

                int parent_chan_id = getClient().getRootChannelID();
                if(curchannel != null)
                    parent_chan_id = curchannel.nChannelID;
                intent = intent.putExtra(ChannelPropActivity.EXTRA_PARENTID, parent_chan_id);

                startActivityForResult(intent, REQUEST_NEWCHANNEL);
            }
            break;
            case R.id.action_settings : {
                Intent intent = new Intent(MainActivity.this, PreferencesActivity.class);
                startActivity(intent);
                break;
            }
            case android.R.id.home : {
                int currentPage = mSectionsPagerAdapter.getIdForPosition(mViewPager.getCurrentItem());
                Channel parentChannel = ((currentPage == SectionsPagerAdapter.CHANNELS_PAGE)
                                         && (curchannel != null)
                                         ) ?
                    getService().getChannels().get(curchannel.nParentID) :
                    null;
                if (currentPage != SectionsPagerAdapter.CHANNELS_PAGE) {
                    mViewPager.setCurrentItem(mSectionsPagerAdapter.getPositionForId(SectionsPagerAdapter.CHANNELS_PAGE));
                } else if ((curchannel != null)) {
                    setCurrentChannel(parentChannel);
                    channelsAdapter.notifyDataSetChanged();
                }
                else if (filesAdapter.getActiveTransfersCount() > 0) {
                    alert.setMessage(R.string.disconnect_alert);
                    alert.setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                        filesAdapter.cancelAllTransfers();
                        finish();
                    });
                    alert.setNegativeButton(android.R.string.cancel, null);
                    alert.show();
                }
                else {
                    finish();
                }
                break;
            }
            default :
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ttsWrapper == null)
            ttsWrapper = new TTSWrapper(this, prefs.get("pref_speech_engine", TTSWrapper.defaultEngineName));

        if (!mConnection.isBound()) {

            Intent intent = new Intent(ctx, TeamTalkService.class);
            Log.d(TAG, "Connecting to TeamTalk service");
            if(!bindService(intent, mConnection, Context.BIND_AUTO_CREATE))
                Log.e(TAG, "Failed to connect to TeamTalk service");
        }
        else {
            adjustSoundSystem();
            if (prefs.get(Preferences.PREF_SOUNDSYSTEM_BLUETOOTH_HEADSET, false)) {
                if (Permissions.BLUETOOTH.request(this))
                    getService().watchBluetoothHeadset();
            }
            else getService().unwatchBluetoothHeadset();

            int mastervol = prefs.get(Preferences.PREF_SOUNDSYSTEM_MASTERVOLUME, SoundLevel.SOUND_VOLUME_DEFAULT);
            int gain = prefs.get(Preferences.PREF_SOUNDSYSTEM_MICROPHONEGAIN, SoundLevel.SOUND_GAIN_DEFAULT);
            int voxlevel = prefs.get(Preferences.PREF_SOUNDSYSTEM_VOICEACTIVATION_LEVEL, 5);
            boolean voxState = getService().isVoiceActivationEnabled();
            boolean txState = getService().isVoiceTransmitting();

            if (getClient().getSoundOutputVolume() != mastervol)
                getClient().setSoundOutputVolume(mastervol);
            if (getClient().getSoundInputGainLevel() != gain)
                getClient().setSoundInputGainLevel(gain);
            if (getClient().getVoiceActivationLevel() != voxlevel)
                getClient().setVoiceActivationLevel(voxlevel);

            adjustMuteButton(findViewById(R.id.speakerBtn));
            adjustVoxState(voxState, voxState ? voxlevel : gain);
            adjustTxState(txState);

            final SeekBar masterSeekBar = findViewById(R.id.master_volSeekBar);
            final SeekBar micSeekBar = findViewById(R.id.mic_gainSeekBar);
            masterSeekBar.setProgress(Utils.refVolumeToPercent(getClient().getSoundOutputVolume()));
            if (getService().isVoiceActivationEnabled()) {
                micSeekBar.setProgress(getClient().getVoiceActivationLevel());
            } else {
                micSeekBar.setProgress(Utils.refVolumeToPercent(getClient().getSoundInputGainLevel()));
            }
            TextView volLevel = findViewById(R.id.vollevel_text);
            volLevel.setText(Utils.refVolumeToPercent(mastervol) + "%");
            volLevel.setContentDescription(getString(R.string.speaker_volume_description, volLevel.getText()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        boolean proximitySensor = prefs.get("proximity_sensor_checkbox", false);
        if (proximitySensor) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            isProximitySensorRegistered = true;
        }

        if (audioIcons != null)
            audioIcons.release();
        sounds.clear();

        audioIcons = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

        if (prefs.get("server_lost_audio_icon", true)) {
            sounds.put(SOUND_SERVERLOST, audioIcons.load(ctx, R.raw.serverlost, 1));
        }
        if (prefs.get("rx_tx_audio_icon", true)) {
            sounds.put(SOUND_VOICETXON, audioIcons.load(ctx, R.raw.on, 1));
            sounds.put(SOUND_VOICETXOFF, audioIcons.load(ctx, R.raw.off, 1));
        }
        if (prefs.get("private_message_audio_icon", true)) {
            sounds.put(SOUND_USERMSG, audioIcons.load(ctx, R.raw.user_message, 1));
        }
        if (prefs.get("channel_message_audio_icon", true)) {
            sounds.put(SOUND_CHANMSG, audioIcons.load(ctx, R.raw.channel_message, 1));
        }
        if (prefs.get("channel_message_sent_audio_icon", true)) {
            sounds.put(SOUND_CHANMSGSENT, audioIcons.load(ctx, R.raw.channel_message_sent, 1));
        }
        if (prefs.get("broadcast_message_audio_icon", true)) {
            sounds.put(SOUND_BCASTMSG, audioIcons.load(ctx, R.raw.broadcast_message, 1));
        }
        if (prefs.get("files_updated_audio_icon", true)) {
            sounds.put(SOUND_FILESUPDATE, audioIcons.load(ctx, R.raw.fileupdate, 1));
        }
        if (prefs.get("voiceact_audio_icon", true)) {
            sounds.put(SOUND_VOXENABLE, audioIcons.load(ctx, R.raw.voiceact_enable, 1));
            sounds.put(SOUND_VOXDISABLE, audioIcons.load(ctx, R.raw.voiceact_disable, 1));
        }
        if (prefs.get("voiceact_triggered_icon", true)) {
            sounds.put(SOUND_VOXON, audioIcons.load(ctx, R.raw.voiceact_on, 1));
            sounds.put(SOUND_VOXOFF, audioIcons.load(ctx, R.raw.voiceact_off, 1));
        }
        if (prefs.get("intercept_audio_icon", true)) {
            sounds.put(SOUND_INTERCEPTON, audioIcons.load(ctx, R.raw.intercept, 1));
            sounds.put(SOUND_INTERCEPTOFF, audioIcons.load(ctx, R.raw.interceptend, 1));
        }
        if (prefs.get("transmitready_icon", true)) {
            sounds.put(SOUND_TXREADY, audioIcons.load(ctx, R.raw.txqueue_start, 1));
            sounds.put(SOUND_TXSTOP, audioIcons.load(ctx, R.raw.txqueue_stop, 1));
        }
        if (prefs.get("userjoin_icon", true)) {
            sounds.put(SOUND_USERJOIN, audioIcons.load(ctx, R.raw.user_join, 1));
        }
        if (prefs.get("userleft_icon", true)) {
            sounds.put(SOUND_USERLEFT, audioIcons.load(ctx, R.raw.user_left, 1));
        }
        if (prefs.get("userloggedin_icon", true)) {
            sounds.put(SOUND_USERLOGGEDIN, audioIcons.load(ctx, R.raw.logged_on, 1));
        }
        if (prefs.get("userloggedoff_icon", true)) {
            sounds.put(SOUND_USERLOGGEDOFF, audioIcons.load(ctx, R.raw.logged_off, 1));
        }

        getTextMessagesAdapter().showLogMessages(prefs.get("show_log_messages", true));

        getWindow().getDecorView().setKeepScreenOn(prefs.get("keep_screen_on_checkbox", false));

        ttsWrapper.useAnnouncements = prefs.get("pref_use_announcements", false);
        ttsWrapper.setAccessibilityStream(prefs.get("pref_a11y_volume", false));
        ttsWrapper.switchEngine(prefs.get("pref_speech_engine", TTSWrapper.defaultEngineName));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(headsetReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (isFinishing()) {
            if (audioIcons != null) {
                audioIcons.release();
                audioIcons = null;
            }
            if (ttsWrapper != null) {
                ttsWrapper.shutdown();
                ttsWrapper = null;
            }

            audioManager.setMode(AudioManager.MODE_NORMAL);

            if (mConnection.isBound()) {
                Log.d(TAG, "Disconnecting from TeamTalk service");
                getService().disablePhoneCallReaction();
                getService().unwatchBluetoothHeadset();
                getService().resetState();

                onServiceDisconnected(getService());
                unbindService(mConnection);
                mConnection.setBound(false);
            }
            notificationManager.cancelAll();
            mViewPager.removeOnPageChangeListener(mSectionsPagerAdapter);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isProximitySensorRegistered) {
            mSensorManager.unregisterListener(this);
            isProximitySensorRegistered = false;
        }

        if(mConnection.isBound()) {
            Log.d(TAG, "Disconnecting from TeamTalk service");

            onServiceDisconnected(getService());
            unbindService(mConnection);
            mConnection.setBound(false);
        }

        Log.d(TAG, "Activity destroyed " + this.hashCode());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_SELECT_FILE) && (resultCode == RESULT_OK)) {
            Uri uri = data.getData();
            String path = AbsolutePathHelper.getRealPath(this.getBaseContext(), uri);
            if (path != null) {
                File localFile = new File(path);
                if (localFile.canRead()) {
                    startFileUpload(path);
                } else {
                    Toast.makeText(this, getString(R.string.upload_failed, path), Toast.LENGTH_LONG).show();
                }
            } else {
                new FileCopyingTask().execute(uri);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private boolean startFileUpload(String path) {
        String remoteName = filesAdapter.getRemoteName(path);
        if (remoteName != null) {
            Toast.makeText(this, getString(R.string.remote_file_exists, remoteName), Toast.LENGTH_LONG).show();
        } else if (getClient().doSendFile(curchannel.nChannelID, path) <= 0) {
            Toast.makeText(this, getString(R.string.upload_failed, path), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.upload_started, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
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
            if ((path != null) && !startFileUpload(path)) {
                File transitFile = new File(path);
                transitFile.delete();
            }
        }

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        boolean proximitySensor = prefs.get("proximity_sensor_checkbox", false);
        if (proximitySensor && (mConnection != null) && mConnection.isBound() && !getService().isInPhoneCall()) {
            if (event.values[0] == 0) {
                proximityWakeLock.acquire();
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.setSpeakerphoneOn(false);
                getService().enableVoiceTransmission(true);
            } else {
                proximityWakeLock.release();
                adjustSoundSystem();
                if (getService().isVoiceTransmissionEnabled())
                    getService().enableVoiceTransmission(false);
            }
        }
    }

    private void showMicInputSelectionDialog() {
        final CharSequence[] items = {
            getString(R.string.mic_input_default),
            getString(R.string.mic_input_internal),
            getString(R.string.mic_input_external)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.mic_input_selection));
        builder.setSingleChoiceItems(items, currentMicInput, (dialog, item) -> {
            setAudioInputDevice(item);
            dialog.dismiss();
        });
        builder.show();
    }

    private void setAudioInputDevice(int inputType) {
        currentMicInput = inputType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (inputType == MIC_INPUT_DEFAULT) {
               audioManager.clearCommunicationDevice();
               return;
            }

            AudioDeviceInfo targetDevice = null;
            List<AudioDeviceInfo> devices = audioManager.getAvailableCommunicationDevices();
            for (AudioDeviceInfo device : devices) {
                if (inputType == MIC_INPUT_INTERNAL && device.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                    targetDevice = device;
                    break;
                } else if (inputType == MIC_INPUT_EXTERNAL && (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET || device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES)) {
                     targetDevice = device;
                     break;
                }
            }
            if (targetDevice != null) {
                audioManager.setCommunicationDevice(targetDevice);
            }
        } else {
            // Fallback for older Android versions
            // This is a "hack" as speakerphone primarily controls output but often effects input routing
            if (inputType == MIC_INPUT_INTERNAL) {
                audioManager.setSpeakerphoneOn(true);
            } else if (inputType == MIC_INPUT_EXTERNAL) {
                audioManager.setSpeakerphoneOn(false); 
            } else {
                audioManager.setSpeakerphoneOn(false); // Default usually means headset if plugged in
            }
        }
    }

    MediaSectionFragment mediaFragment;
    FilesSectionFragment filesFragment;
    GlobalSectionFragment globalFragment;

    ChatSectionFragment channelChatFragment;
    ChannelsSectionFragment channelsFragment;
    PrivateSectionFragment privateChatFragment;
    SettingsSectionFragment settingsFragment;

    public class SectionsPagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {

        public static final int FILES_PAGE          = 0,
                                CHANNELS_PAGE       = 1,
                                MEDIA_PAGE          = 2,
                                MANAGEMENT_PAGE     = 3,
                                GLOBAL_PAGE         = 4,
                                EVENT_HISTORY_PAGE  = 5,
                                CHAT_PAGE           = 6,
                                SETTINGS_PAGE       = 7,
                                PRIVATE_PAGE        = 8,
                                CONNECTION_PAGE     = 9,
                                ONLINE_USERS_PAGE   = 10,
                                MANAGEMENT_STATUS_PAGE = 11,

                                PAGE_COUNT          = 12;

        private class PageItem {
            String title;
            int id;
            PageItem(String t, int i) { title = t; id = i; }
        }

        private final ArrayList<Integer> pageOrder = new ArrayList<>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            
            List<PageItem> pages = new ArrayList<>();
            Locale l = Locale.getDefault();
            
            pages.add(new PageItem(getString(R.string.title_section_files).toUpperCase(l), FILES_PAGE));
            pages.add(new PageItem(getString(R.string.title_section_channels).toUpperCase(l), CHANNELS_PAGE));
            pages.add(new PageItem(getString(R.string.title_section_media).toUpperCase(l), MEDIA_PAGE));
            pages.add(new PageItem(getString(R.string.title_section_management).toUpperCase(l), MANAGEMENT_PAGE));
            pages.add(new PageItem(getString(R.string.title_section_chat).toUpperCase(l), GLOBAL_PAGE));
            pages.add(new PageItem(getString(R.string.title_section_event_history).toUpperCase(l), EVENT_HISTORY_PAGE));
            pages.add(new PageItem(getString(R.string.title_section_channel_chat).toUpperCase(l), CHAT_PAGE));
            pages.add(new PageItem(getString(R.string.title_section_settings).toUpperCase(l), SETTINGS_PAGE));
            pages.add(new PageItem(getString(R.string.title_section_private).toUpperCase(l), PRIVATE_PAGE));
            pages.add(new PageItem(getString(R.string.title_section_connection).toUpperCase(l), CONNECTION_PAGE));
            pages.add(new PageItem(getString(R.string.title_section_online_users).toUpperCase(l), ONLINE_USERS_PAGE));
            pages.add(new PageItem(getString(R.string.title_section_status).toUpperCase(l), MANAGEMENT_STATUS_PAGE));
            
            Collections.sort(pages, (p1, p2) -> p1.title.compareTo(p2.title));
            
            for(PageItem p : pages) {
                pageOrder.add(p.id);
            }
        }
        
        public int getPositionForId(int id) {
            return pageOrder.indexOf(id);
        }

        public int getIdForPosition(int pos) {
            if (pos >= 0 && pos < pageOrder.size())
                return pageOrder.get(pos);
            return -1;
        }

        @Override @NonNull
        public Fragment getItem(int position) {
            int id = getIdForPosition(position);

            switch(id) {
                default :
                case CHANNELS_PAGE :
                    return new ChannelsSectionFragment();
                case CHAT_PAGE :
                    return new ChatSectionFragment();
                case GLOBAL_PAGE :
                    return new GlobalSectionFragment();
                case EVENT_HISTORY_PAGE :
                    return new EventHistorySectionFragment();
                case PRIVATE_PAGE :
                    return new PrivateSectionFragment();
                case MEDIA_PAGE :
                    return new MediaSectionFragment();
                case FILES_PAGE :
                    return new FilesSectionFragment();
                case ONLINE_USERS_PAGE :
                    return new OnlineUsersSectionFragment();
                case MANAGEMENT_PAGE :
                    return new ManagementSectionFragment();
                case SETTINGS_PAGE :
                    return new SettingsSectionFragment();
                case CONNECTION_PAGE :
                    return new ConnectionStatusSectionFragment();
                case MANAGEMENT_STATUS_PAGE :
                    return new ManageStatusFragment();
            }
        }

        @Override
        public int getCount() {
            return pageOrder.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            int id = getIdForPosition(position);
            Locale l = Locale.getDefault();
            switch(id) {
                case CHANNELS_PAGE :
                    return getString(R.string.title_section_channels).toUpperCase(l);
                case CHAT_PAGE :
                    return getString(R.string.title_section_channel_chat).toUpperCase(l);
                case GLOBAL_PAGE :
                    return getString(R.string.title_section_chat).toUpperCase(l);
                case EVENT_HISTORY_PAGE :
                    return getString(R.string.title_section_event_history).toUpperCase(l);
                case PRIVATE_PAGE :
                    return getString(R.string.title_section_private).toUpperCase(l);
                case MEDIA_PAGE :
                    return getString(R.string.title_section_media).toUpperCase(l);
                case FILES_PAGE :
                    return getString(R.string.title_section_files).toUpperCase(l);
                case ONLINE_USERS_PAGE :
                    return getString(R.string.title_section_online_users).toUpperCase(l);
                case MANAGEMENT_PAGE :
                    return getString(R.string.title_section_management).toUpperCase(l);
                case SETTINGS_PAGE:
                    return getString(R.string.title_section_settings).toUpperCase(l);
                case CONNECTION_PAGE:
                    return getString(R.string.title_section_connection).toUpperCase(l);
                case MANAGEMENT_STATUS_PAGE:
                    return getString(R.string.title_section_status).toUpperCase(l);
            }
            return null;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            int id = getIdForPosition(position);
            
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            View v = getCurrentFocus();
            if (v != null)
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            
            accessibilityAssistant.setVisiblePage(id);
            invalidateOptionsMenu();
            
            if (id == SETTINGS_PAGE) {
                mTabLayout.setVisibility(View.GONE);
            } else {
                mTabLayout.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    private void fileSelectionStart() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        Intent i = Intent.createChooser(intent, "File");
        startActivityForResult(i, REQUEST_SELECT_FILE);
    }

    private boolean requestMediaPermissions() {
        Permissions.READ_MEDIA_IMAGES.request(this, true);
        Permissions.READ_MEDIA_VIDEO.request(this, true);
        Permissions.READ_MEDIA_AUDIO.request(this, true);
        return areMediaPermissionsComplete();
    }

    private boolean areMediaPermissionsComplete() {
        return !(Permissions.READ_MEDIA_IMAGES.isPending() ||
                 Permissions.READ_MEDIA_VIDEO.isPending() ||
                 Permissions.READ_MEDIA_AUDIO.isPending());
    }

    private void editChannelProperties(Channel channel) {
        Intent intent = new Intent(this, ChannelPropActivity.class);
        startActivityForResult(intent.putExtra(ChannelPropActivity.EXTRA_CHANNELID, channel.nChannelID), REQUEST_EDITCHANNEL);
    }

    private void leaveChannel() {
        getClient().doLeaveChannel();
        accessibilityAssistant.lockEvents();
        channelsAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();
    }

    private void joinChannelUnsafe(Channel channel, String passwd) {
        int cmdid = getClient().doJoinChannelByID(channel.nChannelID, passwd);
        if(cmdid>0) {
            activecmds.put(cmdid, CmdComplete.CMD_COMPLETE_JOIN);
            channel.szPassword = passwd;
            getService().setJoinChannel(channel);
        }
        else {
            Toast.makeText(this, R.string.text_con_cmderr, Toast.LENGTH_LONG).show();
        }
    }

    private void joinChannel(final Channel channel, final String passwd) {
        if (filesAdapter.getActiveTransfersCount() > 0) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setMessage(R.string.channel_change_alert);
            alert.setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                filesAdapter.cancelAllTransfers();
                joinChannelUnsafe(channel, passwd);
            });
            alert.setNegativeButton(android.R.string.cancel, null);
            alert.show();
        }

        else {
            joinChannelUnsafe(channel, passwd);
        }
    }

    private void joinChannel(final Channel channel) {
        if(channel.bPassword) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(R.string.pref_title_join_channel);
            alert.setMessage(R.string.channel_password_prompt);
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_CLASS_TEXT);
            input.setText(channel.szPassword);
            input.requestFocus();
            alert.setView(input);
            alert.setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(input.getWindowToken(), 0);
                joinChannel(channel, input.getText().toString());
            });
            alert.setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> {
                InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(input.getWindowToken(), 0);
            });
			final AlertDialog dialog = alert.create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dialog.show();
        }
        else {
            joinChannel(channel, "");
        }
    }

    private void setCurrentChannel(Channel channel) {
        curchannel = channel;
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setSubtitle((channel != null) ? channel.szName : null);
        invalidateOptionsMenu();
    }

    private void setMyChannel(Channel channel) {
        mychannel = channel;

        adjustVoiceGain();
    }

    private String getChannelNameForTTS(int channelId) {
        if (channelId == 0) {
            return getString(R.string.text_tts_root_channel_name);
        }
        if (getService() != null && getService().getChannels() != null) {
            Channel chan = getService().getChannels().get(channelId);
            if (chan != null) {
                return chan.szName;
            }
        }
        return "";
    }

    private void logToEventHistory(String message) {
        if (getService() != null) {
            MyTextMessage logMsg = MyTextMessage.createLogMsg(MyTextMessage.MSGTYPE_LOG_INFO, message);
            getService().getChatLogTextMsgs().add(logMsg);
            if (eventHistoryAdapter != null) {
                eventHistoryAdapter.notifyDataSetChanged();
            }
        }
    }

    private void subscriptionChange(User user) {
        User olduser = this.users.get(user.nUserID);

        if (olduser != null && this.ttsWrapper != null) {
            Utils.ttsSubscriptionChanged(getBaseContext(), olduser, user).ifPresent((text -> ttsWrapper.speak(text)));
        }

        if (olduser != null && (this.sounds.get(SOUND_INTERCEPTON) != 0 && this.sounds.get(SOUND_INTERCEPTOFF) != 0)) {
            Utils.subscriptionChanged(olduser, user, Subscription.SUBSCRIBE_INTERCEPT_USER_MSG).ifPresent(isOn -> audioIcons.play((isOn ? sounds.get(SOUND_INTERCEPTON) : sounds.get(SOUND_INTERCEPTOFF)), 1.0f, 1.0f, 0, 0, 1.0f));
            Utils.subscriptionChanged(olduser, user, Subscription.SUBSCRIBE_INTERCEPT_CHANNEL_MSG).ifPresent(isOn -> audioIcons.play((isOn ? sounds.get(SOUND_INTERCEPTON) : sounds.get(SOUND_INTERCEPTOFF)), 1.0f, 1.0f, 0, 0, 1.0f));
            Utils.subscriptionChanged(olduser, user, Subscription.SUBSCRIBE_INTERCEPT_VOICE).ifPresent(isOn -> audioIcons.play((isOn ? sounds.get(SOUND_INTERCEPTON) : sounds.get(SOUND_INTERCEPTOFF)), 1.0f, 1.0f, 0, 0, 1.0f));
            Utils.subscriptionChanged(olduser, user, Subscription.SUBSCRIBE_INTERCEPT_VIDEOCAPTURE).ifPresent(isOn -> audioIcons.play((isOn ? sounds.get(SOUND_INTERCEPTON) : sounds.get(SOUND_INTERCEPTOFF)), 1.0f, 1.0f, 0, 0, 1.0f));
            Utils.subscriptionChanged(olduser, user, Subscription.SUBSCRIBE_INTERCEPT_DESKTOP).ifPresent(isOn -> audioIcons.play((isOn ? sounds.get(SOUND_INTERCEPTON) : sounds.get(SOUND_INTERCEPTOFF)), 1.0f, 1.0f, 0, 0, 1.0f));
            Utils.subscriptionChanged(olduser, user, Subscription.SUBSCRIBE_INTERCEPT_MEDIAFILE).ifPresent(isOn -> audioIcons.play((isOn ? sounds.get(SOUND_INTERCEPTON) : sounds.get(SOUND_INTERCEPTOFF)), 1.0f, 1.0f, 0, 0, 1.0f));
        }
    }

    private boolean isVisibleChannel(int chanid) {
        if (curchannel != null) {
            if (curchannel.nParentID == chanid)
                return true;
            Channel channel = getService().getChannels().get(chanid);
            if (channel != null)
                return curchannel.nChannelID == channel.nParentID;
        }
        else {
            return chanid == getClient().getRootChannelID();
        }
        return false;
    }

    public static class ChannelsSectionFragment extends Fragment {
        MainActivity mainActivity;

        public ChannelsSectionFragment() {
        }

        @Override
        public void onAttach(@NonNull Activity activity) {
            mainActivity = (MainActivity) activity;
            super.onAttach(activity);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_channels, container, false);


            ListView channelsList = rootView.findViewById(R.id.listChannels);
            channelsList.setAdapter(mainActivity.getChannelsAdapter());
            channelsList.setOnItemClickListener(mainActivity);
            channelsList.setOnItemLongClickListener(mainActivity);

            View emptyView = rootView.findViewById(R.id.empty_view);
            if (emptyView != null) {
                channelsList.setEmptyView(emptyView);
            }

            return rootView;
        }
    }

    public static class ChatSectionFragment extends Fragment {
        MainActivity mainActivity;

private EditText newmsg;
        public ChatSectionFragment() {
        }

        @Override
        public void onAttach(@NonNull Activity activity) {
            mainActivity = (MainActivity) activity;
            super.onAttach(activity);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_chat, container, false);
            mainActivity.accessibilityAssistant.registerPage(rootView, SectionsPagerAdapter.CHAT_PAGE);
            newmsg = rootView.findViewById(R.id.channel_im_edittext);
            newmsg.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_NULL) {
                    sendMsgToChannel();
                    return true;
                }
                return false;
            });
            ListView chatlog = rootView.findViewById(R.id.channel_im_listview);
            chatlog.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
            chatlog.setAdapter(mainActivity.channelChatAdapter);

            Button sendBtn = rootView.findViewById(R.id.channel_im_sendbtn);
            sendBtn.setOnClickListener(arg0 -> sendMsgToChannel());
            return rootView;
        }

        private void sendMsgToChannel() {
            String text = newmsg.getText().toString();
            if (text.isEmpty())
                return;

            MyTextMessage textmsg = new MyTextMessage();
            textmsg.nMsgType = TextMsgType.MSGTYPE_CHANNEL;
            textmsg.nChannelID = mainActivity.getClient().getMyChannelID();
            textmsg.szMessage = text;

            int cmdid = 0;
            for (MyTextMessage m : textmsg.split()) {
                cmdid = mainActivity.getClient().doTextMessage(m);
            }

            if (cmdid > 0) {
                mainActivity.activecmds.put(cmdid, CmdComplete.CMD_COMPLETE_TEXTMSG);
                newmsg.setText("");
            }
            else {
                Toast.makeText(mainActivity, getResources().getString(R.string.text_con_cmderr),
                Toast.LENGTH_LONG).show();
            }
        }

    }

    public static class VidcapSectionFragment extends Fragment {
        MainActivity mainActivity;

        public VidcapSectionFragment() {
        }

        @Override
        public void onAttach(@NonNull Activity activity) {
            mainActivity = (MainActivity) activity;
            super.onAttach(activity);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main_vidcap, container, false);
        }
    }

    public static class MediaSectionFragment extends Fragment {
        MainActivity mainActivity;

        public MediaSectionFragment() {
        }

        @Override
        public void onAttach(@NonNull Activity activity) {
            mainActivity = (MainActivity) activity;
            super.onAttach(activity);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_media, container, false);
            mainActivity.accessibilityAssistant.registerPage(rootView, SectionsPagerAdapter.MEDIA_PAGE);

            ExpandableListView mediaview = rootView.findViewById(R.id.media_elist_view);
            mediaview.setAdapter(mainActivity.getMediaAdapter());
            return rootView;
        }
    }

    public static class FilesSectionFragment extends ListFragment {

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity == null) return;
            mainActivity.accessibilityAssistant.registerPage(view, SectionsPagerAdapter.FILES_PAGE);
            if (mainActivity.getFilesAdapter() != null) {
                setListAdapter(mainActivity.getFilesAdapter());
            }
            super.onViewCreated(view, savedInstanceState);
        }
    }

    public static class GlobalSectionFragment extends Fragment {
        MainActivity mainActivity;
        private EditText editMsg;

        public GlobalSectionFragment() {
        }

        @Override
        public void onAttach(@NonNull Activity activity) {
            mainActivity = (MainActivity) activity;
            super.onAttach(activity);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_global, container, false);
            mainActivity.accessibilityAssistant.registerPage(rootView, SectionsPagerAdapter.GLOBAL_PAGE);

            ListView msgList = rootView.findViewById(R.id.global_msg_listview);
            View emptyView = rootView.findViewById(R.id.empty_view);
            if (emptyView != null) {
                msgList.setEmptyView(emptyView);
            }
            msgList.setAdapter(mainActivity.globalChatAdapter);

            editMsg = rootView.findViewById(R.id.global_msg_edittext);
            Button sendBtn = rootView.findViewById(R.id.global_msg_sendbtn);

            sendBtn.setOnClickListener(v -> sendBroadcastMessage());

            editMsg.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_NULL) {
                    sendBroadcastMessage();
                    return true;
                }
                return false;
            });

            return rootView;
        }

        private void sendBroadcastMessage() {
            String msg = editMsg.getText().toString();
            if (!msg.isEmpty()) {
                MyTextMessage textmsg = new MyTextMessage();
                textmsg.nMsgType = TextMsgType.MSGTYPE_BROADCAST;
                textmsg.nChannelID = 0;
                textmsg.szMessage = msg;

                for (MyTextMessage m : textmsg.split()) {
                    mainActivity.getClient().doTextMessage(m);
                }
                editMsg.setText("");
                if (mainActivity.sounds.get(mainActivity.SOUND_BCASTMSG) != 0)
                    mainActivity.audioIcons.play(mainActivity.sounds.get(mainActivity.SOUND_BCASTMSG), 1.0f, 1.0f, 0, 0, 1.0f);

                if (mainActivity.ttsWrapper != null && mainActivity.prefs.get("broadcast_message_checkbox", true)) {
                    UserAccount myaccount = new UserAccount();
                    mainActivity.getClient().getMyUserAccount(myaccount);
                    User me = mainActivity.getService().getUsers().get(mainActivity.getClient().getMyUserID());
                    String name = (me != null) ? Utils.getDisplayName(mainActivity, me) : myaccount.szUsername;
                    mainActivity.ttsWrapper.speak(mainActivity.getString(R.string.text_tts_broadcast_message, name, msg));
                }
            }
        }
    }

    public static class EventHistorySectionFragment extends Fragment {
        MainActivity mainActivity;

        public EventHistorySectionFragment() {
        }

        @Override
        public void onAttach(@NonNull Activity activity) {
            mainActivity = (MainActivity) activity;
            super.onAttach(activity);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            
            
            View rootView = inflater.inflate(R.layout.fragment_main_global, container, false);
            mainActivity.accessibilityAssistant.registerPage(rootView, SectionsPagerAdapter.EVENT_HISTORY_PAGE);

            ListView msgList = rootView.findViewById(R.id.global_msg_listview);
            View emptyView = rootView.findViewById(R.id.empty_view);
            if (emptyView != null) {
                msgList.setEmptyView(emptyView);
            }
            
            msgList.setAdapter(mainActivity.eventHistoryAdapter);

            
            View editMsg = rootView.findViewById(R.id.global_msg_edittext);
            View sendBtn = rootView.findViewById(R.id.global_msg_sendbtn);
            if(editMsg != null) editMsg.setVisibility(View.GONE);
            if(sendBtn != null) sendBtn.setVisibility(View.GONE);

            return rootView;
        }
    }

    public static class PrivateSectionFragment extends Fragment {
        MainActivity mainActivity;

        public PrivateSectionFragment() {
        }

        @Override
        public void onAttach(@NonNull Activity activity) {
            mainActivity = (MainActivity) activity;
            super.onAttach(activity);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_private, container, false);
            mainActivity.accessibilityAssistant.registerPage(rootView, SectionsPagerAdapter.PRIVATE_PAGE);

            ListView convList = rootView.findViewById(R.id.private_conv_listview);
            View emptyView = rootView.findViewById(R.id.empty_view);
            if (emptyView != null) {
                convList.setEmptyView(emptyView);
            }
            convList.setAdapter(mainActivity.privateConversationsAdapter);

            convList.setOnItemClickListener((parent, view, position, id) -> {
                Integer userID = (Integer) mainActivity.privateConversationsAdapter.getItem(position);
                Intent intent = new Intent(mainActivity, TextMessageActivity.class);
                mainActivity.startActivity(intent.putExtra(TextMessageActivity.EXTRA_USERID, userID));
            });

            return rootView;
        }
    }

    public static class OnlineUsersSectionFragment extends Fragment {
        MainActivity mainActivity;

        public OnlineUsersSectionFragment() {
        }

        @Override
        public void onAttach(@NonNull Activity activity) {
            mainActivity = (MainActivity) activity;
            super.onAttach(activity);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_online_users, container, false);
            if (mainActivity == null) return rootView;
            mainActivity.accessibilityAssistant.registerPage(rootView, SectionsPagerAdapter.ONLINE_USERS_PAGE);

            ListView userList = rootView.findViewById(R.id.server_users_listview);

            if (mainActivity.getService() != null && mainActivity.getService().getUsers() != null) {
                ArrayList<dk.bearware.User> userListArray = new ArrayList<>(mainActivity.getService().getUsers().values());
                mainActivity.onlineUsersAdapter = new dk.bearware.gui.OnlineUsersAdapter(mainActivity, mainActivity.getService(), userListArray);
                userList.setAdapter(mainActivity.onlineUsersAdapter);
                userList.setOnItemLongClickListener(mainActivity);
            }

            return rootView;
        }
    }

    public static class ManagementSectionFragment extends Fragment {
        MainActivity mainActivity;
        Button propsBtn, accountsBtn, serverBansBtn, serverStatsBtn;
        private final android.os.Handler handler = new android.os.Handler();
        private final Runnable checkPermissionsRunnable = new Runnable() {
            @Override
            public void run() {
                updateButtonVisibility();
            }
        };

        public ManagementSectionFragment() {
        }

        @Override
        public void onAttach(@NonNull Activity activity) {
            mainActivity = (MainActivity) activity;
            super.onAttach(activity);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_management, container, false);
            if (mainActivity == null) return rootView;
            mainActivity.accessibilityAssistant.registerPage(rootView, SectionsPagerAdapter.MANAGEMENT_PAGE);

            propsBtn = rootView.findViewById(R.id.server_props_btn);
            accountsBtn = rootView.findViewById(R.id.server_accounts_btn);
            serverBansBtn = rootView.findViewById(R.id.server_bans_btn);
            serverStatsBtn = rootView.findViewById(R.id.server_stats_btn);

            propsBtn.setOnClickListener(v -> {
                Intent intent = new Intent(mainActivity, dk.bearware.gui.ServerPropActivity.class);
                mainActivity.startActivity(intent);
            });
            accountsBtn.setOnClickListener(v -> {
                Intent intent = new Intent(mainActivity, dk.bearware.gui.UserAccountsActivity.class);
                mainActivity.startActivity(intent);
            });
            serverBansBtn.setOnClickListener(v -> {
                Intent intent = new Intent(mainActivity, dk.bearware.gui.ServerBannedUsersActivity.class);
                mainActivity.startActivity(intent);
            });
            serverStatsBtn.setOnClickListener(v -> {
                Intent intent = new Intent(mainActivity, dk.bearware.gui.ServerStatisticsActivity.class);
                mainActivity.startActivity(intent);
            });

            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();

            updateButtonVisibility();
        }

        @Override
        public void onPause() {
            super.onPause();
            handler.removeCallbacks(checkPermissionsRunnable);
        }

        private void updateButtonVisibility() {
            if (mainActivity == null || mainActivity.getClient() == null) return;

            if ((mainActivity.getClient().getFlags() & ClientFlag.CLIENT_CONNECTED) == 0) {
                handler.removeCallbacks(checkPermissionsRunnable);
                handler.postDelayed(checkPermissionsRunnable, 1000);
                return;
            }

            UserAccount myAccount = new UserAccount();
            if (mainActivity.getClient().getMyUserAccount(myAccount)) {

                if ((myAccount.uUserType & UserType.USERTYPE_ADMIN) == UserType.USERTYPE_ADMIN) {
                    accountsBtn.setVisibility(View.VISIBLE);
                } else {
                    accountsBtn.setVisibility(View.GONE);
                }

                if ((myAccount.uUserRights & UserRight.USERRIGHT_UPDATE_SERVERPROPERTIES) != 0) {
                    propsBtn.setVisibility(View.VISIBLE);
                } else {
                    propsBtn.setVisibility(View.GONE);
                }

                if ((myAccount.uUserRights & UserRight.USERRIGHT_BAN_USERS) != 0) {
                    serverBansBtn.setVisibility(View.VISIBLE);
                } else {
                    serverBansBtn.setVisibility(View.GONE);
                }

                serverStatsBtn.setVisibility(View.VISIBLE);

            } else {
                handler.removeCallbacks(checkPermissionsRunnable);
                handler.postDelayed(checkPermissionsRunnable, 500);
            }
        }
    }

    public static class SettingsSectionFragment extends Fragment {
        MainActivity mainActivity;

        private static final int[] TITLES = {
            R.string.pref_header_general,
            R.string.pref_title_audio_icons,
            R.string.pref_header_tts,
            R.string.pref_header_serverlist,
            R.string.pref_header_connection,
            R.string.pref_header_soundsystem,
            R.string.pref_cat_about
        };

        private static final String[] FRAGMENTS = {
            dk.bearware.gui.PreferencesActivity.GeneralPreferenceFragment.class.getName(),
            dk.bearware.gui.PreferencesActivity.SoundEventsPreferenceFragment.class.getName(),
            dk.bearware.gui.PreferencesActivity.TtsPreferenceFragment.class.getName(),
            dk.bearware.gui.PreferencesActivity.ServerListPreferenceFragment.class.getName(),
            dk.bearware.gui.PreferencesActivity.ConnectionPreferenceFragment.class.getName(),
            dk.bearware.gui.PreferencesActivity.SoundSystemPreferenceFragment.class.getName(),
            dk.bearware.gui.PreferencesActivity.AboutPreferenceFragment.class.getName()
        };

        public SettingsSectionFragment() {
        }

        @Override
        public void onAttach(@NonNull Activity activity) {
            mainActivity = (MainActivity) activity;
            super.onAttach(activity);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_settings, container, false);
            mainActivity.accessibilityAssistant.registerPage(rootView, SectionsPagerAdapter.SETTINGS_PAGE);

            ListView listView = rootView.findViewById(R.id.settings_list);

            String[] items = new String[TITLES.length];
            for (int i = 0; i < TITLES.length; i++) {
                items[i] = getString(TITLES[i]);
            }

            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                items
            );
            listView.setAdapter(adapter);

            listView.setOnItemClickListener((parent, view, position, id) -> {
                if (position >= 0 && position < FRAGMENTS.length) {
                    Intent intent = new Intent(mainActivity, dk.bearware.gui.PreferencesActivity.class);
                    intent.putExtra(android.preference.PreferenceActivity.EXTRA_SHOW_FRAGMENT, FRAGMENTS[position]);
                    intent.putExtra(android.preference.PreferenceActivity.EXTRA_NO_HEADERS, true);
                    mainActivity.startActivity(intent);
                }
            });



            return rootView;
        }
    }

    public static class ConnectionStatusSectionFragment extends Fragment {
        MainActivity mainActivity;
        TextView connection, ping, total;
        CountDownTimer timer;
        ClientStatistics prev_stats;

        public ConnectionStatusSectionFragment() {}

        @Override
        public void onAttach(@NonNull Activity activity) {
            mainActivity = (MainActivity) activity;
            super.onAttach(activity);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_connstatus, container, false);
            mainActivity.accessibilityAssistant.registerPage(rootView, SectionsPagerAdapter.CONNECTION_PAGE);

            connection = rootView.findViewById(R.id.connectionstat_textview);
            ping = rootView.findViewById(R.id.pingstat_textview);
            total = rootView.findViewById(R.id.totalstat_textview);

            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();
            startTimer();
        }

        @Override
        public void onPause() {
            super.onPause();
            stopTimer();
        }

        private void startTimer() {
            if (timer == null) {
                timer = new CountDownTimer(10000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        if (mainActivity == null || mainActivity.isFinishing()) return;
                        updateStats();
                    }
                    public void onFinish() {
                        start();
                    }
                }.start();
            }
        }

        private void stopTimer() {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }

        private void updateStats() {
            if (mainActivity.accessibilityAssistant.isUiUpdateDiscouraged()) return;

            String con = mainActivity.getString(R.string.stat_offline);
            int con_color = Color.RED;
            int flags = mainActivity.getClient().getFlags();

            if ((flags & ClientFlag.CLIENT_CONNECTING) == ClientFlag.CLIENT_CONNECTING) {
                con = mainActivity.getString(R.string.stat_connecting);
            } else if ((flags & ClientFlag.CLIENT_AUTHORIZED) == ClientFlag.CLIENT_CLOSED) {
                con = mainActivity.getString(R.string.stat_unauthorized);
            } else if ((flags & ClientFlag.CLIENT_AUTHORIZED) == ClientFlag.CLIENT_AUTHORIZED) {
                con = mainActivity.getString(R.string.stat_online);
                con_color = Color.GREEN;
            }

            connection.setText(mainActivity.getString(R.string.label_connection) + " " + con);
            connection.setTextColor(con_color);

            ClientStatistics stats = new ClientStatistics();
            if(!mainActivity.getClient().getClientStatistics(stats)) return;

            if(prev_stats == null) prev_stats = stats;

            long totalrx = stats.nUdpBytesRecv - prev_stats.nUdpBytesRecv;
            long totaltx = stats.nUdpBytesSent - prev_stats.nUdpBytesSent;

            String str;
            if(stats.nUdpPingTimeMs >= 0) {
                str = String.format(Locale.ROOT, "%1$d", stats.nUdpPingTimeMs);
                ping.setText(mainActivity.getString(R.string.label_ping) + " " + str);
                if(stats.nUdpPingTimeMs > 250) ping.setTextColor(Color.RED);
                else ping.setTextColor(connection.getTextColors().getDefaultColor());
            }

            str = String.format(Locale.ROOT, "%1$d/%2$d KB", totalrx/ 1024, totaltx / 1024);
            total.setText(mainActivity.getString(R.string.label_rxtx) + " " + str);

            prev_stats = stats;
        }
    }

    class ChannelListAdapter extends BaseAdapter {

        private static final int PARENT_CHANNEL_VIEW_TYPE = 0,
            CHANNEL_VIEW_TYPE = 1,
            USER_VIEW_TYPE = 2,
            INFO_VIEW_TYPE = 3,

            VIEW_TYPE_COUNT = 4;

        private final LayoutInflater inflater;

        Vector<Channel> subchannels = new Vector<>();
        Vector<Channel> stickychannels = new Vector<>();
        Vector<User> currentusers = new Vector<>();

        ChannelListAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        @Override
        public void notifyDataSetChanged() {
            subchannels.clear();
            stickychannels.clear();
            currentusers.clear();

            int chanid;
            if(curchannel != null) {
                chanid = curchannel.nChannelID;

                subchannels = Utils.getSubChannels(chanid, getService().getChannels());
                stickychannels = Utils.getStickyChannels(chanid, getService().getChannels());
                currentusers = Utils.getUsers(chanid, getService().getUsers());
            }
            else {
                chanid = getClient().getRootChannelID();
                Channel root = getService().getChannels().get(chanid);
                if(root != null)
                    subchannels.add(root);
            }

            Collections.sort(subchannels, (c1, c2) -> c1.szName.compareToIgnoreCase(c2.szName));

            Collections.sort(stickychannels, (c1, c2) -> c1.szName.compareToIgnoreCase(c2.szName));

            Collections.sort(currentusers, (u1, u2) -> {
                if (prefs.get("movetalk_checkbox", true)) {
                    if (((u1.uUserState & UserState.USERSTATE_VOICE) != 0) &&
                        ((u2.uUserState & UserState.USERSTATE_VOICE) == 0))
                        return -1;
                    else if (((u1.uUserState & UserState.USERSTATE_VOICE) == 0) &&
                             ((u2.uUserState & UserState.USERSTATE_VOICE) != 0))
                        return 1;
                }

                String name1 = Utils.getDisplayName(getBaseContext(), u1);
                String name2 = Utils.getDisplayName(getBaseContext(), u2);
                return name1.compareToIgnoreCase(name2);
            });

            super.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            int count = currentusers.size() + subchannels.size() + stickychannels.size();
            if (curchannel != null) {
                count++; 
            }
            return count;
        }

        @Override
        public Object getItem(int position) {

            if (position < stickychannels.size()) {
                return stickychannels.get(position);
            }

            position -= stickychannels.size();

            if (position < currentusers.size()) {
                return currentusers.get(position);
            }

            position -= currentusers.size();

            if (curchannel != null) {
                if(position == 0) {
                    if (curchannel.nParentID > 0) {
                        Channel parent = getService().getChannels().get(curchannel.nParentID);
                        if(parent != null)
                            return parent;
                        return new Channel();
                    } else {
                        return new Channel(); // Signals Root Parent (handled in onItemClick)
                    }
                }

                position--; 
            }
            return subchannels.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {

            if (position < stickychannels.size())
                return INFO_VIEW_TYPE;

            position -= stickychannels.size();

            if (position < currentusers.size())
                return USER_VIEW_TYPE;

            position -= currentusers.size();

            if (curchannel != null) {
                if (position == 0) {
                    return PARENT_CHANNEL_VIEW_TYPE;
                }

                position--; 
            }

            return CHANNEL_VIEW_TYPE;
        }

        @Override
        public int getViewTypeCount() {
            return VIEW_TYPE_COUNT;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            Object item = getItem(position);

            if(item instanceof Channel channel) {

                switch (getItemViewType(position)) {
                    case PARENT_CHANNEL_VIEW_TYPE :

                        if (convertView == null ||
                                convertView.findViewById(R.id.parentname) == null)
                            convertView = inflater.inflate(R.layout.item_channel_back, parent, false);
                        break;

                    case CHANNEL_VIEW_TYPE :
                        if (convertView == null ||
                                convertView.findViewById(R.id.channelname) == null)
                            convertView = inflater.inflate(R.layout.item_channel, parent, false);

                        ImageView chanicon = convertView.findViewById(R.id.channelicon);
                        TextView name = convertView.findViewById(R.id.channelname);
                        TextView topic = convertView.findViewById(R.id.chantopic);
                        Button join = convertView.findViewById(R.id.join_btn);
                        int icon_resource = R.drawable.channel_orange;
                        if(channel.bPassword) {
                            icon_resource = R.drawable.channel_pink;
                            chanicon.setContentDescription(getString(R.string.text_passwdprot));
                            chanicon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                        }
                        else {
                            chanicon.setContentDescription(null);
                            chanicon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                        }
                        chanicon.setImageResource(icon_resource);

                        if(channel.nParentID == 0) {
                            name.setText(R.string.init_channel);
                        }
                        else {
                            if(channel.szName.trim().isEmpty())
                                name.setText(R.string.no_name);
                            else
                                name.setText(channel.szName);
                        }
                        topic.setText(channel.szTopic);

                        final boolean isMyChannel = channel.nChannelID == getClient().getMyChannelID();
                        join.setText(isMyChannel ? R.string.action_leave : R.string.action_join);

                        OnClickListener listener = v -> {
                            if (v.getId() == R.id.join_btn) {
                                if (isMyChannel)
                                    leaveChannel();
                                else
                                    joinChannel(channel);
                            }
                        };
                        join.setOnClickListener(listener);

                        join.setEnabled(true);

                        if (channel.nMaxUsers > 0) {
                            int population = Utils.getUsers(channel.nChannelID, getService().getUsers()).size();
                            ((TextView)convertView.findViewById(R.id.population)).setText((population > 0) ? String.format(Locale.ROOT, "(%d)", population) : "");
                        }

                        break;

                    case INFO_VIEW_TYPE :
                        if (convertView == null ||
                                convertView.findViewById(R.id.titletext) == null)
                            convertView = inflater.inflate(R.layout.item_info, parent, false);
                        TextView title = convertView.findViewById(R.id.titletext);
                        TextView details = convertView.findViewById(R.id.infodetails);
                        title.setText(channel.szName);
                        details.setText(channel.szTopic);
                        break;
                }
            }
            else if(item instanceof User user) {
                if (convertView == null ||
                    convertView.findViewById(R.id.nickname) == null)
                    convertView = inflater.inflate(R.layout.item_user, parent, false);
                ImageView usericon = convertView.findViewById(R.id.usericon);
                TextView nickname = convertView.findViewById(R.id.nickname);
                TextView status = convertView.findViewById(R.id.status);
                String name = Utils.getDisplayName(getBaseContext(), user);
                if(name.trim().isEmpty())
                    nickname.setText(R.string.no_name);
                else
                    nickname.setText(name);
                status.setText(user.szStatusMsg);

                boolean selected = userIDS.contains(user.nUserID);
                boolean isOperator = getClient().isChannelOperator(user.nUserID, user.nChannelID);
                boolean talking = (user.uUserState & UserState.USERSTATE_VOICE) != 0;
                boolean female = (user.nStatusMode & TeamTalkConstants.STATUSMODE_FEMALE) != 0;
                boolean neutral = (user.nStatusMode & TeamTalkConstants.STATUSMODE_NEUTRAL) != 0;
                boolean male = !female && !neutral;
                boolean away =  (user.nStatusMode & TeamTalkConstants.STATUSMODE_AWAY) != 0;
                int icon_resource;

                if(user.nUserID == getService().getTTInstance().getMyUserID()) {
                    talking = getService().isVoiceTransmitting();
                }

                String move = selected ? getString(R.string.user_state_selected) : "";
                String speaking = talking ? getString(R.string.user_state_now_speaking, name) : name;
                String genderText = female ? getString(R.string.gender_female) : neutral ? getString(R.string.gender_neutral) : getString(R.string.gender_male);
                String op = isOperator ? getString(R.string.user_state_operator) : "";
                nickname.setContentDescription(move + speaking + " " + genderText + " " + op);

                if (talking) {
                    if (female) {
                        icon_resource = R.drawable.woman_green;
                    } else if (neutral) {
                        icon_resource = R.drawable.neutral_green;
                    } else {
                        icon_resource = R.drawable.man_green; 
                    }
                } else {
                    if (female) {
                        icon_resource = away ? R.drawable.woman_orange : R.drawable.woman_blue;
                    } else if (neutral) {
                        icon_resource = away ? R.drawable.neutral_orange : R.drawable.neutral_blue;
                    } else {
                        icon_resource = away ? R.drawable.man_orange : R.drawable.man_blue; 
                    }
                }

                status.setContentDescription(away ? getString(R.string.user_state_away) + " " + user.szStatusMsg : null);

                usericon.setImageResource(icon_resource);
                usericon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

                Button sndmsg = convertView.findViewById(R.id.msg_btn);
                OnClickListener listener = v -> {
                    if (v.getId() == R.id.msg_btn) {
                        Intent intent = new Intent(MainActivity.this, TextMessageActivity.class);
                        startActivity(intent.putExtra(TextMessageActivity.EXTRA_USERID, user.nUserID));
                    }
                };
                sndmsg.setOnClickListener(listener);
            }
            return convertView;
        }
    }

    @Override
    public void onItemClick(AdapterView< ? > l, View v, int position, long id) {
        
        if (channelsAdapter.getItemViewType(position) == ChannelListAdapter.PARENT_CHANNEL_VIEW_TYPE) {
            // Check if we are at Root (Initial Channel)
            if (curchannel != null && curchannel.nParentID == 0) {
                 // Fall through to standard channel logic, which will set current channel to null (collapsing view)
            }
        }

        Object item = l.getItemAtPosition(position);

        if(item instanceof User user) {
            Intent intent = new Intent(this, UserPropActivity.class);

            startActivityForResult(intent.putExtra(UserPropActivity.EXTRA_USERID, user.nUserID),
                                   REQUEST_EDITUSER);
        }
        else if(item instanceof Channel channel) {
            setCurrentChannel((channel.nChannelID > 0) ? channel : null);
            channelsAdapter.notifyDataSetChanged();
        }
    }

    Channel selectedChannel;
    User selectedUser;
    List<Integer> userIDS = new ArrayList<>();

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        Object item = parent.getItemAtPosition(position);

        if (item instanceof User) {
            final User user = (User) item;
            selectedUser = user; 

            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, view);

            popup.getMenu().add(getString(R.string.info_copy_name)).setOnMenuItemClickListener(menuItem -> {
                Utils.copyToClipboard(this, getString(R.string.label_user_name), user.szNickname);
                return true;
            });
            popup.getMenu().add(getString(R.string.info_copy_id)).setOnMenuItemClickListener(menuItem -> {
                Utils.copyToClipboard(this, getString(R.string.label_user_id), String.valueOf(user.nUserID));
                return true;
            });
            popup.getMenu().add(getString(R.string.info_copy_ip)).setOnMenuItemClickListener(menuItem -> {
                Utils.copyToClipboard(this, getString(R.string.label_user_ip), user.szIPAddress);
                return true;
            });

            popup.getMenu().add(R.string.action_edit_user).setOnMenuItemClickListener(menuItem -> {
                Intent intent = new Intent(MainActivity.this, UserPropActivity.class);
                startActivityForResult(intent.putExtra(UserPropActivity.EXTRA_USERID, user.nUserID),
                                       REQUEST_EDITUSER);
                return true;
            });

            popup.getMenu().add(R.string.button_msg).setOnMenuItemClickListener(menuItem -> {
                Intent intent = new Intent(MainActivity.this, TextMessageActivity.class);
                startActivity(intent.putExtra(TextMessageActivity.EXTRA_USERID, user.nUserID));
                return true;
            });

            UserAccount myuseraccount = new UserAccount();
            getClient().getMyUserAccount(myuseraccount);
            boolean kickRight = (myuseraccount.uUserRights & UserRight.USERRIGHT_KICK_USERS) != UserRight.USERRIGHT_NONE;
            boolean banRight = (myuseraccount.uUserRights & UserRight.USERRIGHT_BAN_USERS) != UserRight.USERRIGHT_NONE;
            boolean operatorRight = getClient().isChannelOperator(getClient().getMyUserID(), user.nChannelID);

            if (((myuseraccount.uUserType & UserType.USERTYPE_ADMIN) == UserType.USERTYPE_ADMIN) || ((myuseraccount.uUserRights & UserRight.USERRIGHT_OPERATOR_ENABLE) != UserRight.USERRIGHT_NONE)) {
                 boolean isOp = getClient().isChannelOperator(user.nUserID, user.nChannelID);
                 popup.getMenu().add(isOp ? R.string.action_revoke_operator : R.string.action_make_operator).setOnMenuItemClickListener(menuItem -> {
                     getClient().doChannelOp(user.nUserID, user.nChannelID, !isOp);
                     return true;
                 });
            } else {
                 boolean isOp = getClient().isChannelOperator(user.nUserID, user.nChannelID);
                 popup.getMenu().add(isOp ? R.string.action_revoke_operator : R.string.action_make_operator).setOnMenuItemClickListener(menuItem -> {
                     AlertDialog.Builder alert = new AlertDialog.Builder(this);
                     alert.setTitle(isOp ? R.string.action_revoke_operator : R.string.action_make_operator);
                     alert.setMessage(R.string.text_operator_password);
                     final EditText input = new EditText(this);
                     input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
                     alert.setPositiveButton(android.R.string.yes, ((dialog, whichButton) -> getClient().doChannelOpEx(user.nUserID, user.nChannelID, input.getText().toString(), !isOp)));
                     alert.setNegativeButton(android.R.string.no, null);
                     alert.setView(input);
                     alert.show();
                     return true;
                 });
            }

            if (kickRight || operatorRight) {
                 popup.getMenu().add("Expulsar do canal").setOnMenuItemClickListener(menuItem -> {
                    getClient().doKickUser(user.nUserID, user.nChannelID);
                    return true;
                });
            }
            if (banRight || operatorRight) {
                 popup.getMenu().add("Banir do canal").setOnMenuItemClickListener(menuItem -> {
                    getClient().doBanUser(user.nUserID, user.nChannelID);
                    return true;
                });
            }

            if (kickRight) {
                popup.getMenu().add("Expulsar do servidor").setOnMenuItemClickListener(menuItem -> {
                    getClient().doKickUser(user.nUserID, 0);
                    return true;
                });
            }
            if (banRight) {
                popup.getMenu().add("Banir do servidor").setOnMenuItemClickListener(menuItem -> {
                    getClient().doBanUser(user.nUserID, 0);
                    return true;
                });
            }

            popup.show();
            return true;
        }

        else if (item instanceof Channel) {
            final Channel channel = (Channel) item;
            selectedChannel = channel;

            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, view);

            UserAccount myuseraccount = new UserAccount();
            getClient().getMyUserAccount(myuseraccount);
            boolean chanRight = (myuseraccount.uUserRights & UserRight.USERRIGHT_MODIFY_CHANNELS) != UserRight.USERRIGHT_NONE;
            boolean tempChanRight = (myuseraccount.uUserRights & UserRight.USERRIGHT_CREATE_TEMPORARY_CHANNEL) != UserRight.USERRIGHT_NONE;

            if (chanRight || tempChanRight) {

                popup.getMenu().add(R.string.action_new_channel).setOnMenuItemClickListener(menuItem -> {
                    Intent intent = new Intent(MainActivity.this, dk.bearware.gui.ChannelPropActivity.class);
                    intent.putExtra(dk.bearware.gui.ChannelPropActivity.EXTRA_PARENTID, channel.nChannelID);
                    startActivityForResult(intent, REQUEST_NEWCHANNEL);
                    return true;
                });

                popup.getMenu().add(R.string.title_activity_channel_prop).setOnMenuItemClickListener(menuItem -> {
                    Intent intent = new Intent(MainActivity.this, dk.bearware.gui.ChannelPropActivity.class);
                    intent.putExtra(dk.bearware.gui.ChannelPropActivity.EXTRA_CHANNELID, channel.nChannelID);
                    startActivity(intent);
                    return true;
                });
            }

            popup.getMenu().add(getString(R.string.action_join)).setOnMenuItemClickListener(menuItem -> {
                joinChannel(channel);
                return true;
            });

            getClient().getMyUserAccount(myuseraccount);
            boolean moveRight = (myuseraccount.uUserRights & UserRight.USERRIGHT_MOVE_USERS) != UserRight.USERRIGHT_NONE;
             if(moveRight && !userIDS.isEmpty()) {
                  popup.getMenu().add(R.string.action_move).setOnMenuItemClickListener(menuItem -> {

                      for(Integer uid : userIDS) {
                          getClient().doMoveUser(uid, channel.nChannelID);
                      }
                      userIDS.clear();
                      return true;
                  });
             }

             // Add Banned Users option
             boolean banRight = (myuseraccount.uUserRights & UserRight.USERRIGHT_BAN_USERS) != UserRight.USERRIGHT_NONE;
             boolean operatorRight = getClient().isChannelOperator(getClient().getMyUserID(), channel.nChannelID);

             if (banRight || operatorRight) {
                 popup.getMenu().add(R.string.action_banned_users).setOnMenuItemClickListener(menuItem -> {
                     Intent intent = new Intent(MainActivity.this, dk.bearware.gui.ChannelBannedUsersActivity.class);
                     intent.putExtra("channel_id", channel.nChannelID);
                     startActivity(intent);
                     return true;
                 });
             }

            popup.show();
            return true;
        }

        return false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        switch (item.getItemId()) {
        case R.id.action_banchan:
            alert.setMessage(getString(R.string.ban_confirmation, selectedUser.szNickname));
            alert.setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                getClient().doBanUser(selectedUser.nUserID, selectedUser.nChannelID);
                getClient().doKickUser(selectedUser.nUserID, selectedUser.nChannelID);
            });

            alert.setNegativeButton(android.R.string.no, null);
            alert.show();
            break;
        case R.id.action_bansrv:
            alert.setMessage(getString(R.string.ban_confirmation, selectedUser.szNickname));
            alert.setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                getClient().doBanUser(selectedUser.nUserID, 0);
                getClient().doKickUser(selectedUser.nUserID, 0);
            });

            alert.setNegativeButton(android.R.string.no, null);
            alert.show();
            break;
        case R.id.action_edit:
            editChannelProperties(selectedChannel);
            break;
        case R.id.action_edituser: {
            Intent intent = new Intent(this, UserPropActivity.class);
            startActivityForResult(intent.putExtra(UserPropActivity.EXTRA_USERID, selectedUser.nUserID),
                                   REQUEST_EDITUSER);
        }
        break;
        case R.id.action_message: {
            Intent intent = new Intent(MainActivity.this, TextMessageActivity.class);
            startActivity(intent.putExtra(TextMessageActivity.EXTRA_USERID, selectedUser.nUserID));
        }
        break;
        case R.id.action_kickchan:
            alert.setMessage(getString(R.string.kick_confirmation, selectedUser.szNickname));
            alert.setPositiveButton(android.R.string.yes, (dialog, whichButton) -> getClient().doKickUser(selectedUser.nUserID, selectedUser.nChannelID));

            alert.setNegativeButton(android.R.string.no, null);
            alert.show();
            break;
        case R.id.action_kicksrv:
            alert.setMessage(getString(R.string.kick_confirmation, selectedUser.szNickname));
            alert.setPositiveButton(android.R.string.yes, (dialog, whichButton) -> getClient().doKickUser(selectedUser.nUserID, 0));

            alert.setNegativeButton(android.R.string.no, null);
            alert.show();
            break;
            case R.id.action_makeop:
                UserAccount myuseraccount = new UserAccount();
                getClient().getMyUserAccount(myuseraccount);
                if (((myuseraccount.uUserType & UserType.USERTYPE_ADMIN) == UserType.USERTYPE_ADMIN) || ((myuseraccount.uUserRights & UserRight.USERRIGHT_OPERATOR_ENABLE) != UserRight.USERRIGHT_NONE)) {
                    getClient().doChannelOp(selectedUser.nUserID, selectedUser.nChannelID, !getClient().isChannelOperator(selectedUser.nUserID, selectedUser.nChannelID));
                    break;
                }
                alert.setTitle(getClient().isChannelOperator(selectedUser.nUserID , selectedUser.nChannelID) ? R.string.action_revoke_operator : R.string.action_make_operator);
                alert.setMessage(R.string.text_operator_password);
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
                alert.setPositiveButton(android.R.string.yes, ((dialog, whichButton) -> getClient().doChannelOpEx(selectedUser.nUserID, selectedUser.nChannelID, input.getText().toString(), !getClient().isChannelOperator(selectedUser.nUserID, selectedUser.nChannelID))));
                alert.setNegativeButton(android.R.string.no, null);
                alert.setView(input);
                alert.show();
                break;
        case R.id.action_move:
            for (Integer userID : userIDS) {
                getClient().doMoveUser(userID, selectedChannel.nChannelID);
            }
            userIDS.clear();
            break;
        case R.id.action_select:
    if (userIDS.contains(selectedUser.nUserID)) {
        userIDS.remove((Integer) selectedUser.nUserID);
    } else {
        userIDS.add(selectedUser.nUserID);
    }
    accessibilityAssistant.lockEvents();
    channelsAdapter.notifyDataSetChanged();
    accessibilityAssistant.unlockEvents();
    break;
        case R.id.action_remove: {
            alert.setMessage(getString(R.string.channel_remove_confirmation, selectedChannel.szName));
            alert.setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                if (getClient().doRemoveChannel(selectedChannel.nChannelID) <= 0)
                    Toast.makeText(MainActivity.this,
                                   getString(R.string.err_channel_remove,
                                             selectedChannel.szName),
                                   Toast.LENGTH_LONG).show();
            });

            alert.setNegativeButton(android.R.string.no, null);
            alert.show();
            break;
        }

        default:
            return false;
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private void adjustSoundSystem() {
        if (audioManager.isBluetoothA2dpOn())
            return;
        boolean voiceProcessing = prefs.get(Preferences.PREF_SOUNDSYSTEM_VOICEPROCESSING, false);
        audioManager.setMode(voiceProcessing ?
                AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);
        if (voiceProcessing)
            audioManager.setSpeakerphoneOn(prefs.get(Preferences.PREF_SOUNDSYSTEM_SPEAKERPHONE, false) && !audioManager.isWiredHeadsetOn());
    }

    private void adjustMuteButton(ImageButton btn) {
        if (getService().getCurrentMuteState()) {
            btn.setImageResource(R.drawable.mute_blue);
            btn.setContentDescription(getString(R.string.speaker_unmute));
        }
        else {
            btn.setImageResource(R.drawable.speaker_blue);
            btn.setContentDescription(getString(R.string.speaker_mute));
        }
    }

    private void adjustVoxState(boolean voiceActivationEnabled, int level) {
        ImageButton voxSwitch = findViewById(R.id.voxSwitch);
        TextView micLevel = findViewById(R.id.miclevel_text);

        if (voiceActivationEnabled) {
            micLevel.setText(level + "%");
            micLevel.setContentDescription(getString(R.string.vox_level_description, micLevel.getText()));
            voxSwitch.setImageResource(R.drawable.microphone);
            voxSwitch.setContentDescription(getString(R.string.voice_activation_off));
            ((SeekBar) findViewById(R.id.mic_gainSeekBar)).setProgress(getClient().getVoiceActivationLevel());
            findViewById(R.id.mic_gainSeekBar).setContentDescription(getString(R.string.voxlevel));
        }
        else {
            micLevel.setText(Utils.refVolumeToPercent(level) + "%");
            micLevel.setContentDescription(getString(R.string.mic_gain_description, micLevel.getText()));
            voxSwitch.setImageResource(R.drawable.mic_green);
            voxSwitch.setContentDescription(getString(R.string.voice_activation_on));
            ((SeekBar) findViewById(R.id.mic_gainSeekBar)).setProgress(Utils.refVolumeToPercent(getClient().getSoundInputGainLevel()));
            findViewById(R.id.mic_gainSeekBar).setContentDescription(getString(R.string.micgain));
        }
    }

    private void adjustTxState(boolean txEnabled) {
        accessibilityAssistant.lockEvents();

        findViewById(R.id.transmit_voice).setBackgroundColor(txEnabled ? Color.GREEN : Color.RED);
        findViewById(R.id.transmit_voice).setContentDescription(txEnabled ? getString(R.string.tx_on) : getString(R.string.tx_off));

        if ((curchannel != null) && (getClient().getMyChannelID() == curchannel.nChannelID))
            channelsAdapter.notifyDataSetChanged();

        accessibilityAssistant.unlockEvents();
    }

    private void adjustVoiceGain() {

        boolean showMicSeekBar = mychannel == null || !mychannel.audiocfg.bEnableAGC ||  getService().isVoiceActivationEnabled();

        findViewById(R.id.mic_gainSeekBar).setVisibility(showMicSeekBar ? View.VISIBLE : View.GONE);
    }

    private interface OnButtonInteractionListener extends OnTouchListener, OnClickListener {
    }

    private void setupButtons() {

        final Button tx_btn = findViewById(R.id.transmit_voice);
        tx_btn.setAccessibilityDelegate(accessibilityAssistant);

        OnButtonInteractionListener txButtonListener = new OnButtonInteractionListener() {

            boolean tx_state = false;
            long tx_down_start = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean tx = event.getAction() != MotionEvent.ACTION_UP;

                if(tx != tx_state) {

                    if(!tx) {
                        if(System.currentTimeMillis() - tx_down_start < 800) {
                            tx = true;
                            tx_down_start = 0;
                        }
                        else {
                            tx_down_start = System.currentTimeMillis();
                        }

                    }

                    if (getService().isVoiceActivationEnabled())
                        getService().enableVoiceActivation(false);
                    getService().enableVoiceTransmission(tx);
                }
                tx_state = tx;
                return true;
            }

            @Override
            public void onClick(View v) {
                if(System.currentTimeMillis() - tx_down_start < 800) {
                    tx_state = true;
                    tx_down_start = 0;
                }
                else {
                    tx_state = false;
                    tx_down_start = System.currentTimeMillis();
                }
                if (getService().isVoiceActivationEnabled())
                    getService().enableVoiceActivation(false);
                getService().enableVoiceTransmission(tx_state);
            }
        };

        tx_btn.setOnTouchListener(txButtonListener);

        final SeekBar masterSeekBar = findViewById(R.id.master_volSeekBar);
        final SeekBar micSeekBar = findViewById(R.id.mic_gainSeekBar);
        final TextView micLevel = findViewById(R.id.miclevel_text);
        final TextView volLevel = findViewById(R.id.vollevel_text);
        masterSeekBar.setMax(100);
        micSeekBar.setMax(100);

        SeekBar.OnSeekBarChangeListener volListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar == masterSeekBar) {
                    if (getService().isMute()) {
                        getService().setMute(false);
                        ImageButton speakerBtn = findViewById(R.id.speakerBtn);
                        adjustMuteButton(speakerBtn);
                    }
                    int outputVolume = Utils.refVolume(progress);
                    getClient().setSoundOutputVolume(outputVolume);
                    prefs.put(Preferences.PREF_SOUNDSYSTEM_MASTERVOLUME, outputVolume);
                    volLevel.setText(progress + "%");
                    volLevel.setContentDescription(getString(R.string.speaker_volume_description, volLevel.getText()));
            }     else if (seekBar == micSeekBar) {
                    if (getService().isVoiceActivationEnabled()) {
                        int voxLevel = progress;
                        getClient().setVoiceActivationLevel(voxLevel);
                        prefs.put(Preferences.PREF_SOUNDSYSTEM_VOICEACTIVATION_LEVEL, voxLevel);
                        micLevel.setText(progress + "%");
                        micLevel.setContentDescription(getString(R.string.vox_level_description, micLevel.getText()));
                    } else {
                        int inputGain = Utils.refGain(progress);
                        getClient().setSoundInputGainLevel(inputGain);
                        prefs.put(Preferences.PREF_SOUNDSYSTEM_MICROPHONEGAIN, inputGain);
                        micLevel.setText(progress + "%");
                        micLevel.setContentDescription(getString(R.string.mic_gain_description, micLevel.getText()));
                    }
                }
        }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };

        masterSeekBar.setOnSeekBarChangeListener(volListener);
        micSeekBar.setOnSeekBarChangeListener(volListener);

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) && accessibilityAssistant.isServiceActive()) {
            tx_btn.setOnClickListener(txButtonListener);
        }

        ImageButton speakerBtn = findViewById(R.id.speakerBtn);
        speakerBtn.setOnClickListener(v -> {
            if ((mConnection != null) && mConnection.isBound()) {
                getService().setMute(!getService().isMute());
                adjustMuteButton((ImageButton) v);

                int level = getService().isMute() ?
                    0 :
                    Utils.refVolumeToPercent(getClient().getSoundOutputVolume());
                volLevel.setText(level + "%");
                volLevel.setContentDescription(getString(R.string.speaker_volume_description, volLevel.getText()));
            }
        });

        ImageButton voxSwitch = findViewById(R.id.voxSwitch);
        voxSwitch.setOnClickListener(v -> {
            if ((mConnection != null) && mConnection.isBound()) {
                if (getService().isVoiceTransmissionEnabled())
                    getService().enableVoiceTransmission(false);
                getService().enableVoiceActivation(!getService().isVoiceActivationEnabled());

                adjustVoiceGain();
            }
        });
    }

    private void initializeTeamTalkService(TeamTalkService service) {

        this.users = new HashMap<>(service.getUsers());

        int mychanid = getClient().getMyChannelID();
        if (mychanid > 0) {
            setCurrentChannel(service.getChannels().get(mychanid));
        }

        setMyChannel(service.getChannels().get(mychanid));

        mSectionsPagerAdapter.onPageSelected(mViewPager.getCurrentItem());

        channelsAdapter.notifyDataSetChanged();

        textmsgAdapter.setTextMessages(service.getChatLogTextMsgs());
        textmsgAdapter.setMyUserID(getClient().getMyUserID());
        textmsgAdapter.notifyDataSetChanged();

        channelChatAdapter.setTextMessages(service.getChatLogTextMsgs());
        channelChatAdapter.setMyUserID(getClient().getMyUserID());
        channelChatAdapter.notifyDataSetChanged();

        globalChatAdapter.setTextMessages(service.getChatLogTextMsgs());
        globalChatAdapter.setMyUserID(getClient().getMyUserID());
        globalChatAdapter.notifyDataSetChanged();

        eventHistoryAdapter.setTextMessages(service.getChatLogTextMsgs());
        eventHistoryAdapter.setMyUserID(getClient().getMyUserID());
        eventHistoryAdapter.notifyDataSetChanged();

        userIdsWithMessages.clear();
        for (User u : service.getUsers().values()) {
            if (!service.getUserTextMsgs(u.nUserID).isEmpty()) {
                userIdsWithMessages.add(u.nUserID);
            }
        }
        privateConversationsAdapter.setUserIds(new ArrayList<>(userIdsWithMessages));

        mediaAdapter.setTeamTalkService(service);
        mediaAdapter.notifyDataSetChanged();

        filesAdapter.setTeamTalkService(service);
        filesAdapter.update(mychanid);

        int outsndid = SoundDeviceConstants.TT_SOUNDDEVICE_ID_OPENSLES_DEFAULT;

        int flags = getClient().getFlags();
        if (((flags & ClientFlag.CLIENT_SNDOUTPUT_READY) == 0) &&
                !getClient().initSoundOutputDevice(outsndid))
            Toast.makeText(this, R.string.err_init_sound_output, Toast.LENGTH_LONG).show();

        if (!restarting) {
            service.setMute(false);
            service.enableVoiceTransmission(false);
            service.enableVoiceActivation(false);
            if (Permissions.READ_PHONE_STATE.request(this))
                service.enablePhoneCallReaction();
        }

        service.getEventHandler().registerOnConnectionLostListener(this, true);
        service.getEventHandler().registerOnCmdProcessing(this, true);
        service.getEventHandler().registerOnCmdMyselfLoggedIn(this, true);
        service.getEventHandler().registerOnCmdMyselfLoggedOut(this, true);
        service.getEventHandler().registerOnCmdMyselfKickedFromChannel(this, true);
        service.getEventHandler().registerOnCmdUserLoggedIn(this, true);
        service.getEventHandler().registerOnCmdUserLoggedOut(this, true);
        service.getEventHandler().registerOnCmdUserUpdate(this, true);
        service.getEventHandler().registerOnCmdUserJoinedChannel(this, true);
        service.getEventHandler().registerOnCmdUserLeftChannel(this, true);
        service.getEventHandler().registerOnCmdUserTextMessage(this, true);
        service.getEventHandler().registerOnCmdChannelNew(this, true);
        service.getEventHandler().registerOnCmdChannelUpdate(this, true);
        service.getEventHandler().registerOnCmdChannelRemove(this, true);
        service.getEventHandler().registerOnCmdFileNew(this, true);
        service.getEventHandler().registerOnCmdFileRemove(this, true);
        service.getEventHandler().registerOnUserStateChange(this, true);
        service.getEventHandler().registerOnVoiceActivation(this, true);

        service.setOnVoiceTransmissionToggleListener(this);

        adjustSoundSystem();

        if (prefs.get(Preferences.PREF_SOUNDSYSTEM_BLUETOOTH_HEADSET, false)
                && Permissions.BLUETOOTH.request(this))
            service.watchBluetoothHeadset();

        if (Permissions.WAKE_LOCK.request(this))
            wakeLock.acquire();

        int mastervol = prefs.get(Preferences.PREF_SOUNDSYSTEM_MASTERVOLUME, SoundLevel.SOUND_VOLUME_DEFAULT);
        int gain = prefs.get(Preferences.PREF_SOUNDSYSTEM_MICROPHONEGAIN, SoundLevel.SOUND_GAIN_DEFAULT);
        int voxlevel = prefs.get(Preferences.PREF_SOUNDSYSTEM_VOICEACTIVATION_LEVEL, 5);
        boolean voxState = service.isVoiceActivationEnabled();
        boolean txState = service.isVoiceTransmitting();

        if (getClient().getSoundOutputVolume() != mastervol)
            getClient().setSoundOutputVolume(mastervol);
        if (getClient().getSoundInputGainLevel() != gain)
            getClient().setSoundInputGainLevel(gain);
        if (getClient().getVoiceActivationLevel() != voxlevel)
            getClient().setVoiceActivationLevel(voxlevel);

        adjustMuteButton(findViewById(R.id.speakerBtn));
        adjustVoxState(voxState, voxState ? voxlevel : getClient().getSoundInputGainLevel());
        adjustTxState(txState);

        final SeekBar masterSeekBar = findViewById(R.id.master_volSeekBar);
        final SeekBar micSeekBar = findViewById(R.id.mic_gainSeekBar);
        masterSeekBar.setProgress(Utils.refVolumeToPercent(getClient().getSoundOutputVolume()));
        if (service.isVoiceActivationEnabled()) {
            micSeekBar.setProgress(getClient().getVoiceActivationLevel());
        } else {
            micSeekBar.setProgress(Utils.refVolumeToPercent(getClient().getSoundInputGainLevel()));
        }
        TextView volLevel = findViewById(R.id.vollevel_text);
        volLevel.setText(Utils.refVolumeToPercent(mastervol) + "%");
        volLevel.setContentDescription(getString(R.string.speaker_volume_description, volLevel.getText()));
    }

    private void closeTeamTalkService(TeamTalkService service) {
        if (wakeLock.isHeld())
            wakeLock.release();
        service.setOnVoiceTransmissionToggleListener(null);

        service.getEventHandler().unregisterListener(this);

        filesAdapter.setTeamTalkService(null);
        mediaAdapter.clearTeamTalkService(service);
    }

    @Override
    public void onServiceConnected(TeamTalkService service) {
        initializeTeamTalkService(service);
    }

    @Override
    public void onServiceDisconnected(TeamTalkService service) {
        closeTeamTalkService(service);
    }

    TeamTalkService getService() {
        return mConnection.getService();
    }

    TeamTalkBase getClient() {
        return getService().getTTInstance();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permissions granted = Permissions.onRequestResult(this, requestCode, grantResults);
        if (granted == null) {
            granted = Permissions.fromRequestCode(requestCode);
            if ((granted != Permissions.READ_MEDIA_IMAGES) &&
                (granted != Permissions.READ_MEDIA_VIDEO) &&
                (granted != Permissions.READ_MEDIA_AUDIO))
                return;
        }
        switch (granted) {
            case READ_EXTERNAL_STORAGE:
            case READ_MEDIA_IMAGES:
            case READ_MEDIA_VIDEO:
            case READ_MEDIA_AUDIO:
                if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) || areMediaPermissionsComplete())
                    fileSelectionStart();
                break;
            case WAKE_LOCK:
                wakeLock.acquire();
                break;
            case READ_PHONE_STATE:
                if ((mConnection != null) && mConnection.isBound())
                    getService().enablePhoneCallReaction();
                break;
            case BLUETOOTH:
                if ((mConnection != null) && mConnection.isBound())
                    getService().watchBluetoothHeadset();
                break;
            default:
                break;
        }
    }

    @Override
    public void onCmdProcessing(int cmdId, boolean complete) {
        if(complete) {
            activecmds.remove(cmdId);
        }
    }

    @Override
    public void onCmdMyselfLoggedIn(int my_userid, UserAccount useraccount) {
        textmsgAdapter.setMyUserID(my_userid);

        channelsAdapter.notifyDataSetChanged();

        if (mSectionsPagerAdapter != null) {
            mSectionsPagerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCmdMyselfLoggedOut() {
        accessibilityAssistant.lockEvents();
        channelsAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();
    }

    @Override
    public void onCmdMyselfKickedFromChannel() {
        accessibilityAssistant.lockEvents();
        channelsAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();
    }

    @Override
    public void onCmdMyselfKickedFromChannel(User kicker) {
        accessibilityAssistant.lockEvents();
        channelsAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();
    }

    @Override
    public void onCmdUserLoggedIn(User user) {
        users.put(user.nUserID, user);
        prevChannels.remove(user.nUserID);
        
        // Log removed (handled by TeamTalkService)
        // String name = Utils.getDisplayName(getBaseContext(), user);
        // logToEventHistory(name + " " + getString(R.string.text_tts_loggedin));

        accessibilityAssistant.lockEvents();
        textmsgAdapter.notifyDataSetChanged();
        eventHistoryAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();

        if (sounds.get(SOUND_USERLOGGEDIN) != 0)
            audioIcons.play(sounds.get(SOUND_USERLOGGEDIN), 1.0f, 1.0f, 0, 0, 1.0f);
        if (ttsWrapper != null && prefs.get("server_login_checkbox", true)) {
            String name = Utils.getDisplayName(getBaseContext(), user);
            ttsWrapper.speak(name + " " + getResources().getString(R.string.text_tts_loggedin));
        }

        if (onlineUsersAdapter != null) {
            onlineUsersAdapter.updateUsers(new ArrayList<>(users.values()));
        }
    }

    @Override
    public void onCmdUserLoggedOut(User user) {
        users.remove(user.nUserID);
        prevChannels.remove(user.nUserID);

        accessibilityAssistant.lockEvents();
        textmsgAdapter.notifyDataSetChanged();
        eventHistoryAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();

        if (sounds.get(SOUND_USERLOGGEDOFF) != 0)
            audioIcons.play(sounds.get(SOUND_USERLOGGEDOFF), 1.0f, 1.0f, 0, 0, 1.0f);
        if (ttsWrapper != null && prefs.get("server_logout_checkbox", true)) {
            String name = Utils.getDisplayName(getBaseContext(), user);
            ttsWrapper.speak(name + " " + getResources().getString(R.string.text_tts_loggedout));
        }

        // Duplicate log removed - logging is handled by TeamTalkService

        if (onlineUsersAdapter != null) {
            onlineUsersAdapter.updateUsers(new ArrayList<>(users.values()));
        }
    }

    @Override
    public void onCmdUserUpdate(User user) {
        if(curchannel != null && curchannel.nChannelID == user.nChannelID) {
            accessibilityAssistant.lockEvents();
            channelsAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        }

        subscriptionChange(user);

        users.put(user.nUserID, user);

        if (onlineUsersAdapter != null) {
            onlineUsersAdapter.updateUsers(new ArrayList<>(users.values()));
        }
    }

    @Override
    public void onCmdUserJoinedChannel(User user) {
        if(user.nUserID == getClient().getMyUserID()) {

            Channel chan = getService().getChannels().get(user.nChannelID);
            setCurrentChannel(chan);
            filesAdapter.update(curchannel);

            setMyChannel(chan);

            accessibilityAssistant.lockEvents();
            channelsAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        }

        Integer prevChanId = prevChannels.remove(user.nUserID);
        
        // Update user state
        users.put(user.nUserID, user);

        String name = Utils.getDisplayName(getBaseContext(), user);
        String newChanLoggingName = getChannelNameForTTS(user.nChannelID);
        boolean isToMyChannel = (mychannel != null && mychannel.nChannelID == user.nChannelID);

        String joinLogMsg = name + " " + getString(R.string.text_tts_joined_chan) + " " + newChanLoggingName;
        
        // Only log if NOT joined my channel (Service handles my channel)
        if (!isToMyChannel) {
            logToEventHistory(joinLogMsg);
        }
        
        if (ttsWrapper != null && user.nUserID != getClient().getMyUserID() && prefs.get("channel_join_checkbox", true)) {
            
            if (isToMyChannel) {
                 // Joined MY channel - "Fulano entered the channel"
                ttsWrapper.speak(name + " " + getString(R.string.text_tts_joined_chan));
            } else {
                // Joined ANOTHER channel - "Fulano entered the channel [Channel Name]"
                ttsWrapper.speak(joinLogMsg);
            }
        }

        if(mychannel != null && mychannel.nChannelID == user.nChannelID) {

            if(user.nUserID != getClient().getMyUserID()) {
                accessibilityAssistant.lockEvents();
                textmsgAdapter.notifyDataSetChanged();
                eventHistoryAdapter.notifyDataSetChanged();
                channelsAdapter.notifyDataSetChanged();
                
                if (mychannel.nChannelID == user.nChannelID) {
                    if (sounds.get(SOUND_USERJOIN) != 0)
                        audioIcons.play(sounds.get(SOUND_USERJOIN), 1.0f, 1.0f, 0, 0, 1.0f);
                }
                accessibilityAssistant.unlockEvents();
            }
            else {
                textmsgAdapter.notifyDataSetChanged();
                eventHistoryAdapter.notifyDataSetChanged();
                channelsAdapter.notifyDataSetChanged();
            }
        }
        else if (isVisibleChannel(user.nChannelID)) {
            accessibilityAssistant.lockEvents();
            channelsAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        }

        if (onlineUsersAdapter != null) {
            onlineUsersAdapter.updateUsers(new ArrayList<>(users.values()));
        }
    }

    @Override
    public void onCmdUserLeftChannel(int channelid, User user) {
        prevChannels.put(user.nUserID, channelid);
        users.put(user.nUserID, user);
        
        String logName = Utils.getDisplayName(getBaseContext(), user);
        String chanName = getChannelNameForTTS(channelid);
        
        // Log message always uses full format
        boolean isFromMyChannel = (mychannel != null && mychannel.nChannelID == channelid);
        // Only log if NOT from my channel (Service handles my channel)
        if (!isFromMyChannel) {
             logToEventHistory(logName + " " + getString(R.string.text_tts_left_chan) + " " + chanName);
        }

        if(user.nUserID == getClient().getMyUserID()) {

            textmsgAdapter.notifyDataSetChanged();
            eventHistoryAdapter.notifyDataSetChanged();

            // No TTS when I leave the channel

            setCurrentChannel(null);
            setMyChannel(null);
        }
        else if(curchannel != null && channelid == curchannel.nChannelID){

            accessibilityAssistant.lockEvents();
            textmsgAdapter.notifyDataSetChanged();
            eventHistoryAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        }

        if(mychannel != null && mychannel.nChannelID == channelid) {

            accessibilityAssistant.lockEvents();
            textmsgAdapter.notifyDataSetChanged();
            eventHistoryAdapter.notifyDataSetChanged();
            channelsAdapter.notifyDataSetChanged();
            if (getClient().getMyChannelID() == channelid) {
                    if (sounds.get(SOUND_USERLEFT) != 0)
                        audioIcons.play(sounds.get(SOUND_USERLEFT), 1.0f, 1.0f, 0, 0, 1.0f);
                if (ttsWrapper != null && prefs.get("channel_leave_checkbox", true)) {
                    // Remote user left MY channel: "Name has left the channel"
                    String name = Utils.getDisplayName(getBaseContext(), user);
                    ttsWrapper.speak(name + " " + getString(R.string.text_tts_left_chan));
                }
            }
            accessibilityAssistant.unlockEvents();
        }
        else if (isVisibleChannel(channelid)) {
            accessibilityAssistant.lockEvents();
            channelsAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();

            if (ttsWrapper != null && prefs.get("channel_leave_checkbox", true)) {
                 // Remote user left ANOTHER channel: "Name has left the channel X"
                 String name = Utils.getDisplayName(getBaseContext(), user);
                 ttsWrapper.speak(name + " " + getString(R.string.text_tts_left_chan) + " " + chanName);
            }
        }

        if (onlineUsersAdapter != null) {
            onlineUsersAdapter.updateUsers(new ArrayList<>(users.values()));
        }
    }

    @Override
    public void onCmdUserTextMessage(TextMessage textmessage) {
        accessibilityAssistant.lockEvents();

        if (textmessage.nMsgType == TextMsgType.MSGTYPE_USER && textmessage.nFromUserID != getClient().getMyUserID()) {
            if (userIdsWithMessages.add(textmessage.nFromUserID)) {
                privateConversationsAdapter.setUserIds(new ArrayList<>(userIdsWithMessages));
            }
        }

        channelChatAdapter.notifyDataSetChanged();
        globalChatAdapter.notifyDataSetChanged();
        textmsgAdapter.notifyDataSetChanged();
        eventHistoryAdapter.notifyDataSetChanged();
        privateConversationsAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();

        if (textmessage.nFromUserID != getClient().getMyUserID()) {
            switch (textmessage.nMsgType) {
                case TextMsgType.MSGTYPE_CHANNEL :
                    if (sounds.get(SOUND_CHANMSG) != 0)
                        audioIcons.play(sounds.get(SOUND_CHANMSG), 1.0f, 1.0f, 0, 0, 1.0f);
                    if (ttsWrapper != null && prefs.get("channel_message_checkbox", true)) {
                        User sender = getService().getUsers().get(textmessage.nFromUserID);
                        String name = Utils.getDisplayName(getBaseContext(), sender);
                        ttsWrapper.speak(getString(R.string.text_tts_channel_message, (sender != null) ? name : "", textmessage.szMessage));
                    }
                    Log.d(TAG, "Channel message from " + textmessage.nFromUserID);
                    break;
                case TextMsgType.MSGTYPE_BROADCAST :
                    if (sounds.get(SOUND_BCASTMSG) != 0)
                        audioIcons.play(sounds.get(SOUND_BCASTMSG), 1.0f, 1.0f, 0, 0, 1.0f);
                    if (ttsWrapper != null && prefs.get("broadcast_message_checkbox", true)) {
                        User sender = getService().getUsers().get(textmessage.nFromUserID);
                        String name = Utils.getDisplayName(getBaseContext(), sender);
                        ttsWrapper.speak(getString(R.string.text_tts_broadcast_message, (sender != null) ? name : "", textmessage.szMessage));
                    }
                    break;
                case TextMsgType.MSGTYPE_USER :
                    if (sounds.get(SOUND_USERMSG) != 0)
                        audioIcons.play(sounds.get(SOUND_USERMSG), 1.0f, 1.0f, 0, 0, 1.0f);
                    if (ttsWrapper != null && prefs.get("private_message_checkbox", true)) {
                        User sender = getService().getUsers().get(textmessage.nFromUserID);
                        String name = Utils.getDisplayName(getBaseContext(), sender);
                        ttsWrapper.speak(getString(R.string.text_tts_private_message, (sender != null) ? name : "", textmessage.szMessage));
                    }

                    User sender = getService().getUsers().get(textmessage.nFromUserID);
                    String senderName = (sender != null) ? Utils.getDisplayName(getBaseContext(), sender) : "";
                    Intent action = new Intent(this, TextMessageActivity.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel mChannel = new NotificationChannel(MSG_NOTIFICATION_CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH);
                        mChannel.enableVibration(false);
                        mChannel.setVibrationPattern(null);
                        mChannel.enableLights(false);
                        mChannel.setSound(null, null);
                        notificationManager.createNotificationChannel(mChannel);
                    }
                    Notification notification = new NotificationCompat.Builder(this, MSG_NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.message)
                        .setContentTitle(getString(R.string.private_message_notification, senderName))
                        .setContentText(getString(R.string.private_message_notification_hint))
                        .setContentIntent(PendingIntent.getActivity(this, textmessage.nFromUserID, action.putExtra(TextMessageActivity.EXTRA_USERID, textmessage.nFromUserID), PendingIntent.FLAG_IMMUTABLE))
                        .setAutoCancel(true)
                        .build();
                    notificationManager.notify(MESSAGE_NOTIFICATION_TAG, textmessage.nFromUserID, notification);
                    break;
            }
        }
        else if (textmessage.nFromUserID == getClient().getMyUserID() && textmessage.nMsgType == TextMsgType.MSGTYPE_CHANNEL) {
            if (sounds.get(SOUND_CHANMSGSENT) != 0)
                audioIcons.play(sounds.get(SOUND_CHANMSGSENT), 1.0f, 1.0f, 0, 0, 1.0f);
            if (ttsWrapper != null && prefs.get("channel_message_sent_checkbox", true)) {
                ttsWrapper.speak(getString(R.string.text_tts_channel_message_sent, textmessage.szMessage));
            }
        }
    }

    @Override
    public void onCmdChannelNew(Channel channel) {
        if (curchannel != null && curchannel.nChannelID == channel.nParentID) {
            accessibilityAssistant.lockEvents();
            channelsAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        }
    }

    @Override
    public void onCmdChannelUpdate(Channel channel) {
        if (curchannel != null && curchannel.nChannelID == channel.nParentID) {
            accessibilityAssistant.lockEvents();
            channelsAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        }

        if(mychannel != null && mychannel.nChannelID == channel.nChannelID) {

            if (ttsWrapper != null) {
                Utils.ttsTransmitUsersToggled(getBaseContext(), mychannel, channel, getService().getUsers()).ifPresent(text -> ttsWrapper.speak(text));
            }

            int myuserid = getClient().getMyUserID();

            if(channel.transmitUsersQueue[0] == myuserid && mychannel.transmitUsersQueue[0] != myuserid) {
                if(sounds.get(SOUND_TXREADY) != 0) {
                    audioIcons.play(sounds.get(SOUND_TXREADY), 1.0f, 1.0f, 0, 0, 1.0f);
                }
            }
            if(mychannel.transmitUsersQueue[0] == myuserid && channel.transmitUsersQueue[0] != myuserid) {
                if(sounds.get(SOUND_TXSTOP) != 0) {
                    audioIcons.play(sounds.get(SOUND_TXSTOP), 1.0f, 1.0f, 0, 0, 1.0f);
                }
            }

            setMyChannel(channel);
        }
    }

    @Override
    public void onCmdChannelRemove(Channel channel) {
        if (curchannel != null && curchannel.nChannelID == channel.nParentID) {
            accessibilityAssistant.lockEvents();
            channelsAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        }
    }

    @Override
    public void onCmdFileNew(RemoteFile remotefile) {
        filesAdapter.update();

        if(activecmds.size() == 0 && getClient().getMyChannelID() == remotefile.nChannelID) {
            if(sounds.get(SOUND_FILESUPDATE) != 0) {
                audioIcons.play(sounds.get(SOUND_FILESUPDATE), 1.0f, 1.0f, 0, 0, 1.0f);
            }
        }
    }

    @Override
    public void onCmdFileRemove(RemoteFile remotefile) {
        filesAdapter.update();

        if(activecmds.size() == 0 && getClient().getMyChannelID() == remotefile.nChannelID) {
            if(sounds.get(SOUND_FILESUPDATE) != 0) {
                audioIcons.play(sounds.get(SOUND_FILESUPDATE), 1.0f, 1.0f, 0, 0, 1.0f);
            }
        }
    }

    @Override
    public void onConnectionLost() {
        if(sounds.get(SOUND_SERVERLOST) != 0) {
            audioIcons.play(sounds.get(SOUND_SERVERLOST), 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    @Override
    public void onUserStateChange(User user) {
        users.put(user.nUserID, user);

        if (curchannel != null && user.nChannelID == curchannel.nChannelID) {
            accessibilityAssistant.lockEvents();
            channelsAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        }

    }

    @Override
    public void onVoiceTransmissionToggle(boolean voiceTransmissionEnabled, boolean isSuspended) {
        adjustTxState(voiceTransmissionEnabled);

        if (!isSuspended) {
            boolean ptt_vibrate = prefs.get("vibrate_checkbox", true) &&
                Permissions.VIBRATE.request(this);
            if (voiceTransmissionEnabled) {
                accessibilityAssistant.shutUp();
                if (sounds.get(SOUND_VOICETXON) != 0) {
                    audioIcons.play(sounds.get(SOUND_VOICETXON), 1.0f, 1.0f, 0, 0, 1.0f);
                }
                if (ptt_vibrate) {
                    Vibrator vibrat = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibrat.vibrate(50);
                }
            } else {
                if (sounds.get(SOUND_VOICETXOFF) != 0) {
                    audioIcons.play(sounds.get(SOUND_VOICETXOFF), 1.0f, 1.0f, 0, 0, 1.0f);
                }
                if (ptt_vibrate) {
                    Vibrator vibrat = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    long[] pattern = { 0, 20, 80, 20 };
                    vibrat.vibrate(pattern, -1);
                }
            }
        }
    }

    @Override
    public void onVoiceActivationToggle(boolean voiceActivationEnabled, boolean isSuspended) {
        adjustVoxState(voiceActivationEnabled, voiceActivationEnabled ? getClient().getVoiceActivationLevel() : getClient().getSoundInputGainLevel());
        if (voiceActivationEnabled) {
            if (sounds.get(SOUND_VOXENABLE) != 0) {
                audioIcons.play(sounds.get(SOUND_VOXENABLE), 1.0f, 1.0f, 0, 0, 1.0f);
            }
        } else {
            if (sounds.get(SOUND_VOXDISABLE) != 0) {
                audioIcons.play(sounds.get(SOUND_VOXDISABLE), 1.0f, 1.0f, 0, 0, 1.0f);
            }
        }
    }

    @Override
    public void onVoiceActivation(boolean bVoiceActive) {
        adjustTxState(bVoiceActive);

        int sound = sounds.get(bVoiceActive ? SOUND_VOXON : SOUND_VOXOFF);
        if (sound != 0)
            audioIcons.play(sound, 1.0f, 1.0f, 0, 0, 1.0f);
    }

    class PrivateConversationsAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private List<Integer> userIds = new ArrayList<>();

        public PrivateConversationsAdapter(Context context) {
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return userIds.size();
        }

        @Override
        public Object getItem(int position) {
            return userIds.get(position);
        }

        @Override
        public long getItemId(int position) {
            return userIds.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_private_conv, parent, false);
            }

            int userId = userIds.get(position);
            User user = users.get(userId);
            String nickname = "";
            if (user != null) {
                 nickname = Utils.getDisplayName(MainActivity.this, user);
            } else {
                 nickname = "User " + userId;
            }

            TextView nickname_tv = convertView.findViewById(R.id.conv_user_nickname);
            nickname_tv.setText(nickname);
            
            
            
            
            
            
            
            
            
            

            return convertView;
        }

        public void setUserIds(List<Integer> ids) {
            this.userIds = ids;
            notifyDataSetChanged();
        }
    }
}