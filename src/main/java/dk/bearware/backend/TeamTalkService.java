
package dk.bearware.backend;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import java.util.Locale;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.preference.PreferenceManager;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import dk.bearware.AudioPreprocessor;
import dk.bearware.AudioPreprocessorType;
import dk.bearware.Channel;
import dk.bearware.ClientErrorMsg;
import dk.bearware.ClientEvent;
import dk.bearware.ClientFlag;
import dk.bearware.EncryptionContext;
import dk.bearware.FileTransfer;
import dk.bearware.FileTransferStatus;
import dk.bearware.MediaFileInfo;
import dk.bearware.MediaFileStatus;
import dk.bearware.RemoteFile;
import dk.bearware.ServerProperties;
import dk.bearware.SoundDeviceConstants;
import dk.bearware.SoundLevel;
import dk.bearware.StreamType;
import dk.bearware.Subscription;
import dk.bearware.TeamTalk5;
import dk.bearware.TeamTalkBase;
import dk.bearware.TextMessage;
import dk.bearware.TextMsgType;
import dk.bearware.User;
import dk.bearware.UserAccount;
import dk.bearware.UserRight;
import dk.bearware.WebRTCConstants;
import dk.bearware.data.AppInfo;
import dk.bearware.data.License;
import dk.bearware.data.MyTextMessage;
import dk.bearware.data.Preferences;
import dk.bearware.data.ServerEntry;
import dk.bearware.data.UserCached;
import dk.bearware.events.ClientEventListener;
import dk.bearware.events.TeamTalkEventHandler;
import dk.bearware.gui.CmdComplete;
import dk.bearware.gui.LocaleHelper;
import dk.bearware.gui.MainActivity;
import dk.bearware.gui.MediaButtonEventReceiver;
import dk.bearware.gui.R;
import dk.bearware.gui.Utils;

import static dk.bearware.gui.CmdComplete.CMD_COMPLETE_NONE;

