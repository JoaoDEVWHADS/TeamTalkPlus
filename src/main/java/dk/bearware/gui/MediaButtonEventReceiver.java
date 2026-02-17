
package dk.bearware.gui;

import dk.bearware.backend.TeamTalkService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class MediaButtonEventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            TeamTalkService.LocalBinder serviceBinder = (TeamTalkService.LocalBinder)peekService(context, new Intent(context, TeamTalkService.class));
            TeamTalkService ttService = (serviceBinder != null) ? serviceBinder.getService() : null;
            KeyEvent keyEvent = (KeyEvent)intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
            if (ttService != null && keyEvent != null) {
                int keyAction = keyEvent.getAction();
                int keyCode = keyEvent.getKeyCode();
                switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_HEADSETHOOK:
                    if (keyAction == KeyEvent.ACTION_UP) {
                        if (ttService.isVoiceActivationEnabled())
                            ttService.enableVoiceActivation(false);
                        else
                            ttService.enableVoiceTransmission(!ttService.isVoiceTransmissionEnabled());
                    }
                    abortBroadcast();
                    break;
                default:
                    break;
                }
            }
        }
    }

}