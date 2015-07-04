package com.stevenschoen.emojiswitcher;

import android.app.Application;

import com.stevenschoen.emojiswitcher.network.NetworkInterface;

import retrofit.RestAdapter;

public class EmojiSwitcherApplication extends Application {

    private NetworkInterface networkInterface;

    @Override
    public void onCreate() {
        super.onCreate();

        networkInterface = new RestAdapter.Builder()
                .setEndpoint(NetworkInterface.URL)
                .build().create(NetworkInterface.class);
    }

    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }
}
