
package dk.bearware.gui;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import androidx.appcompat.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.provider.MediaStore;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.net.Uri;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.google.gson.Gson;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import dk.bearware.backend.TeamTalkService;
import dk.bearware.AudioCodec;
import dk.bearware.AudioConfig;
import dk.bearware.Channel;
import dk.bearware.ChannelType;
import dk.bearware.ClientError;
import dk.bearware.ClientErrorMsg;
import dk.bearware.Constants;
import dk.bearware.FileTransfer;
import dk.bearware.RemoteFile;
import dk.bearware.SoundLevel;
import dk.bearware.StreamType;
import dk.bearware.Subscription;
import dk.bearware.TeamTalkBase;
import dk.bearware.User;
import dk.bearware.data.AppInfo;
import dk.bearware.data.Preferences;
import dk.bearware.data.ServerEntry;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static final String TAG = "bearware";

    private static final Map<Integer, Integer> errorMessages = new HashMap<>();

    static {
        errorMessages.put(ClientError.CMDERR_INVALID_USERNAME, R.string.err_invalid_username);
        errorMessages.put(ClientError.CMDERR_INCORRECT_SERVER_PASSWORD, R.string.err_incorrect_server_password);
        errorMessages.put(ClientError.CMDERR_INCORRECT_CHANNEL_PASSWORD, R.string.err_incorrect_channel_password);
        errorMessages.put(ClientError.CMDERR_INVALID_ACCOUNT, R.string.err_invalid_account);
        errorMessages.put(ClientError.CMDERR_MAX_SERVER_USERS_EXCEEDED, R.string.err_max_server_users_exceeded);
        errorMessages.put(ClientError.CMDERR_MAX_CHANNEL_USERS_EXCEEDED, R.string.err_max_channel_users_exceeded);
        errorMessages.put(ClientError.CMDERR_SERVER_BANNED, R.string.err_server_banned);
        errorMessages.put(ClientError.CMDERR_NOT_AUTHORIZED, R.string.err_not_authorized);
        errorMessages.put(ClientError.CMDERR_MAX_DISKUSAGE_EXCEEDED, R.string.err_max_diskusage_exceeded);
        errorMessages.put(ClientError.CMDERR_INCORRECT_OP_PASSWORD, R.string.err_incorrect_op_password);
        errorMessages.put(ClientError.CMDERR_MAX_LOGINS_PER_IPADDRESS_EXCEEDED, R.string.err_max_logins_per_ipaddress_exceeded);
        errorMessages.put(ClientError.CMDERR_MAX_CHANNELS_EXCEEDED, R.string.err_max_channels_exceeded);
        errorMessages.put(ClientError.CMDERR_CHANNEL_ALREADY_EXISTS, R.string.err_channel_already_exists);
        errorMessages.put(ClientError.CMDERR_USER_NOT_FOUND, R.string.err_user_not_found);
        errorMessages.put(ClientError.CMDERR_OPENFILE_FAILED, R.string.err_openfile_failed);
        errorMessages.put(ClientError.CMDERR_FILESHARING_DISABLED, R.string.err_filesharing_disabled);
        errorMessages.put(ClientError.CMDERR_CHANNEL_HAS_USERS, R.string.err_channel_has_users);
        errorMessages.put(ClientError.CMDERR_SYNTAX_ERROR, R.string.err_syntax_error);
        errorMessages.put(ClientError.CMDERR_UNKNOWN_COMMAND, R.string.err_unknown_command);
        errorMessages.put(ClientError.CMDERR_MISSING_PARAMETER, R.string.err_missing_parameter);
        errorMessages.put(ClientError.CMDERR_INCOMPATIBLE_PROTOCOLS, R.string.err_incompatible_protocols);
        errorMessages.put(ClientError.CMDERR_UNKNOWN_AUDIOCODEC, R.string.err_unknown_audiocodec);
        errorMessages.put(ClientError.CMDERR_AUDIOCODEC_BITRATE_LIMIT_EXCEEDED, R.string.err_audiocodec_bitrate_limit_exceeded);
        errorMessages.put(ClientError.CMDERR_COMMAND_FLOOD, R.string.err_command_flood);
        errorMessages.put(ClientError.CMDERR_CHANNEL_BANNED, R.string.err_channel_banned);
        errorMessages.put(ClientError.CMDERR_MAX_FILETRANSFERS_EXCEEDED, R.string.err_max_filetransfers_exceeded);
        errorMessages.put(ClientError.CMDERR_NOT_LOGGEDIN, R.string.err_not_loggedin);
        errorMessages.put(ClientError.CMDERR_ALREADY_LOGGEDIN, R.string.err_already_loggedin);
        errorMessages.put(ClientError.CMDERR_NOT_IN_CHANNEL, R.string.err_not_in_channel);
        errorMessages.put(ClientError.CMDERR_ALREADY_IN_CHANNEL, R.string.err_already_in_channel);
        errorMessages.put(ClientError.CMDERR_CHANNEL_NOT_FOUND, R.string.err_channel_not_found);
        errorMessages.put(ClientError.CMDERR_BAN_NOT_FOUND, R.string.err_ban_not_found);
        errorMessages.put(ClientError.CMDERR_FILETRANSFER_NOT_FOUND, R.string.err_filetransfer_not_found);
        errorMessages.put(ClientError.CMDERR_ACCOUNT_NOT_FOUND, R.string.err_account_not_found);
        errorMessages.put(ClientError.CMDERR_FILE_NOT_FOUND, R.string.err_file_not_found);
        errorMessages.put(ClientError.CMDERR_FILE_ALREADY_EXISTS, R.string.err_file_already_exists);
        errorMessages.put(ClientError.CMDERR_LOGINSERVICE_UNAVAILABLE, R.string.err_loginservice_unavailable);
        errorMessages.put(ClientError.CMDERR_CHANNEL_CANNOT_BE_HIDDEN, R.string.err_channel_cannot_be_hidden);
    }

    public static void notifyError(Context context, ClientErrorMsg err) {
        if (err.nErrorNo == ClientError.CMDERR_ALREADY_IN_CHANNEL) {
            return; // Silence this error as per user request
        }
        if (errorMessages.containsKey(err.nErrorNo)) {
            Toast.makeText(context, errorMessages.get(err.nErrorNo), Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(context, err.szErrorMsg, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Announce a title for accessibility services using the app's locale context.
     * This ensures TalkBack reads the correct language based on app settings,
     * overriding the system-locale-resolved AndroidManifest label.
     * Uses post() to delay announcement until view is fully attached.
     */
    public static void announceAccessibilityTitle(android.app.Activity activity, @StringRes int titleResId) {
        android.view.View rootView = activity.getWindow().getDecorView().getRootView();
        if (rootView != null) {
            final String title = activity.getString(titleResId);
            // Delay announcement to ensure TalkBack receives it after initial window focus
            rootView.postDelayed(() -> rootView.announceForAccessibility(title), 300);
        }
    }

    public static void setEditTextPreference(Preference preference, String text, String summary) {
        setEditTextPreference(preference, text, summary, false);
    }

    public static void setEditTextPreference(Preference preference, String text, String summary, boolean forcesummary) {
        EditTextPreference textpref = (EditTextPreference) preference;
        textpref.setText(text);
        if (!summary.isEmpty() || forcesummary)
            textpref.setSummary(summary);
    }

    public static String getEditTextPreference(Preference preference, String def_value) {
        EditTextPreference textpref = (EditTextPreference) preference;
        String s = textpref.getText(); 
        if(s == null)
            return def_value;
        return s;
    }

    public static Intent putServerEntry(Intent intent, ServerEntry entry) {
        return intent.putExtra(ServerEntry.class.getName(), new Gson().toJson(entry));
    }

    public static ServerEntry getServerEntry(Intent intent) {
        if (intent.hasExtra(ServerEntry.class.getName())) {
            return new Gson().fromJson(intent.getExtras().getString(ServerEntry.class.getName()), ServerEntry.class);
        }
        return null;
    }

    public static Intent putAudioCodec(Intent intent, AudioCodec entry) {
        return intent.putExtra(AudioCodec.class.getName(), new Gson().toJson(entry));
    }

    public static AudioCodec getAudioCodec(Intent intent) {
        if (intent.hasExtra(AudioCodec.class.getName())) {
            return new Gson().fromJson(intent.getExtras().getString(AudioCodec.class.getName()), AudioCodec.class);
        }
        return null;
    }

    public static AudioConfig getAudioConfig(Intent intent) {
        if (intent.hasExtra(AudioConfig.class.getName())) {
            return new Gson().fromJson(intent.getExtras().getString(AudioConfig.class.getName()), AudioConfig.class);
        }
        return null;
    }

    public static Intent putAudioConfig(Intent intent, AudioConfig entry) {
        return intent.putExtra(AudioConfig.class.getName(), new Gson().toJson(entry));
    }

    public static Intent putChannel(Intent intent, Channel entry) {
        return intent.putExtra(Channel.class.getName(), new Gson().toJson(entry));
    }

    public static Channel getChannel(Intent intent) {
        if (intent.hasExtra(Channel.class.getName())) {
            return new Gson().fromJson(intent.getExtras().getString(Channel.class.getName()), Channel.class);
        }
        return null;
    }

    public static Vector<Channel> getSubChannels(int chanid, Map<Integer, Channel> channels) {
        Vector<Channel> result = new Vector<>();

        for (Entry<Integer, Channel> integerChannelEntry : channels.entrySet()) {
            Channel chan = integerChannelEntry.getValue();
            if ((chan.nParentID == chanid) && (chan.nMaxUsers > 0))
                result.add(chan);
        }
        return result;
    }

    public static Vector<Channel> getStickyChannels(int chanid, Map<Integer, Channel> channels) {
        Vector<Channel> result = new Vector<>();

        for (Entry<Integer, Channel> integerChannelEntry : channels.entrySet()) {
            Channel chan = integerChannelEntry.getValue();
            if ((chan.nParentID == chanid) && (chan.nMaxUsers <= 0))
                result.add(chan);
        }
        return result;
    }

    public static Vector<User> getUsers(int chanid, Map<Integer, User> users) {
        return getUsers(chanid, users, false, null);
    }

    public static Vector<User> getUsers(int chanid, Map<Integer, User> users, boolean recursive, Map<Integer, Channel> channels) {
        Vector<User> result = new Vector<>();

        if (!recursive) {
            for (Entry<Integer, User> integerUserEntry : users.entrySet()) {
                User user = integerUserEntry.getValue();
                if (user.nChannelID == chanid)
                    result.add(user);
            }
        } else {
            // Recursive user count
            Set<Integer> targetChannels = new HashSet<>();
            targetChannels.add(chanid);
            if (channels != null) {
                fillSubChannelIds(chanid, channels, targetChannels);
            }
            for (User user : users.values()) {
                if (targetChannels.contains(user.nChannelID)) {
                    result.add(user);
                }
            }
        }
        return result;
    }

    private static void fillSubChannelIds(int chanid, Map<Integer, Channel> channels, Set<Integer> result) {
        for (Channel chan : channels.values()) {
            if (chan.nParentID == chanid) {
                result.add(chan.nChannelID);
                fillSubChannelIds(chan.nChannelID, channels, result);
            }
        }
    }

    public static boolean hasSubChannels(int chanid, Map<Integer, Channel> channels) {
        for (Channel chan : channels.values()) {
            if (chan.nParentID == chanid)
                return true;
        }
        return false;
    }

    public static Vector<User> getUsers(Map<Integer, User> users) {
        Vector<User> result = new Vector<>();

        for (Entry<Integer, User> integerUserEntry : users.entrySet()) {
            result.add(integerUserEntry.getValue());
        }
        return result;
    }

    public static Vector<RemoteFile> getRemoteFiles(int chanid, Map<Integer, RemoteFile> remotefiles) {
        Vector<RemoteFile> result = new Vector<>();

        for (Entry<Integer, RemoteFile> integerRemoteFileEntry : remotefiles.entrySet()) {
            RemoteFile remotefile = integerRemoteFileEntry.getValue();
            if (remotefile.nChannelID == chanid)
                result.add(remotefile);
        }
        return result;
    }

    public static Vector<RemoteFile> getRemoteFiles(Map<Integer, RemoteFile> remotefiles) {
        Vector<RemoteFile> result = new Vector<>();

        for (Entry<Integer, RemoteFile> integerRemoteFileEntry : remotefiles.entrySet()) {
            result.add(integerRemoteFileEntry.getValue());
        }
        return result;
    }

    public static Vector<FileTransfer> getFileTransfers(int chanid, Map<Integer, FileTransfer> filetransfers) {
        Vector<FileTransfer> result = new Vector<>();

        for (Entry<Integer, FileTransfer> integerFileTransferEntry : filetransfers.entrySet()) {
            FileTransfer transfer = integerFileTransferEntry.getValue();
            if (transfer.nChannelID == chanid)
                result.add(transfer);
        }
        return result;
    }

    public static Vector<FileTransfer> getFileTransfers(Map<Integer, FileTransfer> filetransfers) {
        Vector<FileTransfer> result = new Vector<>();

        for (Entry<Integer, FileTransfer> integerFileTransferEntry : filetransfers.entrySet()) {
            result.add(integerFileTransferEntry.getValue());
        }
        return result;
    }

    public static int toggleSubscription(TeamTalkBase ttclient, User user, int streamtype, boolean on) {
        if (on) {
            return ttclient.doSubscribe(user.nUserID, streamtype);
        } else {
            return ttclient.doUnsubscribe(user.nUserID, streamtype);
        }
    }

    public static boolean isTransmitAllowed(User user, Channel chan, int streamtype) {
        for (int i = 0; i < chan.transmitUsers.length; i++) {
            if (chan.transmitUsers[i][0] == user.nUserID && (chan.transmitUsers[i][1] & streamtype) == streamtype) {
                return (chan.uChannelType & ChannelType.CHANNEL_CLASSROOM) == ChannelType.CHANNEL_CLASSROOM;
            }
        }
        return (chan.uChannelType & ChannelType.CHANNEL_CLASSROOM) == ChannelType.CHANNEL_DEFAULT;
    }

    public static void toggleTransmitUsers(User user, Channel chan, int streamtype, boolean allow) {

        boolean clear;
        if ((chan.uChannelType & ChannelType.CHANNEL_CLASSROOM) == ChannelType.CHANNEL_CLASSROOM)
            clear = !allow;
        else clear = allow;

        if (clear) {
            for (int i = 0; i < chan.transmitUsers.length; i++) {
                if (chan.transmitUsers[i][0] == user.nUserID || chan.transmitUsers[i][0] == 0) {
                    chan.transmitUsers[i][0] = user.nUserID;
                    chan.transmitUsers[i][1] &= ~streamtype;

                    if (chan.transmitUsers[i][1] == StreamType.STREAMTYPE_NONE) {
                        chan.transmitUsers[i][0] = 0;
                        for (int j=i;j<chan.transmitUsers.length-1;++j) {
                            chan.transmitUsers[j][0] = chan.transmitUsers[j+1][0];
                            chan.transmitUsers[j][1] = chan.transmitUsers[j+1][1];
                        }
                        chan.transmitUsers[chan.transmitUsers.length - 1][0] = 0;
                        chan.transmitUsers[chan.transmitUsers.length - 1][1] = 0;
                    }
                    break;
                }
            }
        } else {
            for (int i = 0; i < chan.transmitUsers.length; i++) {
                if (chan.transmitUsers[i][0] == user.nUserID || chan.transmitUsers[i][0] == 0) {
                    chan.transmitUsers[i][0] = user.nUserID;
                    chan.transmitUsers[i][1] |= streamtype;
                    break;
                }
            }
        }
    }

    public static int transmitUsersToggled(Channel chan, int prev, int curr, int streamType) {
        boolean wasOn = (prev & streamType) != StreamType.STREAMTYPE_NONE;
        boolean isNowOn = (curr & streamType) != StreamType.STREAMTYPE_NONE;

        if (isNowOn && !wasOn)
            return (chan.uChannelType & ChannelType.CHANNEL_CLASSROOM) != 0 ? 1 : -1;
        else if (!isNowOn && wasOn)
            return (chan.uChannelType & ChannelType.CHANNEL_CLASSROOM) != 0 ? -1 : 1;
        else
            return 0;
    }

    public static Map<Integer, Integer> transmitUsersToMap(int[][] transmitUsers) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int[] entry : transmitUsers) {
            if (entry.length >= 2)
                map.put(entry[0], entry[1]);
        }
        return map;
    }

    public static Optional<String> ttsTransmitUsersToggled(Context context, Channel oldchan, Channel updchan, Map<Integer, User> users) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<Integer> allUserIds = new HashSet<>();
        Map<Integer, Integer> oldTransmitUsers = Utils.transmitUsersToMap(oldchan.transmitUsers);
        Map<Integer, Integer> newTransmitUsers = Utils.transmitUsersToMap(updchan.transmitUsers);
        allUserIds.addAll(oldTransmitUsers.keySet());
        allUserIds.addAll(newTransmitUsers.keySet());

        for (int userId : allUserIds) {
            int oldValue = oldTransmitUsers.getOrDefault(userId, StreamType.STREAMTYPE_NONE);
            int newValue = newTransmitUsers.getOrDefault(userId, StreamType.STREAMTYPE_NONE);
            String name;
            if (userId==0) continue;
            if(userId == Constants.TT_CLASSROOM_FREEFORALL)
                name = context.getResources().getString(R.string.text_tts_transmit_name_everyone);
            else {
                User u = users.get(userId);
                if(u!=null && u.nChannelID == oldchan.nChannelID)
                    name = Utils.getDisplayName(context, users.get(userId));
                else
                    continue;
            }

            int result = Utils.transmitUsersToggled(updchan, oldValue, newValue, StreamType.STREAMTYPE_CHANNELMSG);
            if (result < 0 && prefs.getBoolean("transmit_channel_msg_checkbox", true))
                return Optional.of(context.getString(R.string.tts_fmt_2, name, context.getResources().getString(R.string.text_tts_channel_msg_transmit_off)));
            else if (result > 0 && prefs.getBoolean("transmit_channel_msg_checkbox", true))
                return Optional.of(context.getString(R.string.tts_fmt_2, name, context.getResources().getString(R.string.text_tts_channel_msg_transmit_on)));

            result = Utils.transmitUsersToggled(updchan, oldValue, newValue, StreamType.STREAMTYPE_VOICE);
            if (result < 0 && prefs.getBoolean("transmit_voice_checkbox", true))
                return Optional.of(context.getString(R.string.tts_fmt_2, name, context.getResources().getString(R.string.text_tts_voice_transmit_off)));
            else if (result > 0 && prefs.getBoolean("transmit_voice_checkbox", true))
                return Optional.of(context.getString(R.string.tts_fmt_2, name, context.getResources().getString(R.string.text_tts_voice_transmit_on)));

            result = Utils.transmitUsersToggled(updchan, oldValue, newValue, StreamType.STREAMTYPE_VIDEOCAPTURE);
            if (result < 0 && prefs.getBoolean("transmit_vid_checkbox", true))
                return Optional.of(context.getString(R.string.tts_fmt_2, name, context.getResources().getString(R.string.text_tts_vid_transmit_off)));
            else if (result > 0 && prefs.getBoolean("transmit_vid_checkbox", true))
                return Optional.of(context.getString(R.string.tts_fmt_2, name, context.getResources().getString(R.string.text_tts_vid_transmit_on)));

            result = Utils.transmitUsersToggled(updchan, oldValue, newValue, StreamType.STREAMTYPE_DESKTOP);
            if (result < 0 && prefs.getBoolean("transmit_desk_checkbox", true))
                return Optional.of(context.getString(R.string.tts_fmt_2, name, context.getResources().getString(R.string.text_tts_desk_transmit_off)));
            else if (result > 0 && prefs.getBoolean("transmit_desk_checkbox", true))
                return Optional.of(context.getString(R.string.tts_fmt_2, name, context.getResources().getString(R.string.text_tts_desk_transmit_on)));

            result = Utils.transmitUsersToggled(updchan, oldValue, newValue, StreamType.STREAMTYPE_MEDIAFILE);
            if (result < 0 && prefs.getBoolean("transmit_media_checkbox", true))
                return Optional.of(context.getString(R.string.tts_fmt_2, name, context.getResources().getString(R.string.text_tts_media_transmit_off)));
            else if (result > 0 && prefs.getBoolean("transmit_media_checkbox", true))
                return Optional.of(context.getString(R.string.tts_fmt_2, name, context.getResources().getString(R.string.text_tts_media_transmit_on)));
        }
        return Optional.empty();
    }

    public static Optional<Boolean> subscriptionChanged(User oldUser, User newUser, int subscription) {
        if ((oldUser.uPeerSubscriptions & subscription) != (newUser.uPeerSubscriptions & subscription)) {
            return Optional.of((newUser.uPeerSubscriptions & subscription) == subscription);
        }
        return Optional.empty();
    }

    public static boolean ttsScriptionPreferenceEnabled(Context context, int subscription) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        switch (subscription) {
            case Subscription.SUBSCRIBE_USER_MSG :
                return prefs.getBoolean("subscription_user_msg_checkbox", true);
            case Subscription.SUBSCRIBE_CHANNEL_MSG :
                return  prefs.getBoolean("subscription_channel_msg_checkbox", true);
            case Subscription.SUBSCRIBE_BROADCAST_MSG :
                return  prefs.getBoolean("subscription_broadcast_msg_checkbox", true);
            case Subscription.SUBSCRIBE_VOICE :
                return  prefs.getBoolean("subscription_voice_checkbox", true);
            case Subscription.SUBSCRIBE_VIDEOCAPTURE :
                return prefs.getBoolean("subscription_vid_checkbox", true);
            case Subscription.SUBSCRIBE_DESKTOP :
                return prefs.getBoolean("subscription_desk_checkbox", true);
            case Subscription.SUBSCRIBE_MEDIAFILE :
                return prefs.getBoolean("subscription_media_checkbox", true);
            case Subscription.SUBSCRIBE_INTERCEPT_USER_MSG :
                return prefs.getBoolean("subscription_intercept_user_msg_checkbox", true);
            case Subscription.SUBSCRIBE_INTERCEPT_CHANNEL_MSG :
                return prefs.getBoolean("subscription_intercept_channel_msg_checkbox", true);
            case Subscription.SUBSCRIBE_INTERCEPT_VOICE :
                return  prefs.getBoolean("subscription_intercept_voice_checkbox", true);
            case Subscription.SUBSCRIBE_INTERCEPT_VIDEOCAPTURE :
                return  prefs.getBoolean("subscription_intercept_vid_checkbox", true);
            case Subscription.SUBSCRIBE_INTERCEPT_DESKTOP :
                return  prefs.getBoolean("subscription_intercept_desk_checkbox", true);
            case Subscription.SUBSCRIBE_INTERCEPT_MEDIAFILE :
                return prefs.getBoolean("subscription_intercept_media_checkbox", true);
        }
        return false;
    }

    public static String ttsGenerateSubscriptionText(Context context, @StringRes int id, User user, boolean isOn) {
        return context.getString(R.string.tts_fmt_3, getDisplayName(context, user),
                context.getResources().getString(id),
                (isOn ? context.getResources().getString(R.string.text_tts_subscribe_on) :
                        context.getResources().getString(R.string.text_tts_subscribe_off)));
    }

    public static Optional<String> ttsSubscriptionChanged(Context context, User oldUser, User newUser) {
        Optional<Boolean> isOn;
        if (ttsScriptionPreferenceEnabled(context, Subscription.SUBSCRIBE_USER_MSG) &&
                (isOn = subscriptionChanged(oldUser, newUser, Subscription.SUBSCRIBE_USER_MSG)).isPresent()) {
            return Optional.of(ttsGenerateSubscriptionText(context, dk.bearware.gui.R.string.text_tts_subscription_user_msg_changed, newUser, isOn.get()));
        }
        if (ttsScriptionPreferenceEnabled(context, Subscription.SUBSCRIBE_CHANNEL_MSG) &&
                (isOn = subscriptionChanged(oldUser, newUser, Subscription.SUBSCRIBE_CHANNEL_MSG)).isPresent()) {
            return Optional.of(ttsGenerateSubscriptionText(context, dk.bearware.gui.R.string.text_tts_subscription_channel_msg_changed, newUser, isOn.get()));
        }
        if (ttsScriptionPreferenceEnabled(context, Subscription.SUBSCRIBE_BROADCAST_MSG) &&
                (isOn = subscriptionChanged(oldUser, newUser, Subscription.SUBSCRIBE_BROADCAST_MSG)).isPresent()) {
            return Optional.of(ttsGenerateSubscriptionText(context, dk.bearware.gui.R.string.text_tts_subscription_broadcast_msg_changed, newUser, isOn.get()));
        }
        if (ttsScriptionPreferenceEnabled(context, Subscription.SUBSCRIBE_VOICE) &&
                (isOn = subscriptionChanged(oldUser, newUser, Subscription.SUBSCRIBE_VOICE)).isPresent()) {
            return Optional.of(ttsGenerateSubscriptionText(context, dk.bearware.gui.R.string.text_tts_subscription_voice_changed, newUser, isOn.get()));
        }
        if (ttsScriptionPreferenceEnabled(context, Subscription.SUBSCRIBE_VIDEOCAPTURE) &&
                (isOn = subscriptionChanged(oldUser, newUser, Subscription.SUBSCRIBE_VIDEOCAPTURE)).isPresent()) {
            return Optional.of(ttsGenerateSubscriptionText(context, dk.bearware.gui.R.string.text_tts_subscription_vid_changed, newUser, isOn.get()));
        }
        if (ttsScriptionPreferenceEnabled(context, Subscription.SUBSCRIBE_DESKTOP) &&
                (isOn = subscriptionChanged(oldUser, newUser, Subscription.SUBSCRIBE_DESKTOP)).isPresent()) {
            return Optional.of(ttsGenerateSubscriptionText(context, dk.bearware.gui.R.string.text_tts_subscription_desk_changed, newUser, isOn.get()));
        }
        if (ttsScriptionPreferenceEnabled(context, Subscription.SUBSCRIBE_MEDIAFILE) &&
                (isOn = subscriptionChanged(oldUser, newUser, Subscription.SUBSCRIBE_MEDIAFILE)).isPresent()) {
            return Optional.of(ttsGenerateSubscriptionText(context, dk.bearware.gui.R.string.text_tts_subscription_media_changed, newUser, isOn.get()));
        }
        if (ttsScriptionPreferenceEnabled(context, Subscription.SUBSCRIBE_INTERCEPT_USER_MSG) &&
                (isOn = subscriptionChanged(oldUser, newUser, Subscription.SUBSCRIBE_INTERCEPT_USER_MSG)).isPresent()) {
            return Optional.of(ttsGenerateSubscriptionText(context, dk.bearware.gui.R.string.text_tts_subscription_intercept_user_msg_changed, newUser, isOn.get()));
        }
        if (ttsScriptionPreferenceEnabled(context, Subscription.SUBSCRIBE_INTERCEPT_CHANNEL_MSG) &&
                (isOn = subscriptionChanged(oldUser, newUser, Subscription.SUBSCRIBE_INTERCEPT_CHANNEL_MSG)).isPresent()) {
            return Optional.of(ttsGenerateSubscriptionText(context, dk.bearware.gui.R.string.text_tts_subscription_intercept_channel_msg_changed, newUser, isOn.get()));
        }
        if (ttsScriptionPreferenceEnabled(context, Subscription.SUBSCRIBE_INTERCEPT_VOICE) &&
                (isOn = subscriptionChanged(oldUser, newUser, Subscription.SUBSCRIBE_INTERCEPT_VOICE)).isPresent()) {
            return Optional.of(ttsGenerateSubscriptionText(context, dk.bearware.gui.R.string.text_tts_subscription_intercept_voice_changed, newUser, isOn.get()));
        }
        if (ttsScriptionPreferenceEnabled(context, Subscription.SUBSCRIBE_INTERCEPT_VIDEOCAPTURE) &&
                (isOn = subscriptionChanged(oldUser, newUser, Subscription.SUBSCRIBE_INTERCEPT_VIDEOCAPTURE)).isPresent()) {
            return Optional.of(ttsGenerateSubscriptionText(context, dk.bearware.gui.R.string.text_tts_subscription_intercept_vid_changed, newUser, isOn.get()));
        }
        if (ttsScriptionPreferenceEnabled(context, Subscription.SUBSCRIBE_INTERCEPT_DESKTOP) &&
                (isOn = subscriptionChanged(oldUser, newUser, Subscription.SUBSCRIBE_INTERCEPT_DESKTOP)).isPresent()) {
            return Optional.of(ttsGenerateSubscriptionText(context, dk.bearware.gui.R.string.text_tts_subscription_intercept_desk_changed, newUser, isOn.get()));
        }
        if (ttsScriptionPreferenceEnabled(context, Subscription.SUBSCRIBE_INTERCEPT_MEDIAFILE) &&
                (isOn = subscriptionChanged(oldUser, newUser, Subscription.SUBSCRIBE_INTERCEPT_MEDIAFILE)).isPresent()) {
            return Optional.of(ttsGenerateSubscriptionText(context, dk.bearware.gui.R.string.text_tts_subscription_intercept_media_changed, newUser, isOn.get()));
        }
        return Optional.empty();
    }

    public static String getURL(String urlToRead) {
        return postURL(urlToRead, null);
    }

    public static String postURL(String urlString, String body) {
        HttpURLConnection conn = null;
        BufferedReader rd = null;
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            if (body != null) {
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "text/xml");
                java.io.OutputStream os = conn.getOutputStream();
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
            } else {
                conn.setRequestMethod("GET");
            }
            
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            char[] buff = new char[1024];
            int len;
            while((len = rd.read(buff)) > 0) {
                result.append(buff, 0, len);
            }
        }
        catch(IOException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e(TAG, "Failed to " + (body != null ? "POST" : "GET") + " URL: " + urlString + ". Error: " + errorMsg);
            e.printStackTrace();
        } finally {
            if (rd != null) {
                try {
                    rd.close();
                } catch (IOException ignored) {}
            }
            if (conn != null) {
                conn.disconnect();
            }
        }

        return result.toString();
    }

    public static String readInputStream(java.io.InputStream in) throws java.io.IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    public static Vector<ServerEntry> getXmlServerEntries(String xml) {
        Vector<ServerEntry> servers = new Vector<>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        Document doc;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(new InputSource(new StringReader(xml)));
        }
        catch(Exception e) {
            Log.e(TAG, "Failed to parse server entries");
            return servers;
        }

        doc.getDocumentElement().normalize();

        NodeList nList = doc.getElementsByTagName("host");
        for (int i = 0; i < nList.getLength(); i++) {
            Node hostnode = nList.item(i);
            if (hostnode.getNodeType() == Node.ELEMENT_NODE) {
                Element hostelement = (Element) hostnode;
                ServerEntry entry = new ServerEntry();
                NodeList namenode = hostelement.getElementsByTagName("name");
                if (namenode.getLength() > 0)
                    entry.servername = namenode.item(0).getTextContent();
                NodeList ipaddrnode = hostelement.getElementsByTagName("address");
                if (ipaddrnode.getLength() > 0)
                    entry.ipaddr = ipaddrnode.item(0).getTextContent();
                NodeList tcpportnode = hostelement.getElementsByTagName("tcpport");
                try {
                    if (tcpportnode.getLength() > 0)
                        entry.tcpport = Integer.parseInt(tcpportnode.item(0).getTextContent());
                    NodeList udpportnode = hostelement.getElementsByTagName("udpport");
                    if (udpportnode.getLength() > 0)
                        entry.udpport = Integer.parseInt(udpportnode.item(0).getTextContent());
                }
                catch(NumberFormatException e) {
                    continue;
                }
                NodeList listingnode = hostelement.getElementsByTagName("listing");
                if (listingnode.getLength() > 0) {
                    switch (listingnode.item(0).getTextContent()) {
                        case "official":
                            entry.servertype = ServerEntry.ServerType.OFFICIAL;
                            break;
                        case "public":
                            entry.servertype = ServerEntry.ServerType.PUBLIC;
                            break;
                        case "private":
                            entry.servertype = ServerEntry.ServerType.UNOFFICIAL;
                            break;
                    }
                }
                NodeList encryptednode = hostelement.getElementsByTagName("encrypted");
                if (encryptednode.getLength() > 0)
                    entry.encrypted = encryptednode.item(0).getTextContent().equalsIgnoreCase("true");

                NodeList certificatenode = hostelement.getElementsByTagName("trusted-certificate");
                if (certificatenode.getLength() > 0) {
                    Node trustednode = certificatenode.item(0);
                    if (trustednode.getNodeType() == Node.ELEMENT_NODE) {
                        Element trustedelement = (Element)trustednode;
                        NodeList cacertnode = trustedelement.getElementsByTagName("certificate-authority-pem");
                        if (cacertnode.getLength() > 0)
                            entry.cacert = cacertnode.item(0).getTextContent();
                        NodeList clientcertnode = trustedelement.getElementsByTagName("client-certificate-pem");
                        if (clientcertnode.getLength() > 0)
                            entry.clientcert = clientcertnode.item(0).getTextContent();
                        NodeList clientkeynode = trustedelement.getElementsByTagName("client-private-key-pem");
                        if (clientkeynode.getLength() > 0)
                            entry.clientcertkey = clientkeynode.item(0).getTextContent();
                        NodeList verifynode = trustedelement.getElementsByTagName("verify-peer");
                        if (verifynode.getLength() > 0)
                            entry.verifypeer = verifynode.item(0).getTextContent().equalsIgnoreCase("true");
                    }
                }

                NodeList authlist = hostelement.getElementsByTagName("auth");
                if (authlist.getLength() > 0) {
                    Node authnode = authlist.item(0);
                    if (authnode.getNodeType() == Node.ELEMENT_NODE) {
                        Element authelement = (Element) authnode;
                        NodeList usernamenode = authelement.getElementsByTagName("username");
                        if (usernamenode.getLength() > 0)
                            entry.username = usernamenode.item(0).getTextContent();
                        NodeList passwordnode = authelement.getElementsByTagName("password");
                        if (passwordnode.getLength() > 0)
                            entry.password = passwordnode.item(0).getTextContent();
                        NodeList nicknamenode = authelement.getElementsByTagName("nickname");
                        if (nicknamenode.getLength() > 0)
                            entry.nickname = nicknamenode.item(0).getTextContent();
                        NodeList statusmsgnode = authelement.getElementsByTagName("statusmsg");
                        if (statusmsgnode.getLength() > 0)
                            entry.statusmsg = statusmsgnode.item(0).getTextContent();
                    }
                }
                
                // Fallbacks if credentials are put directly under <host> instead of <auth>
                if (entry.username.isEmpty()) {
                    NodeList usernamenode = hostelement.getElementsByTagName("username");
                    if (usernamenode.getLength() > 0) entry.username = usernamenode.item(0).getTextContent();
                }
                if (entry.password.isEmpty()) {
                    NodeList passwordnode = hostelement.getElementsByTagName("password");
                    if (passwordnode.getLength() > 0) entry.password = passwordnode.item(0).getTextContent();
                }
                if (entry.nickname.isEmpty()) {
                    NodeList nicknamenode = hostelement.getElementsByTagName("nickname");
                    if (nicknamenode.getLength() > 0) entry.nickname = nicknamenode.item(0).getTextContent();
                }

                NodeList joinlist = hostelement.getElementsByTagName("join");
                if (joinlist.getLength() > 0) {
                    Node joinnode = joinlist.item(0);
                    if (joinnode.getNodeType() == Node.ELEMENT_NODE) {
                        Element joinelement = (Element) joinnode;
                        NodeList joinlastchannelnode = hostelement.getElementsByTagName("join-last-channel");
                        if (joinlastchannelnode.getLength() > 0)
                            entry.rememberLastChannel = joinlastchannelnode.item(0).getTextContent().equalsIgnoreCase("true");
                        NodeList channelnode = joinelement.getElementsByTagName("channel");
                        if (channelnode.getLength() > 0)
                            entry.channel = channelnode.item(0).getTextContent();
                        NodeList passwordnode = joinelement.getElementsByTagName("password"); // Will also catch <password> anywhere if not careful but XML is usually simple
                        // Let's be safer for chanpasswd
                        NodeList chanpasswordnode = joinelement.getElementsByTagName("password");
                        if (chanpasswordnode.getLength() > 0)
                            entry.chanpasswd = chanpasswordnode.item(0).getTextContent();
                    }
                }
                
                // Fallbacks for join parameters
                if (entry.channel.isEmpty()) {
                    NodeList channelnode = hostelement.getElementsByTagName("channel");
                    if (channelnode.getLength() > 0) entry.channel = channelnode.item(0).getTextContent();
                }
                // If chanpasswd is still empty, we could fall back, but 'password' tag overlap with auth is tricky. 
                // We'll skip chanpasswd fallback to avoid assigning user 'password' to 'chanpasswd'.

                NodeList statslist = hostelement.getElementsByTagName("stats");
                if (statslist.getLength() > 0) {
                    Node statsnode = statslist.item(0);
                    if (statsnode.getNodeType() == Node.ELEMENT_NODE) {
                        Element statselement = (Element) statsnode;
                        NodeList mothnode = statselement.getElementsByTagName("motd");
                        if (mothnode.getLength() > 0) {
                            entry.stats_motd = mothnode.item(0).getTextContent();
                        }
                        NodeList countrynode = statselement.getElementsByTagName("country");
                        if (countrynode.getLength() > 0) {
                            entry.stats_country = countrynode.item(0).getTextContent();
                        }
                        NodeList usercountnode = statselement.getElementsByTagName("user-count");
                        if (usercountnode.getLength() > 0) {
                            try {
                                entry.stats_usercount = Integer.parseInt(usercountnode.item(0).getTextContent());
                            }
                            catch (NumberFormatException ignored) {
                            }
                         }
                    }
                }
                servers.add(entry);
            }
        }

        return servers;
    }

    public static boolean saveServers(Vector<ServerEntry> servers, String path) {
        try {
            FileOutputStream fos = new FileOutputStream(path);
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(fos, "UTF-8");
            serializer.startDocument(null, Boolean.TRUE);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, "teamtalk").attribute(null, "version", "5.0");
            for (ServerEntry server : servers) {
                serializer.startTag(null, "host");
                serializer.startTag(null, "name").text(server.servername).endTag(null, "name");
                serializer.startTag(null, "address").text(server.ipaddr).endTag(null, "address");
                serializer.startTag(null, "tcpport").text(String.valueOf(server.tcpport)).endTag(null, "tcpport");
                serializer.startTag(null, "udpport").text(String.valueOf(server.udpport)).endTag(null, "udpport");
                serializer.startTag(null, "encrypted").text(String.valueOf(server.encrypted)).endTag(null, "encrypted");
                serializer.startTag(null, "auth");
                serializer.startTag(null, "username").text(server.username).endTag(null, "username");
                serializer.startTag(null, "password").text(server.password).endTag(null, "password");
                serializer.startTag(null, "nickname").text(server.nickname).endTag(null, "nickname");
                serializer.startTag(null, "statusmsg").text(server.statusmsg).endTag(null, "statusmsg");
                serializer.endTag(null, "auth");
                serializer.startTag(null, "join");
                serializer.startTag(null, "join-last-channel").text(String.valueOf(server.rememberLastChannel)).endTag(null, "join-last-channel");
                serializer.startTag(null, "channel").text(server.channel).endTag(null, "channel");
                serializer.startTag(null, "password").text(server.chanpasswd).endTag(null, "password");
                serializer.endTag(null, "join");
                serializer.endTag(null, "host");
            }
            serializer.endTag(null, "teamtalk");
            serializer.endDocument();
            serializer.flush();
            fos.close();
        }
        catch(Exception e) {
            Log.d(TAG, "Unable to save file " + path,  e);
            return false;
        }
        return true;
    }

    public static String generateServerEntryXml(ServerEntry server) {
        try {
            java.io.StringWriter writer = new java.io.StringWriter();
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, "teamtalk").attribute(null, "version", "5.0");
            
            serializer.startTag(null, "host");
            if (server.servername != null)
                serializer.startTag(null, "name").text(server.servername).endTag(null, "name");
            if (server.ipaddr != null)
                serializer.startTag(null, "address").text(server.ipaddr).endTag(null, "address");
            serializer.startTag(null, "tcpport").text(String.valueOf(server.tcpport)).endTag(null, "tcpport");
            serializer.startTag(null, "udpport").text(String.valueOf(server.udpport)).endTag(null, "udpport");
            serializer.startTag(null, "encrypted").text(String.valueOf(server.encrypted)).endTag(null, "encrypted");
            
            if ((server.username != null && !server.username.isEmpty()) || 
                (server.password != null && !server.password.isEmpty()) ||
                (server.nickname != null && !server.nickname.isEmpty()) ||
                (server.statusmsg != null && !server.statusmsg.isEmpty())) {
                serializer.startTag(null, "auth");
                if (server.username != null)
                    serializer.startTag(null, "username").text(server.username).endTag(null, "username");
                if (server.password != null)
                    serializer.startTag(null, "password").text(server.password).endTag(null, "password");
                if (server.nickname != null)
                    serializer.startTag(null, "nickname").text(server.nickname).endTag(null, "nickname");
                if (server.statusmsg != null)
                    serializer.startTag(null, "statusmsg").text(server.statusmsg).endTag(null, "statusmsg");
                serializer.endTag(null, "auth");
            }
            
            if ((server.channel != null && !server.channel.isEmpty()) || 
                (server.chanpasswd != null && !server.chanpasswd.isEmpty()) ||
                server.rememberLastChannel) {
                serializer.startTag(null, "join");
                serializer.startTag(null, "join-last-channel").text(String.valueOf(server.rememberLastChannel)).endTag(null, "join-last-channel");
                if (server.channel != null)
                    serializer.startTag(null, "channel").text(server.channel).endTag(null, "channel");
                if (server.chanpasswd != null)
                    serializer.startTag(null, "password").text(server.chanpasswd).endTag(null, "password");
                serializer.endTag(null, "join");
            }

            serializer.endTag(null, "host");
            serializer.endTag(null, "teamtalk");
            serializer.endDocument();
            return writer.toString();
        }
        catch(Exception e) {
            Log.e(TAG, "Unable to generate XML for server",  e);
            return null;
        }
    }

    public static int refVolume(double percent)
    {
        if (percent == 0) {
            return SoundLevel.SOUND_VOLUME_MIN;
        }
        percent = Math.max(0, percent);
        percent = Math.min(100, percent);

        double d = 82.832 * Math.exp(0.0508 * percent) - 50;
        return (int)d;
    }

    public static int refVolumeToPercent(int volume)
    {
        volume = Math.max(volume, SoundLevel.SOUND_VOLUME_MIN);
        volume = Math.min(volume, SoundLevel.SOUND_VOLUME_MAX);

        double d = (volume + 50) / 82.832;
        d = Math.log(d) / 0.0508;
        return (int)(d + .5);
    }

    public static int refGain(double percent)
    {
        if (percent == 0) {
            return 0;
        }
        percent = Math.max(0, percent);
        percent = Math.min(100, percent);

        double d = 106.47 * Math.exp(0.0508 * percent) - 50;
        return (int)d;
    }

    public static int refGainToPercent(int gain)
    {
        gain = Math.max(gain, SoundLevel.SOUND_GAIN_MIN);
        gain = Math.min(gain, SoundLevel.SOUND_GAIN_MAX);

        double d = (gain + 50) / 106.47;
        return (int)(Math.log(d) / 0.0508 + 0.5);
    }

    public static Bitmap drawTextToBitmap(Context gContext, int width, int height, String gText) {
        Resources resources = gContext.getResources();
        float scale = resources.getDisplayMetrics().density;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(resources.getColor(R.color.grey_text_bitmap));

        paint.setTextSize(resources.getDimension(R.dimen.text_size_default));

        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        Rect bounds = new Rect();
        paint.getTextBounds(gText, 0, gText.length(), bounds);
        int x = (bitmap.getWidth() - bounds.width())/2;
        int y = (bitmap.getHeight() + bounds.height())/2;

        canvas.drawText(gText, x, y, paint);

        return bitmap;
    }

    public static String getDisplayName(Context context, User user) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context); 
        if(pref.getBoolean(Preferences.PREF_GENERAL_SHOWUSERNAMES, false)) {
            return user.szUsername;
        }
        if (TextUtils.isEmpty(user.szNickname)) {
            return context.getString(R.string.fmt_anonymous_name, context.getString(R.string.pref_default_nickname), user.nUserID);
        }
        return user.szNickname;
    }


    public static boolean isWebLogin(String username) {
        return username.equals(AppInfo.WEBLOGIN_BEARWARE_USERNAME) ||
                username.endsWith(AppInfo.WEBLOGIN_BEARWARE_USERNAMEPOSTFIX);
    }

    public static void copyToClipboard(Context context, String label, String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, context.getString(R.string.text_copied_to_clipboard, label), Toast.LENGTH_SHORT).show();
    }

    public static String ttsChannelChange(Context context, Channel oldChan, Channel newChan) {
        if (oldChan == null || newChan == null) return "";
        if (oldChan.nChannelID == newChan.nChannelID) return "";

        String oldName = (oldChan.nChannelID <= 1 || "/".equals(oldChan.szName.trim())) ? 
            context.getString(R.string.text_tts_root_channel_name) : oldChan.szName;
            
        String newName = (newChan.nChannelID <= 1 || "/".equals(newChan.szName.trim())) ? 
            context.getString(R.string.text_tts_root_channel_name) : newChan.szName;

        if (oldChan.nChannelID <= 1 || "/".equals(oldChan.szName.trim())) {
             return context.getString(R.string.text_joined_channel, newName);
        }

        return context.getString(R.string.text_left_joined_channel, oldName, newName);
    }

    public static String getChannelPath(int channelID, Map<Integer, Channel> channels) {
        Channel chan = channels.get(channelID);
        if (chan == null) return "";
        if (chan.nParentID == 0) return "/";

        List<String> pathParts = new ArrayList<>();
        while (chan != null && chan.nParentID != 0) {
            pathParts.add(0, chan.szName);
            chan = channels.get(chan.nParentID);
        }

        StringBuilder sb = new StringBuilder();
        for (String part : pathParts) {
            sb.append("/").append(part);
        }
        return sb.toString();
    }

    public static String generateTTLink(ServerEntry server, String username, String password, String channelPath) {
        StringBuilder sb = new StringBuilder("tt://");
        sb.append(server.ipaddr);
        sb.append("?tcpport=").append(server.tcpport);
        sb.append("&udpport=").append(server.udpport);
        sb.append("&encrypted=").append(server.encrypted ? 1 : 0);
        if (username != null && !username.isEmpty()) {
            sb.append("&username=").append(Uri.encode(username));
        }
        if (password != null && !password.isEmpty()) {
            sb.append("&password=").append(Uri.encode(password));
        }
        if (channelPath != null && !channelPath.isEmpty()) {
            sb.append("&channel=").append(Uri.encode(channelPath));
        }
        return sb.toString();
    }

    public static boolean saveTTFileToDownloads(Context context, String xml, String filename) {
        if (xml == null || filename == null) return false;

        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, filename.endsWith(".tt") ? filename : filename + ".tt");
        values.put(MediaStore.Downloads.MIME_TYPE, "application/x-teamtalk-tt");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            values.put(MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);
        }

        Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) return false;

        try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
            if (os != null) {
                os.write(xml.getBytes(StandardCharsets.UTF_8));
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to save .tt file to Downloads", e);
        }
        return false;
    }

    public static String getChannelNameFromPath(String path) {
        if (path == null || path.isEmpty()) return "";
        int lastIndex = path.lastIndexOf("/");
        if (lastIndex == -1) return path;
        if (lastIndex == path.length() - 1) {
            // Path ends in /, try again with substring
            return getChannelNameFromPath(path.substring(0, path.length() - 1));
        }
        return path.substring(lastIndex + 1);
    }

    public interface OnChannelSelectedListener {
        void onChannelSelected(int channelId, String channelName);
    }

    public static void showChannelPicker(final Context context, final TeamTalkService service, final int titleResId, final int initialViewId, final int initialSelectedId, final OnChannelSelectedListener listener) {
        if (service == null) return;
        
        final Map<Integer, Channel> channels = service.getChannels();
        final List<String> items = new ArrayList<>();
        final List<Integer> itemChannelIds = new ArrayList<>();

        // Navigation / Back button
        if (initialViewId != 0) {
            Channel current = channels.get(initialViewId);
            if (current != null) {
                String parentName;
                int parentId = current.nParentID;
                if (parentId > 0) {
                    Channel parent = channels.get(parentId);
                    if (parent != null) {
                        boolean isParentInit = (parent.nChannelID == 1) || (parent.nChannelID == service.getTTInstance().getRootChannelID());
                        parentName = isParentInit ? context.getString(R.string.init_channel) : parent.szName;
                    } else {
                        parentName = context.getString(R.string.root_server);
                    }
                } else {
                    parentName = context.getString(R.string.root_server);
                }
                
                int population;
                if (parentId > 0) {
                    population = Utils.getUsers(parentId, service.getUsers(), true, channels).size();
                } else {
                    population = Utils.getUsers(0, service.getUsers()).size();
                }

                items.add(parentName + " (" + population + ")");
                itemChannelIds.add(-1);
            }
        }

        List<Channel> children = new ArrayList<>();
        if (initialViewId == 0) {
            // Server Root (ID 0) only shows the "Initial Channel"
            int rootChanId = service.getTTInstance().getRootChannelID();
            Channel root = channels.get(rootChanId);
            if (root != null) children.add(root);
        } else {
            // Other channels show their children
            for (Channel c : channels.values()) {
                if (c.nParentID == initialViewId && c.nChannelID != 0) {
                    children.add(c);
                }
            }
        }

        Collections.sort(children, (c1, c2) -> c1.szName.compareToIgnoreCase(c2.szName));

        for (Channel c : children) {
            String check = (initialSelectedId == c.nChannelID) ? context.getString(R.string.indicator_selected) : "";
            
            int population = Utils.getUsers(c.nChannelID, service.getUsers()).size();
            int populationSub = Utils.getUsers(c.nChannelID, service.getUsers(), true, channels).size();
            
            String countStr;
            if (Utils.hasSubChannels(c.nChannelID, channels)) {
                countStr = String.format(Locale.ROOT, "(%d/%d)", population, populationSub);
            } else {
                countStr = String.format(Locale.ROOT, "(%d)", population);
            }

            boolean isInit = (c.nChannelID == 1) || (c.nChannelID == service.getTTInstance().getRootChannelID());
            String name = isInit ? context.getString(R.string.init_channel) : (c.szName.isEmpty() ? context.getString(R.string.no_name) : c.szName);

            items.add(name + " " + countStr + check);
            itemChannelIds.add(c.nChannelID);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        
        Channel currentViewChannel = channels.get(initialViewId);
        String currentViewName;
        if (currentViewChannel != null) {
            boolean isInit = (currentViewChannel.nChannelID == 1) || (currentViewChannel.nChannelID == service.getTTInstance().getRootChannelID());
            currentViewName = isInit ? context.getString(R.string.init_channel) : currentViewChannel.szName;
        } else {
            currentViewName = context.getString(R.string.root_server);
        }

        builder.setTitle(context.getString(R.string.fmt_label_value, context.getString(R.string.current_channel_prefix), currentViewName));

        builder.setItems(items.toArray(new String[0]), (dialog, which) -> {
            int clickedId = itemChannelIds.get(which);
            if (clickedId == -1) {
                // Back
                int nextId = 0;
                if (currentViewChannel != null && currentViewChannel.nParentID >= 0) {
                    nextId = currentViewChannel.nParentID;
                }
                showChannelPicker(context, service, titleResId, nextId, initialSelectedId, listener);
            } else {
                // Dive into child
                showChannelPicker(context, service, titleResId, clickedId, initialSelectedId, listener);
            }
        });

        builder.setPositiveButton(context.getString(R.string.action_select) + ": " + currentViewName, (dialog, which) -> {
            listener.onChannelSelected(initialViewId, currentViewName);
        });

        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }
}
