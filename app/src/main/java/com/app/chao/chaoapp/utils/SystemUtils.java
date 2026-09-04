package com.app.chao.chaoapp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.app.chao.chaoapp.App;

/** Network state helpers used by the HTTP cache policy. */
public final class SystemUtils {
    private SystemUtils() {
    }

    public static boolean isNetworkConnected() {
        ConnectivityManager manager = (ConnectivityManager) App.getInstance()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = manager.getActiveNetwork();
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(activeNetwork);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
