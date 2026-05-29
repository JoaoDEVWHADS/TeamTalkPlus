
package dk.bearware.backend;

import dk.bearware.TeamTalkBase;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import dk.bearware.gui.R;
import dk.bearware.gui.BuildConfig;

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

        String s = ttservice.getString(R.string.tt_instance_info,
                Integer.toHexString(ttservice.getTTInstance().hashCode() & 0xFFFFFFFF),
                ttservice.getString(R.string.tt_instance_connected, BuildConfig.VERSION_NAME));
        Log.i(TAG, s);

        setBound(true);
        ttlistener.onServiceConnected(ttservice);
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        ttlistener.onServiceDisconnected(ttservice);
        setBound(false);

        String s = ttservice.getString(R.string.tt_instance_info,
                Integer.toHexString(ttservice.getTTInstance().hashCode() & 0xFFFFFFFF),
                ttservice.getString(R.string.tt_instance_disconnected));
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