
package dk.bearware.gui;

import android.content.Context;
import android.content.res.TypedArray;
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
    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 100;

    private int mValue = DEFAULT_VALUE;
    private int mMin = DEFAULT_MIN_VALUE;
    private int mMax = DEFAULT_MAX_VALUE;
    private TextView mValueText;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupLayout(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupLayout(context, attrs);
    }

    private void setupLayout(Context context, AttributeSet attrs) {
        mValue = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "defaultValue", DEFAULT_VALUE);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference);
        mMin = a.getInt(R.styleable.SeekBarPreference_min, DEFAULT_MIN_VALUE);
        mMax = a.getInt(R.styleable.SeekBarPreference_android_max, DEFAULT_MAX_VALUE);
        a.recycle();
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout view = new LinearLayout(getContext());
        view.setOrientation(LinearLayout.VERTICAL);
        int padding = getContext().getResources().getDimensionPixelSize(R.dimen.spacing_medium);
        view.setPadding(padding, padding, padding, padding);

        mValueText = new TextView(getContext());
        mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        mValueText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getContext().getResources().getDimension(R.dimen.text_size_preference));
        view.addView(mValueText);

        SeekBar seek = new SeekBar(getContext());
        // Range: 0 to (MAX - MIN)
        seek.setMax(mMax - mMin);
        
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
        if (mValue < mMin) mValue = mMin;
        if (mValue > mMax) mValue = mMax;

        seek.setProgress(mValue - mMin);
        updateValueText(mValue);
        
        seek.setOnSeekBarChangeListener(this);
        view.addView(seek);

        return view;
    }

    private void updateValueText(int value) {
        if (mValueText != null) {
            mValueText.setText(value + getContext().getString(R.string.unit_percent));
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
        mValue = progress + mMin;
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