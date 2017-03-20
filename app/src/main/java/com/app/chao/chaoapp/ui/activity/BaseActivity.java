package com.app.chao.chaoapp.ui.activity;

import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

/**
 * Created by Chao on 2017/3/14.
 */

public class BaseActivity extends AppCompatActivity {

    protected void showToast(String coutent) {
        Toast.makeText(this, coutent, Toast.LENGTH_SHORT).show();
    }


}
