package fm.jiecao.jcvideoplayer_lib;

import android.content.Context;

/**
 * Created by Chao on 2017/3/17.
 */

public class APPS {
    private static Context app;

    public static void init(Context p) {
        app = p;
    }

    public static Context getInstance() {
        return app;
    }
}
