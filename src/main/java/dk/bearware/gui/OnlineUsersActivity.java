
package dk.bearware.gui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dk.bearware.ClientEvent;
import dk.bearware.Channel;
import dk.bearware.User;
import dk.bearware.ClientFlag;
import dk.bearware.ClientStatistics;
import dk.bearware.TeamTalkBase;
import dk.bearware.UserAccount;
import dk.bearware.UserRight;
import dk.bearware.UserState;
import dk.bearware.backend.TeamTalkConnection;
import dk.bearware.backend.TeamTalkConnectionListener;
import dk.bearware.backend.TeamTalkService;
import dk.bearware.events.ClientEventListener;

public class OnlineUsersActivity extends AppCompatActivity implements
        ClientEventListener.OnCmdUserLoggedInListener,
        ClientEventListener.OnCmdUserLoggedOutListener,
        ClientEventListener.OnCmdUserJoinedChannelListener,
        ClientEventListener.OnCmdUserLeftChannelListener,
        ClientEventListener.OnCmdUserUpdateListener, TeamTalkConnectionListener {

    private static final String TAG = "OnlineUsersActivity";

    private TeamTalkConnection mConnection;
    private ListView onlineUsersList;
    private OnlineUsersAdapter adapter;
    private final ArrayList<User> onlineUsers = new ArrayList<>();

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
        setContentView(R.layout.activity_online_users);
        EdgeToEdgeHelper.enableEdgeToEdge(this);

        onlineUsersList = findViewById(R.id.online_users_list);
        adapter = new OnlineUsersAdapter(this, getService(), onlineUsers);
        onlineUsersList.setAdapter(adapter);

        onlineUsersList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                User selectedUser = onlineUsers.get(position);
                Intent intent = new Intent(OnlineUsersActivity.this, UserPropActivity.class);
                intent.putExtra(UserPropActivity.EXTRA_USERID, selectedUser.nUserID);
                startActivity(intent);
            }
        });

        onlineUsersList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                User selectedUser = onlineUsers.get(position);
                AlertDialog.Builder alert = new AlertDialog.Builder(OnlineUsersActivity.this);
                UserAccount myuseraccount = new UserAccount();
                getClient().getMyUserAccount(myuseraccount);
                boolean banRight = (myuseraccount.uUserRights & UserRight.USERRIGHT_BAN_USERS) != UserRight.USERRIGHT_NONE;
                boolean kickRight = (myuseraccount.uUserRights & UserRight.USERRIGHT_KICK_USERS) != UserRight.USERRIGHT_NONE;

                int myuserid = getClient().getMyUserID();
                boolean operatorRight = getClient().isChannelOperator(myuserid, selectedUser.nChannelID);

                PopupMenu onlineActions = new PopupMenu(OnlineUsersActivity.this, v);

                onlineActions.inflate(R.menu.online_actions);

                onlineActions.getMenu().add(0, 1001, 0, getString(R.string.info_copy_name));
                onlineActions.getMenu().add(0, 1002, 0, getString(R.string.info_copy_id));
                onlineActions.getMenu().add(0, 1003, 0, getString(R.string.info_copy_ip));

                onlineActions.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case 1001:
                            Utils.copyToClipboard(OnlineUsersActivity.this, getString(R.string.label_user_name), selectedUser.szNickname);
                            return true;
                        case 1002:
                            Utils.copyToClipboard(OnlineUsersActivity.this, getString(R.string.label_user_id), String.valueOf(selectedUser.nUserID));
                            return true;
                        case 1003:
                             Utils.copyToClipboard(OnlineUsersActivity.this, getString(R.string.label_user_ip), selectedUser.szIPAddress);
                            return true;

                        case R.id.action_edituser: {
                            Intent intent = new Intent(OnlineUsersActivity.this, UserPropActivity.class);
                            startActivity(intent.putExtra(UserPropActivity.EXTRA_USERID, selectedUser.nUserID));
                        }
                            return true;
                        case R.id.action_message: {
                            Intent intent = new Intent(OnlineUsersActivity.this, TextMessageActivity.class);
                            startActivity(intent.putExtra(TextMessageActivity.EXTRA_USERID, selectedUser.nUserID));
                        }
                            return true;
                        case R.id.action_banchan:
                confirmAction(alert, R.string.ban_confirmation, selectedUser,
                        () -> banAndKick(selectedUser, selectedUser.nChannelID));
                            return true;
                        case R.id.action_bansrv:
                confirmAction(alert, R.string.ban_confirmation, selectedUser,
                        () -> banAndKick(selectedUser, 0));
                            return true;
                        case R.id.action_kickchan:
                confirmAction(alert, R.string.kick_confirmation, selectedUser,
                        () -> getClient().doKickUser(selectedUser.nUserID, selectedUser.nChannelID));
                            return true;
                        case R.id.action_kicksrv:
                confirmAction(alert, R.string.kick_confirmation, selectedUser,
                        () -> getClient().doKickUser(selectedUser.nUserID, 0));
                            return true;
                        case R.id.action_makeop:
                boolean isOp = getClient().isChannelOperator(selectedUser.nUserID, selectedUser.nChannelID);
                            if ((myuseraccount.uUserRights & UserRight.USERRIGHT_OPERATOR_ENABLE) != UserRight.USERRIGHT_NONE) {
                                getClient().doChannelOp(selectedUser.nUserID, selectedUser.nChannelID, !isOp);
                                return true;
                            }
                            alert.setTitle(!isOp ? R.string.action_revoke_operator : R.string.action_make_operator);
                            alert.setMessage(R.string.text_operator_password);
                            final EditText input = new EditText(OnlineUsersActivity.this);
                            input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
                            alert.setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                    getClient().doChannelOpEx(selectedUser.nUserID, selectedUser.nChannelID, input.getText().toString(), !isOp));
                            alert.setNegativeButton(android.R.string.no, null);
                            alert.setView(input);
                            alert.show();
                            return true;

                        default:
                            return false;
                    }
                });

            onlineActions.getMenu().findItem(R.id.action_kickchan).setEnabled(kickRight | operatorRight).setVisible(kickRight | operatorRight);
            onlineActions.getMenu().findItem(R.id.action_kicksrv).setEnabled(kickRight).setVisible(kickRight);
            onlineActions.getMenu().findItem(R.id.action_banchan).setEnabled(banRight | operatorRight).setVisible(banRight | operatorRight);
            onlineActions.getMenu().findItem(R.id.action_bansrv).setEnabled(banRight).setVisible(banRight);
            onlineActions.getMenu().findItem(R.id.action_makeop).setTitle(getClient().isChannelOperator(selectedUser.nUserID , selectedUser.nChannelID) ? R.string.action_revoke_operator : R.string.action_make_operator);

                onlineActions.show();
                return true;
            }
        });

        Intent intent = new Intent(this, TeamTalkService.class);
        if(!bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Failed to connect to TeamTalk service");
        }
    }

    @Override
    protected void onDestroy() {
        if (mConnection.isBound()) {
            getService().getEventHandler().unregisterListener(this);
            unbindService(mConnection);
        }
        super.onDestroy();
    }

    private void confirmAction(AlertDialog.Builder alert, int messageResId, User user, Runnable action) {
        alert.setMessage(getString(messageResId, user.szNickname));
        alert.setPositiveButton(android.R.string.yes, (dialog, which) -> action.run());
        alert.setNegativeButton(android.R.string.no, null);
        alert.show();
    }

    private void banAndKick(User user, int channelId) {
        getClient().doBanUser(user.nUserID, channelId);
        getClient().doKickUser(user.nUserID, channelId);
    }

    private void registerEventListeners() {
        getService().getEventHandler().registerOnCmdUserLoggedIn(this, true);
        getService().getEventHandler().registerOnCmdUserLoggedOut(this, true);
        getService().getEventHandler().registerOnCmdUserJoinedChannel(this, true);
        getService().getEventHandler().registerOnCmdUserLeftChannel(this, true);
        getService().getEventHandler().registerOnCmdUserUpdate(this, true);
    }

    private void populateUserList() {
        onlineUsers.clear();
        onlineUsers.addAll(getService().getUsers().values());
        sortAndNotifyDataSetChanged();
    }

    private void sortAndNotifyDataSetChanged() {
        onlineUsers.sort(Comparator.comparing(u -> u.szNickname.toLowerCase(Locale.ROOT)));
        adapter.notifyDataSetChanged();
    }

    private int findUserIndex(User user) {
        for (int i = 0; i < onlineUsers.size(); i++) {
            if (onlineUsers.get(i).nUserID == user.nUserID) {
                return i;
            }
        }
        return -1;
    }

    private void updateUser(User user) {
        int index = findUserIndex(user);
        if (index != -1) {
            onlineUsers.set(index, user);
        } else {
            onlineUsers.add(user);
        }
        sortAndNotifyDataSetChanged();
    }

    @Override
    public void onCmdUserLoggedIn(User user) {
        Log.d(TAG, "User joined: " + user.szNickname);
        updateUser(user);
    }

    @Override
    public void onCmdUserLoggedOut(User user) {
        Log.d(TAG, "User left: " + user.szNickname);
        int index = findUserIndex(user);
        if (index != -1) {
            onlineUsers.remove(index);
        }
        sortAndNotifyDataSetChanged();
    }

    @Override
    public void onCmdUserJoinedChannel(User user) {
        Log.d(TAG, "User " + user.szNickname + " entrou no canal " + user.nChannelID);
        updateUser(user);
    }

    @Override
    public void onCmdUserLeftChannel(int nChannelID, User user) {
        Log.d(TAG, "User " + user.szNickname + " saiu do canal " + nChannelID);
        updateUser(user);
    }

    @Override
    public void onCmdUserUpdate(User user) {
        Log.d(TAG, "User updated: " + user.szNickname);
        updateUser(user);
    }

    @Override
    public void onServiceConnected(TeamTalkService service) {
        registerEventListeners();
        populateUserList();
    }

    @Override
    public void onServiceDisconnected(TeamTalkService service) {

    }

}