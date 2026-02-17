
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

public class TextMessageActivity
extends AppCompatActivity implements TeamTalkConnectionListener, ClientEventListener.OnCmdUserTextMessageListener {

    public static final String TAG = "bearware";

    public static final String EXTRA_USERID = "userid";

    TeamTalkConnection mConnection;
    TextMessageAdapter adapter;
    AccessibilityAssistant accessibilityAssistant;

    TeamTalkService getService() {
        return mConnection.getService();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConnection = new TeamTalkConnection(this);
        setContentView(R.layout.activity_text_message);
        EdgeToEdgeHelper.enableEdgeToEdge(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        accessibilityAssistant = new AccessibilityAssistant(this);
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
                send_msg.setText("");
                adapter.notifyDataSetChanged();
            }
            else {
                Toast.makeText(TextMessageActivity.this,
                               R.string.err_send_text_message,
                               Toast.LENGTH_LONG).show();
            }
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
            setTitle(title + " - " + name);
        }
    }

    @Override
    public void onCmdUserTextMessage(TextMessage textmessage) {
        int userid = TextMessageActivity.this.getIntent().getExtras().getInt(EXTRA_USERID);
        if(adapter != null && textmessage.nFromUserID == userid &&
                textmessage.nMsgType == TextMsgType.MSGTYPE_USER) {
            accessibilityAssistant.lockEvents();
            adapter.notifyDataSetChanged();
            accessibilityAssistant.unlockEvents();
        }
    }
}