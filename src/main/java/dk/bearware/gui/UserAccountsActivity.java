package dk.bearware.gui;

import android.app.AlertDialog;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import android.widget.AdapterView;
import android.widget.Spinner;

import dk.bearware.TeamTalkBase;
import dk.bearware.UserAccount;
import dk.bearware.backend.TeamTalkConnection;
import dk.bearware.backend.TeamTalkConnectionListener;
import dk.bearware.backend.TeamTalkService;
import dk.bearware.events.ClientEventListener;

public class UserAccountsActivity extends AppCompatActivity implements 
        TeamTalkConnectionListener, 
        ClientEventListener.OnCmdUserAccountListener {

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

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
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
        getClient().doListUserAccounts(0, 100);
    }

    @Override
    public void onServiceDisconnected(TeamTalkService service) {}

    @Override
    public void onCmdUserAccount(UserAccount useraccount) {
        runOnUiThread(() -> {

            boolean found = false;
            for (int i = 0; i < allAccounts.size(); i++) {
                if (allAccounts.get(i).szUsername.equals(useraccount.szUsername)) {
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
                if (acc.szUsername.toLowerCase().contains(q) || acc.szNote.toLowerCase().contains(q)) {
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
        String[] options = {getString(R.string.action_edit), getString(R.string.action_delete)};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_options_for, account.szUsername))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {

                        Intent intent = new Intent(this, UserAccountEditActivity.class);
                        intent.putExtra(UserAccountEditActivity.EXTRA_IS_EDIT, true);
                        intent.putExtra(UserAccountEditActivity.EXTRA_USERNAME, account.szUsername);
                        intent.putExtra(UserAccountEditActivity.EXTRA_PASSWORD, account.szPassword);
                        intent.putExtra(UserAccountEditActivity.EXTRA_USERTYPE, account.uUserType);
                        intent.putExtra(UserAccountEditActivity.EXTRA_USERRIGHTS, account.uUserRights);
                        intent.putExtra(UserAccountEditActivity.EXTRA_NOTE, account.szNote);
                        startActivity(intent);
                    } else if (which == 1) {

                        confirmDelete(account);
                    }
                })
                .show();
    }

    private void confirmDelete(UserAccount account) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_confirm_delete)
                .setMessage(getString(R.string.msg_confirm_delete_account, account.szUsername))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    getClient().doDeleteUserAccount(account.szUsername);
                    allAccounts.remove(account);
                    filterAccounts(searchEdit.getText().toString());
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private class UserAccountAdapter extends ArrayAdapter<UserAccount> {
        public UserAccountAdapter(Context context, List<UserAccount> accounts) {
            super(context, android.R.layout.simple_list_item_2, accounts);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            UserAccount account = getItem(position);
            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            if (account != null) {
                String displayName = account.szUsername;
                if (displayName == null || displayName.trim().isEmpty()) {
                    displayName = getContext().getString(R.string.anonymous_account);
                }
                text1.setText(displayName);
                text2.setText(account.szNote.isEmpty() ? getContext().getString(R.string.no_note) : account.szNote);
            }
            return convertView;
        }
    }
}