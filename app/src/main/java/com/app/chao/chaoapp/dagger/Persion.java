package com.app.chao.chaoapp.dagger;

import android.content.Context;
import android.util.Log;

/**
 * Created by Chao on 2017/4/6.
 */

public class Persion {
    private Context context;

    //@Inject  // 添加注解关键字
    public Persion(Context context) {
        this.context = context;
        Log.e("dagger:" + context, "person create!!!");
    }
}
