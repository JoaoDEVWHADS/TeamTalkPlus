
package dk.bearware.backend;

public class TeamTalkConstants {

    public static final int STATUSMODE_AVAILABLE        = 0x00000000;
    public static final int STATUSMODE_AWAY             = 0x00000001; 
    public static final int STATUSMODE_QUESTION         = 0x00000002;
    public static final int STATUSMODE_MODE             = 0x000000FF;

    public static final int STATUSMODE_FLAGS            = 0xFFFFFF00;

    public static final int STATUSMODE_MALE             = 0x00000000;
    public static final int STATUSMODE_FEMALE           = 0x00000100;
    public static final int STATUSMODE_NEUTRAL          = 0x00001000;
    public static final int STATUSMODE_VIDEOTX          = 0x00000200;
    public static final int STATUSMODE_DESKTOP          = 0x00000400;
    public static final int STATUSMODE_STREAM_MEDIAFILE = 0x00000800;

    public static final int OPUS_MIN_TXINTERVALMSEC = 20;
    public static final int OPUS_MAX_TXINTERVALMSEC = 500;
    public static final int OPUS_DEFAULT_FRAMESIZEMSEC = 0; 

    public static final int SPEEX_MIN_TXINTERVALMSEC = 20;
    public static final int SPEEX_MAX_TXINTERVALMSEC = 500;

    public static final int CHANNEL_AUDIOCONFIG_MAX = 32000;
    public static final boolean DEFAULT_CHANNEL_AUDIOCONFIG_ENABLE = false;
    public static final int DEFAULT_CHANNEL_AUDIOCONFIG_LEVEL = 9600; 
}