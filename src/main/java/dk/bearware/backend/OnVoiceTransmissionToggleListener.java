
package dk.bearware.backend;

public interface OnVoiceTransmissionToggleListener {

    void onVoiceTransmissionToggle(boolean voiceTransmissionEnabled, boolean isSuspended);

    void onVoiceActivationToggle(boolean voiceActivationEnabled, boolean isSuspended);

}