package dk.bearware.gui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import dk.bearware.Channel;
import dk.bearware.User;
import dk.bearware.backend.TeamTalkService;

public class OnlineUsersAdapter extends ArrayAdapter<User> {
    private final LayoutInflater inflater;
    private final TeamTalkService service;

    public OnlineUsersAdapter(Context context, TeamTalkService service, List<User> users) {
        super(context, android.R.layout.simple_list_item_1);
        this.service = service;
        inflater = LayoutInflater.from(context);
        updateUsers(users);
    }

    public void updateUsers(List<User> newUsers) {
        Collections.sort(newUsers, (u1, u2) -> {
             String n1 = dk.bearware.gui.Utils.getDisplayName(getContext(), u1);
             String n2 = dk.bearware.gui.Utils.getDisplayName(getContext(), u2);
             return n1.compareToIgnoreCase(n2);
        });
        clear();
        addAll(newUsers);
        notifyDataSetChanged();
    }

    private String getChannelPath(int channelId) {
        if (service == null || service.getChannels() == null) return "";
        
        Channel channel = service.getChannels().get(channelId);
        if (channel == null) return "";

        
        if (channel.nParentID == 0 && channel.nChannelID == 0) {
             return getContext().getString(R.string.init_channel); 
        }

        
        if(channel.nChannelID == 0) return ""; 

        String parentPath = "";
        if (channel.nParentID != 0) {
            parentPath = getChannelPath(channel.nParentID);
        }
        
        
        
        
        
        
        
        
        
        
        StringBuilder path = new StringBuilder();
        Channel current = channel;
        while(current != null) {
            if(current.nChannelID == 0) {
                 path.insert(0, getContext().getString(R.string.init_channel));
                 path.insert(0, "/"); 
                 
                 
                 
                 
                 break;
            }
            path.insert(0, current.szName);
            path.insert(0, "/");
            
            if(current.nParentID == 0) {
                 
                 
                 
                 
                 Channel root = service.getChannels().get(0);
                 String rootName = (root != null) ? getContext().getString(R.string.init_channel) : "Root";
                 path.insert(0, rootName);
                 path.insert(0, "/");
                 break;
            }
            current = service.getChannels().get(current.nParentID);
        }
        return path.toString();
    }
    
    
    private String getFullChannelPath(int channelId) {
        if (service == null || service.getChannels() == null) return "";
        Channel c = service.getChannels().get(channelId);
        if(c == null) return "?";
        
        if (c.nChannelID == 0) {
            return getContext().getString(R.string.init_channel);
        }
        
        String pName = c.szName;
        if(c.nParentID != 0) {
            return getFullChannelPath(c.nParentID) + "/" + pName;
        } else {
            
            return getContext().getString(R.string.init_channel) + "/" + pName;
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        User user = getItem(position);

        if (user != null) {
            StringBuilder sb = new StringBuilder();
            
            
            
            
            
            
            
            
            String nickname = dk.bearware.gui.Utils.getDisplayName(getContext(), user);
            
            sb.append(getContext().getString(R.string.online_user_info_nickname)).append(": ").append(nickname);
            
            
            sb.append(", ").append(getContext().getString(R.string.online_user_info_statusmsg)).append(": ").append(user.szStatusMsg);
            
            
            sb.append(", ").append(getContext().getString(R.string.online_list_username)).append(": ").append(user.szUsername);
            
            
            String channelPath = getFullChannelPath(user.nChannelID);
            sb.append(", ").append(getContext().getString(R.string.online_user_info_channel)).append(": ").append(channelPath);
            
            
            sb.append(", ").append(getContext().getString(R.string.online_user_info_ip)).append(": ").append(user.szIPAddress);
            
            
            String clientVersion = ((user.uVersion >> 16) & 0xFF) + "." + ((user.uVersion >> 8) & 0xFF) + "." + (user.uVersion & 0xFF);
            sb.append(", ").append(getContext().getString(R.string.online_list_version)).append(": ").append(clientVersion); 
            
            
            sb.append(", ").append(getContext().getString(R.string.online_list_id)).append(": ").append(user.nUserID);

            textView.setText(sb.toString());
        }

        return convertView;
    }
}