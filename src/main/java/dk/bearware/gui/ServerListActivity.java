/*
 * Copyright (c) 2005-2018, BearWare.dk
 * 
 * Contact Information:
 *
 * Bjoern D. Rasmussen
 * Kirketoften 5
 * DK-8260 Viby J
 * Denmark
 * Email: contact@bearware.dk
 * Phone: +45 20 20 54 59
 * Web: http://www.bearware.dk
 *
 * This source code is part of the TeamTalk SDK owned by
 * BearWare.dk. Use of this file, or its compiled unit, requires a
 * TeamTalk SDK License Key issued by BearWare.dk.
 *
 * The TeamTalk SDK License Agreement along with its Terms and
 * Conditions are outlined in the file License.txt included with the
 * TeamTalk SDK distribution.
 *
 */

package dk.bearware.gui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import android.media.AudioAttributes;
import android.os.Build;
import android.media.AudioManager;
import android.media.SoundPool;
import android.content.res.AssetFileDescriptor;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import dk.bearware.Constants;
import dk.bearware.TeamTalkBase;
import dk.bearware.UserAccount;
import dk.bearware.backend.TeamTalkConnection;
import dk.bearware.backend.TeamTalkConnectionListener;
import dk.bearware.backend.TeamTalkService;
import dk.bearware.data.AppInfo;
import dk.bearware.data.Permissions;
import dk.bearware.data.Preferences;
import dk.bearware.data.ServerEntry;
import dk.bearware.events.ClientEventListener;

