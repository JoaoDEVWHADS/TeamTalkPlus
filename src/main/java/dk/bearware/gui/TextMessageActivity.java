
package dk.bearware.gui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import dk.bearware.TeamTalkBase;
import dk.bearware.TextMessage;
import dk.bearware.TextMsgType;
import dk.bearware.User;
import dk.bearware.backend.TeamTalkConnection;
import dk.bearware.backend.TeamTalkConnectionListener;
import dk.bearware.backend.TeamTalkService;
import dk.bearware.data.MyTextMessage;
import dk.bearware.data.TextMessageAdapter;
import dk.bearware.events.ClientEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;

public class TextMessageActivity
extends AppCompatActivity implements TeamTalkConnectionListener, ClientEventListener.OnCmdUserTextMessageListener,
        AccessibilityAssistant.OnAccessibilityActionClickListener {

    public static final String TAG = "bearware";

    public static final String EXTRA_USERID = "userid";

    TeamTalkConnection mConnection;
    TextMessageAdapter adapter;
    AccessibilityAssistant accessibilityAssistant;

    private Timer remoteTypingTimer;
    private long lastTypingSent;
    private static final long TYPING_TIMEOUT = 1500;
    private static final long TYPING_SEND_INTERVAL = 500;

    TeamTalkService getService() {
        return mConnection.getService();
    }

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(dk.bearware.gui.LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConnection = new TeamTalkConnection(this);
        setContentView(R.layout.activity_text_message);
        EdgeToEdgeHelper.enableEdgeToEdge(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        accessibilityAssistant = new AccessibilityAssistant(this);
        accessibilityAssistant.setOnAccessibilityActionClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.text_message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if(id == R.id.action_settings) {
            return true;
        }
        else if (id == android.R.id.home) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            View v = getCurrentFocus();
            if ((v != null) && imm.isActive())
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();        

        if (!mConnection.isBound()) {
            Intent intent = new Intent(getApplicationContext(), TeamTalkService.class);
            if(!bindService(intent, mConnection, Context.BIND_AUTO_CREATE))
                Log.e(TAG, "Failed to connect to TeamTalk service");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(mConnection.isBound()) {
            onServiceDisconnected(getService());
            unbindService(mConnection);
            mConnection.setBound(false);
        }
        if (remoteTypingTimer != null) {
            remoteTypingTimer.cancel();
            remoteTypingTimer = null;
        }
    }

    @Override
    public void onServiceConnected(TeamTalkService service) {
        final int userid = this.getIntent().getExtras().getInt(EXTRA_USERID);
        final TeamTalkBase ttclient = service.getTTInstance();
        adapter = new TextMessageAdapter(this.getBaseContext(), accessibilityAssistant,
                                         service.getUserTextMsgs(userid),
                                         ttclient.getMyUserID());

        ListView lv = findViewById(R.id.user_im_listview);
        lv.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        lv.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        Button send_btn = this.findViewById(R.id.user_im_sendbtn);
        final EditText send_msg = this.findViewById(R.id.user_im_edittext);
        send_btn.setOnClickListener(v -> {
            String newmsg = send_msg.getText().toString();
            if(newmsg.isEmpty())
                return;

            User myself = service.getUsers().get(ttclient.getMyUserID());
            String name = Utils.getDisplayName(getBaseContext(), myself);
            MyTextMessage textmsg = new MyTextMessage(myself == null? "" : name);
            textmsg.nMsgType = TextMsgType.MSGTYPE_USER;
            textmsg.nChannelID = 0;
            textmsg.nFromUserID = ttclient.getMyUserID();
            textmsg.nToUserID = userid;
            textmsg.szMessage = newmsg;

            boolean sent = true;
            for (MyTextMessage m : textmsg.split()) {
                sent = sent && ttclient.doTextMessage(m) > 0;
                service.getUserTextMsgs(userid).add(m);
            }
            if (sent) {
                MainActivity.playPrivateMessageSentSound(textmsg.szMessage);
                send_msg.setText("");
                adapter.notifyDataSetChanged();
                runOnUiThread(() -> {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle(null);
                    }
                });
            }
            else {
                Toast.makeText(TextMessageActivity.this,
                               R.string.err_send_text_message,
                               Toast.LENGTH_LONG).show();
            }
        });

        send_msg.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0 && System.currentTimeMillis() - lastTypingSent > TYPING_SEND_INTERVAL) {
                    MyTextMessage typingMsg = new MyTextMessage("");
                    typingMsg.nMsgType = TextMsgType.MSGTYPE_CUSTOM;
                    typingMsg.nChannelID = 0;
                    typingMsg.nFromUserID = ttclient.getMyUserID();
                    typingMsg.nToUserID = userid;
                    typingMsg.szMessage = "typing\r\n1";
                    if (ttclient.doTextMessage(typingMsg) > 0) {
                        lastTypingSent = System.currentTimeMillis();
                    }
                } else if (s.length() == 0 && lastTypingSent != 0) {
                    MyTextMessage typingMsg = new MyTextMessage("");
                    typingMsg.nMsgType = TextMsgType.MSGTYPE_CUSTOM;
                    typingMsg.nChannelID = 0;
                    typingMsg.nFromUserID = ttclient.getMyUserID();
                    typingMsg.nToUserID = userid;
                    typingMsg.szMessage = "typing\r\n0";
                    ttclient.doTextMessage(typingMsg);
                    lastTypingSent = 0;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        send_msg.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND || 
                (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                send_btn.performClick();
                return true;
            }
            return false;
        });

        service.getEventHandler().registerOnCmdUserTextMessage(this, true);

        updateTitle();
    }

    @Override
    public void onServiceDisconnected(TeamTalkService service) {
        service.getEventHandler().registerOnCmdUserTextMessage(this, false);
    }

    void updateTitle() {
        String title = getResources().getString(R.string.title_activity_text_message);
        int userid = this.getIntent().getExtras().getInt(EXTRA_USERID);

        User user = getService().getUsers().get(userid);
        if(user != null) {
            String name = Utils.getDisplayName(getBaseContext(), user);
            setTitle(getString(R.string.text_chat_title_divider, title, name));
        }
    }

    @Override
    public void onAccessibilityActionClick(View view, int actionId) {
        Object item = view.getTag();
        if (item instanceof MyTextMessage) {
            if (actionId == R.string.action_reply) {
                EditText send_msg = findViewById(R.id.user_im_edittext);
                if (send_msg != null) {
                    send_msg.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(send_msg, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }
    }

    @Override
    public List<AccessibilityActionCompat> getCustomActions(View view) {
        Object item = view.getTag();
        List<AccessibilityActionCompat> actions = new ArrayList<>();
        if (item instanceof MyTextMessage) {
            MyTextMessage msg = (MyTextMessage) item;
            if (msg.nFromUserID != getService().getTTInstance().getMyUserID() && msg.nMsgType == TextMsgType.MSGTYPE_USER) {
                actions.add(new AccessibilityActionCompat(R.string.action_reply, getString(R.string.action_reply)));
            }
        }
        return actions;
    }

    @Override
    public void onCmdUserTextMessage(TextMessage textmessage) {
        final int userid = TextMessageActivity.this.getIntent().getExtras().getInt(EXTRA_USERID);
        if (textmessage.nFromUserID == userid) {
            if (textmessage.nMsgType == TextMsgType.MSGTYPE_USER) {
                accessibilityAssistant.lockEvents();
                if (adapter != null)
                    adapter.notifyDataSetChanged();
                accessibilityAssistant.unlockEvents();
            } else if (textmessage.nMsgType == TextMsgType.MSGTYPE_CUSTOM && textmessage.szMessage.startsWith("typing\r\n")) {
                String value = textmessage.szMessage.substring(8);
                if ("1".equals(value)) {
                    showRemoteTyping();
                } else {
                    hideRemoteTyping();
                }
            }
        }
    }

    private void showRemoteTyping() {
        int userid = this.getIntent().getExtras().getInt(EXTRA_USERID);
        User user = getService().getUsers().get(userid);
        if (user == null) return;

        final String name = Utils.getDisplayName(getBaseContext(), user);
        runOnUiThread(() -> {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle(getString(R.string.text_typing_notification, name));
            }
        });

        if (remoteTypingTimer != null) {
            remoteTypingTimer.cancel();
        }
        remoteTypingTimer = new Timer();
        remoteTypingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle(null);
                    }
                });
            }
        }, TYPING_TIMEOUT);
    }

    private void hideRemoteTyping() {
        if (remoteTypingTimer != null) {
            remoteTypingTimer.cancel();
            remoteTypingTimer = null;
        }
        runOnUiThread(() -> {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle(null);
            }
        });
    }
}