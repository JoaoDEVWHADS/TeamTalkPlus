package dk.bearware.gui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.core.view.ViewCompat;

import androidx.annotation.NonNull;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import android.os.Bundle;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import dk.bearware.Channel;
import dk.bearware.User;
import dk.bearware.backend.TeamTalkService;

public class OnlineUsersAdapter extends ArrayAdapter<User> {
    private final LayoutInflater inflater;
    private final TeamTalkService service;
    private final AccessibilityAssistant accessibilityAssistant;

    public OnlineUsersAdapter(Context context, TeamTalkService service, List<User> users, AccessibilityAssistant accessibilityAssistant) {
        super(context, R.layout.item_online_user);
        this.service = service;
        this.accessibilityAssistant = accessibilityAssistant;
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
                 path.insert(0, getContext().getString(R.string.path_delimiter)); 
                 
                 
                 
                 
                 break;
            }
            path.insert(0, current.szName);
            path.insert(0, getContext().getString(R.string.path_delimiter));
            
            if(current.nParentID == 0) {
                 
                 
                 
                 
                 Channel root = service.getChannels().get(0);
                 String rootName = (root != null) ? getContext().getString(R.string.init_channel) : "Root";
                 path.insert(0, rootName);
                 path.insert(0, getContext().getString(R.string.path_delimiter));
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
            return getFullChannelPath(c.nParentID) + getContext().getString(R.string.path_delimiter) + pName;
        } else {
            return getContext().getString(R.string.init_channel) + getContext().getString(R.string.path_delimiter) + pName;
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_online_user, parent, false);
            holder = new ViewHolder();
            holder.nickname = (TextView) convertView.findViewById(R.id.nickname);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        User user = getItem(position);
        if (user != null) {
            boolean isOperator = service.getTTInstance().isChannelOperator(user.nUserID, user.nChannelID);
            String nickname = dk.bearware.gui.Utils.getDisplayName(getContext(), user);
            if (isOperator) nickname += " (Op)";
            String status = (user.szStatusMsg != null) ? user.szStatusMsg : "";
            String username = (user.szUsername != null) ? user.szUsername : "";
            String channel = getFullChannelPath(user.nChannelID);
            String ip = (user.szIPAddress != null) ? user.szIPAddress : "";
            String version = String.format(Locale.ROOT, "%d.%d.%d", (user.uVersion >> 16) & 0xFF, (user.uVersion >> 8) & 0xFF, user.uVersion & 0xFF);
            int identifier = user.nUserID;

            // Simplified formatting: Nickname (#ID) - Status [Username]
            // Followed by Channel and Metadata. 
            // Removed the redundant "User ID: ID" at the end.
            // User specified exact format: Apelido: %, Mensagem de Status: %, Usuário: %, Canal: %, Endereço IP: %, Versão: %, Identificação: %
            // Using localized strings for each label to support all languages.
            String formattedString = String.format(Locale.ROOT,
                getContext().getString(R.string.online_list_item_format),
                getContext().getString(R.string.online_list_nickname), nickname,
                getContext().getString(R.string.online_list_status_msg), status,
                getContext().getString(R.string.online_list_username), username,
                getContext().getString(R.string.online_list_channel), channel,
                getContext().getString(R.string.online_list_ip), ip,
                getContext().getString(R.string.online_list_version), version,
                getContext().getString(R.string.online_list_id), identifier
            );

            holder.nickname.setText(formattedString);

            holder.user = user;

            convertView.setContentDescription(formattedString);

            ViewCompat.setAccessibilityDelegate(convertView, accessibilityAssistant);
        }

        return convertView;
    }

    static class ViewHolder {
        TextView nickname;
        User user;
    }
}