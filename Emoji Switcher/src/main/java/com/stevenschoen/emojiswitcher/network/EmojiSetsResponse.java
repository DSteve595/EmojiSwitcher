package com.stevenschoen.emojiswitcher.network;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class EmojiSetsResponse {
    @SerializedName("emoji_sets")
    public List<EmojiSetListing> emojiSets;
}
