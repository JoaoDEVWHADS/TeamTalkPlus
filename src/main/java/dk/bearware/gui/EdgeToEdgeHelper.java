
package dk.bearware.gui;

import android.app.Activity;
import android.os.Build;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EdgeToEdgeHelper {

    public static void enableEdgeToEdge(Activity activity) {
        View rootView = activity.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            int imeInset = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;

            int bottomInset = Math.max(navBarInset, imeInset);

            v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), bottomInset);
            return insets;
        });
    }
}