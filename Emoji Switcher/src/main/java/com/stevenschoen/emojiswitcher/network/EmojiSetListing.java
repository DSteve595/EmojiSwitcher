package com.stevenschoen.emojiswitcher.network;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class EmojiSetListing implements Parcelable {
    public String name;
    public String url;
    public String md5;
    @SerializedName("google_play_sku")
    public String googlePlaySku;
    @SerializedName("default")
    public boolean selectByDefault;
    public boolean free;

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.url);
        dest.writeString(this.md5);
        dest.writeString(this.googlePlaySku);
        dest.writeByte(selectByDefault ? (byte) 1 : (byte) 0);
        dest.writeByte(free ? (byte) 1 : (byte) 0);
    }

    public EmojiSetListing() { }

    protected EmojiSetListing(Parcel in) {
        this.name = in.readString();
        this.url = in.readString();
        this.md5 = in.readString();
        this.googlePlaySku = in.readString();
        this.selectByDefault = in.readByte() != 0;
        this.free = in.readByte() != 0;
    }

    public static final Parcelable.Creator<EmojiSetListing> CREATOR = new Parcelable.Creator<EmojiSetListing>() {
        public EmojiSetListing createFromParcel(Parcel source) {
            return new EmojiSetListing(source);
        }

        public EmojiSetListing[] newArray(int size) {
            return new EmojiSetListing[size];
        }
    };
}