package dk.bearware.gui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import dk.bearware.ServerProperties;
import dk.bearware.ServerLogEvent;
import dk.bearware.TeamTalkBase;
import dk.bearware.backend.TeamTalkConnection;
import dk.bearware.backend.TeamTalkConnectionListener;
import dk.bearware.backend.TeamTalkService;

public class ServerPropActivity extends AppCompatActivity implements TeamTalkConnectionListener {

    private static final String TAG = "ServerPropActivity";

    private TeamTalkConnection mConnection;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Button btnSave;
    private ServerPropPagerAdapter pagerAdapter;
    private ServerProperties mProps = new ServerProperties();

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
        setContentView(R.layout.activity_server_properties);
        EdgeToEdgeHelper.enableEdgeToEdge(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.pref_title_server_info);
        }
        Utils.announceAccessibilityTitle(this, R.string.pref_title_server_info);

        tabLayout = findViewById(R.id.server_prop_tabs);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        viewPager = findViewById(R.id.server_prop_viewpager);
        btnSave = findViewById(R.id.btn_save_server_props);

        pagerAdapter = new ServerPropPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(4);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText(R.string.tab_server_properties); break;
                case 1: tab.setText(R.string.tab_bandwidth_limits); break;
                case 2: tab.setText(R.string.tab_abuse_prevention); break;
                case 3: tab.setText(R.string.tab_logging); break;
            }
        }).attach();

        btnSave.setOnClickListener(v -> saveServerProperties());

        mConnection = new TeamTalkConnection(this);
        Intent intent = new Intent(this, TeamTalkService.class);
        if (!bindService(intent, mConnection, BIND_AUTO_CREATE)) {
            Log.e(TAG, "Failed to connect to TeamTalk service");
        }
    }

    @Override
    protected void onDestroy() {
        if (mConnection.isBound()) {
            unbindService(mConnection);
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveServerProperties() {

        for (int i = 0; i < pagerAdapter.getItemCount(); i++) {
            Fragment f = getSupportFragmentManager().findFragmentByTag("f" + i);
            if (f instanceof ServerPropFragment) {
                ((ServerPropFragment) f).updateProperties(mProps);
            }
        }

        int cmdId = getClient().doUpdateServer(mProps);
        if (cmdId > 0) {
            Toast.makeText(this, R.string.text_cmd_processing, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.err_update_server_props_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onServiceConnected(TeamTalkService service) {
        if (getClient().getServerProperties(mProps)) {

            refreshFragments();
        }
    }

    private void refreshFragments() {
        for (Fragment f : getSupportFragmentManager().getFragments()) {
            if (f instanceof ServerPropFragment) {
                ((ServerPropFragment) f).refreshUI(mProps);
            }
        }
    }

    @Override
    public void onServiceDisconnected(TeamTalkService service) {}

    public interface ServerPropFragment {
        void refreshUI(ServerProperties props);
        void updateProperties(ServerProperties props);
    }

    private class ServerPropPagerAdapter extends FragmentStateAdapter {
        public ServerPropPagerAdapter(AppCompatActivity activity) {
            super(activity);
        }
        @NonNull @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new GeneralPropFragment();
                case 1: return new BandwidthPropFragment();
                case 2: return new AbusePropFragment();
                case 3: return new LoggingPropFragment();
                default: return new GeneralPropFragment();
            }
        }
        @Override
        public int getItemCount() { return 4; }
    }

    public static class GeneralPropFragment extends Fragment implements ServerPropFragment {
        EditText editName, editMOTD, editMaxUsers, editMaxLogins, editTcp, editUdp;
        CheckBox chkAutoSave;
        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
            android.view.View v = inflater.inflate(R.layout.fragment_server_prop_general, container, false);
            editName = v.findViewById(R.id.edit_server_name);
            editMOTD = v.findViewById(R.id.edit_server_motd);
            editMaxUsers = v.findViewById(R.id.edit_server_max_users);
            editMaxLogins = v.findViewById(R.id.edit_server_max_logins_ip);
            editTcp = v.findViewById(R.id.edit_server_tcp);
            editUdp = v.findViewById(R.id.edit_server_udp);
            chkAutoSave = v.findViewById(R.id.chk_server_autosave);

            ServerPropActivity activity = (ServerPropActivity) getActivity();
            if (activity != null && activity.mProps != null) refreshUI(activity.mProps);
            return v;
        }
        @Override
        public void refreshUI(ServerProperties props) {
            if (editName == null) return;
            editName.setText(props.szServerName);
            editMOTD.setText(props.szMOTD);
            editMaxUsers.setText(String.valueOf(props.nMaxUsers));
            editMaxLogins.setText(String.valueOf(props.nMaxLoginsPerIPAddress));
            editTcp.setText(String.valueOf(props.nTcpPort));
            editUdp.setText(String.valueOf(props.nUdpPort));
            chkAutoSave.setChecked(props.bAutoSave);
        }
        @Override
        public void updateProperties(ServerProperties props) {
            if (editName == null) return;
            props.szServerName = editName.getText().toString();
            props.szMOTD = editMOTD.getText().toString();
            try {
                props.nMaxUsers = Integer.parseInt(editMaxUsers.getText().toString());
                props.nMaxLoginsPerIPAddress = Integer.parseInt(editMaxLogins.getText().toString());
                props.nTcpPort = Integer.parseInt(editTcp.getText().toString());
                props.nUdpPort = Integer.parseInt(editUdp.getText().toString());
            } catch (NumberFormatException ignored) {}
            props.bAutoSave = chkAutoSave.isChecked();
        }
    }

    public static class BandwidthPropFragment extends Fragment implements ServerPropFragment {
        EditText editVoice, editVideo, editMedia, editDesktop, editTotal;
        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
            android.view.View v = inflater.inflate(R.layout.fragment_server_prop_bandwidth, container, false);
            editVoice = v.findViewById(R.id.edit_max_voice);
            editVideo = v.findViewById(R.id.edit_max_video);
            editMedia = v.findViewById(R.id.edit_max_media);
            editDesktop = v.findViewById(R.id.edit_max_desktop);
            editTotal = v.findViewById(R.id.edit_max_total);
            ServerPropActivity activity = (ServerPropActivity) getActivity();
            if (activity != null && activity.mProps != null) refreshUI(activity.mProps);
            return v;
        }
        @Override
        public void refreshUI(ServerProperties props) {
            if (editVoice == null) return;
            editVoice.setText(String.valueOf(props.nMaxVoiceTxPerSecond));
            editVideo.setText(String.valueOf(props.nMaxVideoCaptureTxPerSecond));
            editMedia.setText(String.valueOf(props.nMaxMediaFileTxPerSecond));
            editDesktop.setText(String.valueOf(props.nMaxDesktopTxPerSecond));
            editTotal.setText(String.valueOf(props.nMaxTotalTxPerSecond));
        }
        @Override
        public void updateProperties(ServerProperties props) {
            if (editVoice == null) return;
            try {
                props.nMaxVoiceTxPerSecond = Integer.parseInt(editVoice.getText().toString());
                props.nMaxVideoCaptureTxPerSecond = Integer.parseInt(editVideo.getText().toString());
                props.nMaxMediaFileTxPerSecond = Integer.parseInt(editMedia.getText().toString());
                props.nMaxDesktopTxPerSecond = Integer.parseInt(editDesktop.getText().toString());
                props.nMaxTotalTxPerSecond = Integer.parseInt(editTotal.getText().toString());
            } catch (NumberFormatException ignored) {}
        }
    }

    public static class AbusePropFragment extends Fragment implements ServerPropFragment {
        EditText editAttempts, editDelay, editTimeout;
        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
            android.view.View v = inflater.inflate(R.layout.fragment_server_prop_abuse, container, false);
            editAttempts = v.findViewById(R.id.edit_max_login_attempts);
            editDelay = v.findViewById(R.id.edit_login_delay);
            editTimeout = v.findViewById(R.id.edit_user_timeout);
            ServerPropActivity activity = (ServerPropActivity) getActivity();
            if (activity != null && activity.mProps != null) refreshUI(activity.mProps);
            return v;
        }
        @Override
        public void refreshUI(ServerProperties props) {
            if (editAttempts == null) return;
            editAttempts.setText(String.valueOf(props.nMaxLoginAttempts));
            editDelay.setText(String.valueOf(props.nLoginDelayMSec));
            editTimeout.setText(String.valueOf(props.nUserTimeout));
        }
        @Override
        public void updateProperties(ServerProperties props) {
            if (editAttempts == null) return;
            try {
                props.nMaxLoginAttempts = Integer.parseInt(editAttempts.getText().toString());
                props.nLoginDelayMSec = Integer.parseInt(editDelay.getText().toString());
                props.nUserTimeout = Integer.parseInt(editTimeout.getText().toString());
            } catch (NumberFormatException ignored) {}
        }
    }

    public static class LoggingPropFragment extends Fragment implements ServerPropFragment {
        CheckBox chkLogin, chkKick, chkChan, chkSrv, chkFile;
        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
            android.view.View v = inflater.inflate(R.layout.fragment_server_prop_logging, container, false);
            chkLogin = v.findViewById(R.id.chk_log_user_login);
            chkKick = v.findViewById(R.id.chk_log_user_kick);
            chkChan = v.findViewById(R.id.chk_log_channel_create);
            chkSrv = v.findViewById(R.id.chk_log_server_update);
            chkFile = v.findViewById(R.id.chk_log_file_transfer);
            ServerPropActivity activity = (ServerPropActivity) getActivity();
            if (activity != null && activity.mProps != null) refreshUI(activity.mProps);
            return v;
        }
        @Override
        public void refreshUI(ServerProperties props) {
            if (chkLogin == null) return;
            int e = props.uServerLogEvents;
            chkLogin.setChecked((e & (ServerLogEvent.SERVERLOGEVENT_USER_CONNECTED | ServerLogEvent.SERVERLOGEVENT_USER_DISCONNECTED)) != 0);
            chkKick.setChecked((e & (ServerLogEvent.SERVERLOGEVENT_USER_KICKED | ServerLogEvent.SERVERLOGEVENT_USER_BANNED)) != 0);
            chkChan.setChecked((e & (ServerLogEvent.SERVERLOGEVENT_CHANNEL_CREATED | ServerLogEvent.SERVERLOGEVENT_CHANNEL_UPDATED)) != 0);
            chkSrv.setChecked((e & ServerLogEvent.SERVERLOGEVENT_SERVER_UPDATED) != 0);
            chkFile.setChecked((e & ServerLogEvent.SERVERLOGEVENT_FILE_UPLOADED) != 0);
        }
        @Override
        public void updateProperties(ServerProperties props) {
            if (chkLogin == null) return;
            int e = 0;
            if (chkLogin.isChecked()) e |= (ServerLogEvent.SERVERLOGEVENT_USER_CONNECTED | ServerLogEvent.SERVERLOGEVENT_USER_DISCONNECTED | ServerLogEvent.SERVERLOGEVENT_USER_LOGGEDIN | ServerLogEvent.SERVERLOGEVENT_USER_LOGGEDOUT);
            if (chkKick.isChecked()) e |= (ServerLogEvent.SERVERLOGEVENT_USER_KICKED | ServerLogEvent.SERVERLOGEVENT_USER_BANNED);
            if (chkChan.isChecked()) e |= (ServerLogEvent.SERVERLOGEVENT_CHANNEL_CREATED | ServerLogEvent.SERVERLOGEVENT_CHANNEL_UPDATED | ServerLogEvent.SERVERLOGEVENT_CHANNEL_REMOVED);
            if (chkSrv.isChecked()) e |= ServerLogEvent.SERVERLOGEVENT_SERVER_UPDATED;
            if (chkFile.isChecked()) e |= (ServerLogEvent.SERVERLOGEVENT_FILE_UPLOADED | ServerLogEvent.SERVERLOGEVENT_FILE_DOWNLOADED | ServerLogEvent.SERVERLOGEVENT_FILE_DELETED);
            props.uServerLogEvents = e;
        }
    }
}