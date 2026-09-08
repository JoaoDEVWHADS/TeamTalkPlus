
package dk.bearware.data;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import dk.bearware.Constants;
import dk.bearware.TextMessage;

public class MyTextMessage extends TextMessage {

    public String szNickName = "";
    public Object userData;

    public Date time = Calendar.getInstance().getTime();

    public MyTextMessage(TextMessage msg, String name) {
        super(msg);
        this.szNickName = name;
    }

    public MyTextMessage(String name) {
        this.szNickName = name;
    }

    public MyTextMessage() {
    }

    public MyTextMessage(MyTextMessage msg) {
        super(msg);
        szNickName = msg.szNickName;
        userData = msg.userData;
        time = msg.time;
    }

    public Vector<MyTextMessage> split() {
        MyTextMessage newmsg = new MyTextMessage(this);
        Vector<MyTextMessage> result = new Vector<>();
        if (szMessage.getBytes(StandardCharsets.UTF_8).length <= Constants.TT_STRLEN - 1) {
            newmsg.bMore = false;
            result.add(newmsg);
            return result;
        }

        newmsg = new MyTextMessage(newmsg);
        newmsg.bMore = true;

        String remain = szMessage;
        int curlen = remain.length();
        while (remain.substring(0, curlen).getBytes(StandardCharsets.UTF_8).length > Constants.TT_STRLEN - 1)
            curlen /= 2;

        int half = Constants.TT_STRLEN / 2;
        while (half > 0)
        {
            int utf8strlen = remain.substring(0, Math.min(curlen + half, remain.length())).getBytes(StandardCharsets.UTF_8).length;
            if (utf8strlen <= Constants.TT_STRLEN - 1)
                curlen += half;
            if (utf8strlen == Constants.TT_STRLEN - 1)
                break;
            half /= 2;
        }

        newmsg.szMessage = remain.substring(0, curlen);
        result.add(newmsg);

        newmsg = new MyTextMessage(newmsg);
        newmsg.szMessage = remain.substring(curlen);
        result.addAll(newmsg.split());
        return result;
    }

    public static MyTextMessage mergeMessage(Map<Integer, Vector<MyTextMessage>> pending, MyTextMessage msg) {
        int key = (msg.nMsgType << 16) | msg.nFromUserID;
        Vector<MyTextMessage> parts = pending.get(key);
        if (msg.bMore) {
            if (parts == null) {
                parts = new Vector<>();
                pending.put(key, parts);
            }
            parts.add(msg);
            if (parts.size() > 1000)
                pending.remove(key);
            return null;
        }
        if (parts != null) {
            StringBuilder content = new StringBuilder();
            for (MyTextMessage part : parts)
                content.append(part.szMessage);
            content.append(msg.szMessage);
            msg.szMessage = content.toString();
            pending.remove(key);
        }
        return msg;
    }

    public static void merge(Vector<MyTextMessage> msgs) {
        Map<Integer, Vector<MyTextMessage>> mergeMsgs = new HashMap<>();
        Vector<MyTextMessage> removeMsgs = new Vector<>();
        for (MyTextMessage m : msgs) {
            int key = (m.nMsgType << 16) | m.nFromUserID;
            Vector<MyTextMessage> moreMessages = mergeMsgs.get(key);
            if (m.bMore) {
                if (moreMessages == null) {
                    moreMessages = new Vector<>();
                    mergeMsgs.put(key, moreMessages);
                }
                moreMessages.add(m);
            } else if (moreMessages != null) {
                StringBuilder content = new StringBuilder();
                for (MyTextMessage moreMessage : moreMessages) {
                    content.append(moreMessage.szMessage);
                    removeMsgs.add(moreMessage);
                }
                m.szMessage = content.append(m.szMessage).toString();
                mergeMsgs.remove(key);
            }
        }
        msgs.removeAll(removeMsgs);
    }

    public static final int MSGTYPE_LOG_INFO    = 0x80000000;
    public static final int MSGTYPE_LOG_ERROR   = 0x40000000;
    public static final int MSGTYPE_SERVERPROP  = 0x20000000;

    public static MyTextMessage createLogMsg(int nMsgType, String szMessage) {
        MyTextMessage newmsg = new MyTextMessage();
        newmsg.nMsgType = nMsgType;
        newmsg.szMessage = szMessage;
        return newmsg;
    }

    public static MyTextMessage createUserDefMsg(int nMsgType, Object userData) {
        MyTextMessage newmsg = new MyTextMessage();
        newmsg.nMsgType = nMsgType;
        newmsg.userData = userData;
        return newmsg;
    }
}