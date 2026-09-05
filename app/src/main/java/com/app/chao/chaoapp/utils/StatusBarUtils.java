package com.app.chao.chaoapp.utils;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            setLegacyTransparentStatusBar(activity);
        }
    }

    @SuppressWarnings("deprecation")
    private static void setLegacyTransparentStatusBar(Activity activity) {
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    public static void applyTopInset(View view) {
        applyTopInset(view, view);
    }

    /** A full media page needs gesture-bar and landscape cutout padding as well as toolbar insets. */
    public static void applyPageInsets(View root, View toolbar) {
        applyTopInset(toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, 0, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    public static void applyTopInset(View insetSource, View target) {
        int initialPaddingTop = target.getPaddingTop();
        int initialHeight = target.getLayoutParams().height;
        ViewCompat.setOnApplyWindowInsetsListener(insetSource, (source, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            target.setPadding(target.getPaddingLeft(), initialPaddingTop + topInset,
                    target.getPaddingRight(), target.getPaddingBottom());
            if (initialHeight >= 0) {
                ViewGroup.LayoutParams params = target.getLayoutParams();
                params.height = initialHeight + topInset;
                target.setLayoutParams(params);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(insetSource);
    }
}
