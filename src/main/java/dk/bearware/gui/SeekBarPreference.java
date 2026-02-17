
package dk.bearware.gui;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SeekBarPreference extends DialogPreference implements
    OnSeekBarChangeListener {

    int value = 100;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupLayout(context, attrs);

        value = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "defaultValue", value);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupLayout(context, attrs);
        value = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "defaultValue", value);
    }

    private void setupLayout(Context context, AttributeSet attrs) {
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout view = new LinearLayout(this.getContext());
        view.setOrientation(LinearLayout.VERTICAL);
        view.setGravity(Gravity.CENTER_HORIZONTAL);
        view.setPadding(10, 10, 10, 10);

        SeekBar seek = new SeekBar(getContext());

        seek.setMax(100);

        value = getPersistedInt(value);
        seek.setProgress(value);
        seek.setOnSeekBarChangeListener(this);
        view.addView(seek);

        return view;
    }

    public void onDialogClosed(boolean positiveResult) {
        if(positiveResult) {
            persistInt(value);
        }
    }

    public void onProgressChanged(SeekBar seekBar, int progress,
        boolean fromUser) {
        value = seekBar.getProgress();
    }

    public void onStartTrackingTouch(SeekBar seek) {
    }

    public void onStopTrackingTouch(SeekBar seek) {
    }
}