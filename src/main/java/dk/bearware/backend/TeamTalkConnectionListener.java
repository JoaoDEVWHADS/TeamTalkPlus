
package dk.bearware.backend;

public interface TeamTalkConnectionListener {

    void onServiceConnected(TeamTalkService service);
    void onServiceDisconnected(TeamTalkService service);

}