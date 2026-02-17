package dk.bearware.gui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import dk.bearware.ServerStatistics;
import dk.bearware.TeamTalkBase;
import dk.bearware.backend.TeamTalkConnection;
import dk.bearware.backend.TeamTalkConnectionListener;
import dk.bearware.backend.TeamTalkService;
import dk.bearware.events.ClientEventListener;

public class ServerStatisticsActivity extends AppCompatActivity implements TeamTalkConnectionListener, ClientEventListener.OnCmdServerStatisticsListener {
    private TeamTalkConnection mConnection;
    private TextView tvDesktop;
    private TextView tvMedia;
    private TextView tvTotal;
    private TextView tvUptime;
    private TextView tvVideo;
    private TextView tvVoice;

    @Override
    public void onServiceDisconnected(TeamTalkService teamTalkService) {
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_server_stats);
        this.mConnection = new TeamTalkConnection(this);
        this.tvUptime = (TextView) findViewById(R.id.stats_uptime);
        this.tvTotal = (TextView) findViewById(R.id.stats_total);
        this.tvVoice = (TextView) findViewById(R.id.stats_voice);
        this.tvVideo = (TextView) findViewById(R.id.stats_video);
        this.tvMedia = (TextView) findViewById(R.id.stats_media);
        this.tvDesktop = (TextView) findViewById(R.id.stats_desktop);
        
        ((Button) findViewById(R.id.refresh_btn)).setOnClickListener(v -> {
            if (mConnection.isBound()) {
                getClient().doQueryServerStats();
            }
        });
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.action_server_stats);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (this.mConnection.isBound()) {
            return;
        }
        Intent intent = new Intent(this, TeamTalkService.class);
        bindService(intent, this.mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.mConnection.isBound()) {
            if (getService() != null)
                getService().getEventHandler().unregisterListener(this);
            unbindService(this.mConnection);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private TeamTalkService getService() {
        return this.mConnection.getService();
    }

    private TeamTalkBase getClient() {
        return getService().getTTInstance();
    }

    @Override
    public void onServiceConnected(TeamTalkService teamTalkService) {
        teamTalkService.getEventHandler().registerOnCmdServerStatistics(this, true);
        teamTalkService.getTTInstance().doQueryServerStats();
    }

    @Override
    public void onCmdServerStatistics(final ServerStatistics serverStatistics) {
        runOnUiThread(() -> {
            this.tvUptime.setText(getString(R.string.stats_uptime_format, (serverStatistics.nUptimeMSec / 1000) / 60));
            this.tvTotal.setText(getString(R.string.stats_total_format, describeSize(serverStatistics.nTotalBytesTX), describeSize(serverStatistics.nTotalBytesRX)));
            this.tvVoice.setText(getString(R.string.stats_voice_format, describeSize(serverStatistics.nVoiceBytesTX), describeSize(serverStatistics.nVoiceBytesRX)));
            this.tvVideo.setText(getString(R.string.stats_video_format, describeSize(serverStatistics.nVideoCaptureBytesTX), describeSize(serverStatistics.nVideoCaptureBytesRX)));
            this.tvMedia.setText(getString(R.string.stats_media_format, describeSize(serverStatistics.nMediaFileBytesTX), describeSize(serverStatistics.nMediaFileBytesRX)));
            this.tvDesktop.setText(getString(R.string.stats_desktop_format, describeSize(serverStatistics.nDesktopBytesTX), describeSize(serverStatistics.nDesktopBytesRX)));
        });
    }

    private String describeSize(long j) {
        if (j < 1024) return getString(R.string.unit_bytes, j);
        if (j < 1048576) return getString(R.string.unit_kilobytes, j / 1024);
        if (j < 1073741824) return getString(R.string.unit_megabytes, (double)j / 1048576.0);
        return getString(R.string.unit_gigabytes, (double)j / 1073741824.0);
    }
}
