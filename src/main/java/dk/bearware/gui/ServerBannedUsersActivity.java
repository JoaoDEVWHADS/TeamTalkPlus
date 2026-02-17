package dk.bearware.gui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.CheckedTextView;
import android.widget.Button;

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

import dk.bearware.BanType;
import dk.bearware.BannedUser;
import dk.bearware.backend.TeamTalkConnection;
import dk.bearware.backend.TeamTalkConnectionListener;
import dk.bearware.backend.TeamTalkService;
import dk.bearware.events.ClientEventListener;

public class ServerBannedUsersActivity extends AppCompatActivity implements TeamTalkConnectionListener,
        ClientEventListener.OnCmdBannedUserListener {

    private TeamTalkConnection ttConnection;
    private ListView bannedUsersListView;
    private TextView emptyView;
    private BannedUserAdapter adapter;
    private List<BannedUser> bannedUsers = new ArrayList<>();
    private Set<Integer> selectedPositions = new HashSet<>(); 
    
    private int channelId = 0; // Default to 0 for server bans

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_banned_users);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_activity_server_banned_users);
        }
        Utils.announceAccessibilityTitle(this, R.string.title_activity_server_banned_users);

        bannedUsersListView = findViewById(R.id.list_banned_users);
        emptyView = findViewById(android.R.id.empty);
        emptyView.setText(R.string.msg_no_banned_users); // Generic "no banned users"
        bannedUsersListView.setEmptyView(emptyView);

        adapter = new BannedUserAdapter();
        bannedUsersListView.setAdapter(adapter);
        
        Button btnSelectAll = findViewById(R.id.btn_select_all);
        Button btnSelectNone = findViewById(R.id.btn_select_none);
        Button btnUnban = findViewById(R.id.btn_unban);

        ttConnection = new TeamTalkConnection(this);
        
        bannedUsersListView.setOnItemClickListener((parent, view, position, id) -> {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove(position);
            } else {
                selectedPositions.add(position);
            }
            adapter.notifyDataSetChanged();
        });
        
        btnSelectAll.setOnClickListener(v -> {
            for (int i = 0; i < bannedUsers.size(); i++) {
                selectedPositions.add(i);
            }
            adapter.notifyDataSetChanged();
        });

        btnSelectNone.setOnClickListener(v -> {
            selectedPositions.clear();
            adapter.notifyDataSetChanged();
        });

        btnUnban.setOnClickListener(v -> executeBatchUnban());
    }

    private void executeBatchUnban() {
        if (selectedPositions.isEmpty()) {
            Toast.makeText(this, R.string.err_no_banned_users_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.action_unban)
                .setMessage(getString(R.string.confirm_unban_user, selectedPositions.size() + " users"))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                     if (ttConnection.getService() != null) {
                          int unbannedCount = 0;
                          for (Integer pos : selectedPositions) {
                              if (pos < bannedUsers.size()) {
                                  BannedUser u = bannedUsers.get(pos);
                                  ttConnection.getService().getTTInstance().doUnBanUserEx(u);
                                  unbannedCount++;
                              }
                          }
                          
                          Toast.makeText(this, getString(R.string.msg_users_unbanned, unbannedCount), Toast.LENGTH_SHORT).show();

                          selectedPositions.clear();
                          bannedUsers.clear();
                          adapter.notifyDataSetChanged();
                          ttConnection.getService().getTTInstance().doListBans(channelId, 0, 500);
                     }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
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
        service.getEventHandler().registerOnCmdBannedUser(this, true);
        service.getTTInstance().doListBans(channelId, 0, 500);
    }

    @Override
    public void onServiceDisconnected(TeamTalkService service) {}

    @Override
    public void onCmdBannedUser(BannedUser banneduser) {
        boolean exists = false;
        for(BannedUser u : bannedUsers) {
            if (u.szNickname.equals(banneduser.szNickname) && u.szIPAddress.equals(banneduser.szIPAddress)) {
                exists = true;
                break;
            }
        }
        
        if (!exists) { 
             bannedUsers.add(banneduser);
             runOnUiThread(() -> adapter.notifyDataSetChanged());
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private class BannedUserAdapter extends BaseAdapter {
        @Override
        public int getCount() { return bannedUsers.size(); }
        @Override
        public Object getItem(int position) { return bannedUsers.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ServerBannedUsersActivity.this).inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
            }
            
            CheckedTextView text1 = (CheckedTextView) convertView.findViewById(android.R.id.text1);
            
            BannedUser u = bannedUsers.get(position);
            
            String banType = getBanTypeString(u.uBanTypes);
            String owner = (u.szOwner != null && !u.szOwner.isEmpty()) ? u.szOwner : "SemNome";
            String details = getString(R.string.ban_info_fmt, u.szBanTime, banType, owner);
            
            String displayName = (u.szNickname != null && !u.szNickname.isEmpty()) ? u.szNickname : "SemNome";
            String title = displayName + " (" + u.szIPAddress + ")";
            if (u.szUsername != null && !u.szUsername.isEmpty()) {
                 title += " / " + u.szUsername;
            }
            
            text1.setText(title + "\n" + details);
            text1.setChecked(selectedPositions.contains(position));
            
            return convertView;
        }

        private String getBanTypeString(int uBanTypes) {
            List<String> types = new ArrayList<>();
            if ((uBanTypes & BanType.BANTYPE_IPADDR) != 0) types.add(getString(R.string.ban_type_ip));
            if ((uBanTypes & BanType.BANTYPE_USERNAME) != 0) types.add(getString(R.string.ban_type_username));

            if (types.isEmpty()) return "Unknown";
            return android.text.TextUtils.join(", ", types);
        }
    }
}
