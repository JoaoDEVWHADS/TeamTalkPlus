package dk.bearware.gui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dk.bearware.Channel;
import dk.bearware.User;
import dk.bearware.events.ClientEventListener;
import dk.bearware.backend.TeamTalkConnection;
import dk.bearware.backend.TeamTalkConnectionListener;
import dk.bearware.backend.TeamTalkService;

public class MoveUsersActivity extends AppCompatActivity implements TeamTalkConnectionListener,
        ClientEventListener.OnCmdUserUpdateListener,
        ClientEventListener.OnCmdUserJoinedChannelListener,
        ClientEventListener.OnCmdUserLeftChannelListener,
        ClientEventListener.OnCmdUserLoggedOutListener,
        ClientEventListener.OnCmdUserLoggedInListener,
        ClientEventListener.OnCmdChannelNewListener,
        ClientEventListener.OnCmdChannelUpdateListener,
        ClientEventListener.OnCmdChannelRemoveListener {

    private TeamTalkConnection ttConnection;
    private ListView usersListView;
    private RadioButton radioServer, radioChannel, radioPickChannel;
    private TextView tvSourceChannel;
    private Button btnMove;

    private List<User> filteredUsers = new ArrayList<>();
    private int selectedSourceChannelId = -1;
    
    
    private int currentViewId = 0;      
    private int selectedTargetId = 0;   
    
    private Set<Integer> selectedUserIds = new HashSet<>();

    private UserAdapter userAdapter;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_users);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_move_users);
        }
        Utils.announceAccessibilityTitle(this, R.string.title_move_users);

        usersListView = findViewById(R.id.list_users);
        radioServer = findViewById(R.id.radio_filter_server);
        radioChannel = findViewById(R.id.radio_filter_channels);
        radioPickChannel = findViewById(R.id.radio_filter_pick_channel);
        tvSourceChannel = findViewById(R.id.tv_source_channel);

        btnMove = findViewById(R.id.btn_execute_move);
        Button btnSelectAll = findViewById(R.id.btn_select_all);
        Button btnSelectNone = findViewById(R.id.btn_select_none);

        ttConnection = new TeamTalkConnection(this);

        userAdapter = new UserAdapter();
        usersListView.setAdapter(userAdapter);

        usersListView.setOnItemClickListener((parent, view, position, id) -> {
            User u = filteredUsers.get(position);
            if (selectedUserIds.contains(u.nUserID)) {
                selectedUserIds.remove(u.nUserID);
            } else {
                selectedUserIds.add(u.nUserID);
            }
            userAdapter.notifyDataSetChanged();
        });

        radioServer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedUserIds.clear();
                tvSourceChannel.setText("");
                selectedSourceChannelId = -1;
                refreshData();
            }
        });

        radioChannel.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedUserIds.clear();
                tvSourceChannel.setText("");
                selectedSourceChannelId = -1;
                refreshData();
            }
        });

        radioPickChannel.setOnClickListener(v -> {
            selectedUserIds.clear();
            showSourceChannelPicker();
        });

        btnSelectAll.setOnClickListener(v -> {
            for (User u : filteredUsers) selectedUserIds.add(u.nUserID);
            userAdapter.notifyDataSetChanged();
        });

        btnSelectNone.setOnClickListener(v -> {
            selectedUserIds.clear();
            userAdapter.notifyDataSetChanged();
        });

        btnMove.setOnClickListener(v -> executeMove());
    }

    private void showSourceChannelPicker() {
        currentViewId = 0;
        selectedTargetId = 0;
        showChannelPicker(R.string.dialog_select_source_channel, true);
    }

    
    private void showChannelPicker(final int titleResId, final boolean isSource) {
        if (ttConnection.getService() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        Map<Integer, Channel> channels = ttConnection.getService().getChannels();
        
        
        if (!channels.containsKey(currentViewId)) currentViewId = 0;
        
        
        Channel currentViewChannel = channels.get(currentViewId);
        String currentViewName = "?";
        if (currentViewChannel != null) {
            currentViewName = (currentViewChannel.nChannelID == 0) ? getString(R.string.init_channel) : currentViewChannel.szName;
        }
        
        
        Channel targetChannel = channels.get(selectedTargetId);
        final String targetName;
        if (targetChannel != null) {
            targetName = (targetChannel.nChannelID == 0) ? getString(R.string.init_channel) : targetChannel.szName;
        } else {
            targetName = "?";
        }

        builder.setTitle(getString(R.string.current_channel_prefix) + currentViewName);

        List<String> items = new ArrayList<>();
        List<Integer> itemChannelIds = new ArrayList<>(); 

        
        boolean canGoUp = (currentViewId != 0);
        if (canGoUp) {
            items.add(".. (" + getString(R.string.action_leave) + ")");
            itemChannelIds.add(-1); 
        }

        
        List<Channel> children = new ArrayList<>();
        for (Channel c : channels.values()) {
            if (c.nParentID == currentViewId && c.nChannelID != 0) {
                children.add(c);
            }
        }
        
        if (currentViewId == 0) {
            Channel root = channels.get(0);
            if (root != null) {
                
                String check = (selectedTargetId == 0) ? " [X]" : "";
                int count = 0;
                for (User u : ttConnection.getService().getUsers().values()) if (u.nChannelID == 0) count++;
                items.add(getString(R.string.init_channel) + " (" + count + ")" + check);
                itemChannelIds.add(0);
            }
        }

        Collections.sort(children, (c1, c2) -> c1.szName.compareToIgnoreCase(c2.szName));

        for (Channel c : children) {
            String check = (selectedTargetId == c.nChannelID) ? " [X]" : "";
            int count = 0;
            for (User u : ttConnection.getService().getUsers().values()) if (u.nChannelID == c.nChannelID) count++;
            
            items.add(c.szName + " (" + count + ")" + check);
            itemChannelIds.add(c.nChannelID);
        }

        builder.setItems(items.toArray(new String[0]), (dialog, which) -> {
            int clickedId = itemChannelIds.get(which);
            
            if (clickedId == -1) {
                
                if (currentViewChannel != null && currentViewChannel.nParentID >= 0) {
                    currentViewId = currentViewChannel.nParentID;
                } else {
                    currentViewId = 0;
                }
                showChannelPicker(titleResId, isSource);
            } else {
                
                selectedTargetId = clickedId;
                
                
                currentViewId = clickedId;
                showChannelPicker(titleResId, isSource);
            }
        });

        
        String actionLabel = isSource ? getString(R.string.action_select) : getString(R.string.action_move);
        builder.setPositiveButton(actionLabel + ": " + targetName, (dialog, which) -> {
             if (isSource) {
                 selectedSourceChannelId = selectedTargetId;
                 tvSourceChannel.setText(getString(R.string.current_channel_prefix) + targetName);
                 refreshData();
             } else {
                 performMove(selectedTargetId);
             }
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            if(isSource && selectedSourceChannelId == -1) {
                radioServer.setChecked(true);
            }
        });

        builder.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, TeamTalkService.class);
        bindService(intent, ttConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ttConnection.isBound()) {
            if(ttConnection.getService() != null) {
                ttConnection.getService().getEventHandler().unregisterListener(this);
            }
            unbindService(ttConnection);
        }
    }

    @Override
    public void onServiceConnected(TeamTalkService service) {
        service.getEventHandler().registerOnCmdUserUpdate(this, true);
        service.getEventHandler().registerOnCmdUserJoinedChannel(this, true);
        service.getEventHandler().registerOnCmdUserLeftChannel(this, true);
        service.getEventHandler().registerOnCmdUserLoggedIn(this, true);
        service.getEventHandler().registerOnCmdUserLoggedOut(this, true);
        service.getEventHandler().registerOnCmdChannelNew(this, true);
        service.getEventHandler().registerOnCmdChannelUpdate(this, true);
        service.getEventHandler().registerOnCmdChannelRemove(this, true);
        refreshData();
    }

    @Override
    public void onServiceDisconnected(TeamTalkService service) {}

    @Override
    public void onCmdUserUpdate(User user) { refreshData(); }

    @Override
    public void onCmdUserJoinedChannel(User user) { refreshData(); }

    @Override
    public void onCmdUserLeftChannel(int i, User user) { refreshData(); }

    @Override
    public void onCmdUserLoggedOut(User user) { refreshData(); }

    @Override
    public void onCmdUserLoggedIn(User user) { refreshData(); }

    @Override
    public void onCmdChannelNew(Channel channel) { refreshData(); }

    @Override
    public void onCmdChannelUpdate(Channel channel) { refreshData(); }

    @Override
    public void onCmdChannelRemove(Channel channel) { refreshData(); }

    private void refreshData() {
        if (ttConnection.getService() == null) return;

        Map<Integer, User> usersMap = ttConnection.getService().getUsers();
        int myChannelId = ttConnection.getService().getTTInstance().getMyChannelID();

        filteredUsers.clear();

        if (radioServer.isChecked()) {
            for (User u : usersMap.values()) {
                filteredUsers.add(u);
            }
        } else if (radioChannel.isChecked()) {
             for (User u : usersMap.values()) {
                if (u.nChannelID == myChannelId) {
                    filteredUsers.add(u);
                }
            }
        } else if (radioPickChannel.isChecked() && selectedSourceChannelId != -1) {
            for (User u : usersMap.values()) {
                if (u.nChannelID == selectedSourceChannelId) {
                    filteredUsers.add(u);
                }
            }
        }

        Collections.sort(filteredUsers, (u1, u2) -> getNick(u1).compareToIgnoreCase(getNick(u2)));
        userAdapter.notifyDataSetChanged();
    }

    private String getNick(User u) {
         return Utils.getDisplayName(this, u);
    }

    private void executeMove() {
        if (ttConnection.getService() == null) return;
        if (selectedUserIds.isEmpty()) {
            Toast.makeText(this, R.string.err_no_users_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        currentViewId = 0;
        selectedTargetId = 0;
        showChannelPicker(R.string.title_select_destination, false);
    }

    private void performMove(int targetChannelId) {
        if (ttConnection.getService() == null) return;

        for (Integer uid : selectedUserIds) {
            ttConnection.getService().getTTInstance().doMoveUser(uid, targetChannelId);
        }

        Toast.makeText(this, getString(R.string.msg_users_moved, selectedUserIds.size()), Toast.LENGTH_SHORT).show();
        selectedUserIds.clear();
        refreshData();
    }

    private class UserAdapter extends BaseAdapter {
        @Override
        public int getCount() { return filteredUsers.size(); }
        @Override
        public Object getItem(int position) { return filteredUsers.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MoveUsersActivity.this).inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
            }
            android.widget.CheckedTextView ctv = (android.widget.CheckedTextView) convertView;

            User u = filteredUsers.get(position);
            ctv.setText(getNick(u));
            ctv.setChecked(selectedUserIds.contains(u.nUserID));

            return convertView;
        }
    }
}