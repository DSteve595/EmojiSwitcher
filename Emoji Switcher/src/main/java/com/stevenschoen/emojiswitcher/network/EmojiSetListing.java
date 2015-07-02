package com.stevenschoen.emojiswitcher.network;

import com.google.gson.annotations.SerializedName;

public class EmojiSetListing {
    public String name;
    public String url;
    public String md5;
    @SerializedName("google_play_sku")
    public String googlePlaySku;
    @SerializedName("default")
    public boolean selectByDefault;

    @Override
    public String toString() {
        return name;
    }
}