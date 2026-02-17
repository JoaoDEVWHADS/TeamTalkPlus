package dk.bearware.gui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import dk.bearware.TeamTalkBase;
import dk.bearware.User;
import dk.bearware.backend.TeamTalkConstants;
import dk.bearware.data.Preferences;

public class ManageStatusFragment extends Fragment {

    private Spinner statusSpinner;
    private EditText statusMessageInput;
    private Button saveButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_status, container, false);

        statusSpinner = view.findViewById(R.id.status_spinner);
        statusMessageInput = view.findViewById(R.id.status_message_input);
        saveButton = view.findViewById(R.id.save_status_button);

        saveButton.setOnClickListener(v -> saveStatus());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCurrentStatus();
    }

    private void loadCurrentStatus() {
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            TeamTalkBase client = activity.getClient();
            if (client != null) {
                User myself = new User();
                if (client.getUser(client.getMyUserID(), myself)) {
                    int statusMode = myself.nStatusMode;
                    
                    if ((statusMode & TeamTalkConstants.STATUSMODE_QUESTION) != 0) {
                         statusSpinner.setSelection(2); // Question
                    } else if ((statusMode & TeamTalkConstants.STATUSMODE_AWAY) != 0) {
                        statusSpinner.setSelection(1); // Away
                    } else {
                        statusSpinner.setSelection(0); // Online
                    }

                    String msg = myself.szStatusMsg;
                    if (TextUtils.isEmpty(msg)) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        msg = prefs.getString(Preferences.PREF_GENERAL_STATUSMSG, "");
                    }
                    statusMessageInput.setText(msg);
                }
            }
        }
    }

    private void saveStatus() {
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            TeamTalkBase client = activity.getClient();
            if (client != null) {
                User myself = new User();
                if (client.getUser(client.getMyUserID(), myself)) {
                    int newStatusMode = myself.nStatusMode; // Start with current flags

                    // Clear existing basic status flags
                    newStatusMode &= ~(TeamTalkConstants.STATUSMODE_AWAY | TeamTalkConstants.STATUSMODE_QUESTION);

                    int selectedPosition = statusSpinner.getSelectedItemPosition();
                    // 0 = Online, 1 = Away, 2 = Question
                    if (selectedPosition == 1) {
                        newStatusMode |= TeamTalkConstants.STATUSMODE_AWAY;
                    } else if (selectedPosition == 2) {
                        newStatusMode |= TeamTalkConstants.STATUSMODE_QUESTION;
                    }

                    String newStatusMsg = statusMessageInput.getText().toString();

                    client.doChangeStatus(newStatusMode, newStatusMsg);
                    
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    prefs.edit().putString(Preferences.PREF_GENERAL_STATUSMSG, newStatusMsg).apply();

                    Toast.makeText(getActivity(), R.string.status_updated, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
