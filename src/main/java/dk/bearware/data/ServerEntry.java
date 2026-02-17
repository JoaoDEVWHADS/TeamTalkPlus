
package dk.bearware.data;

public class ServerEntry {

    public enum ServerType {
        LOCAL,
        OFFICIAL,
        PUBLIC,
        UNOFFICIAL
    }

    public static final String KEY_SERVERNAME = "servername",
                               KEY_IPADDR = "ipaddr",
                               KEY_TCPPORT = "tcpport",
                               KEY_UDPPORT = "udpport",
                               KEY_USERNAME = "username",
                               KEY_PASSWORD = "password",
                               KEY_WEBLOGIN = "bearwarelogin",
                               KEY_NICKNAME = "nickname",
                               KEY_STATUSMSG = "statusmsg",
                               KEY_CHANNEL = "channel",
                               KEY_CHANPASSWD = "chanpasswd",
                               KEY_REMEMBER_LAST_CHANNEL = "remember_last_channel",
                               KEY_ENCRYPTED = "encrypted",
                               KEY_MOTD = "motd",
                               KEY_USERCOUNT = "usercount",
                               KEY_COUNTRY = "country",
                               KEY_PREFSCREEN = "serverentry_preferencescreen",
                               KEY_SRVSTATUS = "srv_status";

    public String servername = "";
    public String ipaddr = "";
    public int tcpport = 0, udpport = 0;
    public String username = "", password = "";
    public String nickname = "";
    public String statusmsg = "";
    public String channel = "", chanpasswd = "";
    public boolean rememberLastChannel = false;
    public boolean encrypted = false;
    public String cacert = "", clientcert = "", clientcertkey = "";
    public boolean verifypeer = false;
    public ServerType servertype = ServerType.LOCAL;

    public int stats_usercount = 0;
    public String stats_motd = "", stats_country = "";
}