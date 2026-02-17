
package dk.bearware.backend;

import dk.bearware.TeamTalkBase;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class TeamTalkConnection implements ServiceConnection {

    public String TAG = "bearware";

    TeamTalkConnectionListener ttlistener;
    TeamTalkService ttservice;

    Object waitService = new Object(); 

    public TeamTalkConnection(TeamTalkConnectionListener listener) {
        ttlistener = listener;
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {

        TeamTalkService.LocalBinder binder = (TeamTalkService.LocalBinder) service;

        synchronized (waitService) {
            ttservice = binder.getService();
            waitService.notifyAll();
        }

        String s = "TeamTalk instance 0x" +
                Integer.toHexString(ttservice.getTTInstance().hashCode() & 0xFFFFFFFF) +
                " running v. " + TeamTalkBase.getVersion() + " connected";
        Log.i(TAG, s);

        setBound(true);
        ttlistener.onServiceConnected(ttservice);
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        ttlistener.onServiceDisconnected(ttservice);
        setBound(false);

        String s = "TeamTalk instance 0x" +
            Integer.toHexString(ttservice.getTTInstance().hashCode() & 0xFFFFFFFF) +
            " disconnected";
        Log.i(TAG, s);

        synchronized (waitService) {
            ttservice = null;
        }
    }

    public TeamTalkService getService() {
        synchronized (waitService) {
            return ttservice;
        }
    }

    public TeamTalkBase getClient() {
        return getService().getTTInstance();
    }

    boolean bound = false;
    public void setBound(boolean bound) {
        this.bound = bound;
    }
    public boolean isBound() {
        return bound;
    }
}