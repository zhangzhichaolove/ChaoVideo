package com.app.chao.chaoapp.playback;

import android.content.Context;
import android.os.Looper;
import android.os.Message;
import androidx.media3.common.text.CueGroup;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.text.TextOutput;
import com.shuyu.gsyvideoplayer.cache.ICacheManager;
import com.shuyu.gsyvideoplayer.model.VideoOptionModel;
import java.util.ArrayList;
import java.util.List;
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager;
import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer;

/** GSY's IjkExo2 onCues is a no-op; tee the actual Media3 text renderer without reflecting into its engine. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class SubtitlePlayerManager extends Exo2PlayerManager {
    private final GsyCueOutput subtitles = new GsyCueOutput();
    public GsyCueOutput subtitleOutput() { return subtitles; }

    @Override public void initVideoPlayer(Context context, Message message, List<VideoOptionModel> options, ICacheManager cache) {
        super.initVideoPlayer(context, message, options, cache);
        DefaultRenderersFactory factory = new DefaultRenderersFactory(context.getApplicationContext()) {
            @Override protected void buildTextRenderers(Context context, TextOutput output, Looper looper,
                                                        int extensionMode, ArrayList<Renderer> renderers) {
                super.buildTextRenderers(context, new TextOutput() {
                    @Override public void onCues(CueGroup cues) {
                        output.onCues(cues);
                        subtitles.onCues(cues);
                    }
                }, looper, extensionMode, renderers);
            }
        };
        // Preserve the extension preference used by GSY 12.1's default renderer factory.
        factory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);
        ((IjkExo2MediaPlayer) getMediaPlayer()).setRendererFactory(factory);
    }

    @Override public void release() {
        subtitles.close();
        super.release();
    }
}
