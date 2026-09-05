package com.app.chao.chaoapp.playback;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import com.app.chao.chaoapp.R;

/** Both players expose the same preference choices. Callers own dialog lifecycle. */
public final class VideoSettingsDialogs {
    private VideoSettingsDialogs() { }

    public static AlertDialog speed(Context context, VideoPlaybackSettings settings, Runnable apply) {
        float[] values = {0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f};
        String[] labels = new String[values.length];
        int selected = -1;
        for (int i = 0; i < values.length; i++) {
            labels[i] = context.getString(R.string.speed_value,
                    java.math.BigDecimal.valueOf(values[i]).stripTrailingZeros().toPlainString());
            if (values[i] == settings.speed()) selected = i;
        }
        return new AlertDialog.Builder(context).setTitle(R.string.playback_speed)
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    settings.setSpeed(values[which]);
                    apply.run();
                    dialog.dismiss();
                }).setNegativeButton(R.string.cancel, null).show();
    }

    public static AlertDialog aspect(Context context, VideoPlaybackSettings settings, Runnable apply) {
        String[] labels = {context.getString(R.string.aspect_fit), context.getString(R.string.aspect_crop),
                context.getString(R.string.aspect_stretch)};
        return new AlertDialog.Builder(context).setTitle(R.string.video_aspect)
                .setSingleChoiceItems(labels, settings.aspect(), (dialog, which) -> {
                    settings.setAspect(which);
                    apply.run();
                    dialog.dismiss();
                }).setNegativeButton(R.string.cancel, null).show();
    }
}
