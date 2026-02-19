
package dk.bearware.gui;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SeekBarPreference extends DialogPreference implements
    OnSeekBarChangeListener {

    // Default values
    private static final int DEFAULT_VALUE = 100;
    private static final int MIN_VALUE = 50;
    private static final int MAX_VALUE = 300;

    private int mValue = DEFAULT_VALUE;
    private TextView mValueText;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupLayout(context, attrs);
        // Load default value if provided (though mostly handled by persist)
        mValue = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "defaultValue", DEFAULT_VALUE);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupLayout(context, attrs);
        mValue = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "defaultValue", DEFAULT_VALUE);
    }

    private void setupLayout(Context context, AttributeSet attrs) {
        // Can read custom attributes here if defined
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout view = new LinearLayout(getContext());
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(20, 20, 20, 20);

        mValueText = new TextView(getContext());
        mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        mValueText.setTextSize(18);
        view.addView(mValueText);

        SeekBar seek = new SeekBar(getContext());
        // Range: 0 to (MAX - MIN) i.e. 0 to 250
        seek.setMax(MAX_VALUE - MIN_VALUE);
        
        // Load persisted value
        if (shouldPersist()) {
            try {
                mValue = getPersistedInt(mValue);
            } catch (ClassCastException e) {
                // Legacy value might be a float string (e.g. "1.0")
                try {
                    String valStr = getPersistedString("1.0");
                    float valFloat = Float.parseFloat(valStr);
                    mValue = (int) (valFloat * 100);
                    persistInt(mValue); // Migrate to int immediately
                } catch (Exception ex) {
                    mValue = DEFAULT_VALUE;
                }
            }
        }

        // Ensure value is within bounds
        if (mValue < MIN_VALUE) mValue = MIN_VALUE;
        if (mValue > MAX_VALUE) mValue = MAX_VALUE;

        seek.setProgress(mValue - MIN_VALUE);
        updateValueText(mValue);
        
        seek.setOnSeekBarChangeListener(this);
        view.addView(seek);

        return view;
    }

    private void updateValueText(int value) {
        if (mValueText != null) {
            mValueText.setText(value + "%");
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            persistInt(mValue);
            callChangeListener(mValue);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mValue = progress + MIN_VALUE;
        updateValueText(mValue);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seek) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seek) {
    }
    
    // Allow external access to format the summary
    public static String getSummaryFormat(int value) {
        return value + "%";
    }
}