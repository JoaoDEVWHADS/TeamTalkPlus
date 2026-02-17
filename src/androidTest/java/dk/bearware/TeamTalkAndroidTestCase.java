
package dk.bearware;

import android.Manifest;
import android.os.Environment;

import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;

import java.io.File;

public class TeamTalkAndroidTestCase extends TeamTalkTestCase {

    public TeamTalkBase newClientInstance() {
        TeamTalkBase ttclient = new TeamTalk5();
        ttclients.add(ttclient);
        return ttclient;
    }

    @Rule
    public GrantPermissionRule permissionRule1 = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.INTERNET,
            Manifest.permission.VIBRATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.READ_PHONE_STATE);

    public void setUp() throws Exception {
        ADMIN_USERNAME = "admin";
        ADMIN_PASSWORD = "admin";

        IPADDR = "192.168.0.51";
        TCPPORT = 10333;
        UDPPORT = 10333;

        super.setUp();

        INPUTDEVICEID = SoundDeviceConstants.TT_SOUNDDEVICE_ID_OPENSLES_VOICECOM | SoundDeviceConstants.TT_SOUNDDEVICE_ID_SHARED_FLAG;
        OUTPUTDEVICEID = SoundDeviceConstants.TT_SOUNDDEVICE_ID_OPENSLES_VOICECOM | SoundDeviceConstants.TT_SOUNDDEVICE_ID_SHARED_FLAG;

        File filepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        STORAGEFOLDER = filepath.toString();
    }
}