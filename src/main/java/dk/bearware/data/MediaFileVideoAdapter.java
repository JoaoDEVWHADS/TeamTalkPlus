
package dk.bearware.data;

import java.nio.ByteBuffer;
import java.util.Vector;

import dk.bearware.DesktopInput;
import dk.bearware.MediaFileInfo;
import dk.bearware.User;
import dk.bearware.UserState;
import dk.bearware.VideoFrame;
import dk.bearware.backend.TeamTalkService;
import dk.bearware.events.ClientEventListener;
import dk.bearware.events.UserListener;
import dk.bearware.gui.Utils;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

public class MediaFileVideoAdapter extends MediaAdapter 
implements ClientEventListener.OnUserStateChangeListener, ClientEventListener.OnUserMediaFileVideoListener{

    public static final String TAG = "bearware";

    public MediaFileVideoAdapter(Context context) {
        super(context);
    }

    public void setTeamTalkService(TeamTalkService service) {
        super.setTeamTalkService(service);

        service.getEventHandler().registerOnUserStateChange(this, true);
        service.getEventHandler().registerOnUserMediaFileVideo(this, true);

        Vector<User> vecusers = Utils.getUsers(ttservice.getUsers());
        for(User user : vecusers) {
            if((user.uUserState & UserState.USERSTATE_MEDIAFILE_VIDEO) == UserState.USERSTATE_MEDIAFILE_VIDEO)
                display_users.put(user.nUserID, user);
        }
    }

    public void clearTeamTalkService(TeamTalkService service) {
        super.clearTeamTalkService(service);
        service.getEventHandler().unregisterListener(this);
    }

    @Override
    public Bitmap extractUserBitmap(int userid, Bitmap prev_bmp) {
        VideoFrame wnd = ttservice.getTTInstance().acquireUserMediaVideoFrame(userid);

        if(wnd == null) {
            return null;
        }

        if(prev_bmp != null) {

            if(prev_bmp.getWidth() != wnd.nWidth || prev_bmp.getHeight() != wnd.nHeight)
                prev_bmp = Bitmap.createBitmap(wnd.nWidth, wnd.nHeight, Bitmap.Config.ARGB_8888);
        }
        else {
            prev_bmp = Bitmap.createBitmap(wnd.nWidth, wnd.nHeight, Bitmap.Config.ARGB_8888);
        }

        prev_bmp.copyPixelsFromBuffer(ByteBuffer.wrap(wnd.frameBuffer));

        return prev_bmp;
    }

    @Override
    public void onUserStateChange(User user) {

        if((user.uUserState & UserState.USERSTATE_MEDIAFILE_VIDEO) == UserState.USERSTATE_MEDIAFILE_VIDEO)
            Log.d(TAG, "#" + user.nUserID + " video active");
        else
            Log.d(TAG, "#" + user.nUserID + " video inactive");
    }

    @Override
    public void onUserMediaFileVideo(int nUserID, int nStreamID) {

        if(media_sessions.indexOfKey(nUserID) >= 0)
            updateUserBitmap(nUserID);

        Log.d(TAG, "#" + nUserID + " video stream " + nStreamID);
    }
}