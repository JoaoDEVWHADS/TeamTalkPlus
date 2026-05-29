
package dk.bearware.gui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityAssistant extends AccessibilityDelegateCompat {

    public interface OnAccessibilityActionClickListener {
        void onAccessibilityActionClick(View view, int actionId);
        List<AccessibilityNodeInfoCompat.AccessibilityActionCompat> getCustomActions(View view);
    }

    private final Activity hostActivity;
    private final AccessibilityManager accessibilityService;

    private final SparseArray<View> monitoredPages;
    private View visiblePage;
    private int visiblePageId;

    private volatile boolean discourageUiUpdates;
    private volatile boolean eventsLocked;
    
    private OnAccessibilityActionClickListener actionClickListener;

    public AccessibilityAssistant(Activity activity) {
        hostActivity = activity;
        accessibilityService = (AccessibilityManager) activity.getSystemService(Context.ACCESSIBILITY_SERVICE);
        monitoredPages = new SparseArray<>();
        visiblePage = null;
        visiblePageId = 0;
        discourageUiUpdates = false;
        eventsLocked = false;
    }

    public void setOnAccessibilityActionClickListener(OnAccessibilityActionClickListener listener) {
        this.actionClickListener = listener;
    }

    public boolean isServiceActive() {
        return accessibilityService.isEnabled();
    }

    public void shutUp() {
        if (isServiceActive() && hostActivity.getWindow() != null) {
            View dev = hostActivity.getWindow().getDecorView();
            if (dev != null && !dev.post(accessibilityService::interrupt)) {
                accessibilityService.interrupt();
            }
        }
    }

    public boolean isUiUpdateDiscouraged() {
        return discourageUiUpdates && accessibilityService.isEnabled();
    }

    public void lockEvents() {
        eventsLocked = true;
    }

    public void unlockEvents() {
        if (hostActivity.getWindow() == null) {
            eventsLocked = false;
            return;
        }
        View dev = hostActivity.getWindow().getDecorView();
        if (dev == null || !dev.post(() -> eventsLocked = false))
            eventsLocked = false;
    }

    public void registerPage(View page, int id) {
        monitoredPages.put(id, page);
        if (id == visiblePageId)
            visiblePage = page;
        androidx.core.view.ViewCompat.setAccessibilityDelegate(page, this);
    }

    public void setVisiblePage(int id) {
        visiblePageId = id;
        visiblePage = monitoredPages.get(id);
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child, AccessibilityEvent event) {
        return ((monitoredPages.indexOfValue(host) < 0) || (host == visiblePage)) && super.onRequestSendAccessibilityEvent(host, child, event);
    }

    @Override
    public void sendAccessibilityEvent(View host, int eventType) {
        checkEvent(eventType);
        if (!eventsLocked)
            super.sendAccessibilityEvent(host, eventType);
    }

    @Override
    public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {
        checkEvent(event.getEventType());
        if (!eventsLocked)
            super.sendAccessibilityEventUnchecked(host, event);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
        super.onInitializeAccessibilityNodeInfo(host, info);
        if (actionClickListener != null) {
            List<AccessibilityNodeInfoCompat.AccessibilityActionCompat> actions = actionClickListener.getCustomActions(host);
            if (actions != null) {
                for (AccessibilityNodeInfoCompat.AccessibilityActionCompat action : actions) {
                    info.addAction(action);
                }
            }
        }
    }

    @Override
    public boolean performAccessibilityAction(View host, int action, Bundle args) {
        if (actionClickListener != null) {
            // Check if this is one of our registered custom actions
            List<AccessibilityNodeInfoCompat.AccessibilityActionCompat> customActions =
                    actionClickListener.getCustomActions(host);
            if (customActions != null) {
                for (AccessibilityNodeInfoCompat.AccessibilityActionCompat ca : customActions) {
                    if (ca.getId() == action) {
                        // Dispatch to listener and signal success to TalkBack
                        actionClickListener.onAccessibilityActionClick(host, action);
                        return true;
                    }
                }
            }
        }
        // Fall through to platform for standard actions (ACTION_CLICK, etc.)
        return super.performAccessibilityAction(host, action, args);
    }

    private void checkEvent(int eventType) {
        switch (eventType) {
        case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
            discourageUiUpdates = true;
            break;
        case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED:
            discourageUiUpdates = false;
            break;
        default:
            break;
        }
    }
}