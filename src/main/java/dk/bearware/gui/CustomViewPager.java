package dk.bearware.gui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

/**
 * Custom ViewPager that can restrict swiping past specific boundaries.
 */
public class CustomViewPager extends ViewPager {

    private java.util.Set<Integer> restrictedPositions = new java.util.HashSet<>();
    private float initialX;

    public CustomViewPager(@NonNull Context context) {
        super(context);
    }

    public CustomViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Define which page positions cannot be reached via swiping.
     * These pages will only remain accessible via direct selection (e.g. tab click).
     */
    public void setRestrictedPositions(java.util.Set<Integer> restricted) {
        this.restrictedPositions = restricted;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (restrictedPositions.isEmpty()) {
            return super.onInterceptTouchEvent(ev);
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float diffX = ev.getX() - initialX;
                int current = getCurrentItem();
                
                // If swiping NEXT (diffX < 0) and current + 1 is restricted
                if (diffX < 0 && restrictedPositions.contains(current + 1)) {
                    return false;
                }
                
                // If swiping PREVIOUS (diffX > 0) and current - 1 is restricted
                if (diffX > 0 && restrictedPositions.contains(current - 1)) {
                    return false;
                }
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (restrictedPositions.isEmpty()) {
            return super.onTouchEvent(ev);
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float diffX = ev.getX() - initialX;
                int current = getCurrentItem();
                
                if (diffX < 0 && restrictedPositions.contains(current + 1)) {
                    return false;
                }
                
                if (diffX > 0 && restrictedPositions.contains(current - 1)) {
                    return false;
                }
                break;
        }

        return super.onTouchEvent(ev);
    }
}
