package dk.bearware.gui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dk.bearware.BannedUser;

public class BannedUsersAdapter extends BaseAdapter {
    private Context context;
    private List<BannedUser> bannedUsers;
    private Set<Integer> selectedPositions = new HashSet<>();
    private LayoutInflater inflater;

    public BannedUsersAdapter(Context context, List<BannedUser> bannedUsers) {
        this.context = context;
        this.bannedUsers = bannedUsers;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return bannedUsers.size();
    }

    @Override
    public Object getItem(int position) {
        return bannedUsers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_banned, parent, false);
            holder = new ViewHolder();
            holder.checkBox = convertView.findViewById(R.id.checkbox_banned);
            holder.textName = convertView.findViewById(R.id.text_banned_name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        BannedUser user = bannedUsers.get(position);
        holder.textName.setText(user.szNickname + " (" + user.szIPAddress + ")");

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedPositions.contains(position));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPositions.add(position);
            } else {
                selectedPositions.remove(position);
            }
        });

        convertView.setOnClickListener(v -> {
            holder.checkBox.setChecked(!holder.checkBox.isChecked());
        });

        return convertView;
    }

    public List<BannedUser> getSelectedUsers() {
        List<BannedUser> selected = new ArrayList<>();
        for (int pos : selectedPositions) {
            if (pos < bannedUsers.size()) {
                selected.add(bannedUsers.get(pos));
            }
        }
        return selected;
    }

    public void clearSelection() {
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        CheckBox checkBox;
        TextView textName;
    }
}