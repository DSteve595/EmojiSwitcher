package com.stevenschoen.emojiswitcher;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.stevenschoen.emojiswitcher.network.EmojiSetListing;

import java.util.List;

public class EmojiSetSelectionAdapter extends ArrayAdapter<EmojiSetListing> {

    public EmojiSetSelectionAdapter(Context context, List<EmojiSetListing> objects) {
        super(context, android.R.layout.simple_spinner_item, objects);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }
}