public class ServerListActivity extends AppCompatActivity
        implements TeamTalkConnectionListener,
        Comparator<ServerEntry>,
        ClientEventListener.OnCmdMyselfLoggedInListener,
        AccessibilityAssistant.OnAccessibilityActionClickListener {

    private TeamTalkConnection mConnection;
    private ServerEntry serverentry;

    private ServerListAdapter adapter;
    private RecyclerView recyclerView;
    private ServerEntry mLastClickedServer;
    private EditText searchEditText;
    private TextView emptyView;
    private ExecutorService executorService;
    private Spinner sortSpinner;
    private AccessibilityAssistant accessibilityAssistant;
    private int sortMode = 0; // 0 = default (A-Z), 1 = name Z-A, 2 = popularity, 3 = location
    
    private final Vector<ServerEntry> servers = new Vector<>();
    private GitHubUpdateManager updateManager;

    private static final String TAG = "bearware";
    private static final String SERVERLIST_NAME = "serverlist";
    private static final int REQUEST_EDITSERVER = 1;
    private static final int REQUEST_NEWSERVER = 2;
    private static final int REQUEST_SETTINGS = 100;
    private static final int REQUEST_IMPORT_SERVERLIST = 3;
    private static final String POSITION_NAME = "pos";
    private int sound_server_lost;
    private boolean intentHandled = false;


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mConnection = new TeamTalkConnection(this);
        accessibilityAssistant = new AccessibilityAssistant(this);
        accessibilityAssistant.setOnAccessibilityActionClickListener(this);

        if (savedInstanceState != null) {
            intentHandled = savedInstanceState.getBoolean("intentHandled", false);
            mLastClickedServer = (ServerEntry) savedInstanceState.getSerializable("mLastClickedServer");
        }

        setContentView(R.layout.activity_server_list);
        EdgeToEdgeHelper.enableEdgeToEdge(this);

        initializeViews();
        setupRecyclerView();
        setupSearch();
        setupSortSpinner();
        setTitle(R.string.title_activity_server_list);
        executorService = Executors.newFixedThreadPool(2);

        Permissions.requestAll(this);
        AppInfo.ensureFoldersExist(this);

        updateManager = new GitHubUpdateManager(this);
        updateManager.checkForUpdates();
    }


    private void initializeViews() {
        recyclerView = findViewById(R.id.servers_recycler_view);
        searchEditText = findViewById(R.id.search_edit_text);
        emptyView = findViewById(R.id.empty_view);
        sortSpinner = findViewById(R.id.sort_spinner);
    }

    private void setupRecyclerView() {
        adapter = new ServerListAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        updateEmptyView();
    }

    private void setupSortSpinner() {
        if (sortSpinner == null) return;

        List<String> options = new ArrayList<>();
        options.add(getString(R.string.sort_default));
        options.add(getString(R.string.sort_za));
        options.add(getString(R.string.sort_popularity));
        options.add(getString(R.string.sort_server_location));

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(spinnerAdapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 0 = default (A-Z), 1 = Z-A, 2 = popularity, 3 = location
                sortMode = position;
                synchronized (servers) {
                    Collections.sort(servers, ServerListActivity.this);
                }
                adapter.updateServers();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateEmptyView() {
        if (adapter == null) return;
        boolean isEmpty = adapter.getItemCount() == 0;
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    TeamTalkService getService() {
        return mConnection.getService();
    }

    TeamTalkBase getClient() {
        return getService().getTTInstance();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("intentHandled", intentHandled);
        if (mLastClickedServer != null) {
            outState.putSerializable("mLastClickedServer", mLastClickedServer);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null && !intentHandled) {
            intentHandled = true;
            handleIntentUri(uri);
        }

        // Fix: always refresh server list when returning to this activity
        refreshServerList();

        if (mConnection.isBound()) {
            getService().getEventHandler().registerOnCmdMyselfLoggedIn(this, true);

            // Connect to server if 'serverentry' is specified.
            // Connection to server is either started here or in onServiceConnected()
            if (this.serverentry != null) {
                getService().setServerEntry(this.serverentry);

                if (!getService().reconnect()) {
                    showToast(getString(R.string.err_connection));
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        intentHandled = false;
        if (intent.getData() != null) {
            intentHandled = true;
            handleIntentUri(intent.getData());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mConnection.isBound())
            getService().getEventHandler().unregisterListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        AppInfo.ensureFoldersExist(this);

        // Removed unsafe saveServers() call that was clearing the list on startup via intent

        Permissions.requestAll(this);

        // Bind to LocalService if not already
        if (!mConnection.isBound()) {
            Intent intent = new Intent(getApplicationContext(), TeamTalkService.class);
            if (!bindService(intent, mConnection, Context.BIND_AUTO_CREATE))
                Log.e(TAG, "Failed to bind to TeamTalk service");
            else
                startService(intent);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (isFinishing() && mConnection.isBound()) {
            // Unbind from the service.
            getService().resetState();
            onServiceDisconnected(getService());
            stopService(new Intent(getApplicationContext(), TeamTalkService.class));
            unbindService(mConnection);
            mConnection.setBound(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (executorService != null) {
            executorService.shutdown();
        }

        // Unbind from the service
        if (mConnection.isBound()) {
            Log.d(TAG, "Unbinding TeamTalk service");
            onServiceDisconnected(getService());
            unbindService(mConnection);
            mConnection.setBound(false);
        }

        Log.d(TAG, "Activity destroyed " + this.hashCode());
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case REQUEST_NEWSERVER : {
                if(resultCode == RESULT_OK) {
                    ServerEntry entry = Utils.getServerEntry(data);
                    if(entry != null) {
                        servers.add(entry);
                        Collections.sort(servers, this);
                        adapter.updateServers();
                        saveServers();
                    }
                }
                break;
            }
            case REQUEST_EDITSERVER : {
                if(resultCode == RESULT_OK) {
                    ServerEntry entry = Utils.getServerEntry(data);
                    if(entry != null) {
                        int pos = data.getIntExtra(POSITION_NAME, -1);
                        if ((pos >= 0) && (pos < servers.size())) {
                            servers.removeElementAt(pos);
                            servers.insertElementAt(entry, pos);
                        }
                        else {
                            servers.add(entry);
                        }
                        Collections.sort(servers, this);
                        adapter.updateServers();
                        saveServers();
                    }
                }
                break;
            }
            case REQUEST_IMPORT_SERVERLIST : {
                if(resultCode == RESULT_OK && data != null && data.getData() != null) {
                    importServerFromFile(data.getData(), false);
                }
                break;
            }
            case GitHubUpdateManager.REQUEST_INSTALL_UNKNOWN_SOURCES: {
                if (updateManager != null) {
                    updateManager.resumeInstallation();
                }
                break;
            }
            case REQUEST_SETTINGS: {
                recreate();
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.server_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_newserverentry) {
            Intent edit = new Intent(this, ServerEntryActivity.class);
            startActivityForResult(edit, REQUEST_NEWSERVER);
        } else if (id == R.id.action_refreshserverlist) {
            refreshServerList();
        } else if (id == R.id.action_import_serverlist) {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) || Permissions.READ_EXTERNAL_STORAGE.request(this)) {
                fileSelectionStart();
            }
        } else if (id == R.id.action_export_serverlist) {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) || Permissions.WRITE_EXTERNAL_STORAGE.request(this)) {
                exportServers();
            }
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(ServerListActivity.this, PreferencesActivity.class);
            startActivityForResult(intent, REQUEST_SETTINGS);
        } else if (id == R.id.action_importlink) {
            showImportLinkDialog();
        } else if (id == R.id.action_exit) {
            finish();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void onServerClick(ServerEntry entry) {
        this.serverentry = entry;
        this.mLastClickedServer = entry;

        if (mConnection.isBound()) {
            getService().setServerEntry(this.serverentry);

            if (!getService().reconnect()) {
                showToast(getString(R.string.err_connection));
            }
        }
        // If not bound, onServiceConnected will handle it later
    }

    private void onServerLongClick(View view, ServerEntry entry, int position) {
        PopupMenu serverActions = new PopupMenu(this, view);
        serverActions.inflate(R.menu.server_actions);
        serverActions.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_publishsrv) {
                confirmAndPublishServer(entry);
                return true;
            } else if (id == R.id.action_exportsrv) {
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) || Permissions.WRITE_EXTERNAL_STORAGE.request(this)) {
                    exportServer(entry);
                }
                return true;
            } else if (id == R.id.action_editsrv) {
                Intent intent = new Intent(this, ServerEntryActivity.class);
                startActivityForResult(Utils.putServerEntry(intent, entry).putExtra(POSITION_NAME, position), REQUEST_EDITSERVER);
                return true;
            } else if (id == R.id.action_removesrv) {
                showRemoveServerDialog(entry);
                return true;
            } else {
                return false;
            }
        });
        serverActions.show();
    }

    private void confirmAndPublishServer(ServerEntry entry) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.title_publish_server);
        alert.setMessage(getString(R.string.msg_publish_server_confirmation, entry.servername));
        alert.setPositiveButton(android.R.string.yes, (dialog, whichButton) -> publishServer(entry));
        alert.setNegativeButton(android.R.string.no, null);
        alert.show();
    }

    private void publishServer(ServerEntry entry) {
        if (executorService == null) {
            showToast(getString(R.string.err_publish_server_failed));
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String username = prefs.getString(Preferences.PREF_GENERAL_BEARWARE_USERNAME, "");
        String token = prefs.getString(Preferences.PREF_GENERAL_BEARWARE_TOKEN, "");

        if (username.isEmpty() || token.isEmpty()) {
            showToast(getString(R.string.err_publish_server_failed));
            Intent intent = new Intent(this, WebLoginActivity.class);
            startActivity(intent);
            return;
        }

        executorService.execute(() -> {
            String serverXml = Utils.generateServerEntryXml(entry);
            String response = Utils.postURL(AppInfo.getPublishServerUrl(ServerListActivity.this, username, token), serverXml);
            final boolean finalSuccess = response != null && !response.isEmpty();

            runOnUiThread(() -> {
                if (finalSuccess) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.text_publish_server_success);
                    builder.setMessage(R.string.msg_publish_server_verification_detail);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setNeutralButton(R.string.action_copy_tag, (dialog, which) -> {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(getString(R.string.tag_clipboard), getString(R.string.tag_publish_server));
                        clipboard.setPrimaryClip(clip);
                        showToast(getString(R.string.text_copied_to_clipboard, getString(R.string.tag_publish_server)));
                    });
                    builder.show();
                } else {
                    showToast(getString(R.string.err_publish_server_failed));
                }
            });
        });
    }

    private void handleIntentUri(Uri uri) {
        String scheme = uri.getScheme();
        if ("tt".equals(scheme) || "http".equals(scheme) || "https".equals(scheme)) {
            loadServerFromUri(uri);
        } else if ("file".equals(scheme) || "content".equals(scheme)) {
            importServerFromFile(uri, false);
        }
    }

    private void importServerFromFile(Uri uri, boolean autoConnectIfSingle) {
        // Read file
        StringBuilder xml = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                BufferedReader source = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = source.readLine()) != null) {
                    xml.append(line);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read server file", e);
            showToast(getString(R.string.err_connection)); // Fallback or use a generic error
            return;
        }

        Vector<ServerEntry> entries = Utils.getXmlServerEntries(xml.toString());
        if (entries != null && !entries.isEmpty()) {
            if (entries.size() > 1) {
                // Multiple entries: Ask user which one to open (connect to)
                String[] names = new String[entries.size()];
                boolean[] checkedItems = new boolean[entries.size()];
                for (int i = 0; i < entries.size(); i++) {
                    names[i] = entries.get(i).servername;
                    checkedItems[i] = true;
                }

                final AlertDialog chooseDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.title_select_server)
                    .setMultiChoiceItems(names, checkedItems, (dialogInterface, index, isChecked) -> {
                        checkedItems[index] = isChecked;
                    })
                    .setNeutralButton(R.string.action_select_none, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.action_import, (dialog, which) -> {
                        Vector<ServerEntry> selectedEntries = new Vector<>();
                        for (int i = 0; i < entries.size(); i++) {
                            if (checkedItems[i]) selectedEntries.add(entries.get(i));
                        }
                        if (selectedEntries.isEmpty()) {
                            showToast(getString(R.string.msg_no_servers_selected));
                        } else if (selectedEntries.size() == 1) {
                            showImportConfirmationDialog(selectedEntries, selectedEntries.get(0));
                        } else {
                            importServerEntries(selectedEntries, true);
                        }
                    })
                    .create();

                chooseDialog.setOnShowListener(dialogInterface -> {
                    android.widget.Button neutralButton = chooseDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                    neutralButton.setOnClickListener(v -> {
                        boolean anyUnchecked = false;
                        for (boolean b : checkedItems) {
                            if (!b) { anyUnchecked = true; break; }
                        }
                        boolean newState = anyUnchecked; 
                        android.widget.ListView list = chooseDialog.getListView();
                        for (int i = 0; i < list.getCount(); i++) {
                            list.setItemChecked(i, newState);
                            checkedItems[i] = newState;
                        }
                        neutralButton.setText(newState ? R.string.action_select_none : R.string.action_select_all);
                    });
                });
                chooseDialog.show();
            } else {
                // Single entry: Assume this one
                if (autoConnectIfSingle) {
                    importServerEntries(entries, false);
                    onServerClick(entries.get(0));
                } else {
                    showImportConfirmationDialog(entries, entries.get(0));
                }
            }
        } else {
             showToast(getString(R.string.err_connection));
        }
    }

    private void showImportConfirmationDialog(Vector<ServerEntry> allEntries, ServerEntry targetEntry) {
         AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(R.string.title_import_server);

         if (targetEntry != null) {
             builder.setMessage(getString(R.string.msg_import_server_confirmation, targetEntry.servername));
             builder.setNeutralButton(R.string.action_import_connect, (dialog, which) -> {
                 importServerEntries(allEntries, false);
                 onServerClick(targetEntry); // Connect to the target one
             });
         } else {
             builder.setMessage(getString(R.string.msg_import_multiple_servers_confirmation, allEntries.size()));
         }

         builder.setPositiveButton(R.string.action_import, (dialog, which) -> {
             importServerEntries(allEntries, true);
         });
         
         builder.setNegativeButton(android.R.string.cancel, null);
         builder.show();
    }

    private void importServerEntries(Vector<ServerEntry> entries, boolean navigateToHome) {
         for (ServerEntry entry : entries) {
            entry.servertype = ServerEntry.ServerType.LOCAL;
         }
         synchronized (servers) {
            servers.addAll(entries);
            Collections.sort(servers, this);
         }
         adapter.updateServers();
         saveServers();
         showToast(getString(R.string.msg_server_imported));
         
         if (navigateToHome) {
             Intent intent = new Intent(this, ServerListActivity.class);
             intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
             startActivity(intent);
         }
    }

    private void showImportLinkDialog() {
        final EditText input = new EditText(this);
        input.setHint(R.string.msg_import_link_hint);
        
        // Try to pre-fill from clipboard
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence text = clip.getItemAt(0).getText();
                if (text != null && text.toString().startsWith("tt://")) {
                    input.setText(text);
                }
            }
        }

        final AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.title_import_link)
            .setView(input)
            .setPositiveButton(R.string.action_import, (d, which) -> {
                String link = input.getText().toString().trim();
                if (!link.isEmpty()) {
                    try {
                        Uri uri = Uri.parse(link);
                        if (link.startsWith("tt://")) {
                            loadServerFromUri(uri);
                        } else {
                            ServerEntry entry = parseServerUri(uri);
                            if (entry != null && entry.ipaddr != null && !entry.ipaddr.isEmpty()) {
                                Vector<ServerEntry> entries = new Vector<>();
                                entries.add(entry);
                                importServerEntries(entries, false);
                            } else {
                                showToast(getString(R.string.err_connection));
                            }
                        }
                    } catch (Exception e) {
                        showToast(getString(R.string.err_connection));
                    }
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create();

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                android.widget.Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (button != null) {
                    boolean isValid = text.startsWith("tt://");
                    button.setEnabled(isValid);
                    button.setText(R.string.action_connect);
                }
            }
        });

        dialog.setOnShowListener(d -> {
            String text = input.getText().toString().trim();
            android.widget.Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (button != null) {
                boolean isValid = text.startsWith("tt://");
                button.setEnabled(isValid);
                button.setText(R.string.action_connect);
            }
        });

        dialog.show();
    }

    private ServerEntry parseServerUri(Uri uri) {
        ServerEntry entry = new ServerEntry();
        String scheme = uri.getScheme();
        String host = uri.getHost();

        if ("http".equals(scheme) || "https".equals(scheme)) {
            host = uri.getQueryParameter("host");
            if (host == null) host = uri.getQueryParameter("address");
        }

        if (host != null && !host.isEmpty()) {
            entry.ipaddr = host;
            entry.servername = host;
        } else if (scheme != null && scheme.equals("tt") && uri.getEncodedAuthority() != null) {
            // Handle tt://host format where host is in authority
            entry.ipaddr = uri.getAuthority();
            entry.servername = entry.ipaddr;
        }

        entry.tcpport = getIntParameterOrDefault(uri, "tcpport", Constants.DEFAULT_TCP_PORT);
        entry.udpport = getIntParameterOrDefault(uri, "udpport", Constants.DEFAULT_UDP_PORT);
        entry.username = getStringParameterOrDefault(uri, "username", "");
        entry.password = getStringParameterOrDefault(uri, "password", "");
        entry.channel = getStringParameterOrDefault(uri, "channel", "");
        entry.chanpasswd = getStringParameterOrDefault(uri, "chanpasswd", "");

        String encrypted = uri.getQueryParameter("encrypted");
        entry.encrypted = encrypted != null && (encrypted.equalsIgnoreCase("true") || encrypted.equals("1"));
        
        return entry;
    }

    private void loadServerFromUri(Uri uri) {
        ServerEntry entry = parseServerUri(uri);

        if (entry.ipaddr != null && !entry.ipaddr.isEmpty()) {
            this.serverentry = entry;
            Log.i(TAG, "Connecting to " + entry.servername);

            if (mConnection.isBound()) {
                getService().setServerEntry(entry);
                if (!getService().reconnect()) {
                    showToast(getString(R.string.err_connection));
                }
            }
        }
    }

    private int getIntParameterOrDefault(Uri uri, String parameter, int defaultValue) {
        String value = uri.getQueryParameter(parameter);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    private String getStringParameterOrDefault(Uri uri, String parameter, String defaultValue) {
        String value = uri.getQueryParameter(parameter);
        return value != null ? value : defaultValue;
    }

    private class ServerListAdapter extends RecyclerView.Adapter<ServerListAdapter.ServerViewHolder> {
        private final List<ServerEntry> filteredServers = new ArrayList<>();
        private String currentFilter = "";

        public List<ServerEntry> getFilteredServers() {
            return filteredServers;
        }

        public ServerListAdapter() {
            setHasStableIds(true);
            updateFilteredList();
        }

        @Override
        public long getItemId(int position) {
            ServerEntry entry = filteredServers.get(position);
            // Combine hashCodes for a stable ID.
            return (entry.ipaddr.hashCode() * 31L + entry.tcpport) * 31L + entry.servername.hashCode();
        }

        @NonNull
        @Override
        public ServerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_serverentry, parent, false);
            return new ServerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ServerViewHolder holder, int position) {
            ServerEntry entry = filteredServers.get(position);
            holder.bind(entry, position);
        }

        @Override
        public int getItemCount() {
            return filteredServers.size();
        }

        public void filter(String query) {
            String newFilter = query.toLowerCase(Locale.ROOT).trim();
            if (!newFilter.equals(currentFilter)) {
                currentFilter = newFilter;
                updateFilteredList();
            }
        }

        private void updateFilteredList() {
            List<ServerEntry> newFilteredList = new ArrayList<>();
            synchronized (servers) {
                Collections.sort(servers, ServerListActivity.this);
                if (currentFilter.isEmpty()) {
                    newFilteredList.addAll(servers);
                } else {
                    for (ServerEntry server : servers) {
                        if (matchesFilter(server, currentFilter)) {
                            newFilteredList.add(server);
                        }
                    }
                }
            }
            
            filteredServers.clear();
            filteredServers.addAll(newFilteredList);
            notifyDataSetChanged();
            updateEmptyView();
        }

        private boolean matchesFilter(ServerEntry server, String filter) {
            return server.servername.toLowerCase(Locale.ROOT).contains(filter) ||
                   server.ipaddr.toLowerCase(Locale.ROOT).contains(filter);
        }

        public void updateServers() {
            updateFilteredList();
        }

        public void removeServer(ServerEntry entry) {
            int index = filteredServers.indexOf(entry);
            if (index != -1) {
                filteredServers.remove(index);
                notifyItemRemoved(index);
            }
        }

        private class ServerViewHolder extends RecyclerView.ViewHolder {
            private final ImageView serverIcon;
            private final TextView serverName;
            private final TextView serverSummary;

            public ServerViewHolder(@NonNull View itemView) {
                super(itemView);
                serverIcon = itemView.findViewById(R.id.servericon);
                serverName = itemView.findViewById(R.id.server_name);
                serverSummary = itemView.findViewById(R.id.server_summary);
            }

            public void bind(ServerEntry entry, int position) {
                serverName.setText(entry.servername);
                setServerIcon(entry);
                serverSummary.setText(getString(R.string.text_server_summary, 
                    entry.ipaddr, entry.tcpport, entry.stats_usercount, entry.stats_country));

                itemView.setOnClickListener(v -> onServerClick(entry));
                itemView.setOnLongClickListener(v -> {
                    onServerLongClick(v, entry, position);
                    return true;
                });

                itemView.setTag(entry);
                ViewCompat.setAccessibilityDelegate(itemView, accessibilityAssistant);
            }

            private void setServerIcon(ServerEntry entry) {
                switch (entry.servertype) {
                    case LOCAL:
                        serverIcon.setImageResource(R.drawable.teamtalk_yellow);
                        serverIcon.setContentDescription(getString(R.string.text_localserver));
                        serverIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                        break;
                    case OFFICIAL:
                        serverIcon.setImageResource(R.drawable.teamtalk_blue);
                        serverIcon.setContentDescription(getString(R.string.text_officialserver));
                        serverIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                        break;
                    case PUBLIC:
                        serverIcon.setImageResource(R.drawable.teamtalk_green);
                        serverIcon.setContentDescription(getString(R.string.text_publicserver));
                        serverIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                        break;
                    case UNOFFICIAL:
                        serverIcon.setImageResource(R.drawable.teamtalk_orange);
                        serverIcon.setContentDescription(getString(R.string.text_unofficialserver));
                        serverIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                        break;
                }
            }
        }
    }

    private void saveServers() {
        synchronized (servers) {
            SharedPreferences pref = getSharedPreferences(SERVERLIST_NAME, MODE_PRIVATE);
            SharedPreferences.Editor edit = pref.edit();

            clearExistingServerPreferences(pref, edit);
            saveLocalServersToPreferences(edit);
            edit.commit();
        }
    }

    private void clearExistingServerPreferences(SharedPreferences pref, SharedPreferences.Editor edit) {
        int i = 0;
        while (!pref.getString(i + ServerEntry.KEY_SERVERNAME, "").isEmpty()) {
            removeServerPreferencesAtIndex(edit, i);
            i++;
        }
    }

    private void removeServerPreferencesAtIndex(SharedPreferences.Editor edit, int index) {
        String[] keys = {
            ServerEntry.KEY_SERVERNAME, ServerEntry.KEY_IPADDR, ServerEntry.KEY_TCPPORT,
            ServerEntry.KEY_UDPPORT, ServerEntry.KEY_ENCRYPTED, ServerEntry.KEY_USERNAME,
            ServerEntry.KEY_PASSWORD, ServerEntry.KEY_NICKNAME, ServerEntry.KEY_STATUSMSG, ServerEntry.KEY_REMEMBER_LAST_CHANNEL,
            ServerEntry.KEY_CHANNEL, ServerEntry.KEY_CHANPASSWD
        };
        
        for (String key : keys) {
            edit.remove(index + key);
        }
    }

    private void saveLocalServersToPreferences(SharedPreferences.Editor edit) {
        int localServerIndex = 0;
        for (ServerEntry server : servers) {
            if (server.servertype == ServerEntry.ServerType.LOCAL) {
                saveServerToPreferences(edit, server, localServerIndex);
                localServerIndex++;
            }
        }
    }

    private void saveServerToPreferences(SharedPreferences.Editor edit, ServerEntry server, int index) {
        edit.putString(index + ServerEntry.KEY_SERVERNAME, server.servername);
        edit.putString(index + ServerEntry.KEY_IPADDR, server.ipaddr);
        edit.putInt(index + ServerEntry.KEY_TCPPORT, server.tcpport);
        edit.putInt(index + ServerEntry.KEY_UDPPORT, server.udpport);
        edit.putBoolean(index + ServerEntry.KEY_ENCRYPTED, server.encrypted);
        edit.putString(index + ServerEntry.KEY_USERNAME, server.username);
        edit.putString(index + ServerEntry.KEY_PASSWORD, server.password);
        edit.putString(index + ServerEntry.KEY_NICKNAME, server.nickname);
        edit.putString(index + ServerEntry.KEY_STATUSMSG, server.statusmsg);
        edit.putBoolean(index + ServerEntry.KEY_REMEMBER_LAST_CHANNEL, server.rememberLastChannel);
        edit.putString(index + ServerEntry.KEY_CHANNEL, server.channel);
        edit.putString(index + ServerEntry.KEY_CHANPASSWD, server.chanpasswd);
    }

    private void loadLocalServers() {
        SharedPreferences pref = getSharedPreferences(SERVERLIST_NAME, MODE_PRIVATE);
        int i = 0;
        while (!pref.getString(i + ServerEntry.KEY_SERVERNAME, "").isEmpty()) {
            ServerEntry entry = loadServerFromPreferences(pref, i);
            servers.add(entry);
            i++;
        }

        Collections.sort(servers, this);
        adapter.updateServers();
    }

    private ServerEntry loadServerFromPreferences(SharedPreferences pref, int index) {
        ServerEntry entry = new ServerEntry();
        entry.servername = pref.getString(index + ServerEntry.KEY_SERVERNAME, "");
        entry.ipaddr = pref.getString(index + ServerEntry.KEY_IPADDR, "");
        entry.tcpport = pref.getInt(index + ServerEntry.KEY_TCPPORT, 0);
        entry.udpport = pref.getInt(index + ServerEntry.KEY_UDPPORT, 0);
        entry.encrypted = pref.getBoolean(index + ServerEntry.KEY_ENCRYPTED, false);
        entry.username = pref.getString(index + ServerEntry.KEY_USERNAME, "");
        entry.password = pref.getString(index + ServerEntry.KEY_PASSWORD, "");
        entry.nickname = pref.getString(index + ServerEntry.KEY_NICKNAME, "");
        entry.statusmsg = pref.getString(index + ServerEntry.KEY_STATUSMSG, "");
        entry.rememberLastChannel = pref.getBoolean(index + ServerEntry.KEY_REMEMBER_LAST_CHANNEL, false);
        entry.channel = pref.getString(index + ServerEntry.KEY_CHANNEL, "");
        entry.chanpasswd = pref.getString(index + ServerEntry.KEY_CHANPASSWD, "");
        return entry;
    }

    private void loadServerListAsync() {
        if (executorService == null) return;
        
        executorService.execute(() -> {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

            String urlToRead = AppInfo.getServerListURL(ServerListActivity.this,
                    pref.getBoolean(Preferences.PREF_GENERAL_OFFICIALSERVERS, false),
                    pref.getBoolean(Preferences.PREF_GENERAL_UNOFFICIALSERVERS, false));

            String xml = Utils.getURL(urlToRead);
            Vector<ServerEntry> entries = null;
            if (!xml.isEmpty()) {
                entries = Utils.getXmlServerEntries(xml);
            }

            final Vector<ServerEntry> finalEntries = entries;
            runOnUiThread(() -> {
                if (finalEntries == null) {
                    showToast(getString(R.string.err_retrieve_public_server_list) + " (Network error)");
                } else if (finalEntries.size() > 0) {
                    synchronized (servers) {
                        // Remove existing non-local servers before adding updated ones
                        Vector<ServerEntry> localOnly = new Vector<>();
                        for (ServerEntry s : servers) {
                            if (s.servertype == ServerEntry.ServerType.LOCAL) {
                                localOnly.add(s);
                            }
                        }
                        servers.clear();
                        servers.addAll(localOnly);
                        servers.addAll(finalEntries);
                        Collections.sort(servers, ServerListActivity.this);
                    }
                    adapter.updateServers();
                    restoreServerListPosition();
                }
            });
        });
    }

    private void refreshServerList() {
        // Refactored to avoid clearing the whole list if we already have data
        // This prevents the UI from jumping to top or scrolling randomly.
        
        synchronized(servers) {
            Vector<ServerEntry> localServers = new Vector<>();
            SharedPreferences pref = getSharedPreferences(SERVERLIST_NAME, MODE_PRIVATE);
            int i = 0;
            // Load as long as either servername or ipaddr is present to handle empty names
            while (!pref.getString(i + ServerEntry.KEY_SERVERNAME, "").isEmpty() || 
                   !pref.getString(i + ServerEntry.KEY_IPADDR, "").isEmpty()) {
                localServers.add(loadServerFromPreferences(pref, i));
                i++;
            }
            
            // If we have existing servers, we try to update them rather than clear
            if (servers.isEmpty()) {
                servers.addAll(localServers);
                Collections.sort(servers, this);
            } else {
                // Keep only public servers, then re-add local
                Vector<ServerEntry> publicServers = new Vector<>();
                for (ServerEntry s : servers) {
                    if (s.servertype != ServerEntry.ServerType.LOCAL) {
                        publicServers.add(s);
                    }
                }
                servers.clear();
                servers.addAll(localServers);
                servers.addAll(publicServers);
                Collections.sort(servers, this);
            }
        }
        adapter.updateServers();
        restoreServerListPosition();

        // Get public servers from http.
        loadServerListAsync();
    }

    private void checkVersionAsync() {
        if (executorService == null) return;
        
        executorService.execute(() -> {
            String urlToRead = AppInfo.getUpdateURL(ServerListActivity.this);
            String xml = Utils.getURL(urlToRead);
            String latestClient = "";
            String versionMsg = "";

            if (!xml.isEmpty()) {
                try {
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(new InputSource(new StringReader(xml)));
                    doc.getDocumentElement().normalize();

                    NodeList nList = doc.getElementsByTagName("teamtalk");
                    for (int i = 0; i < nList.getLength(); i++) {
                        Node nNode = nList.item(i);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) nNode;
                            NodeList nName = eElement.getElementsByTagName("name");
                            if (nName.getLength() > 0) {
                                latestClient = nName.item(0).getTextContent();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing version XML", e);
                }
            }

            final String finalLatestClient = latestClient;
            final String finalVersionMsg = versionMsg;
            runOnUiThread(() -> {
                if (finalVersionMsg.length() > 0) {
                    showToast(getString(R.string.version_update, finalLatestClient));
                }
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permissions.onRequestResult(this, requestCode, permissions, grantResults);
        AppInfo.ensureFoldersExist(this);
    }

    private void restoreServerListPosition() {
        if (mLastClickedServer == null || adapter == null || recyclerView == null) return;

        final int index = findServerIndex(mLastClickedServer);
        if (index != -1) {
            recyclerView.post(() -> {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    layoutManager.scrollToPositionWithOffset(index, 0);
                    
                    // For Screen Reader focus
                    recyclerView.postDelayed(() -> {
                        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(index);
                        if (holder != null) {
                            holder.itemView.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED);
                            holder.itemView.requestFocus();
                        }
                    }, 100);
                }
            });
        }
    }

    private int findServerIndex(ServerEntry target) {
        if (target == null) return -1;
        List<ServerEntry> list = adapter.getFilteredServers();
        for (int i = 0; i < list.size(); i++) {
            ServerEntry entry = list.get(i);
            if (entry.ipaddr.equals(target.ipaddr) && 
                entry.tcpport == target.tcpport && 
                entry.servername.equals(target.servername)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onServiceConnected(TeamTalkService service) {

        service.getEventHandler().registerOnCmdMyselfLoggedIn(this, true);

        // Connect to server if 'serverentry' is specified.
        // Connection to server is either started here or in onResume()
        if (serverentry != null) {
            service.setServerEntry(serverentry);

            if (!service.reconnect()) {
                showToast(getString(R.string.err_connection));
            }
        }

        refreshServerList();

        String version = AppInfo.getVersion(this);

        TextView tv_version = findViewById(R.id.version_textview);
        TextView tv_dllversion = findViewById(R.id.dllversion_textview);
        tv_version.setText(getString(R.string.fmt_version_build, getString(R.string.ttversion), version, AppInfo.APPVERSION_POSTFIX, BuildConfig.VERSION_CODE));
        tv_dllversion.setText(getString(R.string.fmt_label_value, getString(R.string.ttdllversion), TeamTalkBase.getVersion()));

        checkVersionAsync();
    }

    @Override
    public void onServiceDisconnected(TeamTalkService service) {
        service.getEventHandler().unregisterListener(this);
    }

    @Override
    public void onCmdMyselfLoggedIn(int my_userid, UserAccount useraccount) {
        if (serverentry != null) {
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            startActivity(intent.putExtra(ServerEntry.KEY_SERVERNAME, serverentry.servername));

            serverentry = null;
        }
    }

    @Override
    public int compare(ServerEntry s1, ServerEntry s2) {
        // Sort by selected mode from spinner
        // 0 = default (type-based), 1 = Z-A, 2 = popularity
        if (sortMode == 1) { // Name Z-A
            return s2.servername.compareToIgnoreCase(s1.servername);
        } else if (sortMode == 2) { // Popularity (user count desc, then name)
            int c1 = s1.stats_usercount;
            int c2 = s2.stats_usercount;
            if (c1 != c2) {
                return c2 - c1;
            }
            return s1.servername.compareToIgnoreCase(s2.servername);
        } else if (sortMode == 3) { // Location (Country alphabetical)
            String c1 = s1.stats_country;
            String c2 = s2.stats_country;
            // Empty countries go to the end
            if (c1.isEmpty() && !c2.isEmpty()) return 1;
            if (!c1.isEmpty() && c2.isEmpty()) return -1;
            if (c1.isEmpty() && c2.isEmpty()) return s1.servername.compareToIgnoreCase(s2.servername);

            int countryCompare = c1.compareToIgnoreCase(c2);
            if (countryCompare != 0) {
                return countryCompare;
            }
            return s1.servername.compareToIgnoreCase(s2.servername);
        }

        switch (s1.servertype) {
            case LOCAL :
                switch (s2.servertype) {
                    case LOCAL :
                        return s1.servername.compareToIgnoreCase(s2.servername);
                    case OFFICIAL :
                    case PUBLIC :
                    case UNOFFICIAL :
                        return -1;
                }
                break;

            case OFFICIAL:
                switch (s2.servertype) {
                    case LOCAL :
                        return 1;
                    case OFFICIAL :
                        return 0; // order determined by xml-reply (from web-request)
                    case PUBLIC :
                    case UNOFFICIAL :
                        return -1;
                }
                break;

            case PUBLIC :
                switch (s2.servertype) {
                    case LOCAL :
                    case OFFICIAL :
                        return 1;
                    case PUBLIC :
                        return 0; // order determined by xml-reply (from web-request)
                    case UNOFFICIAL :
                        return -1;
                }
                break;

            case UNOFFICIAL:
                switch (s2.servertype) {
                    case LOCAL :
                    case OFFICIAL :
                    case PUBLIC :
                        return 1;
                    case UNOFFICIAL :
                        return s1.servername.compareToIgnoreCase(s2.servername);
                }
                break;
        }
        return 0;
    }

    @Override
    public void onAccessibilityActionClick(View view, int actionId) {
        Object tag = view.getTag();
        if (!(tag instanceof ServerEntry)) return;

        ServerEntry entry = (ServerEntry) tag;
        if (actionId == R.id.action_connect) {
            onServerClick(entry);
        } else if (actionId == R.id.action_publishsrv) {
            confirmAndPublishServer(entry);
        } else if (actionId == R.id.action_exportsrv) {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) || Permissions.WRITE_EXTERNAL_STORAGE.request(this)) {
                exportServer(entry);
            }
        } else if (actionId == R.id.action_editsrv) {
            int position = servers.indexOf(entry);
            Intent intent = new Intent(this, ServerEntryActivity.class);
            startActivityForResult(Utils.putServerEntry(intent, entry).putExtra(POSITION_NAME, position), REQUEST_EDITSERVER);
        } else if (actionId == R.id.action_removesrv) {
            showRemoveServerDialog(entry);
        }
    }

    @Override
    public List<AccessibilityNodeInfoCompat.AccessibilityActionCompat> getCustomActions(View view) {
        List<AccessibilityNodeInfoCompat.AccessibilityActionCompat> actions = new ArrayList<>();
        Object tag = view.getTag();
        if (!(tag instanceof ServerEntry)) {
            return actions;
        }

        actions.add(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                R.id.action_connect, getString(R.string.talkback_action_connect)));
        actions.add(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                R.id.action_publishsrv, getString(R.string.title_publish_server)));
        actions.add(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                R.id.action_exportsrv, getString(R.string.talkback_action_export)));
        actions.add(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                R.id.action_editsrv, getString(R.string.talkback_action_edit)));
        actions.add(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                R.id.action_removesrv, getString(R.string.talkback_action_delete)));

        return actions;
    }

    private void fileSelectionStart() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        Intent i = Intent.createChooser(intent, getString(R.string.chooser_file));
        startActivityForResult(i, REQUEST_IMPORT_SERVERLIST);
    }

    private void exportServers() {
        Vector<ServerEntry> localServers = getLocalServers();
        File ttFile = createExportFile("tt5servers.tt");
        if (ttFile != null) {
            exportToFile(localServers, ttFile, R.string.serverlist_export_confirmation);
        }
    }

    private void exportServer(ServerEntry entry) {
        Vector<ServerEntry> singleServer = new Vector<>(Collections.singletonList(entry));
        File ttFile = createExportFile(entry.servername + "_server.tt");
        if (ttFile != null) {
            exportToFile(singleServer, ttFile, R.string.server_export_confirmation);
        }
    }

    private Vector<ServerEntry> getLocalServers() {
        Vector<ServerEntry> localServers = new Vector<>();
        synchronized(servers) {
            for (ServerEntry entry : servers) {
                if (entry.servertype == ServerEntry.ServerType.LOCAL) {
                    localServers.add(entry);
                }
            }
        }
        return localServers;
    }

    private File createExportFile(String fileName) {
        File dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (dirPath.mkdirs() || dirPath.isDirectory()) {
            return new File(dirPath, fileName);
        }
        return null;
    }

    private void exportToFile(Vector<ServerEntry> entries, File ttFile, int successMsgId) {
        final String filePath = ttFile.getAbsolutePath();
        
        if (ttFile.exists()) {
            showFileOverrideDialog(entries, ttFile, filePath, successMsgId);
        } else {
            performExport(entries, filePath, successMsgId);
        }
    }

    private void showFileOverrideDialog(Vector<ServerEntry> entries, File ttFile, String filePath, int successMsgId) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(getString(R.string.alert_file_override, filePath));
        alert.setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
            if (ttFile.delete()) {
                performExport(entries, filePath, successMsgId);
            } else {
                showToast(getString(R.string.err_file_delete, filePath));
            }
        });
        alert.setNegativeButton(android.R.string.no, null);
        alert.show();
    }

    private void performExport(Vector<ServerEntry> entries, String filePath, int successMsgId) {
        boolean success = Utils.saveServers(entries, filePath);
        int msgId = success ? successMsgId : R.string.err_file_write;
        showToast(getString(msgId, filePath));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showRemoveServerDialog(ServerEntry entry) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(getString(R.string.server_remove_confirmation, entry.servername));
        alert.setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
            servers.remove(entry);
            adapter.removeServer(entry);
            saveServers();
            updateEmptyView();
        });
        alert.setNegativeButton(android.R.string.no, null);
        alert.show();
    }
}
