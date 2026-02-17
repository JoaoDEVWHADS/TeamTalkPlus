
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
import android.view.LayoutInflater;

public class WebcamAdapter extends MediaAdapter implements ClientEventListener.OnUserVideoCaptureListener {

    public static final String TAG = "bearware";

    private final LayoutInflater inflater;

    public WebcamAdapter(Context context) {
        super(context);
        inflater = LayoutInflater.from(context);
    }

    public void setTeamTalkService(TeamTalkService service) {
        super.setTeamTalkService(service);

        service.getEventHandler().registerOnUserVideoCapture(this, true);

        Vector<User> vecusers = Utils.getUsers(service.getUsers());
        for(User user : vecusers) {
            if((user.uUserState & UserState.USERSTATE_VIDEOCAPTURE) == UserState.USERSTATE_VIDEOCAPTURE)
                display_users.put(user.nUserID, user);
        }
    }

    public void clearTeamTalkService(TeamTalkService service) {
        super.clearTeamTalkService(service);
        service.getEventHandler().unregisterListener(this);
    }

    @Override
    public Bitmap extractUserBitmap(int userid, Bitmap prev_bmp) {
        VideoFrame wnd = ttservice.getTTInstance().acquireUserVideoCaptureFrame(userid);

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
    public void onUserVideoCapture(int nUserID, int nStreamID) {

        if (media_sessions.indexOfKey(nUserID) >= 0)
            updateUserBitmap(nUserID);
    }
}