
package dk.bearware.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.res.AssetFileDescriptor;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.Settings;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.os.Build;
import androidx.activity.OnBackPressedCallback;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.View.OnClickListener;
import dk.bearware.data.AppInfo;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.app.NotificationCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.ListFragment;
import androidx.viewpager.widget.ViewPager;

import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;
import java.util.Vector;

import dk.bearware.Channel;
import dk.bearware.ClientFlag;
import dk.bearware.ClientStatistics;
import dk.bearware.RemoteFile;
import dk.bearware.ServerProperties;
import dk.bearware.SoundDeviceConstants;
import dk.bearware.SoundLevel;
import dk.bearware.TeamTalk5;
import dk.bearware.data.License;
import dk.bearware.BanType;
import dk.bearware.StreamType;
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
        ClientEventListener.OnCmdFileNewListener,
        AccessibilityAssistant.OnAccessibilityActionClickListener {

    private static final int MIC_INPUT_DEFAULT = 0;
    private static final int MIC_INPUT_INTERNAL = 1;
    private static final int MIC_INPUT_EXTERNAL = 2;
    private int currentMicInput = MIC_INPUT_DEFAULT;

    private ImageButton micInputButton;
    private BroadcastReceiver headsetReceiver;

    private int lastPos = -1;
    SectionsPagerAdapter mSectionsPagerAdapter;

    CustomViewPager mViewPager;
    TabLayout mTabLayout;

    public static final String TAG = "bearware";

    private static final String MESSAGE_NOTIFICATION_TAG = "incoming_message";
    private static final String MSG_NOTIFICATION_CHANNEL_ID = "dk.bearware.gui.MSG_CHANNEL";

    public final int SOUND_VOICETXON = 1,
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
            SOUND_CHANMSGSENT = 20,
            SOUND_MUTEALL = 21,
            SOUND_UNMUTEALL = 22,
            SOUND_USERMSGSENT = 23,
            SOUND_TYPING = 24;

    public final int REQUEST_EDITCHANNEL = 1,
            REQUEST_NEWCHANNEL = 2,
            REQUEST_EDITUSER = 3,
            REQUEST_SELECT_FILE = 4,
            REQUEST_SETTINGS = 5;

    TeamTalkConnection mConnection;

    Channel curchannel;

    Channel mychannel;

    /**
     * True during the very first channel join right after login. The 200ms handler
     * in onCmdMyselfLoggedIn already fires the join TTS, so we skip it in
     * onCmdUserJoinedChannel to avoid duplicates. Cleared after the first join.
     */
    private boolean isFirstJoinAfterLogin = false;

    private ExpandableListView mediaListView;
    private ListView listChannels;
    private dk.bearware.UserAccount myUserAccount = new dk.bearware.UserAccount();

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
    NotificationManager notificationManager;
    PowerManager.WakeLock wakeLock, proximityWakeLock;
    boolean restarting;
    SensorManager mSensorManager;
    Sensor mSensor;
    boolean isProximitySensorRegistered = false;
    Map<Integer, User> users = new HashMap<>();
    Map<Integer, Integer> prevChannels = new HashMap<>();
    java.util.Set<Integer> userIdsWithMessages = new java.util.TreeSet<>();
    private Set<Integer> recentLogins = new HashSet<>();
    private Set<Integer> loginsSoundPlayed = new HashSet<>();
    private Map<Integer, Runnable> pendingLeaveSounds = new HashMap<>();
    private Map<Integer, Runnable> pendingTTSLeaveRunnables = new HashMap<>();
    private Map<Integer, String> pendingTTSLeaveChanNames = new HashMap<>();
    private Runnable pendingMyselfLeftChanTTS = null;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService soundExecutor;

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

    private static MainActivity instance;
    private android.content.SharedPreferences.OnSharedPreferenceChangeListener prefListener;

    public static void playPrivateMessageSentSound(String message) {
        if (instance != null) {
            instance.playSound(instance.SOUND_USERMSGSENT);
            if (instance.prefs.get("private_message_sent_checkbox", true)) {
                String ttsMsg = instance.getString(R.string.text_tts_private_message_sent, message);
                instance.speakAndLog(ttsMsg, MyTextMessage.MSGTYPE_LOG_INFO);
            }
        }
    }

    public static void playChannelMessageSentSound() {
        if (instance != null) {
            instance.playSound(instance.SOUND_CHANMSGSENT);
            // TTS for channel message sent is usually handled in onCmdTextMessage echo
        }
    }
    private void initSoundFilenames() {
        // Obsolete in MainActivity as sounds moved to Service, 
        // but kept empty to avoid compilation errors if called elsewhere in this file.
        // Actually, some parts still call it.
    }

    private void playSound(int soundEvent) {
        if (getService() != null) {
            getService().playSound(soundEvent);
        }
    }

    private void speakAndLog(String ttsMsg, int logType) {
        if (ttsWrapper != null) {
            ttsWrapper.speak(ttsMsg);
        }
        if (getService() != null) {
            MyTextMessage msg = MyTextMessage.createLogMsg(logType, ttsMsg);
            getService().getChatLogTextMsgs().add(msg);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        prefs = new PrefsHelper(this);

        prefListener = (sharedPreferences, key) -> {
            // preferences changes handled here if needed
        };
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(prefListener);

        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            Log.e("bearware", "FATAL EXCEPTION IN MAIN ACTIVITY", e);
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, e);
            }
        });

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initSoundFilenames(); // Initialize sound filenames map
        // Use 'this' instead of getApplicationContext() to ensure the Activity's updated 
        // resources (from attachBaseContext) are used for localized strings.
        ctx = this;
        TeamTalk5.setLicenseInformation(License.REGISTRATION_NAME, License.REGISTRATION_KEY);

        soundExecutor = Executors.newSingleThreadExecutor();

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
        accessibilityAssistant.setOnAccessibilityActionClickListener(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                TAG + ":TeamTalk5");
        proximityWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG + ":TeamTalk5");
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

        mViewPager = findViewById(R.id.pager);
        mTabLayout = findViewById(R.id.tab_layout);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(mSectionsPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
        
    // Initial restricted positions
    updateSwipeRestrictions();
    if (savedInstanceState != null) {
        boolean expanded = savedInstanceState.getBoolean("tabs_expanded", false);
        mSectionsPagerAdapter.updateTabs(expanded);
    }
    if (savedInstanceState == null) {
        mViewPager.setCurrentItem(mSectionsPagerAdapter.getPositionForId(SectionsPagerAdapter.CHANNELS_PAGE));
    }

        initSoundFilenames();
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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (Build.MANUFACTURER.equalsIgnoreCase("Samsung")) {
                    int channelsPageIndex = mSectionsPagerAdapter.getPositionForId(SectionsPagerAdapter.CHANNELS_PAGE);
                    if (channelsPageIndex != -1 && mViewPager.getCurrentItem() != channelsPageIndex) {
                        mViewPager.setCurrentItem(channelsPageIndex);
                    } else {
                        if (getService() != null) {
                            getService().disconnect();
                        }
                        setEnabled(false);
                        onBackPressed();
                        setEnabled(true);
                    }
                } else {
                    setEnabled(false);
                    onBackPressed();
                    setEnabled(true);
                }
            }
        });
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

        boolean uploadRight = (myuseraccount.uUserRights
                & UserRight.USERRIGHT_UPLOAD_FILES) != UserRight.USERRIGHT_NONE;
        boolean isEditable = curchannel != null;
        boolean isJoinable = curchannel != null && getClient().getMyChannelID() != curchannel.nChannelID;
        boolean isLeaveable = getClient().getMyChannelID() > 0;
        boolean isMyChannel = curchannel != null && getClient().getMyChannelID() == curchannel.nChannelID;

        boolean inChannelsTab = currentPage == SectionsPagerAdapter.CHANNELS_PAGE;

        boolean isOperator = false;
        if (curchannel != null && getClient().isChannelOperator(getClient().getMyUserID(), curchannel.nChannelID)) {
            isOperator = true;
        }

        boolean canBan = (myuseraccount.uUserRights & UserRight.USERRIGHT_BAN_USERS) != UserRight.USERRIGHT_NONE;
        boolean canMove = (myuseraccount.uUserRights & UserRight.USERRIGHT_MOVE_USERS) != UserRight.USERRIGHT_NONE;
        boolean canCreateChannel = (myuseraccount.uUserRights
                & UserRight.USERRIGHT_MODIFY_CHANNELS) != UserRight.USERRIGHT_NONE ||
                (myuseraccount.uUserRights & UserRight.USERRIGHT_CREATE_TEMPORARY_CHANNEL) != UserRight.USERRIGHT_NONE;

        boolean showLeave = isLeaveable
                && (curchannel == null || curchannel.nChannelID == getClient().getMyChannelID());
        menu.findItem(R.id.action_edit).setEnabled(isEditable).setVisible(isEditable && inChannelsTab);
        menu.findItem(R.id.action_join).setEnabled(isJoinable).setVisible(isJoinable && inChannelsTab);
        MenuItem leaveItem = menu.findItem(R.id.action_leave);
        leaveItem.setEnabled(isLeaveable).setVisible(showLeave && inChannelsTab);
        if (curchannel == null && isLeaveable) {
            leaveItem.setTitle(R.string.action_stay_server);
        } else {
            leaveItem.setTitle(R.string.action_leave);
        }
        menu.findItem(R.id.action_move).setEnabled(canMove && isLeaveable)
                .setVisible(canMove && isLeaveable && inChannelsTab && curchannel != null);
        menu.findItem(R.id.action_banned_users).setEnabled((isOperator || canBan) && isLeaveable)
                .setVisible((isOperator || canBan) && isLeaveable && inChannelsTab && curchannel != null);
        menu.findItem(R.id.action_newchannel).setVisible(canCreateChannel && inChannelsTab && curchannel != null);

        boolean inFilesTab = currentPage == SectionsPagerAdapter.FILES_PAGE;
        menu.findItem(R.id.action_upload).setEnabled(uploadRight).setVisible(uploadRight && inFilesTab);

        boolean inMediaTab = currentPage == SectionsPagerAdapter.MEDIA_PAGE;
        MenuItem streamItem = menu.findItem(R.id.action_stream);
        streamItem.setEnabled(isMyChannel).setVisible(isMyChannel && inMediaTab);

        int flags = getClient().getFlags();
        boolean isStreaming = (flags & ClientFlag.CLIENT_STREAM_AUDIO) == ClientFlag.CLIENT_STREAM_AUDIO
                || (flags & ClientFlag.CLIENT_STREAM_VIDEO) == ClientFlag.CLIENT_STREAM_VIDEO;
        if (isStreaming) {
            streamItem.setTitle(R.string.action_stop_stream);
        } else {
            streamItem.setTitle(R.string.action_stream);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        if (item.getItemId() == R.id.action_join) {
            {
                if (curchannel != null)
                    joinChannel(curchannel);
            }
        } else if (item.getItemId() == R.id.action_leave) {
            {
                leaveChannel();
            }
        } else if (item.getItemId() == R.id.action_move) {
            {
                Intent intent = new Intent(this, dk.bearware.gui.MoveUsersActivity.class);
                startActivity(intent);
            }
        } else if (item.getItemId() == R.id.action_banned_users) {
            {
                Intent intent = new Intent(this, dk.bearware.gui.ChannelBannedUsersActivity.class);
                if (curchannel != null) {
                    intent.putExtra("channel_id", curchannel.nChannelID);
                }
                startActivity(intent);
            }
        } else if (item.getItemId() == R.id.action_upload) {
            {
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ? requestMediaPermissions()
                        : Permissions.READ_EXTERNAL_STORAGE.request(this)) {
                    fileSelectionStart();
                }
            }
        } else if (item.getItemId() == R.id.action_stream) {
            {
                int flags = getClient().getFlags();
                if ((flags & ClientFlag.CLIENT_STREAM_AUDIO) == ClientFlag.CLIENT_STREAM_AUDIO
                        || (flags & ClientFlag.CLIENT_STREAM_VIDEO) == ClientFlag.CLIENT_STREAM_VIDEO) {
                    getService().stopStreamingMediaFile();
                } else {
                    Intent intent = new Intent(MainActivity.this, StreamMediaActivity.class);
                    startActivity(intent);
                }
            }
        } else if (item.getItemId() == R.id.action_edit) {
            {
                if (curchannel != null)
                    editChannelProperties(curchannel);
            }
        } else if (item.getItemId() == R.id.action_newchannel) {
            {
                Intent intent = new Intent(MainActivity.this, ChannelPropActivity.class);
                int parent_chan_id = getClient().getRootChannelID();
                if (curchannel != null)
                    parent_chan_id = curchannel.nChannelID;
                intent = intent.putExtra(ChannelPropActivity.EXTRA_PARENTID, parent_chan_id);
                startActivityForResult(intent, REQUEST_NEWCHANNEL);
            }
        } else if (item.getItemId() == android.R.id.home) {
            int currentPage = mSectionsPagerAdapter.getIdForPosition(mViewPager.getCurrentItem());
            Channel parentChannel = ((currentPage == SectionsPagerAdapter.CHANNELS_PAGE)
                    && (curchannel != null)) ? getService().getChannels().get(curchannel.nParentID) : null;
            if (currentPage != SectionsPagerAdapter.CHANNELS_PAGE) {
                mViewPager.setCurrentItem(mSectionsPagerAdapter.getPositionForId(SectionsPagerAdapter.CHANNELS_PAGE));
            } else if (curchannel != null) {
                setCurrentChannel(parentChannel);
                channelsAdapter.notifyDataSetChanged();
            } else if (filesAdapter != null && filesAdapter.getActiveTransfersCount() > 0) {
                alert.setMessage(R.string.disconnect_alert);
                alert.setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    filesAdapter.cancelAllTransfers();
                    if (getService() != null) {
                        getService().disconnect();
                    }
                    finish();
                });
                alert.setNegativeButton(android.R.string.cancel, null);
                alert.show();
            } else {
                Log.d(TAG, "Home button pressed at root, ignoring focus exit");
            }
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mConnection.isBound()) {

            Intent intent = new Intent(ctx, TeamTalkService.class);
            Log.d(TAG, "Connecting to TeamTalk service");
            startService(intent); // explicitly start service
            if (!bindService(intent, mConnection, Context.BIND_AUTO_CREATE))
                Log.e(TAG, "Failed to connect to TeamTalk service");
        } else {
            adjustSoundSystem();
            if (prefs.get(Preferences.PREF_SOUNDSYSTEM_BLUETOOTH_HEADSET, false)) {
                if (Permissions.BLUETOOTH.request(this))
                    getService().watchBluetoothHeadset();
            } else
                getService().unwatchBluetoothHeadset();

            int mastervol = prefs.get(Preferences.PREF_SOUNDSYSTEM_MASTERVOLUME, SoundLevel.SOUND_VOLUME_DEFAULT);
            int gain = prefs.get(Preferences.PREF_SOUNDSYSTEM_MICROPHONEGAIN, 1300);
            // Safety: if gain is from a previous high-gain build (e.g. 32000), reset to default 1300
            if (gain > 17500) {
                gain = 1300;
                prefs.put(Preferences.PREF_SOUNDSYSTEM_MICROPHONEGAIN, gain);
            }
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
                micSeekBar.setProgress(Utils.refGainToPercent(getClient().getSoundInputGainLevel()));
            }
            TextView volLevel = findViewById(R.id.vollevel_text);
            volLevel.setText(Utils.refVolumeToPercent(mastervol) + getString(R.string.unit_percent));
            volLevel.setContentDescription(
                    getString(R.string.speaker_volume_description, volLevel.getText().toString()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initSoundFilenames(); // Ensure sound directory exists on resume
        registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        boolean proximitySensor = prefs.get("proximity_sensor_checkbox", false);
        if (proximitySensor) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            isProximitySensorRegistered = true;
        }


        getTextMessagesAdapter().showLogMessages(prefs.get("show_log_messages", true));

        getWindow().getDecorView().setKeepScreenOn(prefs.get("keep_screen_on_checkbox", false));
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
            ttsWrapper = null;

            audioManager.setMode(AudioManager.MODE_NORMAL);

            if (mConnection.isBound()) {
                Log.d(TAG, "Disconnecting from TeamTalk service");
                getService().disablePhoneCallReaction();
                getService().unwatchBluetoothHeadset();
                // getService().resetState(); // Removed to allow service to handle logout cleanup after announcements

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
        if (instance == this) instance = null;
        if (prefListener != null) {
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(prefListener);
            prefListener = null;
        }
        super.onDestroy();

        if (isProximitySensorRegistered) {
            mSensorManager.unregisterListener(this);
            isProximitySensorRegistered = false;
        }

        if (mConnection.isBound()) {
            Log.d(TAG, "Disconnecting from TeamTalk service");

            onServiceDisconnected(getService());
            unbindService(mConnection);
            mConnection.setBound(false);
        }

        Log.d(TAG, "Activity destroyed " + this.hashCode());
    }

    @Override
    public void onBackPressed() {
        int channelsPageIndex = mSectionsPagerAdapter.getPositionForId(SectionsPagerAdapter.CHANNELS_PAGE);
        if (channelsPageIndex != -1 && mViewPager.getCurrentItem() != channelsPageIndex) {
            mViewPager.setCurrentItem(channelsPageIndex);
        } else {
            if (getService() != null) {
                getService().disconnect();
            }
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSectionsPagerAdapter != null) {
            outState.putBoolean("tabs_expanded", mSectionsPagerAdapter.isExpanded);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SETTINGS) {
            return;
        }
        if ((requestCode == REQUEST_SELECT_FILE) && (resultCode == RESULT_OK)) {
            Uri uri = data.getData();
            String path = AbsolutePathHelper.getRealPath(this.getBaseContext(), uri);
            if (path != null) {
                File localFile = new File(path);
                if (localFile.canRead()) {
                    startFileUpload(path);
                } else {
                    new FileCopyingTask().execute(uri);
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
            int columnIndex = ((cursor != null) && cursor.moveToFirst())
                    ? cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    : -1;
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
                } else if (inputType == MIC_INPUT_EXTERNAL && (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES)) {
                    targetDevice = device;
                    break;
                }
            }
            if (targetDevice != null) {
                audioManager.setCommunicationDevice(targetDevice);
            }
        } else {
            // Fallback for older Android versions
            // This is a "hack" as speakerphone primarily controls output but often effects
            // input routing
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

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter implements ViewPager.OnPageChangeListener {

        public static final int FILES_PAGE = 0,
                CHANNELS_PAGE = 1,
                MEDIA_PAGE = 2,
                MANAGEMENT_PAGE = 3,
                GLOBAL_PAGE = 4,
                EVENT_HISTORY_PAGE = 5,
                CHAT_PAGE = 6,
                SETTINGS_PAGE = 7,
                PRIVATE_PAGE = 8,
                CONNECTION_PAGE = 9,
                ONLINE_USERS_PAGE = 10,
                MANAGEMENT_STATUS_PAGE = 11,

                MORE_PAGE = 100,
                LESS_PAGE = 101,

                PAGE_COUNT = 12;

        private class PageItem {
            String title;
            int id;

            PageItem(String t, int i) {
                title = t;
                id = i;
            }
        }

        public static class DummyFragment extends Fragment {
            @Override
            public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                return new View(container.getContext());
            }
        }

        private final ArrayList<PageItem> allPages = new ArrayList<>();
        private final ArrayList<Integer> pageOrder = new ArrayList<>();
        private boolean isExpanded = false;
        private int lastActivePageId = CHANNELS_PAGE;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

            Locale l = Locale.getDefault();

            allPages.add(new PageItem(getString(R.string.title_section_files).toUpperCase(l), FILES_PAGE));
            allPages.add(new PageItem(getString(R.string.title_section_channels).toUpperCase(l), CHANNELS_PAGE));
            allPages.add(new PageItem(getString(R.string.title_section_media).toUpperCase(l), MEDIA_PAGE));
            allPages.add(new PageItem(getString(R.string.title_section_management).toUpperCase(l), MANAGEMENT_PAGE));
            allPages.add(new PageItem(getString(R.string.title_section_chat).toUpperCase(l), GLOBAL_PAGE));
            allPages.add(
                    new PageItem(getString(R.string.title_section_event_history).toUpperCase(l), EVENT_HISTORY_PAGE));
            allPages.add(new PageItem(getString(R.string.title_section_channel_chat).toUpperCase(l), CHAT_PAGE));
            allPages.add(new PageItem(getString(R.string.title_section_settings).toUpperCase(l), SETTINGS_PAGE));
            allPages.add(new PageItem(getString(R.string.title_section_private).toUpperCase(l), PRIVATE_PAGE));
            allPages.add(new PageItem(getString(R.string.title_section_connection).toUpperCase(l), CONNECTION_PAGE));
            allPages.add(
                    new PageItem(getString(R.string.title_section_online_users).toUpperCase(l), ONLINE_USERS_PAGE));
            allPages.add(new PageItem(getString(R.string.title_section_status).toUpperCase(l), MANAGEMENT_STATUS_PAGE));

            updateTabs(false);
        }

        public void updateTabs(boolean expanded) {
            this.isExpanded = expanded;
            pageOrder.clear();

            List<PageItem> primaryPages = new ArrayList<>();
            List<PageItem> secondaryPages = new ArrayList<>();

            for (PageItem p : allPages) {
                if (p.id == CHANNELS_PAGE || p.id == MEDIA_PAGE || p.id == GLOBAL_PAGE || p.id == CHAT_PAGE || p.id == PRIVATE_PAGE) {
                    primaryPages.add(p);
                } else {
                    secondaryPages.add(p);
                }
            }

            Collections.sort(primaryPages, (p1, p2) -> p1.title.compareTo(p2.title));
            Collections.sort(secondaryPages, (p1, p2) -> p1.title.compareTo(p2.title));

            for (PageItem p : primaryPages) {
                pageOrder.add(p.id);
            }

            if (expanded) {
                pageOrder.add(LESS_PAGE);
                for (PageItem p : secondaryPages) {
                    pageOrder.add(p.id);
                }
            } else {
                pageOrder.add(MORE_PAGE);
            }
            notifyDataSetChanged();
            updateSwipeRestrictions();
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        public int getPositionForId(int id) {
            return pageOrder.indexOf(id);
        }

        public int getIdForPosition(int pos) {
            if (pos >= 0 && pos < pageOrder.size())
                return pageOrder.get(pos);
            return -1;
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            int id = getIdForPosition(position);

            switch (id) {
                default:
                case CHANNELS_PAGE:
                    return new ChannelsSectionFragment();
                case CHAT_PAGE:
                    return new ChatSectionFragment();
                case GLOBAL_PAGE:
                    return new GlobalSectionFragment();
                case EVENT_HISTORY_PAGE:
                    return new EventHistorySectionFragment();
                case PRIVATE_PAGE:
                    return new PrivateSectionFragment();
                case MEDIA_PAGE:
                    return new MediaSectionFragment();
                case FILES_PAGE:
                    return new FilesSectionFragment();
                case ONLINE_USERS_PAGE:
                    return new OnlineUsersSectionFragment();
                case MANAGEMENT_PAGE:
                    return new ManagementSectionFragment();
                case SETTINGS_PAGE:
                    return new SettingsSectionFragment();
                case CONNECTION_PAGE:
                    return new ConnectionStatusSectionFragment();
                case MANAGEMENT_STATUS_PAGE:
                    return new ManageStatusFragment();
                case MORE_PAGE:
                case LESS_PAGE:
                    return new DummyFragment(); // Dummy fragment
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

            if (id == MORE_PAGE)
                return getString(R.string.tab_more).toUpperCase(l);
            if (id == LESS_PAGE)
                return getString(R.string.tab_less).toUpperCase(l);

            for (PageItem p : allPages) {
                if (p.id == id)
                    return p.title;
            }
            return null;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            int id = getIdForPosition(position);

            if (id == MORE_PAGE) {
                mViewPager.post(() -> {
                    updateTabs(true);
                    int targetPos = getPositionForId(lastActivePageId);
                    if (targetPos != -1) {
                        mViewPager.setCurrentItem(targetPos, false);
                    } else {
                        mViewPager.setCurrentItem(getPositionForId(CHANNELS_PAGE), false);
                    }
                });
                return;
            } else if (id == LESS_PAGE) {
                mViewPager.post(() -> {
                    updateTabs(false);
                    int targetPos = getPositionForId(lastActivePageId);
                    if (targetPos != -1) {
                        mViewPager.setCurrentItem(targetPos, false);
                    } else {
                        mViewPager.setCurrentItem(getPositionForId(CHANNELS_PAGE), false);
                    }
                });
                return;
            }

            lastActivePageId = id;

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

        private void updateSwipeRestrictions() {
            java.util.Set<Integer> restricted = new java.util.HashSet<>();
            int morePos = getPositionForId(MORE_PAGE);
            if (morePos != -1) restricted.add(morePos);
            int lessPos = getPositionForId(LESS_PAGE);
            if (lessPos != -1) restricted.add(lessPos);
            mViewPager.setRestrictedPositions(restricted);
        }
    }

    private void updateSwipeRestrictions() {
        if (mSectionsPagerAdapter != null) {
            mSectionsPagerAdapter.updateSwipeRestrictions();
        }
    }

    private void fileSelectionStart() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        Intent i = Intent.createChooser(intent, getString(R.string.chooser_file));
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
        startActivityForResult(intent.putExtra(ChannelPropActivity.EXTRA_CHANNELID, channel.nChannelID),
                REQUEST_EDITCHANNEL);
    }

    private void leaveChannel() {
        getClient().doLeaveChannel();
        channelsAdapter.notifyDataSetChanged();
    }

    private void joinChannelUnsafe(Channel channel, String passwd) {
        int cmdid = getClient().doJoinChannelByID(channel.nChannelID, passwd);
        if (cmdid > 0) {
            activecmds.put(cmdid, CmdComplete.CMD_COMPLETE_JOIN);
            channel.szPassword = passwd;
            getService().setJoinChannel(channel);
        } else {
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
        if (channel.bPassword) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(R.string.pref_title_join_channel);
            alert.setMessage(R.string.channel_password_prompt);
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
            input.setText(channel.szPassword);
            input.requestFocus();
            alert.setView(input);
            alert.setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(input.getWindowToken(), 0);
                joinChannel(channel, input.getText().toString());
            });
            alert.setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> {
                InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(input.getWindowToken(), 0);
            });
            final AlertDialog dialog = alert.create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dialog.show();
        } else {
            joinChannel(channel, "");
        }
    }

    private void setCurrentChannel(Channel channel) {
        curchannel = channel;
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            if (channel == null) {
                ab.setTitle(R.string.root_server);
            } else {
                boolean isInitChan = (channel.nChannelID == 1) ||
                                     (channel.nChannelID == getClient().getRootChannelID());

                ab.setTitle(isInitChan ? getString(R.string.init_channel) : (channel.szName.isEmpty() ? getString(R.string.no_name) : channel.szName));
            }
            ab.setSubtitle(null);
        }
        invalidateOptionsMenu();
    }


    private void setMyChannel(Channel channel) {
        mychannel = channel;

        adjustVoiceGain();
        invalidateOptionsMenu();
    }

    private String getChannelNameForTTS(int channelId) {
        if (channelId <= 1 || (getClient() != null && channelId == getClient().getRootChannelID())) {
            return getString(R.string.text_tts_root_channel_name);
        }
        if (getService() != null && getService().getChannels() != null) {
            Channel chan = getService().getChannels().get(channelId);
            if (chan != null) {
                if ("/".equals(chan.szName.trim())) {
                    return getString(R.string.text_tts_root_channel_name);
                }
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
            Utils.ttsSubscriptionChanged(getBaseContext(), olduser, user).ifPresent((text -> speakAndLog(text, MyTextMessage.MSGTYPE_LOG_INFO)));
        }

        if (olduser != null) {
            Utils.subscriptionChanged(olduser, user, Subscription.SUBSCRIBE_INTERCEPT_USER_MSG)
                    .ifPresent(isOn -> playSound(isOn ? SOUND_INTERCEPTON : SOUND_INTERCEPTOFF));
            Utils.subscriptionChanged(olduser, user, Subscription.SUBSCRIBE_INTERCEPT_CHANNEL_MSG)
                    .ifPresent(isOn -> playSound(isOn ? SOUND_INTERCEPTON : SOUND_INTERCEPTOFF));
            Utils.subscriptionChanged(olduser, user, Subscription.SUBSCRIBE_INTERCEPT_VOICE)
                    .ifPresent(isOn -> playSound(isOn ? SOUND_INTERCEPTON : SOUND_INTERCEPTOFF));
            Utils.subscriptionChanged(olduser, user, Subscription.SUBSCRIBE_INTERCEPT_VIDEOCAPTURE)
                    .ifPresent(isOn -> playSound(isOn ? SOUND_INTERCEPTON : SOUND_INTERCEPTOFF));
            Utils.subscriptionChanged(olduser, user, Subscription.SUBSCRIBE_INTERCEPT_DESKTOP)
                    .ifPresent(isOn -> playSound(isOn ? SOUND_INTERCEPTON : SOUND_INTERCEPTOFF));
            Utils.subscriptionChanged(olduser, user, Subscription.SUBSCRIBE_INTERCEPT_MEDIAFILE)
                    .ifPresent(isOn -> playSound(isOn ? SOUND_INTERCEPTON : SOUND_INTERCEPTOFF));
        }
    }

    private boolean isVisibleChannel(int chanid) {
        if (curchannel == null) {
            return chanid == 0 || chanid == getClient().getRootChannelID();
        }
        if (curchannel.nChannelID == chanid)
            return true;
        // Check if chanid is an ancestor of curchannel (i.e. curchannel is inside
        // chanid)
        int id = curchannel.nChannelID;
        while (id != 0) {
            Channel c = getService().getChannels().get(id);
            if (c == null)
                break;
            if (c.nParentID == chanid)
                return true;
            id = c.nParentID;
        }
        // Check if chanid is a descendant of curchannel (chanid is inside curchannel)
        Channel channel = getService().getChannels().get(chanid);
        if (channel == null)
            return false;
        int parentId = channel.nParentID;
        while (parentId != 0) {
            if (parentId == curchannel.nChannelID)
                return true;
            Channel parent = getService().getChannels().get(parentId);
            if (parent == null)
                break;
            parentId = parent.nParentID;
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
            mainActivity.listChannels = channelsList;
            mainActivity.accessibilityAssistant.registerPage(rootView, SectionsPagerAdapter.CHANNELS_PAGE);
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
                MainActivity.playChannelMessageSentSound();
                newmsg.setText("");
            } else {
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

            mainActivity.mediaListView = rootView.findViewById(R.id.media_elist_view);
            mainActivity.mediaListView.setAdapter(mainActivity.getMediaAdapter());
            return rootView;
        }
    }

    public static class FilesSectionFragment extends ListFragment {

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity == null)
                return;
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
                if (mainActivity.prefs.get("broadcast_message_audio_icon", true))
                    mainActivity.playSound(mainActivity.SOUND_BCASTMSG);

                if (mainActivity.ttsWrapper != null && mainActivity.prefs.get("broadcast_message_checkbox", true)) {
                    UserAccount myaccount = new UserAccount();
                    mainActivity.getClient().getMyUserAccount(myaccount);
                    User me = mainActivity.getService().getUsers().get(mainActivity.getClient().getMyUserID());
                    String name = (me != null) ? Utils.getDisplayName(mainActivity, me) : myaccount.szUsername;
                    mainActivity.ttsWrapper
                            .speak(mainActivity.getString(R.string.text_tts_broadcast_message, name, msg));
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
            if (editMsg != null)
                editMsg.setVisibility(View.GONE);
            if (sendBtn != null)
                sendBtn.setVisibility(View.GONE);

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
            if (mainActivity == null)
                return rootView;
            mainActivity.accessibilityAssistant.registerPage(rootView, SectionsPagerAdapter.ONLINE_USERS_PAGE);

            ListView userList = rootView.findViewById(R.id.server_users_listview);

            if (mainActivity.getService() != null && mainActivity.getService().getUsers() != null) {
                ArrayList<dk.bearware.User> userListArray = new ArrayList<>(
                        mainActivity.getService().getUsers().values());
                mainActivity.onlineUsersAdapter = new dk.bearware.gui.OnlineUsersAdapter(mainActivity,
                        mainActivity.getService(), userListArray, mainActivity.accessibilityAssistant);
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
            if (mainActivity == null)
                return rootView;
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
            if (mainActivity == null || mainActivity.getClient() == null)
                return;

            if ((mainActivity.getClient().getFlags() & ClientFlag.CLIENT_CONNECTED) == 0) {
                handler.removeCallbacks(checkPermissionsRunnable);
                handler.postDelayed(checkPermissionsRunnable, 1000);
                return;
            }

            UserAccount myAccount = new UserAccount();
            if (mainActivity.getClient().getMyUserAccount(myAccount)) {

                if ((myAccount.uUserType & UserType.USERTYPE_ADMIN) == UserType.USERTYPE_ADMIN) {
                    accountsBtn.setVisibility(View.VISIBLE);
                    serverStatsBtn.setVisibility(View.VISIBLE);
                } else {
                    accountsBtn.setVisibility(View.GONE);
                    serverStatsBtn.setVisibility(View.GONE);
                }

                propsBtn.setVisibility(View.VISIBLE);

                if ((myAccount.uUserRights & UserRight.USERRIGHT_BAN_USERS) != 0) {
                    serverBansBtn.setVisibility(View.VISIBLE);
                } else {
                    serverBansBtn.setVisibility(View.GONE);
                }

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
                R.string.pref_header_display,
                R.string.pref_title_audio_icons,
                R.string.pref_header_tts,
                R.string.pref_header_serverlist,
                R.string.pref_header_connection,
                R.string.pref_header_soundsystem,
                R.string.pref_cat_about
        };

        private static final String[] FRAGMENTS = {
                dk.bearware.gui.PreferencesActivity.GeneralPreferenceFragment.class.getName(),
                dk.bearware.gui.PreferencesActivity.DisplayPreferenceFragment.class.getName(),
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
                    items);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener((parent, view, position, id) -> {
                if (position >= 0 && position < FRAGMENTS.length) {
                    Intent intent = new Intent(mainActivity, dk.bearware.gui.PreferencesActivity.class);
                    intent.putExtra(android.preference.PreferenceActivity.EXTRA_SHOW_FRAGMENT, FRAGMENTS[position]);
                    intent.putExtra(android.preference.PreferenceActivity.EXTRA_NO_HEADERS, true);
                    mainActivity.startActivityForResult(intent, mainActivity.REQUEST_SETTINGS);
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

        public ConnectionStatusSectionFragment() {
        }

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
                        if (mainActivity == null || mainActivity.isFinishing())
                            return;
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
            if (mainActivity.accessibilityAssistant.isUiUpdateDiscouraged())
                return;

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

            connection.setText(mainActivity.getString(R.string.fmt_label_value, mainActivity.getString(R.string.label_connection), con));
            connection.setTextColor(con_color);

            ClientStatistics stats = new ClientStatistics();
            if (!mainActivity.getClient().getClientStatistics(stats))
                return;

            if (prev_stats == null)
                prev_stats = stats;

            long totalrx = stats.nUdpBytesRecv - prev_stats.nUdpBytesRecv;
            long totaltx = stats.nUdpBytesSent - prev_stats.nUdpBytesSent;

            String str;
            if (stats.nUdpPingTimeMs >= 0) {
                str = String.format(Locale.ROOT, "%1$d", stats.nUdpPingTimeMs);
                ping.setText(mainActivity.getString(R.string.fmt_label_value, mainActivity.getString(R.string.label_ping), str));
                if (stats.nUdpPingTimeMs > 250)
                    ping.setTextColor(Color.RED);
                else
                    ping.setTextColor(connection.getTextColors().getDefaultColor());
            }

            str = getString(R.string.fmt_kb_rxtx, totalrx / 1024, totaltx / 1024);
            total.setText(mainActivity.getString(R.string.fmt_label_value, mainActivity.getString(R.string.label_rxtx), str));

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

            if (curchannel != null) {
                int chanid = curchannel.nChannelID;

                subchannels.addAll(Utils.getSubChannels(chanid, getService().getChannels()));
                stickychannels.addAll(Utils.getStickyChannels(chanid, getService().getChannels()));
                currentusers.addAll(Utils.getUsers(chanid, getService().getUsers()));
            } else {
                // Server Root view (ID 0)
                int rootChanId = getClient().getRootChannelID();
                // Only show users who are not in any channel (channel ID 0)
                currentusers.addAll(Utils.getUsers(0, getService().getUsers()));
                Channel rootObj = getService().getChannels().get(rootChanId);
                if (rootObj != null) subchannels.add(rootObj);
            }

            String sortOrder = prefs.get("pref_channel_sort", "0");
            Comparator<Channel> comparator;

            if ("2".equals(sortOrder)) { // Popularity
                comparator = (c1, c2) -> {
                    int count1 = 0;
                    int count2 = 0;
                    for (User u : getService().getUsers().values()) {
                        if (u.nChannelID == c1.nChannelID)
                            count1++;
                        if (u.nChannelID == c2.nChannelID)
                            count2++;
                    }
                    if (count1 != count2) {
                        return count2 - count1; // Descending
                    }
                    return c1.szName.compareToIgnoreCase(c2.szName); // Secondary sort A-Z
                };
            } else if ("1".equals(sortOrder)) { // Z-A
                comparator = (c1, c2) -> c2.szName.compareToIgnoreCase(c1.szName);
            } else { // Default A-Z
                comparator = (c1, c2) -> c1.szName.compareToIgnoreCase(c2.szName);
            }

            Collections.sort(subchannels, comparator);

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
            if (curchannel == null) {
                if (position < currentusers.size())
                    return currentusers.get(position);
                position -= currentusers.size();

                if (position < subchannels.size())
                    return subchannels.get(position);
                position -= subchannels.size();

                return stickychannels.get(position);
            }

            if (position < stickychannels.size()) {
                return stickychannels.get(position);
            }
            position -= stickychannels.size();

            if (position < currentusers.size()) {
                return currentusers.get(position);
            }
            position -= currentusers.size();

            if (position == 0) {
                if (curchannel.nParentID > 0) {
                    Channel parent = getService().getChannels().get(curchannel.nParentID);
                    if (parent != null)
                        return parent;
                    return new Channel();
                } else {
                    return new Channel(); // Signals Root Parent
                }
            }
            position--;

            return subchannels.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            if (curchannel == null) {
                if (position < currentusers.size())
                    return USER_VIEW_TYPE;
                position -= currentusers.size();

                if (position < subchannels.size())
                    return CHANNEL_VIEW_TYPE;
                position -= subchannels.size();

                return INFO_VIEW_TYPE;
            }

            if (position < stickychannels.size())
                return INFO_VIEW_TYPE;
            position -= stickychannels.size();

            if (position < currentusers.size())
                return USER_VIEW_TYPE;
            position -= currentusers.size();

            if (position == 0)
                return PARENT_CHANNEL_VIEW_TYPE;
            position--;

            return CHANNEL_VIEW_TYPE;
        }

        @Override
        public int getViewTypeCount() {
            return VIEW_TYPE_COUNT;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            Object item = getItem(position);

            if (item instanceof Channel channel) {

                switch (getItemViewType(position)) {
                    case PARENT_CHANNEL_VIEW_TYPE:

                        if (convertView == null ||
                                convertView.findViewById(R.id.parentname) == null)
                            convertView = inflater.inflate(R.layout.item_channel_back, parent, false);

                        TextView parentname = convertView.findViewById(R.id.parentname);
                        TextView parentTopic = convertView.findViewById(R.id.chantopic);
                        TextView parentPop = convertView.findViewById(R.id.population);

                        if (curchannel != null) {
                            if (curchannel.nParentID == 0) {
                                // Root level parent (back to Server Root/Root Server)
                                parentname.setText(R.string.root_server);
                            } else {
                                // Nested subchannel level parent
                                Channel parentChan = getService().getChannels().get(curchannel.nParentID);
                                if (parentChan != null) {
                                    boolean isParentInitChan = (parentChan.nChannelID == 1) ||
                                                               (parentChan.nChannelID == getClient().getRootChannelID()) ||
                                                               (!myUserAccount.szInitChannel.isEmpty() && 
                                                                parentChan.szName.equals(Utils.getChannelNameFromPath(myUserAccount.szInitChannel)));
                                    if (isParentInitChan) {
                                        parentname.setText(R.string.init_channel);
                                    } else {
                                        parentname.setText(parentChan.szName.isEmpty() ? getString(R.string.no_name) : parentChan.szName);
                                    }
                                } else {
                                    parentname.setText(R.string.root_server);
                                }
                            }
                        }
                        if (parentTopic != null) parentTopic.setText("");
                        if (parentPop != null) {
                            if (curchannel != null) {
                                if (curchannel.nParentID > 0) {
                                    Channel parentChan = getService().getChannels().get(curchannel.nParentID);
                                    if (parentChan != null) {
                                        int population = Utils.getUsers(parentChan.nChannelID, getService().getUsers()).size();
                                        int populationSub = Utils.getUsers(parentChan.nChannelID, getService().getUsers(), true, getService().getChannels()).size();
                                        int subPopOnly = populationSub - population;
                                        if (Utils.hasSubChannels(parentChan.nChannelID, getService().getChannels())) {
                                            parentPop.setText(String.format(Locale.ROOT, "(%d/%d)", population, subPopOnly));
                                        } else {
                                            parentPop.setText(String.format(Locale.ROOT, "(%d)", population));
                                        }
                                    } else {
                                        parentPop.setText("");
                                    }
                                } else {
                                    // Root level parent (Server Root) population display:
                                    // Show users who are not in any channel (Channel ID 0)
                                    int chan0Pop = Utils.getUsers(0, getService().getUsers()).size();
                                    parentPop.setText(String.format(Locale.ROOT, "(%d)", chan0Pop));
                                }
                            } else {
                                parentPop.setText("");
                            }
                        }
                        break;

                    case CHANNEL_VIEW_TYPE:
                        if (convertView == null ||
                                convertView.findViewById(R.id.channelname) == null)
                            convertView = inflater.inflate(R.layout.item_channel, parent, false);

                        ImageView chanicon = convertView.findViewById(R.id.channelicon);
                        TextView name = convertView.findViewById(R.id.channelname);
                        TextView topic = convertView.findViewById(R.id.chantopic);
                        Button join = convertView.findViewById(R.id.join_btn);
                        int icon_resource = R.drawable.channel_orange;
                        if (channel.bPassword) {
                            icon_resource = R.drawable.channel_pink;
                            chanicon.setContentDescription(getString(R.string.text_passwdprot));
                            chanicon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                        } else {
                            chanicon.setContentDescription(null);
                            chanicon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                        }
                        chanicon.setImageResource(icon_resource);

                        if (channel.nParentID == 0 && channel.nChannelID == getClient().getRootChannelID()) {
                            name.setText(R.string.init_channel);
                        } else {
                            if (channel.szName.trim().isEmpty())
                                name.setText(R.string.no_name);
                            else
                                name.setText(channel.szName);
                        }
                        topic.setText(channel.szTopic);

                        final boolean isMyChannel = channel.nChannelID == getClient().getMyChannelID();
                        if (isMyChannel) {
                            join.setText((channel.nParentID == 0) ? R.string.action_stay_server : R.string.action_leave);
                        } else {
                            join.setText(R.string.action_join);
                        }

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

                        int population = Utils.getUsers(channel.nChannelID, getService().getUsers()).size();
                        int populationSub = Utils.getUsers(channel.nChannelID, getService().getUsers(), true, getService().getChannels()).size();
                        int subPopOnly = populationSub - population;
                        TextView popView = convertView.findViewById(R.id.population);
                        if (Utils.hasSubChannels(channel.nChannelID, getService().getChannels())) {
                            popView.setText(String.format(Locale.ROOT, "(%d/%d)", population, subPopOnly));
                        } else {
                            popView.setText(String.format(Locale.ROOT, "(%d)", population));
                        }

                        convertView.setTag(channel);
                        ViewCompat.setAccessibilityDelegate(convertView, accessibilityAssistant);
                        break;

                    case INFO_VIEW_TYPE:
                        if (convertView == null ||
                                convertView.findViewById(R.id.titletext) == null)
                            convertView = inflater.inflate(R.layout.item_info, parent, false);
                        TextView title = convertView.findViewById(R.id.titletext);
                        TextView details = convertView.findViewById(R.id.infodetails);
                        title.setText(channel.szName);
                        details.setText(channel.szTopic);
                        break;
                }
            } else if (item instanceof User user) {
                if (convertView == null ||
                        convertView.findViewById(R.id.nickname) == null)
                    convertView = inflater.inflate(R.layout.item_user, parent, false);
                ImageView usericon = convertView.findViewById(R.id.usericon);
                TextView nickname = convertView.findViewById(R.id.nickname);
                TextView status = convertView.findViewById(R.id.status);
                String name = Utils.getDisplayName(getBaseContext(), user);
                boolean isAdmin = (user.uUserType & UserType.USERTYPE_ADMIN) != 0;
                if (!isAdmin && user.nUserID == getClient().getMyUserID()) {
                    UserAccount myAcc = new UserAccount();
                    if (getClient().getMyUserAccount(myAcc)) {
                        isAdmin = (myAcc.uUserType & UserType.USERTYPE_ADMIN) != 0;
                    }
                }
                boolean isOperator = getClient().isChannelOperator(user.nUserID, user.nChannelID);
                
                if (name.trim().isEmpty())
                    name = getString(R.string.no_name);

                boolean selected = userIDS.contains(user.nUserID);
                boolean talking = (user.uUserState & UserState.USERSTATE_VOICE) != 0;
                boolean female = (user.nStatusMode & TeamTalkConstants.STATUSMODE_FEMALE) != 0;
                boolean neutral = (user.nStatusMode & TeamTalkConstants.STATUSMODE_NEUTRAL) != 0;
                boolean male = !female && !neutral;
                boolean away = (user.nStatusMode & TeamTalkConstants.STATUSMODE_AWAY) != 0;
                int icon_resource;

                if (user.nUserID == getService().getTTInstance().getMyUserID()) {
                    talking = getService().isVoiceTransmitting();
                }

                String move = selected ? getString(R.string.user_state_selected) : "";
                String genderText = female ? getString(R.string.gender_female)
                        : neutral ? getString(R.string.gender_neutral) : getString(R.string.gender_male);
                
                boolean isMedia = (user.nStatusMode & TeamTalkConstants.STATUSMODE_STREAM_MEDIAFILE) != 0 ||
                        (user.nStatusMode & TeamTalkConstants.STATUSMODE_VIDEOTX) != 0 ||
                        (user.nStatusMode & TeamTalkConstants.STATUSMODE_DESKTOP) != 0;

                String statusText;
                if ((user.nStatusMode & TeamTalkConstants.STATUSMODE_AWAY) != 0) {
                    statusText = getString(R.string.status_away);
                } else if ((user.nStatusMode & TeamTalkConstants.STATUSMODE_QUESTION) != 0) {
                    statusText = getString(R.string.status_question);
                } else {
                    statusText = getString(R.string.status_online);
                }

                StringBuilder userDisplay = new StringBuilder(name);
                if (user.szStatusMsg != null && !user.szStatusMsg.isEmpty()) {
                    userDisplay.append(" - ").append(user.szStatusMsg).append(", ");
                } else {
                    userDisplay.append(", ");
                }
                userDisplay.append(statusText);
                
                if (isMedia) {
                    userDisplay.append(", ").append(getString(R.string.state_transmitting_media));
                }
                
                String adminLabel = isAdmin ? " (" + getString(R.string.usertype_admin) + ")" : "";
                String opLabel = isOperator ? " (" + getString(R.string.usertype_operator) + ")" : "";
                
                String baseInfo = userDisplay.toString();
                String finalText = baseInfo + ", " + genderText + adminLabel + opLabel;
                
                nickname.setText(finalText);
                status.setVisibility(View.GONE);

                String accOp = (isAdmin ? "(" + getString(R.string.usertype_admin) + ")" : "") + 
                               (isAdmin && isOperator ? " " : "") + 
                               (isOperator ? "(" + getString(R.string.usertype_operator) + ")" : "");
                String speaking = talking ? getString(R.string.user_state_now_speaking, baseInfo) : baseInfo;
                nickname.setContentDescription(
                        getString(R.string.user_accessibility_desc, move, speaking, genderText, accOp));

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

                status.setContentDescription(away
                        ? getString(R.string.user_status_fmt, getString(R.string.user_state_away), user.szStatusMsg)
                        : null);

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

                convertView.setTag(user);
                ViewCompat.setAccessibilityDelegate(convertView, accessibilityAssistant);
                return convertView;
            }

            return convertView;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {

        Object item = channelsAdapter.getItem(position);
        if (item instanceof User user) {
            Intent intent = new Intent(this, UserPropActivity.class);
            // TODO: check 'curchannel' for null
            startActivityForResult(intent.putExtra(UserPropActivity.EXTRA_USERID, user.nUserID),
                    REQUEST_EDITUSER);
        } else if (item instanceof Channel channel) {
            setCurrentChannel((channel.nChannelID > 0) ? channel : null);
            channelsAdapter.notifyDataSetChanged();
        }
    }

    Channel selectedChannel;
    private void setSelectedChannel(Channel channel) {
        selectedChannel = channel;
        if (getService() != null && channel != null) {
            getService().setMarkedChannelID(channel.nChannelID);
        }
    }
    User selectedUser;
    List<Integer> userIDS = new ArrayList<>();

    public boolean handleUserAction(int resId, User selectedUser) {
        if (selectedUser == null)
            return false;
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        if (resId == R.string.info_copy_name) {
            Utils.copyToClipboard(this, getString(R.string.label_user_name), selectedUser.szNickname);
            return true;
        } else if (resId == R.string.info_copy_id) {
            Utils.copyToClipboard(this, getString(R.string.label_user_id), String.valueOf(selectedUser.nUserID));
            return true;
        } else if (resId == R.string.info_copy_ip) {
            Utils.copyToClipboard(this, getString(R.string.label_user_ip), selectedUser.szIPAddress);
            return true;
        } else if (resId == R.string.action_edit_user) {
            Intent intent = new Intent(MainActivity.this, UserPropActivity.class);
            startActivityForResult(intent.putExtra(UserPropActivity.EXTRA_USERID, selectedUser.nUserID),
                    REQUEST_EDITUSER);
            return true;
        } else if (resId == R.string.button_msg) {
            Intent intent = new Intent(MainActivity.this, TextMessageActivity.class);
            startActivity(intent.putExtra(TextMessageActivity.EXTRA_USERID, selectedUser.nUserID));
            return true;
        } else if (resId == R.string.action_make_operator || resId == R.string.action_revoke_operator) {
            UserAccount myuseraccount = new UserAccount();
            getClient().getMyUserAccount(myuseraccount);
            boolean isOp = getClient().isChannelOperator(selectedUser.nUserID, selectedUser.nChannelID);
            boolean operatorRight = getClient().isChannelOperator(getClient().getMyUserID(), selectedUser.nChannelID);
            if (((myuseraccount.uUserType & UserType.USERTYPE_ADMIN) == UserType.USERTYPE_ADMIN)
                    || ((myuseraccount.uUserRights
                            & UserRight.USERRIGHT_OPERATOR_ENABLE) != UserRight.USERRIGHT_NONE)
                    || operatorRight) {
                getClient().doChannelOp(selectedUser.nUserID, selectedUser.nChannelID, !isOp);
                return true;
            } else {
                alert.setTitle(!isOp ? R.string.action_make_operator : R.string.action_revoke_operator);
                alert.setMessage(R.string.text_operator_password);
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
                alert.setPositiveButton(android.R.string.yes,
                        ((dialog, whichButton) -> getClient().doChannelOpEx(selectedUser.nUserID,
                                selectedUser.nChannelID, input.getText().toString(), !isOp)));
                alert.setNegativeButton(android.R.string.no, null);
                alert.setView(input);
                alert.show();
                return true;
            }
        } else if (resId == R.string.action_kickchan) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(getString(R.string.kick_confirmation, selectedUser.szNickname))
                    .setPositiveButton(android.R.string.yes,
                            (dialog, which) -> getClient().doKickUser(selectedUser.nUserID, selectedUser.nChannelID))
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            return true;
        } else if (resId == R.string.action_banchan) {
            showBanDialog(selectedUser, true);
            return true;
        } else if (resId == R.string.action_kicksrv) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(getString(R.string.kick_confirmation, selectedUser.szNickname))
                    .setPositiveButton(android.R.string.yes,
                            (dialog, which) -> getClient().doKickUser(selectedUser.nUserID, 0))
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            return true;
        } else if (resId == R.string.action_bansrv) {
            showBanDialog(selectedUser, false);
            return true;
        } else if (resId == R.string.action_transmission_control) {
            showTransmissionControlDialog(selectedUser);
            return true;
        } else if (resId == R.string.action_subscriptions) {
            showSubscriptionsDialog(selectedUser);
            return true;
        }
        return false;
    }

    private void showBanDialog(final User user, final boolean fromChannel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.ban_confirmation_title);

        View view = getLayoutInflater().inflate(R.layout.dialog_ban_user, null);
        TextView textMsg = view.findViewById(R.id.text_ban_confirmation);
        Spinner spinner = view.findViewById(R.id.spinner_ban_type);

        textMsg.setText(getString(R.string.ban_confirmation, user.szNickname));

        String[] options = {
                getString(R.string.action_ban_type_account),
                getString(R.string.action_ban_type_ip),
                getString(R.string.action_ban_type_both)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        builder.setView(view);
        builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
            int selected = spinner.getSelectedItemPosition();
            int channelBit = fromChannel ? BanType.BANTYPE_CHANNEL : 0;
            
            if (selected == 2) {
                // Ban by both: send two separate commands to ensure compatibility
                getClient().doBanUserEx(user.nUserID, BanType.BANTYPE_USERNAME | channelBit);
                getClient().doBanUserEx(user.nUserID, BanType.BANTYPE_IPADDR | channelBit);
            } else {
                int banTypes = (selected == 0) ? BanType.BANTYPE_USERNAME : BanType.BANTYPE_IPADDR;
                getClient().doBanUserEx(user.nUserID, banTypes | channelBit);
            }
        });
        builder.setNegativeButton(android.R.string.no, null);
        builder.show();
    }

    public boolean handleChannelAction(int resId, Channel channel) {
        if (channel == null)
            return false;
        if (resId == R.string.action_new_channel) {
            Intent intent = new Intent(MainActivity.this, dk.bearware.gui.ChannelPropActivity.class);
            intent.putExtra(dk.bearware.gui.ChannelPropActivity.EXTRA_PARENTID, channel.nChannelID);
            startActivityForResult(intent, REQUEST_NEWCHANNEL);
            return true;
        } else if (resId == R.string.title_activity_channel_prop) {
            Intent intent = new Intent(MainActivity.this, dk.bearware.gui.ChannelPropActivity.class);
            intent.putExtra(dk.bearware.gui.ChannelPropActivity.EXTRA_CHANNELID, channel.nChannelID);
            startActivity(intent);
            return true;
        } else if (resId == R.string.action_delete) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.dialog_confirm_delete)
                    .setMessage(getString(R.string.channel_remove_confirmation, channel.szName))
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        if (getClient().doRemoveChannel(channel.nChannelID) <= 0)
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.err_channel_remove, channel.szName),
                                    Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            return true;
        } else if (resId == R.string.action_join) {
            joinChannel(channel);
            return true;
        } else if (resId == R.string.action_move) {
            for (Integer uid : userIDS) {
                getClient().doMoveUser(uid, channel.nChannelID);
            }
            userIDS.clear();
            return true;
        } else if (resId == R.string.action_banned_users) {
            Intent intent = new Intent(MainActivity.this, dk.bearware.gui.ChannelBannedUsersActivity.class);
            intent.putExtra("channel_id", channel.nChannelID);
            startActivity(intent);
            return true;
        } else if (resId == R.string.action_share_channel) {
            shareChannel(channel);
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.isAltPressed()) {
            if (listChannels != null) {
                int position = listChannels.getSelectedItemPosition();
                if (position != AdapterView.INVALID_POSITION) {
                    onItemClick(listChannels, listChannels.getSelectedView(), position, listChannels.getItemIdAtPosition(position));
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void shareChannel(Channel channel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_share_channel_title);

        View view = getLayoutInflater().inflate(R.layout.dialog_share_channel, null);
        final EditText editUser = view.findViewById(R.id.edit_share_username);
        final EditText editPass = view.findViewById(R.id.edit_share_password);
        final CheckBox chkShowPass = view.findViewById(R.id.chk_share_show_password);

        ServerEntry server = getService().getServerEntry();
        if (server != null) {
            editUser.setText(server.username);
            editPass.setText(server.password);
        }

        chkShowPass.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                editPass.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                editPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            editPass.setSelection(editPass.getText().length());
        });

        final TextView textLink = view.findViewById(R.id.text_share_link);
        final Button btnCopy = view.findViewById(R.id.btn_share_copy);
        final String path = Utils.getChannelPath(channel.nChannelID, getService().getChannels());

        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String u = editUser.getText().toString();
                String p = editPass.getText().toString();
                textLink.setText(Utils.generateTTLink(server, u, p, path));
            }
        };
        editUser.addTextChangedListener(watcher);
        editPass.addTextChangedListener(watcher);
        
        // Initial text
        textLink.setText(Utils.generateTTLink(server, server != null ? server.username : "", server != null ? server.password : "", path));

        btnCopy.setOnClickListener(v -> {
            Utils.copyToClipboard(MainActivity.this, getString(R.string.tag_clipboard), textLink.getText().toString());
            Toast.makeText(MainActivity.this, R.string.msg_link_copied, Toast.LENGTH_SHORT).show();
        });

        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        // Disable long-click context menu for ".." (Parent) item
        // This ensures it is strictly for navigation ("só voltar")
        if (channelsAdapter.getItemViewType(position) == ChannelListAdapter.PARENT_CHANNEL_VIEW_TYPE) {
            return true;
        }

        Object item = parent.getItemAtPosition(position);

        if (item instanceof User) {
            final User user = (User) item;
            selectedUser = user;

            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, view);

            boolean isSelected = userIDS.contains(user.nUserID);
            popup.getMenu().add(isSelected ? R.string.action_deselect : R.string.action_select)
                    .setOnMenuItemClickListener(menuItem -> {
                        if (isSelected) {
                            userIDS.remove((Integer) user.nUserID);
                        } else {
                            userIDS.add(user.nUserID);
                        }
                        accessibilityAssistant.lockEvents();
                        channelsAdapter.notifyDataSetChanged();
                        accessibilityAssistant.unlockEvents();
                        return true;
                    });

            popup.getMenu().add(getString(R.string.info_copy_name))
                    .setOnMenuItemClickListener(menuItem -> handleUserAction(R.string.info_copy_name, user));
            popup.getMenu().add(getString(R.string.info_copy_id))
                    .setOnMenuItemClickListener(menuItem -> handleUserAction(R.string.info_copy_id, user));
            popup.getMenu().add(getString(R.string.info_copy_ip))
                    .setOnMenuItemClickListener(menuItem -> handleUserAction(R.string.info_copy_ip, user));
            popup.getMenu().add(R.string.action_edit_user)
                    .setOnMenuItemClickListener(menuItem -> handleUserAction(R.string.action_edit_user, user));
            popup.getMenu().add(R.string.button_msg)
                    .setOnMenuItemClickListener(menuItem -> handleUserAction(R.string.button_msg, user));
            popup.getMenu().add(R.string.action_subscriptions)
                    .setOnMenuItemClickListener(menuItem -> handleUserAction(R.string.action_subscriptions, user));
            popup.getMenu().add(R.string.action_transmission_control)
                    .setOnMenuItemClickListener(menuItem -> handleUserAction(R.string.action_transmission_control, user));

            UserAccount myuseraccount = new UserAccount();
            getClient().getMyUserAccount(myuseraccount);
            boolean kickRight = (myuseraccount.uUserRights
                    & UserRight.USERRIGHT_KICK_USERS) != UserRight.USERRIGHT_NONE;
            boolean banRight = (myuseraccount.uUserRights & UserRight.USERRIGHT_BAN_USERS) != UserRight.USERRIGHT_NONE;
            boolean operatorRight = getClient().isChannelOperator(getClient().getMyUserID(), user.nChannelID);

            if (user.nChannelID != 0 && (((myuseraccount.uUserType & UserType.USERTYPE_ADMIN) == UserType.USERTYPE_ADMIN)
                    || ((myuseraccount.uUserRights
                            & UserRight.USERRIGHT_OPERATOR_ENABLE) != UserRight.USERRIGHT_NONE)
                    || operatorRight)) {
                boolean isOp = getClient().isChannelOperator(user.nUserID, user.nChannelID);
                popup.getMenu().add(isOp ? R.string.action_revoke_operator : R.string.action_make_operator)
                        .setOnMenuItemClickListener(menuItem -> handleUserAction(
                                isOp ? R.string.action_revoke_operator : R.string.action_make_operator, user));
            }

            if (kickRight || operatorRight) {
                popup.getMenu().add(R.string.action_kickchan)
                        .setOnMenuItemClickListener(menuItem -> handleUserAction(R.string.action_kickchan, user));
            }
            if (banRight || operatorRight) {
                popup.getMenu().add(R.string.action_banchan)
                        .setOnMenuItemClickListener(menuItem -> handleUserAction(R.string.action_banchan, user));
            }
            if (kickRight) {
                popup.getMenu().add(R.string.action_kicksrv)
                        .setOnMenuItemClickListener(menuItem -> handleUserAction(R.string.action_kicksrv, user));
            }
            if (banRight) {
                popup.getMenu().add(R.string.action_bansrv)
                        .setOnMenuItemClickListener(menuItem -> handleUserAction(R.string.action_bansrv, user));
            }

            popup.show();
            return true;
        }

        else if (item instanceof Channel) {
            final Channel channel = (Channel) item;
            setSelectedChannel(channel);

            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, view);

            UserAccount myuseraccount = new UserAccount();
            getClient().getMyUserAccount(myuseraccount);
            boolean chanRight = (myuseraccount.uUserRights
                    & UserRight.USERRIGHT_MODIFY_CHANNELS) != UserRight.USERRIGHT_NONE;
            boolean tempChanRight = (myuseraccount.uUserRights
                    & UserRight.USERRIGHT_CREATE_TEMPORARY_CHANNEL) != UserRight.USERRIGHT_NONE;

            if (chanRight || tempChanRight) {

                popup.getMenu().add(R.string.action_new_channel).setOnMenuItemClickListener(
                        menuItem -> handleChannelAction(R.string.action_new_channel, channel));

                popup.getMenu().add(R.string.title_activity_channel_prop).setOnMenuItemClickListener(
                        menuItem -> handleChannelAction(R.string.title_activity_channel_prop, channel));

                popup.getMenu().add(R.string.action_delete)
                        .setOnMenuItemClickListener(menuItem -> handleChannelAction(R.string.action_delete, channel));
            }

            popup.getMenu().add(getString(R.string.action_join))
                    .setOnMenuItemClickListener(menuItem -> handleChannelAction(R.string.action_join, channel));

            getClient().getMyUserAccount(myuseraccount);
            boolean moveRight = (myuseraccount.uUserRights
                    & UserRight.USERRIGHT_MOVE_USERS) != UserRight.USERRIGHT_NONE;
            if (moveRight && !userIDS.isEmpty()) {
                popup.getMenu().add(R.string.action_move)
                        .setOnMenuItemClickListener(menuItem -> handleChannelAction(R.string.action_move, channel));
            }

            // Add Banned Users option
            boolean banRight = (myuseraccount.uUserRights & UserRight.USERRIGHT_BAN_USERS) != UserRight.USERRIGHT_NONE;
            boolean operatorRight = getClient().isChannelOperator(getClient().getMyUserID(), channel.nChannelID);

            if (banRight || operatorRight) {
                popup.getMenu().add(R.string.action_banned_users).setOnMenuItemClickListener(
                        menuItem -> handleChannelAction(R.string.action_banned_users, channel));
            }

            popup.getMenu().add(R.string.action_share_channel).setOnMenuItemClickListener(
                    menuItem -> handleChannelAction(R.string.action_share_channel, channel));

            popup.show();
            return true;
        }

        return false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        if (item.getItemId() == R.id.action_banchan) {
            alert.setMessage(getString(R.string.ban_confirmation, selectedUser.szNickname));
            alert.setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                getClient().doBanUser(selectedUser.nUserID, selectedUser.nChannelID);
                getClient().doKickUser(selectedUser.nUserID, selectedUser.nChannelID);
            });
            alert.setNegativeButton(android.R.string.no, null);
            alert.show();
        } else if (item.getItemId() == R.id.action_bansrv) {
            alert.setMessage(getString(R.string.ban_confirmation, selectedUser.szNickname));
            alert.setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                getClient().doBanUser(selectedUser.nUserID, 0);
                getClient().doKickUser(selectedUser.nUserID, 0);
            });
            alert.setNegativeButton(android.R.string.no, null);
            alert.show();
        } else if (item.getItemId() == R.id.action_edit) {
            editChannelProperties(selectedChannel);
        } else if (item.getItemId() == R.id.action_edituser) {
            {
                Intent intent = new Intent(this, UserPropActivity.class);
                startActivityForResult(intent.putExtra(UserPropActivity.EXTRA_USERID, selectedUser.nUserID),
                        REQUEST_EDITUSER);
            }
        } else if (item.getItemId() == R.id.action_message) {
            {
                Intent intent = new Intent(MainActivity.this, TextMessageActivity.class);
                startActivity(intent.putExtra(TextMessageActivity.EXTRA_USERID, selectedUser.nUserID));
            }
        } else if (item.getItemId() == R.id.action_kickchan) {
            alert.setMessage(getString(R.string.kick_confirmation, selectedUser.szNickname));
            alert.setPositiveButton(android.R.string.yes,
                    (dialog, whichButton) -> getClient().doKickUser(selectedUser.nUserID, selectedUser.nChannelID));
            alert.setNegativeButton(android.R.string.no, null);
            alert.show();
        } else if (item.getItemId() == R.id.action_kicksrv) {
            alert.setMessage(getString(R.string.kick_confirmation, selectedUser.szNickname));
            alert.setPositiveButton(android.R.string.yes,
                    (dialog, whichButton) -> getClient().doKickUser(selectedUser.nUserID, 0));
            alert.setNegativeButton(android.R.string.no, null);
            alert.show();
        } else if (item.getItemId() == R.id.action_makeop) {
            UserAccount myuseraccount = new UserAccount();
            getClient().getMyUserAccount(myuseraccount);
            if (((myuseraccount.uUserType & UserType.USERTYPE_ADMIN) == UserType.USERTYPE_ADMIN)
                    || ((myuseraccount.uUserRights
                            & UserRight.USERRIGHT_OPERATOR_ENABLE) != UserRight.USERRIGHT_NONE)) {
                getClient().doChannelOp(selectedUser.nUserID, selectedUser.nChannelID,
                        !getClient().isChannelOperator(selectedUser.nUserID, selectedUser.nChannelID));
            }
            alert.setTitle(getClient().isChannelOperator(selectedUser.nUserID, selectedUser.nChannelID)
                    ? R.string.action_revoke_operator
                    : R.string.action_make_operator);
            alert.setMessage(R.string.text_operator_password);
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
            alert.setPositiveButton(android.R.string.yes,
                    ((dialog, whichButton) -> getClient().doChannelOpEx(selectedUser.nUserID, selectedUser.nChannelID,
                            input.getText().toString(),
                            !getClient().isChannelOperator(selectedUser.nUserID, selectedUser.nChannelID))));
            alert.setNegativeButton(android.R.string.no, null);
            alert.setView(input);
            alert.show();
        } else if (item.getItemId() == R.id.action_move) {
            for (Integer userID : userIDS) {
                getClient().doMoveUser(userID, selectedChannel.nChannelID);
            }
            userIDS.clear();
        } else if (item.getItemId() == R.id.action_select) {
            if (userIDS.contains(selectedUser.nUserID)) {
                userIDS.remove((Integer) selectedUser.nUserID);
            } else {
                userIDS.add(selectedUser.nUserID);
            }
            accessibilityAssistant.lockEvents();
            channelsAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        } else if (item.getItemId() == R.id.action_remove) {
            {
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
            }
        } else if (item.getItemId() == R.id.action_settings) {
            startActivityForResult(new Intent(this, PreferencesActivity.class), REQUEST_SETTINGS);
            return true;
        } else {
            return false;
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private void adjustSoundSystem() {
        if (getService() != null) {
            getService().updateAudioRouting();
        }
    }

    private void adjustMuteButton(ImageButton btn) {
        // Use service state if available, otherwise check volume
        boolean isMuted = false;
        if (getService() != null) {
            isMuted = getService().isMute();
        } else {
            isMuted = (getClient().getSoundOutputVolume() == 0);
        }

        if (isMuted) {
            btn.setImageResource(R.drawable.mute_blue);
            btn.setContentDescription(getString(R.string.speaker_unmute));
        } else {
            btn.setImageResource(R.drawable.speaker_blue);
            btn.setContentDescription(getString(R.string.speaker_mute));
        }
    }

    private void adjustVoxState(boolean voiceActivationEnabled, int level) {
        ImageButton voxSwitch = findViewById(R.id.voxSwitch);
        TextView micLevel = findViewById(R.id.miclevel_text);

        if (voiceActivationEnabled) {
            micLevel.setText(level + getString(R.string.unit_percent));
            micLevel.setContentDescription(getString(R.string.vox_level_description, micLevel.getText().toString()));
            voxSwitch.setImageResource(R.drawable.microphone);
            voxSwitch.setContentDescription(getString(R.string.voice_activation_off));
            ((SeekBar) findViewById(R.id.mic_gainSeekBar)).setProgress(getClient().getVoiceActivationLevel());
            findViewById(R.id.mic_gainSeekBar).setContentDescription(getString(R.string.voxlevel));
        } else {
            micLevel.setText(Utils.refGainToPercent(level) + getString(R.string.unit_percent));
            micLevel.setContentDescription(getString(R.string.mic_gain_description, micLevel.getText().toString()));
            voxSwitch.setImageResource(R.drawable.mic_green);
            voxSwitch.setContentDescription(getString(R.string.voice_activation_on));
            ((SeekBar) findViewById(R.id.mic_gainSeekBar))
                    .setProgress(Utils.refGainToPercent(getClient().getSoundInputGainLevel()));
            findViewById(R.id.mic_gainSeekBar).setContentDescription(getString(R.string.micgain));
        }
    }

    private void adjustTxState(boolean txEnabled) {
        accessibilityAssistant.lockEvents();

        findViewById(R.id.transmit_voice).setBackgroundColor(txEnabled ? Color.GREEN : Color.RED);
        findViewById(R.id.transmit_voice)
                .setContentDescription(txEnabled ? getString(R.string.tx_on) : getString(R.string.tx_off));

        if ((curchannel != null) && (getClient().getMyChannelID() == curchannel.nChannelID))
            channelsAdapter.notifyDataSetChanged();

        accessibilityAssistant.unlockEvents();
    }

    private void adjustVoiceGain() {

        boolean showMicSeekBar = mychannel == null || !mychannel.audiocfg.bEnableAGC
                || getService().isVoiceActivationEnabled();

        findViewById(R.id.mic_gainSeekBar).setVisibility(showMicSeekBar ? View.VISIBLE : View.GONE);
    }

    private interface OnButtonInteractionListener extends OnTouchListener, OnClickListener {
    }

    private void setupButtons() {

        final Button tx_btn = findViewById(R.id.transmit_voice);
        tx_btn.setOnClickListener(v -> toggleVoiceTransmission());
        ViewCompat.setAccessibilityDelegate(tx_btn, accessibilityAssistant);

        OnButtonInteractionListener txButtonListener = new OnButtonInteractionListener() {

            boolean tx_state = false;
            long tx_down_start = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean tx = event.getAction() != MotionEvent.ACTION_UP;

                if (tx != tx_state) {

                    if (!tx) {
                        if (System.currentTimeMillis() - tx_down_start < 800) {
                            tx = true;
                            tx_down_start = 0;
                        } else {
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
                if (System.currentTimeMillis() - tx_down_start < 800) {
                    tx_state = true;
                    tx_down_start = 0;
                } else {
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
                    volLevel.setText(progress + getString(R.string.unit_percent));
                    volLevel.setContentDescription(
                            getString(R.string.speaker_volume_description, volLevel.getText().toString()));
                } else if (seekBar == micSeekBar) {
                    if (getService().isVoiceActivationEnabled()) {
                        int voxLevel = progress;
                        getClient().setVoiceActivationLevel(voxLevel);
                        prefs.put(Preferences.PREF_SOUNDSYSTEM_VOICEACTIVATION_LEVEL, voxLevel);
                        micLevel.setText(progress + getString(R.string.unit_percent));
                        micLevel.setContentDescription(
                                getString(R.string.vox_level_description, micLevel.getText().toString()));
                    } else {
                        int inputGain = Utils.refGain(progress);
                        getClient().setSoundInputGainLevel(inputGain);
                        prefs.put(Preferences.PREF_SOUNDSYSTEM_MICROPHONEGAIN, inputGain);
                        micLevel.setText(progress + getString(R.string.unit_percent));
                        micLevel.setContentDescription(
                                getString(R.string.mic_gain_description, micLevel.getText().toString()));
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
                if (getService().isMute()) {
                    Log.d(TAG, "Mute enabled");
                    // Play mute sound if enabled
                    if (prefs.get("mute_all_audio_icon", true)) {
                        playSound(SOUND_MUTEALL);
                    }
                } else {
                    Log.d(TAG, "Mute disabled");
                    // Play unmute sound if enabled
                    if (prefs.get("mute_all_audio_icon", true)) {
                        playSound(SOUND_UNMUTEALL);
                    }
                }
                adjustMuteButton((ImageButton) v);

                int level = getService().isMute() ? 0 : Utils.refVolumeToPercent(getClient().getSoundOutputVolume());
                volLevel.setText(level + getString(R.string.unit_percent));
                volLevel.setContentDescription(
                        getString(R.string.speaker_volume_description, volLevel.getText().toString()));
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
        this.ttsWrapper = service.getTTSWrapper();
        this.users = new HashMap<>(service.getUsers());

        int mychanid = getClient().getMyChannelID();
        if (mychanid > 0) {
            Channel mychan = service.getChannels().get(mychanid);
            setSelectedChannel(mychan);
            if (curchannel == null) {
                setCurrentChannel(mychan);
            }
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
        int gain = prefs.get(Preferences.PREF_SOUNDSYSTEM_MICROPHONEGAIN, 1300);
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
            micSeekBar.setProgress(Utils.refGainToPercent(getClient().getSoundInputGainLevel()));
        }
        TextView volLevel = findViewById(R.id.vollevel_text);
        volLevel.setText(Utils.refVolumeToPercent(mastervol) + getString(R.string.unit_percent));
        volLevel.setContentDescription(getString(R.string.speaker_volume_description, volLevel.getText().toString()));
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
        Permissions granted = Permissions.onRequestResult(this, requestCode, permissions, grantResults);
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
        if (complete) {
            activecmds.remove(cmdId);
        }
    }

    @Override
    public void onCmdMyselfLoggedIn(int my_userid, UserAccount useraccount) {
        textmsgAdapter.setMyUserID(my_userid);
        this.myUserAccount = useraccount;

        setCurrentChannel(null);
        channelsAdapter.notifyDataSetChanged();

        if (mSectionsPagerAdapter != null) {
            mSectionsPagerAdapter.notifyDataSetChanged();
        }

        isFirstJoinAfterLogin = true;
    }

    @Override
    public void onCmdMyselfLoggedOut() {
        // Removemos o showKickedDialog(true, null) duplicado aqui.
        // O onCmdMyselfKickedFromChannel já cuida de exibir o popup com o nome correto
        // do Admin.

        accessibilityAssistant.lockEvents();
        channelsAdapter.notifyDataSetChanged();
        eventHistoryAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();

        // Logout sound/TTS is now handled by TeamTalkService to support background notifications.

        // Cancel any pending "left channel" TTS (user disconnected, no need to announce
        // channel leave)
        if (pendingMyselfLeftChanTTS != null) {
            mainHandler.removeCallbacks(pendingMyselfLeftChanTTS);
            pendingMyselfLeftChanTTS = null;
        }
    }

    // Detecta se o logout foi causado por kick do servidor
    private boolean wasKickedFromServer() {
        // Usa o flag do TeamTalkService
        TeamTalkService service = mConnection != null ? mConnection.getService() : null;
        return service != null && service.wasKickedFromServer();
    }


    @Override
    public void onCmdMyselfKickedFromChannel(int channelID) {
        showKickedDialog(channelID == 0, null);
        accessibilityAssistant.lockEvents();
        channelsAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();
    }

    @Override
    public void onCmdMyselfKickedFromChannel(int channelID, User kicker) {
        showKickedDialog(channelID == 0, kicker);
        accessibilityAssistant.lockEvents();
        channelsAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();
    }

    // Novo método para mostrar expulsão do servidor
    public void onKickedFromServer(User kicker) {
        showKickedDialog(true, kicker);
    }

    /**
     * Exibe o dialog de expulsão, diferenciando canal e servidor
     * 
     * @param fromServer true se expulsão do servidor, false se do canal
     * @param kicker     usuário que expulsou, pode ser null
     */
    private void showKickedDialog(boolean fromServer, User kicker) {
        TeamTalkService service = mConnection != null ? mConnection.getService() : null;
        String kickerName;
        if (kicker != null && getClient() != null && kicker.nUserID == getClient().getMyUserID()) {
            kickerName = getString(R.string.scope_you);
        } else {
            kickerName = (kicker != null && kicker.szNickname != null && !kicker.szNickname.isEmpty())
                    ? kicker.szNickname
                    : getString(R.string.msg_no_owner);
        }

        int msgId = fromServer ? R.string.msg_kicked_from_server : R.string.msg_kicked_from_channel;
        String channelName = getString(R.string.msg_current_channel);
        if (!fromServer && mychannel != null && mychannel.szName != null && !mychannel.szName.isEmpty()) {
            channelName = getString(R.string.msg_channel_prefix, mychannel.szName);
        }

        String message = fromServer ? getString(msgId, kickerName) : getString(msgId, channelName, kickerName);

        new AlertDialog.Builder(this)
                .setTitle(R.string.title_expelled)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
    // ...existing code...

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
        channelsAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();

        // Events handled by TeamTalkService

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
        if (channelsAdapter != null) {
            channelsAdapter.notifyDataSetChanged();
        }
        accessibilityAssistant.unlockEvents();

        if (onlineUsersAdapter != null) {
            onlineUsersAdapter.updateUsers(new ArrayList<>(users.values()));
        }
    }

    @Override
    public void onCmdUserUpdate(User user) {
        subscriptionChange(user);
        users.put(user.nUserID, user);
        if (isVisibleChannel(user.nChannelID)) {
            accessibilityAssistant.lockEvents();
            channelsAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        }
        if (onlineUsersAdapter != null) {
            onlineUsersAdapter.updateUsers(new ArrayList<>(users.values()));
        }
    }

    @Override
    public void onCmdUserJoinedChannel(User user) {
        if(user.nUserID == getClient().getMyUserID()) {
            //myself joined channel
            
            textmsgAdapter.notifyDataSetChanged();

            Channel chan = getService().getChannels().get(user.nChannelID);
            if (chan != null && (curchannel == null || curchannel.nChannelID != user.nChannelID || isFirstJoinAfterLogin)) {
                setCurrentChannel(chan);
                isFirstJoinAfterLogin = false;
            }
            filesAdapter.update(curchannel);
            setMyChannel(chan);

            //update the displayed channel to the one we're currently in
            accessibilityAssistant.lockEvents();
            channelsAdapter.notifyDataSetChanged();
            eventHistoryAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        }
        else if((curchannel != null && curchannel.nChannelID == user.nChannelID) ||
                (curchannel == null && user.nChannelID == getClient().getRootChannelID())) {
            //other user joined current channel or root channel when viewing root
            accessibilityAssistant.lockEvents();
            channelsAdapter.notifyDataSetChanged();
            eventHistoryAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        }
        
        if(mychannel != null && mychannel.nChannelID == user.nChannelID) {
            //event took place in current channel
            
            if(user.nUserID != getClient().getMyUserID()) {
                accessibilityAssistant.lockEvents();
                textmsgAdapter.notifyDataSetChanged();
                channelsAdapter.notifyDataSetChanged();
                eventHistoryAdapter.notifyDataSetChanged();
                accessibilityAssistant.unlockEvents();
            }
            else {
                textmsgAdapter.notifyDataSetChanged();
                channelsAdapter.notifyDataSetChanged();
                eventHistoryAdapter.notifyDataSetChanged();
            }
        }
        else if (isVisibleChannel(user.nChannelID)) {
            accessibilityAssistant.lockEvents();
            channelsAdapter.notifyDataSetChanged();
            eventHistoryAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        }

        users.put(user.nUserID, user);

        if (onlineUsersAdapter != null) {
            onlineUsersAdapter.updateUsers(new ArrayList<>(users.values()));
        }
    }

    @Override
    public void onCmdUserLeftChannel(int channelid, User user) {

        if (users.containsKey(user.nUserID)) {
            users.put(user.nUserID, user);
        }
        
        if(user.nUserID == getClient().getMyUserID()) {
            //myself left current channel
            
            textmsgAdapter.notifyDataSetChanged();

            // Keep current channel view even after leaving
            // setCurrentChannel(null);
            
            setMyChannel(null);
        }
        else if((curchannel != null && channelid == curchannel.nChannelID) ||
                (curchannel == null && channelid == getClient().getRootChannelID())){
            //other user left current channel or root channel when viewing root
            
            accessibilityAssistant.lockEvents();
            textmsgAdapter.notifyDataSetChanged();
            if (channelsAdapter != null) {
                channelsAdapter.notifyDataSetChanged();
            }
            eventHistoryAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        }
        
        if(mychannel != null && mychannel.nChannelID == channelid) {
            //event took place in current channel
            
            accessibilityAssistant.lockEvents();
            textmsgAdapter.notifyDataSetChanged();
            channelsAdapter.notifyDataSetChanged();
            eventHistoryAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        }
        else if (isVisibleChannel(channelid)) {
            accessibilityAssistant.lockEvents();
            channelsAdapter.notifyDataSetChanged();
            eventHistoryAdapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
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
                case TextMsgType.MSGTYPE_CHANNEL:
                    // TeamTalkService already handles SOUND_CHANMSG
                    Log.d(TAG, "Channel message from " + textmessage.nFromUserID);
                    break;
                case TextMsgType.MSGTYPE_BROADCAST:
                    // TeamTalkService already handles SOUND_BCASTMSG
                    break;
                case TextMsgType.MSGTYPE_USER:
                    // TeamTalkService already handles SOUND_USERMSG
                    boolean autoPopup = prefs.get("pref_auto_popup_private_msg", false);
                    if (autoPopup) {
                        // Launch conversation window directly
                        Intent popupIntent = new Intent(this, TextMessageActivity.class);
                        popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(popupIntent.putExtra(TextMessageActivity.EXTRA_USERID, textmessage.nFromUserID));
                    } else {
                        User sender = getService().getUsers().get(textmessage.nFromUserID);
                        String senderName = (sender != null) ? Utils.getDisplayName(getBaseContext(), sender) : "";
                        Intent action = new Intent(this, TextMessageActivity.class);
                        action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            NotificationChannel mChannel = new NotificationChannel(MSG_NOTIFICATION_CHANNEL_ID,
                                    getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH);
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
                                .setContentIntent(PendingIntent.getActivity(this, textmessage.nFromUserID,
                                        action.putExtra(TextMessageActivity.EXTRA_USERID, textmessage.nFromUserID),
                                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                                .setAutoCancel(true)
                                .build();
                        notificationManager.notify(MESSAGE_NOTIFICATION_TAG, textmessage.nFromUserID, notification);
                    }
                    break;
                case TextMsgType.MSGTYPE_CUSTOM:
                    if (textmessage.szMessage.startsWith("typing\r\n")) {
                        String value = textmessage.szMessage.substring(8);
                        if ("1".equals(value)) {
                            playSound(SOUND_TYPING);
                            if (ttsWrapper != null && prefs.get("pref_tts_typing", true)) {
                                User typer = getService().getUsers().get(textmessage.nFromUserID);
                                String name = (typer != null) ? Utils.getDisplayName(getBaseContext(), typer) : "";
                                speakAndLog(getString(R.string.pref_tts_typing_msg, name), MyTextMessage.MSGTYPE_LOG_INFO);
                            }
                        }
                    }
                    break;
            }
        }

        else if (textmessage.nFromUserID == getClient().getMyUserID()) {
            // Outgoing message sounds and TTS are now handled immediately upon sending
            // in sendMsgToChannel and playPrivateMessageSentSound to ensure responsiveness.
        }
    }

    @Override
    public void onCmdChannelNew(Channel channel) {
        accessibilityAssistant.lockEvents();
        channelsAdapter.notifyDataSetChanged();
        eventHistoryAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();
    }

    @Override
    public void onCmdChannelUpdate(Channel channel) {
        accessibilityAssistant.lockEvents();
        channelsAdapter.notifyDataSetChanged();
        eventHistoryAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();

        if (mychannel != null && mychannel.nChannelID == channel.nChannelID) {

            if (ttsWrapper != null) {
                Utils.ttsTransmitUsersToggled(getBaseContext(), mychannel, channel, getService().getUsers())
                        .ifPresent(text -> speakAndLog(text, MyTextMessage.MSGTYPE_LOG_INFO));
            }

            int myuserid = getClient().getMyUserID();

            if (channel.transmitUsersQueue[0] == myuserid && mychannel.transmitUsersQueue[0] != myuserid) {
                // TeamTalkService handles TX state change sounds
            }
            if (mychannel.transmitUsersQueue[0] == myuserid && channel.transmitUsersQueue[0] != myuserid) {
                // TeamTalkService handles TX state change sounds
            }

            setMyChannel(channel);
        }
    }

    @Override
    public void onCmdChannelRemove(Channel channel) {
        accessibilityAssistant.lockEvents();
        channelsAdapter.notifyDataSetChanged();
        eventHistoryAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();
    }

    @Override
    public void onCmdFileNew(RemoteFile remotefile) {
        accessibilityAssistant.lockEvents();
        filesAdapter.update();
        eventHistoryAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();
    }

    @Override
    public void onCmdFileRemove(RemoteFile remotefile) {
        accessibilityAssistant.lockEvents();
        filesAdapter.update();
        eventHistoryAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();
    }

    @Override
    public void onConnectionLost() {
        // If in a channel, play channel-left sound to mirror the channel-join sound on
        // login
        if (mychannel != null && prefs.get("pref_sound_myself_leave", true)) {
            // TeamTalkService already handles SOUND_USERLEFT
        }
        // Clear channel state so next login is treated as initial join (not manual)
        setMyChannel(null);

        // Server lost sound/TTS is now handled by TeamTalkService.


        // Cancel any pending channel-leave TTS (disconnect TTS covers it)
        if (pendingMyselfLeftChanTTS != null) {
            mainHandler.removeCallbacks(pendingMyselfLeftChanTTS);
            pendingMyselfLeftChanTTS = null;
        }
    }

    @Override
    public void onUserStateChange(User user) {
        users.put(user.nUserID, user);
        accessibilityAssistant.lockEvents();
        channelsAdapter.notifyDataSetChanged();
        eventHistoryAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();

        if (user.nUserID == getClient().getMyUserID()) {
            invalidateOptionsMenu();
        }
    }

    @Override
    public void onVoiceTransmissionToggle(boolean voiceTransmissionEnabled, boolean isSuspended) {
        adjustTxState(voiceTransmissionEnabled);

        // Bug fix: update channel list so local user icon turns green when PTT is
        // active
        accessibilityAssistant.lockEvents();
        channelsAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();

        if (!isSuspended) {
            boolean ptt_vibrate = prefs.get("vibrate_checkbox", true) &&
                    Permissions.VIBRATE.request(this);
            if (voiceTransmissionEnabled) {
                accessibilityAssistant.shutUp();
                if (ptt_vibrate) {
                    Vibrator vibrat = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibrat.vibrate(50);
                }
            } else {
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
        adjustVoxState(voiceActivationEnabled,
                voiceActivationEnabled ? getClient().getVoiceActivationLevel() : getClient().getSoundInputGainLevel());
        if (voiceActivationEnabled) {
        } else {
        }
    }

    @Override
    public void onVoiceActivation(boolean bVoiceActive) {
        adjustTxState(bVoiceActive);

        // Bug fix: update channel list so local user icon turns green when VOX is
        // active
        accessibilityAssistant.lockEvents();
        channelsAdapter.notifyDataSetChanged();
        accessibilityAssistant.unlockEvents();

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
                nickname = getString(R.string.fallback_nickname, userId);
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

    @Override
    public void onAccessibilityActionClick(View view, int actionId) {
        Object item = view.getTag();
        if (item instanceof User) {
            if (actionId == R.string.action_select || actionId == R.string.action_deselect) {
                User user = (User) item;
                if (userIDS.contains(user.nUserID)) {
                    userIDS.remove((Integer) user.nUserID);
                } else {
                    userIDS.add(user.nUserID);
                }
                accessibilityAssistant.lockEvents();
                channelsAdapter.notifyDataSetChanged();
                accessibilityAssistant.unlockEvents();
            } else {
                handleUserAction(actionId, (User) item);
            }
        } else if (item instanceof Channel) {
            handleChannelAction(actionId, (Channel) item);
        } else if (item instanceof MyTextMessage) {
            if (actionId == R.string.action_reply) {
                mViewPager.setCurrentItem(2); // Chat tab
                EditText send_msg = findViewById(R.id.channel_im_edittext);
                if (send_msg != null) {
                    send_msg.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(send_msg, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }
    }

    private void toggleVoiceTransmission() {
        if (getService() != null) {
            if (getService().isVoiceActivationEnabled())
                getService().enableVoiceActivation(false);
            getService().enableVoiceTransmission(!getService().isVoiceTransmissionEnabled());
        }
    }

    @Override
    public List<AccessibilityActionCompat> getCustomActions(View view) {
        Object item = view.getTag();
        List<AccessibilityActionCompat> actions = new ArrayList<>();
        if (item instanceof User) {
            User user = (User) item;
            if (userIDS.contains(user.nUserID)) {
                actions.add(new AccessibilityActionCompat(R.string.action_deselect, getString(R.string.action_deselect)));
            } else {
                actions.add(new AccessibilityActionCompat(R.string.action_select, getString(R.string.action_select)));
            }
            actions.add(new AccessibilityActionCompat(R.string.info_copy_name, getString(R.string.info_copy_name)));
            actions.add(new AccessibilityActionCompat(R.string.info_copy_id, getString(R.string.info_copy_id)));
            actions.add(new AccessibilityActionCompat(R.string.info_copy_ip, getString(R.string.info_copy_ip)));
            actions.add(new AccessibilityActionCompat(R.string.action_edit_user, getString(R.string.action_edit_user)));
            actions.add(new AccessibilityActionCompat(R.string.button_msg, getString(R.string.button_msg)));
            actions.add(new AccessibilityActionCompat(R.string.action_transmission_control, getString(R.string.action_transmission_control)));

            UserAccount myuseraccount = new UserAccount();
            getClient().getMyUserAccount(myuseraccount);
            boolean kickRight = (myuseraccount.uUserRights
                    & UserRight.USERRIGHT_KICK_USERS) != UserRight.USERRIGHT_NONE;
            boolean banRight = (myuseraccount.uUserRights & UserRight.USERRIGHT_BAN_USERS) != UserRight.USERRIGHT_NONE;
            boolean operatorRight = getClient().isChannelOperator(getClient().getMyUserID(), user.nChannelID);

            boolean isOp = getClient().isChannelOperator(user.nUserID, user.nChannelID);
            actions.add(new AccessibilityActionCompat(
                    isOp ? R.string.action_revoke_operator : R.string.action_make_operator,
                    getString(isOp ? R.string.action_revoke_operator : R.string.action_make_operator)));

            if (kickRight || operatorRight) {
                actions.add(
                        new AccessibilityActionCompat(R.string.action_kickchan, getString(R.string.action_kickchan)));
            }
            if (banRight || operatorRight) {
                actions.add(new AccessibilityActionCompat(R.string.action_banchan, getString(R.string.action_banchan)));
            }
            if (kickRight) {
                actions.add(new AccessibilityActionCompat(R.string.action_kicksrv, getString(R.string.action_kicksrv)));
            }
            if (banRight) {
                actions.add(new AccessibilityActionCompat(R.string.action_bansrv, getString(R.string.action_bansrv)));
            }
        } else if (item instanceof Channel) {
            Channel channel = (Channel) item;
            UserAccount myuseraccount = new UserAccount();
            getClient().getMyUserAccount(myuseraccount);
            boolean chanRight = (myuseraccount.uUserRights
                    & UserRight.USERRIGHT_MODIFY_CHANNELS) != UserRight.USERRIGHT_NONE;
            boolean tempChanRight = (myuseraccount.uUserRights
                    & UserRight.USERRIGHT_CREATE_TEMPORARY_CHANNEL) != UserRight.USERRIGHT_NONE;

            if (chanRight || tempChanRight) {
                actions.add(new AccessibilityActionCompat(R.string.action_new_channel,
                        getString(R.string.action_new_channel)));
                actions.add(new AccessibilityActionCompat(R.string.title_activity_channel_prop,
                        getString(R.string.title_activity_channel_prop)));
                actions.add(new AccessibilityActionCompat(R.string.action_delete, getString(R.string.action_delete)));
            }

            actions.add(new AccessibilityActionCompat(R.string.action_join, getString(R.string.action_join)));

            boolean moveRight = (myuseraccount.uUserRights
                    & UserRight.USERRIGHT_MOVE_USERS) != UserRight.USERRIGHT_NONE;
            if (moveRight && !userIDS.isEmpty()) {
                actions.add(new AccessibilityActionCompat(R.string.action_move, getString(R.string.action_move)));
            }

            boolean banRight = (myuseraccount.uUserRights & UserRight.USERRIGHT_BAN_USERS) != UserRight.USERRIGHT_NONE;
            boolean operatorRight = getClient().isChannelOperator(getClient().getMyUserID(), channel.nChannelID);

            if (banRight || operatorRight) {
                actions.add(new AccessibilityActionCompat(R.string.action_banned_users,
                        getString(R.string.action_banned_users)));
            }
        } else if (item instanceof MyTextMessage) {
            MyTextMessage msg = (MyTextMessage) item;
            if (msg.nFromUserID != getClient().getMyUserID() && msg.nMsgType != TextMsgType.MSGTYPE_BROADCAST) {
                actions.add(new AccessibilityActionCompat(R.string.action_reply, getString(R.string.action_reply)));
            }
        }
        return actions;
    }

    private void showSubscriptionsDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_subscriptions);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_subscriptions, null);
        builder.setView(view);

        SwitchCompat voiceLocal = view.findViewById(R.id.switch_voice_local);
        SwitchCompat vidLocal = view.findViewById(R.id.switch_vid_local);
        SwitchCompat deskLocal = view.findViewById(R.id.switch_desk_local);
        SwitchCompat mediaLocal = view.findViewById(R.id.switch_media_local);
        SwitchCompat msgLocal = view.findViewById(R.id.switch_msg_local);

        voiceLocal.setChecked((user.uLocalSubscriptions & Subscription.SUBSCRIBE_VOICE) != 0);
        vidLocal.setChecked((user.uLocalSubscriptions & Subscription.SUBSCRIBE_VIDEOCAPTURE) != 0);
        deskLocal.setChecked((user.uLocalSubscriptions & Subscription.SUBSCRIBE_DESKTOP) != 0);
        mediaLocal.setChecked((user.uLocalSubscriptions & Subscription.SUBSCRIBE_MEDIAFILE) != 0);
        msgLocal.setChecked((user.uLocalSubscriptions & Subscription.SUBSCRIBE_USER_MSG) != 0);

        voiceLocal.setOnClickListener(v -> {
            boolean checked = voiceLocal.isChecked();
            Utils.toggleSubscription(getClient(), user, Subscription.SUBSCRIBE_VOICE, checked);
            announceTransmissionState(getString(R.string.user_prop_title_subscribe_voice), checked, false);
        });
        vidLocal.setOnClickListener(v -> {
            boolean checked = vidLocal.isChecked();
            Utils.toggleSubscription(getClient(), user, Subscription.SUBSCRIBE_VIDEOCAPTURE, checked);
            announceTransmissionState(getString(R.string.user_prop_title_subscribe_vid), checked, false);
        });
        deskLocal.setOnClickListener(v -> {
            boolean checked = deskLocal.isChecked();
            Utils.toggleSubscription(getClient(), user, Subscription.SUBSCRIBE_DESKTOP, checked);
            announceTransmissionState(getString(R.string.user_prop_title_subscribe_desk), checked, false);
        });
        mediaLocal.setOnClickListener(v -> {
            boolean checked = mediaLocal.isChecked();
            Utils.toggleSubscription(getClient(), user, Subscription.SUBSCRIBE_MEDIAFILE, checked);
            announceTransmissionState(getString(R.string.user_prop_title_subscribe_media), checked, false);
        });
        msgLocal.setOnClickListener(v -> {
            boolean checked = msgLocal.isChecked();
            Utils.toggleSubscription(getClient(), user, Subscription.SUBSCRIBE_USER_MSG, checked);
            announceTransmissionState(getString(R.string.user_prop_title_subscribe_user_msg), checked, false);
        });

        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    private void showTransmissionControlDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_transmission_control);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_transmission_control, null);
        builder.setView(view);

        getClient().getMyUserAccount(myUserAccount);
        boolean isAdmin = (myUserAccount.uUserType & UserType.USERTYPE_ADMIN) != 0;
        boolean isOp = getClient().isChannelOperator(getClient().getMyUserID(), user.nChannelID);
        boolean canModify = (myUserAccount.uUserRights & UserRight.USERRIGHT_MODIFY_CHANNELS) != 0;

        if (isAdmin || isOp || canModify) {
            view.findViewById(R.id.layout_global_controls).setVisibility(View.VISIBLE);
            SwitchCompat voiceGlobal = view.findViewById(R.id.switch_voice_global);
            SwitchCompat vidGlobal = view.findViewById(R.id.switch_vid_global);
            SwitchCompat deskGlobal = view.findViewById(R.id.switch_desk_global);
            SwitchCompat mediaGlobal = view.findViewById(R.id.switch_media_global);
            SwitchCompat msgGlobal = view.findViewById(R.id.switch_msg_global);

            Channel chan = getService().getChannels().get(user.nChannelID);
            if (chan != null) {
                voiceGlobal.setChecked(Utils.isTransmitAllowed(user, chan, StreamType.STREAMTYPE_VOICE));
                vidGlobal.setChecked(Utils.isTransmitAllowed(user, chan, StreamType.STREAMTYPE_VIDEOCAPTURE));
                deskGlobal.setChecked(Utils.isTransmitAllowed(user, chan, StreamType.STREAMTYPE_DESKTOP));
                mediaGlobal.setChecked(Utils.isTransmitAllowed(user, chan, StreamType.STREAMTYPE_MEDIAFILE));
                msgGlobal.setChecked(Utils.isTransmitAllowed(user, chan, StreamType.STREAMTYPE_CHANNELMSG));

                voiceGlobal.setOnClickListener(v -> {
                    boolean checked = voiceGlobal.isChecked();
                    Utils.toggleTransmitUsers(user, chan, StreamType.STREAMTYPE_VOICE, checked);
                    getClient().doUpdateChannel(chan);
                    announceTransmissionState(getString(R.string.user_prop_title_transmit_voice), checked, true);
                });
                vidGlobal.setOnClickListener(v -> {
                    boolean checked = vidGlobal.isChecked();
                    Utils.toggleTransmitUsers(user, chan, StreamType.STREAMTYPE_VIDEOCAPTURE, checked);
                    getClient().doUpdateChannel(chan);
                    announceTransmissionState(getString(R.string.user_prop_title_transmit_vid), checked, true);
                });
                deskGlobal.setOnClickListener(v -> {
                    boolean checked = deskGlobal.isChecked();
                    Utils.toggleTransmitUsers(user, chan, StreamType.STREAMTYPE_DESKTOP, checked);
                    getClient().doUpdateChannel(chan);
                    announceTransmissionState(getString(R.string.user_prop_title_transmit_desk), checked, true);
                });
                mediaGlobal.setOnClickListener(v -> {
                    boolean checked = mediaGlobal.isChecked();
                    Utils.toggleTransmitUsers(user, chan, StreamType.STREAMTYPE_MEDIAFILE, checked);
                    getClient().doUpdateChannel(chan);
                    announceTransmissionState(getString(R.string.user_prop_title_transmit_media), checked, true);
                });
                msgGlobal.setOnClickListener(v -> {
                    boolean checked = msgGlobal.isChecked();
                    Utils.toggleTransmitUsers(user, chan, StreamType.STREAMTYPE_CHANNELMSG, checked);
                    getClient().doUpdateChannel(chan);
                    announceTransmissionState(getString(R.string.user_prop_title_transmit_chanmsg), checked, true);
                });
            }
        }

        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    private void announceTransmissionState(String type, boolean enabled, boolean global) {
        String state = getString(enabled ? R.string.state_enabled : R.string.state_disabled);
        String scope = getString(global ? R.string.scope_global : R.string.scope_you);
        String message = getString(R.string.transmission_state_fmt, type, state, scope);

        View root = getWindow().getDecorView();
        if (root != null && prefs.get("pref_tts_transmission_control", true)) {
            root.announceForAccessibility(message);
        }
    }
}
