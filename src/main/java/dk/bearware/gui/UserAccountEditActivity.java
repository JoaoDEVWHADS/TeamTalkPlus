package dk.bearware.gui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import dk.bearware.AbusePrevention;
import dk.bearware.Channel;
import dk.bearware.TeamTalkBase;
import dk.bearware.UserAccount;
import dk.bearware.UserRight;
import dk.bearware.UserType;
import dk.bearware.IntPtr;
import dk.bearware.backend.TeamTalkConnection;
import dk.bearware.backend.TeamTalkConnectionListener;
import dk.bearware.backend.TeamTalkService;

public class UserAccountEditActivity extends AppCompatActivity implements TeamTalkConnectionListener {

    public static final String EXTRA_USERNAME = "username";
    public static final String EXTRA_PASSWORD = "password";
    public static final String EXTRA_USERTYPE = "usertype";
    public static final String EXTRA_USERRIGHTS = "userrights";
    public static final String EXTRA_NOTE = "note";
    public static final String EXTRA_IS_EDIT = "is_edit";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Button btnSave;
    private AccountPagerAdapter pagerAdapter;
    private UserAccount mAccount = new UserAccount();

    private TeamTalkConnection ttConnection;
    private boolean isEdit = false;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_account_edit);
        EdgeToEdgeHelper.enableEdgeToEdge(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.user_accounts);
        }

        tabLayout = findViewById(R.id.account_edit_tabs);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        viewPager = findViewById(R.id.account_edit_viewpager);
        btnSave = findViewById(R.id.btn_save_account);

        isEdit = getIntent().getBooleanExtra(EXTRA_IS_EDIT, false);
        if (isEdit) {
            mAccount.szUsername = getIntent().getStringExtra(EXTRA_USERNAME);
            mAccount.szPassword = getIntent().getStringExtra(EXTRA_PASSWORD);
            mAccount.uUserType = getIntent().getIntExtra(EXTRA_USERTYPE, UserType.USERTYPE_DEFAULT);
            mAccount.uUserRights = getIntent().getIntExtra(EXTRA_USERRIGHTS, UserRight.USERRIGHT_NONE);
            mAccount.szNote = getIntent().getStringExtra(EXTRA_NOTE);
        }

        pagerAdapter = new AccountPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(5); 

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText(R.string.tab_basic_info); break;
                case 1: tab.setText(R.string.tab_user_rights); break;
                case 2: tab.setText(R.string.tab_channel_operator); break;
                case 3: tab.setText(R.string.tab_codec_limitations); break;
                case 4: tab.setText(R.string.tab_abuse_prevention); break;
            }
        }).attach();

        ttConnection = new TeamTalkConnection(this);
        btnSave.setOnClickListener(v -> saveAccount());
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
            unbindService(ttConnection);
        }
    }

    @Override
    public void onServiceConnected(TeamTalkService service) {}

    @Override
    public void onServiceDisconnected(TeamTalkService service) {}

    private void saveAccount() {
        TeamTalkService service = ttConnection.getService();
        if (service == null || service.getTTInstance() == null) {
            Toast.makeText(this, R.string.err_service_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < pagerAdapter.getItemCount(); i++) {
            Fragment f = getSupportFragmentManager().findFragmentByTag("f" + i);
            if (f instanceof AccountEditFragment) {
                ((AccountEditFragment) f).updateAccount(mAccount);
            }
        }

        

        int cmdId = service.getTTInstance().doNewUserAccount(mAccount);
        if (cmdId > 0) {
            Toast.makeText(this, R.string.msg_user_account_request_sent, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.err_user_account_request_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public interface AccountEditFragment {
        void updateAccount(UserAccount acc);
    }

    private class AccountPagerAdapter extends FragmentStateAdapter {
        public AccountPagerAdapter(AppCompatActivity activity) {
            super(activity);
        }

        @NonNull @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new BasicInfoFragment();
                case 1: return new UserRightsFragment();
                case 2: return new ChannelOperatorFragment();
                case 3: return new CodecLimitFragment();
                case 4: return new AbusePreventFragment();
                default: return new BasicInfoFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }

    public static class BasicInfoFragment extends Fragment implements AccountEditFragment {
        EditText editUser, editPass, editNote, editInitChan;
        RadioGroup typeGroup;
        TextView noteDisabled;

        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
            android.view.View v = inflater.inflate(R.layout.fragment_user_account_basic, container, false);
            editUser = v.findViewById(R.id.edit_account_username);
            editPass = v.findViewById(R.id.edit_account_password);
            editNote = v.findViewById(R.id.edit_account_note);
            editInitChan = v.findViewById(R.id.edit_account_init_chan);
            typeGroup = v.findViewById(R.id.edit_account_type_group);
            noteDisabled = v.findViewById(R.id.note_disabled_user);

            UserAccountEditActivity activity = (UserAccountEditActivity) getActivity();
            if (activity != null) {
                editUser.setText(activity.mAccount.szUsername);
                if (activity.isEdit) editUser.setEnabled(false);
                editPass.setText(activity.mAccount.szPassword);
                editNote.setText(activity.mAccount.szNote);
                editInitChan.setText(activity.mAccount.szInitChannel);
                if (activity.mAccount.uUserType == UserType.USERTYPE_ADMIN) ((RadioButton)v.findViewById(R.id.radio_type_admin)).setChecked(true);
                else if (activity.mAccount.uUserType == UserType.USERTYPE_NONE) {
                    ((RadioButton)v.findViewById(R.id.radio_type_none)).setChecked(true);
                    noteDisabled.setVisibility(View.VISIBLE);
                }
                else ((RadioButton)v.findViewById(R.id.radio_type_default)).setChecked(true);

                editInitChan.setOnClickListener(view -> showChannelSelectionDialog(activity));
                typeGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    if (checkedId == R.id.radio_type_none) noteDisabled.setVisibility(View.VISIBLE);
                    else noteDisabled.setVisibility(View.GONE);
                });
            }
            return v;
        }

        private void showChannelSelectionDialog(UserAccountEditActivity activity) {
            if (activity.ttConnection.getService() == null) return;
            Vector<Channel> channels = new Vector<>();
            IntPtr n = new IntPtr();
            if (activity.ttConnection.getService().getTTInstance().getServerChannels(null, n)) {
                Channel[] channelArray = new Channel[n.value];
                if (activity.ttConnection.getService().getTTInstance().getServerChannels(channelArray, n)) {
                    for(int i=0;i<n.value;i++)
                        channels.add(channelArray[i]);
                }
            }

            String[] channelNames = new String[channels.size() + 1];
            String[] channelPaths = new String[channels.size() + 1];
            channelNames[0] = "Root";
            channelPaths[0] = "/";

            for(int i=0; i<channels.size(); i++) {
                Channel c = channels.get(i);
                channelNames[i+1] = activity.ttConnection.getService().getTTInstance().getChannelPath(c.nChannelID);
                channelPaths[i+1] = channelNames[i+1];
            }

            new AlertDialog.Builder(activity)
                .setTitle(R.string.init_channel_select)
                .setItems(channelNames, (dialog, which) -> {
                    editInitChan.setText(channelPaths[which]);
                })
                .show();
        }

        @Override
        public void updateAccount(UserAccount acc) {
            if (editUser == null) return;
            acc.szUsername = editUser.getText().toString().trim();
            acc.szPassword = editPass.getText().toString();
            acc.szNote = editNote.getText().toString();
            acc.szInitChannel = editInitChan.getText().toString();
            int id = typeGroup.getCheckedRadioButtonId();
            if (id == R.id.radio_type_admin) acc.uUserType = UserType.USERTYPE_ADMIN;
            else if (id == R.id.radio_type_none) acc.uUserType = UserType.USERTYPE_NONE;
            else acc.uUserType = UserType.USERTYPE_DEFAULT;
        }
    }

    public static class UserRightsFragment extends Fragment implements AccountEditFragment {
        CheckBox chkAll, chkML, chkCN, chkVA, chkVC, chkCC, chkMC, chkESP, chkK, chkB, chkMU, chkOE, chkU, chkD, chkRec, chkV, chkVid, chkDsk, chkDskAcc, chkMA, chkMV, chkTU, chkTC, chkTB;

        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
            android.view.View v = inflater.inflate(R.layout.fragment_user_account_rights, container, false);
            chkAll = v.findViewById(R.id.chk_right_all);

            chkML = v.findViewById(R.id.chk_right_multi_login);
            chkCN = v.findViewById(R.id.chk_right_change_nickname);
            chkVA = v.findViewById(R.id.chk_right_view_all);
            chkVC = v.findViewById(R.id.chk_right_view_hidden);
            chkCC = v.findViewById(R.id.chk_right_create_chan);
            chkMC = v.findViewById(R.id.chk_right_modify_chan);
            chkESP = v.findViewById(R.id.chk_right_update_server_props);
            chkK = v.findViewById(R.id.chk_right_kick);
            chkB = v.findViewById(R.id.chk_right_ban);
            chkMU = v.findViewById(R.id.chk_right_move_users);
            chkOE = v.findViewById(R.id.chk_right_operator_enable);
            chkU = v.findViewById(R.id.chk_right_upload_files);
            chkD = v.findViewById(R.id.chk_right_download_files);
            chkRec = v.findViewById(R.id.chk_right_record_voice);
            chkV = v.findViewById(R.id.chk_right_voice);
            chkVid = v.findViewById(R.id.chk_right_video);
            chkDsk = v.findViewById(R.id.chk_right_desktop);
            chkDskAcc = v.findViewById(R.id.chk_right_desktop_access);
            chkMA = v.findViewById(R.id.chk_right_media_audio);
            chkMV = v.findViewById(R.id.chk_right_media_video);
            chkTU = v.findViewById(R.id.chk_right_text_user);
            chkTC = v.findViewById(R.id.chk_right_text_chan);
            chkTB = v.findViewById(R.id.chk_right_text_broadcast);

            UserAccountEditActivity activity = (UserAccountEditActivity) getActivity();
            if (activity != null) {
                int r = activity.mAccount.uUserRights;
                chkML.setChecked((r & UserRight.USERRIGHT_MULTI_LOGIN) != 0);
                chkCN.setChecked((r & UserRight.USERRIGHT_LOCKED_NICKNAME) == 0); 
                chkVA.setChecked((r & UserRight.USERRIGHT_VIEW_ALL_USERS) != 0);

                chkCC.setChecked((r & UserRight.USERRIGHT_CREATE_TEMPORARY_CHANNEL) != 0);
                chkMC.setChecked((r & UserRight.USERRIGHT_MODIFY_CHANNELS) != 0);

                chkK.setChecked((r & UserRight.USERRIGHT_KICK_USERS) != 0);
                chkB.setChecked((r & UserRight.USERRIGHT_BAN_USERS) != 0);

                chkU.setChecked((r & UserRight.USERRIGHT_UPLOAD_FILES) != 0);
                chkD.setChecked((r & UserRight.USERRIGHT_DOWNLOAD_FILES) != 0);
                chkRec.setChecked((r & UserRight.USERRIGHT_RECORD_VOICE) != 0);
                chkV.setChecked((r & UserRight.USERRIGHT_TRANSMIT_VOICE) != 0);
                chkVid.setChecked((r & UserRight.USERRIGHT_TRANSMIT_VIDEOCAPTURE) != 0);
                chkDsk.setChecked((r & UserRight.USERRIGHT_TRANSMIT_DESKTOP) != 0);
                chkDskAcc.setChecked((r & UserRight.USERRIGHT_TRANSMIT_DESKTOPINPUT) != 0);
                chkMA.setChecked((r & UserRight.USERRIGHT_TRANSMIT_MEDIAFILE_AUDIO) != 0);
                chkMV.setChecked((r & UserRight.USERRIGHT_TRANSMIT_MEDIAFILE_VIDEO) != 0);
                chkTU.setChecked((r & UserRight.USERRIGHT_TEXTMESSAGE_USER) != 0);
                chkTC.setChecked((r & UserRight.USERRIGHT_TEXTMESSAGE_CHANNEL) != 0);
                chkTB.setChecked((r & UserRight.USERRIGHT_TEXTMESSAGE_BROADCAST) != 0);

                if ((r & UserRight.USERRIGHT_ALL) == UserRight.USERRIGHT_ALL) chkAll.setChecked(true);
            }

            chkAll.setOnCheckedChangeListener((cb, checked) -> {
                chkML.setChecked(checked); chkCN.setChecked(checked); chkVA.setChecked(checked); 
                chkVC.setChecked(checked); chkCC.setChecked(checked); chkMC.setChecked(checked); 
                chkESP.setChecked(checked); chkK.setChecked(checked); chkB.setChecked(checked); 
                chkMU.setChecked(checked); chkOE.setChecked(checked); chkU.setChecked(checked); 
                chkD.setChecked(checked); chkRec.setChecked(checked); chkV.setChecked(checked); 
                chkVid.setChecked(checked); chkDsk.setChecked(checked); chkDskAcc.setChecked(checked); 
                chkMA.setChecked(checked); chkMV.setChecked(checked); chkTU.setChecked(checked); 
                chkTC.setChecked(checked); chkTB.setChecked(checked);
            });

            return v;
        }

        @Override
        public void updateAccount(UserAccount acc) {
            if (chkML == null) return;
            int r = 0;
            if (chkML.isChecked()) r |= UserRight.USERRIGHT_MULTI_LOGIN;
            if (!chkCN.isChecked()) r |= UserRight.USERRIGHT_LOCKED_NICKNAME; 
            if (chkVA.isChecked()) r |= UserRight.USERRIGHT_VIEW_ALL_USERS;

            if (chkCC.isChecked()) r |= UserRight.USERRIGHT_CREATE_TEMPORARY_CHANNEL;
            if (chkMC.isChecked()) r |= UserRight.USERRIGHT_MODIFY_CHANNELS;

            if (chkK.isChecked()) r |= UserRight.USERRIGHT_KICK_USERS;
            if (chkB.isChecked()) r |= UserRight.USERRIGHT_BAN_USERS;

            if (chkU.isChecked()) r |= UserRight.USERRIGHT_UPLOAD_FILES;
            if (chkD.isChecked()) r |= UserRight.USERRIGHT_DOWNLOAD_FILES;
            if (chkRec.isChecked()) r |= UserRight.USERRIGHT_RECORD_VOICE;
            if (chkV.isChecked()) r |= UserRight.USERRIGHT_TRANSMIT_VOICE;
            if (chkVid.isChecked()) r |= UserRight.USERRIGHT_TRANSMIT_VIDEOCAPTURE;
            if (chkDsk.isChecked()) r |= UserRight.USERRIGHT_TRANSMIT_DESKTOP;
            if (chkDskAcc.isChecked()) r |= UserRight.USERRIGHT_TRANSMIT_DESKTOPINPUT;
            if (chkMA.isChecked()) r |= UserRight.USERRIGHT_TRANSMIT_MEDIAFILE_AUDIO;
            if (chkMV.isChecked()) r |= UserRight.USERRIGHT_TRANSMIT_MEDIAFILE_VIDEO;
            if (chkTU.isChecked()) r |= UserRight.USERRIGHT_TEXTMESSAGE_USER;
            if (chkTC.isChecked()) r |= UserRight.USERRIGHT_TEXTMESSAGE_CHANNEL;
            if (chkTB.isChecked()) r |= UserRight.USERRIGHT_TEXTMESSAGE_BROADCAST;
            acc.uUserRights = r;
        }
    }

    public static class ChannelOperatorFragment extends Fragment implements AccountEditFragment {
        ListView listOps;
        Button btnAdd;
        List<Integer> channelIds = new ArrayList<>();
        ArrayAdapter<String> adapter;

        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
            android.view.View v = inflater.inflate(R.layout.fragment_user_account_operator, container, false);
            listOps = v.findViewById(R.id.list_op_channels);
            btnAdd = v.findViewById(R.id.btn_add_op_channel);

            UserAccountEditActivity activity = (UserAccountEditActivity) getActivity();
            if (activity != null) {
                for (int id : activity.mAccount.autoOperatorChannels) {
                    if (id > 0) channelIds.add(id);
                }
                updateList(activity);

                btnAdd.setOnClickListener(view -> showChannelAddDialog(activity));
                listOps.setOnItemLongClickListener((adapterView, view, i, l) -> {
                    channelIds.remove(i);
                    updateList(activity);
                    return true;
                });
            }
            return v;
        }

        private void updateList(UserAccountEditActivity activity) {
            if (activity.ttConnection.getService() == null) return;
            List<String> names = new ArrayList<>();
            for (int id : channelIds) {
                String path = activity.ttConnection.getService().getTTInstance().getChannelPath(id);
                names.add(path != null && !path.isEmpty() ? path : activity.getString(R.string.label_channel_id, id));
            }
            adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, names);
            listOps.setAdapter(adapter);
        }

        private void showChannelAddDialog(UserAccountEditActivity activity) {
             if (activity.ttConnection.getService() == null) return;
            Vector<Channel> channels = new Vector<>();
            IntPtr n = new IntPtr();
            if (activity.ttConnection.getService().getTTInstance().getServerChannels(null, n)) {
                Channel[] channelArray = new Channel[n.value];
                if (activity.ttConnection.getService().getTTInstance().getServerChannels(channelArray, n)) {
                    for(int i=0;i<n.value;i++)
                        channels.add(channelArray[i]);
                }
            }

            List<Channel> available = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (Channel c : channels) {
                if (!channelIds.contains(c.nChannelID)) {
                    available.add(c);
                    names.add(activity.ttConnection.getService().getTTInstance().getChannelPath(c.nChannelID));
                }
            }

            if (available.isEmpty()) {
                Toast.makeText(activity, R.string.err_no_channels_available, Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(activity)
                .setTitle(R.string.title_add_operator_channel)
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    channelIds.add(available.get(which).nChannelID);
                    updateList(activity);
                })
                .show();
        }

        @Override
        public void updateAccount(UserAccount acc) {
            acc.autoOperatorChannels = new int[dk.bearware.Constants.TT_CHANNELS_OPERATOR_MAX];
            for (int i = 0; i < channelIds.size() && i < acc.autoOperatorChannels.length; i++) {
                acc.autoOperatorChannels[i] = channelIds.get(i);
            }
        }
    }

    public static class CodecLimitFragment extends Fragment implements AccountEditFragment {
        EditText editBps;
        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
            android.view.View v = inflater.inflate(R.layout.fragment_user_account_codec, container, false);
            editBps = v.findViewById(R.id.edit_account_bps_limit);
            UserAccountEditActivity activity = (UserAccountEditActivity) getActivity();
            if (activity != null) {
                editBps.setText(String.valueOf(activity.mAccount.nAudioCodecBpsLimit));
            }
            return v;
        }
        @Override
        public void updateAccount(UserAccount acc) {
            if (editBps == null) return;
            try {
                acc.nAudioCodecBpsLimit = Integer.parseInt(editBps.getText().toString());
            } catch (NumberFormatException e) {
                acc.nAudioCodecBpsLimit = 0;
            }
        }
    }

    public static class AbusePreventFragment extends Fragment implements AccountEditFragment {
        EditText editLimit, editInterval;
        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
            android.view.View v = inflater.inflate(R.layout.fragment_user_account_abuse, container, false);
            editLimit = v.findViewById(R.id.edit_account_cmd_limit);
            editInterval = v.findViewById(R.id.edit_account_cmd_interval);
            UserAccountEditActivity activity = (UserAccountEditActivity) getActivity();
            if (activity != null) {
                editLimit.setText(String.valueOf(activity.mAccount.abusePrevent.nCommandsLimit));
                editInterval.setText(String.valueOf(activity.mAccount.abusePrevent.nCommandsIntervalMSec));
            }
            return v;
        }
        @Override
        public void updateAccount(UserAccount acc) {
            if (editLimit == null) return;
            try {
                acc.abusePrevent.nCommandsLimit = Integer.parseInt(editLimit.getText().toString());
                acc.abusePrevent.nCommandsIntervalMSec = Integer.parseInt(editInterval.getText().toString());
            } catch (NumberFormatException e) {
                acc.abusePrevent = new AbusePrevention();
            }
        }
    }
}