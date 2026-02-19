
package dk.bearware.data;

import java.util.Vector;
import java.text.DateFormat;

import dk.bearware.ServerProperties;
import dk.bearware.TextMsgType;
import dk.bearware.gui.AccessibilityAssistant;
import dk.bearware.gui.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TextMessageAdapter extends BaseAdapter {

    private Vector<MyTextMessage> messages, 
            messagesUpdateView; 

    private final LayoutInflater inflater;
    private final AccessibilityAssistant accessibilityAssistant;
    private final DateFormat dateFormat;

    private int myuserid;

    private int filterMsgType = -1; 

    private boolean show_logs = true;

    int def_bg_color, def_text_color;

    int user_bg_color = 0xff4c9fff, user_text_color = Color.WHITE;

    int self_bg_color = 0xff659f5d, self_text_color = Color.WHITE;

    int loginfo_bg_color, loginfo_text_color; 

    int logerr_bg_color = 0xffcd0028, logerr_text_color = Color.WHITE;

    int srvinfo_bg_color = Color.DKGRAY, srvinfo_text_color = Color.WHITE;

    public TextMessageAdapter(Context context, AccessibilityAssistant accessibilityAssistant,
                              Vector<MyTextMessage> msgs, int myuserid) {
        this(context, accessibilityAssistant);
        setMyUserID(myuserid);

        setTextMessages(msgs);
    }

    @SuppressWarnings("ResourceType")
    public TextMessageAdapter(Context context, AccessibilityAssistant accessibilityAssistant) {
        inflater = LayoutInflater.from(context);
        this.accessibilityAssistant = accessibilityAssistant;
        this.dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
        setTextMessages(new Vector<>());

        TypedArray array = context.getTheme().obtainStyledAttributes(new int[] {
            android.R.attr.colorBackground, 
            android.R.attr.textColorPrimary, 
        });
        def_bg_color = array.getColor(0, 0xFF00FF);
        def_text_color = array.getColor(1, 0xFF00FF);

        array.recycle();

        loginfo_bg_color = def_bg_color;
        loginfo_text_color = def_text_color;
    }

    public void setTextMessages(Vector<MyTextMessage> msgs) {
        this.messages = msgs;
        copyToMessagesView();
    }

    private void copyToMessagesView() {
        this.messagesUpdateView = (Vector<MyTextMessage>)this.messages.clone();
    }

    private int explicitMsgType = -1; 
    private java.util.Set<Integer> excludedMsgTypes = new java.util.HashSet<>();

    public void setFilterMsgType(int msgType) {
        this.explicitMsgType = msgType;
        this.excludedMsgTypes.clear(); 
    }

    public void setExcludedMsgTypes(java.util.Set<Integer> excludedTypes) {
        this.excludedMsgTypes = new java.util.HashSet<>(excludedTypes);
        this.explicitMsgType = -1; 
    }
    
    Vector<MyTextMessage> getMessages() {
        Vector<MyTextMessage> result = new Vector<>();
        for(MyTextMessage m : this.messagesUpdateView) {
            if (explicitMsgType != -1 && m.nMsgType != explicitMsgType)
                continue;
            
            if (excludedMsgTypes.contains(m.nMsgType))
                continue;

            switch(m.nMsgType) {
                case MyTextMessage.MSGTYPE_LOG_ERROR :
                case MyTextMessage.MSGTYPE_LOG_INFO :
                    if (show_logs)
                        result.add(m);
                    break;
                default :
                    result.add(m);
                    break;
            }
        }
        return result;
    }

    public void setMyUserID(int userid) {
        myuserid = userid;
    }

    public void showLogMessages(boolean enable) {
        show_logs = enable;
    }

    @Override
    public int getCount() {
        return getMessages().size();
    }

    @Override
    public Object getItem(int position) {
        return getMessages().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class TextMessageViewHolder {
        TextView name;
        TextView msgtext;
        TextView msgdate;
    }

    private static class ServerInfoViewHolder {
        TextView logmsg;
        TextView logmotd;
        TextView logtm;
    }

    private static class LogMessageViewHolder {
        TextView logmsg;
        TextView logtm;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MyTextMessage txtmsg = getMessages().get(position);

        int bg_color = Color.BLACK, text_color = Color.WHITE;

        switch(txtmsg.nMsgType) {
            case TextMsgType.MSGTYPE_CHANNEL :
            case TextMsgType.MSGTYPE_BROADCAST :
            case TextMsgType.MSGTYPE_USER : {
                if(convertView == null ||
                   convertView.findViewById(R.id.item_textmsg) == null)
                    convertView = inflater.inflate(R.layout.item_textmsg, parent, false);

                if(txtmsg.nFromUserID == myuserid) {
                    bg_color = self_bg_color;
                    text_color = self_text_color;
                }
                else {
                    bg_color = user_bg_color;
                    text_color = user_text_color;
                }

                TextView name = convertView.findViewById(R.id.name_text);
                TextView msgtext = convertView.findViewById(R.id.msg_text);
                TextView msgdate = convertView.findViewById(R.id.time_text);

                name.setText(txtmsg.szNickName);
                msgdate.setText(dateFormat.format(txtmsg.time));
                msgtext.setText(txtmsg.szMessage);

                name.setTextColor(text_color);
                msgdate.setTextColor(text_color);
                msgtext.setTextColor(text_color);
                break;
            }
            case MyTextMessage.MSGTYPE_SERVERPROP : {
                if(convertView == null ||
                   convertView.findViewById(R.id.item_textmsg_srvinfo) == null) {
                    convertView = inflater.inflate(R.layout.item_textmsg_srvinfo, parent, false);
                }

                bg_color = srvinfo_bg_color;
                text_color = srvinfo_text_color;

                TextView logmsg = convertView.findViewById(R.id.srvname_text);
                TextView logmotd = convertView.findViewById(R.id.srvmotd_text);
                TextView logtm = convertView.findViewById(R.id.logtime_text);

                ServerProperties p = (ServerProperties)txtmsg.userData;
                logmsg.setText(p.szServerName);
                logmotd.setText(p.szMOTD);
                logtm.setText(dateFormat.format(txtmsg.time));

                logmsg.setTextColor(text_color);
                logtm.setTextColor(text_color);
                break;
            }
            case MyTextMessage.MSGTYPE_LOG_ERROR :
            case MyTextMessage.MSGTYPE_LOG_INFO :
            default : {
                if(convertView == null ||
                   convertView.findViewById(R.id.item_textmsg_logmsg) == null) {
                    convertView = inflater.inflate(R.layout.item_textmsg_logmsg, parent, false);
                }

                switch(txtmsg.nMsgType) {
                    case MyTextMessage.MSGTYPE_LOG_ERROR :
                        bg_color = logerr_bg_color;
                        text_color = logerr_text_color;
                        break;
                    case MyTextMessage.MSGTYPE_LOG_INFO : 
                        bg_color = loginfo_bg_color;
                        text_color = loginfo_text_color;
                        break;
                }

                TextView logmsg = convertView.findViewById(R.id.logmsg_text);
                TextView logtm = convertView.findViewById(R.id.logtime_text);

                logmsg.setText(txtmsg.szMessage);
                logtm.setText(dateFormat.format(txtmsg.time));

                logmsg.setTextColor(text_color);
                logtm.setTextColor(text_color);
                break;
            }
        }

        convertView.setBackgroundColor(bg_color);
        convertView.setAccessibilityDelegate(accessibilityAssistant);

        android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(parent.getContext());
        float scale = 1.0f;
        try {
            // Try reading as int (new format: 50-300)
            int scaleInt = prefs.getInt("pref_display_font_scale", 100);
            scale = scaleInt / 100.0f;
        } catch (ClassCastException e) {
            // Fallback: Try reading as string (old format: "1.0")
            try {
                String scaleStr = prefs.getString("pref_display_font_scale", "1.0");
                scale = Float.parseFloat(scaleStr);
            } catch (Exception ex) {
                scale = 1.0f;
            }
        }

        if (convertView instanceof ViewGroup) {
            applyFontScale((ViewGroup) convertView, scale);
        }

        return convertView;
    }

    private void applyFontScale(ViewGroup parent, float scale) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;

                Object tag = tv.getTag(R.id.original_text_size);
                float originalSize;
                if (tag == null) {
                    originalSize = tv.getTextSize(); 
                    tv.setTag(R.id.original_text_size, originalSize);
                } else {
                    originalSize = (Float) tag;
                }
                tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, originalSize * scale);
            } else if (child instanceof ViewGroup) {
                applyFontScale((ViewGroup) child, scale);
            }
        }
    }

    @Override
    public void notifyDataSetChanged() {
        copyToMessagesView();
        super.notifyDataSetChanged();
    }
}