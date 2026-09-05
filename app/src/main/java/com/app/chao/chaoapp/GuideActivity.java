package com.app.chao.chaoapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/** The system launch screen is followed immediately by the usable home page. */
public class GuideActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
