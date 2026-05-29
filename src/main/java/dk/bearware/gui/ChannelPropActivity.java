
package dk.bearware.gui;

import dk.bearware.BannedUser;
import dk.bearware.Channel;
import dk.bearware.ChannelType;
import dk.bearware.ClientErrorMsg;
import dk.bearware.RemoteFile;
import dk.bearware.ServerProperties;
import dk.bearware.TeamTalkBase;
import dk.bearware.TextMessage;
import dk.bearware.User;
import dk.bearware.UserAccount;
import dk.bearware.backend.TeamTalkConnection;
import dk.bearware.backend.TeamTalkConnectionListener;
import dk.bearware.backend.TeamTalkService;
import dk.bearware.UserRight;
import dk.bearware.OpusConstants;
import com.google.gson.Gson;
import dk.bearware.events.ClientEventListener;
import dk.bearware.events.CommandListener;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.text.TextWatcher;
import android.text.Editable;

public class ChannelPropActivity
extends AppCompatActivity
implements TeamTalkConnectionListener, ClientEventListener.OnCmdErrorListener, ClientEventListener.OnCmdSuccessListener {

    public static final String TAG = "bearware";

    public static final String EXTRA_CHANNELID = "channelid",   
                               EXTRA_PARENTID = "parentid";     

    public static final int REQUEST_AUDIOCODEC = 1,
                            REQUEST_AUDIOCONFIG = 2;

    TeamTalkConnection mConnection;
    Channel channel;

    TeamTalkService getService() {
        return mConnection.getService();
    }

    TeamTalkBase getClient() {
        return getService().getTTInstance();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK) {
            channel.audiocodec = Utils.getAudioCodec(data);
            channel.audiocfg = Utils.getAudioConfig(data);
            exchangeChannel(false);
        }
    }

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(dk.bearware.gui.LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConnection = new TeamTalkConnection(this);
        setContentView(R.layout.activity_channel_prop);
        EdgeToEdgeHelper.enableEdgeToEdge(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState != null) {
            String json = savedInstanceState.getString("channel_json");
            if (json != null) {
                channel = new Gson().fromJson(json, Channel.class);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (channel != null) {
            outState.putString("channel_json", new Gson().toJson(channel));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.channel_prop, menu);

        if(getIntent().getExtras().getInt(EXTRA_CHANNELID) == 0) {
            MenuItem item = menu.findItem(R.id.action_updatechannel);
            item.setTitle(getResources().getString(R.string.action_createchannel));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_updatechannel) {
            exchangeChannel(true);
            if(channel.nChannelID > 0) {
                updateCmdId = getClient().doUpdateChannel(channel);
                if(updateCmdId < 0) {
                    Toast.makeText(this, getResources().getString(R.string.text_con_cmderr),
                        Toast.LENGTH_LONG).show();
                }
            }
            else {
                exchangeChannel(true);
                CheckBox joinOnCreate = findViewById(R.id.chan_join_on_create);
                if (joinOnCreate.isChecked()) {
                    updateCmdId = getClient().doJoinChannel(channel);
                    if(updateCmdId > 0)
                        getService().setJoinChannel(channel);
                } else {
                    updateCmdId = getClient().doMakeChannel(channel);
                }
            }
        } else if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
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

    private String getCodecSummary(dk.bearware.AudioCodec codec) {
        switch (codec.nCodec) {
            case dk.bearware.Codec.OPUS_CODEC: {
                dk.bearware.OpusCodec opus = codec.opus;
                String app = (opus.nApplication == dk.bearware.OpusConstants.OPUS_APPLICATION_VOIP) ? "VoIP" : getString(R.string.opus_app_music);
                return getString(R.string.title_section_opus) + " (" + app + ", " + (opus.nSampleRate / 1000) + "kHz)";
            }
            case dk.bearware.Codec.SPEEX_CODEC:
                return getString(R.string.title_section_speex);
            case dk.bearware.Codec.SPEEX_VBR_CODEC:
                return getString(R.string.title_section_speexvbr);
            case dk.bearware.Codec.NO_CODEC:
                return getString(R.string.title_section_noaudio);
            default:
                return "";
        }
    }

    void exchangeChannel(boolean store) {

        EditText chanName = findViewById(R.id.channame);
        EditText chanTopic = findViewById(R.id.chantopic);
        EditText chanPasswd = findViewById(R.id.chanpasswd);
        EditText chanOpPasswd = findViewById(R.id.chanoppasswd);
        EditText chanMaxUsers = findViewById(R.id.chanmaxusers);
        EditText chanDiskQuota = findViewById(R.id.chandiskquota);
        CheckBox chanPermanent = findViewById(R.id.chan_permanent);
        CheckBox chanNoInterrupt = findViewById(R.id.chan_nointerrupt);
        CheckBox chanClassroom = findViewById(R.id.chan_classroom);
        CheckBox chanOpRecvOnly = findViewById(R.id.chan_oprecvonly);
        CheckBox chanNoVoiceAct = findViewById(R.id.chan_novoiceact);
        CheckBox chanNoAudioRec = findViewById(R.id.chan_noaudiorecord);
        CheckBox chanHidden = findViewById(R.id.chan_hidden);
        CheckBox chanJoinOnCreate = findViewById(R.id.chan_join_on_create);

        UserAccount myaccount = new UserAccount();
        boolean hasModifyRights = true;
        if (getService() != null && getClient() != null) {
            if (getClient().getMyUserAccount(myaccount)) {
                hasModifyRights = (myaccount.uUserRights & UserRight.USERRIGHT_MODIFY_CHANNELS) != 0;
            }
        }
        if (store) {
            channel.szName = chanName.getText().toString();
            channel.szTopic = chanTopic.getText().toString();
            channel.szPassword = chanPasswd.getText().toString();
            channel.szOpPassword = chanOpPasswd.getText().toString();
            try {
                channel.nMaxUsers = Integer.parseInt(chanMaxUsers.getText().toString());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid input for maximum channel users");
            }
            try {
                channel.nDiskQuota = Long.parseLong(chanDiskQuota.getText().toString());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid input for channel disk quota");
            }
            channel.nDiskQuota *= 1024;

            if(chanPermanent.isChecked())
                channel.uChannelType |= ChannelType.CHANNEL_PERMANENT;
            else
                channel.uChannelType &= ~ChannelType.CHANNEL_PERMANENT;
            if(chanNoInterrupt.isChecked())
                channel.uChannelType |= ChannelType.CHANNEL_SOLO_TRANSMIT;
            else
                channel.uChannelType &= ~ChannelType.CHANNEL_SOLO_TRANSMIT;
            if(chanClassroom.isChecked())
                channel.uChannelType |= ChannelType.CHANNEL_CLASSROOM;
            else
                channel.uChannelType &= ~ChannelType.CHANNEL_CLASSROOM;
            if(chanOpRecvOnly.isChecked())
                channel.uChannelType |= ChannelType.CHANNEL_OPERATOR_RECVONLY;
            else
                channel.uChannelType &= ~ChannelType.CHANNEL_OPERATOR_RECVONLY;
            if(chanNoVoiceAct.isChecked())
                channel.uChannelType |= ChannelType.CHANNEL_NO_VOICEACTIVATION;
            else
                channel.uChannelType &= ~ChannelType.CHANNEL_NO_VOICEACTIVATION;
            if(chanNoAudioRec.isChecked())
                channel.uChannelType |= ChannelType.CHANNEL_NO_RECORDING;
            else
                channel.uChannelType &= ~ChannelType.CHANNEL_NO_RECORDING;
            if(chanHidden.isChecked())
                channel.uChannelType |= ChannelType.CHANNEL_HIDDEN;
            else
                channel.uChannelType &= ~ChannelType.CHANNEL_HIDDEN;
        }
        else {
            chanName.setFocusable(channel.nParentID > 0);
            chanName.setText(channel.szName);
            chanTopic.setText(channel.szTopic);
            chanPasswd.setText(channel.szPassword);
            chanOpPasswd.setText(channel.szOpPassword);
            chanMaxUsers.setText(Integer.toString(channel.nMaxUsers));
            chanDiskQuota.setText(Long.toString(channel.nDiskQuota / 1024));

            chanPermanent.setChecked((channel.uChannelType & ChannelType.CHANNEL_PERMANENT) != 0);
            chanNoInterrupt.setChecked((channel.uChannelType & ChannelType.CHANNEL_SOLO_TRANSMIT) != 0);
            chanClassroom.setChecked((channel.uChannelType & ChannelType.CHANNEL_CLASSROOM) != 0);
            chanOpRecvOnly.setChecked((channel.uChannelType & ChannelType.CHANNEL_OPERATOR_RECVONLY) != 0);
            chanNoVoiceAct.setChecked((channel.uChannelType & ChannelType.CHANNEL_NO_VOICEACTIVATION) != 0);
            chanNoAudioRec.setChecked((channel.uChannelType & ChannelType.CHANNEL_NO_RECORDING) != 0);
            chanHidden.setChecked((channel.uChannelType & ChannelType.CHANNEL_HIDDEN) != 0);

            if (!hasModifyRights) {
                chanPermanent.setVisibility(View.GONE);
                chanHidden.setVisibility(View.GONE);
                chanJoinOnCreate.setVisibility(View.GONE);
                chanJoinOnCreate.setChecked(true);
            } else {
                chanPermanent.setVisibility(View.VISIBLE);
                chanHidden.setVisibility(View.VISIBLE);
                chanJoinOnCreate.setVisibility(channel.nChannelID == 0 ? View.VISIBLE : View.GONE);
            }

            setupPasswordToggle(chanPasswd, (ImageButton) findViewById(R.id.btn_toggle_chanpasswd));
            setupPasswordCopy(chanPasswd, (ImageButton) findViewById(R.id.btn_copy_chanpasswd), R.string.msg_password_copied);
            setupPasswordToggle(chanOpPasswd, (ImageButton) findViewById(R.id.btn_toggle_chanoppasswd));
            setupPasswordCopy(chanOpPasswd, (ImageButton) findViewById(R.id.btn_copy_chanoppasswd), R.string.msg_password_copied);
            
            setupPasswordCopy(chanName, (ImageButton) findViewById(R.id.btn_copy_channame), R.string.msg_channelname_copied);
            setupPasswordCopy(chanTopic, (ImageButton) findViewById(R.id.btn_copy_chantopic), R.string.msg_note_copied);
            
            SeekBar maxUsersSeekBar = findViewById(R.id.chanmaxusers_seekBar);
            SeekBar diskQuotaSeekBar = findViewById(R.id.chandiskquota_seekBar);
            
            int maxServerUsers = 1000;
            ServerProperties prop = new ServerProperties();
            if (getService().getTTInstance().getServerProperties(prop)) {
                maxServerUsers = prop.nMaxUsers;
            }
            setupSeekBarSync(maxUsersSeekBar, chanMaxUsers, maxServerUsers);
            setupSeekBarSync(diskQuotaSeekBar, chanDiskQuota, 1048576); // 1GB quota slider limit
        }

        TextView codecSummary = findViewById(R.id.setup_audcodec_summary);
        if (codecSummary != null && channel != null && channel.audiocodec != null) {
            codecSummary.setText(getCodecSummary(channel.audiocodec));
        }
    }

    @Override
    public void onServiceConnected(TeamTalkService service) {
        service.getEventHandler().registerOnCmdError(this, true);
        service.getEventHandler().registerOnCmdSuccess(this, true);

        if (channel == null) {
            int channelid = getIntent().getExtras().getInt(EXTRA_CHANNELID);
            int parentid = getIntent().getExtras().getInt(EXTRA_PARENTID);
            if(channelid > 0) {

                channel = service.getChannels().get(channelid);
                if (channel == null) {
                    setResult(RESULT_CANCELED);
                    finish();
                    return;
                }
            }
            else if(parentid > 0) {
                channel = new Channel(true, true);
                channel.nParentID = parentid;
                
                // Fix for Fixed Volume (AGC) being checked by default
                channel.audiocfg.bEnableAGC = false;
                
                // Fix for "Ignore silence" (DTX) being checked by default
                channel.audiocodec.opus.bDTX = false;
                channel.audiocodec.speex_vbr.bDTX = false;
                
                // Default Join on Create to false for new channels. 
                // exchangeChannel will override this to true for limited users.
                ((CheckBox)findViewById(R.id.chan_join_on_create)).setChecked(false);
                ServerProperties prop = new ServerProperties();
                if (service.getTTInstance().getServerProperties(prop)) {
                    channel.nMaxUsers = prop.nMaxUsers;
                }
            }
        }

        exchangeChannel(false);

        Button codec_btn = findViewById(R.id.setup_audcodec_btn);

        OnClickListener listener = v -> {
            if (v.getId() == R.id.setup_audcodec_btn) {
                Intent edit = new Intent(ChannelPropActivity.this, AudioCodecActivity.class);
                edit = Utils.putAudioCodec(edit, channel.audiocodec);
                edit = Utils.putAudioConfig(edit, channel.audiocfg);
                exchangeChannel(true);
                edit = Utils.putChannel(edit, channel);
                startActivityForResult(edit, REQUEST_AUDIOCODEC);
            }
        };
        codec_btn.setOnClickListener(listener);

    }

    @Override
    public void onServiceDisconnected(TeamTalkService service) {
        service.getEventHandler().unregisterListener(this);
    }

    int updateCmdId = 0;

    private void setupPasswordToggle(EditText field, ImageButton btn) {
        if (btn == null || field == null) return;
        btn.setTag(false);
        btn.setOnClickListener(v -> {
            boolean visible = (Boolean) btn.getTag();
            if (visible) {
                field.setTransformationMethod(PasswordTransformationMethod.getInstance());
            } else {
                field.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }
            btn.setTag(!visible);
            field.setSelection(field.getText().length());
        });
    }

    private void setupPasswordCopy(EditText field, ImageButton btn, int msgResId) {
        if (btn == null || field == null) return;
        btn.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.label_copied_text), field.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, msgResId, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onCmdError(int cmdId, ClientErrorMsg errmsg) {
        if (updateCmdId == cmdId) {
            updateCmdId = 0;
            Toast.makeText(this, errmsg.szErrorMsg, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCmdSuccess(int cmdId) {
        setResult(RESULT_OK);
        finish();
    }

    private void setupSeekBarSync(final SeekBar seekBar, final EditText editText, final int max) {
        if (seekBar == null || editText == null) return;
        seekBar.setMax(max);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    editText.setText(String.valueOf(progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    String str = s.toString();
                    if (str.isEmpty()) return;
                    int val = Integer.parseInt(str);
                    if (val >= 0 && val <= max) {
                        seekBar.setProgress(val);
                    }
                } catch (NumberFormatException ignored) {}
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }
}
