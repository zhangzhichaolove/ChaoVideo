package com.app.chao.chaoapp.utils;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/** Edge-to-edge window and status-bar inset helpers. */
public final class StatusBarUtils {
    private StatusBarUtils() {
    }

    public static void setTranslucent(Activity activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    public static void applyTopInset(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (target, insets) -> {
            ViewGroup.LayoutParams params = target.getLayoutParams();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) params;
                margins.topMargin = insets.getInsets(
                        WindowInsetsCompat.Type.statusBars()).top;
                target.setLayoutParams(margins);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
