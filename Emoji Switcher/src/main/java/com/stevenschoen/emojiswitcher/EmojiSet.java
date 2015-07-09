package com.stevenschoen.emojiswitcher;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.stevenschoen.emojiswitcher.network.EmojiSetListing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class EmojiSet extends EmojiSetListing {

    public File path;

    private String fileMd5;

    public EmojiSet(EmojiSetListing listing, File path) {
        name = listing.name;
        url = listing.url;
        md5 = listing.md5;
        googlePlaySku = listing.googlePlaySku;
        selectByDefault = listing.selectByDefault;
        this.path = path;
    }

    public String getFileMd5() throws IOException {
        if (fileMd5 == null) {
            fileMd5 = Files.hash(path, Hashing.md5()).toString();
        }

        return fileMd5;
    }

    public boolean isIntact() throws IOException {
        try {
            return (getFileMd5().equals(md5));
        } catch (FileNotFoundException e) {
            return false;
        }
    }
}