public class TeamTalkService extends Service implements
        BluetoothHeadsetHelper.HeadsetConnectionListener,
        ClientEventListener.OnConnectSuccessListener,
        ClientEventListener.OnConnectFailedListener,
        ClientEventListener.OnConnectionLostListener,
        ClientEventListener.OnEncryptionErrorListener,
        ClientEventListener.OnCmdErrorListener,
        ClientEventListener.OnCmdSuccessListener,
        ClientEventListener.OnCmdProcessingListener,
        ClientEventListener.OnVoiceActivationListener,
        ClientEventListener.OnCmdMyselfLoggedInListener,
        ClientEventListener.OnCmdMyselfKickedFromChannelListener,
        ClientEventListener.OnCmdUserLoggedInListener,
        ClientEventListener.OnCmdUserLoggedOutListener,
        ClientEventListener.OnCmdUserUpdateListener,
        ClientEventListener.OnCmdUserJoinedChannelListener,
        ClientEventListener.OnCmdUserLeftChannelListener,
        ClientEventListener.OnCmdUserTextMessageListener,
        ClientEventListener.OnCmdChannelNewListener,
        ClientEventListener.OnCmdChannelUpdateListener,
        ClientEventListener.OnCmdChannelRemoveListener,
        ClientEventListener.OnCmdServerUpdateListener,
        ClientEventListener.OnCmdFileNewListener,
        ClientEventListener.OnCmdFileRemoveListener,
        ClientEventListener.OnUserStateChangeListener,
        ClientEventListener.OnFileTransferListener,
        ClientEventListener.OnStreamMediaFileListener,
        ClientEventListener.OnCmdMyselfLoggedOutListener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    // Flag para identificar expulsão do servidor
    private boolean kickedFromServer = false;
    private boolean connectionLost = false;
    // ...existing code...

    public static final String CANCEL_TRANSFER = "cancel_transfer";

    public static final String TAG = "bearware";

    private static final int UI_WIDGET_ID = 1;
    private static final String UI_CHANNEL_ID = "TeamtalkConnection";

    public static final int TAG_NOTIFICATION_TRANSFER = 2;
    private dk.bearware.data.TTSWrapper ttsWrapper;




    private BluetoothHeadsetHelper bluetoothHeadsetHelper;
    private TelephonyManager telephonyManager;
    OnVoiceTransmissionToggleListener onVoiceTransmissionToggleListener;
    private boolean listeningPhoneStateChanges;
    private boolean txSuspended;
    private boolean voxSuspended;
    private boolean permanentMuteState;
    private boolean currentMuteState;
    private Notification widget = null;
    private NotificationManager notificationManager;
    private volatile boolean inPhoneCall;
    private MediaSessionCompat mediaSession;
    Handler reconnectHandler = new Handler();
    Runnable reconnectTimer = this::reconnect;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private java.util.concurrent.ExecutorService connectionExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;


    public ServerEntry ttserver;
    Channel joinchannel,
            mychannel;
    private final TeamTalkEventHandler mEventHandler = new TeamTalkEventHandler();
    CountDownTimer eventTimer;
    SparseArray<CmdComplete> activecmds = new SparseArray<>();

    Map<Integer, Channel> channels = new HashMap<>();
    Map<Integer, RemoteFile> remoteFiles = new HashMap<>();
    Map<Integer, FileTransfer> fileTransfers = new HashMap<>();
    Map<Integer, User> users = new HashMap<>();
    Map<Integer, Vector<MyTextMessage>> usertxtmsgs = new HashMap<>();
    Vector<MyTextMessage> chatlogtxtmsgs = new Vector<>();
    Map<String, UserCached> usercache = new HashMap<>();

    private SoundPool audioIcons;
    private final SparseIntArray sounds = new SparseIntArray();
    private final Set<Integer> loadedSounds = new HashSet<>();
    private final Map<Integer, String[]> soundFilenames = new HashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService soundExecutor;
    private int appOpenSampleId = 0;
    private boolean pendingAppOpenSound = false;

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
            SOUND_TYPING = 24,
            SOUND_APP_OPEN = 25,
            SOUND_USERJOIN_MYSELF = 26,
            SOUND_USERLEFT_MYSELF = 27;

    private String currentServerName = "";
    private int markedChannelID = 0;
    private int lastJoinedChannelID = -1;
    private int myUserID = 0;
    private boolean isLoggingIn = false;
    private int currentProcessingCmdId = 0;
    private boolean isLoggingOut = false;

    public void setMarkedChannelID(int channelID) {
        this.markedChannelID = channelID;
    }

    public int getMarkedChannelID() {
        return markedChannelID;
    }

    private String getChannelDisplayName(Channel chan) {
        if (chan == null) return "";
        if (TextUtils.isEmpty(chan.szName) || chan.szName.equals("/")) {
            return localizedContext.getString(R.string.text_tts_root_channel_name);
        }
        return chan.szName;
    }

    private void initSoundFilenames() {
        // Aliases are tried in order; first match wins.
        // Include both Windows (no prefix) and Android (legacy) naming conventions.
        soundFilenames.put(SOUND_VOICETXON,    new String[]{"hotkey", "on", "voiceact_on"});
        soundFilenames.put(SOUND_VOICETXOFF,   new String[]{"hotkey", "off", "voiceact_off"});
        soundFilenames.put(SOUND_USERMSG,      new String[]{"user_message", "user_msg", "personal_message"});
        soundFilenames.put(SOUND_CHANMSG,      new String[]{"channel_message", "channel_msg"});
        soundFilenames.put(SOUND_BCASTMSG,     new String[]{"broadcast_message", "broadcast_msg"});
        soundFilenames.put(SOUND_SERVERLOST,   new String[]{"serverlost"});
        soundFilenames.put(SOUND_FILESUPDATE,  new String[]{"fileupdate"});
        soundFilenames.put(SOUND_VOXENABLE,    new String[]{"voiceact_enable", "vox_me_enable", "vox_enable"});
        soundFilenames.put(SOUND_VOXDISABLE,   new String[]{"voiceact_disable", "vox_me_disable", "vox_disable"});
        // VOXON/VOXOFF = voice-activation triggered (short beep); legacy uses on.ogg / off.ogg
        soundFilenames.put(SOUND_VOXON,        new String[]{"voiceact_on", "on"});
        soundFilenames.put(SOUND_VOXOFF,       new String[]{"voiceact_off", "off"});
        soundFilenames.put(SOUND_TXREADY,      new String[]{"txqueue_start"});
        soundFilenames.put(SOUND_TXSTOP,       new String[]{"txqueue_stop"});
        soundFilenames.put(SOUND_USERJOIN,     new String[]{"user_join", "newuser", "new_user"});
        soundFilenames.put(SOUND_USERLEFT,     new String[]{"user_left", "removeuser", "remove_user"});
        soundFilenames.put(SOUND_USERLOGGEDIN, new String[]{"logged_on"});
        soundFilenames.put(SOUND_USERLOGGEDOFF,new String[]{"logged_off"});
        soundFilenames.put(SOUND_INTERCEPTON,  new String[]{"intercept"});
        // interceptEnd exists with mixed case in Default Windows; interceptend in Legacy Android
        soundFilenames.put(SOUND_INTERCEPTOFF, new String[]{"intercept_end", "interceptEnd", "interceptend"});
        soundFilenames.put(SOUND_CHANMSGSENT,  new String[]{"channel_message_sent", "channel_msg_sent"});
        soundFilenames.put(SOUND_USERMSGSENT,  new String[]{"user_message_sent", "user_msg_sent"});
        soundFilenames.put(SOUND_MUTEALL,      new String[]{"mute_all"});
        soundFilenames.put(SOUND_UNMUTEALL,    new String[]{"unmute_all"});
        soundFilenames.put(SOUND_TYPING,       new String[]{"typing"});
        soundFilenames.put(SOUND_APP_OPEN,     new String[]{"vox_disable", "voiceact_disable", "vox_me_disable"});
        soundFilenames.put(SOUND_USERJOIN_MYSELF, soundFilenames.get(SOUND_USERJOIN));
        soundFilenames.put(SOUND_USERLEFT_MYSELF, soundFilenames.get(SOUND_USERLEFT));
    }

    private int loadSound(int soundEvent, int defaultResId) {
        return loadSound(soundEvent, defaultResId, audioIcons);
    }

    private int loadSound(int soundEvent, int defaultResId, SoundPool pool) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String packName = prefs.getString("pref_sound_pack", "Default Windows");

        String[] basenames = soundFilenames.get(soundEvent);
        if (basenames == null) {
            if (defaultResId != 0) return pool.load(this, defaultResId, 1);
            return 0;
        }

        String[] extensions = {".wav", ".ogg", ".mp3"};

        // Search assets (case-insensitive by trying both original and lowercase basename)
        for (String basename : basenames) {
            String[] caseVariants = basename.equals(basename.toLowerCase(Locale.ROOT))
                    ? new String[]{basename}
                    : new String[]{basename, basename.toLowerCase(Locale.ROOT)};
            for (String variant : caseVariants) {
                for (String ext : extensions) {
                    String assetPath = "sounds/" + packName + "/" + variant + ext;
                    try {
                        android.content.res.AssetFileDescriptor afd = getAssets().openFd(assetPath);
                        return pool.load(afd, 1);
                    } catch (java.io.IOException ignored) {
                    }
                }
            }
        }

        // Search external storage (case is handled by the filesystem)
        for (String basename : basenames) {
            for (String ext : extensions) {
                java.io.File soundFile = new java.io.File(Environment.getExternalStorageDirectory(),
                        AppInfo.FOLDER_NAME + "/Sounds/" + packName + "/" + basename + ext);
                if (soundFile.exists()) {
                    return pool.load(soundFile.getAbsolutePath(), 1);
                }
            }
        }

        if (defaultResId != 0) {
            return pool.load(this, defaultResId, 1);
        }
        return 0;
    }

    public void playSound(int soundEvent) {
        int soundId = sounds.get(soundEvent);
        if (audioIcons != null && soundId != 0 && loadedSounds.contains(soundId)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean play = true;
            switch(soundEvent) {
                case SOUND_SERVERLOST: play = prefs.getBoolean("server_lost_audio_icon", true); break;
                case SOUND_VOICETXON:
                case SOUND_VOICETXOFF: play = prefs.getBoolean("rx_tx_audio_icon", true); break;
                case SOUND_USERMSG: play = prefs.getBoolean("private_message_audio_icon", true); break;
                case SOUND_USERMSGSENT: play = prefs.getBoolean("user_message_sent_audio_icon", true); break;
                case SOUND_CHANMSG: play = prefs.getBoolean("channel_message_audio_icon", true); break;
                case SOUND_CHANMSGSENT: play = prefs.getBoolean("channel_message_sent_audio_icon", true); break;
                case SOUND_BCASTMSG: play = prefs.getBoolean("broadcast_message_audio_icon", true); break;
                case SOUND_FILESUPDATE: play = prefs.getBoolean("files_updated_audio_icon", true); break;
                case SOUND_VOXENABLE:
                case SOUND_VOXDISABLE: play = prefs.getBoolean("voiceact_audio_icon", true); break;
                case SOUND_VOXON:
                case SOUND_VOXOFF: play = prefs.getBoolean("voiceact_triggered_icon", true); break;
                case SOUND_TXREADY:
                case SOUND_TXSTOP: play = prefs.getBoolean("transmitready_icon", true); break;
                case SOUND_USERJOIN: play = prefs.getBoolean("userjoin_icon", true); break;
                case SOUND_USERJOIN_MYSELF: play = prefs.getBoolean("pref_sound_myself_join", true); break;
                case SOUND_USERLEFT: play = prefs.getBoolean("userleft_icon", true); break;
                case SOUND_USERLEFT_MYSELF: play = prefs.getBoolean("pref_sound_myself_leave", true); break;
                case SOUND_USERLOGGEDIN:
                    play = isLoggingIn ? prefs.getBoolean("pref_sound_server_login", true) : prefs.getBoolean("userloggedin_icon", true);
                    break;
                case SOUND_USERLOGGEDOFF:
                    play = isLoggingOut ? prefs.getBoolean("pref_sound_server_logout", true) : prefs.getBoolean("userloggedoff_icon", true);
                    break;
                case SOUND_APP_OPEN: play = prefs.getBoolean("pref_sound_app_open", true); break;
                case SOUND_INTERCEPTON:
                case SOUND_INTERCEPTOFF: play = prefs.getBoolean("intercept_audio_icon", true); break;
                case SOUND_TYPING: play = prefs.getBoolean("typing_audio_icon", true); break;
                case SOUND_MUTEALL: play = prefs.getBoolean("mute_all_audio_icon", true); break;
                case SOUND_UNMUTEALL: play = prefs.getBoolean("unmute_all_audio_icon", true); break;
            }
            if (play) {
                audioIcons.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
            }
        }
    }

    public void disconnect() {
        if (isLoggingOut) return;
        isLoggingOut = true;

        handleLeaveEventBeforeDisconnect();

        ServerProperties prop = new ServerProperties();
        if (ttclient != null && ttclient.getServerProperties(prop) && !TextUtils.isEmpty(prop.szServerName)) {
            currentServerName = prop.szServerName;
        }

        reconnectHandler.removeCallbacks(reconnectTimer);
        if (ttclient != null)
            ttclient.disconnect();

        String ttsMsg;
        if (!TextUtils.isEmpty(currentServerName)) {
            ttsMsg = localizedContext.getString(R.string.text_tts_myself_loggedout, currentServerName);
        } else {
            ttsMsg = localizedContext.getString(R.string.text_tts_myself_loggedout, "").trim();
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("server_logout_checkbox", true)) {
            speakAndLog(ttsMsg, MyTextMessage.MSGTYPE_LOG_ERROR);
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            playSound(SOUND_USERLOGGEDOFF);
            resetState(true);
        }, 200);
    }

    public void resetState() {
        resetState(true);
    }

    public void resetState(boolean clearReconnect) {
        if (clearReconnect) {
            reconnectHandler.removeCallbacks(reconnectTimer);
            ttserver = null;
        }
        disablePhoneCallReaction();

        syncToUserCache();

        if (ttclient != null)
            ttclient.disconnect();

        displayNotification(false);
        joinchannel = null;
        setMyChannel(null);
        activecmds.clear();
        channels.clear();
        remoteFiles.clear();
        fileTransfers.clear();
        users.clear();
        usertxtmsgs.clear();
        chatlogtxtmsgs.clear();
        currentServerName = "";
        lastJoinedChannelID = -1;
        myUserID = 0;
        isLoggingIn = false;
        isLoggingOut = false;

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public Map<Integer, Channel> getChannels() {
        return channels;
    }

    public Map<Integer, RemoteFile> getRemoteFiles() {
        return remoteFiles;
    }

    public Map<Integer, FileTransfer> getFileTransfers() {
        return fileTransfers;
    }

    public Map<Integer, User> getUsers() {
        return users;
    }

    private final MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            super.onMediaButtonEvent(mediaButtonEvent);
            final String intentAction = mediaButtonEvent.getAction();
            if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
                final KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event == null) {
                    return false;
                }
                final int keycode = event.getKeyCode();
                final int action = event.getAction();
                if (event.getRepeatCount() == 0 && action == KeyEvent.ACTION_DOWN) {
                    switch (keycode) {
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                        case KeyEvent.KEYCODE_HEADSETHOOK:
                            if (isVoiceActivationEnabled())
                                enableVoiceActivation(false);
                            else
                                enableVoiceTransmission(!isVoiceTransmissionEnabled());
                            break;
                    }
                    return true;
                }
            }
            return false;
        }
    };

    public class LocalBinder extends Binder {
        public TeamTalkService getService() {

            return TeamTalkService.this;
        }
    }

    private TeamTalkBase ttclient;
    private final IBinder mBinder = new LocalBinder();

    private Context localizedContext;

    public TeamTalkBase getTTInstance() {
        return ttclient;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service onCreate");

        localizedContext = LocaleHelper.onAttach(this);

        TeamTalk5.loadLibrary();

        initSoundFilenames();
        soundExecutor = Executors.newSingleThreadExecutor();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String engineName = prefs.getString("pref_speech_engine", dk.bearware.data.TTSWrapper.defaultEngineName);
        ttsWrapper = new dk.bearware.data.TTSWrapper(localizedContext, engineName);
        ttsWrapper.useAnnouncements = prefs.getBoolean("pref_use_announcements", false);
        ttsWrapper.setAccessibilityStream(prefs.getBoolean("pref_a11y_volume", false));
        boolean startWithA11y = prefs.getBoolean("pref_sound_accessibility_volume", false);
        audioIcons = buildSoundPool(startWithA11y);

        audioIcons.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) {
                loadedSounds.add(sampleId);
                if (pendingAppOpenSound && sampleId == appOpenSampleId) {
                    if (sounds.get(SOUND_APP_OPEN) == 0) {
                        sounds.put(SOUND_APP_OPEN, sampleId);
                    }
                    playSound(SOUND_APP_OPEN);
                    pendingAppOpenSound = false;
                }
            }
        });

        soundExecutor.execute(() -> reloadAllSounds(audioIcons, true));

        TeamTalk5.setLicenseInformation(License.REGISTRATION_NAME, License.REGISTRATION_KEY);

        ttclient = new TeamTalk5();

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        listeningPhoneStateChanges = false;
        txSuspended = false;
        voxSuspended = false;
        permanentMuteState = false;
        currentMuteState = false;
        inPhoneCall = false;

        mEventHandler.registerOnConnectSuccessListener(this, true);
        mEventHandler.registerOnConnectFailedListener(this, true);
        mEventHandler.registerOnConnectionLostListener(this, true);
        mEventHandler.registerOnEncryptionErrorListener(this, true);

        mEventHandler.registerOnCmdError(this, true);
        mEventHandler.registerOnCmdSuccess(this, true);
        mEventHandler.registerOnCmdProcessing(this, true);
        mEventHandler.registerOnCmdMyselfLoggedIn(this, true);
        mEventHandler.registerOnCmdMyselfKickedFromChannel(this, true);
        mEventHandler.registerOnCmdUserLoggedIn(this, true);
        mEventHandler.registerOnCmdUserLoggedOut(this, true);
        mEventHandler.registerOnCmdUserUpdate(this, true);
        mEventHandler.registerOnCmdUserJoinedChannel(this, true);
        mEventHandler.registerOnCmdUserLeftChannel(this, true);
        mEventHandler.registerOnCmdUserTextMessage(this, true);
        mEventHandler.registerOnCmdChannelNew(this, true);
        mEventHandler.registerOnCmdChannelUpdate(this, true);
        mEventHandler.registerOnCmdChannelRemove(this, true);
        mEventHandler.registerOnCmdServerUpdate(this, true);
        mEventHandler.registerOnCmdFileNew(this, true);
        mEventHandler.registerOnCmdFileRemove(this, true);

        mEventHandler.registerOnUserStateChange(this, true);
        mEventHandler.registerOnVoiceActivation(this, true);
        mEventHandler.registerOnFileTransfer(this, true);
        mEventHandler.registerOnStreamMediaFile(this, true);

        createEventTimer();

        bluetoothHeadsetHelper = new BluetoothHeadsetHelper(this);

        ComponentName receiver = new ComponentName(getPackageName(), MediaButtonEventReceiver.class.getName());

        mediaSession = new MediaSessionCompat(this, "TeamTalkService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .build());
        mediaSession.setCallback(mMediaSessionCallback);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.d(TAG, "Network available, checking if we need to reconnect");
                if (ttserver != null && !isLoggingIn && !isLoggingOut && (ttclient == null || (ttclient.getFlags() & ClientFlag.CLIENT_CONNECTED) == 0)) {
                    reconnectHandler.removeCallbacks(reconnectTimer);
                    reconnectHandler.postDelayed(reconnectTimer, 1000);
                }
            }
        };
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        updateAudioRouting();
        mediaSession.setActive(true);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":ServiceWakeLock");
        wakeLock.setReferenceCounted(false);

        Log.d(TAG, "Created TeamTalkPlus service");
    }

    /** Load every sound event from the current pack into the given pool and post results to the main thread. */
    private SoundPool buildSoundPool(boolean useA11yStream) {
        SoundPool newPool;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            newPool = new SoundPool.Builder()
                    .setMaxStreams(32)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(useA11yStream ? AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY : AudioAttributes.USAGE_MEDIA)
                            .setContentType(useA11yStream ? AudioAttributes.CONTENT_TYPE_SONIFICATION : AudioAttributes.CONTENT_TYPE_MUSIC)
                            // Remove setLegacyStreamType(10) as it can cause deadlocks/security issues on Android 11
                            .build())
                    .build();
        } else {
            newPool = new SoundPool(32, AudioManager.STREAM_MUSIC, 0);
        }
        return newPool;
    }

    private void reloadAllSounds(SoundPool pool, boolean playOpenSound) {
        int tt_on        = loadSound(SOUND_VOICETXON,    0, pool);
        int tt_off       = loadSound(SOUND_VOICETXOFF,   0, pool);
        int usermsg      = loadSound(SOUND_USERMSG,      0, pool);
        int chanmsg      = loadSound(SOUND_CHANMSG,      0, pool);
        int bcastmsg     = loadSound(SOUND_BCASTMSG,     0, pool);
        int serverlost   = loadSound(SOUND_SERVERLOST,   0, pool);
        int filesupdate  = loadSound(SOUND_FILESUPDATE,  0, pool);
        int voxenable    = loadSound(SOUND_VOXENABLE,    0, pool);
        int voxdisable   = loadSound(SOUND_VOXDISABLE,   0, pool);
        int voxon        = loadSound(SOUND_VOXON,        0, pool);
        int voxoff       = loadSound(SOUND_VOXOFF,       0, pool);
        int txready      = loadSound(SOUND_TXREADY,      0, pool);
        int txstop       = loadSound(SOUND_TXSTOP,       0, pool);
        int userjoin     = loadSound(SOUND_USERJOIN,     0, pool);
        int userleft     = loadSound(SOUND_USERLEFT,     0, pool);
        int userloggedin = loadSound(SOUND_USERLOGGEDIN, 0, pool);
        int userloggedoff= loadSound(SOUND_USERLOGGEDOFF,0, pool);
        int intercepton  = loadSound(SOUND_INTERCEPTON,  0, pool);
        int interceptoff = loadSound(SOUND_INTERCEPTOFF, 0, pool);
        int chanmsgsent  = loadSound(SOUND_CHANMSGSENT,  0, pool);
        int usermsgsent  = loadSound(SOUND_USERMSGSENT,  0, pool);
        int muteall      = loadSound(SOUND_MUTEALL,      0, pool);
        int unmuteall    = loadSound(SOUND_UNMUTEALL,    0, pool);
        int typing       = loadSound(SOUND_TYPING,       0, pool);
        int appopen      = loadSound(SOUND_APP_OPEN,     0, pool);
        if (playOpenSound) {
            appOpenSampleId = appopen;
            pendingAppOpenSound = true;
        }

        mainHandler.post(() -> {
            sounds.put(SOUND_VOICETXON,     tt_on);
            sounds.put(SOUND_VOICETXOFF,    tt_off);
            sounds.put(SOUND_USERMSG,       usermsg);
            sounds.put(SOUND_CHANMSG,       chanmsg);
            sounds.put(SOUND_BCASTMSG,      bcastmsg);
            sounds.put(SOUND_SERVERLOST,    serverlost);
            sounds.put(SOUND_FILESUPDATE,   filesupdate);
            sounds.put(SOUND_VOXENABLE,     voxenable);
            sounds.put(SOUND_VOXDISABLE,    voxdisable);
            sounds.put(SOUND_VOXON,         voxon);
            sounds.put(SOUND_VOXOFF,        voxoff);
            sounds.put(SOUND_TXREADY,       txready);
            sounds.put(SOUND_TXSTOP,        txstop);
            sounds.put(SOUND_USERJOIN,      userjoin);
            sounds.put(SOUND_USERJOIN_MYSELF, userjoin);
            sounds.put(SOUND_USERLEFT,      userleft);
            sounds.put(SOUND_USERLEFT_MYSELF, userleft);
            sounds.put(SOUND_USERLOGGEDIN,  userloggedin);
            sounds.put(SOUND_USERLOGGEDOFF, userloggedoff);
            sounds.put(SOUND_INTERCEPTON,   intercepton);
            sounds.put(SOUND_INTERCEPTOFF,  interceptoff);
            sounds.put(SOUND_CHANMSGSENT,   chanmsgsent);
            sounds.put(SOUND_USERMSGSENT,   usermsgsent);
            sounds.put(SOUND_MUTEALL,       muteall);
            sounds.put(SOUND_UNMUTEALL,     unmuteall);
            sounds.put(SOUND_TYPING,        typing);
            sounds.put(SOUND_APP_OPEN,      appopen);
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ((intent != null) && intent.hasExtra(CANCEL_TRANSFER)) {
            int transferId = intent.getIntExtra(CANCEL_TRANSFER, 0);
            if ((ttclient != null) && ttclient.cancelFileTransfer(transferId)) {
                fileTransfers.remove(transferId);
                Toast.makeText(this, R.string.transfer_stopped, Toast.LENGTH_LONG).show();
            }
        }
        if (mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0.0f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE).build());
        } else {
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE).build());
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        if (audioIcons != null) {
            audioIcons.release();
            audioIcons = null;
        }
        if (soundExecutor != null) {
            soundExecutor.shutdown();
            soundExecutor = null;
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        eventTimer.cancel();

        mEventHandler.unregisterListener(this);
        disablePhoneCallReaction();
        unwatchBluetoothHeadset();

        if (ttclient != null)
            ttclient.closeTeamTalk();

        if (ttsWrapper != null) {
            ttsWrapper.shutdown();
            ttsWrapper = null;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }

        super.onDestroy();
        mediaSession.release();

        Log.d(TAG, "Destroyed TeamTalkPlus service");
    }

    private String getNotificationText() {
        return (mychannel != null) ? String.format("%s / %s", ttserver.servername, mychannel.szName)
                : ttserver.servername;
    }

    @SuppressLint("NewApi")
    private void displayNotification(boolean enabled) {
        if (enabled) {
            if (widget == null) {
                notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Intent ui = new Intent(this, MainActivity.class);
                ui.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel mChannel = new NotificationChannel(UI_CHANNEL_ID,
                            localizedContext.getString(R.string.notification_channel_connection),
                            NotificationManager.IMPORTANCE_DEFAULT);
                    mChannel.enableVibration(false);
                    mChannel.setVibrationPattern(null);
                    mChannel.enableLights(false);
                    mChannel.setSound(null, null);
                    notificationManager.createNotificationChannel(mChannel);
                }
                int intentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
                CharSequence appName = getApplicationInfo().loadLabel(getPackageManager());
                widget = new NotificationCompat.Builder(this, UI_CHANNEL_ID)
                        .setSmallIcon(R.drawable.teamtalk_green)
                        .setContentTitle(appName)
                        .setContentIntent(PendingIntent.getActivity(this, 0, ui, intentFlags))
                        .setOngoing(true)
                        .setAutoCancel(false)
                        .setContentText(getNotificationText())
                        .setShowWhen(false)
                        .build();
                ServiceCompat.startForeground(this, UI_WIDGET_ID, widget, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST);
            } else {
                widget = new NotificationCompat.Builder(this, widget)
                        .setContentText(getNotificationText())
                        .build();
                notificationManager.notify(UI_WIDGET_ID, widget);
            }
        } else if (widget != null) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
            widget = null;
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private void adjustMuteOnTx(boolean txEnabled) {
        if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getBoolean(Preferences.PREF_SOUNDSYSTEM_MUTE_ON_TRANSMISSION, false)) {
            boolean isMuted = isMute();
            if ((txEnabled && !isMuted) || (isMuted && !txEnabled && !permanentMuteState))
                ttclient.setSoundOutputMute(txEnabled);
        }
    }

    public void enablePhoneCallReaction() {
        txSuspended = false;
        voxSuspended = false;
        inPhoneCall = false;
    }

    public void disablePhoneCallReaction() {
        txSuspended = false;
        voxSuspended = false;
        inPhoneCall = false;
    }

    public boolean isInPhoneCall() {
        return inPhoneCall;
    }

    public void watchBluetoothHeadset() {
        if (bluetoothHeadsetHelper.start()) {
            if (bluetoothHeadsetHelper.isHeadsetConnected())
                bluetoothHeadsetHelper.scoAudioConnect();
            bluetoothHeadsetHelper.registerHeadsetConnectionListener(this);
        }
    }

    public void unwatchBluetoothHeadset() {
        bluetoothHeadsetHelper.unregisterHeadsetConnectionListener(this);
        bluetoothHeadsetHelper.stop();
    }

    private void setMyChannel(Channel chan) {
        this.mychannel = chan;

        setupAudioPreprocessor();
    }

    public TeamTalkEventHandler getEventHandler() {
        return mEventHandler;
    }

    public ServerEntry getServerEntry() {
        return ttserver;
    }

    public dk.bearware.data.TTSWrapper getTTSWrapper() {
        return ttsWrapper;
    }

    public void setServerEntry(ServerEntry entry) {
        ttserver = entry;
    }

    public void setJoinChannel(Channel channel) {
        joinchannel = channel;
    }

    public void setOnVoiceTransmissionToggleListener(OnVoiceTransmissionToggleListener listener) {
        onVoiceTransmissionToggleListener = listener;
    }

    public boolean getCurrentMuteState() {
        return currentMuteState;
    }

    private void handleLeaveEventBeforeDisconnect() {
        if (mychannel != null) {
            User myself = users.get(myUserID != 0 ? myUserID : ttclient.getMyUserID());
            if (myself != null) {
                onCmdUserLeftChannel(mychannel.nChannelID, myself);
            }
        }
    }

    public boolean isMute() {
        return ((ttclient.getFlags() & ClientFlag.CLIENT_SNDOUTPUT_MUTE) != 0);
    }

    public boolean isVoiceTransmissionEnabled() {
        return (ttclient.getFlags() & ClientFlag.CLIENT_TX_VOICE) != 0;
    }

    public boolean isVoiceTransmitting() {
        final int voiceActivationMask = ClientFlag.CLIENT_SNDINPUT_VOICEACTIVATED
                | ClientFlag.CLIENT_SNDINPUT_VOICEACTIVE;
        int flags = ttclient.getFlags();
        return ((flags & ClientFlag.CLIENT_TX_VOICE) != 0) ||
                ((flags & voiceActivationMask) == voiceActivationMask);
    }

    public boolean isVoiceActivationEnabled() {
        return (ttclient.getFlags()
                & (ClientFlag.CLIENT_SNDINPUT_VOICEACTIVATED | ClientFlag.CLIENT_SNDINPUT_VOICEACTIVE)) != 0;
    }

    public void setMute(boolean state) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        permanentMuteState = state;
        currentMuteState = state;
        if ((isMute() != permanentMuteState) &&
                !(prefs.getBoolean(Preferences.PREF_SOUNDSYSTEM_MUTE_ON_TRANSMISSION, false) && isVoiceTransmitting()))
            ttclient.setSoundOutputMute(permanentMuteState);
    }

    public void enableVoiceTransmission(boolean enable) {
        if (enable) {
            txSuspended = false;
            voxSuspended = false;
            int indevid = SoundDeviceConstants.TT_SOUNDDEVICE_ID_OPENSLES_DEFAULT;

            if (((ttclient.getFlags() & ClientFlag.CLIENT_SNDINPUT_READY) != 0)
                    || ttclient.initSoundInputDevice(indevid))
                ttclient.enableVoiceTransmission(true);
        } else {
            ttclient.enableVoiceTransmission(false);
            ttclient.closeSoundInputDevice();
        }
        adjustMuteOnTx(enable);
    }

    public void enableVoiceActivation(boolean enable) {
        if (enable) {
            txSuspended = false;
            voxSuspended = false;
            int indevid = SoundDeviceConstants.TT_SOUNDDEVICE_ID_OPENSLES_DEFAULT;

            if (((ttclient.getFlags() & ClientFlag.CLIENT_SNDINPUT_READY) != 0)
                    || ttclient.initSoundInputDevice(indevid))
                ttclient.enableVoiceActivation(true);
        } else {
            ttclient.enableVoiceActivation(false);
            ttclient.closeSoundInputDevice();
        }
        adjustMuteOnTx(enable);
    }

    public void syncToUserCache(User user) {
        String cacheid = UserCached.getCacheID(user);
        if (!cacheid.isEmpty()) {
            usercache.put(cacheid, new UserCached(user));
        }
    }

    public void syncToUserCache() {

        for (Map.Entry<Integer, User> entry : users.entrySet()) {
            syncToUserCache(entry.getValue());
        }
    }

    public void syncFromUserCache(User user) {
        String cacheid = UserCached.getCacheID(user);
        if (cacheid.isEmpty())
            return;

        UserCached userprop = usercache.get(cacheid);
        if (userprop != null) {
            userprop.sync(ttclient, user);
        }
    }

    public boolean reconnect() {
        if (ttserver == null || ttclient == null)
            return false;

        connectionExecutor.execute(() -> {
            Log.d(TAG, "Reconnecting to " + ttserver.ipaddr);
            syncToUserCache();

            ttclient.disconnect();

            if (!setupEncryption()) {
                createReconnectTimer(5000);
                return;
            }

            if (!ttclient.connect(ttserver.ipaddr, ttserver.tcpport,
                    ttserver.udpport, 0, 0, ttserver.encrypted)) {
                ttclient.disconnect();
                createReconnectTimer(5000);
                return;
            }
        });

        return true;
    }

    private boolean setupEncryption() {
        if (!this.ttserver.encrypted)
            return true;

        File outputDir = getBaseContext().getCacheDir();
        try {
            File cacertfile = File.createTempFile("cacert", "pem", outputDir);
            File clientcertfile = File.createTempFile("clientcert", "pem", outputDir);
            File clientkeyfile = File.createTempFile("clientkey", "pem", outputDir);
            try (FileWriter cawriter = new FileWriter(cacertfile);
                    FileWriter certwriter = new FileWriter(clientcertfile);
                    FileWriter keywriter = new FileWriter(clientkeyfile)) {
                cawriter.write(this.ttserver.cacert);
                certwriter.write(this.ttserver.clientcert);
                keywriter.write(this.ttserver.clientcertkey);
            }
            EncryptionContext context = new EncryptionContext();
            if (!this.ttserver.cacert.isEmpty())
                context.szCAFile = cacertfile.getAbsolutePath();
            if (!this.ttserver.clientcert.isEmpty())
                context.szCertificateFile = clientcertfile.getAbsolutePath();
            if (!this.ttserver.clientcertkey.isEmpty())
                context.szPrivateKeyFile = clientkeyfile.getAbsolutePath();
            context.bVerifyPeer = ttserver.verifypeer;
            if (!context.bVerifyPeer) {
                context.nVerifyDepth = -1;
            }
            return ttclient.setEncryptionContext(context);
        } catch (IOException e) {
            return false;
        }
    }

    public int HISTORY_CHATLOG_MSG_MAX = 100;
    public int HISTORY_USER_MSG_MAX = 100;

    public Vector<MyTextMessage> getUserTextMsgs(int userid) {
        Vector<MyTextMessage> msgs;
        if (usertxtmsgs.get(userid) == null) {
            msgs = new Vector<>();
            usertxtmsgs.put(userid, msgs);
        }
        msgs = usertxtmsgs.get(userid);
        if (msgs.size() > HISTORY_USER_MSG_MAX)
            msgs.remove(0);
        return msgs;
    }

    public Vector<MyTextMessage> getChatLogTextMsgs() {
        if (chatlogtxtmsgs.size() > HISTORY_CHATLOG_MSG_MAX)
            chatlogtxtmsgs.remove(0);

        return chatlogtxtmsgs;
    }

    void createEventTimer() {
        eventTimer = new CountDownTimer(10000, 100) {
            private boolean prevVoiceTransmissionState = isVoiceTransmissionEnabled();
            private boolean prevVoiceActivationState = isVoiceActivationEnabled();

            public void onTick(long millisUntilFinished) {
                while (mEventHandler.processEvent(ttclient, 0))
                    ;
                boolean newVoiceTransmissionState = isVoiceTransmissionEnabled();
                boolean newVoiceActivationState = isVoiceActivationEnabled();

                if (newVoiceTransmissionState != prevVoiceTransmissionState) {
                    playSound(newVoiceTransmissionState ? SOUND_VOICETXON : SOUND_VOICETXOFF);
                    if (onVoiceTransmissionToggleListener != null) {
                        onVoiceTransmissionToggleListener.onVoiceTransmissionToggle(newVoiceTransmissionState,
                                txSuspended);
                    }
                    prevVoiceTransmissionState = newVoiceTransmissionState;
                }
                if (newVoiceActivationState != prevVoiceActivationState) {
                    playSound(newVoiceActivationState ? SOUND_VOXENABLE : SOUND_VOXDISABLE);
                    if (onVoiceTransmissionToggleListener != null) {
                        onVoiceTransmissionToggleListener.onVoiceActivationToggle(newVoiceActivationState,
                                voxSuspended);
                    }
                    prevVoiceActivationState = newVoiceActivationState;
                }
            }

            public void onFinish() {
                start();
            }
        };
        eventTimer.start();
    }

    void createReconnectTimer(long delayMsec) {

        reconnectHandler.removeCallbacks(reconnectTimer);
        reconnectHandler.postDelayed(reconnectTimer, delayMsec);
    }

    private void login() {

        String nickname = ttserver.nickname;
        if (TextUtils.isEmpty(nickname)) {
            nickname = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .getString(Preferences.PREF_GENERAL_NICKNAME, "");
        }

        int loginCmdId = ttclient.doLoginEx(nickname, ttserver.username, ttserver.password, AppInfo.APPNAME_SHORT);
        if (loginCmdId < 0) {
            Toast.makeText(this, localizedContext.getString(R.string.text_cmderr_login),
                    Toast.LENGTH_LONG).show();
        } else {
            activecmds.put(loginCmdId, CmdComplete.CMD_COMPLETE_LOGIN);
        }

        MyTextMessage msg = MyTextMessage.createLogMsg(MyTextMessage.MSGTYPE_LOG_INFO,
                localizedContext.getString(R.string.text_con_success));
        getChatLogTextMsgs().add(msg);
    }

    private void loginComplete() {
        if (joinchannel == null) {

            if (ttserver.channel != null && !ttserver.channel.isEmpty()) {
                int chanid = ttclient.getChannelIDFromPath(ttserver.channel);
                joinchannel = getChannels().get(chanid);
                if (joinchannel != null) {
                    joinchannel.szPassword = ttserver.chanpasswd;
                }
            }

            UserAccount useraccount = new UserAccount();
            ttclient.getMyUserAccount(useraccount);
            if (joinchannel == null && !useraccount.szInitChannel.isEmpty()) {
                int chanid = ttclient.getChannelIDFromPath(useraccount.szInitChannel);
                joinchannel = getChannels().get(chanid);
            }

            boolean joinroot = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .getBoolean(Preferences.PREF_JOIN_ROOT_CHAN, true);
            if (joinroot && joinchannel == null) {
                joinchannel = getChannels().get(ttclient.getRootChannelID());
                if (joinchannel != null) {
                    joinchannel.szPassword = ttserver.chanpasswd;
                }
            }
        }

        if (joinchannel != null) {
            int cmdid = ttclient.doJoinChannel(joinchannel);
            activecmds.put(cmdid, CmdComplete.CMD_COMPLETE_JOIN);
        } else {
            isLoggingIn = false;
        }
    }

    public void updateAudioRouting() {
        if (audioManager == null) return;
        
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean voiceProcessing = sharedPrefs.getBoolean(Preferences.PREF_SOUNDSYSTEM_VOICEPROCESSING, false);
        boolean speakerphone = sharedPrefs.getBoolean(Preferences.PREF_SOUNDSYSTEM_SPEAKERPHONE, false);
        boolean headset = audioManager.isWiredHeadsetOn();
        int selectedMicId = sharedPrefs.getInt(Preferences.PREF_SOUNDSYSTEM_MICROPHONE_DEVICE, -1);

        // 1. Set Mode (Use MODE_IN_COMMUNICATION only when voice processing / AEC is requested, avoiding forced DSP noise reduction)
        int targetMode = voiceProcessing ? AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL;
        if (audioManager.getMode() != targetMode) {
            audioManager.setMode(targetMode);
        }

        // 2. Request Audio Focus with matching stream
        int streamType = voiceProcessing ? AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC;
        audioManager.requestAudioFocus(focusChange -> {}, streamType, AudioManager.AUDIOFOCUS_GAIN);

        // 3. Apply custom selected microphone device if API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (selectedMicId != -1) {
                AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
                AudioDeviceInfo targetDev = null;
                for (AudioDeviceInfo dev : devices) {
                    if (dev.getId() == selectedMicId) {
                        targetDev = dev;
                        break;
                    }
                }
                if (targetDev != null) {
                    audioManager.setCommunicationDevice(targetDev);
                    Log.d(TAG, "Audio Routing applied custom communication device: " + targetDev.getProductName());
                    return;
                }
            } else {
                audioManager.clearCommunicationDevice();
            }
        }

        // 4. Fallback Routing (Speaker vs Earpiece)
        if (voiceProcessing && !headset) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AudioDeviceInfo speaker = null;
                for (AudioDeviceInfo device : audioManager.getAvailableCommunicationDevices()) {
                    if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                        speaker = device;
                        break;
                    }
                }
                if (speakerphone && speaker != null) {
                    audioManager.setCommunicationDevice(speaker);
                } else {
                    audioManager.clearCommunicationDevice();
                }
            } else {
                audioManager.setSpeakerphoneOn(speakerphone);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && selectedMicId == -1) {
                audioManager.clearCommunicationDevice();
            }
            audioManager.setSpeakerphoneOn(speakerphone && !headset);
        }
        
        Log.d(TAG, "Audio Routing Updated: Mode=" + targetMode + ", Speaker=" + speakerphone + ", Stream=" + streamType);
    }

    private void setupAudioPreprocessor() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean localVoiceProcessing = prefs.getBoolean(Preferences.PREF_SOUNDSYSTEM_VOICEPROCESSING, false);
        
        updateAudioRouting();

        if (localVoiceProcessing || (mychannel != null && mychannel.audiocfg.bEnableAGC)) {
            AudioPreprocessor ap = new AudioPreprocessor(AudioPreprocessorType.WEBRTC_AUDIOPREPROCESSOR, true);
            
            // AEC (Acoustic Echo Cancellation)
            ap.webrtc.echocanceller.bEnable = localVoiceProcessing;
            
            // NS (Noise Suppression)
            ap.webrtc.noisesuppression.bEnable = localVoiceProcessing;
            ap.webrtc.noisesuppression.nLevel = 2; // High suppression
            
            // AGC (Auto Gain Control)
            if (mychannel != null && mychannel.audiocfg.bEnableAGC) {
                ap.webrtc.gaincontroller2.bEnable = true;
                float gainPercent = mychannel.audiocfg.nGainLevel / (float) TeamTalkConstants.CHANNEL_AUDIOCONFIG_MAX;
                ap.webrtc.gaincontroller2.fixeddigital.fGainDB = WebRTCConstants.WEBRTC_GAINCONTROLLER2_FIXEDGAIN_MAX
                        * gainPercent;
            } else {
                ap.webrtc.gaincontroller2.bEnable = false;
                ap.webrtc.gaincontroller2.fixeddigital.fGainDB = 0; // No extra gain if not requested by channel
            }
            
            ttclient.setSoundInputPreprocess(ap);
            int gain = prefs.getInt(Preferences.PREF_SOUNDSYSTEM_MICROPHONEGAIN, 1300);
            ttclient.setSoundInputGainLevel(gain);
            Log.d(TAG, "WebRTC Audio Preprocessor enabled: AEC=" + ap.webrtc.echocanceller.bEnable + ", NS=" + ap.webrtc.noisesuppression.bEnable + ", Gain=" + gain);
        } else {
            ttclient.setSoundInputPreprocess(new AudioPreprocessor());
            int gain = prefs.getInt(Preferences.PREF_SOUNDSYSTEM_MICROPHONEGAIN, 1300);
            ttclient.setSoundInputGainLevel(gain);
            Log.d(TAG, "Audio Preprocessor disabled (raw input), Gain=" + gain);
        }
    }

    @Override
    public void onConnectSuccess() {
        isLoggingOut = false;

        assert (ttserver != null);

        if (Utils.isWebLogin(ttserver.username)) {
            new WebLoginAccessToken().execute();
        } else {
            login();
        }
        connectionLost = false;
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    @Override
    public void onEncryptionError(int opensslErrorNo, ClientErrorMsg errmsg) {
        Log.i(TAG, "Encryption error: " + errmsg.szErrorMsg + " connecting to " + ttserver.ipaddr + ":"
                + ttserver.tcpport);
        Toast.makeText(this, localizedContext.getString(R.string.text_con_encryption_error, errmsg.szErrorMsg),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectFailed() {

        Log.i(TAG, "Failed to connect " + ttserver.ipaddr + ":" + ttserver.tcpport);

        Toast.makeText(this, localizedContext.getString(R.string.text_con_failed),
                Toast.LENGTH_SHORT).show();

        createReconnectTimer(5000);
    }

    @Override
    public void onConnectionLost() {
        if (isLoggingOut || connectionLost) return;
        connectionLost = true;

        String ttsMsg = localizedContext.getString(R.string.text_con_lost);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        if (prefs.getBoolean("server_lost_tts_checkbox", true)) {
            speakAndLog(ttsMsg, MyTextMessage.MSGTYPE_LOG_INFO);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            playSound(SOUND_SERVERLOST);
            // Don't call resetState(false) as it stops foreground service and clears everything
            // Instead, just disconnect and clear transient state
            if (ttclient != null) ttclient.disconnect();
            activecmds.clear();
            displayNotification(true); // Update notification to show disconnected/reconnecting
        }, 200);

        if (ttserver != null) {
            Log.i(TAG, "Connection lost to " + ttserver.ipaddr + ":" + ttserver.tcpport);
            createReconnectTimer(5000);
        }
    }

    @Override
    public void onCmdError(int cmdId, ClientErrorMsg errmsg) {
        mainHandler.post(() -> Utils.notifyError(this, errmsg));

        if (activecmds.get(cmdId) == CmdComplete.CMD_COMPLETE_LOGIN) {
            reconnectHandler.removeCallbacks(reconnectTimer);
        }
    }

    @Override
    public void onCmdSuccess(int cmdId) {
        if (activecmds.get(cmdId) == CmdComplete.CMD_COMPLETE_LOGIN) {

            reconnectHandler.removeCallbacks(reconnectTimer);

            displayNotification(true);
        }
    }

    @Override
    public void onCmdProcessing(int cmdId, boolean complete) {

        if (!complete) {
            currentProcessingCmdId = cmdId;
            switch (activecmds.get(cmdId, CMD_COMPLETE_NONE)) {
                case CMD_COMPLETE_LOGIN:

                    users.clear();
                    remoteFiles.clear();
                    fileTransfers.clear();
                    channels.clear();
                    break;
            }
        } else {
            switch (activecmds.get(cmdId, CMD_COMPLETE_NONE)) {
                case CMD_COMPLETE_LOGIN: {
                    loginComplete();
                }
                    break;
                case CMD_COMPLETE_JOIN:
                    isLoggingIn = false;
                    break;
            }
            activecmds.delete(cmdId);
            currentProcessingCmdId = 0;
        }
    }

    @Override
    public void onCmdMyselfLoggedOut() {
        if (isLoggingOut) return;
        isLoggingOut = true;

        handleLeaveEventBeforeDisconnect();

        reconnectHandler.removeCallbacks(reconnectTimer);
        if (ttclient != null)
            ttclient.disconnect();

        String ttsMsg = localizedContext.getString(R.string.text_tts_myself_loggedout, currentServerName);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("server_logout_checkbox", true)) {
            speakAndLog(ttsMsg, MyTextMessage.MSGTYPE_LOG_INFO);
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            playSound(SOUND_USERLOGGEDOFF);
            resetState(true);
        }, 200);
    }

    @Override
    public void onCmdMyselfLoggedIn(int my_userid, UserAccount useraccount) {
        myUserID = my_userid;
        isLoggingIn = true;
        playSound(SOUND_USERLOGGEDIN);

        ServerProperties prop = new ServerProperties();
        if (ttclient.getServerProperties(prop) && !TextUtils.isEmpty(prop.szServerName)) {
            currentServerName = prop.szServerName;
        }

        String ttsMsg = localizedContext.getString(R.string.text_tts_myself_loggedin, currentServerName);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("server_login_checkbox", true)) {
            speakAndLog(ttsMsg, MyTextMessage.MSGTYPE_LOG_INFO);
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int statusmode = TeamTalkConstants.STATUSMODE_AVAILABLE;
        String statusmsg = ttserver.statusmsg;

        if (TextUtils.isEmpty(statusmsg)) {
            statusmsg = prefs.getString(Preferences.PREF_GENERAL_STATUSMSG, "");
        }

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

        ttclient.doChangeStatus(statusmode, statusmsg);

        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    @Override
    public void onCmdMyselfKickedFromChannel(int channelID) {
        // Bug fix: based on Qt Client, if channelID (nSource) is 0, it's a server kick.
        if (channelID == 0) {
            kickedFromServer = true;
        } else {
            kickedFromServer = false;
        }
    }

    @Override
    public void onCmdMyselfKickedFromChannel(int channelID, User kicker) {
        users.put(kicker.nUserID, kicker);
        // Kick from server: channelID (nSource) is 0
        if (channelID == 0) {
            kickedFromServer = true;
        } else {
            kickedFromServer = false;
        }
    }

    // Chame este método quando detectar expulsão do servidor (exemplo: via comando
    // kick com channelID=0)
    public void setKickedFromServer() {
        kickedFromServer = true;
    }

    public boolean wasKickedFromServer() {
        return kickedFromServer;
    }

    @Override
    public void onCmdUserLoggedIn(User user) {
        if (user.nUserID == myUserID) return; // Myself login handled in onCmdMyselfLoggedIn
        users.put(user.nUserID, user);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int def_unsub = Subscription.SUBSCRIBE_NONE;
        if (!pref.getBoolean(Preferences.PREF_SUB_TEXTMESSAGE, true))
            def_unsub |= Subscription.SUBSCRIBE_USER_MSG;
        if (!pref.getBoolean(Preferences.PREF_SUB_CHANMESSAGE, true))
            def_unsub |= Subscription.SUBSCRIBE_CHANNEL_MSG;
        if (!pref.getBoolean(Preferences.PREF_SUB_BCAST_MESSAGES, true))
            def_unsub |= Subscription.SUBSCRIBE_BROADCAST_MSG;
        if (!pref.getBoolean(Preferences.PREF_SUB_VOICE, true))
            def_unsub |= Subscription.SUBSCRIBE_VOICE;
        if (!pref.getBoolean(Preferences.PREF_SUB_VIDCAP, true))
            def_unsub |= Subscription.SUBSCRIBE_VIDEOCAPTURE;
        if (!pref.getBoolean(Preferences.PREF_SUB_DESKTOP, true))
            def_unsub |= Subscription.SUBSCRIBE_DESKTOP;
        if (!pref.getBoolean(Preferences.PREF_SUB_MEDIAFILE, true))
            def_unsub |= Subscription.SUBSCRIBE_MEDIAFILE;

        if ((user.uLocalSubscriptions & def_unsub) != 0) {
            int cmdid = ttclient.doUnsubscribe(user.nUserID, def_unsub);
            if (cmdid > 0)
                activecmds.put(cmdid, CmdComplete.CMD_COMPLETE_UNSUBSCRIBE);
        }

        String name = Utils.getDisplayName(localizedContext, user);
        String msgContent = localizedContext.getString(R.string.text_log_user_loggedin, name);

        // Suppress announcements for the initial user list delivered right after own login
        if (!isLoggingIn) {
            if (pref.getBoolean("user_login_checkbox", true)) {
                speakAndLog(msgContent, MyTextMessage.MSGTYPE_LOG_INFO);
            } else {
                MyTextMessage msg = MyTextMessage.createLogMsg(MyTextMessage.MSGTYPE_LOG_INFO, msgContent);
                getChatLogTextMsgs().add(msg);
            }
            playSound(SOUND_USERLOGGEDIN);
        } else {
            // Still log but don't speak/play sound during initial sync for other users
            MyTextMessage msg = MyTextMessage.createLogMsg(MyTextMessage.MSGTYPE_LOG_INFO, msgContent);
            getChatLogTextMsgs().add(msg);
        }

        syncFromUserCache(user);
    }

    @Override
    public void onCmdUserLoggedOut(User user) {
        users.remove(user.nUserID);

        String name = Utils.getDisplayName(localizedContext, user);
        String msgContent = localizedContext.getString(R.string.text_log_user_loggedout, name);
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (pref.getBoolean("user_logout_checkbox", true)) {
            speakAndLog(msgContent, MyTextMessage.MSGTYPE_LOG_INFO);
        } else {
            MyTextMessage msg = MyTextMessage.createLogMsg(MyTextMessage.MSGTYPE_LOG_INFO, msgContent);
            getChatLogTextMsgs().add(msg);
        }

        playSound(SOUND_USERLOGGEDOFF);

        syncToUserCache(user);
    }

    @Override
    public void onCmdUserUpdate(User user) {
        users.put(user.nUserID, user);
    }

    @Override
    public void onCmdUserJoinedChannel(User user) {
        users.put(user.nUserID, user);
        if (ttserver.rememberLastChannel && (user.nUserID == myUserID)) {
            ttserver.channel = ttclient.getChannelPath(user.nChannelID);
            if (joinchannel != null && joinchannel.nChannelID == user.nChannelID) {
                ttserver.chanpasswd = joinchannel.szPassword;
            }
            saveServerChannel();
        }

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (user.nUserID == myUserID) {

            setMyChannel(getChannels().get(user.nChannelID));
            displayNotification(true);

            if (mychannel != null) {
                String chanName = getChannelDisplayName(mychannel);
                MyTextMessage msg;
                msg = MyTextMessage.createLogMsg(MyTextMessage.MSGTYPE_LOG_INFO,
                        localizedContext.getString(R.string.text_log_joined_channel, chanName));
                getChatLogTextMsgs().add(msg);

                // No longer suppressing root channel joined announcement during login startup
                // User wants to hear all channel entries
                boolean ttsJoinEnabled = pref.getBoolean("pref_tts_myself_join", true);
                if (mychannel.nChannelID != lastJoinedChannelID) {
                    lastJoinedChannelID = mychannel.nChannelID;
                    if (ttsJoinEnabled) {
                        speakAndLog(msg.szMessage, MyTextMessage.MSGTYPE_LOG_INFO);
                    }
                    playSound(SOUND_USERJOIN_MYSELF);
                }
            }
        } else if (mychannel != null && mychannel.nChannelID == user.nChannelID) {

            String name = Utils.getDisplayName(localizedContext, user);
            String msgContent = localizedContext.getString(R.string.text_log_user_joined_channel, name);
            if (!isLoggingIn && pref.getBoolean("channel_join_checkbox", true)) {
                speakAndLog(msgContent, MyTextMessage.MSGTYPE_LOG_INFO);
                playSound(SOUND_USERJOIN);
            } else {
                MyTextMessage msg = MyTextMessage.createLogMsg(MyTextMessage.MSGTYPE_LOG_INFO, msgContent);
                getChatLogTextMsgs().add(msg);
                if (!isLoggingIn) {
                    playSound(SOUND_USERJOIN);
                }
            }
        }

        int mf_volume = pref.getInt(Preferences.PREF_SOUNDSYSTEM_MEDIAFILE_VOLUME, 50);
        mf_volume = Utils.refVolume(mf_volume);
        ttclient.setUserVolume(user.nUserID, StreamType.STREAMTYPE_MEDIAFILE_AUDIO, mf_volume);
        ttclient.pumpMessage(ClientEvent.CLIENTEVENT_USER_STATECHANGE, user.nUserID);

        if (user.nUserID != myUserID && !isLoggingIn) {
            if (mychannel != null && mychannel.nChannelID == user.nChannelID) {
                // User joined MY channel — play sound and announce
                // Sound and speech already handled above in the mychannel check for users other than me
            } else if (pref.getBoolean("all_users_channel_movement_checkbox", true)) {
                // User joined a different channel — TTS only, no sound
                String name = Utils.getDisplayName(localizedContext, user);
                Channel chan = getChannels().get(user.nChannelID);
                String chanName = getChannelDisplayName(chan);
                if (chan == null) {
                    chanName = localizedContext.getString(R.string.pref_title_channel) + " " + user.nChannelID;
                }
                if (!TextUtils.isEmpty(chanName)) {
                    speakAndLog(localizedContext.getString(R.string.text_tts_user_joined_channel, name, chanName), MyTextMessage.MSGTYPE_LOG_INFO);
                }
            }
        }

        if (!UserCached.getCacheID(user).isEmpty()) {
            UserAccount myaccount = new UserAccount();
            if (ttclient.getMyUserAccount(myaccount)
                    && (myaccount.uUserRights & UserRight.USERRIGHT_VIEW_ALL_USERS) == UserRight.USERRIGHT_NONE) {

                syncFromUserCache(user);
            }
        }
    }

    @Override
    public void onCmdUserLeftChannel(int channelid, User user) {
        if (users.containsKey(user.nUserID)) {
            users.put(user.nUserID, user);
        }

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (mychannel != null && mychannel.nChannelID == channelid) {
            // User left MY channel
            Channel chan = getChannels().get(channelid);
            String chanName = getChannelDisplayName(chan);
            MyTextMessage msg;
            if (user.nUserID == myUserID) {
                msg = MyTextMessage.createLogMsg(MyTextMessage.MSGTYPE_LOG_INFO,
                        localizedContext.getString(R.string.text_log_left_channel, chanName));
            } else {
                String name = Utils.getDisplayName(localizedContext, user);
                msg = MyTextMessage.createLogMsg(MyTextMessage.MSGTYPE_LOG_INFO,
                        localizedContext.getString(R.string.text_log_user_left_channel, name));
            }
            if (user.nUserID == myUserID) {
                lastJoinedChannelID = -1;
                if (pref.getBoolean("pref_tts_myself_leave", true)) {
                    speakAndLog(msg.szMessage, MyTextMessage.MSGTYPE_LOG_INFO);
                } else {
                    getChatLogTextMsgs().add(msg);
                }
                playSound(SOUND_USERLEFT_MYSELF);
            } else {
                // Sound only when another user leaves MY channel
                playSound(SOUND_USERLEFT);
                if (pref.getBoolean("channel_leave_checkbox", true)) {
                    speakAndLog(msg.szMessage, MyTextMessage.MSGTYPE_LOG_INFO);
                } else {
                    getChatLogTextMsgs().add(msg);
                }
            }
        } else if (user.nUserID != myUserID && pref.getBoolean("all_users_channel_movement_checkbox", true)) {
            // User left a DIFFERENT channel — TTS only, no sound
            Channel chan = getChannels().get(channelid);
            String chanName = getChannelDisplayName(chan);
            if (chan == null) {
                chanName = localizedContext.getString(R.string.pref_title_channel) + " " + channelid;
            }
            if (!TextUtils.isEmpty(chanName)) {
                String name = Utils.getDisplayName(localizedContext, user);
                String ttsMsg = localizedContext.getString(R.string.text_tts_user_left_channel, name, chanName);
                speakAndLog(ttsMsg, MyTextMessage.MSGTYPE_LOG_INFO);
            }
        }

        if (user.nUserID == myUserID) {
            setMyChannel(null);
        }
        String cacheid = UserCached.getCacheID(user);
        if (!cacheid.isEmpty()) {
            UserAccount myaccount = new UserAccount();
            if (ttclient.getMyUserAccount(myaccount)
                    && (myaccount.uUserRights & UserRight.USERRIGHT_VIEW_ALL_USERS) == UserRight.USERRIGHT_NONE) {
                syncToUserCache(user);
            }
        }
    }

    private void saveServerChannel() {
        if (ttserver == null) return;
        
        SharedPreferences pref = getSharedPreferences("serverlist", MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();
        
        int i = 0;
        while (true) {
            String ip = pref.getString(i + ServerEntry.KEY_IPADDR, "");
            int port = pref.getInt(i + ServerEntry.KEY_TCPPORT, 0);
            
            if (ip.isEmpty()) break;
            
            if (ip.equals(ttserver.ipaddr) && port == ttserver.tcpport) {
                edit.putString(i + ServerEntry.KEY_CHANNEL, ttserver.channel);
                edit.putString(i + ServerEntry.KEY_CHANPASSWD, ttserver.chanpasswd);
                edit.apply();
                break;
            }
            i++;
        }
    }

    public void speakAndLog(String ttsMsg, int logType) {
        if (ttsWrapper != null) {
            ttsWrapper.speak(ttsMsg);
        }
        MyTextMessage msg = MyTextMessage.createLogMsg(logType, ttsMsg);
        getChatLogTextMsgs().add(msg);
    }

    @Override
    public void onCmdUserTextMessage(TextMessage textmessage) {

        User user = getUsers().get(textmessage.nFromUserID);
        MyTextMessage newmsg = new MyTextMessage(textmessage,
                user == null ? "" : Utils.getDisplayName(localizedContext, user));

        switch (textmessage.nMsgType) {
            case TextMsgType.MSGTYPE_USER: {
                getUserTextMsgs(textmessage.nFromUserID).add(newmsg);
                break;
            }
            case TextMsgType.MSGTYPE_BROADCAST: {
                getChatLogTextMsgs().add(newmsg);
                break;
            }
            case TextMsgType.MSGTYPE_CHANNEL: {
                getChatLogTextMsgs().add(newmsg);
                break;
            }
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean fromMyself = (textmessage.nFromUserID == myUserID || textmessage.nFromUserID == ttclient.getMyUserID());
        String name = (user != null) ? Utils.getDisplayName(localizedContext, user) : "";

        if (ttsWrapper != null) {
            switch (textmessage.nMsgType) {
                case TextMsgType.MSGTYPE_CHANNEL:
                    if (fromMyself) {
                        if (prefs.getBoolean("channel_message_sent_checkbox", true)) {
                            String ttsMsg = localizedContext.getString(R.string.text_tts_channel_message_sent, textmessage.szMessage);
                            speakAndLog(ttsMsg, MyTextMessage.MSGTYPE_LOG_INFO);
                        }
                    } else {
                        boolean inChannel = (mychannel != null && mychannel.nChannelID == textmessage.nChannelID);
                        boolean isMarked = (markedChannelID != 0 && markedChannelID == textmessage.nChannelID);
                        if ((prefs.getBoolean("channel_message_checkbox", true) && inChannel) ||
                            (prefs.getBoolean("subscription_channel_msg_checkbox", true) && isMarked)) {
                            String ttsMsg = localizedContext.getString(R.string.text_tts_channel_message, name, textmessage.szMessage);
                            speakAndLog(ttsMsg, MyTextMessage.MSGTYPE_LOG_INFO);
                        }
                    }
                    break;
                case TextMsgType.MSGTYPE_BROADCAST:
                    if (!fromMyself && prefs.getBoolean("broadcast_message_checkbox", true)) {
                        String broadcastMsg = localizedContext.getString(R.string.text_tts_broadcast_message, name, textmessage.szMessage);
                        speakAndLog(broadcastMsg, MyTextMessage.MSGTYPE_LOG_INFO);
                    }
                    break;
                case TextMsgType.MSGTYPE_USER:
                    if (!fromMyself && prefs.getBoolean("private_message_checkbox", true)) {
                        if (!prefs.getBoolean("pref_auto_popup_private_msg", false)) {
                            String privateMsg = localizedContext.getString(R.string.text_tts_private_message, name, textmessage.szMessage);
                            speakAndLog(privateMsg, MyTextMessage.MSGTYPE_LOG_INFO);
                        }
                    }
                    break;
            }
        }

        // Background Sound
        switch (fromMyself ? -1 : textmessage.nMsgType) {
            case TextMsgType.MSGTYPE_CHANNEL:
                playSound(SOUND_CHANMSG);
                break;
            case TextMsgType.MSGTYPE_USER:
                playSound(SOUND_USERMSG);
                break;
            case TextMsgType.MSGTYPE_BROADCAST:
                playSound(SOUND_BCASTMSG);
                break;
        }
    }

    @Override
    public void onCmdChannelNew(Channel channel) {
        channels.put(channel.nChannelID, channel);

        // Auto-join if this is the target channel from the link/file
        if (mychannel == null && ttserver != null && ttserver.channel != null && !ttserver.channel.isEmpty()) {
            if (ttclient.getChannelPath(channel.nChannelID).equalsIgnoreCase(ttserver.channel)) {
                joinchannel = channel;
                joinchannel.szPassword = ttserver.chanpasswd;
                int cmdid = ttclient.doJoinChannel(joinchannel);
                activecmds.put(cmdid, CmdComplete.CMD_COMPLETE_JOIN);
            }
        }
    }

    @Override
    public void onCmdChannelUpdate(Channel channel) {
        channels.put(channel.nChannelID, channel);

        if (mychannel != null && mychannel.nChannelID == channel.nChannelID) {
            setMyChannel(channel);
        }
    }

    @Override
    public void onCmdChannelRemove(Channel channel) {
        channels.remove(channel.nChannelID);
    }

    @Override
    public void onCmdServerUpdate(ServerProperties serverproperties) {
        if (!TextUtils.isEmpty(serverproperties.szServerName)) {
            currentServerName = serverproperties.szServerName;
        }
        MyTextMessage msg;
        msg = MyTextMessage.createUserDefMsg(MyTextMessage.MSGTYPE_SERVERPROP,
                serverproperties);
        getChatLogTextMsgs().add(msg);
    }

    @Override
    public void onCmdFileNew(RemoteFile remotefile) {
        remoteFiles.put(remotefile.nFileID, remotefile);
        
        CmdComplete cmd = activecmds.get(currentProcessingCmdId, CMD_COMPLETE_NONE);
        boolean isSyncing = (cmd == CmdComplete.CMD_COMPLETE_LOGIN || cmd == CmdComplete.CMD_COMPLETE_JOIN);
        
        if (mychannel != null && remotefile.nChannelID == mychannel.nChannelID && !isSyncing && !isLoggingIn) {
            playSound(SOUND_FILESUPDATE);
        }
    }

    @Override
    public void onCmdFileRemove(RemoteFile remotefile) {
        remoteFiles.remove(remotefile.nFileID);
        
        if (mychannel != null && remotefile.nChannelID == mychannel.nChannelID) {
            playSound(SOUND_FILESUPDATE);
        }
    }

    @Override
    public void onUserStateChange(User user) {
        users.put(user.nUserID, user);
    }

    @Override
    public void onVoiceActivation(boolean bVoiceActive) {
        adjustMuteOnTx(bVoiceActive);
        playSound(bVoiceActive ? SOUND_VOXON : SOUND_VOXOFF);
    }

    @Override
    public void onFileTransfer(FileTransfer transfer) {
        if (transfer.nStatus == FileTransferStatus.FILETRANSFER_ACTIVE) {
            fileTransfers.put(transfer.nTransferID, transfer);
        } else {
            fileTransfers.remove(transfer.nTransferID);
        }
    }

    @Override
    public void onStreamMediaFile(MediaFileInfo mediafileinfo) {
        User myself = new User();
        if (!ttclient.getUser(myUserID != 0 ? myUserID : ttclient.getMyUserID(), myself))
            return;

        switch (mediafileinfo.nStatus) {
            case MediaFileStatus.MFS_STARTED:
                ttclient.doChangeStatus(myself.nStatusMode | TeamTalkConstants.STATUSMODE_STREAM_MEDIAFILE,
                        myself.szStatusMsg);
                break;
            case MediaFileStatus.MFS_ERROR:
                Toast.makeText(this, R.string.err_stream_media, Toast.LENGTH_LONG).show();
            case MediaFileStatus.MFS_ABORTED:
            case MediaFileStatus.MFS_FINISHED:
            case MediaFileStatus.MFS_CLOSED:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                String statusmsg = prefs.getString(Preferences.PREF_GENERAL_STATUSMSG, "");
                ttclient.doChangeStatus(myself.nStatusMode & ~TeamTalkConstants.STATUSMODE_STREAM_MEDIAFILE,
                        statusmsg);
                break;
            case MediaFileStatus.MFS_PLAYING:
            case MediaFileStatus.MFS_PAUSED:
            default:
                break;
        }
    }

    public void stopStreamingMediaFile() {
        if (ttclient == null) return;

        ttclient.stopStreamingMediaFileToChannel();

        User myself = new User();
        if (ttclient.getUser(myUserID != 0 ? myUserID : ttclient.getMyUserID(), myself)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String statusmsg = prefs.getString(Preferences.PREF_GENERAL_STATUSMSG, "");
            ttclient.doChangeStatus(myself.nStatusMode & ~TeamTalkConstants.STATUSMODE_STREAM_MEDIAFILE,
                    statusmsg);
        }
    }

    @Override
    public void onHeadsetConnected() {
        bluetoothHeadsetHelper.scoAudioConnect();
    }

    @Override
    public void onHeadsetDisconnected() {
        bluetoothHeadsetHelper.scoAudioDisconnect();
    }

    class WebLoginAccessToken extends AsyncTask<Void, Void, Void> {

        String username = "", token = "", accesstoken = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

            this.username = prefs.getString(Preferences.PREF_GENERAL_BEARWARE_USERNAME, "");
            this.token = prefs.getString(Preferences.PREF_GENERAL_BEARWARE_TOKEN, "");

            ServerProperties srvprop = new ServerProperties();
            if (ttclient.getServerProperties(srvprop))
                accesstoken = srvprop.szAccessToken;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (username.length() > 0) {
                ttserver.username = this.username;
                login();
            } else {
                Toast.makeText(TeamTalkService.this, localizedContext.getString(R.string.text_weblogin_authfailure),
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String xml = Utils.getURL(AppInfo.getBearWareAccessTokenUrl(getBaseContext(),
                    this.username, this.token, accesstoken));
            Log.d(AppInfo.TAG, xml);

            try {
                InputSource src = new InputSource(new StringReader(xml));
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document document = db.parse(src);
                XPathFactory factory = XPathFactory.newInstance();
                XPath xPath = factory.newXPath();

                this.username = (String) xPath.evaluate("/teamtalk/bearware/username", document, XPathConstants.STRING);

            } catch (XPathExpressionException e) {
                Log.e(AppInfo.TAG, "XPath failed: " + e);
            } catch (ParserConfigurationException e) {
                Log.e(AppInfo.TAG, "Parser cfg failed: " + e);
            } catch (IOException e) {
                Log.e(AppInfo.TAG, "XML IOException: " + e);
            } catch (SAXException e) {
                Log.e(AppInfo.TAG, "XML SAXException: " + e);
            }

            return null;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("pref_language".equals(key)) {
            String newLang = sharedPreferences.getString(key, "default");
            Log.d(TAG, "Language changed to: " + newLang);

            // Update service context first so future getString() calls and TTS initialization
            // use the correctly localized resources.
            localizedContext = LocaleHelper.onAttach(this);
            LocaleHelper.onAttach(getApplicationContext());

            // Reinitialize the TTS engine so the new locale's voice is loaded immediately.
            // A simple setLanguage() call is not enough — Android TTS may keep the old voice
            // until the engine is restarted.
            if (ttsWrapper != null) {
                boolean useAnnouncements = ttsWrapper.useAnnouncements;
                boolean useA11yStream = sharedPreferences.getBoolean("pref_a11y_volume", false);
                String engine = sharedPreferences.getString("pref_speech_engine", dk.bearware.data.TTSWrapper.defaultEngineName);
                
                // Use reinitialize() so the existing TTSWrapper instance remains valid,
                // preventing stale references in MainActivity or other components.
                ttsWrapper.reinitialize(localizedContext, engine);
                ttsWrapper.useAnnouncements = useAnnouncements;
                ttsWrapper.setAccessibilityStream(useA11yStream);
            }

            // Refresh the foreground notification with the new language
            if (ttserver != null && (ttclient != null && (ttclient.getFlags() & dk.bearware.ClientFlag.CLIENT_CONNECTED) != 0)) {
                displayNotification(true);
            }
        } else if (Preferences.PREF_TTS_LANGUAGE_BEHAVIOR.equals(key)) {
            Log.d(TAG, "TTS language behavior changed");
            if (ttsWrapper != null) {
                boolean useAnnouncements = ttsWrapper.useAnnouncements;
                boolean useA11yStream = sharedPreferences.getBoolean("pref_a11y_volume", false);
                String engine = sharedPreferences.getString("pref_speech_engine", dk.bearware.data.TTSWrapper.defaultEngineName);
                
                ttsWrapper.reinitialize(localizedContext, engine);
                ttsWrapper.useAnnouncements = useAnnouncements;
                ttsWrapper.setAccessibilityStream(useA11yStream);
            }
        } else if (key.equals(Preferences.PREF_SOUNDSYSTEM_MEDIAFILE_VOLUME)) {
            Log.d(TAG, "Media file volume preference changed");
            int vol_percent = sharedPreferences.getInt(
                    Preferences.PREF_SOUNDSYSTEM_MEDIAFILE_VOLUME,
                    SoundLevel.SOUND_VOLUME_DEFAULT);
            int ref_volume = Utils.refVolume(vol_percent);

            // Use local users map which is kept in sync via events
            for (User user : users.values()) {
                ttclient.setUserVolume(user.nUserID,
                        StreamType.STREAMTYPE_MEDIAFILE_AUDIO, ref_volume);
            }
        } else if (key.equals(Preferences.PREF_SOUNDSYSTEM_VOICEPROCESSING)) {
            Log.d(TAG, "Voice processing preference changed");
            setupAudioPreprocessor();
        } else if (key.equals("pref_speech_engine")) {
            String engine = sharedPreferences.getString("pref_speech_engine", dk.bearware.data.TTSWrapper.defaultEngineName);
            if (ttsWrapper != null) ttsWrapper = ttsWrapper.switchEngine(engine);
        } else if (key.equals("pref_use_announcements")) {
            if (ttsWrapper != null) ttsWrapper.useAnnouncements = sharedPreferences.getBoolean("pref_use_announcements", false);
        } else if (key.equals("pref_a11y_volume")) {
            if (ttsWrapper != null) ttsWrapper.setAccessibilityStream(sharedPreferences.getBoolean("pref_a11y_volume", false));
        } else if (key.equals("pref_sound_pack") || key.equals("pref_sound_accessibility_volume")) {
            // Rebuild SoundPool with sounds from the newly selected pack or stream
            SoundPool oldPool = audioIcons;
            boolean useA11yAudio = sharedPreferences.getBoolean("pref_sound_accessibility_volume", false);
            SoundPool newPool = buildSoundPool(useA11yAudio);
            loadedSounds.clear();
            newPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
                if (status == 0) loadedSounds.add(sampleId);
            });
            audioIcons = newPool;
            soundExecutor.execute(() -> {
                reloadAllSounds(newPool, false);
                mainHandler.post(() -> {
                    if (oldPool != null) oldPool.release();
                });
            });
        }
    }
}