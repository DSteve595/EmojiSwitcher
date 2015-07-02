package com.stevenschoen.emojiswitcher.network;

import retrofit.http.GET;
import rx.Observable;

public interface NetworkInterface {
    String URL = "https://s3.amazonaws.com/emojiswitcher";

    @GET("/sets.json")
    Observable<EmojiSetsResponse> getEmojiSets();
}