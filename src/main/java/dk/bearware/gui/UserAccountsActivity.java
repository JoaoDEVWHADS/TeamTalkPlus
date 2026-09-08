package dk.bearware.gui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import android.widget.AdapterView;
import android.widget.Spinner;

import dk.bearware.TeamTalkBase;
import dk.bearware.ClientErrorMsg;
import dk.bearware.UserAccount;
import dk.bearware.backend.TeamTalkConnection;
import dk.bearware.backend.TeamTalkConnectionListener;
import dk.bearware.backend.TeamTalkService;
import dk.bearware.events.ClientEventListener;
import dk.bearware.UserType;
import dk.bearware.data.ServerEntry;

public class UserAccountsActivity extends AppCompatActivity implements 
        TeamTalkConnectionListener, 
        ClientEventListener.OnCmdUserAccountListener,
        ClientEventListener.OnCmdSuccessListener,
        ClientEventListener.OnCmdErrorListener {

    private static final String TAG = "UserAccountsActivity";
    public static final String EXTRA_USERID = "userid";
    private TeamTalkConnection mConnection;
    private ListView accountsListView;
    private UserAccountAdapter adapter;
    private List<UserAccount> allAccounts = new ArrayList<>();
    private List<UserAccount> filteredAccounts = new ArrayList<>();
    private EditText searchEdit;
    private Spinner sortSpinner;
    private boolean isAscending = true;
    private final java.util.Map<Integer, String> pendingDeleteAccounts = new java.util.HashMap<>();


    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(dk.bearware.gui.LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_accounts);
        EdgeToEdgeHelper.enableEdgeToEdge(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.user_accounts);
        }
        Utils.announceAccessibilityTitle(this, R.string.user_accounts);

        mConnection = new TeamTalkConnection(this);
        accountsListView = findViewById(R.id.user_accounts_listview);
        searchEdit = findViewById(R.id.search_accounts_edit);

        adapter = new UserAccountAdapter(this, filteredAccounts);
        accountsListView.setAdapter(adapter);

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAccounts(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        sortSpinner = findViewById(R.id.spinner_sort_accounts);
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, 
                new String[]{getString(R.string.sort_ascending), getString(R.string.sort_descending)});
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                isAscending = (position == 0);
                filterAccounts(searchEdit.getText().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        findViewById(R.id.btn_add_user_account).setOnClickListener(v -> {
            Intent intent = new Intent(this, UserAccountEditActivity.class);
            intent.putExtra(UserAccountEditActivity.EXTRA_IS_EDIT, false);
            startActivity(intent);
        });

        accountsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            UserAccount account = filteredAccounts.get(position);
            showAccountOptions(account);
            return true;
        });

        Intent intent = new Intent(this, TeamTalkService.class);
        if(!bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Failed to connect to TeamTalk service");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mConnection.isBound()) {
            if (getService() != null && getService().getTTInstance() != null) {
                allAccounts.clear();
                getClient().doListUserAccounts(0, 100000);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mConnection.isBound()) {
            if (getService() != null) {
                getService().getEventHandler().unregisterListener(this);
            }
            unbindService(mConnection);
        }
        super.onDestroy();
    }

    private TeamTalkService getService() {
        return mConnection.getService();
    }

    private TeamTalkBase getClient() {
        return getService().getTTInstance();
    }

    @Override
    public void onServiceConnected(TeamTalkService service) {
        service.getEventHandler().registerOnCmdUserAccount(this, true);
        service.getEventHandler().registerOnCmdSuccess(this, true);
        service.getEventHandler().registerOnCmdError(this, true);
        getClient().doListUserAccounts(0, 100000);
    }

    @Override
    public void onServiceDisconnected(TeamTalkService service) {}

    @Override
    public void onCmdUserAccount(UserAccount useraccount) {
        runOnUiThread(() -> {

            boolean found = false;
            for (int i = 0; i < allAccounts.size(); i++) {
                String existingUsername = allAccounts.get(i).szUsername;
                String incomingUsername = useraccount.szUsername;
                if (existingUsername == null ? incomingUsername == null : existingUsername.equals(incomingUsername)) {
                    allAccounts.set(i, useraccount);
                    found = true;
                    break;
                }
            }
            if (!found) {
                allAccounts.add(useraccount);
            }
            filterAccounts(searchEdit.getText().toString());
        });
    }

    private void filterAccounts(String query) {
        filteredAccounts.clear();
        if (query.isEmpty()) {
            filteredAccounts.addAll(allAccounts);
        } else {
            String q = query.toLowerCase();
            for (UserAccount acc : allAccounts) {
                String username = acc.szUsername == null ? "" : acc.szUsername;
                String note = acc.szNote == null ? "" : acc.szNote;
                String channel = acc.szInitChannel == null ? "" : acc.szInitChannel;
                if (username.toLowerCase().contains(q) || note.toLowerCase().contains(q) || channel.toLowerCase().contains(q)) {
                    filteredAccounts.add(acc);
                }
            }
        }
        
        Collections.sort(filteredAccounts, new Comparator<UserAccount>() {
            @Override
            public int compare(UserAccount u1, UserAccount u2) {
                String n1 = (u1.szUsername == null) ? "" : u1.szUsername;
                String n2 = (u2.szUsername == null) ? "" : u2.szUsername;
                return isAscending ? n1.compareToIgnoreCase(n2) : n2.compareToIgnoreCase(n1);
            }
        });
        
        adapter.notifyDataSetChanged();
    }

    private void showAccountOptions(UserAccount account) {
        String[] options = {
            getString(R.string.action_account_properties),
            getString(R.string.action_edit),
            getString(R.string.action_delete),
            getString(R.string.action_export_tt)
        };
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_options_for, account.szUsername))
                .setItems(options, (dialog, which) -> {
                    handleAccountOption(account, which);
                })
                .show();
    }

    public void handleAccountOption(UserAccount account, int which) {
        if (which == 0) {
            Intent intent = new Intent(this, UserAccountEditActivity.class);
            intent.putExtra(UserAccountEditActivity.EXTRA_IS_VIEW, true);
            intent.putExtra(UserAccountEditActivity.EXTRA_USERNAME, account.szUsername);
            intent.putExtra(UserAccountEditActivity.EXTRA_PASSWORD, account.szPassword);
            intent.putExtra(UserAccountEditActivity.EXTRA_USERTYPE, account.uUserType);
            intent.putExtra(UserAccountEditActivity.EXTRA_USERRIGHTS, account.uUserRights);
            intent.putExtra(UserAccountEditActivity.EXTRA_USER_DATA, account.nUserData);
            intent.putExtra(UserAccountEditActivity.EXTRA_NOTE, account.szNote);
            intent.putExtra(UserAccountEditActivity.EXTRA_INIT_CHANNEL, account.szInitChannel);
            intent.putExtra(UserAccountEditActivity.EXTRA_OPERATOR_CHANNELS, account.autoOperatorChannels);
            intent.putExtra(UserAccountEditActivity.EXTRA_AUDIO_CODEC_BPS_LIMIT, account.nAudioCodecBpsLimit);
            if (account.abusePrevent != null) {
                intent.putExtra(UserAccountEditActivity.EXTRA_ABUSE_COMMANDS_LIMIT, account.abusePrevent.nCommandsLimit);
                intent.putExtra(UserAccountEditActivity.EXTRA_ABUSE_INTERVAL_MSEC, account.abusePrevent.nCommandsIntervalMSec);
            }
            startActivity(intent);
        } else if (which == 1) {
            Intent intent = new Intent(this, UserAccountEditActivity.class);
            intent.putExtra(UserAccountEditActivity.EXTRA_IS_EDIT, true);
            intent.putExtra(UserAccountEditActivity.EXTRA_USERNAME, account.szUsername);
            intent.putExtra(UserAccountEditActivity.EXTRA_PASSWORD, account.szPassword);
            intent.putExtra(UserAccountEditActivity.EXTRA_USERTYPE, account.uUserType);
            intent.putExtra(UserAccountEditActivity.EXTRA_USERRIGHTS, account.uUserRights);
            intent.putExtra(UserAccountEditActivity.EXTRA_USER_DATA, account.nUserData);
            intent.putExtra(UserAccountEditActivity.EXTRA_NOTE, account.szNote);
            intent.putExtra(UserAccountEditActivity.EXTRA_INIT_CHANNEL, account.szInitChannel);
            intent.putExtra(UserAccountEditActivity.EXTRA_OPERATOR_CHANNELS, account.autoOperatorChannels);
            intent.putExtra(UserAccountEditActivity.EXTRA_AUDIO_CODEC_BPS_LIMIT, account.nAudioCodecBpsLimit);
            if (account.abusePrevent != null) {
                intent.putExtra(UserAccountEditActivity.EXTRA_ABUSE_COMMANDS_LIMIT, account.abusePrevent.nCommandsLimit);
                intent.putExtra(UserAccountEditActivity.EXTRA_ABUSE_INTERVAL_MSEC, account.abusePrevent.nCommandsIntervalMSec);
            }
            startActivity(intent);
        } else if (which == 2) {
            confirmDelete(account);
        } else if (which == 3) {
            exportAccount(account);
        }
    }

    private void exportAccount(UserAccount account) {
        ServerEntry server = getService().getServerEntry();
        if (server != null) {
            ServerEntry exportEntry = new ServerEntry();
            exportEntry.ipaddr = server.ipaddr;
            exportEntry.tcpport = server.tcpport;
            exportEntry.udpport = server.udpport;
            exportEntry.encrypted = server.encrypted;
            exportEntry.cacert = server.cacert;
            exportEntry.clientcert = server.clientcert;
            exportEntry.clientcertkey = server.clientcertkey;
            exportEntry.verifypeer = server.verifypeer;
            exportEntry.username = account.szUsername;
            exportEntry.password = account.szPassword;
            exportEntry.servername = server.servername;
            exportEntry.channel = "/";

            String xml = Utils.generateServerEntryXml(exportEntry);
            String filename = account.szUsername + ".tt";
            if (Utils.saveTTFileToDownloads(this, xml, filename)) {
                Toast.makeText(this, getString(R.string.msg_file_saved_to_downloads, filename), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.err_save_tt_file_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void confirmDelete(UserAccount account) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_confirm_delete)
                .setMessage(getString(R.string.msg_confirm_delete_account, account.szUsername))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    int cmdId = getClient().doDeleteUserAccount(account.szUsername);
                    if (cmdId > 0) {
                        pendingDeleteAccounts.put(cmdId, account.szUsername);
                        Toast.makeText(this, R.string.text_cmd_processing, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.err_user_account_request_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    @Override
    public void onCmdSuccess(int cmdId) {
        String username = pendingDeleteAccounts.remove(cmdId);
        if (username == null) return;
        for (int i = allAccounts.size() - 1; i >= 0; i--) {
            String existing = allAccounts.get(i).szUsername;
            if (username.equals(existing)) allAccounts.remove(i);
        }
        filterAccounts(searchEdit.getText().toString());
    }

    @Override
    public void onCmdError(int cmdId, ClientErrorMsg errmsg) {
        if (pendingDeleteAccounts.remove(cmdId) == null) return;
        Utils.notifyError(this, errmsg);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private class UserAccountAdapter extends ArrayAdapter<UserAccount> {
        public UserAccountAdapter(Context context, List<UserAccount> accounts) {
            super(context, R.layout.item_user_account, accounts);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_user_account, parent, false);
            }
            UserAccount account = getItem(position);
            TextView textUsername = convertView.findViewById(R.id.account_username);
            TextView textUserType = convertView.findViewById(R.id.account_usertype);
            TextView textPasswordChannel = convertView.findViewById(R.id.account_password_channel);
            TextView textDates = convertView.findViewById(R.id.account_dates);
            TextView textNote = convertView.findViewById(R.id.account_note);

            if (account != null) {
                String username = (account.szUsername != null && !account.szUsername.trim().isEmpty()) 
                        ? account.szUsername 
                        : getContext().getString(R.string.anonymous_account);
                
                String userTypeStr;
                if ((account.uUserType & UserType.USERTYPE_ADMIN) == UserType.USERTYPE_ADMIN) {
                    userTypeStr = getContext().getString(R.string.user_type_admin);
                } else if ((account.uUserType & UserType.USERTYPE_DEFAULT) == UserType.USERTYPE_DEFAULT) {
                    userTypeStr = getContext().getString(R.string.user_type_default);
                } else if (account.uUserType == UserType.USERTYPE_NONE) {
                    userTypeStr = getContext().getString(R.string.user_type_disabled);
                } else {
                    userTypeStr = getContext().getString(R.string.user_type_unknown);
                }

                String password = (account.szPassword != null) ? account.szPassword : "";
                String channel = (account.szInitChannel != null) ? account.szInitChannel : "";
                String modified = formatDateTime(account.szLastModified);
                String lastLogin = formatDateTime(account.szLastLoginTime);
                String note = (account.szNote != null) ? account.szNote : "";

                // Visual presentation (Qt 1:1 format)
                textUsername.setText(username);
                textUserType.setText(userTypeStr);

                String pwdText = getContext().getString(R.string.account_field_password, password);
                String chanText = getContext().getString(R.string.account_field_channel, channel);
                textPasswordChannel.setText(pwdText + " | " + chanText);

                String modText = getContext().getString(R.string.account_field_modified, modified);
                String logText = getContext().getString(R.string.account_field_lastlogin, lastLogin);
                textDates.setText(modText + " | " + logText);

                textNote.setVisibility(View.VISIBLE);
                textNote.setText(getContext().getString(R.string.account_field_note, note));

                // Accessible text (Exact 1:1 with qtTeamTalk Qt::AccessibleTextRole)
                String accessibleText = String.format("%s: %s, %s: %s, %s: %s, %s: %s, %s: %s, %s: %s, %s: %s",
                        getContext().getString(R.string.account_col_username), username,
                        getContext().getString(R.string.account_col_password), password,
                        getContext().getString(R.string.account_col_usertype), userTypeStr,
                        getContext().getString(R.string.account_col_note), note,
                        getContext().getString(R.string.account_col_channel), channel,
                        getContext().getString(R.string.account_col_modified), modified,
                        getContext().getString(R.string.account_col_lastlogin), lastLogin);

                convertView.setContentDescription(accessibleText);
            }
            return convertView;
        }
    }

    private static String formatDateTime(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        String s = raw.trim();
        String[] patterns = {
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "MMM dd yyyy HH:mm:ss"
        };
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat src = new java.text.SimpleDateFormat(pattern, java.util.Locale.US);
                java.util.Date date = src.parse(s);
                if (date != null) {
                    java.text.SimpleDateFormat dest = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault());
                    return dest.format(date);
                }
            } catch (Exception ignored) {}
        }
        return s;
    }
}